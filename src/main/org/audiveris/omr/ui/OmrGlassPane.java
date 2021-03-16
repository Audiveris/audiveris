//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     O m r G l a s s P a n e                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.sheet.curve.Curves;
import org.audiveris.omr.sig.ui.InterDnd;
import org.audiveris.omr.ui.dnd.GhostGlassPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

/**
 * Class {@code OmrGlassPane} is a GhostGlassPane to draw draggable shape.
 * <p>
 * Whenever possible over the sheet target, we draws the ghost inter, plus staff reference plus
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

    /** Current inter Dnd operation, if any. */
    private InterDnd interDnd;

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
        if ((draggedImage == null) || (localPoint == null)) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;

        if (overTarget) {
            // Use composition with display underneath
            g2.setComposite(targetComposite);

            if (interDnd == null || !interDnd.hasReference()) {
                Rectangle rect = getImageBounds(localPoint);
                g2.drawImage(draggedImage, null, rect.x, rect.y);
            } else {
                // Draw (w/ proper transform) staff reference, inter, decorations
                final AffineTransform saveAT = g2.getTransform();
                g2.transform(targetTransform);

                interDnd.render(g2);

                g2.setTransform(saveAT);
            }
        } else {
            g2.setComposite(nonTargetComposite);

            Rectangle rect = getImageBounds(localPoint);
            g2.drawImage(draggedImage, null, rect.x, rect.y);
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
        // Use image bounds
        Rectangle rect = super.getSceneBounds(center);

        if (interDnd != null && overTarget) {
            // Use inter decorations, etc (this depends on staff reference availability)
            Rectangle box = interDnd.getSceneBounds();

            if (box != null) {
                rect.add(targetTransform.createTransformedShape(box).getBounds());
            }
        }

        // To cope with curve thickness, not taken into account in inter bounds
        if (targetTransform != null) {
            double ratio = targetTransform.getScaleX();
            int margin = (int) Math.ceil(ratio * Curves.DEFAULT_THICKNESS / 2.0);
            rect.grow(margin, margin);
        }

        return rect;
    }

    //-------------//
    // setInterDnd //
    //-------------//
    /**
     * Activate inter Dnd operation.
     *
     * @param interDnd the inter-based Dnd operation, or null
     */
    public void setInterDnd (InterDnd interDnd)
    {
        this.interDnd = interDnd;
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
