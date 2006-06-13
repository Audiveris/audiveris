//-----------------------------------------------------------------------//
//                                                                       //
//                     E v a l u a t o r s P a n e l                     //
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
import omr.glyph.GlyphNetwork;
import omr.glyph.Shape;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.ui.util.Panel;
import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Class <code>EvaluatorsPanel</code> is dedicated to the display of
 * evaluation results performed by a pool of evaluators
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class EvaluatorsPanel
{
    private static Logger logger = Logger.getLogger(EvaluatorsPanel.class);

    // Max number of buttons in the shape selector
    private static final int EVAL_NB = 3;

    private static final Color EVAL_GOOD_COLOR = new Color(100, 150, 0);
    private static final Color EVAL_SOSO_COLOR = new Color(255, 100, 150);

    //~ Instance variables ------------------------------------------------

    // Concrete panel
    private Panel component;

    // Related sheet & GlyphPane (if any)
    private final Sheet     sheet;
    private final GlyphPane pane;

    // Lag view (if any)
    private SymbolGlyphView view;

    // Pool of evaluators
    private final GlyphNetwork    network = GlyphNetwork.getInstance();
    private final EvaluatorPanel  networkPanel;

    //~ Constructors ------------------------------------------------------

    //-----------------//
    // EvaluatorsPanel //
    //-----------------//
    /**
     * Create a panel with one neural network evaluator
     *
     * @param sheet the related sheet, or null
     * @param pane the glyph pane, or null if this panel is just an output
     */
    public EvaluatorsPanel (Sheet     sheet,
                            GlyphPane pane)
    {
        this.sheet = sheet;
        this.pane  = pane;

        component = new Panel();
        if (pane != null) {
            component.setNoInsets();
        }

        networkPanel = new EvaluatorPanel(network);

        FormLayout layout = new FormLayout
            ("pref",
             "pref," + Panel.getPanelInterline() + "," +
             "pref");
        PanelBuilder builder = new PanelBuilder(layout, component);
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int r = 1;                  // --------------------------------
        builder.add(networkPanel,       cst.xy (1, r));
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the concrete component
     *
     * @return the created component
     */
    public JComponent getComponent()
    {
        return component;
    }

    //---------//
    // setView //
    //---------//
    /**
     * Connect the glyph view. This could not be done from the constructor
     *
     * @param view the glyph lag view
     */
    void setView (SymbolGlyphView view)
    {
        this.view = view;
    }

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
            networkPanel.selector.setEvals(null);
        } else {
            networkPanel.selector.setEvals(network.getEvaluations(glyph));
        }
    }

    //~ Classes -----------------------------------------------------------

    //----------------//
    // EvaluatorPanel //
    //----------------//
    /**
     * Class <code>EvaluatorPanel</code> is dedicated to one evaluator
     */
    private class EvaluatorPanel
        extends Panel
    {
        // The evaluator this display is related to
        private final Evaluator evaluator;

        private final JButton testButton = new JButton(new TestAction());

        // Pane for detailed info display about the glyph evaluation
        private final Selector selector = new Selector();

        // Numeric result of whole sheet test
        private final JLabel testPercent = new JLabel("0%", SwingConstants.CENTER);
        private final JLabel testResult = new JLabel("", SwingConstants.CENTER);

        //~ Constructors --------------------------------------------------

        /**
         * Create a dedicated evaluator panel
         *
         * @param evaluator the related evaluator
         */
        //----------------//
        // EvaluatorPanel //
        //----------------//
        public EvaluatorPanel (Evaluator evaluator)
        {
            this.evaluator = evaluator;

            setNoInsets();

            defineLayout();
        }

        //~ Methods -------------------------------------------------------

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
            //layout.setRowGroups(new int[][]{{1, 3, 4, 5 }});

            PanelBuilder builder = new PanelBuilder(layout, this);
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
                if (pane != null) {
                    builder.add(selector.buttons[i].button, cst.xyw(3,  r, 5));
                } else {
                    builder.add(selector.buttons[i].field,  cst.xyw(3,  r, 5));
                }
            }
        }

        //~ Classes -------------------------------------------------------

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
             * @param evals the ordered list of evaluations from best to
             * worst
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
            JTextField field;
            JButton    button;

            // The related grade
            JLabel grade = new JLabel("", SwingConstants.RIGHT);

            public EvalButton()
            {
                grade.setToolTipText("Grade of the evaluation");
                if (pane != null) {
                    button = new JButton();
                    button.addActionListener(this);
                    button.setToolTipText("Assignable shape");
                    button.setHorizontalAlignment(SwingConstants.LEFT);
                } else {
                    field = new JTextField();
                    field.setHorizontalAlignment(JTextField.CENTER);
                    field.setEditable(false);
                    field.setToolTipText("Evaluated shape");
                }
            }

            public void setEval (Evaluation eval)
            {
                JComponent comp;
                if (pane != null) {
                    comp = button;
                } else {
                    comp = field;
                }
                if (eval != null) {
                    if (pane != null) {
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
                if (pane != null) {
                    pane.assignShape(Shape.valueOf(button.getText()),
                                     /* asGuessed => */ false,
                                     /* compound => */ false);
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
}
