//-----------------------------------------------------------------------//
//                                                                       //
//                     E v a l u a t i o n B o a r d                     //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph.ui;

import omr.glyph.Evaluation;
import omr.glyph.Evaluator;
import omr.glyph.Glyph;
import omr.glyph.GlyphInspector;
import omr.glyph.GlyphModel;
import omr.glyph.GlyphNetwork;
import omr.glyph.Shape;
import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.ui.Board;
import omr.ui.util.Panel;
import omr.util.Logger;

import static omr.selection.SelectionTag.*;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Collections;
import javax.swing.*;

/**
 * Class <code>EvaluationBoard</code> is a board dedicated to the display
 * of evaluation results performed by an evaluator.
 *
 * <p>By pressing one of the result buttons, the user can force the
 * assignment of the evaluated glyph.
 *
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>*_GLYPH
 * </ul>
 *
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>*_GLYPH (flagged with GLYPH_INIT) TO BE CONFIRMED !!!
 * </ul>
 * </dl>
 *
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class EvaluationBoard
    extends Board
{
    private Logger logger = Logger.getLogger(EvaluationBoard.class);

    // Max number of buttons in the shape selector
    private static final int EVAL_NB = 3;

    private static final Color EVAL_GOOD_COLOR = new Color(100, 150, 0);
    private static final Color EVAL_SOSO_COLOR = new Color(255, 100, 150);

    //~ Instance variables ------------------------------------------------

    // Related sheet & GlyphModel
    private final Sheet      sheet;
    private final GlyphModel glyphModel;

    // Should we use buttons (or plain output fields) ?
    private final boolean useButtons;

    // Lag view (if any)
    private GlyphLagView view;

    // The evaluator this display is related to
    private final Evaluator evaluator = GlyphNetwork.getInstance();

    private final JButton testButton = new JButton(new TestAction());

    // Pane for detailed info display about the glyph evaluation
    private final Selector selector;

    // Numeric result of whole sheet test
    private final JLabel testPercent = new JLabel("0%", SwingConstants.CENTER);
    private final JLabel testResult = new JLabel("", SwingConstants.CENTER);

    //~ Constructors ------------------------------------------------------

    //-----------------//
    // EvaluationBoard //
    //-----------------//
    /**
     * Create a board with one neural network evaluator
     *
     * @param sheet the related sheet, or null
     * @param glyphModel the related glyph model
     * @param view the related symbol glyph view
     * @param inputSelection the Glyph input to evaluate
     */
    public EvaluationBoard (Sheet        sheet,
                            GlyphModel   glyphModel,
                            boolean      useButtons,
                            GlyphLagView view,
                            Selection    inputSelection)
    {
        super(Board.Tag.CUSTOM, "EvaluationBoard");

        // Useful ??? TBD
        this.sheet = sheet;
        this.glyphModel = glyphModel;
        this.useButtons = useButtons;
        this.view = view;

        selector = new Selector();
        defineLayout();
        setInputSelectionList(Collections.singletonList(inputSelection));
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // evaluate //
    //----------//
    /**
     * Evaluate the glyph at hand, and display the result from each
     * evaluator in its dedicated area
     *
     * @param glyph the glyph at hand
     */
    public void evaluate (Glyph glyph)
    {
        if (glyph == null || glyph.getShape() == Shape.COMBINING_STEM) {
            // Blank the output
            selector.setEvals(null);
        } else {
            selector.setEvals(evaluator.getEvaluations(glyph));
        }
    }

    //--------//
    // update //
    //--------//
    /**
     * Call-back triggered when Glyph Selection has been modified
     *
     * @param selection the (Glyph) Selection
     * @param hint potential notification hint
     */
    @Override
    public void update (Selection selection,
                        SelectionHint hint)
    {
        switch (selection.getTag()) {
        case VERTICAL_GLYPH :
            Glyph glyph = (Glyph) selection.getEntity();

            // Make sure the glyph interline has been set
            if (glyph != null &&
                    glyph.getInterline() == 0) {
                glyph.setInterline(sheet.getScale().interline());
            }

            evaluate(glyph);
            break;

        default :
            logger.severe("Unexpected selection event from " + selection);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout()
    {
        final String buttonWidth    = Panel.getButtonWidth();
        final String fieldInterval  = Panel.getFieldInterval();
        final String fieldInterline = Panel.getFieldInterline();

        FormLayout layout = new FormLayout
            (buttonWidth + "," + fieldInterval + "," +
             buttonWidth + "," + fieldInterval + "," +
             buttonWidth + "," + fieldInterval + "," +
             buttonWidth,
             "pref," + fieldInterline + "," +
             "pref," +
             "pref," +
             "pref");

        // Uncomment following line to have fixed sized rows, whether
        // they are filled or not
        ///layout.setRowGroups(new int[][]{{1, 3, 4, 5 }});

        PanelBuilder builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int r = 1;                  // --------------------------------
        if (sheet != null) {
            builder.addSeparator(evaluator.getName(), cst.xyw(1,  r, 1));
            builder.add(testButton,                   cst.xy (3,  r));
            builder.add(testPercent,                  cst.xy (5,  r));
            builder.add(testResult,                   cst.xy (7,  r));
        } else {
            builder.addSeparator(evaluator.getName(), cst.xyw(1,  r, 7));
        }

        for (int i = 0 ; i < EVAL_NB; i++) {
            r = i + 3;              // --------------------------------
            builder.add(selector.buttons[i].grade,  cst.xy (1,  r));
            if (useButtons) {
                builder.add(selector.buttons[i].button, cst.xyw(3,  r, 5));
            } else {
                builder.add(selector.buttons[i].field,  cst.xyw(3,  r, 5));
            }
        }
    }

    //~ Classes -----------------------------------------------------------

    //----------//
    // Selector //
    //----------//
    private class Selector
    {
        // A collection of EvalButton's
        EvalButton[] buttons;

        public Selector()
        {
            buttons = new EvalButton[EVAL_NB];
            for (int i = 0; i < buttons.length; i++) {
                buttons[i] = new EvalButton();
            }
            setEvals(null);
        }

        //----------//
        // setEvals //
        //----------//
        /**
         * Display the evaluations with some text highlighting. Only
         * relevant evaluations are displayed (distance less than an
         * evaluator-dependent threshold, and less than x times (also
         * evaluator-dependent) the best (first) eval whichever comes
         * first)
         *
         * @param evals the ordered list of evaluations from best to worst
         */
        public void setEvals (Evaluation[] evals)
        {
            // Special case to empty the selector
            if (evals == null) {
                for (EvalButton evalButton : buttons) {
                    evalButton.setEval(null);
                }
                return;
            }

            double maxDist = evaluator.getMaxDistance();
            double maxRatio = evaluator.getMaxDistanceRatio();
            double best = -1;       // i.e. Not set

            int iBound = Math.min(EVAL_NB, evals.length);
            int i;
            for (i = 0; i < iBound; i++) {
                Evaluation eval = evals[i];
                if (eval.grade > maxDist) {
                    break;
                }
                if (best < 0) {
                    best = eval.grade;
                }
                if (eval.grade > best * maxRatio) {
                    break;
                }
                buttons[i].setEval(eval);
            }
            // Zero the remaining buttons
            for (; i < EVAL_NB; i++) {
                buttons[i].setEval(null);
            }
        }
    }

    //------------//
    // EvalButton //
    //------------//
    private class EvalButton
        implements ActionListener
    {
        // A shape button or text field. Only one of them will be
        // allocated and used
        final JTextField field;
        final JButton    button;

        // The related grade
        JLabel grade = new JLabel("", SwingConstants.RIGHT);

        public EvalButton()
        {
            grade.setToolTipText("Grade of the evaluation");
            if (useButtons) {
                button = new JButton();
                button.addActionListener(this);
                button.setToolTipText("Assignable shape");
                button.setHorizontalAlignment(SwingConstants.LEFT);
                field = null;
            } else {
                field = new JTextField();
                field.setHorizontalAlignment(JTextField.CENTER);
                field.setEditable(false);
                field.setToolTipText("Evaluated shape");
                button = null;
            }
        }

        public void setEval (Evaluation eval)
        {
            JComponent comp;
            if (useButtons) {
                comp = button;
            } else {
                comp = field;
            }
            if (eval != null) {
                if (useButtons) {
                    button.setText(eval.shape.toString());
                    button.setIcon(eval.shape.getIcon());
                } else {
                    field.setText(eval.shape.toString());
                }

                comp.setVisible(true);
                if (eval.grade <= GlyphInspector.getSymbolMaxGrade()) {
                    comp.setForeground(EVAL_GOOD_COLOR);
                } else {
                    comp.setForeground(EVAL_SOSO_COLOR);
                }

                grade.setVisible(true);
                grade.setText(String.format("%.3f", eval.grade));
            } else {
                grade.setVisible(false);
                comp.setVisible(false);
            }
        }

        // Triggered by button
        public void actionPerformed(ActionEvent e)
        {
            // Assign current glyph with selected shape
            if (glyphModel != null) {
                glyphModel.assignGlyphShape
                    ((Glyph) inputSelectionList.get(0).getEntity(),
                     Shape.valueOf(button.getText()));
            }
        }
    }

    //------------//
    // TestAction //
    //------------//
    /**
     * Class <code>TestAction</code> uses the evaluator on all known
     * glyphs within the sheet, to check if they can be correctly
     * recognized. This does not modify the current glyph assignments.
     */
    private class TestAction
        extends AbstractAction
    {
        //~ Constructors ----------------------------------------------

        public TestAction ()
        {
            super("Global");
        }

        //~ Methods ---------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            int ok = 0;
            int total = 0;
            final double maxGrade = GlyphInspector.getSymbolMaxGrade();
            for (SystemInfo system : sheet.getSystems()) {
                for (Glyph glyph : system.getGlyphs()) {
                    if (glyph.isKnown() &&
                        glyph.getShape() != Shape.COMBINING_STEM) {
                        total++;
                        Shape guess = evaluator.vote(glyph, maxGrade);
                        if (glyph.getShape() == guess) {
                            ok++;
                            view.colorizeGlyph(glyph,
                                               Shape.okColor);
                        } else {
                            view.colorizeGlyph(glyph,
                                               Shape.missedColor);
                        }
                    }
                }
            }
            String pc = String.format(" %5.2f%%",
                                      (double) ok * 100 /
                                      (double) total);
            testPercent.setText(pc);
            testResult.setText(ok + "/" + total);

            // Almost all glyphs may have been modified, so...
            view.repaint();
        }
    }
}
