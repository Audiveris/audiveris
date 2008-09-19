//----------------------------------------------------------------------------//
//                                                                            //
//                 H o r i z o n t a l O r i e n t a t i o n                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.lag;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.util.Implement;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class <code>HorizontalOrientation</code> defines an orientation where
 * sections are horizontal (coord is x, pos is y)
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class HorizontalOrientation
    implements Oriented
{
    //~ Methods ----------------------------------------------------------------

    //------------//
    // isVertical //
    //------------//
    /**
     * A horizontal lag is NOT vertical
     *
     * @return false
     */
    @Implement(Oriented.class)
    public boolean isVertical ()
    {
        return false;
    }

    //-----------//
    // switchRef //
    //-----------//
    /**
     * Retrieve absolute coordinates of a point relative to the (horizontal)
     * lag. Based on current lag implementation, this method is a pass-through.
     *
     * @param cp the relative coordinates
     * @param xy variable for the absolute coordinates, or null
     *
     * @return the absolute coordinates
     */
    @Implement(Oriented.class)
    public PixelPoint switchRef (Point      cp,
                                 PixelPoint xy)
    {
        if (xy == null) {
            xy = new PixelPoint();
        }

        // Horizontal: coord->x, pos->y
        xy.x = cp.x;
        xy.y = cp.y;

        return xy;
    }

    //-----------//
    // switchRef //
    //-----------//
    /**
     * Retrieve absolute coordinates of a rectangle relative to the (horizontal)
     * lag. Based on current lag implementation, this method is a pass-through.
     *
     * @param cplt the relative coordinates
     * @param xywh variable for the absolute coordinates, or null
     *
     * @return the absolute coordinates
     */
    @Implement(Oriented.class)
    public PixelRectangle switchRef (Rectangle      cplt,
                                     PixelRectangle xywh)
    {
        if (xywh == null) {
            xywh = new PixelRectangle();
        }

        // Horizontal: coord->x, pos->y, length->width, thickness->height
        xywh.x = cplt.x;
        xywh.y = cplt.y;
        xywh.width = cplt.width;
        xywh.height = cplt.height;

        return xywh;
    }
}
