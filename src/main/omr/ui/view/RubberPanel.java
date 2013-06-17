//----------------------------------------------------------------------------//
//                                                                            //
//                           R u b b e r P a n e l                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.view;

import omr.constant.ConstantSet;

import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import static omr.selection.SelectionHint.*;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.ui.PixelCount;

import omr.util.ClassUtil;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ConcurrentModificationException;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code RubberPanel} is a combination of two linked entities:
 * a {@link Zoom} and a {@link Rubber}.
 *
 * <p>Its <i>paintComponent</i> method is declared final to ensure that the
 * rendering is done in proper sequence, with the rubber rectangle rendered at
 * the end on top of any other stuff. Any specific rendering required by a
 * subclass is performed by overriding the {@link #render} method.
 *
 * <p>The Zoom instance and the Rubber instance can be provided separately,
 * after this RubberPanel has been constructed. This is meant for cases
 * where the same Zoom and Rubber instances are shared by several views, as in
 * the {@link omr.sheet.ui.SheetAssembly} example.
 *
 * <p>When using this class, we have to provide our own Zoom instance, either at
 * contruction time by using the proper constructor or later by using the {@link
 * #setZoom} method. The class then registers itself as an observer of the
 * Zoom instance, to be notified when the zoom ratio is modified.
 *
 * @author Hervé Bitteur
 */
public class RubberPanel
        extends JPanel
        implements ChangeListener, MouseMonitor, EventSubscriber<UserEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            RubberPanel.class);

    //~ Instance fields --------------------------------------------------------
    /** Current display zoom, if any */
    protected Zoom zoom;

    /** Rubber band mouse handling, if any */
    protected Rubber rubber;

    /** Model size (independent of display zoom) */
    protected Dimension modelSize;

    /** Location Service if any (for Location event) */
    protected SelectionService locationService;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // RubberPanel //
    //-------------//
    /**
     * Create a bare RubberPanel, assuming zoom and rubber will be
     * assigned later.
     */
    public RubberPanel ()
    {
        logger.debug("new RubberPanel");
    }

    //-------------//
    // RubberPanel //
    //-------------//
    /**
     * Create a RubberPanel, with the specified Rubber to interact via the
     * mouse, and a specified Zoom instance
     *
     * @param zoom   related display zoom
     * @param rubber the rubber instance to be linked to this panel
     */
    public RubberPanel (Zoom zoom,
                        Rubber rubber)
    {
        setZoom(zoom);
        setRubber(rubber);

        logger.debug("new RubberPanel zoom={} rubber={}", zoom, rubber);
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // setRubber //
    //-----------//
    /**
     * Allows to provide the rubber instance, only after this RubberPanel
     * has been built. This can be used to solve circular elaboration problems.
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

    //--------------//
    // contextAdded //
    //--------------//
    @Override
    public void contextAdded (Point pt,
                              MouseMovement movement)
    {
        // Nothing by default
    }

    //-----------------//
    // contextSelected //
    //-----------------//
    @Override
    public void contextSelected (Point pt,
                                 MouseMovement movement)
    {
        // Nothing by default
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

    //----------------//
    // getPanelCenter //
    //----------------//
    /**
     * Retrieve the current center of the display, and report its
     * corresponding model location.
     *
     * @return the unscaled coordinates of the panel center
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

    //----------------------//
    // getSelectedRectangle //
    //----------------------//
    /**
     * Report the rectangle currently selected, or null
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
    // onEvent //
    //---------//
    /**
     * Notification of a location selection (pixel or score)
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
                // Location => move view focus on this location w/ markers
                LocationEvent locationEvent = (LocationEvent) event;
                showFocusLocation(locationEvent.getData(), false);
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
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    Rectangle vr = getVisibleRect();
                    double zoomX = (double) vr.width / (double) rect.width;
                    double zoomY = (double) vr.height / (double) rect.height;
                    zoom.setRatio(Math.min(zoomX, zoomY));
                }
            });
        }
    }

    //--------------------//
    // setLocationService //
    //--------------------//
    /**
     * Allow to inject a dependency on a location service.
     * This location is used
     * for two purposes: <ol>
     *
     * <li>First, this panel is a producer of location information. The
     * location can be modified both programmatically (by calling method
     * {@link #setFocusLocation}) and interactively by mouse event
     * (pointSelected or rectangleSelected which in turn call
     * setFocusLocation) .</li>
     *
     * <li>Second, this panel is a consumer of location information, since
     * it makes the selected location visible in the display, through the
     * method {@link #showFocusLocation}.</li> </ol>
     *
     * <p><b>Nota</b>: Setting the location selection does not
     * automatically register this view on the selection object. If
     * such registering is needed, it must be done manually through method
     * {@link #subscribe}. (TODO: Question: Why?)
     *
     * @param locationService the proper location service to be updated
     */
    public void setLocationService (SelectionService locationService)
    {
        if ((this.locationService != null)
            && (this.locationService != locationService)) {
            this.locationService.unsubscribe(LocationEvent.class, this);
        }

        this.locationService = locationService;
    }

    //--------------//
    // setModelSize //
    //--------------//
    /**
     * Assign the size of the model object, that is the unscaled size.
     *
     * @param modelSize the model size to use
     */
    public void setModelSize (Dimension modelSize)
    {
        this.modelSize = new Dimension(modelSize);
    }

    //-------------------//
    // showFocusLocation //
    //-------------------//
    /**
     * Update the display, so that the location rectangle gets visible.
     *
     * <b>NOTA</b>: Subclasses that override this method should call this
     * super implementation or the display will not be updated by default.
     *
     * @param rect     the location information
     * @param centered true to center the display on rect center
     */
    public void showFocusLocation (final Rectangle rect,
                                   final boolean centered)
    {
        if (zoom == null) {
            return; // For degenerated cases (no real view)
        }

        if (getModelSize() == null) {
            return;
        }

        setPreferredSize(zoom.scaled(getModelSize()));
        revalidate();
        repaint();

        if (rect != null) {
            rubber.resetRectangle(rect);

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
        // Force a redisplay?
        //        if (isShowing()) {
        showFocusLocation(getSelectedRectangle(), true);

        //        }
    }

    //-----------//
    // subscribe //
    //-----------//
    /**
     * Subscribe to the (previously injected) location service (either Sheet or
     * Score location, depending on the context)
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
     * Unsubscribe from the related location service
     */
    public void unsubscribe ()
    {
        logger.debug(
                "Unsubscribe {} {}",
                getClass().getSimpleName(),
                getName());

        // Unsubscribe to location events
        if (locationService != null) {
            locationService.unsubscribe(LocationEvent.class, this);
        }
    }

    //----------------//
    // paintComponent //
    //----------------//
    /**
     * Final method, called by Swing. If something has to be changed in the
     * rendering of the model, override the render method instead.
     *
     * @param initialGraphics the graphic context
     */
    @Override
    protected final void paintComponent (Graphics initialGraphics)
    {
        // Paint background first
        super.paintComponent(initialGraphics);

        // Adjust graphics context to desired zoom ratio
        if (zoom != null) {
            Graphics2D g = (Graphics2D) initialGraphics.create();
            g.scale(zoom.getRatio(), zoom.getRatio());

            try {
                // Then, drawing specific to the view (to be provided in subclass)
                render(g);
            } catch (ConcurrentModificationException ex) {
                // It's hard to avoid concurrent modifs since the GUI may need to
                // repaint a view, while some processing is taking place ...
                ///logger.warn("RubberPanel paintComponent failed", ex);
                repaint(); // To trigger another painting later ...
            } catch (Throwable ex) {
                logger.warn("RubberPanel paintComponent ", ex);
            } finally {
                // Finally the rubber, now that everything else has been drawn
                if (rubber != null) {
                    rubber.render(initialGraphics);
                }

                g.dispose();
            }
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * This is just a place holder, the real rendering must be provided by a
     * subclass to actually render the object displayed, since the rubber is
     * automatically rendered after this one.
     *
     * @param g the graphic context
     */
    protected void render (Graphics2D g)
    {
        // Empty by default
    }

    //------------------//
    // setFocusLocation //
    //------------------//
    /**
     * Modifies the location information. This method simply posts the
     * location information on the proper Service object, provided that
     * such object has been previously injected (by means of the method
     * {@link #setLocationService}.
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
            locationService.publish(
                    new LocationEvent(this, hint, movement, new Rectangle(rect)));
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        PixelCount focusMargin = new PixelCount(
                20,
                "Margin visible around a focus");

    }
}
