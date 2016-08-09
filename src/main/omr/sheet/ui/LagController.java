//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    L a g C o n t r o l l e r                                   //
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
package omr.sheet.ui;

import omr.glyph.GlyphIndex;
import omr.glyph.GlyphsModel;
import omr.glyph.ui.GlyphService;
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
        super(
                new GlyphsModel(sheet, (GlyphService) sheet.getGlyphIndex().getEntityService(), null));
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
        view = new MyView(sheet.getGlyphIndex());

        sheet.getStub().getAssembly().addViewTab(
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
