//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    L a g C o n t r o l l e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.glyph.GlyphIndex;
import omr.glyph.GlyphsModel;
import omr.glyph.ui.GlyphsController;
import omr.glyph.ui.NestView;
import omr.glyph.ui.SymbolGlyphBoard;

import omr.lag.Lag;
import omr.lag.ui.SectionBoard;

import omr.sheet.Sheet;

import omr.ui.BoardsPane;
import omr.ui.view.ScrollView;

import java.util.Arrays;

/**
 * Class {@code LagController} is a first attempt to provide an easy view on a lag in
 * the context of sheet glyphs.
 *
 * @author Hervé Bitteur
 */
public class LagController
        extends GlyphsController
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The underlying lag. */
    private final Lag lag;

    /** Tab name. */
    private final SheetTab tab;

    /** Related user display if any */
    private MyView view;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code LagController} object.
     *
     * @param sheet related sheet
     * @param lag   the full horizontal lag
     * @param tab   a name for tab
     */
    public LagController (Sheet sheet,
                          Lag lag,
                          SheetTab tab)
    {
        super(new GlyphsModel(sheet, sheet.getGlyphIndex(), null));
        this.lag = lag;
        this.tab = tab;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the display if any, with proper colors for sections
     */
    public void refresh ()
    {
        if (view == null) {
            displayFrame();
        } else if (view != null) {
            view.repaint();
        }
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        // Specific rubber display
        view = new MyView(getNest());

        sheet.getAssembly().addViewTab(
                tab,
                new ScrollView(view),
                new BoardsPane(
                        new PixelBoard(sheet),
                        new SectionBoard(lag, false),
                        new SymbolGlyphBoard(this, true, true)));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // MyView //
    //--------//
    private final class MyView
            extends NestView
    {
        //~ Constructors ---------------------------------------------------------------------------

        public MyView (GlyphIndex nest)
        {
            super(nest.getEntityService(), Arrays.asList(lag), sheet);

            setLocationService(sheet.getLocationService());

            setName(tab + "-MyView");
        }
    }
}
