//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       A l t e r I n t e r                                      //
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

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.math.GeoOrder;

import omr.sheet.Scale;
import omr.sheet.Staff;
import omr.sheet.rhythm.Voice;

import omr.sig.GradeImpacts;
import omr.sig.relation.AlterHeadRelation;
import omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

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
    private final double measuredPitch;

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
                       int pitch,
                       double measuredPitch)
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
                       int pitch,
                       double measuredPitch)
    {
        super(glyph, null, shape, impacts, staff, pitch);
        this.measuredPitch = measuredPitch;
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private AlterInter ()
    {
        super(null, null, null, null, null, 0);
        this.measuredPitch = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * Create an Alter inter.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param staff closest staff (questionable)
     * @return the created instance or null if failed
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
     * Create an Alter inter.
     *
     * @param glyph   underlying glyph
     * @param shape   precise shape
     * @param impacts assignment details
     * @param staff   related staff
     * @return the created instance or null if failed
     */
    public static AlterInter create (Glyph glyph,
                                     Shape shape,
                                     GradeImpacts impacts,
                                     Staff staff)
    {
        Pitches pitches = computePitch(glyph, shape, staff);

        return new AlterInter(glyph, shape, impacts, staff, pitches.pitch, pitches.measuredPitch);
    }

    //--------------------//
    // getFlatPitchOffset //
    //--------------------//
    public static double getFlatPitchOffset ()
    {
        return constants.flatPitchOffset.getValue();
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //--------------------//
    // detectNoteRelation //
    //--------------------//
    /**
     * Try to detect a relation between this Alter instance and a note nearby.
     *
     * @param systemHeads ordered collection of notes in system
     */
    public void detectNoteRelation (List<Inter> systemHeads)
    {
        // Look for notes nearby on the right side of accidental
        final Scale scale = sig.getSystem().getSheet().getScale();
        final int xGapMax = scale.toPixels(AlterHeadRelation.getXOutGapMaximum());
        final int yGapMax = scale.toPixels(AlterHeadRelation.getYGapMaximum());

        // Accid ref point is on accid right side and precise y depends on accid shape
        Rectangle accidBox = getBounds();
        Point accidPt = new Point(
                accidBox.x + accidBox.width,
                ((shape != Shape.FLAT) && (shape != Shape.DOUBLE_FLAT))
                        ? (accidBox.y + (accidBox.height / 2))
                        : (accidBox.y + ((3 * accidBox.height) / 4)));
        Rectangle luBox = new Rectangle(accidPt.x, accidPt.y - yGapMax, xGapMax, 2 * yGapMax);
        List<Inter> notes = sig.intersectedInters(systemHeads, GeoOrder.BY_ABSCISSA, luBox);

        if (!notes.isEmpty()) {
            if (getGlyph().isVip()) {
                logger.info("accid {} glyph#{} notes:{}", this, getGlyph().getId(), notes);
            }

            AlterHeadRelation bestRel = null;
            Inter bestNote = null;
            double bestYGap = Double.MAX_VALUE;

            for (Inter note : notes) {
                // Note ref point is on note left side and y is at note mid height
                // We are strict on pitch concordance (through yGapMax value)
                Point notePt = note.getCenterLeft();
                double xGap = notePt.x - accidPt.x;
                double yGap = Math.abs(notePt.y - accidPt.y);
                AlterHeadRelation rel = new AlterHeadRelation();
                rel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

                if (rel.getGrade() >= rel.getMinGrade()) {
                    if ((bestRel == null) || (bestYGap > yGap)) {
                        bestRel = rel;
                        bestNote = note;
                        bestYGap = yGap;
                    }
                }
            }

            if (bestRel != null) {
                sig.addEdge(this, bestNote, bestRel);
            }
        }
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        return super.getDetails() + String.format(" mPitch:%.1f", measuredPitch);
    }

    /**
     * @return the measuredPitch
     */
    public Double getMeasuredPitch ()
    {
        return measuredPitch;
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

    //--------------//
    // computePitch //
    //--------------//
    /**
     * Compute pitch and measuredPitch values according to glyph centroid and shape.
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
        // Determine pitch according to shape and glyph centroid
        Point centroid = glyph.getCentroid();
        double measuredPitch = staff.pitchPositionOf(centroid);

        // Pitch offset for flat-based alterations
        if ((shape == Shape.FLAT) || (shape == Shape.DOUBLE_FLAT)) {
            measuredPitch += constants.flatPitchOffset.getValue();
        }

        return new Pitches((int) Math.rint(measuredPitch), measuredPitch);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Pitches //
    //---------//
    protected static class Pitches
    {
        //~ Instance fields ------------------------------------------------------------------------

        public final int pitch;

        public final double measuredPitch;

        //~ Constructors ---------------------------------------------------------------------------
        public Pitches (int pitch,
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
                0.75,
                "Pitch offset of flat WRT centroid-based pitch");
    }
}
