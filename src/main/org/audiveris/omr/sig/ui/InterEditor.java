//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I n t e r E d i t o r                                     //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>InterEditor</code> allows to edit an Inter instance set into edit mode.
 * <p>
 * Edition means the ability to modify the underlying inter, by shifting or resizing it.
 * Creation of an InterEditor instance, can start in two different ways:
 * <ul>
 * <li>From an existing inter, by a double-click or by opening the edit mode.
 * <li>By clicking on a location while the repetitive mode is on.
 * This creates a new inter at current location and sets it immediately in edition mode.
 * Dragging operates on the global handle.
 * </ul>
 * The edited inter cannot leave its initial system (as opposed to an {@link InterDnd}) but view
 * can be shifted if needed when the inter gets close to view border.
 * <p>
 * An InterEditor handles 2 versions of the same data model that is used to modify Inter:
 * <ol>
 * <li>The "original" version which is recorded at edition start.
 * <li>The "latest" version which always corresponds to the latest modification.
 * </ol>
 * This kind of data is kept internal to the editor, it <b>can't be shared live</b> with inter,
 * because it is used to set the inter either to the original (undo) or the latest (do or redo)
 * version.
 * <p>
 * Handles are specific points that the user can select and move via mouse or keyboard.
 * Handles can share live points with latest data version for convenience but this is not necessary.
 * <p>
 * <img alt="Edition diagram" src="doc-files/Editor.png">
 *
 * @see InterDnd
 *
 * @author Hervé Bitteur
 */
