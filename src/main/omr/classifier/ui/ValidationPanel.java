//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 V a l i d a t i o n P a n e l                                  //
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

import omr.classifier.Classifier;
import omr.classifier.Evaluation;
import omr.classifier.NeuralClassifier;
import static omr.classifier.NeuralClassifier.getRawDataSet;
import omr.classifier.Sample;
import omr.classifier.ui.Trainer.Task;
import static omr.classifier.ui.Trainer.Task.Activity.*;

import omr.glyph.Grades;
import omr.glyph.ShapeSet;

import omr.ui.Colors;
import omr.ui.field.LLabel;
import omr.ui.util.Panel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
 * <p>
 * It is a dedicated companion of class {@link Trainer}.
 *
 * @author Hervé Bitteur
 */
class ValidationPanel
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

    /** The classifier to validate. */
    private final Classifier classifier;

    /** User progress bar to visualize the validation process. */
    private final JProgressBar progressBar = new JProgressBar();

    /** User interface that handles samples selection. */
    private final SelectionPanel selectionPanel;

    /** User action to validate the classifier against training base. */
    private final ValidateAction validateAction = new ValidateAction();

    /** Display percentage of samples correctly recognized. */
    private final LLabel pcValue = new LLabel("%:", "Percentage of samples correctly recognized");

    /** Display number of samples correctly recognized. */
    private final LLabel positiveValue = new LLabel(
            "True Positives:",
            "Number of samples correctly recognized");

    /** Display number of samples mistaken with some other shape. */
    private final LLabel falsePositiveValue = new LLabel(
            "False Positives:",
            "Number of samples incorrectly recognized");

    /** Collection of samples leading to false positives. */
    private final List<Sample> falsePositives = new ArrayList<Sample>();

    /** User action to investigate on false positives. */
    private final FalsePositiveAction falsePositiveAction = new FalsePositiveAction();

    /** Display number of samples weakly recognized. */
    private final LLabel weakPositiveValue = new LLabel(
            "Weak Positives:",
            "Number of samples weakly recognized");

    /** Collection of samples not recognized (false negatives). */
    private final List<Sample> weakPositives = new ArrayList<Sample>();

    /** User action to investigate on weak positives. */
    private final WeakPositiveAction weakPositiveAction = new WeakPositiveAction();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ValidationPanel object.
     *
     * @param task           the current training activity
     * @param classifier     the classifier to validate
     * @param selectionPanel user panel for selection
     */
    public ValidationPanel (Trainer.Task task,
                            Classifier classifier,
                            SelectionPanel selectionPanel)
    {
        this.classifier = classifier;
        this.selectionPanel = selectionPanel;
        this.task = task;
        task.addObserver(this);

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
                3,
                4,
                "",
                Trainer.LABEL_WIDTH,
                Trainer.FIELD_WIDTH);
        PanelBuilder builder = new PanelBuilder(layout, component);

        // Validation title & progress bar
        int r = 1;
        builder.addSeparator("Validation", cst.xyw(1, r, 7));
        builder.add(progressBar, cst.xyw(9, r, 7));
        progressBar.setForeground(Colors.PROGRESS_BAR);

        r += 2; // ----------------------------

        builder.add(positiveValue.getLabel(), cst.xy(5, r));
        builder.add(positiveValue.getField(), cst.xy(7, r));
        builder.add(weakPositiveValue.getLabel(), cst.xy(9, r));
        builder.add(weakPositiveValue.getField(), cst.xy(11, r));
        builder.add(falsePositiveValue.getLabel(), cst.xy(13, r));
        builder.add(falsePositiveValue.getField(), cst.xy(15, r));

        r += 2; // ----------------------------

        JButton validateButton = new JButton(validateAction);
        validateButton.setToolTipText("Validate the classifier on current base of samples");
        builder.add(validateButton, cst.xy(3, r));

        JButton weakPositiveButton = new JButton(weakPositiveAction);
        weakPositiveButton.setToolTipText("Display the impacted samples for verification");

        JButton falsePositiveButton = new JButton(falsePositiveAction);
        falsePositiveButton.setToolTipText("Display the impacted samples for verification");

        builder.add(pcValue.getLabel(), cst.xy(5, r));
        builder.add(pcValue.getField(), cst.xy(7, r));
        builder.add(weakPositiveButton, cst.xy(11, r));
        builder.add(falsePositiveButton, cst.xy(15, r));
    }

    //---------------//
    // runValidation //
    //---------------//
    private void runValidation ()
    {
        logger.info("Validating {} classifier ...", classifier.getName());

        // Empty the display
        positiveValue.setText("");
        pcValue.setText("");
        weakPositiveValue.setText("");
        falsePositiveValue.setText("");
        weakPositiveAction.setEnabled(false);
        falsePositiveAction.setEnabled(false);

        weakPositives.clear();
        falsePositives.clear();

        int positives = 0;

        final List<Sample> samples = selectionPanel.getTestSamples();

        progressBar.setValue(0);
        progressBar.setMaximum(samples.size());

        int index = 0;

        for (Sample sample : samples) {
            Evaluation[] evals = classifier.evaluate(
                    sample,
                    sample.getInterline(),
                    1,
                    Grades.validationMinGrade,
                    Classifier.NO_CONDITIONS);

            if (evals.length == 0) {
                weakPositives.add(sample);
                System.out.printf("%-35s not recognized%n", sample.toString());
            } else if (evals[0].shape.getPhysicalShape() == sample.getShape().getPhysicalShape()) {
                positives++;
            } else {
                falsePositives.add(sample);
                System.out.printf(
                        "%-35s mistaken for %s%n",
                        sample.toString(),
                        evals[0].shape.getPhysicalShape());
            }

            progressBar.setValue(++index); // Update progress bar
        }

        int total = samples.size();
        double pc = ((double) positives * 100) / (double) total;
        String pcStr = String.format("%.2f%%", pc);
        logger.info(
                "{}Classifier ratio= {} : {}/{}",
                classifier.getName(),
                pcStr,
                positives,
                total);
        positiveValue.setText(Integer.toString(positives));
        pcValue.setText(String.format("%.2f", pc));
        weakPositiveValue.setText(Integer.toString(weakPositives.size()));
        falsePositiveValue.setText(Integer.toString(falsePositives.size()));

        // Evaluate
        final NeuralClassifier neuralClassifier = (NeuralClassifier) classifier;
        final MultiLayerNetwork model = neuralClassifier.getModel();
        DataSet dataSet = getRawDataSet(samples);
        neuralClassifier.normalize(dataSet.getFeatures());

        final List<String> names = Arrays.asList(ShapeSet.getPhysicalShapeNames());
        org.deeplearning4j.eval.Evaluation eval = new org.deeplearning4j.eval.Evaluation(names);
        INDArray guesses = model.output(dataSet.getFeatureMatrix());
        eval.eval(dataSet.getLabels(), guesses);
        System.out.println(eval.stats(true));

        DecimalFormat df = new DecimalFormat("#.####");
        logger.info(
                String.format(
                        "Accuracy: %s Precision: %s Recall: %s F1 Score: %s",
                        df.format(eval.accuracy()),
                        df.format(eval.precision()),
                        df.format(eval.recall()),
                        df.format(eval.f1())));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------------------//
    // FalsePositiveAction //
    //---------------------//
    private class FalsePositiveAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public FalsePositiveAction ()
        {
            super("Verify");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            SampleVerifier.getInstance().displayAll(falsePositives);
            SampleVerifier.getInstance().setVisible();
        }
    }

    //----------------//
    // ValidateAction //
    //----------------//
    private class ValidateAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public ValidateAction ()
        {
            super("Test");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            executor.execute(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    setEnabled(false);
                    task.setActivity(VALIDATION);
                    runValidation();
                    weakPositiveAction.setEnabled(weakPositives.size() > 0);
                    falsePositiveAction.setEnabled(!falsePositives.isEmpty());
                    task.setActivity(INACTIVE);
                    setEnabled(true);
                }
            });
        }
    }

    //--------------------//
    // WeakPositiveAction //
    //--------------------//
    private class WeakPositiveAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public WeakPositiveAction ()
        {
            super("Verify");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            SampleVerifier.getInstance().displayAll(weakPositives);
            SampleVerifier.getInstance().setVisible();
        }
    }
}
