//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t N o t e I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.image.ShapeDescriptor;
import omr.image.Template;

import omr.lag.BasicLag;
import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.SectionsBuilder;

import omr.run.Orientation;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Class {@code AbstractNoteInter} is an abstract base for heads and notes interpretations.
 *
 * @author Hervé Bitteur
 */
public class AbstractNoteInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractNoteInter.class);

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Shape template descriptor. */
    protected final ShapeDescriptor descriptor;

    /** Pitch step. */
    protected final int pitch;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AbstractNoteInter object.
     *
     * @param descriptor the shape template descriptor
     * @param box        the object bounds
     * @param shape      the underlying shape
     * @param impacts    the grade details
     * @param pitch      the note pitch
     */
    public AbstractNoteInter (ShapeDescriptor descriptor,
                              Rectangle box,
                              Shape shape,
                              GradeImpacts impacts,
                              int pitch)
    {
        super(box, shape, impacts);
        this.descriptor = descriptor;
        this.pitch = pitch;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //---------------//
    // getCoreBounds //
    //---------------//
    @Override
    public Rectangle2D getCoreBounds ()
    {
        return shrink(getBounds());
    }

    //---------------//
    // getDescriptor //
    //---------------//
    public ShapeDescriptor getDescriptor ()
    {
        return descriptor;
    }

    //----------//
    // getPitch //
    //----------//
    /**
     * @return the pitch
     */
    public int getPitch ()
    {
        return pitch;
    }

    //--------------------//
    // getShrinkHoriRatio //
    //--------------------//
    public static double getShrinkHoriRatio ()
    {
        return constants.shrinkHoriRatio.getValue();
    }

    //--------------------//
    // getShrinkVertRatio //
    //--------------------//
    public static double getShrinkVertRatio ()
    {
        return constants.shrinkVertRatio.getValue();
    }

    //----------//
    // overlaps //
    //----------//
    /**
     * Specific overlap implementation between notes, based on their pitch value
     *
     * @param that another inter (perhaps a note)
     * @return true if overlap is detected
     */
    @Override
    public boolean overlaps (Inter that)
    {
        // Specific between notes
        if (that instanceof AbstractNoteInter) {
            AbstractNoteInter thatNote = (AbstractNoteInter) that;

            // Check vertical distance
            if (Math.abs(thatNote.getPitch() - pitch) > 1) {
                return false;
            }

            // Check horizontal distance
            Rectangle common = box.intersection(thatNote.box);
            boolean res = common.width > (constants.maxOverlapDxRatio.getValue() * box.width);

            if (this.isVip() || that.isVip()) {
                logger.info("{} vs {} dx:{} overlap:{}", getId(), that.getId(), common.width, res);
            }

            return res;
        }

        // Basic test
        return super.overlaps(that);
    }

    //---------------//
    // retrieveGlyph //
    //---------------//
    /**
     * Use descriptor to build an underlying glyph.
     *
     * @param image the image to read pixels from
     * @param nest  the nest to hold the created glyph
     */
    public void retrieveGlyph (ByteProcessor image,
                               GlyphNest nest)
    {
        final boolean hasLine = (pitch % 2) == 0;
        final Template tpl = descriptor.getTemplate(new Template.Key(getShape(), hasLine));
        final Rectangle descBox = descriptor.getBounds(getBounds());
        final List<Point> fores = tpl.getForegroundPixels(descBox, image);

        ByteProcessor buf = new ByteProcessor(descBox.width, descBox.height);
        buf.invert();

        for (Point p : fores) {
            buf.set(p.x, p.y, 0);
        }

        // Runs
        RunsTable runTable = new RunsTableFactory(Orientation.VERTICAL, buf, 0).createTable(
                "note");

        // Sections
        Lag lag = new BasicLag("note", Orientation.VERTICAL);
        SectionsBuilder sectionsBuilder = new SectionsBuilder(lag, new JunctionRatioPolicy());
        List<Section> sections = sectionsBuilder.createSections(runTable, false);

        // Translate sections to absolute coordinates
        for (Section section : sections) {
            section.translate(descBox.getLocation());
        }

        glyph = nest.buildGlyph(sections, GlyphLayer.DEFAULT, true, Glyph.Linking.NO_LINK);
    }

    //--------//
    // shrink //
    //--------//
    /**
     * Shrink a bit a bounding box when checking for note overlap.
     *
     * @param box the bounding box
     * @return the shrunk box
     */
    public static Rectangle2D shrink (Rectangle box)
    {
        double newWidth = constants.shrinkHoriRatio.getValue() * box.width;
        double newHeight = constants.shrinkVertRatio.getValue() * box.height;

        return new Rectangle2D.Double(
                box.getCenterX() - (newWidth / 2.0),
                box.getCenterY() - (newHeight / 2.0),
                newWidth,
                newHeight);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends BasicImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[]{"dist"};

        private static final double[] WEIGHTS = new double[]{1};

        //~ Constructors ---------------------------------------------------------------------------
        public Impacts (double dist)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, dist);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Ratio shrinkHoriRatio = new Constant.Ratio(
                0.5,
                "Horizontal shrink ratio to apply when checking note overlap");

        final Constant.Ratio shrinkVertRatio = new Constant.Ratio(
                0.5,
                "Vertical shrink ratio to apply when checking note overlap");

        final Constant.Ratio maxOverlapDxRatio = new Constant.Ratio(
                0.2,
                "Maximum acceptable abscissa overlap ratio between notes");
    }
}
