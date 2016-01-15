//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       U s e r E v e n t                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.selection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface {@code UserEvent} defines the common behavior of user events that are
 * stored as selections, and handled by the EventBus.
 *
 * @author Hervé Bitteur
 */
public abstract class UserEvent
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(UserEvent.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The entity which created this event (cannot be null). */
    public final Object source;

    /** Hint about the event origin (can be null). */
    public final SelectionHint hint;

    /** Precise user mouse action (can be null). */
    public MouseMovement movement;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new UserEvent object.
     *
     * @param source   the (non null) entity that created this event
     * @param hint     hint about the origin
     * @param movement the originating mouse movement
     */
    public UserEvent (Object source,
                      SelectionHint hint,
                      MouseMovement movement)
    {
        if (source == null) {
            throw new IllegalArgumentException("event source cannot be null");
        }

        this.source = source;
        this.hint = hint;
        this.movement = movement;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    /**
     * Report the data conveyed by this event.
     *
     * @return the conveyed data (which may be null)
     */
    public abstract Object getData ();

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append(" src:").append(source);

        if (hint != null) {
            sb.append(" ").append(hint);
        }

        if (movement != null) {
            sb.append(" ").append(movement);
        }

        sb.append(" ").append(internalString());
        sb.append("}");

        return sb.toString();
    }

    //----------------//
    // internalString //
    //----------------//
    /**
     * Report a string about the internals of the specific subclass.
     *
     * @return the (sub)class internals as a string
     */
    protected String internalString ()
    {
        if (getData() != null) {
            return getData().toString();
        } else {
            return "";
        }
    }
}
