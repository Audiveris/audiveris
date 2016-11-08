//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        G l y p h M e n u                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sig.ui;

import omr.classifier.ui.SampleMenu;

import omr.glyph.Glyph;

import omr.sheet.Sheet;

import omr.ui.selection.EntityListEvent;
import omr.ui.selection.MouseMovement;
import omr.ui.selection.SelectionHint;
import omr.ui.util.AbstractMouseListener;
import omr.ui.util.UIUtil;
import omr.ui.view.LocationDependentMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JMenuItem;

/**
 * Class {@code GlyphMenu} displays a collection of glyphs.
 *
 * @author Hervé Bitteur
 */
public class GlyphMenu
        extends LocationDependentMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final GlyphListener glyphListener = new GlyphListener();

    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new GlyphMenu object.
     *
     * @param sheet the related sheet
     */
    public GlyphMenu (Sheet sheet)
    {
        super("Glyphs");
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void updateUserLocation (Rectangle rect)
    {
        // We rebuild the menu items on each update, since the set of glyphs is brand new.
        removeAll();

        Collection<Glyph> glyphs = sheet.getGlyphIndex().getSelectedGlyphList();

        if ((glyphs != null) && !glyphs.isEmpty()) {
            UIUtil.insertTitle(this, "Glyphs:");

            for (Glyph glyph : glyphs) {
                JMenuItem item = new SampleMenu(glyph, sheet);

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

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------------//
    // GlyphListener //
    //---------------//
    /**
     * Publish related glyph when entered by mouse.
     */
    private class GlyphListener
            extends AbstractMouseListener
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void mouseEntered (MouseEvent e)
        {
            SampleMenu sampleMenu = (SampleMenu) e.getSource();
            Glyph glyph = sampleMenu.getGlyph();

            sheet.getGlyphIndex().getEntityService().publish(
                    new EntityListEvent<Glyph>(
                            this,
                            SelectionHint.ENTITY_INIT,
                            MouseMovement.PRESSING,
                            Arrays.asList(glyph)));
        }
    }
}
