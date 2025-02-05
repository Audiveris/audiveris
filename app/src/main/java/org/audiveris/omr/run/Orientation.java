//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     O r i e n t a t i o n                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.run;

import org.audiveris.omr.math.BasicLine;
import org.audiveris.omr.math.Line;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Class <code>Orientation</code> defines orientation as horizontal or vertical, and provides
 * convenient methods to convert entities between absolute and oriented definitions.
 *
 * @author Hervé Bitteur
 */
public enum Orientation
{
    HORIZONTAL,
    VERTICAL;

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // absolute //
    //----------//
    /**
     * Given a (coord, pos) oriented point, return the point (x, y) in the absolute
     * space taking the entity orientation into account.
     *
     * @param cp the oriented (coord, pos) point
     * @return the corresponding absolute (x, y) point
     */
    public Point absolute (Point cp)
    {
        if (cp == null) {
            return null;
        }

        return switch (this) {
            case HORIZONTAL -> new Point(cp.x, cp.y); // Identity: coord->x, pos->y
            case VERTICAL -> new Point(cp.y, cp.x); // swap: coord->y, pos->x
        };
    }

    //----------//
    // absolute //
    //----------//
    /**
     * Given a (coord, pos) oriented point, return the point (x, y) in the absolute
     * space taking the entity orientation into account.
     *
     * @param cp the oriented (coord, pos) point
     * @return the corresponding absolute (x, y) point
     */
    public Point2D.Double absolute (Point2D cp)
    {
        if (cp == null) {
            return null;
        }

        return switch (this) {
            case HORIZONTAL -> new Point2D.Double(cp.getX(), cp.getY()); // Identity
            case VERTICAL -> new Point2D.Double(cp.getY(), cp.getX()); // Swap: coord->y, pos->x
        };
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

        return switch (this) {
            // coord->x, pos->y, length->width, thickness->height
            case HORIZONTAL -> new Rectangle(cplt);

            // coord->y, pos->x, length->height, thickness->width
            case VERTICAL -> new Rectangle(cplt.y, cplt.x, cplt.height, cplt.width);
        };
    }

    //------------//
    // isVertical //
    //------------//
    /**
     * Return true if the entity is vertical, false if horizontal.
     * Not a very object-oriented approach but who cares?
     *
     * @return true if vertical, false otherwise
     */
    public boolean isVertical ()
    {
        return this == VERTICAL;
    }

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
        return switch (this) {
            case HORIZONTAL -> VERTICAL;
            case VERTICAL -> HORIZONTAL;
        };
    }

    //----------//
    // oriented //
    //----------//
    /**
     * Given a point (x, y) in the absolute space, return the corresponding
     * (coord, pos) oriented point taking the entity orientation into account.
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
     * (coord, pos) oriented point taking the entity orientation into account.
     *
     * @param xy the absolute (x, y) point
     * @return the corresponding oriented (coord, pos) point
     */
    public Point2D oriented (Point2D xy)
    {
        return absolute(xy); // Since involutive
    }

    //----------//
    // oriented //
    //----------//
    /**
     * Given an absolute rectangle (x, y, width, height) return the
     * corresponding oriented rectangle (coord, pos, length, thickness).
     *
     * @param xywh absolute rectangle (x, y, width, height).
     * @return the corresponding oriented rectangle (coord, pos, length, thickness)
     */
    public Rectangle oriented (Rectangle xywh)
    {
        // Use the fact that 'absolute' is involutive
        return new Rectangle(absolute(xywh));
    }

    //-----------//
    // switchRef //
    //-----------//
    /**
     * Given an oriented line, return the corresponding absolute line, or vice versa.
     *
     * @param relLine the oriented line
     * @return the corresponding absolute line.
     */
    public Line switchRef (Line relLine)
    {
        if (relLine == null) {
            return null;
        }

        return switch (this) {
            case HORIZONTAL -> {
                final Line absLine = new BasicLine();
                absLine.includeLine(relLine);
                yield absLine;
            }

            case VERTICAL -> relLine.swappedCoordinates();
        };
    }
}
