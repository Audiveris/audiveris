//----------------------------------------------------------------------------//
//                                                                            //
//                           R u b b e r P a n e l                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.ui.view;

import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import static omr.selection.SelectionHint.*;
import omr.selection.SelectionService;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.ui.PixelCount;

import omr.util.ClassUtil;
import omr.util.Implement;

import org.bushe.swing.event.EventSubscriber;

import java.awt.*;
import java.util.ConcurrentModificationException;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class <code>RubberPanel</code> is a combination of two linked entities:
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
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class RubberPanel
    extends JPanel
    implements ChangeListener, MouseMonitor, EventSubscriber<UserEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(RubberPanel.class);

    //~ Instance fields --------------------------------------------------------

    /** Current display zoom, if any */
    protected Zoom zoom;

    /** Rubber band mouse handling, if any */
    protected Rubber rubber;

    /** Model size (independent of display zoom) */
    protected Dimension modelSize;

    /** Location Service if any  (either SheetLocation or ScoreLocation) */
    protected SelectionService locationService;

    /** Precise location information event (sheet or score related) */
    protected Class<?extends LocationEvent> locationClass;

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
        if (logger.isFineEnabled()) {
            logger.fine("new RubberPanel");
        }
    }

    //-------------//
    // RubberPanel //
    //-------------//
    /**
     * Create a RubberPanel, with the specified Rubber to interact via the
     * mouse, and a specified Zoom instance
     *
     * @param zoom related display zoom
     * @param rubber the rubber instance to be linked to this panel
     */
    public RubberPanel (Zoom   zoom,
                        Rubber rubber)
    {
        setZoom(zoom);
        setRubber(rubber);

        if (logger.isFineEnabled()) {
            logger.fine(
                "new RubberZoomedPanel" + " zoom=" + zoom + " rubber=" +
                rubber);
        }
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
     * such registering is needed, it must be done manually through method
     * {@link #subscribe}. (TBD Question: Why?)
     *
     * @param locationService the proper location service to be updated
     * @param locationClass the location class of interest (score or sheet)
     */
    public void setLocationService (SelectionService              locationService,
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
     * @param modelSize the model size to use
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

    //-----------//
    // setRubber //
    //-----------//
    /**
     * Allows to provide the rubber instance, only after this RubberPanel
     * has been built. This can be used to solve circular elaboration problems.
     *
     * @param rubber the rubber instance to be used
     */
    public void setRubber (Rubber rubber)
    {
        this.rubber = rubber;

        rubber.setZoom(zoom);
        rubber.setComponent(this);
        rubber.setMouseMonitor(this);
    }

    //---------------------//
    // getSelectedLocation //
    //---------------------//
    public Rectangle getSelectedLocation ()
    {
        LocationEvent locationEvent = (LocationEvent) locationService.getLastEvent(
            locationClass);

        return (locationEvent != null) ? locationEvent.getData() : null;
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
        try {
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
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
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

    //---------//
    // publish //
    //---------//
    public void publish (LocationEvent locationEvent)
    {
        locationService.publish(locationClass, locationEvent);
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

        // Modify the rubber accordingly
        if (rubber != null) {
            rubber.resetRectangle(rect);
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
        // Force a redisplay
        showFocusLocation(getSelectedLocation());
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
    /**
     * Unsubscribe from the related location  service
     */
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
        Graphics2D g = (Graphics2D) initialGraphics.create();
        g.scale(zoom.getRatio(), zoom.getRatio());

        try {
            // Then, drawing specific to the view (to be provided in subclass)
            render(g);
        } catch (ConcurrentModificationException ex) {
            // It's hard to avoid concurrent modifs since the GUI may need to
            // repaint a view, while some processing is taking place ...
            logger.warning("RubberPanel paintComponent failed", ex);
            repaint(); // To trigger another painting later ...
        } finally {
            // Finally the rubber, now that everything else has been drawn
            if (rubber != null) {
                rubber.render(initialGraphics);
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
    protected void render (Graphics g)
    {
        // Empty by default
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
