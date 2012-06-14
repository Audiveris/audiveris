//----------------------------------------------------------------------------//
//                                                                            //
//                          T e s s e r a c t O C R                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text.tesseract;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.text.OCR;
import omr.glyph.text.OcrLine;

import omr.log.Logger;

import tesseract.Tesseract3Bridge.TessBaseAPI.SegmentationMode;
import static tesseract.Tesseract3Bridge.*;

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
import omr.glyph.facets.BasicGlyph;
import omr.glyph.text.BasicContent;
import omr.glyph.text.Sentence;
import omr.util.ClassUtil;

/**
 * Class {@code TesseractOCR} is an OCR service built on the Google
 * Tesseract engine.
 *
 * <p>It relies on the <b>tesseract3</b> C++ program, accessed through a
 * <b>JavaCPP</b>-based bridge.</p>
 *
 * @author Hervé Bitteur
 */
public class TesseractOCR
        implements OCR
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TesseractOCR.class);

    /** Singleton */
    private static final OCR INSTANCE = new TesseractOCR();

    /** Latin encoder, to check character validity */
    private static final CharsetEncoder encoder = Charset.forName("iso-8859-1").
            newEncoder();

    //~ Instance fields --------------------------------------------------------
    //
    /** To assign a serial number to each image processing order */
    private final AtomicInteger serial = new AtomicInteger(0);

    //~ Constructors -----------------------------------------------------------
    //
    //--------------//
    // TesseractOCR //
    //--------------//
    /**
     * Creates the TesseractOCR singleton.
     */
    private TesseractOCR ()
    {
        // Debug
        if (constants.keepImages.isSet()) {
            WellKnowns.TEMP_FOLDER.mkdir();
        }
    }

    //~ Methods ----------------------------------------------------------------
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

    //-----------------------//
    // getSupportedLanguages //
    //-----------------------//
    @Override
    public Set<String> getSupportedLanguages ()
    {
        if (isAvailable()) {
            try {
                String[] langs = TessBaseAPI.getInstalledLanguages(
                        WellKnowns.OCR_FOLDER.getPath());
                return new TreeSet<>(Arrays.asList(langs));
            } catch (Throwable ex) {
                logger.warning("Error in loading languages", ex);
                throw new OcrUnavailable();
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
    public List<OcrLine> recognize (BufferedImage bufferedImage,
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
            TesseractOrder order;

            // DEBUG
            String name = "";
            if (true) {
                StackTraceElement elem = ClassUtil.getCallingFrame(
                        BasicGlyph.class,
                        BasicContent.class,
                        Sentence.class,
                        TesseractOCR.class);

                if (elem != null) {
                    name += ("-" + elem.getMethodName());
                }
            }

            order = new TesseractOrder(label + name,
                                       serial.incrementAndGet(),
                                       constants.keepImages.isSet(),
                                       languageCode,
                                       getMode(layoutMode),
                                       bufferedImage);

            // Process the order
            List<OcrLine> lines = order.process();

            if (lines != null) {
                // Translate relative coordinates to absolute ones
                for (OcrLine ol : lines) {
                    ol.translate(topLeft.x, topLeft.y);
                }
            }

            return lines;

        } catch (IOException ex) {
            logger.warning("Could not create OCR order", ex);
            return null;
        } catch (UnsatisfiedLinkError ex) {
            logger.warning("OCR link error", ex);
            throw new OcrUnavailable();
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

    //----------------//
    // OcrUnavailable //
    //----------------//
    /**
     * Exception used to signal that no OCR is actually available.
     */
    public static class OcrUnavailable
            extends RuntimeException
    {
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean useOCR = new Constant.Boolean(
                true,
                "Should we use the OCR feature?");

        Constant.Boolean keepImages = new Constant.Boolean(
                false,
                "Should we keep the images sent to Tesseract?");
    }
}
