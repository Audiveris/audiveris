//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        H e a d I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
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
import omr.sig.relation.AlterHeadRelation;
import omr.sig.relation.HeadStemRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.SlurHeadRelation;

import omr.util.ByteUtil;
import omr.util.HorizontalSide;

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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code HeadInter} represents a note head, that is any head shape including
 * whole and breve, but not a rest.
 * <p>
 * These rather round-shaped symbols are retrieved via template-matching technique.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "head")
@XmlAccessorType(XmlAccessType.NONE)
public class HeadInter
        extends AbstractNoteInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HeadInter.class);

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Absolute location of head template pivot. */
    @XmlElement
    private final Point pivot;

    /** Relative pivot position WRT head. */
    @XmlAttribute
    private final Anchor anchor;

    // Transient data
    //---------------
    //
    /** Shape template descriptor. */
    private ShapeDescriptor descriptor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code HeadInter} object.
     *
     * @param pivot   the template pivot
     * @param anchor  relative pivot configuration
     * @param bounds  the object bounds
     * @param shape   the underlying shape
     * @param impacts the grade details
     * @param staff   the related staff
     * @param pitch   the note pitch
     */
    public HeadInter (Point pivot,
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

    /** No-arg constructor needed by JAXB. */
    private HeadInter ()
    {
        this.pivot = null;
        this.anchor = null;
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

    //-----------//
    // duplicate //
    //-----------//
    public HeadInter duplicate ()
    {
        return duplicateAs(shape);
    }

    //-------------//
    // duplicateAs //
    //-------------//
    public HeadInter duplicateAs (Shape shape)
    {
        HeadInter clone = new HeadInter(pivot, anchor, bounds, shape, impacts, staff, pitch);
        clone.setGlyph(this.glyph);
        clone.setMirror(this);

        if (impacts == null) {
            clone.setGrade(this.grade);
        }

        sig.addVertex(clone);
        setMirror(clone);

        return clone;
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
        for (Relation rel : sig.getRelations(this, AlterHeadRelation.class)) {
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
     * measure, a tie from previous measure and finally the current key signature.
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
                    HeadInter note = (HeadInter) inter;

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

        // Look for tie from previous measure (same system or previous system)
        for (Relation rel : sig.getRelations(this, SlurHeadRelation.class)) {
            SlurInter slur = (SlurInter) sig.getOppositeInter(this, rel);

            if (slur.isTie() && (slur.getHead(HorizontalSide.RIGHT) == this)) {
                // Is the starting head in same system?
                HeadInter startHead = slur.getHead(HorizontalSide.LEFT);

                if (startHead != null) {
                    // Use start head alter
                    return startHead.getAlter(fifths);
                }

                // Use slur extension to look into previous system
                SlurInter prevSlur = slur.getExtension(HorizontalSide.LEFT);

                if (prevSlur != null) {
                    startHead = prevSlur.getHead(HorizontalSide.LEFT);

                    if (startHead != null) {
                        // Use start head alter
                        return startHead.getAlter(fifths);
                    }
                }

                // TODO: Here we should look in previous sheet/page...
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
     * Precise overlap implementation between notes, based on their pitch value.
     * <p>
     * TODO: A clean overlap check might use true distance tables around each of the heads.
     * For the time being, we simply play with the width and area of intersection rectangle.
     *
     * @param that another inter (perhaps a note)
     * @return true if overlap is detected
     * @throws omr.sig.inter.DeletedInterException
     */
    @Override
    public boolean overlaps (Inter that)
            throws DeletedInterException
    {
        // Specific between notes
        if (that instanceof HeadInter) {
            if (this.isVip() && ((HeadInter) that).isVip()) {
                logger.info("AbstractHeadInter checking overlaps between {} and {}", this, that);
            }

            HeadInter thatHead = (HeadInter) that;

            // Check vertical distance
            if (this.getStaff() == that.getStaff()) {
                if (Math.abs(thatHead.getIntegerPitch() - getIntegerPitch()) > 1) {
                    return false;
                }
            } else {
                // We have two note heads from different staves and with overlapping bounds!
                fixDuplicateWith(thatHead); // Throws DeletedInterException when fixed

                return true;
            }

            // Check horizontal distance
            Rectangle thisBounds = this.getBounds();
            Rectangle thatBounds = thatHead.getBounds();
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

            return res;

            //        } else if (that instanceof StemInter) {
            //            // Head with respect to a stem
            //            // First, standard check
            //            if (!Glyphs.intersect(this.getGlyph(), that.getGlyph(), false)) {
            //                return false;
            //            }
            //
            //            // Second, limit stem vertical range to connection points of ending heads if any
            //            // (Assuming wrong-side ending heads have been pruned beforehand)
            //            StemInter stem = (StemInter) that;
            //            Line2D line = stem.computeAnchoredLine();
            //            int top = (int) Math.ceil(line.getY1());
            //            int bottom = (int) Math.floor(line.getY2());
            //            Rectangle box = stem.getBounds();
            //            Rectangle anchorRect = new Rectangle(box.x, top, box.width, bottom - top + 1);
            //
            //            return this.getCoreBounds().intersects(anchorRect);
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
     * @param image      the image to read pixels from
     * @param interline  scaling info
     * @param glyphIndex the index to hold the created glyph
     */
    public void retrieveGlyph (ByteProcessor image,
                               int interline,
                               GlyphIndex glyphIndex)
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
        glyph = glyphIndex.registerOriginal(
                new BasicGlyph(descBox.x + foreBox.x, descBox.y + foreBox.y, runTable));

        // Use glyph bounds as inter bounds
        bounds = glyph.getBounds();
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

    //------------------//
    // fixDuplicateWith //
    //------------------//
    /**
     * Fix head duplication on two staves.
     * <p>
     * We have two note heads from different staves and with overlapping bound.
     * Vertical gap between the staves must be small and crowded, leading to head being "duplicated"
     * in both staves.
     * <p>
     * Assuming there is a linked stem, we could use sibling stem/head in a beam group if any.
     * Or we can simply use stem direction, assumed to point to the "true" containing staff.
     *
     * @param that the other inter
     */
    private void fixDuplicateWith (HeadInter that)
            throws DeletedInterException
    {
        for (Relation rel : sig.getRelations(this, HeadStemRelation.class)) {
            StemInter thisStem = (StemInter) sig.getOppositeInter(this, rel);
            int thisDir = thisStem.computeDirection();
            Inter dupli = ((thisDir * (that.getStaff().getId() - this.getStaff().getId())) > 0)
                    ? this : that;

            logger.debug("Deleting duplicated {}", dupli);
            dupli.delete();
            throw new DeletedInterException(dupli);
        }

        //TODO: What if we have no stem? It's a WHOLE_NOTE or SMALL_WHOLE_NOTE
        // Perhaps check for a weak ledger, tangent to the note towards staff
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
                0.25,
                "Maximum acceptable box area overlap ratio between notes");
    }
}
