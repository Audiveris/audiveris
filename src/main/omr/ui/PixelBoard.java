//-----------------------------------------------------------------------//
//                                                                       //
//                          P i x e l B o a r d                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.sheet.PixelPoint;
import omr.ui.field.LIntegerField;
import omr.ui.util.Panel;
import omr.ui.view.PixelFocus;
import omr.ui.view.PixelObserver;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class <code>PixelBoard</code> is a board that displays pixel information
 * as provided by other entities (output side), and which can also be used
 * by a user to directly specify pixel coordinate values by entering
 * numerical values in the fields (input side).
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class PixelBoard
    extends Board
    implements PixelObserver
{
    //~ Instance variables ------------------------------------------------

    // Upper Left point
    private final LIntegerField x = new LIntegerField
        ("X", "Abscissa of upper left corner");
    private final LIntegerField y = new LIntegerField
        ("Y", "Ordinate of upper left corner");
    private final LIntegerField level = new LIntegerField
        (false, "Level", "Pixel level");

    // Lower Right point
    private final LIntegerField width = new LIntegerField
        ("Width", "Width of rectangle");
    private final LIntegerField height = new LIntegerField
        ("Height", "Height of rectangle");

    // Pixel Focus if any
    private PixelFocus pixelFocus;

    // To avoid circular updates
    private volatile transient boolean selfUpdating = false;

    //~ Constructors ------------------------------------------------------

    //------------//
    // PixelBoard //
    //------------//
    /**
     * Create a PixelBoard
     */
    public PixelBoard ()
    {
        super(Board.Tag.PIXEL);

        getComponent().getInputMap
            (JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put
            (KeyStroke.getKeyStroke("ENTER"),
             "ParamAction");
        getComponent().getActionMap().put("ParamAction", new ParamAction());

        defineLayout();
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout()
    {
        FormLayout layout = Panel.makeFormLayout(3, 3);

        // Specify that columns 1 & 5 as well as 3 & 7 have equal widths.
        layout.setColumnGroups(new int[][]{{1, 5, 9}, {3, 7, 11}});

        PanelBuilder builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int r = 1;                      // --------------------------------
        builder.addSeparator("Pixel",   cst.xyw(1,  r, 11));

        r += 2;                         // --------------------------------
        builder.add(x.getLabel(),       cst.xy (1,  r));
        builder.add(x.getField(),       cst.xy (3,  r));
        x.getField().setColumns(5);

        builder.add(width.getLabel(),   cst.xy (5,  r));
        builder.add(width.getField(),   cst.xy (7,  r));

        builder.add(level.getLabel(),   cst.xy (9,  r));
        builder.add(level.getField(),   cst.xy (11, r));

        r += 2;                         // --------------------------------
        builder.add(y.getLabel(),       cst.xy (1,  r));
        builder.add(y.getField(),       cst.xy (3,  r));

        builder.add(height.getLabel(),  cst.xy (5,  r));
        builder.add(height.getField(),  cst.xy (7,  r));
    }

    //---------------//
    // setPixelFocus //
    //---------------//
    /**
     * Connect an entity to be later notified of pixel focus, as input by a
     * user (when the ENTER key is pressed)
     *
     * @param pixelFocus
     */
    public void setPixelFocus (PixelFocus pixelFocus)
    {
        this.pixelFocus = pixelFocus;
    }

    //--------//
    // update //
    //--------//
    /**
     * Inform about the new pixel coordinates of a display rectangle
     *
     * @param rect the (rubber) rectangle
     */
    public void update (Rectangle rect)
    {
        level.setText("");
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

    //--------//
    // update //
    //--------//
    /**
     * Inform about the point (coordinates and grey level) where the mouse
     * was clicked
     *
     * @param ul coordinates of the upper left corner
     * @param lVal grey level (-1 means no value)
     */
    public void update (PixelPoint ul,
                        int lVal)
    {
        if (ul != null) {
            x.setValue(ul.x);
            y.setValue(ul.y);

            if (lVal != -1) {
                level.setValue(lVal);
            } else {
                level.setText("");
            }
        } else {
            x.setText("");
            y.setText("");
            level.setText("");
        }

        // Hide the lower right part
        width.setText("");
        height.setText("");
    }

    //--------//
    // update //
    //--------//
    /**
     * Inform about the point (coordinates) where the mouse was clicked
     *
     * @param ul coordinates of the upper left corner
     */
    public void update (PixelPoint ul)
    {
        update(ul, -1);
    }

    //~ Classes -----------------------------------------------------------

    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
        extends AbstractAction
    {
        // Method run whenever user presses Return/Enter in one of the
        // parameter fields
        public void actionPerformed (ActionEvent e)
        {
            if (pixelFocus != null && !selfUpdating) {
                selfUpdating = true;
                // A rectangle (which can be degenerated to a point)
                if (width.getValue() != 0 || height.getValue() != 0) {
                    pixelFocus.setFocusRectangle
                        (new Rectangle
                         (x.getValue(), y.getValue(),
                          width.getValue(), height.getValue()));
                } else {
                    pixelFocus.setFocusPoint
                        (new Point(x.getValue(), y.getValue()));
                }
                selfUpdating = false;
            }
        }
    }
}
