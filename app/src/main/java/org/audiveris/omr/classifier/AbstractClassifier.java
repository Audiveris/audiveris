//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A b s t r a c t C l a s s i f i e r                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.classifier;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeChecker;
import org.audiveris.omr.math.PoorManAlgebra.DataSet;
import org.audiveris.omr.math.PoorManAlgebra.INDArray;
import org.audiveris.omr.math.PoorManAlgebra.Nd4j;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.ZipFileSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.CREATE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import javax.xml.bind.JAXBException;

/**
 * Class <code>AbstractClassifier</code> is an abstract basis for all Classifier
 * implementations.
 * <p>
 * It handles the storing and loading of shape classifier model together with features norms
 * (means and standard deviations).
 * <p>
 * The classifier data is thus composed of two parts (model and norms) which are loaded as a whole
 * according to the following algorithm:
 * <ol>
 * <li>It first tries to find data in the application user local area ('train').
 * If found, this data contains a custom definition of model+norms, typically after a user
 * training.</li>
 * <li>If not found, it falls back reading the default definition from the application resource,
 * reading the 'res' folder in the application program area.
 * </ol>
 * <p>
 * After any user training, the data is stored as the custom definition in the user local area,
 * which will be picked up first when the application is run again.
 *
 * @param <M> model class to be used
 * @param <N> norms class to be used
 * @author Hervé Bitteur
 */
