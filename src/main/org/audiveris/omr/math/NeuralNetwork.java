//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    N e u r a l N e t w o r k                                   //
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
package org.audiveris.omr.math;

import org.audiveris.omr.classifier.TrainingMonitor;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * Class {@code NeuralNetwork} implements a back-propagation neural
 * network, with one input layer, one hidden layer and one output layer.
 * The transfer function is the sigmoid.
 * <p>
 * <b>NOTA</b>: This class has been resurrected until a dl4j solution is found.
 * <p>
 * This neuralNetwork class can be stored on disk in XML form (through the {@link #marshal} and
 * {@link #unmarshal} methods).
 * <p>
 * The class also allows in-memory {@link #backup} and {@link #restore} operation, mainly used to
 * save the most efficient weight values during the network training.
 *
 * @author Hervé Bitteur
 */
@Deprecated
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "neural-network")
public class NeuralNetwork
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            NeuralNetwork.class);

    /** Un/marshalling context for use with JAXB */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Size of input layer. */
    @XmlAttribute(name = "input-size")
    private final int inputSize;

    /** Size of hidden layer. */
    @XmlAttribute(name = "hidden-size")
    private final int hiddenSize;

    /** Size of output layer. */
    @XmlAttribute(name = "output-size")
    private final int outputSize;

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

    /** Default learning Rate parameter. */
    private transient volatile double learningRate = 0.40;

    /** Default momentum for faster convergence. */
    private transient volatile double momentum = 0.25;

    /** Default number of epochs when training. */
    private transient volatile int epochs = 10;

    /** To trigger training stop. */
    private transient volatile boolean stopping = false;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a neural network, with specified number of cells in each
     * layer, and default values.
     *
     * @param inputSize    number of cells in input layer
     * @param hiddenSize   number of cells in hidden layer
     * @param outputSize   number of cells in output layer
     * @param amplitude    amplitude (less than or = 1.0) for initial random values
     * @param inputLabels  array of labels for input cells, perhaps empty
     * @param outputLabels array of labels for output cells, perhaps empty
     */
    public NeuralNetwork (int inputSize,
                          int hiddenSize,
                          int outputSize,
                          double amplitude,
                          String[] inputLabels,
                          String[] outputLabels)
    {
        // Cache parameters
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;

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
    }

    /**
     * Create a neural network, with specified number of cells in each
     * layer, and specific parameters
     *
     * @param inputSize    number of cells in input layer
     * @param hiddenSize   number of cells in hidden layer
     * @param outputSize   number of cells in output layer
     * @param amplitude    amplitude (less than or = 1.0) for initial random values
     * @param inputLabels  array of labels for input cells, perhaps empty
     * @param outputLabels array of labels for output cells, perhaps empty
     * @param learningRate learning rate factor
     * @param momentum     momentum from last adjustment
     * @param epochs       number of epochs in training
     */
    public NeuralNetwork (int inputSize,
                          int hiddenSize,
                          int outputSize,
                          double amplitude,
                          String[] inputLabels,
                          String[] outputLabels,
                          double learningRate,
                          double momentum,
                          int epochs)
    {
        this(inputSize, hiddenSize, outputSize, amplitude, inputLabels, outputLabels);

        // Cache parameters
        this.learningRate = learningRate;
        this.momentum = momentum;
        this.epochs = epochs;
    }

    /** Private no-arg constructor meant for the JAXB compiler only. */
    private NeuralNetwork ()
    {
        inputSize = -1;
        hiddenSize = -1;
        outputSize = -1;
        inputLabels = null;
        outputLabels = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    //--------//
    // backup //
    //--------//
    /**
     * Return a backup of the internal memory of this network.
     * Generally used right after network creation to save the initial
     * conditions.
     *
     * @return an opaque copy of the network memory
     */
    public Backup backup ()
    {
        logger.debug("Network memory backup");

        return new Backup(hiddenWeights, outputWeights);
    }

    //------//
    // dump //
    //------//
    /**
     * Dumps the network
     */
    public void dump ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Network%n"));
        sb.append(String.format("LearningRate = %f%n", learningRate));
        sb.append(String.format("Momentum     = %f%n", momentum));
        sb.append(String.format("Epochs       = %d%n", epochs));

        // Input
        sb.append(String.format("%nInputs  : %d cells%n", inputSize));

        // Hidden
        sb.append(dumpOfMatrix(hiddenWeights));
        sb.append(String.format("%nHidden  : %d cells%n", hiddenSize));

        // Output
        sb.append(dumpOfMatrix(outputWeights));
        sb.append(String.format("%nOutputs : %d cells%n", outputSize));

        logger.info(sb.toString());
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

    //---------//
    // restore //
    //---------//
    /**
     * Restore the internal memory of a Network, from a previous Backup.
     * This does not reset the current parameters such as learning rate, momentum, maxError or
     * epochs.
     *
     * @param backup a backup previously made
     */
    public void restore (Backup backup)
    {
        // Check parameter
        if (backup == null) {
            throw new IllegalArgumentException("Backup is null");
        }

        // Make sure backup is compatible with this neural network
        if ((backup.hiddenWeights.length != hiddenSize)
            || (backup.hiddenWeights[0].length != (inputSize + 1))
            || (backup.outputWeights.length != outputSize)
            || (backup.outputWeights[0].length != (hiddenSize + 1))) {
            throw new IllegalArgumentException("Incompatible backup");
        }

        logger.debug("Network memory restore");
        this.hiddenWeights = cloneMatrix(backup.hiddenWeights);
        this.outputWeights = cloneMatrix(backup.outputWeights);
    }

    //-----//
    // run //
    //-----//
    /**
     * Run the neural network on an array of input values, and return the
     * computed output values.
     * This method writes into the hiddens buffer.
     *
     * @param inputs  the provided input values
     * @param hiddens provided buffer for hidden values, or null
     * @param outputs preallocated array for the computed output values, or null if not already
     *                allocated
     *
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
    // setEpochs //
    //-----------//
    /**
     * Set the number of epochs for training the network with a given input.
     *
     * @param epochs number of epochs
     */
    public void setEpochs (int epochs)
    {
        this.epochs = epochs;
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

    //------//
    // stop //
    //------//
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
     * @param listener       listener to be kept informed
     * @param iterPeriod     period for iteration notification
     */
    public void train (double[][] inputs,
                       double[][] desiredOutputs,
                       TrainingMonitor listener,
                       int iterPeriod)
    {
        stopping = false;

        Objects.requireNonNull(inputs, "inputs array is null");
        Objects.requireNonNull(desiredOutputs, "desiredOutputs array is null");
        logger.info("Network is being trained on {} epochs...", epochs);

        final int patterns = inputs.length;
        final long startTime = System.currentTimeMillis();

        // Allocate needed arrays
        final double[] gottenOutputs = new double[outputSize];
        final double[] hiddenGrads = new double[hiddenSize];
        final double[] outputGrads = new double[outputSize];
        final double[][] hiddenDeltas = createMatrix(hiddenSize, inputSize + 1, 0);
        final double[][] outputDeltas = createMatrix(outputSize, hiddenSize + 1, 0);
        final double[] hiddens = new double[hiddenSize];
        int iter = 0;

        for (int ie = 1; ie <= epochs; ie++) {
            iter++; // For this old engine, iter = epoch

            if (listener != null) {
                listener.epochStarted(ie);
            }

            // Loop on all input patterns
            for (int ip = 0; ip < patterns; ip++) {
                // Run the network with input values and current weights
                run(inputs[ip], hiddens, gottenOutputs);

                // Compute the output layer error terms
                for (int io = outputSize - 1; io >= 0; io--) {
                    double out = gottenOutputs[io];
                    double dif = desiredOutputs[ip][io] - out;
                    ///outputGrads[io] = dif * out * (1 - out); // Sigmoid'
                    outputGrads[io] = dif * sigmoidDif(out); // Sigmoid'
                    ///outputGrads[io] = dif * reluDif(out); // ReLU'
                }

                // Compute the hidden layer error terms
                for (int ih = hiddenSize - 1; ih >= 0; ih--) {
                    double sum = 0;
                    double hid = hiddens[ih];

                    for (int o = outputSize - 1; o >= 0; o--) {
                        sum += (outputGrads[o] * outputWeights[o][ih + 1]);
                    }

                    ///hiddenGrads[h] = sum * hid * (1 - hid); // Sigmoid'
                    hiddenGrads[ih] = sum * sigmoidDif(hid); // Sigmoid'
                    ///hiddenGrads[h] = sum * reluDif(hid); // ReLU'
                }

                // Update the output weights
                for (int io = outputSize - 1; io >= 0; io--) {
                    for (int ih = hiddenSize - 1; ih >= 0; ih--) {
                        double dw = (learningRate * outputGrads[io] * hiddens[ih])
                                    + (momentum * outputDeltas[io][ih + 1]);
                        outputWeights[io][ih + 1] += dw;
                        outputDeltas[io][ih + 1] = dw;
                    }

                    // Bias
                    double dw = (learningRate * outputGrads[io])
                                + (momentum * outputDeltas[io][0]);
                    outputWeights[io][0] += dw;
                    outputDeltas[io][0] = dw;
                }

                // Update the hidden weights
                for (int ih = hiddenSize - 1; ih >= 0; ih--) {
                    for (int i = inputSize - 1; i >= 0; i--) {
                        double dw = (learningRate * hiddenGrads[ih] * inputs[ip][i])
                                    + (momentum * hiddenDeltas[ih][i + 1]);
                        hiddenWeights[ih][i + 1] += dw;
                        hiddenDeltas[ih][i + 1] = dw;
                    }

                    // Bias
                    double dw = (learningRate * hiddenGrads[ih])
                                + (momentum * hiddenDeltas[ih][0]);
                    hiddenWeights[ih][0] += dw;
                    hiddenDeltas[ih][0] = dw;
                }
            }

            if (listener != null) {
                if ((iter % iterPeriod) == 0) {
                    double mse = 0d; // Mean Squared Error

                    for (int ip = 0; ip < patterns; ip++) {
                        final double[] patternDesiredOutputs = desiredOutputs[ip];
                        run(inputs[ip], hiddens, gottenOutputs);

                        for (int o = outputSize - 1; o >= 0; o--) {
                            double out = gottenOutputs[o];
                            double dif = patternDesiredOutputs[o] - out;
                            mse += (dif * dif);
                        }
                    }

                    mse /= patterns;
                    listener.iterationPeriodDone(iter, mse);
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
                        "Duration %,d seconds, %d iterations on %d patterns",
                        dur / 1000,
                        epochs,
                        patterns));
        stopping = false;
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal the provided XML stream to allocate the corresponding NeuralNetwork.
     *
     * @param in the input stream that contains the network definition in XML format.
     *           The stream is not closed by this method
     *
     * @return the allocated network.
     * @exception JAXBException raised when unmarshalling goes wrong
     */
    public static NeuralNetwork unmarshal (InputStream in)
            throws JAXBException
    {
        Unmarshaller um = getJaxbContext().createUnmarshaller();
        NeuralNetwork nn = (NeuralNetwork) um.unmarshal(in);
        logger.debug("Network unmarshalled");

        return nn;
    }

    //-------------//
    // cloneMatrix //
    //-------------//
    /**
     * Create a clone of the provided matrix.
     *
     * @param matrix the matrix to clone
     * @return the clone
     */
    private static double[][] cloneMatrix (double[][] matrix)
    {
        final int rowNb = matrix.length;
        final int colNb = matrix[0].length;

        double[][] clone = new double[rowNb][];

        for (int row = rowNb - 1; row >= 0; row--) {
            clone[row] = new double[colNb];
            System.arraycopy(matrix[row], 0, clone[row], 0, colNb);
        }

        return clone;
    }

    //--------------//
    // createMatrix //
    //--------------//
    /**
     * Create and initialize a matrix, with random values.
     * Random values are between -amplitude and +amplitude
     *
     * @param rowNb number of rows
     * @param colNb number of columns
     *
     * @return the properly initialized matrix
     */
    private static double[][] createMatrix (int rowNb,
                                            int colNb,
                                            double amplitude)
    {
        double[][] matrix = new double[rowNb][];

        for (int row = rowNb - 1; row >= 0; row--) {
            double[] vector = new double[colNb];
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
     * Re-entrant method.
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

        for (int o = outs.length - 1; o >= 0; o--) {
            sum = 0;
            ws = weights[o];

            for (int i = ins.length - 1; i >= 0; i--) {
                sum += (ws[i + 1] * ins[i]);
            }

            // Bias
            sum += ws[0];

            outs[o] = sigmoid(sum);

            ///outs[o] = relu(sum);
        }
    }

    private double relu (double val)
    {
        return Math.max(0, val);
    }

    private double reluDif (double val)
    {
        return (val >= 0) ? 1 : 0;
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

    private double sigmoidDif (double val)
    {
        return val * (1 - val);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //
    //--------//
    // Backup //
    //--------//
    /**
     * Class {@code Backup} is an opaque class that encapsulates a
     * snapshot of a NeuralNetwork internal memory (its weights).
     * A Backup instance can only be obtained through the use of {@link #backup}
     * method of a NeuralNetwork.
     * A Backup instance is the needed parameter for a NeuralNetwork {@link #restore} action.
     */
    public static class Backup
    {
        //~ Instance fields ------------------------------------------------------------------------

        private double[][] hiddenWeights;

        private double[][] outputWeights;

        //~ Constructors ---------------------------------------------------------------------------
        // Private constructor
        private Backup (double[][] hiddenWeights,
                        double[][] outputWeights)
        {
            this.hiddenWeights = cloneMatrix(hiddenWeights);
            this.outputWeights = cloneMatrix(outputWeights);
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
