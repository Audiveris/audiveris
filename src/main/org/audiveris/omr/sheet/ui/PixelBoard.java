//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P i x e l B o a r d                                       //
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
package org.audiveris.omr.sheet.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.field.LIntegerField;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.PixelEvent;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.util.Panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Class {@code PixelBoard} is a board that displays pixel information as provided by
 * other entities (output side), and which can also be used by a user to directly
 * specify pixel coordinate values by entering numerical values in the fields
 * (input side).
 *
 * @author Hervé Bitteur
 */
public class PixelBoard
        extends Board
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PixelBoard.class);

    /** Events this board is interested in. */
    private static final Class<?>[] eventsRead = new Class<?>[]{
        LocationEvent.class, PixelEvent.class
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Abscissa of upper Left point. */
    private final LIntegerField x = new LIntegerField("X", "Abscissa of upper left corner");

    /** Ordinate of upper Left point. */
    private final LIntegerField y = new LIntegerField("Y", "Ordinate of upper left corner");

    /** Width of rectangle. */
    private final LIntegerField width = new LIntegerField("Width", "Width of rectangle");

    /** Height of rectangle. */
    private final LIntegerField height = new LIntegerField("Height", "Height of rectangle");

    /** Pixel level. */
    protected final LIntegerField level = new LIntegerField(false, "Level", "Pixel level");

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a PixelBoard.
     *
     * @param sheet the related sheet
     */
    public PixelBoard (Sheet sheet)
    {
        super(Board.PIXEL, sheet.getLocationService(), eventsRead, true, false, false, false);

        // Needed to process user input when RETURN/ENTER is pressed
        getComponent().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"),
                "ParamAction");
        getComponent().getActionMap().put("ParamAction", new ParamAction());

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when Selection has been modified.
     *
     * @param event the selection event
     */
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            logger.debug("PixelBoard: {}", event);

            if (event instanceof LocationEvent) {
                handleEvent((LocationEvent) event); // Display rectangle attributes
            } else if (event instanceof PixelEvent) {
                handleEvent((PixelEvent) event); // Display pixel gray level
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        FormLayout layout = Panel.makeFormLayout(2, 3);

        // Specify that columns 1, 5 & 9 as well as 3, 7 & 11 have equal widths
        //        layout.setColumnGroups(
        //                new int[][]{
        //                    {1, 5, 9},
        //                    {3, 7, 11}
        //                });
        PanelBuilder builder = new PanelBuilder(layout, getBody());

        ///builder.setDefaultDialogBorder();
        CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------

        builder.add(level.getLabel(), cst.xy(1, r));
        builder.add(level.getField(), cst.xy(3, r));

        builder.add(x.getLabel(), cst.xy(5, r));
        builder.add(x.getField(), cst.xy(7, r));
        ///x.getField().setColumns(5);
        builder.add(width.getLabel(), cst.xy(9, r));
        builder.add(width.getField(), cst.xy(11, r));

        r += 2; // --------------------------------
        builder.add(y.getLabel(), cst.xy(5, r));
        builder.add(y.getField(), cst.xy(7, r));

        builder.add(height.getLabel(), cst.xy(9, r));
        builder.add(height.getField(), cst.xy(11, r));
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Display rectangle attributes
     *
     * @param locEvent
     */
    private void handleEvent (LocationEvent locEvent)
    {
        Rectangle rect = locEvent.getData();

        if (rect != null) {
            x.setValue(rect.x);
            y.setValue(rect.y);
            width.setValue(rect.width);
            height.setValue(rect.height);
        } else {
            x.setText("");
            y.setText("");
            width.setText("");
            height.setText("");
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Display pixel gray level
     *
     * @param pixelEvent
     */
    private void handleEvent (PixelEvent pixelEvent)
    {
        final Integer pixelLevel = pixelEvent.getData();

        if (pixelLevel != null) {
            level.setValue(pixelLevel);
        } else {
            level.setText("");
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        // Method run whenever user presses Return/Enter in one of the parameter fields
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Publish the new pixel selection rectangle (which can be degenerated to a point)
            getSelectionService().publish(
                    new LocationEvent(
                            PixelBoard.this,
                            SelectionHint.LOCATION_INIT,
                            MouseMovement.PRESSING,
                            new Rectangle(x.getValue(), y.getValue(), width.getValue(), height.getValue())));
        }
    }
}
