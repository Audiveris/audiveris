//----------------------------------------------------------------------------//
//                                                                            //
//                           O r i e n t a t i o n                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.math.BasicLine;
import omr.math.Line;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class <code>Orientation</code> defines orientation as horizontal or
 * vertical
 *
 * @author Hervé Bitteur
 */
public enum Orientation
    implements Oriented {
    HORIZONTAL,
    VERTICAL;

    //----------------//
    // getOrientation //
    //----------------//
    public Orientation getOrientation ()
    {
        return this;
    }

    //------------//
    // isVertical //
    //------------//
    public boolean isVertical ()
    {
        return this == VERTICAL;
    }

    //-----------//
    // switchRef //
    //-----------//
    public PixelPoint switchRef (Point      cp,
                                 PixelPoint xy)
    {
        if (cp == null) {
            return null;
        }

        if (xy == null) {
            xy = new PixelPoint();
        }

        switch (this) {
        case HORIZONTAL :
            // coord->x, pos->y
            xy.x = cp.x;
            xy.y = cp.y;

            return xy;

        default :
        case VERTICAL :
            // swap: coord->y, pos->x
            xy.x = cp.y;
            xy.y = cp.x;

            return xy;
        }
    }

    //-----------//
    // switchRef //
    //-----------//
    public PixelRectangle switchRef (Rectangle      cplt,
                                     PixelRectangle xywh)
    {
        if (cplt == null) {
            return null;
        }

        if (xywh == null) {
            xywh = new PixelRectangle();
        }

        switch (this) {
        case HORIZONTAL :
            // coord->x, pos->y, length->width, thickness->height
            xywh.x = cplt.x;
            xywh.y = cplt.y;
            xywh.width = cplt.width;
            xywh.height = cplt.height;

            return xywh;

        default :
        case VERTICAL :
            // coord->y, pos->x, length->height, thickness->width
            xywh.x = cplt.y;
            xywh.y = cplt.x;
            xywh.width = cplt.height;
            xywh.height = cplt.width;

            return xywh;
        }
    }

    //-----------//
    // switchRef //
    //-----------//
    public Line switchRef (Line relLine)
    {
        if (relLine == null) {
            return null;
        }

        switch (this) {
        case HORIZONTAL :

            Line absLine = new BasicLine();
            absLine.includeLine(relLine);

            return absLine;

        default :
        case VERTICAL :
            return relLine.swappedCoordinates();
        }
    }
}
