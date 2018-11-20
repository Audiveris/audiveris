//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P i x e l S o u r c e                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
 * Interface {@code PixelSource} defines the operations expected from a rectangular
 * pixel source.
 * <p>
 * It is a <b>raw</b> pixel source, because just the pixel gray value is returned, with no
 * interpretation as foreground or background.
 * This additional interpretation is reported by a {@link PixelSource}.
 *
 * @author Hervé Bitteur
 */
public interface PixelSource
{

    /** Default value for background pixel. */
    public static final int BACKGROUND = 255;

    /** Default value for foreground pixel. */
    public static final int FOREGROUND = 0;

    /**
     * Report the pixel element, as read at location (x, y) in the source.
     *
     * @param x abscissa value
     * @param y ordinate value
     * @return the pixel value using range 0..255 (0/black for foreground,
     *         255/white for background)
     */
    int get (int x,
             int y);

    /**
     * Report the height of the rectangular source
     *
     * @return the source height
     */
    int getHeight ();

    /**
     * Report the width of the rectangular source.
     *
     * @return the source width
     */
    int getWidth ();
}
