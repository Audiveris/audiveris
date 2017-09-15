//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         M a i n G u i                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui;

import org.audiveris.omr.Main;
import org.audiveris.omr.OMR;
import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.classifier.ShapeClassifier;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantManager;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.image.jai.JaiLoader;
import org.audiveris.omr.log.LogPane;
import org.audiveris.omr.plugin.PluginManager;
import org.audiveris.omr.score.PartwiseBuilder;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.BookActions;
import org.audiveris.omr.sheet.ui.SheetResultPainter;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.step.ui.StepMenu;
import org.audiveris.omr.step.ui.StepMonitoring;
import org.audiveris.omr.ui.action.ActionManager;
import org.audiveris.omr.ui.action.Actions;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.StubEvent;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.util.ModelessOptionPane;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.SeparableMenu;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.OmrExecutors;
import org.audiveris.omr.util.WeakPropertyChangeListener;

import org.bushe.swing.event.EventSubscriber;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.concurrent.Callable;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

/**
 * Class {@code MainGui} is the Java User Interface, the main class for displaying a
 * sheet, the message log and the various tools.
 * <p>
 * This user interface is structured according to BSAF lifecycle:
 * <p>
 * <img src="https://kenai.com/attachments/wiki_images/bsaf/ApplicationLifecycle.png">
 *
 * @author Hervé Bitteur
 */
