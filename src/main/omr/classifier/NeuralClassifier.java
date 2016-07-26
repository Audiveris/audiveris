//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 N e u r a l C l a s s i f i e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright Â© HervÃ© Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier;

import omr.WellKnowns;
import static omr.classifier.ShapeDescription.FEATURE_COUNT;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;

import omr.util.OmrExecutors;
import omr.util.UriUtil;

import org.deeplearning4j.nn.api.Updater;
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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
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
 * @author HervÃ© Bitteur
 */
public class NeuralClassifier
        extends AbstractClassifier
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            NeuralClassifier.class);

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
    public static final String MODEL_FILE_NAME = "model.zip";

    /** Feature norms file name. */
    public static final String NORMS_FILE_NAME = "norms.zip";

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** The underlying neural network. */
    private MultiLayerNetwork model;

    /** Features means and standard deviations. */
    private Norms norms;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Private constructor, to create a glyph neural network.
     */
    private NeuralClassifier ()
    {
        // Unmarshal from user or default data, if compatible
        load();

        if (model == null) {
            // Get a brand new one (not trained)
            logger.info("Creating a brand new {}", getName());
            model = createNetwork();
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

    //------------------//
    // getMaxIterations //
    //------------------//
    /**
     * Selector on the maximum number of training iterations.
     *
     * @return the upper limit on iteration counter
     */
    public static int getMaxIterations ()
    {
        return constants.maxIterations.getValue();
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

    //------//
    // dump //
    //------//
    /**
     * Dump the internals of the neural network to the standard output.
     */
    public void dump ()
    {
        logger.warn("dump not yet implemented");

        ///engine.dump();
    }

    //--------------//
    // getAmplitude //
    //--------------//
    /**
     * Selector for the amplitude value (used in initial random values).
     *
     * @return the amplitude value
     */
    public double getAmplitude ()
    {
        return constants.amplitude.getValue();
    }

    //-----------------//
    // getLearningRate //
    //-----------------//
    /**
     * Selector of the current value for network learning rate.
     *
     * @return the current learning rate
     */
    public double getLearningRate ()
    {
        return constants.learningRate.getValue();
    }

    //-------------//
    // getMaxError //
    //-------------//
    /**
     * Report the error threshold to potentially stop the training
     * process.
     *
     * @return the threshold currently in use
     */
    public double getMaxError ()
    {
        return constants.maxError.getValue();
    }

    //------------//
    // getNetwork //
    //------------//
    /**
     * Selector to the encapsulated Neural Network.
     *
     * @return the neural network
     */
    public MultiLayerNetwork getModel ()
    {
        return model;
    }

    //-------------//
    // getMomentum //
    //-------------//
    /**
     * Report the momentum training value currently in use.
     *
     * @return the momentum in use
     */
    public double getMomentum ()
    {
        return constants.momentum.getValue();
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

        INDArray output = model.output(features);

        // Normalize all outputs, so that they sum to 1
        Number sum = output.sumNumber();
        output.divi(sum);

        Evaluation[] evals = new Evaluation[SHAPE_COUNT];
        Shape[] values = Shape.values();

        for (int s = 0; s < SHAPE_COUNT; s++) {
            evals[s] = new Evaluation(values[s], output.getDouble(s));
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
    // setAmplitude //
    //--------------//
    /**
     * Set the amplitude value for initial random values (UNUSED).
     *
     * @param amplitude
     */
    public void setAmplitude (double amplitude)
    {
        constants.amplitude.setValue(amplitude);
    }

    //-----------------//
    // setLearningRate //
    //-----------------//
    /**
     * Dynamically modify the learning rate of the neural network for
     * its training task.
     *
     * @param learningRate new learning rate to use
     */
    public void setLearningRate (double learningRate)
    {
        constants.learningRate.setValue(learningRate);

        ///model.setLearningRate(learningRate);
    }

    //--------------//
    // setListeners //
    //--------------//
    public void setListeners (Monitor... monitor)
    {
        model.setListeners(monitor);
    }

    //-------------//
    // setMaxError //
    //-------------//
    /**
     * Modify the error threshold to potentially stop the training
     * process.
     *
     * @param maxError the new threshold value to use
     */
    public void setMaxError (double maxError)
    {
        constants.maxError.setValue(maxError);

        ///engine.setMaxError(maxError);
    }

    //------------------//
    // setMaxIterations //
    //------------------//
    /**
     * Modify the upper limit on the number of iterations for the training process.
     *
     * @param maxIterations new value for iteration limit
     */
    public void setMaxIterations (int maxIterations)
    {
        constants.maxIterations.setValue(maxIterations);

        ///engine.setMaxIterations(maxIterations);
    }

    //-------------//
    // setMomentum //
    //-------------//
    /**
     * Modify the value for momentum used from learning epoch to the
     * other.
     *
     * @param momentum the new momentum value to be used
     */
    public void setMomentum (double momentum)
    {
        constants.momentum.setValue(momentum);

        ///engine.setMomentum(momentum);
    }

    //------//
    // stop //
    //------//
    /**
     * Stop the on-going training.
     * By default, this is a no-op
     */
    public void stop ()
    {
    }

    //-------//
    // store //
    //-------//
    /**
     * Store the engine internals (model + norms), always as user files.
     */
    public void store ()
    {
        final Path modelPath = WellKnowns.EVAL_FOLDER.resolve(MODEL_FILE_NAME);
        final Path normsPath = WellKnowns.EVAL_FOLDER.resolve(NORMS_FILE_NAME);

        try {
            if (!Files.exists(WellKnowns.EVAL_FOLDER)) {
                Files.createDirectories(WellKnowns.EVAL_FOLDER);
                logger.info("Created directory {}", WellKnowns.EVAL_FOLDER);
            }

            storeModel(modelPath);
            storeNorms(normsPath);

            logger.info("{} data stored to folder {}", getName(), WellKnowns.EVAL_FOLDER);
        } catch (Exception ex) {
            logger.warn("Error storing {} {}", getName(), ex.toString(), ex);
        }
    }

    //-------//
    // train //
    //-------//
    /**
     * Train the network using the provided collection of shape samples.
     *
     * @param samples the provided collection of shapes samples
     * @param monitor the monitoring entity if any
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

        // Train
        model = createNetwork();
        model.setListeners(monitor);
        model.fit(dataSet);

        // Evaluate
        final List<String> names = Arrays.asList(ShapeSet.getPhysicalShapeNames());
        org.deeplearning4j.eval.Evaluation eval = new org.deeplearning4j.eval.Evaluation(names);
        INDArray guesses = model.output(dataSet.getFeatureMatrix());
        eval.eval(dataSet.getLabels(), guesses);
        logger.info(eval.stats(true));

        // Store
        store();
    }

    //---------------//
    // createNetwork //
    //---------------//
    private MultiLayerNetwork createNetwork ()
    {
        final int hiddenNum = SHAPE_COUNT;
        final long seed = 6;
        final int iterations = getMaxIterations();
        final double learningRate = getLearningRate();
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder() //
                .seed(seed).iterations(iterations) //
                .activation("relu") //
                .weightInit(WeightInit.XAVIER) //
                .learningRate(learningRate) //
                .regularization(true) //
                .l2(1e-4) //
                .list() //
                .layer(
                        0,
                        new DenseLayer.Builder() //
                        .nIn(ShapeDescription.length()).nOut(hiddenNum).build()) //
                .layer(
                        1,
                        new DenseLayer.Builder() //
                        .nIn(hiddenNum).nOut(hiddenNum).build()) //
                .layer(
                        2,
                        new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD) //
                        .activation("softplus") // softplus vs tanh vs sigmoid vs softmax
                        .nIn(hiddenNum).nOut(SHAPE_COUNT).build()) //
                .backprop(true) //
                .pretrain(false) //
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        return model;

        //
        //        // Note : We allocate a hidden layer with as many cells as the output layer
        //        return new NeuralNetwork(
        //                ShapeDescription.length(),
        //                SHAPE_COUNT,
        //                SHAPE_COUNT,
        //                getAmplitude(),
        //                ShapeDescription.getParameterLabels(), // Input labels
        //                ShapeSet.getPhysicalShapeNames(), // Output labels
        //                getLearningRate(),
        //                getMomentum(),
        //                getMaxError(),
        //                getMaxIterations());
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
            final Path modelPath = WellKnowns.EVAL_FOLDER.resolve(MODEL_FILE_NAME);
            final Path normsPath = WellKnowns.EVAL_FOLDER.resolve(NORMS_FILE_NAME);

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
            ///return ModelSerializer.restoreMultiLayerNetwork(is);
            return restoreMultiLayerNetwork(is);
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

    /**
     * HB: ability to read model from an inputStream, to be replaced ASAP by a suitable
     * version of ModelSerializer.
     */
    private static MultiLayerNetwork restoreMultiLayerNetwork (InputStream is)
            throws IOException
    {
        final ZipInputStream zis = new ZipInputStream(is);

        boolean gotConfig = false;
        boolean gotCoefficients = false;
        boolean gotUpdater = false;

        String json = "";
        INDArray params = null;
        Updater updater = null;

        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {
            logger.info("zis entry: {}", entry);

            switch (entry.getName()) {
            case "configuration.json":

                BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
                String line = "";
                StringBuilder js = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    js.append(line).append("\n");
                }

                json = js.toString();

                gotConfig = true;

                break;

            case "coefficients.bin":

                DataInputStream dis = new DataInputStream(zis);
                params = Nd4j.read(dis);
                gotCoefficients = true;

                break;

            case "updater.bin":

                ObjectInputStream ois = new ObjectInputStream(zis);

                try {
                    updater = (Updater) ois.readObject();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                gotUpdater = true;

                break;
            }

            zis.closeEntry();
        }

        logger.info("zis closed.");
        zis.close();

        if (gotConfig && gotCoefficients) {
            MultiLayerConfiguration confFromJson = MultiLayerConfiguration.fromJson(json);
            MultiLayerNetwork network = new MultiLayerNetwork(confFromJson);
            network.init();
            network.setParameters(params);

            if (gotUpdater && (updater != null)) {
                network.setUpdater(updater);
            }

            return network;
        } else {
            throw new IllegalStateException(
                    "Model wasnt found within file: gotConfig: [" + gotConfig
                    + "], gotCoefficients: [" + gotCoefficients + "], gotUpdater: [" + gotUpdater + "]");
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
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio amplitude = new Constant.Ratio(
                0.5,
                "Initial weight amplitude");

        private final Constant.Ratio learningRate = new Constant.Ratio(0.1, "Learning Rate");

        private final Constant.Integer maxIterations = new Constant.Integer(
                "Iterations",
                5000,
                "Maximum number of iterations in training");

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
