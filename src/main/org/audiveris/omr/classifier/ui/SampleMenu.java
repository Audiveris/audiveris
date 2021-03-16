//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S a m p l e M e n u                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.ui.util.SeparableMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumSet;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code SampleMenu} is a menu dedicated to picking a glyph as a shape sample.
 *
 * @author Hervé Bitteur
 */
public class SampleMenu
        extends SeparableMenu
{

    private static final Logger logger = LoggerFactory.getLogger(SampleMenu.class);

    /** Containing sheet. */
    private final Sheet sheet;

    /** Selected glyph. */
    private final Glyph glyph;

    /**
     * Creates a new {@code SampleMenu} object.
     *
     * @param glyph the selected glyph
     * @param sheet the containing sheet
     */
    public SampleMenu (Glyph glyph,
                       Sheet sheet)
    {
        this.sheet = sheet;
        this.glyph = glyph;

        populateMenu();
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

    //-----------//
    // addSample //
    //-----------//
    private void addSample (Shape shape)
    {
        // TODO: we need staff information (-> interline and pitch)
        final Book book = sheet.getStub().getBook();
        final SampleRepository repository = book.getSampleRepository();
        repository.addSample(shape, glyph, sheet);
    }

    //-----------//
    // getShapes //
    //-----------//
    private Set<Shape> getShapes ()
    {
        final Set<Shape> shapes = EnumSet.noneOf(Shape.class);

        for (Inter inter : sheet.getInterIndex().getEntityService().getSelectedEntityList()) {
            if (inter.getGlyph() == glyph) {
                shapes.add(inter.getShape());
            }
        }

        return shapes;
    }

    //--------------//
    // populateMenu //
    //--------------//
    private void populateMenu ()
    {
        setText(Integer.toString(glyph.getId()));

        // Glyph interpretations
        Set<Shape> shapes = getShapes();

        if (!shapes.isEmpty()) {
            add(new AssignMenu(shapes));
        }

        // Manual shape selection
        add(new SelectMenu());
    }

    //------------//
    // AssignMenu //
    //------------//
    private class AssignMenu
            extends JMenu
    {

        private final ActionListener listener = new ActionListener()
        {
            @Override
            public void actionPerformed (ActionEvent e)
            {
                JMenuItem source = (JMenuItem) e.getSource();
                Shape shape = Shape.valueOf(source.getText());
                addSample(shape);
            }
        };

        AssignMenu (Set<Shape> shapes)
        {
            super("Assign sample");

            populate(shapes);
        }

        private void populate (Set<Shape> shapes)
        {
            for (Shape shape : shapes) {
                JMenuItem menuItem = new JMenuItem(shape.toString(), shape.getDecoratedSymbol());
                menuItem.addActionListener(listener);
                add(menuItem);
            }
        }
    }

    //------------//
    // SelectMenu //
    //------------//
    private class SelectMenu
            extends JMenu
    {

        SelectMenu ()
        {
            super("Select sample");

            populate();
        }

        private void populate ()
        {
            ShapeSet.addAllShapes(this, new ActionListener()
                          {
                              @Override
                              public void actionPerformed (ActionEvent e)
                              {
                                  JMenuItem source = (JMenuItem) e.getSource();
                                  Shape shape = Shape.valueOf(source.getText());
                                  addSample(shape);
                              }
                          });
        }
    }
}
