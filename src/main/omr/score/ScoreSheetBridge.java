//----------------------------------------------------------------------------//
//                                                                            //
//                      S c o r e S h e e t B r i d g e                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.log.Logger;

import omr.score.common.PagePoint;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.common.ScorePoint;
import omr.score.common.ScoreRectangle;
import omr.score.entity.ScoreSystem;

import omr.selection.LocationEvent;
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
 * @author Herv&eacute Bitteur
 * @version $Id$
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
        eventService = sheet.getEventService();

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
     * Notification of selection objects (disabled when already bridging, to
     * avoid endless loop)
     *
     * @param event the notified event
     */
    @Implement(EventSubscriber.class)
    public void onEvent (LocationEvent event)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Bridge : onEvent " + event);
        }

        if (!bridging) {
            bridging = true; // Prevent re-entry

            if (event instanceof SheetLocationEvent) {
                // Forward to Score side
                SheetLocationEvent sheetLocation = (SheetLocationEvent) event;
                ScoreRectangle     scoreRect = null;

                if (sheetLocation.rectangle != null) {
                    PagePoint pagPt = sheet.getScale()
                                           .toPagePoint(
                        new PixelPoint(
                            sheetLocation.rectangle.x +
                            (sheetLocation.rectangle.width / 2),
                            sheetLocation.rectangle.y +
                            (sheetLocation.rectangle.height / 2)));

                    if (pagPt != null) {
                        // Which system ?
                        ScoreSystem system = score.pageLocateSystem(pagPt);
                        ScorePoint  scrPt = system.toScorePoint(pagPt);
                        scoreRect = new ScoreRectangle(scrPt);
                    }
                }

                eventService.publish(
                    new ScoreLocationEvent(
                        this,
                        event.hint,
                        sheetLocation.movement,
                        scoreRect));
            } else if (event instanceof ScoreLocationEvent) {
                // Forward to Sheet side
                ScoreLocationEvent scoreLocation = (ScoreLocationEvent) event;
                PixelRectangle     pixRect = null;

                if (scoreLocation.rectangle != null) {
                    // We forge a ScorePoint from the display point
                    ScorePoint  scrPt = new ScorePoint(
                        scoreLocation.rectangle.x,
                        scoreLocation.rectangle.y);

                    // The enclosing system
                    ScoreSystem system = score.scoreLocateSystem(scrPt);
                    PagePoint   pagPt = system.toPagePoint(scrPt);
                    PixelPoint  pixPt = sheet.getScale()
                                             .toPixelPoint(pagPt, null);
                    pixRect = new PixelRectangle(pixPt);
                }

                eventService.publish(
                    new SheetLocationEvent(
                        this,
                        event.hint,
                        scoreLocation.movement,
                        pixRect));
            }

            bridging = false;
        }
    }
}
