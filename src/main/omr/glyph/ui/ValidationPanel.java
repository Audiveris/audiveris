//----------------------------------------------------------------------------//
//                                                                            //
//                       V a l i d a t i o n P a n e l                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Evaluator;
import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.ui.field.LDoubleField;
import omr.ui.field.LIntegerField;
import omr.ui.util.Panel;

import omr.util.Implement;
import omr.util.Logger;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;

/**
 * Class <code>ValidationPanel</code> handles the validation of an evaluator
 * against the selected population of glyphs (either the whole base or the core
 * base)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ValidationPanel
    extends Panel
    implements Observer
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger    logger = Logger.getLogger(
        ValidationPanel.class);

    //~ Instance fields --------------------------------------------------------

    /** Dedicated executor for validation */
    protected ExecutorService executor = Executors.newSingleThreadExecutor();

    /** The evaluator to validate */
    private final Evaluator evaluator;

    /** User progress bar to visualize the validation process */
    protected JProgressBar progressBar = new JProgressBar();

    /** Repository of known glyphs */
    private final GlyphRepository repository = GlyphRepository.getInstance();

    /** User interface that handles glyphs selection */
    private final SelectionPanel selectionPanel;

    /** User interface that handles evaluator training*/
    private final TrainingPanel trainingPanel;

    /** User action to validate the evaluator against whole or core base */
    protected ValidateAction validateAction = new ValidateAction();

    /** Display percentage of glyphs correctly recognized */
    protected LDoubleField pcValue = new LDoubleField(
        false,
        "% OK",
        "Percentage of recognized glyphs");

    /** Display number of glyphs correctly recognized */
    protected LIntegerField positiveValue = new LIntegerField(
        false,
        "Glyphs OK",
        "Number of glyphs correctly recognized");

    /** Display number of glyphs mistaken with some other shape */
    protected LIntegerField falsePositiveValue = new LIntegerField(
        false,
        "False Pos.",
        "Number of glyphs incorrectly recognized");

    /** Collection of glyph names leading to false positives */
    protected List<String> falsePositives = new ArrayList<String>();

    /** User action to investigate on false positives */
    protected FalsePositiveAction falsePositiveAction = new FalsePositiveAction();

    /** Display number of glyphs not recognized */
    protected LIntegerField negativeValue = new LIntegerField(
        false,
        "Negative",
        "Number of glyphs not recognized");

    /** Collection of glyph names not recognized (negatives) */
    protected List<String> negatives = new ArrayList<String>();

    /** User action to investigate on negatives */
    protected NegativeAction negativeAction = new NegativeAction();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ValidationPanel object.
     *
     * @param task the current training activity
     * @param standardWidth standard width for fields & buttons
     * @param evaluator the evaluator to validate
     * @param selectionPanel user panel for glyph selection
     * @param trainingPanel user panel for evaluator training
     */
    public ValidationPanel (GlyphTrainer.Task task,
                            String            standardWidth,
                            Evaluator         evaluator,
                            SelectionPanel    selectionPanel,
                            TrainingPanel     trainingPanel)
    {
        this.evaluator = evaluator;
        this.selectionPanel = selectionPanel;
        this.trainingPanel = trainingPanel;
        task.addObserver(this);

        defineLayout(standardWidth);
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // update //
    //--------//
    @Implement(Observer.class)
    public void update (Observable obs,
                        Object     unused)
    {
        negativeAction.setEnabled(negatives.size() > 0);
        falsePositiveAction.setEnabled(falsePositives.size() > 0);
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout (String standardWidth)
    {
        /** Common JGoogies constraints for this class and its subclass if any */
        CellConstraints cst = new CellConstraints();

        /** Common JGoogies builder for this class and its subclass if any */
        FormLayout layout = Panel.makeFormLayout(
            3,
            4,
            "",
            standardWidth,
            standardWidth);
        PanelBuilder               builder = new PanelBuilder(layout, this);

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
        validateButton.setToolTipText(
            "Validate the evaluator on current base of glyphs");

        JButton negativeButton = new JButton(negativeAction);
        negativeButton.setToolTipText(
            "Display the impacted glyphs for verification");

        JButton falsePositiveButton = new JButton(falsePositiveAction);
        falsePositiveButton.setToolTipText(
            "Display the impacted glyphs for verification");

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
            "Validating " + evaluator.getName() + " evaluator on " +
            (trainingPanel.useWhole() ? "whole" : "core") + " base ...");

        // Empty the display
        positiveValue.setText("");
        pcValue.setText("");
        negativeValue.setText("");
        falsePositiveValue.setText("");
        negativeAction.setEnabled(false);
        falsePositiveAction.setEnabled(false);

        negatives.clear();
        falsePositives.clear();

        int                positives = 0;
        final double       maxGrade = constants.maxGrade.getValue();
        Collection<String> gNames = selectionPanel.getBase(
            trainingPanel.useWhole());

        progressBar.setValue(0);
        progressBar.setMaximum(gNames.size());

        int index = 0;
        for (String gName : gNames) {
            index++;
            Glyph glyph = repository.getGlyph(gName);

            if (glyph != null) {
                Shape vote = evaluator.vote(glyph, maxGrade);

                if (vote == glyph.getShape()) {
                    positives++;
                } else if (vote == null) {
                    negatives.add(gName);
                    System.out.printf("%-35s: Not recognized%n", gName);
                } else {
                    falsePositives.add(gName);
                    System.out.printf("%-35s: Mistaken as %s%n", gName, vote);
                }
            }

            // Update progress bar
            progressBar.setValue(index);
        }

        int    total = gNames.size();
        double pc = ((double) positives * 100) / (double) total;
        String pcStr = String.format(" %5.2f%%", pc);
        logger.info(
            evaluator.getName() + "Evaluator. Ratio=" + pcStr + " : " +
            positives + "/" + total);
        positiveValue.setValue(positives);
        pcValue.setValue(pc, " %5.2f%%");
        negativeValue.setValue(negatives.size());
        falsePositiveValue.setValue(falsePositives.size());
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
        extends ConstantSet
    {
        Constant.Double maxGrade = new Constant.Double(
            1.2,
            "Maximum acceptance grade");

        Constants ()
        {
            initialize();
        }
    }

    //---------------------//
    // FalsePositiveAction //
    //---------------------//
    private class FalsePositiveAction
        extends AbstractAction
    {
        public FalsePositiveAction ()
        {
            super("Verify");
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            GlyphVerifier.getInstance()
                         .verify(falsePositives);
            GlyphVerifier.getInstance()
                         .setVisible(true);
        }
    }

    //----------------//
    // NegativeAction //
    //----------------//
    private class NegativeAction
        extends AbstractAction
    {
        public NegativeAction ()
        {
            super("Verify");
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            GlyphVerifier.getInstance()
                         .verify(negatives);
            GlyphVerifier.getInstance()
                         .setVisible(true);
        }
    }

    //----------------//
    // ValidateAction //
    //----------------//
    private class ValidateAction
        extends AbstractAction
    {
        public ValidateAction ()
        {
            super("Validate");
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            executor.execute(
                new Runnable() {
                        public void run ()
                        {
                            setEnabled(false);
                            runValidation();
                            negativeAction.setEnabled(negatives.size() > 0);
                            falsePositiveAction.setEnabled(
                                falsePositives.size() > 0);
                            setEnabled(true);
                        }
                    });
        }
    }
}