public abstract class AbstractClassifier<M extends Object, N extends Object>
        implements Classifier
{
    //~ Static fields/initializers -----------------------------------------------------------------

    static {
        // We need class WellKnowns to be elaborated before anything else (when in standalone mode)
        WellKnowns.ensureLoaded();
    }

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AbstractClassifier.class);

    /** A special evaluation array, used to report NOISE. */
    private static final Evaluation[] noiseEvaluations = { new Evaluation(
            Shape.NOISE,
            Evaluation.ALGORITHM) };

    //~ Instance fields ----------------------------------------------------------------------------

    /** Glyph features descriptor. */
    protected GlyphDescriptor descriptor;

    /** The glyph checker for additional specific checks. */
    protected ShapeChecker glyphChecker = ShapeChecker.getInstance();

    //~ Constructors -------------------------------------------------------------------------------

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // evaluate //
    //----------//
    @Override
    public Evaluation[] evaluate (Glyph glyph,
                                  int interline,
                                  int count,
                                  double minGrade,
                                  EnumSet<Condition> conditions)
    {
        return evaluate(glyph, null, count, minGrade, conditions, interline);
    }

    //----------//
    // evaluate //
    //----------//
    @Override
    public Evaluation[] evaluate (Glyph glyph,
                                  SystemInfo system,
                                  int count,
                                  double minGrade,
                                  EnumSet<Classifier.Condition> conditions)
    {
        final int interline = system.getSheet().getInterline();

        return evaluate(glyph, system, count, minGrade, conditions, interline);
    }

    //----------//
    // evaluate //
    //----------//
    private Evaluation[] evaluate (Glyph glyph,
                                   SystemInfo system,
                                   int count,
                                   double minGrade,
                                   EnumSet<Classifier.Condition> conditions,
                                   int interline)
    {
        List<Evaluation> bests = new ArrayList<>();
        Evaluation[] evals = getSortedEvaluations(glyph, interline);

        EvalsLoop:
        for (Evaluation eval : evals) {
            // Bounding test?
            if ((bests.size() >= count) || (eval.grade < minGrade)) {
                break;
            }

            // Successful checks?
            if ((conditions != null) && conditions.contains(Condition.CHECKED)) {
                // This may change the eval shape in only one case:
                // HW_REST_set may be changed for HALF_REST or WHOLE_REST based on pitch
                glyphChecker.annotate(system, eval, glyph);

                if (eval.failure != null) {
                    continue;
                }
            }

            // Everything is OK, add the shape if not already in the list
            // (this can happen when checks have modified the eval original shape)
            for (Evaluation e : bests) {
                if (e.shape == eval.shape) {
                    continue EvalsLoop;
                }
            }

            bests.add(eval);
        }

        return bests.toArray(new Evaluation[bests.size()]);
    }

    //---------------//
    // getDescriptor //
    //---------------//
    @Override
    public GlyphDescriptor getGlyphDescriptor ()
    {
        return descriptor;
    }

    //---------------//
    // getRawDataSet //
    //---------------//
    /**
     * Build a raw (non normalized) dataset out of the provided collection of samples.
     *
     * @param samples the provided samples
     * @return a raw DataSet for use by a MultiLayerNetwork
     */
    public DataSet getRawDataSet (Collection<Sample> samples)
    {
        final StopWatch watch = new StopWatch("getRawDataSet");
        watch.start("allocate doubles");

        final double[][] inputs = new double[samples.size()][];
        final double[][] desiredOutputs = new double[samples.size()][];
        int ig = 0;

        watch.start("browse samples");

        for (Sample sample : samples) {
            double[] ins = descriptor.getFeatures(sample, sample.getInterline());
            inputs[ig] = ins;

            double[] des = new double[SHAPE_COUNT];
            Arrays.fill(des, 0);
            des[sample.getShape().getPhysicalShape().ordinal()] = 1;
            desiredOutputs[ig] = des;

            ig++;
        }

        // Build the collection of features from the glyph data
        watch.start("features");

        final INDArray features = Nd4j.create(inputs);
        watch.start("labels");

        final INDArray labels = Nd4j.create(desiredOutputs);

        if (constants.printWatch.isSet()) {
            watch.print();
        }

        return new DataSet(features, labels, null, null);
    }

    //----------------------//
    // getSortedEvaluations //
    //----------------------//
    /**
     * Run the classifier with the specified glyph, and return a sequence of all
     * interpretations (ordered from best to worst) with no additional check.
     *
     * @param glyph     the glyph to be examined
     * @param interline the global sheet interline
     * @return the ordered best evaluations
     */
    protected Evaluation[] getSortedEvaluations (Glyph glyph,
                                                 int interline)
    {
        // If too small, it's just NOISE
        if (!isBigEnough(glyph, interline)) {
            return noiseEvaluations;
        } else {
            Evaluation[] evals = getNaturalEvaluations(glyph, interline);
            Arrays.sort(evals, Evaluation.byReverseGrade); // Order the evals from best to worst

            return evals;
        }
    }

    //----------------//
    // getTrainFolder //
    //----------------//
    /**
     * Report the user train folder.
     *
     * @return the path to user train folder (which may not exist)
     */
    protected Path getTrainFolder ()
    {
        try {
            return getTrainFolder(false);
        } catch (IOException cannotHappen) {
            return null;
        }
    }

    //----------------//
    // getTrainFolder //
    //----------------//
    /**
     * Report the user train folder, after creating it if required.
     *
     * @param create true for creating the folder if needed
     * @return the path to user train folder
     * @throws IOException
     */
    protected Path getTrainFolder (boolean create)
        throws IOException
    {
        if (create && !Files.exists(WellKnowns.TRAIN_FOLDER)) {
            Files.createDirectories(WellKnowns.TRAIN_FOLDER);
            logger.info("Created directory {}", WellKnowns.TRAIN_FOLDER);
        }

        return WellKnowns.TRAIN_FOLDER;
    }

    //-------------//
    // isBigEnough //
    //-------------//
    @Override
    public boolean isBigEnough (double weight)
    {
        return weight >= constants.minWeight.getValue();
    }

    //-------------//
    // isBigEnough //
    //-------------//
    @Override
    public boolean isBigEnough (Glyph glyph,
                                int interline)
    {
        return isBigEnough(glyph.getNormalizedWeight(interline));
    }

    //--------------//
    // isCompatible //
    //--------------//
    /**
     * Make sure the provided pair (model + norms) is compatible with the current
     * application version.
     *
     * @param model non-null model instance
     * @param norms non-null norms instance
     * @return true if engine is usable and found compatible
     */
    protected abstract boolean isCompatible (M model,
                                             N norms);

    //------//
    // load //
    //------//
    /**
     * Load model and norms from the most suitable classifier data files.
     * If user files do not exist or cannot be unmarshalled, the default files are used.
     *
     * @param fileNames names for model file and for optional norms file
     * @return the network loaded (model + norms)
     */
    protected abstract Network load (String... fileNames);

    //-----------//
    // loadModel //
    //-----------//
    /**
     * Load model using the provided input path.
     *
     * @param input path to input data
     * @return the loaded model
     * @throws Exception if something goes wrong
     */
    protected abstract M loadModel (Path input)
        throws Exception;

    //-----------//
    // loadNorms //
    //-----------//
    /**
     * Try to load normalization data using the provided input path.
     *
     * @param input the path to input
     * @return the loaded normalization
     * @throws Exception if something goes wrong
     */
    protected abstract N loadNorms (Path input)
        throws Exception;

    //-------//
    // store //
    //-------//
    /**
     * Store the engine internals (model and norms) always as user files.
     */
    protected abstract void store ()
        throws Exception;

    //------------//
    // storeModel //
    //------------//
    /**
     * Store the model to disk, always as user data.
     *
     * @param modelPath path to model data
     * @throws Exception if something goes wrong
     */
    protected abstract void storeModel (Path modelPath)
        throws Exception;

    //------------//
    // storeNorms //
    //------------//
    /**
     * Store the norms based on training samples.
     *
     * @param normsPath path to normalization data
     * @throws Exception if something goes wrong
     */
    protected abstract void storeNorms (Path normsPath)
        throws Exception;

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Scale.AreaFraction minWeight = new Scale.AreaFraction(
                0.04,
                "Minimum normalized weight to be considered not a noise");
    }

    //---------//
    // Network //
    //---------//
    protected class Network
    {
        public M model;

        public N norms;

        public Network ()
        {
        }

        public Network (M model,
                        N norms)
        {
            this.model = model;
            this.norms = norms;
        }
    }
}
