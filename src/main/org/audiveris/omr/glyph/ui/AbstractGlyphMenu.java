//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t G l y p h M e n u                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.glyph.ui;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.util.SeparableMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Abstract class {@code AbstractGlyphMenu} is the base for glyph-based menus.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractGlyphMenu
{

    private static final Logger logger = LoggerFactory.getLogger(AbstractGlyphMenu.class);

    /** Concrete menu. */
    protected final SeparableMenu menu = new SeparableMenu();

    /** Related sheet. */
    protected final Sheet sheet;

    /** Related nest. */
    protected final GlyphIndex nest;

    /** The selected glyphs. */
    protected Collection<Glyph> glyphs;

    /** Current number of selected glyphs. */
    protected int glyphNb;

    /** To manage elaboration. */
    protected boolean initDone = false;

    /**
     * Creates a new AbstractGlyphMenu object.
     *
     * @param sheet the related sheet
     * @param text  the menu text
     */
    public AbstractGlyphMenu (Sheet sheet,
                              String text)
    {
        this.sheet = sheet;
        nest = sheet.getGlyphIndex();
        menu.setText(text);
    }

    //---------//
    // getMenu //
    //---------//
    /**
     * Report the concrete menu.
     *
     * @return the usable menu
     */
    public SeparableMenu getMenu ()
    {
        return menu;
    }

    //------------//
    // updateMenu //
    //------------//
    /**
     * Update the menu according to the currently selected glyphs.
     *
     * @param glyphs the selected glyphs
     * @return the number of selected glyphs
     */
    public int updateMenu (Collection<Glyph> glyphs)
    {
        if (!initDone) {
            initMenu();
            initDone = true;
        }

        this.glyphs = glyphs;

        // Analyze the context
        glyphNb = (glyphs != null) ? glyphs.size() : 0;

        // Update the menu root item
        menu.setEnabled(glyphNb > 0);

        return glyphNb;
    }

    //----------//
    // initMenu //
    //----------//
    /**
     * Initialize the menu instance, once for all.
     * Further updates will be implemented through updateMenu() method.
     */
    protected void initMenu ()
    {
    }
}
