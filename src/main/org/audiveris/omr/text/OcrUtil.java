//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          O c r U t i l                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.audiveris.omr.text.OCR.LayoutMode;
import org.audiveris.omr.text.tesseract.TesseractOCR;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Class {@code OcrUtil} gathers utility features related to OCR.
 *
 * @author Hervé Bitteur
 */
public abstract class OcrUtil
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The related OCR. */
    private static final OCR ocr = TesseractOCR.getInstance();

    //~ Constructors -------------------------------------------------------------------------------
    /** Not meant to be instantiated. */
    private OcrUtil ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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
     *
     * @param image      the provided image
     * @param margin     amount of white pixels added around the image (can be zero)
     * @param layoutMode MULTI_BLOCK or SINGLE_BLOCK
     * @param language   language specification
     * @param interline  scaling interline
     * @param label      some label meant for debugging
     * @return the raw lines of text found, with coordinates relative to image origin
     */
    public static List<TextLine> scan (BufferedImage image,
                                       int margin,
                                       LayoutMode layoutMode,
                                       String language,
                                       int interline,
                                       String label)
    {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final Point origin = new Point(0, 0);

        // Add some white some white margin around the image?
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

        return ocr.recognize(interline, bi, origin, language, layoutMode, label);
    }
}
