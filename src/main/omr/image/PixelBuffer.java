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
 * Each byte is signed so value is either 0 (foreground) or -1 (background).
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class PixelBuffer
        implements PixelFilter
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
    private byte[] buffer;

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
     * Creates a PixelBuffer from a BufferedImage.
     *
     * @param image the provided BufferedImage
     */
    public PixelBuffer (BufferedImage image)
    {
        this(new Dimension(image.getWidth(), image.getHeight()));

        StopWatch watch = new StopWatch("PixelBuffer");
        watch.start("toBuffer");

        int[] pixel = new int[3];
        Raster raster = image.getRaster();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.getPixel(x, y, pixel);
                setPixel(x, y, (byte) pixel[0]);
            }
        }

        watch.print();
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
        return buffer[(y * width) + x];
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
     * That buffer is assumed to be within this buffer bounds.
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

                if (val < 0) {
                    val += 256;
                }

                if (val != BACKGROUND) {
                    this.setPixel(x + origin.x, y + origin.y, (byte) val);
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
        int val = getPixel(x, y);

        if (val < 0) {
            val += 256;
        }

        return val < 150;
    }

    //----------//
    // setPixel //
    //----------//
    public void setPixel (int x,
                          int y,
                          byte val)
    {
        buffer[(y * width) + x] = val;
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
        int val;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                val = getPixel(x, y);

                if (val < 0) {
                    val += 256;
                }

                pixel[0] = val;

                raster.setPixel(x, y, pixel);
            }
        }

        ///watch.print();
        return img;
    }
}
