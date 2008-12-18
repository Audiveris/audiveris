//----------------------------------------------------------------------------//
//                                                                            //
//                        P i c t u r e L o a d e r                           //
//                                                                            //
//  Copyright (C) Brenton Partridge 2007-2008.                                //
//  All rights reserved.                                                      //
//  This software is released under the GNU General Public License.           //
//  Contact dev@audiveris.dev.java.net to report bugs & suggestions.          //
//----------------------------------------------------------------------------//
//
package omr.sheet.picture;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;

/**
 * PictureLoader separates static methods for loading an image from a file
 * from the image processing methods in the Picture class. It leverages
 * the JAI, ImageIO, and PDF Renderer libraries. While loading, if possible,
 * empty info strings will be sent to the logger, incrementing the step monitor
 * to show loading progress.
 *
 * @author Brenton Partridge
 * @version $Id$
 */
public class PictureLoader
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Picture.class);

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    //~ Constructors -----------------------------------------------------------

    /**
     * To disallow instantiation.
     */
    private PictureLoader ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // loadFile //
    //----------//
    /**
     * Loads a RenderedImage from a file.
     * If ImageIO can read the file, it is used preferentially.
     * If not, or ImageIO has an error, PDF Renderer is used for files
     * ending with ".pdf" and JAI is used for all other files.
     *
     * @return the RenderedImage (often but not always a BufferedImage),
     * guaranteed not to be null
     * @throws IllegalArgumentException if file does not exist
     * @throws RuntimeException if all libraries unable to load file
     */
    public static RenderedImage loadFile (File imgFile)
    {
        if (!imgFile.exists()) {
            throw new IllegalArgumentException(imgFile + " does not exist");
        }

        logger.info("Loading image from " + imgFile + " ...");

        RenderedImage image = loadImageIO(imgFile);

        if (image == null) {
            if (imgFile.getName()
                       .endsWith(".pdf")) {
                logger.fine("Using PDF renderer");
                image = loadPDF(imgFile);
            } else {
                logger.fine("Using JAI");
                image = (JAI.create("fileload", imgFile.getPath()));
            }
        }

        if (image == null) {
            throw new RuntimeException("Unable to load image");
        }

        return image;
    }

    //-------------//
    // loadImageIO //
    //-------------//
    public static BufferedImage loadImageIO (File imgFile)
    {
        ImageInputStream stream = null;

        try {
            try {
                stream = ImageIO.createImageInputStream(imgFile);

                if (stream == null) {
                    logger.fine("No ImageIO input stream provider");

                    return null;
                }
            } catch (IOException e) {
                stream = null;
                logger.warning("Unable to make ImageIO stream", e);

                return null;
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

            if (!readers.hasNext()) {
                logger.fine("No ImageIO reader");

                return null;
            }

            ImageReader reader = readers.next();

            try {
                reader.addIIOReadProgressListener(new Listener());
                reader.setInput(stream, false);

                if (reader.getNumImages(false) > 1) {
                    logger.info("Using only first image in multi-image file");
                }

                return reader.read(0);
            } catch (Exception ex) {
                logger.warning("ImageIO failed", ex);

                return null;
            } finally {
                reader.dispose();
            }
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
            }
        }
    }

    //---------//
    // loadPDF //
    //---------//
    private static BufferedImage loadPDF (File file)
    {
        double res = constants.pdfScale.getValue();

        try {
            // set up the PDF reading
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel      channel = raf.getChannel();
            ByteBuffer       buf = channel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                channel.size());

            try {
                /*
                   PDFFile pdffile = new PDFFile(buf);
                
                   // get the first page
                   PDFPage page = pdffile.getPage(0);
                
                   // do the actual drawing
                   PDFRenderer renderer = new PDFRenderer(page, g2,
                       [bounding box], null, Color.RED);
                   page.waitForFinish();
                   renderer.run();
                 */
                Class<?>      PDFFile = Class.forName(
                    "com.sun.pdfview.PDFFile");
                Object        pdf = PDFFile.getConstructor(ByteBuffer.class)
                                           .newInstance(buf);

                // get the first page
                Class<?>      PDFPage = Class.forName(
                    "com.sun.pdfview.PDFPage");
                Object        page = PDFFile.getMethod("getPage", int.class)
                                            .invoke(pdf, 0);

                // get the dimensions
                Rectangle2D   bbox = (Rectangle2D) PDFPage.getMethod("getBBox")
                                                          .invoke(page);
                Rectangle     rect = new Rectangle(
                    0,
                    0,
                    (int) (bbox.getWidth() * res),
                    (int) (bbox.getHeight() * res));

                // create and configure a graphics object
                BufferedImage img = new BufferedImage(
                    rect.width,
                    rect.height,
                    BufferedImage.TYPE_BYTE_GRAY);

                Graphics2D    g2 = img.createGraphics();
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

                // do the actual drawing
                Class<?>       PDFRenderer = Class.forName(
                    "com.sun.pdfview.PDFRenderer");
                Constructor<?> renderConst = PDFRenderer.getConstructor(
                    PDFPage,
                    Graphics2D.class,
                    Rectangle.class,
                    Rectangle2D.class,
                    Color.class);
                Object         renderer = renderConst.newInstance(
                    page,
                    g2,
                    rect,
                    null,
                    Color.WHITE);
                logger.info("");
                PDFPage.getMethod("waitForFinish")
                       .invoke(page);
                logger.info("");
                PDFRenderer.getMethod("run")
                           .invoke(renderer);

                return img;
            } catch (ClassNotFoundException e) {
                logger.warning("Unable to access PDF Renderer library", e);

                return null;
            } catch (Exception e) {
                logger.warning("Unable to render PDF", e);

                return null;
            } finally {
                raf.close();
            }
        } catch (IOException e) {
            logger.warning("Unable to load PDF", e);

            return null;
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

        Constant.Ratio pdfScale = new Constant.Ratio(
            4,
            "Upscaling of PDF resolution from default dimensions");
    }

    //----------//
    // Listener //
    //----------//
    /**
     * Listener allows ImageIO to log image loading status.
     */
    private static class Listener
        implements IIOReadProgressListener, IIOWriteProgressListener
    {
        //~ Instance fields ----------------------------------------------------

        /** Previous progress state */
        private volatile float lastProgress = 0;

        //~ Methods ------------------------------------------------------------

        public void imageComplete (ImageReader source)
        {
            logger.info("Image loading complete");

            lastProgress = 0;
        }

        public void imageComplete (ImageWriter imagewriter)
        {
            logger.info("Image writing complete");

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

                logger.info("");
            }
        }

        public void imageProgress (ImageWriter imagewriter,
                                   float       percentageDone)
        {
            if ((percentageDone - lastProgress) > 10) {
                lastProgress = percentageDone;

                if (logger.isFineEnabled()) {
                    logger.fine("Image written " + percentageDone + "%");
                }

                logger.info("");
            }
        }

        public void imageStarted (ImageReader source,
                                  int         imageIndex)
        {
            if (logger.isFineEnabled()) {
                logger.fine("Image loading started");
            }

            logger.info("");
        }

        public void imageStarted (ImageWriter imagewriter,
                                  int         i)
        {
            if (logger.isFineEnabled()) {
                logger.fine("Image writing started");
            }
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

        public void thumbnailComplete (ImageWriter imagewriter)
        {
        }

        public void thumbnailProgress (ImageReader source,
                                       float       percentageDone)
        {
        }

        public void thumbnailProgress (ImageWriter imagewriter,
                                       float       f)
        {
        }

        public void thumbnailStarted (ImageReader source,
                                      int         imageIndex,
                                      int         thumbnailIndex)
        {
        }

        public void thumbnailStarted (ImageWriter imagewriter,
                                      int         i,
                                      int         j)
        {
        }

        public void writeAborted (ImageWriter imagewriter)
        {
            logger.warning("Image writing aborted");
        }
    }

    //----------//
    // Observer //
    //----------//
    /**
     * Observer allows the PDF renderer to log image loading status.
     * TODO: Adding this to the renderer currently causes a bug,
     * so it is not used.
     */
    @SuppressWarnings("unused")
    private static class Observer
        implements ImageObserver
    {
        //~ Methods ------------------------------------------------------------

        public boolean imageUpdate (Image img,
                                    int   infoflags,
                                    int   x,
                                    int   y,
                                    int   width,
                                    int   height)
        {
            if (logger.isFineEnabled()) {
                logger.fine(format(infoflags, x, y, width, height));
            }

            logger.info("");

            return true;
        }

        private static String format (int infoflags,
                                      int x,
                                      int y,
                                      int width,
                                      int height)
        {
            StringBuffer buf = new StringBuffer();

            for (Field f : ImageObserver.class.getFields()) {
                if (Modifier.isStatic(f.getModifiers()) &&
                    f.getType()
                     .isPrimitive()) {
                    String name = f.getName();
                    int    mask = 0;

                    try {
                        mask = f.getInt(null);
                    } catch (Exception e) {
                    }

                    if ((infoflags & mask) > 0) {
                        buf.append(name)
                           .append(' ');
                    }
                }
            }

            if (buf.length() > 0) {
                buf.deleteCharAt(buf.length() - 1);
            }

            return buf.toString();
        }
    }
}
