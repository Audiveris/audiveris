//----------------------------------------------------------------------------//
//                                                                            //
//                    G h o s t M o t i o n A d a p t e r                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.dnd;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Class {@code GhostMotionAdapter} is a special MouseMotion adapter meant for
 * dragging the ghost image.
 *
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public class GhostMotionAdapter
    extends MouseMotionAdapter
{
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
    @Override
    public void mouseDragged (MouseEvent e)
    {
        // Determine proper glasspane-based location of the mouse
        ScreenPoint screenPoint = new ScreenPoint(
            e.getComponent(),
            e.getPoint());
        glassPane.setPoint(screenPoint.getLocalPoint(glassPane));
        glassPane.repaint();
    }
}
