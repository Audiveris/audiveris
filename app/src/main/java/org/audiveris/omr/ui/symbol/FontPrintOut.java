//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     F o n t P r i n t O u t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2024. All rights reserved.
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
import com.itextpdf.text.DocumentException;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Class <code>FontPrintOut</code> generates a PDF file for a provided font
 * with all non-degenerated symbols from the font.
 *
 * @author Hervé Bitteur
 */
public class FontPrintOut
{
    //~ Static fields/initializers -----------------------------------------------------------------

    protected static final int itemsPerLine = 8;

    protected static final int linesPerPage = 8;

    protected static final int xMargin = 100;

    protected static final int yMargin = 100;

    protected static final int cellWidth = 200;

    protected static final int cellHeight = 200;

    protected static final int pageWidth = (itemsPerLine * cellWidth) + (2 * xMargin);

    protected static final int pageHeight = (linesPerPage * cellHeight) + (2 * yMargin);

    protected static final String frm = "x:%4.1f y:%4.1f w:%4.1f h:%4.1f";

    /** Needed for font size computation. */
    public static final FontRenderContext frc = new FontRenderContext(null, true, true);

    //~ Instance fields ----------------------------------------------------------------------------

    protected final Font font;

    protected File file;

    protected FileOutputStream fos;

    Rectangle rect;

    Document document;

    PdfWriter writer;

    PdfContentByte cb;

    Graphics2D g;

    Font stringFont;

    Font infoFont;

    int x;

    int y;

    int line = 0;

    int n = -1;

    //~ Constructors -------------------------------------------------------------------------------

    public FontPrintOut (Font font)
    {
        this.font = font;
    }

    //~ Methods ------------------------------------------------------------------------------------

    public void printSymbol (int i)
    {
        final TextLayout layout = new TextLayout(MusicFont.getString(i), font, frc);
        final Rectangle2D r = layout.getBounds();

        if (r.getWidth() == 0 && r.getHeight() == 0) {
            return;
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
        final String info = String.format(frm, r.getX(), r.getY(), r.getWidth(), r.getHeight());
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

    public void start ()
    {
        try {
            final File dir = new File("build/data/temp");
            dir.mkdirs();

            file = new File(dir, font.getFontName() + ".pdf");
            fos = new FileOutputStream(file);

            rect = new Rectangle(pageWidth, pageHeight);
            document = new Document(rect);
            writer = PdfWriter.getInstance(document, fos);
            document.open();

            cb = writer.getDirectContent();
            g = new PdfGraphics2D(cb, pageWidth, pageHeight);
            stringFont = g.getFont().deriveFont(24f);
            infoFont = stringFont.deriveFont(15f);

            x = xMargin; // Cell left side
            y = yMargin; // Cell top side

        } catch (DocumentException | FileNotFoundException ex) {
            System.out.println("Exception " + ex);
        }
    }

    public void stop ()
    {
        g.dispose();
        document.close();

        try {
            fos.close();
        } catch (IOException ex) {
            System.out.println("Exception " + ex);
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------//
    // main //
    //------//
    public static void main (String... args)
    {
        if (args != null && args.length > 0) {
            printFont(args[0]);
        }
    }

    //-----------//
    // printFont //
    //-----------//
    public static void printFont (String fontName)
    {
        final int iMin = 0;
        final int iMax = 0xFFFF;

        System.out.println("fontName: " + fontName);
        final Font font = Font.decode(fontName);
        System.out.println("font: " + font);
        final FontPrintOut fp = new FontPrintOut(font.deriveFont((float) 64));
        fp.start();

        for (int i = iMin; i <= iMax; i++) {
            fp.printSymbol(i);
        }

        fp.stop();
    }

}
