//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h N e t w o r k                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.math.NeuralNetwork;

import omr.util.Logger;

import java.io.*;
import java.util.*;

import javax.xml.bind.JAXBException;

/**
 * Class <code>GlyphNetwork</code> encapsulates a neural network dedicated to
 * glyph recognition
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphNetwork
    extends Evaluator
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger    logger = Logger.getLogger(
        GlyphNetwork.class);

    /** The singleton */
    private static GlyphNetwork INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** The underlying neural network */
    private NeuralNetwork network;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // GlyphNetwork //
    //--------------//
    /**
     * Create an instance of glyph neural network
     */
    private GlyphNetwork ()
    {
        // Deserialize from binary file(s)
        network = unmarshal();

        // Basic check
        if (network != null) {
            if (network.getOutputSize() != outSize) {
                logger.warning(
                    "Obsolete Network data," + " reconstructing from scratch");
                network = null;
            }
        }

        if (network == null) {
            // Get a brand new one
            logger.info("Creating a brand new GlyphNetwork");
            network = createNetwork();
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of GlyphNetwork in the Audiveris application
     *
     * @return the instance
     */
    public static GlyphNetwork getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new GlyphNetwork();
        }

        return INSTANCE;
    }

    //--------------//
    // setAmplitude //
    //--------------//
    /**
     * Set the amplitude value for initial random values (UNUSED)
     *
     * @param amplitude
     */
    public void setAmplitude (double amplitude)
    {
        constants.amplitude.setValue(amplitude);
    }

    //--------------//
    // getAmplitude //
    //--------------//
    /**
     * Selector for the amplitude value (used in initial random values)
     *
     * @return the amplitude value
     */
    public double getAmplitude ()
    {
        return constants.amplitude.getValue();
    }

    //----------------//
    // getEvaluations //
    //----------------//
    /**
     * Report the results of evaluating a glyph
     *
     * @param glyph the glyph to evaluate
     *
     * @return the ordered array of evaluations
     */
    @Override
    public Evaluation[] getEvaluations (Glyph glyph)
    {
        // If too small, it's just NOISE
        if (!isBigEnough(glyph)) {
            return noiseEvaluations;
        } else {
            double[]     ins = feedInput(glyph, null);
            double[]     outs = new double[outSize];
            Evaluation[] evals = new Evaluation[outSize];

            network.run(ins, null, outs);

            for (int s = 0; s < outSize; s++) {
                evals[s] = new Evaluation();
                evals[s].shape = Shape.values()[s];
                evals[s].grade = 1d / outs[s];
            }

            // Order the evals from best to worst
            Arrays.sort(evals, comparator);

            return evals;
        }
    }

    //-----------------//
    // setLearningRate //
    //-----------------//
    /**
     * Dynamically modify the learning rate of the neural network for its
     * training task
     *
     * @param learningRate new learning rate to use
     */
    public void setLearningRate (double learningRate)
    {
        constants.learningRate.setValue(learningRate);
        network.setLearningRate(learningRate);
    }

    //-----------------//
    // getLearningRate //
    //-----------------//
    /**
     * Selector of the current value for network learning rate
     *
     * @return the current learning rate
     */
    public double getLearningRate ()
    {
        return constants.learningRate.getValue();
    }

    //---------------//
    // setListEpochs //
    //---------------//
    /**
     * Modify the upper limit on the number of epochs (training iterations) for
     * the training process
     *
     * @param listEpochs new value for iteration limit
     */
    public void setListEpochs (int listEpochs)
    {
        constants.listEpochs.setValue(listEpochs);
        network.setEpochs(listEpochs);
    }

    //---------------//
    // getListEpochs //
    //---------------//
    /**
     * Selector on the maximum numner of training iterations
     *
     * @return the upper limit on iteration counter
     */
    public int getListEpochs ()
    {
        return constants.listEpochs.getValue();
    }

    //-------------//
    // setMaxError //
    //-------------//
    /**
     * Modify the error threshold to potentially stop the training process
     *
     * @param maxError the new threshold value to use
     */
    public void setMaxError (double maxError)
    {
        constants.maxError.setValue(maxError);
        network.setMaxError(maxError);
    }

    //-------------//
    // getMaxError //
    //-------------//
    /**
     * Report the error threshold to potentially stop the training process
     *
     * @return the threshold currently in use
     */
    public double getMaxError ()
    {
        return constants.maxError.getValue();
    }

    //-------------//
    // setMomentum //
    //-------------//
    /**
     * Modify the value for momentum used from learning epoch to the other
     *
     * @param momentum the new momentum value to be used
     */
    public void setMomentum (double momentum)
    {
        constants.momentum.setValue(momentum);
        network.setMomentum(momentum);
    }

    //-------------//
    // getMomentum //
    //-------------//
    /**
     * Report the momentum training value currently in use
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
     * Report a name for this network
     *
     * @return a simple name
     */
    @Override
    public String getName ()
    {
        return "Neural";
    }

    //------------//
    // getNetwork //
    //------------//
    /**
     * Selector to the encapsulated Neural Network
     *
     * @return the neural network
     */
    public NeuralNetwork getNetwork ()
    {
        return network;
    }

    //-----------//
    // isTrained //
    //-----------//
    /**
     * Predicate on whether the network has been trained
     *
     * @return true if trained
     */
    @Override
    public boolean isTrained ()
    {
        return true; // TBD ////
    }

    //------//
    // dump //
    //------//
    /**
     * Dump the internals of the neural network to the standard output
     */
    @Override
    public void dump ()
    {
        network.dump();
    }

    //---------//
    // marshal //
    //---------//
    /**
     * Store the neural network in XML format
     */
    public void marshal ()
    {
        final File file = getCustomFile();

        try {
            OutputStream os = new FileOutputStream(file);
            network.marshal(os);
            os.close();
            logger.info("Glyph network marshalled to " + file);
        } catch (FileNotFoundException ex) {
            logger.warning("Could not find file " + file);
        } catch (IOException ex) {
            logger.warning("IO error on file " + file);
        } catch (JAXBException ex) {
            logger.warning("Error marshalling glyph network from " + file);
        }
    }

    //------//
    // stop //
    //------//
    /**
     * Forward "Stop" order to the network being trained
     */
    @Override
    public void stop ()
    {
        network.stop();
    }

    //-------//
    // train //
    //-------//
    /**
     * Train the network using the provided collection of lists
     *
     * @param glyphs  the provided list of glyphs
     * @param monitor the monitoring entity if any
     * @param mode the starting mode of the trainer (scratch, replay or
     * incremental)
     */
    public void train (List<Glyph>  glyphs,
                       Monitor      monitor,
                       StartingMode mode)
    {
        if (glyphs.size() == 0) {
            logger.warning("No glyph to retrain Network Evaluator");

            return;
        }

        // Starting options
        switch (mode) {
        case SCRATCH :
            network = createNetwork();

            break;

        case INCREMENTAL :
            break;
        }

        Collections.shuffle(glyphs);

        double[][] inputs = new double[glyphs.size()][];
        double[][] desiredOutputs = new double[glyphs.size()][];

        int        ig = 0;

        for (Glyph glyph : glyphs) {
            double[] ins = new double[inSize];
            feedInput(glyph, ins);
            inputs[ig] = ins;

            double[] des = new double[outSize];
            Arrays.fill(des, 0);

            des[glyph.getShape()
                     .ordinal()] = 1;
            desiredOutputs[ig] = des;

            ig++;
        }

        // Train
        network.train(inputs, desiredOutputs, monitor);
    }

    //---------------//
    // createNetwork //
    //---------------//
    private NeuralNetwork createNetwork ()
    {
        // Note : We allocate a hidden layer with as many cells as the output
        // layer
        NeuralNetwork nn = new NeuralNetwork(
            inSize,
            outSize,
            outSize,
            getAmplitude(),
            getLearningRate(),
            getMomentum(),
            getMaxError(),
            getListEpochs());

        return nn;
    }

    //-----------//
    // unmarshal //
    //-----------//
    private NeuralNetwork unmarshal (File file)
    {
        try {
            if (file.exists()) {
                logger.info("Unmarshalling GlyphNetwork from " + file);

                InputStream   is = new FileInputStream(file);
                NeuralNetwork nn = NeuralNetwork.unmarshal(is);
                is.close();

                return nn;
            }
        } catch (FileNotFoundException ex) {
            logger.warning("Cannot find or read " + file);
        } catch (IOException ex) {
            logger.warning("IO error on " + file);
        } catch (JAXBException ex) {
            logger.warning("Error unmarshalling glyph network from " + file);
        }

        return null;
    }

    //-----------//
    // unmarshal //
    //-----------//
    private NeuralNetwork unmarshal ()
    {
        // First, look for a custom version
        NeuralNetwork nn = unmarshal(getCustomFile());

        // Second, use the system default
        if (nn == null) {
            nn = unmarshal(getDefaultFile());
        }

        return nn;
    }

    //~ Inner Classes ----------------------------------------------------------

    private static final class Constants
        extends ConstantSet
    {
        Constant.Double  amplitude = new Constant.Double(
            0.5,
            "Initial weight amplitude");
        Constant.Double  learningRate = new Constant.Double(
            0.25,
            "Learning Rate");
        Constant.Integer listEpochs = new Constant.Integer(
            2000,
            "Number of epochs for training on list of glyphs");
        Constant.Double  maxError = new Constant.Double(
            1E-4,
            "Threshold to stop training");
        Constant.Double  momentum = new Constant.Double(
            0.02,
            "Training momentum");
    }
}
