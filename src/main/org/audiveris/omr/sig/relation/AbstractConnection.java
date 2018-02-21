//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A b s t r a c t C o n n e c t i o n                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
 * Class {@code AbstractConnection} serves as a basis for support based on precise
 * connection.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "connection")
public abstract class AbstractConnection
        extends AbstractSupport
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            AbstractConnection.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * Horizontal gap at connection (specified in interline fraction).
     * Positive value for an 'out' distance (true gap).
     * Negative value for an 'in' distance (overlap).
     */
    @XmlAttribute
    @XmlJavaTypeAdapter(Jaxb.Double3Adapter.class)
    protected Double dx;

    /**
     * Vertical gap at connection (specified in interline fraction).
     * Absolute value.
     */
    @XmlAttribute
    @XmlJavaTypeAdapter(Jaxb.Double3Adapter.class)
    protected Double dy;

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
     * Set the gaps for this connection.
     *
     * @param dx the horizontal distance (positive for gap and negative for overlap)
     * @param dy the vertical distance (absolute)
     */
    public void setGaps (double dx,
                         double dy)
    {
        this.dx = dx;
        this.dy = dy;

        // Infer impact data
        double yMax = getYGapMax().getValue();
        double yImpact = (yMax - dy) / yMax;

        if (dx >= 0) {
            double xMax = getXOutGapMax().getValue();
            double xImpact = (xMax - dx) / xMax;
            setImpacts(new OutImpacts(xImpact, yImpact, getOutWeights()));
        } else {
            double xMax = getXInGapMax().getValue();
            double xImpact = (xMax + dx) / xMax;
            setImpacts(new InImpacts(xImpact, yImpact, getInWeights()));
        }

        // Compute grade
        setGrade(impacts.getGrade());
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

    protected abstract Scale.Fraction getXOutGapMax ();

    protected abstract Scale.Fraction getYGapMax ();

    /**
     * Report maximum acceptable overlap.
     * This method is disabled by default, to be overridden if overlap is possible
     *
     * @return the maximum overlap acceptable
     */
    protected Scale.Fraction getXInGapMax ()
    {
        throw new UnsupportedOperationException();
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if ((dx != null) && (dy != null)) {
            sb.append("@(").append(String.format("%.2f", dx)).append(",")
                    .append(String.format("%.2f", dy)).append(")");
        }

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // InImpacts //
    //-----------//
    public static class InImpacts
            extends SupportImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[]{"xInGap", "yGap"};

        // Default weights
        private static final double[] WEIGHTS = new double[]{
            constants.xInWeight.getValue(),
            constants.yWeight.getValue()
        };

        //~ Constructors ---------------------------------------------------------------------------
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
    public static class OutImpacts
            extends SupportImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[]{"xOutGap", "yGap"};

        // Defaults weights
        private static final double[] WEIGHTS = new double[]{
            constants.xOutWeight.getValue(),
            constants.yWeight.getValue()
        };

        //~ Constructors ---------------------------------------------------------------------------
        public OutImpacts (double xOutGap,
                           double yGap,
                           double[] weights)
        {
            super(NAMES, weights);
            setImpact(0, xOutGap);
            setImpact(1, yGap);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

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
}
