//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     P i x e l F i l t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.image;

import ij.process.ByteProcessor;

/**
 * Interface {@code PixelFilter} reports the foreground pixels of a {@link PixelSource}.
 *
 * @author Hervé Bitteur
 */
public interface PixelFilter
        extends PixelSource
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Run the filter on source image and report the filtered image.
     *
     * @return the filtered image
     */
    ByteProcessor filteredImage ();

    /**
     * Report the source context at provided location.
     * This is meant for administration and display purposes, it does not need
     * to be very efficient.
     *
     * @param x abscissa value
     * @param y ordinate value
     * @return the contextual data at this location
     */
    Context getContext (int x,
                        int y);

    /**
     * Report whether the pixel at location (x,y) is a foreground pixel
     * or not.
     * It is assumed that this feature is efficiently implemented, since it will
     * be typically called several million times.
     *
     * @param x abscissa value
     * @param y ordinate value
     * @return true for a foreground pixel, false for a background pixel
     */
    boolean isFore (int x,
                    int y);

    //~ Inner Classes ------------------------------------------------------------------------------
    /**
     * Structure used to report precise context of the source.
     * It can be extended for more specialized data.
     */
    class Context
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Threshold used on pixel value. */
        public final double threshold;

        //~ Constructors ---------------------------------------------------------------------------
        public Context (double threshold)
        {
            this.threshold = threshold;
        }
    }
}
