//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t G l y p h M e n u                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphIndex;

import omr.sheet.Sheet;

import omr.ui.util.SeparableMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Abstract class {@code AbstractGlyphMenu} is the base for glyph-based menus such as
 * {@link SymbolMenu}.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractGlyphMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractGlyphMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
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

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
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
