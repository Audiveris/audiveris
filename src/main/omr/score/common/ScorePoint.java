//----------------------------------------------------------------------------//
//                                                                            //
//                            S c o r e P o i n t                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.common;


/**
 * Class <code>ScorePoint</code> is a simple Point that is meant to represent a
 * point in the Score display, where Systems are arranged horizontally one after
 * the other, and where coordinates are expressed in units.
 *
 * <p>This specialization is used to take benefit of compiler checks, to prevent
 * the use of points with incorrect meaning or units.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScorePoint
    extends SimplePoint
{
    //~ Constructors -----------------------------------------------------------

    //------------//
    // ScorePoint //
    //------------//
    /**
     * Creates a new ScorePoint object.
     */
    public ScorePoint ()
    {
    }

    //------------//
    // ScorePoint //
    //------------//
    /**
     * Creates a new ScorePoint object, by cloning an untyped point
     *
     * @param x abscissa
     * @param y ordinate
     */
    public ScorePoint (int x,
                       int y)
    {
        super(x, y);
    }
}
