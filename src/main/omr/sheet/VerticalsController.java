//----------------------------------------------------------------------------//
//                                                                            //
//                   V e r t i c a l s C o n t r o l l e r                    //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.check.CheckBoard;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphsModel;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphLagView;
import omr.glyph.ui.GlyphsController;

import omr.lag.ui.RunBoard;
import omr.lag.ui.ScrollLagView;
import omr.lag.ui.SectionBoard;

import omr.log.Logger;

import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.SheetPainter;

import omr.step.Step;
import omr.step.StepException;

import omr.stick.Stick;

import omr.ui.BoardsPane;

import java.awt.*;
import java.util.*;

/**
 * Class <code>VerticalsController</code> is in charge of handling assignment
 * and deassignment of vertical entities (stems) at sheet level
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class VerticalsController
    extends GlyphsController
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        VerticalsController.class);

    /** Events this entity is interested in */
    private static final Collection<Class<?extends UserEvent>> eventClasses;

    static {
        eventClasses = new ArrayList<Class<?extends UserEvent>>();
        eventClasses.add(GlyphEvent.class);
    }

    //~ Instance fields --------------------------------------------------------

    /** Related user display if any */
    private MyView view;

    //~ Constructors -----------------------------------------------------------

    //---------------------//
    // VerticalsController //
    //---------------------//
    /**
     * Creates a new VerticalsController object.
     *
     * @param sheet the related sheet
     */
    public VerticalsController (Sheet sheet)
    {
        // We work with the sheet vertical lag
        super(new GlyphsModel(sheet, sheet.getVerticalLag(), Step.VERTICALS));
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
        if ((view == null) && constants.displayFrame.getValue()) {
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
        GlyphLag lag = getLag();

        // Specific rubber display
        view = new MyView(lag);

        // Create a hosting frame for the view
        final String unit = sheet.getRadix() + ":VerticalsBuilder";

        sheet.getAssembly()
             .addViewTab(
            Step.VERTICALS,
            new ScrollLagView(view),
            new BoardsPane(
                sheet,
                view,
                new PixelBoard(unit, sheet),
                new RunBoard(unit, lag),
                new SectionBoard(unit, lag.getLastVertexId(), lag),
                new GlyphBoard(unit, this, null),
                new MyCheckBoard(unit, lag.getSelectionService(), eventClasses)));
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean displayFrame = new Constant.Boolean(
            true,
            "Should we display a frame on the stem sticks");
    }

    //--------------//
    // MyCheckBoard //
    //--------------//
    private class MyCheckBoard
        extends CheckBoard<Stick>
    {
        //~ Constructors -------------------------------------------------------

        public MyCheckBoard (String                                unit,
                             SelectionService                      eventService,
                             Collection<Class<?extends UserEvent>> eventList)
        {
            super(unit, null, eventService, eventList);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void onEvent (UserEvent event)
        {
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                if (event instanceof GlyphEvent) {
                    GlyphEvent glyphEvent = (GlyphEvent) event;
                    Glyph      glyph = glyphEvent.getData();

                    if (glyph instanceof Stick) {
                        try {
                            Stick      stick = (Stick) glyph;
                            SystemInfo system = sheet.getSystemOf(stick);
                            // Get a fresh suite
                            setSuite(system.createStemCheckSuite(true));
                            tellObject(stick);
                        } catch (StepException ex) {
                            logger.warning("Glyph cannot be processed");
                        }
                    } else {
                        tellObject(null);
                    }
                }
            } catch (Exception ex) {
                logger.warning(getClass().getName() + " onEvent error", ex);
            }
        }
    }

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends GlyphLagView
    {
        //~ Constructors -------------------------------------------------------

        public MyView (GlyphLag lag)
        {
            super(lag, null, null, VerticalsController.this, null);
            setName("VerticalsBuilder-MyView");
            colorizeAllSections();
        }

        //~ Methods ------------------------------------------------------------

        //---------------------//
        // colorizeAllSections //
        //---------------------//
        @Override
        public void colorizeAllSections ()
        {
            super.colorizeAllSections();

            int viewIndex = lag.viewIndexOf(this);

            // Use light gray color for past successful entities
            sheet.colorize(lag, viewIndex, Color.lightGray);

            // Use bright yellow color for recognized stems
            for (Glyph glyph : sheet.getActiveGlyphs()) {
                if (glyph.isStem()) {
                    Stick stick = (Stick) glyph;
                    stick.colorize(lag, viewIndex, Color.yellow);
                }
            }
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        public void renderItems (Graphics g)
        {
            // Render all physical info known so far
            sheet.accept(new SheetPainter(g, false));

            super.renderItems(g);
        }
    }
}
