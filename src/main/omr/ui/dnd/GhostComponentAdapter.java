//----------------------------------------------------------------------------//
//                                                                            //
//                 G h o s t C o m p o n e n t A d a p t e r                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.dnd;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Class {@code GhostComponentAdapter} is a {@link GhostDropAdapter}
 * whose image is copied from the appearance of the component where
 * the mouse is pressed.
 *
 * @param <A> The precise type of action carried by the drop
 *
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public class GhostComponentAdapter<A>
        extends GhostDropAdapter<A>
{
    //~ Constructors -----------------------------------------------------------

    //-----------------------//
    // GhostComponentAdapter //
    //-----------------------//
    /**
     * Create a new GhostComponentAdapter object
     *
     * @param glassPane the related glasspane
     * @param action    the carried action
     */
    public GhostComponentAdapter (GhostGlassPane glassPane,
                                  A action)
    {
        super(glassPane, action);
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // mousePressed //
    //--------------//
    @Override
    public void mousePressed (MouseEvent e)
    {
        Component c = e.getComponent();

        // Copy the component current appearance
        image = new BufferedImage(
                c.getWidth(),
                c.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        Graphics g = image.getGraphics();
        c.paint(g);

        // Standard processing
        super.mousePressed(e);
    }
}
