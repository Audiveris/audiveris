//-----------------------------------------------------------------------//
//                                                                       //
//                          S c r o l l V i e w                          //
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

import java.awt.*;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class <code>ScrollView</code>, is a subclass of JScrollPane, which
 * provides a comprehensive combination of the following entities: <dl>
 *
 * <dt> <b>view:</b> </dt> <dd>the display of a {@link RubberZoomedPanel},
 * a component potentially linked to a {@link Zoom} and a {@link
 * Rubber}</dd>
 *
 * <dt> <b>zoom:</b> </dt> <dd>the {@link Zoom} whose ratio is to be used
 * when the component is rendered </dd>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScrollView
    extends JScrollPane
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(ScrollView.class);

    //~ Instance variables ------------------------------------------------

    /** Current view inside the scrolled pane */
    protected RubberZoomedPanel view;

    /** Related zoom */
    protected Zoom zoom;

    //~ Constructors ------------------------------------------------------

    //------------//
    // ScrollView //
    //------------//
    /**
     * Create a bare view pane. Other related entities, such as view, pixel
     * monitor or zoom, can be added later programmatically via setXXX()
     * methods).
     */
    public ScrollView ()
    {
    }

    //------------//
    // ScrollView //
    //------------//
    /**
     * Create a view pane, with pre-built view
     *
     * @param view the pre-built panel
     */
    public ScrollView (RubberZoomedPanel view)
    {
        setView(view);
    }

    //------------//
    // ScrollView //
    //------------//
    /**
     * Create a view pane, with pre-built view, pixel monitor and zoom
     *
     * @param view the pre-built panel
     * @param zoom the zoom that drives the rendering of the view
     */
    public ScrollView (RubberZoomedPanel view,
                       Zoom zoom)
    {
        setView(view);
        setZoom(zoom);
    }

    //~ Methods -----------------------------------------------------------

    //---------//
    // setView //
    //---------//
    /**
     * Allow to set the panel to be displayed
     *
     * @param view the pre-built panel
     */
    public void setView (RubberZoomedPanel view)
    {
        // Display the view in the scrollpane
        setViewportView(view);

        // At the end, to avoid too early setting of the zoom ratio
        this.view = view;
    }

    //---------//
    // getView //
    //---------//
    /**
     * Report the view displayed in the view pane
     *
     * @return the view
     */
    public RubberZoomedPanel getView()
    {
        return view;
    }

    //----------//
    // getPixel //
    //----------//
    /**
     * Report the pixel level at the designated point. This is the default
     * implementation which always return -1.
     *
     * @param pt the designated point
     * @return the pixel level (0->255) or -1 if info is not available
     */
    public int getPixel(Point pt)
    {
        return -1;
    }

    //---------//
    // setZoom //
    //---------//
    /**
     * Assign a zoom to drive the rendering scale of the displayed view
     *
     * @param zoom the zoom entity
     */
    public void setZoom (final Zoom zoom)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("setZoom zoom=" + zoom);
        }

        this.zoom = zoom;

        if (view != null) {
            view.setZoom(zoom);
        }
    }

    //---------//
    // getZoom //
    //---------//
    /**
     * Retrieve the zoom entity related to the displayed view.
     *
     * @return the zoom entity
     */
    public Zoom getZoom ()
    {
        return zoom;
    }

    //----------------//
    // getRubberFocus //
    //----------------//
    /**
     * Retrieve the coordinates of what is currently the focus point of the
     * display.  Typically, this is the center of the rubber rectangle.
     *
     * @return the focus point
     */
    public Point getRubberFocus ()
    {
        Point center = view.rubber.getCenter();

        if (center != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("getRubberFocus rubber center=" + center);
            }

            return center; // Of rubber band
        } else if (view != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("getRubberFocus panelcenter="
                             + view.getPanelCenter());
            }

            return view.getPanelCenter(); // Of visible rectangle
        } else {
            logger.warning("getRubberFocus : no rubber, no view ???");

            return null;
        }
    }

    //--------------//
    // setZoomRatio //
    //--------------//
    /**
     * A convenient method which allows to set another zoom ratio,
     * programmatically.
     *
     * @param zoomRatio the new display zoom ratio
     */
    public void setZoomRatio (double zoomRatio)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("setZoomRatio zoomRatio=" + zoomRatio
                         + " zoom=" + zoom);
        }

        if (zoom != null) {
            zoom.setRatio(zoomRatio);
        } else {
            logger.warning("setZoomRatio. No zoom assigned");
        }
    }

    //-----------//
    // fitHeight //
    //-----------//
    /**
     * Define the zoom ratio so that the full height of the model can be
     * visible.  We force the zoom ratio to stand within the range of the
     * slider.
     */
    public void fitHeight ()
    {
        Rectangle vr = view.getVisibleRect();
        Dimension dim = view.getModelSize();

        if (logger.isDebugEnabled()) {
            logger.debug("fitHeight vr=" + vr + " dim=" + dim);
        }

        setZoomRatio((double) (vr.height) / (double) dim.height);
    }

    //----------//
    // fitWhole //
    //----------//
    /**
     * Define the zoom ratio so that the full model can be visible.  We
     * force the zoom ratio to stand within the range of the slider.
     */
    public void fitWhole ()
    {
        Rectangle vr = view.getVisibleRect();
        Dimension dim = view.getModelSize();

        if (logger.isDebugEnabled()) {
            logger.debug("fitWhole vr=" + vr + " dim=" + dim);
        }

        setZoomRatio(Math.min((double) (vr.width) / (double) dim.width,
                              (double) (vr.height) / (double) dim.height));
    }

    //----------//
    // fitWidth //
    //----------//
    /**
     * Define the zoom ratio so that the full width of the model can be
     * visible.  We force the zoom ratio to stand within the range of the
     * slider.
     *
     * @return true if we have been able to fit the display width, false
     *         otherwise which happens when the display width is zero
     */
    public boolean fitWidth ()
    {
        Rectangle vr = view.getVisibleRect();
        Dimension dim = view.getModelSize();

        if (logger.isDebugEnabled()) {
            logger.debug("fitWidth vr=" + vr + " dim=" + dim);
        }

        if ((vr.width > 0) && (dim.width > 0)) {
            setZoomRatio((double) (vr.width) / (double) dim.width);

            return true;
        } else {
            return false;
        }
    }
}
