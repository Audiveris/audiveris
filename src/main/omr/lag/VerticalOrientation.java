//-----------------------------------------------------------------------//
//                                                                       //
//                 V e r t i c a l O r i e n t a t i o n                 //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.lag;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class <code>VerticalOrientation</code> defines an orientation where
 * sections are vertical (coord is y, pos is x)
 */
public class VerticalOrientation
    implements Oriented,
               java.io.Serializable
{
    //------------//
    // isVertical //
    //------------//
    public boolean isVertical ()
    {
        return true;
    }

    //-----------//
    // switchRef //
    //-----------//
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
