//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        P i x e l S i n k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.image;

/**
 * Interface {@code PixelSink} defines the operations expected from a rectangular pixel
 * sink.
 *
 * @author Hervé Bitteur
 */
public interface PixelSink
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the height of the rectangular sink
     *
     * @return the sink height
     */
    int getHeight ();

    /**
     * Report the width of the rectangular sink.
     *
     * @return the sink width
     */
    int getWidth ();

    /**
     * Assign the provided value to the pixel at location (x, y).
     *
     * @param x   pixel abscissa
     * @param y   pixel ordinate
     * @param val new pixel value, assumed to be in range 0..255
     */
    void setValue (int x,
                   int y,
                   int val);
}
