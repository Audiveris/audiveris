//----------------------------------------------------------------------------//
//                                                                            //
//                           S c o r e E d i t o r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ScoreSheetBridge;
import omr.score.common.PagePoint;
import omr.score.common.ScoreLocation;
import omr.score.common.ScorePoint;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.common.UnitDimension;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import static omr.score.ui.ScoreConstants.*;

import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.ScoreLocationEvent;
import omr.selection.SelectionHint;
import omr.selection.UserEvent;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetAssembly;

import omr.ui.PixelCount;
import omr.ui.util.Panel;
import omr.ui.view.LogSlider;
import omr.ui.view.Rubber;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;
import omr.ui.view.Zoom;

import omr.util.Implement;
import omr.util.WeakPropertyChangeListener;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;

/**
 * Class <code>ScoreEditor</code> is a specific ScoreView meant for the
 * edition of all the systems of a score. As any other score view, it relies on
 * a provided layout orientation (horizontal or vertical), plus some painting
 * parameters.
 * <p>This editor provides additional features such as well as a display zoom,
 * the ability to designate a point or rectangle, and a contextual popup menu.
 *
 * @author Hervé Bitteur
 */
public class ScoreEditor
    extends ScoreView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreEditor.class);

    //~ Instance fields --------------------------------------------------------

    /** Zoom log slider */
    LogSlider slider = new LogSlider(2, 5, LogSlider.VERTICAL, -2, 1, 0);

    /** Display zoom */
    private final Zoom zoom = new Zoom(slider, 0.5); // Default ratio set to 1/2

    /** Mouse rubber */
    private final Rubber rubber = new Rubber(zoom);

    /** The displayed view */
    private final MyView view;

    /** The scroll pane */
    private final ScrollView scrollPane;

    /** The info zone */
    private final JLabel info = new JLabel("Score");

    /** The scroll pane + info zone */
    private final Panel compoundPanel = new Panel();

    /** Related popup menu */
    private final ScoreMenu scoreMenu;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // ScoreEditor //
    //-------------//
    /**
     * Create the view dedicated to the edition of the provided score.
     *
     * @param score the score to display
     */
    public ScoreEditor (Score score)
    {
        super(
            score,
            score.getLayout(
                LayoutParameter.getInstance().getScoreOrientation()),
            PaintingParameters.getInstance());

        scoreLayout.computeLayout();

        if (logger.isFineEnabled()) {
            logger.fine("new ScoreEditor on " + score);
        }

        view = new MyView(zoom, rubber);
        scrollPane = new ScrollView(view);

        // Layout
        info.setHorizontalAlignment(SwingConstants.LEFT);
        compoundPanel.setLayout(new BorderLayout());
        compoundPanel.add(slider, BorderLayout.WEST);
        compoundPanel.add(scrollPane.getComponent(), BorderLayout.CENTER);
        compoundPanel.add(info, BorderLayout.SOUTH);

        // Popup
        scoreMenu = new ScoreMenu(this);

        // Compute origin of all systems
        update();

        // Bridge companion between Score and Sheet locations both ways
        new ScoreSheetBridge(score);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the encapsulated UI component
     *
     * @return the UI component
     */
    public Component getComponent ()
    {
        return compoundPanel;
    }

    //-------------//
    // getViewport //
    //-------------//
    public JViewport getViewport ()
    {
        return scrollPane.getComponent()
                         .getViewport();
    }

    //-------//
    // close //
    //-------//
    /**
     * Close the score display, by removing it from the enclosing tabbed pane.
     */
    @Override
    public void close ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("Closing scoreView");
        }

        Sheet sheet = score.getSheet();

        if (sheet != null) {
            SheetAssembly assembly = sheet.getAssembly();

            if (assembly != null) {
                assembly.closeScoreEditor();
            }
        }
    }

    //-----------//
    // highLight //
    //-----------//
    /**
     * Highlight the corresponding slot within the score display, using the
     * values of measure and slot.
     * @param measure the measure that contains the highlighted slot
     * @param slot the slot to highlight
     */
    public void highLight (final Measure measure,
                           final Slot    slot)
    {
        SwingUtilities.invokeLater(
            new Runnable() {
                    public void run ()
                    {
                        view.highLight(measure, slot);
                    }
                });
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return a string based on the score XML file name
     */
    @Override
    public String toString ()
    {
        return "{ScoreEditor " + score.getRadix() + "}";
    }

    //--------//
    // update //
    //--------//
    /**
     * Update the display when some parameters or size have changed
     */
    @Override
    public void update ()
    {
        view.setModelSize(scoreLayout.getScoreDimension());
        zoom.fireStateChanged();

        view.updateSelection();
    }

    //-----------//
    // tellPoint //
    //-----------//
    private void tellPoint (ScorePoint  scrPt,
                            PagePoint   pagPt,
                            SystemPoint sysPt)
    {
        StringBuilder sb = new StringBuilder();

        if (scrPt != null) {
            sb.append("ScorePoint")
              .append(" X:")
              .append(scrPt.x)
              .append(" Y:")
              .append(scrPt.y)
              .append(" ");
        }

        if (pagPt != null) {
            sb.append("PagePoint")
              .append(" X:")
              .append(pagPt.x)
              .append(" Y:")
              .append(pagPt.y)
              .append(" ");
        }

        if (sysPt != null) {
            sb.append("SystemPoint")
              .append(" X:")
              .append(sysPt.x)
              .append(" Y:")
              .append(sysPt.y)
              .append(" ");
        }

        info.setText(sb.toString());
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        PixelCount measureMargin = new PixelCount(
            10,
            "Number of pixels as margin when highlighting a measure");
    }

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends RubberPanel
        implements PropertyChangeListener
    {
        //~ Instance fields ----------------------------------------------------

        // Currently highlighted slot & measure
        private Slot    highlightedSlot;
        private Measure highlightedMeasure;

        //~ Constructors -------------------------------------------------------

        //--------//
        // MyView //
        //--------//
        MyView (Zoom   zoom,
                Rubber rubber)
        {
            super(zoom, rubber);
            setName("ScoreEditor-MyPanel");

            // Initialize drawing colors, border, opacity.
            setBackground(new Color(255, 255, 220));
            setForeground(Color.black);

            rubber.setMouseMonitor(this);

            // Explicitly register the display to Score Selection
            setLocationService(
                score.getSheet().getSelectionService(),
                ScoreLocationEvent.class);
            subscribe();

            // Force selection update
            updateSelection();

            // Weakly listen to PaintingParameters properties
            PaintingParameters.getInstance()
                              .addPropertyChangeListener(
                new WeakPropertyChangeListener(this));
        }

        //~ Methods ------------------------------------------------------------

        //----------------------//
        // getSelectedRectangle //
        //----------------------//
        /**
         * This method overrides the computation of selected rectangle, because
         * the score location is a combination of system id and of a system
         * rectangle relative to the system. So we must compute the related
         * absolute rectangle
         * @return the absolute (non system-based) selected rectangle, if any
         */
        @Override
        public Rectangle getSelectedRectangle ()
        {
            ScoreLocationEvent locationEvent = (ScoreLocationEvent) locationService.getLastEvent(
                locationClass);

            if (locationEvent != null) {
                ScoreLocation scoreLocation = locationEvent.location;

                if (scoreLocation != null) {
                    SystemRectangle rect = scoreLocation.rectangle;

                    if (rect != null) {
                        // Beware, the id may no longer be valid
                        try {
                            int        id = scoreLocation.systemId;
                            SystemView systemView = scoreLayout.getSystemView(
                                id);

                            return systemView.toScoreRectangle(rect);
                        } catch (Exception ignored) {
                            return null;
                        }
                    }
                }
            }

            return null;
        }

        //-----------------//
        // contextSelected //
        //-----------------//
        @Override
        public void contextSelected (Point         pt,
                                     MouseMovement movement)
        {
            super.pointSelected(pt, movement);

            // Context parameters
            ScorePoint scrPt = new ScorePoint(pt.x, pt.y);

            // Update the popup menu according to selected scores
            scoreMenu.updateMenu(scrPt);

            // Show the popup menu
            scoreMenu.getPopup()
                     .show(
                view,
                getZoom().scaled(scrPt.x) + 20,
                getZoom().scaled(scrPt.y) + 30);
        }

        //-----------//
        // highLight //
        //-----------//
        /**
         * Make the provided slot stand out
         * @param measure the current measure
         * @param slot the current slot
         */
        public void highLight (Measure measure,
                               Slot    slot)
        {
            this.highlightedMeasure = measure;
            this.highlightedSlot = slot;

            // Safer
            if ((measure == null) || (slot == null)) {
                repaint(); // To erase previous highlight

                return;
            }

            ScoreSystem     system = measure.getSystem();
            UnitDimension   dimension = system.getDimension();
            SystemView      systemView = scoreLayout.getSystemView(system);
            SystemRectangle systemBox = new SystemRectangle(
                0,
                0,
                dimension.width,
                dimension.height + STAFF_HEIGHT);

            // If the current measure is at the beginning of a system,
            // make the most of this (new) system as visible as possible
            // We need absolute rectangle (non system-based)
            if (measure.getPreviousSibling() == null) {
                showFocusLocation(systemView.toScoreRectangle(systemBox));
            }

            // Make the measure rectangle visible
            SystemRectangle rect = measure.getBox();
            int             margin = constants.measureMargin.getValue();
            // Actually, use the whole system height
            rect.y = systemBox.y;
            rect.height = systemBox.height;
            rect.grow(margin, margin);
            showFocusLocation(systemView.toScoreRectangle(rect));
        }

        //---------//
        // onEvent //
        //---------//
        /**
         * Call-back triggered when location has been modified
         *
         * @param event the notified event
         */
        @Override
        public void onEvent (UserEvent event)
        {
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                if (logger.isFineEnabled()) {
                    logger.fine("ScoreEditor: onEvent " + event);
                }

                // Show location in display (default dehavior)
                super.onEvent(event);

                if (event instanceof ScoreLocationEvent) {
                    // Display location coordinates
                    ScoreLocation scoreLocation = ((ScoreLocationEvent) event).location;

                    if (scoreLocation != null) {
                        SystemRectangle rect = scoreLocation.rectangle;

                        if (rect != null) {
                            int         id = scoreLocation.systemId;
                            SystemView  systemView = scoreLayout.getSystemView(
                                id);
                            ScoreSystem system = systemView.getSystem();
                            SystemPoint sysPt = new SystemPoint(rect.x, rect.y);
                            ScorePoint  scrPt = systemView.toScorePoint(sysPt);
                            PagePoint   pagPt = system.toPagePoint(sysPt);
                            tellPoint(scrPt, pagPt, sysPt);
                        } else {
                            tellPoint(null, null, null);
                        }
                    } else {
                        tellPoint(null, null, null);
                    }
                }
            } catch (Exception ex) {
                logger.warning(getClass().getName() + " onEvent error", ex);
            }
        }

        //----------------//
        // propertyChange //
        //----------------//
        @Implement(PropertyChangeListener.class)
        public void propertyChange (PropertyChangeEvent evt)
        {
            repaint();
        }

        //--------//
        // render //
        //--------//
        @Override
        public void render (Graphics g)
        {
            ScorePainter painter = new ScorePainter(scoreLayout, g, zoom);
            score.accept(painter);

            if (highlightedSlot != null) {
                painter.drawAbsoluteSlot(
                    true,
                    highlightedMeasure,
                    highlightedSlot,
                    Color.MAGENTA);
            }
        }

        //-------------------//
        // getEventRectangle //
        //-------------------//
        @Override
        protected Rectangle getEventRectangle (LocationEvent event)
        {
            if (event instanceof ScoreLocationEvent) {
                ScoreLocationEvent locationEvent = (ScoreLocationEvent) event;
                ScoreLocation      location = locationEvent.getData();

                if (location != null) {
                    ScoreSystem system = score.getSystemById(location.systemId);
                    SystemView  systemView = scoreLayout.getSystemView(system);

                    return systemView.toScoreRectangle(location.rectangle);
                } else {
                    return null;
                }
            } else {
                return super.getEventRectangle(event);
            }
        }

        //------------------//
        // setFocusLocation //
        //------------------//
        /**
         * Modifies the location information. This method simply posts the
         * score location information on the sheet event service.
         *
         * @param rect the location information
         * @param movement the button movement
         * @param hint the related selection hint
         */
        @Override
        protected void setFocusLocation (Rectangle     rect,
                                         MouseMovement movement,
                                         SelectionHint hint)
        {
            if (logger.isFineEnabled()) {
                logger.fine("setFocusLocation rect=" + rect + " hint=" + hint);
            }

            ScoreLocation scoreLocation = null;

            if (rect != null) {
                ScorePoint  scrPt = new ScorePoint(rect.x, rect.y);
                ScoreSystem system = scoreLayout.scoreLocateSystem(scrPt);
                SystemView  systemView = scoreLayout.getSystemView(system);
                SystemPoint sysPt = systemView.toSystemPoint(scrPt);
                scoreLocation = new ScoreLocation(system.getId(), sysPt);
            }

            // Write & forward the new selection
            publish(
                new ScoreLocationEvent(this, hint, movement, scoreLocation));
        }

        //--------------------------//
        // getSelectedScoreLocation //
        //--------------------------//
        private ScoreLocation getSelectedScoreLocation ()
        {
            ScoreLocationEvent locationEvent = (ScoreLocationEvent) locationService.getLastEvent(
                locationClass);

            return (locationEvent != null) ? locationEvent.getData() : null;
        }

        //-----------------//
        // updateSelection //
        //-----------------//
        private void updateSelection ()
        {
            ScoreLocation location = getSelectedScoreLocation();

            if (location != null) {
                publish(new ScoreLocationEvent(this, null, null, location));
            }
        }
    }
}
