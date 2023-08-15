//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A b s t r a c t C o n n e c t i o n                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>AbstractConnection</code> serves as a basis for support based on precise
 * connection.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "connection")
public abstract class AbstractConnection
        extends Support
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AbstractConnection.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * Horizontal gap at connection.
     * <p>
     * The gap value is specified in interline fraction with 3 digits maximum after the dot.
     * <p>
     * This value can be:
     * <ul>
     * <li><i>Positive</i> for an '<b>out</b>' distance (we have a true gap).
     * <li><i>Negative</i> for an '<b>in</b>' distance (we have an overlap).
     * </ul>
     */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double3Adapter.class)
    protected double dx;

    /**
     * Vertical gap at connection.
     * <p>
     * The gap value is specified in interline fraction with 3 digits maximum after the dot.
     * <p>
     * This is always an absolute value.
     */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double3Adapter.class)
    protected double dy;

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the horizontal gap (positive or negative) in interline fraction
     *
     * @return the horizontal gap
     */
    public double getDx ()
    {
        return dx;
    }

    /**
     * Report the vertical gap (positive) in interline fraction
     *
     * @return the vertical gap
     */
    public double getDy ()
    {
        return dy;
    }

    /**
     * Method to override to provide specific weights for xInGap and yGap.
     *
     * @return weights to use
     */
    protected double[] getInWeights ()
    {
        return InImpacts.WEIGHTS;
    }

    /**
     * Method to override to provide specific weights for xOutGap and yGap.
     *
     * @return weights to use
     */
    protected double[] getOutWeights ()
    {
        return OutImpacts.WEIGHTS;
    }

    /**
     * Report maximum acceptable abscissa overlap.
     * <p>
     * This method is disabled by default, to be overridden if overlap is possible
     *
     * @param profile desired profile level
     * @return the maximum overlap acceptable
     */
    protected Scale.Fraction getXInGapMax (int profile)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Report the maximum acceptable for outer abscissa gap.
     *
     * @param profile desired profile level
     * @return maximum x out gap
     */
    protected abstract Scale.Fraction getXOutGapMax (int profile);

    /**
     * Report the maximum acceptable ordinate gap.
     *
     * @param profile desired profile level
     * @return max y gap
     */
    protected abstract Scale.Fraction getYGapMax (int profile);

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        // @formatter:off
        return new StringBuilder(super.internals()).append("@(")
                .append(String.format("%.2f", dx)).append(",")
                .append(String.format("%.2f", dy)).append(")").toString();
        // @formatter:on
    }

    /**
     * Set the gaps for this connection, with different handling for xIn and xOut gaps.
     * <p>
     * Beware, if dx is positive or null, it's an xOut gap, otherwise it's an xIn gap
     *
     * @param dx      the horizontal distance (positive for gap and negative for overlap)
     * @param dy      the vertical distance (absolute)
     * @param profile desired profile level
     */
    public void setInOutGaps (double dx,
                              double dy,
                              int profile)
    {
        this.dx = dx;
        this.dy = dy;

        // Infer impact data
        double yMax = getYGapMax(profile).getValue();
        double yImpact = (yMax - dy) / yMax;

        if (dx >= 0) {
            double xMax = getXOutGapMax(profile).getValue();
            double xImpact = (xMax - dx) / xMax;
            setImpacts(new OutImpacts(xImpact, yImpact, getOutWeights()));
        } else {
            double xMax = getXInGapMax(profile).getValue();
            double xImpact = (xMax + dx) / xMax;
            setImpacts(new InImpacts(xImpact, yImpact, getInWeights()));
        }

        // Compute grade
        setGrade(impacts.getGrade());
    }

    /**
     * Set the gaps for this connection, with no difference between xIn and xOut gaps.
     *
     * @param dx      the horizontal distance (only its absolute value is considered)
     * @param dy      the vertical distance (absolute)
     * @param profile desired profile level
     */
    public void setOutGaps (double dx,
                            double dy,
                            int profile)
    {
        setInOutGaps(Math.abs(dx), dy, profile);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio xInWeight = new Constant.Ratio(
                1,
                "Relative impact weight for xInGap");

        private final Constant.Ratio xOutWeight = new Constant.Ratio(
                2,
                "Relative impact weight for xOutGap");

        private final Constant.Ratio yWeight = new Constant.Ratio(
                1,
                "Relative impact weight for yGap");
    }

    //-----------//
    // InImpacts //
    //-----------//
    /**
     * Grade impacts for abscissa overlap.
     */
    public static class InImpacts
            extends SupportImpacts
    {

        private static final String[] NAMES = new String[]
        { "xInGap", "yGap" };

        // Default weights
        private static final double[] WEIGHTS = new double[]
        { constants.xInWeight.getValue(), constants.yWeight.getValue() };

        /**
         * Create an InImpacts object.
         *
         * @param xInGap  horizontal overlap
         * @param yGap    vertical gap
         * @param weights array of impacts weight
         */
        public InImpacts (double xInGap,
                          double yGap,
                          double[] weights)
        {
            super(NAMES, weights);
            setImpact(0, xInGap);
            setImpact(1, yGap);
        }
    }

    //------------//
    // OutImpacts //
    //------------//
    /**
     * Grade impacts for abscissa gap.
     */
    public static class OutImpacts
            extends SupportImpacts
    {

        private static final String[] NAMES = new String[]
        { "xOutGap", "yGap" };

        // Defaults weights
        private static final double[] WEIGHTS = new double[]
        { constants.xOutWeight.getValue(), constants.yWeight.getValue() };

        /**
         * Create an OutImpacts object.
         *
         * @param xOutGap horizontal gap
         * @param yGap    vertical gap
         * @param weights array of impacts weight
         */
        public OutImpacts (double xOutGap,
                           double yGap,
                           double[] weights)
        {
            super(NAMES, weights);
            setImpact(0, xOutGap);
            setImpact(1, yGap);
        }
    }
}
