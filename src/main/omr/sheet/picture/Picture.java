//----------------------------------------------------------------------------//
//                                                                            //
//                               P i c t u r e                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur and Brenton Partridge 2000-2013.              //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.picture;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.run.PixelSource;

import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.PixelLevelEvent;
import omr.selection.SelectionService;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

/**
 * Class {@code Picture} encapsulates an image, allowing modifications
 * and rendering.
 * Its current implementation is based on JAI (Java Advanced Imaging).
 *
 * <p> Operations allow : <ul>
 * <li> To <b>render</b> the (original) image in a graphic context </li>
 * <li> To report current image <b>dimension</b> parameters</li>
 * <li> To <b>read</b> a pixel knowing its location in the current image </li>
 * </ul> </p>
 *
 * <p>TODO: Rather than the custom grayfactor trick, consider using the standard
 * normalized form of ColorModel.
 * <p>TODO: When an alpha channel is involved, perform the alpha multiplication
 * if the components are not yet premultiplied.
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 */
public class Picture
        implements PixelSource, EventSubscriber<LocationEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Picture.class);

    /** Identity transformation used for display */
    private static final AffineTransform identity = new AffineTransform();

    //~ Instance fields --------------------------------------------------------
    //
    /** Dimension of current image. */
    private Dimension dimension;

    /** Current image. */
    private PlanarImage image;

    /** Service object where gray level of pixel is to be written to
     * when so asked for by the onEvent() method. */
    private final SelectionService levelService;

    /** The image (read-only) raster. */
    private Raster raster;

    /** The factor to apply to raw pixel value to get gray level on 0..255 */
    private int grayFactor = 1;

    /**
     * The implicit (maximum) value for foreground pixels, as determined
     * by the picture itself, null if undetermined.
     */
    private Integer implicitForeground;

    //~ Constructors -----------------------------------------------------------
    //
    //---------//
    // Picture //
    //---------//
    /**
     * Build a picture instance from a given image.
     *
     * @param image        the provided image
     * @param levelService service where pixel events are to be written
     * @throws ImageFormatException
     */
    public Picture (RenderedImage image,
                    SelectionService levelService)
            throws ImageFormatException
    {
        this.levelService = levelService;
        setImage(image);
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // invert //
    //--------//
    public static PlanarImage invert (RenderedImage image)
    {
        return JAI.create(
                "Invert",
                new ParameterBlock().addSource(image).add(null),
                null);
    }

    //----------//
    // getPixel //
    //----------//
    /**
     * Report the pixel element read at location (x, y) in the picture.
     *
     * @param x abscissa value
     * @param y ordinate value
     * @return the pixel value
     */
    @Override
    public final int getPixel (int x,
                               int y)
    {
        int[] pixel = raster.getPixel(x, y, (int[]) null); // Allocates pixel!

        if (grayFactor == 1) {
            // Speed up the normal case
            return pixel[0];
        } else {
            return (grayFactor / 2) + (grayFactor * pixel[0]);
        }
    }

    //
    //-------//
    // close //
    //-------//
    /**
     * Release the resources linked to the picture image.
     */
    public void close ()
    {
        if (image != null) {
            image.dispose();
        }
    }

    //---------------//
    // dumpRectangle //
    //---------------//
    /**
     * Debugging routine, that prints a basic representation of a
     * rectangular portion of the picture.
     *
     * @param title an optional title for this image dump
     * @param xMin  x first coord
     * @param xMax  x last coord
     * @param yMin  y first coord
     * @param yMax  y last coord
     */
    public void dumpRectangle (String title,
                               int xMin,
                               int xMax,
                               int yMin,
                               int yMax)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%n"));

        if (title != null) {
            sb.append(String.format("%s%n", title));
        }

        // Abscissae
        sb.append("     ");

        for (int x = xMin; x <= xMax; x++) {
            sb.append(String.format("%4d", x));
        }

        sb.append(String.format("%n    +"));

        for (int x = xMin; x <= xMax; x++) {
            sb.append(" ---");
        }

        sb.append(String.format("%n"));

        // Pixels
        for (int y = yMin; y <= yMax; y++) {
            sb.append(String.format("%4d", y));
            sb.append("|");

            for (int x = xMin; x <= xMax; x++) {
                int pix = getPixel(x, y);

                if (pix == 255) {
                    sb.append("   .");
                } else {
                    sb.append(String.format("%4d", pix));
                }
            }

            sb.append(String.format("%n"));
        }

        sb.append(String.format("%n"));

        logger.info(sb.toString());
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report (a copy of) the dimension in pixels of the current image.
     *
     * @return the image dimension
     */
    public Dimension getDimension ()
    {
        return new Dimension(dimension.width, dimension.height);
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the picture height in pixels.
     *
     * @return the height value
     */
    @Override
    public int getHeight ()
    {
        return dimension.height;
    }

    //----------//
    // getImage //
    //----------//
    /**
     * Report the underlying image.
     *
     * @return the image
     */
    public RenderedImage getImage ()
    {
        return image;
    }

    //-----------------------//
    // getImplicitForeground //
    //-----------------------//
    public Integer getImplicitForeground ()
    {
        return implicitForeground;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the name for this Observer.
     *
     * @return Observer name
     */
    public String getName ()
    {
        return "Picture";
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the current width of the picture image.
     * Note that it may have been modified by a rotation.
     *
     * @return the current width value, in pixels.
     */
    @Override
    public int getWidth ()
    {
        return dimension.width;
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when sheet location has been modified.
     * Based on sheet location, we forward the pixel gray level to whoever is
     * interested in it.
     *
     * @param event the (sheet) location event
     */
    @Override
    public void onEvent (LocationEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            Integer level = null;

            // Compute and forward pixel gray level
            Rectangle rect = event.getData();

            if (rect != null) {
                Point pt = rect.getLocation();

                // Check that we are not pointing outside the image
                if ((pt.x >= 0)
                    && (pt.x < getWidth())
                    && (pt.y >= 0)
                    && (pt.y < getHeight())) {
                    level = Integer.valueOf(getPixel(pt.x, pt.y));
                }
            }

            levelService.publish(
                    new PixelLevelEvent(this, event.hint, event.movement, level));
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Paint the picture image in the provided graphic context.
     *
     * @param g the Graphics context
     */
    public void render (Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        g2.drawRenderedImage(image, identity);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return getName();
    }

    //------------//
    // RGBAToGray //
    //------------//
    private static PlanarImage RGBAToGray (PlanarImage image)
    {
        logger.info("Discarding alpha band ...");

        PlanarImage pi = JAI.create("bandselect", image, new int[]{0, 1, 2});

        return RGBToGray(pi);
    }

    //-----------//
    // RGBToGray //
    //-----------//
    private static PlanarImage RGBToGray (PlanarImage image)
    {
        logger.info("Converting RGB image to gray ...");

        if (constants.useMaxChannelInColorToGray.isSet()) {
            // We use the max value among the RGB channels
            int width = image.getWidth();
            int height = image.getHeight();
            BufferedImage im = new BufferedImage(
                    width,
                    height,
                    BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = im.getRaster();
            Raster source = image.getData();
            int[] levels = new int[3];
            int maxLevel;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    source.getPixel(x, y, levels);
                    maxLevel = 0;

                    for (int level : levels) {
                        if (maxLevel < level) {
                            maxLevel = level;
                        }
                    }

                    raster.setSample(x, y, 0, maxLevel);
                }
            }

            return PlanarImage.wrapRenderedImage(im);
        } else {
            // We use luminance value based on standard RGB combination
            double[][] matrix = {
                {0.114d, 0.587d, 0.299d, 0.0d}
            };

            return JAI.create(
                    "bandcombine",
                    new ParameterBlock().addSource(image).add(matrix),
                    null);
        }
    }

    //------------//
    // checkImage //
    //------------//
    private void checkImage ()
            throws ImageFormatException
    {
        // Check that the whole image has been loaded
        if ((image.getWidth() == -1) || (image.getHeight() == -1)) {
            throw new RuntimeException("Unusable image for Picture");
        } else {
            // Check & cache all parameters
            updateParams();
        }
    }

    //------------------//
    // checkImageFormat //
    //------------------//
    /**
     * Check if the image format (and especially its color model) is
     * properly handled by Audiveris.
     *
     * @throws ImageFormatException is the format is not supported
     */
    private void checkImageFormat ()
            throws ImageFormatException
    {
        ColorModel colorModel = image.getColorModel();
        int pixelSize = colorModel.getPixelSize();
        boolean hasAlpha = colorModel.hasAlpha();
        logger.debug("{}", colorModel);

        if (pixelSize == 1) {
            ///image = binaryToGray(image); // Only if rotation is needed!
            implicitForeground = 0;
        }

        // Check nb of bands
        SampleModel sampleModel = image.getSampleModel();
        int numBands = sampleModel.getNumBands();
        logger.debug("numBands={}", numBands);

        if (numBands == 1) {
            // Pixel gray value. Nothing to do
        } else if ((numBands == 2) && hasAlpha) {
            // Pixel + alpha
            // Discard alpha (TODO: check if premultiplied!!!)
            image = JAI.create("bandselect", image, new int[]{});
        } else if ((numBands == 3) && !hasAlpha) {
            // RGB
            image = RGBToGray(image);
        } else if ((numBands == 4) && hasAlpha) {
            // RGB + alpha
            image = RGBAToGray(image);
        } else {
            throw new ImageFormatException(
                    "Unsupported sample model numBands=" + numBands);
        }
    }

    //-------------//
    // printBounds //
    //-------------//
    private void printBounds ()
    {
        logger.info(
                "minX:{} minY:{} maxX:{} maxY:{}",
                image.getMinX(),
                image.getMinY(),
                image.getMaxX(),
                image.getMaxY());
    }

    //----------//
    // setImage //
    //----------//
    private void setImage (RenderedImage renderedImage)
            throws ImageFormatException
    {
        image = PlanarImage.wrapRenderedImage(renderedImage);

        checkImage();
    }

    //--------------//
    // updateParams //
    //--------------//
    private void updateParams ()
            throws ImageFormatException
    {
        checkImageFormat();

        // Cache dimensions
        dimension = new Dimension(image.getWidth(), image.getHeight());
        raster = Raster.createRaster(
                image.getData().getSampleModel(),
                image.getData().getDataBuffer(),
                null);
        logger.debug("raster={}", raster);

        // Check pixel size and compute grayFactor accordingly
        ColorModel colorModel = image.getColorModel();
        int pixelSize = colorModel.getPixelSize();
        logger.debug("colorModel={} pixelSize={}", colorModel, pixelSize);

        if (pixelSize == 1) {
            grayFactor = 1;
        } else if (pixelSize <= 8) {
            grayFactor = (int) Math.rint(128 / Math.pow(2, pixelSize - 1));
        } else if (pixelSize <= 16) {
            grayFactor = (int) Math.rint(32768 / Math.pow(2, pixelSize - 1));
        } else {
            throw new RuntimeException("Unsupported pixel size: " + pixelSize);
        }

        logger.debug("grayFactor={}", grayFactor);
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean useMaxChannelInColorToGray = new Constant.Boolean(
                true,
                "Should we use max channel rather than standard luminance?");

    }
}
