//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t H e a d I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.BasicGlyph;
import omr.glyph.GlyphIndex;
import omr.glyph.Shape;

import omr.image.Anchored.Anchor;
import omr.image.ShapeDescriptor;
import omr.image.Template;
import omr.image.TemplateFactory;
import omr.image.TemplateFactory.Catalog;

import omr.math.PointUtil;
import static omr.run.Orientation.VERTICAL;
import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.sheet.Staff;
import omr.sheet.rhythm.Measure;
import omr.sheet.rhythm.MeasureStack;
import omr.sheet.rhythm.Slot;

import omr.sig.BasicImpacts;
import omr.sig.GradeImpacts;
import omr.sig.relation.AccidHeadRelation;
import omr.sig.relation.Relation;

import omr.util.ByteUtil;
import omr.util.Jaxb;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ListIterator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code AbstractHeadInter} is the base class for notes heads, that is
 * all notes, including whole and breve, but not rests.
 * <p>
 * These rather round-shaped symbols are retrieved via template-matching technique.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractHeadInter
        extends AbstractNoteInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            AbstractHeadInter.class);

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Absolute location of head template pivot. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.PointAdapter.class)
    protected final Point pivot;

    /** Relative pivot position WRT head. */
    @XmlElement
    protected final Anchor anchor;

    /** Shape template descriptor. */
    protected ShapeDescriptor descriptor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AbstractTemplateNoteInter} object.
     *
     * @param pivot   the template pivot
     * @param anchor  relative pivot configuration
     * @param bounds  the object bounds
     * @param shape   the underlying shape
     * @param impacts the grade details
     * @param staff   the related staff
     * @param pitch   the note pitch
     */
    public AbstractHeadInter (Point pivot,
                              Anchor anchor,
                              Rectangle bounds,
                              Shape shape,
                              GradeImpacts impacts,
                              Staff staff,
                              double pitch)
    {
        super(null, bounds, shape, impacts, staff, pitch);
        this.pivot = pivot;
        this.anchor = anchor;
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
    // getAccidental //
    //---------------//
    /**
     * Report the (local) accidental, if any, related to this head.
     *
     * @return the related accidental, or null
     */
    public AlterInter getAccidental ()
    {
        for (Relation rel : sig.getRelations(this, AccidHeadRelation.class)) {
            return (AlterInter) sig.getOppositeInter(this, rel);
        }

        return null;
    }

    //----------//
    // getAlter //
    //----------//
    /**
     * Report the actual alteration of this note, taking into account the accidental of
     * this note if any, the accidental of previous note with same step within the same
     * measure, and finally the current key signature.
     *
     * @param fifths fifths value for current key signature
     * @return the actual alteration
     */
    public int getAlter (Integer fifths)
    {
        // Look for local accidental
        AlterInter accidental = getAccidental();

        if (accidental != null) {
            return alterationOf(accidental);
        }

        // Look for previous accidental with same note step in the measure
        Measure measure = getChord().getMeasure();
        MeasureStack stack = measure.getStack();
        List<Slot> slots = stack.getSlots();

        boolean started = false;

        for (ListIterator<Slot> it = slots.listIterator(slots.size()); it.hasPrevious();) {
            Slot slot = it.previous();

            // Inspect all notes of all chords
            for (AbstractChordInter chord : slot.getChords()) {
                if (chord.isRest() || (chord.getMeasure() != measure)) {
                    continue;
                }

                for (Inter inter : chord.getNotes()) {
                    AbstractHeadInter note = (AbstractHeadInter) inter;

                    if (note == this) {
                        started = true;
                    } else if (started && (note.getStep() == getStep())) {
                        AlterInter accid = note.getAccidental();

                        if (accid != null) {
                            return alterationOf(accid);
                        }
                    }
                }
            }
        }

        // Finally, use the current key signature
        if (fifths != null) {
            return KeyInter.getAlterFor(getStep(), fifths);
        }

        // Nothing found, so...
        return 0;
    }

    //----------//
    // getChord //
    //----------//
    @Override
    public AbstractChordInter getChord ()
    {
        return (AbstractChordInter) getEnsemble();
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
    public ShapeDescriptor getDescriptor (int interline)
    {
        if (descriptor == null) {
            final Catalog catalog = TemplateFactory.getInstance().getCatalog(interline);
            descriptor = catalog.getDescriptor(shape);
        }

        return descriptor;
    }

    //---------------//
    // getDescriptor //
    //---------------//
    public ShapeDescriptor getDescriptor ()
    {
        if (descriptor == null) {
            final int interline = sig.getSystem().getSheet().getInterline();

            return getDescriptor(interline);
        }

        return descriptor;
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
     * Specific overlap implementation between notes, based on their pitch value.
     * <p>
     * TODO: A clean overlap check might use true distance tables around each of the heads.
     * For the time being, we simply play with the width and area of intersection rectangle.
     *
     * @param that another inter (perhaps a note)
     * @return true if overlap is detected
     */
    @Override
    public boolean overlaps (Inter that)
    {
        // Specific between notes
        if (that instanceof AbstractHeadInter) {
            AbstractHeadInter thatNote = (AbstractHeadInter) that;

            // Check vertical distance
            if (Math.abs(thatNote.getIntegerPitch() - getIntegerPitch()) > 1) {
                return false;
            }

            // Check horizontal distance
            Rectangle thisBounds = this.getBounds();
            Rectangle thatBounds = thatNote.getBounds();
            Rectangle common = thisBounds.intersection(thatBounds);

            if (common.width <= 0) {
                return false;
            }

            int thisArea = thisBounds.width * thisBounds.height;
            int thatArea = thatBounds.width * thatBounds.height;
            int minArea = Math.min(thisArea, thatArea);
            int commonArea = common.width * common.height;
            double areaRatio = (double) commonArea / minArea;
            boolean res = (common.width > (constants.maxOverlapDxRatio.getValue() * thisBounds.width))
                          && (areaRatio > constants.maxOverlapAreaRatio.getValue());

            //logger.info("*** {}% {} {} vs {}", (int) Math.rint(frac * 100), res, this, that);
            if (this.isVip() || that.isVip()) {
                logger.info(
                        "VIP {} vs {} dx:{} areaRatio:{} overlap:{}",
                        this,
                        that,
                        common.width,
                        areaRatio,
                        res);
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
     * @param image     the image to read pixels from
     * @param interline scaling info
     * @param nest      the nest to hold the created glyph
     */
    public void retrieveGlyph (ByteProcessor image,
                               int interline,
                               GlyphIndex nest)
    {
        final Template tpl = getDescriptor(interline).getTemplate();
        final Rectangle interBox = getBounds();
        final Rectangle descBox = getDescriptor().getBounds(interBox);

        // Foreground points (coordinates WRT descBox)
        final List<Point> fores = tpl.getForegroundPixels(descBox, image);
        final Rectangle foreBox = PointUtil.boundsOf(fores);

        final ByteProcessor buf = new ByteProcessor(foreBox.width, foreBox.height);
        ByteUtil.raz(buf);

        for (Point p : fores) {
            buf.set(p.x - foreBox.x, p.y - foreBox.y, 0);
        }

        // Runs
        RunTable runTable = new RunTableFactory(VERTICAL).createTable(buf);

        // Glyph
        glyph = new BasicGlyph(descBox.x + foreBox.x, descBox.y + foreBox.y, runTable);
        nest.register(glyph);
    }

    //--------//
    // shrink //
    //--------//
    /**
     * Shrink a bit a bounding bounds when checking for note overlap.
     *
     * @param box the bounding bounds
     * @return the shrunk bounds
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

    //--------------//
    // alterationOf //
    //--------------//
    /**
     * Report the pitch alteration that corresponds to the provided accidental.
     *
     * @param accidental the provided accidental
     * @return the pitch impact
     */
    private int alterationOf (AlterInter accidental)
    {
        switch (accidental.getShape()) {
        case SHARP:
            return 1;

        case DOUBLE_SHARP:
            return 2;

        case FLAT:
            return -1;

        case DOUBLE_FLAT:
            return -2;

        case NATURAL:
            return 0;

        default:
            logger.warn(
                    "Weird shape {} for accidental {}",
                    accidental.getShape(),
                    accidental.getId());

            return 0; // Should not happen
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //
    //    //-----------//
    //    // internals //
    //    //-----------//
    //    @Override
    //    protected String internals ()
    //    {
    //        return super.internals() + " " + anchor + "@[" + pivot.x + "," + pivot.y + "]";
    //    }
    //
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

        private final Constant.Ratio shrinkHoriRatio = new Constant.Ratio(
                0.5,
                "Horizontal shrink ratio to apply when checking note overlap");

        private final Constant.Ratio shrinkVertRatio = new Constant.Ratio(
                0.5,
                "Vertical shrink ratio to apply when checking note overlap");

        private final Constant.Ratio maxOverlapDxRatio = new Constant.Ratio(
                0.2,
                "Maximum acceptable abscissa overlap ratio between notes");

        private final Constant.Ratio maxOverlapAreaRatio = new Constant.Ratio(
                0.2,
                "Maximum acceptable box area overlap ratio between notes");
    }
}
