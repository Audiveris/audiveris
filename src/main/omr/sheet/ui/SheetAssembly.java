//----------------------------------------------------------------------------//
//                                                                            //
//                         S h e e t A s s e m b l y                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.Main;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.selection.SheetLocationEvent;

import omr.sheet.Sheet;

import omr.step.Step;

import omr.ui.BoardsPane;
import omr.ui.GuiActions;
import omr.ui.MainGui;
import omr.ui.util.Panel;
import omr.ui.util.UIUtilities;
import omr.ui.view.LogSlider;
import omr.ui.view.Rubber;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;
import omr.ui.view.Zoom;

import omr.util.Implement;
import omr.util.WeakPropertyChangeListener;

import org.bushe.swing.event.EventService;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class <code>SheetAssembly</code> is a UI assembly dedicated to the display of
 * one sheet, gathering: <ul>
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
 * @author Hervé Bitteur
 */
public class SheetAssembly
    implements ChangeListener, PropertyChangeListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetAssembly.class);

    //~ Instance fields --------------------------------------------------------

    /** Link with sheet */
    private final Sheet sheet;

    /** Service of sheetlocation */
    private final EventService locationService;

    /*
     *  component:                                      boardsPane1:
     * +---+------------------------------------+      +----------+
     * | s | viewsPane:                         |      | board A  |boardsPane2:
     * | l |tab1\tab2\_________________________ |      |          |--+
     * | i |                                    |      |          |  |
     * | d |  ViewTab.scrollView:               |      |----------|  |
     * | e |                                    |      | board B  |  |
     * | r |                                    |      |----------|--|
     * |   |                                    |      | board C  |  |
     * |   |                                    |      |          |--|
     * |   |                                    |      |----------|  |
     * |   |                                    |      | board X  |  |
     * +---+------------------------------------+      |          |--|
     *                                                 |          |  |
     *                                                 |          |  |
     *                                                 +----------+  |
     *                                                    |          |
     *                                                    +----------+
     */

    /** The concrete UI component */
    private Panel component = new Panel();

    /** To manually control the zoom ratio */
    private final LogSlider slider = new LogSlider(
        2,
        5,
        LogSlider.VERTICAL,
        -3,
        4,
        0);

    /** Tabbed container for all views of the sheet */
    private final JTabbedPane viewsPane = new JTabbedPane();

    /** Zoom, with default ratio set to 1 */
    private final Zoom zoom = new Zoom(slider, 1);

    /** Mouse adapter */
    private final Rubber rubber = new Rubber(zoom);

    /** Map: scrollPane -> view tab */
    private final Map<JScrollPane, ViewTab> tabs = new HashMap<JScrollPane, ViewTab>();

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

        // Cross links between sheet and its assembly
        this.sheet = sheet;
        sheet.setAssembly(this);

        // Service for sheet location events
        locationService = sheet.getSelectionService();

        // GUI stuff
        slider.setToolTipText("Adjust Zoom Ratio");

        // To be notified of view selection (manually or programmatically)
        viewsPane.addChangeListener(this);

        // Weakly listen to GUI Actions parameters
        GuiActions.getInstance()
                  .addPropertyChangeListener(
            GuiActions.ERRORS_DISPLAYED,
            new WeakPropertyChangeListener(this));

        if (logger.isFineEnabled()) {
            logger.fine("SheetAssembly created.");
        }

        defineLayout();
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
     * @param label the label to use for the tab
     * @param sv the view on the sheet
     * @param boardsPane the board pane associated to the tab
     */
    public void addViewTab (String     label,
                            ScrollView sv,
                            BoardsPane boardsPane)
    {
        JScrollPane scroll = sv.getComponent();
        UIUtilities.suppressBorders(scroll);

        if (boardsPane != null) {
            boardsPane.setName(sheet.getId() + ":" + label);
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                "addViewTab begin " + label + " boardsPane=" + boardsPane +
                " comp=@" + Integer.toHexString(scroll.hashCode()));
        }

        // Remove any existing viewTab with the same label
        for (ViewTab tab : tabs.values()) {
            if (tab.title.equals(label)) {
                tab.remove();

                break;
            }
        }

        // Register the component
        tabs.put(scroll, new ViewTab(label, boardsPane, sv));

        // Actually insert the related Swing tab
        viewsPane.addTab(label, scroll);

        // Select this new tab
        viewsPane.setSelectedComponent(scroll);

        if (logger.isFineEnabled()) {
            logger.fine(
                "addViewTab end " + label + " boardsPane=" + boardsPane);
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
            logger.fine(sheet.getId() + " assemblySelected");
        }

        // Display the related boards
        displayBoards();

        // Display the errors pane of this assembly?
        if (GuiActions.getInstance()
                      .isErrorsDisplayed()) {
            if (logger.isFineEnabled()) {
                logger.fine("Errors window ON");
            }

            Main.getGui()
                .showErrorsPane(getErrorsPane());
        } else {
            if (logger.isFineEnabled()) {
                logger.fine("Errors window OFF");
            }

            Main.getGui()
                .hideErrorsPane(null);
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
        MainGui gui = Main.getGui();
        gui.removeBoardsPane();

        // Disconnect all keyboard bindings from PixelBoard's (as a workaround
        // for a Swing memory leak)
        for (ViewTab tab : tabs.values()) {
            tab.disconnectKeyboard();
        }

        tabs.clear(); // Useful ???

        // Remove the error pane (for this sheet)
        Main.getGui()
            .hideErrorsPane(sheet.getErrorsEditor().getComponent());
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

    //---------------//
    // selectViewTab //
    //---------------//
    /**
     * Force a tab selection programmatically
     * @param step the step whose related tab must be selected
     */
    public void selectViewTab (Step step)
    {
        final String title = step.getTab();

        for (int i = 0, count = viewsPane.getTabCount(); i < count; i++) {
            if (viewsPane.getTitleAt(i)
                         .equals(title)) {
                viewsPane.setSelectedIndex(i);
                viewsPane.repaint();

                if (logger.isFineEnabled()) {
                    logger.fine("Selected view tab " + title);
                }

                return;
            }
        }

        // Currently, there is no view tab displayed for this step
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * This method is called whenever another view tab is selected in the Sheet
     * Assembly (or when a tab is removed)
     * @param e the originating change event (not used)
     */
    @Implement(ChangeListener.class)
    public void stateChanged (ChangeEvent e)
    {
        ViewTab currentTab = getCurrentViewTab();

        if (logger.isFineEnabled()) {
            logger.fine(
                "SheetAssembly stateChanged previousTab:" + previousTab +
                " currentTab:" + currentTab);
        }

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

    //-------------------//
    // getCurrentViewTab //
    //-------------------//
    /**
     * Report the ViewTab currently selected, if any
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

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        component.setLayout(new BorderLayout());
        component.setNoInsets();
        component.add(slider, BorderLayout.WEST);
        component.add(viewsPane, BorderLayout.CENTER);
    }

    //---------------//
    // displayBoards //
    //---------------//
    /**
     * Make the boards pane visible (for this sheet & view)
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

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // ViewTab //
    //---------//
    /**
     * A simple structure to gather the various aspects of a view tab.
     * All instances are kept in the {@link SheetAssembly#tabs} map.
     */
    private class ViewTab
    {
        //~ Instance fields ----------------------------------------------------

        String     title; // Title used for the tab
        BoardsPane boardsPane; // Related boards pane
        ScrollView scrollView; // Component in the JTabbedPane

        //~ Constructors -------------------------------------------------------

        public ViewTab (String     title,
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

            // Set the model size
            if (sheet.getPicture() != null) {
                rubberPanel.setModelSize(sheet.getDimension());
            }

            // Force scroll bar computations
            zoom.fireStateChanged();
        }

        //~ Methods ------------------------------------------------------------

        //------------//
        // deselected //
        //------------//
        /** Run when this tab gets deselected */
        public void deselected ()
        {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "SheetAssembly: " + sheet.getId() +
                    " viewTab.deselected for " + this);
            }

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
        /** Remove this viewTab instance */
        public void remove ()
        {
            RubberPanel rubberPanel = scrollView.getView();
            rubberPanel.unsetZoom(zoom);
            rubberPanel.unsetRubber(rubber);

            JScrollPane scrollPane = scrollView.getComponent();
            viewsPane.remove(scrollPane);
            tabs.remove(scrollPane);

            if (logger.isFineEnabled()) {
                logger.fine("Removed tab: " + this);
            }
        }

        //----------//
        // selected //
        //----------//
        /** Run when this tab gets selected */
        public void selected ()
        {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "SheetAssembly: " + sheet.getId() +
                    " viewTabSelected for " + this + " dim:" +
                    scrollView.getView().getPreferredSize());
            }

            // Link rubber with proper view
            RubberPanel rubberPanel = scrollView.getView();
            rubber.connectComponent(rubberPanel);
            rubber.setMouseMonitor(rubberPanel);

            // Make connections to events
            rubberPanel.subscribe();

            // Restore display of proper context
            if (logger.isFineEnabled()) {
                logger.fine(this + " showing:" + component.isShowing());
            }

            if (component.isShowing()) {
                displayBoards();
            }

            // Force update of SheetLocationEvent
            SheetLocationEvent locationEvent = (SheetLocationEvent) locationService.getLastEvent(
                SheetLocationEvent.class);
            PixelRectangle     location = (locationEvent != null)
                                          ? locationEvent.getData() : null;

            if (location != null) {
                locationService.publish(
                    new SheetLocationEvent(
                        this,
                        locationEvent.hint,
                        null,
                        location));
            }

            // Keep the same scroll bar positions as with previous tab
            if (previousTab != null) {
                JScrollPane prev = previousTab.scrollView.getComponent();
                final int   vert = prev.getVerticalScrollBar()
                                       .getValue();
                final int   hori = prev.getHorizontalScrollBar()
                                       .getValue();
                JScrollPane scrollPane = scrollView.getComponent();
                scrollPane.getVerticalScrollBar()
                          .setValue(vert);
                scrollPane.getHorizontalScrollBar()
                          .setValue(hori);
            }
        }

        //----------//
        // toString //
        //----------//
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{");
            sb.append(getClass().getSimpleName());
            sb.append(" ")
              .append(title);
            sb.append("}");

            return sb.toString();
        }

        //--------------------//
        // disconnectKeyboard //
        //--------------------//
        private void disconnectKeyboard ()
        {
            if (boardsPane != null) {
                for (Component topComp : boardsPane.getComponent()
                                                   .getComponents()) {
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
                Main.getGui()
                    .setBoardsPane(boardsPane.getComponent());
            } else {
                Main.getGui()
                    .setBoardsPane(null);
            }
        }
    }
}
