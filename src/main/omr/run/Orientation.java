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
import java.awt.geom.Point2D;

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

    //----------//
    // absolute //
    //----------//
    public Point oriented (PixelPoint xy)
    {
        return new Point(absolute(xy)); // Since involutive
    }

    //----------//
    // absolute //
    //----------//
    public PixelPoint absolute (Point cp)
    {
        if (cp == null) {
            return null;
        }

        switch (this) {
        case HORIZONTAL :

            // Identity: coord->x, pos->y
            return new PixelPoint(cp.x, cp.y);

        default :
        case VERTICAL :

            // swap: coord->y, pos->x
            return new PixelPoint(cp.y, cp.x);
        }
    }

    //----------//
    // absolute //
    //----------//
    public Point2D.Double absolute (Point2D cp)
    {
        if (cp == null) {
            return null;
        }

        switch (this) {
        case HORIZONTAL :

            // Identity
            return new Point2D.Double(cp.getX(), cp.getY());

        default :
        case VERTICAL :

            // Swap: coord->y, pos->x
            return new Point2D.Double(cp.getY(), cp.getX());
        }
    }

    //----------//
    // absolute //
    //----------//
    public PixelRectangle absolute (Rectangle cplt)
    {
        if (cplt == null) {
            return null;
        }

        switch (this) {
        case HORIZONTAL :

            // coord->x, pos->y, length->width, thickness->height
            return new PixelRectangle(cplt);

        default :
        case VERTICAL :

            // coord->y, pos->x, length->height, thickness->width
            return new PixelRectangle(cplt.y, cplt.x, cplt.height, cplt.width);
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

    //----------//
    // oriented //
    //----------//
    public Rectangle oriented (PixelRectangle xywh)
    {
        // Use the fact that 'absolute' is involutive
        return new Rectangle(absolute(xywh));
    }
}
