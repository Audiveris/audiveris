//----------------------------------------------------------------------------//
//                                                                            //
//                                M a i n G u i                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.ui;

import omr.Main;

import omr.action.ActionManager;
import omr.action.Actions;

import omr.constant.*;

import omr.score.midi.MidiActions;
import omr.score.ui.ScoreController;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionObserver;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.step.Step;
import omr.step.StepMenu;

import omr.ui.icon.IconManager;
import omr.ui.util.MemoryMeter;
import omr.ui.util.Panel;
import omr.ui.util.SeparableMenu;
import omr.ui.util.UIUtilities;

import omr.util.Implement;
import omr.util.Logger;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.EventObject;

import javax.swing.*;

/**
 * Class <code>MainGui</code> is the Java User Interface, the main class for
 * displaying a score, the related sheet, the message log and the various tools.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class MainGui
    implements SelectionObserver
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MainGui.class);

    //~ Instance fields --------------------------------------------------------

    /** Cache the application */
    private final Application app;

    /** Log pane, which displays logging info */
    public LogPane logPane;

    /** User actions for scores */
    public final ScoreController scoreController;

    /** Sheet tabbed pane, which may contain several views */
    public final SheetController sheetController = new SheetController();

    /** The related concrete frame */
    private JFrame frame;

    /** The splitted panes */
    private JSplitPane bigSplitPane;
    private JSplitPane splitPane;

    /** Bottom pane split betwen the logPane and the errorsPane */
    private BottomPane bottomPane;

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
     *
     * @param frame the frame provided by SAF
     */
    public MainGui (JFrame frame)
    {
        this.frame = frame;
        app = Application.getInstance();

        defineMenus();

        scoreController = new ScoreController();

        // Frame title
        updateGui();

        defineLayout();

        // Stay informed on sheet selection
        SheetManager.getSelection()
                    .addObserver(this);

        // Exit listener
        app.addExitListener(new MaybeExit());
    }

    //~ Methods ----------------------------------------------------------------

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

    //---------------//
    // setErrorsPane //
    //---------------//
    /**
     * Set/show a new errors pane
     *
     * @param errorsPane the errors pane to be shown
     */
    public void setErrorsPane (JComponent errorsPane)
    {
        bottomPane.addErrors(errorsPane);
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
    @Implement(SelectionObserver.class)
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

    //------------------//
    // removeErrorsPane //
    //------------------//
    /**
     * Remove the current errors pane, if any
     */
    public void removeErrorsPane ()
    {
        bottomPane.removeErrors();
    }

    //--------//
    // update //
    //--------//
    /**
     * Notification of sheet selection, to update frame title
     *
     * @param selection the selection object (SHEET)
     * @param hint processing hint (not used)
     */
    @Implement(SelectionObserver.class)
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        switch (selection.getTag()) {
        case SHEET :
            updateGui();

            break;

        default :
        }
    }

    //-----------//
    // updateGui //
    //-----------//
    /**
     * This method is called whenever a modification has occurred, either a
     * score or sheet, so that the frame title and the pull-down menus are
     * always consistent with the current context.
     */
    public void updateGui ()
    {
        final Sheet sheet = SheetManager.getSelectedSheet();

        SwingUtilities.invokeLater(
            new Runnable() {
                    public void run ()
                    {
                        final StringBuilder sb = new StringBuilder();

                        if (sheet != null) {
                            // Frame title tells sheet name + step
                            sb.append(sheet.getRadix())
                              .append(" - ")
                              .append(sheet.currentStep())
                              .append(" - ");
                        }

                        // Update frame title
                        sb.append(
                            app.getContext().getResourceMap().getString(
                                "mainFrame.title"));
                        frame.setTitle(sb.toString());
                    }
                });
    }

    //------------------------------//
    // useSwingApplicationFramework //
    //------------------------------//
    public static boolean useSwingApplicationFramework ()
    {
        return constants.useSwingApplicationFramework.getValue();
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

        splitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            sheetController.getComponent(),
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

        bigSplitPane = new JSplitPane(
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
        JMenuItem historyMenu = SheetManager.getInstance()
                                            .getHistory()
                                            .menu(
            "Sheet History",
            new HistoryListener());
        sheetMenu.add(historyMenu);

        // Specific step menu
        JMenu stepMenu = new StepMenu(new SeparableMenu()).getMenu();

        if (constants.useSwingApplicationFramework.getValue()) {
            // For history sub-menu
            ResourceMap resource = Main.getInstance()
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
        } else {
            // Old implementation
            MidiActions.getInstance();
            UIDressing.dressUp(
                historyMenu,
                getClass().getName() + ".historyMenu");
            UIDressing.dressUp(sheetMenu, getClass().getName() + ".sheetMenu");
            UIDressing.dressUp(stepMenu, getClass().getName() + ".stepMenu");

            omr.ui.ActionManager mgr = omr.ui.ActionManager.getInstance();
            mgr.injectMenu("File", sheetMenu);
            mgr.injectMenu("Step", stepMenu);

            // All other commands
            mgr.loadAllClasses();
            mgr.registerAllActions();
            toolBar = mgr.getToolBar();
            frame.setJMenuBar(mgr.getMenuBar());
        }

        // Mac Application menu
        MacApplication.setupMacMenus();
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
    private class BottomPane
        extends JSplitPane
    {
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
            revalidate();
            repaint();
        }

        public void removeErrors ()
        {
            setRightComponent(null);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean useSwingApplicationFramework = new Constant.Boolean(
            true,
            "Should we use the Swing Application Framework to define Actions?");
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
            String fileName = e.getActionCommand();
            Step.LOAD.performParallel(null, new File(fileName));
        }
    }

    //-----------//
    // MaybeExit //
    //-----------//
    private class MaybeExit
        implements Application.ExitListener
    {
        //~ Methods ------------------------------------------------------------

        public boolean canExit (EventObject e)
        {
            // Make sure all scripts are stored (or explicitly ignored)
            return SheetManager.getInstance()
                               .areAllScriptsStored();
        }

        public void willExit (EventObject e)
        {
            // Store latest constant values on disk
            ConstantManager.storeResource();
        }
    }
}
