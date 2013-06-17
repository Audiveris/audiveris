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

import omr.sheet.picture.jai.JaiLoader;

import omr.util.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Class {@code PictureLoader} gathers helper functions for {@link
 * Picture} to handle the loading of one or several images out of an
 * input file.
 *
 * <p>It leverages several software pieces: JAI, ImageIO, and Ghostscript.
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 * @author Maxim Poliakovski
 */
public class PictureLoader
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(PictureLoader.class);

    //~ Constructors -----------------------------------------------------------
    /**
     * To disallow instantiation.
     */
    private PictureLoader ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //
    //----------//
    // loadFile //
    //----------//
    /**
     * Loads a sequence of RenderedImage instances from a file.
     *
     * If ImageIO can read the file, it is used preferentially.
     * If not, or if ImageIO has an error, a PDF loader is used for files
     * ending with ".pdf" and JAI is used for all other files.
     *
     * @param imgFile the image file to load
     * @param pages   if not null or empty, specifies (counted from 1) which
     *                pages are desired. Otherwise all pages are loaded.
     * @return a sorted map of RenderedImage's (often but not always a
     *         BufferedImage), guaranteed not to be null, id counted from 1.
     * @throws IllegalArgumentException if file does not exist
     * @throws RuntimeException         if we are unable to load the file
     */
    public static SortedMap<Integer, RenderedImage> loadImages (File imgFile,
                                                                SortedSet<Integer> pages)
    {
        if (!imgFile.exists()) {
            throw new IllegalArgumentException(imgFile + " does not exist");
        }

        logger.info("Loading {} ...", imgFile);

        logger.debug("Trying ImageIO");

        SortedMap<Integer, RenderedImage> images = loadImageIO(imgFile, pages, 0);

        if (images == null) {
            String extension = FileUtil.getExtension(imgFile);

            if (extension.equalsIgnoreCase(".pdf")) {
                images = loadPDF(imgFile, pages);
            } else {
                logger.debug("Using JAI");
                images = JaiLoader.loadJAI(imgFile);
            }
        }

        if (images == null) {
            logger.warn("Unable to load any image from {}", imgFile);
        }

        return images;
    }

    //-------------//
    // loadImageIO //
    //-------------//
    /**
     * Try to load a sequence of images, using ImageIO.
     *
     * @param imgFile the input image file
     * @param pages   if not null or empty, specifies (counted from 1) which
     *                precise pages are desired. Otherwise all pages are loaded.
     * @param offset  specify offset on page ids.
     * @return a map (id -> image), or null if failed to load
     */
    private static SortedMap<Integer, RenderedImage> loadImageIO (File imgFile,
                                                                  SortedSet<Integer> pages,
                                                                  int offset)
    {
        logger.debug("loadImageIO {} pages:{} offset:{}", imgFile, pages, offset);

        // Input stream
        ImageInputStream stream;

        try {
            stream = ImageIO.createImageInputStream(imgFile);
        } catch (IOException ex) {
            logger.warn("Unable to make ImageIO stream", ex);

            return null;
        }

        if (stream == null) {
            logger.debug("No ImageIO input stream provider");

            return null;
        }

        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

            if (!readers.hasNext()) {
                logger.debug("No ImageIO reader");

                return null;
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(stream, false);

                int imageCount = reader.getNumImages(true);

                if (imageCount > 1) {
                    logger.info("{} contains {} images",
                            imgFile.getName(), imageCount);
                }

                SortedMap<Integer, RenderedImage> images = new TreeMap<>();

                for (int i = 1; i <= imageCount; i++) {
                    int id = i + offset;
                    if ((pages == null) || pages.isEmpty()
                        || (pages.contains(id))) {
                        BufferedImage img = reader.read(i - 1);
                        images.put(id, img);
                        logger.info("Loaded image #{} ({} x {})",
                                id, img.getWidth(), img.getHeight());
                    }
                }

                return images;
            } catch (Exception ex) {
                logger.warn("ImageIO failed", ex);

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
    // loadPDF //
    //---------//
    /**
     * Load a sequence of images out of a PDF file.
     * We spawn a Ghostscript subprocess to convert PDF to TIFF and then
     * load the temporary TIFF file via loadImageIO().
     *
     * @param imgFile the input PDF file
     * @param pages   if not null or empty, specifies (counted from 1) which
     *                precise images are desired. Otherwise all pages are
     *                loaded.
     * @return a map of images, or null if failed to load
     */
    private static SortedMap<Integer, RenderedImage> loadPDF (File imgFile,
                                                              SortedSet<Integer> pages)
    {
        logger.debug("loadPDF {} pages:{}", imgFile, pages);

        // Create a temporary tiff file from the PDF input
        Path temp = null;
        try {
            temp = Files.createTempFile("pic-", ".tif");
        } catch (IOException ex) {
            logger.warn("Cannot create temporary file " + temp, ex);
            return null;
        }

        // Arguments for Ghostscript
        List<String> gsArgs = new ArrayList<>();
        gsArgs.add(Ghostscript.getPath());
        gsArgs.add("-dQUIET");
        gsArgs.add("-dNOPAUSE");
        gsArgs.add("-dBATCH");
        gsArgs.add("-dSAFER");
        gsArgs.add("-sDEVICE=" + constants.pdfDevice.getValue());
        gsArgs.add("-r" + constants.pdfResolution.getValue());
        gsArgs.add("-sOutputFile=" + temp);
        if (pages != null && !pages.isEmpty()) {
            gsArgs.add("-dFirstPage=" + pages.first());
            gsArgs.add("-dLastPage=" + pages.last());
        }
        gsArgs.add(imgFile.toString());
        logger.debug("gsArgs:{}", gsArgs);

        try {
            // Spawn Ghostscript process and wait for its completion
            new ProcessBuilder(gsArgs).start().waitFor();

            // Now load the temporary tiff file
            if (pages != null && !pages.isEmpty()) {
                return loadImageIO(temp.toFile(), pages, pages.first() - 1);
            } else {
                return loadImageIO(temp.toFile(), null, 0);
            }
        } catch (IOException | InterruptedException ex) {
            logger.warn("Error running Ghostscript " + gsArgs, ex);
            return null;
        } finally {
            try {
                Files.delete(temp);
            } catch (IOException ex) {
                logger.warn("Error deleting file " + temp, ex);
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

        Constant.Integer pdfResolution = new Constant.Integer(
                "DPI",
                300,
                "DPI resolution for PDF images");

        Constant.String pdfDevice = new Constant.String(
                "tiff24nc",
                "Ghostscript output device (tiff24nc or tiffscaled8)");

    }
}
