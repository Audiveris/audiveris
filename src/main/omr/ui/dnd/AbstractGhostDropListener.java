//----------------------------------------------------------------------------//
//                                                                            //
//             A b s t r a c t G h o s t D r o p L i s t e n e r              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.dnd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;

/**
 * Class {@code AbstractGhostDropListener} is a default implementation
 * for the {@link GhostDropListener} interface.
 *
 * @param <A> The type of action carried by the drop
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public abstract class AbstractGhostDropListener<A>
        implements GhostDropListener<A>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            AbstractGhostDropListener.class);

    //~ Instance fields --------------------------------------------------------
    /** The related component */
    protected JComponent component;

    //~ Constructors -----------------------------------------------------------
    //---------------------------//
    // AbstractGhostDropListener //
    //---------------------------//
    /**
     * Create a new AbstractGhostDropListener object
     *
     * @param component the related component
     */
    public AbstractGhostDropListener (JComponent component)
    {
        this.component = component;
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // dropped //
    //---------//
    /*
     * Default (void) implementation of the processing of a drop event
     */
    @Override
    public void dropped (GhostDropEvent<A> e)
    {
        // Empty by default
    }

    //---------------//
    // getLocalPoint //
    //---------------//
    /**
     * Report the local point wrt the listener component of a screen-based point
     *
     * @param screenPoint the screen-based point
     * @return the component-based point
     */
    protected Point getLocalPoint (ScreenPoint screenPoint)
    {
        return screenPoint.getLocalPoint(component);
    }

    //------------//
    // isInTarget //
    //------------//
    /**
     * Check whether the provided local point lies within the component
     * bounds
     *
     * @param localPoint the local point (component-based)
     * @return true if point is over the component, false otherwise
     */
    protected boolean isInTarget (Point localPoint)
    {
        Rectangle bounds = new Rectangle(
                0,
                0,
                component.getWidth(),
                component.getHeight());

        return bounds.contains(localPoint);
    }
}
