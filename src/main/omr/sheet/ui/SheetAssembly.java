//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S h e e t A s s e m b l y                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.OMR;

import omr.sheet.Sheet;
import omr.sheet.SheetStub;

import omr.ui.Board;
import omr.ui.BoardsPane;
import omr.ui.GuiActions;
import omr.ui.selection.LocationEvent;
import omr.ui.selection.SelectionService;
import omr.ui.util.ClosableTabbedPane;
import omr.ui.util.Panel;
import omr.ui.util.UIUtil;
import omr.ui.view.LogSlider;
import omr.ui.view.Rubber;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;
import omr.ui.view.Zoom;

import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code SheetAssembly} is a UI assembly dedicated to the display of one sheet.
 *
 * It gathers: <ul>
 * <li>a {@link Zoom} with its dedicated graphical {@link LogSlider}</li>
 * <li>a mouse adapter {@link Rubber}</li>
 * <li>a tabbed pane of {@link ScrollView}'s for all views of this sheet</li>
 * </ul>
 * <p>
 * Although not part of the same Swing container, the SheetAssembly also refers to a sequence of
 * {@link BoardsPane}'s which is parallel to the sequence of ScrollView's.
 *
 * @author Hervé Bitteur
 */
public class SheetAssembly
        implements ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetAssembly.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Link with sheet stub. */
    @Navigable(false)
    private final SheetStub stub;

    /** The concrete UI component. */
    private final Panel component = new Panel();

    /** To manually control the zoom ratio. */
    private final LogSlider slider = new LogSlider(2, 5, LogSlider.VERTICAL, -3, 5, 0);

    /** Tabbed container for all views of the sheet. */
    private final JTabbedPane viewsPane = new ClosableTabbedPane()
    {
        @Override
        public boolean tabAboutToClose (int tabIndex)
        {
            return OMR.gui.displayConfirmation(
                    getTitleAt(tabIndex) + " tab about to close.\nConfirm?");
        }
    };

    /** Zoom, with default ratio set to 1. */
    private final Zoom zoom = new Zoom(slider, 1);

    /** Mouse adapter. */
    private final Rubber rubber = new Rubber(zoom);

    /** Map: scrollPane -> view tab. */
    private final Map<JScrollPane, ViewTab> tabs = new HashMap<JScrollPane, ViewTab>();

    /** Previously selected tab. */
    private ViewTab previousTab = null;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new {@code SheetAssembly} instance dedicated to one sheet stub.
     *
     * @param stub the related sheet stub
     */
    public SheetAssembly (SheetStub stub)
    {
        logger.debug("creating SheetAssembly on {}", stub);

        this.stub = stub;

        // GUI stuff
        slider.setToolTipText("Adjust Zoom Ratio");

        // To be notified of view selection (manually or programmatically)
        viewsPane.addChangeListener(this);

        logger.debug("SheetAssembly created.");

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addBoard //
    //----------//
    /**
     * Add a board into the BoardsPane corresponding to the provided
     * view tab title.
     *
     * @param tab   the tab of the targeted view
     * @param board the board to add dynamically
     */
    public void addBoard (SheetTab tab,
                          Board board)
    {
        JScrollPane pane = getPane(tab.label);

        if (pane != null) {
            ViewTab viewTab = tabs.get(pane);
            ///logger.warn("Adding " + board + " to " + title);
            viewTab.boardsPane.addBoard(board);
        }
    }

    //------------//
    // addViewTab //
    //------------//
    /**
     * Add a new tab, that contains a new view on the sheet.
     *
     * @param tab        the label to use for the tab
     * @param sv         the view on the sheet
     * @param boardsPane the board pane associated to the tab
     */
    public void addViewTab (SheetTab tab,
                            ScrollView sv,
                            BoardsPane boardsPane)
    {
        addViewTab(tab.label, sv, boardsPane);
    }

    //------------//
    // addViewTab //
    //------------//
    /**
     * Add a new tab, that contains a new view on the sheet.
     *
     * @param label      the label to use for the tab
     * @param sv         the view on the sheet
     * @param boardsPane the board pane associated to the tab
     */
    public void addViewTab (String label,
                            ScrollView sv,
                            BoardsPane boardsPane)
    {
        JScrollPane scroll = sv.getComponent();
        UIUtil.suppressBorders(scroll);

        if (boardsPane != null) {
            boardsPane.setName(label);
        }

        logger.debug(
                "addViewTab begin {} boardsPane={} comp=@{}",
                label,
                boardsPane,
                Integer.toHexString(scroll.hashCode()));

        // Remove any existing viewTab with the same label
        for (ViewTab t : tabs.values()) {
            if (t.title.equals(label)) {
                t.remove();

                break;
            }
        }

        // Register the component
        tabs.put(scroll, new ViewTab(label, boardsPane, sv));

        // Actually insert the related Swing tab (at proper index?)
        viewsPane.addTab(label, scroll);

        // Select this new tab
        viewsPane.setSelectedComponent(scroll);

        logger.debug("addViewTab end {} boardsPane={}", label, boardsPane);
    }

    //------------------//
    // assemblySelected //
    //------------------//
    /**
     * Method called when this sheet assembly is selected.
     * (we can have several sheets displayed, each with its sheet assembly).
     * This is called from {@link omr.sheet.ui.StubsController} when the tab
     * of another sheet is selected.
     */
    public void assemblySelected ()
    {
        logger.debug("{} assemblySelected", stub.getId());

        // Display the related boards
        displayBoards();

        // Display the errors pane of this assembly?
        if (GuiActions.getInstance().isErrorsDisplayed()) {
            OMR.gui.setErrorsPane(stub.getSheet().getErrorsEditor().getComponent());
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
        OMR.gui.removeBoardsPane();

        // Disconnect all keyboard bindings from PixelBoard's (workaround for a Swing memory leak)
        for (ViewTab tab : tabs.values()) {
            tab.disconnectKeyboard();
        }

        // Hide the error messages (for this sheet)
        if (stub.hasSheet()) {
            OMR.gui.removeErrorsPane(stub.getSheet().getErrorsEditor().getComponent());
        }
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component.
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return component;
    }

    //---------//
    // getPane //
    //---------//
    /**
     * Find the view that corresponds to the provided tab title.
     *
     * @param title the tab title.
     * @return the view found, or null
     */
    public JScrollPane getPane (String title)
    {
        for (int i = 0, count = viewsPane.getTabCount(); i < count; i++) {
            if (viewsPane.getTitleAt(i).equals(title)) {
                return (JScrollPane) viewsPane.getComponentAt(i);
            }
        }

        return null;
    }

    //-----------------//
    // getSelectedView //
    //-----------------//
    /**
     * Report the tabbed view currently selected.
     *
     * @return the current tabbed view
     */
    public ScrollView getSelectedView ()
    {
        ViewTab currentTab = getCurrentViewTab();

        if (currentTab != null) {
            return currentTab.scrollView;
        } else {
            return null;
        }
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the sheet this assembly is related to.
     *
     * @return the related sheet
     */
    public Sheet getSheet ()
    {
        return stub.getSheet();
    }

    //---------//
    // getStub //
    //---------//
    public SheetStub getStub ()
    {
        return stub;
    }

    //-----------//
    // renameTab //
    //-----------//
    /**
     * Change the name of a tab.
     *
     * @param oldName old tab name
     * @param newName new tab name
     */
    public void renameTab (String oldName,
                           String newName)
    {
        for (int i = 0, count = viewsPane.getTabCount(); i < count; i++) {
            if (viewsPane.getTitleAt(i).equals(oldName)) {
                viewsPane.setTitleAt(i, newName);

                return;
            }
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reset the assembly to its initial empty state.
     */
    public void reset ()
    {
        close();

        for (ViewTab tab : new ArrayList<ViewTab>(tabs.values())) {
            tab.remove();
        }

        tabs.clear();

        previousTab = null;
    }

    //---------------//
    // selectViewTab //
    //---------------//
    /**
     * Force a tab selection programmatically.
     *
     * @param tab the tab to be selected
     */
    public void selectViewTab (SheetTab tab)
    {
        for (int i = 0, count = viewsPane.getTabCount(); i < count; i++) {
            if (viewsPane.getTitleAt(i).equals(tab.label)) {
                viewsPane.setSelectedIndex(i);
                viewsPane.repaint();

                logger.debug("Selected view tab {}", tab);

                return;
            }
        }
    }

    //--------------//
    // setZoomRatio //
    //--------------//
    /**
     * Modify the ratio of the global zoom for all views of the sheet.
     *
     * @param ratio the new display ratio
     */
    public void setZoomRatio (double ratio)
    {
        zoom.setRatio(ratio);
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * This method is called whenever another view tab is selected
     * in the SheetAssembly (or when a tab is removed).
     *
     * @param e the originating change event (not used)
     */
    @Override
    public void stateChanged (ChangeEvent e)
    {
        ViewTab currentTab = getCurrentViewTab();
        logger.debug(
                "SheetAssembly stateChanged previousTab:{} currentTab:{}",
                previousTab,
                currentTab);

        if (currentTab != previousTab) {
            if (previousTab != null) {
                previousTab.deselected();
            }

            if (currentTab != null) {
                currentTab.selected();
            }
        }

        previousTab = currentTab;
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout of this assembly.
     */
    private void defineLayout ()
    {
        component.setLayout(new BorderLayout());
        component.setNoInsets();
        component.add(slider, BorderLayout.WEST);
        component.add(viewsPane, BorderLayout.CENTER);

        // Avoid slider to react on (and consume) page up/down keys or arrow keys
        InputMap inputMap = slider.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), "none");
        inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "none");
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "none");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "none");
        inputMap.put(KeyStroke.getKeyStroke("UP"), "none");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "none");
    }

    //---------------//
    // displayBoards //
    //---------------//
    /**
     * Make the boards pane visible (for this sheet & view).
     */
    private void displayBoards ()
    {
        // Make sure the view tab is ready
        JScrollPane comp = (JScrollPane) viewsPane.getSelectedComponent();

        if (comp != null) {
            // Retrieve the proper boards pane, if any
            ViewTab tab = tabs.get(comp);

            if (tab != null) {
                tab.displayBoards();
            }
        }
    }

    //-------------------//
    // getCurrentViewTab //
    //-------------------//
    /**
     * Report the ViewTab currently selected, if any.
     *
     * @return the current ViewTab, or null
     */
    private ViewTab getCurrentViewTab ()
    {
        JScrollPane comp = (JScrollPane) viewsPane.getSelectedComponent();

        if (comp != null) {
            return tabs.get(comp);
        } else {
            return null;
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // ViewTab //
    //---------//
    /**
     * A simple structure to gather the various aspects of a view tab.
     * All instances are kept in the {@link SheetAssembly#tabs} map.
     */
    private class ViewTab
    {
        //~ Instance fields ------------------------------------------------------------------------

        String title; // Title used for the tab

        BoardsPane boardsPane; // Related boards pane

        ScrollView scrollView; // Component in the JTabbedPane

        //~ Constructors ---------------------------------------------------------------------------
        public ViewTab (String title,
                        BoardsPane boardsPane,
                        ScrollView scrollView)
        {
            this.title = title;
            this.boardsPane = boardsPane;
            this.scrollView = scrollView;

            // Make the new view reuse the common zoom and rubber instances
            RubberPanel rubberPanel = scrollView.getView();
            rubberPanel.setZoom(zoom);
            rubberPanel.setRubber(rubber);

            // Set the model size?
            if (stub.hasSheet()) {
                Sheet sheet = stub.getSheet();

                if (sheet.getPicture() != null) {
                    rubberPanel.setModelSize(new Dimension(sheet.getWidth(), sheet.getHeight()));
                }
            }

            // Force scroll bar computations
            zoom.fireStateChanged();
        }

        //~ Methods --------------------------------------------------------------------------------
        //------------//
        // deselected //
        //------------//
        /**
         * Run when this tab gets deselected.
         */
        public void deselected ()
        {
            logger.debug("SheetAssembly: {} viewTab.deselected for {}", stub.getId(), this);

            // Disconnection of events
            RubberPanel rubberPanel = scrollView.getView();
            rubberPanel.unsubscribe();

            // Disconnection of related boards, if any
            if ((boardsPane != null) && component.isVisible()) {
                boardsPane.disconnect();
            }
        }

        //--------//
        // remove //
        //--------//
        /**
         * Remove this viewTab instance.
         */
        public void remove ()
        {
            RubberPanel rubberPanel = scrollView.getView();
            rubberPanel.unsetZoom(zoom);
            rubberPanel.unsetRubber(rubber);

            JScrollPane scrollPane = scrollView.getComponent();
            viewsPane.remove(scrollPane);
            tabs.remove(scrollPane);

            logger.debug("Removed tab: {}", this);
        }

        //----------//
        // selected //
        //----------//
        /**
         * Run when this tab gets selected.
         */
        public void selected ()
        {
            logger.debug(
                    "SheetAssembly: {} viewTabSelected for {} dim:{}",
                    stub.getId(),
                    this,
                    scrollView.getView().getPreferredSize());

            // Link rubber with proper view
            RubberPanel rubberPanel = scrollView.getView();
            rubber.connectComponent(rubberPanel);
            rubber.setMouseMonitor(rubberPanel);

            // Make connections to events
            rubberPanel.subscribe();

            // Restore display of proper context
            logger.debug("{} showing:{}", this, component.isShowing());

            if (component.isShowing()) {
                displayBoards();
            }

            // Force update of LocationEvent
            if (stub.hasSheet()) {
                // Service for sheet location events
                SelectionService locationService = stub.getSheet().getLocationService();

                LocationEvent locationEvent = (LocationEvent) locationService.getLastEvent(
                        LocationEvent.class);
                Rectangle location = (locationEvent != null) ? locationEvent.getData() : null;

                if (location != null) {
                    locationService.publish(
                            new LocationEvent(this, locationEvent.hint, null, location));
                }
            }

            // Keep the same scroll bar positions as with previous tab
            if (previousTab != null) {
                JScrollPane prev = previousTab.scrollView.getComponent();
                final int vert = prev.getVerticalScrollBar().getValue();
                final int hori = prev.getHorizontalScrollBar().getValue();
                JScrollPane scrollPane = scrollView.getComponent();
                scrollPane.getVerticalScrollBar().setValue(vert);
                scrollPane.getHorizontalScrollBar().setValue(hori);
            }
        }

        //----------//
        // toString //
        //----------//
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append("{");
            sb.append(title);
            sb.append("}");

            return sb.toString();
        }

        //--------------------//
        // disconnectKeyboard //
        //--------------------//
        private void disconnectKeyboard ()
        {
            if (boardsPane != null) {
                for (Component topComp : boardsPane.getComponent().getComponents()) {
                    for (Component comp : ((Container) topComp).getComponents()) {
                        if (comp instanceof JComponent) {
                            ((JComponent) comp).resetKeyboardActions();
                        }
                    }
                }
            }
        }

        //---------------//
        // displayBoards //
        //---------------//
        private void displayBoards ()
        {
            if (boardsPane != null) {
                // (Re)connect the boards to their selection inputs if needed
                boardsPane.connect();

                // Display the boards pane related to the selected view
                OMR.gui.setBoardsPane(boardsPane.getComponent());
            } else {
                OMR.gui.setBoardsPane(null);
            }
        }
    }
}
