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
package omr.sig;

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
        extends BasicSupport
{
    //~ Static fields/initializers -----------------------------------------------------------------

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
            setImpacts(new OutImpacts(yImpact, xImpact));
        } else {
            double xMax = getXInGapMax().getValue();
            double xImpact = (xMax + xDistance) / xMax;
            setImpacts(new InImpacts(yImpact, xImpact));
        }

        // Compute grade
        setGrade(impacts.getGrade());
    }

    protected abstract Scale.Fraction getXInGapMax ();

    protected abstract Scale.Fraction getXOutGapMax ();

    protected abstract Scale.Fraction getYGapMax ();

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append("@(").append(String.format("%.2f", xDistance)).append(",").append(
                String.format("%.2f", yDistance)).append(")");

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

        protected static final String[] NAMES = new String[]{"yGap", "xInGap"};

        protected static final double[] WEIGHTS = new double[]{1, 2};

        //~ Constructors ---------------------------------------------------------------------------
        public InImpacts (double yGap,
                          double xInGap)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, yGap);
            setImpact(1, xInGap);
        }
    }

    //------------//
    // OutImpacts //
    //------------//
    public static class OutImpacts
            extends SupportImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        protected static final String[] NAMES = new String[]{"yGap", "xOutGap"};

        protected static final double[] WEIGHTS = new double[]{1, 4};

        //~ Constructors ---------------------------------------------------------------------------
        public OutImpacts (double yGap,
                           double xOutGap)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, yGap);
            setImpact(1, xOutGap);
        }
    }
}
