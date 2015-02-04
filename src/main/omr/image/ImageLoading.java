//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     I m a g e L o a d i n g                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright (C) Brenton Partridge 2007-2008.   4
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.util.FileUtil;

import de.intarsys.cwt.awt.environment.CwtAwtGraphicsContext;
import de.intarsys.cwt.environment.IGraphicsContext;
import de.intarsys.pdf.content.CSContent;
import de.intarsys.pdf.parser.COSLoadException;
import de.intarsys.pdf.pd.PDDocument;
import de.intarsys.pdf.pd.PDPage;
import de.intarsys.pdf.platform.cwt.rendering.CSPlatformRenderer;
import de.intarsys.tools.locator.FileLocator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;

/**
 * Class {@code ImageLoading} handles the loading of one or several images out of an
 * input file.
 * <p>
 * It works in two phases:<ol>
 * <li>An initial call to {@link #getLoader(Path)} tries to return a {@link Loader} instance that
 * fits the provided input file.</li>
 * <li>Then this Loader instance can be used via:<ul>
 * <li>{@link Loader#getImageCount()} to know how many images are available in the input file,</li>
 * <li>{@link Loader#getImage(int)} to return any specific image,</li>
 * <li>{@link Loader#dispose()} to finally release any resources.</li>
 * </ul>
 * </ol>
 * This class leverages several software pieces, each with its own Loader subclass:<ul>
 * <li><b>JPod</b> for PDF files. This replaces former use of GhostScript sub-process.</li>
 * <li><b>ImageIO</b> for all files except PDF.</li>
 * <li><b>JAI</b> if ImageIO failed. Note that JAI can find only one image per file.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 * @author Maxim Poliakovski
 */
