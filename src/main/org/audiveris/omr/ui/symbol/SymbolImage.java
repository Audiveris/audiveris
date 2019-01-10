//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S y m b o l I m a g e                                      //
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
package org.audiveris.omr.ui.symbol;

import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * Class {@code SymbolImage} is a {@link BufferedImage} with the ability to define a
 * reference point, specified as a translation from the area center.
 *
 * @author Hervé Bitteur
 */
public class SymbolImage
        extends BufferedImage
{

    /** The reference point for this image. */
    private final Point refPoint;

    /**
     * Creates a new SymbolImage object.
     *
     * @param width    image width in pixels
     * @param height   image height in pixels
     * @param refPoint the reference point, if any, with coordinates defined
     *                 from image center
     */
    public SymbolImage (int width,
                        int height,
                        Point refPoint)
    {
        super(width, height, BufferedImage.TYPE_INT_ARGB);
        this.refPoint = refPoint;
    }

    //-------------//
    // getRefPoint //
    //-------------//
    /**
     * Report the (copy of) image reference point if any.
     *
     * @return the refPoint if any, otherwise null
     */
    public Point getRefPoint ()
    {
        if (refPoint != null) {
            return new Point(refPoint);
        } else {
            return null;
        }
    }
}
