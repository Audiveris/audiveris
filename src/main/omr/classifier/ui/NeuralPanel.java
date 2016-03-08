//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     N e u r a l P a n e l                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier.ui;

import omr.classifier.AbstractClassifier.StartingMode;
import omr.classifier.NeuralClassifier;
import omr.classifier.ui.TrainingPanel.DumpAction;

import omr.math.NeuralNetwork;

import omr.ui.field.LDoubleField;
import omr.ui.field.LIntegerField;
import omr.ui.field.LLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.util.Date;
import java.util.Observable;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

/**
 * Class {@code NeuralPanel} is the user interface that handles the training of the
 * neural network engine.
 * It is a dedicated companion of class {@link Trainer}.
 *
 * @author Hervé Bitteur
 */
class NeuralPanel
        extends TrainingPanel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(NeuralPanel.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Best neural weights so far */
    private NeuralNetwork.Backup bestSnap;

    /** Last neural weights */
    private NeuralNetwork.Backup lastSnap;

    /** To display ETA as a date */
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM, // Date
            DateFormat.MEDIUM); // Time

    /** Input field for Learning rate of the neural network */
    private final LDoubleField learningRate = new LDoubleField(
            "Learning Rate",
            "Learning rate of the neural network",
            "%.2f");

    /** Input field for Momentum value of the neural network */
    private final LDoubleField momentum = new LDoubleField(
            "Momentum",
            "Momentum value for the neural network",
            "%.2f");

    /** Output of Estimated time for end of training */
    private final LLabel eta = new LLabel("ETA:", "Estimated time for end of training");

    /** Input field for Maximum number of iterations to perform */
    private final LIntegerField listEpochs = new LIntegerField(
            "Epochs",
            "Maximum number of iterations to perform");

    /** Input field for Error threshold to stop learning */
    private final LDoubleField maxError = new LDoubleField(
            "Max Error",
            "Error threshold to stop learning");

    /** Output for Index of best configuration so far */
    private final LLabel bestIndex = new LLabel(
            "Best Index:",
            "Index of best configuration so far");

    /** Output for Number of iterations performed so far */
    private final LLabel trainIndex = new LLabel(
            "Last Index:",
            "Number of iterations performed so far");

    /** Output for Best recorded value of remaining error */
    private final LLabel bestError = new LLabel(
            "Best Error:",
            "Best recorded value of remaining error");

    /** Output for Last value of remaining error */
    private final LLabel trainError = new LLabel("Last Error:", "Last value of remaining error");

    /** User action to pick the last weight */
    private final LastAction lastAction = new LastAction();

    /** User action to launch an incremental training */
    private final NetworkTrainAction incrementalTrainAction;

    /** User action to pick the best recorded weights */
    private final BestAction bestAction = new BestAction();

    /** User action to gracefully stop the training */
    private final StopAction stopAction = new StopAction();

    /** Remaining error corresponding to best weights */
    private double bestMse;

    /** Potential listener on best error */
    private ChangeListener errorListener;

    /** Remaining error corresponding to last run */
    private double lastMse;

    /** Training start time */
    private long startTime;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new NetworkPanel object.
     *
     *
     * @param task           the current training activity
     * @param standardWidth  standard width for fields & buttons
     * @param selectionPanel the panel for glyph repository
     */
    public NeuralPanel (Trainer.Task task,
                        String standardWidth,
                        SelectionPanel selectionPanel)
    {
        super(task, standardWidth, NeuralClassifier.getInstance(), selectionPanel, 6);
        task.addObserver(this);

        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"),
                "readParams");
        component.getActionMap().put("readParams", new ParamAction());

        trainAction = new NetworkTrainAction(
                "Re-Train",
                StartingMode.SCRATCH, /* confirmationRequired => */
                true);
        incrementalTrainAction = new NetworkTrainAction(
                "Inc-Train",
                StartingMode.INCREMENTAL,
                /* confirmationRequired => */ false);

        defineSpecificLayout();
        displayParams();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // epochEnded //
    //------------//
    @Override
    public void epochEnded (final int epochIndex,
                            final double mse)
    {
        // This part is run on trainer thread
        final int index = epochIndex + 1;
        lastMse = mse;

        boolean snap = false;

        if (mse < bestMse) {
            bestMse = mse;

            // Take a snap
            NeuralClassifier glyphNetwork = (NeuralClassifier) engine;
            NeuralNetwork network = glyphNetwork.getNetwork();
            bestSnap = network.backup();
            snap = true;

            // Belt & suspenders: make a copy on disk!
            ///glyphNetwork.marshal();
        }

        final boolean snapTaken = snap;

        SwingUtilities.invokeLater(
                new Runnable()
        {
            // This part is run on swing thread
            @Override
            public void run ()
            {
                // Update current values
                trainIndex.setText(Integer.toString(index));
                trainError.setText(String.format("%.5f", mse));

                // Update best values
                if (snapTaken) {
                    bestIndex.setText(Integer.toString(index));
                    bestError.setText(String.format("%.5f", mse));

                    if (errorListener != null) {
                        errorListener.stateChanged(null);
                    }
                }

                // Update progress bar ?
                progressBar.setValue(index);

                // Compute ETA
                long sofar = System.currentTimeMillis() - startTime;
                long total = (NeuralClassifier.getInstance().getListEpochs() * sofar) / index;
                Date etaDate = new Date(startTime + total);
                eta.setText(dateFormat.format(etaDate));

                component.repaint();
            }
        });
    }

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

    public void setErrorListener (ChangeListener errorListener)
    {
        this.errorListener = errorListener;
    }

    //-----------------//
    // trainingStarted //
    //-----------------//
    @Override
    public void trainingStarted (final int epochIndex,
                                 final int epochMax,
                                 final double mse)
    {
        // This part is run on trainer thread
        final int index = epochIndex + 1;
        NeuralNetwork network = ((NeuralClassifier) engine).getNetwork();
        bestSnap = network.backup();
        bestMse = mse;

        SwingUtilities.invokeLater(
                new Runnable()
        {
            // This part is run on swing thread
            @Override
            public void run ()
            {
                progressBar.setMaximum(epochMax);

                // Update best values
                bestIndex.setText(Integer.toString(index));
                bestError.setText(String.format("%.5f", mse));

                if (errorListener != null) {
                    errorListener.stateChanged(null);
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
     * @param obs    the task object
     * @param unused not used
     */
    @Override
    public void update (Observable obs,
                        Object unused)
    {
        super.update(obs, unused);

        switch (task.getActivity()) {
        case INACTIVE:
            incrementalTrainAction.setEnabled(true);
            stopAction.setEnabled(false);

            break;

        case SELECTING:
            incrementalTrainAction.setEnabled(false);
            stopAction.setEnabled(false);

            break;

        case TRAINING:
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
        NeuralClassifier network = (NeuralClassifier) engine;
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
        NeuralClassifier network = (NeuralClassifier) engine;
        network.setListEpochs(listEpochs.getValue());
        network.setLearningRate(learningRate.getValue());
        network.setMomentum(momentum.getValue());
        network.setMaxError(maxError.getValue());

        progressBar.setMaximum(network.getListEpochs());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // BestAction //
    //------------//
    private class BestAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public BestAction ()
        {
            super("Use Best");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            NeuralClassifier glyphNetwork = (NeuralClassifier) engine;
            NeuralNetwork network = glyphNetwork.getNetwork();
            network.restore(bestSnap);
            logger.info("Network remaining error : {}", (float) bestMse);
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
        //~ Constructors ---------------------------------------------------------------------------

        public LastAction ()
        {
            super("Use Last");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
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

            NeuralClassifier glyphNetwork = (NeuralClassifier) engine;
            NeuralNetwork network = glyphNetwork.getNetwork();
            network.restore(lastSnap);
            logger.info("Network remaining error : {}", (float) lastMse);
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
        //~ Constructors ---------------------------------------------------------------------------

        public NetworkTrainAction (String title,
                                   StartingMode mode,
                                   boolean confirmationRequired)
        {
            super(title);
            this.mode = mode;
            this.confirmationRequired = confirmationRequired;
        }

        //~ Methods --------------------------------------------------------------------------------
        //-------//
        // train //
        //-------//
        @Override
        public void train ()
        {
            super.train();

            NeuralNetwork network = ((NeuralClassifier) engine).getNetwork();
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
        //~ Methods --------------------------------------------------------------------------------

        // Purpose is just to read and remember the data from the various
        // input fields. Triggered when user presses Enter in one of these
        // fields.
        @Override
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
        //~ Constructors ---------------------------------------------------------------------------

        public StopAction ()
        {
            super("Stop");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            engine.stop();
        }
    }
}
