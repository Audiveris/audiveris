//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B o o k P d f O u t p u t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.score.ui;

import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.SheetResultPainter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Class {@code BookPdfOutput} produces a physical PDF output of a book.
 *
 * @author Hervé Bitteur
 */
public class BookPdfOutput
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BookPdfOutput.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related book. */
    private final Book book;

    /** The file to print to. */
    private final File file;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SheetPdfOutput object.
     *
     * @param book the book to print
     * @param file the target PDF file
     */
    public BookPdfOutput (Book book,
                          File file)
    {
        this.book = book;
        this.file = file;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Write the PDF output for the provided sheet if any, otherwise for the whole book.
     *
     * @param sheet desired sheet or null
     * @throws Exception if printing goes wrong
     */
    public void write (Sheet sheet)
            throws Exception
    {
        FileOutputStream fos = null;
        Document document = null;
        PdfWriter writer = null;

        try {
            final List<SheetStub> stubs = (sheet != null) ? Arrays.asList(sheet.getStub())
                    : book.getValidStubs();
            fos = new FileOutputStream(file);

            for (SheetStub stub : stubs) {
                final int width = stub.getSheet().getWidth();
                final int height = stub.getSheet().getHeight();

                if (document == null) {
                    document = new Document(new Rectangle(width, height));
                    writer = PdfWriter.getInstance(document, fos);
                    document.open();
                } else {
                    document.setPageSize(new Rectangle(width, height));
                    document.newPage();
                }

                PdfContentByte cb = writer.getDirectContent();
                Graphics2D g2 = cb.createGraphics(width, height);

                // Scale: 1
                g2.scale(1, 1);

                // Anti-aliasing ON
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                // Painting
                SheetResultPainter painter = new SheetResultPainter(
                        stub.getSheet(),
                        g2,
                        false, // No voice painting
                        true, // Paint staff lines
                        false); // No annotations
                g2.setColor(Color.BLACK);

                painter.process();

                // This is the end...
                g2.dispose();
            }

            logger.info("Book printed to {}", file);
        } finally {
            if (document != null) {
                document.close();
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
