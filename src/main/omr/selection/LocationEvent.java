//----------------------------------------------------------------------------//
//                                                                            //
//                         L o c a t i o n E v e n t                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;

/**
 * Class {@code LocationEvent} is UI Event that represents a new
 * location (a rectangle, perhaps degenerated to a point) within the
 * Sheet coordinates space.
 *
 * @author Hervé Bitteur
 */
public class LocationEvent
        extends UserEvent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            LocationEvent.class);

    //~ Instance fields --------------------------------------------------------
    /**
     * The location rectangle, which can be degenerated to a point when both
     * width and height values equal zero
     */
    private final Rectangle rectangle;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new LocationEvent object.
     *
     * @param source    the actual entity that created this event
     * @param hint      how the event originated
     * @param movement  the precise mouse movement
     * @param rectangle the location within the sheet space
     */
    public LocationEvent (Object source,
                          SelectionHint hint,
                          MouseMovement movement,
                          Rectangle rectangle)
    {
        super(source, hint, movement);
        this.rectangle = rectangle;
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public Rectangle getData ()
    {
        return rectangle;
    }
}
