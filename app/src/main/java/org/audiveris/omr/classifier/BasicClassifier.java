//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  B a s i c C l a s s i f i e r                                 //
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
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.math.NeuralNetwork;
import org.audiveris.omr.math.PoorManAlgebra.DataSet;
import org.audiveris.omr.math.PoorManAlgebra.INDArray;
import org.audiveris.omr.math.PoorManAlgebra.Nd4j;
import org.audiveris.omr.util.ChartPlotter;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.UriUtil;
import org.audiveris.omr.util.ZipFileSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.FileUtils;
import org.jfree.data.xy.XYSeries;

import java.awt.Color;
import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.CREATE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>BasicClassifier</code> is the pre-DL4J classifier, based on a home-built
 * shallow network operating on MixGlyphDescriptor.
 *
 * @author Hervé Bitteur
 */
public class BasicClassifier
        extends AbstractClassifier<NeuralNetwork, BasicNorms>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BasicClassifier.class);

    /** Classifier file name. */
    public static final String MODEL_FILE_NAME = "basic-classifier.zip";

    /** Model entry name. */
    public static final String MODEL_ENTRY_NAME = "model.xml";

    /** Entry name for mean values. */
    public static final String MEANS_ENTRY_NAME = "means.bin";

    /** Entry name for mean XML values. */
    public static final String MEANS_XML_ENTRY_NAME = "means.xml";

    /** Entry name for standard deviation values. */
    public static final String STDS_ENTRY_NAME = "stds.bin";

    /** Entry name for standard deviation XML values. */
    public static final String STDS_XML_ENTRY_NAME = "stds.xml";

    //~ Instance fields ----------------------------------------------------------------------------

    /** The underlying neural network. */
    private Network network;

    /** Training listeners, if any. */
    private final Set<TrainingMonitor> listeners = new LinkedHashSet<>();

    /** Specific listener to feed the training charts. */
    private ChartListener chartListener = null;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Private constructor, to create a glyph neural network.
     */
    private BasicClassifier ()
    {
        descriptor = new MixGlyphDescriptor();

        // Unmarshal from user or default data, if compatible
        network = load(MODEL_FILE_NAME);

        if (network == null) {
            network = new Network();
            network.model = createModel();
        }

        if (constants.dumpNetwork.isSet()) {
            network.model.dump();
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------------//
    // addListener //
    //-------------//
    @Override
    public void addListener (TrainingMonitor listener)
    {
        listeners.add(listener);
    }

    //-------------//
    // createModel //
    //-------------//
    private NeuralNetwork createModel ()
    {
        // Get a brand new one (not trained)
        logger.info("Creating a brand new {}", getName());

        // We allocate a hidden layer with as many cells as the output layer
        return new NeuralNetwork(
                descriptor.length(),
                SHAPE_COUNT,
                SHAPE_COUNT,
                constants.amplitude.getValue(),
                descriptor.getFeatureLabels(), // Input labels
                ShapeSet.getPhysicalShapeNames(), // Output labels
                constants.learningRate.getValue(),
                constants.momentum.getValue(),
                constants.lambda.getValue());
    }

    //----------------//
    // getEpochsTotal //
    //----------------//
    @Override
    public int getEpochsTotal ()
    {
        return network.model.getEpochsTotal();
    }

    //-----------//
    // getLambda //
    //-----------//
    @Override
    public double getLambda ()
    {
        return network.model.getLambda();
    }

    //-----------------//
    // getLearningRate //
    //-----------------//
    @Override
    public double getLearningRate ()
    {
        return network.model.getLearningRate();
    }

    //--------------//
    // getMaxEpochs //
    //--------------//
    @Override
    public int getMaxEpochs ()
    {
        return constants.maxEpochs.getValue();
    }

    //-------------//
    // getMomentum //
    //-------------//
    @Override
    public double getMomentum ()
    {
        return network.model.getMomentum();
    }

    //---------//
    // getName //
    //---------//
    @Override
    public final String getName ()
    {
        return "Basic Classifier";
    }

    //-----------------------//
    // getNaturalEvaluations //
    //-----------------------//
    @Override
    public Evaluation[] getNaturalEvaluations (Glyph glyph,
                                               int interline)
    {
        double[] ins = descriptor.getFeatures(glyph, interline);
        final INDArray features = Nd4j.create(ins);
        normalize(features);

        Shape[] values = Shape.values();
        Evaluation[] evals = new Evaluation[SHAPE_COUNT];

        for (int i = 0; i < ins.length; i++) {
            ins[i] = features.getDouble(i);
        }

        double[] outs = new double[SHAPE_COUNT];
        network.model.run(ins, null, outs);

        for (int s = 0; s < SHAPE_COUNT; s++) {
            evals[s] = new Evaluation(values[s], outs[s]);
        }

        return evals;
    }

    //--------------//
    // isCompatible //
    //--------------//
    @Override
    protected boolean isCompatible (NeuralNetwork model,
                                    BasicNorms norms)
    {
        if (!Arrays.equals(model.getInputLabels(), descriptor.getFeatureLabels())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Engine inputs: {}", Arrays.toString(model.getInputLabels()));
                logger.debug("Shape  inputs: {}", Arrays.toString(descriptor.getFeatureLabels()));
            }

            return false;
        }

        if (!Arrays.equals(model.getOutputLabels(), ShapeSet.getPhysicalShapeNames())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Engine  outputs: {}", Arrays.toString(model.getOutputLabels()));
                logger.debug(
                        "Physical shapes: {}",
                        Arrays.toString(ShapeSet.getPhysicalShapeNames()));
            }

            return false;
        }

        return true;
    }

    //------//
    // load //
    //------//
    @Override
    protected Network load (String... fileNames)
    {
        final String modelFileName = fileNames[0];

        // First, try user data, if any, in local train folder
        logger.debug("AbstractClassifier. Trying user data");

        final Path path = getTrainFolder().resolve(modelFileName);

        if (Files.exists(path)) {
            try {
                final Path root = ZipFileSystem.open(path);
                final NeuralNetwork model = loadModel(root);
                final BasicNorms norms = loadNorms(root);
                root.getFileSystem().close();

                if (!isCompatible(model, norms)) {
                    final String msg = "Incompatible classifier user data in " + path
                            + ", trying default data";
                    logger.warn(msg);
                } else {
                    // Tell user we are not using the default
                    logger.info("Local classifier data found at {}", path);

                    return new Network(model, norms); // Normal exit
                }
            } catch (Exception ex) {
                logger.warn("Load error {}", ex.toString(), ex);
            }
        }

        // Second, use default data (in program RES folder)
        logger.debug("AbstractClassifier. Trying default data");

        final URI uri = UriUtil.toURI(WellKnowns.RES_URI, modelFileName);

        try {
            // Must be a path to a true zip *file*
            final Path zipPath;
            logger.debug("uri={}", uri);

            if (uri.toString().startsWith("jar:")) {
                // We have a .zip within a .jar
                // Quick fix: copy the .zip into a separate temp file
                // TODO: investigate a better solution!
                File tmpFile = Files.createTempFile("AbstractClassifier-", ".tmp").toFile();
                logger.debug("tmpFile={}", tmpFile);
                tmpFile.deleteOnExit();

                try (InputStream is = uri.toURL().openStream()) {
                    FileUtils.copyInputStreamToFile(is, tmpFile);
                }
                zipPath = tmpFile.toPath();
            } else {
                zipPath = Paths.get(uri);
            }

            final Path root = ZipFileSystem.open(zipPath);
            final NeuralNetwork model = loadModel(root);
            final BasicNorms norms = loadNorms(root);
            root.getFileSystem().close();

            //            if (!isCompatible(model, norms)) {
            //                final String msg = "Obsolete classifier default data in " + uri
            //                        + ", please retrain from scratch";
            //                logger.warn(msg);
            //            } else {
            //                logger.debug("Classifier data loaded from default uri {}", uri);
            //
            return new Network(model, norms); // Normal exit
            //            }
        } catch (Exception ex) {
            logger.warn("Load error on {} {}", uri, ex.toString(), ex);
        }

        return null; // Failure
    }

    //-----------//
    // loadModel //
    //-----------//
    @Override
    protected NeuralNetwork loadModel (Path root)
        throws Exception
    {
        Path modelPath = root.resolve(MODEL_ENTRY_NAME);

        try (InputStream is = Files.newInputStream(modelPath)) {
            return NeuralNetwork.unmarshal(is);
        }
    }

    //-----------//
    // loadNorms //
    //-----------//
    /**
     * {@inheritDoc}.
     * <p>
     * We use XML format.
     *
     * @param root the root path to file system
     * @return the loaded BasicNorms instance, or exception is thrown
     * @throws Exception if anything goes wrong
     */
    @Override
    protected BasicNorms loadNorms (Path root)
        throws Exception
    {
        final JAXBContext jaxbContext = JAXBContext.newInstance(MyVector.class);
        final Unmarshaller um = jaxbContext.createUnmarshaller();

        INDArray means = null;
        INDArray stds = null;

        final Path meansEntry = root.resolve(MEANS_XML_ENTRY_NAME);

        if (meansEntry != null) {
            try (InputStream is = Files.newInputStream(meansEntry);
                    BufferedInputStream bis = new BufferedInputStream(is)) {
                MyVector vector = (MyVector) um.unmarshal(bis);
                means = Nd4j.create(vector.data);
                logger.debug("means:{}", means);
            }
        }

        final Path stdsEntry = root.resolve(STDS_XML_ENTRY_NAME);

        if (stdsEntry != null) {
            try (InputStream is = Files.newInputStream(stdsEntry);
                    BufferedInputStream bis = new BufferedInputStream(is)) {
                MyVector vector = (MyVector) um.unmarshal(bis);
                stds = Nd4j.create(vector.data);
                logger.debug("stds:{}", stds);
            }
        }

        if ((means != null) && (stds != null)) {
            logger.debug("Classifier loaded XML norms.");

            return new BasicNorms(means, stds);
        }

        return null;
    }

    //-----------//
    // normalize //
    //-----------//
    /**
     * Apply the known norms on the provided (raw) features.
     *
     * @param features raw features, to be normalized in situ
     */
    private void normalize (INDArray features)
    {
        features.subiRowVector(network.norms.means);
        features.diviRowVector(network.norms.stds);
    }

    //-------//
    // reset //
    //-------//
    @Override
    public void reset ()
    {
        network.model = createModel();

        if (constants.dumpNetwork.isSet()) {
            network.model.dump();
        }

        if (chartListener != null) {
            chartListener.reset();
        }
    }

    //-----------//
    // setLambda //
    //-----------//
    @Override
    public void setLambda (double lambda)
    {
        network.model.setLambda(lambda);
        logger.info("Lambda set to {}", lambda);
    }

    //-----------------//
    // setLearningRate //
    //-----------------//
    @Override
    public void setLearningRate (double learningRate)
    {
        network.model.setLearningRate(learningRate);
        logger.info("Learning set to {}", learningRate);
    }

    //-------------//
    // setMomentum //
    //-------------//
    @Override
    public void setMomentum (double momentum)
    {
        network.model.setMomentum(momentum);
        logger.info("Momentum set to {}", momentum);
    }

    //------//
    // stop //
    //------//
    @Override
    public void stop ()
    {
        network.model.stop();
    }

    //-------//
    // store //
    //-------//
    @Override
    protected void store ()
        throws Exception
    {
        final Path trainFolder = getTrainFolder(true);
        final Path path = trainFolder.resolve(MODEL_FILE_NAME);

        try {
            final Path root = ZipFileSystem.create(path); // Delete if already exists

            storeModel(root);
            storeNorms(root);

            root.getFileSystem().close();

            logger.info("{} data stored to {}", getName(), path);
        } catch (Exception ex) {
            logger.warn("Error storing {} {}", getName(), ex.toString(), ex);
        }
    }

    //------------//
    // storeModel //
    //------------//
    @Override
    protected void storeModel (Path root)
        throws Exception
    {
        Path modelPath = root.resolve(MODEL_ENTRY_NAME);

        try (OutputStream bos = new BufferedOutputStream(
                Files.newOutputStream(modelPath, CREATE))) {
            network.model.marshal(bos);
            bos.flush();
        }

        logger.info("Engine marshalled to {}", modelPath);
    }

    //------------//
    // storeNorms //
    //------------//
    @Override
    protected void storeNorms (Path root)
        throws Exception
    {
        final JAXBContext jaxbContext = JAXBContext.newInstance(MyVector.class);
        final Path means = root.resolve(MEANS_XML_ENTRY_NAME);
        final Path stds = root.resolve(STDS_XML_ENTRY_NAME);

        try (OutputStream bos = new BufferedOutputStream(Files.newOutputStream(means, CREATE))) {
            MyVector vector = new MyVector(network.norms.means);
            Jaxb.marshal(vector, bos, jaxbContext);
            bos.flush();
        }

        try (OutputStream bos = new BufferedOutputStream(Files.newOutputStream(stds, CREATE))) {
            MyVector vector = new MyVector(network.norms.stds);
            Jaxb.marshal(vector, bos, jaxbContext);
            bos.flush();
        }
    }

    //-------//
    // train //
    //-------//
    @SuppressWarnings("unchecked")
    @Override
    public void train (Collection<Sample> samples,
                       int epochs)
    {
        logger.info("Training on {} samples", samples.size());

        if (samples.isEmpty()) {
            logger.warn("No sample to retrain neural classifier");

            return;
        }

        if (chartListener == null) {
            addListener(chartListener = new ChartListener());
        }

        final StopWatch watch = new StopWatch("train");
        watch.start("shuffle");

        // Shuffle the collection of samples
        final List<Sample> newSamples = new ArrayList<>(samples);
        Collections.shuffle(newSamples);

        // Build raw dataset
        watch.start("getRawDataSet");

        final DataSet dataSet = getRawDataSet(newSamples);
        final INDArray features = dataSet.getFeatures();

        // Record mean and standard deviation for every feature
        watch.start("norms");
        network.norms = new BasicNorms(features.mean(0), features.std(0));
        network.norms.stds.addi(Nd4j.scalar(Nd4j.EPS_THRESHOLD)); // To avoid later division by 0
        logger.debug("means:{}", network.norms.means);
        logger.debug("stds:{}", network.norms.stds);
        watch.start("normalize");
        normalize(features);

        // Convert features for NeuralNetwork data format
        int rows = features.rows();
        int cols = features.columns();
        logger.info("samples: {}", rows);
        logger.info("features: {}", cols);

        INDArray labels = dataSet.getLabels();
        double[][] inputs = new double[newSamples.size()][];
        double[][] desiredOutputs = new double[newSamples.size()][];
        watch.start("build input & desiredOutputs");

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

        if (constants.printWatch.isSet()) {
            watch.print();
        }

        // Train
        network.model.train(inputs, desiredOutputs, listeners, epochs);

        // Store
        try {
            store();
        } catch (Exception ex) {
            logger.warn("Error storing {} {}", getName(), ex.getMessage(), ex);
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of BasicClassifier in the application.
     *
     * @return the instance
     */
    public static BasicClassifier getInstance ()
    {
        return LazySingleton.INSTANCE;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------------//
    // ChartListener //
    //---------------//
    /**
     * A listener meant to feed the training charts.
     */
    private static class ChartListener
            implements TrainingMonitor
    {
        XYSeries scoreSeries;

        XYSeries hiddenSeries;

        XYSeries outputSeries;

        ChartPlotter scorePlotter;

        ChartPlotter weightsPlotter;

        public ChartListener ()
        {
            reset();
        }

        @Override
        public int getIterationPeriod ()
        {
            return constants.internalIterPeriod.getValue();
        }

        @Override
        public void iterationPeriodDone (int epochsCount,
                                         int iteration,
                                         double score,
                                         double hiddenSquaredWeights,
                                         double outputSquaredWeights)
        {
            scoreSeries.add(epochsCount, score);
            hiddenSeries.add(epochsCount, hiddenSquaredWeights);
            outputSeries.add(epochsCount, outputSquaredWeights);
        }

        public final void reset ()
        {
            scoreSeries = new XYSeries("Score", false);
            hiddenSeries = new XYSeries("Hidden", false);
            outputSeries = new XYSeries("Output", false);
            scorePlotter = new ChartPlotter("Score", "Epochs", "Score");
            weightsPlotter = new ChartPlotter("Weights", "Epochs", "Weights");

            scorePlotter.add(scoreSeries, Color.red);
            scorePlotter.display("Score chart", new Point(50, 50));

            weightsPlotter.add(hiddenSeries, Color.green);
            weightsPlotter.add(outputSeries, Color.blue);
            weightsPlotter.display("Weights  chart", new Point(100, 100));
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean dumpNetwork = new Constant.Boolean(
                false,
                "Should we print out the network parameters?");

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Ratio amplitude = new Constant.Ratio(
                0.5,
                "Initial weight amplitude");

        private final Constant.Ratio learningRate = new Constant.Ratio( //
                0.1,
                "Learning Rate");

        private final Constant.Ratio momentum = new Constant.Ratio( //
                0.9,
                "Training momentum");

        private final Constant.Ratio lambda = new Constant.Ratio( //
                0.0001,
                "Regularization factor");

        private final Constant.Integer maxEpochs = new Constant.Integer(
                "Epochs",
                100,
                "Number of epochs in training session");

        private final Constant.Integer internalIterPeriod = new Constant.Integer(
                "Internal iteration period",
                1,
                "Period to trigger the internal listener");
    }

    //---------------//
    // LazySingleton //
    //---------------//
    private static class LazySingleton
    {
        static final BasicClassifier INSTANCE = new BasicClassifier();
    }

    //----------//
    // MyVector //
    //----------//
    /**
     * Meant to allow JAXB (un)marshalling of norms vectors.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "vector")
    private static class MyVector
    {
        @XmlElement(name = "value")
        public double[] data;

        /** Meant for JAXB. */
        private MyVector ()
        {
        }

        MyVector (INDArray features)
        {
            int cols = features.columns();

            data = new double[cols];

            for (int j = 0; j < cols; j++) {
                data[j] = features.getDouble(j);
            }
        }
    }
}
