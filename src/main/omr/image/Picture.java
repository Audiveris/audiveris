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
package omr.image;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.PixelLevelEvent;
import omr.selection.SelectionService;

import omr.sheet.Sheet;

import omr.util.StopWatch;

import com.jhlabs.image.GaussianFilter;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.util.EnumMap;
import java.util.Map;

import javax.media.jai.JAI;

/**
 * Class {@code Picture} handles the sheet initial gray-level image,
 * as well as the images derived from it (such as by filtering).
 * <p>
 * The {@code Picture} constructor takes a provided original image, whatever its
 * format and color model, and converts it if necessary to come up with a usable
 * gray-level image (of type TYPE_BYTE_GRAY): the INITIAL BufferedImage.
 * <p>
 * Besides the INITIAL image, this class handles a collection of images, all of
 * the same dimension, with the ability to retrieve them on demand or dispose
 * them, via {@link #getImage} and {@link #disposeImage} methods.
 * <p>
 * Any instance of this class is registered on the related Sheet location
 * service, so that each time a location event is received, the corresponding
 * pixel gray value of the INITIAL image is published.
 *
 * <p>
 * TODO: When an alpha channel is involved, perform the alpha multiplication
 * if the components are not yet premultiplied.
 *
 * <h4>Overview of transforms:<br/>
 * <img src="doc-files/transforms.jpg"/>
 * </h4>
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 */
public class Picture
        implements EventSubscriber<LocationEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Picture.class);

    //~ Enumerations -----------------------------------------------------------
    /**
     * Set of known images, to be extended as needed.
     */
    public static enum ImageKey
    {
        //~ Enumeration constant initializers ----------------------------------

        /** The initial gray-level image. */
        INITIAL,
        /** The Gaussian-filtered image. */
        GAUSSIAN,
        /** The Median-filtered image. */
        MEDIAN;

    }

    /**
     * Set of known buffers, to be extended as needed.
     */
    public static enum BufferKey
    {
        //~ Enumeration constant initializers ----------------------------------

        /** The binarized image. */
        BINARY;

    }

    //~ Instance fields --------------------------------------------------------
    //
    /** Related sheet. */
    private final Sheet sheet;

    /** Image dimension. */
    private Dimension dimension;

    /**
     * Service object where gray level of pixel is to be written to
     * when so asked for by the onEvent() method.
     */
    private final SelectionService levelService;

    /** Map of all handled images. */
    private final Map<ImageKey, BufferedImage> images = new EnumMap<ImageKey, BufferedImage>(
            ImageKey.class);

    /** Map of all handled buffers. */
    private final Map<BufferKey, PixelBuffer> buffers = new EnumMap<BufferKey, PixelBuffer>(
            BufferKey.class);

    /** A wrapping around the initial (default) image. */
    private BufferedSource initialSource;

    //~ Constructors -----------------------------------------------------------
    //
    //---------//
    // Picture //
    //---------//
    /**
     * Build a picture instance from a given original image.
     *
     * @param sheet        the related sheet
     * @param image        the provided original image
     * @param levelService service where pixel events are to be written
     * @throws ImageFormatException
     */
    public Picture (Sheet sheet,
                    BufferedImage image,
                    SelectionService levelService)
            throws ImageFormatException
    {
        this.sheet = sheet;
        this.levelService = levelService;

        // Make sure format, colors, etc are OK for us
        printInfo(image, "Original image");
        image = checkImage(image);
        printInfo(image, "Initial  image");
        images.put(ImageKey.INITIAL, image);
        dimension = new Dimension(image.getWidth(), image.getHeight());

        //        // Cache results
        //        this.image = image;
        //        raster = Raster.createRaster(
        //                image.getData().getSampleModel(),
        //                image.getData().getDataBuffer(),
        //                null);
        // Wrap the initial image
        initialSource = new BufferedSource(image);
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // printInfo //
    //-----------//
    /**
     * Convenient method to print some characteristics of the provided
     * image.
     *
     * @param img   the image to query
     * @param title a title to be printed
     */
    public static void printInfo (BufferedImage img,
                                  String title)
    {
        int type = img.getType();
        ColorModel colorModel = img.getColorModel();
        logger.info(
                "{} type: {}={} {}",
                (title != null) ? title : "",
                type,
                typeOf(type),
                colorModel);
    }

    //--------------//
    // disposeImage //
    //--------------//
    public void disposeImage (ImageKey key)
    {
        BufferedImage img = images.get(key);

        if (img != null) {
            synchronized (images) {
                images.put(key, null);

                // Nullify cached data, if needed
                if (key == ImageKey.INITIAL) {
                    initialSource = null;
                }

                logger.info("{} image disposed.", key);
            }
        }
    }

    //---------------//
    // disposeBuffer //
    //---------------//
    public void disposeImage (BufferKey key)
    {
        PixelBuffer buf = buffers.get(key);

        if (buf != null) {
            synchronized (buffers) {
                buffers.put(key, null);
                logger.info("{} buffer disposed.", key);
            }
        }
    }

    //---------------//
    // dumpRectangle //
    //---------------//
    /**
     * Debugging routine, that prints a basic representation of a
     * rectangular portion of the selected image.
     *
     * @param key   the selected image key
     * @param title an optional title for this image dump
     * @param xMin  x first abscissa
     * @param xMax  x last abscissa
     * @param yMin  y first ordinate
     * @param yMax  y last ordinate
     */
    public void dumpRectangle (ImageKey key,
                               String title,
                               int xMin,
                               int xMax,
                               int yMin,
                               int yMax)
    {
        PixelSource source = new BufferedSource(getImage(key));
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
                int pix = source.getPixel(x, y);

                if (pix == 255) { // White background
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

    //-----------//
    // getBuffer //
    //-----------//
    /**
     * Report the desired buffer, creating it if necessary.
     *
     * @param key the key of desired buffer
     * @return the buffer ready to use
     */
    public PixelBuffer getBuffer (BufferKey key)
    {
        PixelBuffer buf = buffers.get(key);

        if (buf == null) {
            synchronized (buffers) {
                buf = buffers.get(key);

                if (buf == null) {
                    switch (key) {
                    case BINARY:
                        buf = binarized(images.get(ImageKey.INITIAL));
                        buffers.put(key, buf);

                        break;
                    }
                }
            }
        }

        return buf;
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
    public int getHeight ()
    {
        return dimension.height;
    }

    //----------//
    // getImage //
    //----------//
    /**
     * Report the desired image, creating it if necessary.
     *
     * @param key the key of desired image
     * @return the image ready to use
     */
    public BufferedImage getImage (ImageKey key)
    {
        BufferedImage img = images.get(key);

        if (img == null) {
            synchronized (images) {
                img = images.get(key);

                if (img == null) {
                    switch (key) {
                    case GAUSSIAN:
                        img = gaussianFiltered(images.get(ImageKey.INITIAL));
                        images.put(key, img);

                        break;

                    case MEDIAN:
                        img = medianFiltered(images.get(ImageKey.INITIAL));
                        images.put(key, img);

                        break;
                    }
                }
            }
        }

        return img;
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
    public int getWidth ()
    {
        return dimension.width;
    }

    //--------//
    // invert //
    //--------//
    /**
     * Convenient method on invert an image.
     *
     * @param image the image to process
     * @return the invert of provided image
     */
    public static BufferedImage invert (BufferedImage image)
    {
        return JAI.create(
                "Invert",
                new ParameterBlock().addSource(image).add(null),
                null)
                .getAsBufferedImage();
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when sheet location has been modified.
     * Based on sheet location, we forward the INITIAL pixel gray level to
     * whoever is interested in it.
     *
     * @param event the (sheet) location event
     */
    @Override
    public void onEvent (LocationEvent event)
    {
        if (initialSource == null) {
            return;
        }

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
                    level = Integer.valueOf(initialSource.getPixel(pt.x, pt.y));
                }
            }

            levelService.publish(
                    new PixelLevelEvent(this, event.hint, event.movement, level));
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
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
    private static BufferedImage RGBAToGray (BufferedImage image)
    {
        logger.info("Discarding alpha band ...");

        return RGBToGray(
                JAI.create("bandselect", image, new int[]{0, 1, 2}).getAsBufferedImage());
    }

    //-----------//
    // RGBToGray //
    //-----------//
    private static BufferedImage RGBToGray (BufferedImage image)
    {
        logger.info("Converting RGB image to gray ...");

        if (constants.useMaxChannelInColorToGray.isSet()) {
            // We use the max value among the RGB channels
            int width = image.getWidth();
            int height = image.getHeight();
            BufferedImage img = new BufferedImage(
                    width,
                    height,
                    BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = img.getRaster();
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

            return img;
        } else {
            // We use luminance value based on standard RGB combination
            double[][] matrix = {
                {0.114d, 0.587d, 0.299d, 0.0d}
            };

            return JAI.create(
                    "bandcombine",
                    new ParameterBlock().addSource(image).add(matrix),
                    null)
                    .getAsBufferedImage();
        }
    }

    //------------//
    // checkImage //
    //------------//
    private BufferedImage checkImage (BufferedImage img)
            throws ImageFormatException
    {
        // Check that the whole image has been loaded
        if ((img.getWidth() == -1) || (img.getHeight() == -1)) {
            throw new RuntimeException("Unusable image for Picture");
        }

        // Check image format
        img = checkImageFormat(img);

        // Check pixel size and compute grayFactor accordingly
        ColorModel colorModel = img.getColorModel();
        int pixelSize = colorModel.getPixelSize();
        logger.debug("colorModel={} pixelSize={}", colorModel, pixelSize);

        //        if (pixelSize == 1) {
        //            grayFactor = 1;
        //        } else if (pixelSize <= 8) {
        //            grayFactor = (int) Math.rint(128 / Math.pow(2, pixelSize - 1));
        //        } else if (pixelSize <= 16) {
        //            grayFactor = (int) Math.rint(32768 / Math.pow(2, pixelSize - 1));
        //        } else {
        //            throw new RuntimeException("Unsupported pixel size: " + pixelSize);
        //        }
        //
        //        logger.debug("grayFactor={}", grayFactor);
        return img;
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
    private BufferedImage checkImageFormat (BufferedImage img)
            throws ImageFormatException
    {
        ColorModel colorModel = img.getColorModel();
        boolean hasAlpha = colorModel.hasAlpha();
        logger.debug("{}", colorModel);

        // Check nb of bands
        SampleModel sampleModel = img.getSampleModel();
        int numBands = sampleModel.getNumBands();
        logger.debug("numBands={}", numBands);

        if (numBands == 1) {
            // Pixel gray value. Nothing to do
            return img;
        } else if ((numBands == 2) && hasAlpha) {
            // Pixel + alpha
            // Discard alpha (TODO: check if premultiplied!!!)
            return JAI.create("bandselect", img, new int[]{})
                    .getAsBufferedImage();
        } else if ((numBands == 3) && !hasAlpha) {
            // RGB
            return RGBToGray(img);
        } else if ((numBands == 4) && hasAlpha) {
            // RGB + alpha
            return RGBAToGray(img);
        } else {
            throw new ImageFormatException(
                    "Unsupported sample model numBands=" + numBands);
        }
    }

    //------------------//
    // gaussianFiltered //
    //------------------//
    private BufferedImage gaussianFiltered (BufferedImage img)
    {
        StopWatch watch = new StopWatch("Gaussian");

        try {
            watch.start("Filter " + img.getWidth() + "x" + img.getHeight());

            final int radius = constants.gaussianRadius.getValue();
            logger.info(
                    "{}Image blurred with gaussian kernel radius: {}",
                    sheet.getLogPrefix(),
                    radius);

            GaussianFilter gaussianFilter = new GaussianFilter(radius);

            return gaussianFilter.filter(img, null);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //----------------//
    // medianFiltered //
    //----------------//
    private BufferedImage medianFiltered (BufferedImage img)
    {
        StopWatch watch = new StopWatch("Median");

        try {
            watch.start("Filter " + img.getWidth() + "x" + img.getHeight());

            final int radius = constants.medianRadius.getValue();
            logger.info(
                    "{}Image filtered with median kernel radius: {}",
                    sheet.getLogPrefix(),
                    radius);

            MedianGrayFilter medianFilter = new MedianGrayFilter(radius);

            return medianFilter.filter(img);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //--------//
    // typeOf //
    //--------//
    private static String typeOf (int type)
    {
        switch (type) {
        case BufferedImage.TYPE_CUSTOM:
            return "TYPE_CUSTOM";

        case BufferedImage.TYPE_INT_RGB:
            return "TYPE_INT_RGB";

        case BufferedImage.TYPE_INT_ARGB:
            return "TYPE_INT_ARGB";

        case BufferedImage.TYPE_INT_ARGB_PRE:
            return "TYPE_INT_ARGB_PRE";

        case BufferedImage.TYPE_INT_BGR:
            return "TYPE_INT_BGR";

        case BufferedImage.TYPE_3BYTE_BGR:
            return "TYPE_3BYTE_BGR";

        case BufferedImage.TYPE_4BYTE_ABGR:
            return "TYPE_4BYTE_ABGR";

        case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            return "TYPE_4BYTE_ABGR_PRE";

        case BufferedImage.TYPE_USHORT_565_RGB:
            return "TYPE_USHORT_565_RGB";

        case BufferedImage.TYPE_USHORT_555_RGB:
            return "TYPE_USHORT_555_RGB";

        case BufferedImage.TYPE_BYTE_GRAY:
            return "TYPE_BYTE_GRAY";

        case BufferedImage.TYPE_USHORT_GRAY:
            return "TYPE_USHORT_GRAY";

        case BufferedImage.TYPE_BYTE_BINARY:
            return "TYPE_BYTE_BINARY";

        case BufferedImage.TYPE_BYTE_INDEXED:
            return "TYPE_BYTE_INDEXED";

        default:
            return "?";
        }
    }

    //-----------//
    // binarized //
    //-----------//
    private PixelBuffer binarized (BufferedImage img)
    {
        FilterDescriptor desc = sheet.getPage()
                .getFilterParam()
                .getTarget();
        logger.info("{}{} {}", sheet.getLogPrefix(), "Binarization", desc);
        sheet.getPage()
                .getFilterParam()
                .setActual(desc);

        PixelFilter filter = desc.getFilter(new BufferedSource(img));

        return new PixelBuffer(filter);
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        Constant.Boolean useMaxChannelInColorToGray = new Constant.Boolean(
                true,
                "Should we use max channel rather than standard luminance?");

        Constant.Boolean filterImage = new Constant.Boolean(
                true,
                "Should we slightly filter the source image?");

        Constant.Integer gaussianRadius = new Constant.Integer(
                "pixels",
                1,
                "Radius of Gaussian filtering kernel (1 for 3x3, 2 for 5x5)");

        Constant.Integer medianRadius = new Constant.Integer(
                "pixels",
                1,
                "Radius of Median filtering kernel (1 for 3x3, 2 for 5x5)");

    }
}
