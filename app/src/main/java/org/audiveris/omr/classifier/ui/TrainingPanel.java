//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T r a i n i n g P a n e l                                    //
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
package org.audiveris.omr.classifier.ui;

import org.audiveris.omr.classifier.Sample;
import org.audiveris.omr.classifier.TrainingMonitor;
import static org.audiveris.omr.classifier.ui.Trainer.Task.Activity.INACTIVE;
import static org.audiveris.omr.classifier.ui.Trainer.Task.Activity.TRAINING;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.LAST_PHYSICAL_SHAPE;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.field.LDoubleField;
import org.audiveris.omr.ui.field.LIntegerField;
import org.audiveris.omr.ui.field.LLabel;
import org.audiveris.omr.ui.util.Panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Class <code>TrainingPanel</code> is a panel dedicated to the training of a classifier.
 * <p>
 * It is a dedicated companion of class {@link Trainer}.
 *
 * @author Hervé Bitteur
 */
class TrainingPanel
        implements TrainingMonitor, PropertyChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TrainingPanel.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The swing component. */
    protected final Panel component;

    /** Current activity (selecting population, or training classifier on selection. */
    protected final Trainer.Task task;

    /** User action to reset the classifier. */
    protected ResetAction resetAction;

    /** User action to launch incremental training. */
    protected TrainAction trainAction;

    /** User action to stop training. */
    protected StopAction stopAction;

    /** User progress bar to visualize the training process. */
    protected JProgressBar progressBar = new JProgressBar();

    /** UI panel dealing with samples selection. */
    private final SelectionPanel selectionPanel;

    /** Field for learning rate. */
    private final LDoubleField learning = new LDoubleField("Learning", "Learning rate parameter");

    /** Field for momentum. */
    private final LDoubleField momentum = new LDoubleField("Momentum", "Momentum parameter");

    /** Field for regularization rate. */
    private final LDoubleField lambda = new LDoubleField("Lambda", "Regularization parameter");

    /** Field for number of epochs to perform. */
    private final LIntegerField epochs = new LIntegerField("Epochs", "Number of epochs to perform");

    /** Output for the total number of epochs performed so far. */
    private final LLabel epochsTotal = new LLabel("Total:", "Total epochs so far");

    /** Output for current epoch. */
    private final LLabel epochIndex = new LLabel("Epoch:", "Current epoch");

    /** Output for score on last iteration. */
    private final LLabel trainScore = new LLabel("Score:", "Score on last epoch");

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new TrainingPanel object.
     *
     * @param task           the current training task
     * @param selectionPanel user panel for samples selection
     */
    TrainingPanel (Trainer.Task task,
                   SelectionPanel selectionPanel)
    {
        this.task = task;
        this.selectionPanel = selectionPanel;

        component = new Panel();
        component.setNoInsets();

        task.addPropertyChangeListener(this);

        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"),
                "readParams");
        component.getActionMap().put("readParams", new ParamAction());

        resetAction = new ResetAction();
        trainAction = new TrainAction();
        stopAction = new StopAction();

        defineLayout();

        task.classifier.addListener(this);

        learning.setValue(task.classifier.getLearningRate());
        momentum.setValue(task.classifier.getMomentum());
        lambda.setValue(task.classifier.getLambda());

        epochs.setValue(task.classifier.getMaxEpochs());
        inputParams();

        display(task.classifier.getEpochsTotal(), 0, null);
    }

    //~ Methods ------------------------------------------------------------------------------------

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
        final EnumMap<Shape, List<Sample>> shapeSamples = new EnumMap<>(Shape.class);

        for (Iterator<Sample> it = samples.iterator(); it.hasNext();) {
            final Sample sample = it.next();

            try {
                final Shape physicalShape = sample.getShape().getPhysicalShape();

                if (physicalShape.isTrainable()) {
                    List<Sample> list = shapeSamples.get(physicalShape);

                    if (list == null) {
                        shapeSamples.put(physicalShape, list = new ArrayList<>());
                    }

                    list.add(sample);
                } else {
                    logger.warn("Removing non trainable shape: {}", physicalShape);
                    it.remove();
                }
            } catch (Exception ex) {
                logger.warn("Removing weird sample: " + sample, ex);
                it.remove();
            }
        }

        final Shape[] shapes = Shape.values();
        final int iMax = LAST_PHYSICAL_SHAPE.ordinal();
        final int minCount = SelectionPanel.getMinTrainCount();
        final List<Sample> newSamples = new ArrayList<>();

        for (int is = 0; is <= iMax; is++) {
            final Shape physicalShape = shapes[is];
            final List<Sample> list = shapeSamples.get(physicalShape);

            if (list == null) {
                logger.warn("Missing shape: {}", physicalShape);
            } else if (!list.isEmpty()) {
                logger.debug(String.format("%4d %s", list.size(), physicalShape));
                final int size = list.size();
                int togo = minCount - size;
                newSamples.addAll(list);

                // Ensure minimum sample count is reached for this shape
                if ((togo > 0) && (physicalShape != Shape.CLUTTER)) {
                    Collections.shuffle(list);

                    do {
                        final int added = Math.min(size, togo);
                        newSamples.addAll(list.subList(0, added));
                        logger.debug(String.format("     added %d", added));
                        togo -= added;
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

        final FormLayout layout = Panel.makeFormLayout(
                4,
                3,
                "",
                Trainer.LABEL_WIDTH,
                Trainer.FIELD_WIDTH);
        final FormBuilder builder = FormBuilder.create().layout(layout).panel(component);

        int r = 1; // ----------------------------

        // Evaluator Title & Progress Bar
        final String title = "Training";
        builder.addSeparator(title).xyw(1, r, 3);
        builder.addRaw(progressBar).xyw(5, r, 7);

        r += 2; // ----------------------------

        builder.addRaw(learning.getLabel()).xy(1, r);
        builder.addRaw(learning.getField()).xy(3, r);

        builder.addRaw(momentum.getLabel()).xy(5, r);
        builder.addRaw(momentum.getField()).xy(7, r);

        builder.addRaw(lambda.getLabel()).xy(9, r);
        builder.addRaw(lambda.getField()).xy(11, r);

        r += 2; // ----------------------------

        builder.addRaw(new JButton(resetAction)).xy(1, r);
        builder.addRaw(new JButton(trainAction)).xy(3, r);

        builder.addRaw(epochs.getLabel()).xy(5, r);
        builder.addRaw(epochs.getField()).xy(7, r);

        builder.addRaw(epochsTotal.getLabel()).xy(9, r);
        builder.addRaw(epochsTotal.getField()).xy(11, r);

        r += 2; // ----------------------------

        builder.addRaw(new JButton(stopAction)).xy(3, r);

        builder.addRaw(epochIndex.getLabel()).xy(5, r);
        builder.addRaw(epochIndex.getField()).xy(7, r);

        builder.addRaw(trainScore.getLabel()).xy(9, r);
        builder.addRaw(trainScore.getField()).xy(11, r);
    }

    //---------//
    // display //
    //---------//
    private void display (final int total,
                          final int epoch,
                          final Double score)
    {
        // This part is run on swing thread
        SwingUtilities.invokeLater( () -> {
            // Update current values
            epochsTotal.setText(Integer.toString(total));
            epochIndex.setText(Integer.toString(epoch));
            trainScore.setText(score != null ? String.format("%.4f", score) : "");

            // Update progress bar
            progressBar.setValue(epoch);

            component.repaint();
        });
    }

    //--------------//
    // getComponent //
    //--------------//
    public JComponent getComponent ()
    {
        return component;
    }

    //--------------------//
    // getIterationPeriod //
    //--------------------//
    @Override
    public int getIterationPeriod ()
    {
        return constants.listenerPeriod.getValue();
    }

    //-------------//
    // inputParams //
    //-------------//
    private void inputParams ()
    {
        //task.classifier.setMaxEpochs(epochs.getValue());

        if (learning.getValue() != task.classifier.getLearningRate()) {
            task.classifier.setLearningRate(learning.getValue());
        }

        if (momentum.getValue() != task.classifier.getMomentum()) {
            task.classifier.setMomentum(momentum.getValue());
        }

        if (lambda.getValue() != task.classifier.getLambda()) {
            task.classifier.setLambda(lambda.getValue());
        }

        progressBar.setMaximum(epochs.getValue());
    }

    //---------------------//
    // iterationPeriodDone //
    //---------------------//
    @Override
    public void iterationPeriodDone (int total,
                                     int epoch,
                                     double mse,
                                     double hsw,
                                     double osw)
    {
        logger.info(
                String.format(
                        "epochsTotal:%4d epoch:%4d mse: %.5f hiddenW2:%5.0f outputW2:%5.0f",
                        total,
                        epoch,
                        mse,
                        0.5 * hsw,
                        0.5 * osw));
        display(total, epoch, mse);
    }

    //----------------//
    // propertyChange //
    //----------------//
    /**
     * Method triggered by new task activity : the train action is enabled only
     * when no activity is going on.
     *
     * @param evt the signaled event
     */
    @Override
    public void propertyChange (PropertyChangeEvent evt)
    {
        epochs.setEnabled(task.getActivity() == INACTIVE);
        resetAction.setEnabled(task.getActivity() == INACTIVE);
        trainAction.setEnabled(task.getActivity() == INACTIVE);
        stopAction.setEnabled(task.getActivity() == TRAINING);

        learning.setEnabled(task.getActivity() == INACTIVE);
        momentum.setEnabled(task.getActivity() == INACTIVE);
        lambda.setEnabled(task.getActivity() == INACTIVE);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer listenerPeriod = new Constant.Integer(
                "period",
                1,
                "Period (in iterations) between listener calls");
    }

    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
            extends AbstractAction
    {
        // Purpose is just to read and remember the data from the various input fields.
        // Triggered when user presses Enter in one of these fields.
        @Override
        public void actionPerformed (ActionEvent e)
        {
            inputParams();
        }
    }

    //-------------//
    // ResetAction //
    //-------------//
    protected class ResetAction
            extends AbstractAction
    {
        ResetAction ()
        {
            super("Reset");
            putValue(Action.SHORT_DESCRIPTION, "Restart from scratch");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Ask user confirmation
            int answer = JOptionPane.showConfirmDialog(component, "Confirm reset of classifier?");

            if (answer == JOptionPane.YES_OPTION) {
                task.classifier.reset();
                display(0, 0, null);
            }
        }
    }

    //------------//
    // StopAction //
    //------------//
    protected class StopAction
            extends AbstractAction
    {
        StopAction ()
        {
            super("Stop");
            putValue(Action.SHORT_DESCRIPTION, "Stop the training");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            task.classifier.stop();
        }
    }

    //-------------//
    // TrainAction //
    //-------------//
    protected class TrainAction
            extends AbstractAction
    {
        TrainAction ()
        {
            super("Train");
            putValue(Action.SHORT_DESCRIPTION, "Train the classifier");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            class Worker
                    extends Thread
            {
                @Override
                public void run ()
                {
                    task.setActivity(TRAINING);

                    List<Sample> samples = selectionPanel.getTrainSamples();
                    progressBar.setMaximum(epochs.getValue());
                    progressBar.setValue(0);

                    // Check that all trainable shapes (and only those ones) are present
                    // And fill up to quorum count on each shape
                    samples = checkPopulation(samples);

                    // Train on the data set
                    task.classifier.train(samples, epochs.getValue());

                    task.setActivity(INACTIVE);
                }
            }

            Worker worker = new Worker();
            worker.setPriority(Thread.MIN_PRIORITY);
            worker.start();
        }
    }
}
