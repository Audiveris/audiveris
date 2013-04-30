//----------------------------------------------------------------------------//
//                                                                            //
//                       S i m p l e R e c t a n g l e                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.common;

import java.awt.Rectangle;

/**
 * Class {@code SimpleRectangle} is meant to be subclassed to
 * represent a rectangle in a given context.
 *
 * @author Hervé Bitteur
 */
public abstract class SimpleRectangle
    extends Rectangle
{
    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // SimpleRectangle //
    //-----------------//
    /**
     * Creates an instance of {@code SimpleRectangle} with all items set to
     * zero.
     */
    public SimpleRectangle ()
    {
    }

    //-----------------//
    // SimpleRectangle //
    //-----------------//
    /**
     * Constructs a {@code SimpleRectangle} and initializes it with the
     * specified data.
     *
     * @param x the specified x
     * @param y the specified y
     * @param width the specified width
     * @param height the specified height
     */
    public SimpleRectangle (int x,
                            int y,
                            int width,
                            int height)
    {
        super(x, y, width, height);
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
          .append(",width=")
          .append(width)
          .append(",height=")
          .append(height)
          .append("]");

        return sb.toString();
    }
}
