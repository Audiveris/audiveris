//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    N e u r a l N e t w o r k                                   //
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
package org.audiveris.omr.math;

import org.audiveris.omr.classifier.TrainingMonitor;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.stream.XMLStreamException;

/**
 * Class <code>NeuralNetwork</code> implements a back-propagation neural
 * network, with one input layer, one hidden layer and one output layer.
 * The transfer function is the sigmoid.
 * <p>
 * This neuralNetwork class can be stored on disk in XML form (through the {@link #marshal} and
 * {@link #unmarshal} methods).
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "neural-network")
public class NeuralNetwork
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(NeuralNetwork.class);

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** Size of input layer. */
    @XmlAttribute(name = "input-size")
    private final int inputSize;

    /** Size of hidden layer. */
    @XmlAttribute(name = "hidden-size")
    private final int hiddenSize;

    /** Size of output layer. */
    @XmlAttribute(name = "output-size")
    private final int outputSize;

    /** Learning Rate parameter. */
    @XmlAttribute(name = "learning-rate")
    private double learningRate;

    /** Momentum for faster convergence. */
    @XmlAttribute(name = "momentum")
    private double momentum;

    /** L2 regularization factor. */
    @XmlAttribute(name = "lambda")
    private double lambda;

    /** Total count of epochs run so far. */
    @XmlAttribute(name = "epochs-total")
    private int epochsTotal = 0;

    /** Labels of input cells. */
    @XmlElement(name = "input-labels")
    private final StringArray inputLabels;

    /** Labels of output cells. */
    @XmlElement(name = "output-labels")
    private final StringArray outputLabels;

    /** Weights to hidden layer. */
    @XmlElementWrapper(name = "hidden-weights")
    @XmlElement(name = "row")
    private double[][] hiddenWeights;

    /** Weights to output layer. */
    @XmlElementWrapper(name = "output-weights")
    @XmlElement(name = "row")
    private double[][] outputWeights;

    // Transient data
    //---------------

    /** To trigger training stop. */
    private transient volatile boolean stopping = false;

    //~ Constructors -------------------------------------------------------------------------------

    /** Private No-argument constructor meant for the JAXB compiler only. */
    private NeuralNetwork ()
    {
        inputSize = -1;
        hiddenSize = -1;
        outputSize = -1;
        inputLabels = null;
        outputLabels = null;
    }

    /**
     * Create a neural network, with specified number of cells in each layer, and default values.
     *
     * @param inputSize    number of cells in input layer
     * @param hiddenSize   number of cells in hidden layer
     * @param outputSize   number of cells in output layer
     * @param amplitude    amplitude (less than or = 1.0) for initial random values
     * @param inputLabels  array of labels for input cells, perhaps empty
     * @param outputLabels array of labels for output cells, perhaps empty
     * @param learningRate learning rate factor
     * @param momentum     momentum from last adjustment
     * @param lambda       L2 regularization factor
     */
    public NeuralNetwork (int inputSize,
                          int hiddenSize,
                          int outputSize,
                          double amplitude,
                          String[] inputLabels,
                          String[] outputLabels,
                          double learningRate,
                          double momentum,
                          double lambda)
    {
        // Cache parameters
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;
        this.learningRate = learningRate;
        this.momentum = momentum;
        this.lambda = lambda;

        // Allocate weights (from input) to hidden layer
        // +1 for bias
        hiddenWeights = createMatrix(hiddenSize, inputSize + 1, amplitude);

        // Allocate weights (from hidden) to output layer
        // +1 for bias
        outputWeights = createMatrix(outputSize, hiddenSize + 1, amplitude);

        // Labels for input, if any
        this.inputLabels = new StringArray(inputLabels);

        if (inputLabels.length != inputSize) {
            throw new IllegalArgumentException(
                    "Inconsistent input labels size " + inputLabels.length + " vs " + inputSize);
        }

        // Labels for output, if any
        this.outputLabels = new StringArray(outputLabels);

        if (outputLabels.length != outputSize) {
            throw new IllegalArgumentException(
                    "Inconsistent output labels size " + outputLabels.length + " vs " + outputSize);
        }

        logger.debug("Network created");

        logger.info(
                String.format(
                        "Initial complexity, hiddenW2: %.1f outputW2: %.1f",
                        w2(hiddenWeights),
                        w2(outputWeights)));
    }

    //~ Methods ------------------------------------------------------------------------------------

    //------//
    // dump //
    //------//
    /**
     * Dump the network parameters.
     */
    public void dump ()
    {
        final StringBuilder sb = new StringBuilder() //
                .append(String.format("Network%n")) //
                .append(String.format("   Input        : %d cells%n", inputSize)) //
                .append(String.format("   Hidden       : %d cells%n", hiddenSize)) //
                .append(String.format("   Output       : %d cells%n", outputSize)) //
                .append(String.format("   LearningRate : %f%n", learningRate)) //
                .append(String.format("   Momentum     : %f%n", momentum)) //
                .append(String.format("   Lambda       : %f%n", lambda)) //
                .append(String.format("   EpochsTotal  : %d%n", epochsTotal));

        logger.info(sb.toString());
    }

    //------------//
    // dumpMatrix //
    //------------//
    /**
     * Dump a matrix (assumed to be a true rectangular matrix,
     * with all rows of the same length).
     *
     * @param matrix the matrix to dump
     * @return the matrix representation
     */
    private String dumpOfMatrix (double[][] matrix)
    {
        StringBuilder sb = new StringBuilder();

        for (int col = 0; col < matrix[0].length; col++) {
            sb.append(String.format("%14d", col));
        }

        sb.append(String.format("%n"));

        for (int row = 0; row < matrix.length; row++) {
            sb.append(String.format("%2d:", row));

            for (int col = 0; col < matrix[0].length; col++) {
                sb.append(String.format("%14e", matrix[row][col]));
            }

            sb.append(String.format("%n"));
        }

        return sb.toString();
    }

    //---------//
    // forward //
    //---------//
    /**
     * Computation of the outputs of a layer, based on its inputs, weights
     * and activation function.
     *
     * @param ins     input cells
     * @param weights applied weights
     * @param outs    output cells
     */
    private void forward (double[] ins,
                          double[][] weights,
                          double[] outs)
    {
        double sum;
        double[] ws;

        for (int io = outs.length - 1; io >= 0; io--) {
            sum = 0;
            ws = weights[io];

            for (int i = ins.length - 1; i >= 0; i--) {
                sum += (ws[i + 1] * ins[i]);
            }

            // Bias
            sum += ws[0];

            outs[io] = sigmoid(sum);

            ///outs[o] = relu(sum);
        }
    }

    //----------------//
    // getEpochsTotal //
    //----------------//
    /**
     * Report the total number of epochs already run.
     *
     * @return the total number of epochs
     */
    public int getEpochsTotal ()
    {
        return epochsTotal;
    }

    //---------------//
    // getHiddenSize //
    //---------------//
    /**
     * Report the number of cells in the hidden layer
     *
     * @return the size of the hidden layer
     */
    public int getHiddenSize ()
    {
        return hiddenSize;
    }

    //----------------//
    // getInputLabels //
    //----------------//
    /**
     * Report the input labels, if any.
     *
     * @return the inputLabels, perhaps null
     */
    public String[] getInputLabels ()
    {
        return inputLabels.strings;
    }

    //--------------//
    // getInputSize //
    //--------------//
    /**
     * Report the number of cells in the input layer
     *
     * @return the size of input layer
     */
    public int getInputSize ()
    {
        return inputSize;
    }

    //-----------//
    // getLambda //
    //-----------//
    /**
     * Get the regularization factor.
     *
     * @return the L2 coefficient
     */
    public double getLambda ()
    {
        return lambda;
    }

    //-----------------//
    // getLearningRate //
    //-----------------//
    /**
     * Get the learning rate.
     *
     * @return the learning rate to use for each iteration
     */
    public double getLearningRate ()
    {
        return learningRate;
    }

    //-------------//
    // getMomentum //
    //-------------//
    /**
     * Get the momentum value.
     *
     * @return the fraction of previous move to be reported on the next correction
     */
    public double getMomentum ()
    {
        return momentum;
    }

    //-----------------//
    // getOutputLabels //
    //-----------------//
    /**
     * Report the output labels, if any.
     *
     * @return the outputLabels, perhaps null
     */
    public String[] getOutputLabels ()
    {
        return outputLabels.strings;
    }

    //---------------//
    // getOutputSize //
    //---------------//
    /**
     * Report the size of the output layer
     *
     * @return the number of cells in the output layer
     */
    public int getOutputSize ()
    {
        return outputSize;
    }

    //---------//
    // marshal //
    //---------//
    /**
     * Marshal the NeuralNetwork to its XML file
     *
     * @param os the XML output stream, which is not closed by this method
     * @throws JAXBException      if a XML serialization error occurred
     * @throws XMLStreamException if there are any problems writing to the stream
     * @throws IOException        if something goes wrong during IO operations
     */
    public void marshal (OutputStream os)
        throws JAXBException, XMLStreamException, IOException
    {
        Jaxb.marshal(this, os, getJaxbContext());
        logger.debug("Network marshalled");
    }

    //------//
    // relu //
    //------//
    /**
     * Rectified linear unit.
     *
     * @param val argument
     * @return value at val
     */
    private double relu (double val)
    {
        return Math.max(0, val);
    }

    /**
     * First derivative of {@link relu} function.
     *
     * @param val argument
     * @return derivative value at val
     */
    //---------//
    // reluDif //
    //---------//
    private double reluDif (double val)
    {
        return (val >= 0) ? 1 : 0;
    }

    //-----//
    // run //
    //-----//
    /**
     * Run the neural network on an array of input values, and return the
     * computed output values.
     *
     * @param inputs  the provided input values
     * @param hiddens provided buffer for hidden values, or null
     * @param outputs pre-allocated array for the computed output values,
     *                or null if not already allocated
     * @return the computed output values
     */
    public double[] run (double[] inputs,
                         double[] hiddens,
                         double[] outputs)
    {
        // Check size consistencies.
        if (inputs == null) {
            logger.error("run method. inputs array is null");
        } else if (inputs.length != inputSize) {
            logger.error(
                    "run method. input size {} not consistent with network input layer {}",
                    inputs.length,
                    inputSize);
        }

        // Allocate the hiddens if not provided
        if (hiddens == null) {
            hiddens = new double[hiddenSize];
        }

        // Compute the hidden values
        forward(inputs, hiddenWeights, hiddens);

        // Allocate the outputs if not done yet
        if (outputs == null) {
            outputs = new double[outputSize];
        } else if (outputs.length != outputSize) {
            logger.error(
                    "run method. output size {} not consistent with network output layer {}",
                    outputs.length,
                    outputSize);
        }

        // Then, compute the output values
        forward(hiddens, outputWeights, outputs);

        return outputs;
    }

    //-----------//
    // setLambda //
    //-----------//
    /**
     * Set the regularization factor.
     *
     * @param lambda the L2 coefficient (typically 0.0001)
     */
    public void setLambda (double lambda)
    {
        this.lambda = lambda;
    }

    //-----------------//
    // setLearningRate //
    //-----------------//
    /**
     * Set the learning rate.
     *
     * @param learningRate the learning rate to use for each iteration
     *                     (typically in the 0.0 .. 1.0 range)
     */
    public void setLearningRate (double learningRate)
    {
        this.learningRate = learningRate;
    }

    //-------------//
    // setMomentum //
    //-------------//
    /**
     * Set the momentum value.
     *
     * @param momentum the fraction of previous move to be reported on the next correction
     */
    public void setMomentum (double momentum)
    {
        this.momentum = momentum;
    }

    //---------//
    // sigmoid //
    //---------//
    /**
     * Simple sigmoid function, with a step around 0 abscissa.
     *
     * @param val abscissa
     * @return the related function value
     */
    private double sigmoid (double val)
    {
        return 1.0d / (1.0d + Math.exp(-val));
    }

    //------------//
    // sigmoidDif //
    //------------//
    /**
     * First derivative of {@link sigmoid} function.
     *
     * @param val argument
     * @return derivative at val
     */
    private double sigmoidDif (double val)
    {
        return val * (1 - val);
    }

    //------//
    // stop //
    //------//
    /**
     * Record the need to stop.
     */
    public void stop ()
    {
        stopping = true;
    }

    //-------//
    // train //
    //-------//
    /**
     * Train the neural network on a collection of input patterns.
     *
     * @param inputs         the provided patterns of values for input cells
     * @param desiredOutputs the corresponding desired values for output cells
     * @param listeners      listeners to be kept informed
     * @param epochs         number of training epochs
     */
    public void train (double[][] inputs,
                       double[][] desiredOutputs,
                       Collection<TrainingMonitor> listeners,
                       int epochs)
    {
        stopping = false;

        Objects.requireNonNull(inputs, "inputs array is null");
        Objects.requireNonNull(desiredOutputs, "desiredOutputs array is null");
        logger.info("Network is being trained on {} epochs...", epochs);

        final int patterns = inputs.length;
        final long startTime = System.currentTimeMillis();
        final double multiplier = 1.0 - (learningRate * lambda); // For L2 regularization

        // Allocate needed arrays
        final double[] gottenOutputs = new double[outputSize];
        final double[] hiddenGrads = new double[hiddenSize];
        final double[] outputGrads = new double[outputSize];
        final double[][] hiddenDeltas = createMatrix(hiddenSize, inputSize + 1, 0);
        final double[][] outputDeltas = createMatrix(outputSize, hiddenSize + 1, 0);
        final double[] hiddens = new double[hiddenSize];

        for (int epoch = 1; epoch <= epochs; epoch++) {
            epochsTotal++;

            // Loop on every pattern (batch size = 1)
            for (int ip = 0; ip < patterns; ip++) {
                // Run the network with input values and current weights
                run(inputs[ip], hiddens, gottenOutputs);

                // Compute the output layer error terms
                for (int io = outputSize - 1; io >= 0; io--) {
                    final double out = gottenOutputs[io];
                    final double delta = desiredOutputs[ip][io] - out;
                    outputGrads[io] = delta * sigmoidDif(out); // Sigmoid'
                    ///outputGrads[io] = delta * reluDif(out); // ReLU'
                }

                // Compute the hidden layer error terms
                for (int ih = hiddenSize - 1; ih >= 0; ih--) {
                    double sum = 0;
                    final double hid = hiddens[ih];

                    for (int io = outputSize - 1; io >= 0; io--) {
                        sum += (outputGrads[io] * outputWeights[io][ih + 1]);
                    }

                    hiddenGrads[ih] = sum * sigmoidDif(hid); // Sigmoid'
                    ///hiddenGrads[h] = sum * reluDif(hid); // ReLU'
                }

                // Update the output weights
                for (int io = outputSize - 1; io >= 0; io--) {
                    for (int ih = hiddenSize - 1; ih >= 0; ih--) {
                        final double dw = (learningRate * outputGrads[io] * hiddens[ih]) //
                                + (momentum * outputDeltas[io][ih + 1]);
                        outputWeights[io][ih + 1] += dw;
                        outputWeights[io][ih + 1] *= multiplier; // Regularization
                        outputDeltas[io][ih + 1] = dw;
                    }

                    // Bias
                    final double dw = (learningRate * outputGrads[io]) //
                            + (momentum * outputDeltas[io][0]);
                    outputWeights[io][0] += dw;
                    outputWeights[io][0] *= multiplier; // Regularization
                    outputDeltas[io][0] = dw;
                }

                // Update the hidden weights
                for (int ih = hiddenSize - 1; ih >= 0; ih--) {
                    for (int i = inputSize - 1; i >= 0; i--) {
                        final double dw = (learningRate * hiddenGrads[ih] * inputs[ip][i]) //
                                + (momentum * hiddenDeltas[ih][i + 1]);
                        hiddenWeights[ih][i + 1] += dw;
                        hiddenWeights[ih][i + 1] *= multiplier; // Regularization
                        hiddenDeltas[ih][i + 1] = dw;
                    }

                    // Bias
                    final double dw = (learningRate * hiddenGrads[ih]) //
                            + (momentum * hiddenDeltas[ih][0]);
                    hiddenWeights[ih][0] += dw;
                    hiddenWeights[ih][0] *= multiplier; // Regularization
                    hiddenDeltas[ih][0] = dw;
                }
            }

            Double mse = null; // Mean Squared Error
            Double hw2 = null; // Hidden squared weights
            Double ow2 = null; // Output squared weights

            for (TrainingMonitor listener : listeners) {
                if ((epoch % listener.getIterationPeriod()) == 0) {
                    if (mse == null) {
                        mse = 0d;

                        for (int ip = 0; ip < patterns; ip++) {
                            run(inputs[ip], hiddens, gottenOutputs);

                            final double[] patternDesiredOutputs = desiredOutputs[ip];

                            for (int io = outputSize - 1; io >= 0; io--) {
                                final double out = gottenOutputs[io];
                                final double dif = patternDesiredOutputs[io] - out;
                                mse += (dif * dif);
                            }
                        }

                        mse /= patterns;
                    }

                    if (hw2 == null) {
                        hw2 = w2(hiddenWeights);
                    }

                    if (ow2 == null) {
                        ow2 = w2(outputWeights);
                    }

                    listener.iterationPeriodDone(epochsTotal, epoch, mse, hw2, ow2);
                }
            }

            // Stop required?
            if (stopping) {
                logger.info("Stopping.");

                break;
            }
        }

        final long dur = System.currentTimeMillis() - startTime;
        logger.info(
                String.format(
                        "Duration %,d seconds, %d epochs on %d patterns",
                        dur / 1000,
                        epochs,
                        patterns));
        stopping = false;
    }

    //----//
    // w2 //
    //----//
    /**
     * Compute the sum of squared weights of a layer.
     *
     * @param weights the layer weights
     * @return the sum of square weights
     */
    private double w2 (double[][] weights)
    {
        double sum = 0d;

        for (int io = weights.length - 1; io >= 0; io--) {
            final double[] ww = weights[io];

            for (int ii = ww.length - 1; ii >= 0; ii--) {
                sum += ww[ii] * ww[ii];
            }
        }

        return sum;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------------//
    // createMatrix //
    //--------------//
    /**
     * Create and initialize a matrix, with random values.
     * Random values are between -amplitude and +amplitude
     *
     * @param rowNb number of rows
     * @param colNb number of columns
     * @return the properly initialized matrix
     */
    private static double[][] createMatrix (int rowNb,
                                            int colNb,
                                            double amplitude)
    {
        final double[][] matrix = new double[rowNb][];

        for (int row = rowNb - 1; row >= 0; row--) {
            final double[] vector = new double[colNb];
            matrix[row] = vector;

            for (int col = colNb - 1; col >= 0; col--) {
                vector[col] = amplitude * (1.0 - (2 * Math.random()));
            }
        }

        return matrix;
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
        throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(NeuralNetwork.class);
        }

        return jaxbContext;
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal the provided XML stream to allocate the corresponding NeuralNetwork.
     *
     * @param in the input stream that contains the network definition in XML format.
     *           The stream is not closed by this method
     * @return the allocated network.
     * @exception JAXBException raised when unmarshalling goes wrong
     */
    public static NeuralNetwork unmarshal (InputStream in)
        throws JAXBException
    {
        final Unmarshaller um = getJaxbContext().createUnmarshaller();
        final NeuralNetwork nn = (NeuralNetwork) um.unmarshal(in);
        logger.debug("Network unmarshalled");

        return nn;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-------------//
    // StringArray //
    //-------------//
    private static class StringArray
    {
        @XmlValue
        String[] strings;

        StringArray ()
        {
        }

        StringArray (String[] strings)
        {
            this.strings = strings;
        }
    }
}
