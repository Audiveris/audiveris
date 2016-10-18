//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         D e b u g                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr;

import omr.classifier.NeuralClassifier;
import omr.classifier.Sample;
import omr.classifier.SampleRepository;
import omr.classifier.ShapeDescription;

import omr.glyph.ShapeSet;

import omr.image.TemplateFactory;

import omr.sheet.Picture;
import omr.sheet.SheetStub;
import omr.sheet.ui.StubDependent;
import omr.sheet.ui.StubsController;

import org.jdesktop.application.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Convenient class meant to temporarily inject some debugging.
 * To be used in sync with file user-actions.xml in config folder
 *
 * @author Hervé Bitteur
 */
public class Debug
        extends StubDependent
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
        SheetStub stub = StubsController.getCurrentStub();

        if ((stub != null) && stub.hasSheet()) {
            Picture picture = stub.getSheet().getPicture();

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
     * Generate a file (format csv) to be used by deep learning software,
     * with the training data.
     *
     * @param e unused
     */
    @Action
    public void saveTrainingData (ActionEvent e)
            throws FileNotFoundException
    {
        Path path = WellKnowns.TRAIN_FOLDER.resolve(
                "samples-" + ShapeDescription.getName() + ".csv");
        OutputStream os = new FileOutputStream(path.toFile());
        final PrintWriter out = getPrintWriter(os);

        SampleRepository repository = SampleRepository.getLoadedInstance(true);

        List<Sample> samples = repository.getAllSamples();
        logger.info("Samples: {}", samples.size());

        for (Sample sample : samples) {
            double[] ins = ShapeDescription.features(sample, sample.getInterline());

            for (double in : ins) {
                out.print((float) in);
                out.print(",");
            }

            ///out.println(sample.getShape().getPhysicalShape());
            out.println(sample.getShape().getPhysicalShape().ordinal());
        }

        out.flush();
        out.close();
        logger.info("Classifier data saved in " + path.toAbsolutePath());

        final List<String> names = Arrays.asList(ShapeSet.getPhysicalShapeNames());

        // Shape names
        StringBuilder sb = new StringBuilder("{ //\n");

        for (int i = 0; i < names.size(); i++) {
            String comma = (i < names.size() - 1) ? "," : "";
            sb.append(String.format("\"%-18s // %3d\n", names.get(i) + "\"" + comma, i));
        }

        sb.append("};");
        System.out.println(sb.toString());
    }

    //--------------//
    // trainAndSave //
    //--------------//
    /**
     *
     *
     * @param e unused
     */
    @Action
    public void trainAndSave (ActionEvent e)
            throws FileNotFoundException, IOException
    {
        Path modelPath = WellKnowns.TRAIN_FOLDER.resolve(NeuralClassifier.MODEL_FILE_NAME);
        Files.deleteIfExists(modelPath);

        Path normsPath = WellKnowns.TRAIN_FOLDER.resolve(NeuralClassifier.NORMS_FILE_NAME);
        Files.deleteIfExists(normsPath);

        SampleRepository repository = SampleRepository.getLoadedInstance(true);

        List<Sample> samples = repository.getAllSamples();
        logger.info("Samples: {}", samples.size());

        NeuralClassifier classifier = NeuralClassifier.getInstance();
        classifier.train(samples, null);
        classifier.store();
    }

    //----------------//
    // getPrintWriter //
    //----------------//
    private static PrintWriter getPrintWriter (OutputStream os)
    {
        try {
            final BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(os, WellKnowns.FILE_ENCODING));

            return new PrintWriter(bw);
        } catch (Exception ex) {
            logger.warn("Error creating PrintWriter " + ex, ex);

            return null;
        }
    }
}
