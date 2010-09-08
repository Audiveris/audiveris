//----------------------------------------------------------------------------//
//                                                                            //
//                        S c o r e P d f O u t p u t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ScoreManager;

import omr.ui.view.Zoom;

import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Class {@code ScorePdfOutput} defines a specific ScoreView meant to produce
 * a PDF output of a score
 *
 * @author Hervé Bitteur
 */
public class ScorePdfOutput
    extends ScoreView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreManager.class);

    //~ Instance fields --------------------------------------------------------

    /** The file to print to */
    private File file;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ScorePdfOutput object.
     *
     * @param score the score to print
     * @param file the target PDF file
     */
    public ScorePdfOutput (Score score,
                           File  file)
    {
        super(
            score,
            score.getLayout(ScoreOrientation.VERTICAL),
            PaintingParameters.getInstance());
        this.file = file;
    }

    //~ Methods ----------------------------------------------------------------

    public void write ()
        throws Exception
    {
        Document document = null;

        try {
            Dimension dim = scoreLayout.getScoreDimension();
            document = new Document(new Rectangle(dim.width, dim.height));

            FileOutputStream fos = new FileOutputStream(file);
            PdfWriter        writer = PdfWriter.getInstance(document, fos);
            document.open();

            PdfContentByte cb = writer.getDirectContent();
            Graphics2D     g2 = cb.createGraphics(dim.width, dim.height);
            g2.scale(1, 1);

            // Painting
            Zoom         zoom = new Zoom(1);
            ScorePainter painter = new ScorePainter(scoreLayout, g2, zoom);
            score.accept(painter);
            // This is the end...
            g2.dispose();
        } catch (Exception ex) {
            logger.warning("Error printing " + file, ex);
            throw ex;
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }
}
