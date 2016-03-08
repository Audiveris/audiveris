//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S a m p l e B o a r d                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier.ui;

import omr.classifier.Sample;
import omr.classifier.SampleRepository;

import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.ui.Board;
import omr.ui.EntityBoard;
import omr.ui.PixelCount;
import omr.ui.field.LLabel;
import omr.ui.selection.EntityListEvent;
import omr.ui.selection.MouseMovement;
import omr.ui.selection.UserEvent;
import omr.ui.util.Panel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import omr.classifier.SampleSheet;
import omr.classifier.SheetContainer.Descriptor;
import omr.classifier.ui.SampleVerifier.SampleController;

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

    /** Shape icon. */
    private final JLabel shapeIcon = new JLabel();

    /** Shape name. */
    private final LLabel shapeField = new LLabel("Shape:", "Shape for this sample");

    /** To remove. */
    private final RemoveAction removeAction = new RemoveAction();

    /** To reassign. */
    private final AssignAction assignAction = new AssignAction();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SampleBoard} object.
     *
     * @param controller the sample controller
     */
    public SampleBoard (SampleController controller)
    {
        super(Board.SAMPLE, controller.getGlyphService(), true, false, false, true, IdOption.ID_LABEL);
        this.controller = controller;

        // Force a constant dimension for the shapeIcon field, despite variation in size of the icon
        Dimension dim = new Dimension(
                constants.shapeIconWidth.getValue(),
                constants.shapeIconHeight.getValue());
        shapeIcon.setPreferredSize(dim);
        shapeIcon.setMaximumSize(dim);
        shapeIcon.setMinimumSize(dim);

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
        logger.debug("SampleBoard event:{}", event);

        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            super.onEvent(event);

            if (event instanceof EntityListEvent) {
                handleEvent((EntityListEvent<Sample>) event);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //---------------//
    // getFormLayout //
    //---------------//
    @Override
    protected FormLayout getFormLayout ()
    {
        return Panel.makeFormLayout(5, 3);
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

        JButton removeButton = new JButton(removeAction);
        removeButton.setHorizontalTextPosition(SwingConstants.LEFT);
        removeButton.setHorizontalAlignment(SwingConstants.RIGHT);
        removeAction.setEnabled(false);
        builder.add(removeButton, cst.xyw(5, r, 3));

        JButton assignButton = new JButton(assignAction);
        assignButton.setHorizontalTextPosition(SwingConstants.LEFT);
        assignButton.setHorizontalAlignment(SwingConstants.RIGHT);
        assignAction.setEnabled(false);
        builder.add(assignButton, cst.xyw(9, r, 3));

        r += 2; // --------------------------------

        // Shape Icon (start, spans several rows)
        builder.add(shapeIcon, cst.xywh(3, r, 1, 7));

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
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in InterList
     *
     * @param interListEvent
     */
    private void handleEvent (EntityListEvent<Sample> sampleListEvent)
    {
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

            SampleRepository repository = SampleRepository.getInstance();
            Descriptor desc = repository.getSheetDescriptor(sample);
            final String sh = desc.toString();
            sheetName.setText(sh);
            removeAction.setEnabled(!SampleSheet.isSymbols(desc.id));
            assignAction.setEnabled(!SampleSheet.isSymbols(desc.id));
        } else {
            iLine.setText("");
            weight.setText("");
            width.setText("");
            height.setText("");
            sheetName.setText("");
            removeAction.setEnabled(false);
            assignAction.setEnabled(false);
        }

    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------------//
    // AssignAction //
    //--------------//
    private static class AssignAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public AssignAction ()
        {
            super("Reassign");
            this.putValue(Action.SHORT_DESCRIPTION, "Assign a new shape");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            logger.info("Not yet implemented");
        }
    }

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

    //--------------//
    // RemoveAction //
    //--------------//
    private class RemoveAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public RemoveAction ()
        {
            super("Remove");
            this.putValue(Action.SHORT_DESCRIPTION, "Remove this sample");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            final Sample sample = SampleBoard.this.getSelectedEntity();
            logger.info("Removing {}", sample);
            controller.removeSample(sample);
        }
    }
}
