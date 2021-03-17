//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       A l t e r I n t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
    private final Double measuredPitch;

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
                       Double grade,
                       Staff staff,
                       Double pitch,
                       Double measuredPitch)
    {
        super(glyph, null, shape, grade, staff, pitch);
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
        super(null, null, null, (Double) null, null, null);
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

    //--------------//
    // alterationOf //
    //--------------//
    /**
     * Report the pitch alteration that corresponds to the provided accidental.
     *
     * @param accidental the provided accidental, perhaps null
     * @return the pitch impact
     */
    public static int alterationOf (AlterInter accidental)
    {
        if (accidental == null) {
            return 0;
        }

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
    public Point2D getRelationCenter ()
    {
        final Point2D center = getCenter2D();

        switch (shape) {
        case FLAT:
        case DOUBLE_FLAT: {
            double il = (staff != null) ? staff.getSpecificInterline()
                    : getBounds().height / constants.flatTypicalHeight.getValue();
            return new Point2D.Double(center.getX(),
                                      center.getY() + (0.5 * il * getAreaPitchOffset(Shape.FLAT)));
        }

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
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final List<Inter> systemHeads = system.getSig().inters(HeadInter.class);
        Collections.sort(systemHeads, Inters.byAbscissa);

        final int profile = Math.max(getProfile(), system.getProfile());

        return lookupLinks(systemHeads, profile);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, AlterHeadRelation.class);
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
        final int profile = Math.max(getProfile(), getSig().getSystem().getProfile());
        final Collection<Link> links = lookupLinks(systemHeads, profile);

        for (Link link : links) {
            link.applyTo(this);
        }
    }

    //-------------//
    // lookupLinks //
    //-------------//
    /**
     * Try to detect link between this Alter instance and a head nearby (and its
     * mirror head if any)
     *
     * @param systemHeads ordered collection of heads in system
     * @param profile     desired profile level
     * @return the collection of links found, perhaps null
     */
    private Collection<Link> lookupLinks (List<Inter> systemHeads,
                                          int profile)
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
        final int xGapMax = scale.toPixels(AlterHeadRelation.getXOutGapMaximum(profile));
        final int yGapMax = scale.toPixels(AlterHeadRelation.getYGapMaximum(profile));

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
                rel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), profile);

                if (rel.getGrade() >= rel.getMinGrade()) {
                    if ((bestRel == null) || (bestYGap > yGap)) {
                        bestRel = rel;
                        bestHead = head;
                        bestYGap = yGap;
                    }
                }
            }

            if (bestRel != null) {
                Set<Link> set = new LinkedHashSet<>();
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
                                     Double grade,
                                     Staff staff)
    {
        Pitches pitches = computePitch(glyph, shape, staff);

        return new AlterInter(glyph, shape, grade, staff, pitches.pitch, pitches.measuredPitch);
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
     * We use two heuristics:
     * <ul>
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
            // Heuristic pitch offset WRT pitch of mass center
            massPitch += constants.flatMassPitchOffset.getValue();

            // Heuristic pitch offset WRT pitch of area center
            Rectangle box = glyph.getBounds();
            Point2D center = glyph.getCenter2D();
            double geoPitch = staff.pitchPositionOf(center);
            geoPitch += getAreaPitchOffset(Shape.FLAT);

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

    //--------------//
    // computePitch //
    //--------------//
    protected static Pitches computePitch (Rectangle bounds,
                                           OmrShape omrShape,
                                           Staff staff)
    {
        final Point2D center = GeoUtil.center2D(bounds);
        double geoPitch = staff.pitchPositionOf(center);

        // Pitch offset for flat-based alterations
        if ((omrShape == OmrShape.keyFlat)
                    || (omrShape == OmrShape.accidentalFlat)
                    || (omrShape == OmrShape.accidentalDoubleFlat)
                    || (omrShape == OmrShape.accidentalFlatSmall)) {
            geoPitch += getAreaPitchOffset(Shape.FLAT);
        }

        return new Pitches((int) Math.rint(geoPitch), geoPitch);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Pitches //
    //---------//
    /**
     * Gather both rounded and precise pitch values.
     */
    protected static class Pitches
    {

        /** Rounded to integer value. */
        public final double pitch;

        /** Precise value. */
        public final double measuredPitch;

        /**
         * Create a Pitches object
         *
         * @param pitch         rounded value
         * @param measuredPitch precise value
         */
        Pitches (double pitch,
                 double measuredPitch)
        {
            this.pitch = pitch;
            this.measuredPitch = measuredPitch;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction flatTypicalHeight = new Scale.Fraction(
                2.7,
                "Typical flat height");

        private final Constant.Double flatMassPitchOffset = new Constant.Double(
                "pitch",
                0.65,
                "Pitch offset of flat WRT mass center");
    }
}
