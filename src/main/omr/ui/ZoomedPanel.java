//-----------------------------------------------------------------------//
//                                                                       //
//                         Z o o m e d P a n e l                         //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.util.Logger;

import javax.swing.*;
import java.awt.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.MouseEvent;

/**
 * Class <code>ZoomedPanel</code> is an abstract class meant to handle
 * recurrent tasks of a display with a magnifying lens, as provided by a
 * related {@link Zoom} entity.
 *
 * <p>Via the {@link PixelFocus} interface, we can ask this panel fo focus
 * on a specific point or rectangle.
 *
 * <p>Via the {@link MouseMonitor} interface, this panel traps the various
 * mouse actions.
 *
 * <p> This class is best used when enclosed in a scroll pane, so that the
 * zooming and focus features are really effective. </p>
 *
 * <p> Remark : this class cannot directly implement PixelSubject because
 * of several informations to be pushed. To be more thoroughly
 * investigated, TBD.
 *
 * @see RubberZoomedPanel
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class ZoomedPanel
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

    //~ Constructors ------------------------------------------------------

    //-------------//
    // ZoomedPanel //
    //-------------//
    /**
     * Create a bare zoomed panel, assuming zoom will be assigned later
     */
    public ZoomedPanel ()
    {
    }

    //-------------//
    // ZoomedPanel //
    //-------------//
    /**
     * Create a zoomed panel
     */
    public ZoomedPanel (Zoom zoom)
    {
        setZoom(zoom);
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

        if (logger.isDebugEnabled()) {
            logger.debug("getPanelCenter=" + pt);
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

    //---------------//
    // pointSelected //
    //---------------//
    public void pointSelected (MouseEvent e,
                               Point pt)
    {
        pointUpdated(e, pt);
    }

    //--------------//
    // pointUpdated //
    //--------------//
    public void pointUpdated (MouseEvent e,
                              Point pt)
    {
        notifyObservers(pt);
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
        if (logger.isDebugEnabled()) {
            logger.debug("reDisplay");
        }

        setFocusRectangle(null);
    }

    //-------------------//
    // rectangleSelected //
    //-------------------//
    public void rectangleSelected (MouseEvent e,
                                   Rectangle  rect)
    {
        rectangleUpdated(e, rect);
    }

    //------------------//
    // rectangleUpdated //
    //------------------//
    public void rectangleUpdated (MouseEvent e,
                                  Rectangle rect)
    {
        if ((rect.width != 0) || (rect.height != 0)) {
            notifyObservers(rect);
        } else {
            pointUpdated(e, rect.getLocation());
        }
    }

    //-----------------//
    // rectangleZoomed //
    //-----------------//
    public void rectangleZoomed (MouseEvent e,
                                 final Rectangle rect)
    {
        // First center of the specified rectangle
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
        if (logger.isDebugEnabled()) {
            logger.debug("setFocusPoint pt=" + pt);
        }

        updatePreferredSize();

        if (pt != null) {
            Rectangle vr = getVisibleRect();
            vr.x = zoom.scaled(pt.x) - (vr.width /2);
            vr.y = zoom.scaled(pt.y) - (vr.height /2);
            scrollRectToVisible(vr);
        }

        repaint();
    }

    //-------------------//
    // setFocusRectangle //
    //-------------------//
    public void setFocusRectangle (Rectangle rect)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("setFocusRectangle rect=" + rect);
        }

        updatePreferredSize();

        if (rect != null) {
            // To center rectangle in the window
            //             Rectangle vr = getVisibleRect();
            //             vr.x = zoom.scaled(rect.x) +
            //                 zoom.scaled(rect.width/2) - (vr.width / 2);
            //             vr.y = zoom.scaled(rect.y) +
            //                 zoom.scaled(rect.height/2) - (vr.height / 2);
            //             scrollRectToVisible(vr);

            // Or, with less movements
            int margin = constants.focusMargin.getValue();
            Rectangle vr = new Rectangle
                (zoom.scaled(rect.x) - margin,
                 zoom.scaled(rect.y) - margin,
                 zoom.scaled(rect.width   + 2*margin),
                 zoom.scaled(rect.height) + 2*margin);
            scrollRectToVisible(vr);
        }

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
     * Push the point information to registered observer
     *
     * @param ul the point info
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
     * @param ul the point info
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

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        //~ Instance variables -----------------------------------------------

        Constant.Integer focusMargin = new Constant.Integer
                (20,
                 "Margin (in pixels) visible around a focus");

        Constants ()
        {
            initialize();
        }
    }
}
