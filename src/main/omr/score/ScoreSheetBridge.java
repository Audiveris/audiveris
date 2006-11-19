//----------------------------------------------------------------------------//
//                                                                            //
//                      S c o r e S h e e t B r i d g e                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionObserver;
import omr.selection.SelectionTag;

import omr.sheet.PixelPoint;
import omr.sheet.Sheet;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.Rectangle;

/**
 * Class <code>ScoreSheetBridge</code> is in charge of keeping in sync the
 * (sheet) Pixel Selection and the Score Selection. There should exactly one
 * instance of this class per score (and thus per sheet).
 *
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>PIXEL Location (if not already bridging)
 * <li>SCORE Location (if not already bridging)
 * </ul>
 *
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>PIXEL Location
 * <li>SCORE Location
 * </ul>
 * </dl>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreSheetBridge
    implements SelectionObserver
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        ScoreSheetBridge.class);

    //~ Instance fields --------------------------------------------------------

    /** The sheet instance */
    private Sheet sheet;

    /** The score instance */
    private Score score;

    /** The sheet location selection */
    private final Selection pixelSelection;

    /** The score location selection */
    private final Selection scoreSelection;

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

        // Keep reference of the bridged selections
        pixelSelection = sheet.getSelection(SelectionTag.PIXEL);
        scoreSelection = sheet.getSelection(SelectionTag.SCORE);

        // Register to  Selections
        pixelSelection.addObserver(this);
        scoreSelection.addObserver(this);
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
    @Implement(SelectionObserver.class)
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
     * @param selection the originating selection object
     * @param hint processing hint, if any
     */
    @Implement(SelectionObserver.class)
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Bridge : selection updated " + selection);
        }

        if (!bridging) {
            bridging = true; // Prevent re-entry

            Object entity = selection.getEntity();

            if (logger.isFineEnabled()) {
                logger.fine("Bridge " + selection.getTag() + ": " + entity);
            }

            Rectangle rect = (Rectangle) entity;

            switch (selection.getTag()) {
            case PIXEL :

                // Forward to Score side
                if (rect != null) {
                    PagePoint pagPt = sheet.getScale()
                                           .toPagePoint(
                        new PixelPoint(rect.x, rect.y));

                    if (pagPt != null) {
                        // Which system ?
                        final System system = score.pageLocateSystem(pagPt);
                        ScorePoint   scrPt = system.toScorePoint(pagPt);
                        scoreSelection.setEntity(new Rectangle(scrPt), hint);
                    }
                } else {
                    scoreSelection.setEntity(null, hint);
                }

                break;

            case SCORE :

                // Forward to Sheet side
                if (rect != null) {
                    // We forge a ScorePoint from the display point
                    ScorePoint scrPt = new ScorePoint(rect.x, rect.y); // ???

                    // The enclosing system
                    System     system = score.scoreLocateSystem(scrPt);
                    PagePoint  pagPt = system.toPagePoint(scrPt);
                    PixelPoint pt = sheet.getScale()
                                         .toPixelPoint(pagPt, null);
                    pixelSelection.setEntity(new Rectangle(pt), hint);
                } else {
                    pixelSelection.setEntity(null, hint);
                }

                break;

            default :
                logger.severe("Unexpected selection event from " + selection);
            }

            bridging = false;
        }
    }
}
