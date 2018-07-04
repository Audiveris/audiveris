//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                G h o s t D r o p A d a p t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui.dnd;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class {@code GhostDropAdapter} is a MouseAdapter specifically meant for handling
 * {@link GhostDropEvent} instances.
 *
 * @param <A> the precise type for action carried by the drop
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public abstract class GhostDropAdapter<A>
        extends MouseAdapter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The related glasspane */
    protected final GhostGlassPane glassPane;

    /** The registered listeners */
    private final Set<GhostDropListener<A>> listeners = new LinkedHashSet<GhostDropListener<A>>();

    /** The event-carried action */
    protected A action;

    /** The image to be displayed on the glasspane */
    protected BufferedImage image;

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // addDropListener //
    //-----------------//
    /**
     * Register a drop listener
     *
     * @param listener the listener to register
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

        ScreenPoint screenPoint = new ScreenPoint(e.getXOnScreen(), e.getYOnScreen());

        glassPane.setImage(image);
        glassPane.setPoint(screenPoint);
    }

    //---------------//
    // mouseReleased //
    //---------------//
    @Override
    public void mouseReleased (MouseEvent e)
    {
        ScreenPoint screenPoint = new ScreenPoint(e.getXOnScreen(), e.getYOnScreen());

        glassPane.setVisible(false);
        glassPane.setImage(null);

        fireDropEvent(new GhostDropEvent<A>(action, screenPoint));
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
