//----------------------------------------------------------------------------//
//                                                                            //
//                    S h e e t L o c a t i o n E v e n t                     //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.selection;

import omr.score.common.PixelRectangle;

import java.awt.Rectangle;

/**
 * Class <code>SheetLocationEvent</code> is a UI Event that represents a new
 * location within the Sheet space
 *
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>GlyphBrowser, GlyphLag, Lag, PixelBoard, 
 * ScoreSheetBridge, ScoreView, SheetAssembly, ZoomedPanel
 * <dt><b>Subscribers:</b><dd>GlyphBrowser, GlyphLag(hLag, vLag), GlyphLagView,
 * LagView, Picture, PixelBoard, ScoreSheetBridge, SystemsBuilder
 * <dt><b>Readers:</b><dd>ScoreView, SheetAssembly, TextAreaBrowser
 * </dl>
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SheetLocationEvent
    extends LocationEvent
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The location rectangle, which can be degenerated to a point when both
     * width and height values equal zero
     */
    public final PixelRectangle rectangle;

    //~ Constructors -----------------------------------------------------------

    //--------------------//
    // SheetLocationEvent //
    //--------------------//
    /**
     * Creates a new SheetLocationEvent object, with value for each final data
     *
     * @param source the entity that created this event
     * @param rectangle the location within the sheet space
     * @param hint how this event originated
     * @param movement what is the originating mouse movement
     */
    public SheetLocationEvent (Object         source,
                               SelectionHint  hint,
                               MouseMovement  movement,
                               PixelRectangle rectangle)
    {
        super(source, hint, movement);
        this.rectangle = rectangle;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getEntity //
    //-----------//
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
