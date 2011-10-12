//----------------------------------------------------------------------------//
//                                                                            //
//                         L o c a t i o n E v e n t                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import java.awt.Rectangle;

/**
 * Class <code>LocationEvent</code> is UI Event that represents a new
 * location (a rectangle, perhaps degenerated to a point) within the Sheet space
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>GlyphBrowser, GlyphLag, Lag, PixelBoard,
 * ScoreSheetBridge, ScoreView, SheetAssembly, ZoomedPanel
 * <dt><b>Subscribers:</b><dd>ZoomedPanel, GlyphBrowser, GlyphLag(hLag, vLag),
 *  LagView, Picture, PixelBoard, ScoreSheetBridge, SystemsBuilder
 * <dt><b>Readers:</b><dd>ScoreView, SheetAssembly, TextAreaBrowser
 * </dl>
 * @author Hervé Bitteur
 */
public class LocationEvent
    extends UserEvent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LocationEvent.class);

    //~ Instance fields --------------------------------------------------------

    /**
     * The location rectangle, which can be degenerated to a point when both
     * width and height values equal zero
     */
    public final PixelRectangle rectangle;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LocationEvent object.
     * @param source the actual entity that created this event
     * @param hint how the event originated
     * @param movement the precise mouse movement
     * @param rectangle the location within the sheet space
     */
    public LocationEvent (Object         source,
                          SelectionHint  hint,
                          MouseMovement  movement,
                          PixelRectangle rectangle)
    {
        super(source, hint, movement);
        this.rectangle = rectangle;
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getData //
    //---------//
    @Override
    public PixelRectangle getData ()
    {
        return rectangle;
    }

    //--------------//
    // getRectangle //
    //--------------//
    public Rectangle getRectangle ()
    {
        return rectangle;
    }
}
