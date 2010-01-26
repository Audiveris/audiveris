//----------------------------------------------------------------------------//
//                                                                            //
//                    S c o r e L o c a t i o n E v e n t                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.score.common.ScoreLocation;

import java.awt.Rectangle;

/**
 * Class <code>ScoreLocationEvent</code> is a UI Event that represents a new
 * location within the Score space
 *
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>ErrorsEditor, ScoreSheetBridge, ScoreView
 * <dt><b>Subscribers:</b><dd>ScoreSheetBridge, ScoreView
 * <dt><b>Readers:</b><dd>
 * </dl>
 * @author Herv&eacute Bitteur
 */
public class ScoreLocationEvent
    extends LocationEvent
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The location information, which combines containing system with rectangle
     * related to that system
     */
    public final ScoreLocation location;

    //~ Constructors -----------------------------------------------------------

    //--------------------//
    // ScoreLocationEvent //
    //--------------------//
    /**
     * Creates a new ScoreLocationEvent object, with value for each final data
     *
     * @param source the entity that created this event
     * @param hint how the event originated
     * @param movement what is the originating mouse movement
     * @param location the location within the score space
     */
    public ScoreLocationEvent (Object        source,
                               SelectionHint hint,
                               MouseMovement movement,
                               ScoreLocation location)
    {
        super(source, hint, movement);
        this.location = location;
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getData //
    //---------//
    @Override
    public ScoreLocation getData ()
    {
        return location;
    }

    //--------------//
    // getRectangle //
    //--------------//
    @Override
    public Rectangle getRectangle ()
    {
        if (location == null) {
            return null;
        } else {
            return location.rectangle;
        }
    }
}
