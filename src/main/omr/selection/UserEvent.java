//----------------------------------------------------------------------------//
//                                                                            //
//                             U s e r E v e n t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.log.Logger;

/**
 * Interface <code>UserEvent</code> defines the common behavior of user events
 * that are stored as selections, and handled by the EventBus.
 *
 * <p>All events are subclasses of this abstract class, and managed by proper
 * EventService instances: <ul>
 *
 * <li>SheetManager event service: a singleton which handles THE currently
 * selected sheet as {@link SheetEvent}
 *
 * <li>Sheet event service: each Sheet instance handles {@link
 * ScoreLocationEvent}, {@link SheetLocationEvent} and {@link PixelLevelEvent}
 *
 * <li>Lag event service: each Lag instance handles {@link RunEvent}, {@link
 * SectionEvent} and {@link SectionIdEvent} (all are subclasses of {@link
 * LagEvent})
 *
 * <li>GlyphLag: each GlyphLag instance adds to its Lag event service the
 * handling of {@link GlyphEvent}, {@link GlyphIdEvent} and {@link
 * GlyphSetEvent} (all are subclasses of {@link GlyphLagEvent})
 *
 * </ul>
 *
 * <p>All event classes are documented as follows:</p>
 *
 * <dl> <dt><b>Publishers:</b><dd>All classes that publish this event (using
 * {@link org.bushe.swing.event.EventService#subscribeStrongly})
 *
 * <dt><b>Subscribers:</b><dd>All classes that subscribe to this event (and
 * implement the {@link org.bushe.swing.event.EventSubscriber#onEvent} method)
 *
 * <dt><b>Readers:</b><dd>All classes that simply read this event (using the
 * {@link org.bushe.swing.event.EventService#getLastEvent} method) </dl>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class UserEvent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(UserEvent.class);

    //~ Instance fields --------------------------------------------------------

    /** The entity which created this event */
    public final Object source;

    /** Hint about the event origin (can be null) */
    public final SelectionHint hint;

    /** Precise user mouse action (can be null) */
    public final MouseMovement movement;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // UserEvent //
    //-----------//
    /**
     * Creates a new UserEvent object.
     *
     * @param source the (non null) entity that created this event
     * @param hint hint about the origin
     * @param movement the originating mouse movement
     */
    public UserEvent (Object        source,
                      SelectionHint hint,
                      MouseMovement movement)
    {
        if (source == null) {
            throw new IllegalArgumentException("event source cannot be null");
        }

        this.source = source;
        this.hint = hint;
        this.movement = movement;

        //        logger.warning(
        //            ClassUtil.nameOf(this) + " created by:" + source);
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getData //
    //---------//
    /**
     * Report the data conveyed by this event
     * @return the conveyed data (which may be null)
     */
    public abstract Object getData ();

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());
        sb.append(" src:")
          .append(source);

        if (hint != null) {
            sb.append(" ")
              .append(hint);
        }

        if (movement != null) {
            sb.append(" ")
              .append(movement);
        }

        sb.append(" ")
          .append(internalString());
        sb.append("}");

        return sb.toString();
    }

    //----------------//
    // internalString //
    //----------------//
    /**
     * Report a string that describes the internals of the specific subclass
     * @return the (sub)class internals as a string
     */
    protected String internalString ()
    {
        if (getData() != null) {
            return getData()
                       .toString();
        } else {
            return "";
        }
    }
}
