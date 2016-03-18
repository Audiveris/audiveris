//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        G l y p h M e n u                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
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
        super("Glyphs ...");
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
