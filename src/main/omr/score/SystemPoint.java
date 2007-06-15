//----------------------------------------------------------------------------//
//                                                                            //
//                           S y s t e m P o i n t                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import java.awt.*;

/**
 * Class <code>SystemPoint</code> is a simple Point that is meant to represent a
 * point inside a system, and where coordinates are expressed in units, the
 * origin being the upper-left corner of the system.
 *
 * <p>This specialization is used to take benefit of compiler checks, to prevent
 * the use of points with incorrect meaning or units.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SystemPoint
    extends Point
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
    public double distance (SystemPoint pt)
    {
        return Math.hypot(pt.x - x, pt.y - y);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "SystemPoint[x=" + x + ",y=" + y + "]";
    }
}
