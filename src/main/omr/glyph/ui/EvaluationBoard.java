//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 E v a l u a t i o n B o a r d                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.classifier.Classifier;
import omr.classifier.Evaluation;
import omr.classifier.GlyphClassifier;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.selection.EntityListEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import omr.selection.UserEvent;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.SystemManager;

import omr.ui.Board;
import omr.ui.Colors;
import omr.ui.util.Panel;

import omr.util.Navigable;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Class {@code EvaluationBoard} is a board dedicated to the display of evaluation
 * results performed by an evaluator.
 * It also provides through buttons the ability to manually assign a shape to the glyph at hand.
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 */
public class EvaluationBoard
        extends Board
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(EvaluationBoard.class);

    /** Events this board is interested in */
    private static final Class<?>[] eventsRead = new Class<?>[]{EntityListEvent.class};

    /** Color for well recognized glyphs */
    private static final Color EVAL_GOOD_COLOR = new Color(100, 200, 100);

    /** Color for hardly recognized glyphs */
    private static final Color EVAL_SOSO_COLOR = new Color(150, 150, 150);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The evaluator this display is related to */
    private final Classifier evaluator = GlyphClassifier.getInstance();

    /** Related glyphs controller */
    private final GlyphsController glyphsController;

    /** Related sheet */
    @Navigable(false)
    private final Sheet sheet;

    /** Pane for detailed info display about the glyph evaluation */
    private final Selector selector;

    /** Do we use GlyphChecker annotations? */
    private boolean useAnnotations;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a simplified passive evaluation board with one neural
     * network evaluator.
     *
     * @param glyphModel the related glyph model
     */
    public EvaluationBoard (GlyphsController glyphModel,
                            boolean expanded)
    {
        this(null, glyphModel, expanded);
        useAnnotations = false;
    }

    //-----------------//
    // EvaluationBoard //
    //-----------------//
    /**
     * Create an evaluation board with one neural network evaluator and the ability to
     * force glyph shape.
     *
     * @param glyphController the related glyph controller
     * @param sheet           the related sheet, or null
     */
    public EvaluationBoard (Sheet sheet,
                            GlyphsController glyphController,
                            boolean expanded)
    {
        super(
                Board.EVAL,
                glyphController.getNest().getEntityService(),
                eventsRead,
                false,
                false,
                false,
                expanded);

        this.glyphsController = glyphController;
        this.sheet = sheet;

        selector = new Selector();
        defineLayout();
        useAnnotations = false; //true;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // evaluate //
    //----------//
    /**
     * Evaluate the glyph at hand, and display the result in the evaluator dedicated area.
     *
     * @param glyph the glyph at hand
     */
    public void evaluate (Glyph glyph)
    {
        if (glyph == null) {
            // Blank the output
            selector.setEvals(null, null);
        } else if (evaluator != null) {
            SystemManager systemManager = sheet.getSystemManager();

            for (SystemInfo system : systemManager.getSystemsOf(glyph)) {
                selector.setEvals(
                        evaluator.evaluate(
                                glyph,
                                system,
                                selector.evalCount(),
                                constants.minGrade.getValue(),
                                useAnnotations ? EnumSet.of(Classifier.Condition.CHECKED)
                                        : Classifier.NO_CONDITIONS,
                                null),
                        glyph);

                return;
            }
        }
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when Glyph Selection has been modified.
     *
     * @param event the (Glyph) Selection
     */
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            // Don't evaluate Added glyph, since this would hide Compound evaluation
            if (event.hint == SelectionHint.LOCATION_ADD) {
                return;
            }

            if (event instanceof EntityListEvent) {
                EntityListEvent<Glyph> listEvent = (EntityListEvent<Glyph>) event;
                Glyph glyph = listEvent.getEntity();

                if (glyph != null) {
                    evaluate(glyph);
                }
            }
        } catch (Exception ex) {
            logger.warn(sheet.getLogPrefix() + getClass().getName() + " output error", ex);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        String colSpec = Panel.makeColumns(3);
        FormLayout layout = new FormLayout(colSpec, "");

        int visibleButtons = Math.min(
                constants.visibleButtons.getValue(),
                selector.buttons.size());

        for (int i = 0; i < visibleButtons; i++) {
            layout.appendRow(FormSpecs.PREF_ROWSPEC);
        }

        ///SystemUtils.IS_OS_WINDOWS
        // Uncomment following line to have fixed sized rows, whether they are filled or not
        ///layout.setRowGroups(new int[][]{{1, 3, 4, 5 }});
        PanelBuilder builder = new PanelBuilder(layout, getBody());

        ///builder.setDefaultDialogBorder();
        CellConstraints cst = new CellConstraints();

        for (int i = 0; i < visibleButtons; i++) {
            int r = i + 1; // --------------------------------
            EvalButton evb = selector.buttons.get(i);
            builder.add(evb.grade, cst.xy(5, r));
            builder.add((sheet != null) ? evb.button : evb.field, cst.xyw(7, r, 5));
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Evaluation.Grade minGrade = new Evaluation.Grade(
                0.0,
                "Threshold on displayable grade");

        private final Constant.Integer visibleButtons = new Constant.Integer(
                "buttons",
                5,
                "Max number of buttons in the shape selector");
    }

    //------------//
    // EvalButton //
    //------------//
    private class EvalButton
            implements ActionListener
    {
        //~ Instance fields ------------------------------------------------------------------------

        // Shape button or text field. Only one of them will be created and used
        final JButton button;

        final JLabel field;

        // The related grade
        JLabel grade = new JLabel("", SwingConstants.RIGHT);

        //~ Constructors ---------------------------------------------------------------------------
        public EvalButton ()
        {
            grade.setToolTipText("(Logarithmic) Grade of the evaluation");

            if (sheet != null) {
                button = new JButton();
                button.addActionListener(this);
                button.setToolTipText("Assignable shape");
                button.setHorizontalAlignment(SwingConstants.LEFT);
                field = null;
            } else {
                field = new JLabel();
                field.setHorizontalAlignment(JTextField.CENTER);
                field.setToolTipText("Evaluated shape");
                field.setHorizontalAlignment(SwingConstants.LEFT);
                button = null;
            }
        }

        //~ Methods --------------------------------------------------------------------------------
        // Triggered by button
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Assign current glyph with selected shape
            if (glyphsController != null) {
                Glyph glyph = glyphsController.getNest().getSelectedGlyph();

                if (glyph != null) {
                    String str = button.getText();
                    Shape shape = Shape.valueOf(str);

                    // Actually assign the shape
                    glyphsController.asyncAssignGlyphs(Arrays.asList(glyph), shape, false);
                }
            }
        }

        public void setEval (Evaluation eval,
                             boolean barred,
                             boolean enabled)
        {
            JComponent comp;

            if (sheet != null) {
                comp = button;
            } else {
                comp = field;
            }

            if (eval != null) {
                Evaluation.Failure failure = eval.failure;
                String text = eval.shape.toString();
                String tip = (failure != null) ? failure.toString() : null;

                if (sheet != null) {
                    button.setEnabled(enabled);

                    if (barred) {
                        button.setBackground(Colors.EVALUATION_BARRED);
                    } else {
                        button.setBackground(null);
                    }

                    button.setText(text);
                    button.setToolTipText(tip);
                    button.setIcon(eval.shape.getDecoratedSymbol());
                } else {
                    if (barred) {
                        field.setBackground(Colors.EVALUATION_BARRED);
                    } else {
                        field.setBackground(null);
                    }

                    field.setText(text);
                    field.setToolTipText(tip);
                    field.setIcon(eval.shape.getDecoratedSymbol());
                }

                comp.setVisible(true);

                if (failure == null) {
                    comp.setForeground(EVAL_GOOD_COLOR);
                } else {
                    comp.setForeground(EVAL_SOSO_COLOR);
                }

                grade.setVisible(true);
                grade.setText(String.format("%.2f", Math.log(eval.grade)));
            } else {
                grade.setVisible(false);
                comp.setVisible(false);
            }
        }
    }

    //----------//
    // Selector //
    //----------//
    private class Selector
    {
        //~ Instance fields ------------------------------------------------------------------------

        // A collection of EvalButton's
        final List<EvalButton> buttons = new ArrayList<EvalButton>();

        //~ Constructors ---------------------------------------------------------------------------
        public Selector ()
        {
            for (int i = 0; i < evalCount(); i++) {
                buttons.add(new EvalButton());
            }

            setEvals(null, null);
        }

        //~ Methods --------------------------------------------------------------------------------
        //-----------//
        // evalCount //
        //-----------//
        /**
         * Report the number of displayed evaluations
         *
         * @return the number of eval buttons
         */
        public final int evalCount ()
        {
            return constants.visibleButtons.getValue();
        }

        //----------//
        // setEvals //
        //----------//
        /**
         * Display the evaluations with some text highlighting.
         * Only first evalCount evaluations are displayed.
         *
         * @param evals top evaluations sorted from best to worst
         * @param glyph evaluated glyph, to check forbidden shapes if any
         */
        public final void setEvals (Evaluation[] evals,
                                    Glyph glyph)
        {
            // Special case to empty the selector
            if (evals == null) {
                for (EvalButton evalButton : buttons) {
                    evalButton.setEval(null, false, false);
                }

                return;
            }

            boolean enabled = !glyph.isVirtual();
            double minGrade = constants.minGrade.getValue();
            int iBound = Math.min(evalCount(), positiveEvals(evals));
            int i;

            for (i = 0; i < iBound; i++) {
                Evaluation eval = evals[i];

                // Limitation on shape relevance
                if (eval.grade < minGrade) {
                    break;
                }

                // Active buttons
                buttons.get(i).setEval(eval, false, enabled);
            }

            // Zero the remaining buttons
            for (; i < evalCount(); i++) {
                buttons.get(i).setEval(null, false, false);
            }
        }

        /**
         * Return the number of evaluations with grade strictly positive
         *
         * @param evals the evaluations sorted from best to worst
         * @return the number of evaluations with grade > 0
         */
        private int positiveEvals (Evaluation[] evals)
        {
            for (int i = 0; i < evals.length; i++) {
                if (evals[i].grade <= 0) {
                    return i;
                }
            }

            return evals.length;
        }
    }
}
