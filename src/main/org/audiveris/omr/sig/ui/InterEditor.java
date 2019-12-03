//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I n t e r E d i t o r                                     //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code InterEditor} allows to edit an Inter instance set into edit mode.
 * <p>
 * Edition means the ability to modify the underlying inter, by shifting or resizing it.
 * <p>
 * An InterEditor handles 2 versions of the same data model that is used to modify Inter:
 * <ol>
 * <li>An "original" version which is recorded at edition start.
 * <li>A "latest" version which always corresponds to the latest modification.
 * </ol>
 * This kind of data is kept internal to the editor, it <b>can't be shared live</b> with inter,
 * because it is used to set the inter either to the original (undo) or the latest (do or redo)
 * version.
 * <p>
 * Handles are specific points that the user can select and move via mouse or keyboard.
 * Handles can share live points with latest data version for convenience but this is not necessary.
 *
 * @author Hervé Bitteur
 */
public abstract class InterEditor
{

    private static final Logger logger = LoggerFactory.getLogger(InterEditor.class);

    private static final Constants constants = new Constants();

    /** Radius for detection. */
    private static final double HANDLE_DETECTION_RADIUS = constants.handleDetectionRadius.getValue();

    /** Half side of handle icon square. */
    private static final double HANDLE_HALF_SIDE = constants.handleHalfSide.getValue();

    /** Radius of handle square corners. */
    private static final double HANDLE_ARC_RADIUS = constants.handleArcRadius.getValue();

    /** The underlying inter. */
    protected final Inter inter;

    /** Original glyph, if any. */
    protected Glyph originalGlyph;

    /** List of handles. */
    protected final List<Handle> handles = new ArrayList<>();

    /** Currently selected handle. */
    protected Handle selectedHandle;

    /** Current last reference point. */
    private Point lastReference;

    /** Has the mouse been actually moved since being pressed?. */
    private boolean hasMoved = false;

    /**
     * Create a new {@code InterEditor} object.
     *
     * @param inter the inter instance being edited
     */
    protected InterEditor (Inter inter)
    {
        this.inter = inter;
    }

    //----------//
    // getInter //
    //----------//
    public Inter getInter ()
    {
        return inter;
    }

    //---------//
    // process //
    //---------//
    /**
     * Process user mouse action.
     *
     * @param pt       current mouse location
     * @param movement (PRESSING, DRAGGING or RELEASING)
     * @return true if editor is still active (and thus consumes user input)
     */
    public boolean process (Point pt,
                            MouseMovement movement)
    {
        boolean active = false;

        if (null != movement) {
            switch (movement) {
            case PRESSING:
                // Drag must start within a handle vicinity
                selectedHandle = null;
                lastReference = null;

                // Find closest handle
                double bestSq = Double.MAX_VALUE;
                Handle bestHandle = null;

                for (Handle handle : handles) {
                    double sq = handle.getHandleCenter().distanceSq(pt);

                    if ((bestHandle == null || sq < bestSq)) {
                        bestHandle = handle;
                        bestSq = sq;
                    }
                }

                if ((bestHandle != null) && (bestHandle.contains(pt))) {
                    selectedHandle = bestHandle;
                    lastReference = pt;
                    active = true;

                    // Keep underlying inter as selected
                    inter.getSig().publish(inter, SelectionHint.ENTITY_TRANSIENT);
                } else {
                    processEnd();
                }

                hasMoved = false;

                break;
            case DRAGGING:
                if (selectedHandle != null) {
                    if (lastReference != null) {
                        Point vector = PointUtil.subtraction(pt, lastReference);

                        if ((vector.x != 0) || (vector.y != 0)) {
                            if (selectedHandle.applyMove(vector)) {
                                doit();
                                hasMoved = true;
                            }
                        }
                    }

                    lastReference = pt;
                    active = true;
                } else {
                    lastReference = null;
                    hasMoved = false;
                }

                break;
            case RELEASING:
                if (hasMoved) {
                    // Perhaps switch selection to a specific handle
                    switchHandleOnRelease();
                }

                lastReference = null;
                active = true;

                break;
            default:
                break;
            }
        }

        return active;
    }

    //---------//
    // process //
    //---------//
    /**
     * Process user keyboard action.
     *
     * @param vector shift of a handle
     */
    public void process (Point vector)
    {
        if (selectedHandle == null) {
            return;
        }

        if ((vector.x != 0) || (vector.y != 0)) {
            if (selectedHandle.applyMove(vector)) {
                hasMoved = true;
                doit();
            }
        }
    }

