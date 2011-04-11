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

import java.awt.*;

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

    //-----------//
    // switchRef //
    //-----------//
    /**
     * Given a (coord, pos) oriented point, return the point (x, y) in the
     * absolute space taking the lag orientation into account. The same method
     * can be used for the opposite computation, since the method is involutive.
     *
     * @param cp the oriented (coord, pos) point
     * @param xy output parameter, or null if not pre-allocated
     *
     * @return the corresponding absolute (x, y) point
     */
    PixelPoint switchRef (Point      cp,
                          PixelPoint xy);

    //-----------//
    // switchRef //
    //-----------//
    /**
     * Given a (coord, pos, length, thickness) oriented rectangle, return the
     * corresponding absolute rectangle, or vice versa.
     *
     * @param cplt the oriented rectangle (coord, pos, length, thickness)
     * @param xywh output parameter (x, y, width, height), or null if not
     * pre-allocated
     *
     * @return the corresponding absolute rectangle (x, y, width, height).
     */
    PixelRectangle switchRef (Rectangle      cplt,
                              PixelRectangle xywh);

    //-----------//
    // switchRef //
    //-----------//
    /**
     * Given an oriented line, return the corresponding absolute line, or vice
     * versa.
     *
     * @param relLine the oriented line
     *
     * @return the corresponding absolute line.
     */
    Line switchRef (Line relLine);
}
