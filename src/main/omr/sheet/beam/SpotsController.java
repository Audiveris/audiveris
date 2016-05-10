//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S p o t s C o n t r o l l e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.beam;

import omr.glyph.Glyph;
import omr.glyph.GlyphIndex;
import omr.glyph.ui.GlyphBoard;

import omr.sheet.Sheet;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.SheetTab;

import omr.ui.BoardsPane;
import omr.ui.EntityView;
import omr.ui.util.UIUtil;
import omr.ui.view.ScrollView;

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
