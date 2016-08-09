//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  T e s s e r a c t O r d e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.text.tesseract;

import omr.WellKnowns;

import omr.text.FontInfo;
import omr.text.TextChar;
import omr.text.TextLine;
import omr.text.TextWord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bytedeco.javacpp.*;
import static org.bytedeco.javacpp.lept.*;
import static org.bytedeco.javacpp.tesseract.*;

import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageOutputStream;

/**
 * Class {@code TesseractOrder} carries a processing order submitted to Tesseract OCR
 * program.
 *
 * @author Hervé Bitteur
 */
public class TesseractOrder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TesseractOrder.class);

    /** To avoid repetitive warnings if OCR binding failed */
    private static boolean userWarned;

    static {
        IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(
                new com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi());
        registry.registerServiceProvider(
                new com.sun.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi());
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Serial number for this order. */
    private final int serial;

    /** Image label. */
    private final String label;

    /** Should we keep a disk copy of the image?. */
    private final boolean keepImage;

    /** Language specification. */
    private final String lang;

    /** Desired handling of layout. */
    private final int segMode;

    /** The dedicated API. */
    private TessBaseAPI api;

    /** The image being processed. */
    private final PIX image;

    //~ Constructors -------------------------------------------------------------------------------
    //
    //----------------//
    // TesseractOrder //
    //----------------//
    /**
     * Creates a new TesseractOrder object.
     *
     * @param label         A debugging label (such as sheet name or glyph id)
     * @param serial        A unique id for this order instance
     * @param keepImage     True to keep a disk copy of the image
     * @param lang          The language specification
     * @param segMode       The desired page segmentation mode
     * @param bufferedImage The image to process
     *
     * @throws UnsatisfiedLinkError When bridge to C++ could not be loaded
     * @throws IOException          When temporary Tiff buffer failed
     * @throws RuntimeException     When PIX image failed
     */
    public TesseractOrder (String label,
                           int serial,
                           boolean keepImage,
                           String lang,
                           int segMode,
                           BufferedImage bufferedImage)
            throws UnsatisfiedLinkError, IOException
    {
        this.label = label;
        this.serial = serial;
        this.keepImage = keepImage;
        this.lang = lang;
        this.segMode = segMode;

        // Build a PIX from the image provided
        ByteBuffer buf = toTiffBuffer(bufferedImage);
        buf.position(0);
        image = pixReadMemTiff(buf, buf.capacity(), 0);

        if (image == null) {
            logger.warn("Invalid image {}", label);
            throw new RuntimeException("Invalid image");
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    //---------//
    // process //
    //---------//
    /**
     * Actually allocate a Tesseract API and recognize the image.
     *
     * @return the sequence of lines found
     */
    public List<TextLine> process ()
    {
        try {
            //api = new TessBaseAPI(WellKnowns.OCR_FOLDER.toString());
            api = new TessBaseAPI();

            // Init API with proper language
            if (api.Init(WellKnowns.OCR_FOLDER.toString(), lang) != 0) {
                logger.warn("Could not initialize Tesseract with lang {}", lang);

                return finish(null);
            }

            // Set API image
            api.SetImage(image);

            // Perform layout analysis according to segmentation mode
            api.SetPageSegMode(segMode);
            api.AnalyseLayout();

            // Perform image recognition
            if (api.Recognize(null) != 0) {
                logger.warn("Error in Tesseract recognize");

                return finish(null);
            }

            // Extract lines
            return finish(getLines());
        } catch (UnsatisfiedLinkError ex) {
            if (!userWarned) {
                logger.warn("Could not link Tesseract engine", ex);
                logger.warn("java.library.path=" + System.getProperty("java.library.path"));
                userWarned = true;
            }

            throw new RuntimeException(ex);
        }
    }

    //--------//
    // finish //
    //--------//
    /**
     * Convenient way to cleanup Tesseract resources while ending the current processing
     *
     * @param lines the lines found, if any
     * @return the lines found, if nay
     */
    private List<TextLine> finish (List<TextLine> lines)
    {
        if (image != null) {
            pixDestroy(image);
        }

        if (api != null) {
            api.End();
        }

        return lines;
    }

    //---------//
    // getFont //
    //---------//
    /**
     * Map Tesseract3 font attributes to our own FontInfo class.
     *
     * @param rit ResultIterator to query for font attributes out of OCR
     * @return our FontInfo structure, or null
     */
    private FontInfo getFont (ResultIterator rit)
    {
        BoolPointer is_bold = new BoolPointer(0);
        BoolPointer is_italic = new BoolPointer(0);
        BoolPointer is_underlined = new BoolPointer(0);
        BoolPointer is_monospace = new BoolPointer(0);
        BoolPointer is_serif = new BoolPointer(0);
        BoolPointer is_smallcaps = new BoolPointer(0);
        IntPointer pointSize = new IntPointer(0);
        IntPointer font_id = new IntPointer(0);

        String fontName = null;

        BytePointer bp = rit.WordFontAttributes(
                is_bold,
                is_italic,
                is_underlined,
                is_monospace,
                is_serif,
                is_smallcaps,
                pointSize,
                font_id);

        // don't try to decode fontName from null bytepointer!
        if (bp != null) {
            fontName = bp.getString();
        }

        if (fontName != null) {
            return new FontInfo(
                    is_bold.get(),
                    is_italic.get(),
                    is_underlined.get(),
                    is_monospace.get(),
                    is_serif.get(),
                    is_smallcaps.get(),
                    pointSize.get(),
                    fontName);
        } else {
            return null;
        }
    }

    private Rectangle BoundingBox (PageIterator it,
                                   int level)
    {
        IntPointer left = new IntPointer(0);
        IntPointer top = new IntPointer(0);
        IntPointer right = new IntPointer(0);
        IntPointer bottom = new IntPointer(0);

        if (it.BoundingBox(level, left, top, right, bottom)) {
            return new Rectangle(
                    left.get(),
                    top.get(),
                    right.get() - left.get(),
                    bottom.get() - top.get());
        } else {
            return null;
        }
    }

    private Line2D Baseline (ResultIterator rit,
                             int level)
    {
        IntPointer x1 = new IntPointer(0);
        IntPointer y1 = new IntPointer(0);
        IntPointer x2 = new IntPointer(0);
        IntPointer y2 = new IntPointer(0);

        if (rit.Baseline(level, x1, y1, x2, y2)) {
            return new Line2D.Double(x1.get(), y1.get(), x2.get(), y2.get());
        } else {
            return null;
        }
    }

    //----------//
    // getLines //
    //----------//
    /**
     * Build the hierarchy of TextLine / TextWord / TextChar instances out of the raw
     * results of OCR recognition.
     *
     * @return the sequence of lines
     */
    private List<TextLine> getLines ()
    {
        final ResultIterator it = api.GetIterator();
        final List<TextLine> lines = new ArrayList<TextLine>(); // All lines built so far
        TextLine line = null; // The line being built
        TextWord word = null; // The word being built
        int nextLevel;

        try {
            do {
                nextLevel = RIL_SYMBOL;

                // SKip empty stuff
                if (it.Empty(RIL_SYMBOL)) {
                    continue;
                }

                // Start of line?
                if (it.IsAtBeginningOf(RIL_TEXTLINE)) {
                    line = new TextLine();
                    logger.debug("{} {}", label, line);
                    lines.add(line);
                }

                // Start of word?
                if (it.IsAtBeginningOf(RIL_WORD)) {
                    FontInfo fontInfo = getFont(it);

                    if (fontInfo == null) {
                        logger.debug("No font info on {}", label);
                        nextLevel = RIL_WORD; // skip words without font info
                        continue;
                    }

                    word = new TextWord(
                            BoundingBox(it, RIL_WORD),
                            it.GetUTF8Text(RIL_WORD).getString(),
                            Baseline(it, RIL_WORD),
                            it.Confidence(RIL_WORD) / 100.0,
                            fontInfo,
                            line);
                    logger.debug("    {}", word);
                    line.appendWord(word);

                    // // Heuristic... (just to test)
                    // boolean isDict = it.WordIsFromDictionary();
                    // boolean isNumeric = it.WordIsNumeric();
                    // boolean isLatin = encoder.canEncode(wordContent);
                    // int conf = (int) Math.rint(it.Confidence(WORD));
                    // int len = wordContent.length();
                    // boolean isValid = isLatin
                    //         && (conf >= 80
                    //   || (conf >= 50 && ((isDict && len > 1) || isNumeric)));
                }

                // Char/symbol to be processed
                word.addChar(
                        new TextChar(BoundingBox(it, RIL_SYMBOL), it.GetUTF8Text(RIL_SYMBOL).getString()));
            } while (it.Next(nextLevel));

            return lines;
        } catch (Exception ex) {
            logger.warn("Error decoding tesseract output", ex);

            return null;
        } finally {
            it.deallocate();
        }
    }

    //--------------//
    // toTiffBuffer //
    //--------------//
    /**
     * Convert the given image into a TIFF-formatted ByteBuffer for
     * passing it directly to Tesseract.
     * A copy of the tiff buffer can be saved on disk, if so desired.
     *
     * @param image the input image
     * @return a buffer in TIFF format
     */
    private ByteBuffer toTiffBuffer (BufferedImage image)
            throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (final ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("tiff").
                    next();
            writer.setOutput(ios);
            writer.write(image);
        }

        ByteBuffer buf = ByteBuffer.allocate(baos.size());
        byte[] bytes = baos.toByteArray();
        buf.put(bytes);

        // Should we keep a local copy of this buffer on disk?
        if (keepImage) {
            String name = String.format("%03d-", serial) + ((label != null) ? label : "");
            Path path = WellKnowns.TEMP_FOLDER.resolve(name + ".tif");

            // Make sure the TEMP directory exists
            if (!Files.exists(WellKnowns.TEMP_FOLDER)) {
                Files.createDirectories(WellKnowns.TEMP_FOLDER);
            }

            try (final FileOutputStream fos = new FileOutputStream(path.toFile())) {
                fos.write(bytes);
            }
        }

        return buf;
    }
}
