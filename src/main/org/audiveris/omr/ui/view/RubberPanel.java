//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     R u b b e r P a n e l                                      //
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
package org.audiveris.omr.ui.view;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.ui.PixelCount;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import static org.audiveris.omr.ui.selection.SelectionHint.*;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.util.ClassUtil;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.util.ConcurrentModificationException;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;

import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code RubberPanel} is a JPanel combined with two linked entities:
 * a {@link Zoom} and a {@link Rubber}.
 * <p>
 * Its {@link #paintComponent} method is declared final to ensure that the rendering is done in
 * proper sequence, with the rubber rectangle rendered at the end, on top of any other stuff.
 * Any specific rendering required by a subclass is performed by overriding the {@link #render}
 * method for global objects and/or the {@link #renderItems} method for some selected items.
 * <p>
 * The Zoom instance and the Rubber instance can be provided separately, after this RubberPanel has
 * been constructed. This is meant for cases where the same Zoom and Rubber instances are shared by
 * several views, as in the {@link org.audiveris.omr.sheet.ui.SheetAssembly} example.
 * <p>
 * When using this class, we have to provide our own Zoom instance, either at construction time by
 * using the proper constructor or later by using the {@link #setZoom} method. The class then
 * registers itself as an observer of the Zoom instance, to be notified when the zoom ratio is
 * modified.
 *
 * @author Hervé Bitteur
 */
public class RubberPanel
        extends JPanel
        implements ChangeListener, MouseMonitor, EventSubscriber<UserEvent>
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(RubberPanel.class);

    /** Current display zoom, if any. */
    protected Zoom zoom;

    /** Rubber band mouse handling, if any. */
    protected Rubber rubber;

    /** Model size (independent of display zoom). */
    protected Dimension modelSize;

    /** Location Service if any (for Location event). */
    protected SelectionService locationService;

    /**
     * Create a bare RubberPanel, assuming zoom and rubber will be assigned later.
     */
    public RubberPanel ()
    {
        setBackground(Color.white);

        bindKeys();
    }

    /**
     * Create a RubberPanel, with the specified Rubber to interact via the mouse,
     * and a specified Zoom instance
     *
     * @param zoom   related display zoom
     * @param rubber the rubber instance to be linked to this panel
     */
    public RubberPanel (Zoom zoom,
                        Rubber rubber)
    {
        this();
        setZoom(zoom);
        setRubber(rubber);

        bindKeys();
        logger.debug("new RubberPanel zoom={} rubber={}", zoom, rubber);
    }

    //--------------//
    // contextAdded //
    //--------------//
    @Override
    public void contextAdded (Point pt,
                              MouseMovement movement)
    {
        // Void by default
    }

    //-----------------//
    // contextSelected //
    //-----------------//
    @Override
    public void contextSelected (Point pt,
                                 MouseMovement movement)
    {
        // Void by default
    }

    //--------------//
    // getModelSize //
    //--------------//
    /**
     * Report the size of the model object, that is the unscaled size.
     *
     * @return the original size
     */
    public Dimension getModelSize ()
    {
        if (modelSize != null) {
            return new Dimension(modelSize);
        } else {
            return null;
        }
    }

    //--------------//
    // setModelSize //
    //--------------//
    /**
     * Assign the size of the model object, that is the un-scaled size.
     *
     * @param modelSize the model size to use
     */
    public void setModelSize (Dimension modelSize)
    {
        this.modelSize = new Dimension(modelSize);
    }

    //----------------//
    // getPanelCenter //
    //----------------//
    /**
     * Retrieve the current center of the display, and report its
     * corresponding model location.
     *
     * @return the un-scaled coordinates of the panel center
     */
    public Point getPanelCenter ()
    {
        Rectangle vr = getVisibleRect();
        Point pt = new Point(
                zoom.unscaled(vr.x + (vr.width / 2)),
                zoom.unscaled(vr.y + (vr.height / 2)));

        logger.debug("getPanelCenter={}", pt);

        return pt;
    }

    //------------------//
    // getPreferredSize //
    //------------------//
    @Override
    public Dimension getPreferredSize ()
    {
        if (modelSize != null) {
            return zoom.scaled(modelSize);
        }

        return super.getPreferredSize();
    }

    //-----------//
    // getRubber //
    //-----------//
    public Rubber getRubber ()
    {
        return rubber;
    }

    //--------------------//
    // getRubberRectangle //
    //--------------------//
    /**
     * Return the model rectangle defined by the rubber.
     * This is a de-zoomed rectangle, should the component have a related zoom.
     *
     * @return the model rectangle
     */
    public Rectangle getRubberRectangle ()
    {
        if (rubber != null) {
            return rubber.getRectangle();
        } else {
            return null;
        }
    }

    //----------------------//
    // getSelectedRectangle //
    //----------------------//
    /**
     * Report the rectangle currently selected, or null.
     *
     * @return the absolute rectangle selected
     */
    public Rectangle getSelectedRectangle ()
    {
        if (locationService == null) {
            logger.error("No locationService for {}", this);

            return null;
        }

        LocationEvent locationEvent = (LocationEvent) locationService.getLastEvent(
                LocationEvent.class);

        return (locationEvent != null) ? locationEvent.getData() : null;
    }

    //---------------------//
    // getTransformToGlass //
    //---------------------//
    /**
     * Report the affine transform that converts sheet-based coordinates to coordinates
     * in the provided glass, taking account zoom, scroll and view vs glass shift.
     * <p>
     * In fact, this method can be used with any other target JPanel.
     *
     * @param glass the target (glass) panel
     * @return the affine transform from sheet to (glass) panel
     */
    public AffineTransform getTransformToGlass (JPanel glass)
    {
        Point gOffset = SwingUtilities.convertPoint(this, 0, 0, glass);
        double z = zoom.getRatio();

        return new AffineTransform(z, 0, 0, z, gOffset.x, gOffset.y);
    }

    //---------//
    // getZoom //
    //---------//
    /**
     * Return the current zoom
     *
     * @return the used zoom
     */
    public Zoom getZoom ()
    {
        return zoom;
    }

    //---------//
    // setZoom //
    //---------//
    /**
     * Assign a zoom to this panel
     *
     * @param zoom the zoom assigned
     */
    public final void setZoom (final Zoom zoom)
    {
        // Clean up if needed
        unsetZoom(this.zoom);

        this.zoom = zoom;

        if (zoom != null) {
            // Add a listener on this zoom
            zoom.addChangeListener(this);
        }
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Notification of a location selection.
     *
     * @param event the location event
     */
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            logger.debug("{} onEvent {}", getClass().getName(), event);

            if (event instanceof LocationEvent) {
                handleLocationEvent((LocationEvent) event);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //------------//
    // pointAdded //
    //------------//
    @Override
    public void pointAdded (Point pt,
                            MouseMovement movement)
    {
        setFocusLocation(new Rectangle(pt), movement, LOCATION_ADD);
    }

    //---------------//
    // pointSelected //
    //---------------//
    @Override
    public void pointSelected (Point pt,
                               MouseMovement movement)
    {
        setFocusLocation(new Rectangle(pt), movement, LOCATION_INIT);
    }

    //---------//
    // publish //
    //---------//
    /**
     * Publish the provided location event on the proper service.
     *
     * @param locationEvent the provided location event
     */
    public void publish (LocationEvent locationEvent)
    {
        locationService.publish(locationEvent);
    }

    //-------------------//
    // rectangleSelected //
    //-------------------//
    @Override
    public void rectangleSelected (Rectangle rect,
                                   MouseMovement movement)
    {
        setFocusLocation(rect, movement, LOCATION_INIT);
    }

    //-----------------//
    // rectangleZoomed //
    //-----------------//
    @Override
    public void rectangleZoomed (final Rectangle rect,
                                 MouseMovement movement)
    {
        logger.debug("{} rectangleZoomed {}", getClass().getName(), rect);

        if (rect != null) {
            // First focus on center of the specified rectangle
            setFocusLocation(rect, movement, LOCATION_INIT);
            showFocusLocation(rect, true);

            // Then, adjust zoom ratio to fit the rectangle size
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run ()
                {
                    Rectangle vr = getVisibleRect();
                    double zoomX = vr.width / (double) rect.width;
                    double zoomY = vr.height / (double) rect.height;
                    zoom.setRatio(Math.min(zoomX, zoomY));
                }
            });
        }
    }

    //-------------//
    // selectPoint //
    //-------------//
    /**
     * Convenient method to simulate a user point selection and focus.
     *
     * @param point the point to focus upon
     */
    public void selectPoint (Point point)
    {
        pointSelected(point, MouseMovement.PRESSING);

        ///showFocusLocation(new Rectangle(point), true);
    }

    //--------------------//
    // setLocationService //
    //--------------------//
    /**
     * Inject a dependency on a location service.
     * This location is used for two purposes:
     * <ol>
     * <li>First, this panel is a producer of location information. The location can be modified
     * both programmatically (by calling method {@link #setFocusLocation}) and interactively by
     * mouse event (pointSelected or rectangleSelected which in turn call setFocusLocation) .</li>
     * <li>Second, this panel is a consumer of location information, since it makes the selected
     * location visible in the display, through the method {@link #showFocusLocation}.</li>
     * </ol>
     * <p>
     * <b>Nota</b>: Setting the location selection does not automatically register this view on the
     * selection object. If such registering is needed, it must be done manually through method
     * {@link #subscribe}. (TODO: Question: Why is registering differed?)
     *
     * @param locationService the proper location service to be updated
     */
    public void setLocationService (SelectionService locationService)
    {
        if ((this.locationService != null) && (this.locationService != locationService)) {
            this.locationService.unsubscribe(LocationEvent.class, this);
        }

        this.locationService = locationService;
    }

    //-----------//
    // setRubber //
    //-----------//
    /**
     * Allows to provide the rubber instance, only after this RubberPanel has been built.
     * This can be used to solve circular elaboration problems.
     *
     * @param rubber the rubber instance to be used
     */
    public final void setRubber (Rubber rubber)
    {
        this.rubber = rubber;

        rubber.setZoom(zoom);
        rubber.connectComponent(this);
        rubber.setMouseMonitor(this);
    }

    //-------------------//
    // showFocusLocation //
    //-------------------//
    /**
     * Update the display, so that the location rectangle gets visible.
     * <p>
     * <b>NOTA</b>: Subclasses that override this method should call this
     * super implementation or the display will not be updated by default.
     *
     * @param rect     the location information
     * @param centered true to center the display on 'rect' center
     */
    public void showFocusLocation (final Rectangle rect,
                                   final boolean centered)
    {
        if (zoom == null) {
            return; // For degenerated cases (no real view)
        }

        if (modelSize == null) {
            return;
        }

        setPreferredSize(zoom.scaled(modelSize));
        revalidate();
        repaint();

        rubber.resetRectangle(rect);

        if (rect != null) {
            // Check whether the rectangle is fully visible,
            // if not, scroll so as to make (most of) it visible
            Rectangle scaledRect = zoom.scaled(rect);
            Point center = new Point(
                    scaledRect.x + (scaledRect.width / 2),
                    scaledRect.y + (scaledRect.height / 2));

            if (centered) {
                Rectangle vr = getVisibleRect();
                scaledRect = new Rectangle(
                        center.x - (vr.width / 2),
                        center.y - (vr.height / 2),
                        vr.width,
                        vr.height);
            } else {
                int margin = constants.focusMargin.getValue();

                if (margin == 0) {
                    scaledRect.grow(1, 1); // Workaround
                } else {
                    scaledRect.grow(margin, margin);
                }
            }

            scrollRectToVisible(scaledRect);
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * Entry called when the ratio of the related zoom has changed
     *
     * @param e the zoom event
     */
    @Override
    public void stateChanged (ChangeEvent e)
    {
        showFocusLocation(getSelectedRectangle(), true);
    }

    //-----------//
    // subscribe //
    //-----------//
    /**
     * Subscribe to the (previously injected) location service.
     */
    public void subscribe ()
    {
        logger.debug("Subscribe {} {}", getClass().getSimpleName(), getName());

        // Subscribe to location events
        if (locationService != null) {
            locationService.subscribeStrongly(LocationEvent.class, this);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return ClassUtil.nameOf(this);
    }

    //-------------//
    // unsetRubber //
    //-------------//
    /**
     * Cut the connection between this view and the rubber
     *
     * @param rubber the rubber to disconnect
     */
    public void unsetRubber (Rubber rubber)
    {
        rubber.setMouseMonitor(null);
        rubber.connectComponent(null);
    }

    //-----------//
    // unsetZoom //
    //-----------//
    /**
     * Deassign the zoom, unregistering this component as a zoom listener
     *
     * @param zoom the zoom to unregister from
     * @return true if actually disconnected
     */
    public boolean unsetZoom (Zoom zoom)
    {
        if (zoom != null) {
            return zoom.removeChangeListener(this);
        } else {
            return false;
        }
    }

    //-------------//
    // unsubscribe //
    //-------------//
    /**
     * Un-subscribe from the related location service.
     */
    public void unsubscribe ()
    {
        logger.debug("Unsubscribe {} {}", getClass().getSimpleName(), getName());

        // Unsubscribe to location events
        if (locationService != null) {
            locationService.unsubscribe(LocationEvent.class, this);
        }
    }

    //----------------//
    // objectSelected //
    //----------------//
    @Override
    public void objectSelected (Point pt,
                                MouseMovement movement)
    {
        setFocusLocation(new Rectangle(pt), movement, ENTITY_INIT);
    }

    //----------//
    // bindKeys //
    //----------//
    /**
     * Bind keys to shift rubber location and modify zoom ratio.
     */
    protected void bindKeys ()
    {
        final InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap actionMap = getActionMap();

        // Slight translation
        inputMap.put(KeyStroke.getKeyStroke("alt UP"), "UpTranslateAction");
        actionMap.put("UpTranslateAction", new TranslateAction(0, -1));

        inputMap.put(KeyStroke.getKeyStroke("alt DOWN"), "DownTranslateAction");
        actionMap.put("DownTranslateAction", new TranslateAction(0, 1));

        inputMap.put(KeyStroke.getKeyStroke("alt LEFT"), "LeftTranslateAction");
        actionMap.put("LeftTranslateAction", new TranslateAction(-1, 0));

        inputMap.put(KeyStroke.getKeyStroke("alt RIGHT"), "RightTranslateAction");
        actionMap.put("RightTranslateAction", new TranslateAction(1, 0));

        // Zoom modifications
        inputMap.put(KeyStroke.getKeyStroke("ctrl ADD"), "ZoomInAction");
        inputMap.put(KeyStroke.getKeyStroke("ctrl PLUS"), "ZoomInAction");
        actionMap.put("ZoomInAction", new ZoomAction(1));

        inputMap.put(KeyStroke.getKeyStroke("ctrl SUBTRACT"), "ZoomOutAction");
        inputMap.put(KeyStroke.getKeyStroke("ctrl MINUS"), "ZoomOutAction");
        actionMap.put("ZoomOutAction", new ZoomAction(-1));

        inputMap.put(KeyStroke.getKeyStroke("ctrl NUMPAD0"), "ZoomResetAction");
        inputMap.put(KeyStroke.getKeyStroke("ctrl 0"), "ZoomResetAction");
        actionMap.put("ZoomResetAction", new ZoomAction(0));
    }

    //----------------//
    // paintComponent //
    //----------------//
    /**
     * Final method, called by Swing.
     * If something has to be changed in the rendering of the model, override the {@link #render}
     * and/or the {@link #renderItems} methods instead.
     *
     * @param initialGraphics the graphic context
     */
    @Override
    protected final void paintComponent (Graphics initialGraphics)
    {
        // First, paint view background
        super.paintComponent(initialGraphics);

        // Adjust graphics context to desired zoom ratio
        if (zoom != null) {
            Graphics2D g = (Graphics2D) initialGraphics.create();
            g.scale(zoom.getRatio(), zoom.getRatio());

            try {
                // Second, drawing specific to the view (to be provided in subclass)
                render(g);

                // Third, draw selected items (to be provided in subclass)
                renderItems(g);
            } catch (ConcurrentModificationException ex) {
                // It's hard to avoid all concurrent modifs since the GUI may need to
                // repaint a view, while some processing is taking place ...
                repaint(); // Simply trigger another painting later ...
            } catch (Throwable ex) {
                logger.warn("RubberPanel paintComponent " + ex, ex);
            } finally {
                // Finally, draw the location rubber, now that everything else has been drawn
                if (rubber != null) {
                    rubber.render(initialGraphics);
                }

                g.dispose();
            }
        }
    }

    //---------------------//
    // handleLocationEvent //
    //---------------------//
    /**
     * Handle the provided location event
     *
     * @param locationEvent the location event to process
     */
    protected void handleLocationEvent (LocationEvent locationEvent)
    {
        // Location => move view focus on this location w/ markers
        showFocusLocation(locationEvent.getData(), false); // Centered: false
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the global data in the provided Graphics context, perhaps already scaled.
     * This is just a place holder, the real global rendering must be provided by a subclass.
     *
     * @param g the graphic context
     */
    protected void render (Graphics2D g)
    {
        // Void by default
    }

    //---------------//
    // renderBoxArea //
    //---------------//
    /**
     * Render the provided box area, using inverted color.
     *
     * @param box the rectangle whose area is to be rendered
     * @param g   the graphic context
     */
    protected void renderBoxArea (Rectangle box,
                                  Graphics2D g)
    {
        // Check the clipping
        Rectangle clip = g.getClipBounds();

        if ((box != null) && ((clip == null) || clip.intersects(box))) {
            g.drawRect(box.x, box.y, box.width, box.height);
        }
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Room for rendering additional items, typically some user-selected items if any,
     * on top of the global ones already rendered by the {@link #render} method.
     *
     * @param g the graphic context
     */
    protected void renderItems (Graphics2D g)
    {
        // Void by default
    }

    //------------------//
    // setFocusLocation //
    //------------------//
    /**
     * Modifies the location information.
     * This method simply posts the location information on the proper Service object, provided that
     * such object has been previously injected (by means of the method {@link #setLocationService}.
     *
     * @param rect     the location information
     * @param movement the button movement
     * @param hint     the related selection hint
     */
    protected void setFocusLocation (Rectangle rect,
                                     MouseMovement movement,
                                     SelectionHint hint)
    {
        logger.debug("setFocusLocation rect={} hint={}", rect, hint);

        // Publish the new user-selected location
        if (locationService != null) {
            locationService.publish(new LocationEvent(this, hint, movement, new Rectangle(rect)));
        }
    }

    //-----------------//
    // TranslateAction //
    //-----------------//
    /**
     * Translate rubber location.
     */
    protected class TranslateAction
            extends AbstractAction
    {

        /** Translation on abscissa axis. */
        protected final int dx;

        /** Translation on ordinate axis. */
        protected final int dy;

        public TranslateAction (int dx,
                                int dy)
        {
            this.dx = dx;
            this.dy = dy;
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            rubber.translate(dx, dy);
            setFocusLocation(rubber.getRectangle(), null, LOCATION_INIT);
        }
    }

    //------------//
    // ZoomAction //
    //------------//
    /**
     * Modify zoom ratio.
     */
    protected class ZoomAction
            extends AbstractAction
    {

        /** Zoom increment. */
        protected final int val;

        public ZoomAction (int val)
        {
            this.val = val;
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            rubber.modifyZoomRatio(val);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final PixelCount focusMargin = new PixelCount(20, "Margin visible around a focus");
    }
}
