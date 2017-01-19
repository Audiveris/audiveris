//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T r a i n i n g P a n e l                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.classifier.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.classifier.NeuralClassifier;
import org.audiveris.omr.classifier.Sample;
import static org.audiveris.omr.classifier.ui.Trainer.Task.Activity.*;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.*;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.field.LIntegerField;
import org.audiveris.omr.ui.field.LLabel;
import org.audiveris.omr.ui.util.Panel;

import org.deeplearning4j.nn.api.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
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

    /** UI panel dealing with samples selection. */
    private final SelectionPanel selectionPanel;

    /** Field for Maximum number of epochs to perform. */
    private final LIntegerField maxEpochs = new LIntegerField(
            "Max Epochs",
            "Maximum number of epochs to perform");

    /** Output for Number of iterations performed so far. */
    private final LLabel trainIndex = new LLabel("Epoch:", "Number of epochs performed so far");

    /** Output for score on last iteration. */
    private final LLabel trainScore = new LLabel("Score:", "Score on last iteration");

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
    @Override
    public void epochPeriodDone (int epoch,
                                 double score)
    {
        logger.info(String.format("epoch:%4d score: %.5f", epoch, score));
        display(epoch, score);
    }

    public JComponent getComponent ()
    {
        return component;
    }

    @Override
    public int getEpochPeriod ()
    {
        return constants.listenerPeriod.getValue();
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

            final double score = model.score();
            final int count = (int) iterCount;
            logger.info("Score at iteration " + count + " is " + score);
            display(count, score);
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
     * If quorum is not reached, samples are artificially replicated.
     *
     * @param samples the population of samples to check
     * @return the filtered / augmented collection
     */
    private List<Sample> checkPopulation (List<Sample> samples)
    {
        EnumMap<Shape, List<Sample>> shapeSamples = new EnumMap<Shape, List<Sample>>(Shape.class);

        for (Iterator<Sample> it = samples.iterator(); it.hasNext();) {
            Sample sample = it.next();
            Shape shape = sample.getShape();

            try {
                Shape physicalShape = shape.getPhysicalShape();

                if (physicalShape.isTrainable()) {
                    List<Sample> list = shapeSamples.get(shape);

                    if (list == null) {
                        shapeSamples.put(shape, list = new ArrayList<Sample>());
                    }

                    list.add(sample);
                } else {
                    logger.warn("Removing non trainable shape: {}", physicalShape);
                    it.remove();
                }
            } catch (Exception ex) {
                logger.warn("Removing weird shape: " + shape, ex);
                it.remove();
            }
        }

        final Shape[] shapes = Shape.values();
        final int iMax = LAST_PHYSICAL_SHAPE.ordinal();
        final int minCount = SelectionPanel.getMinShapeSampleCount();
        final List<Sample> newSamples = new ArrayList<Sample>();

        for (int is = 0; is <= iMax; is++) {
            Shape shape = shapes[is];
            List<Sample> list = shapeSamples.get(shape);

            if (list == null) {
                logger.warn("Missing shape: {}", shape);
            } else if (!list.isEmpty()) {
                final int size = list.size();
                int togo = minCount - size;
                newSamples.addAll(list);

                // Ensure minimum sample count is reached for this shape
                if (togo > 0) {
                    Collections.shuffle(list);

                    do {
                        newSamples.addAll(list.subList(0, Math.min(size, togo)));
                        togo -= size;
                    } while (togo > 0);
                }
            }
        }

        return newSamples;
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
                3,
                "",
                Trainer.LABEL_WIDTH,
                Trainer.FIELD_WIDTH);
        PanelBuilder builder = new PanelBuilder(layout, component);
        CellConstraints cst = new CellConstraints();

        // Evaluator Title & Progress Bar
        int r = 1; // ----------------------------
        String title = "Training";
        builder.addSeparator(title, cst.xyw(1, r, 3));
        builder.add(progressBar, cst.xyw(5, r, 7));

        r += 2; // ----------------------------

        builder.add(wholeNumber, cst.xy(5, r)); // ???????????????

        builder.add(maxEpochs.getLabel(), cst.xy(5, r));
        builder.add(maxEpochs.getField(), cst.xy(7, r));

        r += 2; // ----------------------------

        JButton trainButton = new JButton(trainAction);
        trainButton.setToolTipText("Train the classifier from scratch");
        builder.add(trainButton, cst.xy(3, r));

        builder.add(trainIndex.getLabel(), cst.xy(5, r));
        builder.add(trainIndex.getField(), cst.xy(7, r));

        builder.add(trainScore.getLabel(), cst.xy(9, r));
        builder.add(trainScore.getField(), cst.xy(11, r));
    }

    private void display (final int count,
                          final double score)
    {
        SwingUtilities.invokeLater(
                new Runnable()
        {
            // This part is run on swing thread
            @Override
            public void run ()
            {
                // Update current values
                trainIndex.setText(Integer.toString(count));
                trainScore.setText(String.format("%.4f", score));

                // Update progress bar
                progressBar.setValue(count);

                component.repaint();
            }
        });
    }

    //---------------//
    // displayParams //
    //---------------//
    private void displayParams ()
    {
        maxEpochs.setValue(NeuralClassifier.getMaxEpochs());
    }

    //-------------//
    // inputParams //
    //-------------//
    private void inputParams ()
    {
        engine.setMaxEpochs(maxEpochs.getValue());

        progressBar.setMaximum(maxEpochs.getValue());
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
                    progressBar.setMaximum(NeuralClassifier.getMaxEpochs());
                    progressBar.setValue(0);

                    // Check that all trainable shapes (and only those ones) are present
                    // And fill up to quorum count on each shape
                    samples = checkPopulation(samples);

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
                "Period (in epochs) between listener calls");
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
