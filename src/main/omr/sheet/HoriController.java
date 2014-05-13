//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   H o r i C o n t r o l l e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import java.awt.Graphics2D;
import java.awt.Stroke;
import omr.glyph.GlyphNest;
import omr.glyph.GlyphsModel;
import omr.glyph.ui.GlyphsController;
import omr.glyph.ui.NestView;
import omr.glyph.ui.SymbolGlyphBoard;

import omr.lag.Lag;
import omr.lag.ui.SectionBoard;

import omr.run.RunBoard;

import omr.selection.MouseMovement;
import omr.selection.UserEvent;

import omr.sheet.ui.PixelBoard;

import omr.step.Step;

import omr.ui.BoardsPane;
import omr.ui.view.ScrollView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import omr.glyph.ui.ViewParameters;
import omr.lag.Section;
import omr.ui.util.UIUtil;

/**
 * Class {@code HoriController} display horizontal glyphs for ledgers etc.
 *
 * @author Hervé Bitteur
 */
public class HoriController
        extends GlyphsController
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HoriController.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final Lag lag;

    /** Related user display if any */
    private MyView view;

    //~ Constructors -------------------------------------------------------------------------------
    //----------------//
    // HoriController //
    //----------------//
    /**
     * Creates a new HoriController object.
     *
     * @param sheet related sheet
     * @param lag   the full horizontal lag
     */
    public HoriController (Sheet sheet,
                           Lag lag)
    {
        super(new GlyphsModel(sheet, sheet.getNest(), null));
        this.lag = lag;
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
            view.refresh();
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
                Step.HORI_TAB,
                new ScrollView(view),
                new BoardsPane(
                new PixelBoard(sheet),
                new RunBoard(lag, false),
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

        public MyView (GlyphNest nest)
        {
            super(nest, Arrays.asList(lag), sheet);

            setLocationService(sheet.getLocationService());

            setName("HoriController-MyView");
        }

        //~ Methods --------------------------------------------------------------------------------
        //---------//
        // onEvent //
        //---------//
        /**
         * Call-back triggered from selection objects.
         *
         * @param event the notified event
         */
        @Override
        public void onEvent (UserEvent event)
        {
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                // Keep normal view behavior (rubber, etc...)
                super.onEvent(event);

                //
                //                // Additional tasks
                //                if (event instanceof LocationEvent) {
                //                    LocationEvent sheetLocation = (LocationEvent) event;
                //
                //                    if (sheetLocation.hint == SelectionHint.LOCATION_INIT) {
                //                        Rectangle rect = sheetLocation.getData();
                //
                //                        if ((rect != null) &&
                //                            (rect.width == 0) &&
                //                            (rect.height == 0)) {
                //                            // Look for pointed glyph
                //                            logger.info("Rect: {}", rect);
                //
                //                            //                            int index = glyphLookup(rect);
                //                            //                            navigator.setIndex(index, sheetLocation.hint);
                //                        }
                //                    }
                //                } else if (event instanceof GlyphEvent) {
                //                    GlyphEvent glyphEvent = (GlyphEvent) event;
                //
                //                    if (glyphEvent.hint == GLYPH_INIT) {
                //                        Glyph glyph = glyphEvent.getData();
                //
                //                        // Display glyph contour
                //                        if (glyph != null) {
                //                            locationService.publish(
                //                                new LocationEvent(
                //                                    this,
                //                                    glyphEvent.hint,
                //                                    null,
                //                                    glyph.getBounds()));
                //                        }
                //                    }
                //                }
            } catch (Exception ex) {
                logger.warn(getClass().getName() + " onEvent error", ex);
            }
        }

    //--------//
    // render //
    //--------//
    /**
     * Render the nest in the provided Graphics context, which may be
     * already scaled.
     *
     * @param g the graphics context
     */
    @Override
    public void render (Graphics2D g)
    {
        // Should we draw the section borders?
        final boolean drawBorders = ViewParameters.getInstance().isSectionMode();

        // Stroke for borders
        final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);

        for (Lag lag : lags) {
            // Render all sections, using the colors they have been assigned
            for (Section section : lag.getVertices()) {
                section.render(g, drawBorders, null);
            }
        }

        // Paint additional items, such as recognized items, etc...
        renderItems(g);

        // Restore stroke
        g.setStroke(oldStroke);
    }

        //        //-------------//
        //        // renderItems //
        //        //-------------//
        //        @Override
        //        public void renderItems (Graphics2D g)
        //        {
        //            super.renderItems(g);
        //
        //            Rectangle clip = g.getClipBounds();
        //
        //            Color oldColor = g.getColor();
        //            g.setColor(Color.RED);
        //
        //            for (Glyph glyph : nest.getAllGlyphs()) {
        //                final Shape shape = glyph.getShape();
        //
        //                if (relevantShapes.contains(shape)
        //                    && clip.intersects(glyph.getBounds())) {
        //                    // Draw mean line
        //                    glyph.renderLine(g);
        //                }
        //            }
        //
        //            g.setColor(oldColor);
        //        }
    }
}
