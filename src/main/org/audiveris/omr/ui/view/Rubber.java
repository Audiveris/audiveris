//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          R u b b e r                                           //
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
package org.audiveris.omr.ui.view;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.ui.Colors;
import static org.audiveris.omr.ui.selection.MouseMovement.CLICKING;
import static org.audiveris.omr.ui.selection.MouseMovement.DRAGGING;
import static org.audiveris.omr.ui.selection.MouseMovement.PRESSING;
import static org.audiveris.omr.ui.selection.MouseMovement.RELEASING;
import static org.audiveris.omr.ui.util.UIPredicates.isAdditionWanted;
import static org.audiveris.omr.ui.util.UIPredicates.isContextWanted;
import static org.audiveris.omr.ui.util.UIPredicates.isDragWanted;
import static org.audiveris.omr.ui.util.UIPredicates.isRezoomWanted;
import static org.audiveris.omr.ui.util.UIPredicates.isRubberWanted;
import org.audiveris.omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

/**
 * Class <code>Rubber</code> keeps track of nothing more than a rectangle,
 * to define an area of interest.
 * <p>
 * The rectangle can be degenerated to a simple point, when both its width and height are zero.
 * Moreover, the display can be moved or resized (see the precise triggers below).
 * <p>
 * The rubber data is rendered as a 'rubber', so the name, using a rectangle, reflecting the dragged
 * position of the mouse.
 * <p>
 * Rubber data is meant to be modified by the user when he presses and/or drags the mouse. But it
 * can also be modified programmatically, thanks to the {@link #resetOrigin} and
 * {@link #resetRectangle} methods.
 * <p>
 * Basic mouse handling is provided in the following way :
 * <ul>
 * <li>Define the point of interest. Default trigger is to click with the <b>Left</b> button.</li>
 * <li>Define the rectangle of interest. Default trigger is to keep <b>Shift</b> pressed when mouse
 * is moved.</li>
 * <li>Zoom the display to the area delimited by the rubber. Default trigger is <b>Shift +
 * Control</b> when mouse is released.</li>
 * <li>Drag the component itself. One trigger is when both <b>Left + Right</b> buttons are
 * dragged. Another one is when <b>Middle</b> button (mouse wheel) is dragged.</li>
 * </ul>
 * Note: Actual triggers are defined by protected predicate methods that can be redefined in a
 * subclass.
 * <p>
 * Mouse Events are handled in the following way:
 * <ul>
 * <li><b>Low-level events</b> originate from a JComponent, where the Rubber is registered as a
 * MouseListener and a MouseMotionListener. The component can be linked by the Rubber constructor,
 * or later by using the {@link #connectComponent} method. Rubber is then called on its
 * <i>mouseDragged, mousePressed, mouseReleased</i> methods.
 * <li><b>High-level events</b>, as computed by Rubber from low-level mouse events, are forwarded
 * to a connected {@link MouseMonitor} if any, which is then called on its <i>pointSelected,
 * pointAdded, contextSelected, rectangleSelected, rectangleZoomed</i> methods. Generally, this
 * MouseMonitor is the originating JComponent, but this is not mandatory.
 * </ul>
 * The Rubber can be linked to a {@link Zoom} to cope with display factor of the related component,
 * but this is not mandatory: If no zoom is connected, a display factor of 1.0 is assumed.
 *
 * @author Hervé Bitteur
 */
