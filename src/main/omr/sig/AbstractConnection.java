//----------------------------------------------------------------------------//
//                                                                            //
//                     A b s t r a c t C o n n e c t i o n                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code AbstractConnection} serves as a basis for support
 * based on precise connection.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractConnection
        extends BasicSupport
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            AbstractConnection.class);

    //~ Instance fields --------------------------------------------------------
    /** Horizontal distance at connection (in interline). */
    protected Double xDistance;

    /** Vertical distance at connection (in interline). */
    protected Double yDistance;

    //~ Methods ----------------------------------------------------------------
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
     * @param xDistance the horizontal distance
     * @param yDistance the vertical distance
     */
    public void setDistances (double xDistance,
                              double yDistance)
    {
        this.xDistance = xDistance;
        this.yDistance = yDistance;

        // Infer a grade?
        setGrade(computeGrade());
    }

    //--------------//
    // computeGrade //
    //--------------//
    protected double computeGrade ()
    {
        double xWeight = getXWeight();
        double yWeight = getYWeight();
        double wx = (xDistance == null) ? 0 : (xWeight * xDistance);
        double wy = yWeight * yDistance;

        double norm = Math.hypot(xWeight, yWeight);
        double dist = Math.hypot(wx, wy) / norm;
        double g = Math.max(0, 1 - (norm * dist));

        if (logger.isDebugEnabled()) {
            logger.debug(
                    "x:{} y:{} d:{} grade:{}",
                    String.format("%.2f", xDistance),
                    String.format("%.2f", yDistance),
                    String.format("%.2f", dist),
                    String.format("%.2f", g));
        }

        return g;
    }

    protected abstract double getXWeight ();

    protected abstract double getYWeight ();

    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append("@(")
                .append(String.format("%.1f", xDistance))
                .append(",")
                .append(String.format("%.1f", yDistance))
                .append(")");

        return sb.toString();
    }
}
