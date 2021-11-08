//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   M u s i c F o n t T e s t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import org.junit.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Class <code>MusicFontTest</code> generates a PDF file with all symbols
 * from MusicalsSymbols font.
 *
 * @author Hervé Bitteur
 */
public class MusicFontTest
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
     * Printout of each MusicFont character.
     */
    @Test
    public void textPrintout ()
            throws Exception
    {
        File dir = new File("data/temp");
        dir.mkdirs();

        File file = new File(dir, MusicFont.FONT_NAME + ".pdf");

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
            MusicFont musicFont = MusicFont.getPointFont(64, 0);
            Font stringFont = g.getFont().deriveFont(24f);
            Font infoFont = stringFont.deriveFont(15f);
            String frm = "x:%4.1f y:%4.1f w:%4.1f h:%4.1f";

            for (int i = 0; i < 256; i++) {
                BasicSymbol symbol = new BasicSymbol(false, i);
                TextLayout layout = symbol.layout(musicFont);

                if (i > 0) {
                    // Compute x,y for current cell
                    x = xMargin + (cellWidth * (i % itemsPerLine));

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
                g.drawString(Integer.toString(i), x + 10, y + 30);

                // Draw info
                Rectangle2D r = layout.getBounds();
                String info = String.format(
                        frm,
                        r.getX(),
                        r.getY(),
                        r.getWidth(),
                        r.getHeight());
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
