//----------------------------------------------------------------------------//
//                                                                            //
//                               D e b u g                                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.glyph.GlyphRepository;
import omr.glyph.Shape;
import omr.glyph.ShapeDescription;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.score.ui.ScoreDependent;

import org.jdesktop.application.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

/**
 * Convenient class meant to temporarily inject some debugging.
 * To be used in sync with file user-actions.xml in config folder
 *
 * @author Hervé Bitteur
 */
public class Debug
        extends ScoreDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Debug.class);

    //~ Methods ----------------------------------------------------------------
    //    //------------------//
    //    // injectChordNames //
    //    //------------------//
    //    @Action(enabledProperty = SHEET_AVAILABLE)
    //    public void injectChordNames (ActionEvent e)
    //    {
    //        Score score = ScoreController.getCurrentScore();
    //
    //        if (score == null) {
    //            return;
    //        }
    //
    //        ScoreSystem system = score.getFirstPage()
    //                                  .getFirstSystem();
    //        system.acceptChildren(new ChordInjector());
    //    }
    //    //---------------//
    //    // ChordInjector //
    //    //---------------//
    //    private static class ChordInjector
    //        extends AbstractScoreVisitor
    //    {
    //        //~ Static fields/initializers -----------------------------------------
    //
    //        /** List of symbols to inject. */
    //        private static final String[] shelf = new String[] {
    //                                                  "BMaj7/D#", "BMaj7", "G#m9",
    //                                                  "F#", "C#7sus4", "F#"
    //                                              };
    //
    //        //~ Instance fields ----------------------------------------------------
    //
    //        /** Current index to symbol to inject. */
    //        private int symbolCount = 0;
    //
    //        //~ Methods ------------------------------------------------------------
    //
    //        @Override
    //        public boolean visit (ChordSymbol symbol)
    //        {
    //            // Replace chord info by one taken from the shelf
    //            if (symbolCount < shelf.length) {
    //                symbol.info = ChordInfo.create(shelf[symbolCount++]);
    //            }
    //
    //            return false;
    //        }
    //    }
    //------------------//
    // saveTrainingData //
    //------------------//
    @Action
    public void saveTrainingData (ActionEvent e)
    {
        final PrintWriter out = getPrintWriter(new File("glyphs.arff"));

        out.println("@relation " + "glyphs");
        out.println();

        for (String label : ShapeDescription.getParameterLabels()) {
            out.println("@attribute " + label + " real");
        }

        // Last attribute: shape
        out.print("@attribute shape {");

        for (Shape shape : ShapeSet.allPhysicalShapes) {
            out.print(shape);

            if (shape != Shape.LAST_PHYSICAL_SHAPE) {
                out.print(", ");
            }
        }

        out.println("}");

        out.println();
        out.println("@data");

        GlyphRepository repository = GlyphRepository.getInstance();
        List<String> gNames = repository.getWholeBase(null);
        logger.info("Glyphs: {}", gNames.size());

        for (String gName : gNames) {
            Glyph glyph = repository.getGlyph(gName, null);

            if (glyph != null) {
                double[] ins = ShapeDescription.features(glyph);

                for (double in : ins) {
                    out.print((float) in);
                    out.print(",");
                }

                out.println(glyph.getShape().getPhysicalShape());

                //break; /////////////////////////////////////////////////////////
            }
        }

        out.flush();
        out.close();
        logger.info("Done.");
    }

    //----------------//
    // getPrintWriter //
    //----------------//
    private static PrintWriter getPrintWriter (File file)
    {
        try {
            final BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(
                    new FileOutputStream(file),
                    WellKnowns.FILE_ENCODING));

            return new PrintWriter(bw);
        } catch (Exception ex) {
            System.err.println("Error creating " + file + ex);

            return null;
        }
    }
}
