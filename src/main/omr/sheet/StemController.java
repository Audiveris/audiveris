//----------------------------------------------------------------------------//
//                                                                            //
//                         S t e m C o n t r o l l e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import java.awt.Graphics2D;
import java.util.Arrays;
import omr.glyph.GlyphsModel;
import omr.glyph.Nest;
import omr.glyph.ui.GlyphsController;
import omr.glyph.ui.NestView;
import omr.glyph.ui.SymbolGlyphBoard;
import omr.lag.Lag;
import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.UserEvent;
import omr.sheet.ui.PixelBoard;
import omr.ui.BoardsPane;
import omr.ui.view.ScrollView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class {@code StemController}
 *
 * @author Hervé Bitteur
 */
public class StemController
    extends GlyphsController
{
        //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
        StemController.class);

    /** Events that can be published on internal service (TODO: Check this!) */
    private static final Class<?>[] locEvents = new Class<?>[] {
                                                    LocationEvent.class
                                                };

    //~ Instance fields --------------------------------------------------------

    private final Lag         lag;

    /** Related user display if any */
    private MyView view;
    
    
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new StemController object.
     *
     * @param sheet related sheet
     * @param nest  spot nest
     */
    public StemController (Sheet       sheet,
                           Nest        nest,
                           Lag         lag)
    {
        super(new GlyphsModel(sheet, nest, null));
        this.lag = lag;

        nest.setServices(sheet.getLocationService());
        lag.setServices(
            sheet.getLocationService(),
            getNest().getGlyphService());
    }


    //~ Methods ----------------------------------------------------------------

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

        sheet.getAssembly()
             .addViewTab(
            "Stems",
            new ScrollView(view),
            new BoardsPane(
                new PixelBoard(sheet),
                new SymbolGlyphBoard(this, true, true)));
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // MyView //
    //--------//
    private final class MyView
        extends NestView
    {
        //~ Constructors -------------------------------------------------------

        public MyView (Nest nest)
        {
            super(
                nest,
                StemController.this,
                Arrays.asList(lag),
                sheet.getItemRenderers());

            setLocationService(sheet.getLocationService());

            setName("StemController-MyView");
        }

        //~ Methods ------------------------------------------------------------

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

        //-------------//
        // renderItems //
        //-------------//
        @Override
        public void renderItems (Graphics2D g)
        {
            super.renderItems(g);
        }
    }


}
