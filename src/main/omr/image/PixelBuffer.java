//----------------------------------------------------------------------------//
//                                                                            //
//                           P i x e l B u f f e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import static omr.image.PixelSource.BACKGROUND;

import omr.math.TableUtil;

import omr.util.StopWatch;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

/**
 * Class {@code PixelBuffer} handles a plain rectangular buffer of
 * bytes.
 * It is an efficient {@link PixelFilter} both for writing and for reading.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class PixelBuffer
        implements PixelFilter, PixelSink
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            PixelBuffer.class);

    //~ Instance fields --------------------------------------------------------
    /** Width of the table */
    private final int width;

    /** Height of the table */
    private final int height;

    /** Underlying buffer */
    private final byte[] buffer;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // PixelBuffer //
    //-------------//
    /**
     * Creates a new PixelBuffer object.
     *
     * @param dimension the buffer dimension
     */
    public PixelBuffer (Dimension dimension)
    {
        width = dimension.width;
        height = dimension.height;

        buffer = new byte[width * height];

        // Initialize the whole buffer with background color value
        Arrays.fill(buffer, (byte) BACKGROUND);
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

        final int numBands = image.getSampleModel()
                .getNumBands();
        final int[] pixel = new int[numBands];
        final Raster raster = image.getRaster();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                raster.getPixel(x, y, pixel);
                // We use just the first band
                setPixel(x, y, pixel[0]);
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
                    setPixel(x, y, 0);
                }
            }
        }

        ///watch.print();
    }

    //-------------//
    // PixelBuffer //
    //-------------//
    /**
     * Clone a PixelBuffer.
     *
     * @param buf the buffer to clone
     */
    public PixelBuffer (PixelBuffer buf)
    {
        this(new Dimension(buf.getWidth(), buf.getHeight()));

        final StopWatch watch = new StopWatch("PixelBuffer");
        watch.start("copy");

        System.arraycopy(buf.buffer, 0, buffer, 0, buffer.length);

        ///watch.print();
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // dump //
    //------//
    public void dump (String title)
    {
        TableUtil.dump(title, this);
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

    //-----------//
    // getHeight //
    //-----------//
    @Override
    public int getHeight ()
    {
        return height;
    }

    //----------//
    // getPixel //
    //----------//
    @Override
    public int getPixel (int x,
                         int y)
    {
        return buffer[(y * width) + x] & 0xff;
    }

    //-----------//
    // getPixels //
    //-----------//
    public byte[] getPixels ()
    {
        return buffer;
    }

    //----------//
    // getWidth //
    //----------//
    @Override
    public int getWidth ()
    {
        return width;
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
                int val = that.getPixel(x, y);

                if (val != BACKGROUND) {
                    this.setPixel(x + origin.x, y + origin.y, val);
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
        return getPixel(x, y) < 225;
    }

    //----------//
    // setPixel //
    //----------//
    @Override
    public final void setPixel (int x,
                                int y,
                                int val)
    {
        buffer[(y * width) + x] = (byte) val;
    }

    //-----------------//
    // toBufferedImage //
    //-----------------//
    public BufferedImage toBufferedImage ()
    {
        StopWatch watch = new StopWatch("PixelBuffer");
        watch.start("toImage");

        final BufferedImage img = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_BYTE_GRAY);
        final WritableRaster raster = img.getRaster();
        final int[] pixel = new int[1];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixel[0] = getPixel(x, y);
                raster.setPixel(x, y, pixel);
            }
        }

        ///watch.print();
        return img;
    }
}
