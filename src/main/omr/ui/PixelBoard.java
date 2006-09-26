//----------------------------------------------------------------------------//
//                                                                            //
//                            P i x e l B o a r d                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.ui;

import omr.selection.Selection;
import omr.selection.SelectionHint;

import omr.ui.field.LIntegerField;
import omr.ui.util.Panel;

import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * Class <code>PixelBoard</code> is a board that displays pixel information as
 * provided by other entities (output side), and which can also be used by a
 * user to directly specify pixel coordinate values by entering numerical values
 * in the fields (input side).
 *
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>PIXEL
 * <li>LEVEL
 * </ul>
 *
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>PIXEL Location (flagged with LOCATION_INIT hint)
 * </ul>
 * </dl>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class PixelBoard
    extends Board
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(PixelBoard.class);

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

    /** Pixel grey level */
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
     */
    public PixelBoard (String unitName)
    {
        super(Board.Tag.PIXEL, unitName + "-PixelBoard");

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

    //--------//
    // update //
    //--------//
    /**
     * Call-back triggered when Location Selection has been modified
     *
     * @param selection the notified Selection
     * @param hint potential notification hint
     */
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        Object entity = selection.getEntity();

        if (logger.isFineEnabled()) {
            logger.fine("PixelBoard " + selection.getTag() + ": " + entity);
        }

        switch (selection.getTag()) {
        case PIXEL : // Display rectangle characteristics

            Rectangle rect = (Rectangle) entity;

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

            break;

        case LEVEL : // Display pixel grey level

            Integer val = (Integer) entity;

            if ((val != null) && (val != -1)) {
                level.setValue(val);
            } else {
                level.setText("");
            }

            break;

        default :
            logger.severe("Unexpected selection event from " + selection);
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

        PanelBuilder builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int             r = 1; // --------------------------------
        builder.addSeparator("Pixel", cst.xyw(1, r, 11));

        r += 2; // --------------------------------
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

    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
        extends AbstractAction
    {
        // Method run whenever user presses Return/Enter in one of the parameter
        // fields
        public void actionPerformed (ActionEvent e)
        {
            // Remember & forward the new pixel selection
            if (outputSelection != null) {
                // A rectangle (which can be degenerated to a point)
                outputSelection.setEntity(
                    new Rectangle(
                        x.getValue(),
                        y.getValue(),
                        width.getValue(),
                        height.getValue()),
                    SelectionHint.LOCATION_INIT);
            }
        }
    }
}
