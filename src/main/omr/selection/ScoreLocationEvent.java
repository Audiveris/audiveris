//----------------------------------------------------------------------------//
//                                                                            //
//                    S c o r e L o c a t i o n E v e n t                     //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.selection;

import omr.score.common.ScoreRectangle;

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
 * @version $Id$
 */
public class ScoreLocationEvent
    extends LocationEvent
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The location rectangle, which can be degenerated to a point when both
     * width and height values equal zero
     */
    public final ScoreRectangle rectangle;

    //~ Constructors -----------------------------------------------------------

    //--------------------//
    // ScoreLocationEvent //
    //--------------------//
    /**
     * Creates a new ScoreLocationEvent object, with value for each final data
     *
     * @param source the entity that created this event
     * @param rectangle the location within the score space
     * @param hint how the event originated
     * @param movement what is the originating mouse movement
     */
    public ScoreLocationEvent (Object         source,
                               SelectionHint  hint,
                               MouseMovement  movement,
                               ScoreRectangle rectangle)
    {
        super(source, hint, movement);
        this.rectangle = rectangle;
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getData //
    //---------//
    @Override
    public ScoreRectangle getData ()
    {
        return rectangle;
    }

    @Override
    public Rectangle getRectangle ()
    {
        return getData();
    }
}
