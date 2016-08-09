//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                           G h o s t C o m p o n e n t A d a p t e r                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.ui.dnd;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Class {@code GhostComponentAdapter} is a {@link GhostDropAdapter} whose image is
 * copied from the appearance of the component where the mouse is pressed.
 *
 * @param <A> The precise type of action carried by the drop
 *
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public class GhostComponentAdapter<A>
        extends GhostDropAdapter<A>
{
    //~ Constructors -------------------------------------------------------------------------------

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

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // mousePressed //
    //--------------//
    @Override
    public void mousePressed (MouseEvent e)
    {
        Component c = e.getComponent();

        // Copy the component current appearance
        image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics g = image.getGraphics();
        c.paint(g);

        // Standard processing
        super.mousePressed(e);
    }
}
