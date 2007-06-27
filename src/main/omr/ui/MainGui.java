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

import omr.glyph.ui.GlyphLagView;
import omr.glyph.ui.GlyphTrainer;
import omr.glyph.ui.GlyphVerifier;
import omr.glyph.ui.ShapeColorChooser;

import omr.score.ScoreController;
import omr.score.visitor.ScorePainter;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionObserver;

import omr.sheet.HorizontalsBuilder;
import omr.sheet.LinesBuilder;
import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.step.Step;
import omr.step.StepMenu;

import omr.ui.SheetController;
import omr.ui.icon.IconManager;
import omr.ui.treetable.JTreeTable;
import omr.ui.util.MemoryMeter;
import omr.ui.util.Panel;
import static omr.ui.util.UIUtilities.*;

import omr.util.Implement;
import omr.util.Logger;
import omr.util.Memory;

import java.awt.*;
import java.awt.event.*;

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

    /**
     * Log pane, which displays logging info
     */
    public final LogPane logPane;

    /** User actions for scores */
    public final ScoreController scoreController;

    /**
     * Sheet tabbed pane, which may contain several views
     */
    public final SheetController sheetController;

    /** The related concrete frame */
    private JFrame frame;

    // Menus & tools in the frame
    private final JMenu      fileMenu = new JMenu("File");
    private final JMenu      helpMenu = new JMenu("Help");
    private final JMenu      stepMenu = new StepMenu("Step").getMenu();
    private final JMenu      viewMenu = new JMenu("Views");
    private final JMenu      toolMenu = new JMenu("Tools");
    private final JSplitPane bigSplitPane;

    /** The splitted panes */
    private final JSplitPane splitPane;

    /** Bottom pane split betwen the logPane and the errorsPane */
    private final BottomPane bottomPane;

    /** The tool bar */
    private final JToolBar toolBar;

    /** Color chooser for shapes */
    private JFrame shapeColorFrame;

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

        frame.addWindowListener(
            new WindowAdapter() {
                    public void windowClosing (WindowEvent e)
                    {
                        exit(); // Needed for last wishes.
                    }
                });

        // Build the UI part
        //------------------
        // Tools in the frame and set of actions
        toolBar = new JToolBar(JToolBar.HORIZONTAL); // VERTICAL

        // File actions
        new ExitAction(fileMenu);

        // Sheet actions
        sheetController = new SheetController(this, toolBar);

        // Score actions
        toolBar.addSeparator();
        scoreController = new ScoreController(toolBar);

        // Test actions
        toolBar.addSeparator();

        if (constants.showTestAction.getValue()) {
            new TestAction();
        }

        if (constants.showFineAction.getValue()) {
            new FineAction();
        }

        // Frame title
        updateTitle();

        // Views
        ScorePainter.insertMenuItems(viewMenu);
        viewMenu.addSeparator();
        GlyphLagView.insertMenuItems(viewMenu);
        viewMenu.addSeparator();
        viewMenu.add(new JCheckBoxMenuItem(new LineAction()))
                .setSelected(LinesBuilder.getDisplayOriginalStaffLines());
        viewMenu.add(new JCheckBoxMenuItem(new LedgerAction()))
                .setSelected(HorizontalsBuilder.getDisplayLedgerLines());
        viewMenu.addSeparator();
        new ClearLogAction(viewMenu);

        // Tools
        new ShapeAction(toolMenu);
        toolMenu.addSeparator();
        new MaterialAction(toolMenu);
        new TrainerAction(toolMenu);
        toolMenu.addSeparator();
        new MemoryAction(toolMenu);
        toolMenu.addSeparator();
        new OptionAction(toolMenu);

        // Help
        new AboutAction(helpMenu);

        // Menus in the frame
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        menuBar.add(fileMenu);
        menuBar.add(sheetController.getMenu());
        menuBar.add(stepMenu);
        menuBar.add(scoreController.getMenu());
        menuBar.add(viewMenu);
        menuBar.add(toolMenu);
        menuBar.add(Box.createHorizontalStrut(30));
        menuBar.add(helpMenu);

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
        toolKeyPanel.add(toolBar, BorderLayout.WEST);
        toolKeyPanel.add(
            Step.createMonitor().getComponent(),
            BorderLayout.CENTER);
        toolKeyPanel.add(
            new MemoryMeter(
                IconManager.getInstance().loadImageIcon("general/Delete")).getComponent(),
            BorderLayout.EAST);

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
             .add(toolKeyPanel, BorderLayout.NORTH);
        frame.getContentPane()
             .add(bigSplitPane, BorderLayout.CENTER);

        // Stay informed on sheet selection
        SheetManager.getSelection()
                    .addObserver(this);

        // Differ realization
        EventQueue.invokeLater(new FrameShower(frame));
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // addBoardsPane //
    //---------------//
    /**
     * Add a new boardspane to the boards holder
     *
     * @param boards the boards pane to be added
     */
    public void addBoardsPane (JComponent boards)
    {
        boardsPane.addBoards(boards);
    }

    //---------------//
    // addErrorsPane //
    //---------------//
    /**
     * Add/show a new errors pane
     *
     * @param errorsPane the errors pane to be added
     */
    public void addErrorsPane (JComponent errorsPane)
    {
        bottomPane.addErrors(errorsPane);
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
        JOptionPane.showMessageDialog(frame, htmlPane);
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
            "Warning",
            JOptionPane.WARNING_MESSAGE);
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
            updateTitle();

            break;

        default :
        }
    }

    //-------------//
    // updateTitle //
    //-------------//
    /**
     * This method is called whenever a display modification has occurred,
     * either a score or sheet, so that the frame title always shows what the
     * current context is.
     */
    public void updateTitle ()
    {
        StringBuilder sb = new StringBuilder();
        Sheet         sheet = SheetManager.getSelectedSheet();

        if (sheet != null) {
            sb.append(sheet.getRadix());

            Step step = sheet.currentStep();

            if (step != null) {
                sb.append(" - ")
                  .append(step);
            }

            sb.append(" - ");
        }

        sb.append(Main.getToolName())
          .append(" ")
          .append(Main.getToolVersion());

        frame.setTitle(sb.toString());
    }

    //------//
    // exit // Last wishes before application actually exit
    //------//
    private void exit ()
    {
        // Remember latest gui frame parameters
        final int state = frame.getExtendedState();
        constants.frameState.setValue(state);

        // Remember frame location?
        if (state == Frame.NORMAL) {
            Rectangle bounds = frame.getBounds();
            constants.frameX.setValue(bounds.x);
            constants.frameY.setValue(bounds.y);
            constants.frameWidth.setValue(bounds.width);
            constants.frameHeight.setValue(bounds.height);
        }

        // Remember internal split locations?
        if ((state == Frame.NORMAL) || (state == Frame.MAXIMIZED_BOTH)) {
            constants.logDivider.setValue(splitPane.getDividerLocation());
            constants.boardDivider.setValue(bigSplitPane.getDividerLocation());
            SheetAssembly.storeScoreSheetDivider();
        }

        // Remember internal division of the bottom pane?
        bottomPane.storeDivider();

        // Store latest constant values on disk
        ConstantManager.storeResource();

        // That's all folks !
        java.lang.System.exit(0);
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
    // ExitAction //
    //------------//
    private class ExitAction
        extends AbstractAction
    {
        public ExitAction (JMenu menu)
        {
            super(
                "Exit",
                IconManager.getInstance().loadImageIcon("general/Stop"));
            putValue(SHORT_DESCRIPTION, "Exit the program");
            menu.add(this);
            toolBar.add(this)
                   .setBorder(getToolBorder());
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            exit();
        }
    }

    //------------//
    // FineAction //
    //------------//
    private class FineAction
        extends AbstractAction
    {
        public FineAction ()
        {
            super(
                "Fine",
                IconManager.getInstance().loadImageIcon("general/Find"));
            putValue(SHORT_DESCRIPTION, "Generic Fine Action");
            toolBar.add(this)
                   .setBorder(getToolBorder());
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Logger.getLogger(omr.selection.Selection.class)
                  .setLevel("FINE");
        }
    }

    //--------------//
    // MemoryAction //
    //--------------//
    private static class MemoryAction
        extends AbstractAction
    {
        public MemoryAction (JMenu menu)
        {
            super(
                "Memory",
                IconManager.getInstance().loadImageIcon("general/Find"));
            putValue(SHORT_DESCRIPTION, "Show occupied memory");
            menu.add(this);
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            logger.info("Occupied memory is " + Memory.getValue() + " bytes");
        }
    }

    //--------------//
    // OptionAction //
    //--------------//
    private static class OptionAction
        extends AbstractAction
    {
        public OptionAction (JMenu menu)
        {
            super(
                "Options",
                IconManager.getInstance().loadImageIcon("general/Properties"));
            putValue(SHORT_DESCRIPTION, "Constants tree for all units");
            menu.add(this);
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            // Preload constant units
            UnitManager.getInstance(Main.class.getName());

            JFrame frame = new JFrame("Units Options");
            frame.getContentPane()
                 .setLayout(new BorderLayout());

            JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
            frame.getContentPane()
                 .add(toolBar, BorderLayout.NORTH);

            JButton button = new JButton(
                new AbstractAction() {
                        public void actionPerformed (ActionEvent e)
                        {
                            UnitManager.getInstance()
                                       .dumpAllUnits();
                        }
                    });
            button.setText("Dump all Units");
            toolBar.add(button);

            UnitModel  cm = new UnitModel();
            JTreeTable jtt = new UnitTreeTable(cm);
            frame.getContentPane()
                 .add(new JScrollPane(jtt));

            frame.pack();
            frame.setSize(
                constants.paramWidth.getValue(),
                constants.paramHeight.getValue());
            frame.setVisible(true);
        }
    }

    //----------------//
    // ClearLogAction //
    //----------------//
    private class ClearLogAction
        extends AbstractAction
    {
        public ClearLogAction (JMenu menu)
        {
            super(
                "Clear Log",
                IconManager.getInstance().loadImageIcon("general/Cut"));
            putValue(SHORT_DESCRIPTION, "Clear the whole log display");
            menu.add(this);
            toolBar.add(this)
                   .setBorder(getToolBorder());
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            logPane.clearLog();
        }
    }

    //----------------//
    // MaterialAction //
    //----------------//
    private static class MaterialAction
        extends AbstractAction
    {
        public MaterialAction (JMenu menu)
        {
            super(
                "Training Material",
                IconManager.getInstance().loadImageIcon("media/Movie"));
            putValue(SHORT_DESCRIPTION, "Verify training material");
            menu.add(this);
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            GlyphVerifier.getInstance()
                         .setVisible(true);
        }
    }

    //-------------//
    // AboutAction //
    //-------------//
    private class AboutAction
        extends AbstractAction
    {
        public AboutAction (JMenu menu)
        {
            super(
                "About",
                IconManager.getInstance().loadImageIcon("general/About"));
            putValue(SHORT_DESCRIPTION, "About " + Main.getToolName());
            menu.add(this);
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            StringBuffer sb = new StringBuffer();
            sb.append("<HTML>")
              .append("<B>")
              .append(Main.getToolName())
              .append("</B> ")
              .append("<I>version ")
              .append(Main.getToolVersion());

            if (Main.getToolBuild() != null) {
                sb.append("<BR>")
                  .append(" build ")
                  .append(Main.getToolBuild());
            }

            sb.append("</I>")
              .append("<BR>")
              .append("Refer to <B>https://audiveris.dev.java.net</B>")
              .append("</HTML>");

            displayMessage(sb.toString());
        }
    }

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
        PixelCount       paramHeight = new PixelCount(
            500,
            "Height of the param frame");
        PixelCount       paramWidth = new PixelCount(
            900,
            "Width of the param frame");
        Constant.Boolean showTestAction = new Constant.Boolean(
            false,
            "DEBUG- Should we show the Test button ?");
        Constant.Boolean showFineAction = new Constant.Boolean(
            false,
            "DEBUG- Should we show the Fine button ?");
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
                    logger.info("Divider stored as " + divider.getValue());
                } else {
                    logger.info("Divider not stored");
                }
            }
        }

        private void loadDivider ()
        {
            logger.info("Divider loaded as " + divider.getValue());
            setDividerLocation(divider.getValue());
        }
    }

    //-------------//
    // ShapeAction //
    //-------------//
    private class ShapeAction
        extends AbstractAction
    {
        public ShapeAction (JMenu menu)
        {
            super(
                "Shape Colors",
                IconManager.getInstance().loadImageIcon("general/Properties"));
            putValue(SHORT_DESCRIPTION, "Manage colors of all shapes");
            menu.add(this);
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            if (shapeColorFrame == null) {
                shapeColorFrame = new JFrame("ShapeColorChooser");

                // Create and set up the content pane.
                JComponent newContentPane = new ShapeColorChooser().getComponent();
                newContentPane.setOpaque(true); //content panes must be opaque
                shapeColorFrame.setContentPane(newContentPane);

                // Realize the window.
                shapeColorFrame.pack();
            }

            // Display the window.
            shapeColorFrame.setVisible(true);
        }
    }

    //------------//
    // TestAction //
    //------------//
    private class TestAction
        extends AbstractAction
    {
        public TestAction ()
        {
            super(
                "Test",
                IconManager.getInstance().loadImageIcon("general/TipOfTheDay"));
            putValue(SHORT_DESCRIPTION, "Generic Test Action");
            toolBar.add(this)
                   .setBorder(getToolBorder());
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            UITest.test();
        }
    }

    //---------------//
    // TrainerAction //
    //---------------//
    private static class TrainerAction
        extends AbstractAction
    {
        public TrainerAction (JMenu menu)
        {
            super(
                "Trainer",
                IconManager.getInstance().loadImageIcon("media/Play"));
            putValue(SHORT_DESCRIPTION, "Launch trainer interface");
            menu.add(this);
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            GlyphTrainer.launch();
        }
    }

    //--------------//
    // LedgerAction //
    //--------------//
    private class LedgerAction
        extends AbstractAction
    {
        public LedgerAction ()
        {
            super("Show original ledger lines");
            putValue(SHORT_DESCRIPTION, "Show the original ledger lines");
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
            HorizontalsBuilder.setDisplayLedgerLines(item.isSelected());
        }
    }

    //------------//
    // LineAction //
    //------------//
    private class LineAction
        extends AbstractAction
    {
        public LineAction ()
        {
            super("Show original staff lines");
            putValue(SHORT_DESCRIPTION, "Show the original staff lines");
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
            LinesBuilder.setDisplayOriginalStaffLines(item.isSelected());
        }
    }
}
