//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S p o t s C o n t r o l l e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.beam;

import omr.glyph.Glyph;
import omr.glyph.GlyphIndex;
import omr.glyph.GlyphsModel;
import omr.glyph.Symbol.Group;
import omr.glyph.ui.GlyphsController;
import omr.glyph.ui.SymbolGlyphBoard;

import omr.lag.Lag;

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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code SpotsController} is a quick & dirty hack to display the retrieved spots.
 *
 * @author Hervé Bitteur
 */
public class SpotsController
        extends GlyphsController
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SpotsController.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final Lag[] lags;

    /** Related user display if any */
    private MyView view;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SpotsController object.
     *
     * @param sheet related sheet
     * @param lags  collection of lags to handle
     */
    public SpotsController (Sheet sheet,
                            Lag... lags)
    {
        super(new GlyphsModel(sheet, sheet.getGlyphIndex(), null));
        this.lags = lags;
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
        view = new MyView(getNest(), Arrays.asList(lags));

        sheet.getAssembly().addViewTab(
                SheetTab.BEAM_SPOT_TAB,
                new ScrollView(view),
                new BoardsPane(new PixelBoard(sheet), new SymbolGlyphBoard(this, true, true)));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // MyView //
    //--------//
    private final class MyView
            extends EntityView<Glyph>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public MyView (GlyphIndex nest,
                       List<Lag> lags)
        {
            super(nest.getEntityService());

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
            final GlyphIndex nest = (GlyphIndex) entityIndex;
            final Rectangle clip = g.getClipBounds();
            final Color oldColor = g.getColor();
            final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);

            for (Iterator<Glyph> it = nest.iterator(); it.hasNext();) {
                final Glyph glyph = it.next();

                if (glyph.hasGroup(Group.BEAM_SPOT)
                    && ((clip == null) || clip.intersects(glyph.getBounds()))) {
                    g.setColor(Color.LIGHT_GRAY);
                    glyph.getRunTable().render(g, glyph.getTopLeft()); // Draw glyph
                    g.setColor(Color.RED);
                    glyph.renderLine(g); // Draw glyph mean line
                }
            }

            g.setStroke(oldStroke);
            g.setColor(oldColor);
        }
    }
}
