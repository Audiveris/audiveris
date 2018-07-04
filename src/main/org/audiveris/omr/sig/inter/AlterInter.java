//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       A l t e r I n t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.AlterHeadRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omrdataset.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code AlterInter} represents an alteration (sharp, flat, natural,
 * double-sharp, double-flat).
 * It can be an accidental alteration or a part of a key signature.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "alter")
public class AlterInter
        extends AbstractPitchedInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AlterInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Measured pitch value. */
    private Double measuredPitch;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AlterInter object.
     *
     * @param glyph         underlying glyph
     * @param shape         precise shape
     * @param grade         evaluation value
     * @param staff         the related staff
     * @param pitch         the pitch value WRT staff
     * @param measuredPitch the measured pitch
     */
    public AlterInter (Glyph glyph,
                       Shape shape,
                       double grade,
                       Staff staff,
                       Double pitch,
                       Double measuredPitch)
    {
        super(glyph, null, shape, grade, staff, pitch);
        this.measuredPitch = measuredPitch;
    }

    /**
     * Creates a new AlterInter object.
     *
     * @param annotationId  ID of the original annotation if any
     * @param bounds        bounding box
     * @param omrShape      precise shape
     * @param grade         evaluation value
     * @param staff         the related staff
     * @param pitch         the pitch value WRT staff
     * @param measuredPitch the measured pitch
     */
    public AlterInter (int annotationId,
                       Rectangle bounds,
                       OmrShape omrShape,
                       double grade,
                       Staff staff,
                       Double pitch,
                       Double measuredPitch)
    {
        super(annotationId, bounds, omrShape, grade, staff, pitch);
        this.measuredPitch = measuredPitch;
    }

    /**
     * Creates a new AlterlInter object.
     *
     * @param glyph         underlying glyph
     * @param shape         precise shape
     * @param impacts       assignment details
     * @param staff         the related staff
     * @param pitch         the pitch value WRT staff
     * @param measuredPitch the measured pitch
     */
    public AlterInter (Glyph glyph,
                       Shape shape,
                       GradeImpacts impacts,
                       Staff staff,
                       Double pitch,
                       Double measuredPitch)
    {
        super(glyph, null, shape, impacts, staff, pitch);
        this.measuredPitch = measuredPitch;
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private AlterInter ()
    {
        super(null, null, null, null, null, null);
        this.measuredPitch = null;
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

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        setAbnormal(true); // No head linked yet
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if a head is connected
        setAbnormal(!sig.hasRelation(this, AlterHeadRelation.class));

        return isAbnormal();
    }

    //--------//
    // create //
    //--------//
    /**
     * Create an Alter inter, with a grade value, determining pitch WRT provided staff.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param staff closest staff (questionable)
     * @return the created instance
     */
    public static AlterInter create (Glyph glyph,
                                     Shape shape,
                                     double grade,
                                     Staff staff)
    {
        Pitches pitches = computePitch(glyph, shape, staff);

        return new AlterInter(glyph, shape, grade, staff, pitches.pitch, pitches.measuredPitch);
    }

    //--------//
    // create //
    //--------//
    /**
     * Create an Alter inter, with a grade value, determining pitch WRT provided staff.
     *
     * @param annotationId ID of original annotation if any
     * @param bounds       bounding box
     * @param omrShape     detected shape
     * @param grade        evaluation value
     * @param staff        closest staff (questionable)
     * @return the created instance
     */
    public static AlterInter create (int annotationId,
                                     Rectangle bounds,
                                     OmrShape omrShape,
                                     double grade,
                                     Staff staff)
    {
        Pitches p = computePitch(bounds, omrShape, staff);

        return new AlterInter(
                annotationId,
                bounds,
                omrShape,
                grade,
                staff,
                p.pitch,
                p.measuredPitch);
    }

    //--------//
    // create //
    //--------//
    /**
     * Create an Alter inter, with impacts data, determining pitch WRT provided staff.
     *
     * @param glyph   underlying glyph
     * @param shape   precise shape
     * @param impacts assignment details
     * @param staff   related staff
     * @return the created instance
     */
    public static AlterInter create (Glyph glyph,
                                     Shape shape,
                                     GradeImpacts impacts,
                                     Staff staff)
    {
        Pitches pitches = computePitch(glyph, shape, staff);

        return new AlterInter(glyph, shape, impacts, staff, pitches.pitch, pitches.measuredPitch);
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        if (measuredPitch != null) {
            return super.getDetails() + String.format(" mPitch:%.1f", measuredPitch);
        }

        return super.getDetails();
    }

    //-------------------//
    // getFlatAreaOffset //
    //-------------------//
    /**
     * Report for a flat sign the center offset WRT area center.
     *
     * @return height offset of pitch
     */
    public static double getFlatAreaOffset ()
    {
        return constants.flatAreaOffset.getValue();
    }

    //--------------------//
    // getFlatPitchOffset //
    //--------------------//
    /**
     * Report for a flat sign the vertical offset of pitch ordinate WRT sign top ordinate.
     *
     * @return height offset of pitch
     */
    public static double getFlatPitchOffset ()
    {
        return constants.flatPitchOffset.getValue();
    }

    /**
     * @return the measuredPitch
     */
    public Double getMeasuredPitch ()
    {
        return measuredPitch;
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    @Override
    public Point getRelationCenter ()
    {
        Point center = getCenter();

        switch (shape) {
        case FLAT:
        case DOUBLE_FLAT:
            return new Point(
                    center.x,
                    (int) Math.rint(center.y + (getFlatAreaOffset() * getBounds().height)));

        default:
            return center;
        }
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, AlterHeadRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system,
                                         boolean doit)
    {
        // Not very optimized!
        List<Inter> systemHeads = system.getSig().inters(HeadInter.class);
        Collections.sort(systemHeads, Inters.byAbscissa);

        Collection<Link> links = lookupLinks(systemHeads);

        if (doit) {
            for (Link link : links) {
                link.applyTo(this);
            }
        }

        return links;
    }

    //----------//
    // setLinks //
    //----------//
    /**
     * Try to detect relation with heads nearby (perhaps with their mirror).
     *
     * @param systemHeads ordered collection of heads in system
     */
    public void setLinks (List<Inter> systemHeads)
    {
        Collection<Link> links = lookupLinks(systemHeads);

        for (Link link : links) {
            link.applyTo(this);
        }
    }

    //--------------//
    // computePitch //
    //--------------//
    /**
     * Compute pitch (integer) and measuredPitch (double) values related to the provided
     * staff, according to alteration glyph and shape.
     * <p>
     * Sharp and natural signs are symmetric, hence their pitch can be directly derived from
     * centroid ordinate.
     * <p>
     * But sharp signs are not symmetric, hence we need a more precise point.
     * We use two heuristics:<ul>
     * <li>Augment centroid pitch by a fixed pitch offset, around 0.65</li>
     * <li>Use point located at a fixed ratio of glyph height, around 0.65, to retrieve pitch.</li>
     * </ul>
     * And we use the average value from these two heuristics.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param staff related staff
     * @return the pitch values (assigned, measured)
     */
    protected static Pitches computePitch (Glyph glyph,
                                           Shape shape,
                                           Staff staff)
    {
        Point centroid = glyph.getCentroid();
        double massPitch = staff.pitchPositionOf(centroid);

        // Pitch offset for flat-based alterations
        if ((shape == Shape.FLAT) || (shape == Shape.DOUBLE_FLAT)) {
            // Heuristic pitch offset WRT centroid pitch
            massPitch += getFlatPitchOffset();

            // Heuristic center WRT glyph box
            Rectangle box = glyph.getBounds();
            Point center = glyph.getCenter();
            double geoPitch = staff.pitchPositionOf(
                    new Point2D.Double(center.x, center.y + (getFlatAreaOffset() * box.height)));

            // Average value of both heuristics
            double mix = 0.5 * (massPitch + geoPitch);

            // logger.info(
            //         "G#{} {}",
            //         glyph.getId(),
            //         String.format("mass:%+.2f geo:%+.2f mix:%+.2f", massPitch, geoPitch, mix));
            return new Pitches((int) Math.rint(mix), mix);
        } else {
            return new Pitches((int) Math.rint(massPitch), massPitch);
        }
    }

    protected static Pitches computePitch (Rectangle bounds,
                                           OmrShape omrShape,
                                           Staff staff)
    {
        final Point center = GeoUtil.centerOf(bounds);
        final double geoPitch;

        // Pitch offset for flat-based alterations
        if ((omrShape == OmrShape.keyFlat)
            || (omrShape == OmrShape.accidentalFlat)
            || (omrShape == OmrShape.accidentalDoubleFlat)
            || (omrShape == OmrShape.accidentalFlatSmall)) {
            // Heuristic center WRT area box
            geoPitch = staff.pitchPositionOf(
                    new Point2D.Double(center.x, center.y + (getFlatAreaOffset() * bounds.height)));
        } else {
            geoPitch = staff.pitchPositionOf(center);
        }

        return new Pitches((int) Math.rint(geoPitch), geoPitch);
    }

    //-------------//
    // lookupLinks //
    //-------------//
    /**
     * Try to detect link between this Alter instance and a head nearby (and its
     * mirror head if any)
     *
     * @param systemHeads ordered collection of heads in system
     * @return the collection of links found, perhaps null
     */
    private Collection<Link> lookupLinks (List<Inter> systemHeads)
    {
        if (systemHeads.isEmpty()) {
            return Collections.emptySet();
        }

        if (isVip()) {
            logger.info("VIP searchLinks for {}", this);
        }

        // Look for notes nearby on the right side of accidental
        final SystemInfo system = systemHeads.get(0).getSig().getSystem();
        final Scale scale = system.getSheet().getScale();
        final int xGapMax = scale.toPixels(AlterHeadRelation.getXOutGapMaximum(manual));
        final int yGapMax = scale.toPixels(AlterHeadRelation.getYGapMaximum(manual));

        // Accid ref point is on accid right side and precise y depends on accid shape
        Rectangle accidBox = getBounds();
        Point accidPt = new Point(
                accidBox.x + accidBox.width,
                ((shape != Shape.FLAT) && (shape != Shape.DOUBLE_FLAT))
                        ? (accidBox.y + (accidBox.height / 2))
                        : (accidBox.y + ((3 * accidBox.height) / 4)));
        Rectangle luBox = new Rectangle(accidPt.x, accidPt.y - yGapMax, xGapMax, 2 * yGapMax);
        List<Inter> notes = Inters.intersectedInters(systemHeads, GeoOrder.BY_ABSCISSA, luBox);

        if (!notes.isEmpty()) {
            if ((getGlyph() != null) && getGlyph().isVip()) {
                logger.info("accid {} glyph#{} notes:{}", this, getGlyph().getId(), notes);
            }

            AlterHeadRelation bestRel = null;
            Inter bestHead = null;
            double bestYGap = Double.MAX_VALUE;

            for (Inter head : notes) {
                // Note ref point is on head left side and y is at head mid height
                // We are strict on pitch concordance (through yGapMax value)
                Point notePt = head.getCenterLeft();
                double xGap = notePt.x - accidPt.x;
                double yGap = Math.abs(notePt.y - accidPt.y);
                AlterHeadRelation rel = new AlterHeadRelation();
                rel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), manual);

                if (rel.getGrade() >= rel.getMinGrade()) {
                    if ((bestRel == null) || (bestYGap > yGap)) {
                        bestRel = rel;
                        bestHead = head;
                        bestYGap = yGap;
                    }
                }
            }

            if (bestRel != null) {
                Set<Link> set = new LinkedHashSet<Link>();
                set.add(new Link(bestHead, bestRel, true));

                // If any, include head mirror as well
                if (bestHead.getMirror() != null) {
                    set.add(new Link(bestHead.getMirror(), bestRel.duplicate(), true));
                }

                return set;
            }
        }

        return Collections.emptySet();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Pitches //
    //---------//
    protected static class Pitches
    {
        //~ Instance fields ------------------------------------------------------------------------

        public final double pitch;

        public final double measuredPitch;

        //~ Constructors ---------------------------------------------------------------------------
        public Pitches (double pitch,
                        double measuredPitch)
        {
            this.pitch = pitch;
            this.measuredPitch = measuredPitch;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Double flatPitchOffset = new Constant.Double(
                "pitch",
                0.65,
                "Pitch offset of flat WRT centroid-based pitch");

        private final Constant.Ratio flatAreaOffset = new Constant.Ratio(
                0.25,
                "Center offset of flat WRT area center");
    }
}
