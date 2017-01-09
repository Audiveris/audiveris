//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             M a s k                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;

/**
 * Class {@code Mask} defines a mask to be applied on an image.
 * The mask defines a collection of points at which an image can be tested.
 * The mask is defined in an absolute location.
 *
 * @author Hervé Bitteur
 */
public class Mask
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Mask.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Mask area. */
    private final Area area;

    private final Rectangle rect;

    private final Table.UnsignedByte bitmap;

    /** Number of relevant points. */
    private int pointCount;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Mask object.
     *
     * @param area definition of the relevant area
     */
    public Mask (Area area)
    {
        this.area = area;

        rect = area.getBounds();
        bitmap = computeRelevantPoints(area);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // apply //
    //-------//
    /**
     * Apply the mask and call the provided Adapter for each relevant
     * point of the mask.
     *
     * @param adapter call-back adapter for each relevant point of the mask
     */
    public void apply (Adapter adapter)
    {
        Point loc = rect.getLocation();

        for (int y = 0; y < rect.height; y++) {
            int ay = y + loc.y; // Absolute ordinate

            for (int x = 0; x < rect.width; x++) {
                if (bitmap.getValue(x, y) == 0) {
                    int ax = x + loc.x; // Absolute abscissa
                    adapter.process(ax, ay);
                }
            }
        }
    }

    //------//
    // dump //
    //------//
    public void dump (String title)
    {
        bitmap.dump(title);
    }

    //---------//
    // getArea //
    //---------//
    /**
     * @return the area
     */
    public Area getArea ()
    {
        return area;
    }

    //---------------//
    // getPointCount //
    //---------------//
    /**
     * @return the pointCount
     */
    public int getPointCount ()
    {
        return pointCount;
    }

    //-----------------------//
    // computeRelevantPoints //
    //-----------------------//
    private Table.UnsignedByte computeRelevantPoints (Area area)
    {
        Table.UnsignedByte table = new Table.UnsignedByte(rect.width, rect.height);
        Point loc = rect.getLocation();
        table.fill(PixelFilter.BACKGROUND);

        for (int y = 0; y < rect.height; y++) {
            int ay = y + loc.y; // Absolute ordinate

            for (int x = 0; x < rect.width; x++) {
                int ax = x + loc.x; // Absolute abscissa

                if (area.contains(ax, ay)) {
                    table.setValue(x, y, 0);
                    pointCount++;
                }
            }
        }

        return table;
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    public static interface Adapter
    {
        //~ Methods --------------------------------------------------------------------------------

        /**
         * Method called on each mask relevant point.
         *
         * @param x absolute point abscissa
         * @param y absolute point ordinate
         */
        public void process (int x,
                             int y);
    }
}
