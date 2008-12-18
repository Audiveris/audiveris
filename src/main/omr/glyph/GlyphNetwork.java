//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h N e t w o r k                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Evaluator.StartingMode;

import omr.math.NeuralNetwork;

import omr.log.Logger;

import java.io.*;
import java.util.*;

import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;

/**
 * Class <code>GlyphNetwork</code> encapsulates a neural network dedicated to
 * glyph recognition. It wraps the generic {@link omr.math.NeuralNetwork} with
 * application information, for training, storing, loading and using the neural
 * network.
 *
 * <p>The application neural network data is loaded as follows: <ol>
 * <li>It first tries to find a file in the /config sub-folder of the
 * application, looking for a file named 'neural-network.xml' which contains a
 * custom definition of the network, typically after a training.</li>
 * <li>If not found, it falls back reading the default definition from the
 * application resource, reading the /config/neural-network.xml file provided
 * in the distribution jar file.</ol></p>
 *
 * <p>Similarly, after a training of the neural network, the data is stored as
 * the custom definition in the local file 'config/neural-network.xml', which
 * will be picked first when the application is run again.</p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphNetwork
    extends Evaluator
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphNetwork.class);

    /** The singleton */
    private static GlyphNetwork INSTANCE;

    /** Neural network backup file name */
    private static final String NETWORK_FILE_NAME = "neural-network.xml";

    /** URL for default backup within distribution */
    private static final String NETWORK_DEFAULT_URL = "/config/" +
                                                      NETWORK_FILE_NAME;

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
                final String msg = "Neural Network data is obsolete," +
                                   " it must be retrained from scratch";
                logger.warning(msg);
                JOptionPane.showMessageDialog(null, msg);

                network = null;
            }
        }

        if (network == null) {
            // Get a brand new one (not trained)
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

    //-------------------//
    // getAllEvaluations //
    //-------------------//
    @Override
    public Evaluation[] getAllEvaluations (Glyph glyph)
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
                Shape shape = GlyphChecks.specificCheck(Shape.values()[s], glyph);

                if (shape != null) {
                    evals[s] = new Evaluation(shape, 1d / outs[s]);
                } else {
                    evals[s] = new Evaluation(
                        Shape.values()[s],
                        Double.MAX_VALUE);
                }
            }

            // Order the evals from best to worst
            Arrays.sort(evals, comparator);

            return evals;
        }
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
    @Override
    public Evaluation[] getEvaluations (Glyph glyph)
    {
        List<Evaluation> kept = new ArrayList<Evaluation>();

        for (Evaluation eval : getAllEvaluations(glyph)) {
            if (!glyph.isShapeForbidden(eval.shape)) {
                kept.add(eval);
            }
        }

        return kept.toArray(new Evaluation[0]);
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
     * Store the neural network in XML format, always as the custom file
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
    @SuppressWarnings("unchecked")
    public void train (List<Glyph>  glyphs,
                       Monitor      monitor,
                       StartingMode mode)
    {
        if (glyphs.size() == 0) {
            logger.warning("No glyph to retrain Network Evaluator");

            return;
        }

        int    quorum = constants.quorum.getValue();

        // Determine cardinality for each shape
        List[] shapeGlyphs = new List[outSize];

        for (int i = 0; i < shapeGlyphs.length; i++) {
            shapeGlyphs[i] = new ArrayList<Glyph>();
        }

        for (Glyph glyph : glyphs) {
            shapeGlyphs[glyph.getShape()
                             .ordinal()].add(glyph);
        }

        List<Glyph> newGlyphs = new ArrayList<Glyph>();

        for (List l : shapeGlyphs) {
            int     card = 0;
            boolean first = true;

            if (l.size() > 0) {
                while (card < quorum) {
                    for (int i = 0; i < l.size(); i++) {
                        newGlyphs.add((Glyph) l.get(i));
                        card++;

                        if (!first && (card >= quorum)) {
                            break;
                        }
                    }

                    first = false;
                }
            }
        }

        // Shuffle the final collection of glyphs
        Collections.shuffle(newGlyphs);

        // Build the collection of patterns from the glyph data
        double[][] inputs = new double[newGlyphs.size()][];
        double[][] desiredOutputs = new double[newGlyphs.size()][];

        int        ig = 0;

        for (Glyph glyph : newGlyphs) {
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

        // Starting options
        if (mode == StartingMode.SCRATCH) {
            network = createNetwork();
        }

        // Train on the patterns
        network.train(inputs, desiredOutputs, monitor);
    }

    //---------------//
    // getCustomFile //
    //---------------//
    /**
     * Report the custom file used to store or load the internal evaluator data
     *
     * @return the evaluator custom backup file
     */
    private File getCustomFile ()
    {
        // The custom file, if any, is located in the configuration folder
        return new File(Main.getConfigFolder(), NETWORK_FILE_NAME);
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
    private NeuralNetwork unmarshal (InputStream is,
                                     String      name)
    {
        try {
            NeuralNetwork nn = NeuralNetwork.unmarshal(is);
            is.close();

            return nn;
        } catch (FileNotFoundException ex) {
            logger.warning("Cannot find or read " + name);
        } catch (IOException ex) {
            logger.warning("IO error on " + name);
        } catch (JAXBException ex) {
            logger.warning("Error unmarshalling glyph network from " + name);
        }

        return null;
    }

    //-----------//
    // unmarshal //
    //-----------//
    private NeuralNetwork unmarshal ()
    {
        NeuralNetwork nn = null;

        // Look for a custom version first
        File file = getCustomFile();

        if (file.exists()) {
            try {
                logger.fine("Unmarshalling GlyphNetwork from " + file);
                nn = unmarshal(
                    new java.io.FileInputStream(file),
                    file.getPath());
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find file " + file, ex);
            }
        }

        // If no custom file is loaded, use system default from distribution
        if (nn == null) {
            logger.fine(
                "Unmarshalling GlyphNetwork from resource " +
                NETWORK_DEFAULT_URL);
            nn = unmarshal(
                getClass().getResourceAsStream(NETWORK_DEFAULT_URL),
                " resource " + NETWORK_DEFAULT_URL);
        }

        return nn;
    }

    //~ Inner Classes ----------------------------------------------------------

    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Ratio   amplitude = new Constant.Ratio(
            0.5,
            "Initial weight amplitude");
        Constant.Ratio   learningRate = new Constant.Ratio(
            0.25,
            "Learning Rate");
        Constant.Integer listEpochs = new Constant.Integer(
            "Epochs",
            2000,
            "Number of epochs for training on list of glyphs");
        Constant.Integer quorum = new Constant.Integer(
            "Glyphs",
            10,
            "Minimum number of glyphs for each shape");
        Evaluation.Doubt maxError = new Evaluation.Doubt(
            1E-4,
            "Threshold to stop training");
        Constant.Ratio   momentum = new Constant.Ratio(
            0.02,
            "Training momentum");
    }
}
