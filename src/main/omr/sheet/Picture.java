//-----------------------------------------------------------------------//
//                                                                       //
//                             P i c t u r e                             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.util.Logger;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.media.jai.*;
import javax.media.jai.operator.MosaicDescriptor;

/**
 * Class <code>Picture</code> encapsulates an image, allowing modifications
 * on it, but still rendering (displaying) the original one. Its current
 * implementation is based on JAI (Java Advanced Imaging). And JAI is not
 * used outside of this class.
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
 * <li> To <b>read</b> or to <b>write</b> a pixel knowing its location in
 * the current image </li>
 *
 * </ul> </p>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Picture
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(Picture.class);

    /**
     * Constant color of the picture background (generally white).
     */
    public static final int BACKGROUND = 255;

    /**
     * Constant color of the picture foreground (generally black), which
     * means that any pixel whose level is higher than or equal to this
     * value will be considered as foreground
     */
    public static final int FOREGROUND = 227;

    //~ Instance variables ------------------------------------------------

    // Original image dimension
    private Dimension originalDimension;

    // Transformation used for display
    private final AffineTransform scaleTransform = AffineTransform
            .getScaleInstance(1d,
                              1d);

    // Current image
    private RenderedOp image;
    private Dimension dimension;
    private int dimensionWidth;         // To speedup ...
    private DataBuffer dataBuffer;

    // Remember if we have actually rotated the image
    private boolean rotated = false;

    // Work variables
    private int[] pix = new int[1];

    //~ Constructors ------------------------------------------------------

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
            throws FileNotFoundException,
                   IOException,
                   ImageFormatException
    {
        // Try to read the image file
        logger.info("Loading image from " + imgFile + " ...");
        if (image != null) {
            image.dispose();
        }

        image = JAI.create("fileload", imgFile.getPath());

        // Check that the whole image has been loaded
        if ((image.getWidth() == -1) || (image.getHeight() == -1)) {
            throw new IOException("Could not load image file "
                                  + imgFile.getPath());
        } else {
            // Check image format, and convert to gray if needed. This may
            // throw ImageFormatException
            checkImageFormat();

            // Cache dimensions
            updateParams();

            // Remember original dimension
            originalDimension = getDimension();

            logger.info("Image loaded "
                        + image.getWidth() + " x " + image.getHeight());
        }
    }

    //---------//
    // Picture //
    //---------//
    /**
     * Create a picture as a mosaic or other images, which are to be
     * composed one above the following one
     *
     * @param files  ordered array of image files,
     * @param thetas array parallel to files, that specifies the needed
     *               rotation angles
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public Picture (File[] files,
                    double[] thetas)
            throws FileNotFoundException,
                   IOException
    {
        int globalWidth = 0;            // Width of resulting mosaic
        int globalHeight = 0;           // Height of resulting mosaic

        int narrowestIndex = 0;              // Index of narrowest image
        int narrowestWidth = Integer.MAX_VALUE;// Width of narrowest image

        // Array of images and related shifts
        RenderedOp[] images = new RenderedOp[files.length];
        AffineTransform[] shifts = new AffineTransform[files.length];

        for (int i = 0; i < files.length; i++) {
            // Load from file
            RenderedOp img0 = JAI.create("fileload", files[i].getPath());
            System.out.println("i=" + i + " file=" + files[i]);
            System.out.println("img0 width=" + img0.getWidth() +
                               ", height=" + img0.getHeight());

            // Rotation
            RenderedOp img1;
            if (thetas[i] != 0) {
                img1 = invert(JAI.create("Rotate",
                                         (new ParameterBlock())
                                         .addSource(invert(img0))
                                         .add(0.0F)
                                         .add(0.0F)
                                         .add((float) thetas[i])
                                         .add(new InterpolationBilinear()),
                                         null));
            } else {
                img1 = img0;
            }
            System.out.println("img1 width=" + img1.getWidth() +
                               ", height=" + img1.getHeight());

            // Shift
            AffineTransform shift = AffineTransform.getTranslateInstance(0, globalHeight);
            images[i] = JAI.create("Affine",
                                   (new ParameterBlock())
                                   .addSource(img1)
                                   .add(shift)
                                   .add(new InterpolationBilinear()));
            System.out.println("final width=" + images[i].getWidth() +
                               ", height=" + images[i].getHeight());

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
        mosaicParam.addSource(images[narrowestIndex]);  // Narrowest first !!!
        for (int i = 0; i < files.length; i++) {
            if (i != narrowestIndex) {
                mosaicParam.addSource(images[i]);
            }
        }

        double[][] threshold = {{0}};
        double[] bgColor = new double[]{150};
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

        logger.info("Mosaic " + " "
                    + image.getWidth() + " x " + image.getHeight());
    }
    //~ Methods -----------------------------------------------------------

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
    public int getHeight ()
    {
        return dimension.height;
    }

    //---------------//
    // getOrigHeight //
    //---------------//
    /**
     * Report the original picture height, as read from the image file,
     * before any potential rotation.
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
     * Report the original picture width, as read from the image file,
     * before any potential rotation.
     *
     * @return the original width value, in pixels
     */
    public int getOrigWidth ()
    {
        return originalDimension.width;
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
    private void checkImageFormat()
        throws ImageFormatException
    {
        // Check nb of bands
        int numBands = image.getSampleModel().getNumBands();
        if (numBands != 1) {
            if (numBands == 3) {
                image = colorToGray(image);
            } else {
                throw new ImageFormatException
                    ("Unsupported sample model" +
                     " numBands=" + numBands);
            }
        }

        // Check pixel size
        ColorModel colorModel = image.getColorModel();
        int pixelSize = colorModel.getPixelSize();
        if (pixelSize != 8) {
            image = grayToGray256(image);
        }
    }

    //----------//
    // setPixel //
    //----------//
    /**
     * Write a pixel at the provided location, in the currently writable
     * data buffer
     *
     * @param pt  pixel coordinates
     * @param val pixel value
     */
    public void setPixel (Point pt,
                          int val)
    {
        dataBuffer.setElem(pt.x + (pt.y * dimensionWidth), val);
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
    public int getPixel (int x,
                         int y)
    {
        return dataBuffer.getElem(x + (y * dimensionWidth));
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
     * Report the current width of the picture image. Note that it may have
     * been modified by a rotation.
     *
     * @return the current width value, in pixels.
     */
    public int getWidth ()
    {
        return dimensionWidth;
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
                        double ratio)
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
    {
        // Invert
        RenderedOp img = invert(image);

        // Rotate
        image = JAI.create
            ("Rotate",
             new ParameterBlock()
             .addSource(img)            // Source image
             .add(0f)                   // x origin
             .add(0f)                   // y origin
             .add((float) theta)        // angle
             .add(new InterpolationBilinear()), // Interpolation hint
             null);

        // Crop the image to fit the size of the previous one
        ParameterBlock cpb = new ParameterBlock()
            .addSource(image)           // The source image
            .add(0f)                     // x
            .add(0f);                    // y
        if (theta < 0d) {                   // clock wise
            cpb.add((float) (dimension.width));
            cpb.add((float) (dimension.height * Math.cos(theta) -1f));
        } else {                             // counter-clock wise
            cpb.add((float) (dimension.width * Math.cos(theta) -1f));
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
    // invert //
    //--------//
    private static RenderedOp invert (RenderedOp image)
    {
        return JAI.create("Invert",
                          new ParameterBlock()
                          .addSource(image)
                          .add(null)
                          .add(null)
                          .add(null)
                          .add(null)
                          .add(null),
                          null);
    }

    //-------------//
    // colorToGray //
    //-------------//
    private static RenderedOp colorToGray (RenderedOp image)
    {
        logger.info("Converting color image to gray ...");

        double[][] matrix = { {0.114d, 0.587d, 0.299d, 0.0d} };

        return JAI.create("bandcombine",
                          new ParameterBlock()
                          .addSource(image)
                          .add(matrix),
                          null);
    }

    //---------------//
    // grayToGray256 //
    //---------------//
    private static RenderedOp grayToGray256 (RenderedOp image)
    {
        logger.info("Converting gray image to gray-256 ...");

        ColorSpace colorSpace = ColorSpace.getInstance
            (java.awt.color.ColorSpace.CS_GRAY);

        int[] bits = new int[]{8};
        int opaque = Transparency.OPAQUE;
        int dataType = DataBuffer.TYPE_BYTE;
        ColorModel colorModel = new ComponentColorModel
            (colorSpace, bits, false, false, opaque, dataType);

        return JAI.create("colorConvert", image, colorSpace, null);
    }

    //--------------//
    // updateParams //
    //--------------//
    private void updateParams()
    {
        // Cache dimensions
        dimension = new Dimension(image.getWidth(), image.getHeight());
        dimensionWidth = dimension.width;

        // Point to the image data buffer
        dataBuffer = image.getData().getDataBuffer();
    }
}
