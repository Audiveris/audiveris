//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B o o k P d f O u t p u t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.sheet.Book;
import omr.sheet.Sheet;
import omr.sheet.ui.SheetResultPainter;

import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileOutputStream;

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
    public void write ()
            throws Exception
    {
        FileOutputStream fos = new FileOutputStream(file);
        Document document = null;
        PdfWriter writer = null;

        try {
            for (Sheet sheet : book.getSheets()) {
                Dimension dim = sheet.getDimension();

                if (document == null) {
                    document = new Document(new Rectangle(dim.width, dim.height));
                    writer = PdfWriter.getInstance(document, fos);
                    document.open();
                } else {
                    document.setPageSize(new Rectangle(dim.width, dim.height));
                    document.newPage();
                }

                PdfContentByte cb = writer.getDirectContent();
                Graphics2D g2 = cb.createGraphics(dim.width, dim.height);

                // Scale: 1
                g2.scale(1, 1);

                // Anti-aliasing ON
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                // Painting
                SheetResultPainter painter = new SheetResultPainter(
                        sheet,
                        g2,
                        false, // No voice painting
                        true, // Paint staff lines
                        false); // No annotations
                g2.setColor(Color.BLACK);

                painter.process();

                // This is the end...
                g2.dispose();
            }
        } catch (Exception ex) {
            logger.warn("Error printing " + book.getRadix(), ex);
            throw ex;
        } finally {
            if (document != null) {
                document.close();
            }
        }

        fos.close();
    }
}
