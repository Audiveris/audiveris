//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  B a s i c C l a s s i f i e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.math.NeuralNetwork;
import org.audiveris.omr.math.PoorManAlgebra.DataSet;
import org.audiveris.omr.math.PoorManAlgebra.INDArray;
import org.audiveris.omr.math.PoorManAlgebra.Nd4j;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.StopWatch;

//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.dataset.DataSet;
//import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.CREATE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BasicClassifier} is the pre-DL4J classifier, based on a home-built
 * shallow network operating on MixGlyphDescriptor.
 *
 * @author Hervé Bitteur
 */
public class BasicClassifier
        extends AbstractClassifier<NeuralNetwork>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BasicClassifier.class);

    /** The singleton. */
    private static volatile BasicClassifier INSTANCE;

    /** Classifier file name. */
    public static final String FILE_NAME = "basic-classifier.zip";

    /** Model entry name. */
    public static final String MODEL_ENTRY_NAME = "model.xml";

    //~ Instance fields ----------------------------------------------------------------------------
    /** The underlying (old) neural network. */
    private NeuralNetwork model;

    /** Training listener, if any. */
    private TrainingMonitor listener;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Private constructor, to create a glyph neural network.
     */
    private BasicClassifier ()
    {
        descriptor = new MixGlyphDescriptor();

        // Unmarshal from user or default data, if compatible
        model = load(FILE_NAME);

        if (model == null) {
            model = createNetwork();
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        if (INSTANCE == null) {
            synchronized (BasicClassifier.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BasicClassifier();
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
    @Override
    public int getMaxEpochs ()
    {
        return constants.maxEpochs.getValue();
    }

    //---------//
    // getName //
    //---------//
    @Override
    public final String getName ()
    {
        return "Basic Classifier";
    }

    //-------------//
    // addListener //
    //-------------//
    @Override
    public void addListener (TrainingMonitor listener)
    {
        this.listener = listener;
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
        model.run(ins, null, outs);

        for (int s = 0; s < SHAPE_COUNT; s++) {
            evals[s] = new Evaluation(values[s], outs[s]);
        }

        return evals;
    }

    //-------//
    // reset //
    //-------//
    @Override
    public void reset ()
    {
        model = createNetwork();
    }

    //--------------//
    // setMaxEpochs //
    //--------------//
    /**
     * Modify the upper limit on the number of epochs for the training process.
     *
     * @param maxEpochs new value for epochs limit
     */
    @Override
    public void setMaxEpochs (int maxEpochs)
    {
        model.setEpochs(maxEpochs);
        constants.maxEpochs.setValue(maxEpochs);
    }

    //------//
    // stop //
    //------//
    @Override
    public void stop ()
    {
        model.stop();
    }

    //-------//
    // train //
    //-------//
    @SuppressWarnings("unchecked")
    @Override
    public void train (Collection<Sample> samples)
    {
        logger.info("Training on {} samples", samples.size());

        if (samples.isEmpty()) {
            logger.warn("No sample to retrain neural classifier");

            return;
        }

        StopWatch watch = new StopWatch("train");
        watch.start("shuffle");

        // Shuffle the collection of samples
        final List<Sample> newSamples = new ArrayList<Sample>(samples);
        Collections.shuffle(newSamples);

        // Build raw dataset
        watch.start("getRawDataSet");

        final DataSet dataSet = getRawDataSet(newSamples);
        final INDArray features = dataSet.getFeatures();

        // Record mean and standard deviation for every feature
        watch.start("norms");
        norms = new Norms(features.mean(0), features.std(0));
        norms.stds.addi(Nd4j.scalar(Nd4j.EPS_THRESHOLD)); // Safer, to avoid later division by 0
        logger.debug("means:{}", norms.means);
        logger.debug("stds:{}", norms.stds);
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
        model.train(inputs, desiredOutputs, listener, listener.getIterationPeriod());

        // Store
        store(FILE_NAME);
    }

    //--------------//
    // isCompatible //
    //--------------//
    @Override
    protected boolean isCompatible (NeuralNetwork model,
                                    Norms norms)
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

    //-----------//
    // loadModel //
    //-----------//
    @Override
    protected NeuralNetwork loadModel (Path root)
            throws Exception
    {
        Path modelPath = root.resolve(MODEL_ENTRY_NAME);
        InputStream is = Files.newInputStream(modelPath);
        NeuralNetwork nn = NeuralNetwork.unmarshal(is);
        is.close();

        return nn;
    }

    //-----------//
    // loadNorms //
    //-----------//
    /**
     * {@inheritDoc}.
     * <p>
     * Rather than binary we use XML format.
     *
     * @param root the root path to file system
     * @return the loaded Norms instance, or exception is thrown
     * @throws Exception
     */
    @Override
    protected Norms loadNorms (Path root)
            throws Exception
    {
        final JAXBContext jaxbContext = JAXBContext.newInstance(MyVector.class);
        final Unmarshaller um = jaxbContext.createUnmarshaller();

        INDArray means = null;
        INDArray stds = null;

        final Path meansEntry = root.resolve(MEANS_XML_ENTRY_NAME);

        if (meansEntry != null) {
            InputStream is = Files.newInputStream(meansEntry); // READ by default
            BufferedInputStream bis = new BufferedInputStream(is);
            MyVector vector = (MyVector) um.unmarshal(bis);
            means = Nd4j.create(vector.data);
            logger.debug("means:{}", means);
            bis.close();
        }

        final Path stdsEntry = root.resolve(STDS_XML_ENTRY_NAME);

        if (stdsEntry != null) {
            InputStream is = Files.newInputStream(stdsEntry); // READ by default
            BufferedInputStream bis = new BufferedInputStream(is);
            MyVector vector = (MyVector) um.unmarshal(bis);
            stds = Nd4j.create(vector.data);
            logger.debug("stds:{}", stds);
            bis.close();
        }

        if ((means != null) && (stds != null)) {
            logger.info("Classifier loaded XML norms.");

            return new Norms(means, stds);
        }

        return null;
    }

    //------------//
    // storeModel //
    //------------//
    @Override
    protected void storeModel (Path root)
            throws Exception
    {
        Path modelPath = root.resolve(MODEL_ENTRY_NAME);
        OutputStream bos = new BufferedOutputStream(Files.newOutputStream(modelPath, CREATE));
        model.marshal(bos);
        bos.flush();
        bos.close();
        logger.info("Engine marshalled to {}", modelPath);
    }

    //------------//
    // storeNorms //
    //------------//
    /**
     * {@inheritDoc}.
     * <p>
     * Rather than binary, we use XML format.
     *
     * @throws Exception
     */
    @Override
    protected void storeNorms (Path root)
            throws Exception
    {
        final JAXBContext jaxbContext = JAXBContext.newInstance(MyVector.class);

        {
            Path means = root.resolve(MEANS_XML_ENTRY_NAME);
            OutputStream bos = new BufferedOutputStream(Files.newOutputStream(means, CREATE));
            MyVector vector = new MyVector(norms.means);
            Jaxb.marshal(vector, bos, jaxbContext);
            bos.flush();
            bos.close();
        }

        {
            Path stds = root.resolve(STDS_XML_ENTRY_NAME);
            OutputStream bos = new BufferedOutputStream(Files.newOutputStream(stds, CREATE));
            MyVector vector = new MyVector(norms.stds);
            Jaxb.marshal(vector, bos, jaxbContext);
            bos.flush();
            bos.close();
        }
    }

    //---------------//
    // createNetwork //
    //---------------//
    private NeuralNetwork createNetwork ()
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
                getMaxEpochs());
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
        features.subiRowVector(norms.means);
        features.diviRowVector(norms.stds);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Ratio amplitude = new Constant.Ratio(
                0.5,
                "Initial weight amplitude");

        private final Constant.Ratio learningRate = new Constant.Ratio(0.1, "Learning Rate");

        private final Constant.Integer maxEpochs = new Constant.Integer(
                "Epochs",
                500,
                "Maximum number of epochs in training");

        private final Constant.Ratio momentum = new Constant.Ratio(0.2, "Training momentum");
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
        //~ Instance fields ------------------------------------------------------------------------

        @XmlElement(name = "value")
        public double[] data;

        //~ Constructors ---------------------------------------------------------------------------
        public MyVector (INDArray features)
        {
            int cols = features.columns();

            data = new double[cols];

            for (int j = 0; j < cols; j++) {
                data[j] = features.getDouble(j);
            }
        }

        /** Meant for JAXB. */
        private MyVector ()
        {
        }
    }
}
