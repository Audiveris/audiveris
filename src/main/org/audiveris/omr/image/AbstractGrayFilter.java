//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A b s t r a c t G r a y F i l t e r                              //
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
package org.audiveris.omr.image;

import ij.process.ByteProcessor;

import org.audiveris.omr.util.StopWatch;

import java.awt.image.BufferedImage;

/**
 * Class {@code AbstractGrayFilter} is the basis for filters operating on gray-level
 * images.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractGrayFilter
{

    //--------//
    // filter //
    //--------//
    /**
     * Apply this filter on a given input image and store results in
     * the given output image.
     *
     * @param input  the input image
     * @param output the output image.
     */
    public abstract void filter (final ByteProcessor input,
                                 final ByteProcessor output);

    //--------//
    // filter //
    //--------//
    /**
     * Apply this filter on a given input image.
     *
     * @param input the input image, assumed of TYPE_BYTE_GRAY
     * @return the filtered image
     */
    public BufferedImage filter (final BufferedImage input)
    {
        if (input == null) {
            throw new IllegalArgumentException("Input image is null");
        }

        if (input.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            throw new IllegalArgumentException("Input image is not of type TYPE_BYTE_GRAY");
        }

        final BufferedImage output = new BufferedImage(
                input.getWidth(),
                input.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        filter(new ByteProcessor(input), new ByteProcessor(output));

        return output;
    }

    //--------//
    // filter //
    //--------//
    /**
     * Apply this filter on a given input image.
     *
     * @param input the input image
     * @return the filtered image
     */
    public ByteProcessor filter (final ByteProcessor input)
    {
        final ByteProcessor output = new ByteProcessor(input.getWidth(), input.getHeight());
        StopWatch watch = new StopWatch(getClass().getSimpleName());
        watch.start("filter");

        filter(input, output);

        ///watch.print();
        return output;
    }
}
