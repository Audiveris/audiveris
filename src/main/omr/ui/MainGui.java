//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         M a i n G u i                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.Main;
import omr.WellKnowns;

import omr.action.ActionManager;
import omr.action.Actions;

import omr.constant.Constant;
import omr.constant.ConstantManager;
import omr.constant.ConstantSet;

import omr.image.jai.JaiLoader;

import omr.log.LogPane;

import omr.plugin.PluginManager;

import omr.score.PartwiseBuilder;

import omr.selection.MouseMovement;
import omr.selection.SheetEvent;

import omr.sheet.Book;
import omr.sheet.BookManager;
import omr.sheet.Sheet;
import omr.sheet.ui.SheetActions;
import omr.sheet.ui.SheetsController;

import omr.step.StepMenu;
import omr.step.Stepping;

import omr.ui.dnd.GhostGlassPane;
import omr.ui.symbol.MusicFont;
import omr.ui.util.ModelessOptionPane;
import omr.ui.util.Panel;
import omr.ui.util.SeparableMenu;
import omr.ui.util.UIUtil;

import omr.util.OmrExecutors;
import omr.util.WeakPropertyChangeListener;

import org.bushe.swing.event.EventSubscriber;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;
import java.util.concurrent.Callable;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

/**
 * Class {@code MainGui} is the Java User Interface, the main class for displaying a
 * sheet, the message log and the various tools.
 *
 * @author Hervé Bitteur
 */