public class Rubber
        extends MouseInputAdapter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Rubber.class);

    private static AtomicInteger globalId = new AtomicInteger(0);

    /** To handle zoom through mouse wheel. */
    private static final double BASE = 2;

    private static final double INTERVALS = 5;

    /** Factor by which zoom level is increased/decreased for one increment. */
    private static final double ZOOM_FACTOR = Math.pow(BASE, 1d / INTERVALS);

    //~ Instance fields ----------------------------------------------------------------------------

    /** View from which the rubber will receive physical mouse events. */
    protected JComponent component;

    /** The controller to be notified about mouse actions. */
    protected MouseMonitor mouseMonitor;

    /** Related zoom if any. */
    protected Zoom zoom;

    // The raw (zoomed) rubber rectangle, with x & y as the original point
    // where mouse was pressed, with possibly negative width & height, and
    // may be going past the component borders.
    private Rectangle rawRect;

    // The normalized unzoomed rubber rectangle, inside the component, with
    // x & y at the top left and positive width & height.
    private Rectangle rect;

    // Normalized unzoomed vector
    private Line2D vector;

    // To ease debugging
    private final int id;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a rubber, with no predefined parameter (zoom, component) which are meant
     * to be provided later.
     *
     * @see #setZoom
     */
    public Rubber ()
    {
        id = globalId.addAndGet(1);
    }

    /**
     * Create a rubber linked to a component, with a related display zoom.
     *
     * @param component the related component
     * @param zoom      the zoom entity to handle the display zoom
     */
    public Rubber (JComponent component,
                   Zoom zoom)
    {
        id = globalId.addAndGet(1);
        connectComponent(component);
        setZoom(zoom);
    }

    /**
     * Create a rubber, with a linked zoom, the related component being linked later
     *
     * @param zoom the related zoom
     */
    public Rubber (Zoom zoom)
    {
        id = globalId.addAndGet(1);
        setZoom(zoom);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------------//
    // clampCenter //
    //-------------//
    /**
     * Maintain rubber center within component bounds.
     */
    private void clampCenter ()
    {
        final Point initial = getCenter();

        if (initial == null) {
            return;
        }

        int x = initial.x;
        int y = initial.y;

        // The center must stay within the component
        final int compWidth = unscaled(component.getWidth());
        final int compHeight = unscaled(component.getHeight());

        if (x < 0) {
            x = 0;
        } else if (x > compWidth) {
            x = compWidth;
        }

        if (y < 0) {
            y = 0;
        } else if (y > compHeight) {
            y = compHeight;
        }

        rect.translate(x - initial.x, y - initial.y);
    }

    //------------------//
    // connectComponent //
    //------------------//
    /**
     * Actually register the rubber as the mouse listener for the provided component.
     *
     * @param component the related component
     */
    public final void connectComponent (JComponent component)
    {
        // Clean up if needed
        disconnectComponent(this.component);

        // Remember the related component (to get visible rect, etc ...)
        this.component = component;

        if (component != null) {
            // To be notified of mouse clicks
            component.removeMouseListener(this); // No multiple notifications
            component.addMouseListener(this);

            // To be notified of mouse mouvements
            component.removeMouseMotionListener(this); // No multiple notifs
            component.addMouseMotionListener(this);

            // To be notified of mouse  wheel mouvements
            component.removeMouseWheelListener(this); // No multiple notifs
            component.addMouseWheelListener(this);
        }
    }

    //---------------//
    // cropRectangle //
    //---------------//
    /**
     * Crop rubber rectangle with component bounds.
     */
    private void cropRectangle ()
    {
        // The origin must stay within the component
        final int compWidth = unscaled(component.getWidth());
        final int compHeight = unscaled(component.getHeight());

        if (rect.x < 0) {
            rect.x = 0;
        } else if (rect.x > compWidth) {
            rect.x = compWidth;
        }

        if (rect.y < 0) {
            rect.y = 0;
        } else if (rect.y > compHeight) {
            rect.y = compHeight;
        }

        // The rectangle shouldn't extend past the drawing area.
        if ((rect.x + rect.width) > compWidth) {
            rect.width = compWidth - rect.x;
        }

        if ((rect.y + rect.height) > compHeight) {
            rect.height = compHeight - rect.y;
        }
    }

    //---------------------//
    // disconnectComponent //
    //---------------------//
    /**
     * Disconnect the provided component
     *
     * @param component the component to disconnect
     */
    private void disconnectComponent (JComponent component)
    {
        if (component != null) {
            component.removeMouseListener(this);
            component.removeMouseMotionListener(this);
            component.removeMouseWheelListener(this);
        }
    }

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Return the center of the model rectangle as defined by the rubber.
     * This is a de-zoomed point, should the component have a related zoom.
     *
     * @return the model center point
     */
    public Point getCenter ()
    {
        if (rect != null) {
            return new Point(rect.x + (rect.width / 2), rect.y + (rect.height / 2));
        } else {
            return null;
        }
    }

    //--------------//
    // getRectangle //
    //--------------//
    /**
     * Return the model rectangle defined by the rubber.
     * This is a de-zoomed rectangle, should the component have a related zoom.
     *
     * @return the model rectangle
     */
    public Rectangle getRectangle ()
    {
        return rect;
    }

    //-----------------//
    // modifyZoomRatio //
    //-----------------//
    /**
     * Adjust the zoom ratio, according to the provided increment value.
     * <ul>
     * <li>+1 to increase
     * <li>-1 to decrease
     * <li>0 to reset to 100%
     * </ul>
     *
     * @param increment how to modify
     */
    public void modifyZoomRatio (int increment)
    {
        double ratio = zoom.getRatio();

        switch (increment) {
        case -1:
            ratio /= ZOOM_FACTOR;

            break;

        case 0:
            ratio = 1.0;

            break;

        case +1:
            ratio *= ZOOM_FACTOR;

            break;

        default:
            return;
        }

        zoom.setRatio(ratio);
    }

    //--------------//
    // mouseClicked //
    //--------------//
    /**
     * Called when the mouse is clicked.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseClicked (MouseEvent e)
    {
        if (mouseMonitor == null) {
            return;
        }

        if (e.getClickCount() == 2) {
            mouseMonitor.objectSelected(getCenter(), CLICKING);
        }
    }

    //--------------//
    // mouseDragged //
    //--------------//
    /**
     * Called when the mouse is dragged.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseDragged (MouseEvent e)
    {
        if (mouseMonitor == null) {
            return;
        }

        setCursor(e);

        if (isDragWanted(e)) {
            final Rectangle vr = component.getVisibleRect();
            vr.setBounds(
                    (vr.x + rawRect.x) - e.getX(),
                    (vr.y + rawRect.y) - e.getY(),
                    vr.width,
                    vr.height);
            SwingUtilities.invokeLater( () -> component.scrollRectToVisible(vr));
        } else if (isRubberWanted(e)) {
            updateSize(e);
            mouseMonitor.rectangleSelected(rect, DRAGGING);
        } else {
            // Behavior equivalent to simple selection
            reset(e);

            if (isAdditionWanted(e)) {
                if (isContextWanted(e)) {
                    mouseMonitor.contextAdded(getCenter(), DRAGGING);
                } else {
                    mouseMonitor.pointAdded(getCenter(), DRAGGING);
                }
            } else if (isContextWanted(e)) {
                mouseMonitor.contextSelected(getCenter(), DRAGGING);
            } else {
                mouseMonitor.pointSelected(getCenter(), DRAGGING);
            }
        }
    }

    //--------------//
    // mousePressed //
    //--------------//
    /**
     * Called when the mouse is pressed.
     *
     * @param e the mouse event
     */
    @Override
    public void mousePressed (MouseEvent e)
    {
        reset(e);

        if (mouseMonitor == null) {
            return;
        }

        setCursor(e);

        if (!isDragWanted(e)) {
            if (isAdditionWanted(e)) {
                if (isContextWanted(e)) {
                    mouseMonitor.contextAdded(getCenter(), PRESSING);
                } else {
                    mouseMonitor.pointAdded(getCenter(), PRESSING);
                }
            } else if (isContextWanted(e)) {
                mouseMonitor.contextSelected(getCenter(), PRESSING);
            } else {
                mouseMonitor.pointSelected(getCenter(), PRESSING);
            }
        }
    }

    //---------------//
    // mouseReleased //
    //---------------//
    /**
     * Called when the mouse is released.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseReleased (MouseEvent e)
    {
        if (mouseMonitor == null) {
            return;
        }

        try {
            if (isRezoomWanted(e)) {
                updateSize(e);
                mouseMonitor.rectangleZoomed(rect, RELEASING);
            } else if (isDragWanted(e)) {
                Rectangle vr = component.getVisibleRect();
                rawRect.setBounds(vr.x + (vr.width / 2), vr.y + (vr.height / 2), 0, 0);
                normalize();
            } else if (isAdditionWanted(e)) {
                if (isContextWanted(e)) {
                    mouseMonitor.contextAdded(getCenter(), RELEASING);
                } else {
                    mouseMonitor.pointAdded(getCenter(), RELEASING);
                }
            } else if (rect != null) {
                if (isContextWanted(e)) {
                    mouseMonitor.contextSelected(getCenter(), RELEASING);
                } else if ((rect.width != 0) && (rect.height != 0)) {
                    updateSize(e);
                    mouseMonitor.rectangleSelected(rect, RELEASING);
                } else {
                    mouseMonitor.pointSelected(getCenter(), RELEASING);
                }
            }

            e.getComponent().setCursor(Cursor.getDefaultCursor());
        } catch (Exception ex) {
            logger.warn("Error in mouseReleased " + ex, ex);
        }
    }

    //-----------------//
    // mouseWheelMoved //
    //-----------------//
    /**
     * Called when the mouse wheel is moved.
     * If CTRL key is down, modify current zoom ratio accordingly, otherwise
     * forward the wheel event to proper container (JScrollPane usually).
     *
     * @param e the mouse wheel event
     */
    @Override
    public void mouseWheelMoved (MouseWheelEvent e)
    {
        // CTRL is down?
        if (e.isControlDown()) {
            double ratio = zoom.getRatio();

            if (e.getWheelRotation() > 0) {
                ratio /= ZOOM_FACTOR;
            } else {
                ratio *= ZOOM_FACTOR;
            }

            zoom.setRatio(ratio);
        } else {
            // Forward event to some container of the component?
            Container container = component.getParent();

            while (container != null) {
                if (container instanceof JComponent) {
                    JComponent comp = (JComponent) container;
                    MouseWheelListener[] listeners = comp.getMouseWheelListeners();

                    if (listeners.length > 0) {
                        for (MouseWheelListener listener : listeners) {
                            listener.mouseWheelMoved(e);
                        }

                        return;
                    }
                }

                container = container.getParent();
            }
        }
    }

    //-----------//
    // normalize //
    //-----------//
    private void normalize ()
    {
        if (rect == null) {
            rect = new Rectangle(
                    unscaled(rawRect.x),
                    unscaled(rawRect.y),
                    unscaled(rawRect.width),
                    unscaled(rawRect.height));
        } else {
            rect.setBounds(
                    unscaled(rawRect.x),
                    unscaled(rawRect.y),
                    unscaled(rawRect.width),
                    unscaled(rawRect.height));
        }

        // The x & y are the original coordinates when mouse began
        // But width & height may be negative
        // Make the width and height positive, if necessary.
        if (rect.width < 0) {
            rect.width = -rect.width;
            rect.x = rect.x - rect.width + 1;

            if (rect.x < 0) {
                rect.width += rect.x;
                rect.x = 0;
            }
        }

        if (rect.height < 0) {
            rect.height = -rect.height;
            rect.y = rect.y - rect.height + 1;

            if (rect.y < 0) {
                rect.height += rect.y;
                rect.y = 0;
            }
        }

        // Crop rect within the component
        cropRectangle();
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the rubber rectangle. This should be called late, typically
     * when everything else has already been painted. Note that this needs an
     * un-scaled graphics, since we want to draw the rubber lines (vertical
     * and horizontal) perfectly on top of image pixels.
     *
     * @param unscaledGraphics the graphic context (not transformed!)
     */
    public void render (Graphics unscaledGraphics)
    {
        if (rect != null) {
            Graphics2D g = (Graphics2D) unscaledGraphics.create();

            Rectangle r = new Rectangle(rect);

            if (zoom != null) {
                zoom.scale(r);
            }

            // Is this is a true rectangle ?
            if ((r.width != 0) || (r.height != 0)) {
                g.setColor(Colors.RUBBER_RECT);
                g.drawRect(r.x, r.y, r.width, r.height);
            }

            // Draw horizontal & vertical rules (point or rectangle)
            g.setColor(Colors.RUBBER_RULE);

            int x = scaled(rect.x + (rect.width / 2));
            int y = scaled(rect.y + (rect.height / 2));
            float pixelSize = scaled(1);

            if (pixelSize < 1) {
                pixelSize = 1f;
            } else {
                int halfPen = (int) Math.rint(pixelSize / 2);
                x += halfPen;
                y += halfPen;
            }

            Stroke s = new BasicStroke(pixelSize);
            g.setStroke(s);

            if (constants.showCross.getValue()) {
                // Draw just a small cross
                int legLength = constants.crossLegLength.getValue();
                g.drawLine(x, y - legLength, x, y + legLength); // Vertical
                g.drawLine(x - legLength, y, x + legLength, y); // Horizontal
            } else {
                // Draw full vertical & horizontal lines
                Rectangle bounds = component.getBounds(null);
                g.drawLine(x, 0, x, bounds.height); // Vertical
                g.drawLine(0, y, bounds.width, y); // Horizontal
            }

            // Vector
            if (vector != null) {
                Line2D v = new Line2D.Double(
                        vector.getX1(),
                        vector.getY1(),
                        vector.getX2(),
                        vector.getY2());

                if (zoom != null) {
                    zoom.scale(v);
                }

                UIUtil.setAbsoluteStroke(g, 1f);
                g.setColor(Color.BLACK);
                g.draw(v);
            }

            g.dispose();
        }
    }

    //-------//
    // reset //
    //-------//
    private void reset (MouseEvent e)
    {
        if (rawRect == null) {
            rawRect = new Rectangle(e.getX(), e.getY(), 0, 0);
        } else {
            rawRect.setBounds(e.getX(), e.getY(), 0, 0);
        }

        normalize();
        resetOrigin(rect.x, rect.y);
    }

    //-------------//
    // resetOrigin //
    //-------------//
    /**
     * Reset the rubber information, programmatically.
     *
     * @param x the new center abscissa
     * @param y the new center ordinate
     */
    public void resetOrigin (int x,
                             int y)
    {
        if (rect == null) {
            rect = new Rectangle(x, y, 0, 0);
        } else {
            rect.setBounds(x, y, 0, 0);
        }

        if (component != null) {
            component.repaint();
        }
    }

    //----------------//
    // resetRectangle //
    //----------------//
    /**
     * Reset the rubber information, programmatically.
     *
     * @param newRect the new rectangle, which can be null
     */
    public void resetRectangle (Rectangle newRect)
    {
        if (newRect != null) {
            if (rect == null) {
                rect = new Rectangle(newRect);
            } else {
                rect.setBounds(newRect);
            }
        } else {
            rect = null;
        }
    }

    //--------//
    // scaled //
    //--------//
    private int scaled (int val)
    {
        if (zoom != null) {
            return zoom.scaled(val);
        } else {
            return val;
        }
    }

    //-----------//
    // setCursor //
    //-----------//
    private void setCursor (MouseEvent e)
    {
        if (isDragWanted(e)) {
            e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        } else if (isAdditionWanted(e)) {
            e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else if (isContextWanted(e)) {
            e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (isRubberWanted(e)) {
            e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
        } else if (isRezoomWanted(e)) {
            e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
        } else {
            e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    //-----------------//
    // setMouseMonitor //
    //-----------------//
    /**
     * Define the interface of callback to be notified of mouse events
     *
     * @param mouseMonitor the entity to be notified
     */
    public void setMouseMonitor (MouseMonitor mouseMonitor)
    {
        this.mouseMonitor = mouseMonitor;
    }

    //---------//
    // setZoom //
    //---------//
    /**
     * Allows to specify that a zoom is attached to the displayed
     * component, and thus the reported rectangle or center must be
     * de-zoomed on the fly.
     *
     * @param zoom the component related zoom
     */
    public final void setZoom (Zoom zoom)
    {
        this.zoom = zoom;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Rubber #" + id + " " + rect + "}";
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Move rubber according to the provided translation vector.
     *
     * @param dx abscissa move
     * @param dy ordinate move
     */
    public void translate (int dx,
                           int dy)
    {
        if (rect == null) {
            return;
        }

        rect.translate(dx, dy);
        clampCenter();

        if (component != null) {
            component.repaint();
        }
    }

    //----------//
    // unscaled //
    //----------//
    private int unscaled (int val)
    {
        if (zoom != null) {
            return zoom.truncUnscaled(val);
        } else {
            return val;
        }
    }

    //------------//
    // updateSize //
    //------------//
    private void updateSize (MouseEvent e)
    {
        // Update width and height of rawRect
        rawRect.setSize(e.getX() - rawRect.x, e.getY() - rawRect.y);
        normalize();

        // Repaint the component (with the resized rectangle)
        component.repaint();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean showCross = new Constant.Boolean(
                true,
                "Should we show just a cross for rubber (or whole lines)");

        private final Constant.Integer crossLegLength = new Constant.Integer(
                "Pixels",
                100,
                "Length for each leg of the rubber cross");
    }
}
