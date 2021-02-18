//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  H o r i z o n t a l S i d e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.util;

/**
 * Enum {@code HorizontalSide} is meant to ease programmatic use of left and right
 * sides of staves, lines, etc...
 *
 * @author Hervé Bitteur
 */
public enum HorizontalSide
{
    LEFT,
    RIGHT;

    /**
     * Report the opposite of this side
     *
     * @return the opposite side
     */
    public HorizontalSide opposite ()
    {
        return (this == LEFT) ? RIGHT : LEFT;
    }

    /**
     * Report the abscissa direction when going on this side.
     *
     * @return xDir
     */
    public int direction ()
    {
        return (this == LEFT) ? -1 : 1;
    }

    /**
     * Report the HorizontalSide for the provided abscissa direction
     *
     * @param xDir abscissa direction
     * @return side for xDir
     */
    public static HorizontalSide of (int xDir)
    {
        return (xDir == -1) ? LEFT : (xDir == +1) ? RIGHT : null;
    }
}
