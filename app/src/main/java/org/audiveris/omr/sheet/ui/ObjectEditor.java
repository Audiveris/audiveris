//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     O b j e c t E d i t o r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.ui.InterDnd;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.ui.view.Zoom;

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
 * Class <code>ObjectEditor</code> allows to edit a graphical object set into an edit mode.
 * <p>
 * Editing means the ability to modify the underlying object, by shifting or resizing it.
 * The edited object cannot leave its initial system (as opposed to an {@link InterDnd}) but view
 * can be shifted if needed when the object gets close to view border.
 * <p>
 * An ObjectEditor handles 2 versions of the same data model that is used to modify the object:
 * <ol>
 * <li>The "original" version which is recorded at editing start.
 * <li>The "latest" version which always corresponds to the latest modification.
 * </ol>
 * This kind of data is kept internal to the editor, it <b>can't be shared live</b> with the object,
 * because it is used to set the object either to the original (undo) or the latest (do or redo)
 * version.
 * <p>
 * Handles are specific points that the user can select and move via mouse or keyboard.
 * Handles can share live points with latest data version for convenience but this is not necessary.
 *
 * @author Hervé Bitteur
 */
public abstract class ObjectEditor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ObjectEditor.class);

    private static final Constants constants = new Constants();

    /** Radius for detection. */
    private static final double HANDLE_DETECTION_RADIUS = constants.handleDetectionRadius
            .getValue();

    /** Half side of handle icon square. */
    private static final double HANDLE_HALF_SIDE = constants.handleHalfSide.getValue();

    /** Radius of handle square corners. */
    private static final double HANDLE_ARC_RADIUS = constants.handleArcRadius.getValue();

    //~ Instance fields ----------------------------------------------------------------------------

    /** The underlying object being edited. */
    protected final Object object;

    /** Containing system. */
    protected SystemInfo system;

    /** List of handles. */
    protected final List<Handle> handles = new ArrayList<>();

    /** Currently selected handle. */
    protected Handle selectedHandle;

    /** Previous mouse location. */
    protected Point lastPt;

    /** Has the mouse been actually moved since being pressed?. */
    protected boolean hasMoved = false;

    /** Current zoom. */
    protected Zoom zoom;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a new <code>ObjectEditor</code> object.
     *
     * @param object the object to edit
     * @param system the containing system
     */
    public ObjectEditor (Object object,
                         SystemInfo system)
    {
        this.object = object;
        this.system = system;

        zoom = system.getSheet().getSheetEditor().getSheetView().getZoom();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //------//
    // doit //
    //------//
    /**
     * Apply (modified) model to object geometry.
     * <p>
     * This method is called only if a non-zero move has occurred on the selected handle.
     */
    protected void doit ()
    {
        // Void
    }

    //------------//
    // endProcess //
    //------------//
    /**
     * End of editing, triggered by keyboard Enter or by mouse pointing outside a handle.
     */
    public void endProcess ()
    {
        // Void
    }

    //-----------//
    // finalDoit //
    //-----------//
    /**
     * Apply final (modified) model to object geometry.
     * <p>
     * This method is called at end of editing.
     */
    public void finalDoit ()
    {
        doit(); //By default
    }

    //-----------//
    // getObject //
    //-----------//
    /**
     * Report the object being edited.
     *
     * @return the edited object
     */
    public Object getObject ()
    {
        return object;
    }

    //-----------//
    // getSystem //
    //-----------//
    public SystemInfo getSystem ()
    {
        return system;
    }

    //------------//
    // moveHandle //
    //------------//
    /**
     * Move the selected handle, according to user drag.
     * <p>
     * The move can be limited due to handle specific limitations (horizontal, vertical) or to
     * system area.
     *
     * @param dx desired move in x
     * @param dy desired move in y
     */
    protected void moveHandle (int dx,
                               int dy)
    {
        if ((dx != 0) || (dy != 0)) {
            final Point newPt = new Point(lastPt.x + dx, lastPt.y + dy);

            // Make sure we stay within system area
            if (system.getArea().contains(newPt)) {
                // Move the selected handle
                if (selectedHandle.move(dx, dy)) {
                    hasMoved = true;
                    lastPt = newPt;
                    doit(); // Apply modified model to the object position/geometry
                }
            } else {
                // Remain on last point
                system.getSheet().getLocationService().publish(
                        new LocationEvent(
                                this,
                                SelectionHint.ENTITY_TRANSIENT,
                                MouseMovement.DRAGGING,
                                new Rectangle(lastPt)));
            }
        }
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

        if (lastPt == null) {
            lastPt = PointUtil.rounded(selectedHandle.getPoint());
        }

        moveHandle(vector.x, vector.y);
    }

    //--------------//
    // processMouse //
    //--------------//
    /**
     * Process user mouse action.
     *
     * @param pt       new mouse location
     * @param movement (PRESSING, DRAGGING or RELEASING)
     * @return true if editor is still active (and thus consumes user input)
     */
    public boolean processMouse (Point pt,
                                 MouseMovement movement)
    {
        boolean active = false;

        switch (movement) {
            case PRESSING -> {
                // Drag must start within a handle vicinity
                selectedHandle = null;
                lastPt = null;

                // Find closest handle
                double bestSq = Double.MAX_VALUE;
                Handle bestHandle = null;

                for (Handle handle : handles) {
                    double sq = handle.getPoint().distanceSq(pt);

                    if (((bestHandle == null) || (sq < bestSq))) {
                        bestHandle = handle;
                        bestSq = sq;
                    }
                }

                if ((bestHandle != null) && (bestHandle.contains(pt, zoom))) {
                    selectedHandle = bestHandle;
                    lastPt = pt;
                    active = true;

                    // Keep underlying object as selected
                    publish();
                } else {
                    endProcess();
                }

                hasMoved = false;
            }

            case DRAGGING -> {
                if (selectedHandle != null) {
                    if (lastPt != null) {
                        final Point vector = PointUtil.subtraction(pt, lastPt);
                        moveHandle(vector.x, vector.y);
                    } else {
                        lastPt = pt;
                    }

                    active = true;
                } else {
                    lastPt = null;
                    hasMoved = false;
                }
            }

            case RELEASING -> {
                if (hasMoved) {
                    // Perhaps switch selection to a specific handle
                    switchHandleOnRelease();
                }

                lastPt = null;
                active = true;
            }

            case null, default -> {}
        }

        return active;
    }

    //---------//
    // publish //
    //---------//
    protected void publish ()
    {
        // Void
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the editor handles onto the provided graphics.
     *
     * @param g provided graphics.
     *          (this can be the sheet zoomed and scrolled view, or the global glass pane)
     */
    public void render (Graphics2D g)
    {
        // Each handle rectangle
        g.setColor(Colors.EDITING_HANDLE);
        UIUtil.setAbsoluteStroke(g, 1.5f);

        for (Handle handle : handles) {
            handle.render(g, handle == selectedHandle);
        }
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return new StringBuilder(getClass().getSimpleName()).append('{').append(object).append('}')
                .toString();
    }

    //------//
    // undo //
    //------//
    /**
     * Re-apply original model to object geometry.
     */
    public void undo ()
    {
        // Void
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Ratio handleMaxZoom = new Constant.Ratio(
                2.0,
                "Maximum effective zoom on handle");

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

    //--------//
    // Handle //
    //--------//
    /**
     * A <code>Handle</code> represents a control point in the geometry of an inter.
     * <p>
     * If is rendered as a small rounded square and can be moved by the user via mouse or keyboard.
     * <p>
     * Any movement of the handle impacts the symbol geometry (position or shape) according to the
     * role of the underlying control point with respect to the symbol.
     */
    public static abstract class Handle
    {
        /** Handle center point. */
        protected final Point2D center;

        /**
         * Create a <code>Handle</code> object with a reference to live center point.
         *
         * @param center (live) center point, using absolute coordinates
         */
        public Handle (Point2D center)
        {
            this.center = center;
        }

        /**
         * Report whether this handle contains the provided point.
         *
         * @param pt   the provided point
         * @param zoom current display zoom
         * @return true if the provided point lies within handle square
         */
        public boolean contains (Point pt,
                                 Zoom zoom)
        {
            final double ratio = Math.min(constants.handleMaxZoom.getValue(), zoom.getRatio());
            final double maxDist = HANDLE_DETECTION_RADIUS / ratio;
            final double dx = Math.abs(pt.x - center.getX());
            final double dy = Math.abs(pt.y - center.getY());
            final double dist = Math.max(dx, dy);

            return dist <= maxDist;
        }

        /**
         * Report (live) handle center point.
         *
         * @return (live) handle center
         */
        public Point2D getPoint ()
        {
            return center;
        }

        /**
         * Apply the suggested handle move (based on user location translation) to data model,
         * depending on the role of this handle.
         *
         * @param dx abscissa translation of user location
         * @param dy ordinate translation of user location
         * @return true if some real move has been performed
         */
        public abstract boolean move (int dx,
                                      int dy);

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
            final double zoom = Math.min(
                    constants.handleMaxZoom.getValue(),
                    g.getTransform().getScaleX());
            final double halfSide = HANDLE_HALF_SIDE / zoom;
            final double arcRadius = HANDLE_ARC_RADIUS / zoom;
            final RoundRectangle2D square = new RoundRectangle2D.Double(
                    center.getX() - halfSide,
                    center.getY() - halfSide,
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
            sb.append("@").append(center);
            sb.append('}');

            return sb.toString();
        }
    }
}
