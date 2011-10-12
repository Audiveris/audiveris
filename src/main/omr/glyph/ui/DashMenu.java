//----------------------------------------------------------------------------//
//                                                                            //
//                              D a s h M e n u                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Shape;
import omr.glyph.ShapeRange;

import java.util.EnumSet;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class <code>DashMenu</code> is a GlyphMenu popup, dedicated to horizontal
 * dashes (ledgers and endings glyphs)
 *
 * @author Hervé Bitteur
 * @version $Id$
 */
public class DashMenu
    extends GlyphMenu
{
    //~ Static fields/initializers ---------------------------------------------

    /** A specific shape range, meant just for ending and ledger shapes */
    private static final ShapeRange Dashes = new ShapeRange(
        null,
        EnumSet.of(Shape.LEDGER, Shape.ENDING_HORIZONTAL));

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new DashMenu object.
     *
     * @param controller the related glyphs controller
     */
    public DashMenu (GlyphsController controller)
    {
        super(controller);
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
            ShapeRange.addRangeItems(Dashes, menu, new AssignListener(false));

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
            ShapeRange.addRangeItems(Dashes, menu, new AssignListener(true));

            return menu;
        }
    }
}
