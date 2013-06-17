//----------------------------------------------------------------------------//
//                                                                            //
//                      G h o s t D r o p A d a p t e r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.dnd;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code GhostDropAdapter} is a MouseAdapter specifically meant
 * for handling {@link GhostDropEvent} instances.
 *
 * @param <A> the precise type for action carried by the drop
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public abstract class GhostDropAdapter<A>
        extends MouseAdapter
{
    //~ Instance fields --------------------------------------------------------

    /** The related glasspane */
    protected final GhostGlassPane glassPane;

    /** The registered listeners */
    private final Set<GhostDropListener<A>> listeners = new HashSet<>();

    /** The event-carried action */
    protected A action;

    /** The image to be displayed on the glasspane */
    protected BufferedImage image;

    //~ Constructors -----------------------------------------------------------
    //------------------//
    // GhostDropAdapter //
    //------------------//
    /**
     * Create a new GhostDropAdapter object
     *
     * @param glassPane the related glasspane
     * @param action    the carried action
     */
    public GhostDropAdapter (GhostGlassPane glassPane,
                             A action)
    {
        this.glassPane = glassPane;
        this.action = action;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // addDropListener //
    //-----------------//
    /**
     * Register a drop listener
     *
     * @param listener the listener to registrate
     */
    public void addDropListener (GhostDropListener<A> listener)
    {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    //----------//
    // getImage //
    //----------//
    public BufferedImage getImage ()
    {
        return image;
    }

    //--------------//
    // mousePressed //
    //--------------//
    @Override
    public void mousePressed (MouseEvent e)
    {
        glassPane.setVisible(true);

        ScreenPoint screenPoint = new ScreenPoint(
                e.getXOnScreen(),
                e.getYOnScreen());

        glassPane.setImage(image);
        glassPane.setPoint(screenPoint);
    }

    //---------------//
    // mouseReleased //
    //---------------//
    @Override
    public void mouseReleased (MouseEvent e)
    {
        ScreenPoint screenPoint = new ScreenPoint(
                e.getXOnScreen(),
                e.getYOnScreen());

        glassPane.setVisible(false);
        glassPane.setImage(null);

        fireDropEvent(new GhostDropEvent<>(action, screenPoint));
    }

    //--------------------//
    // removeDropListener //
    //--------------------//
    /**
     * Unregister a drop listener
     *
     * @param listener the listener to remove
     */
    public void removeDropListener (GhostDropListener<A> listener)
    {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    //---------------//
    // fireDropEvent //
    //---------------//
    /**
     * Forward the provided drop event to all registered listeners
     *
     * @param event the drop event to forward
     */
    protected void fireDropEvent (GhostDropEvent<A> event)
    {
        for (GhostDropListener<A> listener : listeners) {
            listener.dropped(event);
        }
    }
}