public class MainGui
        extends OmrGui
        implements EventSubscriber<StubEvent>, PropertyChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(MainGui.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Official name of the application. */
    private String appName;

    /** Sheet tabbed pane, which may contain several views. */
    public StubsController stubsController;

    /** The related concrete frame. */
    private JFrame frame;

    /** Bottom pane split between the logPane and the errorsPane. */
    private JSplitPane bottomPane;

    /** Log pane, which displays logging info. */
    private LogPane logPane;

    /** Boards pane, which displays a specific set of boards per sheet. */
    private BoardsScrollPane boardsScrollPane;

    /** GlassPane needed to handle drag and drop from shape palette. */
    private final OmrGlassPane glassPane = new OmrGlassPane();

    /** Main pane with Sheet on top and Log+Errors on bottom. */
    private JSplitPane mainPane;

    /** Application pane with Main pane on left and Boeards on right. */
    private Panel appPane;

    /** Step menu. */
    private StepMenu stepMenu;

    /** Map of class resources. */
    private ResourceMap resources;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code MainGui} instance, to handle any user display and interaction.
     */
    public MainGui ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    //----------//
    // clearLog //
    //----------//
    @Override
    public void clearLog ()
    {
        logPane.clearLog();
    }

    //---------------------//
    // displayConfirmation //
    //---------------------//
    @Override
    public boolean displayConfirmation (String message)
    {
        int answer = JOptionPane.showConfirmDialog(
                frame,
                message,
                "Confirm - " + appName,
                JOptionPane.YES_NO_OPTION);

        return answer == JOptionPane.YES_OPTION;
    }

    //--------------//
    // displayError //
    //--------------//
    @Override
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
    @Override
    public void displayMessage (String htmlStr)
    {
        JEditorPane htmlPane = new JEditorPane("text/html", htmlStr);
        htmlPane.setEditable(false);
        JOptionPane.showMessageDialog(frame, htmlPane, appName, JOptionPane.INFORMATION_MESSAGE);
    }

    //------------------------//
    // displayModelessConfirm //
    //------------------------//
    @Override
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
    @Override
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
    @Override
    public JFrame getFrame ()
    {
        return frame;
    }

    //--------------//
    // getGlassPane //
    //--------------//
    @Override
    public OmrGlassPane getGlassPane ()
    {
        return glassPane;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report an Observer name, as an EventSubscriber.
     *
     * @return observer name
     */
    public String getName ()
    {
        return "MainGui";
    }

    //-----------//
    // notifyLog //
    //-----------//
    @Override
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
     * @param stubEvent the event about selected sheet
     */
    @Override
    public void onEvent (StubEvent stubEvent)
    {
        try {
            // Ignore RELEASING
            if (stubEvent.movement == MouseMovement.RELEASING) {
                return;
            }

            final SheetStub stub = stubEvent.getData();
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    final StringBuilder sb = new StringBuilder();

                    if (stub != null) {
                        Book book = stub.getBook();
                        // Frame title tells score name
                        sb.append(book.getRadix());
                    }

                    // Update frame title
                    sb.append(" - ");

                    sb.append(resources.getString("mainFrame.title"));
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
        case GuiActions.BOARDS_WINDOW_DISPLAYED:

            // Toggle display of boards
            if (display) {
                appPane.add(boardsScrollPane, BorderLayout.EAST);
            } else {
                appPane.remove(boardsScrollPane);
            }

            appPane.revalidate();

            break;

        case GuiActions.LOG_WINDOW_DISPLAYED:

            // Toggle display of log
            if (display) {
                bottomPane.setLeftComponent(logPane.getComponent());
            } else {
                bottomPane.setLeftComponent(null);
            }

            break;

        case GuiActions.ERRORS_WINDOW_DISPLAYED:

            // Toggle display of errors
            if (display) {
                JComponent comp = null;
                SheetStub stub = stubsController.getSelectedStub();

                if (stub != null) {
                    ErrorsEditor editor = stub.getSheet().getErrorsEditor();

                    if (editor != null) {
                        comp = editor.getComponent();
                    }
                }

                bottomPane.setRightComponent(comp);
            } else {
                bottomPane.setRightComponent(null);
            }

            break;

        default:
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
    @Override
    public void removeBoardsPane ()
    {
        boardsScrollPane.setBoards(null);
    }

    //------------------//
    // removeErrorsPane //
    //------------------//
    @Override
    public void removeErrorsPane (JComponent errorsPane)
    {
        // To avoid race conditions, check we remove the proper errors pane
        if ((errorsPane != null) && (errorsPane == bottomPane.getRightComponent())) {
            bottomPane.setRightComponent(null);
        }
    }

    //---------------//
    // setBoardsPane //
    //---------------//
    @Override
    public void setBoardsPane (JComponent boards)
    {
        boardsScrollPane.setBoards(boards);
    }

    //---------------//
    // setErrorsPane //
    //---------------//
    @Override
    public void setErrorsPane (JComponent errorsPane)
    {
        bottomPane.setRightComponent(errorsPane);
    }

    //------------//
    // initialize //
    //------------//
    @Override
    protected void initialize (String[] args)
    {
        logger.debug("MainGui. 1/initialize");

        // Launch background pre-loading tasks?
        if (constants.preloadCostlyPackages.isSet()) {
            ShapeClassifier.preload();
            JaiLoader.preload();
            PartwiseBuilder.preload();
        }
    }

    //-------//
    // ready //
    //-------//
    @Override
    protected void ready ()
    {
        logger.debug("MainGui. 3/ready");

        // Get resources, now that application is available
        resources = Application.getInstance().getContext().getResourceMap(getClass());

        // Set application exit listener
        addExitListener(new GuiExitListener());

        // Weakly listen to OmrGui Actions parameters
        PropertyChangeListener weak = new WeakPropertyChangeListener(this);
        GuiActions.getInstance().addPropertyChangeListener(weak);

        // Check MusicFont is loaded
        MusicFont.checkMusicFont();

        // Just in case we already have messages pending
        notifyLog();

        // Launch inputs, books & scripts
        for (Callable<Void> task : Main.getCli().getCliTasks()) {
            logger.info("MainGui submitting {}", task);
            OmrExecutors.getCachedLowExecutor().submit(task);
        }
    }

    //---------//
    // startup //
    //---------//
    @Override
    protected void startup ()
    {
        logger.debug("MainGui. 2/startup");

        // Make the OmrGui instance available for the other classes
        OMR.gui = this;

        frame = getMainFrame();

        stubsController = StubsController.getInstance();
        stubsController.subscribe(this);

        defineMenus();
        defineLayout();

        // Allow dropping of files
        frame.setTransferHandler(new FileDropHandler());

        // Handle ghost drop from shape palette
        frame.setGlassPane(glassPane);

        // Use the defined application name
        appName = getContext().getResourceMap().getString("Application.name");

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
         * | | stubsController . . . . . . . . . . . .| . . . . . . . . ||
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

        // mainPane =  stubsController / bottomPane
        mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, stubsController.getComponent(), null);
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
        if (GuiActions.getInstance().isBoardsWindowDisplayed()) {
            appPane.add(boardsScrollPane, BorderLayout.EAST);
        }

        // Display the log pane?
        if (GuiActions.getInstance().isLogWindowDisplayed()) {
            bottomPane.setLeftComponent(logPane.getComponent());
        }

        // Display the errors pane?
        if (GuiActions.getInstance().isErrorsWindowDisplayed()) {
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

        // Specific history sub-menus in fileMenu
        for (int i = 0, itemCount = fileMenu.getItemCount(); i < itemCount; i++) {
            final JMenuItem item = fileMenu.getItem(i);

            if (item != null) {
                final String itemName = item.getName();

                if (itemName != null) {
                    BookActions ba = BookActions.getInstance();

                    if (itemName.equals("inputHistory")) {
                        ba.getImageHistoryMenu().populate((JMenu) item, BookActions.class);
                    }
                }
            }
        }

        // Specific history sub-menus in bookMenu
        JMenu bookMenu = mgr.getMenu(Actions.Domain.BOOK.name());
        mgr.injectMenu(Actions.Domain.BOOK.name(), bookMenu);

        for (int i = 0, itemCount = bookMenu.getItemCount(); i < itemCount; i++) {
            final JMenuItem item = bookMenu.getItem(i);

            if (item != null) {
                final String itemName = item.getName();

                if (itemName != null) {
                    BookActions ba = BookActions.getInstance();

                    if (itemName.equals("bookHistory")) {
                        ba.getBookHistoryMenu().populate((JMenu) item, BookActions.class);
                    }
                }
            }
        }

        // Menu bar
        JMenuBar innerBar = mgr.getMenuBar();

        // Gauges = voices | progress | memory
        JPanel gauges = new JPanel();
        gauges.setLayout(new BorderLayout());
        gauges.add(SheetResultPainter.getVoicePanel(), BorderLayout.WEST);
        gauges.add(StepMonitoring.createMonitor().getComponent(), BorderLayout.CENTER);
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
        return GuiActions.getInstance().isLogWindowDisplayed()
               || GuiActions.getInstance().isErrorsWindowDisplayed();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //
    //------------------//
    // BoardsScrollPane //
    //------------------//
    /**
     * Just a scrollPane to host the pane of user boards, trying to offer
     * enough room for the boards.
     */
    private static class BoardsScrollPane
            extends JScrollPane
    {
        //~ Methods --------------------------------------------------------------------------------

        public void setBoards (JComponent boards)
        {
            setViewportView(boards);
            revalidate();
        }
    }

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

    //-----------------//
    // GuiExitListener //
    //-----------------//
    /**
     * Listener called when application asks for exit and does exit.
     */
    private static class GuiExitListener
            implements ExitListener
    {
        //~ Constructors ---------------------------------------------------------------------------

        public GuiExitListener ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean canExit (EventObject eo)
        {
            for (Book book : OMR.engine.getAllBooks()) {
                //                // Is script stored (or explicitly ignored)?
                //                if (!ScriptActions.checkStored(book.getScript())) {
                //                    return false;
                //                }
                //
                // Check whether the book has been saved (or user has declined)
                if (!BookActions.checkStored(book)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void willExit (EventObject eo)
        {
            // Store latest constant values on disk
            ConstantManager.getInstance().storeResource();

            // Close all books
            int count = 0;

            // NB: Use a COPY of instances, to avoid concurrent modification
            for (Book book : new ArrayList<Book>(OMR.engine.getAllBooks())) {
                book.close();
                count++;
            }

            logger.debug("{} book(s) closed", count);
        }
    }
}
