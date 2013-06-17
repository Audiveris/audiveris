//----------------------------------------------------------------------------//
//                                                                            //
//                    G h o s t M o t i o n A d a p t e r                     //
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Class {@code GhostMotionAdapter} is a special MouseMotion adapter
 * meant for dragging the ghost image.
 *
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public class GhostMotionAdapter
        extends MouseMotionAdapter
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            GhostMotionAdapter.class);

    //~ Instance fields --------------------------------------------------------
    /** The related glasspane */
    protected GhostGlassPane glassPane;

    //~ Constructors -----------------------------------------------------------
    //--------------------//
    // GhostMotionAdapter //
    //--------------------//
    /**
     * Create a new GhostMotionAdapter object
     *
     * @param glassPane The related glasspane
     */
    public GhostMotionAdapter (GhostGlassPane glassPane)
    {
        this.glassPane = glassPane;
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // mouseDragged //
    //--------------//
    /**
     * In this default implementation, we don't modify the current image, we
     * simply tell the glassPane where to redraw the image
     *
     * @param e the mouse event
     */
    @Override
    public void mouseDragged (MouseEvent e)
    {
        Point absPt = e.getLocationOnScreen();
        glassPane.setPoint(new ScreenPoint(absPt.x, absPt.y));
    }
}
