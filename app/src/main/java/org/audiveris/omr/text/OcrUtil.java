//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          O c r U t i l                                         //
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
package org.audiveris.omr.text;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.text.OCR.LayoutMode;
import org.audiveris.omr.text.tesseract.TesseractOCR;
import org.audiveris.omr.ui.symbol.TextFamily;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/**
 * Class <code>OcrUtil</code> gathers utility features related to OCR.
 *
 * @author Hervé Bitteur
 */
public abstract class OcrUtil
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(OcrUtil.class);

    /** The related OCR. */
    private static final OCR ocr = TesseractOCR.getInstance();

    //~ Constructors -------------------------------------------------------------------------------
    /** Not meant to be instantiated. */
    private OcrUtil ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------
    //--------//
    // getOcr //
    //--------//
    /**
     * Report the related OCR engine, if one is available.
     *
     * @return the available OCR engine, or null
     */
    public static OCR getOcr ()
    {
        return ocr;
    }

    //------//
    // scan //
    //------//
    /**
     * Scan the provided image for lines of text.
     * <p>
     * Tesseract OCR generally gives better results if the processed image exhibits white pixels
     * on the image contour, so here we (transparently) add a white margin around the buffer.
     *
     * @param image      the provided image
     * @param layoutMode MULTI_BLOCK or SINGLE_BLOCK
     * @param languages  languages specification
     * @param sheet      the related sheet
     * @param label      some label meant for debugging
     * @return the raw lines of text found, with coordinates relative to image origin
     */
    public static List<TextLine> scan (BufferedImage image,
                                       LayoutMode layoutMode,
                                       String languages,
                                       Sheet sheet,
                                       String label)
    {
        // Make sure we can use the OCR
        if (!ocr.isAvailable()) {
            logger.info("No OCR available");
            return Collections.emptyList();
        }

        // Make sure we have all the specified languages
        if (!ocr.supports(languages)) {
            logger.info("Missing support for '{}' language(s)", languages);
            return Collections.emptyList();
        }

        final int width = image.getWidth();
        final int height = image.getHeight();
        final Point origin = new Point(0, 0);

        // Add some white margin around the image?
        final int margin = constants.whiteMarginAdded.getValue();
        final BufferedImage bi;

        if (margin > 0) {
            bi = new BufferedImage(
                    width + (2 * margin),
                    height + (2 * margin),
                    BufferedImage.TYPE_BYTE_GRAY);

            // Background filled with white
            Graphics g = bi.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, bi.getWidth(), bi.getHeight());

            // Draw image with margins shift
            g.drawImage(image, margin, margin, null);
            origin.translate(-margin, -margin);
        } else {
            bi = image;
        }

        final List<TextLine> lines = ocr.recognize(sheet, bi, origin, languages, layoutMode, label);

        if (lines == null) {
            logger.info("No OCR'd lines");
            return Collections.emptyList();
        }

        Collections.sort(lines, TextLine.byOrdinate(sheet.getSkew()));

        final TextFamily family = sheet.getStub().getTextFamily();
        lines.forEach(line -> line.getWords().forEach(word -> word.adjustFontSize(family)));

        if (logger.isDebugEnabled()) {
            TextLine.dump("Raw OCR'd lines:", lines, constants.dumpWords.isSet());
        }

        return lines;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer whiteMarginAdded = new Constant.Integer(
                "pixels",
                10,
                "Margin of white pixels added around image to OCR");

        private final Constant.Boolean dumpWords = new Constant.Boolean(
                false,
                "Should we dump words when we dump lines?");
    }
}