    //------------//
    // processEnd //
    //------------//
    /**
     * End of edition, triggered by keyboard Enter or by pointing outside a handle.
     */
    public void processEnd ()
    {
        logger.debug("End of edition");
        final Sheet sheet = inter.getSig().getSystem().getSheet();

        if (hasMoved) {
            sheet.getInterController().editInter(this);
        }

        inter.getSig().publish(inter); // To update the edit checkbox on interboard
        sheet.getSymbolsEditor().closeEditMode();
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the editor handles onto the provided graphics, together with current status
     * inter and its decorations (support links, needed ledgers).
     *
     * @param g provided graphics
     */
    public void render (Graphics2D g)
    {
        final SIGraph sig = inter.getSig();

        if (sig == null) {
            return;
        }

        final SystemInfo system = sig.getSystem();
        final InterTracker tracker = inter.getTracker(system.getSheet());
        tracker.setSystem(system);
        tracker.render(g, true);

        // Each handle rectangle
        g.setColor(Colors.EDITION_HANDLE);
        UIUtil.setAbsoluteStroke(g, 1.5f);

        for (Handle handle : handles) {
            handle.render(g, handle == selectedHandle);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('{');
        sb.append(inter);
        sb.append('}');
        return sb.toString();
    }

    //------//
    // doit //
    //------//
    /**
     * Set internal inter data, now that handle(s) have worked.
     * <p>
     * This method is called only if a real move has been performed.
     */
    protected void doit ()
    {
        if (inter.getGlyph() != null) {
            originalGlyph = inter.getGlyph();
        }

        // Since inter has moved, link to its glyph is no longer valid
        inter.setGlyph(null);

        updateEnsemble();
    }

    //------//
    // undo //
    //------//
    /**
     * Reset internal inter data.
     */
    public void undo ()
    {
        if (originalGlyph != null) {
            inter.setGlyph(originalGlyph);
        }

        // Lookup attachments are no longer consistent, we can simply remove them
        // They will be recreated anew on next edition action
        inter.removeAttachments("");

        updateEnsemble();
    }

    //-----------------------//
    // switchHandleOnRelease //
    //-----------------------//
    /**
     * Triggered on mouse release after a move, to potentially switch selectedHandle.
     */
    protected void switchHandleOnRelease ()
    {
        // Void by default
    }

    //----------------//
    // updateEnsemble //
    //----------------//
    /**
     * Update bounds of containing ensemble, if any.
     */
    protected void updateEnsemble ()
    {
        // Update ensemble bounds
        Inter ens = inter.getEnsemble();

        if (ens != null) {
            ens.setBounds(null);
        }
    }

    //--------//
    // Handle //
    //--------//
    /**
     * A {@code Handle} represents a control point in the geometry of an inter.
     * <p>
     * If is rendered as a small square and can be moved by the user via mouse or keyboard.
     * <p>
     * Any movement of the handle impacts the symbol geometry (position or shape) according to the
     * role of the underlying control point with respect to the symbol.
     */
    public abstract class Handle
    {

        /** Handle center point. */
        protected final Point2D handleCenter;

        /**
         * Create a {@code InterHandle} object.
         *
         * @param center absolute coordinates of handle center
         */
        public Handle (Point2D center)
        {
            this.handleCenter = center;
        }

        /**
         * Apply the handle move, depending on the role of this handle, to data model.
         * <p>
         * This method is called only if vector length is not 0.
         *
         * @param vector handle translation given by mouse movement or keyboard arrow increment
         * @return true if some real move has been performed
         */
        public abstract boolean applyMove (Point vector);

        /**
         * Report whether this handle contains the provided point.
         *
         * @param pt the provided point
         * @return true if the provided point lies within detection distance from handle center
         */
        public boolean contains (Point pt)
        {
            return handleCenter.distanceSq(pt) <= HANDLE_DETECTION_RADIUS * HANDLE_DETECTION_RADIUS;
        }

        /**
         * Report (live) handle center point.
         *
         * @return handle center
         */
        public Point2D getHandleCenter ()
        {
            return handleCenter;
        }

        /**
         * Render this handle as a rounded rectangle centered on the control point.
         *
         * @param g          graphics context
         * @param isSelected true if this handle is the selected one
         */
        public void render (Graphics2D g,
                            boolean isSelected)
        {
            // Draw handle rectangle with a fixed size, regardless of current zoom of score view
            final double zoom = g.getTransform().getScaleX();
            final double halfSide = HANDLE_HALF_SIDE / zoom;
            final double arcRadius = HANDLE_ARC_RADIUS / zoom;
            final RoundRectangle2D square
                    = new RoundRectangle2D.Double(handleCenter.getX() - halfSide,
                                                  handleCenter.getY() - halfSide,
                                                  2 * halfSide,
                                                  2 * halfSide,
                                                  arcRadius,
                                                  arcRadius);

            if (isSelected) {
                g.fill(square);
            }

            g.draw(square);
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("Handle{");
            sb.append("@").append(handleCenter);
            sb.append(" for ").append(inter);
            sb.append('}');

            return sb.toString();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Double handleDetectionRadius = new Constant.Double(
                "pixels",
                6.0,
                "Detection radius around inter handle");

        private final Constant.Double handleHalfSide = new Constant.Double(
                "pixels",
                4.0,
                "Half side of handle rounded rectangle");

        private final Constant.Double handleArcRadius = new Constant.Double(
                "pixels",
                3.0,
                "Arc radius at each corner of handle rounded rectangle");
    }
}
