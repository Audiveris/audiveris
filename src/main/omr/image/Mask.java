//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             M a s k                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

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
