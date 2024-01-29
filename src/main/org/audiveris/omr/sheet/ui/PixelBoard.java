//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P i x e l B o a r d                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2024. All rights reserved.
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

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Class <code>PixelBoard</code> is a board that displays pixel information as provided by
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
    private static final Class<?>[] eventsRead = new Class<?>[]
    { LocationEvent.class, PixelEvent.class };

    //~ Instance fields ----------------------------------------------------------------------------

    /** Pixel level. */
    protected final LIntegerField level = new LIntegerField(false, "Level", "Pixel level");

    /** Abscissa of upper Left point. */
    private final LIntegerField x = new LIntegerField("X", "Abscissa of upper left corner");

    /** Ordinate of upper Left point. */
    private final LIntegerField y = new LIntegerField("Y", "Ordinate of upper left corner");

    /** Width of rectangle. */
    private final LIntegerField width = new LIntegerField("Width", "Width of rectangle");

    /** Height of rectangle. */
    private final LIntegerField height = new LIntegerField("Height", "Height of rectangle");

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a PixelBoard, pre-selected by default
     *
     * @param sheet the related sheet
     */
    public PixelBoard (Sheet sheet)
    {
        this(sheet, true);
    }

    /**
     * Create a PixelBoard.
     *
     * @param sheet    the related sheet
     * @param selected true for pre-selected, false for collapsed
     */
    public PixelBoard (Sheet sheet,
                       boolean selected)
    {
        super(Board.PIXEL, sheet.getLocationService(), eventsRead, selected, false, false, false);

        // Needed to process user input when RETURN/ENTER is pressed
        getComponent().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"),
                "ParamAction");
        getComponent().getActionMap().put("ParamAction", new ParamAction());

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        final FormLayout layout = Panel.makeFormLayout(2, 3);

        // Specify that columns 1, 5 & 9 as well as 3, 7 & 11 have equal widths
        //        layout.setColumnGroups(
        //                new int[][]{
        //                    {1, 5, 9},
        //                    {3, 7, 11}
        //                });
        final FormBuilder builder = FormBuilder.create().layout(layout).panel(getBody());

        int r = 1; // --------------------------------

        builder.addRaw(level.getLabel()).xy(1, r);
        builder.addRaw(level.getField()).xy(3, r);

        builder.addRaw(x.getLabel()).xy(5, r);
        builder.addRaw(x.getField()).xy(7, r);
        ///x.getField().setColumns(5);
        builder.addRaw(width.getLabel()).xy(9, r);
        builder.addRaw(width.getField()).xy(11, r);

        r += 2; // --------------------------------
        builder.addRaw(y.getLabel()).xy(5, r);
        builder.addRaw(y.getField()).xy(7, r);

        builder.addRaw(height.getLabel()).xy(9, r);
        builder.addRaw(height.getField()).xy(11, r);
    }

    //---------------------//
    // handleLocationEvent //
    //---------------------//
    /**
     * Display rectangle attributes
     *
     * @param locEvent the location event
     */
    protected void handleLocationEvent (LocationEvent locEvent)
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

    //------------------//
    // handlePixelEvent //
    //------------------//
    /**
     * Display pixel gray level
     *
     * @param pixelEvent the pixel event
     */
    protected void handlePixelEvent (PixelEvent pixelEvent)
    {
        final Integer pixelLevel = pixelEvent.getData();

        if (pixelLevel != null) {
            level.setValue(pixelLevel);
        } else {
            level.setText("");
        }
    }

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
                handleLocationEvent((LocationEvent) event); // Display rectangle attributes
            } else if (event instanceof PixelEvent) {
                handlePixelEvent((PixelEvent) event); // Display pixel gray level
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    private class ParamAction
            extends AbstractAction
    {
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
                            new Rectangle(
                                    x.getValue(),
                                    y.getValue(),
                                    width.getValue(),
                                    height.getValue())));
        }
    }
}