public class MainGui
        extends SingleFrameApplication
        implements EventSubscriber<SheetEvent>, PropertyChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(MainGui.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Official name of the application. */
    private String appName;

    /** Sheet tabbed pane, which may contain several views. */
    public SheetsController sheetsController;

    /** The related concrete frame. */
    private JFrame frame;

    /** Bottom pane split between the logPane and the errorsPane. */
    private JSplitPane bottomPane;

    /** Log pane, which displays logging info. */
    private LogPane logPane;

    /** Boards pane, which displays a specific set of boards per sheet. */
    private BoardsScrollPane boardsScrollPane;

    /** GlassPane needed to handle drag and drop from shape palette. */
    private final GhostGlassPane glassPane = new GhostGlassPane();

    /** Main pane with Sheet on top and Log+Errors on bottom. */
    private JSplitPane mainPane;

    /** Application pane with Main pane on left and Boeards on right. */
    private Panel appPane;

    /** Step menu. */
    private StepMenu stepMenu;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code MainGui} instance, to handle any user
     * display and interaction.
     */
    public MainGui ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    //----------//
    // clearLog //
    //----------//
    /**
     * Erase the content of the log window.
     */
    public void clearLog ()
    {
        logPane.clearLog();
    }

    //---------------------//
    // displayConfirmation //
    //---------------------//
    /**
     * Allow to display a modal confirmation dialog with a message.
     *
     * @param message the message asking for confirmation
     * @return true if confirmed, false otherwise
     */
    public boolean displayConfirmation (String message)
    {
        int answer = JOptionPane.showConfirmDialog(
                frame,
                message,
                "Confirm - " + appName,
                JOptionPane.WARNING_MESSAGE);

        return answer == JOptionPane.YES_OPTION;
    }

    //--------------//
    // displayError //
    //--------------//
    /**
     * Allow to display a modal dialog with an error message.
     *
     * @param message the error message
     */
    public void displayError (String message)
    {
        JOptionPane.showMessageDialog(
                frame,
                message,
                "Error - " + appName,
                JOptionPane.ERROR_MESSAGE);
    }

    //----------------//
    // displayMessage //
    //----------------//
    /**
     * Allow to display a modal dialog with an html content.
     *
     * @param htmlStr the HTML string
     */
    public void displayMessage (String htmlStr)
    {
        JEditorPane htmlPane = new JEditorPane("text/html", htmlStr);
        htmlPane.setEditable(false);
        JOptionPane.showMessageDialog(frame, htmlPane, appName, JOptionPane.INFORMATION_MESSAGE);
    }

    //------------------------//
    // displayModelessConfirm //
    //------------------------//
    /**
     * Allow to display a non-modal confirmation dialog.
     *
     * @param message the confirmation message
     * @return the option chosen
     */
    public int displayModelessConfirm (String message)
    {
        return ModelessOptionPane.showModelessConfirmDialog(
                frame,
                message,
                "Confirm - " + appName,
                JOptionPane.YES_NO_OPTION);
    }

    //----------------//
    // displayWarning //
    //----------------//
    /**
     * Allow to display a modal dialog with a message.
     *
     * @param message the warning message
     */
    public void displayWarning (String message)
    {
        JOptionPane.showMessageDialog(
                frame,
                message,
                "Warning - " + appName,
                JOptionPane.WARNING_MESSAGE);
    }

    //----------//
    // getFrame //
    //----------//
    /**
     * Report the concrete frame.
     *
     * @return the ui frame
     */
    public JFrame getFrame ()
    {
        return frame;
    }

    //--------------//
    // getGlassPane //
    //--------------//
    /**
     * Report the main window glassPane, needed for shape drag and drop.
     *
     * @return the ghost glass pane
     */
    public GhostGlassPane getGlassPane ()
    {
        return glassPane;
    }

    //--------------//
    // getIconsRoot //
    //--------------//
    public String getIconsRoot ()
    {
        ResourceMap resource = Application.getInstance().getContext().getResourceMap(getClass());

        return resource.getString("icons.root");
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this application.
     *
     * @return the SingleFrameApplication instance
     */
    public static SingleFrameApplication getInstance ()
    {
        return (SingleFrameApplication) Application.getInstance();
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report an Observer name.
     *
     * @return observer name
     */
    public String getName ()
    {
        return "MainGui";
    }

    //-------------//
    // getStepMenu //
    //-------------//
    /**
     * Report the menu dedicated to steps
     *
     * @return the step menu
     */
    public StepMenu getStepMenu ()
    {
        return stepMenu;
    }

    //------------//
    // hideErrors //
    //------------//
    /**
     * Remove the specific component of the errors pane.
     *
     * @param component the precise component to remove
     */
    public void hideErrors (JComponent component)
    {
        // To avoid race conditions, check we remove the proper component
        if ((component != null) && (component == bottomPane.getRightComponent())) {
            bottomPane.setRightComponent(null);
        }
    }

    //-----------//
    // notifyLog //
    //-----------//
    /**
     * Tell that one or several new log records are waiting for display.
     */
    public void notifyLog ()
    {
        logPane.notifyLog();
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Notification of sheet selection, to update frame title.
     *
     * @param sheetEvent the event about selected sheet
     */
    @Override
    public void onEvent (SheetEvent sheetEvent)
    {
        try {
            // Ignore RELEASING
            if (sheetEvent.movement == MouseMovement.RELEASING) {
                return;
            }

            final Sheet sheet = sheetEvent.getData();
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        @Override
                        public void run ()
                        {
                            final StringBuilder sb = new StringBuilder();

                            if (sheet != null) {
                                Book book = sheet.getBook();
                                // Frame title tells score name
                                sb.append(book.getRadix());
                            }

                            // Update frame title
                            sb.append(" - ");

                            ResourceMap resource = Application.getInstance().getContext().getResourceMap(
                                    getClass());
                            sb.append(resource.getString("mainFrame.title"));
                            frame.setTitle(sb.toString());
                        }
                    });
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //----------------//
    // propertyChange //
    //----------------//
    /**
     * Called when notified from GuiActions.
     *
     * @param evt the event details
     */
    @Override
    public void propertyChange (PropertyChangeEvent evt)
    {
        String propertyName = evt.getPropertyName();
        Boolean display = (Boolean) evt.getNewValue();

        switch (propertyName) {
        case GuiActions.BOARDS_DISPLAYED:

            // Toggle display of boards
            if (display) {
                appPane.add(boardsScrollPane, BorderLayout.EAST);
            } else {
                appPane.remove(boardsScrollPane);
            }

            appPane.revalidate();

            break;

        case GuiActions.LOG_DISPLAYED:

            // Toggle display of log
            if (display) {
                bottomPane.setLeftComponent(logPane.getComponent());
            } else {
                bottomPane.setLeftComponent(null);
            }

            break;

        case GuiActions.ERRORS_DISPLAYED:

            // Toggle display of errors
            if (display) {
                JComponent comp = null;
                Sheet sheet = sheetsController.getSelectedSheet();

                if (sheet != null) {
                    ErrorsEditor editor = sheet.getErrorsEditor();

                    if (editor != null) {
                        comp = editor.getComponent();
                    }
                }

                bottomPane.setRightComponent(comp);
            } else {
                bottomPane.setRightComponent(null);
            }

            break;
        }

        // BottomPane = LogPane | ErrorsPane
        // Totally remove it when it displays no log and no errors
        if (needBottomPane()) {
            mainPane.setBottomComponent(bottomPane);
        } else {
            mainPane.setBottomComponent(null);
        }
    }

    //------------------//
    // removeBoardsPane //
    //------------------//
    /**
     * Remove the current boardsScrollPane, if any.
     */
    public void removeBoardsPane ()
    {
        boardsScrollPane.setBoards(null);
    }

    //---------------//
    // setBoardsPane //
    //---------------//
    /**
     * Set a new boardspane to the boards holder.
     *
     * @param boards the boards pane to be shown
     */
    public void setBoardsPane (JComponent boards)
    {
        boardsScrollPane.setBoards(boards);
    }

    //------------//
    // showErrors //
    //------------//
    /**
     * Show the provided errors.
     *
     * @param errorsPane the errors to be shown
     */
    public void showErrors (JComponent errorsPane)
    {
        bottomPane.setRightComponent(errorsPane);
    }

    //------------//
    // initialize //
    //------------//
    /** {@inheritDoc} */
    @Override
    protected void initialize (String[] args)
    {
        logger.debug("MainGui. 1/initialize");

        // Launch background pre-loading tasks?
        if (constants.preloadCostlyPackages.getValue()) {
            JaiLoader.preload();
            PartwiseBuilder.preload();
        }
    }

    //-------//
    // ready //
    //-------//
    /** {@inheritDoc} */
    @Override
    protected void ready ()
    {
        logger.debug("MainGui. 3/ready");

        // Set exit listener
        addExitListener(BookManager.getInstance().getExitListener());

        // Weakly listen to GUI Actions parameters
        PropertyChangeListener weak = new WeakPropertyChangeListener(this);
        GuiActions.getInstance().addPropertyChangeListener(weak);

        // Make the GUI instance available for the other classes
        Main.setGui(this);

        // Check MusicFont is loaded
        MusicFont.checkMusicFont();

        // Just in case we already have messages pending
        notifyLog();

        // Launch scores
        for (Callable<Void> task : Main.getFilesTasks()) {
            OmrExecutors.getCachedLowExecutor().submit(task);
        }

        // Launch scripts
        for (Callable<Void> task : Main.getScriptsTasks()) {
            OmrExecutors.getCachedLowExecutor().submit(task);
        }
    }

    //---------//
    // startup //
    //---------//
    /** {@inheritDoc} */
    @Override
    protected void startup ()
    {
        logger.debug("MainGui. 2/startup");

        frame = getMainFrame();

        sheetsController = SheetsController.getInstance();
        sheetsController.subscribe(this);

        defineMenus();
        defineLayout();

        // Allow dropping files
        frame.setTransferHandler(new FileDropHandler());

        // Handle ghost drop from shape palette
        frame.setGlassPane(glassPane);

        // Use the defined application name
        appName = getContext().getResourceMap().getString("Application.name");

        // Define an exit listener
        addExitListener(
                new ExitListener()
                {
                    @Override
                    public boolean canExit (EventObject e)
                    {
                        return true;
                    }

                    @Override
                    public void willExit (EventObject e)
                    {
                        // Store latest constant values on disk
                        ConstantManager.getInstance().storeResource();
                    }
                });

        // Here we go...
        show(frame);
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        /*
         * +=============================================================+
         * |toolKeyPanel . . . . . . . . . . . . . . . . . . . . . . . . |
         * |+=================+=============================+===========+|
         * || toolBar . . . . . . . . . .| progressBar . . .| Memory . .||
         * |+=================+=============================+===========+|
         * +=============================================================+
         * | horiSplitPane . . . . . . . . . . . . . . . . . . . . . . . |
         * |+=========================================+=================+|
         * | . . . . . . . . . . . . . . . . . . . . .|boardsScrollPane ||
         * | +========================================+ . . . . . . . . ||
         * | | sheetController . . . . . . . . . . . .| . . . . . . . . ||
         * | | . . . . . . . . . . . . . . . . . . . .| . . . . . . . . ||
         * | | . . . . . . . . . . . . . . . . . . . .| . . . . . . . . ||
         * | | . . . . . . . . . . . . . . . . . . . .| . . . . . . . . ||
         * | | . . . . . . . . . . . . . . . . . . . .| . . . . . . . . ||
         * | | . . . . . . . . . . . . . . . . . . . .| . . . . . . . . ||
         * | | . . . . . . . . . . . . . . . . . . . .| . . . . . . . . ||
         * | | . . . . . . . . . . . . . . . . . . . .| . . . . . . . . ||
         * |m| . . . . . . . . . . . . . . . . . . . .| . . . . . . . . ||
         * |a| . . . . . . . . . . . . . . . . . . . .| . . . . . . . . ||
         * |i| . . . . . . . . . . . . . . . . . . . .| . . . . . . . . ||
         * |n| . . . . . . . . . . . . . . . . . . . .| . . . . . . . . ||
         * |P+=====================+==================+ . . . . . . . . ||
         * |a| logPane . . . . . . | errors . . . . . | . . . . . . . . ||
         * |n| . . . . . . . . . . |. . . . . . . . . | . . . . . . . . ||
         * |e| . . . . . . . . . . |. . . . . . . . . | . . . . . . . . ||
         * | +=====================+==================+=================+|
         * +=============================================================+
         */

        // Individual panes
        logPane = new LogPane();
        boardsScrollPane = new BoardsScrollPane();
        boardsScrollPane.setPreferredSize(new Dimension(350, 500));

        // Bottom = Log & Errors
        bottomPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bottomPane.setBorder(null);
        bottomPane.setDividerSize(1);
        bottomPane.setResizeWeight(0.5d); // Cut in half initially

        // mainPane =  sheetsController / bottomPane
        mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sheetsController.getComponent(), null);
        mainPane.setBorder(null);
        mainPane.setOneTouchExpandable(true);
        mainPane.setResizeWeight(0.9d); // Give bulk space to upper part

        // horiSplitPane = mainPane | boards
        appPane = new Panel();
        appPane.setNoInsets();
        appPane.setBorder(null);
        appPane.setLayout(new BorderLayout());
        appPane.add(mainPane, BorderLayout.CENTER); // + boardsScrollPane later
        appPane.setName("appPane");

        // Global layout: Use a toolbar on top and a double split pane below
        ///toolBar.add(toolKeyPanel);
        Container content = frame.getContentPane();
        content.setLayout(new BorderLayout());
        content.add(ActionManager.getInstance().getToolBar(), BorderLayout.NORTH);
        content.add(appPane, BorderLayout.CENTER);

        // Suppress all internal borders, recursively
        ///UIUtilities.suppressBorders(frame.getContentPane());
        // Display the boards pane?
        if (GuiActions.getInstance().isBoardsDisplayed()) {
            appPane.add(boardsScrollPane, BorderLayout.EAST);
        }

        // Display the log pane?
        if (GuiActions.getInstance().isLogDisplayed()) {
            bottomPane.setLeftComponent(logPane.getComponent());
        }

        // Display the errors pane?
        if (GuiActions.getInstance().isErrorsDisplayed()) {
            bottomPane.setRightComponent(null);
        }

        // BottomPane = Log & Errors
        if (needBottomPane()) {
            mainPane.setBottomComponent(bottomPane);
        }
    }

    //-------------//
    // defineMenus //
    //-------------//
    private void defineMenus ()
    {
        // Specific file menu
        JMenu fileMenu = new SeparableMenu();

        // Specific history sub-menu of sheet menu
        fileMenu.add(SheetActions.HistoryMenu.getInstance().getMenu());

        // Specific step menu
        stepMenu = new StepMenu(new SeparableMenu());

        // Specific plugin menu
        JMenu pluginMenu = PluginManager.getInstance().getMenu(null);

        // For some specific top-level menus
        ActionManager mgr = ActionManager.getInstance();
        mgr.injectMenu(Actions.Domain.FILE.name(), fileMenu);
        mgr.injectMenu(Actions.Domain.STEP.name(), stepMenu.getMenu());
        mgr.injectMenu(Actions.Domain.PLUGIN.name(), pluginMenu);

        // All other commands (which may populate the toolBar as well)
        mgr.loadAllDescriptors();
        mgr.registerAllActions();

        // Menu bar
        JMenuBar innerBar = mgr.getMenuBar();

        // Gauges = progress | memory
        JPanel gauges = new JPanel();
        gauges.setLayout(new BorderLayout());
        gauges.add(Stepping.createMonitor().getComponent(), BorderLayout.CENTER);
        gauges.add(new MemoryMeter().getComponent(), BorderLayout.EAST);

        // Outer bar = menu | gauges
        JMenuBar outerBar = new JMenuBar();
        outerBar.setLayout(new GridLayout(1, 0));
        outerBar.add(innerBar);
        outerBar.add(gauges);

        // Remove useless borders
        UIUtil.suppressBorders(gauges);
        innerBar.setBorder(null);
        outerBar.setBorder(null);

        frame.setJMenuBar(outerBar);

        // Mac Application menu
        if (WellKnowns.MAC_OS_X) {
            MacApplication.setupMacMenus();
        }
    }

    //----------------//
    // needBottomPane //
    //----------------//
    /**
     * Check whether we should keep the bottomPane.
     *
     * @return true if bottomPane is not empty
     */
    private boolean needBottomPane ()
    {
        return GuiActions.getInstance().isLogDisplayed()
               || GuiActions.getInstance().isErrorsDisplayed();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean preloadCostlyPackages = new Constant.Boolean(
                true,
                "Should we preload costly packages in the background?");
    }

    //
    //------------------//
    // BoardsScrollPane //
    //------------------//
    /**
     * Just a scrollPane to host the pane of user boards, trying to offer
     * enough room for the boards.
     */
    private class BoardsScrollPane
            extends JScrollPane
    {
        //~ Methods --------------------------------------------------------------------------------

        public void setBoards (JComponent boards)
        {
            setViewportView(boards);
            revalidate();
        }
    }
}
