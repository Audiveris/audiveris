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

import omr.constant.*;

import omr.score.ScoreController;

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
import static omr.ui.util.UIUtilities.*;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.reflect.Constructor;

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

    /** Fix internal split locations (workaround TBD) */
    private static final int DELTA_DIVIDER = 10;

    //~ Instance fields --------------------------------------------------------

    /** Log pane, which displays logging info */
    public LogPane logPane;

    /** User actions for scores */
    public final ScoreController scoreController;

    /** Sheet tabbed pane, which may contain several views */
    public final SheetController sheetController = new SheetController();

    /** The related concrete frame */
    private JFrame frame;

    // Menus & tools in the frame
    private final JMenu stepMenu = new StepMenu("Step").getMenu();

    /** The splitted panes */
    private JSplitPane bigSplitPane;
    private JSplitPane  splitPane;

    /** Bottom pane split betwen the logPane and the errorsPane */
    private BottomPane bottomPane;

    /** The tool bar */
    private JToolBar toolBar;

    /** Used to remember the current user desired target */
    private Object target;

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
        frame = new JFrame();

        defineMenus();
        
        scoreController = new ScoreController();

        // Frame title
        updateGui();

        defineLayout();

        // Stay informed on sheet selection
        SheetManager.getSelection()
                    .addObserver(this);

        frame.addWindowListener(
            new WindowAdapter() {
                    @Override
                    public void windowClosing (WindowEvent e)
                    {
                        exit(); // Needed for last wishes.
                    }
                });

        // Differ realization
        EventQueue.invokeLater(new FrameShower(frame));
    }

    //~ Methods ----------------------------------------------------------------

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

    //----------//
    // isTarget //
    //----------//
    /**
     * Check whether the provided sheet file name is consistent with the
     * recorded user target.
     *
     * @param name the (canonical) sheet file name
     *
     * @return true if the name is consistent with user target
     */
    public boolean isTarget (String name)
    {
        boolean result = false;

        if (target instanceof omr.score.Score) {
            omr.score.Score targetScore = (omr.score.Score) target;
            result = targetScore.getImagePath()
                                .equals(name);
        } else if (target instanceof Sheet) {
            Sheet targetSheet = (Sheet) target;
            result = targetSheet.getPath()
                                .equals(name);
        } else if (target instanceof String) {
            String targetString = (String) target;
            result = targetString.equals(name);
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                "isTarget this=" + target + " test=" + name + " -> " + result);
        }

        return result;
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

    //-----------//
    // setTarget //
    //-----------//
    /**
     * Specify what the current interest of the user is, by means of the current
     * score. Thus, when for example a sheet image is loaded sometime later,
     * this information will be used to trigger or not the actual display of the
     * sheet view.
     *
     * @param score the contextual score
     */
    public void setTarget (omr.score.Score score)
    {
        setObjectTarget(score);
    }

    //-----------//
    // setTarget //
    //-----------//
    /**
     * Specify what the current interest of the user is, by means of the desired
     * sheet file name.
     *
     * @param name the (canonical) sheet file name
     */
    public void setTarget (String name)
    {
        setObjectTarget(name);
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
                            // Menus
                            stepMenu.setEnabled(true);
                            scoreController.setEnabled(
                                sheet.getScore() != null);

                            // Frame title tells sheet name + step
                            sb.append(sheet.getRadix())
                              .append(" - ")
                              .append(sheet.currentStep())
                              .append(" - ");
                        } else {
                            // Menus
                            stepMenu.setEnabled(false);
                            scoreController.setEnabled(false);
                        }

                        // Update frame title
                        sb.append(Main.getToolName())
                          .append(" ")
                          .append(Main.getToolVersion());
                        frame.setTitle(sb.toString());
                    }
                });
    }

    //------//
    // exit // 
    //------//
    /**
     * Last wishes before application actually exits
     */
    void exit ()
    {
        // Save scripts for opened sheets?
        SheetManager.getInstance()
                    .closeAll();

        // Remember latest gui frame parameters
        final int state = frame.getExtendedState();
        constants.frameState.setValue(state);

        if (state == Frame.NORMAL) {
            // Remember frame location?
            Rectangle bounds = frame.getBounds();
            constants.frameX.setValue(bounds.x);
            constants.frameY.setValue(bounds.y);
            constants.frameWidth.setValue(bounds.width);
            constants.frameHeight.setValue(bounds.height);

            // Remember internal split locations
            constants.logDivider.setValue(splitPane.getDividerLocation());
            constants.boardDivider.setValue(bigSplitPane.getDividerLocation());
            SheetAssembly.storeScoreSheetDivider();
        } else { // Maximized/Iconified window

            if (state == Frame.MAXIMIZED_BOTH) {
                // Remember internal split locations
                constants.logDivider.setValue(
                    splitPane.getDividerLocation() - DELTA_DIVIDER);

                constants.boardDivider.setValue(
                    bigSplitPane.getDividerLocation() - DELTA_DIVIDER);
                SheetAssembly.storeScoreSheetDivider();
            }
        }

        // Remember internal division of the bottom pane?
        bottomPane.storeDivider();

        // Store latest constant values on disk
        ConstantManager.storeResource();

        // That's all folks !
        java.lang.System.exit(0);
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

        // Bottom = Log & Errors
        bottomPane = new BottomPane(logPane.getComponent());

        splitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            sheetController.getComponent(),
            bottomPane);
        splitPane.setBorder(null);
        splitPane.setDividerSize(2);
        splitPane.setDividerLocation(constants.logDivider.getValue());
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
        bigSplitPane.setBorder(null);
        bigSplitPane.setDividerSize(2);
        bigSplitPane.setDividerLocation(constants.boardDivider.getValue());
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
        ActionManager mgr = ActionManager.getInstance();

        // Specific sheet menu
        JMenu sheetMenu = new SeparableMenu("File");
        UIDressing.dressUp(sheetMenu, getClass().getName() + ".sheetMenu");

        // Specific history submenu
        JMenuItem historyMenu = SheetManager.getInstance()
                                            .getHistory()
                                            .menu(
            "Sheet History",
            new HistoryListener());
        UIDressing.dressUp(historyMenu, getClass().getName() + ".historyMenu");
        sheetMenu.add(historyMenu);
        mgr.injectMenu("File", sheetMenu);

        // Specific step menu
        UIDressing.dressUp(stepMenu, getClass().getName() + ".stepMenu");
        mgr.injectMenu("Step", stepMenu);

        // All other commands
        mgr.registerAllActions();

        toolBar = mgr.getToolBar();
        frame.setJMenuBar(mgr.getMenuBar());

        //        // Help (TBC with Brenton!)
        //        if (!omr.Main.MAC_OS_X) {
        //            new AboutAction(helpMenu);
        //        }

        //Mac Application menu
        if (omr.Main.MAC_OS_X) {
            try {
                Class       clazz = Class.forName("omr.ui.MacApplication");
                Constructor constructor = clazz.getConstructor(
                    new Class[] { Action.class, Action.class, Action.class });
                constructor.newInstance(
                    new GuiActions.AboutAction(),
                    new GuiActions.OptionsAction(),
                    new GuiActions.ExitAction());
            } catch (Exception e) {
                logger.warning("Unable to load Mac OS X Application menu", e);
            }
        }
    }

    //-----------------//
    // setObjectTarget //
    //-----------------//
    private synchronized void setObjectTarget (Object target)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setObjectTarget " + target);
        }

        this.target = target;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        PixelCount       boardDivider = new PixelCount(
            200,
            "Where the separation on left of board pane should be");
        PixelCount       bottomDivider = new PixelCount(
            200,
            "Where the separation on top of bottom pane should be");
        PixelCount       frameHeight = new PixelCount(
            740,
            "Height of the main frame");
        Constant.Integer frameState = new Constant.Integer(
            null,
            Frame.NORMAL,
            "Initial frame state (0=normal, 1=iconified, 6=maximized)");
        PixelCount       frameWidth = new PixelCount(
            1024,
            "Width of the main frame");
        PixelCount       frameX = new PixelCount(
            0,
            "Left position of the main frame");
        PixelCount       frameY = new PixelCount(
            0,
            "Top position of the main frame");
        PixelCount       logDivider = new PixelCount(
            622,
            "Where the separation above log pane should be");
    }

    //------------//
    // BoardsPane //
    //------------//
    private static class BoardsPane
        extends Panel
    {
        public BoardsPane ()
        {
            setNoInsets();
        }

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

    //-------------//
    // FrameShower //
    //-------------//
    private static class FrameShower
        implements Runnable
    {
        final Frame frame;

        public FrameShower (Frame frame)
        {
            this.frame = frame;
        }

        @Implement(Runnable.class)
        public void run ()
        {
            frame.pack();
            frame.setBounds(
                constants.frameX.getValue(),
                constants.frameY.getValue(),
                constants.frameWidth.getValue(),
                constants.frameHeight.getValue());
            frame.setExtendedState(constants.frameState.getValue());
            frame.setVisible(true);
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
        PixelCount divider = constants.bottomDivider;

        public BottomPane (JComponent left)
        {
            super(JSplitPane.HORIZONTAL_SPLIT, left, null);
            setBorder(null);
            setDividerSize(2);
        }

        public void addErrors (JComponent errorsPane)
        {
            removeErrors();
            setRightComponent(errorsPane);
            loadDivider();
            revalidate();
            repaint();
        }

        public void removeErrors ()
        {
            storeDivider();
            setRightComponent(null);
        }

        public void storeDivider ()
        {
            if (getRightComponent() != null) {
                final int state = frame.getExtendedState();

                if ((state == Frame.NORMAL) || (state == Frame.MAXIMIZED_BOTH)) {
                    divider.setValue(getDividerLocation());

                    ///logger.info("Divider stored as " + divider.getValue());
                } else {
                    ///logger.info("Divider not stored");
                }
            }
        }

        private void loadDivider ()
        {
            ///logger.info("Divider loaded as " + divider.getValue());
            setDividerLocation(divider.getValue());
        }
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
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            String fileName = e.getActionCommand();
            Main.getGui()
                .setTarget(fileName);
            Step.LOAD.performParallel(null, new File(fileName));
        }
    }
}
