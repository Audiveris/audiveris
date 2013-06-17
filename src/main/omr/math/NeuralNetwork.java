//----------------------------------------------------------------------------//
//                                                                            //
//                         N e u r a l N e t w o r k                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code NeuralNetwork} implements a back-propagation neural
 * network, with one input layer, one hidden layer and one output layer.
 * The transfer function is the sigmoid.
 *
 * <p>This neuralNetwork class can be stored on disk in XML form (through
 * the {@link #marshal} and {@link #unmarshal} methods).
 *
 * <p>The class also allows in-memory {@link #backup} and {@link #restore}
 * operation, mainly used to save the most performant weight values during the
 * network training.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "neural-network")
public class NeuralNetwork
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            NeuralNetwork.class);

    /** Un/marshalling context for use with JAXB */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields --------------------------------------------------------
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
    @XmlElementWrapper(name = "input-labels")
    @XmlElement(name = "input")
    private final String[] inputLabels;

    /** Labels of output cells. */
    @XmlElementWrapper(name = "output-labels")
    @XmlElement(name = "output")
    private final String[] outputLabels;

    /** Weights to hidden layer. */
    @XmlElementWrapper(name = "hidden-weights")
    @XmlElement(name = "row")
    private double[][] hiddenWeights;

    /** Weights to output layer. */
    @XmlElementWrapper(name = "output-weights")
    @XmlElement(name = "row")
    private double[][] outputWeights;

    /** Flag to stop training. */
    private transient volatile boolean stopping = false;

    /** Learning Rate parameter. */
    private transient volatile double learningRate = 0.40;

    /** Max Error parameter. */
    private transient volatile double maxError = 1E-4;

    /** Momentum for faster convergence. */
    private transient volatile double momentum = 0.25;

    /** Number of epochs when training. */
    private transient volatile int epochs = 1000;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // NeuralNetwork //
    //---------------//
    /**
     * Create a neural network, with specified number of cells in each
     * layer, and default values.
     *
     * @param inputSize    number of cells in input layer
     * @param hiddenSize   number of cells in hidden layer
     * @param outputSize   number of cells in output layer
     * @param amplitude    amplitude ( <= 1.0) for initial random values
     *                     @param inputLabels array
     * o      f
     *                                                                                                                                                                                                                                                                                                                                                                                                                       labels
     *                                                                                                                                                                                                                                                                                                                                                                                                                       for
     *                                                                                                                                                                                                                                                                                                                                                                                                                       input
     *                                                                                                                                                                                                                                                                                                                                                                                                                       cells,
     *                                                                                                                                                                                                                                                                                                                                                                                                                       or
     *                                                                                                                                                                                                                                                                                                                                                                                                                       null
     * @param outputLabels array of labels for output cells, or null
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
        this.inputLabels = inputLabels;

        if ((inputLabels != null) && (inputLabels.length != inputSize)) {
            throw new IllegalArgumentException(
                    "Inconsistent input labels " + inputLabels + " vs "
                    + inputSize);
        }

        // Labels for output, if any
        this.outputLabels = outputLabels;

        if ((outputLabels != null) && (outputLabels.length != outputSize)) {
            throw new IllegalArgumentException(
                    "Inconsistent output labels " + outputLabels + " vs "
                    + outputSize);
        }

        logger.debug("Network created");
    }

    //---------------//
    // NeuralNetwork //
    //---------------//
    /**
     * Create a neural network, with specified number of cells in each
     * layer, and specific parameters
     *
     * @param inputSize    number of cells in input layer
     * @param hiddenSize   number of cells in hidden layer
     * @param outputSize   number of cells in output layer
     * @param amplitude    amplitude ( <= 1.0) for initial random values
     *                     @param inputLabels array
     * o      f
     *                                                                                                                                                                                                                                                                                                                                                                                                                           labels
     *                                                                                                                                                                                                                                                                                                                                                                                                                           for
     *                                                                                                                                                                                                                                                                                                                                                                                                                           input
     *                                                                                                                                                                                                                                                                                                                                                                                                                           cells,
     *                                                                                                                                                                                                                                                                                                                                                                                                                           or
     *                                                                                                                                                                                                                                                                                                                                                                                                                           null
     * @param outputLabels array of labels for output cells, or null
     * @param learningRate learning rate factor
     * @param momentum     momentum from last adjustment
     * @param maxError     threshold to stop training
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
                          double maxError,
                          int epochs)
    {
        this(
                inputSize,
                hiddenSize,
                outputSize,
                amplitude,
                inputLabels,
                outputLabels);

        // Cache parameters
        this.learningRate = learningRate;
        this.momentum = momentum;
        this.maxError = maxError;
        this.epochs = epochs;
    }

    //---------------//
    // NeuralNetwork //
    //---------------//
    /** Private no-arg constructor meant for the JAXB compiler only */
    private NeuralNetwork ()
    {
        inputSize = -1;
        hiddenSize = -1;
        outputSize = -1;
        inputLabels = null;
        outputLabels = null;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal the provided XML stream to allocate the corresponding
     * NeuralNetwork.
     *
     * @param in the input stream that contains the network definition in XML
     *           format. The stream is not closed by this method
     *
     * @return the allocated network.
     * @exception JAXBException raised when unmarshalling goes wrong
     */
    public static NeuralNetwork unmarshal (InputStream in)
            throws JAXBException
    {
        Unmarshaller um = getJaxbContext()
                .createUnmarshaller();
        NeuralNetwork nn = (NeuralNetwork) um.unmarshal(in);
        logger.debug("Network unmarshalled");

        return nn;
    }

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
        sb.append(String.format("MaxError     = %f%n", maxError));
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
        return inputLabels;
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
        return outputLabels;
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
     * @exception JAXBException raised when marshalling goes wrong
     */
    public void marshal (OutputStream os)
            throws JAXBException
    {
        Marshaller m = getJaxbContext()
                .createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(this, os);
        logger.debug("Network marshalled");
    }

    //---------//
    // restore //
    //---------//
    /**
     * Restore the internal memory of a Network, from a previous Backup.
     * This does not reset the current parameters such as learning rate,
     * momentum, maxError or epochs.
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
     * @param outputs preallocated array for the computed output values, or null
     *                if not already allocated
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
                    "run method. input size {} not consistent with"
                    + " network input layer {}",
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
                    "run method. output size {} not consistent with"
                    + " network output layer {}",
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
     * Set the number of iterations for training the network with a
     * given input.
     *
     * @param epochs number of iterations
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
    // setMaxError //
    //-------------//
    /**
     * Set the maximum error level.
     *
     * @param maxError maximum error
     */
    public void setMaxError (double maxError)
    {
        this.maxError = maxError;
    }

    //-------------//
    // setMomentum //
    //-------------//
    /**
     * Set the momentum value.
     *
     * @param momentum the fraction of previous move to be reported on the next
     *                 correction
     */
    public void setMomentum (double momentum)
    {
        this.momentum = momentum;
    }

    //------//
    // stop //
    //------//
    /**
     * A means to externally stop the current training.
     */
    public void stop ()
    {
        stopping = true;
        logger.debug("Network training being stopped ...");
    }

    //-------//
    // train //
    //-------//
    /**
     * Train the neural network on a collection of input patterns,
     * so that it delivers the expected outputs within maxError.
     * This method is not optimized for absolute speed, but rather for being
     * able to keep the best weights values.
     *
     * @param inputs         the provided patterns of values for input cells
     * @param desiredOutputs the corresponding desired values for output cells
     * @param monitor        a monitor interface to be kept informed (or null)
     *
     * @return mse, the final mean square error
     */
    public double train (double[][] inputs,
                         double[][] desiredOutputs,
                         Monitor monitor)
    {
        logger.debug("Network being trained");
        stopping = false;

        long startTime = System.currentTimeMillis();

        // Check size consistencies.
        if (inputs == null) {
            throw new IllegalArgumentException("inputs array is null");
        }

        final int patternNb = inputs.length;

        if (desiredOutputs == null) {
            throw new IllegalArgumentException("desiredOutputs array is null");
        }

        // Allocate needed arrays
        double[] gottenOutputs = new double[outputSize];
        double[] hiddenGrads = new double[hiddenSize];
        double[] outputGrads = new double[outputSize];
        double[][] hiddenDeltas = createMatrix(hiddenSize, inputSize + 1, 0);
        double[][] outputDeltas = createMatrix(outputSize, hiddenSize + 1, 0);
        double[] hiddens = new double[hiddenSize];

        // Mean Square Error
        double mse = 0;

        // Notify Monitor we are starting
        if (monitor != null) {
            // Compute the initial mse
            for (int ip = 0; ip < patternNb; ip++) {
                run(inputs[ip], hiddens, gottenOutputs);

                for (int o = outputSize - 1; o >= 0; o--) {
                    double out = gottenOutputs[o];
                    double dif = desiredOutputs[ip][o] - out;
                    mse += (dif * dif);
                }
            }

            mse /= patternNb;
            mse = Math.sqrt(mse);
            monitor.trainingStarted(0, mse);
        }

        int ie = 0;

        for (; ie < epochs; ie++) {
            // Have we been told to stop ?
            if (stopping) {
                logger.debug("Network stopped.");

                break;
            }

            // Compute the output layer error terms
            mse = 0;

            // Loop on all input patterns
            for (int ip = 0; ip < patternNb; ip++) {
                // Run the network with input values and current weights
                run(inputs[ip], hiddens, gottenOutputs);

                for (int o = outputSize - 1; o >= 0; o--) {
                    double out = gottenOutputs[o];
                    double dif = desiredOutputs[ip][o] - out;
                    mse += (dif * dif);
                    outputGrads[o] = dif * out * (1 - out);
                }

                // Compute the hidden layer error terms
                for (int h = hiddenSize - 1; h >= 0; h--) {
                    double sum = 0;
                    double hid = hiddens[h];

                    for (int o = outputSize - 1; o >= 0; o--) {
                        sum += (outputGrads[o] * outputWeights[o][h + 1]);
                    }

                    hiddenGrads[h] = sum * hid * (1 - hid);
                }

                // Now update the output weights
                for (int o = outputSize - 1; o >= 0; o--) {
                    for (int h = hiddenSize - 1; h >= 0; h--) {
                        double dw = (learningRate * outputGrads[o] * hiddens[h])
                                    + (momentum * outputDeltas[o][h + 1]);
                        outputWeights[o][h + 1] += dw;
                        outputDeltas[o][h + 1] = dw;
                    }

                    // Bias
                    double dw = (learningRate * outputGrads[o])
                                + (momentum * outputDeltas[o][0]);
                    outputWeights[o][0] += dw;
                    outputDeltas[o][0] = dw;
                }

                // And the hidden weights
                for (int h = hiddenSize - 1; h >= 0; h--) {
                    for (int i = inputSize - 1; i >= 0; i--) {
                        double dw = (learningRate * hiddenGrads[h] * inputs[ip][i])
                                    + (momentum * hiddenDeltas[h][i + 1]);
                        hiddenWeights[h][i + 1] += dw;
                        hiddenDeltas[h][i + 1] = dw;
                    }

                    // Bias
                    double dw = (learningRate * hiddenGrads[h])
                                + (momentum * hiddenDeltas[h][0]);
                    hiddenWeights[h][0] += dw;
                    hiddenDeltas[h][0] = dw;
                }
            } // for (int ip = 0; i < patternNb; i++)

            // Compute true current mse
            mse = 0d;

            for (int ip = 0; ip < patternNb; ip++) {
                run(inputs[ip], hiddens, gottenOutputs);

                for (int o = outputSize - 1; o >= 0; o--) {
                    double out = gottenOutputs[o];
                    double dif = desiredOutputs[ip][o] - out;
                    mse += (dif * dif);
                }
            }

            mse /= patternNb;
            mse = Math.sqrt(mse);

            if (monitor != null) {
                monitor.epochEnded(ie, mse);
            }

            if (mse <= maxError) {
                logger.info(
                        "Network exiting training, remaining error limit reached");
                logger.info("Network remaining error was : {}", mse);

                break;
            }
        } // for (int ie = 0; ie < epochs; ie++)

        if (logger.isDebugEnabled()) {
            long stopTime = System.currentTimeMillis();
            logger.debug(
                    String.format(
                    "Duration  %,d seconds, %d epochs on %d patterns",
                    (stopTime - startTime) / 1000,
                    ie,
                    patternNb));
        }

        return mse;
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
        }
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

    //~ Inner Interfaces -------------------------------------------------------
    //
    //---------//
    // Monitor //
    //---------//
    /**
     * Interface {@code Monitor} allows to plug a monitor to a Neural
     * Network instance, and inform the monitor about the progress of
     * the training activity.
     */
    public static interface Monitor
    {
        //~ Methods ------------------------------------------------------------

        /**
         * Entry called at end of each epoch during the training phase.
         *
         * @param epochIndex the sequential index of completed epoch
         * @param mse        the remaining mean square error
         */
        void epochEnded (int epochIndex,
                         double mse);

        /**
         * Entry called at the beginning of the training phase, to allow
         * initial snap shots for example.
         *
         * @param epochIndex the sequential index (0)
         * @param mse        the starting mean square error
         * */
        void trainingStarted (final int epochIndex,
                              final double mse);
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //--------//
    // Backup //
    //--------//
    /**
     * Class {@code Backup} is an opaque class that encapsulates a
     * snapshot of a NeuralNetwork internal memory (its weights).
     * A Backup instance can only be obtained through the use of {@link #backup}
     * method of a NeuralNetwork.
     * A Backup instance is the needed parameter for a NeuralNetwork {@link
     * #restore} action.
     */
    public static class Backup
    {
        //~ Instance fields ----------------------------------------------------

        private double[][] hiddenWeights;

        private double[][] outputWeights;

        //~ Constructors -------------------------------------------------------
        // Private constructor
        private Backup (double[][] hiddenWeights,
                        double[][] outputWeights)
        {
            this.hiddenWeights = cloneMatrix(hiddenWeights);
            this.outputWeights = cloneMatrix(outputWeights);
        }
    }
}
