//----------------------------------------------------------------------------//
//                                                                            //
//                      G h o s t D r o p A d a p t e r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.dnd;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code GhostDropAdapter} is a MouseAdapter specifically meant for
 * handling {@link GhostDropEvent} instances.
 *
 * @param <A> the precise type for action carried by the drop
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public abstract class GhostDropAdapter<A>
    extends MouseAdapter
{
    //~ Instance fields --------------------------------------------------------

    /** The related glasspane */
    protected GhostGlassPane glassPane;

    /** The event-carried action */
    protected A action;

    /** The image to be displayed on the glasspane */
    protected BufferedImage image;

    /** The registered listeners */
    private List<GhostDropListener<A>> listeners;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // GhostDropAdapter //
    //------------------//
    /**
     * Create a new GhostDropAdapter object
     *
     * @param glassPane the related glasspane
     * @param action the carried action
     */
    public GhostDropAdapter (GhostGlassPane glassPane,
                             A              action)
    {
        this.glassPane = glassPane;
        this.action = action;
        this.listeners = new ArrayList<GhostDropListener<A>>();
    }

    //~ Methods ----------------------------------------------------------------

    //----------------------//
    // addGhostDropListener //
    //----------------------//
    /**
     * Register a drop listener
     * @param listener the listener to registrate
     */
    public void addGhostDropListener (GhostDropListener<A> listener)
    {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    //--------------//
    // mousePressed //
    //--------------//
    @Override
    public void mousePressed (MouseEvent e)
    {
        Component c = e.getComponent();

        glassPane.setVisible(true);

        // Determine the proper location (glasspane-based)
        ScreenPoint screenPoint = new ScreenPoint(c, e.getPoint());
        glassPane.setPoint(screenPoint.getLocalPoint(glassPane));
        glassPane.setImage(image);
        glassPane.repaint();
    }

    //---------------//
    // mouseReleased //
    //---------------//
    @Override
    public void mouseReleased (MouseEvent e)
    {
        ScreenPoint eventPoint = new ScreenPoint(
            e.getComponent(),
            e.getPoint());

        glassPane.setPoint(eventPoint.getLocalPoint(glassPane));
        glassPane.setVisible(false);
        glassPane.setImage(null);

        fireGhostDropEvent(new GhostDropEvent<A>(action, eventPoint));
    }

    //-------------------------//
    // removeGhostDropListener //
    //-------------------------//
    /**
     * Unregister a drop listener
     * @param listener the listener to remove
     */
    public void removeGhostDropListener (GhostDropListener listener)
    {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    //--------------------//
    // fireGhostDropEvent //
    //--------------------//
    /**
     * Forward the provided drop event to all registered listeners
     * @param event the drop event to forward
     */
    protected void fireGhostDropEvent (GhostDropEvent<A> event)
    {
        for (GhostDropListener<A> listener : listeners) {
            listener.ghostDropped(event);
        }
    }
}
