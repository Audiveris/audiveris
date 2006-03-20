//-----------------------------------------------------------------------//
//                                                                       //
//                         Z o o m e d P a n e l                         //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui.view;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.ui.*;
import omr.util.Logger;

import java.awt.*;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
 * <p>Via the {@link PixelFocus} interface, we can programmatically ask
 * this panel fo focus on a specific point or rectangle.
 *
 * <p>Via the {@link MouseMonitor} interface, this panel traps the various
 * mouse actions.
 *
 * <p> This class cannot directly declare that it implements the interface
 * {@link PixelSubject}, because there are several informations to be
 * pushed. However, the PixelSubject methods (such as addObserver, etc) are
 * indeed provided, to allow a registered {@link PixelObserver} to be
 * notified of any modification in the pixel information (point and/or
 * rectangle).
 *
 * @see RubberZoomedPanel
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ZoomedPanel
    extends JPanel
    implements ChangeListener,
               MouseMonitor,
               PixelFocus
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(ZoomedPanel.class);
    private static final Constants constants = new Constants();

    //~ Instance variables ------------------------------------------------

    /** Model size (independent of display zoom) */
    protected Dimension modelSize;

    /** Current display zoom */
    protected Zoom zoom;

    /** Subject for pixel observers if any */
    protected DefaultPixelSubject pixelSubject
        = new DefaultPixelSubject();

    /** To avoid circular updating */
    protected volatile transient boolean selfUpdating;

    //~ Constructors ------------------------------------------------------

    //-------------//
    // ZoomedPanel //
    //-------------//
    /**
     * Create a zoomed panel, with no predefined zoom, assuming a zoom
     * instance will be provided later via the {@link #setZoom} method..
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

    //-------------//
    // ZoomedPanel //
    //-------------//
    /**
     * Create a zoomed panel, with a driving zoom instance and the
     * underlying model size.
     *
     * @param zoom the related zoom instance
     * @param modelSize the underlying model size
     */
    public ZoomedPanel (Zoom      zoom,
                        Dimension modelSize)
    {
        setZoom(zoom);
        setModelSize(modelSize);
    }

    //~ Methods -----------------------------------------------------------

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
        Point pt = new Point(zoom.unscaled(vr.x + (vr.width / 2)),
                             zoom.unscaled(vr.y + (vr.height / 2)));

        if (logger.isFineEnabled()) {
            logger.fine("getPanelCenter=" + pt);
        }

        return pt;
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
    public void contextSelected (MouseEvent e,
                                 Point pt)
    {
        // Nothing by default
    }

    //---------------//
    // pointSelected //
    //---------------//
    /**
     * Point designation, which does nothing except notifying registered
     * observers about the designated point.
     *
     * @param e the mouse event
     * @param pt the selected point in model pixel coordinates
     */
    public void pointSelected (MouseEvent e,
                               Point pt)
    {
        selfUpdating = true;
        setFocusPoint(pt);
        selfUpdating = false;
    }

    //------------//
    // pointAdded //
    //------------//
    public void pointAdded (MouseEvent e,
                            Point pt)
    {
        selfUpdating = true;
        pointSelected(e, pt);
        selfUpdating = false;
    }

    //-----------//
    // reDisplay //
    //-----------//
    /**
     * Redisplay, keeping the focus of the display, that is the focus
     * (point of interest) remains visible.
     */
    public void reDisplay ()
    {
        setFocusRectangle(null);
    }

    //-------------------//
    // rectangleSelected //
    //-------------------//
    public void rectangleSelected (MouseEvent e,
                                   Rectangle  rect)
    {
        selfUpdating = true;
        setFocusRectangle(rect);
        selfUpdating = false;
    }

    //-----------------//
    // rectangleZoomed //
    //-----------------//
    public void rectangleZoomed (MouseEvent e,
                                 final Rectangle rect)
    {
        // First focus on center of the specified rectangle
        setFocusRectangle(rect);

        // Then, adjust zoom ratio to fit the rectangle size
        SwingUtilities.invokeLater(new Runnable()
            {
                public void run ()
                {
                    Rectangle vr = getVisibleRect();
                    double zoomX = (double) vr.width / (double) rect.width;
                    double zoomY = (double) vr.height / (double) rect.height;
                    zoom.setRatio(Math.min(zoomX, zoomY));
                }
            });
    }

    //---------------//
    // setFocusPoint //
    //---------------//
    /**
     * Force the panel to move so that the provided focus point gets
     * visible
     *
     * @param pt the desired focus point (expressed in model coordinates)
     */
    public void setFocusPoint (Point pt)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setFocusPoint pt=" + pt);
        }

        updatePreferredSize();

        if (pt != null && !selfUpdating) {
            int margin = constants.focusMargin.getValue();
            Rectangle vr = new Rectangle
                (zoom.scaled(pt.x) - margin,
                 zoom.scaled(pt.y) - margin,
                 zoom.scaled(2 * margin),
                 zoom.scaled(2 * margin));
            scrollRectToVisible(vr);
        }

        notifyObservers(pt);
        repaint();
    }

    //-------------------//
    // setFocusRectangle //
    //-------------------//
    public void setFocusRectangle (Rectangle rect)
    {
        updatePreferredSize();

        if (rect != null && !selfUpdating) {
            if (rect.width != 0 || rect.height != 0) {
            int margin = constants.focusMargin.getValue();
            Rectangle vr = new Rectangle
                (zoom.scaled(rect.x) - margin,
                 zoom.scaled(rect.y) - margin,
                 zoom.scaled(rect.width   + 2 * margin),
                 zoom.scaled(rect.height) + 2 * margin);
                scrollRectToVisible(vr);
            } else {
                setFocusPoint(rect.getLocation());
            }
        }

        notifyObservers(rect);
        repaint();
    }

    //-------------//
    // addObserver //
    //-------------//
    /**
     * Register an observing entity for both point and rectangle
     *
     * @param observer the entity to register
     */
    public void addObserver (PixelObserver observer)
    {
        pixelSubject.addObserver(observer);
    }

    //----------------//
    // removeObserver //
    //----------------//
    /**
     * Unregister an observer
     *
     * @param observer the observer to remove
     */
    public void removeObserver (PixelObserver observer)
    {
        pixelSubject.removeObserver(observer);
    }

    //-----------------//
    // notifyObservers //
    //-----------------//
    /**
     * Push the point information to registered observers
     *
     * @param ul the point information
     */
    public void notifyObservers (Point ul)
    {
        pixelSubject.notifyObservers(ul);
    }

    //-----------------//
    // notifyObservers //
    //-----------------//
    /**
     * Push the point information and the grey level to registered
     * observers
     *
     * @param ul the point information
     * @param level the grey level of the pixel
     */
    public void notifyObservers (Point ul,
                                 int level)
    {
        pixelSubject.notifyObservers(ul, level);
    }

    //-----------------//
    // notifyObservers //
    //-----------------//
    /**
     * Push the rectangle information to registered observers
     *
     * @param rect the rectangle information
     */
    public void notifyObservers (Rectangle rect)
    {
        pixelSubject.notifyObservers(rect);
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

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * Entry called when the ratio of the related zoom has changed
     *
     * @param e the zoom event
     */
    public void stateChanged (ChangeEvent e)
    {
        // Force a redisplay
        reDisplay();
    }

    //--------------//
    // getModelSize //
    //--------------//
    /**
     * Report the size of the model object, that is the unscaled size.
     *
     * @return the original size
     */
    protected Dimension getModelSize()
    {
        if (modelSize != null)
            return new Dimension(modelSize);
        else
            return null;
    }

    //--------------//
    // setModelSize //
    //--------------//
    /**
     * Assign the size of the model object, that is the unscaled size.
     */
    public void setModelSize(Dimension modelSize)
    {
        this.modelSize = new Dimension(modelSize);
    }

    //---------------------//
    // updatePreferredSize //
    //---------------------//
    private void updatePreferredSize()
    {
        setPreferredSize(zoom.scaled(getModelSize()));
        revalidate();
    }

    //~ Classes -----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
        extends ConstantSet
    {
        Constant.Integer focusMargin = new Constant.Integer
                (20,
                 "Margin (in pixels) visible around a focus");

        Constants ()
        {
            initialize();
        }
    }
}
