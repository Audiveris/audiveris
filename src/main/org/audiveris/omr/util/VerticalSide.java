//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    V e r t i c a l S i d e                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
 * Enum {@code VerticalSide} is meant to ease programmatic use of top
 * and bottom sides of staves, system boundaries, etc...
 *
 * @author Hervé Bitteur
 */
public enum VerticalSide
{
    TOP,
    BOTTOM;

    /**
     * Report the opposite of this side
     *
     * @return the opposite side
     */
    public VerticalSide opposite ()
    {
        return (this == TOP) ? BOTTOM : TOP;
    }

    /**
     * Report the ordinate direction when going on this side.
     *
     * @return yDir
     */
    public int direction ()
    {
        return (this == TOP) ? (-1) : 1;
    }

    /**
     * Report the VerticalSide for the provided ordinate direction
     *
     * @param yDir ordinate direction
     * @return side for yDir
     */
    public static VerticalSide of (int yDir)
    {
        return (yDir == -1) ? TOP : ((yDir == +1) ? BOTTOM : null);
    }
}
