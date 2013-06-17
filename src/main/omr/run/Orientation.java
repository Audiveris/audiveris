//----------------------------------------------------------------------------//
//                                                                            //
//                           O r i e n t a t i o n                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.math.BasicLine;
import omr.math.Line;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Class {@code Orientation} defines orientation as horizontal or
 * vertical, and provides convenient methods to convert entities between
 * absolute and oriented definitions.
 *
 * @author Hervé Bitteur
 */
public enum Orientation
{

    HORIZONTAL,
    VERTICAL;

    //----------//
    // opposite //
    //----------//
    /**
     * Report the orientation opposite to this one
     *
     * @return the opposite orientation
     */
    public Orientation opposite ()
    {
        switch (this) {
        case HORIZONTAL:
            return VERTICAL;

        default:
        case VERTICAL:
            return HORIZONTAL;
        }
    }

    //------------//
    // isVertical //
    //------------//
    /**
     * Return true if the entity is vertical, false if horizontal. Not a very
     * object-oriented approach but who cares?
     *
     * @return true if vertical, false otherwise
     */
    public boolean isVertical ()
    {
        return this == VERTICAL;
    }

    //----------//
    // oriented //
    //----------//
    /**
     * Given a point (x, y) in the absolute space, return the corresponding
     * (coord, pos) oriented point taking the lag orientation into account.
     *
     * @param xy the absolute (x, y) point
     * @return the corresponding oriented (coord, pos) point
     */
    public Point oriented (Point xy)
    {
        return new Point(absolute(xy)); // Since involutive
    }

    //----------//
    // oriented //
    //----------//
    /**
     * Given a point (x, y) in the absolute space, return the corresponding
     * (coord, pos) oriented point taking the lag orientation into account.
     *
     * @param xy the absolute (x, y) point
     * @return the corresponding oriented (coord, pos) point
     */
    public Point2D oriented (Point2D xy)
    {
        return absolute(xy); // Since involutive
    }

    //----------//
    // absolute //
    //----------//
    /**
     * Given a (coord, pos) oriented point, return the point (x, y) in the
     * absolute space taking the lag orientation into account.
     *
     * @param cp the oriented (coord, pos) point
     * @return the corresponding absolute (x, y) point
     */
    public Point absolute (Point cp)
    {
        if (cp == null) {
            return null;
        }

        switch (this) {
        case HORIZONTAL:

            // Identity: coord->x, pos->y
            return new Point(cp.x, cp.y);

        default:
        case VERTICAL:

            // swap: coord->y, pos->x
            return new Point(cp.y, cp.x);
        }
    }

    //----------//
    // absolute //
    //----------//
    /**
     * Given a (coord, pos) oriented point, return the point (x, y) in the
     * absolute space taking the lag orientation into account.
     *
     * @param cp the oriented (coord, pos) point
     * @return the corresponding absolute (x, y) point
     */
    public Point2D.Double absolute (Point2D cp)
    {
        if (cp == null) {
            return null;
        }

        switch (this) {
        case HORIZONTAL:

            // Identity
            return new Point2D.Double(cp.getX(), cp.getY());

        default:
        case VERTICAL:

            // Swap: coord->y, pos->x
            return new Point2D.Double(cp.getY(), cp.getX());
        }
    }

    //----------//
    // absolute //
    //----------//
    /**
     * Given a (coord, pos, length, thickness) oriented rectangle, return the
     * corresponding absolute rectangle.
     *
     * @param cplt the oriented rectangle (coord, pos, length, thickness)
     * @return the corresponding absolute rectangle (x, y, width, height).
     */
    public Rectangle absolute (Rectangle cplt)
    {
        if (cplt == null) {
            return null;
        }

        switch (this) {
        case HORIZONTAL:

            // coord->x, pos->y, length->width, thickness->height
            return new Rectangle(cplt);

        default:
        case VERTICAL:

            // coord->y, pos->x, length->height, thickness->width
            return new Rectangle(cplt.y, cplt.x, cplt.height, cplt.width);
        }
    }

    //-----------//
    // switchRef //
    //-----------//
    /**
     * Given an oriented line, return the corresponding absolute line, or vice
     * versa.
     *
     * @param relLine the oriented line
     * @return the corresponding absolute line.
     */
    public Line switchRef (Line relLine)
    {
        if (relLine == null) {
            return null;
        }

        switch (this) {
        case HORIZONTAL:

            Line absLine = new BasicLine();
            absLine.includeLine(relLine);

            return absLine;

        default:
        case VERTICAL:
            return relLine.swappedCoordinates();
        }
    }

    //----------//
    // oriented //
    //----------//
    /**
     * Given an absolute rectangle (x, y, width, height) return the
     * corresponding oriented rectangle (coord, pos, length, thickness).
     *
     * @param xywh absolute rectangle (x, y, width, height).
     * @return the corresponding oriented rectangle (coord, pos, length,
     *         thickness)
     */
    public Rectangle oriented (Rectangle xywh)
    {
        // Use the fact that 'absolute' is involutive
        return new Rectangle(absolute(xywh));
    }
}
