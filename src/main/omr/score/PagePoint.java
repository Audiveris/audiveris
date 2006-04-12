//-----------------------------------------------------------------------//
//                                                                       //
//                           P a g e P o i n t                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import java.awt.*;

/**
 * Class <code>PagePoint</code> is a simple Point that is meant to
 * represent a point in a virtual page, where Systems are arranged one
 * under the other as in a normal Sheet display, and where coordinates are
 * expressed in units.
 *
 * <p>This specialization is used to take benefit of compiler checks, to
 * prevent the use of points with incorrect meaning or units.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class PagePoint
    extends Point
{
    //-----------//
    // PagePoint //
    //-----------//
    public PagePoint ()
    {
    }

    //-----------//
    // PagePoint //
    //-----------//
    public PagePoint (int x,
                      int y)
    {
        super(x, y);
    }
}
