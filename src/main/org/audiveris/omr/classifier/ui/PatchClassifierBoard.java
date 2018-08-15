//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             P a t c h C l a s s i f i e r B o a r d                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import com.jgoodies.forms.layout.FormSpecs;

import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.classifier.OmrEvaluation;
import org.audiveris.omr.classifier.OmrShapeMapping;
import org.audiveris.omr.classifier.PatchClassifier;

import static org.audiveris.omr.classifier.PatchClassifier.CONTEXT_HEIGHT;
import static org.audiveris.omr.classifier.PatchClassifier.CONTEXT_WIDTH;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.util.FixedWidthIcon;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Class {@code PatchClassifierBoard} is a board dedicated to the results of
 * {@link PatchClassifier} when triggered by user location selection.
 *
 * @author Hervé Bitteur
 */
public class PatchClassifierBoard
        extends Board
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            PatchClassifierBoard.class);

    /** Events this board is interested in. */
    private static final Class<?>[] eventsRead = new Class<?>[]{LocationEvent.class};

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Pane for detailed info display about evaluations. */
    private final EvaluationSelector selector;

    /** Output: sub-image. */
    private final SubImagePanel subImagePanel = new SubImagePanel();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a {@code PatchClassifierBoard} instance.
     *
     * @param sheet           the related sheet, or null
     * @param locationService the service to get location
     * @param selected        true for pre-selection
     */
    public PatchClassifierBoard (Sheet sheet,
                                 SelectionService locationService,
                                 boolean selected)
    {
        super(
                new Board.Desc("Patch Classifier", 800),
                locationService,
                eventsRead,
                selected,
                false,
                false,
                false);

        this.sheet = sheet;

        selector = new EvaluationSelector(
                constants.visibleButtons.getValue(),
                constants.minGrade.getValue());

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when Selection has been modified.
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

            if (event instanceof LocationEvent) {
                handleEvent((LocationEvent) event); // Eval sub-image at location
            }
        } catch (Exception ex) {
            logger.warn("ImageBoard error", ex);
        }
    }

    /**
     * Define the panel layout, with sub-image on left and selector on right.
     */
    private void defineLayout ()
    {
        FormLayout layout = new FormLayout(
                "pref," + Panel.getFieldInterval() + ",pref",
                "pref");

        PanelBuilder builder = new PanelBuilder(layout, getBody());
        CellConstraints cst = new CellConstraints();

        builder.add(subImagePanel, cst.xy(1, 1, CellConstraints.CENTER, CellConstraints.TOP));
        builder.add(selector.getPanel(), cst.xy(3, 1));
    }

    /**
     * The received location event triggers evaluation.
     *
     * @param locEvent received location event
     */
    private void handleEvent (LocationEvent locEvent)
    {
        Rectangle rect = locEvent.getData();

        if (rect != null) {
            Wrapper<BufferedImage> imageWrapper = new Wrapper<BufferedImage>(null);
            OmrEvaluation[] evals = PatchClassifier.getInstance().getOmrEvaluations(
                    sheet,
                    GeoUtil.centerOf(rect),
                    sheet.getInterline(),
                    imageWrapper);
            subImagePanel.setImage(imageWrapper.value);
            selector.setEvals(evals);
        } else {
            subImagePanel.setImage(null);
            selector.setEvals(null);
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
                0.0001,
                "Threshold on displayable evaluation");

        private final Constant.Integer visibleButtons = new Constant.Integer(
                "buttons",
                5,
                "Max number of buttons in the shape selector");
    }

    //---------------//
    // SubImagePanel //
    //---------------//
    private static class SubImagePanel
            extends Panel
    {
        //~ Instance fields ------------------------------------------------------------------------

        private BufferedImage image;

        //~ Constructors ---------------------------------------------------------------------------
        public SubImagePanel ()
        {
            final Dimension dim = new Dimension(CONTEXT_WIDTH + 2, CONTEXT_HEIGHT + 2);
            setPreferredSize(dim);
            setMaximumSize(dim);
            setMinimumSize(dim);
        }

        //~ Methods --------------------------------------------------------------------------------
        public void setImage (BufferedImage image)
        {
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent (Graphics g)
        {
            super.paintComponent(g); // For background

            if (image != null) {
                // Draw the sub-image, leaving room for bounds rectangle
                g.drawImage(image, 1, 1, this);
            }

            g.setColor(Color.RED);

            // Draw the sub-image bounds
            g.drawRect(0, 0, CONTEXT_WIDTH + 1, CONTEXT_HEIGHT + 1);

            // Draw precise center
            g.drawLine(1, 1, CONTEXT_WIDTH + 1, CONTEXT_HEIGHT + 1);
            g.drawLine(1, CONTEXT_HEIGHT + 1, CONTEXT_WIDTH + 1, 1);
        }
    }

    //--------------------//
    // EvaluationSelector //
    //--------------------//
    private class EvaluationSelector
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Selector panel. */
        private final JPanel panel = new JPanel();

        /** The sequence of buttons. */
        final List<EvalButton> buttons = new ArrayList<EvalButton>();

        /** Minimum grade for a button to be displayed. */
        final double minGrade;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new {@code EvaluationSelector} object.
         *
         * @param evalCount maximum number of buttons displayed
         * @param minGrade  minimum grade for a button to be displayed
         */
        public EvaluationSelector (int evalCount,
                                   double minGrade)
        {
            this.minGrade = minGrade;

            for (int i = 0; i < evalCount; i++) {
                buttons.add(new EvalButton());
            }

            setEvals(null);

            defineLayout();
        }

        //~ Methods --------------------------------------------------------------------------------
        //----------//
        // setEvals //
        //----------//
        /**
         * Display the evaluations.
         * Only first evalCount evaluations are displayed.
         *
         * @param evals top evaluations sorted from best to worst
         */
        public final void setEvals (OmrEvaluation[] evals)
        {
            // Special case to empty the selector
            if (evals == null) {
                for (EvalButton evalButton : buttons) {
                    evalButton.setEval(null, false);
                }

                return;
            }

            int i;

            for (i = 0; i < buttons.size(); i++) {
                if (i >= evals.length) {
                    break;
                }

                OmrEvaluation eval = evals[i];

                // Limitation on shape relevance
                if (eval.grade < minGrade) {
                    break;
                }

                // Active buttons
                buttons.get(i).setEval(eval, false);
            }

            // Zero the remaining buttons
            for (; i < buttons.size(); i++) {
                buttons.get(i).setEval(null, false);
            }
        }

        /**
         * @return the panel
         */
        public JPanel getPanel ()
        {
            return panel;
        }

        private void defineLayout ()
        {
            String colSpec = "right:25dlu, 1dlu, 136dlu";
            FormLayout layout = new FormLayout(colSpec, "");

            for (int i = 0; i < buttons.size(); i++) {
                if (i != 0) {
                    layout.appendRow(FormSpecs.LINE_GAP_ROWSPEC);
                }

                layout.appendRow(FormSpecs.PREF_ROWSPEC);
            }

            PanelBuilder builder = new PanelBuilder(layout, panel);
            CellConstraints cst = new CellConstraints();

            for (int i = 0; i < buttons.size(); i++) {
                int r = (2 * i) + 1; // --------------------------------
                EvalButton evb = buttons.get(i);
                builder.add(evb.grade, cst.xy(1, r));
                builder.add(evb.button, cst.xy(3, r));
            }
        }

        //~ Inner Classes --------------------------------------------------------------------------
        //------------//
        // EvalButton //
        //------------//
        private class EvalButton
                implements ActionListener
        {
            //~ Instance fields --------------------------------------------------------------------

            // Shape button or text field. Only one of them will be created and used
            final JButton button;

            // The related grade
            JLabel grade = new JLabel("", SwingConstants.RIGHT);

            //~ Constructors -----------------------------------------------------------------------
            public EvalButton ()
            {
                grade.setToolTipText("Grade of the evaluation");
                button = new JButton();
                button.addActionListener(this);
                button.setToolTipText("Assignable shape");
                button.setHorizontalAlignment(SwingConstants.LEFT);
            }

            //~ Methods ----------------------------------------------------------------------------
            // Triggered by button selection
            @Override
            public void actionPerformed (ActionEvent e)
            {
                //                if (assigner != null) {
                //                    assigner.assignShape(Shape.valueOf(button.getText()));
                //                }

                //            // Assign inter on current glyph with selected shape
                //            if (intersController != null) {
                //                Glyph glyph = ((EntityService<Glyph>) getSelectionService()).getSelectedEntity();
                //
                //                if (glyph != null) {
                //                    String str = button.getText();
                //                    Shape shape = Shape.valueOf(str);
                //
                //                    // Actually assign the shape
                //                    intersController.addInter(glyph, shape);
                //                }
                //            }
            }

            public void setEval (OmrEvaluation eval,
                                 boolean enabled)
            {
                if (eval != null) {
                    String text = eval.omrShape.toString();
                    button.setVisible(true);
                    button.setEnabled(enabled);
                    button.setText(text);

                    // Display a shape icon if possible
                    Shape shape = OmrShapeMapping.shapeOf(eval.omrShape);
                    ShapeSymbol symbol = (shape != null) ? shape.getDecoratedSymbol() : null;
                    button.setIcon((symbol != null) ? new FixedWidthIcon(symbol) : null);

                    grade.setVisible(true);
                    grade.setText(String.format("%.4f", eval.grade));
                } else {
                    button.setVisible(false);
                    grade.setVisible(false);
                }
            }
        }
    }
}
