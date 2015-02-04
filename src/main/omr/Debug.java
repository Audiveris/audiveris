//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         D e b u g                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.glyph.GlyphRepository;
import omr.glyph.Shape;
import omr.glyph.ShapeDescription;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.image.TemplateFactory;

import omr.score.ui.ScoreDependent;

import omr.sheet.Picture;
import omr.sheet.Sheet;
import omr.sheet.ui.SheetsController;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Debug.class);

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // checkSources //
    //--------------//
    /**
     * Check which sources are still in Picture cache.
     *
     * @param e unused
     */
    @Action
    public void checkSources (ActionEvent e)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet != null) {
            Picture picture = sheet.getPicture();

            if (picture != null) {
                picture.checkSources();
            }
        }
    }

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
    //----------------//
    // checkTemplates //
    //----------------//
    /**
     * Generate the templates for all relevant shapes for a range of interline values.
     *
     * @param e unused
     */
    @Action
    public void checkTemplates (ActionEvent e)
    {
        TemplateFactory factory = TemplateFactory.getInstance();

        for (int i = 10; i < 40; i++) {
            logger.info("Catalog for interline {}", i);
            factory.getCatalog(i);
        }

        logger.info("Done.");
    }

    //------------------//
    // saveTrainingData //
    //------------------//
    /**
     * Generate a file (format arff) to be used by Weka machine learning software,
     * with the training data.
     *
     * @param e unused
     */
    @Action
    public void saveTrainingData (ActionEvent e)
    {
        File file = new File(
                WellKnowns.EVAL_FOLDER,
                "samples-" + ShapeDescription.getName() + ".arff");
        final PrintWriter out = getPrintWriter(file);

        out.println("@relation " + "glyphs-" + ShapeDescription.getName());
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
            }
        }

        out.flush();
        out.close();
        logger.info("Classifier data saved in " + file.getAbsolutePath());
    }

    //----------------//
    // getPrintWriter //
    //----------------//
    private static PrintWriter getPrintWriter (File file)
    {
        try {
            final BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), WellKnowns.FILE_ENCODING));

            return new PrintWriter(bw);
        } catch (Exception ex) {
            System.err.println("Error creating " + file + ex);

            return null;
        }
    }
}
