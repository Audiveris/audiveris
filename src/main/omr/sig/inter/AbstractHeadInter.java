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

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.image.Anchored.Anchor;
import omr.image.ShapeDescriptor;
import omr.image.Template;

import omr.lag.JunctionRatioPolicy;
import omr.lag.Section;
import omr.lag.SectionFactory;
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

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ListIterator;

/**
 * Class {@code AbstractHeadInter} is the base class for notes heads, that is
 * all notes, including whole and breve, but not rests.
 * <p>
 * These rather round-shaped symbols are retrieved via template-matching technique.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractHeadInter
        extends AbstractNoteInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractHeadInter.class);

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Shape template descriptor. */
    protected final ShapeDescriptor descriptor;

    /** Absolute location of head template pivot. */
    protected final Point pivot;

    /** Relative pivot position WRT head. */
    protected final Anchor anchor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AbstractTemplateNoteInter} object.
     *
     * @param descriptor the shape template descriptor
     * @param pivot      the template pivot
     * @param anchor     relative pivot configuration
     * @param box        the object bounds
     * @param shape      the underlying shape
     * @param impacts    the grade details
     * @param staff      the related staff
     * @param pitch      the note pitch
     */
    public AbstractHeadInter (ShapeDescriptor descriptor,
                              Point pivot,
                              Anchor anchor,
                              Rectangle box,
                              Shape shape,
                              GradeImpacts impacts,
                              Staff staff,
                              double pitch)
    {
        super(null, box, shape, impacts, staff, pitch);
        this.descriptor = descriptor;
        this.pivot = pivot;
        this.anchor = anchor;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
            for (ChordInter chord : slot.getChords()) {
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
    public ChordInter getChord ()
    {
        return (ChordInter) getEnsemble();
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
            Rectangle common = box.intersection(thatNote.box);

            if (common.width <= 0) {
                return false;
            }

            int thisArea = box.width * box.height;
            int thatArea = thatNote.box.width * thatNote.box.height;
            int minArea = Math.min(thisArea, thatArea);
            int commonArea = common.width * common.height;
            double areaRatio = (double) commonArea / minArea;
            boolean res = (common.width > (constants.maxOverlapDxRatio.getValue() * box.width))
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
     * @param image the image to read pixels from
     * @param nest  the nest to hold the created glyph
     */
    public void retrieveGlyph (ByteProcessor image,
                               GlyphNest nest)
    {
        final Template tpl = descriptor.getTemplate();
        final Rectangle descBox = descriptor.getBounds(getBounds());
        final Point boxLocation = descBox.getLocation();
        final List<Point> fores = tpl.getForegroundPixels(descBox, image);

        ByteProcessor buf = new ByteProcessor(descBox.width, descBox.height);
        buf.invert();

        for (Point p : fores) {
            buf.set(p.x, p.y, 0);
        }

        // Runs
        RunTable runTable = new RunTableFactory(VERTICAL).createTable("note", buf);

        // Sections
        SectionFactory sectionFactory = new SectionFactory(VERTICAL, new JunctionRatioPolicy());
        List<Section> sections = sectionFactory.createSections(runTable);

        // Translate sections to absolute coordinates
        for (Section section : sections) {
            section.translate(boxLocation);
        }

        glyph = nest.buildGlyph(sections, GlyphLayer.DEFAULT, true, Glyph.Linking.NO_LINK);
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

        final Constant.Ratio shrinkHoriRatio = new Constant.Ratio(
                0.5,
                "Horizontal shrink ratio to apply when checking note overlap");

        final Constant.Ratio shrinkVertRatio = new Constant.Ratio(
                0.5,
                "Vertical shrink ratio to apply when checking note overlap");

        final Constant.Ratio maxOverlapDxRatio = new Constant.Ratio(
                0.2,
                "Maximum acceptable abscissa overlap ratio between notes");

        final Constant.Ratio maxOverlapAreaRatio = new Constant.Ratio(
                0.2,
                "Maximum acceptable box area overlap ratio between notes");
    }
}
