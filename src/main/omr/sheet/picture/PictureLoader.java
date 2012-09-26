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

import omr.WellKnowns;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;

import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;
import org.jpedal.fonts.FontMappings;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFRenderer;

import net.sf.ghost4j.Ghostscript;
import net.sf.ghost4j.document.DocumentException;
import net.sf.ghost4j.document.PDFDocument;
import net.sf.ghost4j.renderer.RendererException;
import net.sf.ghost4j.renderer.SimpleRenderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * <p>It leverages several software pieces: JAI, ImageIO, and PDF libraries.
 *
 * <p>Regarding PDF, we have not yet found the perfect solution that works in
 * all cases. Several libraries are made available here and can be switched
 * at run time by setting the value of constant {@code PDFLibrary} between:
 * PDFRenderer, PDFBox or JPedal.
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
    private static final Logger logger = Logger.getLogger(PictureLoader.class);

    /** Scaling for PDF rendering. */
    private static final float PDF_SCALING = 4.0f;

    /** Resolution for PDF rendering (scaling * screen resolution). */
    private static final int PDF_DPI = (int) PDF_SCALING * 72;

    /** Name of Ghostscript executable, if known. */
    private static String ghostscriptExecName = null;

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
     * If not, or if ImageIO has an error, a PDF library is used for files
     * ending with ".pdf" and JAI is used for all other files.
     *
     * @param imgFile the image file to load
     * @param id      if not null, specifies (counted from 1) which single image
     *                is desired
     * @return a sorted map of RenderedImage's (often but not always a
     *         BufferedImage), guaranteed not to be null, id counted from 1.
     * @throws IllegalArgumentException if file does not exist
     * @throws RuntimeException         if all libraries are unable to load the
     *                                  file
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
            logger.warning("Unable to load any image from " + imgFile);
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
     *
     * @param imgFile the input PDF file
     * @param id      if not null, specifies (counted from 1) which single image
     *                is desired
     * @return a map of images, or null if failed to load
     */
    private static SortedMap<Integer, RenderedImage> loadPDF (File imgFile,
                                                              Integer id)
    {
        switch (constants.PDFLibrary.getValue().toLowerCase()) {
        case "pdfrenderer":
            return try_PDFRenderer(imgFile, id);
        case "pdfbox":
            return try_PDFBox(imgFile, id);
        case "jpedal":
            return try_JPedal(imgFile, id);
        case "ghostscript":
            return try_Ghostscript(imgFile, id);
        case "gsprocess":
            return try_GSProcess(imgFile, id);
        default:
            logger.warning("Unknown PDF library: {0}",
                    constants.PDFLibrary.getValue());
            return null;
        }
    }

    //-----------------//
    // try_PDFRenderer //
    //-----------------//
    private static SortedMap<Integer, RenderedImage> try_PDFRenderer (
            File imgFile,
            Integer id)
    {
        logger.fine("Trying PDFRenderer");

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
                    if ((id == null) || (id == i)) {
                        PDFPage page = pdfFile.getPage(i); // 1-based

                        // Get the dimensions
                        Rectangle2D bbox = page.getBBox();
                        Rectangle rect = new Rectangle(
                                0,
                                0,
                                (int) (bbox.getWidth() * PDF_SCALING),
                                (int) (bbox.getHeight() * PDF_SCALING));

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
                                imgFile.getName(), i,
                                img.getWidth(), img.getHeight());
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

    //------------//
    // try_PDFBox //
    //------------//
    private static SortedMap<Integer, RenderedImage> try_PDFBox (File imgFile,
                                                                 Integer id)
    {
        logger.fine("Trying PDFBox");
        try {
            PDDocument doc = null;

            try {
                doc = PDDocument.load(imgFile.getCanonicalPath());

                // Image count
                int imageCount = doc.getNumberOfPages();

                if (imageCount > 1) {
                    logger.info("{0} contains {1} images",
                            imgFile.getName(), imageCount);
                }

                PDDocumentCatalog catalog = doc.getDocumentCatalog();
                List<PDPage> pages = (List<PDPage>) catalog.getAllPages();

                SortedMap<Integer, RenderedImage> images = new TreeMap<>();

                for (int i = 1; i <= imageCount; i++) {
                    if ((id == null) || (id == i)) {
                        PDPage page = pages.get(i - 1); // 0-based
                        BufferedImage img = page.convertToImage(
                                BufferedImage.TYPE_BYTE_GRAY, PDF_DPI);
                        images.put(i, img);

                        logger.info("{0} loaded image #{1} ({2} x {3})",
                                imgFile.getName(), i,
                                img.getWidth(), img.getHeight());
                    }
                }

                return images;
            } finally {
                if (doc != null) {
                    doc.close();
                }
            }
        } catch (IOException ex) {
            logger.warning("Unable to load PDF", ex);
            return null;
        }
    }

    //------------//
    // try_JPedal //
    //------------//
    private static SortedMap<Integer, RenderedImage> try_JPedal (File imgFile,
                                                                 Integer id)
    {
        logger.fine("Trying JPedal");
        try {
            PdfDecoder decode_pdf = null;

            try {
                decode_pdf = new PdfDecoder(true);
                decode_pdf.openPdfFile(imgFile.getPath());

                //decode_pdf.setExtractionMode(255, 288, 4);
                FontMappings.setFontReplacements();
                decode_pdf.useHiResScreenDisplay(true);
                decode_pdf.getDPIFactory().setDpi(PDF_DPI);
                decode_pdf.setPageParameters(PDF_SCALING, 1);

                // Image count
                int imageCount = decode_pdf.getPageCount();

                if (imageCount > 1) {
                    logger.info("{0} contains {1} images",
                            imgFile.getName(), imageCount);
                }

                SortedMap<Integer, RenderedImage> images = new TreeMap<>();

                for (int i = 1; i <= imageCount; i++) {
                    if ((id == null) || (id == i)) {
                        BufferedImage img = decode_pdf.getPageAsImage(i); // 1-based
                        images.put(i, img);

                        logger.info("{0} loaded image #{1} ({2} x {3})",
                                imgFile.getName(), i,
                                img.getWidth(), img.getHeight());
                    }
                }

                return images;
            } finally {
                if (decode_pdf != null && decode_pdf.isOpen()) {
                    decode_pdf.closePdfFile();
                }
            }
        } catch (PdfException ex) {
            logger.warning("Unable to load PDF", ex);
            return null;
        }
    }

    //-----------------//
    // try_Ghostscript //
    //-----------------//
    private static SortedMap<Integer, RenderedImage> try_Ghostscript (
            File imgFile,
            Integer id)
    {
        logger.fine("Trying Ghostscript");

        PDFDocument document = null;
        try {
            // Load PDF document
            document = new PDFDocument();
            document.load(imgFile);

            // Image count
            int imageCount = document.getPageCount();

            if (imageCount > 1) {
                logger.info("{0} contains {1} images",
                        imgFile.getName(), imageCount);
            }

            // Create renderer
            SimpleRenderer renderer = new SimpleRenderer();
            renderer.setResolution(300);
            SortedMap<Integer, RenderedImage> images = new TreeMap<>();

            // Render
            if (id != null) {
                // Render just the desired page
                List<Image> imagesList = renderer.render(document, id - 1, id - 1); // 0-based
                BufferedImage img = (BufferedImage) imagesList.get(0);
                images.put(id, img);

                logger.info("{0} loaded image #{1} ({2} x {3})",
                        imgFile.getName(), id,
                        img.getWidth(), img.getHeight());
            } else {
                try {
                    // Render all pages
                    List<Image> imagesList = renderer.render(document);
                    for (int i = 1; i <= imageCount; i++) {
                        BufferedImage img = (BufferedImage) imagesList.get(i - 1); // 0-based
                        images.put(i, img);

                        logger.info("{0} loaded image #{1} ({2} x {3})",
                                imgFile.getName(), i,
                                img.getWidth(), img.getHeight());
                    }
                } catch (UnsatisfiedLinkError ex) {
                    logger.severe("Ghost4j cannot load Ghostscript library."
                                  + " Please check your PATH environment variable",
                            ex);
                    return null;
                }
            }

            // To fix memory leak
            Ghostscript.getInstance().setDisplayCallback(null);

            return images;

        } catch (IOException | DocumentException | RendererException ex) {
            logger.warning("Unable to load PDF", ex);
            return null;
        } finally {
        }
    }

    //---------------//
    // try_GSProcess //
    //---------------//
    private static SortedMap<Integer, RenderedImage> try_GSProcess (
            File imgFile,
            Integer id)
    {
        logger.fine("Trying Ghostscript process");

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
        ////gsArgs.add(execName);
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

        try {
            // Spawn Ghostscript process
            Process process = null;

            if (ghostscriptExecName == null) {
                // Try the possible names
                List<String> execNames = getGhostscriptNames();
                for (String execName : execNames) {
                    try {
                        process = spawnGhostcript(execName, gsArgs);
                    } catch (IOException ignored) {
                        logger.fine("IOException with {0}", execName);
                        continue;
                    }

                    // Remember correct name found
                    ghostscriptExecName = execName;
                    break;
                }

                if (ghostscriptExecName == null) {
                    logger.severe("No Ghostscript executable found among {0}."
                                  + " Please check your PATH environment variable",
                            execNames);
                    return null;
                }
            } else {
                try {
                    process = spawnGhostcript(ghostscriptExecName, gsArgs);
                } catch (IOException ex) {
                    logger.warning("IOException on Ghostscript process", ex);
                    return null;
                }
            }

            // Make sure the process has completed...
            process.waitFor();

            // Now load the temporary tiff file
            if (id != null) {
                return loadImageIO(temp.toFile(), 1, id);
            } else {
                return loadImageIO(temp.toFile(), null, null);
            }
        } catch (InterruptedException ex) {
            logger.warning("Ghostscript process interrupted", ex);
            return null;
        } finally {
            try {
                Files.delete(temp);
            } catch (IOException ex) {
                logger.warning("Error deleting file " + temp, ex);
            }
        }
    }

    //-----------------//
    // spawnGhostcript //
    //-----------------//
    /**
     * (Try to) launch the Ghostscript process.
     *
     * @param execName a possible name for executable
     * @param args     other arguments
     * @return the process launched, if any
     * @throws IOException if executable could not be found
     */
    private static Process spawnGhostcript (String execName,
                                            List<String> args)
            throws IOException
    {
        List<String> allArgs = new ArrayList<>();
        allArgs.add(execName);
        allArgs.addAll(args);
        logger.fine("Arguments: {0}", allArgs);
        ProcessBuilder builder = new ProcessBuilder(allArgs);

        return builder.start();
    }

    //---------------------//
    // getGhostscriptNames //
    //---------------------//
    /**
     * Report the sequence of possible names for Ghostscript executable
     * on this machine / environment.
     * <p>On Windows, we can have "gswin32c" and "gswin64c".
     * On Linux and Mac, it is named "gs".
     *
     * @return the sequence of possible names
     */
    private static List<String> getGhostscriptNames ()
    {
        if (WellKnowns.LINUX || WellKnowns.MAC_OS_X) {
            return Arrays.asList("gs");
        } else if (WellKnowns.WINDOWS) {
            boolean is64bit = System.getenv("ProgramFiles(x86)") != null;
            if (is64bit) {
                return Arrays.asList("gswin64c", "gswin32c");
            } else {
                return Arrays.asList("gswin32c");
            }
        }

        return Collections.EMPTY_LIST;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {

        Constant.String PDFLibrary = new Constant.String(
                "GSProcess",
                "Library: PDFRenderer, PDFBox, JPedal, Ghostscript or GSProcess");

    }
}
