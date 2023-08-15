//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              G h o s t M o t i o n A d a p t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Class <code>GhostMotionAdapter</code> is a special MouseMotion adapter meant for dragging
 * the ghost image.
 *
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public class GhostMotionAdapter
        extends MouseMotionAdapter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GhostMotionAdapter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The related glass pane. */
    protected GhostGlassPane glassPane;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a new GhostMotionAdapter object
     *
     * @param glassPane The related glass pane
     */
    public GhostMotionAdapter (GhostGlassPane glassPane)
    {
        this.glassPane = glassPane;
    }

    //~ Methods ------------------------------------------------------------------------------------

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
        final ScreenPoint screenPoint = new ScreenPoint(e.getXOnScreen(), e.getYOnScreen());
        glassPane.setScreenPoint(screenPoint);
    }
}
