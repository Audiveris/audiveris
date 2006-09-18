//----------------------------------------------------------------------------//
//                                                                            //
//                         S h e e t A s s e m b l y                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.ui;

import omr.Main;

import omr.score.ScoreView;

import omr.selection.Selection;
import omr.selection.SelectionTag;

import omr.sheet.Sheet;

import omr.ui.util.Panel;
import omr.ui.view.LogSlider;
import omr.ui.view.Rubber;
import omr.ui.view.RubberZoomedPanel;
import omr.ui.view.ScrollView;
import omr.ui.view.Zoom;

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
 * refers to a collection of {@link BoardsPane} which is parallel to the
 * collection of ScrollView's.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SheetAssembly
    implements ChangeListener // -> stateChanged() on tab selection

{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger  logger = Logger.getLogger(SheetAssembly.class);

    //~ Instance fields --------------------------------------------------------

    // My parallel list of Tab
    private final ArrayList<Tab> tabs = new ArrayList<Tab>();

    // Split pane for score and sheet views
    private final JSplitPane  splitPane = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT);

    // Tabbed container for all views of the sheet
    private final JTabbedPane tabbedPane = new JTabbedPane();

    // To manually control the zoom ratio
    private final LogSlider slider = new LogSlider(
        2,
        5,
        LogSlider.VERTICAL,
        -3,
        4,
        0);

    // The concrete UI component
    private Panel        component;

    // Link with sheet
    private final Sheet  sheet;

    // Zoom , with default ratio set to 1
    private final Zoom   zoom = new Zoom(slider, 1);

    // Mouse adapter
    private final Rubber rubber = new Rubber(zoom);

    // Related Score view
    private ScoreView scoreView;

    // Selection of pixel location
    private Selection locationSelection;
    private int       previousIndex = -1;

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

        locationSelection = sheet.getSelection(SelectionTag.PIXEL);

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
        component.add(splitPane);

        splitPane.setBorder(null);
        splitPane.setDividerSize(2);

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
        splitPane.setDividerLocation(220); // TO BE IMPROVED !!!

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
        int index = tabbedPane.getSelectedIndex();

        if (index != -1) {
            return tabs.get(index).scrollView;
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

        // Own tab structure
        tabs.add(new Tab(title, boardsPane, sv));

        // Actually insert a Swing tab
        tabbedPane.addTab(title, sv.getComponent());
        tabbedPane.setSelectedComponent(sv.getComponent());

        // Add the boardsPane to Jui
        Main.getJui()
            .addBoardsPane(boardsPane);
    }

    //--------------------//
    // assemblyDeselected //
    //--------------------//
    /**
     * Method called when this sheet assembly is no longer selected.
     */
    public void assemblyDeselected ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("assemblyDeselected");
        }

        // Disconnect the current tab
        int index = tabbedPane.getSelectedIndex();

        if (index != -1) {
            tabs.get(index).boardsPane.hidden();
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
        Jui jui = Main.getJui();

        jui.sheetPane.close(this);

        // Disconnect all keyboard bindings from PixelBoard's (as a workaround
        // for a Swing memory leak)
        for (Tab tab : tabs) {
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

        // Disconnect all boards panes for this assembly
        for (Tab tab : tabs) {
            jui.removeBoardsPane(tab.boardsPane);
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
            logger.fine("Closing scoreView for " + scoreView.getScore());
            splitPane.setTopComponent(null);
            scoreView = null;
        }
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
    public void stateChanged (ChangeEvent e)
    {
        if (previousIndex != -1) {
            tabDeselected(previousIndex);
        }

        final int index = tabbedPane.getSelectedIndex();

        if (index != -1) {
            tabSelected(index);
        }

        previousIndex = index;
    }

    //----------------//
    // displayContext //
    //----------------//
    private void displayContext ()
    {
        ///logger.info("displayContext");

        // Make sure the tab is ready
        int index = tabbedPane.getSelectedIndex();

        if (index == -1) {
            return;
        }

        // Display the proper boards pane
        BoardsPane boardsPane = tabs.get(index).boardsPane;

        if (logger.isFineEnabled()) {
            logger.fine("displaying " + boardsPane);
        }

        boardsPane.shown();

        Main.getJui()
            .showBoardsPane(boardsPane);
    }

    //---------------//
    // tabDeselected //
    //---------------//
    private void tabDeselected (int previousIndex)
    {
        ///logger.info("tabDeselected for " + tabs.get(previousIndex).title);
        final Tab tab = tabs.get(previousIndex);

        // Disconnection of related view
        locationSelection.deleteObserver(tab.scrollView.getView());

        // Disconnection of related boards
        tab.boardsPane.hidden();
    }

    //-------------//
    // tabSelected //
    //-------------//
    private void tabSelected (int index)
    {
        ///logger.info("tabSelected for " + tabs.get(index).title);
        final Tab         tab = tabs.get(index);

        ScrollView        scrollView = getSelectedView();
        RubberZoomedPanel view = scrollView.getView();

        // Link rubber with proper view
        rubber.setComponent(view);
        rubber.setMouseMonitor(view);

        // Keep previous scroll bar positions
        if (previousIndex != -1) {
            JScrollPane prev = tabs.get(previousIndex).scrollView.getComponent();
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

    //-----//
    // Tab //
    //-----//
    /**
     * A simple structure to gather the various aspects of a tab.  All instances
     * are kept in an ordered list parallel to JTabbedPane index.
     */
    private static class Tab
    {
        BoardsPane boardsPane; // Related boards pane
        ScrollView scrollView; // Component in the JTabbedPane
        String     title; // Title used for the tab

        public Tab (String     title,
                    BoardsPane boardsPane,
                    ScrollView scrollView)
        {
            this.title = title;
            this.boardsPane = boardsPane;
            this.scrollView = scrollView;
        }
    }
}
