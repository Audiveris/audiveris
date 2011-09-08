//----------------------------------------------------------------------------//
//                                                                            //
//                              O r i e n t e d                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.math.Line;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Interface <code>Oriented</code> defines the various methods linked to
 * orientation (vertical and horizontal).
 *
 * @author Hervé Bitteur
 */
public interface Oriented
{
    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getOrientation //
    //----------------//
    /**
     * Report the orientation constant
     * @return HORIZONTAL or VERTICAL
     */
    Orientation getOrientation ();

    //------------//
    // isVertical //
    //------------//
    /**
     * Return true if the entity is vertical, false if horizontal. Not a very
     * object-oriented approach but who cares?
     *
     * @return true if vertical, false otherwise
     */
    boolean isVertical ();

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
    Point2D.Double absolute (Point2D cp);

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
    PixelPoint absolute (Point cp);

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
    PixelRectangle absolute (Rectangle cplt);

    //----------//
    // oriented //
    //----------//
    /**
     * Given a point (x, y) in the absolute space, return the corresponding
     * (coord, pos) oriented point taking the lag orientation into account.
     *
     * @param cp the oriented (coord, pos) point
     * @return the corresponding absolute (x, y) point
     */
    Point oriented (PixelPoint cp);

    //----------//
    // oriented //
    //----------//
    /**
     * Given a point (x, y) in the absolute space, return the corresponding
     * (coord, pos) oriented point taking the lag orientation into account.
     *
     * @param cp the oriented (coord, pos) point
     * @return the corresponding absolute (x, y) point
     */
    Point2D oriented (Point2D cp);

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
    Rectangle oriented (PixelRectangle xywh);

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
    Line switchRef (Line relLine);
}
