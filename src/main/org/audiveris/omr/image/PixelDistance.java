//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    P i x e l D i s t a n c e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.image;

import java.util.Comparator;

/**
 * Class {@code PixelDistance} records a distance at a given pixel location.
 *
 * @author Hervé Bitteur
 */
public class PixelDistance

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
