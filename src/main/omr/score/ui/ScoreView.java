//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e V i e w                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ScoreSheetBridge;
import omr.score.common.PagePoint;
import omr.score.common.ScoreLocation;
import omr.score.common.ScorePoint;
import omr.score.common.ScoreRectangle;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.common.UnitDimension;
import omr.score.entity.Measure;
import omr.score.entity.ScorePart;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import static omr.score.ui.ScoreConstants.*;
import omr.score.visitor.PaintingParameters;
import omr.score.visitor.ScorePainter;

import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.ScoreLocationEvent;
import omr.selection.SelectionHint;
import omr.selection.UserEvent;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetAssembly;

import omr.ui.PixelCount;
import omr.ui.util.Panel;
import omr.ui.view.Rubber;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;
import omr.ui.view.Zoom;

import omr.util.TreeNode;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

/**
 * Class <code>ScoreView</code> encapsulates the display of all the systems of
 * a score (which may encompass several pages & systems). It has an orientation
 * (horizontal or vertical) as well as a display zoom ratio.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreView.class);

    //~ Instance fields --------------------------------------------------------

    /** The related score */
    private final Score score;

    /**
     * Should we display systems using a horizontal layout (side by side) rather
     * than a vertical layout (systems one above the other).
     */
    private ScoreOrientation orientation = PaintingParameters.getInstance()
                                                             .getScoreOrientation();

    /**
     * Display zoom. NOTA: To use a value different from 0.5, the definition
     * of symbols bitmaps must be redone at scale 1, and the painting of these
     * symbols must be simplified to use the graphics context directly (see the
     * ScorePainter class for modifications)
     */
    private final Zoom zoom = new Zoom(0.5d);

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

    /** The most recent system pointed at */
    private WeakReference<ScoreSystem> recentSystemRef = null;

    /** Sequence of system views, ordered by system id */
    private List<SystemView> systemViews;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // ScoreView //
    //-----------//
    /**
     * Create the view dedicated to the display of the provided score.
     *
     * @param score the score to display
     */
    public ScoreView (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("new ScoreView on " + score);
        }

        this.score = score;
        view = new MyView(zoom, rubber);
        scrollPane = new ScrollView(view);

        // Layout
        info.setHorizontalAlignment(SwingConstants.LEFT);
        compoundPanel.setLayout(new BorderLayout());
        compoundPanel.add(scrollPane.getComponent(), BorderLayout.CENTER);
        compoundPanel.add(info, BorderLayout.SOUTH);

        // Cross referencing between score and its view
        score.addView(this);

        // Popup
        scoreMenu = new ScoreMenu(this);

        // Compute origin of all systems
        computeModelSize();

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

    //----------------//
    // setOrientation //
    //----------------//
    /**
     * Dynamically change the orientation of the score display
     * @param orientation the new orientation
     */
    public void setOrientation (ScoreOrientation orientation)
    {
        this.orientation = orientation;
        update();
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Report the score this view is dedicated to
     *
     * @return the related score
     */
    public Score getScore ()
    {
        return score;
    }

    //---------------//
    // getSystemView //
    //---------------//
    /**
     * Report the specific system view for the system in this specific score
     * view
     * @param system the provided system
     * @return the specific system view
     */
    public SystemView getSystemView (ScoreSystem system)
    {
        return getSystemView(system.getId());
    }

    //-------//
    // close //
    //-------//
    /**
     * Close the score display, by removing it from the enclosing tabbed pane.
     */
    public void close ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("Closing scoreView");
        }

        Sheet sheet = score.getSheet();

        if (sheet != null) {
            SheetAssembly assembly = sheet.getAssembly();

            if (assembly != null) {
                assembly.closeScoreView();
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

    //-------------------//
    // scoreLocateSystem //
    //-------------------//
    /**
     * Retrieve the system 'scrPt' is pointing to, according to the current
     * layout.
     *
     * @param scrPt the point in the SCORE horizontal display
     * @return the nearest system
     */
    public ScoreSystem scoreLocateSystem (ScorePoint scrPt)
    {
        ScoreSystem recentSystem = getRecentSystem();

        if (recentSystem != null) {
            // Check first with most recent system (loosely)
            SystemView systemView = getSystemView(recentSystem);

            switch (systemView.locate(scrPt)) {
            case -1 :

                // Check w/ previous system
                ScoreSystem prevSystem = (ScoreSystem) recentSystem.getPreviousSibling();

                if (prevSystem == null) {
                    // Very first system
                    return recentSystem;
                } else {
                    if (getSystemView(prevSystem)
                            .locate(scrPt) > 0) {
                        return recentSystem;
                    }
                }

                break;

            case 0 :
                return recentSystem;

            case +1 :

                // Check w/ next system
                ScoreSystem nextSystem = (ScoreSystem) recentSystem.getNextSibling();

                if (nextSystem == null) {
                    // Very last system
                    return recentSystem;
                } else {
                    if (getSystemView(nextSystem)
                            .locate(scrPt) < 0) {
                        return recentSystem;
                    }
                }

                break;
            }
        }

        // Recent system is not OK, Browse though all the score systems
        ScoreSystem system = null;

        for (TreeNode node : score.getSystems()) {
            system = (ScoreSystem) node;

            SystemView systemView = getSystemView(system);

            // How do we locate the point wrt the system  ?
            switch (systemView.locate(scrPt)) {
            case -1 : // Point is before system (but after previous), give up.
            case 0 : // Point is within system.
                return setRecentSystem(system);

            case +1 : // Point is after the system, go on.
                break;
            }
        }

        // Return the last system in the score
        return setRecentSystem(system);
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
        return "{ScoreView " + score.getRadix() + "}";
    }

    //--------//
    // update //
    //--------//
    /**
     * Update the display when some parameters or size have changed
     */
    public void update ()
    {
        computeModelSize();
        view.updateSelection();
    }

    //-----------------//
    // setRecentSystem //
    //-----------------//
    private ScoreSystem setRecentSystem (ScoreSystem system)
    {
        recentSystemRef = new WeakReference<ScoreSystem>(system);

        return system;
    }

    //-----------------//
    // getRecentSystem //
    //-----------------//
    private ScoreSystem getRecentSystem ()
    {
        return (recentSystemRef == null) ? null : recentSystemRef.get();
    }

    //---------------//
    // getSystemView //
    //---------------//
    private SystemView getSystemView (int id)
    {
        return systemViews.get(id - 1);
    }

    //------------------//
    // computeModelSize //
    //------------------//
    /**
     * Run computations on the collection of systems so that they are displayed
     * in a nice manner, using  horizontal or vertical layout
     */
    private void computeModelSize ()
    {
        // Get a fresh set of system views
        createSystemViews();

        // Browse all system views and compute the union of all rectangles
        ScoreRectangle scoreContour = null;

        for (TreeNode node : score.getSystems()) {
            ScoreSystem    system = (ScoreSystem) node;
            SystemView     systemView = getSystemView(system);
            ScoreRectangle absSystemContour = systemView.toScoreRectangle(
                system.getContour());

            if (scoreContour == null) {
                scoreContour = absSystemContour;
            } else {
                Rectangle r = scoreContour.union(absSystemContour);
                scoreContour = new ScoreRectangle(r.x, r.y, r.width, r.height);
            }
        }

        view.setModelSize(
            new Dimension(scoreContour.width, scoreContour.height));
        zoom.fireStateChanged();
    }

    //-------------------//
    // createSystemViews //
    //-------------------//
    /**
     * Set the display parameters of each system
     */
    private void createSystemViews ()
    {
        final int        highestTop = score.getHighestSystemTop();
        List<SystemView> views = new ArrayList<SystemView>();
        SystemView       prevSystemView = null;

        for (TreeNode node : score.getSystems()) {
            ScoreSystem     system = (ScoreSystem) node;
            SystemRectangle contour = system.getContour();
            ScorePoint      origin = new ScorePoint();

            if (orientation == ScoreOrientation.HORIZONTAL) {
                if (prevSystemView == null) {
                    // Very first system in the score
                    origin.x = STAFF_MARGIN_WIDTH - contour.x;
                } else {
                    // Not the first system
                    origin.x = (prevSystemView.getDisplayOrigin().x +
                               prevSystemView.getSystem().getDimension().width) +
                               INTER_SYSTEM_WIDTH;
                }

                ScorePart scorePart = system.getFirstPart()
                                            .getScorePart();
                origin.y = STAFF_MARGIN_HEIGHT - highestTop +
                           system.getDummyOffset() +
                           ((scorePart != null)
                            ? scorePart.getDisplayOrdinate() : 0);
            } else {
                if (prevSystemView == null) {
                    // Very first system in the score
                    origin.y = STAFF_MARGIN_HEIGHT - contour.y;
                } else {
                    // Not the first system
                    origin.y = (prevSystemView.getDisplayOrigin().y +
                               prevSystemView.getSystem()
                                             .getDimension().height +
                               STAFF_HEIGHT) + INTER_SYSTEM_HEIGHT;
                }

                origin.x = STAFF_MARGIN_WIDTH - contour.x;
            }

            // Create an immutable view for this system
            recentSystemRef = null;

            SystemView systemView = new SystemView(system, orientation, origin);
            views.add(systemView);
            prevSystemView = systemView;

            if (logger.isFineEnabled()) {
                logger.fine(system + " origin:" + origin);
            }
        }

        // Write the new collection of SystemView instances
        systemViews = views;
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

        PixelCount       measureMargin = new PixelCount(
            10,
            "Number of pixels as margin when highlighting a measure");
        Constant.Boolean popupEnabled = new Constant.Boolean(
            true,
            "Should we allow popup menu in score view?");
    }

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends RubberPanel
    {
        //~ Instance fields ----------------------------------------------------

        // Currently highlighted slot & measure
        private Slot    highlightedSlot;
        private Measure highlightedMeasure;

        //~ Constructors -------------------------------------------------------

        //---------//
        // MyView //
        //---------//
        MyView (Zoom   zoom,
                Rubber rubber)
        {
            super(zoom, rubber);
            setName("ScoreView-MyPanel");

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
                        int        id = scoreLocation.systemId;
                        SystemView systemView = getSystemView(id);

                        return systemView.toScoreRectangle(rect);
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

            // Let the user disable this popup feature if so desired
            if (!constants.popupEnabled.getValue()) {
                return;
            }

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
        public void highLight (Measure measure,
                               Slot    slot)
        {
            this.highlightedMeasure = measure;
            this.highlightedSlot = slot;

            int margin = constants.measureMargin.getValue();

            // Safer
            if ((measure == null) || (slot == null)) {
                repaint(); // To erase previous highlight

                return;
            }

            ScoreSystem   system = measure.getSystem();
            UnitDimension dimension = system.getDimension();
            SystemView    systemView = getSystemView(system);

            // If the current measure is at the beginning of a system,
            // make the most of this (new) system as visible as possible
            // We need absolute rectangle (non system-based)
            if (measure.getPreviousSibling() == null) {
                SystemRectangle rect = new SystemRectangle(
                    0,
                    0,
                    dimension.width,
                    dimension.height + STAFF_HEIGHT);

                showFocusLocation(systemView.toScoreRectangle(rect));
            }

            // Make the measure rectangle visible
            SystemRectangle rect = new SystemRectangle(
                (measure.getLeftX()) - margin,
                -margin,
                measure.getWidth() + (2 * margin),
                dimension.height + STAFF_HEIGHT + (2 * margin));
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
                    logger.fine("ScoreView: onEvent " + event);
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
                            SystemView  systemView = getSystemView(id);
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

        //--------//
        // render //
        //--------//
        @Override
        public void render (Graphics g)
        {
            ScorePainter painter = new ScorePainter(ScoreView.this, g, zoom);
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
                    SystemView  systemView = getSystemView(system);

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
                ScoreSystem system = scoreLocateSystem(scrPt);
                SystemView  systemView = getSystemView(system);
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
            ScoreLocation location = (ScoreLocation) getSelectedScoreLocation();

            if (location != null) {
                publish(new ScoreLocationEvent(this, null, null, location));
            }
        }
    }
}
