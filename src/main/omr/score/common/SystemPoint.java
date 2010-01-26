//----------------------------------------------------------------------------//
//                                                                            //
//                           S y s t e m P o i n t                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.common;


/**
 * Class <code>SystemPoint</code> is a simple Point that is meant to represent a
 * point inside a system, and where coordinates are expressed in units, the
 * origin being the upper-left corner of the system.
 *
 * <p>This specialization is used to take benefit of compiler checks, to prevent
 * the use of points with incorrect meaning or units.
 *
 * @author Herv&eacute; Bitteur
 */
public class SystemPoint
    extends SimplePoint
{
    //~ Constructors -----------------------------------------------------------

    //-------------//
    // SystemPoint //
    //-------------//
    /**
     * Creates a new SystemPoint object.
     */
    public SystemPoint ()
    {
    }

    //-------------//
    // SystemPoint //
    //-------------//
    /**
     * Creates a new SystemPoint object, by cloning another system point
     *
     * @param point the system point to clone
     */
    public SystemPoint (SystemPoint point)
    {
        super(point);
    }

    //-------------//
    // SystemPoint //
    //-------------//
    /**
     * Creates a new SystemPoint object
     *
     * @param x abscissa
     * @param y ordinate
     */
    public SystemPoint (int x,
                        int y)
    {
        super(x, y);
    }

    //-------------//
    // SystemPoint //
    //-------------//
    /**
     * Creates a new SystemPoint object
     *
     * @param x abscissa
     * @param y ordinate
     */
    public SystemPoint (double x,
                        double y)
    {
        super((int) Math.rint(x), (int) Math.rint(y));
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // distance //
    //----------//
    /**
     * Report the distance from this SystemPoint to another SystemPoint
     * @param pt the other SystemPoint
     * @return the euclidian distance
     */
    public double distance (SystemPoint pt)
    {
        return Math.hypot(pt.x - x, pt.y - y);
    }

    //----//
    // to //
    //----//
    /**
     * Report the vector that goed from this point to the other point
     * @param other the other point
     * @return the oriented vector towards the other point
     */
    public SystemPoint to (SystemPoint other)
    {
        return new SystemPoint(other.x - x, other.y - y);
    }
}
