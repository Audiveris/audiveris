//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S c r o l l V i e w                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.view;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

/**
 * Class {@code ScrollView} contains a JScrollPane, which provides a comprehensive
 * combination of the following entities.
 * <dl>
 * <dt> <b>view:</b> </dt> <dd>the display of a {@link RubberPanel}, a component potentially linked
 * to a {@link Zoom} and a mouse adapter {@link Rubber}</dd>
 * <dt> <b>zoom:</b> </dt> <dd>the {@link Zoom} whose ratio is to be used when the component is
 * rendered </dd>
 * </dl>
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 */
public class ScrollView
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ScrollView.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Current view inside the scrolled pane. */
    protected RubberPanel view;

    /** The concrete UI component. */
    private final JScrollPane component = new JScrollPane();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a bare view pane.
     * Other related entities, such as view, pixel monitor or zoom, can be added later
     * programmatically via setXXX() methods).
     * <p>
     * The increment related to mouse wheel movement can be adapted via the 'unitIncrement' class
     * constant.
     */
    public ScrollView ()
    {
        JScrollBar vertical = component.getVerticalScrollBar();
        vertical.setUnitIncrement(constants.unitIncrement.getValue());

        JScrollBar horizontal = component.getHorizontalScrollBar();
        horizontal.setUnitIncrement(constants.unitIncrement.getValue());

        bindKeys(component);
    }

    /**
     * Create a view pane, with pre-built view.
     *
     * @param view the pre-built panel
     */
    public ScrollView (RubberPanel view)
    {
        this();
        setView(view);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // fitHeight //
    //-----------//
    /**
     * Define the zoom ratio so that the full height of the model can be visible.
     * We force the zoom ratio to stand within the range of the slider.
     */
    public void fitHeight ()
    {
        Rectangle vr = view.getVisibleRect();
        Dimension dim = view.getModelSize();
        logger.debug("fitHeight vr={} dim={}", vr, dim);

        setZoomRatio(vr.height / (double) dim.height);
    }

    //----------//
    // fitWhole //
    //----------//
    /**
     * Define the zoom ratio so that the full model can be visible.
     * We force the zoom ratio to stand within the range of the slider.
     */
    public void fitWhole ()
    {
        Rectangle vr = view.getVisibleRect();
        Dimension dim = view.getModelSize();
        logger.debug("fitWhole vr={} dim={}", vr, dim);

        setZoomRatio(Math.min(vr.width / (double) dim.width, vr.height / (double) dim.height));
    }

    //----------//
    // fitWidth //
    //----------//
    /**
     * Define the zoom ratio so that the full width of the model can be visible.
     * We force the zoom ratio to stand within the range of the slider.
     *
     * @return true if we have been able to fit the display width, false otherwise which happens
     *         when the display width is zero
     */
    public boolean fitWidth ()
    {
        Rectangle vr = view.getVisibleRect();
        Dimension dim = view.getModelSize();
        logger.debug("fitWidth vr={} dim={}", vr, dim);

        if ((vr.width > 0) && (dim.width > 0)) {
            setZoomRatio(vr.width / (double) dim.width);

            return true;
        } else {
            return false;
        }
    }

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
     * Retrieve the coordinates of what is currently the focus point of the display.
     * Typically, this is the center of the rubber rectangle.
     *
     * @return the focus point, or null if the view is null
     */
    public Point getRubberFocus ()
    {
        if (view != null) {
            Point center = view.rubber.getCenter();

            if (center != null) {
                logger.debug("getRubberFocus rubber center={}", center);

                return center; // Of rubber band
            } else {
                logger.debug("getRubberFocus panelcenter={}", view.getPanelCenter());

                return view.getPanelCenter(); // Of visible rectangle
            }
        } else {
            logger.warn("getRubberFocus : no rubber, no view ???");

            return null;
        }
    }

    //--------------------//
    // getRubberSelection //
    //--------------------//
    /**
     * Retrieve a copy of the rubber rectangle, or a zero-height, zero-width rectangle
     * at {@code getRubberFocus()} if the rubber rectangle does not exist.
     *
     * @return the rubber selection, or null if the view is null
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
    // getView //
    //---------//
    /**
     * Report the view displayed in the view pane
     *
     * @return the view
     */
    public RubberPanel getView ()
    {
        return view;
    }

    //---------//
    // setView //
    //---------//
    /**
     * Allow to set the panel to be displayed
     *
     * @param view the pre-built panel
     */
    public void setView (RubberPanel view)
    {
        // Display the view in the scrollpane
        component.setViewportView(view);

        // At the end, to avoid too early setting of the zoom ratio
        this.view = view;
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
        logger.debug("setZoomRatio zoomRatio={}", zoomRatio);

        if (view.getZoom() != null) {
            view.getZoom().setRatio(zoomRatio);
        } else {
            logger.warn("setZoomRatio. No zoom assigned");
        }
    }

    //----------//
    // bindKeys //
    //----------//
    private void bindKeys (JComponent component)
    {
        InputMap inputMap;
        //        inputMap = component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        //        inputMap = component.getInputMap(JComponent.WHEN_FOCUSED); // Default
        inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        ActionMap actionMap = component.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("UP"), "UpAction");
        actionMap.put("UpAction", new UpAction());

        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "DownAction");
        actionMap.put("DownAction", new DownAction());

        inputMap.put(KeyStroke.getKeyStroke("shift UP"), "ShiftUpAction");
        actionMap.put("ShiftUpAction", new ShiftUpAction());

        inputMap.put(KeyStroke.getKeyStroke("shift DOWN"), "ShiftDownAction");
        actionMap.put("ShiftDownAction", new ShiftDownAction());

        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "LeftAction");
        actionMap.put("LeftAction", new LeftAction());

        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "RightAction");
        actionMap.put("RightAction", new RightAction());

        inputMap.put(KeyStroke.getKeyStroke("shift LEFT"), "ShiftLeftAction");
        actionMap.put("ShiftLeftAction", new ShiftLeftAction());

        inputMap.put(KeyStroke.getKeyStroke("shift RIGHT"), "ShiftRightAction");
        actionMap.put("ShiftRightAction", new ShiftRightAction());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer unitIncrement = new Constant.Integer(
                "Pixels",
                20,
                "Size of mouse wheel increment for ScrollView");
    }

    private class DownAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final JScrollBar vertical = component.getVerticalScrollBar();
            vertical.setValue(vertical.getValue() + vertical.getUnitIncrement());
        }
    }

    private class LeftAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final JScrollBar horizontal = component.getHorizontalScrollBar();
            horizontal.setValue(horizontal.getValue() - horizontal.getUnitIncrement());
        }
    }

    private class RightAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final JScrollBar horizontal = component.getHorizontalScrollBar();
            horizontal.setValue(horizontal.getValue() + horizontal.getUnitIncrement());
        }
    }

    private class ShiftDownAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final JScrollBar vertical = component.getVerticalScrollBar();
            vertical.setValue(vertical.getValue() + 1);
        }
    }

    private class ShiftLeftAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final JScrollBar horizontal = component.getHorizontalScrollBar();
            horizontal.setValue(horizontal.getValue() - 1);
        }
    }

    private class ShiftRightAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final JScrollBar horizontal = component.getHorizontalScrollBar();
            horizontal.setValue(horizontal.getValue() + 1);
        }
    }

    private class ShiftUpAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final JScrollBar vertical = component.getVerticalScrollBar();
            vertical.setValue(vertical.getValue() - 1);
        }
    }

    private class UpAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final JScrollBar vertical = component.getVerticalScrollBar();
            vertical.setValue(vertical.getValue() - vertical.getUnitIncrement());
        }
    }
}
