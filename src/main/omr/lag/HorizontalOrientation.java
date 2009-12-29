//----------------------------------------------------------------------------//
//                                                                            //
//                 H o r i z o n t a l O r i e n t a t i o n                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
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
    //~ Static fields/initializers ---------------------------------------------

    /** Constant orientation */
    public static final LagOrientation orientation = LagOrientation.HORIZONTAL;

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getOrientation //
    //----------------//
    @Implement(Oriented.class)
    public LagOrientation getOrientation ()
    {
        return orientation;
    }

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
