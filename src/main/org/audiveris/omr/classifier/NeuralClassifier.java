//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 N e u r a l C l a s s i f i e r                                //
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
import static org.audiveris.omr.classifier.ShapeDescription.FEATURE_COUNT;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.math.NeuralNetwork;
import org.audiveris.omr.util.OmrExecutors;
import org.audiveris.omr.util.UriUtil;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.util.ModelSerializer;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Class {@code NeuralClassifier} encapsulates a neural network customized for glyph
 * recognition.
 * <p>
 * It wraps the {@link MultiLayerNetwork} with application information, for training, storing,
 * loading and using the neural network.
 * <p>
 * Besides the MultiLayerNetwork model, it handles features norms (means and standard deviations)
 * needed to normalize glyph features before they are submitted to the engine.
 * <p>
 * The classifier data is thus composed of two parts (model and norms) which are loaded as a whole
 * according to the following algorithm: <ol>
 * <li>It first tries to find data in the application user local area ('eval').
 * If found, this data contains a custom definition of model+norms, typically after a user
 * training.</li>
 * <li>If not found, it falls back reading the default definition from the application resource,
 * reading the 'res' folder in the application program area.</ol></p>
 * <p>
 * After any user training, the data is stored as the custom definition in the user local area,
 * which will be picked up first when the application is run again.</p>
 *
 * @author Hervé Bitteur
 */
