//----------------------------------------------------------------------------//
//                                                                            //
//                            P i x e l B o a r d                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.selection.MouseMovement;
import omr.selection.PixelLevelEvent;
import omr.selection.SelectionHint;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.sheet.*;

import omr.ui.*;
import omr.ui.field.LIntegerField;
import omr.ui.util.Panel;

import omr.util.Implement;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import org.bushe.swing.event.EventSubscriber;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.*;

/**
 * Class <code>PixelBoard</code> is a board that displays pixel information as
 * provided by other entities (output side), and which can also be used by a
 * user to directly specify pixel coordinate values by entering numerical values
 * in the fields (input side).
 *
 * @author Hervé Bitteur
 */
public class PixelBoard
    extends Board
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(PixelBoard.class);

    /** Events this entity is interested in */
    private static final Collection<Class<?extends UserEvent>> eventClasses = new ArrayList<Class<?extends UserEvent>>();

    static {
        eventClasses.add(SheetLocationEvent.class);
        eventClasses.add(PixelLevelEvent.class);
    }

    //~ Instance fields --------------------------------------------------------

    /** Abscissa of upper Left point */
    private final LIntegerField x = new LIntegerField(
        "X",
        "Abscissa of upper left corner");

    /** Ordinate of upper Left point */
    private final LIntegerField y = new LIntegerField(
        "Y",
        "Ordinate of upper left corner");

    /** Width of rectangle */
    private final LIntegerField width = new LIntegerField(
        "Width",
        "Width of rectangle");

    /** Height of rectangle */
    private final LIntegerField height = new LIntegerField(
        "Height",
        "Height of rectangle");

    /** Pixel gray level */
    private final LIntegerField level = new LIntegerField(
        false,
        "Level",
        "Pixel level");

    //~ Constructors -----------------------------------------------------------

    //------------//
    // PixelBoard //
    //------------//
    /**
     * Create a PixelBoard
     *
     * @param unitName name of the unit which declares a pixel board
     * @param sheet the related sheet
     */
    public PixelBoard (String unitName,
                       Sheet  sheet)
    {
        super(
            unitName + "-PixelBoard",
            "Pixel-" + unitName,
            sheet.getSelectionService(),
            eventClasses);

        // Needed to process user input when RETURN/ENTER is pressed
        getComponent()
            .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke("ENTER"), "ParamAction");
        getComponent()
            .getActionMap()
            .put("ParamAction", new ParamAction());

        defineLayout();
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when Location Selection has been modified
     *
     * @param event the selection event
     */
    @Implement(EventSubscriber.class)
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            if (logger.isFineEnabled()) {
                logger.fine("PixelBoard: " + event);
            }

            if (event instanceof SheetLocationEvent) {
                // Display rectangle attributes
                SheetLocationEvent sheetLocation = (SheetLocationEvent) event;
                Rectangle          rect = sheetLocation.rectangle;

                if (rect != null) {
                    x.setValue(rect.x);
                    y.setValue(rect.y);
                    width.setValue(rect.width);
                    height.setValue(rect.height);

                    return;
                }

                x.setText("");
                y.setText("");
                width.setText("");
                height.setText("");
            } else if (event instanceof PixelLevelEvent) {
                // Display pixel gray level
                PixelLevelEvent pixelLevelEvent = (PixelLevelEvent) event;
                final Integer   pixelLevel = pixelLevelEvent.pixelLevel;

                if (pixelLevel != null) {
                    level.setValue(pixelLevel);
                } else {
                    level.setText("");
                }
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
        FormLayout layout = Panel.makeFormLayout(3, 3);

        // Specify that columns 1, 5 & 9 as well as 3, 7 & 11 have equal widths
        layout.setColumnGroups(
            new int[][] {
                { 1, 5, 9 },
                { 3, 7, 11 }
            });

        PanelBuilder builder = new PanelBuilder(layout, getBody());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int             r = 1; // --------------------------------
        builder.add(x.getLabel(), cst.xy(1, r));
        builder.add(x.getField(), cst.xy(3, r));
        x.getField()
         .setColumns(5);

        builder.add(width.getLabel(), cst.xy(5, r));
        builder.add(width.getField(), cst.xy(7, r));

        builder.add(level.getLabel(), cst.xy(9, r));
        builder.add(level.getField(), cst.xy(11, r));

        r += 2; // --------------------------------
        builder.add(y.getLabel(), cst.xy(1, r));
        builder.add(y.getField(), cst.xy(3, r));

        builder.add(height.getLabel(), cst.xy(5, r));
        builder.add(height.getField(), cst.xy(7, r));
    }

    //~ Inner Classes ----------------------------------------------------------

    //
    //    private class Groom
    //    {
    //        //~ Instance fields ----------------------------------------------------
    //
    //        boolean     collapsed = false;
    //        GroomAction action = new GroomAction();
    //        JButton     button = new JButton("Essai");
    //
    //        //~ Methods ------------------------------------------------------------
    //
    //        JButton getButton ()
    //        {
    //            return button;
    //        }
    //    }
    //
    //    private class GroomAction
    //        extends AbstractAction
    //    {
    //        //~ Constructors -------------------------------------------------------
    //
    //        public GroomAction ()
    //        {
    //            super("-");
    //            putValue(Action.SHORT_DESCRIPTION, "Collapse");
    //        }
    //
    //        //~ Methods ------------------------------------------------------------
    //
    //        public void actionPerformed (ActionEvent e)
    //        {
    //            logger.info("Groom action");
    //            groom.collapsed = !groom.collapsed;
    //
    //            if (groom.collapsed) {
    //                putValue(Action.NAME, "+");
    //                putValue(Action.SHORT_DESCRIPTION, "Expand");
    //            } else {
    //                putValue(Action.NAME, "-");
    //                putValue(Action.SHORT_DESCRIPTION, "Collapse");
    //            }
    //        }
    //    }

    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        // Method run whenever user presses Return/Enter in one of the parameter
        // fields
        public void actionPerformed (ActionEvent e)
        {
            // Remember & forward the new pixel selection
            // A rectangle (which can be degenerated to a point)
            selectionService.publish(
                new SheetLocationEvent(
                    PixelBoard.this,
                    SelectionHint.LOCATION_INIT,
                    MouseMovement.PRESSING,
                    new PixelRectangle(
                        x.getValue(),
                        y.getValue(),
                        width.getValue(),
                        height.getValue())));
        }
    }
}
