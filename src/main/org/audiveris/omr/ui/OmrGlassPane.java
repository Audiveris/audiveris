//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     O m r G l a s s P a n e                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import org.audiveris.omr.sig.ui.InterTracker;
import org.audiveris.omr.ui.dnd.GhostGlassPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

/**
 * Class {@code OmrGlassPane} is a GhostGlassPane to draw draggable shape plus
 * related decoration (such as links to some neighboring entities, or ledgers for heads).
 *
 * @author Hervé Bitteur
 */
public class OmrGlassPane
        extends GhostGlassPane
{

    private static final Logger logger = LoggerFactory.getLogger(OmrGlassPane.class);

    /** The transform to apply when painting on top of target (zoomed sheet view). */
    private AffineTransform targetTransform;

    /** Current staff reference point (glass-based), if any. */
    private Point staffReference;

    /** Current ghost tracker, if any. */
    private InterTracker ghostTracker;

    //----------------//
    // paintComponent //
    //----------------//
    /**
     * Paint inter image, plus a line from inter center to staff reference point if any,
     * plus inter decorations if any.
     *
     * @param g graphic environment
     */
    @Override
    public void paintComponent (Graphics g)
    {
        // Paint inter (the symbol image in fact)
        super.paintComponent(g);

        if ((staffReference != null) && overTarget) {
            // Draw line to staff reference
            g.setColor(Color.RED);
            g.drawLine(localPoint.x, localPoint.y, staffReference.x, staffReference.y);

            if (ghostTracker != null) {
                final Graphics2D g2 = (Graphics2D) g;
                final AffineTransform saveAT = g2.getTransform();
                g2.transform(targetTransform);

                // Draw inter decorations
                ghostTracker.render(g2, false /* renderInter */);

                g2.setTransform(saveAT);
            }
        }
    }

    //----------------//
    // getSceneBounds //
    //----------------//
    /**
     * The scene is composed of inter image plus its decorations if any
     * (staff reference, support links, ledgers).
     *
     * @param center inter center (glass-based)
     * @return bounding box of inter + reference point + decorations if any
     */
    @Override
    protected Rectangle getSceneBounds (Point center)
    {
        Rectangle rect = super.getSceneBounds(center);

        if (staffReference != null && overTarget) {
            rect.add(staffReference);

            if (ghostTracker != null) {
                // Use inter decorations
                Rectangle box = ghostTracker.getSceneBounds();

                if (box != null) {
                    rect.add(targetTransform.createTransformedShape(box).getBounds());
                }
            }
        }

        return rect;
    }

    //-----------------//
    // setGhostTracker //
    //-----------------//
    /**
     * Activate inter tracking.
     *
     * @param ghostTracker the inter-based ghostTracker, or null
     */
    public void setGhostTracker (InterTracker ghostTracker)
    {
        this.ghostTracker = ghostTracker;
    }

    //-------------------//
    // setStaffReference //
    //-------------------//
    /**
     * Set current staff reference point.
     *
     * @param staffReference the reference (glass-based) point to set
     */
    public void setStaffReference (Point staffReference)
    {
        this.staffReference = staffReference;
    }

    //--------------------//
    // setTargetTransform //
    //--------------------//
    /**
     * Remember the affine transform to operate on glass graphics when drawing ghost
     * related informations (such as ghost links).
     *
     * @param targetTransform the transform to use
     */
    public void setTargetTransform (AffineTransform targetTransform)
    {
        this.targetTransform = targetTransform;
    }
}
