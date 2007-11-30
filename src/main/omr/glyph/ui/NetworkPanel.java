//----------------------------------------------------------------------------//
//                                                                            //
//                          N e t w o r k P a n e l                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.Evaluator;
import omr.glyph.GlyphNetwork;
import omr.glyph.ui.TrainingPanel.DumpAction;

import omr.math.NeuralNetwork;

import omr.ui.field.LDoubleField;
import omr.ui.field.LField;
import omr.ui.field.LIntegerField;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.event.*;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>NetworkPanel</code> is the user interface that handles the
 * training of the neural network evaluator. It is a dedicated companion of
 * class {@link GlyphTrainer}.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
class NetworkPanel
    extends TrainingPanel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(NetworkPanel.class);

    //~ Instance fields --------------------------------------------------------

    /** Best neural weights so far */
    private NeuralNetwork.Backup bestSnap;

    /** Last neural weights */
    private NeuralNetwork.Backup lastSnap;

    /** To display ETA as a date */
    private DateFormat dateFormat = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM, // Date
        DateFormat.MEDIUM); // Time

    /** Input field for Learning rate of the neural network */
    private LDoubleField learningRate = new LDoubleField(
        "Learning Rate",
        "Learning rate of the neural network",
        "%.2f");

    /** Input field for Momentum value of the neural network */
    private LDoubleField momentum = new LDoubleField(
        "Momentum",
        "Momentum value for the neural network",
        "%.2f");

    /** Output of Estimated time for end of training */
    private LField eta = new LField(
        "ETA",
        "Estimated time for end of training");

    /** Input field for Maximum number of iterations to perform */
    private LIntegerField listEpochs = new LIntegerField(
        "Epochs",
        "Maximum number of iterations to perform");

    /** Output for Index of best configuration so far */
    private LIntegerField bestIndex = new LIntegerField(
        false,
        "Best Index",
        "Index of best configuration so far");

    /** Output for Number of iterations performed so far */
    private LIntegerField trainIndex = new LIntegerField(
        false,
        "Last Index",
        "Number of iterations performed so far");

    /** Input field for Error threshold to stop learning */
    private LDoubleField maxError = new LDoubleField(
        "Max Error",
        "Error threshold to stop learning");

    /** Output for Best recorded value of remaining error */
    private LDoubleField bestError = new LDoubleField(
        false,
        "Best Error",
        "Best recorded value of remaining error");

    /** Output for Last value of remaining error */
    private LDoubleField trainError = new LDoubleField(
        false,
        "Last Error",
        "Last value of remaining error");

    /** User action to pick the last weight */
    private LastAction lastAction = new LastAction();

    /** User action to launch an incremental training */
    private NetworkTrainAction incrementalTrainAction;

    /** User action to pick the best recorded weights */
    private BestAction bestAction = new BestAction();

    /** User action to gracefully stop the training */
    private StopAction stopAction = new StopAction();

    /** Remaining error corresponding to best weights */
    private double bestMse;

    /** Potential listener on best error */
    private final ChangeListener errorListener;

    /** Event related to best error */
    private final ChangeEvent errorEvent;

    /** Remaining error corresponding to last run */
    private double lastMse;

    /** Training start time */
    private long startTime;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // NetworkPanel //
    //--------------//
    /**
     * Creates a new NetworkPanel object.
     *
     *
     * @param task the current training activity
     * @param standardWidth standard width for fields & buttons
     * @param errorListener a listener on remaining error
     * @param selectionPanel the panel for glyph repository
     */
    public NetworkPanel (GlyphTrainer.Task task,
                         String            standardWidth,
                         ChangeListener    errorListener,
                         SelectionPanel    selectionPanel)
    {
        super(
            task,
            standardWidth,
            GlyphNetwork.getInstance(),
            selectionPanel,
            6);
        this.errorListener = errorListener;
        task.addObserver(this);

        if (errorListener != null) {
            errorEvent = new ChangeEvent(this);
        } else {
            errorEvent = null;
        }

        eta.getField()
           .setEditable(false); // ETA is just an output

        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                 .put(KeyStroke.getKeyStroke("ENTER"), "readParams");
        component.getActionMap()
                 .put("readParams", new ParamAction());

        trainAction = new NetworkTrainAction(
            "Re-Train",
            Evaluator.StartingMode.SCRATCH,
            /* confirmationRequired => */ true);
        incrementalTrainAction = new NetworkTrainAction(
            "Inc-Train",
            Evaluator.StartingMode.INCREMENTAL,
            /* confirmationRequired => */ false);

        defineSpecificLayout();
        displayParams();
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getBestError //
    //--------------//
    /**
     * Report the best remaining error so far
     *
     * @return the best error so far
     */
    public double getBestError ()
    {
        return bestMse;
    }

    //------------//
    // epochEnded //
    //------------//
    @Override
    public void epochEnded (final int    epochIndex,
                            final double mse)
    {
        // This part is run on trainer thread
        final int index = epochIndex + 1;
        lastMse = mse;

        boolean snap = false;

        if (mse < bestMse) {
            bestMse = mse;

            // Take a snap
            GlyphNetwork  glyphNetwork = (GlyphNetwork) evaluator;
            NeuralNetwork network = glyphNetwork.getNetwork();
            bestSnap = network.backup();
            snap = true;
            
            // Belt & suspenders: make a copy on disk!
            glyphNetwork.marshal();            
        }

        final boolean snapTaken = snap;

        SwingUtilities.invokeLater(
            new Runnable() {
                    // This part is run on swing thread
                    public void run ()
                    {
                        // Update current values
                        trainIndex.setValue(index);
                        trainError.setValue(mse);

                        // Update best values
                        if (snapTaken) {
                            bestIndex.setValue(index);
                            bestError.setValue(mse);

                            if (errorListener != null) {
                                errorListener.stateChanged(errorEvent);
                            }
                        }

                        // Update progress bar ?
                        progressBar.setValue(index);

                        // Compute ETA
                        long sofar = System.currentTimeMillis() - startTime;
                        long total = (GlyphNetwork.getInstance()
                                                  .getListEpochs() * sofar) / index;
                        Date etaDate = new Date(startTime + total);
                        eta.setText(dateFormat.format(etaDate));

                        component.repaint();
                    }
                });
    }

    //-----------------//
    // trainingStarted //
    //-----------------//
    @Override
    public void trainingStarted (final int    epochIndex,
                                 final double mse)
    {
        // This part is run on trainer thread
        final int     index = epochIndex + 1;
        NeuralNetwork network = ((GlyphNetwork) evaluator).getNetwork();
        bestSnap = network.backup();
        bestMse = mse;

        SwingUtilities.invokeLater(
            new Runnable() {
                    // This part is run on swing thread
                    public void run ()
                    {
                        // Update best values
                        bestIndex.setValue(index);
                        bestError.setValue(mse);

                        ///setFrameTitle(mse);
                        if (errorListener != null) {
                            errorListener.stateChanged(errorEvent);
                        }

                        // Remember starting time
                        startTime = System.currentTimeMillis();
                    }
                });
    }

    //--------//
    // update //
    //--------//
    /**
     * Specific behavior when a new task activity is notified. In addition to
     * {@link TrainingPanel#update}, actions specific to training a neural
     * network are handled here.
     *
     * @param obs the task object
     * @param unused not used
     */
    @Implement(Observer.class)
    public void update (Observable obs,
                        Object     unused)
    {
        super.update(obs, unused);

        switch (task.getActivity()) {
        case INACTIVE :
            incrementalTrainAction.setEnabled(true);
            stopAction.setEnabled(false);

            break;

        case SELECTING :
            incrementalTrainAction.setEnabled(false);
            stopAction.setEnabled(false);

            break;

        case TRAINING :
            incrementalTrainAction.setEnabled(false);
            stopAction.setEnabled(true);
            inputParams();
            displayParams();
            bestMse = Double.MAX_VALUE;
            bestSnap = null;

            break;
        }

        bestAction.setEnabled(false);
        lastAction.setEnabled(false);
    }

    //----------------------//
    // defineSpecificLayout //
    //----------------------//
    private void defineSpecificLayout ()
    {
        int r = 3;
        // ETA field
        builder.add(eta.getLabel(), cst.xy(9, r));
        builder.add(eta.getField(), cst.xyw(11, r, 5));

        // Neural network parameters
        r += 2; // ----------------------------
        builder.add(momentum.getLabel(), cst.xy(9, r));
        builder.add(momentum.getField(), cst.xy(11, r));

        builder.add(learningRate.getLabel(), cst.xy(13, r));
        builder.add(learningRate.getField(), cst.xy(15, r));

        r += 2; // ----------------------------
        builder.add(listEpochs.getLabel(), cst.xy(9, r));
        builder.add(listEpochs.getField(), cst.xy(11, r));

        builder.add(maxError.getLabel(), cst.xy(13, r));
        builder.add(maxError.getField(), cst.xy(15, r));

        // Training entities
        r += 2; // ----------------------------

        JButton dumpButton = new JButton(new DumpAction());
        dumpButton.setToolTipText("Dump the evaluator internals");

        JButton trainButton = new JButton(trainAction);
        trainButton.setToolTipText("Re-Train the evaluator from scratch");

        JButton bestButton = new JButton(bestAction);
        bestButton.setToolTipText("Use the weights of best snap");

        builder.add(dumpButton, cst.xy(3, r));
        builder.add(trainButton, cst.xy(5, r));
        builder.add(bestButton, cst.xy(7, r));

        builder.add(bestIndex.getLabel(), cst.xy(9, r));
        builder.add(bestIndex.getField(), cst.xy(11, r));

        builder.add(bestError.getLabel(), cst.xy(13, r));
        builder.add(bestError.getField(), cst.xy(15, r));

        r += 2; // ----------------------------

        JButton stopButton = new JButton(stopAction);
        stopButton.setToolTipText("Stop the training of the evaluator");

        JButton incTrainButton = new JButton(incrementalTrainAction);
        incTrainButton.setToolTipText("Incrementally train the evaluator");

        JButton lastButton = new JButton(lastAction);
        lastButton.setToolTipText("Use the last weights");

        builder.add(stopButton, cst.xy(3, r));
        builder.add(incTrainButton, cst.xy(5, r));
        builder.add(lastButton, cst.xy(7, r));

        builder.add(trainIndex.getLabel(), cst.xy(9, r));
        builder.add(trainIndex.getField(), cst.xy(11, r));

        builder.add(trainError.getLabel(), cst.xy(13, r));
        builder.add(trainError.getField(), cst.xy(15, r));
    }

    //---------------//
    // displayParams //
    //---------------//
    private void displayParams ()
    {
        GlyphNetwork network = (GlyphNetwork) evaluator;
        listEpochs.setValue(network.getListEpochs());
        learningRate.setValue(network.getLearningRate());
        momentum.setValue(network.getMomentum());
        maxError.setValue(network.getMaxError());
    }

    //-------------//
    // inputParams //
    //-------------//
    private void inputParams ()
    {
        GlyphNetwork network = (GlyphNetwork) evaluator;
        network.setListEpochs(listEpochs.getValue());
        network.setLearningRate(learningRate.getValue());
        network.setMomentum(momentum.getValue());
        network.setMaxError(maxError.getValue());

        progressBar.setMaximum(network.getListEpochs());
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // BestAction //
    //------------//
    private class BestAction
        extends AbstractAction
    {
        public BestAction ()
        {
            super("Use Best");
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            GlyphNetwork  glyphNetwork = (GlyphNetwork) evaluator;
            NeuralNetwork network = glyphNetwork.getNetwork();
            network.restore(bestSnap);
            logger.info("Network remaining error : " + (float) bestMse);
            glyphNetwork.marshal();

            // Let the user choose the other possibility
            setEnabled(false);
            lastAction.setEnabled(true);
        }
    }

    //------------//
    // LastAction //
    //------------//
    private class LastAction
        extends AbstractAction
    {
        public LastAction ()
        {
            super("Use Last");
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            // Ask user confirmation if needed
            if (lastMse > bestMse) {
                final int answer = JOptionPane.showConfirmDialog(
                    component,
                    "Do you want to switch to this non-optimal network ?");

                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            GlyphNetwork  glyphNetwork = (GlyphNetwork) evaluator;
            NeuralNetwork network = glyphNetwork.getNetwork();
            network.restore(lastSnap);
            logger.info("Network remaining error : " + (float) lastMse);
            glyphNetwork.marshal();

            // Let the user choose the other possibility
            setEnabled(false);
            bestAction.setEnabled(true);
        }
    }

    //--------------------//
    // NetworkTrainAction //
    //--------------------//
    private class NetworkTrainAction
        extends TrainingPanel.TrainAction
    {
        public NetworkTrainAction (String                 title,
                                   Evaluator.StartingMode mode,
                                   boolean                confirmationRequired)
        {
            super(title);
            this.mode = mode;
            this.confirmationRequired = confirmationRequired;
        }

        //-------//
        // train //
        //-------//
        @Override
        public void train ()
        {
            super.train();

            NeuralNetwork network = ((GlyphNetwork) evaluator).getNetwork();
            lastSnap = network.backup();

            // By default, keep the better between best recorded and last
            if (lastMse <= bestMse) {
                lastAction.actionPerformed(null);
            } else {
                bestAction.actionPerformed(null);
            }
        }
    }

    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
        extends AbstractAction
    {
        // Purpose is just to read and remember the data from the various
        // input fields. Triggered when user presses Enter in one of these
        // fields.
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            inputParams();
            displayParams();
        }
    }

    //------------//
    // StopAction //
    //------------//
    private class StopAction
        extends AbstractAction
    {
        public StopAction ()
        {
            super("Stop");
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            evaluator.stop();
        }
    }
}
