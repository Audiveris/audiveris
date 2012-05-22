//----------------------------------------------------------------------------//
//                                                                            //
//                         P i c t u r e L o a d e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Brenton Partridge 2007-2008.                                //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.picture;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.util.FileUtil;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFRenderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;

/**
 * PictureLoader separates static methods for loading an image from a file
 * from the image processing methods in the Picture class. It leverages
 * the JAI, ImageIO, and PDF Renderer libraries. While loading, if possible,
 * empty info strings will be sent to the logger, incrementing the step monitor
 * to show loading progress.
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 */
public class PictureLoader
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(PictureLoader.class);

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
     * Loads a sequence of RenderedImage's from a file.
     *
     * If ImageIO can read the file, it is used preferentially.
     * If not, or ImageIO has an error, PDF Renderer is used for files
     * ending with ".pdf" and JAI is used for all other files.
     *
     * @param imgFile the image file to load
     * @param index   if not null, specifies (counted from 1) which single image
     * is desired
     * @return a sorted map of RenderedImage's (often but not always a
     * BufferedImage), guaranteed not to be null, index counted from 1.
     * @throws IllegalArgumentException if file does not exist
     * @throws RuntimeException         if all libraries are unable to load the
     *                                  file
     */
    public static SortedMap<Integer, RenderedImage> loadImages (File imgFile,
                                                                Integer index)
    {
        if (!imgFile.exists()) {
            throw new IllegalArgumentException(imgFile + " does not exist");
        }

        logger.info("Loading {0} ...", imgFile);

        logger.fine("Using ImageIO");

        SortedMap<Integer, RenderedImage> images = loadImageIO(imgFile, index);

        if (images == null) {
            String extension = FileUtil.getExtension(imgFile);

            if (extension.equalsIgnoreCase(".pdf")) {
                logger.fine("Using PDF renderer");
                images = loadPDF(imgFile, index);
            } else {
                logger.fine("Using JAI");
                images = loadJAI(imgFile);
            }
        }

        if (images == null) {
            throw new RuntimeException(
                    "Unable to load any image from " + imgFile);
        }

        return images;
    }

    //-------------//
    // loadImageIO //
    //-------------//
    /**
     * Try to load a sequence of images out of the provided stream
     *
     * @param imgFile the input image file
     * @param index   if not null, specifies (counted from 1) which single image
     * is desired
     * @return a map of images, or null if failed to load
     */
    private static SortedMap<Integer, RenderedImage> loadImageIO (File imgFile,
                                                                  Integer index)
    {
        // Input stream
        ImageInputStream stream;

        try {
            stream = ImageIO.createImageInputStream(imgFile);
        } catch (IOException ex) {
            logger.warning("Unable to make ImageIO stream", ex);

            return null;
        }

        if (stream == null) {
            logger.fine("No ImageIO input stream provider");

            return null;
        }

        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

            if (!readers.hasNext()) {
                logger.fine("No ImageIO reader");

                return null;
            }

            ImageReader reader = readers.next();

            try {
                ///reader.addIIOReadProgressListener(new Listener());
                reader.setInput(stream, false);

                int imageCount = reader.getNumImages(true);

                if (imageCount > 1) {
                    logger.info("{0} contains {1} images", new Object[]{imgFile.
                                getName(), imageCount});
                }

                SortedMap<Integer, RenderedImage> images = new TreeMap<>();

                for (int i = 1; i <= imageCount; i++) {
                    if ((index == null) || (index == i)) {
                        BufferedImage img = reader.read(i - 1);
                        images.put(i, img);
                        logger.info("Loaded image #{0} ({1} x {2})",
                                    new Object[]{i, img.getWidth(), img.
                                    getHeight()});
                    }
                }

                return images;
            } catch (Exception ex) {
                logger.warning("ImageIO failed", ex);

                return null;
            } finally {
                reader.dispose();
            }
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    //---------//
    // loadJAI //
    //---------//
    /**
     * Load an image, using JAI
     *
     * @param imgFile the input file
     * @return a map of one image, or null if failed to load
     */
    private static SortedMap<Integer, RenderedImage> loadJAI (File imgFile)
    {
        RenderedImage image = JAI.create("fileload", imgFile.getPath());

        try {
            if ((image.getWidth() > 0) && (image.getHeight() > 0)) {
                SortedMap<Integer, RenderedImage> images = new TreeMap<>();
                images.put(1, image);

                return images;
            }
        } catch (Exception ex) {
            logger.fine(ex.getMessage());
        }

        return null;
    }

    //---------//
    // loadPDF //
    //---------//
    /**
     * Load a sequence of images out of a PDF file
     *
     * @param imgFile the input PDF file
     * @param index   if not null, specifies (counted from 1) which single image
     * is desired
     * @return a map of images, or null if failed to load
     */
    private static SortedMap<Integer, RenderedImage> loadPDF (File imgFile,
                                                              Integer index)
    {
        double res = constants.pdfScale.getValue();

        try {
            // set up the PDF reading
            RandomAccessFile raf = new RandomAccessFile(imgFile, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    0,
                    channel.size());

            try {
                PDFFile pdfFile = new PDFFile(buf);

                // Image count
                int imageCount = pdfFile.getNumPages();

                if (imageCount > 1) {
                    logger.info("{0} contains {1} images", new Object[]{imgFile.
                                getName(), imageCount});
                }

                SortedMap<Integer, RenderedImage> images = new TreeMap<>();

                for (int i = 1; i <= imageCount; i++) {
                    if ((index == null) || (index == i)) {
                        // PDF pages are accessed starting from 1 (vs 0)!!!
                        PDFPage page = pdfFile.getPage(i);

                        // Get the dimensions
                        Rectangle2D bbox = page.getBBox();
                        Rectangle rect = new Rectangle(
                                0,
                                0,
                                (int) (bbox.getWidth() * res),
                                (int) (bbox.getHeight() * res));

                        // Create and configure a graphics object
                        BufferedImage img = new BufferedImage(
                                rect.width,
                                rect.height,
                                BufferedImage.TYPE_BYTE_GRAY); // 0..255

                        Graphics2D g2 = img.createGraphics();
                        // Use antialiasing
                        g2.setRenderingHint(
                                RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

                        // Do the actual drawing
                        PDFRenderer renderer = new PDFRenderer(
                                page,
                                g2,
                                rect, // bounds into which to fit the page
                                null, // No clipping
                                Color.WHITE); // Background color
                        page.waitForFinish();
                        renderer.run();
                        images.put(i, img);

                        logger.info("{0} loaded image #{1} ({2} x {3})",
                                    new Object[]{imgFile.getName(), i, img.
                                    getWidth(), img.getHeight()});
                    }
                }

                return images;
            } catch (Throwable e) {
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
}
