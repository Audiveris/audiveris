//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     T e x t F o n t T e s t                                    //
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
package org.audiveris.omr.ui.symbol;

import org.junit.Test;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Class <code>TextFontTest</code> generates a PDF file for a provided font name.
 *
 * @author Hervé Bitteur
 */
public class TextFontTest
{
    final int itemsPerLine = 8;

    final int linesPerPage = 8;

    final int xMargin = 100;

    final int yMargin = 100;

    final int cellWidth = 200;

    final int cellHeight = 200;

    final int pageWidth = (itemsPerLine * cellWidth) + (2 * xMargin);

    final int pageHeight = (linesPerPage * cellHeight) + (2 * yMargin);

    /**
     * Printout of each text character.
     */
    @Test
    public void textPrintout ()
        throws Exception
    {
        File dir = new File("data/temp");
        dir.mkdirs();

        final String fontName = "Jazz Text";
        final String trimmed = fontName.replaceAll(" ", "");
        final File file = new File(dir, trimmed + ".pdf");
        final Font font = OmrFont.getFont(fontName, trimmed + ".ttf", 0, 64);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            Rectangle rect = new Rectangle(pageWidth, pageHeight);
            int x = xMargin; // Cell left side
            int y = yMargin; // Cell top side
            int line = 0;
            Document document = new Document(rect);
            PdfWriter writer = PdfWriter.getInstance(document, fos);
            document.open();

            PdfContentByte cb = writer.getDirectContent();
            Graphics2D g = new PdfGraphics2D(cb, pageWidth, pageHeight);
            final FontRenderContext frc = g.getFontRenderContext();

            Font stringFont = g.getFont().deriveFont(24f);
            Font infoFont = stringFont.deriveFont(15f);
            String frm = "x:%4.1f y:%4.1f w:%4.1f h:%4.1f";
            int n = -1;

            final Symbols.CodeRange range = new Symbols.CodeRange(0x0000, 0xFFFF);
            for (int i = range.start; i <= range.stop; i++) {
                final String str = MusicFont.getString(i);
                final TextLayout layout = new TextLayout(str, font, frc);
                Rectangle2D r = layout.getBounds();

                if (r.getWidth() == 0 && r.getHeight() == 0) {
                    continue;
                }

                n++;
                if (n > 0) {
                    // Compute x,y for current cell
                    x = xMargin + (cellWidth * (n % itemsPerLine));

                    if (x == xMargin) {
                        line++;

                        if (line >= linesPerPage) {
                            // New page
                            g.dispose();
                            document.setPageSize(rect);
                            document.newPage();
                            cb = writer.getDirectContent();
                            g = new PdfGraphics2D(cb, pageWidth, pageHeight);
                            x = xMargin;
                            y = yMargin;
                            line = 0;
                        } else {
                            y = yMargin + (line * cellHeight);
                        }
                    }
                }

                // Draw axes
                g.setColor(Color.PINK);
                g.drawLine(
                        x + (cellWidth / 4),
                        y + (cellHeight / 2),
                        (x + cellWidth) - (cellWidth / 4),
                        y + (cellHeight / 2));
                g.drawLine(
                        x + (cellWidth / 2),
                        y + (cellHeight / 4),
                        x + (cellWidth / 2),
                        (y + cellHeight) - (cellHeight / 4));

                // Draw number
                g.setFont(stringFont);
                g.setColor(Color.RED);
                g.drawString(Integer.toHexString(i), x + 10, y + 30);

                // Draw info
                String info = String.format(frm, r.getX(), r.getY(), r.getWidth(), r.getHeight());
                g.setFont(infoFont);
                g.setColor(Color.GRAY);
                g.drawString(info, x + 5, (y + cellHeight) - 5);

                // Draw cell rectangle
                g.setColor(Color.BLUE);
                g.drawRect(x, y, cellWidth, cellHeight);

                // Draw symbol
                g.setColor(Color.BLACK);
                layout.draw(g, x + (cellWidth / 2), y + (cellHeight / 2));
            }

            // This is the end...
            g.dispose();
            document.close();
        }
    }
}

//~ Static fields/initializers -----------------------------------------------------------------

//~ Enumerations -------------------------------------------------------------------------------

//~ Instance fields ----------------------------------------------------------------------------

//~ Constructors -------------------------------------------------------------------------------

//~ Methods ------------------------------------------------------------------------------------

//~ Inner Classes ------------------------------------------------------------------------------
