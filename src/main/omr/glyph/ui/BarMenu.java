//----------------------------------------------------------------------------//
//                                                                            //
//                               B a r M e n u                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.ShapeSet;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code BarMenu} defines the popup menu to interact with barlines
 *
 * @author Hervé Bitteur
 */
public class BarMenu
    extends GlyphMenu
{
    //~ Constructors -----------------------------------------------------------

    //---------//
    // BarMenu //
    //---------//
    /**
     * Create the Bar popup menu
     *
     * @param barsController the top companion
     */
    public BarMenu (GlyphsController barsController)
    {
        super(barsController);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // registerActions //
    //-----------------//
    @Override
    protected void registerActions ()
    {
        // Copy & Paste actions
        register(0, new PasteAction());
        register(0, new CopyAction());

        // Deassign selected glyph(s)
        register(0, new DeassignAction());

        // Manually assign a shape
        register(0, new AssignAction());

        // Build a compound, with menu for shape selection
        register(0, new CompoundAction());

        // Dump current glyph
        register(0, new DumpAction());
    }

    //~ Inner Classes ----------------------------------------------------------

    private class AssignAction
        extends GlyphMenu.AssignAction
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public JMenuItem getMenuItem ()
        {
            JMenu menu = new JMenu(this);
            ShapeSet.addSetShapes(
                ShapeSet.Barlines,
                menu,
                new AssignListener(false));

            return menu;
        }
    }

    private class CompoundAction
        extends GlyphMenu.CompoundAction
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public JMenuItem getMenuItem ()
        {
            JMenu menu = new JMenu(this);
            ShapeSet.addSetShapes(
                ShapeSet.Barlines,
                menu,
                new AssignListener(true));

            return menu;
        }
    }
}
