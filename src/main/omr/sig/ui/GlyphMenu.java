//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        G l y p h M e n u                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.ui;

import omr.glyph.facets.Glyph;

import omr.sig.Inter;

import omr.ui.util.AbstractMouseListener;
import omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code GlyphMenu} displays a collection of glyphs.
 *
 * @author Hervé Bitteur
 */
public class GlyphMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final JMenu menu;

    private GlyphListener glyphListener = new GlyphListener();

    //~ Constructors -------------------------------------------------------------------------------
    //----------//
    // GlyphMenu //
    //----------//
    /**
     * Creates a new GlyphMenu object.
     */
    public GlyphMenu ()
    {
        menu = new JMenu("Pile ...");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getMenu //
    //---------//
    public JMenu getMenu ()
    {
        return menu;
    }

    //------------//
    // updateMenu //
    //------------//
    public int updateMenu (Collection<Glyph> glyphs)
    {
        // We rebuild the menu items on each update, since the set of glyphs
        // is brand new.
        menu.removeAll();

        if (!glyphs.isEmpty()) {
            UIUtil.insertTitle(menu, "Glyphs:");

            for (Glyph glyph : glyphs) {
                final Collection<Inter> inters = glyph.getInterpretations();

                if (inters.isEmpty()) {
                    // Just a glyph item
                    JMenuItem item = new JMenuItem(new GlyphAction(glyph));
                    item.addMouseListener(glyphListener);
                    menu.add(item);
                } else {
                    // A whole menu of inters for this glyph
                    JMenu interMenu = new InterMenu(glyph, inters).getMenu();
                    interMenu.addMouseListener(glyphListener);
                    menu.add(interMenu);
                }
            }
        }

        return glyphs.size();
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
            JMenuItem item = (JMenuItem) e.getSource();
            GlyphAction action = (GlyphAction) item.getAction();
            action.publish();
        }
    }
}
