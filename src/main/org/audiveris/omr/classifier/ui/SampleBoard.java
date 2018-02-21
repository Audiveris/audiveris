//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S a m p l e B o a r d                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.classifier.Sample;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.classifier.SheetContainer.Descriptor;
import org.audiveris.omr.classifier.ui.SampleController.AssignAction;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.EntityBoard;
import org.audiveris.omr.ui.PixelCount;
import org.audiveris.omr.ui.field.LLabel;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.util.Panel;

import org.jdesktop.application.ApplicationAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Class {@code SampleBoard} defines a UI board for {@link Sample} entities.
 *
 * @author Hervé Bitteur
 */
public class SampleBoard
        extends EntityBoard<Sample>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SampleBoard.class);

    private static final String DBL_FORMAT = "%.3f"; // Format for double output

    //~ Instance fields ----------------------------------------------------------------------------
    /** User controller for samples. */
    private final SampleController controller;

    private final SampleRepository repository;

    /** Sheet name. */
    private final LLabel sheetName = new LLabel("Sheet:", "Containing sheet");

    /** Field for interline. */
    private final LLabel iLine = new LLabel("iLine:", "Interline value in pixels");

    /** Glyph characteristics : normalized weight. */
    private final LLabel weight = new LLabel("Weight:", "Normalized weight");

    /** Glyph characteristics : normalized width. */
    private final LLabel width = new LLabel("Width:", "Normalized width");

    /** Glyph characteristics : normalized height. */
    private final LLabel height = new LLabel("Height:", "Normalized height");

    /** Staff-based pitch. */
    private final LLabel pitch = new LLabel("Pitch:", "Staff-based pitch");

    /** Shape icon. */
    private final JLabel shapeIcon = new JLabel();

    /** Shape name. */
    private final LLabel shapeField = new LLabel("Shape:", "Shape for this sample");

    /** The (re)assign button. (used as location for popup menu) */
    private JButton assignButton;

    /** To remove. */
    private final ApplicationAction removeAction;

    /** To reassign. */
    private final AssignAction assignAction;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SampleBoard} object.
     *
     * @param controller the sample controller
     */
    public SampleBoard (SampleController controller)
    {
        super(
                Board.SAMPLE,
                (EntityService<Sample>) controller.getGlyphService(),
                true,
                false,
                false,
                true,
                IdOption.ID_LABEL);
        this.controller = controller;
        this.repository = ((SampleModel) controller.getModel()).getRepository();

        // Force a constant dimension for the shapeIcon field, despite variation in size of the icon
        Dimension dim = new Dimension(
                constants.shapeIconWidth.getValue(),
                constants.shapeIconHeight.getValue());
        shapeIcon.setPreferredSize(dim);
        shapeIcon.setMaximumSize(dim);
        shapeIcon.setMinimumSize(dim);

        removeAction = controller.getRemoveAction();
        assignAction = controller.getAssignAction();

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // getFormLayout //
    //---------------//
    @Override
    protected FormLayout getFormLayout ()
    {
        return Panel.makeFormLayout(6, 3);
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout for SampleBoard specific fields.
     */
    private void defineLayout ()
    {
        final CellConstraints cst = new CellConstraints();

        // Layout
        int r = 1; // -----------------------------

        JButton removeButton = new JButton(controller.getRemoveAction());
        removeButton.setHorizontalTextPosition(SwingConstants.LEFT);
        removeButton.setHorizontalAlignment(SwingConstants.RIGHT);
        removeAction.setEnabled(false);
        builder.add(removeButton, cst.xyw(5, r, 3));

        assignButton = new JButton(assignAction);
        assignButton.setHorizontalTextPosition(SwingConstants.LEFT);
        assignButton.setHorizontalAlignment(SwingConstants.RIGHT);
        assignAction.setEnabled(false);
        builder.add(assignButton, cst.xyw(9, r, 3));

        r += 2; // --------------------------------

        // Shape Icon (start, spans several rows)
        builder.add(shapeIcon, cst.xywh(3, r, 1, 9));

        builder.add(sheetName.getLabel(), cst.xy(1, r));
        builder.add(sheetName.getField(), cst.xyw(3, r, 9));

        r += 2; // --------------------------------

        builder.add(shapeField.getLabel(), cst.xy(5, r));
        builder.add(shapeField.getField(), cst.xyw(7, r, 5));

        r += 2; // --------------------------------

        builder.add(iLine.getLabel(), cst.xy(5, r));
        builder.add(iLine.getField(), cst.xy(7, r));

        builder.add(width.getLabel(), cst.xy(9, r));
        builder.add(width.getField(), cst.xy(11, r));

        r += 2; // --------------------------------

        builder.add(weight.getLabel(), cst.xy(5, r));
        builder.add(weight.getField(), cst.xy(7, r));

        builder.add(height.getLabel(), cst.xy(9, r));
        builder.add(height.getField(), cst.xy(11, r));

        r += 2; // --------------------------------

        builder.add(pitch.getLabel(), cst.xy(9, r));
        builder.add(pitch.getField(), cst.xy(11, r));
    }

    //-----------------------//
    // handleEntityListEvent //
    //-----------------------//
    /**
     * Interest in InterList
     *
     * @param sampleListEvent sample event list
     */
    @Override
    protected void handleEntityListEvent (EntityListEvent<Sample> sampleListEvent)
    {
        super.handleEntityListEvent(sampleListEvent);

        final Sample sample = sampleListEvent.getEntity();

        // Shape text and icon
        Shape shape = (sample != null) ? sample.getShape() : null;

        if (shape != null) {
            shapeField.setText(shape.toString());
            shapeIcon.setIcon(shape.getDecoratedSymbol());
        } else {
            shapeField.setText("");
            shapeIcon.setIcon(null);
        }

        // Sample characteristics
        if (sample != null) {
            // Interline
            final int interline = sample.getInterline();
            iLine.setText(Integer.toString(interline));

            weight.setText(String.format(DBL_FORMAT, sample.getNormalizedWeight(interline)));
            width.setText(String.format(DBL_FORMAT, (double) sample.getWidth() / interline));
            height.setText(String.format(DBL_FORMAT, (double) sample.getHeight() / interline));

            if (sample.getPitch() != null) {
                pitch.setText(String.format(DBL_FORMAT, sample.getPitch()));
            } else {
                pitch.setText("");
            }

            Descriptor desc = repository.getDescriptor(sample);

            if (desc != null) {
                final String sh = desc.toString();
                sheetName.setText(sh);
                sheetName.getField().setToolTipText(desc.getAliasesString());
                removeAction.setEnabled(!SampleRepository.isSymbols(desc.getName()));
                assignAction.setEnabled(!SampleRepository.isSymbols(desc.getName()));
            } else {
                sheetName.setText("");
                sheetName.getField().setToolTipText(null);
                removeAction.setEnabled(false);
                assignAction.setEnabled(false);
            }
        } else {
            iLine.setText("");
            weight.setText("");
            width.setText("");
            height.setText("");
            pitch.setText("");
            sheetName.setText("");
            sheetName.getField().setToolTipText(null);
            removeAction.setEnabled(false);
            assignAction.setEnabled(false);
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

        private final PixelCount shapeIconHeight = new PixelCount(
                70,
                "Exact pixel height for the shape icon field");

        private final PixelCount shapeIconWidth = new PixelCount(
                50,
                "Exact pixel width for the shape icon field");
    }
}
