//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A b s t r a c t C l a s s i f i e r                              //
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
package org.audiveris.omr.classifier;

import org.audiveris.omr.WellKnowns;
import static org.audiveris.omr.classifier.Classifier.SHAPE_COUNT;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeChecker;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.util.UriUtil;
import org.audiveris.omr.util.Zip;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBException;

/**
 * Class {@code AbstractClassifier} is an abstract basis for all Classifier
 * implementations.
 * <p>
 * It handles the storing and loading of shape classifier model together with features norms
 * (means and standard deviations).
 * <p>
 * The classifier data is thus composed of two parts (model and norms) which are loaded as a whole
 * according to the following algorithm: <ol>
 * <li>It first tries to find data in the application user local area ('train').
 * If found, this data contains a custom definition of model+norms, typically after a user
 * training.</li>
 * <li>If not found, it falls back reading the default definition from the application resource,
 * reading the 'res' folder in the application program area.
 * </ol>
 * After any user training, the data is stored as the custom definition in the user local area,
 * which will be picked up first when the application is run again.</p>
 *
 * @param <M> precise model class to be used
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractClassifier<M extends Object>
        implements Classifier
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AbstractClassifier.class);

    /** A special evaluation array, used to report NOISE. */
    protected static final Evaluation[] noiseEvaluations = {
        new Evaluation(
        Shape.NOISE,
        Evaluation.ALGORITHM)
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Features means and standard deviations. */
    protected Norms norms;

    /** Glyph features descriptor. */
    protected GlyphDescriptor descriptor;

    /** The glyph checker for additional specific checks. */
    protected ShapeChecker glyphChecker = ShapeChecker.getInstance();

    //~ Methods ------------------------------------------------------------------------------------
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
    @Override
    public Evaluation[] evaluate (Glyph glyph,
                                  int interline,
                                  int count,
                                  double minGrade,
                                  EnumSet<Condition> conditions)
    {
        return evaluate(glyph, null, count, minGrade, conditions, interline);
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
        final double[][] inputs = new double[samples.size()][];
        final double[][] desiredOutputs = new double[samples.size()][];
        int ig = 0;

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
        final INDArray features = Nd4j.create(inputs);
        final INDArray labels = Nd4j.create(desiredOutputs);

        return new DataSet(features, labels, null, null);
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

    //-------------//
    // isBigEnough //
    //-------------//
    @Override
    public boolean isBigEnough (double weight)
    {
        return weight >= constants.minWeight.getValue();
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
                                             Norms norms);

    //-----------//
    // loadModel //
    //-----------//
    /**
     * Load classifier model out of the provided input stream.
     *
     * @param is non-null input stream
     * @return the loaded model
     * @throws Exception
     */
    protected abstract M loadModel (InputStream is)
            throws Exception;

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
            // Order the evals from best to worst
            Arrays.sort(evals);

            return evals;
        }
    }

    //------//
    // load //
    //------//
    /**
     * Load model and norms from the most suitable data files.
     * If user files do not exist or cannot be unmarshalled, the default files are used.
     *
     * @param modelFileName file name for model data
     * @param normsFileName file name for norms data
     * @return the model loaded
     */
    protected M load (String modelFileName,
                      String normsFileName)
    {
        // First, try user data, if any, in local EVAL folder
        logger.debug("Trying user data");

        {
            final Path modelPath = WellKnowns.TRAIN_FOLDER.resolve(modelFileName);
            final Path normsPath = WellKnowns.TRAIN_FOLDER.resolve(normsFileName);

            if (Files.exists(modelPath) && Files.exists(normsPath)) {
                try {
                    logger.debug("loadModel...");

                    M model = loadModel(new FileInputStream(modelPath.toFile()));
                    logger.debug("loadNorms...");
                    norms = loadNorms(new FileInputStream(normsPath.toFile()));
                    logger.debug("loaded.");

                    if (!isCompatible(model, norms)) {
                        final String msg = "Obsolete neural user data in " + modelPath
                                           + ", trying default data";
                        logger.warn(msg);
                    } else {
                        // Tell user we are not using the default
                        logger.info("Classifier model loaded from local {}", modelPath);
                        logger.info("Classifier norms loaded from local {}", normsPath);

                        return model; // Normal exit
                    }
                } catch (Exception ex) {
                    logger.warn("Load error {}", ex.toString(), ex);
                    norms = null;
                }
            }
        }

        // Second, use default data (in program RES folder)
        logger.debug("Trying default data");

        final URI modelUri = UriUtil.toURI(WellKnowns.RES_URI, modelFileName);
        final URI normsUri = UriUtil.toURI(WellKnowns.RES_URI, normsFileName);

        try {
            M model = loadModel(modelUri.toURL().openStream());
            norms = loadNorms(normsUri.toURL().openStream());

            if (!isCompatible(model, norms)) {
                final String msg = "Obsolete neural default data in " + modelUri
                                   + ", please retrain from scratch";
                logger.warn(msg);
            } else {
                logger.info("Classifier model loaded from default {}", modelUri);
                logger.info("Classifier norms loaded from default {}", normsUri);

                return model; // Normal exit
            }
        } catch (Exception ex) {
            logger.warn("Load error {}", ex.toString(), ex);
        }

        norms = null; // No norms

        return null; // No model
    }

    //-----------//
    // loadNorms //
    //-----------//
    /**
     * Try to load Norms data from the provided input stream.
     *
     * @param is the input stream
     * @return the loaded Norms instance, or exception is thrown
     * @throws IOException
     * @throws JAXBException
     */
    protected Norms loadNorms (InputStream is)
            throws IOException, JAXBException
    {
        INDArray means = null;
        INDArray stds = null;

        try {
            final ZipInputStream zis = new ZipInputStream(is);
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                switch (entry.getName()) {
                case "means.bin": {
                    DataInputStream dis = new DataInputStream(zis);
                    means = Nd4j.read(dis);
                }

                break;

                case "stds.bin": {
                    DataInputStream dis = new DataInputStream(zis);
                    stds = Nd4j.read(dis);
                }

                break;
                }

                zis.closeEntry();
            }

            zis.close();

            if ((means != null) && (stds != null)) {
                return new Norms(means, stds);
            }
        } finally {
            is.close();
        }

        throw new IllegalStateException(
                "Norms wasnt found within file, means: " + means + ", stds: " + stds);
    }

    //-------//
    // store //
    //-------//
    /**
     * Store the engine internals (model + norms), always as user files.
     *
     * @param modelFileName file name for model
     * @param normsFileName file name for norms
     */
    protected void store (String modelFileName,
                          String normsFileName)
    {
        final Path modelPath = WellKnowns.TRAIN_FOLDER.resolve(modelFileName);
        final Path normsPath = WellKnowns.TRAIN_FOLDER.resolve(normsFileName);

        try {
            if (!Files.exists(WellKnowns.TRAIN_FOLDER)) {
                Files.createDirectories(WellKnowns.TRAIN_FOLDER);
                logger.info("Created directory {}", WellKnowns.TRAIN_FOLDER);
            }

            storeModel(modelPath);
            storeNorms(normsPath);

            logger.info("{} data stored to folder {}", getName(), WellKnowns.TRAIN_FOLDER);
        } catch (Exception ex) {
            logger.warn("Error storing {} {}", getName(), ex.toString(), ex);
        }
    }

    //------------//
    // storeModel //
    //------------//
    /**
     * Store the model to disk.
     *
     * @param modelPath path to model file
     * @throws Exception
     */
    protected abstract void storeModel (Path modelPath)
            throws Exception;

    //------------//
    // storeNorms //
    //------------//
    /**
     * Store the norms based on training samples.
     *
     * @param normsPath path to the norms file
     * @throws FileNotFoundException
     * @throws IOException
     * @throws JAXBException
     */
    protected void storeNorms (Path normsPath)
            throws FileNotFoundException, IOException, JAXBException
    {
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(normsPath.toFile()));
        ZipOutputStream zos = new ZipOutputStream(stream);

        {
            ZipEntry means = new ZipEntry("means.bin");
            zos.putNextEntry(means);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            Nd4j.write(norms.means, dos);
            dos.flush();
            dos.close();

            InputStream inputStream = new ByteArrayInputStream(bos.toByteArray());
            Zip.copyEntry(inputStream, zos);
        }

        {
            ZipEntry stds = new ZipEntry("stds.bin");
            zos.putNextEntry(stds);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            Nd4j.write(norms.stds, dos);
            dos.flush();
            dos.close();

            InputStream inputStream = new ByteArrayInputStream(bos.toByteArray());
            Zip.copyEntry(inputStream, zos);
        }

        zos.flush();
        zos.close();
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
        List<Evaluation> bests = new ArrayList<Evaluation>();
        Evaluation[] evals = getSortedEvaluations(glyph, interline);

        EvalsLoop:
        for (Evaluation eval : evals) {
            // Bounding test?
            if ((bests.size() >= count) || (eval.grade < minGrade)) {
                break;
            }

            // Successful checks?
            if (conditions.contains(Condition.CHECKED)) {
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Norms //
    //-------//
    /**
     * Class that encapsulates the means and standard deviations of glyph features.
     */
    protected static class Norms
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Features means. */
        final INDArray means;

        /** Features standard deviations. */
        final INDArray stds;

        //~ Constructors ---------------------------------------------------------------------------
        public Norms (INDArray means,
                      INDArray stds)
        {
            this.means = means;
            this.stds = stds;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.AreaFraction minWeight = new Scale.AreaFraction(
                0.08,
                "Minimum normalized weight to be considered not a noise");
    }
}
