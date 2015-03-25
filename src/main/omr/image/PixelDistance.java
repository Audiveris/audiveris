//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    P i x e l D i s t a n c e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import java.util.Comparator;

/**
 * Class {@code PixelDistance} records a distance at a given pixel location.
 *
 * @author Hervé Bitteur
 */
public class PixelDistance ///implements Comparable<PixelDistance>

{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** To sort by increasing value, regardless of (x,y). */
    public static final Comparator<PixelDistance> byValue = new Comparator<PixelDistance>()
    {
        @Override
        public int compare (PixelDistance o1,
                            PixelDistance o2)
        {
            return Double.compare(o1.d, o2.d);
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Location abscissa. */
    public final int x;

    /** Location ordinate. */
    public final int y;

    /** Distance. */
    public final double d;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new PixelDistance object.
     *
     * @param x location abscissa
     * @param y location ordinate
     * @param d measured distance at this location
     */
    public PixelDistance (int x,
                          int y,
                          double d)
    {
        this.x = x;
        this.y = y;
        this.d = d;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return String.format("(x:%4d y:%4d dist:%.3f)", x, y, d);
    }
}
