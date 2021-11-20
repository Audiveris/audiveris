//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 B e a m R e s t R e l a t i o n                                //
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

import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.RestInter;
import static org.audiveris.omr.sig.relation.BeamPortion.CENTER;
import static org.audiveris.omr.sig.relation.BeamPortion.LEFT;
import static org.audiveris.omr.sig.relation.BeamPortion.RIGHT;
import org.jgrapht.event.GraphEdgeChangeEvent;

/**
 * Class <code>BeamRestRelation</code> implements the geometric link between a beam
 * and an interleaved rest.
 * <p>
 * <img src="doc-files/BeamRest.png" alt="Example of beam-rest relation">
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "beam-rest")
public class BeamRestRelation
        extends Support
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BeamRestRelation.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * The dy attribute is the absolute vertical distance, in pixels, between the rest
     * center and the beam median line.
     */
    @XmlAttribute
    private int dy;

    /**
     * The beam-portion attribute indicates on which portion of the beam (center or side)
     * the rest is connected.
     */
    @XmlAttribute(name = "beam-portion")
    private BeamPortion beamPortion;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>BeamRestRelation</code> object.
     */
    public BeamRestRelation ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final BeamInter beam = (BeamInter) e.getEdgeSource();
        final RestInter rest = (RestInter) e.getEdgeTarget();
        final Point restCenter = rest.getCenter();
        final SystemInfo system = beam.getSig().getSystem();
        final Scale scale = system.getSheet().getScale();

        // dy
        final Line2D median = beam.getMedian();
        final Line2D vertical = system.getSkew().skewedVertical(restCenter);
        final Point2D cross = LineUtil.intersection(median, vertical);
        final double dist = PointUtil.length(PointUtil.subtraction(restCenter, cross));
        dy = (int) Math.rint(dist);

        // beam portion
        beamPortion = computeBeamPortion(beam, restCenter.x, scale);

        beam.checkAbnormal();
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

    //-------------//
    // getDistance //
    //-------------//
    /**
     * Report the absolute vertical distance between beam median and rest center.
     *
     * @return length of (nearly) vertical distance between beam and rest
     */
    public int getDistance ()
    {
        return dy;
    }

    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        // A rest can "belong" to at most one beam.
        return true;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        // A beam can have several interleaved rests.
        return false;
    }

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final AbstractBeamInter beam = (AbstractBeamInter) e.getEdgeSource();

        beam.checkAbnormal();
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return new StringBuilder(super.internals()).append(" dy:").append(dy).toString();
    }

    //--------------------//
    // computeBeamPortion //
    //--------------------//
    /**
     * Determine beam portion where rest is linked.
     *
     * @param beam  provided beam
     * @param xRest abscissa of rest center
     * @param scale scaling information
     * @return the beam portion
     */
    public static BeamPortion computeBeamPortion (BeamInter beam,
                                                  double xRest,
                                                  Scale scale)
    {
        int maxDx = scale.toPixels(constants.xInGapMax);
        double left = beam.getMedian().getX1();
        double right = beam.getMedian().getX2();

        if (xRest < (left + maxDx)) {
            return LEFT;
        } else if (xRest > (right - maxDx)) {
            return RIGHT;
        } else {
            return CENTER;
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction xInGapMax = new Scale.Fraction(
                1.0,
                "Maximum horizontal overlap between rest & beam");
//
//        @SuppressWarnings("unused")
//        private final Scale.Fraction xInGapMax_p1 = new Scale.Fraction(
//                1.5,
//                "Idem for profile 1");
//
//        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
//                0.5,
//                "Maximum horizontal gap between rest & beam");
//
//        @SuppressWarnings("unused")
//        private final Scale.Fraction xOutGapMax_p1 = new Scale.Fraction(
//                0.75,
//                "Idem for profile 1");
    }
}
