//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 B e a m S t e m R e l a t i o n                                //
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;
import static org.audiveris.omr.sig.relation.BeamPortion.*;
import static org.audiveris.omr.util.VerticalSide.*;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import org.audiveris.omr.util.VerticalSide;

/**
 * Class {@code BeamStemRelation} implements the geometric link between a beam
 * (or beam hook) and a stem.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "beam-stem")
public class BeamStemRelation
        extends AbstractStemConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BeamStemRelation.class);

    private static final double[] OUT_WEIGHTS = new double[]{constants.xOutWeight.getValue(),
                                                             constants.yWeight.getValue()};

    //~ Instance fields ----------------------------------------------------------------------------
    /** Which portion of beam is used?. */
    @XmlAttribute(name = "beam-portion")
    private BeamPortion beamPortion;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BeamStemRelation} object.
     */
    public BeamStemRelation ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // setBeamPortion //
    //----------------//
    /**
     * Set the beam portion where stem is connected.
     *
     * @param beamPortion the beam portion to set
     */
    public void setBeamPortion (BeamPortion beamPortion)
    {
        this.beamPortion = beamPortion;
    }

    //----------------//
    // getBeamPortion //
    //----------------//
    /**
     * @return the beamPortion
     */
    public BeamPortion getBeamPortion ()
    {
        return beamPortion;
    }

    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        return false;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        return false;
    }

    //----------------//
    // getStemPortion //
    //----------------//
    @Override
    public StemPortion getStemPortion (Inter source,
                                       Line2D stemLine,
                                       Scale scale)
    {
        double midStem = (stemLine.getY1() + stemLine.getY2()) / 2;

        return (extensionPoint.getY() < midStem) ? StemPortion.STEM_TOP : StemPortion.STEM_BOTTOM;
    }

    //------------------//
    // getXInGapMaximum //
    //------------------//
    public static Scale.Fraction getXInGapMaximum (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.xInGapMax, profile);
    }

    //-------------------//
    // getXOutGapMaximum //
    //-------------------//
    public static Scale.Fraction getXOutGapMaximum (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.xOutGapMax, profile);
    }

    //----------------//
    // getYGapMaximum //
    //----------------//
    public static Scale.Fraction getYGapMaximum (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.yGapMax, profile);
    }

    //-------//
    // added //
    //-------//
    /**
     * Populate beam portion if needed and update the chord(s) if any that use this stem.
     * <p>
     * In the rare case where a stem is shared by several chords, the beam connection applies to
     * all these chords.
     *
     * @param e edge change event
     */
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final AbstractBeamInter beam = (AbstractBeamInter) e.getEdgeSource();
        final StemInter stem = (StemInter) e.getEdgeTarget();

        if (beam.isVip() && stem.isVip()) {
            logger.info("VIP BeamStemRelation added between {} and {}", beam, stem);
        }

        if (extensionPoint == null) {
            extensionPoint = computeExtensionPoint(beam, stem);
        }

        if (beamPortion == null) {
            final Scale scale = beam.getSig().getSystem().getSheet().getScale();
            beamPortion = computeBeamPortion(beam, stem.getCenter().x, scale);
        }

        for (HeadChordInter chord : stem.getChords()) {
            chord.invalidateCache();
        }

        beam.checkAbnormal();
    }

    //-----------//
    // checkLink //
    //-----------//
    /**
     * Check if a Beam-Stem link is possible between provided beam and stem.
     *
     * @param beam       the provided beam
     * @param stem       the provided stem
     * @param headToBeam vertical direction from head to beam
     * @param scale      scaling information
     * @param profile    desired profile level
     * @return the link if OK, otherwise null
     */
    public static Link checkLink (AbstractBeamInter beam,
                                  StemInter stem,
                                  VerticalSide headToBeam,
                                  Scale scale,
                                  int profile)
    {
        if (beam.isVip() && stem.isVip()) {
            logger.info("VIP checkLink {} & {}", beam, stem);
        }

        final BeamStemRelation bRel = checkRelation(beam, stem.getMedian(), headToBeam, scale,
                                                    profile);

        return (bRel != null) ? new Link(stem, bRel, true) : null;
    }

    //---------------//
    // checkRelation //
    //---------------//
    /**
     * Check if a Beam-Stem relation is possible between provided beam and stem.
     *
     * @param beam       the provided beam
     * @param stemLine   stem median line (top down)
     * @param headToBeam vertical direction from head to beam
     * @param scale      scaling information
     * @param profile    desired profile level
     * @return the relation if OK, otherwise null
     */
    public static BeamStemRelation checkRelation (AbstractBeamInter beam,
                                                  Line2D stemLine,
                                                  VerticalSide headToBeam,
                                                  Scale scale,
                                                  int profile)
    {
        if (beam.isVip()) {
            logger.info("VIP checkRelation {} & {}", beam, LineUtil.toString(stemLine));
        }

        // Relation beam -> stem
        final int yDir = (headToBeam == TOP) ? (-1) : 1;
        final Line2D beamBorder = beam.getBorder(headToBeam.opposite());
        final BeamStemRelation bRel = new BeamStemRelation();

        // Precise cross point
        final Point2D crossPt = LineUtil.intersection(stemLine, beamBorder);
        final double xStem = crossPt.getX();

        // Beam portion
        final BeamPortion portion = BeamStemRelation.computeBeamPortion(beam, xStem, scale);
        bRel.setBeamPortion(portion);

        // Abscissa
        final double xGap = (portion == BeamPortion.CENTER) ? 0
                : ((portion == BeamPortion.LEFT)
                        ? Math.max(0, beamBorder.getX1() - xStem)
                        : Math.max(0, xStem - beamBorder.getX2()));

        // Ordinate
        final double yGap = Math.max(Math.max(0, stemLine.getY1() - crossPt.getY()),
                                     Math.max(0, crossPt.getY() - stemLine.getY2()));

        bRel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), profile);

        if (bRel.getGrade() >= bRel.getMinGrade()) {
            // Beware: extension must be the maximum y extension in beam y range
            bRel.setExtensionPoint(new Point2D.Double(
                    xStem,
                    crossPt.getY() + (yDir * (beam.getHeight() - 1))));

            return bRel;
        }

        return null;
    }

    //--------------------//
    // computeBeamPortion //
    //--------------------//
    /**
     * Determine beam portion where stem is linked.
     *
     * @param beam  provided beam
     * @param xStem abscissa of stem connection
     * @param scale scaling information
     * @return the beam portion
     */
    public static BeamPortion computeBeamPortion (AbstractBeamInter beam,
                                                  double xStem,
                                                  Scale scale)
    {
        if (beam instanceof BeamHookInter) {
            return (beam.getCenter().x < xStem) ? RIGHT : LEFT;
        } else {
            int maxDx = scale.toPixels(getXInGapMaximum(0)); // No profile used
            double left = beam.getMedian().getX1();
            double right = beam.getMedian().getX2();

            if (xStem < (left + maxDx)) {
                return LEFT;
            } else if (xStem > (right - maxDx)) {
                return RIGHT;
            } else {
                return CENTER;
            }
        }
    }

    //-----------------------//
    // computeExtensionPoint //
    //-----------------------//
    /**
     * Compute the extension point where beam meets stem.
     * <p>
     * As for HeadStemRelation, the extension point is the <b>last</b> point where stem meets beam
     * when going along stem from head to beam.
     *
     * @param beam the provided beam
     * @param stem the provided stem
     * @return the corresponding extension point
     */
    public static Point2D computeExtensionPoint (AbstractBeamInter beam,
                                                 StemInter stem)
    {
        // Determine if stem is above or below the beam, to choose proper beam border
        // If stem is below the beam, we choose the top border of beam.
        Line2D stemMedian = stem.getMedian();
        Point2D stemMiddle = PointUtil.middle(stemMedian);
        int above = beam.getMedian().relativeCCW(stemMiddle);
        Line2D beamBorder = beam.getBorder((above < 0) ? TOP : BOTTOM);

        return LineUtil.intersection(stemMedian, beamBorder);
    }

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        // If stem has a chord with heads, remove all beam-head relations
        final AbstractBeamInter beam = (AbstractBeamInter) e.getEdgeSource();
        final StemInter stem = (StemInter) e.getEdgeTarget();

        /**
         * CAVEAT: if a beam (with beam-stem and beam-head relations) is removed,
         * the graph will automatically remove these relations, so also removing here
         * the beam-head relation might lead to NPE in graph...
         */
        if (!beam.isRemoved()) {
            if (!stem.isRemoved()) {
                final SIGraph sig = stem.getSig();

                for (HeadChordInter headChord : stem.getChords()) {
                    for (Inter inter : headChord.getNotes()) {
                        HeadInter head = (HeadInter) inter;
                        sig.removeEdge(beam, head);
                    }

                    headChord.invalidateCache();
                }
            }

            beam.checkAbnormal();
        }
    }

    //---------------//
    // getOutWeights //
    //---------------//
    @Override
    protected double[] getOutWeights ()
    {
        return OUT_WEIGHTS;
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    @Override
    protected double getSourceCoeff ()
    {
        return constants.beamSupportCoeff.getValue();
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    /**
     * A stem connected on beam side receives a much higher support that a stem connected
     * on beam center.
     *
     * @return support coefficient for connected stem
     */
    @Override
    protected double getTargetCoeff ()
    {
        if (beamPortion == BeamPortion.CENTER) {
            return constants.stemSupportCoeff.getValue();
        } else {
            return constants.sideStemSupportCoeff.getValue();
        }
    }

    //--------------//
    // getXInGapMax //
    //--------------//
    @Override
    protected Scale.Fraction getXInGapMax (int profile)
    {
        return getXInGapMaximum(profile);
    }

    //---------------//
    // getXOutGapMax //
    //---------------//
    @Override
    protected Scale.Fraction getXOutGapMax (int profile)
    {
        return getXOutGapMaximum(profile);
    }

    //------------//
    // getYGapMax //
    //------------//
    @Override
    protected Scale.Fraction getYGapMax (int profile)
    {
        return getYGapMaximum(profile);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" ").append(beamPortion);

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio beamSupportCoeff = new Constant.Ratio(
                4,
                "Value for source (beam) coeff in support formula");

        private final Constant.Ratio stemSupportCoeff = new Constant.Ratio(
                3,
                "Value for target (stem) coeff in support formula");

        private final Constant.Ratio sideStemSupportCoeff = new Constant.Ratio(
                10,
                "Value for target (stem on beam side) coeff in support formula");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                0.8,
                "Maximum vertical gap between stem & beam");

        @SuppressWarnings("unused")
        private final Scale.Fraction yGapMax_p1 = new Scale.Fraction(
                1.2,
                "Idem for profile 1");

        @SuppressWarnings("unused")
        private final Scale.Fraction yGapMax_p2 = new Scale.Fraction(
                2.0,
                "Idem for profile 2");

        @SuppressWarnings("unused")
        private final Scale.Fraction yGapMax_p3 = new Scale.Fraction(
                4.0,
                "Idem for profile 3");

        private final Scale.Fraction xInGapMax = new Scale.Fraction(
                0.5,
                "Maximum horizontal overlap between stem & beam");

        @SuppressWarnings("unused")
        private final Scale.Fraction xInGapMax_p1 = new Scale.Fraction(
                0.75,
                "Idem for profile 1");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
                0.15,
                "Maximum horizontal gap between stem & beam");

        @SuppressWarnings("unused")
        private final Scale.Fraction xOutGapMax_p1 = new Scale.Fraction(
                0.2,
                "Idem for profile 1");

        private final Constant.Ratio xOutWeight = new Constant.Ratio(
                1,
                "Relative impact weight for xOutGap");

        private final Constant.Ratio yWeight = new Constant.Ratio(
                4,
                "Relative impact weight for yGap");
    }
}
