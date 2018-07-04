//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     O m r G l a s s P a n e                                    //
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
package org.audiveris.omr.ui;

import org.audiveris.omr.ui.dnd.GhostGlassPane;
import org.audiveris.omr.ui.dnd.ScreenPoint;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code OmrGlassPane} is a GhostGlassPane to draw draggable shape plus
 * related decoration (such as a link to some neighboring entity).
 *
 * @author Hervé Bitteur
 */
public class OmrGlassPane
        extends GhostGlassPane
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Reference point relative to this glassPane. */
    private Point reference;

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Set current reference point.
     *
     * @param reference the reference (screen-based) point to set
     */
    public void setReference (ScreenPoint reference)
    {
        if (reference != null) {
            this.reference = reference.getLocalPoint(this);
        } else {
            this.reference = null;
        }
    }

    /**
     * On top of inter image, draw a link from inter center to reference point.
     *
     * @param g graphic environment
     */
    @Override
    public void paintComponent (Graphics g)
    {
        super.paintComponent(g);

        if (reference != null && overTarget) {
            g.setColor(Color.RED);
            g.drawLine(localPoint.x, localPoint.y, reference.x, reference.y);
        }
    }

    /**
     * The scene is composed of inter image plus reference point if any.
     *
     * @param center inter center
     * @return bounding box of inter + reference point if any
     */
    @Override
    protected Rectangle getSceneBounds (Point center)
    {
        Rectangle rect = getImageBounds(center);

        if (reference != null) {
            rect.add(reference);
        }

        return rect;
    }
}
