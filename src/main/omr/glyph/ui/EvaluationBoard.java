//----------------------------------------------------------------------------//
//                                                                            //
//                       E v a l u a t i o n B o a r d                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphEvaluator;
import omr.glyph.Glyph;
import omr.glyph.GlyphInspector;
import omr.glyph.GlyphNetwork;
import omr.glyph.Shape;

import omr.log.Logger;

import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import omr.selection.UserEvent;

import omr.sheet.Sheet;

import omr.ui.Board;
import omr.ui.util.Panel;

import omr.util.Implement;
import static omr.util.Synchronicity.*;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;

import org.bushe.swing.event.EventSubscriber;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

/**
 * Class <code>EvaluationBoard</code> is a board dedicated to the display of
 * evaluation results performed by an evaluator.
 *
 * <p>By pressing one of the result buttons, the user can force the assignment
 * of the evaluated glyph.
 *
 * @author Herv&eacute; Bitteur and Brenton Partridge
 * @version $Id$
 */
class EvaluationBoard
    extends Board
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        EvaluationBoard.class);

    /** Events this boards is interested in */
    private static final Collection<Class<?extends UserEvent>> eventClasses;

    static {
        eventClasses = new ArrayList<Class<?extends UserEvent>>();
        eventClasses.add(GlyphEvent.class);
    }

    /** Color for well recognized glyphs */
    private static final Color EVAL_GOOD_COLOR = new Color(100, 150, 0);

    /** Color for hardly recognized glyphs */
    private static final Color EVAL_SOSO_COLOR = new Color(255, 100, 150);

    //~ Instance fields --------------------------------------------------------

    /** The evaluator this display is related to */
    private final GlyphEvaluator evaluator = GlyphNetwork.getInstance();

    /** Related glyph model */
    private final GlyphsController glyphController;

    /** Related sheet & GlyphsController */
    private final Sheet sheet;

    /** Lag view (if any) */
    private final GlyphLagView view;

    /** Pane for detailed info display about the glyph evaluation */
    private final Selector selector;

    /** Button for testing ratio of recognized glyphs */
    private JButton testButton;

    /** Numeric result of whole sheet test */
    private JLabel testPercent;

    /** Percentage result of whole sheet test */
    private JLabel testResult;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // EvaluationBoard //
    //-----------------//
    /**
     * Create a simplified passive evaluation board with one neural network
     * evaluator
     *
     * @param glyphModel the related glyph model
     */
    public EvaluationBoard (String           name,
                            GlyphsController glyphModel)
    {
        this(name, glyphModel, null, null);
    }

    //-----------------//
    // EvaluationBoard //
    //-----------------//
    /**
     * Create an evaluation board with one neural network evaluator, ability to
     * force glyph shape, and test buttons
     *
     * @param name a rather unique name for this board
     * @param glyphController the related glyph controller
     * @param sheet the related sheet, or null
     * @param view the related symbol glyph view
     */
    public EvaluationBoard (String           name,
                            GlyphsController glyphController,
                            Sheet            sheet,
                            GlyphLagView     view)
    {
        super(
            name,
            glyphController.getLag().getSelectionService(),
            eventClasses);

        this.glyphController = glyphController;
        this.sheet = sheet;
        this.view = view;

        // Buttons
        if (sheet != null) {
            testButton = new JButton(new TestAction());
            testPercent = new JLabel("0%", SwingConstants.CENTER);
            testResult = new JLabel("", SwingConstants.CENTER);
        }

        selector = new Selector();
        defineLayout();
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // evaluate //
    //----------//
    /**
     * Evaluate the glyph at hand, and display the result from each evaluator in
     * its dedicated area
     *
     * @param glyph the glyph at hand
     */
    public void evaluate (Glyph glyph)
    {
        if ((glyph == null) ||
            (glyph.getShape() == Shape.COMBINING_STEM) ||
            glyph.isBar()) {
            // Blank the output
            selector.setEvals(null, null);
        } else {
            selector.setEvals(evaluator.getAllEvaluations(glyph), glyph);
        }
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when Glyph Selection has been modified
     *
     * @param event the (Glyph) Selection
     */
    @Implement(EventSubscriber.class)
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            //        logger.info(
            //            "EvaluationBoard/" + getClass().getSimpleName() + " " + getName() +
            //            " " + event);

            // Don't evaluate Added glyph, since this would hide Compound evaluation
            if (event.hint == SelectionHint.LOCATION_ADD) {
                return;
            }

            if (event instanceof GlyphEvent) {
                GlyphEvent glyphEvent = (GlyphEvent) event;
                Glyph      glyph = glyphEvent.getData();
                evaluate(glyph);
            }
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        final String buttonWidth = Panel.getButtonWidth();
        final String fieldInterval = Panel.getFieldInterval();
        final String fieldInterline = Panel.getFieldInterline();

        FormLayout   layout = new FormLayout(
            buttonWidth + "," + fieldInterval + "," + buttonWidth + "," +
            fieldInterval + "," + buttonWidth + "," + fieldInterval + "," +
            buttonWidth,
            "pref," + fieldInterline);

        int          visibleButtons = Math.min(
            constants.visibleButtons.getValue(),
            selector.buttons.size());

        for (int i = 0; i < visibleButtons; i++) {
            layout.appendRow(FormFactory.PREF_ROWSPEC);
        }

        // Uncomment following line to have fixed sized rows, whether
        // they are filled or not
        ///layout.setRowGroups(new int[][]{{1, 3, 4, 5 }});
        PanelBuilder builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int             r = 1; // --------------------------------

        if (sheet != null) {
            builder.addSeparator(evaluator.getName(), cst.xyw(1, r, 1));
            builder.add(testButton, cst.xy(3, r));
            builder.add(testPercent, cst.xy(5, r));
            builder.add(testResult, cst.xy(7, r));
        } else {
            builder.addSeparator(evaluator.getName(), cst.xyw(1, r, 7));
        }

        for (int i = 0; i < visibleButtons; i++) {
            r = i + 3; // --------------------------------
            builder.add(selector.buttons.get(i).grade, cst.xy(1, r));

            if (sheet != null) {
                builder.add(selector.buttons.get(i).button, cst.xyw(3, r, 5));
            } else {
                builder.add(selector.buttons.get(i).field, cst.xyw(3, r, 5));
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Evaluation.Doubt maxDoubt = new Evaluation.Doubt(
            100000.0,
            "Threshold on displayable doubt");
        Constant.Integer visibleButtons = new Constant.Integer(
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
        //~ Instance fields ----------------------------------------------------

        // Shape button or text field. Only one of them will be created and used
        final JButton button;
        final JLabel  field;

        // The related doubt
        JLabel grade = new JLabel("", SwingConstants.RIGHT);

        //~ Constructors -------------------------------------------------------

        public EvalButton ()
        {
            grade.setToolTipText("Doubt of the evaluation");

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

        //~ Methods ------------------------------------------------------------

        public void setEval (Evaluation eval,
                             boolean    barred)
        {
            JComponent comp;

            if (sheet != null) {
                comp = button;
            } else {
                comp = field;
            }

            if (eval != null) {
                if (sheet != null) {
                    if (barred) {
                        button.setBackground(Color.PINK);
                    } else {
                        button.setBackground(null);
                    }

                    button.setText(eval.shape.toString());
                    button.setIcon(eval.shape.getIcon());
                } else {
                    if (barred) {
                        field.setBackground(Color.PINK);
                    } else {
                        field.setBackground(null);
                    }

                    field.setText(eval.shape.toString());
                    field.setIcon(eval.shape.getIcon());
                }

                comp.setVisible(true);

                if (eval.doubt <= GlyphInspector.getSymbolMaxDoubt()) {
                    comp.setForeground(EVAL_GOOD_COLOR);
                } else {
                    comp.setForeground(EVAL_SOSO_COLOR);
                }

                grade.setVisible(true);
                grade.setText(String.format("%.3f", eval.doubt));
            } else {
                grade.setVisible(false);
                comp.setVisible(false);
            }
        }

        // Triggered by button
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            // Assign current glyph with selected shape
            if (glyphController != null) {
                Glyph glyph = (Glyph) glyphController.getLag()
                                                     .getSelectedGlyph();

                Shape shape = Shape.valueOf(button.getText());

                // Actually assign the shape
                glyphController.asyncAssignGlyphSet(
                    Collections.singleton(glyph),
                    shape,
                    false);
            }
        }
    }

    //----------//
    // Selector //
    //----------//
    private class Selector
    {
        //~ Instance fields ----------------------------------------------------

        // A collection of EvalButton's
        List<EvalButton> buttons;

        //~ Constructors -------------------------------------------------------

        public Selector ()
        {
            buttons = new ArrayList<EvalButton>();

            for (int i = 0; i < constants.visibleButtons.getValue(); i++) {
                buttons.add(new EvalButton());
            }

            setEvals(null, null);
        }

        //~ Methods ------------------------------------------------------------

        //----------//
        // setEvals //
        //----------//
        /**
         * Display the evaluations with some text highlighting. Only relevant
         * evaluations are displayed (distance less than a given threshold)
         *
         * @param evals the ordered list of <b>all</b>evaluations from best to
         *              worst
         * @param glyph evaluated glyph, to check forbidden shapes if any
         */
        public void setEvals (Evaluation[] evals,
                              Glyph        glyph)
        {
            // Special case to empty the selector
            if (evals == null) {
                for (EvalButton evalButton : buttons) {
                    evalButton.setEval(null, false);
                }

                return;
            }

            double maxDist = constants.maxDoubt.getValue();
            int    iBound = Math.min(buttons.size(), evals.length);
            int    i;

            for (i = 0; i < iBound; i++) {
                Evaluation eval = evals[i];

                // Limitation on shape relevance
                if (eval.doubt > maxDist) {
                    break;
                }

                // Barred on non-barred button
                buttons.get(i)
                       .setEval(eval, glyph.isShapeForbidden(eval.shape));
            }

            // Zero the remaining buttons
            for (; i < buttons.size(); i++) {
                buttons.get(i)
                       .setEval(null, false);
            }
        }
    }

    //------------//
    // TestAction //
    //------------//
    /**
     * Class <code>TestAction</code> uses the evaluator on all known glyphs
     * within the sheet, to check if they can be correctly recognized. This does
     * not modify the current glyph assignments.
     */
    private class TestAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public TestAction ()
        {
            super("Global");
        }

        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            int          ok = 0;
            int          total = 0;
            final double maxDoubt = GlyphInspector.getSymbolMaxDoubt();
            final int    viewIndex = glyphController.getLag()
                                                    .viewIndexOf(view);

            for (Glyph glyph : sheet.getActiveGlyphs()) {
                if (glyph.isKnown() &&
                    (glyph.getShape() != Shape.COMBINING_STEM)) {
                    total++;

                    Evaluation guess = evaluator.vote(glyph, maxDoubt);

                    if ((guess != null) && (glyph.getShape() == guess.shape)) {
                        ok++;
                        view.colorizeGlyph(viewIndex, glyph, Shape.okColor);
                    } else {
                        view.colorizeGlyph(viewIndex, glyph, Shape.missedColor);
                    }
                }
            }

            String pc = String.format(
                " %5.2f%%",
                ((double) ok * 100) / (double) total);
            testPercent.setText(pc);
            testResult.setText(ok + "/" + total);

            // Almost all glyphs may have been modified, so...
            view.repaint();
        }
    }
}
