//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    G l y p h L i s t M e n u                                   //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.classifier.ui.ShapeMenu;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.util.AbstractMouseListener;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.ui.view.LocationDependentMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.JMenuItem;

/**
 * Class {@code GlyphListMenu} displays a collection of glyphs.
 *
 * @author Hervé Bitteur
 */
public class GlyphListMenu
        extends LocationDependentMenu
{

    private static final Logger logger = LoggerFactory.getLogger(GlyphListMenu.class);

    private final GlyphListener glyphListener = new GlyphListener();

    private final Sheet sheet;

    /**
     * Creates a new {@code GlyphMenu} object.
     *
     * @param sheet the related sheet
     */
    public GlyphListMenu (Sheet sheet)
    {
        super("Glyphs");
        this.sheet = sheet;
    }

    @Override
    public void updateUserLocation (Rectangle rect)
    {
        // We rebuild the menu items on each update, since the set of glyphs is brand new.
        removeAll();

        Collection<Glyph> glyphs = sheet.getGlyphIndex().getSelectedGlyphList();

        if ((glyphs != null) && !glyphs.isEmpty()) {
            UIUtil.insertTitle(this, "Glyphs:");

            for (Glyph glyph : glyphs) {
                ///JMenuItem item = new SampleMenu(glyph, sheet);
                JMenuItem item = new ShapeMenu(glyph, sheet);

                if (!glyph.getGroups().isEmpty()) {
                    item.setToolTipText(glyph.getGroups().toString());
                }

                item.addMouseListener(glyphListener);
                add(item);
            }

            setVisible(true);
        } else {
            setVisible(false);
        }

        super.updateUserLocation(rect);
    }

    //---------------//
    // GlyphListener //
    //---------------//
    /**
     * Publish related glyph when entered by mouse.
     */
    private class GlyphListener
            extends AbstractMouseListener
    {

        @Override
        public void mouseEntered (MouseEvent e)
        {
            ShapeMenu shapeMenu = (ShapeMenu) e.getSource();
            Glyph glyph = shapeMenu.getGlyph();

            sheet.getGlyphIndex().getEntityService().publish(
                    new EntityListEvent<>(
                            this,
                            SelectionHint.ENTITY_INIT,
                            MouseMovement.PRESSING,
                            glyph));
        }
    }
}
