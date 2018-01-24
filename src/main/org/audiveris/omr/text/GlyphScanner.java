//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G l y p h S c a n n e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright ©  Audiveris 2017. All rights reserved.
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

import ij.process.ByteProcessor;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.sheet.Sheet;

import static org.audiveris.omr.text.TextBuilder.getOcr;

import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Class {@code GlyphScanner} launches the OCR on a glyph, to retrieve the TextLine
 * instance(s) this glyph represents.
 * <p>
 * As opposed to [@link SheetScanner}, here Tesseract is used in SINGLE_BLOCK layout mode,
 * since the glyph, as complex as it can be with many lines and words, is considered as a single
 * block of text.
 * <p>
 * The raw OCR output will later be processed at system level by dedicated TextBuilder instances.
 *
 * @author Hervé Bitteur
 */
public class GlyphScanner
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(GlyphScanner.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code GlyphScanner} object that can work on a glyph or a buffer.
     *
     * @param sheet underlying sheet
     */
    public GlyphScanner (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // scanBuffer //
    //------------//
    /**
     * Launch the OCR on the provided buffer, to retrieve the TextLine instance(s) with
     * coordinates <b>relative</b> to buffer origin.
     * <p>
     * Tesseract OCR generally gives better results if the processed image exhibits white pixels
     * on the image contour, so here we (transparently) add a white margin around the buffer.
     *
     * @param buffer   the ByteProcessor buffer
     * @param language the probable language spec
     * @param id       an arbitrary id, used only when keeping the image on disk
     * @return a list, not null but perhaps empty, of raw TextLine's with relative coordinates.
     */
    public List<TextLine> scanBuffer (ByteProcessor buffer,
                                      String language,
                                      int id)
    {
        final BufferedImage img = buffer.getBufferedImage();
        final int width = buffer.getWidth();
        final int height = buffer.getHeight();
        final Point origin = new Point(0, 0);

        // Add some white some white margin around the glyph
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

            // Glyph image drawn with margins shift
            g.drawImage(img, margin, margin, null);
            origin.translate(-margin, -margin);
        } else {
            bi = img;
        }

        return getOcr().recognize(
                sheet.getScale().getInterline(),
                bi,
                origin,
                language,
                OCR.LayoutMode.SINGLE_BLOCK,
                sheet.getId() + "-g" + id);
    }

    //-----------//
    // scanGlyph //
    //-----------//
    /**
     * Launch the OCR on the provided glyph, to retrieve the TextLine instance(s)
     * with <b>absolute</b> coordinates (sheet origin).
     * <p>
     * Tesseract OCR generally gives better results if the processed image exhibits white pixels
     * on the image contour, so here we (transparently) add a white margin around the glyph.
     *
     * @param glyph    the glyph to OCR
     * @param language the probable language spec
     * @return a list, not null but perhaps empty, of raw TextLine's with absolute coordinates.
     */
    public List<TextLine> scanGlyph (Glyph glyph,
                                     String language)
    {
        final BufferedImage img = glyph.getBuffer().getBufferedImage();
        final Rectangle box = glyph.getBounds();
        final Point origin = box.getLocation();

        // Add some white some white margin around the glyph
        final int margin = constants.whiteMarginAdded.getValue();
        final BufferedImage bi;

        if (margin > 0) {
            bi = new BufferedImage(
                    box.width + (2 * margin),
                    box.height + (2 * margin),
                    BufferedImage.TYPE_BYTE_GRAY);

            // Background filled with white
            Graphics g = bi.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, bi.getWidth(), bi.getHeight());

            // Glyph image drawn with margins shift
            g.drawImage(img, margin, margin, null);
            origin.translate(-margin, -margin);
        } else {
            bi = img;
        }

        return getOcr().recognize(
                sheet.getScale().getInterline(),
                bi,
                origin,
                language,
                OCR.LayoutMode.SINGLE_BLOCK,
                sheet.getId() + "-g" + glyph.getId());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer whiteMarginAdded = new Constant.Integer(
                "pixels",
                10,
                "Margin of white pixels added around a glyph image");
    }
}
