//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T e s s e r a c t O C R                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.text.tesseract;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Scale;

import omr.text.OCR;
import omr.text.TextChar;
import omr.text.TextLine;
import omr.text.TextWord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static tesseract.TessBridge.*;
import tesseract.TessBridge.TessBaseAPI.SegmentationMode;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class {@code TesseractOCR} is an OCR service built on the Google Tesseract engine.
 *
 * <p>
 * It relies on <b>tesseract3</b> C++ program, accessed through a <b>JavaCPP</b>-based bridge.</p>
 *
 * @author Hervé Bitteur
 */
public class TesseractOCR
        implements OCR
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TesseractOCR.class);

    /** Singleton. */
    private static final OCR INSTANCE = new TesseractOCR();

    /** Latin encoder, to check character validity. (not used yet) */
    private static final CharsetEncoder encoder = Charset.forName("iso-8859-1").newEncoder();

    //~ Instance fields ----------------------------------------------------------------------------
    //
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
    //
    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the service singleton.
     *
     * @return the TesseractOCR service instance
     */
    public static OCR getInstance ()
    {
        return INSTANCE;
    }

    //--------------//
    // getLanguages //
    //--------------//
    @Override
    public Set<String> getLanguages ()
    {
        if (isAvailable()) {
            try {
                String[] langs = TessBaseAPI.getInstalledLanguages(WellKnowns.OCR_FOLDER.toString());

                return new TreeSet<String>(Arrays.asList(langs));
            } catch (Throwable ex) {
                logger.warn("Error in loading Tesseract languages", ex);
                throw new UnavailableOcrException();
            }
        }

        return Collections.emptySet();
    }

    //-------------//
    // isAvailable //
    //-------------//
    @Override
    public boolean isAvailable ()
    {
        return constants.useOCR.isSet();
    }

    //-----------//
    // recognize //
    //-----------//
    @Override
    public List<TextLine> recognize (int interline,
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
            TesseractOrder order = new TesseractOrder(
                    label,
                    serial.incrementAndGet(),
                    constants.keepImages.isSet(),
                    languageCode,
                    getMode(layoutMode),
                    bufferedImage);

            // Process the order
            List<TextLine> lines = order.process();

            // Post-processing
            if (lines != null) {
                final int maxDashWidth = (int) Math.rint(
                        interline * constants.maxDashWidth.getValue());

                for (TextLine line : lines) {
                    // Chars: Fix long "—" vs short "-"
                    for (TextWord word : line.getWords()) {
                        boolean updated = false;

                        for (TextChar ch : word.getChars()) {
                            String charValue = ch.getValue();

                            if (charValue.equals("—") && (ch.getBounds().width <= maxDashWidth)) {
                                ch.setValue("-");
                                updated = true;
                            }
                        }

                        if (updated) {
                            word.checkValue(); // So that word value is consistent with its chars
                        }
                    }

                    // Translate relative coordinates to absolute ones?
                    if (topLeft != null) {
                        line.translate(topLeft.x, topLeft.y);
                    }
                }
            }

            return lines;
        } catch (IOException ex) {
            logger.warn("Could not create OCR order", ex);

            return null;
        } catch (UnsatisfiedLinkError ex) {
            logger.warn("OCR link error", ex);
            throw new UnavailableOcrException();
        }
    }

    //---------//
    // getMode //
    //---------//
    /**
     * Map the OCR layout mode to Tesseract segmentation mode.
     *
     * @param layoutMode the desired OCR layout mode
     * @return the corresponding Tesseract segmentation mode
     */
    private SegmentationMode getMode (LayoutMode layoutMode)
    {
        switch (layoutMode) {
        case MULTI_BLOCK:
            return SegmentationMode.AUTO;

        default:
        case SINGLE_BLOCK:
            return SegmentationMode.SINGLE_BLOCK;
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean useOCR = new Constant.Boolean(
                true,
                "Should we use the OCR feature?");

        private final Constant.Boolean keepImages = new Constant.Boolean(
                false,
                "Should we keep the images sent to Tesseract?");

        private final Scale.Fraction maxDashWidth = new Scale.Fraction(
                1.0,
                "Maximum width for a dash character");
    }
}
