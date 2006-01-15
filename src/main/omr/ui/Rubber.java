//-----------------------------------------------------------------------//
//                                                                       //
//                              R u b b e r                              //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.util.Logger;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import static java.awt.event.InputEvent.*;

/**
 * Class <code>Rubber</code> keeps track of nothing more than a rectangle,
 * to define an area of interest. The rectangle can be degenerated to a
 * simple point, when both its width and height are zero. Morover, the
 * display can be moved or resized (see the precise triggers below).
 *
 * <p> The rubber data is rendered as a 'rubber', so the name, using a
 * rectangle in inverted video, reflecting the dragged position of the
 * mouse.
 *
 * <p> Rubber data is meant to be modified by the user when he presses
 * and/or drags the mouse. But it can also be modified programmatically,
 * thanks to the {@link #resetOrigin} and {@link #resetRectangle} methods.
 *
 * <p> Basic mouse handling is provided in the following way : <ul>
 *
 * <li> Define the point of interest. Default trigger is to click with the
 * <b>Left</b> button. </li>
 *
 * <li> Define the rectangle of interest. Default trigger is to keep
 * <b>Shift</b> pressed when mouse is moved. </li>
 *
 * <li> Zoom the display to the area delimited by the rubber. Default
 * trigger is <b>Shift + Control</b> when mouse is released. </li>
 *
 * <li> Drag the component itself. Default trigger is when both <b>Left +
 * Right</b> buttons are dragged. </li> </ul>
 *
 * <p/> Note: Actual triggers are defined by protected predicate methods
 * that can be redefined in a subclass.  <p/>
 *
 * <p> Mouse Events are handled in the following way: <ul>
 *
 * <li> <b>Low-level events</b> originate from a JComponent, where the
 * Rubber is registered as a MouseListener and a MouseMotionListener. The
 * component can be linked by the Rubber constructor, or later by using the
 * {@link #setComponent} method. Rubber is then called on its
 * <i>mouseDragged, mousePressed, mouseReleased</i> methods.
 *
 * <li> <b>High-level events</b>, as computed by Rubber from low-level
 * mouse events, are forwarded to a connected {@link MouseMonitor} if any,
 * which is then called on its <i>pointSelected, rectangleSelected,
 * rectangleZoomed</i> methods. Generally, this MouseMonitor is the
 * originating JComponent, but this is not mandatory.  </ul>
 *
 * <p> The Rubber can be linked to a {@link Zoom} to cope with display
 * factor of the related component, but this is not mandatory: If no zoom
 * is connected, a display factor of 1.0 is assumed.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Rubber
    extends MouseInputAdapter
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(Rubber.class);

    // Length in pixels of mark half legs
    private static final int leg = 20;

    // Color used for drawing horizontal & vertical rules
    private static final Color ruleColor = new Color(255, 200, 0);

    //~ Instance variables ------------------------------------------------

    /** JComponent partner from which the rubber will receive physical
        mouse events */
    protected JComponent component;

    /** The entity to be notified about mouse actions (can be the
        originating component) */
    protected MouseMonitor mouseMonitor;

    /** Related zoom if any */
    protected Zoom zoom;

    // The raw (zoomed) rubber rectangle, with x & y as the original point
    // where mouse was pressed, with possibly negative width & height, and
    // may be going past the component borders
    private Rectangle rawRect;

    // The normalized unzoomed rubber rectangle, inside the component, with
    // x & y at the top left and positive width & height
    private Rectangle rect;

    //~ Constructors ------------------------------------------------------

    //--------//
    // Rubber //
    //--------//
    /**
     * Create a rubber, which will be linked to a component later, and
     * which will have a zoom attached later
     *
     * @see #setComponent
     * @see #setZoom
     */
    public Rubber ()
    {
    }

    //--------//
    // Rubber //
    //--------//
    /**
     * Create a rubber, with a linked zoom, the related component being
     * linked later
     *
     * @param zoom the related zoom
     * @see #setComponent
     */
    public Rubber (Zoom zoom)
    {
        this.zoom = zoom;
    }

    //--------//
    // Rubber //
    //--------//
    /**
     * Create a rubber linked to a component, and no zoom initially
     *
     * @param component the related component
     * @see #setZoom
     */
    public Rubber (JComponent component)
    {
        setComponent(component);
    }

    //--------//
    // Rubber //
    //--------//
    /**
     * Create a rubber linked to a component, with a related display zoom.
     *
     * @param component the related component
     * @param zoom      the zoom entity to handle the display zoom
     */
    public Rubber (JComponent component,
                   Zoom zoom)
    {
        setComponent(component);
        setZoom(zoom);
    }

    //~ Methods -----------------------------------------------------------

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

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Return the center of the model rectangle as defined by the
     * rubber. This is a dezoomed point, should the component have a
     * related zoom.
     *
     * @return the model center point
     */
    public Point getCenter ()
    {
        Point pt = null;

        if (rect != null) {
            pt = new Point(rect.x + (rect.width / 2),
                           rect.y + (rect.height / 2));
        }

        return pt;
    }

    //--------------//
    // setComponent //
    //--------------//
    /**
     * Actually register the rubber as the mouse listener for the provided
     * component.
     *
     * @param component the related component
     */
    public void setComponent (JComponent component)
    {
        // Remember the related component (to get visible rect, etc ...)
        this.component = component;

        // To be notified of mouse clicks
        component.removeMouseListener(this); // No multiple notifications
        component.addMouseListener(this);

        // To be notified of mouse mouvements
        component.removeMouseMotionListener(this); // No multiple notifs
        component.addMouseMotionListener(this);
    }

    //--------------//
    // getRectangle //
    //--------------//
    /**
     * Return the model rectangle defined by the rubber.  This is a
     * dezoomed rectangle, should the component have a related zoom.
     *
     * @return the model rectangle
     */
    public Rectangle getRectangle ()
    {
        return rect;
    }

    //---------//
    // setZoom //
    //---------//
    /**
     * Allows to specify that a zoom is attached to the displayed
     * component, and thus the reported rectangle or center must be
     * dezoomed on the fly.
     *
     * @param zoom the component related zoom
     */
    public void setZoom (Zoom zoom)
    {
        this.zoom = zoom;
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
        if (isDragWanted(e)) {
            final Rectangle vr = component.getVisibleRect();
            vr.setBounds(vr.x + rawRect.x - e.getX(),
                         vr.y + rawRect.y - e.getY(),
                         vr.width,
                         vr.height);
            SwingUtilities.invokeLater(new Runnable()
                {
                    public void run ()
                    {
                        component.scrollRectToVisible(vr);
                    }
                });
        } else if (isRubberWanted(e)) {
            updateSize(e);
            if (mouseMonitor != null) {
                mouseMonitor.rectangleSelected(e, rect);
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
        if (logger.isDebugEnabled()) {
            logger.debug("\nmousePressed : " + InputEvent.getModifiersExText(e.getModifiersEx()));
        }

        reset(e);

        if (mouseMonitor != null) {
            if (isDragWanted(e)) {
                // Nothing
            } else if (isAdditionWanted(e)) {
                mouseMonitor.pointAdded(e, getCenter());
            } else if (isContextWanted(e)) {
                mouseMonitor.contextSelected(e, getCenter());
            } else {
                mouseMonitor.pointSelected(e, getCenter());
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
        if (mouseMonitor != null) {
            if (isRezoomWanted(e)) {
                updateSize(e);
                mouseMonitor.rectangleZoomed(e, rect);
            } else if (rect != null && rect.width != 0 && rect.height != 0) {
                updateSize(e);
                mouseMonitor.rectangleSelected(e, rect);
            } else if (isDragWanted(e)) {
                Rectangle vr = component.getVisibleRect();
                rawRect.setBounds(vr.x + (vr.width / 2),
                                  vr.y + (vr.height / 2), 0, 0);
                normalize();
            } else {
                reset(e);
            }
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the rubber rectangle. This should be called late, typically
     * when everything else has already been painted.
     *
     * @param g the graphic context
     */
    public void render (Graphics g)
    {
        if (rect != null) {
            g.setXORMode(Color.white);

            // Is this is a true rectangle ?
            if (rect.width != 0 || rect.height != 0) {
                g.setColor(Color.black);
                g.drawRect(scaled(rect.x), scaled(rect.y),
                           scaled(rect.width), scaled(rect.height));
            }

            // Draw horizontal & vertical rules (point or rectangle)
            g.setColor(ruleColor);
            Rectangle vr = component.getVisibleRect();
            int x = scaled(rect.x + rect.width/2);
            int y = scaled(rect.y + rect.height/2);
            g.drawLine(x, vr.y, x, vr.y + vr.height);
            g.drawLine(vr.x, y, vr.x + vr.width, y);
        }
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

        if (component != null) {
            component.repaint();
        }
    }

    //-- protected access -------------------------------------------------

    //--------------//
    // isDragWanted //
    //--------------//
    /**
     * Predicate to check whether the zoomed display must be dragged.  This
     * method can simply be overridden to adapt to another policy.  Default
     * is to have both left and right buttons pressed when moving.
     *
     * @param e the mouse event to check
     *
     * @return true if drag is desired
     */
    protected boolean isDragWanted (MouseEvent e)
    {
        int onmask = BUTTON1_DOWN_MASK | BUTTON3_DOWN_MASK;
        int offmask = 0;

        return (e.getModifiersEx() & (onmask | offmask)) == onmask;
    }

    //----------------//
    // isRezoomWanted //
    //----------------//
    /**
     * Predicate to check if the display should be rezoomed to fit as close
     * as possible to the rubber definition. Default is to have both Shift
     * and Control keys pressed when the mouse is released.
     *
     * @param e the mouse context
     *
     * @return the predicate result
     */
    protected boolean isRezoomWanted (MouseEvent e)
    {
        return
            e.isControlDown() &&
            e.isShiftDown();
    }

    //----------------//
    // isRubberWanted //
    //----------------//
    /**
     * Predicate to check if the rubber must be extended while the mouse is
     * being moved. Default is the typical pressing of Shift key while
     * moving the mouse.
     *
     * @param e the mouse context
     *
     * @return the predicate result
     */
    protected boolean isRubberWanted (MouseEvent e)
    {
        int onmask = BUTTON1_DOWN_MASK;
        int offmask = BUTTON2_DOWN_MASK | BUTTON3_DOWN_MASK;

        return (e.getModifiersEx() & (onmask | offmask)) == onmask;

    }

    //-----------------//
    // isContextWanted //
    //-----------------//
    /**
     * Predicate to check if a context selection is wanted. Default is the
     * typical pressing with Right button only.
     *
     * @param e the mouse context
     *
     * @return the predicate result
     */
    protected boolean isContextWanted (MouseEvent e)
    {
        return
            SwingUtilities.isRightMouseButton(e) &&
            !SwingUtilities.isLeftMouseButton(e);
    }

    //------------------//
    // isAdditionWanted //
    //------------------//
    /**
     * Predicate to check if an additional selection is wanted. Default is
     * the typical selection (left button), while control key is pressed.
     *
     * @param e the mouse context
     *
     * @return the predicate result
     */
    protected boolean isAdditionWanted (MouseEvent e)
    {
        return
            !SwingUtilities.isRightMouseButton(e) &&
            SwingUtilities.isLeftMouseButton(e) &&
            e.isControlDown();
    }

    //-- private access ---------------------------------------------------

    //-----------//
    // normalize //
    //-----------//
    private void normalize ()
    {
        if (rect == null) {
            rect = new Rectangle(unscaled(rawRect.x), unscaled(rawRect.y),
                                 unscaled(rawRect.width),
                                 unscaled(rawRect.height));
        } else {
            rect.setBounds(unscaled(rawRect.x), unscaled(rawRect.y),
                           unscaled(rawRect.width), unscaled(rawRect.height));
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

    //-------//
    // reset //
    //-------//
    private final void reset (MouseEvent e)
    {
        if (rawRect == null) {
            rawRect = new Rectangle(e.getX(), e.getY(), 0, 0);
        } else {
            rawRect.setBounds(e.getX(), e.getY(), 0, 0);
        }

        normalize();
        resetOrigin(rect.x, rect.y);
    }

    //--------//
    // scaled //
    //--------//
    private int scaled (int val)
    {
        if (zoom != null) {
            return zoom.truncScaled(val);
        } else {
            return val;
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
}
