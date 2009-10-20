//----------------------------------------------------------------------------//
//                                                                            //
//                         S h e e t A s s e m b l y                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.Main;

import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.common.PixelRectangle;
import omr.score.ui.ScoreView;

import omr.selection.SheetLocationEvent;

import omr.sheet.Sheet;

import omr.step.Step;

import omr.ui.BoardsPane;
import omr.ui.GuiActions;
import omr.ui.MainGui;
import omr.ui.PixelCount;
import omr.ui.util.Panel;
import omr.ui.view.LogSlider;
import omr.ui.view.Rubber;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;
import omr.ui.view.Zoom;

import omr.util.Implement;
import omr.util.WeakPropertyChangeListener;

import org.bushe.swing.event.EventService;

import java.awt.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>SheetAssembly</code> is a UI assembly dedicated to the display of
 * one sheet, gathering: <ul>
 *
 * <li>a single {@link omr.score.ui.ScoreView}</li>
 *
 * <li>a {@link Zoom} with its dedicated graphical {@link LogSlider}</li>
 *
 * <li>a mouse adapter {@link Rubber}</li>
 *
 * <li>a tabbed collection of {@link ScrollView}'s for all views of this sheet.
 *
 * </ul><p>Although not part of the same Swing container, the SheetAssembly also
 * refers to a collection of {@link BoardsPane}'s which is parallel to the
 * collection of ScrollView's.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SheetAssembly
    implements ChangeListener, PropertyChangeListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetAssembly.class);

    //~ Instance fields --------------------------------------------------------

    /** Map: component -> view tab */
    private final Map<JScrollPane, ViewTab> tabs = new HashMap<JScrollPane, ViewTab>();

    /** Tabbed container for all views of the sheet */
    private final JTabbedPane tabbedPane = new JTabbedPane();

    /** Split pane for score and sheet views */
    private final JSplitPane splitPane = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT);

    /** To manually control the zoom ratio */
    private final LogSlider slider = new LogSlider(
        2,
        5,
        LogSlider.VERTICAL,
        -3,
        4,
        0);

    /** The concrete UI component */
    private Panel component;

    /** Link with sheet */
    private final Sheet sheet;

    /** Zoom , with default ratio set to 1 */
    private final Zoom zoom = new Zoom(slider, 1);

    /** Mouse adapter */
    private final Rubber rubber = new Rubber(zoom);

    /** Related Score view */
    private ScoreView scoreView;

    /** Service of sheetlocation */
    private EventService locationService;

    /** Previously selected tab */
    private ViewTab previousTab = null;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // SheetAssembly //
    //---------------//
    /**
     * Create a new <code>SheetAssembly</code> instance, dedicated to one sheet.
     *
     * @param sheet the related sheet
     */
    public SheetAssembly (Sheet sheet)
    {
        if (logger.isFineEnabled()) {
            logger.fine("creating SheetAssembly on " + sheet);
        }

        component = new Panel();

        // Cross links between sheet and its assembly
        this.sheet = sheet;
        sheet.setAssembly(this);

        locationService = sheet.getSelectionService();

        // GUI stuff
        slider.setToolTipText("Adjust Zoom Ratio");
        tabbedPane.addChangeListener(this);

        // General layout
        component.setLayout(new BorderLayout());
        component.setNoInsets();

        Panel views = new Panel();
        views.setNoInsets();
        views.setLayout(new BorderLayout());
        views.add(slider, BorderLayout.WEST);
        views.add(tabbedPane, BorderLayout.CENTER);
        splitPane.setBottomComponent(views);
        splitPane.setOneTouchExpandable(true);
        component.add(splitPane);

        splitPane.setBorder(null);
        splitPane.setDividerSize(2);

        // Weakly listen to GUI Actions parameters
        GuiActions.getInstance()
                  .addPropertyChangeListener(
            GuiActions.ERRORS_DISPLAYED,
            new WeakPropertyChangeListener(this));

        if (logger.isFineEnabled()) {
            logger.fine("SheetAssembly created.");
        }
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
    public JComponent getComponent ()
    {
        return component;
    }

    //--------------//
    // setScoreView //
    //--------------//
    /**
     * Assign the ScoreView part to the assembly
     * @param scoreView the Score View
     */
    public void setScoreView (final ScoreView scoreView)
    {
        if (this.scoreView != null) {
            closeScoreView();
        }

        // Position score view as the higher part of the splitPane
        splitPane.setTopComponent(scoreView.getComponent());
        splitPane.setDividerLocation(constants.scoreSheetDivider.getValue());
        this.scoreView = scoreView;
    }

    //-----------------//
    // getSelectedView //
    //-----------------//
    /**
     * Report the tabbed view currently selected
     *
     * @return the current tabbed view
     */
    public ScrollView getSelectedView ()
    {
        JScrollPane comp = (JScrollPane) tabbedPane.getSelectedComponent();

        if (comp != null) {
            ViewTab tab = tabs.get(comp);

            if (tab != null) {
                return tab.scrollView;
            }
        }

        return null;
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the sheet this assembly is related to
     *
     * @return the related sheet
     */
    public Sheet getSheet ()
    {
        return sheet;
    }

    //--------------//
    // setZoomRatio //
    //--------------//
    /**
     * Modify the ratio of the global zoom for all views of the sheet
     *
     * @param ratio the new display ratio
     */
    public void setZoomRatio (double ratio)
    {
        zoom.setRatio(ratio);
    }

    //------------//
    // addViewTab //
    //------------//
    /**
     * Add a new tab, that contains a new view on the sheet
     * @param step the step whose results are displayed in this tab
     * @param sv the view on the sheet
     * @param boardsPane the board pane associated to the tab
     */
    public void addViewTab (Step       step,
                            ScrollView sv,
                            BoardsPane boardsPane)
    {
        boardsPane.setName(sheet.getRadix() + ":" + step.label);

        if (logger.isFineEnabled()) {
            logger.fine(
                "addViewTab begin " + step.label + " boardsPane=" + boardsPane +
                " comp=@" + Integer.toHexString(sv.getComponent().hashCode()));
        }

        // Remove any existing viewTab with the same label
        removeViewTab(step);

        // Make the new view reuse the common zoom and rubber instances
        sv.getView()
          .setZoom(zoom);
        sv.getView()
          .setRubber(rubber);

        // Set the model size
        if (sheet.getPicture() != null) {
            sv.getView()
              .setModelSize(sheet.getPicture().getDimension());
        }

        // Force scroll bar computations
        zoom.fireStateChanged();
        tabs.put(sv.getComponent(), new ViewTab(step.label, boardsPane, sv));

        // Actually insert a Swing tab
        tabbedPane.addTab(step.label, sv.getComponent());
        tabbedPane.setSelectedComponent(sv.getComponent());

        if (logger.isFineEnabled()) {
            logger.fine(
                "addViewTab end " + step.label + " boardsPane=" + boardsPane);
        }
    }

    //------------------//
    // assemblySelected //
    //------------------//
    /**
     * Method called when this sheet assembly is selected (since we can have
     * several sheets displayed, each one with its own sheet assembly). This is
     * called from {@link omr.sheet.ui.SheetsController} when the tab of another
     * sheet is selected.
     */
    public void assemblySelected ()
    {
        if (logger.isFineEnabled()) {
            logger.fine(sheet.getRadix() + " assemblySelected");
        }

        // Display current context (no reconnection required)
        displayContext( /* connectBoards => */
        false);

        // Display the errors pane of this assembly?
        MainGui gui = Main.getGui();

        if (GuiActions.getInstance()
                      .isErrorsDisplayed()) {
            gui.showErrorsPane(getErrorsPane());
        } else {
            gui.hideErrorsPane();
        }
    }

    //-------//
    // close //
    //-------//
    /**
     * Close the assembly, by removing it from the containing sheet tabbed pane.
     */
    public void close ()
    {
        // Save current value for sheet divider (height of scoreview)
        int divider = splitPane.getDividerLocation();

        if (divider > 0) {
            constants.scoreSheetDivider.setValue(divider);
        }

        MainGui gui = Main.getGui();
        gui.removeBoardsPane(); // Disconnect boards pane
        gui.hideErrorsPane(); // Disconnect errors pane (USEFUL??? TODO)

        // Disconnect all keyboard bindings from PixelBoard's (as a workaround
        // for a Swing memory leak)
        for (ViewTab tab : tabs.values()) {
            BoardsPane pane = tab.boardsPane;

            for (Component topComp : pane.getComponent()
                                         .getComponents()) {
                for (Component comp : ((Container) topComp).getComponents()) {
                    if (comp instanceof JComponent) {
                        ((JComponent) comp).resetKeyboardActions();
                    }
                }
            }
        }

        tabs.clear(); // Useful ???
    }

    //----------------//
    // closeScoreView //
    //----------------//
    /**
     * Close the score view part
     */
    public void closeScoreView ()
    {
        if (scoreView != null) {
            if (logger.isFineEnabled()) {
                logger.fine("Closing scoreView for " + scoreView.getScore());
            }

            splitPane.setTopComponent(null);
            scoreView = null;
        }
    }

    //----------------//
    // propertyChange //
    //----------------//
    /**
     * Called whenever the property ERRORS_DISPLAYED has changed in the
     * GuiActions instance. This will trigger the inclusion or exclusion of the
     * errors panel into/from the assembly display.
     * @param evt unused
     */
    @Implement(PropertyChangeListener.class)
    public void propertyChange (PropertyChangeEvent evt)
    {
        assemblySelected();
    }

    //-----------//
    // selectTab //
    //-----------//
    /**
     * Force a tab selection programmatically
     * @param step the step whose related tab must be selected
     */
    public void selectTab (Step step)
    {
        final String title = step.label;

        for (int i = 0, count = tabbedPane.getTabCount(); i < count; i++) {
            if (tabbedPane.getTitleAt(i)
                          .equals(title)) {
                tabbedPane.setSelectedIndex(i);

                if (logger.isFineEnabled()) {
                    logger.fine("Selected view tab " + title);
                }

                return;
            }
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * This method is called whenever another view tab is selected in the Sheet
     * Assembly (or when a tab is removed)
     *
     * @param e the originating change event (not used actually)
     */
    @Implement(ChangeListener.class)
    public void stateChanged (ChangeEvent e)
    {
        JScrollPane comp = (JScrollPane) tabbedPane.getSelectedComponent();

        if (comp != null) {
            ViewTab currentTab = tabs.get(comp);

            if (currentTab != previousTab) {
                if (previousTab != null) {
                    viewTabDeselected(previousTab);
                }

                viewTabSelected(currentTab);
                previousTab = currentTab;
            }
        } else {
            previousTab = null;
        }
    }

    //---------------//
    // getErrorsPane //
    //---------------//
    /**
     * Report the UI pane dedicated to the current errors
     * @return the errors pane
     */
    private JComponent getErrorsPane ()
    {
        return sheet.getErrorsEditor()
                    .getComponent();
    }

    //----------------//
    // displayContext //
    //----------------//
    private void displayContext (boolean connectBoards)
    {
        // Make sure the tab is ready
        JScrollPane comp = (JScrollPane) tabbedPane.getSelectedComponent();

        if (comp != null) {
            // Retrieve the proper boards pane
            BoardsPane boardsPane = tabs.get(comp).boardsPane;

            if (logger.isFineEnabled()) {
                logger.fine("displaying " + boardsPane);
            }

            // (Re)connect the boards to their selection inputs?
            if (connectBoards) {
                boardsPane.shown();
            }

            // Display the boards pane related to the selected view
            Main.getGui()
                .setBoardsPane(boardsPane.getComponent());
        }
    }

    //---------------//
    // removeViewTab //
    //---------------//
    /**
     * Remove an existing view tab for the provided step, if such a tab exists
     * @param step the provided step
     * @return true if a view tab has been actually removed
     */
    private boolean removeViewTab (Step step)
    {
        for (Map.Entry<JScrollPane, ViewTab> entry : tabs.entrySet()) {
            ViewTab tab = entry.getValue();

            if (tab.title.equals(step.label)) {
                ScrollView sv = tab.scrollView;
                sv.getView()
                  .unsetZoom(zoom);
                sv.getView()
                  .unsetRubber(rubber);
                tabbedPane.remove(sv.getComponent());
                tabs.remove(sv.getComponent());

                if (logger.isFineEnabled()) {
                    logger.fine("Removed tab: " + step.label);
                }

                return true;
            }
        }

        return false;
    }

    //-------------------//
    // viewTabDeselected //
    //-------------------//
    /**
     * Called when moving away from the provided tab
     * @param tab the tab we are leaving from
     */
    private void viewTabDeselected (ViewTab tab)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "SheetAssembly: " + sheet.getRadix() +
                " viewTabDeselected for " + tab.title);
        }

        // Disconnection of events
        tab.scrollView.getView()
                      .unsubscribe();

        // Disconnection of related boards
        tab.boardsPane.hidden();
    }

    //-----------------//
    // viewTabSelected //
    //-----------------//
    /**
     * Called when arriving to a provided tab
     * @param tab the tab we are moving to
     */
    private void viewTabSelected (ViewTab tab)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "SheetAssembly: " + sheet.getRadix() + " viewTabSelected for " +
                tab.title);
        }

        ScrollView  scrollView = tab.scrollView;
        RubberPanel view = scrollView.getView();

        // Link rubber with proper view
        rubber.connectComponent(view);
        rubber.setMouseMonitor(scrollView.getView());

        // Keep previous scroll bar positions
        if (previousTab != null) {
            JScrollPane prev = previousTab.scrollView.getComponent();
            scrollView.getComponent()
                      .getVerticalScrollBar()
                      .setValue(prev.getVerticalScrollBar().getValue());
            scrollView.getComponent()
                      .getHorizontalScrollBar()
                      .setValue(prev.getHorizontalScrollBar().getValue());
        }

        // Make connections to events
        scrollView.getView()
                  .subscribe();

        // Restore display of proper context
        displayContext( /* connectBoards => */
        true);

        // Force update
        SheetLocationEvent locationEvent = (SheetLocationEvent) locationService.getLastEvent(
            SheetLocationEvent.class);
        PixelRectangle     location = (locationEvent != null)
                                      ? locationEvent.getData() : null;

        if (location != null) {
            locationService.publish(
                new SheetLocationEvent(this, null, null, location));
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

        /** Where the separation between score and sheet views should be */
        PixelCount scoreSheetDivider = new PixelCount(
            200,
            "Where the separation between score and sheet views should be");
    }

    //---------//
    // ViewTab //
    //---------//
    /**
     * A simple structure to gather the various aspects of a view tab.
     * All instances are kept in the tabs map.
     */
    private static class ViewTab
    {
        //~ Instance fields ----------------------------------------------------

        BoardsPane boardsPane; // Related boards pane
        ScrollView scrollView; // Component in the JTabbedPane
        String     title; // Title used for the tab

        //~ Constructors -------------------------------------------------------

        public ViewTab (String     title,
                        BoardsPane boardsPane,
                        ScrollView scrollView)
        {
            this.title = title;
            this.boardsPane = boardsPane;
            this.scrollView = scrollView;
        }
    }
}