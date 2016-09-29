//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T r a i n i n g P a n e l                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.classifier.ui;

import omr.classifier.NeuralClassifier;
import omr.classifier.Sample;
import static omr.classifier.ui.Trainer.Task.Activity.*;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.ui.Colors;
import omr.ui.field.LDoubleField;
import omr.ui.field.LIntegerField;
import omr.ui.field.LLabel;
import omr.ui.util.Panel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.deeplearning4j.nn.api.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Class {@code TrainingPanel} is a panel dedicated to the training of a classifier.
 * <p>
 * It is a dedicated companion of class {@link Trainer}.
 *
 * @author Hervé Bitteur
 */
class TrainingPanel
        implements NeuralClassifier.Monitor, Observer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TrainingPanel.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The swing component. */
    protected final Panel component;

    /** Current activity (selecting population, or training engine on selection. */
    protected final Trainer.Task task;

    /** User action to launch the training. */
    protected TrainAction trainAction;

    /** The underlying engine to be trained. */
    protected NeuralClassifier engine;

    /** User progress bar to visualize the training process. */
    protected JProgressBar progressBar = new JProgressBar();

    /**
     * Flag to indicate that the whole population of recorded samples (and not
     * just the core ones) is to be considered
     */
    private boolean useWhole = true;

    /** Display of cardinality of whole population */
    private final JLabel wholeNumber = new JLabel();

    /** Display of cardinality of core population */
    private final JLabel coreNumber = new JLabel();

    /** UI panel dealing with samples selection. */
    private final SelectionPanel selectionPanel;

    /** [no Input] field for Learning rate of the neural network. */
    private final LDoubleField learningRate = new LDoubleField(
            false, // Not editable for the time being
            "Learning Rate",
            "Learning rate of the neural network",
            "%.2f");

    /** [no Input] field for Maximum number of iterations to perform. */
    private final LIntegerField maxIterations = new LIntegerField(
            false, // Not editable for the time being
            "Max Iterations",
            "Maximum number of iterations to perform");

    /** Output for Number of iterations performed so far. */
    private final LLabel trainIndex = new LLabel(
            "Last Iteration:",
            "Number of iterations performed so far");

    /** Output for score on last iteration. */
    private final LLabel trainScore = new LLabel("Last Score:", "Score on last iteration");

    /** Current iteration count. */
    private long iterCount;

    /* Useful? */
    private boolean invoked;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TrainingPanel object.
     *
     * @param task           the current training task
     * @param selectionPanel user panel for samples selection
     */
    public TrainingPanel (Trainer.Task task,
                          SelectionPanel selectionPanel)
    {
        this.engine = NeuralClassifier.getInstance();
        this.task = task;
        this.selectionPanel = selectionPanel;

        component = new Panel();
        component.setNoInsets();

        task.addObserver(this);

        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"),
                "readParams");
        component.getActionMap().put("readParams", new TrainingPanel.ParamAction());

        trainAction = new TrainAction("Train");

        defineLayout();

        engine.setListeners(this);
        displayParams();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // getComponent //
    //--------------//
    /**
     * Give access to the encapsulated swing component
     *
     * @return the user panel
     */
    public JComponent getComponent ()
    {
        return component;
    }

    @Override
    public void invoke ()
    {
        invoked = true;
    }

    @Override
    public boolean invoked ()
    {
        return invoked;
    }

    @Override
    public void iterationDone (Model model,
                               int iteration)
    {
        iterCount++;

        if ((iterCount % constants.listenerPeriod.getValue()) == 0) {
            invoke();

            final double result = model.score();
            final int count = (int) iterCount;
            logger.info("Score at iteration " + count + " is " + result);

            SwingUtilities.invokeLater(
                    new Runnable()
            {
                // This part is run on swing thread
                @Override
                public void run ()
                {
                    // Update current values
                    trainIndex.setText(Integer.toString(count));
                    trainScore.setText(String.format("%.4f", result));

                    // Update progress bar ?
                    progressBar.setValue(count);

                    component.repaint();
                }
            });
        }
    }

    //--------//
    // update //
    //--------//
    /**
     * Method triggered by new task activity : the train action is enabled only
     * when no activity is going on.
     *
     * @param obs    the task object
     * @param unused not used
     */
    @Override
    public void update (Observable obs,
                        Object unused)
    {
        trainAction.setEnabled(task.getActivity() == INACTIVE);
    }

    //----------//
    // useWhole //
    //----------//
    /**
     * Tell whether the whole sample base is to be used, or just the core base
     *
     * @return true if whole, false if core
     */
    public boolean useWhole ()
    {
        return useWhole;
    }

    //-----------------//
    // checkPopulation //
    //-----------------//
    /**
     * Check that all trainable shapes are present in the training population and that
     * only legal shapes are present.
     * If illegal (non trainable) shapes are found, they are removed from the population.
     *
     * @param samples the population of samples to check
     */
    private void checkPopulation (List<Sample> samples)
    {
        boolean[] present = new boolean[LAST_PHYSICAL_SHAPE.ordinal() + 1];
        Arrays.fill(present, false);

        for (Iterator<Sample> it = samples.iterator(); it.hasNext();) {
            Sample sample = it.next();
            Shape shape = sample.getShape();

            try {
                Shape physicalShape = shape.getPhysicalShape();

                if (physicalShape.isTrainable()) {
                    present[physicalShape.ordinal()] = true;
                } else {
                    logger.warn("Removing non trainable shape: {}", physicalShape);
                    it.remove();
                }
            } catch (Exception ex) {
                logger.warn("Removing weird shape: " + shape, ex);
                it.remove();
            }
        }

        for (int i = 0; i < present.length; i++) {
            if (!present[i]) {
                logger.warn("Missing shape: {}", Shape.values()[i]);
            }
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout.
     */
    private void defineLayout ()
    {
        progressBar.setForeground(Colors.PROGRESS_BAR);

        FormLayout layout = Panel.makeFormLayout(
                3,
                4,
                "",
                Trainer.LABEL_WIDTH,
                Trainer.FIELD_WIDTH);
        PanelBuilder builder = new PanelBuilder(layout, component);
        CellConstraints cst = new CellConstraints();

        // Evaluator Title & Progress Bar
        int r = 1; // ----------------------------
        String title = "Training";
        builder.addSeparator(title, cst.xyw(1, r, 7));
        builder.add(progressBar, cst.xyw(9, r, 7));

        r += 2; // ----------------------------

        builder.add(wholeNumber, cst.xy(5, r)); // ???????????????

        builder.add(maxIterations.getLabel(), cst.xy(9, r));
        builder.add(maxIterations.getField(), cst.xy(11, r));

        builder.add(learningRate.getLabel(), cst.xy(13, r));
        builder.add(learningRate.getField(), cst.xy(15, r));

        r += 2; // ----------------------------

        JButton trainButton = new JButton(trainAction);
        trainButton.setToolTipText("Train the classifier from scratch");
        builder.add(trainButton, cst.xy(3, r));

        builder.add(trainIndex.getLabel(), cst.xy(9, r));
        builder.add(trainIndex.getField(), cst.xy(11, r));

        builder.add(trainScore.getLabel(), cst.xy(13, r));
        builder.add(trainScore.getField(), cst.xy(15, r));
    }

    //---------------//
    // displayParams //
    //---------------//
    private void displayParams ()
    {
        maxIterations.setValue(NeuralClassifier.getMaxIterations());
        learningRate.setValue(engine.getLearningRate());
    }

    //-------------//
    // inputParams //
    //-------------//
    private void inputParams ()
    {
        engine.setMaxIterations(maxIterations.getValue());
        engine.setLearningRate(learningRate.getValue());

        progressBar.setMaximum(maxIterations.getValue());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // TrainAction //
    //-------------//
    protected class TrainAction
            extends AbstractAction
    {
        //~ Instance fields ------------------------------------------------------------------------

        protected boolean confirmationRequired = true;

        //~ Constructors ---------------------------------------------------------------------------
        public TrainAction (String title)
        {
            super(title);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Ask user confirmation
            if (confirmationRequired) {
                int answer = JOptionPane.showConfirmDialog(
                        component,
                        "Confirm retrain Neural Network from scratch?");

                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            class Worker
                    extends Thread
            {

                @Override
                public void run ()
                {
                    task.setActivity(TRAINING);

                    List<Sample> samples = selectionPanel.getTrainSamples();
                    progressBar.setMaximum(NeuralClassifier.getMaxIterations());
                    progressBar.setValue(0);

                    // Check that all trainable shapes (and only those ones) are
                    // present in the training population
                    checkPopulation(samples);

                    // Train on the data set
                    engine.train(samples, TrainingPanel.this);

                    task.setActivity(INACTIVE);
                }
            }

            Worker worker = new Worker();
            worker.setPriority(Thread.MIN_PRIORITY);
            worker.start();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer listenerPeriod = new Constant.Integer(
                "period",
                50,
                "Iteration period between listener calls");
    }

    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        // Purpose is just to read and remember the data from the various input fields.
        // Triggered when user presses Enter in one of these fields.
        @Override
        public void actionPerformed (ActionEvent e)
        {
            inputParams();
            displayParams();
        }
    }
}
//
//    //------------//
//    // CoreAction //
//    //------------//
//    private class CoreAction
//            extends AbstractAction
//    {
//        //~ Instance fields ------------------------------------------------------------------------
//
//        final SwingWorker<Integer, Object> worker = new SwingWorker<Integer, Object>()
//        {
//            @Override
//            public void done ()
//            {
//                try {
//                    coreNumber.setText("" + get());
//                } catch (Exception ex) {
//                    logger.warn("Error while loading core base", ex);
//                }
//            }
//
//            @Override
//            protected Integer doInBackground ()
//            {
//                return selectionPanel.getTrainSamples(false).size();
//            }
//        };
//
//        //~ Constructors ---------------------------------------------------------------------------
//        public CoreAction ()
//        {
//            super("Core");
//        }
//
//        //~ Methods --------------------------------------------------------------------------------
//        @Override
//        public void actionPerformed (ActionEvent e)
//        {
//            useWhole = false;
//            worker.execute();
//        }
//    }
//
