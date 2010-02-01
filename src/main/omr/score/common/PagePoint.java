//----------------------------------------------------------------------------//
//                                                                            //
//                             P a g e P o i n t                              //
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
 * Class <code>PagePoint</code> is a simple Point that is meant to represent a
 * point in a virtual page, where Systems are arranged one under the other as in
 * a normal Sheet display, and where coordinates are expressed in units.
 *
 * <p>This specialization is used to take benefit of compiler checks, to prevent
 * the use of points with incorrect meaning or units.
 *
 * @author Hervé Bitteur
 */
public class PagePoint
    extends SimplePoint
{
    //~ Constructors -----------------------------------------------------------

    //-----------//
    // PagePoint //
    //-----------//
    /**
     * Creates a new PagePoint object.
     */
    public PagePoint ()
    {
    }

    //-----------//
    // PagePoint //
    //-----------//
    /**
     * Creates a new PagePoint object, by cloning an untyped point
     *
     * @param x abscissa
     * @param y ordinate
     */
    public PagePoint (int x,
                      int y)
    {
        super(x, y);
    }
}
