//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     P i x e l B u f f e r                                      //
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

import ij.process.ByteProcessor;

import net.jcip.annotations.ThreadSafe;
import static org.audiveris.omr.image.PixelSource.BACKGROUND;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Class {@code PixelBuffer} handles a plain rectangular buffer of bytes.
 * <p>
 * It is an efficient {@link PixelFilter} both for writing and for reading.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class PixelBuffer
        extends Table.UnsignedByte
        implements PixelFilter, PixelSink
{

    private static final Logger logger = LoggerFactory.getLogger(PixelBuffer.class);

    /**
     * Creates a new PixelBuffer object.
     *
     * @param dimension the buffer dimension
     */
    public PixelBuffer (Dimension dimension)
    {
        super(dimension.width, dimension.height);

        // Initialize the whole buffer with background color value
        fill(BACKGROUND);
    }

    //-------------//
    // PixelBuffer //
    //-------------//
    /**
     * Creates a PixelBuffer from (the first band of) a BufferedImage.
     *
     * @param image the provided BufferedImage
     */
    public PixelBuffer (BufferedImage image)
    {
        this(new Dimension(image.getWidth(), image.getHeight()));

        final StopWatch watch = new StopWatch("PixelBuffer");
        watch.start("image->buffer");

        final int numBands = image.getSampleModel().getNumBands();
        final int[] pixel = new int[numBands];
        final Raster raster = image.getRaster();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                raster.getPixel(x, y, pixel);
                // We use just the first band
                setValue(x, y, pixel[0]);
            }
        }

        ///watch.print();
    }

    //-------------//
    // PixelBuffer //
    //-------------//
    /**
     * Creates a new PixelBuffer object from a PixelFilter.
     *
     * @param filter a filter to deliver foreground/background pixels
     */
    public PixelBuffer (PixelFilter filter)
    {
        this(new Dimension(filter.getWidth(), filter.getHeight()));

        final StopWatch watch = new StopWatch("PixelBuffer");
        watch.start("filter->buffer");

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (filter.isFore(x, y)) {
                    setValue(x, y, 0);
                }
            }
        }

        ///watch.print();
    }

    @Override
    public ByteProcessor filteredImage ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int get (int x,
                    int y)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //------------//
    // getContext //
    //------------//
    @Override
    public Context getContext (int x,
                               int y)
    {
        return new Context(BACKGROUND / 2);
    }

    //---------//
    // getCopy //
    //---------//
    @Override
    public PixelBuffer getCopy (Rectangle roi)
    {
        PixelBuffer copy;

        if (roi == null) {
            copy = new PixelBuffer(new Dimension(width, height));
            System.arraycopy(data, 0, copy.data, 0, data.length);
        } else {
            checkRoi(roi);

            copy = new PixelBuffer(new Dimension(roi.width, roi.height));

            for (int y = 0; y < roi.height; y++) {
                int p = ((y + roi.y) * width) + roi.x;
                System.arraycopy(data, p, copy.data, y * roi.width, roi.width);
            }
        }

        return copy;
    }

    //--------------//
    // injectBuffer //
    //--------------//
    /**
     * Inject all non-background pixels of that buffer into this buffer.
     * That buffer bounds are assumed to be within this buffer bounds.
     *
     * @param that   the buffer to inject
     * @param origin relative location where that buffer must be injected
     */
    public void injectBuffer (PixelBuffer that,
                              Point origin)
    {
        for (int x = 0, w = that.getWidth(); x < w; x++) {
            for (int y = 0, h = that.getHeight(); y < h; y++) {
                int val = that.getValue(x, y);

                if (val != BACKGROUND) {
                    this.setValue(x + origin.x, y + origin.y, val);
                }
            }
        }
    }

    //--------//
    // isFore //
    //--------//
    @Override
    public boolean isFore (int x,
                           int y)
    {
        return getValue(x, y) < 225; //TODO: Why not 255 ?????????? A typo?
    }

    //-----------------//
    // toBufferedImage //
    //-----------------//
    /**
     * Report the BufferedImage for this buffer.
     *
     * @return corresponding image
     */
    public BufferedImage toBufferedImage ()
    {
        StopWatch watch = new StopWatch("PixelBuffer");
        watch.start("toImage");

        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final WritableRaster raster = img.getRaster();
        final int[] pixel = new int[1];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixel[0] = getValue(x, y);
                raster.setPixel(x, y, pixel);
            }
        }

        ///watch.print();
        return img;
    }
}
