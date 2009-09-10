//----------------------------------------------------------------------------//
//                                                                            //
//                           S i m p l e P o i n t                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.common;

import java.awt.Point;

/**
 * Class <code>SimplePoint</code> is meant to be subclassed by more specific
 * definitions according to the context
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class SimplePoint
    extends Point
{
    //~ Constructors -----------------------------------------------------------

    //-------------//
    // SimplePoint //
    //-------------//
    /**
     * Creates a new SimplePoint object.
     */
    public SimplePoint ()
    {
    }

    //-------------//
    // SimplePoint //
    //-------------//
    /**
     * Creates a new SimplePoint object, by cloning an untyped point
     *
     * @param x abscissa
     * @param y ordinate
     */
    public SimplePoint (int x,
                        int y)
    {
        super(x, y);
    }

    //-------------//
    // SimplePoint //
    //-------------//
    /**
     * Creates a new SimplePoint object, by cloning another SimplePoint
     *
     * @param point the point to clone
     */
    public SimplePoint (SimplePoint point)
    {
        super(point);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("[x=")
          .append(x)
          .append(",y=")
          .append(y)
          .append("]");

        return sb.toString();
    }
}
