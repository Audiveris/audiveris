//----------------------------------------------------------------------------//
//                                                                            //
//                               P i c t u r e                                //
//                                                                            //
//  Copyright (C) Herve Bitteur and Brenton Partridge 2000-2007.              //
//  All rights reserved.                                                      //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.lag.PixelSource;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionObserver;
import omr.step.LogStepMonitorHandler;

import omr.util.Implement;
import omr.util.JaiLoader;
import omr.util.Logger;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.*;
import javax.imageio.event.*;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.MosaicDescriptor;

/**
 * Class <code>Picture</code> encapsulates an image, allowing modifications and
 * rendering. Its current implementation is based on JAI (Java Advanced
 * Imaging). JAI is not used outside of this class.
 *
 * <p> Operations allow : <ul>
 *
 * <li> To <b>load</b> the original image from a file </li>
 *
 * <li> To <b>store</b> the current image to a file </li>
 *
 * <li> To <b>render</b> the (original) image in a graphic context </li>
 *
 * <li> To report current image <b>dimension</b> parameters </li>
 *
 * <li> To <b>rotate</b> the image </li>
 *
 * <li> To <b>read</b> or to <b>write</b> a pixel knowing its location in the
 * current image </li>
 *
 * </ul> </p>
 *
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>PIXEL Location (if LOCATION_INIT or LOCATION_ADD)
 * </ul>
 *
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>LEVEL
 * </ul>
 * </dl>
 *
 * @author Herv&eacute; Bitteur and Brenton Partridge
 * @version $Id$
 */
