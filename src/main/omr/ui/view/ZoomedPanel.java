//-----------------------------------------------------------------------//
//                                                                       //
//                         Z o o m e d P a n e l                         //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
package omr.ui.view;

import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import static omr.selection.SelectionHint.*;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.ui.PixelCount;

import omr.util.ClassUtil;
import omr.util.Implement;

import org.bushe.swing.event.EventService;
import org.bushe.swing.event.EventSubscriber;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>ZoomedPanel</code> is a class meant to handle common
 * task of a display with a magnifying lens, as provided by a related
 * {@link Zoom} entity.
 *
 * <p>This class does not allocate any zoom instance. When using this
 * class, we have to provide our own Zoom instance, either at contruction
 * time by using the proper constructor or later by using the {@link
 * #setZoom} method. The class then registers itself as an observer of the
 * Zoom instance, to be notified when the zoom ratio is modified.
 *
 * <p>The ModelSize is the unzoomed size of the data to be displayed, it
 * can be updated through {@link #setModelSize}. This is useful when used
 * in combination with a JScrollPane container (see {@link ScrollView}
 * example).
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ZoomedPanel
    extends JPanel
    implements ChangeListener, MouseMonitor, EventSubscriber<UserEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ZoomedPanel.class);

    //~ Instance fields --------------------------------------------------------

    /** Model size (independent of display zoom) */
    protected Dimension modelSize;

    /** Related location Service if any */
    protected EventService locationService;

    /** Precise location information (sheet or score related) */
    protected Class<?extends LocationEvent> locationClass;

    /** Current display zoom */
    protected Zoom zoom;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // ZoomedPanel //
    //-------------//
    /**
     * Create a zoomed panel, with no predefined zoom, assuming a zoom
     * instance will be provided later via the {@link #setZoom} method.
     */
    public ZoomedPanel ()
    {
    }

    //-------------//
    // ZoomedPanel //
    //-------------//
    /**
     * Create a zoomed panel, with a driving zoom instance.
     *
     * @param zoom the related zoom instance
     */
    public ZoomedPanel (Zoom zoom)
    {
        setZoom(zoom);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // setLocationService //
    //--------------------//
    /**
     * Allow to inject a dependency on a location service. This location is used
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
     * such registering is needed, it must be done manually.
     *
     * @param locationService the proper location service to be updated
     * @param locationClass the location class of interest (score or sheet)
     */
    public void setLocationService (EventService                  locationService,
                                    Class<?extends LocationEvent> locationClass)
    {
        if ((this.locationService != null) &&
            (this.locationService != locationService)) {
            this.locationService.unsubscribe(locationClass, this);
        }

        this.locationService = locationService;
        this.locationClass = locationClass;
    }

    //--------------//
    // setModelSize //
    //--------------//
    /**
     * Assign the size of the model object, that is the unscaled size.
     */
    public void setModelSize (Dimension modelSize)
    {
        this.modelSize = new Dimension(modelSize);
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
        Point     pt = new Point(
            zoom.unscaled(vr.x + (vr.width / 2)),
            zoom.unscaled(vr.y + (vr.height / 2)));

        if (logger.isFineEnabled()) {
            logger.fine("getPanelCenter=" + pt);
        }

        return pt;
    }

    //---------//
    // setZoom //
    //---------//
    /**
     * Assign a zoom to this panel
     *
     * @param zoom the zomm assigned
     */
    public void setZoom (final Zoom zoom)
    {
        // Clean up if needed
        if (this.zoom != null) {
            this.zoom.removeChangeListener(this);
        }

        this.zoom = zoom;

        if (zoom != null) {
            // Add a listener on this zoom
            zoom.addChangeListener(this);
        }
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

    //-----------------//
    // contextSelected //
    //-----------------//
    @Implement(MouseMonitor.class)
    public void contextSelected (Point         pt,
                                 MouseMovement movement)
    {
        // Nothing by default
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Notification of a location selection  (pixel or score)
     *
     * @param event the location event
     */
    @Implement(EventSubscriber.class)
    public void onEvent (UserEvent event)
    {
        // Ignore RELEASING
        if (event.movement == MouseMovement.RELEASING) {
            return;
        }

        if (logger.isFineEnabled()) {
            logger.fine(this.getClass().getName() + " onEvent " + event);
        }

        if (event instanceof LocationEvent) {
            LocationEvent locationEvent = (LocationEvent) event;
            showFocusLocation(locationEvent.getRectangle());
        }
    }

    //------------//
    // pointAdded //
    //------------//
    @Implement(MouseMonitor.class)
    public void pointAdded (Point         pt,
                            MouseMovement movement)
    {
        setFocusLocation(new Rectangle(pt), movement, LOCATION_ADD);
    }

    //---------------//
    // pointSelected //
    //---------------//
    @Implement(MouseMonitor.class)
    public void pointSelected (Point         pt,
                               MouseMovement movement)
    {
        setFocusLocation(new Rectangle(pt), movement, LOCATION_INIT);
    }

    //-------------------//
    // rectangleSelected //
    //-------------------//
    @Implement(MouseMonitor.class)
    public void rectangleSelected (Rectangle     rect,
                                   MouseMovement movement)
    {
        setFocusLocation(rect, movement, LOCATION_INIT);
    }

    //-----------------//
    // rectangleZoomed //
    //-----------------//
    @Implement(MouseMonitor.class)
    public void rectangleZoomed (final Rectangle rect,
                                 MouseMovement   movement)
    {
        if (logger.isFineEnabled()) {
            logger.fine(getClass().getName() + " rectangleZoomed " + rect);
        }

        if (rect != null) {
            // First focus on center of the specified rectangle
            setFocusLocation(rect, movement, LOCATION_INIT);
            showFocusLocation(rect);

            // Then, adjust zoom ratio to fit the rectangle size
            SwingUtilities.invokeLater(
                new Runnable() {
                        public void run ()
                        {
                            Rectangle vr = getVisibleRect();
                            double    zoomX = (double) vr.width / (double) rect.width;
                            double    zoomY = (double) vr.height / (double) rect.height;
                            zoom.setRatio(Math.min(zoomX, zoomY));
                        }
                    });
        }
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
     * @param rect the location information
     */
    public void showFocusLocation (Rectangle rect)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                getClass().getName() + " showFocusLocation rect=" + rect);
        }

        updatePreferredSize();

        if (rect != null) {
            // Check whether the rectangle is fully visible,
            // if not, scroll so as to make (most of) it visible
            Rectangle scaledRect = zoom.scaled(rect);

            // Needed to work around a strange behavior of 'contains' method
            if (scaledRect.width == 0) {
                scaledRect.width = 1;
            }

            if (scaledRect.height == 0) {
                scaledRect.height = 1;
            }

            if (!getVisibleRect()
                     .contains(scaledRect)) {
                int margin = constants.focusMargin.getValue();
                scrollRectToVisible(
                    new Rectangle(
                        zoom.scaled(rect.x) - margin,
                        zoom.scaled(rect.y) - margin,
                        zoom.scaled(rect.width + (2 * margin)),
                        zoom.scaled(rect.height) + (2 * margin)));
            }
        }

        repaint();
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * Entry called when the ratio of the related zoom has changed
     *
     * @param e the zoom event
     */
    @Implement(ChangeListener.class)
    public void stateChanged (ChangeEvent e)
    {
        Rectangle rect = null;

        // Force a redisplay
        if (locationService != null) {
            LocationEvent locationEvent = (LocationEvent) locationService.getLastEvent(
                locationClass);
            rect = (locationEvent != null) ? locationEvent.getData() : null;
        }

        showFocusLocation(rect);
    }

    //-----------//
    // subscribe //
    //-----------//
    public void subscribe ()
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                getClass().getName() + " ZP subscribing to " + locationClass);
        }

        // Subscribe to location events
        locationService.subscribeStrongly(locationClass, this);
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
    // unsubscribe //
    //-------------//
    public void unsubscribe ()
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                getClass().getName() + " ZP unsubscribing from " +
                locationClass);
        }

        // Unsubscribe to location events
        locationService.unsubscribe(locationClass, this);
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
     * @param rect the location information
     * @param movement the button movement
     * @param hint the related selection hint
     */
    protected void setFocusLocation (Rectangle     rect,
                                     MouseMovement movement,
                                     SelectionHint hint)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setFocusLocation rect=" + rect + " hint=" + hint);
        }

        // Publish the new user-selected location
        if (locationService != null) {
            locationService.publish(
                new SheetLocationEvent(
                    this,
                    hint,
                    movement,
                    new PixelRectangle(rect)));
        }
    }

    //---------------------//
    // updatePreferredSize //
    //---------------------//
    private void updatePreferredSize ()
    {
        setPreferredSize(zoom.scaled(getModelSize()));
        revalidate();
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
            50,
            "Margin visible around a focus");
    }
}
