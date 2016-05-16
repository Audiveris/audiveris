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
import omr.classifier.Evaluation;
import omr.classifier.Sample;
import omr.classifier.SampleRepository;

import omr.glyph.Grades;

import omr.ui.Colors;
import omr.ui.field.LLabel;
import omr.ui.util.Panel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
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
 * Class {@code ValidationPanel} handles the validation of an classifier against the
 * selected population of samples (either the whole base or the core base).
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

    /** Dedicated executor for validation. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** The classifier to validate. */
    private final Classifier classifier;

    /** User progress bar to visualize the validation process. */
    private final JProgressBar progressBar = new JProgressBar();

    /** Repository of training samples. */
    private final SampleRepository repository = SampleRepository.getInstance();

    /** User interface that handles samples selection. */
    private final SelectionPanel selectionPanel;

    /** User action to validate the classifier against training base. */
    private final ValidateAction validateAction = new ValidateAction();

    /** Display percentage of samples correctly recognized. */
    private final LLabel pcValue = new LLabel("%:", "Percentage of samples correctly recognized");

    /** Display number of samples correctly recognized. */
    private final LLabel positiveValue = new LLabel(
            "Positives:",
            "Number of samples correctly recognized");

    /** Display number of samples mistaken with some other shape. */
    private final LLabel falsePositiveValue = new LLabel(
            "False Pos.:",
            "Number of samples incorrectly recognized");

    /** Collection of samples leading to false positives. */
    private final List<Sample> falsePositives = new ArrayList<Sample>();

    /** User action to investigate on false positives. */
    private final FalsePositiveAction falsePositiveAction = new FalsePositiveAction();

    /** Display number of samples not recognized. */
    private final LLabel falseNegativeValue = new LLabel(
            "False Neg.:",
            "Number of samples not recognized");

    /** Collection of samples not recognized (false negatives). */
    private final List<Sample> falseNegatives = new ArrayList<Sample>();

    /** User action to investigate on false negatives. */
    private final FalseNegativeAction falseNegativeAction = new FalseNegativeAction();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ValidationPanel object.
     *
     * @param task           the current training activity
     * @param standardWidth  standard width for fields & buttons
     * @param classifier     the classifier to validate
     * @param selectionPanel user panel for selection
     */
    public ValidationPanel (Trainer.Task task,
                            String standardWidth,
                            Classifier classifier,
                            SelectionPanel selectionPanel)
    {
        this.classifier = classifier;
        this.selectionPanel = selectionPanel;
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
        falseNegativeAction.setEnabled(!falseNegatives.isEmpty());
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
        builder.addSeparator("Validation", cst.xyw(3, r, 5));
        builder.add(progressBar, cst.xyw(9, r, 7));
        progressBar.setForeground(Colors.PROGRESS_BAR);

        r += 2; // ----------------------------

        JButton validateButton = new JButton(validateAction);
        validateButton.setToolTipText("Validate the classifier on current base of samples");
        builder.add(validateButton, cst.xy(3, r));

        builder.add(positiveValue.getLabel(), cst.xy(5, r));
        builder.add(positiveValue.getField(), cst.xy(7, r));
        builder.add(falseNegativeValue.getLabel(), cst.xy(9, r));
        builder.add(falseNegativeValue.getField(), cst.xy(11, r));
        builder.add(falsePositiveValue.getLabel(), cst.xy(13, r));
        builder.add(falsePositiveValue.getField(), cst.xy(15, r));

        r += 2; // ----------------------------

        JButton negativeButton = new JButton(falseNegativeAction);
        negativeButton.setToolTipText("Display the impacted samples for verification");

        JButton falsePositiveButton = new JButton(falsePositiveAction);
        falsePositiveButton.setToolTipText("Display the impacted samples for verification");

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
        logger.info("Validating {} classifier ...", classifier.getName());

        // Empty the display
        positiveValue.setText("");
        pcValue.setText("");
        falseNegativeValue.setText("");
        falsePositiveValue.setText("");
        falseNegativeAction.setEnabled(false);
        falsePositiveAction.setEnabled(false);

        falseNegatives.clear();
        falsePositives.clear();

        int positives = 0;

        if (!repository.isLoaded()) {
            repository.loadRepository(false);
        }

        final List<Sample> samples = repository.getAllSamples();

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
                falseNegatives.add(sample);
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
        String pcStr = String.format(" %5.2f%%", pc);
        logger.info("{}Evaluator. Ratio={} : {}/{}", classifier.getName(), pcStr, positives, total);
        positiveValue.setText(Integer.toString(positives));
        pcValue.setText(String.format("%.2f", pc));
        falseNegativeValue.setText(Integer.toString(falseNegatives.size()));
        falsePositiveValue.setText(Integer.toString(falsePositives.size()));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------------------//
    // FalseNegativeAction //
    //---------------------//
    private class FalseNegativeAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public FalseNegativeAction ()
        {
            super("Verify");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            SampleVerifier.getInstance().verify(falseNegatives);
            SampleVerifier.getInstance().setVisible();
        }
    }

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
                    falseNegativeAction.setEnabled(falseNegatives.size() > 0);
                    falsePositiveAction.setEnabled(!falsePositives.isEmpty());
                    setEnabled(true);
                }
            });
        }
    }
}
