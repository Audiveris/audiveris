//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 V a l i d a t i o n P a n e l                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier.ui;

import omr.classifier.Classifier;
import static omr.classifier.Classifier.Condition.ALLOWED;
import omr.classifier.Evaluation;
import omr.classifier.Sample;
import omr.classifier.SampleRepository;

import omr.glyph.Grades;

import omr.ui.field.LDoubleField;
import omr.ui.field.LIntegerField;
import omr.ui.util.Panel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
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
 * Class {@code ValidationPanel} handles the validation of an evaluator against the
 * selected population of s (either the whole base or the core base).
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
    /** Swing component */
    private final Panel component;

    /** Dedicated executor for validation */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** The evaluator to validate */
    private final Classifier evaluator;

    /** User progress bar to visualize the validation process */
    private final JProgressBar progressBar = new JProgressBar();

    /** Repository of known s */
    private final SampleRepository repository = SampleRepository.getInstance();

    /** User interface that handles s selection */
    private final SelectionPanel selectionPanel;

    /** User interface that handles evaluator training */
    private final TrainingPanel trainingPanel;

    /** User action to validate the evaluator against whole or core base */
    private final ValidateAction validateAction = new ValidateAction();

    /** Display percentage of s correctly recognized */
    private final LDoubleField pcValue = new LDoubleField(
            false,
            "% OK",
            "Percentage of recognized s",
            " %5.2f%%");

    /** Display number of s correctly recognized */
    private final LIntegerField positiveValue = new LIntegerField(
            false,
            "Glyphs OK",
            "Number of s correctly recognized");

    /** Display number of s mistaken with some other shape */
    private final LIntegerField falsePositiveValue = new LIntegerField(
            false,
            "False Pos.",
            "Number of s incorrectly recognized");

    /** Collection of names leading to false positives */
    private final List<String> falsePositives = new ArrayList<String>();

    /** User action to investigate on false positives */
    private final FalsePositiveAction falsePositiveAction = new FalsePositiveAction();

    /** Display number of s not recognized */
    private final LIntegerField negativeValue = new LIntegerField(
            false,
            "Negative",
            "Number of s not recognized");

    /** Collection of names not recognized (negatives) */
    private final List<String> negatives = new ArrayList<String>();

    /** User action to investigate on negatives */
    private final NegativeAction negativeAction = new NegativeAction();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ValidationPanel object.
     *
     * @param task           the current training activity
     * @param standardWidth  standard width for fields & buttons
     * @param evaluator      the evaluator to validate
     * @param selectionPanel user panel for selection
     * @param trainingPanel  user panel for evaluator training
     */
    public ValidationPanel (Trainer.Task task,
                            String standardWidth,
                            Classifier evaluator,
                            SelectionPanel selectionPanel,
                            TrainingPanel trainingPanel)
    {
        this.evaluator = evaluator;
        this.selectionPanel = selectionPanel;
        this.trainingPanel = trainingPanel;
        task.addObserver(this);

        component = new Panel();
        component.setNoInsets();

        defineLayout(standardWidth);
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
        negativeAction.setEnabled(!negatives.isEmpty());
        falsePositiveAction.setEnabled(!falsePositives.isEmpty());
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout (String standardWidth)
    {
        /** Common JGoogies constraints for this class and its subclass if any */
        CellConstraints cst = new CellConstraints();

        /** Common JGoogies builder for this class and its subclass if any */
        FormLayout layout = Panel.makeFormLayout(3, 4, "", standardWidth, standardWidth);
        PanelBuilder builder = new PanelBuilder(layout, component);

        // Validation title & progress bar
        int r = 1;
        builder.addSeparator("Validation", cst.xyw(1, r, 7));
        builder.add(progressBar, cst.xyw(9, r, 7));

        r += 2; // ----------------------------

        builder.add(positiveValue.getLabel(), cst.xy(5, r));
        builder.add(positiveValue.getField(), cst.xy(7, r));
        builder.add(negativeValue.getLabel(), cst.xy(9, r));
        builder.add(negativeValue.getField(), cst.xy(11, r));
        builder.add(falsePositiveValue.getLabel(), cst.xy(13, r));
        builder.add(falsePositiveValue.getField(), cst.xy(15, r));

        r += 2; // ----------------------------

        JButton validateButton = new JButton(validateAction);
        validateButton.setToolTipText("Validate the evaluator on current base of s");

        JButton negativeButton = new JButton(negativeAction);
        negativeButton.setToolTipText("Display the impacted s for verification");

        JButton falsePositiveButton = new JButton(falsePositiveAction);
        falsePositiveButton.setToolTipText("Display the impacted s for verification");

        builder.add(validateButton, cst.xy(3, r));
        builder.add(pcValue.getLabel(), cst.xy(5, r));
        builder.add(pcValue.getField(), cst.xy(7, r));
        builder.add(negativeButton, cst.xy(11, r));
        builder.add(falsePositiveButton, cst.xy(15, r));
    }

    //---------------//
    // runValidation //
    //---------------//
    private void runValidation ()
    {
        logger.info(
                "Validating {} evaluator on {} base ...",
                evaluator.getName(),
                trainingPanel.useWhole() ? "whole" : "core");

        // Empty the display
        positiveValue.setText("");
        pcValue.setText("");
        negativeValue.setText("");
        falsePositiveValue.setText("");
        negativeAction.setEnabled(false);
        falsePositiveAction.setEnabled(false);

        negatives.clear();
        falsePositives.clear();

        int positives = 0;
        Collection<String> gNames = selectionPanel.getBase(trainingPanel.useWhole());

        progressBar.setValue(0);
        progressBar.setMaximum(gNames.size());

        int index = 0;

        for (String gName : gNames) {
            index++;

            Sample sample = repository.getSample(gName, selectionPanel);

            if (sample != null) {
                Evaluation[] evals = evaluator.evaluate(
                        sample,
                        null,
                        1,
                        Grades.validationMinGrade,
                        EnumSet.of(ALLOWED),
                        null);

                if (evals.length == 0) {
                    negatives.add(gName);
                    System.out.printf("%-35s not recognized%n", gName);
                } else if (evals[0].shape.getPhysicalShape() == sample.getShape().getPhysicalShape()) {
                    positives++;
                } else {
                    falsePositives.add(gName);
                    System.out.printf(
                            "%-35s mistaken for %s%n",
                            gName,
                            evals[0].shape.getPhysicalShape());
                }
            }

            // Update progress bar
            progressBar.setValue(index);
        }

        int total = gNames.size();
        double pc = ((double) positives * 100) / (double) total;
        String pcStr = String.format(" %5.2f%%", pc);
        logger.info("{}Evaluator. Ratio={} : {}/{}", evaluator.getName(), pcStr, positives, total);
        positiveValue.setValue(positives);
        pcValue.setValue(pc);
        negativeValue.setValue(negatives.size());
        falsePositiveValue.setValue(falsePositives.size());
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
            SampleVerifier.getInstance().verify(falsePositives);
            SampleVerifier.getInstance().setVisible(true);
        }
    }

    //----------------//
    // NegativeAction //
    //----------------//
    private class NegativeAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public NegativeAction ()
        {
            super("Verify");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            SampleVerifier.getInstance().verify(negatives);
            SampleVerifier.getInstance().setVisible(true);
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
            super("Validate");
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
                            runValidation();
                            negativeAction.setEnabled(negatives.size() > 0);
                            falsePositiveAction.setEnabled(!falsePositives.isEmpty());
                            setEnabled(true);
                        }
                    });
        }
    }
}
