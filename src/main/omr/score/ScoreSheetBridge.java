//----------------------------------------------------------------------------//
//                                                                            //
//                      S c o r e S h e e t B r i d g e                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.log.Logger;

import omr.score.common.PagePoint;
import omr.score.common.PageRectangle;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.common.ScoreLocation;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.entity.ScoreSystem;

import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.ScoreLocationEvent;
import omr.selection.SheetLocationEvent;

import omr.sheet.Sheet;

import omr.util.Implement;

import org.bushe.swing.event.EventService;
import org.bushe.swing.event.EventSubscriber;

/**
 * Class <code>ScoreSheetBridge</code> is in charge of keeping in sync the
 * (sheet) Pixel Selection and the Score Selection. There should be exactly one
 * instance of this class per score (and thus per sheet).
 *
 * @author Hervé Bitteur
 */
public class ScoreSheetBridge
    implements EventSubscriber<LocationEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        ScoreSheetBridge.class);

    //~ Instance fields --------------------------------------------------------

    /** The sheet instance */
    private final Sheet sheet;

    /** The score instance */
    private final Score score;

    /** The event service to use */
    private final EventService eventService;

    /** Needed to force only one-way sync at a time */
    private volatile boolean bridging;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // ScoreSheetBridge //
    //------------------//
    /**
     * Creates a new ScoreSheetBridge object.
     *
     * @param score the related score (and thus the related sheet)
     */
    public ScoreSheetBridge (Score score)
    {
        this.score = score;
        sheet = score.getSheet();
        eventService = sheet.getSelectionService();

        // Register to event service
        eventService.subscribeStrongly(LocationEvent.class, this);
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getName //
    //---------//
    /**
     * Report the name of this observer
     *
     * @return name of the bridge
     */
    public String getName ()
    {
        return "Score-Sheet-Bridge";
    }

    //--------//
    // update //
    //--------//
    /**
     * Purpose of this bridge is to convert sheet location to score location
     * and vice versa.
     *
     * @param event the notified event, either sheet or score location event
     */
    @Implement(EventSubscriber.class)
    public void onEvent (LocationEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            if (logger.isFineEnabled()) {
                logger.fine("Bridge : onEvent " + event);
            }

            // This function is disabled when we are already bridging,
            // in order to avoid endless loop.
            if (!bridging) {
                bridging = true;

                if (event instanceof SheetLocationEvent) {
                    // Forward location information from Sheet to Score side
                    ScoreLocation      scoreLocation = null;
                    SheetLocationEvent sheetLocation = (SheetLocationEvent) event;

                    if (sheetLocation.rectangle != null) {
                        // Which system ?
                        PagePoint   pagPt = sheet.getScale()
                                                 .toPagePoint(
                            new PixelPoint(
                                sheetLocation.rectangle.x +
                                (sheetLocation.rectangle.width / 2),
                                sheetLocation.rectangle.y +
                                (sheetLocation.rectangle.height / 2)));
                        ScoreSystem system = score.pageLocateSystem(pagPt);

                        // Convert to the point at the center of the rectangle
                        SystemPoint sysPt = system.toSystemPoint(pagPt);
                        scoreLocation = new ScoreLocation(
                            system.getId(),
                            new SystemRectangle(sysPt.x, sysPt.y, 0, 0));

                        ///system.toSystemRectangle(sheetLocation.rectangle));
                    }

                    eventService.publish(
                        new ScoreLocationEvent(
                            this,
                            event.hint,
                            sheetLocation.movement,
                            scoreLocation));
                } else if (event instanceof ScoreLocationEvent) {
                    // Forward location information from Score to Sheet side
                    ScoreLocationEvent scoreLocationEvent = (ScoreLocationEvent) event;
                    ScoreLocation      scoreLocation = scoreLocationEvent.location;

                    if (scoreLocation != null) {
                        PixelRectangle pixRect = null;

                        if (scoreLocation.rectangle != null) {
                            ScoreSystem   system = score.getSystemById(
                                scoreLocation.systemId);
                            PageRectangle pagRect = system.toPageRectangle(
                                scoreLocation.rectangle);
                            pixRect = (PixelRectangle) system.getScale()
                                                             .toPixels(
                                pagRect,
                                new PixelRectangle());
                        }

                        eventService.publish(
                            new SheetLocationEvent(
                                this,
                                event.hint,
                                scoreLocationEvent.movement,
                                pixRect));
                    }
                }

                bridging = false;
            }
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return getName();
    }
}
