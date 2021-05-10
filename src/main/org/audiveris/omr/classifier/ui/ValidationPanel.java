//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 V a l i d a t i o n P a n e l                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.classifier.Classifier;
import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.classifier.Sample;
import org.audiveris.omr.classifier.SampleSource;
import org.audiveris.omr.classifier.ui.Trainer.Task;
import static org.audiveris.omr.classifier.ui.Trainer.Task.Activity.*;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.field.LLabel;
import org.audiveris.omr.ui.util.Panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JProgressBar;

/**
 * Class {@code ValidationPanel} handles the validation of a classifier against a
 * selected population of samples.
 *
 * @author Hervé Bitteur
 */
public class ValidationPanel
        implements Observer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ValidationPanel.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Swing component. */
    private final Panel component;

    /** Current activity. */
    private final Trainer.Task task;

    /** Dedicated executor for validation. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** User progress bar to visualize the validation process. */
    private final JProgressBar progressBar = new JProgressBar();

    /** Where samples are got from. */
    private final SampleSource sampleSource;

    /** Validation of train set vs test set?. */
    private final boolean isTrainSet;

    /** Name of set to validate. */
    private final String setName;

    /** User action to validate the classifier against training base. */
    private final ValidateAction validateAction = new ValidateAction();

    /** Display percentage of samples correctly recognized. */
    private final LLabel accuracyValue = new LLabel(
            "Accuracy:",
            "Percentage of samples correctly recognized");

    /** Display number of samples correctly recognized. */
    private final LLabel positiveValue = new LLabel(
            "True Positives:",
            "Number of samples correctly recognized");

    /** Display number of samples mistaken with some other shape. */
    private final LLabel falsePositiveValue = new LLabel(
            "False Positives:",
            "Number of samples incorrectly recognized");

    /** Collection of samples leading to false positives. */
    private final List<Sample> falsePositives = new ArrayList<>();

    /** User action to investigate on false positives. */
    private final FalsePositiveAction falsePositiveAction = new FalsePositiveAction();

    /** Display number of samples weakly recognized. */
    private final LLabel weakPositiveValue = new LLabel(
            "Weak Positives:",
            "Number of samples weakly recognized");

    /** Collection of samples not recognized (false negatives). */
    private final List<Sample> weakPositives = new ArrayList<>();

    /** User action to investigate on weak positives. */
    private final WeakPositiveAction weakPositiveAction = new WeakPositiveAction();

    /** Display number of samples weakly negative. */
    private final LLabel weakNegativeValue = new LLabel(
            "Weak Negatives:",
            "Number of samples weakly negative");

    /** Collection of samples weakly negatives. */
    private final List<Sample> weakNegatives = new ArrayList<>();

    /** User action to investigate on weak negatives. */
    private final WeakNegativeAction weakNegativeAction = new WeakNegativeAction();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ValidationPanel object.
     *
     * @param task       the current training activity
     * @param source     source for samples
     * @param isTrainSet True for train set, False for test set
     */
    public ValidationPanel (Trainer.Task task,
                            SampleSource source,
                            boolean isTrainSet)
    {
        this.sampleSource = source;
        this.task = task;
        this.isTrainSet = isTrainSet;

        setName = (isTrainSet ? "train" : "test");

        if (task != null) {
            task.addObserver(this);
        }

        component = new Panel();
        component.setNoInsets();

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // getComponent //
    //--------------//
    /**
     * Give access to the encapsulated swing component.
     *
     * @return the user panel
     */
    public JComponent getComponent ()
    {
        return component;
    }

    //--------//
    // update //
    //--------//
    /**
     * A degenerated version, just to disable by default the
     * verification actions whenever a new task activity is notified.
     * These actions are then re-enabled only at the end of the validation run.
     *
     * @param obs    not used
     * @param unused not used
     */
    @Override
    public void update (Observable obs,
                        Object unused)
    {
        weakPositiveAction.setEnabled(!weakPositives.isEmpty());
        falsePositiveAction.setEnabled(!falsePositives.isEmpty());
        weakNegativeAction.setEnabled(!weakNegatives.isEmpty());

        Task task = (Task) obs;
        validateAction.setEnabled(task.getActivity() == Task.Activity.INACTIVE);
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        /** Common JGoogies constraints for this class and its subclass if any */
        CellConstraints cst = new CellConstraints();

        /** Common JGoogies builder for this class and its subclass if any */
        FormLayout layout = Panel.makeFormLayout(
                5,
                3,
                "",
                Trainer.LABEL_WIDTH,
                Trainer.FIELD_WIDTH);
        PanelBuilder builder = new PanelBuilder(layout, component);

        // Validation title & progress bar
        int r = 1;
        String title = "Validation [" + setName + " set]";
        builder.addSeparator(title, cst.xyw(1, r, 3));
        builder.add(progressBar, cst.xyw(5, r, 7));
        progressBar.setForeground(Colors.PROGRESS_BAR);

        r += 2; // ----------------------------

        builder.add(positiveValue.getLabel(), cst.xyw(4, r, 2));
        builder.add(positiveValue.getField(), cst.xy(7, r));

        builder.add(falsePositiveValue.getLabel(), cst.xyw(8, r, 2));
        builder.add(falsePositiveValue.getField(), cst.xy(11, r));

        r += 2; // ----------------------------

        JButton validateButton = new JButton(validateAction);
        validateButton.setToolTipText("Validate the classifier on current base of samples");
        builder.add(validateButton, cst.xy(3, r));

        JButton falsePositiveButton = new JButton(falsePositiveAction);
        falsePositiveButton.setToolTipText("Display the impacted samples for verification");
        builder.add(falsePositiveButton, cst.xy(11, r));

        r += 2; // ----------------------------

        builder.add(accuracyValue.getLabel(), cst.xy(1, r));
        builder.add(accuracyValue.getField(), cst.xy(3, r));

        builder.add(weakPositiveValue.getLabel(), cst.xyw(4, r, 2));
        builder.add(weakPositiveValue.getField(), cst.xy(7, r));

        builder.add(weakNegativeValue.getLabel(), cst.xyw(8, r, 2));
        builder.add(weakNegativeValue.getField(), cst.xy(11, r));

        r += 2; // ----------------------------

        JButton weakPositiveButton = new JButton(weakPositiveAction);
        weakPositiveButton.setToolTipText("Display the impacted samples for verification");
        builder.add(weakPositiveButton, cst.xy(7, r));

        JButton weakNegativeButton = new JButton(weakNegativeAction);
        weakNegativeButton.setToolTipText("Display the impacted samples for verification");
        builder.add(weakNegativeButton, cst.xy(11, r));
    }

    //---------------//
    // runValidation //
    //---------------//
    private void runValidation ()
    {
        logger.info("Validating {} on {} set...", task.classifier.getName(), setName);

        // Empty the display
        positiveValue.setText("");
        accuracyValue.setText("");
        weakPositiveValue.setText("");
        falsePositiveValue.setText("");
        weakNegativeValue.setText("");

        weakPositiveAction.setEnabled(false);
        falsePositiveAction.setEnabled(false);
        weakNegativeAction.setEnabled(false);

        weakPositives.clear();
        falsePositives.clear();
        weakNegatives.clear();

        int positives = 0;

        // Validation is performed on TRAIN or TEST set
        final List<Sample> samples = isTrainSet ? sampleSource.getTrainSamples()
                : sampleSource.getTestSamples();

        progressBar.setValue(0);
        progressBar.setMaximum(samples.size());

        int index = 0;

        for (Sample sample : samples) {
            Evaluation[] evals = task.classifier.evaluate(
                    sample,
                    sample.getInterline(),
                    1,
                    0,
                    Classifier.NO_CONDITIONS);
            Evaluation eval = evals[0];

            if (eval.shape.getPhysicalShape() == sample.getShape().getPhysicalShape()) {
                if (eval.grade >= Grades.validationMinGrade) {
                    positives++;
                } else {
                    weakPositives.add(sample);
                }
            } else {
                if (eval.grade >= Grades.validationMinGrade) {
                    falsePositives.add(sample);
                } else {
                    weakNegatives.add(sample);
                }
            }

            progressBar.setValue(++index); // Update progress bar
        }

        int total = samples.size();
        int allPositives = positives + weakPositives.size();
        double accuracy = allPositives / (double) total;
        DecimalFormat df = new DecimalFormat("#.####");
        String accuStr = df.format(accuracy);
        logger.info(
                "{} accuracy: {}  {}/{}",
                task.classifier.getName(),
                accuStr,
                allPositives,
                total);
        accuracyValue.setText(accuStr);
        positiveValue.setText(Integer.toString(positives));
        weakPositiveValue.setText(Integer.toString(weakPositives.size()));
        falsePositiveValue.setText(Integer.toString(falsePositives.size()));
        weakNegativeValue.setText(Integer.toString(weakNegatives.size()));

        //
        //        // Additional evaluation
        //        if (task.classifier instanceof DeepClassifier) {
        //            final DeepClassifier deepClassifier = (DeepClassifier) task.classifier;
        //            final MultiLayerNetwork model = deepClassifier.getModel();
        //            DataSet dataSet = deepClassifier.getRawDataSet(samples);
        //            deepClassifier.normalize(dataSet.getFeatures());
        //
        //            final List<String> names = Arrays.asList(
        //                    ShapeSet.getPhysicalShapeNames());
        //            org.deeplearning4j.eval.Evaluation eval = new org.deeplearning4j.eval.Evaluation(names);
        //            INDArray guesses = model.output(dataSet.getFeatureMatrix());
        //            eval.eval(dataSet.getLabels(), guesses);
        //            System.out.println(eval.stats(true));
        //
        //            logger.info(
        //                    String.format(
        //                            "Accuracy: %s Precision: %s Recall: %s F1 Score: %s",
        //                            df.format(eval.accuracy()),
        //                            df.format(eval.precision()),
        //                            df.format(eval.recall()),
        //                            df.format(eval.f1())));
        //        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------------------//
    // FalsePositiveAction //
    //---------------------//
    private class FalsePositiveAction
            extends AbstractAction
    {

        FalsePositiveAction ()
        {
            super("View");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            SampleBrowser.getInstance().displayAll(falsePositives);
            SampleBrowser.getInstance().setVisible();
        }
    }

    //----------------//
    // ValidateAction //
    //----------------//
    private class ValidateAction
            extends AbstractAction
    {

        ValidateAction ()
        {
            super("Test");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            executor.execute(() -> {
                setEnabled(false);

                if (task != null) {
                    task.setActivity(VALIDATION);
                }

                runValidation();

                weakPositiveAction.setEnabled(weakPositives.size() > 0);
                falsePositiveAction.setEnabled(!falsePositives.isEmpty());
                weakNegativeAction.setEnabled(weakNegatives.size() > 0);

                if (task != null) {
                    task.setActivity(INACTIVE);
                }

                setEnabled(true);
            });
        }
    }

    //--------------------//
    // WeakNegativeAction //
    //--------------------//
    private class WeakNegativeAction
            extends AbstractAction
    {

        WeakNegativeAction ()
        {
            super("View");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            SampleBrowser.getInstance().displayAll(weakNegatives);
            SampleBrowser.getInstance().setVisible();
        }
    }

    //--------------------//
    // WeakPositiveAction //
    //--------------------//
    private class WeakPositiveAction
            extends AbstractAction
    {

        WeakPositiveAction ()
        {
            super("View");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            SampleBrowser.getInstance().displayAll(weakPositives);
            SampleBrowser.getInstance().setVisible();
        }
    }
}
