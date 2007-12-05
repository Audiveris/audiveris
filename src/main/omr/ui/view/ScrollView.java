//-----------------------------------------------------------------------//
//                                                                       //
//                          S c r o l l V i e w                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
package omr.ui.view;

import omr.util.Logger;

import java.awt.*;

import javax.swing.*;

/**
 * Class <code>ScrollView</code> contains a JScrollPane, which provides a
 * comprehensive combination of the following entities: <dl>
 *
 * <dt> <b>view:</b> </dt> <dd>the display of a {@link RubberZoomedPanel},
 * a component potentially linked to a {@link Zoom} and a mouse adapter
 * {@link Rubber}</dd>
 *
 * <dt> <b>zoom:</b> </dt> <dd>the {@link Zoom} whose ratio is to be used
 * when the component is rendered </dd>
 *
 * @author Herv&eacute; Bitteur
 * @author Brenton Partridge
 * @version $Id$
 */
public class ScrollView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScrollView.class);

    //~ Instance fields --------------------------------------------------------

    /** Current view inside the scrolled pane */
    protected RubberZoomedPanel view;

    // The concrete UI component
    private JScrollPane component = new JScrollPane();

    //~ Constructors -----------------------------------------------------------

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

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component
     *
     * @return the concrete component
     */
    public JScrollPane getComponent ()
    {
        return component;
    }

    //----------------//
    // getRubberFocus //
    //----------------//
    /**
     * Retrieve the coordinates of what is currently the focus point of the
     * display.  Typically, this is the center of the rubber rectangle.
     *
     * @return the focus point, or null if the view is null
     */
    public Point getRubberFocus ()
    {
        Point center = view.rubber.getCenter();

        if (center != null) {
            if (logger.isFineEnabled()) {
                logger.fine("getRubberFocus rubber center=" + center);
            }

            return center; // Of rubber band
        } else if (view != null) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "getRubberFocus panelcenter=" + view.getPanelCenter());
            }

            return view.getPanelCenter(); // Of visible rectangle
        } else {
            logger.warning("getRubberFocus : no rubber, no view ???");

            return null;
        }
    }
    
    //--------------------//
    // getRubberSelection //
    //--------------------//
    /**
     * Retrieve a copy of the rubber rectangle, or
     * a zero-height, zero-width rectangle at 
     * <code>getRubberFocus()</code> if the rubber
     * rectangle does not exist.
     *
     * @return the dezoomed rubber selection, or null
     * if the view is null
     */
    public Rectangle getRubberSelection ()
    {
        Rectangle rect = view.rubber.getRectangle();
        if (rect != null) {
            return new Rectangle(rect);
        } else {
            Point focus = getRubberFocus();
            if (focus != null) {
                return new Rectangle(focus);
            }
        }
        
        return null;
    }

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
        component.setViewportView(view);

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
    public RubberZoomedPanel getView ()
    {
        return view;
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
        if (logger.isFineEnabled()) {
            logger.fine("setZoomRatio zoomRatio=" + zoomRatio);
        }

        if (view.getZoom() != null) {
            view.getZoom()
                .setRatio(zoomRatio);
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

        if (logger.isFineEnabled()) {
            logger.fine("fitHeight vr=" + vr + " dim=" + dim);
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

        if (logger.isFineEnabled()) {
            logger.fine("fitWhole vr=" + vr + " dim=" + dim);
        }

        setZoomRatio(
            Math.min(
                (double) (vr.width) / (double) dim.width,
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

        if (logger.isFineEnabled()) {
            logger.fine("fitWidth vr=" + vr + " dim=" + dim);
        }

        if ((vr.width > 0) && (dim.width > 0)) {
            setZoomRatio((double) (vr.width) / (double) dim.width);

            return true;
        } else {
            return false;
        }
    }
}
