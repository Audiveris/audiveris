//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S p o t s C o n t r o l l e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.sheet.beam;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.ui.GlyphBoard;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.ui.PixelBoard;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.EntityView;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.ui.view.ScrollView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;

/**
 * Class {@code SpotsController} displays the retrieved spot glyphs for beams.
 *
 * @author Hervé Bitteur
 */
public class SpotsController
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SpotsController.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /**
     * Spot glyphs.
     * Purpose of this collection is to prevent the BEAM_SPOT glyphs to get garbage
     * collected (as long as this SpotsController instance exists).
     */
    private final List<Glyph> spots;

    /** User display. */
    private MyView view;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SpotsController object.
     *
     * @param sheet related sheet
     * @param spots relevant glyphs
     */
    public SpotsController (Sheet sheet,
                            List<Glyph> spots)
    {
        this.sheet = sheet;
        this.spots = spots;
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
        final GlyphIndex index = sheet.getGlyphIndex();
        view = new MyView(index);
        sheet.getStub().getAssembly().addViewTab(
                SheetTab.BEAM_SPOT_TAB,
                new ScrollView(view),
                new BoardsPane(new PixelBoard(sheet), new GlyphBoard(index.getEntityService(), true)));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // MyView //
    //--------//
    private final class MyView
            extends EntityView<Glyph>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public MyView (GlyphIndex glyphIndex)
        {
            super(glyphIndex.getEntityService());

            setLocationService(sheet.getLocationService());

            setName("SpotController-MyView");
        }

        //~ Methods --------------------------------------------------------------------------------
        //-------------//
        // renderItems //
        //-------------//
        @Override
        public void renderItems (Graphics2D g)
        {
            // We render all BEAM_SPOT glyphs
            final Rectangle clip = g.getClipBounds();
            final Color oldColor = g.getColor();
            final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);

            for (Glyph spot : spots) {
                if ((clip == null) || clip.intersects(spot.getBounds())) {
                    // Draw glyph
                    g.setColor(Color.LIGHT_GRAY);
                    spot.getRunTable().render(g, spot.getTopLeft());

                    // Draw glyph mean line
                    g.setColor(Color.RED);
                    spot.renderLine(g);
                }

                g.setStroke(oldStroke);
                g.setColor(oldColor);
            }
        }
    }
}