public class Picture
    implements PixelSource, SelectionObserver
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Picture.class);

    /**
     * Constant color of the picture background (generally white).
     */
    public static final int BACKGROUND = 255;

    /**
     * Constant color of the picture foreground (generally black), which means
     * that any pixel whose level is higher than this value will be considered
     * as background. [Should be an application constant? TBD]
     */
    public static final int FOREGROUND = 227;

    static {
        long startTime = System.currentTimeMillis();

        if (logger.isFineEnabled()) {
            logger.fine("Awaiting JAI loading");
        }

        JaiLoader.ensureLoaded();

        if (logger.isFineEnabled()) {
            logger.fine(
                "Waited " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    //~ Instance fields --------------------------------------------------------

    /** Transformation used for display */
    private final AffineTransform scaleTransform = AffineTransform.getScaleInstance(
        1d,
        1d);

    /** Direct access to image buffer (not used) */
    private DataBuffer dataBuffer;

    /** Dimension of current image */
    private Dimension dimension;

    /** Original image dimension */
    private Dimension originalDimension;

    /** Current image */
    private PlanarImage image;

    /** Selection objects where gray level of pixel is to be written to when so
       asked for by calling the update method */
    private Selection levelSelection;

    /** Remember if we have actually rotated the image */
    private boolean rotated = false;

    /** Cached dimension */
    ///private int dimensionWidth;

    /** The image (writable) raster */
    private WritableRaster raster;

    /** The factor to apply to raw pixel value to get gray level on 0..255 */
    private int grayFactor = 1;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // Picture //
    //---------//
    /**
     * Build a picture instance, using a given image.
     *
     * @param image the image provided
     * @exception ImageFormatException
     */
    public Picture (RenderedImage image)
        throws ImageFormatException
    {
        this(image, 1f);
    }

    //---------//
    // Picture //
    //---------//
    /**
     * Build a picture instance, using a given image.
     *
     * @param image the image provided
     * @exception ImageFormatException
     */
    public Picture (BufferedImage image)
        throws ImageFormatException
    {
        this(image, 1f);
    }

    //---------//
    // Picture //
    //---------//
    /**
     * Build a picture instance, using a given image, and the scaling to apply
     * on the image
     *
     * @param image the image provided
     * @param scaling the scaling to apply (1.0 means no scaling)
     * @exception ImageFormatException
     */
    public Picture (RenderedImage image,
                    float         scaling)
        throws ImageFormatException
    {
        RenderedImage src = image;

        if (scaling != 1.0f) {
            ParameterBlock pb = new ParameterBlock().addSource(image)
                                                    .add(scaling)
                                                    .add(scaling)
                                                    .add(0f)
                                                    .add(0f)
                                                    .add(
                new InterpolationNearest());
            src = JAI.create("scale", pb);
        }

        setImage(src);
    }

    //---------//
    // Picture //
    //---------//
    /**
     * Build a picture instance, using a given image, and the scaling to apply
     * on the image
     *
     * @param image the image provided
     * @param scaling the scaling to apply (1.0 means no scaling)
     * @exception ImageFormatException
     */
    public Picture (BufferedImage image,
                    float         scaling)
        throws ImageFormatException
    {
        setImage(image, scaling);
    }

    //---------//
    // Picture //
    //---------//
    /**
     * Build a picture instance, given the name of a file where the related
     * image should be read.
     *
     * @param imgFile the image file
     *
     * @throws FileNotFoundException raised when the file is not found
     * @throws IOException           raised when an IO error occurred
     */
    public Picture (File imgFile)
        throws FileNotFoundException, IOException, ImageFormatException
    {
        setImage(loadFile(imgFile));

        logger.info(
            "Image loaded " + image.getWidth() + " x " + image.getHeight());
    }

    //---------//
    // Picture //
    //---------//
    /**
     * Create a picture as a mosaic of other images, which are to be composed
     * one above the other.
     *
     * This method is not currently used
     *
     * @param files  ordered array of image files,
     * @param thetas array parallel to files, that specifies the needed rotation
     *               angles
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public Picture (File[]   files,
                    double[] thetas)
        throws FileNotFoundException, IOException, ImageFormatException
    {
        int           globalWidth = 0; // Width of resulting mosaic
        int           globalHeight = 0; // Height of resulting mosaic

        int           narrowestIndex = 0; // Index of narrowest image
        int           narrowestWidth = Integer.MAX_VALUE; // Width of narrowest image

        // Array of images and related shifts
        PlanarImage[] images = new PlanarImage[files.length];

        for (int i = 0; i < files.length; i++) {
            logger.info("Loading image " + files[i].getPath());

            PlanarImage img0 = JAI.create("fileload", files[i].getPath());
            System.out.println("i=" + i + " file=" + files[i]);
            System.out.println(
                "img0 width=" + img0.getWidth() + ", height=" +
                img0.getHeight());

            // Rotation
            PlanarImage img1;

            if (thetas[i] != 0) {
                img1 = invert(
                    JAI.create(
                        "Rotate",
                        (new ParameterBlock()).addSource(invert(img0)).add(
                            0.0F).add(0.0F).add((float) thetas[i]).add(
                            new InterpolationBilinear()),
                        null));
            } else {
                img1 = img0;
            }

            System.out.println(
                "img1 width=" + img1.getWidth() + ", height=" +
                img1.getHeight());

            // Shift
            AffineTransform shift = AffineTransform.getTranslateInstance(
                0,
                globalHeight);
            images[i] = JAI.create(
                "Affine",
                (new ParameterBlock()).addSource(img1).add(shift).add(
                    new InterpolationBilinear()));
            System.out.println(
                "final width=" + images[i].getWidth() + ", height=" +
                images[i].getHeight());

            int width = images[i].getWidth();

            if (width < narrowestWidth) {
                narrowestWidth = width;
                narrowestIndex = i;
            }

            System.out.println("globalHeight=" + globalHeight);
            globalHeight += images[i].getHeight();
        }

        // Compute the mosaic parameter block, with narrowest image first
        System.out.println("narrowestIndex=" + narrowestIndex);

        ParameterBlock mosaicParam = new ParameterBlock();
        mosaicParam.addSource(images[narrowestIndex]); // Narrowest first !!!

        for (int i = 0; i < files.length; i++) {
            if (i != narrowestIndex) {
                mosaicParam.addSource(images[i]);
            }
        }

        double[][] threshold = {
                                   { 0 }
                               };
        double[]   bgColor = new double[] { 150 };
        //double[]   bgColor = new double[] {255};
        mosaicParam.add(MosaicDescriptor.MOSAIC_TYPE_OVERLAY);
        mosaicParam.add(null);
        mosaicParam.add(null);
        mosaicParam.add(threshold);
        mosaicParam.add(bgColor);

        image = JAI.create("mosaic", mosaicParam);

        // Cache dimensions
        updateParams();

        // Remember original stuff
        originalDimension = getDimension();

        logger.info(
            "Mosaic " + " " + image.getWidth() + " x " + image.getHeight());
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // loadFile //
    //----------//
    private static RenderedImage loadFile (File imgFile)
        throws IOException
    {
        logger.info("Loading image from " + imgFile + " ...");

        ImageInputStream stream = ImageIO.createImageInputStream(imgFile);

        if (stream == null) {
            throw new IOException("Cannot create image input stream");
        }

        Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

        if (readers.hasNext()) {
            ImageReader reader = readers.next();
            reader.addIIOReadProgressListener(new Listener());
            reader.setInput(stream, true);

            return (reader.read(0));
        } else {
            logger.info("ImageIO cannot read file, using JAI");

            return (JAI.create("fileload", imgFile.getPath()));
        }
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the dimension in pixels of the current image
     *
     * @return the image dimension
     */
    public Dimension getDimension ()
    {
        return new Dimension(dimension);
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the picture height in pixels.
     *
     * @return the height value
     */
    @Implement(PixelSource.class)
    public int getHeight ()
    {
        return dimension.height;
    }

    //------------------//
    // getMaxForeground //
    //------------------//
    @Implement(PixelSource.class)
    public int getMaxForeground ()
    {
        return FOREGROUND;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the name for this Observer
     *
     * @return Observer name
     */
    @Implement(SelectionObserver.class)
    public String getName ()
    {
        return "Picture";
    }

    //---------------//
    // getOrigHeight //
    //---------------//
    /**
     * Report the original picture height, as read from the image file, before
     * any potential rotation.
     *
     * @return the original height value, in pixels
     */
    public int getOrigHeight ()
    {
        return originalDimension.height;
    }

    //--------------//
    // getOrigWidth //
    //--------------//
    /**
     * Report the original picture width, as read from the image file, before
     * any potential rotation.
     *
     * @return the original width value, in pixels
     */
    public int getOrigWidth ()
    {
        return originalDimension.width;
    }

    //----------//
    // getPixel //
    //----------//
    /**
     * Report the pixel element, as read at location (x, y) in the picture.
     *
     * @param x abscissa value
     * @param y ordinate value
     *
     * @return the pixel value
     */
    @Implement(PixelSource.class)
    public final int getPixel (int x,
                               int y)
    {
        int[] pixel = null;
        pixel = raster.getPixel(x, y, pixel); // This allocates pixel!

        return grayFactor * pixel[0];

        ///            return dataBuffer.getElem(x + (y * dimensionWidth));
    }

    //--------------------//
    // getAsBufferedImage //
    //--------------------//
    /**
     * Return a copy of the underlying image as a buffered image.
     *
     * @return the proper buffered image
     */
    public final BufferedImage getAsBufferedImage ()
    {
        return image.getAsBufferedImage();
    }

    //-------------------//
    // setLevelSelection //
    //-------------------//
    /**
     * Inject the selection object where pixel gray level must be written to,
     * when triggered through the update method.
     *
     * @param levelSelection the output selection object
     */
    public void setLevelSelection (Selection levelSelection)
    {
        this.levelSelection = levelSelection;
    }

    //----------//
    // setPixel //
    //----------//
    /**
     * Write a pixel at the provided location, in the currently writable data
     * buffer
     *
     * @param pt  pixel coordinates
     * @param val pixel value
     */
    public final void setPixel (Point pt,
                                int   val)
    {
        ///        dataBuffer.setElem(pt.x + (pt.y * dimensionWidth), val);
        int[] pixel = new int[1];

        if (grayFactor == 1) {
            pixel[0] = val;
        } else {
            pixel[0] = (val + (grayFactor - FOREGROUND - 1)) / grayFactor;
        }

        raster.setPixel(pt.x, pt.y, pixel);
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

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the current width of the picture image. Note that it may have been
     * modified by a rotation.
     *
     * @return the current width value, in pixels.
     */
    @Implement(PixelSource.class)
    public int getWidth ()
    {
        return dimension.width;
    }

    //-------//
    // close //
    //-------//
    /**
     * Release the resources linked to the picture image
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
     * Debugging routine, that prints a basic representation of a rectangular
     * portion of the picture.
     *
     * @param title an optional title for this image dump
     * @param xMin  x first coord
     * @param xMax  x last coord
     * @param yMin  y first coord
     * @param yMax  y last coord
     */
    public void dumpRectangle (String title,
                               int    xMin,
                               int    xMax,
                               int    yMin,
                               int    yMax)
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

    //--------//
    // render //
    //--------//
    /**
     * Paint the picture image in the provided graphic context.
     *
     * @param g     the Graphics context
     * @param ratio the display zoom ratio
     */
    public void render (Graphics g,
                        double   ratio)
    {
        Graphics2D g2 = (Graphics2D) g;
        scaleTransform.setToScale(ratio, ratio);
        g2.drawRenderedImage(image, scaleTransform);
    }

    //--------//
    // rotate //
    //--------//
    /**
     * Rotate the image according to the provided angle.
     *
     * @param theta the desired rotation angle, in radians, positive for
     * clockwise, negative for counter-clockwise
     */
    public void rotate (double theta)
        throws ImageFormatException
    {
        // Invert
        PlanarImage img = invert(image);

        // Rotate
        image = JAI.create(
            "Rotate",
            new ParameterBlock().addSource(img) // Source image
            .add(0f) // x origin
            .add(0f) // y origin
            .add((float) theta) // angle
            .add(new InterpolationBilinear()), // Interpolation hint
            null);

        // Crop the image to fit the size of the previous one
        ParameterBlock cpb = new ParameterBlock().addSource(image) // The source image

                                                 .add(0f) // x

                                                 .add(0f); // y

        if (theta < 0d) { // clock wise
            cpb.add((float) (dimension.width));
            cpb.add((float) ((dimension.height * Math.cos(theta)) - 1f));
        } else { // counter-clock wise
            cpb.add((float) ((dimension.width * Math.cos(theta)) - 1f));
            cpb.add((float) (dimension.height));
        }

        image = JAI.create("crop", cpb, null);

        // de-Invert
        image = invert(image);
        rotated = true;

        // Force immediate mode
        image.getTiles();

        // Update relevant parameters
        updateParams();

        logger.info("Image rotated " + getWidth() + " x " + getHeight());
    }

    //-------//
    // store //
    //-------//
    /**
     * Save the current image to a file, in PNG format
     *
     * @param fname the name of the file, without its extension
     *
     * @return the path name of the created file
     */
    public String store (String fname)
    {
        fname = fname + ".png";
        JAI.create("filestore", image, fname, "PNG", null);
        logger.info("Image stored in " + fname);

        return fname;
    }

    //--------//
    // update //
    //--------//
    /**
     * Call-back triggered when Pixel Selection has been modified.  Based on
     * pixel location, we forward the pixel gray level to whoever is interested
     * in it.
     *
     * @param selection the (Pixel) Selection
     * @param hint potential notification hint
     */
    @Implement(SelectionObserver.class)
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        switch (selection.getTag()) {
        case SHEET_RECTANGLE :

            Integer level = null;

            if ((hint == SelectionHint.LOCATION_ADD) ||
                (hint == SelectionHint.LOCATION_INIT)) {
                // Compute and forward pixel gray level
                Rectangle rect = (Rectangle) selection.getEntity();

                if (rect != null) {
                    Point pt = rect.getLocation();

                    // Check that we are not pointing outside the image
                    if ((pt.x >= 0) &&
                        (pt.x < getWidth()) &&
                        (pt.y >= 0) &&
                        (pt.y < getHeight())) {
                        level = new Integer(getPixel(pt.x, pt.y));
                    }
                }
            }

            levelSelection.setEntity(level, hint);

            break;

        default :
            logger.severe("Unexpected selection event from " + selection);
        }
    }

    //------------//
    // RGBAToGray //
    //------------//
    private static PlanarImage RGBAToGray (PlanarImage image)
    {
        logger.fine("Discarding alpha band ...");

        PlanarImage pi = JAI.create("bandselect", image, new int[] { 0, 1, 2 });

        return RGBToGray(pi);
    }

    //-----------//
    // RGBToGray //
    //-----------//
    private static PlanarImage RGBToGray (PlanarImage image)
    {
        logger.fine("Converting RGB image to gray ...");

        double[][] matrix = {
                                { 0.114d, 0.587d, 0.299d, 0.0d }
                            };

        return JAI.create(
            "bandcombine",
            new ParameterBlock().addSource(image).add(matrix),
            null);
    }

    //------------------//
    // checkImageFormat //
    //------------------//
    /**
     * Check if the image format (and especially its color model) is properly
     * handled by Audiveris.
     *
     * @throws ImageFormatException is the format is not supported
     */
    private void checkImageFormat ()
        throws ImageFormatException
    {
        // Check nb of bands
        int numBands = image.getSampleModel()
                            .getNumBands();

        if (logger.isFineEnabled()) {
            logger.fine("checkImageFormat. numBands=" + numBands);
        }

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

    //    //---------------//
    //    // grayToGray256 //
    //    //---------------//
    //    private static PlanarImage grayToGray256 (PlanarImage image)
    //    {
    //        logger.info("Converting gray image to gray-256 ...");
    //
    //        ColorSpace colorSpace = ColorSpace.getInstance(
    //            java.awt.color.ColorSpace.CS_GRAY);
    //
    //        return JAI.create("colorConvert", image, colorSpace, null);
    //
    //        //        ParameterBlock pb = new ParameterBlock();
    //        //
    //        //        pb.addSource(image);
    //        //
    //        //        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
    //        //        ColorModel colorModel = new ColorModel
    //        //        //pb.add(cs);
    //        //
    //        //        return JAI.create("ColorConvert", pb);
    //    }

    //--------//
    // invert //
    //--------//
    private static PlanarImage invert (PlanarImage image)
    {
        return JAI.create(
            "Invert",
            new ParameterBlock().addSource(image).add(null).add(null).add(null).add(
                null).add(null),
            null);
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

    //----------//
    // setImage //
    //----------//
    private void setImage (BufferedImage bufferedImage,
                           float         scaling)
        throws ImageFormatException
    {
        image = PlanarImage.wrapRenderedImage(bufferedImage);

        if (logger.isFineEnabled()) {
            logger.fine(
                "planarImage hasAlpha=" + image.getColorModel().hasAlpha());
        }

        if (scaling != 1.0f) {
            ParameterBlock pb = new ParameterBlock().addSource(image)
                                                    .add(scaling)
                                                    .add(scaling)
                                                    .add(0f)
                                                    .add(0f)
                                                    .add(
                new InterpolationNearest());
            image = JAI.create("scale", pb);

            if (logger.isFineEnabled()) {
                logger.fine(
                    "scaled hasAlpha=" + image.getColorModel().hasAlpha());
            }
        }

        checkImage();
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

            // Remember original dimension
            originalDimension = getDimension();
        }
    }

    //--------------//
    // updateParams //
    //--------------//
    private void updateParams ()
        throws ImageFormatException
    {
        checkImageFormat();

        // Cache dimensions
        dimension = new java.awt.Dimension(image.getWidth(), image.getHeight());
        ///dimensionWidth = dimension.width;
        raster = Raster.createWritableRaster(
            image.getData().getSampleModel(),
            image.getData().getDataBuffer(),
            null);

        if (logger.isFineEnabled()) {
            logger.fine("raster=" + raster);
        }

        ///dataBuffer = raster.getDataBuffer();

        // Check pixel size
        ColorModel colorModel = image.getColorModel();
        int        pixelSize = colorModel.getPixelSize();

        if (logger.isFineEnabled()) {
            logger.fine("colorModel=" + colorModel + " pixelSize=" + pixelSize);
        }

        if (pixelSize == 8) {
            grayFactor = 1;
        } else if (pixelSize == 1) {
            logger.warning(
                "Images with pixels coded on 1 bit are expensive to process");
            logger.warning(
                "Consider converting to a format which codes pixels on 1 byte");
            grayFactor = 255;
        } else {
            throw new java.lang.RuntimeException(
                "Unsupported pixel size:" + pixelSize);
        }

        if (logger.isFineEnabled()) {
            logger.fine("grayFactor=" + grayFactor);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------//
    // Listener //
    //----------//
    /**
     * Listener allows ImageIO to log image loading status.
     */
    private static class Listener
        implements IIOReadProgressListener, IIOWriteProgressListener
    {
        private volatile float lastProgress = 0;

        public void imageComplete (ImageReader source)
        {
            if (logger.isFineEnabled()) {
                logger.fine("Image loading complete");
            }

            lastProgress = 0;
        }

        public void imageProgress (ImageReader source,
                                   float       percentageDone)
        {
        	if ((percentageDone - lastProgress) > 10) {
        		lastProgress = percentageDone;
        		if (logger.isFineEnabled()) {
                    logger.fine("Image loaded " + percentageDone + "%");
                }
        		logger.info(LogStepMonitorHandler.FORCE);
            }
        }

        public void imageStarted (ImageReader source,
                                  int         imageIndex)
        {
            if (logger.isFineEnabled()) {
                logger.fine("Image loading started");
            }
            logger.info(LogStepMonitorHandler.FORCE);
        }

        public void readAborted (ImageReader source)
        {
            logger.warning("Image loading aborted");
        }

        public void sequenceComplete (ImageReader source)
        {
        }

        public void sequenceStarted (ImageReader source,
                                     int         minIndex)
        {
        }

        public void thumbnailComplete (ImageReader source)
        {
        }

        public void thumbnailProgress (ImageReader source,
                                       float       percentageDone)
        {
        }

        public void thumbnailStarted (ImageReader source,
                                      int         imageIndex,
                                      int         thumbnailIndex)
        {
        }

        public void imageComplete (ImageWriter imagewriter)
        {
            if (logger.isFineEnabled()) {
                logger.info("Image writing complete");
            }

            lastProgress = 0;
        }

        public void imageProgress (ImageWriter imagewriter,
                                   float       percentageDone)
        {
            if (logger.isFineEnabled()) {
                if ((percentageDone - lastProgress) > 10) {
                    lastProgress = percentageDone;
                    logger.info("Image written " + percentageDone + "%");
                }
            }
        }

        public void imageStarted (ImageWriter imagewriter,
                                  int         i)
        {
            if (logger.isFineEnabled()) {
                logger.info("Image writing started");
            }
        }

        public void thumbnailComplete (ImageWriter imagewriter)
        {
        }

        public void thumbnailProgress (ImageWriter imagewriter,
                                       float       f)
        {
        }

        public void thumbnailStarted (ImageWriter imagewriter,
                                      int         i,
                                      int         j)
        {
        }

        public void writeAborted (ImageWriter imagewriter)
        {
        }
    }
}