public abstract class ImageLoading
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ImageLoading.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * To disallow instantiation.
     */
    private ImageLoading ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // getImageIOLoader //
    //------------------//
    /**
     * Try to use ImageIO.
     *
     * @param imgPath the provided input file
     * @return proper (ImageIO) loader or null if failed
     */
    public static Loader getImageIOLoader (Path imgPath)
    {
        logger.debug("getImageIOLoader {}", imgPath);

        // Input stream
        ImageInputStream stream = null;

        try {
            stream = ImageIO.createImageInputStream(imgPath.toFile());
        } catch (IOException ex) {
            logger.warn("Unable to create ImageIO stream for " + imgPath, ex);
        }

        if (stream == null) {
            logger.debug("No ImageIO input stream provider for {}", imgPath);

            return null;
        }

        Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

        if (!readers.hasNext()) {
            logger.debug("No ImageIO reader for {}", imgPath);

            return null;
        }

        try {
            ImageReader reader = readers.next();
            reader.setInput(stream, false, true);

            int imageCount = reader.getNumImages(true);

            return new ImageIOLoader(reader, imageCount);
        } catch (Exception ex) {
            logger.warn("ImageIO failed for " + imgPath, ex);

            return null;
        }
    }

    //-----------//
    // getLoader //
    //-----------//
    /**
     * Build a proper loader instance dedicated to the provided image file.
     *
     * @param imgPath the provided image path
     * @return the loader instance or null if failed
     */
    public static Loader getLoader (Path imgPath)
    {
        String extension = FileUtil.getExtension(imgPath);
        Loader loader;

        if (extension.equalsIgnoreCase(".pdf")) {
            // Use JPod
            loader = getJPodLoader(imgPath);
        } else {
            // Try ImageIO
            loader = getImageIOLoader(imgPath);

            if (loader == null) {
                // Use JAI
                loader = getJaiLoader(imgPath);
            }
        }

        if (loader != null) {
            final int count = loader.getImageCount();
            logger.info("{} sheet{} in {}", count, ((count > 1) ? "s" : ""), imgPath);
        }

        return loader;
    }

    //---------------//
    // getJPodLoader //
    //---------------//
    /**
     * Try to use JPod.
     *
     * @param imgPath the provided (PDF) input file.
     * @return proper (JPod) loader or null if failed
     */
    private static Loader getJPodLoader (Path imgPath)
    {
        logger.debug("getJPodLoader {}", imgPath);

        PDDocument doc = null;

        try {
            FileLocator locator = new FileLocator(imgPath.toFile());
            doc = PDDocument.createFromLocator(locator);
        } catch (IOException ex) {
            logger.warn("Error opening pdf file " + imgPath, ex);
        } catch (COSLoadException ex) {
            logger.warn("Invalid pdf file " + imgPath, ex);
        }

        if (doc == null) {
            return null;
        }

        int imageCount = doc.getPageTree().getCount();

        return new JPodLoader(doc, imageCount);
    }

    //--------------//
    // getJaiLoader //
    //--------------//
    /**
     * Try to use JAI.
     *
     * @param imgPath the provided input file
     * @return proper (JAI) loader or null if failed
     */
    private static Loader getJaiLoader (Path imgPath)
    {
        logger.debug("getJaiLoader {}", imgPath);

        try {
            BufferedImage image = JAI.create("fileload", imgPath.toString()).getAsBufferedImage();

            if ((image != null) && (image.getWidth() > 0) && (image.getHeight() > 0)) {
                return new JaiLoader(image);
            }

            logger.debug("No image read by JAI for {}", imgPath);
        } catch (Exception ex) {
            logger.warn("JAI failed opening " + imgPath + " ", ex);
        }

        return null;
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //--------//
    // Loader //
    //--------//
    /**
     * A loader dedicated to an input file.
     */
    public static interface Loader
    {
        //~ Methods --------------------------------------------------------------------------------

        /**
         * Report whether all images has already been loaded.
         *
         * @return false if at least one image has not been loaded yet
         */
        boolean allImagesLoaded ();

        /**
         * Release any loader resources.
         */
        void dispose ();

        /**
         * Load the specific image.
         *
         * @param id specified image id (its index counted from 1)
         * @return the image, or null if failed
         * @throws IOException for any IO error
         */
        BufferedImage getImage (int id)
                throws IOException;

        /**
         * Report the count of images available in input file.
         *
         * @return the count of images
         */
        int getImageCount ();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Constant.Integer pdfResolution = new Constant.Integer(
                "DPI",
                300,
                "DPI resolution for PDF images");
    }

    //----------------//
    // AbstractLoader //
    //----------------//
    private abstract static class AbstractLoader
            implements Loader
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Count of images available in input file. */
        protected final int imageCount;

        /** Images already loaded. */
        protected final boolean[] loaded;

        //~ Constructors ---------------------------------------------------------------------------
        public AbstractLoader (int imageCount)
        {
            this.imageCount = imageCount;
            loaded = new boolean[imageCount];
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean allImagesLoaded ()
        {
            for (boolean b : loaded) {
                if (!b) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void dispose ()
        {
        }

        @Override
        public int getImageCount ()
        {
            return imageCount;
        }

        protected void checkId (int id)
        {
            if ((id < 1) || (id > imageCount)) {
                throw new IllegalArgumentException("Invalid image id " + id);
            }
        }

        protected void setLoaded (int id)
        {
            loaded[id - 1] = true;
        }
    }

    //---------------//
    // ImageIOLoader //
    //---------------//
    private static class ImageIOLoader
            extends AbstractLoader
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final ImageReader reader;

        //~ Constructors ---------------------------------------------------------------------------
        public ImageIOLoader (ImageReader reader,
                              int imageCount)
        {
            super(imageCount);
            this.reader = reader;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void dispose ()
        {
            reader.dispose();
        }

        @Override
        public BufferedImage getImage (int id)
                throws IOException
        {
            checkId(id);

            BufferedImage img = reader.read(id - 1);
            setLoaded(id);

            return img;
        }
    }

    //------------//
    // JPodLoader //
    //------------//
    private static class JPodLoader
            extends AbstractLoader
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final PDDocument doc;

        //~ Constructors ---------------------------------------------------------------------------
        public JPodLoader (PDDocument doc,
                           int imageCount)
        {
            super(imageCount);
            this.doc = doc;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void dispose ()
        {
            try {
                doc.close();
            } catch (IOException ex) {
                logger.warn("Could not close PDDocument", ex);
            }
        }

        @Override
        public BufferedImage getImage (int id)
                throws IOException
        {
            checkId(id);

            PDPage page = doc.getPageTree().getPageAt(id - 1);
            Rectangle2D rect = page.getCropBox().toNormalizedRectangle();
            float scale = constants.pdfResolution.getValue() / 72.0f;

            BufferedImage image = new BufferedImage(
                    Math.abs((int) (rect.getWidth() * scale)),
                    Math.abs((int) (rect.getHeight() * scale)),
                    BufferedImage.TYPE_BYTE_GRAY);

            Graphics2D g2 = (Graphics2D) image.getGraphics();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
            //g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
            //                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            //g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
            //   		    RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            //g2.setRenderingHint(RenderingHints.KEY_DITHERING,
            //		    RenderingHints.VALUE_DITHER_ENABLE);
            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            //g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
            //	            RenderingHints.VALUE_STROKE_PURE);
            //g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            //		    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            //g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
            //		    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            IGraphicsContext gctx = new CwtAwtGraphicsContext(g2);
            AffineTransform transform = gctx.getTransform();
            transform.scale(scale, -scale);
            transform.translate(-rect.getMinX(), -rect.getMaxY());
            gctx.setTransform(transform);
            gctx.setBackgroundColor(Color.WHITE);
            gctx.fill(rect);

            CSContent content = page.getContentStream();

            if (content != null) {
                CSPlatformRenderer renderer = new CSPlatformRenderer(null, gctx);
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_OFF);
                renderer.process(content, page.getResources());
            }

            setLoaded(id);

            return image;
        }
    }

    //-----------//
    // JaiLoader //
    //-----------//
    /**
     * A (degenerated) loader, since the only available image has already been cached.
     */
    private static class JaiLoader
            extends AbstractLoader
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final BufferedImage image; // The single image

        //~ Constructors ---------------------------------------------------------------------------
        public JaiLoader (BufferedImage image)
        {
            super(1); // JAI can return just one image
            this.image = image;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public BufferedImage getImage (int id)
                throws IOException
        {
            checkId(id);
            setLoaded(id);

            return image;
        }
    }
}
//    //
//    //------------//
//    // loadImages //
//    //------------//
//    /**
//     * Loads a sequence of BufferedImage instances from a file.
//     * <p>
//     * If ImageIO can read the file, it is used preferentially.
//     * If not, or if ImageIO has an error, a PDF loader is used for files
//     * ending with ".pdf" and JAI is used for all other files.
//     *
//     * @param imgFile the image file to load
//     * @param pages   if not null or empty, specifies (counted from 1) which
//     *                pages are desired. Otherwise all pages are loaded.
//     * @return a sorted map of RenderedImage's (often but not always a
//     *         BufferedImage), guaranteed not to be null, id counted from 1.
//     * @throws IllegalArgumentException if file does not exist
//     * @throws RuntimeException         if we are unable to load the file
//     */
//    public static SortedMap<Integer, BufferedImage> loadImages (File imgFile,
//                                                                SortedSet<Integer> pages)
//    {
//        if (!imgFile.exists()) {
//            throw new IllegalArgumentException(imgFile + " does not exist");
//        }
//
//        logger.info("Loading {} ...", imgFile);
//
//        logger.debug("Trying ImageIO");
//
//        SortedMap<Integer, BufferedImage> images = loadImageIO(imgFile, pages, 0);
//
//        if (images == null) {
//            String extension = FileUtil.getExtension(imgFile);
//
//            if (extension.equalsIgnoreCase(".pdf")) {
//                images = loadPDF(imgFile, pages);
//            } else {
//                logger.debug("Using JAI");
//                images = JaiLoader.loadJAI(imgFile);
//            }
//        }
//
//        if (images == null) {
//            logger.warn("Unable to load any image from {}", imgFile);
//        }
//
//        return images;
//    }
//
//    //---------//
//    // loadPDF //
//    //---------//
//    /**
//     * Load a sequence of images out of a PDF file.
//     * We spawn a Ghostscript subprocess to convert PDF to TIFF and then
//     * load the temporary TIFF file via loadImageIO().
//     *
//     * @param imgFile the input PDF file
//     * @param pages   if not null or empty, specifies (counted from 1) which
//     *                precise images are desired. Otherwise all pages are
//     *                loaded.
//     * @return a map of images, or null if failed to load
//     */
//    private static SortedMap<Integer, BufferedImage> loadPDF (File imgFile,
//                                                              SortedSet<Integer> pages)
//    {
//        logger.debug("loadPDF {} pages:{}", imgFile, pages);
//
//        // Create a temporary tiff file from the PDF input
//        Path temp = null;
//
//        try {
//            temp = Files.createTempFile("pic-", ".tif");
//        } catch (IOException ex) {
//            logger.warn("Cannot create temporary file " + temp, ex);
//
//            return null;
//        }
//
//        // Arguments for Ghostscript
//        List<String> gsArgs = new ArrayList<String>();
//        gsArgs.add(Ghostscript.getPath());
//        gsArgs.add("-dQUIET");
//        gsArgs.add("-dNOPAUSE");
//        gsArgs.add("-dBATCH");
//        gsArgs.add("-dSAFER");
//        gsArgs.add("-sDEVICE=" + constants.pdfDevice.getValue());
//        gsArgs.add("-r" + constants.pdfResolution.getValue());
//        gsArgs.add("-sOutputFile=" + temp);
//
//        if ((pages != null) && !pages.isEmpty()) {
//            gsArgs.add("-dFirstPage=" + pages.first());
//            gsArgs.add("-dLastPage=" + pages.last());
//        }
//
//        gsArgs.add(imgFile.toString());
//        logger.debug("gsArgs:{}", gsArgs);
//
//        try {
//            // Spawn Ghostscript process and wait for its completion
//            new ProcessBuilder(gsArgs).start().waitFor();
//
//            // Now load the temporary tiff file
//            if ((pages != null) && !pages.isEmpty()) {
//                return loadImageIO(temp.toFile(), pages, pages.first() - 1);
//            } else {
//                return loadImageIO(temp.toFile(), null, 0);
//            }
//        } catch (Exception ex) {
//            logger.warn("Error running Ghostscript " + gsArgs, ex);
//
//            return null;
//        } finally {
//            try {
//                Files.delete(temp);
//            } catch (IOException ex) {
//                logger.warn("Error deleting file " + temp, ex);
//            }
//        }
//    }
//
//    //--------------//
//    // getPdfLoader //
//    //--------------//
//    private static Loader getPdfLoader (Path imgPath)
//    {
//        logger.info("getPdfLoader {} ", imgPath);
//
//        // Create a temporary tiff file from the PDF input
//        Path tmpPath = null;
//
//        try {
//            tmpPath = Files.createTempFile("pic-", ".tif");
//            tmpPath.toFile().deleteOnExit();
//        } catch (IOException ex) {
//            logger.warn("Cannot create temporary file " + tmpPath, ex);
//
//            return null;
//        }
//
//        // Arguments for Ghostscript
//        List<String> gsArgs = new ArrayList<String>();
//        gsArgs.add(Ghostscript.getPath());
//        gsArgs.add("-dQUIET");
//        gsArgs.add("-dNOPAUSE");
//        gsArgs.add("-dBATCH");
//        gsArgs.add("-dSAFER");
//        gsArgs.add("-sDEVICE=" + constants.pdfDevice.getValue());
//        gsArgs.add("-r" + constants.pdfResolution.getValue());
//        gsArgs.add("-sOutputFile=" + tmpPath);
//
//        gsArgs.add(imgPath.toString());
//        logger.debug("gsArgs:{}", gsArgs);
//
//        try {
//            // Spawn Ghostscript process and wait for its completion
//            new ProcessBuilder(gsArgs).start().waitFor();
//
//            // Now load the temporary tiff file
//            Loader loader = getImageIOLoader(tmpPath);
//
//            if (loader == null) {
//                logger.warn("Cannot read temp file {}", tmpPath);
//            }
//
//            return loader;
//        } catch (Exception ex) {
//            logger.warn("Error running Ghostscript " + gsArgs, ex);
//
//            return null;
//        }
//    }

//            // This seems needed when called from getPdfLoader with a temp path!
//            if (stream == null) {
//                stream = ImageIO.createImageInputStream(Files.newInputStream(imgPath));
//            }
//
//    //-------------//
//    // loadImageIO //
//    //-------------//
//    /**
//     * Try to load a sequence of images, using ImageIO.
//     *
//     * @param imgFile the input image file
//     * @param pages   if not null or empty, specifies (counted from 1) which
//     *                precise pages are desired. Otherwise all pages are loaded.
//     * @param offset  specify offset on page ids.
//     * @return a map (id -> image), or null if failed to load
//     */
//    private static SortedMap<Integer, BufferedImage> loadImageIO (File               imgFile,
//                                                                  SortedSet<Integer> pages,
//                                                                  int                offset)
//    {
//        logger.debug("loadImageIO {} pages:{} offset:{}", imgFile, pages, offset);
//
//        // Input stream
//        ImageInputStream stream;
//
//        try {
//            stream = ImageIO.createImageInputStream(imgFile);
//        } catch (IOException ex) {
//            logger.warn("Unable to make ImageIO stream", ex);
//
//            return null;
//        }
//
//        if (stream == null) {
//            logger.debug("No ImageIO input stream provider");
//
//            return null;
//        }
//
//        try {
//            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
//
//            if (!readers.hasNext()) {
//                logger.debug("No ImageIO reader");
//
//                return null;
//            }
//
//            ImageReader reader = readers.next();
//
//            try {
//                reader.setInput(stream, false);
//
//                int imageCount = reader.getNumImages(true);
//
//                if (imageCount > 1) {
//                    logger.info("{} contains {} images", imgFile.getName(), imageCount);
//                }
//
//                SortedMap<Integer, BufferedImage> images = new TreeMap<Integer, BufferedImage>();
//
//                for (int i = 1; i <= imageCount; i++) {
//                    int id = i + offset;
//
//                    if ((pages == null) || pages.isEmpty() || (pages.contains(id))) {
//                        BufferedImage img = reader.read(i - 1);
//                        images.put(id, img);
//                        logger.info(
//                            "Loaded image #{} ({} x {})",
//                            id,
//                            img.getWidth(),
//                            img.getHeight());
//                    }
//                }
//
//                return images;
//            } catch (Exception ex) {
//                logger.warn("ImageIO failed", ex);
//
//                return null;
//            } finally {
//                reader.dispose();
//            }
//        } finally {
//            try {
//                stream.close();
//            } catch (IOException ignored) {
//            }
//        }
//    }
//
//        Constant.String pdfDevice = new Constant.String(
//                "tiff24nc",
//                "Ghostscript output device (tiff24nc or tiffscaled8)");