public abstract class InterEditor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(InterEditor.class);

    private static final Constants constants = new Constants();

    /** Radius for detection. */
    private static final double HANDLE_DETECTION_RADIUS = constants.handleDetectionRadius.getValue();

    /** Half side of handle icon square. */
    private static final double HANDLE_HALF_SIDE = constants.handleHalfSide.getValue();

    /** Radius of handle square corners. */
    private static final double HANDLE_ARC_RADIUS = constants.handleArcRadius.getValue();

    //~ Instance fields ----------------------------------------------------------------------------
    /** The underlying inter. */
    protected final Inter inter;

    /** Containing system. */
    protected final SystemInfo system;

    /** Tracker to render inter and its decorations. */
    private final InterTracker tracker;

    /** List of handles. */
    protected final List<Handle> handles = new ArrayList<>();

    /** Currently selected handle. */
    protected Handle selectedHandle;

    /** Original glyph, if any. */
    protected Glyph originalGlyph;

    /** Current last point. */
    protected Point lastPoint;

    /** Has the mouse been actually moved since being pressed?. */
    protected boolean hasMoved = false;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new <code>InterEditor</code> object.
     *
     * @param inter the inter instance being edited
     */
    protected InterEditor (Inter inter)
    {
        this.inter = inter;

        if (inter.getSig() != null) {
            system = inter.getSig().getSystem();
        } else if (inter.getStaff() != null) {
            system = inter.getStaff().getSystem();
        } else {
            system = null;
        }

        // Tracker
        tracker = inter.getTracker(system.getSheet());
        tracker.setSystem(system);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // endProcess //
    //------------//
    /**
     * End of edition, triggered by keyboard Enter or by mouse pointing outside a handle.
     */
    public void endProcess ()
    {
        logger.debug("End of edition");

        if (hasMoved) {
            system.getSheet().getInterController().editInter(this);
        }

        inter.getSig().publish(inter); // To update the edit checkbox on interboard
        system.getSheet().getSheetEditor().closeEditMode();
    }

    //----------//
    // getInter //
    //----------//
    /**
     * Report the inter being edited.
     *
     * @return the edited inter
     */
    public Inter getInter ()
    {
        return inter;
    }

    //--------------//
    // processMouse //
    //--------------//
    /**
     * Process user mouse action.
     *
     * @param pt       current mouse location
     * @param movement (PRESSING, DRAGGING or RELEASING)
     * @return true if editor is still active (and thus consumes user input)
     */
    public boolean processMouse (Point pt,
                                 MouseMovement movement)
    {
        boolean active = false;

        if (null != movement) {
            switch (movement) {
            case PRESSING:
                // Drag must start within a handle vicinity
                selectedHandle = null;
                lastPoint = null;

                // Find closest handle
                double bestSq = Double.MAX_VALUE;
                Handle bestHandle = null;

                for (Handle handle : handles) {
                    double sq = handle.getHandleCenter().distanceSq(pt);

                    if (((bestHandle == null) || (sq < bestSq))) {
                        bestHandle = handle;
                        bestSq = sq;
                    }
                }

                if ((bestHandle != null) && (bestHandle.contains(pt))) {
                    selectedHandle = bestHandle;
                    lastPoint = pt;
                    active = true;

                    // Keep underlying inter as selected
                    inter.getSig().publish(inter, SelectionHint.ENTITY_TRANSIENT);
                } else {
                    endProcess();
                }

                hasMoved = false;

                break;

            case DRAGGING:

                if (selectedHandle != null) {
                    if (lastPoint != null) {
                        Point vector = PointUtil.subtraction(pt, lastPoint);
                        moveHandle(vector);
                    } else {
                        lastPoint = pt;
                    }

                    active = true;
                } else {
                    lastPoint = null;
                    hasMoved = false;
                }

                break;

            case RELEASING:

                if (hasMoved) {
                    // Perhaps switch selection to a specific handle
                    switchHandleOnRelease();
                }

                lastPoint = null;
                active = true;

                break;

            default:
                break;
            }
        }

        return active;
    }

    //-----------------//
    // processKeyboard //
    //-----------------//
    /**
     * Process user keyboard action.
     *
     * @param vector shift of a handle
     */
    public void processKeyboard (Point vector)
    {
        if (selectedHandle == null) {
            return;
        }

        if (lastPoint == null) {
            lastPoint = PointUtil.rounded(selectedHandle.getHandleCenter());
        }

        moveHandle(vector);
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the editor handles onto the provided graphics, together with current status
     * inter and its decorations (support links, needed ledgers).
     *
     * @param g provided graphics.
     *          (this can be the sheet zoomed and scrolled view, or the global glass pane)
     */
    public void render (Graphics2D g)
    {
        final SIGraph sig = inter.getSig();

        if (sig == null) {
            return;
        }

        // Inter with decorations, etc.
        tracker.render(g);

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

    //------------//
    // moveHandle //
    //------------//
    /**
     * Move the selected handle, along the desired vector.
     * <p>
     * The move can be limited due to handle specific limitations (horizontal, vertical) or to
     * system area.
     *
     * @param vector desired move
     */
    private void moveHandle (Point vector)
    {
        if ((vector.x != 0) || (vector.y != 0)) {
            // Make sure we stay within system area
            Point newPt = PointUtil.addition(lastPoint, vector);

            if (system.getArea().contains(newPt)) {
                // Move the selected handle
                if (selectedHandle.move(vector)) {
                    hasMoved = true;
                    lastPoint = newPt;
                    doit();
                }
            } else {
                // Remain on last point
                system.getSheet().getLocationService().publish(
                        new LocationEvent(
                                this,
                                SelectionHint.ENTITY_TRANSIENT,
                                MouseMovement.DRAGGING,
                                new Rectangle(lastPoint)));
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Handle //
    //--------//
    /**
     * A <code>Handle</code> represents a control point in the geometry of an inter.
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
         * Create a <code>InterHandle</code> object.
         *
         * @param center absolute coordinates of handle center
         */
        public Handle (Point2D center)
        {
            this.handleCenter = center;
        }

        /**
         * Report whether this handle contains the provided point.
         *
         * @param pt the provided point
         * @return true if the provided point lies within detection distance from handle center
         */
        public boolean contains (Point pt)
        {
            return handleCenter.distanceSq(pt)
                   <= (HANDLE_DETECTION_RADIUS * HANDLE_DETECTION_RADIUS);
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
         * Apply the handle move, depending on the role of this handle, to data model.
         * <p>
         * This method is called only if vector length is not 0.
         *
         * @param vector handle translation given by mouse movement or keyboard arrow increment
         * @return true if some real move has been performed
         */
        public abstract boolean move (Point vector);

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
            final RoundRectangle2D square = new RoundRectangle2D.Double(
                    handleCenter.getX() - halfSide,
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
