//----------------------------------------------------------------------------//
//                                                                            //
//                               M a i n G u i                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.Main;

import omr.action.ActionManager;
import omr.action.Actions;

import omr.constant.*;

import omr.log.Logger;

import omr.score.ScoreExporter;
import omr.score.midi.MidiAgent;
import omr.score.ui.ScoreController;

import omr.selection.MouseMovement;
import omr.selection.SheetEvent;

import omr.sheet.Sheet;
import omr.sheet.SheetsManager;
import omr.sheet.ui.SheetActions.OpenTask;
import omr.sheet.ui.SheetsController;

import omr.step.Step;
import omr.step.StepMenu;

import omr.ui.icon.IconManager;
import omr.ui.util.MemoryMeter;
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
 * @author Herv&eacute; Bitteur
 * @version $Id$
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

    /** Cache the application */
    private Application app;

    /** User actions for scores */
    public ScoreController scoreController;

    /** Sheet tabbed pane, which may contain several views */
    public SheetsController sheetSetController;

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
            app.getContext().getResourceMap().getString("Application.name"),
            JOptionPane.INFORMATION_MESSAGE);
    }

    //----------------//
    // displayWarning //
    //----------------//
    /**
     * Allow to display a modal dialog with an html content
     *
     * @param htmlStr the HTML string
     */
    public void displayWarning (String htmlStr)
    {
        JEditorPane htmlPane = new JEditorPane("text/html", htmlStr);
        htmlPane.setEditable(false);

        JOptionPane.showMessageDialog(
            frame,
            htmlPane,
            "Warning - " +
            app.getContext().getResourceMap().getString("Application.name"),
            JOptionPane.WARNING_MESSAGE);
    }

    //----------------//
    // hideErrorsPane //
    //----------------//
    /**
     * Hide the errors pane
     */
    public void hideErrorsPane ()
    {
        bottomPane.removeErrors();
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
                                // Frame title tells sheet name + step
                                sb.append(sheet.getRadix());

                                Step lastStep = sheet.getSheetSteps()
                                                     .getLatestStep();

                                if (lastStep != null) {
                                    sb.append(" - ")
                                      .append(lastStep)
                                      .append(" -");
                                }
                            }

                            // Update frame title
                            sb.append(" ")
                              .append(
                                app.getContext().getResourceMap().getString(
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

        Main.setGui(this);

        // Just in case we already have messages pending
        notifyLog();

        for (Callable<Void> task : Main.getSheetsTasks()) {
            OmrExecutors.getCachedLowExecutor()
                        .submit(task);
        }

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

        sheetSetController = SheetsController.getInstance();
        SheetsManager.getInstance()
                     .setController(sheetSetController);
        sheetSetController.subscribe(this);

        scoreController = new ScoreController();

        defineMenus();
        defineLayout();

        // Stay informed on sheet selection

        // Allow dropping files
        frame.setTransferHandler(new FileDropHandler());

        // Define an exit listener
        app = Application.getInstance();
        app.addExitListener(new MaybeExit());

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
           |bigSplitPane                                                |
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

        // Use a layout with toolbar on top and a double split pane below
        frame.getContentPane()
             .setLayout(new BorderLayout());

        logPane = new LogPane();

        // Boards
        boardsPane = new BoardsPane();
        boardsPane.setName("boardsPane");

        // Bottom = Log & Errors
        bottomPane = new BottomPane(logPane.getComponent());
        bottomPane.setName("bottomPane");

        /** The splitted panes */
        final JSplitPane splitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            sheetSetController.getComponent(),
            bottomPane);
        splitPane.setName("splitPane");
        splitPane.setBorder(null);
        splitPane.setDividerSize(2);
        splitPane.setResizeWeight(1d); // Give extra space to left part

        JPanel toolKeyPanel = new JPanel();
        toolKeyPanel.setLayout(new BorderLayout());
        //        toolKeyPanel.add(toolBar, BorderLayout.WEST);
        toolKeyPanel.add(
            Step.createMonitor().getComponent(),
            BorderLayout.CENTER);
        toolKeyPanel.add(
            new MemoryMeter(
                IconManager.getInstance().loadImageIcon("general/Delete")).getComponent(),
            BorderLayout.EAST);
        toolBar.add(toolKeyPanel);
        frame.getContentPane()
             .add(toolBar, BorderLayout.NORTH);

        final JSplitPane bigSplitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            splitPane,
            boardsPane);
        bigSplitPane.setName("bigSplitPane");
        bigSplitPane.setBorder(null);
        bigSplitPane.setDividerSize(2);
        bigSplitPane.setResizeWeight(1d); // Give extra space to left part

        // Global layout
        frame.getContentPane()
             .add(bigSplitPane, BorderLayout.CENTER);
    }

    //-------------//
    // defineMenus //
    //-------------//
    private void defineMenus ()
    {
        // Specific sheet menu
        JMenu     sheetMenu = new SeparableMenu();

        // Specific history sub-menu
        JMenuItem historyMenu = SheetsManager.getInstance()
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
        mgr.injectMenu(Actions.Domain.SHEET.name(), sheetMenu);
        mgr.injectMenu(Actions.Domain.STEP.name(), stepMenu);

        // All other commands
        mgr.loadAllDescriptors();
        mgr.registerAllActions();
        toolBar = mgr.getToolBar();
        frame.setJMenuBar(mgr.getMenuBar());

        // Mac Application menu
        if (omr.Main.MAC_OS_X) {
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
            add(boards);
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
            removeErrors();
            setRightComponent(errorsPane);

            if (dividerLocation == -1) {
                dividerLocation = getWidth() / 2;
            }

            setDividerLocation(dividerLocation);
            revalidate();
            repaint();
        }

        public void removeErrors ()
        {
            if (dividerLocation != -1) {
                dividerLocation = getDividerLocation();
            }

            setRightComponent(null);
            dividerLocation = -1;
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
     * Class <code>HistoryListener</code> is used to reload a sheet file, when
     * selected from the history of previous sheets.
     */
    private static class HistoryListener
        implements ActionListener
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            File file = new File(e.getActionCommand());

            //            if (file.exists()) {
            new OpenTask(file).execute();

            //            } else {
            //                logger.warning("File not found " + file);
            //            }
        }
    }

    //-----------//
    // MaybeExit //
    //-----------//
    private static class MaybeExit
        implements Application.ExitListener
    {
        //~ Methods ------------------------------------------------------------

        public boolean canExit (EventObject e)
        {
            // Make sure all scripts are stored (or explicitly ignored)
            MainGui gui = Main.getGui();

            if (gui != null) {
                return SheetsController.getInstance()
                                       .areAllScriptsStored();
            } else {
                return true;
            }
        }

        public void willExit (EventObject e)
        {
            // Store latest constant values on disk
            ConstantManager.getInstance()
                           .storeResource();
        }
    }
}
