//----------------------------------------------------------------------------//
//                                                                            //
//                               B a r M e n u                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.ShapeRange;

import javax.swing.*;
import javax.swing.JMenu;

/**
 * Class <code>BarMenu</code> defines the popup menu to interact with barlines
 *
 * @author Herv&eacute; Bitteur
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
    // allocateActions //
    //-----------------//
    @Override
    protected void allocateActions ()
    {
        // Copy & Paste actions
        new PasteAction();
        new CopyAction();

        // Deassign selected glyph(s)
        new DeassignAction();

        // Manually assign a shape
        new AssignAction();

        // Build a compound, with menu for shape selection
        new CompoundAction();

        // Dump current glyph
        new DumpAction();
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
            ShapeRange.addRangeItems(
                ShapeRange.Barlines,
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
            ShapeRange.addRangeItems(
                ShapeRange.Barlines,
                menu,
                new AssignListener(true));

            return menu;
        }
    }
}
