//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t e m C o n n e c t i o n                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import omr.sheet.Scale;

import omr.sig.inter.Inter;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Class {@code StemConnection} is the basis for connections to a stem.
 *
 * @author Hervé Bitteur
 */
public abstract class StemConnection
        extends AbstractConnection
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Logical connection point. */
    protected Point2D anchorPoint;

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getStemPortion //
    //----------------//
    /**
     * Report the portion of the stem the provided source is connected to
     *
     * @param source   the item connected to the stem
     * @param stemLine logical range of the stem
     * @param scale    global scale
     * @return the stem Portion
     */
    public abstract StemPortion getStemPortion (Inter source,
                                                Line2D stemLine,
                                                Scale scale);

    //----------------//
    // getAnchorPoint //
    //----------------//
    /**
     * Report the logical connection point, which is defined as the point with maximum
     * extension along the logical stem.
     * This definition allows to use the anchor ordinate to determine the precise stem portion of
     * the connection.
     *
     * @return the anchor point
     */
    public Point2D getAnchorPoint ()
    {
        return anchorPoint;
    }

    //----------------//
    // setAnchorPoint //
    //----------------//
    /**
     * Set the logical connection point.
     *
     * @param anchorPoint the anchor point to set
     */
    public void setAnchorPoint (Point2D anchorPoint)
    {
        this.anchorPoint = anchorPoint;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (anchorPoint != null) {
            sb.append(String.format(" [x:%.0f,y:%.0f]", anchorPoint.getX(), anchorPoint.getY()));
        }

        return sb.toString();
    }
}
