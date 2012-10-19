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

import omr.log.Logger;

import omr.util.FileUtil;

import omr.WellKnowns;

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
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;

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

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(PictureLoader.class);

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
     * @param id      if not null, specifies (counted from 1) which single image
     *                is desired
     * @return a sorted map of RenderedImage's (often but not always a
     *         BufferedImage), guaranteed not to be null, id counted from 1.
     * @throws IllegalArgumentException if file does not exist
     * @throws RuntimeException         if we are unable to load the file
     */
    public static SortedMap<Integer, RenderedImage> loadImages (File imgFile,
                                                                Integer id)
    {
        if (!imgFile.exists()) {
            throw new IllegalArgumentException(imgFile + " does not exist");
        }

        logger.info("Loading {0} ...", imgFile);

        logger.fine("Trying ImageIO");

        SortedMap<Integer, RenderedImage> images = loadImageIO(imgFile, id, null);

        if (images == null) {
            String extension = FileUtil.getExtension(imgFile);

            if (extension.equalsIgnoreCase(".pdf")) {
                images = loadPDF(imgFile, id);
            } else {
                logger.fine("Using JAI");
                images = loadJAI(imgFile);
            }
        }

        if (images == null) {
            logger.warning("Unable to load any image from {0}", imgFile);
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
     * @param id      if not null, specifies (counted from 1) which single
     *                image is desired
     * @param idUser  if not null, identity used in the returned map
     * @return a map of images, or null if failed to load
     */
    private static SortedMap<Integer, RenderedImage> loadImageIO (File imgFile,
                                                                  Integer id,
                                                                  Integer idUser)
    {
        logger.fine("loadImageIO {0} id:{1}", imgFile, id);

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
                reader.setInput(stream, false);

                int imageCount = reader.getNumImages(true);

                if (imageCount > 1) {
                    logger.info("{0} contains {1} images",
                            imgFile.getName(), imageCount);
                }

                SortedMap<Integer, RenderedImage> images = new TreeMap<>();

                for (int i = 1; i <= imageCount; i++) {
                    if ((id == null) || (id == i)) {
                        BufferedImage img = reader.read(i - 1);
                        int identity = idUser != null ? idUser : i;
                        images.put(identity, img);
                        logger.info("Loaded image #{0} ({1} x {2})",
                                identity, img.getWidth(), img.getHeight());
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
     * Try to load an image, using JAI.
     * This seems limited to a single image, thus no id parameter is to be
     * provided.
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
     * Load a sequence of images out of a PDF file.
     * We spawn a Ghostscript subprocess to convert PDF to TIFF and then
     * load the temporary TIFF file via loadImageIO().
     *
     * @param imgFile the input PDF file
     * @param id      if not null, specifies (counted from 1) which single image
     *                is desired
     * @return a map of images, or null if failed to load
     */
    private static SortedMap<Integer, RenderedImage> loadPDF (File imgFile,
                                                              Integer id)
    {
        logger.fine("loadPDF {0} id:{1}", imgFile, id);

        // Create a temporary tiff file from the PDF input
        Path temp = null;
        try {
            temp = Files.createTempFile("pic-", ".tif");
        } catch (IOException ex) {
            logger.warning("Cannot create temporary file " + temp, ex);
            return null;
        }

        // Arguments for Ghostscript
        List<String> gsArgs = new ArrayList<>();
        gsArgs.add(getGhostscriptExec());
        gsArgs.add("-dQUIET");
        gsArgs.add("-dNOPAUSE");
        gsArgs.add("-dBATCH");
        gsArgs.add("-dSAFER");
        gsArgs.add("-sDEVICE=tiffscaled8");
        gsArgs.add("-r300");
        gsArgs.add("-sOutputFile=" + temp);
        if (id != null) {
            gsArgs.add("-dFirstPage=" + id);
            gsArgs.add("-dLastPage=" + id);
        }
        gsArgs.add(imgFile.toString());
        logger.fine("gsArgs:{0}", gsArgs);

        try {
            // Spawn Ghostscript process and wait for its completion
            new ProcessBuilder(gsArgs).start().waitFor();

            // Now load the temporary tiff file
            if (id != null) {
                return loadImageIO(temp.toFile(), 1, id);
            } else {
                return loadImageIO(temp.toFile(), null, null);
            }
        } catch (IOException | InterruptedException ex) {
            logger.warning("Error running Ghostscript " + gsArgs, ex);
            return null;
        } finally {
            try {
                Files.delete(temp);
            } catch (IOException ex) {
                logger.warning("Error deleting file " + temp, ex);
            }
        }
    }

    //--------------------//
    // getGhostscriptExec //
    //--------------------//
    /**
     * Report the path to proper Ghostscript executable
     * on this machine / environment.
     *
     * @return the path to Ghostscript executable
     */
    private static String getGhostscriptExec ()
    {
        File f;

        if (WellKnowns.LINUX || WellKnowns.MAC_OS_X) {
            // TODO: certainly modify this line
            return "gs";
        } else if (WellKnowns.WINDOWS) {
            if (WellKnowns.WINDOWS_64) {
                f = new File(WellKnowns.GS_FOLDER, "windows/x64/gswin64c.exe");
            } else {
                f = new File(WellKnowns.GS_FOLDER, "windows/x86/gswin32c.exe");
            }
            return f.toString();
        }

        return null;
    }
}
