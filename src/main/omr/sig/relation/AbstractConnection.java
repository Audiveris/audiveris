//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A b s t r a c t C o n n e c t i o n                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code AbstractConnection} serves as a basis for support based on precise
 * connection.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractConnection
        extends AbstractSupport
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AbstractConnection.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * Horizontal distance at connection (in interline).
     * Positive value for an 'out' distance (gap).
     * Negative value for an 'in' distance (overlap).
     */
    protected Double xDistance;

    /**
     * Vertical distance at connection (in interline).
     * Absolute value.
     */
    protected Double yDistance;

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * @return the horizontal distance
     */
    public double getXDistance ()
    {
        return xDistance;
    }

    /**
     * @return the vertical distance
     */
    public double getYDistance ()
    {
        return yDistance;
    }

    /**
     * Set the gaps for this connection.
     *
     * @param xDistance the horizontal distance (positive for gap and negative for overlap)
     * @param yDistance the vertical distance (absolute)
     */
    public void setDistances (double xDistance,
                              double yDistance)
    {
        this.xDistance = xDistance;
        this.yDistance = yDistance;

        // Infer impact data
        double yMax = getYGapMax().getValue();
        double yImpact = (yMax - yDistance) / yMax;

        if (xDistance >= 0) {
            double xMax = getXOutGapMax().getValue();
            double xImpact = (xMax - xDistance) / xMax;
            setImpacts(new OutImpacts(xImpact, yImpact, getOutWeights()));
        } else {
            double xMax = getXInGapMax().getValue();
            double xImpact = (xMax + xDistance) / xMax;
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

        if ((xDistance != null) && (yDistance != null)) {
            sb.append("@(").append(String.format("%.2f", xDistance)).append(",").append(
                    String.format("%.2f", yDistance)).append(")");
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

        final Constant.Ratio xInWeight = new Constant.Ratio(1, "Relative impact weight for xInGap");

        final Constant.Ratio xOutWeight = new Constant.Ratio(
                2,
                "Relative impact weight for xOutGap");

        final Constant.Ratio yWeight = new Constant.Ratio(1, "Relative impact weight for yGap");
    }
}
