//-----------------------------------------------------------------------//
//                                                                       //
//                        G l y p h N e t w o r k                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph;

import omr.Main;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.math.NeuralNetwork;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.stick.StickSection;
import omr.util.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Class <code>GlyphNetwork</code> encapsulates a neural network dedicated
 * to glyph recognition
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class GlyphNetwork
    extends Evaluator
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(GlyphNetwork.class);

    // The singleton
    private static GlyphNetwork INSTANCE;

    //~ Instance variables ------------------------------------------------

    // The underlying neural network
    private NeuralNetwork network;

    //~ Constructors ------------------------------------------------------

    //--------------//
    // GlyphNetwork //
    //--------------//
    /**
     * Create an instance of glyph neural network
     */
    private GlyphNetwork ()
    {
        // Retrieve last custom version if any
        try {
            // First, look for a custom version
            File custom = getCustomBackup();
            if (custom.exists()){
                logger.info ("Deserializing GlyphNetwork from custom"
                             + " file " + custom);
                network = NeuralNetwork.deserialize
                    (new FileInputStream(custom));
            }
        } catch (FileNotFoundException ex) {
            logger.warning ("Cannot find or read custom backup " +
                            getCustomBackup());
        }

        // Second, use the system default
        if (network == null){
            String resource = getSystemBackup();
            InputStream is = GlyphNetwork.class.getResourceAsStream
                (resource);
            if (is != null) {
                logger.info ("Deserializing GlyphNetwork from " +
                             "system resource "+ resource);
                network = NeuralNetwork.deserialize(is);
            } else {
                logger.warning ("Cannot find system resource " + resource);
            }
        }

        // Basic check
        if (network != null) {
            if (network.getOutputSize() != outSize) {
                logger.warning("Obsolete Network data," +
                               " reconstructing from scratch");
                network = null;
            }
        }

        if (network == null) {
            // Get a brand new one
            logger.info ("Creating a brand new GlyphNetwork");
            network = createNetwork();
        }
    }

    //~ Methods -----------------------------------------------------------

    //------//
    // dump //
    //------//
    @Override
        public void dump ()
    {
        network.dump();
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of GlyphNetwork in the Audiveris
     * application
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
    public void setAmplitude(double amplitude)
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
    public double getAmplitude()
    {
        return constants.amplitude.getValue();
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

    //-------------//
    // setMaxError //
    //-------------//
    /**
     * Modify the error threshold to potentially stop the training process
     *
     * @param maxError the new threshold value to use
     */
    public void  setMaxError (double maxError)
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

    //---------------//
    // setListEpochs //
    //---------------//
    /**
     * Modify the upper limit on the number of epochs (training iterations)
     * for the training process
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

    //------------//
    // getNetwork //
    //------------//
    /**
     * Selector to the encapsulated Neural Network
     *
     * @return the neural network
     */
    public NeuralNetwork getNetwork()
    {
        return network;
    }

    //----------------//
    // getEvaluations //
    //----------------//
    @Override
        public Evaluation[] getEvaluations (Glyph glyph)
    {
        // If too small, it's just NOISE
        if (!isBigEnough(glyph)) {
            return noiseEvaluations;
        }

        double[] ins = feedInput(glyph, null);
        double[] outs = new double[outSize];
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

    //---------//
    // getName //
    //---------//
    @Override
        public String getName()
    {
        return "Neural";
    }

    //-----------//
    // isTrained //
    //-----------//
    @Override
        public boolean isTrained ()
    {
        return true;                    // TBD ////
    }

    //---------------//
    // createNetwork //
    //---------------//
    private NeuralNetwork createNetwork ()
    {
        // Note : We allocate a hidden layer with as many cells as the
        // output layer
        NeuralNetwork nn =  new NeuralNetwork(inSize, outSize, outSize,
                                              getAmplitude(),
                                              getLearningRate(),
                                              getMomentum(),
                                              getMaxError(),
                                              getListEpochs());
        return nn;
    }

    //------//
    // stop //
    //------//
    /**
     * Forward s "Stop" order to the network neing trained
     */
    public void stop()
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
        switch (mode){
        case SCRATCH :
            network = createNetwork();
          break;

        case INCREMENTAL :
            break;
        }
        Collections.shuffle(glyphs);

        double[][] inputs         = new double[glyphs.size()][];
        double[][] desiredOutputs = new double[glyphs.size()][];

        int ig = 0;
        for (Glyph glyph : glyphs) {
            double[] ins = new double[inSize];
            feedInput(glyph, ins);
            inputs[ig] = ins;

            double[] des = new double[outSize];
            Arrays.fill(des, 0);
            des[glyph.getShape().ordinal()] = 1;
            desiredOutputs[ig] = des;

            ig++;
        }

        // Train
        network.train(inputs, desiredOutputs, monitor);

        // Store the trained network as a custom file
        network.serialize(getCustomBackup());
    }

    //~ Classes -----------------------------------------------------------

    private static class Constants
            extends ConstantSet
    {
        //~ Instance variables --------------------------------------------

        Constant.Integer listEpochs = new Constant.Integer
                (2000,
                 "Number of epochs for training on list of glyphs");

        Constant.Double amplitude = new Constant.Double
                (0.5,
                 "Initial weight amplitude");

        Constant.Double learningRate = new Constant.Double
                (0.25,
                 "Learning Rate");

        Constant.Double momentum = new Constant.Double
                (0.02,
                 "Training momentum");

        Constant.Double maxError = new Constant.Double
                (1E-4,
                 "Threshold to stop training");

        Constants ()
        {
            initialize();
        }
    }
}
