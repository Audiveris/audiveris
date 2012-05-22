//----------------------------------------------------------------------------//
//                                                                            //
//                               P i c t u r e                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur and Brenton Partridge 2000-2012.              //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.picture;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.lag.PixelSource;

import omr.log.Logger;

import omr.score.common.PixelDimension;

import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.PixelLevelEvent;
import omr.selection.SelectionService;

import omr.util.JaiLoader;

import org.bushe.swing.event.EventSubscriber;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.BorderExtender;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedImageAdapter;

/**
 * Class {@code Picture} encapsulates an image, allowing modifications
 * and rendering.
 * Its current implementation is based on JAI (Java Advanced Imaging).
 *
 * <p> Operations allow : <ul>
 * <li> To <b>store</b> the current image to a file </li>
 * <li> To <b>render</b> the (original) image in a graphic context </li>
 * <li> To report current image <b>dimension</b> parameters </li>
 * <li> To <b>rotate</b> the image </li>
 * <li> To <b>read</b> or to <b>write</b> a pixel knowing its location in the
 * current image </li>
 * </ul> </p>
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 *
 * TODO: work on grayFactor
 */
public class Picture
        implements PixelSource,
                   EventSubscriber<LocationEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Picture.class);

    static {
        JaiLoader.ensureLoaded();
    }

    /** Identity transformation used for display */
    private static final AffineTransform identity = new AffineTransform();

    //~ Instance fields --------------------------------------------------------
    /** Dimension of current image */
    private PixelDimension dimension;

    /** Current image */
    private PlanarImage image;

    /** Service object where gray level of pixel is to be written to when so
     * asked for by calling the update method */
    private final SelectionService levelService;

    /** Remember if we have actually rotated the image */
    private boolean rotated = false;

    /** Cached dimension */
    ///private int dimensionWidth;
    /** The image (writable) raster */
    private WritableRaster raster;

    /** The factor to apply to raw pixel value to get gray level on 0..255 */
    private int grayFactor = 1;

    /**
     * The implicit (maximum) value for foreground pixels, as determined
     * by the picture itself, null if undetermined.
     */
    private Integer implicitForeground;

    /** The current maximum value for foreground pixels, null if not set */
    private Integer maxForeground;

    //~ Constructors -----------------------------------------------------------
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
        System.out.println();

        if (title != null) {
            System.out.println(title);
        }

        // Abscissae
        System.out.print("     ");

        for (int x = xMin; x <= xMax; x++) {
            System.out.printf("%4d", x);
        }

        System.out.println();
        System.out.print("    +");

        for (int x = xMin; x <= xMax; x++) {
            System.out.print(" ---");
        }

        System.out.println();

        // Pixels
        for (int y = yMin; y <= yMax; y++) {
            System.out.printf("%4d", y);
            System.out.print("|");

            for (int x = xMin; x <= xMax; x++) {
                int pix = getPixel(x, y);

                if (pix == 255) {
                    System.out.print("   .");
                } else {
                    System.out.printf("%4d", pix);
                }
            }

            System.out.println();
        }

        System.out.println();
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report (a copy of) the dimension in pixels of the current image.
     *
     * @return the image dimension
     */
    public PixelDimension getDimension ()
    {
        return new PixelDimension(dimension.width, dimension.height);
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

    //------------------//
    // getMaxForeground //
    //------------------//
    @Override
    public int getMaxForeground ()
    {
        if (maxForeground != null) {
            return maxForeground;
        } else {
            return implicitForeground;
        }
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

    //-----------//
    // isRotated //
    //-----------//
    /**
     * Predicate to report whether the picture has been rotated.
     *
     * @return true if rotated
     */
    public boolean isRotated ()
    {
        return rotated;
    }

    //--------//
    // update //
    //--------//
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
            logger.warning(getClass().getName() + " onEvent error", ex);
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

    //--------//
    // rotate //
    //--------//
    /**
     * Rotate the image according to the provided angle.
     * <p>Experience with JAI shows that we must use bytes rather than bits
     *
     * @param theta the desired rotation angle, in radians, positive for
     * clockwise, negative for counter-clockwise
     * @throws ImageFormatException
     */
    public void rotate (double theta)
            throws ImageFormatException
    {
        // Move bit -> byte if needed
        if (image.getColorModel().getPixelSize() == 1) {
            image = binaryToGray(image);
        }

        // Invert
        image = invert(image);
        ///printBounds();

        // Rotate
        image = JAI.create(
                "Rotate",
                new ParameterBlock().addSource(image) // Source image
                .add(0f) // x origin
                .add(0f) // y origin
                .add((float) theta) // angle
                .add(new InterpolationBilinear()), // Interpolation hint
                null);

        ///printBounds();

        // Translate the image so that we stay in non-negative coordinates
        if ((image.getMinX() != 0) || (image.getMinY() != 0)) {
            image = JAI.create(
                    "translate",
                    new ParameterBlock().addSource(image) // Source
                    .add(image.getMinX() * -1.0f) // dx
                    .add(image.getMinY() * -1.0f), // dy
                    null);

            ///printBounds();
        }

        //        // Crop the image to fit the size of the previous one
        //        image = JAI.create(
        //            "crop",
        //            new ParameterBlock().addSource(image) // The source image
        //            .add(0f) // x
        //            .add(0f) // y
        //            .add(dimension.width * 0.735f) // width
        //            .add(dimension.height * 0.825f), // height
        //            null);
        //        printBounds();

        // de-Invert
        image = invert(image);

        // Force immediate mode
        image.getTiles();

        // Update relevant parameters
        rotated = true;
        updateParams();

        logger.info("Image rotated {0} x {1}", new Object[]{getWidth(),
                                                            getHeight()});
    }

    //------------------//
    // setMaxForeground //
    //------------------//
    @Override
    public void setMaxForeground (int level)
    {
        this.maxForeground = level;
    }

    //----------//
    // setPixel //
    //----------//
    /**
     * Write a pixel at the provided location, in the currently writable
     * data buffer.
     *
     * @param pt  pixel coordinates
     * @param val pixel value
     */
    public final void setPixel (Point pt,
                                int val)
    {
        int[] pixel = new int[1];

        if (grayFactor == 1) {
            pixel[0] = val;
        } else {
            pixel[0] = (val - (grayFactor / 2)) / grayFactor;
        }

        raster.setPixel(pt.x, pt.y, pixel);
    }

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
        logger.fine("Discarding alpha band ...");

        PlanarImage pi = JAI.create("bandselect", image, new int[]{0, 1, 2});

        return RGBToGray(pi);
    }

    //-----------//
    // RGBToGray //
    //-----------//
    private static PlanarImage RGBToGray (PlanarImage image)
    {
        logger.info("Converting RGB image to gray ...");

        if (constants.useMaxChannelForColorToGray.isSet()) {
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

    //--------------//
    // binaryToGray //
    //--------------//
    private PlanarImage binaryToGray (PlanarImage image)
    {
        logger.info("Converting binary image to gray ...");

        // hint with border extender
        RenderingHints hint = new RenderingHints(
                JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_REFLECT));
        float subsample = (float) constants.binaryToGrayscaleSubsampling.
                getValue();

        if ((subsample <= 0) || (subsample > 1)) {
            throw new IllegalStateException(
                    "blackWhiteSubsampling must be > 0 and <= 1");
        }

        PlanarImage result;

        if (subsample < 1) {
            logger.info("Subsampling binary image");
            result = JAI.create(
                    "subsamplebinarytogray",
                    new ParameterBlock().addSource(image).add(subsample).add(
                    subsample),
                    hint);
        } else {
            logger.info("Buffering and converting binary image");

            ColorConvertOp grayOp = new ColorConvertOp(
                    ColorSpace.getInstance(ColorSpace.CS_GRAY),
                    null);
            BufferedImage gray = grayOp.filter(
                    image.getAsBufferedImage(),
                    null);
            result = new RenderedImageAdapter(gray);

            //If the result has an alpha component, remove it
            if (result.getColorModel().hasAlpha()) {
                result = JAI.create("bandselect", result, new int[]{0});
            }
        }

        implicitForeground = null;

        return result;
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
        int pixelSize = image.getColorModel().getPixelSize();

        if (pixelSize == 1) {
            ///image = binaryToGray(image); // Only if rotation is needed!
            implicitForeground = 0;
        }

        // Check nb of bands
        int numBands = image.getSampleModel().getNumBands();
        logger.fine("checkImageFormat. numBands={0}", numBands);

        if (numBands != 1) {
            if (numBands == 3) {
                image = RGBToGray(image);
            } else if (numBands == 4) {
                image = RGBAToGray(image);
            } else {
                throw new ImageFormatException(
                        "Unsupported sample model" + " numBands=" + numBands);
            }
        }
    }

    //-------------//
    // printBounds //
    //-------------//
    private void printBounds ()
    {
        logger.info("minX:{0} minY:{1} maxX:{2} maxY:{3}",
                    new Object[]{image.getMinX(), image.getMinY(),
                                 image.getMaxX(), image.getMaxY()});
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
        dimension = new PixelDimension(image.getWidth(), image.getHeight());
        raster = Raster.createWritableRaster(
                image.getData().getSampleModel(),
                image.getData().getDataBuffer(),
                null);
        logger.fine("raster={0}", raster);

        ///dataBuffer = raster.getDataBuffer();

        // Check pixel size
        ColorModel colorModel = image.getColorModel();
        int pixelSize = colorModel.getPixelSize();
        logger.fine("colorModel={0} pixelSize={1}", new Object[]{colorModel,
                                                                 pixelSize});

        if (pixelSize == 1) {
            grayFactor = 1;
        } else if (pixelSize <= 8) {
            grayFactor = (int) Math.rint(128 / Math.pow(2, pixelSize - 1));
        } else {
            throw new RuntimeException("Unsupported pixel size:" + pixelSize);
        }

        //        if (pixelSize != 8) {
        //            logger.warning(
        //                "The input image has a pixel size of " + pixelSize + " bits." +
        //                "\nConsider converting to a format with pixel color on 8 bits (1 byte)");
        //        }
        logger.fine("grayFactor={0}", grayFactor);
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean useMaxChannelForColorToGray = new Constant.Boolean(
                true,
                "Should we use max channel rather than standard luminance in "
                + "RGAtoGray transform");

        //
        Constant.Ratio binaryToGrayscaleSubsampling = new Constant.Ratio(
                1,
                "Subsampling ratio between 0 and 1, or 1 for no subsampling "
                + "(memory intensive)");
    }
}
