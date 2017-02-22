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
import org.audiveris.omr.glyph.ui.NestView;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.SectionService;
import org.audiveris.omr.lag.ui.SectionBoard;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.ui.PixelBoard;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.ui.view.ScrollView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Arrays;
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

    /** Lag that indexes all glyph sections. */
    private final Lag spotLag;

    /** User display. */
    private MyView view;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SpotsController object.
     *
     * @param sheet   related sheet
     * @param spots   relevant glyphs
     * @param spotLag lag of glyph sections
     */
    public SpotsController (Sheet sheet,
                            List<Glyph> spots,
                            Lag spotLag)
    {
        this.sheet = sheet;
        this.spots = spots;
        this.spotLag = spotLag;
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
        spotLag.setEntityService(new SectionService(spotLag, sheet.getLocationService()));
        view = new MyView(index);
        sheet.getStub().getAssembly().addViewTab(
                SheetTab.BEAM_SPOT_TAB,
                new ScrollView(view),
                new BoardsPane(
                        new PixelBoard(sheet),
                        new GlyphBoard(index.getEntityService(), true),
                        new SectionBoard(spotLag, true)));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // MyView //
    //--------//
    private final class MyView
            extends NestView
    {
        //~ Constructors ---------------------------------------------------------------------------

        public MyView (GlyphIndex glyphIndex)
        {
            super(glyphIndex.getEntityService(), Arrays.asList(spotLag), sheet);

            setLocationService(sheet.getLocationService());

            setName("SpotsController-MyView");
        }

        //~ Methods --------------------------------------------------------------------------------
        //--------//
        // render //
        //--------//
        @Override
        public void render (Graphics2D g)
        {
            // (Phase #1) Render all spots
            final Rectangle clip = g.getClipBounds();
            final Color oldColor = g.getColor();
            g.setColor(Color.LIGHT_GRAY);

            for (Glyph spot : spots) {
                if ((clip == null) || clip.intersects(spot.getBounds())) {
                    spot.getRunTable().render(g, spot.getTopLeft()); // Draw glyph
                }
            }

            g.setColor(oldColor);
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        public void renderItems (Graphics2D g)
        {
            // (Phase #2) Render sections (on top of rendered spots)
            super.render(g);

            // (Phase #3) Render spots mean line
            final Rectangle clip = g.getClipBounds();
            final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
            final Color oldColor = g.getColor();
            g.setColor(Color.RED);

            for (Glyph spot : spots) {
                if ((clip == null) || clip.intersects(spot.getBounds())) {
                    spot.renderLine(g); // Draw glyph mean line
                }
            }

            g.setColor(oldColor);
            g.setStroke(oldStroke);
        }
    }
}
