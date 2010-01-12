//----------------------------------------------------------------------------//
//                                                                            //
//                              D a s h M e n u                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
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
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class DashMenu
    extends GlyphMenu
{
    //~ Static fields/initializers ---------------------------------------------

    /** A specific shape range, meant just for ending and ledger shapes */
    private static final ShapeRange Dashes = new ShapeRange(
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
