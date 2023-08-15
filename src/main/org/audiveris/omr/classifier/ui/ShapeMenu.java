//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S h a p e M e n u                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.util.SeparableMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class <code>ShapeMenu</code> is a menu dedicated to assigning an inter to a glyph.
 *
 * @author Hervé Bitteur
 */
public class ShapeMenu
        extends SeparableMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ShapeMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Containing sheet. */
    private final Sheet sheet;

    /** Selected glyph. */
    private final Glyph glyph;

    private final ActionListener shapeListener;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>ShapeMenu</code> object.
     *
     * @param glyph the selected glyph
     * @param sheet the containing sheet
     */
    public ShapeMenu (final Glyph glyph,
                      final Sheet sheet)
    {
        this.sheet = sheet;
        this.glyph = glyph;

        shapeListener = (ActionEvent e) ->
        {
            JMenuItem source = (JMenuItem) e.getSource();
            Shape shape = Shape.valueOf(source.getText());
            sheet.getInterController().assignGlyph(glyph, shape);
        };

        populateMenu();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------------//
    // addRecentShapes //
    //-----------------//
    private void addRecentShapes ()
    {
        List<Shape> shapes = sheet.getSheetEditor().getShapeBoard().getHistory();

        if (!shapes.isEmpty()) {
            final MusicFamily family = sheet.getStub().getMusicFamily();

            for (Shape shape : shapes) {
                JMenuItem menuItem = new JMenuItem(
                        shape.toString(),
                        shape.getDecoratedSymbol(family));
                menuItem.setToolTipText(shape.getDescription());
                menuItem.addActionListener(shapeListener);
                add(menuItem);
            }

            addSeparator();
        }
    }

    //----------//
    // getGlyph //
    //----------//
    /**
     * @return the glyph
     */
    public Glyph getGlyph ()
    {
        return glyph;
    }

    //--------------//
    // populateMenu //
    //--------------//
    private void populateMenu ()
    {
        setText(Integer.toString(glyph.getId()));

        // Convenient assignment to most recent shapes
        addRecentShapes();

        // Manual shape selection
        add(new AssignMenu());
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //------------//
    // AssignMenu //
    //------------//
    private class AssignMenu
            extends JMenu
    {

        AssignMenu ()
        {
            super("Assign");

            populate();
        }

        private void populate ()
        {
            ShapeSet.addAllShapes(sheet.getStub().getMusicFamily(), this, shapeListener);
        }
    }
}
