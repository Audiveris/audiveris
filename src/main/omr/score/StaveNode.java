//-----------------------------------------------------------------------//
//                                                                       //
//                           S t a v e N o d e                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import java.awt.*;

/**
 * Class <code>StaveNode</code> is an abstract class that is subclassed for
 * any MusicNode, whose location is known with respect to its containing
 * stave. So this class encapsulated a direct link to the enclosing stave.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class StaveNode
        extends MusicNode
{
    //~ Instance variables ------------------------------------------------

    /**
     * Containing stave
     */
    Stave stave;

    //~ Constructors ------------------------------------------------------

    //-----------//
    // StaveNode //
    //-----------//

    /**
     * Create a StaveNode
     *
     * @param container the (direct) container of the node
     * @param stave     the enclosing stave, which is never the direct
     *                  container by the way
     */
    public StaveNode (MusicNode container,
                      Stave stave)
    {
        super(container);
        this.stave = stave;
    }

    //~ Methods -----------------------------------------------------------

    //-----------//
    // getOrigin //
    //-----------//

    /**
     * The display origin which is relevant for this node (this is the
     * stave origin)
     *
     * @return the display origin
     */
    public Point getOrigin ()
    {
        return stave.getOrigin();
    }

    //----------//
    // setStave //
    //----------//

    /**
     * Set the containing stave
     *
     * @param stave the stave entity
     */
    public void setStave (Stave stave)
    {
        this.stave = stave;
    }
}
