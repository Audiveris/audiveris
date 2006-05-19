//-----------------------------------------------------------------------//
//                                                                       //
//                          S t a f f P o i n t                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import java.awt.*;

/**
 * Class <code>StaffPoint</code> is a simple Point that is meant to
 * represent a point inside a staff, and where coordinates are expressed in
 * units, the origin being the upper-left corner of the staff.
 *
 * <p>This specialization is used to take benefit of compiler checks, to
 * prevent the use of points with incorrect meaning or units.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class StaffPoint
    extends Point
{
    //------------//
    // StaffPoint //
    //------------//
    public StaffPoint ()
    {
    }

    //------------//
    // StaffPoint //
    //------------//
    public StaffPoint (int x,
		       int y)
    {
        super(x, y);
    }
}
