//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 E v a l u a t i o n B o a r d                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.glyph.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;

import org.audiveris.omr.classifier.Classifier;
import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.classifier.Sample;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.SystemManager;
import org.audiveris.omr.sig.ui.InterController;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.util.FixedWidthIcon;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Class {@code EvaluationBoard} is a board dedicated to the display of evaluation
 * results performed by a classifier.
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
    /** Underlying glyph classifier. */
    protected final Classifier classifier;

    /** Related inters controller */
    protected final InterController intersController;

    /** Related sheet */
    @Navigable(false)
    private final Sheet sheet;

    /** Pane for detailed info display about the glyph evaluation */
    protected final Selector selector;

    /** Do we use GlyphChecker annotations? */
    private boolean useAnnotations;

    /** True for active buttons, false for passive fields. */
    protected final boolean isActive;

    //~ Constructors -------------------------------------------------------------------------------
    //-----------------//
    // EvaluationBoard //
    //-----------------//
    /**
     * Create an evaluation board with one neural network classifier and the ability to
     * force glyph shape.
     *
     * @param isActive         true for active buttons
     * @param sheet            the related sheet, or null
     * @param classifier       the classifier to use
     * @param glyphService     the service to get glyphs
     * @param intersController the related inters controller
     * @param selected         true for pre-selection
     */
    public EvaluationBoard (boolean isActive,
                            Sheet sheet,
                            Classifier classifier,
                            EntityService<Glyph> glyphService,
                            InterController intersController,
                            boolean selected)
    {
        super(
                new Desc(classifier.getName(), 700),
                glyphService,
                eventsRead,
                selected,
                false,
                false,
                false);

        this.classifier = classifier;
        this.intersController = intersController;
        this.isActive = isActive;
        this.sheet = sheet;

        selector = new Selector();
        defineLayout();
        useAnnotations = false; //true;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
                evaluate(glyph);
            }
        } catch (Exception ex) {
            logger.warn("EvaluationBoard error", ex);
        }
    }

    //----------//
    // evaluate //
    //----------//
    /**
     * Evaluate the glyph at hand, and display the result in classifier dedicated area.
     *
     * @param glyph the glyph at hand
     */
    protected void evaluate (Glyph glyph)
    {
        if (glyph == null) {
            // Blank the output
            selector.setEvals(null, null);
        } else if (classifier != null) {
            if (sheet != null) {
                // TODO: this picks up the first system that may be interested by the glyph!
                // TODO: there is no support for staff specific scale!
                SystemManager systemManager = sheet.getSystemManager();

                for (SystemInfo system : systemManager.getSystemsOf(glyph)) {
                    selector.setEvals(
                            classifier.evaluate(
                                    glyph,
                                    system,
                                    selector.evalCount(),
                                    constants.minGrade.getValue(),
                                    useAnnotations ? EnumSet.of(Classifier.Condition.CHECKED)
                                            : Classifier.NO_CONDITIONS),
                            glyph);

                    return;
                }
            } else if (glyph instanceof Sample) {
                selector.setEvals(
                        classifier.evaluate(
                                glyph,
                                ((Sample) glyph).getInterline(),
                                selector.evalCount(),
                                constants.minGrade.getValue(),
                                useAnnotations ? EnumSet.of(Classifier.Condition.CHECKED)
                                        : Classifier.NO_CONDITIONS),
                        glyph);
            }
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
            if (i != 0) {
                layout.appendRow(FormSpecs.LINE_GAP_ROWSPEC);
            }

            layout.appendRow(FormSpecs.PREF_ROWSPEC);
        }

        PanelBuilder builder = new PanelBuilder(layout, getBody());
        CellConstraints cst = new CellConstraints();

        for (int i = 0; i < visibleButtons; i++) {
            int r = (2 * i) + 1; // --------------------------------
            EvalButton evb = selector.buttons.get(i);
            builder.add(evb.grade, cst.xy(5, r));
            builder.add(isActive ? evb.button : evb.field, cst.xyw(7, r, 5));
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // Selector //
    //----------//
    protected class Selector
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
         * Display the evaluations.
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
                    evalButton.setEval(null, false);
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
                buttons.get(i).setEval(eval, enabled);
            }

            // Zero the remaining buttons
            for (; i < evalCount(); i++) {
                buttons.get(i).setEval(null, false);
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

            if (isActive) {
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
            // Assign inter on current glyph with selected shape
            if (intersController != null) {
                Glyph glyph = ((EntityService<Glyph>) getSelectionService()).getSelectedEntity();

                if (glyph != null) {
                    String str = button.getText();
                    Shape shape = Shape.valueOf(str);

                    // Actually assign the shape
                    intersController.addInter(glyph, shape);
                }
            }
        }

        public void setEval (Evaluation eval,
                             boolean enabled)
        {
            final JComponent comp = isActive ? button : field;

            if (eval != null) {
                Evaluation.Failure failure = eval.failure;
                String text = eval.shape.toString();
                String tip = (failure != null) ? failure.toString() : null;

                if (isActive) {
                    button.setEnabled(enabled);
                    button.setText(text);
                    button.setToolTipText(tip);

                    ShapeSymbol symbol = eval.shape.getDecoratedSymbol();
                    button.setIcon((symbol != null) ? new FixedWidthIcon(symbol) : null);
                } else {
                    field.setText(text);
                    field.setToolTipText(tip);

                    ShapeSymbol symbol = eval.shape.getDecoratedSymbol();
                    field.setIcon((symbol != null) ? new FixedWidthIcon(symbol) : null);
                }

                comp.setVisible(true);

                if (failure == null) {
                    comp.setForeground(EVAL_GOOD_COLOR);
                } else {
                    comp.setForeground(EVAL_SOSO_COLOR);
                }

                grade.setVisible(true);
                ///grade.setText(String.format("%.3f", Math.log(eval.grade)));
                grade.setText(String.format("%.4f", eval.grade));
            } else {
                grade.setVisible(false);
                comp.setVisible(false);
            }
        }
    }
}
