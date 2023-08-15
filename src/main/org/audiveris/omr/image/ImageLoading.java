//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     I m a g e L o a d i n g                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.image;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.util.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intarsys.cwt.awt.environment.CwtAwtGraphicsContext;
import de.intarsys.cwt.environment.IGraphicsContext;
import de.intarsys.pdf.content.CSContent;
import de.intarsys.pdf.parser.COSLoadException;
import de.intarsys.pdf.pd.PDDocument;
import de.intarsys.pdf.pd.PDPage;
import de.intarsys.pdf.platform.cwt.rendering.CSPlatformRenderer;
import de.intarsys.pdf.tools.kernel.PDFGeometryTools;
import de.intarsys.tools.locator.FileLocator;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Class <code>ImageLoading</code> handles the loading of one or several images out of an
 * input file.
 * <p>
 * It works in two phases:
 * <ol>
 * <li>An initial call to {@link #getLoader(Path)} tries to return a {@link Loader} instance that
 * fits the provided input file.</li>
 * <li>Then this Loader instance can be used via:
 * <ul>
 * <li>{@link Loader#getImageCount()} to know how many images are available in the input file,</li>
 * <li>{@link Loader#getImage(int)} to return any specific image,</li>
 * <li>{@link Loader#dispose()} to finally release any resources.</li>
 * </ul>
 * </ol>
 * This class leverages several software pieces, each with its own Loader subclass:
 * <ul>
 * <li><b>JPod</b> for PDF files. This replaces former use of GhostScript sub-process.</li>
 * <li><b>ImageIO</b> for all files except PDF.</li>
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

    //~ Static Methods -----------------------------------------------------------------------------

    //------------------//
    // getImageIOLoader //
    //------------------//
    /**
     * Try to use ImageIO.
     *
     * @param imgPath the provided input file
     * @return proper (ImageIO) loader or null if failed
     */
    private static Loader getImageIOLoader (Path imgPath)
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
        } catch (IOException ex) {
            logger.warn("ImageIO failed for " + imgPath, ex);

            return null;
        }
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
        // Avoid stupid errors
        if (!Files.exists(imgPath)) {
            logger.warn("File {} does not exist", imgPath);

            return null;
        }

        if (Files.isDirectory(imgPath)) {
            logger.warn("{} is a directory!", imgPath);

            return null;
        }

        String extension = FileUtil.getExtension(imgPath);
        Loader loader;

        if (extension.equalsIgnoreCase(".pdf")) {
            // Use JPod
            loader = getJPodLoader(imgPath);
        } else {
            // Try ImageIO
            loader = getImageIOLoader(imgPath);
        }

        if (loader == null) {
            logger.warn("Cannot find a loader for {}", imgPath);
        }

        return loader;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------------//
    // AbstractLoader //
    //----------------//
    private abstract static class AbstractLoader
            implements Loader
    {

        /** Count of images available in input file. */
        protected final int imageCount;

        AbstractLoader (int imageCount)
        {
            this.imageCount = imageCount;
        }

        protected void checkId (int id)
        {
            if ((id < 1) || (id > imageCount)) {
                throw new IllegalArgumentException("Invalid image id " + id);
            }
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
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer pdfResolution = new Constant.Integer(
                "DPI",
                300,
                "DPI resolution for PDF images");
    }

    //---------------//
    // ImageIOLoader //
    //---------------//
    private static class ImageIOLoader
            extends AbstractLoader
    {

        private final ImageReader reader;

        ImageIOLoader (ImageReader reader,
                       int imageCount)
        {
            super(imageCount);
            this.reader = reader;
        }

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

            return img;
        }
    }

    //------------//
    // JPodLoader //
    //------------//
    private static class JPodLoader
            extends AbstractLoader
    {

        private final PDDocument doc;

        JPodLoader (PDDocument doc,
                    int imageCount)
        {
            super(imageCount);
            this.doc = doc;
        }

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

            // desired scale = pdfResolution / default PDF resolution
            float scale = constants.pdfResolution.getValue() / 72.0f;

            // obtain relevant page parameters
            PDPage page = doc.getPageTree().getPageAt(id - 1);
            Rectangle2D rect = page.getCropBox().toNormalizedRectangle();
            int rotation = page.getRotate();
            logger.debug("Page #{} rotation: {}°", id, rotation);

            // swap width and height according with the rotation angle
            AffineTransform pageTransform = new AffineTransform();
            PDFGeometryTools.adjustTransform(pageTransform, rotation, rect);

            Point2D newDims = new Point2D.Double(rect.getWidth(), rect.getHeight());
            pageTransform.deltaTransform(newDims, newDims);

            double pageWidth = Math.abs(newDims.getX());
            double pageHeight = Math.abs(newDims.getY());

            BufferedImage image = new BufferedImage(
                    (int) (pageWidth * scale),
                    (int) (pageHeight * scale),
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
            transform.translate(0.0, -pageHeight);
            transform.concatenate(pageTransform);

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

            return image;
        }
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
}
