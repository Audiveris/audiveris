//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S h a p e M e n u                                       //
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.util.SeparableMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code ShapeMenu} is a menu dedicated to assigning an inter to a glyph.
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

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ShapeMenu} object.
     *
     * @param glyph the selected glyph
     * @param sheet the containing sheet
     */
    public ShapeMenu (Glyph glyph,
                      Sheet sheet)
    {
        this.sheet = sheet;
        this.glyph = glyph;

        populateMenu();
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        //~ Constructors ---------------------------------------------------------------------------

        public AssignMenu ()
        {
            super("Assign");

            populate();
        }

        //~ Methods --------------------------------------------------------------------------------
        private void populate ()
        {
            ShapeSet.addAllShapes(
                    this,
                    new ActionListener()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    JMenuItem source = (JMenuItem) e.getSource();
                    Shape shape = Shape.valueOf(source.getText());
                    sheet.getInterController().assignGlyph(glyph, shape);
                }
            });
        }
    }
}
