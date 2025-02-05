//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  T e s s e r a c t O r d e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.text.tesseract;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.text.OcrUtil;
import org.audiveris.omr.text.TextChar;
import org.audiveris.omr.text.TextLine;
import org.audiveris.omr.text.TextWord;
import static org.audiveris.omr.text.tesseract.TesseractOCR.LANGUAGE_FILE_EXT;

import org.bytedeco.javacpp.BoolPointer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.leptonica.PIX;
import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
import static org.bytedeco.leptonica.global.leptonica.pixReadMemTiff;
import org.bytedeco.tesseract.ResultIterator;
import org.bytedeco.tesseract.TessBaseAPI;
import static org.bytedeco.tesseract.global.tesseract.OEM_TESSERACT_ONLY;
import static org.bytedeco.tesseract.global.tesseract.RIL_SYMBOL;
import static org.bytedeco.tesseract.global.tesseract.RIL_TEXTLINE;
import static org.bytedeco.tesseract.global.tesseract.RIL_WORD;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageOutputStream;

/**
 * Class <code>TesseractOrder</code> carries a processing order submitted to Tesseract OCR
 * library.
 *
 * @author Hervé Bitteur
 */
public class TesseractOrder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TesseractOrder.class);

    /** To specify UTF-8 encoding. */
    private static final String UTF8 = "UTF-8";

    /** Tesseract variable name for white list. */
    private static final String WHITE_LIST_NAME = "tessedit_char_whitelist";

    /** Tesseract variable name for black list. */
    private static final String BLACK_LIST_NAME = "tessedit_char_blacklist";

    /** To avoid repetitive warnings if OCR binding failed. */
    private static volatile boolean userWarned;

    static {
        IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(
                new com.github.jaiimageio.impl.plugins.tiff.TIFFImageWriterSpi());
        registry.registerServiceProvider(
                new com.github.jaiimageio.impl.plugins.tiff.TIFFImageReaderSpi());
    }

    //~ Instance fields ----------------------------------------------------------------------------

    /** Containing sheet. */
    private final Sheet sheet;

    /** Serial number for this order. */
    private final int serial;

    /** Image label. */
    private final String label;

    /** Should we keep a disk copy of the image?. */
    private final boolean saveImage;

    /** Language(s) specification. */
    private final String langSpec;

    /** Desired handling of layout. */
    private final int segMode;

    /** The dedicated API. */
    private TessBaseAPI api;

    /** The image being processed. */
    private final PIX image;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new TesseractOrder object.
     *
     * @param sheet         the containing sheet
     * @param label         A debugging label (such as sheet name or glyph id)
     * @param serial        A unique id for this order instance
     * @param saveImage     True to keep a disk copy of the image
     * @param langSpec      The language(s) specification
     * @param segMode       The desired page segmentation mode
     * @param bufferedImage The image to process
     * @throws UnsatisfiedLinkError When bridge to C++ could not be loaded
     * @throws IOException          When temporary Tiff buffer failed
     * @throws RuntimeException     When PIX image failed
     */
    public TesseractOrder (Sheet sheet,
                           String label,
                           int serial,
                           boolean saveImage,
                           String langSpec,
                           int segMode,
                           BufferedImage bufferedImage)
            throws UnsatisfiedLinkError, IOException
    {
        this.sheet = sheet;
        this.label = label;
        this.serial = serial;
        this.saveImage = saveImage;
        this.langSpec = langSpec;
        this.segMode = segMode;

        // Build a PIX from the image provided
        final ByteBuffer buf = toTiffBuffer(bufferedImage);
        buf.position(0);
        image = pixReadMemTiff(buf, buf.capacity(), 0);

        if (image == null) {
            logger.warn("Invalid image {}", label);
            throw new RuntimeException("Invalid image");
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

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

    //-------------//
    // getBaseline //
    //-------------//
    /**
     * Report the baseline of the provided OCR'd item.
     *
     * @param rit   iterator on results structure
     * @param level desired level (word)
     * @return item baseline as a Line2D or null
     */
    private Line2D getBaseline (ResultIterator rit,
                                int level)
    {
        final IntPointer x1 = new IntPointer(0);
        final IntPointer y1 = new IntPointer(0);
        final IntPointer x2 = new IntPointer(0);
        final IntPointer y2 = new IntPointer(0);

        if (rit.Baseline(level, x1, y1, x2, y2)) {
            return new Line2D.Double(x1.get(), y1.get(), x2.get(), y2.get());
        }

        return null;
    }

    //----------------//
    // getBoundingBox //
    //----------------//
    /**
     * Report the bounding box of the provided OCR'd item.
     *
     * @param it    iterator on results structure
     * @param level desired level (word or char/symbol)
     * @return item bounding box as a Rectangle or null
     */
    private Rectangle getBoundingBox (ResultIterator it,
                                      int level)
    {
        final IntPointer left = new IntPointer(0);
        final IntPointer top = new IntPointer(0);
        final IntPointer right = new IntPointer(0);
        final IntPointer bottom = new IntPointer(0);

        if (it.BoundingBox(level, left, top, right, bottom)) {
            return new Rectangle(
                    left.get(),
                    top.get(),
                    right.get() - left.get(),
                    bottom.get() - top.get());
        }

        return null;
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
        final BoolPointer is_bold = new BoolPointer(0);
        final BoolPointer is_italic = new BoolPointer(0);
        final BoolPointer is_underlined = new BoolPointer(0);
        final BoolPointer is_monospace = new BoolPointer(0);
        final BoolPointer is_serif = new BoolPointer(0);
        final BoolPointer is_smallcaps = new BoolPointer(0);
        final IntPointer pointSize = new IntPointer(0);
        final IntPointer font_id = new IntPointer(0);

        final BytePointer bp = rit.WordFontAttributes(
                is_bold,
                is_italic,
                is_underlined,
                is_monospace,
                is_serif,
                is_smallcaps,
                pointSize,
                font_id);

        if (bp == null) {
            return null;
        }

        final String fontName = bp.getString();

        if (fontName == null) {
            return null;
        }

        return new FontInfo(
                is_bold.get(),
                is_italic.get(),
                is_underlined.get(),
                is_monospace.get(),
                is_serif.get(),
                is_smallcaps.get(),
                pointSize.get(),
                fontName);
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
        final List<TextLine> lines = new ArrayList<>(); // All lines built so far
        TextLine line = null; // The line being built
        TextWord word = null; // The word being built
        int nextLevel;

        try {
            do {
                nextLevel = RIL_SYMBOL;

                // Skip empty stuff
                if (it.Empty(RIL_SYMBOL)) {
                    continue;
                }

                // Start of line?
                if (it.IsAtBeginningOf(RIL_TEXTLINE)) {
                    line = new TextLine(sheet);
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
                            sheet,
                            getBoundingBox(it, RIL_WORD),
                            it.GetUTF8Text(RIL_WORD).getString(UTF8),
                            getBaseline(it, RIL_WORD),
                            it.Confidence(RIL_WORD) / 100.0,
                            fontInfo,
                            line);
                    logger.debug("   {}", word);
                    line.appendWord(word);
                }

                // Char/symbol to be processed
                wordAddChars(
                        word,
                        getBoundingBox(it, RIL_SYMBOL),
                        it.GetUTF8Text(RIL_SYMBOL).getString(UTF8));
            } while (it.Next(nextLevel));

            lines.removeIf( (l) -> l.getValue().equals(" "));

            return lines;
        } catch (UnsupportedEncodingException ex) {
            logger.warn("Error decoding tesseract output", ex);

            return null;
        } finally {
            it.deallocate();
        }
    }

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
        if (!OcrUtil.getOcr().isAvailable()) {
            return Collections.emptyList();
        }

        try {
            api = new TessBaseAPI();

            // Init API with proper language
            final Path ocrFolder = TesseractOCR.getInstance().getOcrFolder();

            if (logger.isDebugEnabled()) {
                logger.info("ocrFolder: {}", ocrFolder);
                final File langsDir = ocrFolder.toFile();
                for (File file : langsDir.listFiles()) {
                    if (file.toString().endsWith(LANGUAGE_FILE_EXT)) {
                        logger.info("Lang file: {} bytes: {}", file, file.length());
                    }
                }
            }

            if (api.Init(ocrFolder.toString(), langSpec, OEM_TESSERACT_ONLY) != 0) {
                logger.warn(
                        "TesseractOrder. Could not initialize TessBaseAPI languages: {} in legacy mode",
                        langSpec);

                return finish(null);
            }

            // Set character white list?
            if (!constants.whiteList.getValue().isBlank()) {
                if (!api.SetVariable(WHITE_LIST_NAME, constants.whiteList.getValue())) {
                    logger.error("Error setting Tesseract variable {}", WHITE_LIST_NAME);
                }
            }

            // Set character black list?
            if (!constants.blackList.getValue().isBlank()) {
                if (!api.SetVariable(BLACK_LIST_NAME, constants.blackList.getValue())) {
                    logger.error("Error setting Tesseract variable {}", BLACK_LIST_NAME);
                }
            }

            // Set API image
            api.SetImage(image);

            // Specify image resolution (experimental)
            if (constants.typicalImageResolution.getValue() != -1) {
                api.SetSourceResolution(constants.typicalImageResolution.getValue());
            }

            // Perform layout analysis according to segmentation mode
            api.SetPageSegMode(segMode);
            api.AnalyseLayout();

            // Perform image recognition
            final int recognizeResult = api.Recognize(null);

            if (recognizeResult != 0) {
                logger.warn("Error in Tesseract recognize, result: {}", recognizeResult);

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

    //--------------//
    // toTiffBuffer //
    //--------------//
    /**
     * Convert the given image into a TIFF-formatted ByteBuffer for passing it directly
     * to Tesseract.
     * <p>
     * A copy of the tiff buffer can be saved on disk, if so desired.
     *
     * @param image the input image
     * @return a buffer in TIFF format
     */
    private ByteBuffer toTiffBuffer (BufferedImage image)
        throws IOException
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            final ImageWriter writer = ImageIO.getImageWritersByFormatName("tiff").next();
            writer.setOutput(ios);
            writer.write(image);
        } catch (IOException ex) {
            logger.warn("Could not write image", ex);
        }

        final ByteBuffer buf = ByteBuffer.allocate(baos.size());
        final byte[] bytes = baos.toByteArray();
        buf.put(bytes);

        // Should we keep a local copy of this buffer on disk?
        if (saveImage) {
            final Path dirPath = WellKnowns.TEMP_FOLDER.resolve(label);

            // Make sure the target directory exists
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            final String name = String.format("text-%03d.tif", serial);
            final Path path = dirPath.resolve(name);
            try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
                fos.write(bytes);
            } catch (IOException ex) {
                logger.warn("Could not write to {}", path, ex);
            }
        }

        return buf;
    }

    /**
     * Add TextChar(s) to the provided word.
     * <p>
     * Beware, the 'value' may be longer than 1, for example: "sz" as symbol value
     *
     * @param word   the word to populate
     * @param bounds the char/symbol bounds
     * @param value  the char/symbol value
     */
    private void wordAddChars (TextWord word,
                               Rectangle bounds,
                               String value)
    {
        final int len = value.length();

        if (len == 1) {
            word.addChar(new TextChar(bounds, value)); // Normal case
        } else {
            final double meanCharWidth = (double) bounds.width / len;

            for (int i = 0; i < len; i++) {
                Rectangle cb = new Rectangle2D.Double(
                        bounds.x + (i * meanCharWidth),
                        bounds.y,
                        meanCharWidth,
                        bounds.height).getBounds();
                word.addChar(new TextChar(cb, value.substring(i, i + 1)));
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer typicalImageResolution = new Constant.Integer(
                "dpi",
                70,
                "Typical image resolution in DPI (disabled when set to -1)");

        private final Constant.String blackList = new Constant.String(
                "",
                "Character black list (disabled when empty)");

        private final Constant.String whiteList = new Constant.String(
                "", // abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.,'",
                "Character white list (disabled when empty)");
    }
}