public class NeuralClassifier
        extends AbstractClassifier
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            NeuralClassifier.class);

    /** True for DeepLearning4J, false for (obsolete) NeuralNetwork implementation. */
    public static final boolean USE_DL4J = constants.useDl4j.isSet();

    /** The singleton. */
    private static volatile NeuralClassifier INSTANCE;

    /** A future which reflects whether instance has been initialized. */
    private static final Future<Void> loading = OmrExecutors.getCachedLowExecutor().submit(
            new Callable<Void>()
    {
        @Override
        public Void call ()
                throws Exception
        {
            try {
                logger.debug("Allocating instance for NeuralClassifier...");
                NeuralClassifier.getInstance();
                logger.debug("NeuralClassifier allocated.");
            } catch (Exception ex) {
                logger.warn("Error pre-loading NeuralClassifier", ex);
                throw ex;
            }

            return null;
        }
    });

    /** Neural network file name. */
    public static final String MODEL_FILE_NAME = USE_DL4J ? "model.zip" : "old-model.xml";

    /** Feature norms file name. */
    public static final String NORMS_FILE_NAME = "norms.zip";

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** The underlying (new) neural network. */
    private MultiLayerNetwork model;

    /** The underlying (old) neural network. */
    @Deprecated
    private NeuralNetwork oldModel;

    /** Features means and standard deviations. */
    private Norms norms;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Private constructor, to create a glyph neural network.
     */
    private NeuralClassifier ()
    {
        if (USE_DL4J) {
            // Unmarshal from user or default data, if compatible
            load();

            if (model == null) {
                // Get a brand new one (not trained)
                logger.info("Creating a brand new {}", getName());
                model = createNetwork();
            }
        } else {
            // Unmarshal from user or default data, if compatible
            loadOld();

            if (oldModel == null) {
                // Get a brand new one (not trained)
                logger.info("Creating a brand new {}", getName());
                oldModel = createOldNetwork();
            }
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of NeuralClassifier in the application.
     *
     * @return the instance
     */
    public static NeuralClassifier getInstance ()
    {
        if (INSTANCE == null) {
            synchronized (NeuralClassifier.class) {
                if (INSTANCE == null) {
                    INSTANCE = new NeuralClassifier();
                }
            }
        }

        return INSTANCE;
    }

    //--------------//
    // getMaxEpochs //
    //--------------//
    /**
     * Selector on the maximum number of training epochs.
     *
     * @return the upper limit on epochs counter
     */
    public static int getMaxEpochs ()
    {
        return constants.maxEpochs.getValue();
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
    public static DataSet getRawDataSet (Collection<Sample> samples)
    {
        final double[][] inputs = new double[samples.size()][];
        final double[][] desiredOutputs = new double[samples.size()][];
        int ig = 0;

        for (Sample sample : samples) {
            double[] ins = ShapeDescription.features(sample, sample.getInterline());
            inputs[ig] = ins;

            double[] des = new double[SHAPE_COUNT];
            Arrays.fill(des, 0);

            des[sample.getShape().getPhysicalShape().ordinal()] = 1;
            desiredOutputs[ig] = des;

            ig++;
        }

        // Build the collection of patterns from the glyph data
        final INDArray features = Nd4j.create(inputs);
        final INDArray labels = Nd4j.create(desiredOutputs);

        return new DataSet(features, labels, null, null);
    }

    //---------//
    // preload //
    //---------//
    /**
     * Empty static method, just to trigger class elaboration (and thus INSTANCE).
     */
    public static void preload ()
    {
    }

    //----------//
    // getModel //
    //----------//
    /**
     * Selector to the encapsulated Neural Network.
     *
     * @return the neural network
     */
    public Object getModel ()
    {
        return USE_DL4J ? model : oldModel;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report a name for this network.
     *
     * @return a simple name
     */
    @Override
    public final String getName ()
    {
        return "Neural";
    }

    //-----------------------//
    // getNaturalEvaluations //
    //-----------------------//
    @Override
    public Evaluation[] getNaturalEvaluations (Glyph glyph,
                                               int interline)
    {
        double[] ins = ShapeDescription.features(glyph, interline);
        final INDArray features = Nd4j.create(ins);
        normalize(features);

        Shape[] values = Shape.values();
        Evaluation[] evals = new Evaluation[SHAPE_COUNT];

        if (USE_DL4J) {
            INDArray output = model.output(features);

            //
            //        INDArray probs = model.labelProbabilities(features);
            //
            //        logger.info("\n--- {}", glyph);
            //
            //        for (int s = 0; s < SHAPE_COUNT; s++) {
            //            logger.info(
            //                    String.format(
            //                            "%20s prob:%.5f new:%.5f",
            //                            values[s],
            //                            probs.getDouble(s),
            //                            output.getDouble(s)));
            //        }
            //
            for (int s = 0; s < SHAPE_COUNT; s++) {
                evals[s] = new Evaluation(values[s], output.getDouble(s));
            }
        } else {
            // Normalize inputs
            for (int i = 0; i < ins.length; i++) {
                ins[i] = features.getDouble(i);
            }

            double[] outs = new double[SHAPE_COUNT];
            oldModel.run(ins, null, outs);
            normalize(outs);

            for (int s = 0; s < SHAPE_COUNT; s++) {
                evals[s] = new Evaluation(values[s], outs[s]);
            }
        }

        return evals;
    }

    //-----------//
    // normalize //
    //-----------//
    /**
     * Apply the known norms on the provided (raw) features.
     *
     * @param features raw features, to be normalized in situ
     */
    public void normalize (INDArray features)
    {
        features.subiRowVector(norms.means);
        features.diviRowVector(norms.stds);
    }

    //--------------//
    // setListeners //
    //--------------//
    public void setListeners (Monitor... monitor)
    {
        if (model != null) {
            model.setListeners(monitor);
        }
    }

    //--------------//
    // setMaxEpochs //
    //--------------//
    /**
     * Modify the upper limit on the number of epochs for the training process.
     *
     * @param maxEpochs new value for epochs limit
     */
    public void setMaxEpochs (int maxEpochs)
    {
        constants.maxEpochs.setValue(maxEpochs);
    }

    //-------//
    // train //
    //-------//
    /**
     * Train the network using the provided collection of shape samples.
     *
     * @param samples the provided collection of shapes samples
     * @param monitor the monitoring entity
     */
    @SuppressWarnings("unchecked")
    public void train (Collection<Sample> samples,
                       Monitor monitor)
    {
        if (samples.isEmpty()) {
            logger.warn("No sample to retrain Neural Network classifier");

            return;
        }

        // Shuffle the collection of samples
        final List<Sample> newSamples = new ArrayList<Sample>(samples);
        Collections.shuffle(newSamples);

        final DataSet dataSet = getRawDataSet(newSamples);

        // Record means and standard deviations
        norms = new Norms(dataSet.getFeatures().mean(0), dataSet.getFeatures().std(0));
        norms.stds.addi(Nd4j.scalar(Nd4j.EPS_THRESHOLD)); // Safer, to avoid later division by 0

        // Normalize
        dataSet.normalizeZeroMeanZeroUnitVariance();

        if (USE_DL4J) {
            // Train
            model = createNetwork();
            model.setListeners(monitor);

            final int epochs = getMaxEpochs();

            for (int epoch = 0; epoch < epochs; epoch++) {
                model.fit(dataSet);
            }

            // Evaluate
            final List<String> names = Arrays.asList(
                    ShapeSet.getPhysicalShapeNames());
            org.deeplearning4j.eval.Evaluation eval = new org.deeplearning4j.eval.Evaluation(names);
            INDArray guesses = model.output(dataSet.getFeatureMatrix());
            eval.eval(dataSet.getLabels(), guesses);
            logger.info(eval.stats(true));

            // Store
            store();
        } else {
            // Build the collection of patterns from the glyph data
            INDArray features = dataSet.getFeatureMatrix();
            int rows = features.rows();
            int cols = features.columns();
            logger.info("patterns: {}", rows);
            logger.info("features: {}", cols);

            INDArray labels = dataSet.getLabels();
            double[][] inputs = new double[newSamples.size()][];
            double[][] desiredOutputs = new double[newSamples.size()][];

            for (int ig = 0; ig < rows; ig++) {
                INDArray featureRow = features.getRow(ig);
                double[] ins = new double[cols];
                inputs[ig] = ins;

                for (int j = 0; j < cols; j++) {
                    ins[j] = featureRow.getDouble(j);
                }

                INDArray labelRow = labels.getRow(ig);
                double[] des = new double[SHAPE_COUNT];
                desiredOutputs[ig] = des;

                for (int j = 0; j < SHAPE_COUNT; j++) {
                    des[j] = labelRow.getDouble(j);
                }
            }

            // Train
            oldModel = createOldNetwork();
            oldModel.train(inputs, desiredOutputs, monitor, monitor.getEpochPeriod());

            // Store
            storeOld();
        }
    }

    //---------------//
    // createNetwork //
    //---------------//
    private MultiLayerNetwork createNetwork ()
    {
        final int hiddenNum = SHAPE_COUNT;
        final long seed = 6;
        final double learningRate = constants.learningRate.getValue();

        //        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed).iterations(1)
        //                .activation("relu").weightInit(WeightInit.RELU).learningRate(learningRate)
        //                .regularization(true).l2(1e-4)
        //                .list()
        //                .layer(0, new DenseLayer.Builder().nIn(ShapeDescription.length()).nOut(hiddenNum).build())
        //                .layer(1, new DenseLayer.Builder().nIn(hiddenNum).nOut(hiddenNum).build())
        //                .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
        //                        .activation("softmax").nIn(hiddenNum).nOut(SHAPE_COUNT).build())
        //                .backprop(true).pretrain(false).build();
        //
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder() //
                .seed(seed) //
                .iterations(1) //
                .activation("relu") //
                .weightInit(WeightInit.RELU) //
                .learningRate(learningRate) //
                .regularization(true) //
                .l2(1e-4) //
                .list() //
                .layer(
                        0,
                        new DenseLayer.Builder() //
                        .nIn(ShapeDescription.length()) //
                        .nOut(hiddenNum) //
                        .build()) //
                .layer(
                        1,
                        new DenseLayer.Builder() //
                        .nIn(hiddenNum) //
                        .nOut(hiddenNum) //
                        .build()) //
                .layer(
                        2,
                        new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD) //
                        .activation("softmax") // softplus vs tanh vs sigmoid vs softmax
                        .nIn(hiddenNum) //
                        .nOut(SHAPE_COUNT) //
                        .build()) //
                .backprop(true) //
                .pretrain(false) //
                .build();
        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        return model;
    }

    //------------------//
    // createOldNetwork //
    //------------------//
    private NeuralNetwork createOldNetwork ()
    {
        // We allocate a hidden layer with as many cells as the output layer
        return new NeuralNetwork(
                ShapeDescription.length(),
                SHAPE_COUNT,
                SHAPE_COUNT,
                constants.amplitude.getValue(),
                ShapeDescription.getParameterLabels(), // Input labels
                ShapeSet.getPhysicalShapeNames(), // Output labels
                constants.learningRate.getValue(),
                constants.momentum.getValue(),
                constants.maxError.getValue(),
                getMaxEpochs());
    }

    //--------------//
    // isCompatible //
    //--------------//
    @Deprecated
    private final boolean isCompatible (NeuralNetwork oldModel)
    {
        if (!Arrays.equals(oldModel.getInputLabels(), ShapeDescription.getParameterLabels())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Engine inputs: {}", Arrays.toString(oldModel.getInputLabels()));
                logger.debug(
                        "Shape  inputs: {}",
                        Arrays.toString(ShapeDescription.getParameterLabels()));
            }

            return false;
        }

        if (!Arrays.equals(oldModel.getOutputLabels(), ShapeSet.getPhysicalShapeNames())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Engine  outputs: {}", Arrays.toString(oldModel.getOutputLabels()));
                logger.debug(
                        "Physical shapes: {}",
                        Arrays.toString(ShapeSet.getPhysicalShapeNames()));
            }

            return false;
        }

        return true;
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
    private boolean isCompatible (MultiLayerNetwork model,
                                  Norms norms)
    {
        // Retrieve input numbers for norms
        final int normsIn = norms.means.columns();

        if (normsIn != FEATURE_COUNT) {
            logger.warn("Incompatible feature count {} vs {}", normsIn, FEATURE_COUNT);

            return false;
        }

        // Retrieve input numbers for model
        final org.deeplearning4j.nn.layers.feedforward.dense.DenseLayer inputLayer = (org.deeplearning4j.nn.layers.feedforward.dense.DenseLayer) model.getLayer(
                0);
        final org.deeplearning4j.nn.conf.layers.DenseLayer confInputLayer = (org.deeplearning4j.nn.conf.layers.DenseLayer) inputLayer.conf()
                .getLayer();
        final int modelIn = confInputLayer.getNIn();

        if (modelIn != FEATURE_COUNT) {
            logger.warn("Incompatible feature count {} vs {}", modelIn, FEATURE_COUNT);

            return false;
        }

        // Retrieve output numbers for model
        final org.deeplearning4j.nn.layers.OutputLayer outputLayer = (org.deeplearning4j.nn.layers.OutputLayer) model.getOutputLayer();
        final org.deeplearning4j.nn.conf.layers.OutputLayer confOutputLayer = (org.deeplearning4j.nn.conf.layers.OutputLayer) outputLayer.conf()
                .getLayer();
        final int modelOut = confOutputLayer.getNOut();

        if (modelOut != SHAPE_COUNT) {
            logger.warn("Incompatible shape count {} vs {}", modelOut, SHAPE_COUNT);

            return false;
        }

        return true;
    }

    //------//
    // load //
    //------//
    /**
     * Load model and norms from the most suitable data files.
     * If user files do not exist or cannot be unmarshalled, the default files are used.
     */
    private void load ()
    {
        // First, try user data, if any, in local EVAL folder
        logger.debug("Trying user data");

        {
            final Path modelPath = WellKnowns.TRAIN_FOLDER.resolve(MODEL_FILE_NAME);
            final Path normsPath = WellKnowns.TRAIN_FOLDER.resolve(NORMS_FILE_NAME);

            if (Files.exists(modelPath) && Files.exists(normsPath)) {
                try {
                    logger.debug("loadModel...");
                    model = loadModel(new FileInputStream(modelPath.toFile()));
                    logger.debug("loadNorms...");
                    norms = loadNorms(new FileInputStream(normsPath.toFile()));
                    logger.debug("loaded.");

                    if (!isCompatible(model, norms)) {
                        final String msg = "Obsolete neural user data in " + modelPath
                                           + ", trying default data";
                        logger.warn(msg);
                    } else {
                        // Tell user we are not using the default
                        logger.debug("Neural loaded from {}", modelPath);

                        return; // Normal exit
                    }
                } catch (Exception ex) {
                    logger.warn("Load error {}", ex.toString(), ex);
                    model = null;
                    norms = null;
                }
            }
        }

        // Second, use default data (in program RES folder)
        logger.debug("Trying default data");

        final URI modelUri = UriUtil.toURI(WellKnowns.RES_URI, MODEL_FILE_NAME);
        final URI normsUri = UriUtil.toURI(WellKnowns.RES_URI, NORMS_FILE_NAME);

        try {
            model = loadModel(modelUri.toURL().openStream());
            norms = loadNorms(normsUri.toURL().openStream());

            if (!isCompatible(model, norms)) {
                final String msg = "Obsolete neural default data in " + modelUri
                                   + ", please retrain from scratch";
                logger.warn(msg);
            } else {
                logger.debug("Neural loaded from {}", modelUri);

                return; // Normal exit
            }
        } catch (Exception ex) {
            logger.warn("Load error {}", ex.toString(), ex);
        }

        model = null;
        norms = null;
    }

    //-----------//
    // loadModel //
    //-----------//
    private MultiLayerNetwork loadModel (InputStream is)
            throws IOException
    {
        try {
            return ModelSerializer.restoreMultiLayerNetwork(is);
        } catch (Throwable ex) {
            logger.warn("Error restoring network {}", ex.toString(), ex);

            return null;
        } finally {
            is.close();
        }
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
     */
    private Norms loadNorms (InputStream is)
            throws IOException, JAXBException
    {
        INDArray means = null;
        INDArray stds = null;

        try {
            final ZipInputStream zis = new ZipInputStream(is);
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                switch (entry.getName()) {
                case "names.xml": {
                    //                    DataInputStream dis = new DataInputStream(zis);
                    //                    Unmarshaller um = FeatureNames.getJaxbContext().createUnmarshaller();
                    //                    FeatureNames fn = (FeatureNames) um.unmarshal(dis); // Closes stream!!!
                }

                break;

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

    //---------//
    // loadOld //
    //---------//
    /**
     * Load model and norms from the most suitable data files.
     * If user files do not exist or cannot be unmarshalled, the default files are used.
     */
    @Deprecated
    private void loadOld ()
    {
        // First, try user data, if any, in local EVAL folder
        logger.debug("Trying user data");

        {
            final Path modelPath = WellKnowns.TRAIN_FOLDER.resolve(MODEL_FILE_NAME);
            final Path normsPath = WellKnowns.TRAIN_FOLDER.resolve(NORMS_FILE_NAME);

            if (Files.exists(modelPath) && Files.exists(normsPath)) {
                InputStream is = null;

                try {
                    logger.debug("loadModel...");
                    ///oldModel = loadModel(new FileInputStream(modelPath.toFile()));
                    is = new FileInputStream(modelPath.toFile());
                    oldModel = NeuralNetwork.unmarshal(is);

                    logger.debug("loadNorms...");
                    norms = loadNorms(new FileInputStream(normsPath.toFile()));
                    logger.debug("loaded.");

                    if (!isCompatible(oldModel)) {
                        final String msg = "Obsolete neural user data in " + modelPath
                                           + ", trying default data";
                        logger.warn(msg);
                    } else {
                        // Tell user we are not using the default
                        logger.debug("Neural loaded from {}", modelPath);

                        return; // Normal exit
                    }
                } catch (Exception ex) {
                    logger.warn("Load error {}", ex.toString(), ex);
                    oldModel = null;
                    norms = null;
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        // Second, use default data (in program RES folder)
        logger.debug("Trying default data");

        final URI modelUri = UriUtil.toURI(WellKnowns.RES_URI, MODEL_FILE_NAME);
        final URI normsUri = UriUtil.toURI(WellKnowns.RES_URI, NORMS_FILE_NAME);
        InputStream is = null;

        try {
            ///oldModel = loadModel(modelUri.toURL().openStream());
            is = modelUri.toURL().openStream();
            oldModel = NeuralNetwork.unmarshal(is);

            norms = loadNorms(normsUri.toURL().openStream());

            if (!isCompatible(oldModel)) {
                final String msg = "Obsolete neural default data in " + modelUri
                                   + ", please retrain from scratch";
                logger.warn(msg);
            } else {
                logger.debug("Neural loaded from {}", modelUri);

                return; // Normal exit
            }
        } catch (Exception ex) {
            logger.warn("Load error {}", ex.toString(), ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        }

        oldModel = null;
        norms = null;
    }

    //-----------//
    // normalize //
    //-----------//
    /**
     * Adjust all values, so that their sum equals 1
     *
     * @param vals the probability values
     */
    private void normalize (double[] vals)
    {
        double sum = 0;

        for (double val : vals) {
            sum += val;
        }

        for (int i = 0; i < vals.length; i++) {
            vals[i] /= sum;
        }
    }

    //-------//
    // store //
    //-------//
    /**
     * Store the engine internals (model + norms), always as user files.
     */
    private void store ()
    {
        final Path modelPath = WellKnowns.TRAIN_FOLDER.resolve(MODEL_FILE_NAME);
        final Path normsPath = WellKnowns.TRAIN_FOLDER.resolve(NORMS_FILE_NAME);

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
    private void storeModel (Path modelPath)
            throws IOException
    {
        ModelSerializer.writeModel(model, modelPath.toFile(), false);
    }

    //------------//
    // storeNorms //
    //------------//
    private void storeNorms (Path normsPath)
            throws FileNotFoundException, IOException, JAXBException
    {
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(normsPath.toFile()));
        ZipOutputStream zos = new ZipOutputStream(stream);

        {
            ZipEntry names = new ZipEntry("names.xml");
            zos.putNextEntry(names);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            Marshaller m = FeatureNames.getJaxbContext().createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            FeatureNames fn = new FeatureNames(ShapeDescription.getParameterLabels());
            m.marshal(fn, dos);
            dos.flush();
            dos.close();

            InputStream inputStream = new ByteArrayInputStream(bos.toByteArray());
            writeEntry(inputStream, zos);
        }

        {
            ZipEntry means = new ZipEntry("means.bin");
            zos.putNextEntry(means);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            Nd4j.write(norms.means, dos);
            dos.flush();
            dos.close();

            InputStream inputStream = new ByteArrayInputStream(bos.toByteArray());
            writeEntry(inputStream, zos);
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
            writeEntry(inputStream, zos);
        }

        zos.flush();
        zos.close();
    }

    //----------//
    // storeOld //
    //----------//
    @Deprecated
    private void storeOld ()
    {
        final Path modelPath = WellKnowns.TRAIN_FOLDER.resolve(MODEL_FILE_NAME);
        final Path normsPath = WellKnowns.TRAIN_FOLDER.resolve(NORMS_FILE_NAME);

        OutputStream os = null;

        try {
            if (!Files.exists(WellKnowns.TRAIN_FOLDER)) {
                Files.createDirectories(WellKnowns.TRAIN_FOLDER);
                logger.info("Created directory {}", WellKnowns.TRAIN_FOLDER);
            }

            ///storeModel(modelPath);
            os = new FileOutputStream(modelPath.toFile());
            oldModel.marshal(os);
            logger.info("Engine marshalled to {}", modelPath);

            storeNorms(normsPath);

            logger.info("{} data stored to folder {}", getName(), WellKnowns.TRAIN_FOLDER);
        } catch (Exception ex) {
            logger.warn("Error storing {} {}", getName(), ex.toString(), ex);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    //------------//
    // writeEntry //
    //------------//
    private static void writeEntry (InputStream inputStream,
                                    ZipOutputStream zipStream)
            throws IOException
    {
        byte[] bytes = new byte[1024];
        int bytesRead;

        while ((bytesRead = inputStream.read(bytes)) != -1) {
            zipStream.write(bytes, 0, bytesRead);
        }
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //---------//
    // Monitor //
    //---------//
    /**
     * Monitoring interface about the training status of a classifier.
     */
    public static interface Monitor
            extends IterationListener
    {
        //~ Methods --------------------------------------------------------------------------------

        public void epochPeriodDone (int epoch,
                                     double score);

        public int getEpochPeriod ();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean useDl4j = new Constant.Boolean(
                true,
                "Should we use DL4J (instead of old NN)?");

        private final Constant.Ratio amplitude = new Constant.Ratio(
                0.5,
                "Initial weight amplitude");

        private final Constant.Ratio learningRate = new Constant.Ratio(0.1, "Learning Rate");

        private final Constant.Integer maxEpochs = new Constant.Integer(
                "Epochs",
                1000,
                "Maximum number of epochs in training");

        private final Evaluation.Grade maxError = new Evaluation.Grade(
                1E-3,
                "Threshold to stop training");

        private final Constant.Ratio momentum = new Constant.Ratio(0.2, "Training momentum");
    }

    //--------------//
    // FeatureNames //
    //--------------//
    @XmlRootElement(name = "features")
    private static class FeatureNames
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static volatile JAXBContext jaxbContext;

        //~ Instance fields ------------------------------------------------------------------------
        @XmlElement(name = "names")
        private final StringArray names;

        //~ Constructors ---------------------------------------------------------------------------
        public FeatureNames (String[] strs)
        {
            names = new StringArray(strs);
        }

        /** Meant for JAXB. */
        private FeatureNames ()
        {
            this.names = null;
        }

        //~ Methods --------------------------------------------------------------------------------
        private static JAXBContext getJaxbContext ()
                throws JAXBException
        {
            // Lazy creation
            if (jaxbContext == null) {
                jaxbContext = JAXBContext.newInstance(FeatureNames.class);
            }

            return jaxbContext;
        }
    }

    //-------//
    // Norms //
    //-------//
    /**
     * Class that encapsulates the means and standard deviations of glyph features.
     */
    private static class Norms
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

    //-------------//
    // StringArray //
    //-------------//
    private static class StringArray
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlValue
        String[] strings;

        //~ Constructors ---------------------------------------------------------------------------
        public StringArray ()
        {
        }

        public StringArray (String[] strings)
        {
            this.strings = strings;
        }
    }
}
