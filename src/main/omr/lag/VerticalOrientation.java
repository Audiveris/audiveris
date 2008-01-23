//----------------------------------------------------------------------------//
//                                                                            //
//                   V e r t i c a l O r i e n t a t i o n                    //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.lag;

import omr.util.Implement;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class <code>VerticalOrientation</code> defines an orientation where sections
 * are vertical (coord is y, pos is x)
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class VerticalOrientation
    implements Oriented
{
    //~ Methods ----------------------------------------------------------------

    //------------//
    // isVertical //
    //------------//
    /**
     * A vertical lag IS indeed vertical !
     *
     * @return true
     */
    @Implement(Oriented.class)
    public boolean isVertical ()
    {
        return true;
    }

    //-----------//
    // switchRef //
    //-----------//
    /**
     * Retrieve absolute coordinates of a point relative to the (horizontal)
     * lag. Based on current lag implementation, this method implies to switch
     * coordinates values
     *
     * @param cp the relative coordinates
     * @param xy variable for the absolute coordinates, or null
     *
     * @return the absolute coordinates
     */
    @Implement(Oriented.class)
    public Point switchRef (Point cp,
                            Point xy)
    {
        if (xy == null) {
            xy = new Point();
        }

        // Vertical swap: coord->y, pos->x
        xy.x = cp.y;
        xy.y = cp.x;

        return xy;
    }

    //-----------//
    // switchRef //
    //-----------//
    /**
     * Retrieve absolute coordinates of a rectangle relative to the (horizontal)
     * lag. Based on current lag implementation, this method implies to switch
     * coordinates values
     *
     * @param cplt the relative coordinates
     * @param xywh variable for the absolute coordinates, or null
     *
     * @return the absolute coordinates
     */
    @Implement(Oriented.class)
    public Rectangle switchRef (Rectangle cplt,
                                Rectangle xywh)
    {
        if (xywh == null) {
            xywh = new Rectangle();
        }

        // Vertical swap: coord->y, pos->x, length->height, thickness->width
        xywh.x = cplt.y;
        xywh.y = cplt.x;
        xywh.width = cplt.height;
        xywh.height = cplt.width;

        return xywh;
    }
}
