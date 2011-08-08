//----------------------------------------------------------------------------//
//                                                                            //
//                               M a i n G u i                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.Main;
import omr.WellKnowns;

import omr.action.ActionManager;
import omr.action.Actions;

import omr.constant.*;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ScoreExporter;
import omr.score.ScoresManager;
import omr.score.midi.MidiAgent;

import omr.selection.MouseMovement;
import omr.selection.SheetEvent;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetActions.OpenTask;
import omr.sheet.ui.SheetsController;

import omr.step.StepMenu;
import omr.step.Stepping;

import omr.ui.dnd.GhostGlassPane;
import omr.ui.symbol.MusicFont;
import omr.ui.util.ModelessOptionPane;
import omr.ui.util.Panel;
import omr.ui.util.SeparableMenu;
import omr.ui.util.UIUtilities;

import omr.util.Implement;
import omr.util.JaiLoader;
import omr.util.OmrExecutors;

import org.bushe.swing.event.EventSubscriber;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.EventObject;
import java.util.concurrent.Callable;

import javax.swing.*;

/**
 * Class <code>MainGui</code> is the Java User Interface, the main class for
 * displaying a score, the related sheet, the message log and the various tools.
 *
 * @author Hervé Bitteur
 */
public class MainGui
    extends SingleFrameApplication
    implements EventSubscriber<SheetEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MainGui.class);

    //~ Instance fields --------------------------------------------------------

    /** Official name of the application */
    private String appName;

    /** Sheet tabbed pane, which may contain several views */
    public SheetsController sheetsController;

    /** The related concrete frame */
    private JFrame frame;

    /** Bottom pane split betwen the logPane and the errorsPane */
    private BottomPane bottomPane;

    /** Log pane, which displays logging info */
    private LogPane logPane;

    /** The tool bar */
    private JToolBar toolBar;

    /** Boards pane, which displays a specific set of boards per sheet */
    private BoardsPane boardsPane;

    /** GlassPane needed to handle drag and drop from shape palette */
    private GhostGlassPane glassPane = new GhostGlassPane();

    /** Needed to rectify their dividers */
    private JSplitPane vertSplitPane;
    private JSplitPane horiSplitPane;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // MainGui //
    //---------//
    /**
     * Creates a new <code>MainGui</code> instance, to handle any user display
     * and interaction.
     */
    public MainGui ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getGlassPane //
    //--------------//
    /**
     * Report the main window glassPane, needed for shape drag 'n drop
     * @return the ghost glass pane
     */
    public GhostGlassPane getGlassPane ()
    {
        return glassPane;
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this application
     *
     * @return the SingleFrameApplication instance
     */
    public static SingleFrameApplication getInstance ()
    {
        return (SingleFrameApplication) Application.getInstance();
    }

    //---------------//
    // setBoardsPane //
    //---------------//
    /**
     * Set a new boardspane to the boards holder
     *
     * @param boards the boards pane to be shown
     */
    public void setBoardsPane (JComponent boards)
    {
        boardsPane.addBoards(boards);
    }

    //----------//
    // getFrame //
    //----------//
    /**
     * Report the concrete frame
     *
     * @return the ui frame
     */
    public JFrame getFrame ()
    {
        return frame;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report an Observer name
     *
     * @return observer name
     */
    public String getName ()
    {
        return "MainGui";
    }

    //--------------//
    // addOnToolBar //
    //--------------//
    public JButton addOnToolBar (Action action)
    {
        JButton button = toolBar.add(action);
        button.setBorder(UIUtilities.getToolBorder());

        return button;
    }

    //----------//
    // clearLog //
    //----------//
    /**
     * Erase the content of the log window
     */
    public void clearLog ()
    {
        logPane.clearLog();
    }

    //---------------------//
    // displayConfirmation //
    //---------------------//
    /**
     * Allow to display a confirmation dialog with a message
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
     * Allow to display a modal dialog with an error message
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
     * Allow to display a modal dialog with an html content
     *
     * @param htmlStr the HTML string
     */
    public void displayMessage (String htmlStr)
    {
        JEditorPane htmlPane = new JEditorPane("text/html", htmlStr);
        htmlPane.setEditable(false);
        JOptionPane.showMessageDialog(
            frame,
            htmlPane,
            appName,
            JOptionPane.INFORMATION_MESSAGE);
    }

    //------------------------//
    // displayModelessConfirm //
    //------------------------//
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
     * Allow to display a modal dialog with a message
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

    //----------------//
    // hideErrorsPane //
    //----------------//
    /**
     * Remove the specific component the errors pane
     * @param component the component to remove (or null if we don't care)
     */
    public void hideErrorsPane (JComponent component)
    {
        bottomPane.removeErrors(component);
    }

    //-----------//
    // notifyLog //
    //-----------//
    /**
     * Tell that one or several new log records are waiting for display
     */
    public void notifyLog ()
    {
        logPane.notifyLog();
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Notification of sheet selection, to update frame title
     *
     * @param sheetEvent the event about selected sheet
     */
    @Implement(EventSubscriber.class)
    public void onEvent (SheetEvent sheetEvent)
    {
        try {
            // Ignore RELEASING
            if (sheetEvent.movement == MouseMovement.RELEASING) {
                return;
            }

            final Sheet sheet = sheetEvent.getData();
            SwingUtilities.invokeLater(
                new Runnable() {
                        public void run ()
                        {
                            final StringBuilder sb = new StringBuilder();

                            if (sheet != null) {
                                Score score = sheet.getScore();
                                // Frame title tells score name (+ step?)
                                sb.append(score.getImageFile().getName());
                            }

                            // Update frame title
                            sb.append(" ")
                              .append(
                                MainGui.this.getContext().getResourceMap().getString(
                                    "mainFrame.title"));
                            frame.setTitle(sb.toString());
                        }
                    });
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
        }
    }

    //------------------//
    // removeBoardsPane //
    //------------------//
    /**
     * Remove the current boardsPane, if any
     */
    public void removeBoardsPane ()
    {
        boardsPane.removeBoards();
    }

    //----------------//
    // showErrorsPane //
    //----------------//
    /**
     * Show the errors pane
     *
     * @param errorsPane the errors pane to be shown
     */
    public void showErrorsPane (JComponent errorsPane)
    {
        bottomPane.addErrors(errorsPane);
    }

    //------------//
    // initialize //
    //------------//
    /** {@inheritDoc} */
    @Override
    protected void initialize (String[] args)
    {
        if (logger.isFineEnabled()) {
            logger.fine("MainGui. initialize");
        }

        // Provide default tool parameters if not already set

        // Tool name
        if (Main.getToolName() == null) {
            Main.setToolName(
                getContext().getResourceMap().getString("Application.id"));
        }

        // Tool version
        if (Main.getToolVersion() == null) {
            Main.setToolVersion(
                getContext().getResourceMap().getString("Application.version"));
        }

        // Tool build
        if (Main.getToolBuild() == null) {
            Main.setToolBuild(
                getContext().getResourceMap().getString("Application.build"));
        }

        // Launch background pre-loading tasks?
        if (constants.preloadCostlyPackages.getValue()) {
            JaiLoader.preload();
            ScoreExporter.preload();
            MidiAgent.preload();
        }
    }

    //-------//
    // ready //
    //-------//
    /** {@inheritDoc} */
    @Override
    protected void ready ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("MainGui. ready");
        }

        // Kludge! Workaround to persist correctly when maximized
        if ((frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            vertSplitPane.setDividerLocation(
                vertSplitPane.getDividerLocation() - 10);
            horiSplitPane.setDividerLocation(
                horiSplitPane.getDividerLocation() - 10);
        }

        // Make the GUI instance available for the other classes
        Main.setGui(this);

        // Check MusicFont is loaded
        MusicFont.checkMusicFont();

        // Just in case we already have messages pending
        notifyLog();

        // Launch scores
        for (Callable<Void> task : Main.getFilesTasks()) {
            OmrExecutors.getCachedLowExecutor()
                        .submit(task);
        }

        // Launch scripts
        for (Callable<Void> task : Main.getScriptsTasks()) {
            OmrExecutors.getCachedLowExecutor()
                        .submit(task);
        }
    }

    //---------//
    // startup //
    //---------//
    /** {@inheritDoc} */
    @Override
    protected void startup ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("MainGui. startup");
        }

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
        appName = getContext()
                      .getResourceMap()
                      .getString("Application.name");

        // Define an exit listener
        addExitListener(
            new ExitListener() {
                    public boolean canExit (EventObject e)
                    {
                        return true;
                    }

                    public void willExit (EventObject e)
                    {
                        // Store latest constant values on disk
                        ConstantManager.getInstance()
                                       .storeResource();
                    }
                });

        show(frame);
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        /*
           +============================================================+
           |toolKeyPanel                                                |
           |+================+=============================+===========+|
           || toolBar        | progressBar                 |   Memory  ||
           |+================+=============================+===========+|
           +============================================================+
           |vertSplitPane                                               |
           |+===========================================+==============+|
           || sheetController                           | boardsPane   ||
           ||                                           |              ||
           ||                                           |              ||
           ||                                           |              ||
           ||                                           |              ||
           ||                                           |              ||
           ||                                           |              ||
           ||                                           |              ||
           ||                                           |              ||
           ||                                           |              ||
           ||                                           |              ||
           ||                                           |              ||
           |+=====================+=====================+              ||
           || logPane             | errorsHolder        |              ||
           ||                     |                     |              ||
           ||                     |                     |              ||
           |+=====================+=====================+==============+|
           +================================================+===========+
         */

        // Individual panes
        logPane = new LogPane();
        boardsPane = new BoardsPane();

        // Bottom = Log & Errors
        bottomPane = new BottomPane(logPane.getComponent());

        // vertSplitPane =  sheetsController / bottomPane
        vertSplitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            sheetsController.getComponent(),
            bottomPane);
        vertSplitPane.setName("vertSplitPane");
        vertSplitPane.setBorder(null);
        vertSplitPane.setDividerSize(2);
        vertSplitPane.setResizeWeight(1d); // Give extra space to upper part

        // toolKeyPanel = progress & memory
        JPanel toolKeyPanel = new JPanel();
        toolKeyPanel.setLayout(new BorderLayout());
        toolKeyPanel.add(
            Stepping.createMonitor().getComponent(),
            BorderLayout.CENTER);
        toolKeyPanel.add(new MemoryMeter().getComponent(), BorderLayout.EAST);

        // horiSplitPane = splitPane | boards
        horiSplitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            vertSplitPane,
            new JScrollPane(boardsPane));
        horiSplitPane.setName("horiSplitPane");
        horiSplitPane.setBorder(null);
        horiSplitPane.setDividerSize(2);
        horiSplitPane.setResizeWeight(1d); // Give extra space to left part

        // Global layout: Use a toolbar on top and a double split pane below
        toolBar.add(toolKeyPanel);
        frame.getContentPane()
             .setLayout(new BorderLayout());
        frame.getContentPane()
             .add(toolBar, BorderLayout.NORTH);
        frame.getContentPane()
             .add(horiSplitPane, BorderLayout.CENTER);
    }

    //-------------//
    // defineMenus //
    //-------------//
    private void defineMenus ()
    {
        // Specific sheet menu
        JMenu     sheetMenu = new SeparableMenu();

        // Specific history sub-menu
        JMenuItem historyMenu = ScoresManager.getInstance()
                                             .getHistory()
                                             .menu(
            "Sheet History",
            new HistoryListener());
        sheetMenu.add(historyMenu);

        // Specific step menu
        JMenu       stepMenu = new StepMenu(new SeparableMenu()).getMenu();

        // For history sub-menu
        ResourceMap resource = MainGui.getInstance()
                                      .getContext()
                                      .getResourceMap(Actions.class);
        historyMenu.setName("historyMenu");
        resource.injectComponents(historyMenu);

        // For some specific top-level menus
        ActionManager mgr = ActionManager.getInstance();
        mgr.injectMenu(Actions.Domain.FILE.name(), sheetMenu);
        mgr.injectMenu(Actions.Domain.STEP.name(), stepMenu);

        // All other commands
        mgr.loadAllDescriptors();
        mgr.registerAllActions();
        toolBar = mgr.getToolBar();
        frame.setJMenuBar(mgr.getMenuBar());

        // Mac Application menu
        if (WellKnowns.MAC_OS_X) {
            MacApplication.setupMacMenus();
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // BoardsPane //
    //------------//
    private static class BoardsPane
        extends Panel
    {
        //~ Constructors -------------------------------------------------------

        public BoardsPane ()
        {
            setNoInsets();
        }

        //~ Methods ------------------------------------------------------------

        public void addBoards (JComponent boards)
        {
            removeBoards();

            if (boards != null) {
                add(boards);
            }

            revalidate();
            repaint();
        }

        public void removeBoards ()
        {
            for (Component component : getComponents()) {
                remove(component);
            }

            repaint();
        }
    }

    //------------//
    // BottomPane //
    //------------//
    /**
     * A split pane which handles the bottom pane which contains the log pane
     * and potentially an errors pane on the right. We try to remember the last
     * divider location
     */
    private static class BottomPane
        extends JSplitPane
    {
        //~ Instance fields ----------------------------------------------------

        /** Saves the current location of the divider */
        private int dividerLocation = -1;

        //~ Constructors -------------------------------------------------------

        public BottomPane (JComponent left)
        {
            super(JSplitPane.HORIZONTAL_SPLIT, left, null);
            setBorder(null);
            setDividerSize(2);
        }

        //~ Methods ------------------------------------------------------------

        public void addErrors (JComponent errorsPane)
        {
            removeErrors(null);
            setRightComponent(errorsPane);

            if (dividerLocation == -1) {
                dividerLocation = getWidth() / 2;
            }

            setDividerLocation(dividerLocation);
            revalidate();
            repaint();
        }

        public void removeErrors (JComponent component)
        {
            // Check that we remove the desired component
            if ((component == null) || (component == getRightComponent())) {
                if (dividerLocation != -1) {
                    dividerLocation = getDividerLocation();
                }

                setRightComponent(null);
                dividerLocation = -1;
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Flag for the preloading of costly packages in the background */
        private final Constant.Boolean preloadCostlyPackages = new Constant.Boolean(
            true,
            "Should we preload costly packages in the background?");
    }

    //-----------------//
    // HistoryListener //
    //-----------------//
    /**
     * Class <code>HistoryListener</code> is used to reload an image file, when
     * selected from the history of previous image files.
     */
    private static class HistoryListener
        implements ActionListener
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            File file = new File(e.getActionCommand());
            new OpenTask(file).execute();
        }
    }
}
