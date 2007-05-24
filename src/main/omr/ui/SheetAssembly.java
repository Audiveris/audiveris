//----------------------------------------------------------------------------//
//                                                                            //
//                         S h e e t A s s e m b l y                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.ui;

import omr.Main;

import omr.constant.ConstantSet;

import omr.score.ScoreView;

import omr.selection.Selection;
import omr.selection.SelectionTag;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.ui.util.Panel;
import omr.ui.view.LogSlider;
import omr.ui.view.Rubber;
import omr.ui.view.RubberZoomedPanel;
import omr.ui.view.ScrollView;
import omr.ui.view.Zoom;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>SheetAssembly</code> is a UI assembly dedicated to the display of
 * one sheet, gathering : <ul>
 *
 * <li>a single {@link omr.score.ScoreView}</li>
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
    implements ChangeListener // -> stateChanged() on tab selection

{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetAssembly.class);

    //~ Instance fields --------------------------------------------------------

    /** My parallel list of view Tabs */
    private final ArrayList<ViewTab> viewTabs = new ArrayList<ViewTab>();

    /** Split pane for score and sheet views */
    private final JSplitPane splitPane = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT);

    /** Tabbed container for all views of the sheet */
    private final JTabbedPane tabbedPane = new JTabbedPane();

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

    /** Selection of pixel location */
    private Selection locationSelection;

    /** Index of previously selected tab */
    private int previousViewIndex = -1;

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

        locationSelection = sheet.getSelection(SelectionTag.SHEET_RECTANGLE);

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

        if (logger.isFineEnabled()) {
            logger.fine("SheetAssembly created.");
        }
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // addViewTab //
    //------------//
    /**
     * Add a new tab, that contains a new view on the sheet
     * @param title label to be used for the tab
     * @param sv the view on the sheet
     * @param boardsPane the board pane associated to the tab
     */
    public void addViewTab (String     title,
                            ScrollView sv,
                            BoardsPane boardsPane)
    {
        boardsPane.setName(sheet.getRadix() + ":" + title);

        if (logger.isFineEnabled()) {
            logger.fine(
                "addViewTab title=" + title + " boardsPane=" + boardsPane);
        }

        // Make the new view reuse the common zoom and rubber instances
        sv.getView()
          .setZoom(zoom);
        sv.getView()
          .setRubber(rubber);

        // Set the model size
        if (sheet.getWidth() != -1) {
            sv.getView()
              .setModelSize(new Dimension(sheet.getWidth(), sheet.getHeight()));
        } else {
            sv.getView()
              .setModelSize(sheet.getPicture().getDimension());
        }

        // Force scroll bar computations
        zoom.fireStateChanged();
        viewTabs.add(new omr.ui.SheetAssembly.ViewTab(title, boardsPane, sv));

        // Actually insert a Swing tab
        tabbedPane.addTab(title, sv.getComponent());
        tabbedPane.setSelectedComponent(sv.getComponent());
    }

    //--------------------//
    // assemblyDeselected //
    //--------------------//
    /**
     * Method called when this sheet assembly is no longer selected.
     */
    public void assemblyDeselected ()
    {
        int viewIndex = tabbedPane.getSelectedIndex();

        // Disconnect the current board
        if (logger.isFineEnabled()) {
            logger.fine("assemblyDeselected viewIndex=" + viewIndex);
        }

        if (viewIndex != -1) {
            viewTabs.get(viewIndex).boardsPane.hidden();
        }
    }

    //------------------//
    // assemblySelected //
    //------------------//
    /**
     * Method called when this sheet assembly is selected (since we can have
     * several sheets displayed, each one with its own sheet assembly). This is
     * called from {@link omr.sheet.SheetController} when the tab of another
     * sheet is selected.
     */
    public void assemblySelected ()
    {
        ///logger.info("assemblySelected");

        // Display current context
        displayContext();
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
        gui.removeBoardsPane(); // Disconnect boards pane
        gui.removeErrorsPane(); // Disconnect errors pane
        gui.sheetController.close(this);

        // Disconnect all keyboard bindings from PixelBoard's (as a workaround
        // for a Swing memory leak)
        for (ViewTab tab : viewTabs) {
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

        viewTabs.clear(); // Useful ???
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
            logger.fine("Closing scoreView for " + scoreView.getScore());
            splitPane.setTopComponent(null);
            scoreView = null;
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
    public JComponent getComponent ()
    {
        return component;
    }

    //---------------//
    // getErrorsPane //
    //---------------//
    /**
     * Report the UI pane dedicated to the current errors
     * @return the errors pane
     */
    public JComponent getErrorsPane ()
    {
        return sheet.getErrorsEditor().getComponent();
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
        int viewIndex = tabbedPane.getSelectedIndex();

        if (viewIndex != -1) {
            return viewTabs.get(viewIndex).scrollView;
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

        // Needed to make the scroll bars visible
        scoreView.getScrollPane()
                 .getView()
                 .getZoom()
                 .fireStateChanged();

        component.invalidate();
        component.validate();
        component.repaint();

        this.scoreView = scoreView;

        // Pre-position vertical scroll bar to its middle (50 for 0 - 100)
        SwingUtilities.invokeLater(
            new Runnable() {
                    public void run ()
                    {
                        scoreView.getScrollPane()
                                 .getComponent()
                                 .getVerticalScrollBar()
                                 .setValue(50);
                    }
                });
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

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * This method is called whenever a view tab is selected in the Sheet
     * Assembly.
     *
     * @param e the originating change event (not used actually)
     */
    @Implement(ChangeListener.class)
    public void stateChanged (ChangeEvent e)
    {
        if (previousViewIndex != -1) {
            tabDeselected(previousViewIndex);
        }

        final int viewIndex = tabbedPane.getSelectedIndex();

        if (viewIndex != -1) {
            tabSelected(viewIndex);
        }

        previousViewIndex = viewIndex;
    }

    //------------------------//
    // storeScoreSheetDivider //
    //------------------------//
    static void storeScoreSheetDivider ()
    {
        Sheet sheet = SheetManager.getSelectedSheet();

        if (sheet != null) {
            constants.scoreSheetDivider.setValue(
                sheet.getAssembly().splitPane.getDividerLocation());
        }
    }

    //----------------//
    // displayContext //
    //----------------//
    private void displayContext ()
    {
        ///logger.info("displayContext");

        // Make sure the tab is ready
        int viewIndex = tabbedPane.getSelectedIndex();

        if (viewIndex == -1) {
            return;
        }

        // Display the proper boards pane
        BoardsPane boardsPane = viewTabs.get(viewIndex).boardsPane;

        if (logger.isFineEnabled()) {
            logger.fine("displaying " + boardsPane);
        }

        boardsPane.shown();

        Main.getGui()
            .addBoardsPane(boardsPane.getComponent());
    }

    //---------------//
    // tabDeselected //
    //---------------//
    private void tabDeselected (int previousIndex)
    {
        ///logger.info("tabDeselected for " + tabs.get(previousIndex).title);
        final ViewTab tab = viewTabs.get(previousIndex);

        // Disconnection of related view
        locationSelection.deleteObserver(tab.scrollView.getView());

        // Disconnection of related boards
        tab.boardsPane.hidden();
    }

    //-------------//
    // tabSelected //
    //-------------//
    private void tabSelected (int viewIndex)
    {
        ///logger.info("tabSelected for " + tabs.get(viewIndex).title);
        final ViewTab     tab = viewTabs.get(viewIndex);

        ScrollView        scrollView = getSelectedView();
        RubberZoomedPanel view = scrollView.getView();

        // Link rubber with proper view
        rubber.setComponent(view);
        rubber.setMouseMonitor(view);

        // Keep previous scroll bar positions
        if (previousViewIndex != -1) {
            JScrollPane prev = viewTabs.get(previousViewIndex).scrollView.getComponent();
            scrollView.getComponent()
                      .getVerticalScrollBar()
                      .setValue(prev.getVerticalScrollBar().getValue());
            scrollView.getComponent()
                      .getHorizontalScrollBar()
                      .setValue(prev.getHorizontalScrollBar().getValue());
        }

        // Handle connection to location selection
        locationSelection.addObserver(scrollView.getView());

        // Restore display of proper context
        displayContext();

        // Force update
        locationSelection.reNotifyObservers(null);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
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
     * All instances are kept in an ordered list parallel to JTabbedPane index.
     */
    private static class ViewTab
    {
        BoardsPane boardsPane; // Related boards pane
        ScrollView scrollView; // Component in the JTabbedPane
        String     title; // Title used for the tab

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
