//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T e s s e r a c t O C R                                     //
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
package org.audiveris.omr.text.tesseract;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.text.OCR;
import org.audiveris.omr.text.TextLine;

import org.bytedeco.tesseract.StringVector;
import org.bytedeco.tesseract.TessBaseAPI;
import org.bytedeco.tesseract.global.tesseract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class <code>TesseractOCR</code> is an OCR service built on Google Tesseract engine.
 * <p>
 * It relies on <b>tesseract-ocr</b> C++ program, accessed through a <b>JavaCPP</b>-based bridge.
 *
 * @author Hervé Bitteur
 */
public class TesseractOCR
        implements OCR
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TesseractOCR.class);

    /** Latin encoder, to check character validity. (not used yet) */
    private static final CharsetEncoder encoder = Charset.forName("iso-8859-1").newEncoder();

    /** Specific name of folder where Tesseract language files are located. */
    private static final String TESSDATA = "tessdata";

    /** System environment variable pointing to TESSDATA location. */
    private static final String TESSDATA_PREFIX = "TESSDATA_PREFIX";

    /** Warning message when OCR folder cannot be found. */
    private static final String ocrNotFoundMsg = "Tesseract data could not be found. "
            + "Try setting " + TESSDATA_PREFIX + " environment variable to point to " + TESSDATA
            + " folder.";

    //~ Instance fields ----------------------------------------------------------------------------

    /** The folder where Tesseract OCR material is stored. */
    private Path OCR_FOLDER;

    /** Boolean to avoid any new search for OCR folder. */
    private boolean OCR_FOLDER_SEARCHED;

    /** To assign a serial number to each image processing order. */
    private final AtomicInteger serial = new AtomicInteger(0);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates the TesseractOCR singleton.
     */
    private TesseractOCR ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // findOcrFolder //
    //---------------//
    /**
     * Look for Tesseract TESSDATA folder, according to environment.
     *
     * @return TESSDATA folder found, perhaps null
     */
    private Path findOcrFolder ()
    {
        // First, try to use TESSDATA_PREFIX environment variable
        final String tessPrefix = System.getenv(TESSDATA_PREFIX);
        logger.info("{} value: {}", TESSDATA_PREFIX, tessPrefix);

        if (tessPrefix != null) {
            Path dir = Paths.get(tessPrefix);

            if (Files.isDirectory(dir)) {
                return dir;
            }
        }

        // Second, scan OS typical TESSDATA locations
        if (WellKnowns.WINDOWS) {
            return scanOcrLocations(new String[]
            {
                    Paths.get(System.getenv("ProgramFiles")).resolve("tesseract-ocr").toString(),
                    Paths.get(System.getenv("ProgramFiles(x86)")).resolve("tesseract-ocr")
                            .toString() //
            });
        } else if (WellKnowns.LINUX) {
            return scanOcrLocations(new String[]
            {
                    "/usr/share/tesseract-ocr", // Debian, Ubuntu and derivatives
                    "/usr/share", // OpenSUSE
                    "/usr/share/tesseract" // Fedora
            });
        } else if (WellKnowns.MAC_OS_X) {
            return scanOcrLocations(new String[]
            {
                    "/opt/local/share", // Macports
                    "/usr/local/opt/tesseract/share" // Homebrew
            });
        }

        logger.warn(ocrNotFoundMsg);

        return null;
    }

    //--------------//
    // getLanguages //
    //--------------//
    /**
     * Report the set of languages currently available for OCR.
     *
     * @return the set of available languages, perhaps empty.
     */
    @Override
    public Set<String> getLanguages ()
    {
        if (isAvailable()) {
            final Path ocrFolder = getOcrFolder();
            final TreeSet<String> set = new TreeSet<>();

            try {
                final TessBaseAPI api = new TessBaseAPI();

                if (api.Init(ocrFolder.toString(), "eng") == 0) {
                    final StringVector languages = new StringVector();
                    api.GetAvailableLanguagesAsVector(languages);

                    while (!languages.empty()) {
                        set.add(languages.pop_back().getString());
                    }
                } else {
                    logger.warn("Error in loading Tesseract languages");
                }

                api.End();

                return set;
            } catch (Throwable ex) {
                final String msg = "Error in loading Tesseract languages";
                logger.warn(msg);
                throw new UnavailableOcrException(msg, ex);
            }
        }

        return Collections.emptySet();
    }

    //------------------//
    // getMinConfidence //
    //------------------//
    @Override
    public double getMinConfidence ()
    {
        return constants.minConfidence.getValue();
    }

    //---------//
    // getMode //
    //---------//
    /**
     * Map the OCR layout mode to Tesseract segmentation mode.
     *
     * @param layoutMode the desired OCR layout mode (MULTI_BLOCK or SINGLE_BLOCK)
     * @return the corresponding Tesseract segmentation mode
     */
    private int getMode (LayoutMode layoutMode)
    {
        if (constants.forceSingleBlock.isSet()) {
            return tesseract.PSM_SINGLE_BLOCK;
        }

        switch (layoutMode) {
        case MULTI_BLOCK:
            return tesseract.PSM_AUTO;

        default:
        case SINGLE_BLOCK:
            return tesseract.PSM_SINGLE_BLOCK;
        }
    }

    //--------------//
    // getOcrFolder //
    //--------------//
    /**
     * Report the folder where Tesseract OCR data is available.
     *
     * @return the OCR folder
     */
    public Path getOcrFolder ()
    {
        if (!OCR_FOLDER_SEARCHED) {
            OCR_FOLDER_SEARCHED = true;
            OCR_FOLDER = findOcrFolder();
        }

        return OCR_FOLDER;
    }

    //----------//
    // identify //
    //----------//
    @Override
    public String identify ()
    {
        if (isAvailable()) {
            return "Tesseract OCR, version " + TessBaseAPI.Version().getString();
        } else {
            return OCR.NO_OCR;
        }
    }

    //-------------//
    // isAvailable //
    //-------------//
    @Override
    public boolean isAvailable ()
    {
        return constants.useOCR.isSet() && (getOcrFolder() != null);
    }

    //-----------//
    // recognize //
    //-----------//
    @Override
    public List<TextLine> recognize (Sheet sheet,
                                     BufferedImage bufferedImage,
                                     Point topLeft,
                                     String languageCode,
                                     LayoutMode layoutMode,
                                     String label)
    {
        // Make sure we have an OCR engine available
        if (!isAvailable()) {
            return null;
        }

        try {
            // Allocate a processing order
            final TesseractOrder order = new TesseractOrder(
                    sheet,
                    label,
                    serial.incrementAndGet(),
                    constants.saveImages.isSet(),
                    languageCode,
                    getMode(layoutMode),
                    bufferedImage);

            // Process the order
            final List<TextLine> lines = order.process();

            if ((topLeft != null) && (lines != null)) {
                // Translate topLeft-relative coordinates to origin-relative ones
                for (TextLine line : lines) {
                    line.translate(topLeft.x, topLeft.y);
                }
            }

            // Print raw lines, right out of Tesseract OCR, except " " lines
            if (lines != null) {
                for (TextLine textLine : lines) {
                    logger.debug("raw {}", textLine);
                }
            }

            return lines;
        } catch (IOException ex) {
            logger.warn("Could not create OCR order", ex);

            return null;
        } catch (UnsatisfiedLinkError ex) {
            final String msg = "OCR link error";
            logger.warn(msg);
            throw new UnavailableOcrException(msg, ex);
        }
    }

    //------------------//
    // scanOcrLocations //
    //------------------//
    /**
     * Scan the provided sequence of locations for a TESSDATA folder.
     *
     * @param locations the locations to scan
     * @return the first suitable location or null
     */
    private Path scanOcrLocations (String... locations)
    {
        for (String loc : locations) {
            final Path path = Paths.get(loc).resolve(TESSDATA);

            if (Files.exists(path)) {
                return path;
            }
        }

        logger.warn(ocrNotFoundMsg);

        return null;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class in application.
     *
     * @return the instance
     */
    public static TesseractOCR getInstance ()
    {
        return LazySingleton.INSTANCE;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean useOCR = new Constant.Boolean(
                true,
                "Should we use the OCR feature?");

        private final Constant.Boolean forceSingleBlock = new Constant.Boolean(
                false,
                "Should we force OCR to use PSM_SINGLE_BLOCK rather than PSM_AUTO?");

        private final Constant.Boolean saveImages = new Constant.Boolean(
                false,
                "Should we save on disk the images sent to Tesseract?");

        //
        //        private final Scale.Fraction maxDashWidth = new Scale.Fraction(
        //                1.0,
        //                "Maximum width for a dash character");
        //
        private final Constant.Double minConfidence = new Constant.Double(
                "0..1",
                0.65,
                "Minimum confidence for OCR validity");
    }

    //---------------//
    // LazySingleton //
    //---------------//
    private static class LazySingleton
    {
        static final TesseractOCR INSTANCE = new TesseractOCR();
    }
}
