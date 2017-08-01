//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   D e e p C l a s s i f i e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import static org.audiveris.omr.classifier.Classifier.SHAPE_COUNT;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.math.Population;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.layers.BaseLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.IterationListener;

import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code DeepClassifier} is a shape classifier implemented by a deep
 * convolutional network operating on {@link ImgGlyphDescriptor}.
 *
 * @author Hervé Bitteur
 */
public class DeepClassifier
        extends AbstractClassifier<MultiLayerNetwork>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(DeepClassifier.class);

    /** The singleton. */
    private static volatile DeepClassifier INSTANCE;

    /** Classifier file name. */
    public static final String FILE_NAME = "deep-classifier.zip";

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** The underlying convolutional neural network. */
    private MultiLayerNetwork model;

    //~ Constructors -------------------------------------------------------------------------------
    private DeepClassifier ()
    {
        descriptor = new ImgGlyphDescriptor();

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
     * Report the single instance of NeuralClassifier in the application.
     *
     * @return the instance
     */
    public static DeepClassifier getInstance ()
    {
        if (INSTANCE == null) {
            synchronized (DeepClassifier.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DeepClassifier();
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

    //----------//
    // getModel //
    //----------//
    /**
     * @return the model
     */
    public MultiLayerNetwork getModel ()
    {
        return model;
    }

    //---------//
    // getName //
    //---------//
    @Override
    public final String getName ()
    {
        return "Deep Classifier";
    }

    //-------------//
    // addListener //
    //-------------//
    @Override
    public void addListener (IterationListener listener)
    {
        if (listener != null) {
            Collection<IterationListener> listeners = model.getListeners();

            if (!listeners.contains(listener)) {
                listeners.add(listener);
                model.setListeners(listeners);
            }
        }
    }

    //-----------------------//
    // getNaturalEvaluations //
    //-----------------------//
    @Override
    public Evaluation[] getNaturalEvaluations (Glyph glyph,
                                               int interline)
    {
        final double[] doubles = descriptor.getFeatures(glyph, interline);
        final INDArray features = Nd4j.create(doubles);
        normalize(features);

        final INDArray preOutput;

        synchronized (this) {
            INDArray output = model.output(features, false);
            BaseLayer outputLayer = (BaseLayer) model.getOutputLayer();
            preOutput = outputLayer.preOutput(false);
        }

        Evaluation[] evals = new Evaluation[SHAPE_COUNT];
        Shape[] values = Shape.values();

        for (int s = 0; s < SHAPE_COUNT; s++) {
            double grade = sigmoid(preOutput.getDouble(s)); // Rather than normalized output
            evals[s] = new Evaluation(values[s], grade);
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
        features.subi(norms.means.getDouble(0));
        features.divi(norms.stds.getDouble(0));
    }

    //----------------//
    // removeListener //
    //----------------//
    @Override
    public void removeListener (IterationListener listener)
    {
        if (listener != null) {
            Collection<IterationListener> listeners = model.getListeners();

            if (listeners.contains(listener)) {
                listeners.remove(listener);
                model.setListeners(listeners);
            }
        }
    }

    //-------//
    // reset //
    //-------//
    @Override
    public void reset ()
    {
        // Create a new model and transfer listeners from previous model
        Collection<IterationListener> listeners = model.getListeners();
        model = createNetwork();
        model.setListeners(listeners);
    }

    //--------------//
    // setMaxEpochs //
    //--------------//
    @Override
    public void setMaxEpochs (int maxEpochs)
    {
        constants.maxEpochs.setValue(maxEpochs);
    }

    //-------//
    // train //
    //-------//
    @SuppressWarnings("unchecked")
    @Override
    public void train (Collection<Sample> samples)
    {
        if (samples.isEmpty()) {
            logger.warn("No sample to retrain neural classifier");

            return;
        }

        // Shuffle the collection of samples
        final List<Sample> newSamples = new ArrayList<Sample>(samples);
        Collections.shuffle(newSamples);

        // Build raw dataset
        final DataSet dataSet = getRawDataSet(newSamples);

        // Record mean and standard deviation for *ALL* pixels
        final INDArray features = dataSet.getFeatures();
        logger.info("features rows:{} cols:{}", features.rows(), features.columns());

        Population pop = new Population();
        final int rows = features.rows();
        final int cols = features.columns();

        for (int r = 0; r < rows; r++) {
            INDArray row = features.getRow(r);

            for (int c = 0; c < cols; c++) {
                pop.includeValue(row.getDouble(c));
            }
        }

        logger.info("pop: {}", pop);

        INDArray mean = Nd4j.create(new double[]{pop.getMeanValue()});
        INDArray std = Nd4j.create(
                new double[]{pop.getStandardDeviation() + Nd4j.EPS_THRESHOLD});
        norms = new Norms(mean, std);

        logger.info("norms.means: {}", norms.means);
        logger.info("norms.stds: {}", norms.stds);

        // Normalize
        ///dataSet.normalizeZeroMeanZeroUnitVariance();
        normalize(features);

        logger.info("Training network...");

        final int epochs = getMaxEpochs();

        for (int epoch = 1; epoch <= epochs; epoch++) {
            epochStarted(epoch);

            model.fit(dataSet);

            // Evaluate
            logger.info("Epoch:{} evaluating on training set...", epoch);

            final List<String> names = Arrays.asList(
                    ShapeSet.getPhysicalShapeNames());
            org.deeplearning4j.eval.Evaluation eval = new org.deeplearning4j.eval.Evaluation(names);
            INDArray guesses = model.output(dataSet.getFeatureMatrix());
            eval.eval(dataSet.getLabels(), guesses);
            logger.info(eval.stats(true));

            // Store
            store(FILE_NAME);
        }
    }

    //--------------//
    // isCompatible //
    //--------------//
    @Override
    protected boolean isCompatible (MultiLayerNetwork model,
                                    Norms norms)
    {
        // Check input numbers for norms
        final int normsIn = norms.means.columns();

        if (normsIn != 1) {
            logger.warn("Incompatible norms count:{} expected:{}", normsIn, 1);

            return false;
        }

        // Check input numbers for model
        final org.deeplearning4j.nn.layers.convolution.ConvolutionLayer inputLayer = (org.deeplearning4j.nn.layers.convolution.ConvolutionLayer) model.getLayer(
                0);
        final org.deeplearning4j.nn.conf.layers.ConvolutionLayer confInputLayer = (org.deeplearning4j.nn.conf.layers.ConvolutionLayer) inputLayer.conf()
                .getLayer();
        final int modelIn = confInputLayer.getNIn();

        if (modelIn != 1) {
            logger.warn("Incompatible features count:{} expected:{}", modelIn, 1);

            return false;
        }

        // Check output numbers for model
        final org.deeplearning4j.nn.layers.OutputLayer outputLayer = (org.deeplearning4j.nn.layers.OutputLayer) model.getOutputLayer();
        final org.deeplearning4j.nn.conf.layers.OutputLayer confOutputLayer = (org.deeplearning4j.nn.conf.layers.OutputLayer) outputLayer.conf()
                .getLayer();
        final int modelOut = confOutputLayer.getNOut();

        if (modelOut != SHAPE_COUNT) {
            logger.warn("Incompatible shape count model:{} expected:{}", modelOut, SHAPE_COUNT);

            return false;
        }

        return true;
    }

    //-----------//
    // loadModel //
    //-----------//
    @Override
    protected MultiLayerNetwork loadModel (Path root)
            throws IOException
    {
        return ModelSystemSerializer.restoreMultiLayerNetwork(root, false);
    }

    //------------//
    // storeModel //
    //------------//
    @Override
    protected void storeModel (Path root)
            throws IOException
    {
        ModelSystemSerializer.writeModel(model, root, false);
    }

    //---------//
    // sigmoid //
    //---------//
    /**
     * Simple sigmoid function, with a step around 0 abscissa.
     * It is used here simply to squash raw values into the [0..1] range
     *
     * @param val abscissa
     * @return the related function value
     */
    private static double sigmoid (double val)
    {
        // Lambda chosen to avoid too many values near 0.9999
        return 1.0 / (1.0 + Math.exp(-val / 20));
    }

    //---------------//
    // createNetwork //
    //---------------//
    private MultiLayerNetwork createNetwork ()
    {
        logger.info("Creating a brand new {}", getName());

        final long seed = 6;
        final double learningRate = constants.learningRate.getValue();
        final int iterations = constants.iterations.getValue();

        MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder() //
                .seed(seed) //
                .iterations(iterations) //
                .regularization(true) //
                .l2(0.0005) //
                .learningRate(learningRate) // was .01 in original MNIST example
                //.biasLearningRate(0.02)
                //.learningRateDecayPolicy(LearningRatePolicy.Inverse).lrPolicyDecayRate(0.001).lrPolicyPower(0.75)
                .weightInit(WeightInit.XAVIER) //
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT) //
                .updater(Updater.NESTEROVS).momentum(0.9) //
                .list() //
                .layer(
                        0,
                        new ConvolutionLayer.Builder(5, 5) //
                                .nIn(1) //
                                .stride(1, 1) //
                                .nOut(20) //
                                .activation(Activation.IDENTITY) //
                                .build()) //
                .layer(
                        1,
                        new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX) //
                                .kernelSize(2, 2) //
                                .stride(2, 2) //
                                .build()) //
                .layer(
                        2,
                        new ConvolutionLayer.Builder(5, 5) //
                                .stride(1, 1) //
                                .nOut(50) //
                                .activation(Activation.IDENTITY) //
                                .build()) //
                .layer(
                        3,
                        new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX) //
                                .kernelSize(2, 2) //
                                .stride(2, 2) //
                                .build()) //
                .layer(
                        4,
                        new DenseLayer.Builder() //
                                .nOut(500) //
                                .activation(Activation.RELU) //
                                .build()) //
                .layer(
                        5,
                        new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD) //
                                .nOut(SHAPE_COUNT) //
                                .activation(Activation.SOFTMAX) //
                                .build()) //
                .setInputType(InputType.convolutionalFlat(ScaledBuffer.HEIGHT, ScaledBuffer.WIDTH, 1));

        MultiLayerConfiguration conf = builder.build();
        model = new MultiLayerNetwork(conf);
        model.init();

        return model;
    }

    //--------------//
    // epochStarted //
    //--------------//
    private void epochStarted (int epoch)
    {
        for (IterationListener listener : model.getListeners()) {
            if (listener instanceof TrainingMonitor) {
                TrainingMonitor monitor = (TrainingMonitor) listener;
                monitor.epochStarted(epoch);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio learningRate = new Constant.Ratio(0.002, "Learning Rate");

        private final Constant.Integer maxEpochs = new Constant.Integer(
                "Epochs",
                30,
                "Maximum number of epochs in training");

        private final Constant.Integer iterations = new Constant.Integer(
                "Iterations",
                2,
                "Number of iterations on each minibatch");
    }
}
