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
package omr.sheet;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.image.BufferedSource;
import omr.image.FilterDescriptor;
import omr.image.GaussianGrayFilter;
import omr.image.ImageFormatException;
import omr.image.ImageUtil;
import omr.image.MedianGrayFilter;
import omr.image.PixelBuffer;
import omr.image.PixelFilter;
import omr.image.PixelSource;

import omr.lag.Lags;

import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.PixelLevelEvent;
import omr.selection.SelectionService;

import omr.util.StopWatch;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.util.EnumMap;
import java.util.Map;

import javax.media.jai.JAI;

/**
 * Class {@code Picture} starts from the original BufferedImage to
 * provide all {@link PixelSource} instances derived from it.
 * <p>
 * The {@code Picture} constructor takes a provided original image, whatever
 * its format and color model, and converts it if necessary to come up with a
 * usable gray-level PixelSource: the INITIAL source.
 * <p>
 * Besides the INITIAL source, this class handles a collection of sources, all
 * of the same dimension, with the ability to retrieve them on demand or dispose
 * them, via {@link #getSource} and {@link #disposeSource} methods.
 * <p>
 * Any instance of this class is registered on the related Sheet location
 * service, so that each time a location event is received, the corresponding
 * pixel gray value of the INITIAL sources is published.
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
     * The set of handled sources, to be extended as needed.
     */
    public static enum SourceKey
    {
        //~ Enumeration constant initializers ----------------------------------

        /** The initial gray-level source. */
        INITIAL,
        /** The binarized black &
         * white source. */
        BINARY,
        /** The Gaussian-filtered source. */
        GAUSSIAN,
        /** The Median-filtered source. */
        MEDIAN,
        /** The source with staff
         * lines removed. */
        STAFF_LINE_FREE;

    }

    //~ Instance fields --------------------------------------------------------
    //
    /** Related sheet. */
    private final Sheet sheet;

    /** Image dimension. */
    private final Dimension dimension;

    /**
     * Service object where gray level of pixel is to be written to
     * when so asked for by the onEvent() method.
     */
    private final SelectionService levelService;

    /** Map of all handled sources. */
    private final Map<SourceKey, PixelSource> sources = new EnumMap<SourceKey, PixelSource>(
            SourceKey.class);

    /** The initial (gray-level) image. */
    private BufferedImage initialImage;

    /** The initial (gray-level) source. (for onEvent only) */
    private PixelSource initialSource;

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
        ImageUtil.printInfo(image, "Original image");
        image = checkImage(image);
        dimension = new Dimension(image.getWidth(), image.getHeight());

        // Remember the initial image
        initialImage = image;
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // disposeSource //
    //---------------//
    public void disposeSource (SourceKey key)
    {
        // Nullify cached data, if needed
        if (key == SourceKey.INITIAL) {
            initialImage = null;
            initialSource = null;
            logger.debug("{} source disposed.", key);

            return;
        }

        PixelSource src = sources.get(key);

        if (src != null) {
            synchronized (sources) {
                sources.put(key, null);
                logger.debug("{} source disposed.", key);
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
    public void dumpRectangle (SourceKey key,
                               String title,
                               int xMin,
                               int xMax,
                               int yMin,
                               int yMax)
    {
        PixelSource source = getSource(key);
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
                int pix = source.getValue(x, y);

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

    //-----------------//
    // getInitialImage //
    //-----------------//
    /** Report the initial (BufferedImage) image.
     *
     * @return the initial image
     */
    public BufferedImage getInitialImage ()
    {
        return initialImage;
    }

    //------------------//
    // getInitialSource //
    //------------------//
    /** Report the initial source.
     *
     * @return the initial source
     */
    public PixelSource getInitialSource ()
    {
        return new BufferedSource(initialImage);
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
    // getSource //
    //----------//
    /**
     * Report the desired image, creating it if necessary.
     *
     * @param key the key of desired image
     * @return the image ready to use
     */
    public PixelSource getSource (SourceKey key)
    {
        // Initial source is special, because it's a BufferedImage which needs
        // a dedicated wrapper for each user.
        if (key == SourceKey.INITIAL) {
            return getInitialSource();
        }

        PixelSource src = sources.get(key);

        if (src == null) {
            synchronized (sources) {
                src = sources.get(key);

                if (src == null) {
                    switch (key) {
                    case BINARY:
                        src = binarized(getInitialSource());

                        break;

                    case GAUSSIAN:
                        src = gaussianFiltered(getSource(SourceKey.MEDIAN));

                        break;

                    case MEDIAN:
                        src = medianFiltered(getSource(SourceKey.BINARY));

                        break;

                    case STAFF_LINE_FREE:
                        src = Lags.buildBuffer(
                                dimension,
                                sheet.getLag(Lags.HLAG),
                                sheet.getLag(Lags.VLAG));

                        break;
                    }

                    sources.put(key, src);
                }
            }
        }

        return src;
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
        if (initialImage == null) {
            return;
        }

        if (initialSource == null) {
            initialSource = getInitialSource();
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
                    level = Integer.valueOf(initialSource.getValue(pt.x, pt.y));
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

    //-----------//
    // binarized //
    //-----------//
    private PixelBuffer binarized (PixelSource src)
    {
        FilterDescriptor desc = sheet.getPage()
                .getFilterParam()
                .getTarget();
        logger.info("{}{} {}", sheet.getLogPrefix(), "Binarization", desc);
        sheet.getPage()
                .getFilterParam()
                .setActual(desc);

        PixelFilter filter = desc.getFilter(src);

        return new PixelBuffer(filter);
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
            // Discard alpha
            return JAI.create("bandselect", img, new int[]{0})
                    .getAsBufferedImage();
        } else if ((numBands == 3) && !hasAlpha) {
            // RGB
            return ImageUtil.maxRgbToGray(img);
        } else if ((numBands == 4) && hasAlpha) {
            // RGB + alpha
            return ImageUtil.maxRgbaToGray(img);
        } else {
            throw new ImageFormatException(
                    "Unsupported sample model numBands=" + numBands + " hasAlpha="
                    + hasAlpha);
        }
    }

    //------------------//
    // gaussianFiltered //
    //------------------//
    public PixelBuffer gaussianFiltered (PixelSource src)
    {
        StopWatch watch = new StopWatch("Gaussian");

        try {
            watch.start("Filter " + src.getWidth() + "x" + src.getHeight());

            final int radius = constants.gaussianRadius.getValue();
            logger.info(
                    "{}Image blurred with gaussian kernel radius: {}",
                    sheet.getLogPrefix(),
                    radius);

            GaussianGrayFilter gaussianFilter = new GaussianGrayFilter(radius);

            return gaussianFilter.filter(src);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //----------------//
    // medianFiltered //
    //----------------//
    private PixelBuffer medianFiltered (PixelSource src)
    {
        StopWatch watch = new StopWatch("Median");

        try {
            watch.start("Filter " + src.getWidth() + "x" + src.getHeight());

            final int radius = constants.medianRadius.getValue();
            logger.info(
                    "{}Image filtered with median kernel radius: {}",
                    sheet.getLogPrefix(),
                    radius);

            MedianGrayFilter medianFilter = new MedianGrayFilter(radius);

            return medianFilter.filter(src);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
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
                2,
                "Radius of Gaussian filtering kernel (1 for 3x3, 2 for 5x5, etc)");

        Constant.Integer medianRadius = new Constant.Integer(
                "pixels",
                1,
                "Radius of Median filtering kernel (1 for 3x3, 2 for 5x5)");

    }
}
