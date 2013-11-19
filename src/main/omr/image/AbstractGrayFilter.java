//----------------------------------------------------------------------------//
//                                                                            //
//                     A b s t r a c t G r a y F i l t e r                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import omr.util.StopWatch;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

/**
 * Class {@code AbstractGrayFilter} is the basis for filters
 * operating on gray-level images.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractGrayFilter
{
    //~ Methods ----------------------------------------------------------------

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
    public abstract void filter (final PixelSource input,
                                 final PixelSink output);

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
        if (!(input instanceof RenderedImage)) {
            throw new IllegalArgumentException(
                    "Input image is not a RenderedImage");
        }

        if (input.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            throw new IllegalArgumentException(
                    "Input image is not of type TYPE_BYTE_GRAY");
        }

        final BufferedImage output = new BufferedImage(
                input.getWidth(),
                input.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        filter(new BufferedSource(input), new BufferedSink(output));

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
    public PixelBuffer filter (final PixelSource input)
    {
        final PixelBuffer output = new PixelBuffer(
                new Dimension(input.getWidth(), input.getHeight()));
        StopWatch watch = new StopWatch(getClass().getSimpleName());
        watch.start("filter");

        filter(input, output);

        ///watch.print();

        return output;
    }
}
