//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B a s i c L U T                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.moments;

/**
 * Class {@code BasicLUT} is a straightforward LUT implementation.
 *
 * @author Hervé Bitteur
 */
public final class BasicLUT
        implements LUT
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** LUT radius. */
    private final int RADIUS;

    /** LUT size (to implement arrays [-RADIUS, RADIUS]). */
    private final int SIZE;

    /** The table of values for each integer (x,y) location. */
    private final double[][] table;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicLUT object.
     *
     * @param radius the desired LUT radius for a [-radius .. radius] table.
     */
    public BasicLUT (int radius)
    {
        if (radius <= 0) {
            throw new IllegalArgumentException("Cannot allocate LUT with radius " + radius);
        }

        this.RADIUS = radius;
        SIZE = 1 + (2 * radius);
        table = new double[SIZE][SIZE];
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // assign //
    //--------//
    @Override
    public void assign (int x,
                        int y,
                        double value)
    {
        table[x][y] = value;
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (double radius)
    {
        return radius < RADIUS;
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (double x,
                             double y)
    {
        return (x >= 0) && (x < SIZE) && (y >= 0) && (y < SIZE);
    }

    //-----------//
    // getRadius //
    //-----------//
    @Override
    public int getRadius ()
    {
        return RADIUS;
    }

    //---------//
    // getSize //
    //---------//
    @Override
    public int getSize ()
    {
        return SIZE;
    }

    //-------------//
    // interpolate //
    //-------------//
    @Override
    public double interpolate (double px,
                               double py)
    {
        // Integer coordinates, by truncating precise coordinates
        final int x = (int) px;
        final int y = (int) py;

        // Beware of point on LUT border
        final int max = SIZE - 1;

        // Value at [x,y]
        final double vxy = table[x][y];

        if (x == max) {
            if (y == max) {
                return vxy; // v[x,y]
            } else {
                final double iy = py - y;

                return vxy + (iy * (table[x][y + 1] - vxy)); // v[x,py]
            }
        } else {
            final double ix = px - x;

            // Value at [px,y]
            final double vpxy = vxy + (ix * (table[x + 1][y] - vxy));

            if (y == max) {
                return vpxy; // v[px,y]
            } else {
                final double iy = py - y;

                // Value at [x,y+1]
                final double vxy1 = table[x][y + 1];

                // Value at [px, y+1]
                final double vpxy1 = vxy1 + (ix * (table[x + 1][y + 1] - vxy1));

                return vpxy + (iy * (vpxy1 - vpxy)); // v[px,py]
            }
        }
    }
}
