//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    L a g C o n t r o l l e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.GlyphsModel;
import org.audiveris.omr.glyph.ui.GlyphService;
import org.audiveris.omr.glyph.ui.GlyphsController;
import org.audiveris.omr.glyph.ui.NestView;
import org.audiveris.omr.glyph.ui.SymbolGlyphBoard;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.ui.SectionBoard;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.view.ScrollView;

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

    /** The underlying lag. */
    private final Lag lag;

    /** Tab name. */
    private final SheetTab tab;

    /** Related user display if any */
    private MyView view;

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
        super(new GlyphsModel(sheet, (GlyphService) sheet.getGlyphIndex().getEntityService()));
        this.lag = lag;
        this.tab = tab;
    }

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
        } else {
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

        sheet.getStub().getAssembly().addViewTab(tab, new ScrollView(view), new BoardsPane(
                                                 new PixelBoard(sheet), new SectionBoard(lag, false),
                                                 new SymbolGlyphBoard(this, true, true)));
    }

    //--------//
    // MyView //
    //--------//
    private final class MyView
            extends NestView
    {

        public MyView (GlyphIndex glyphIndex)
        {
            super(glyphIndex.getEntityService(), Arrays.asList(lag), sheet);

            setLocationService(sheet.getLocationService());

            setName(tab + "-MyView");
        }
    }
}
