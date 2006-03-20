//-----------------------------------------------------------------------//
//                                                                       //
//                                 J u i                                 //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.Main;
import omr.Step;
import omr.StepMenu;
import omr.constant.*;
import omr.glyph.Shape;
import omr.glyph.ui.GlyphTrainer;
import omr.glyph.ui.ShapeColorChooser;
import omr.glyph.ui.GlyphVerifier;
import omr.score.ScoreController;
import omr.sheet.Sheet;
import omr.sheet.SheetController;
import omr.ui.icon.IconManager;
import omr.ui.util.Panel;
import omr.ui.treetable.JTreeTable;
import omr.ui.util.MemoryMeter;
import omr.util.Logger;
import omr.util.Memory;

import static omr.ui.util.UIUtilities.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.border.Border;

/**
 * Class <code>Jui</code> is the Java User Interface, the main class for
 * displaying a score, the related sheet, the message log and the various
 * tools.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Jui
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(Jui.class);

    //~ Instance variables ------------------------------------------------

    // The related concrete frame
    private JFrame frame;

    // Used to remember the current user desired target
    private Object target;

    // Menus & tools in the frame
    private final JMenu    fileMenu = new JMenu("File");
    private final JMenu    stepMenu = new StepMenu("Step").getMenu();
    private final JMenu    toolMenu = new JMenu("Tool");
    private final JMenu    helpMenu = new JMenu("Help");
    private final JToolBar toolBar;

    /**
     * Sheet tabbed pane, which may contain several views
     */
    public final SheetController sheetPane;

    /**
     * Log pane, which displays logging info
     */
    public final LogPane logPane;

    /**
     * Boards pane, which displays several boards
     */
    private Panel boardsHolder;

    // The splitted panes
    private final JSplitPane splitPane;
    private final JSplitPane bigSplitPane;

    // Color chooser for shapes
    private JFrame shapeColorFrame;

    /** User actions for scores */
    public final ScoreController scoreController;

    //~ Constructors ------------------------------------------------------

    //-----//
    // Jui //
    //-----//
    /**
     * Creates a new <code>Jui</code> instance, to handle any user display
     * and interaction.
     */
    public Jui ()
    {
        frame = new JFrame();

        frame.addWindowListener(new WindowAdapter()
        {
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
        sheetPane = new SheetController(this, toolBar);

        // Score actions
        toolBar.addSeparator();
        scoreController = new ScoreController(toolBar);

        // Frame title
        updateTitle();

        // Tools
        new ShapeAction(toolMenu);
        toolMenu.addSeparator();
        new MaterialAction(toolMenu);
        new TrainerAction(toolMenu);
        toolMenu.addSeparator();
        new MemoryAction(toolMenu);
        new OptionAction(toolMenu);

        // Help
        new AboutAction(helpMenu);

        // Menus in the frame
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        menuBar.add(fileMenu);
        menuBar.add(sheetPane.getMenu());
        menuBar.add(stepMenu);
        menuBar.add(scoreController.getMenu());
        menuBar.add(toolMenu);
        menuBar.add(Box.createHorizontalStrut(30));
        menuBar.add(helpMenu);

        /*
           +==============================================================+
           | toolKeyPanel                                                 |
           | +================+============================+============+ |
           | | toolBar        | progressBar                |   Memory   | |
           | +================+============================+============+ |
           +=================================================+============+
           | bigSplitPane                                    |            |
           | +=============================================+ |            |
           | | sheetPane                                   | | boardsPane |
           | |                                             | |            |
           | |                                             | |            |
           | |                                             | |            |
           | |                                             | |            |
           | |                                             | |            |
           | |                                             | |            |
           | |                                             | |            |
           | |                                             | |            |
           | |                                             | |            |
           | |                                             | |            |
           | |                                             | |            |
           | +=============================================+ |            |
           | | logPane                                     | |            |
           | |                                             | |            |
           | |                                             | |            |
           | +=============================================+ |            |
           +=================================================+============+
         */
        // Use a layout with toolbar on top and a double split pane below
        frame.getContentPane().setLayout(new BorderLayout());

        logPane = new LogPane();
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                   sheetPane.getComponent(),
                                   logPane.getComponent());
        splitPane.setBorder(null);
        splitPane.setDividerSize(2);
        splitPane.setDividerLocation(constants.logDivider.getValue());
        splitPane.setResizeWeight(1d);  // Give extra space to left part

        JPanel toolKeyPanel = new JPanel();
        toolKeyPanel.setLayout(new BorderLayout());
        toolKeyPanel.add(toolBar, BorderLayout.WEST);
        toolKeyPanel.add(Step.createMonitor().getComponent(),
                         BorderLayout.CENTER);
        toolKeyPanel.add
            (new MemoryMeter
             (IconManager.buttonIconOf("general/Delete")).getComponent(),
             BorderLayout.EAST);

        // Boards
        boardsHolder = new Panel();
        boardsHolder.setNoInsets();

        bigSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                      splitPane, boardsHolder);
        bigSplitPane.setBorder(null);
        bigSplitPane.setDividerSize(2);
        bigSplitPane.setDividerLocation(constants.boardDivider.getValue());
        bigSplitPane.setResizeWeight(1d); // Give extra space to left part

        // Global layout
        frame.getContentPane().add(toolKeyPanel, BorderLayout.NORTH);
        frame.getContentPane().add(bigSplitPane, BorderLayout.CENTER);

        // Differ realization
        EventQueue.invokeLater(new FrameShower(frame));
    }

    //-------------//
    // FrameShower //
    //-------------//
    private static class FrameShower
        implements Runnable
    {
        final Frame frame;

        public FrameShower(Frame frame)
        {
            this.frame = frame;
        }

        public void run()
        {
            frame.pack();
            frame.setBounds(constants.frameX.getValue(),
                            constants.frameY.getValue(),
                            constants.frameWidth.getValue(),
                            constants.frameHeight.getValue());
            frame.setExtendedState(constants.frameState.getValue());
            frame.setVisible(true);
        }
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // getFrame //
    //----------//
    /**
     * Report the concrete frame
     *
     * @return the ui frame
     */
    public JFrame getFrame()
    {
        return frame;
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
        JEditorPane htmlPane = new JEditorPane("text/html",
                                               htmlStr);
        htmlPane.setEditable(false);

        JOptionPane.showMessageDialog
            (frame, htmlPane, "Warning", JOptionPane.WARNING_MESSAGE);
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
        JEditorPane htmlPane = new JEditorPane("text/html",
                                               htmlStr);
        htmlPane.setEditable(false);

        JOptionPane.showMessageDialog
            (frame, htmlPane);
    }

    //---------------//
    // addBoardsPane //
    //---------------//
    /**
     * Add a new boardspane to the boards holder
     *
     * @param boards the boards pane to be added
     */
    public void addBoardsPane (BoardsPane boards)
    {
        boards.getComponent().setVisible(false);
        boardsHolder.add(boards.getComponent());
    }

    //----------------//
    // showBoardsPane //
    //----------------//
    /**
     * Display the selected boardspane
     *
     * @param boards the boards pane to be displayed
     */
    public void showBoardsPane (BoardsPane boards)
    {
        logger.fine("showing " + boards);

        for (Component component : boardsHolder.getComponents()) {
            if (component != boards.getComponent()) {
                component.setVisible(false);
            }
        }
        boards.getComponent().setVisible(true);
    }

    //------------------//
    // removeBoardsPane //
    //------------------//
    /**
     * Remove the selected boardspane
     *
     * @param boards the boards pane to be removed
     */
    public void removeBoardsPane (BoardsPane boards)
    {
        boardsHolder.remove(boards.getComponent());
        logger.fine("removed " + boards + " holderCount=" +
                     boardsHolder.getComponentCount());

        // Refresh the display
        boardsHolder.repaint();
    }

    //-----------//
    // setTarget //
    //-----------//
    /**
     * Specify what the current interest of the user is, by means of the
     * current score. Thus, when for example a sheet image is loaded
     * sometime later, this information will be used to trigger or not the
     * actual display of the sheet view.
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
     * Specify what the current interest of the user is, by means of the
     * desired sheet file name.
     *
     * @param name the (canonical) sheet file name
     */
    public void setTarget (String name)
    {
        setObjectTarget(name);
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
            result = targetScore.getImageFPath().equals(name);
        } else if (target instanceof Sheet) {
            Sheet targetSheet = (Sheet) target;
            result = targetSheet.getPath().equals(name);
        } else if (target instanceof String) {
            String targetString = (String) target;
            result = targetString.equals(name);
        }

        if (logger.isFineEnabled()) {
            logger.fine("isTarget this=" + target +
                         " test=" + name + " -> " + result);
        }

        return result;
    }

    //-------------//
    // updateTitle //
    //-------------//
    /**
     * This method is called whenever a display modification has occurred,
     * either a score or sheet, so that the frame title always shows what
     * the current context is.
     */
    public void updateTitle ()
    {
        final String toolInfo = Main.toolName + " " + Main.toolVersion;

        // Look for sheet first
        Sheet sheet = sheetPane.getCurrentSheet();

        if (sheet != null) {
            StringBuffer sb = new StringBuffer();
            sb.append(sheet.getName());

            Step step = sheet.currentStep();
            if (step != null) {
                sb.append(" - ").append(step);
            }

            sb.append(" - ").append(toolInfo);

            frame.setTitle(sb.toString());
        } else {
            // Look for score second
            omr.score.Score score = scoreController.getCurrentScore();

            if (score != null) {
                frame.setTitle(score.getRadix() + " - " + toolInfo);
            } else {
                frame.setTitle(toolInfo);
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
        notify();
    }

    //------//
    // exit // Last wishes before application actually exit
    //------//
    private void exit ()
    {
        // Remember latest jui frame parameters
        final int state = frame.getExtendedState();
        constants.frameState.setValue(state);


        if (state == Frame.NORMAL) {
            Rectangle bounds = frame.getBounds();
            constants.frameX.setValue(bounds.x);
            constants.frameY.setValue(bounds.y);
            constants.frameWidth.setValue(bounds.width);
            constants.frameHeight.setValue(bounds.height);

            // Remember internal split locations
            constants.logDivider.setValue(splitPane.getDividerLocation());
            constants.boardDivider.setValue(bigSplitPane.getDividerLocation());
        } else {                        // Mamimized/Iconified window
            if (state == Frame.MAXIMIZED_BOTH) {
                // Remember internal split locations
                // Fix internal split locations (workaround TBD)
                final int deltaDivider = 10;
                constants.logDivider.setValue
                    (splitPane.getDividerLocation() - deltaDivider);
                constants.boardDivider.setValue
                    (bigSplitPane.getDividerLocation() - deltaDivider);
            }
        }

        // Store latest constant values on disk
        ConstantManager.storeResource();

        // That's all folks !
        System.exit(0);
    }

    //~ Classes -----------------------------------------------------------

    //------------//
    // ExitAction //
    //------------//
    private class ExitAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public ExitAction (JMenu menu)
        {
            super("Exit", IconManager.buttonIconOf("general/Stop"));

            final String tiptext = "Exit the program";
            menu.add(this).setToolTipText(tiptext);

            final JButton button = toolBar.add(this);
            button.setBorder(getToolBorder());
            button.setToolTipText(tiptext);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            exit();
        }
    }

    //--------------//
    // MemoryAction //
    //--------------//
    private class MemoryAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public MemoryAction (JMenu menu)
        {
            super("Memory", IconManager.buttonIconOf("general/Find"));

            final String tiptext = "Show occupied memory";

            menu.add(this).setToolTipText(tiptext);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            logger.info("Occupied memory is " + Memory.getValue() + " bytes");
        }
    }

    //---------------//
    // TrainerAction //
    //---------------//
    private class TrainerAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public TrainerAction (JMenu menu)
        {
            super("Trainer", IconManager.buttonIconOf("media/Movie"));

            final String tiptext = "Launch trainer interface";
            menu.add(this).setToolTipText(tiptext);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            GlyphTrainer glyphTrainer = GlyphTrainer.getInstance();
            glyphTrainer.getFrame().setVisible(true);
            glyphTrainer.getFrame().toFront();
        }
    }

    //--------------//
    // OptionAction //
    //--------------//
    private class OptionAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public OptionAction (JMenu menu)
        {
            super("Options", IconManager.buttonIconOf("general/Properties"));
            menu.add(this).setToolTipText("Constants tree for all units");
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            // Preload constant units
            UnitManager.getInstance(Main.class.getName());

            JFrame frame = new JFrame("Units Options");
            frame.getContentPane().setLayout(new BorderLayout());

            JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
            frame.getContentPane().add(toolBar, BorderLayout.NORTH);

            JButton button = new JButton(new AbstractAction()
            {
                public void actionPerformed (ActionEvent e)
                {
                    UnitManager.getInstance().dumpAllUnits();
                }
            });
            button.setText("Dump all Units");
            toolBar.add(button);

            UnitModel cm = new UnitModel();
            JTreeTable jtt = new UnitTreeTable(cm);
            frame.getContentPane().add(new JScrollPane(jtt));

            frame.pack();
            frame.setSize(constants.paramWidth.getValue(),
                          constants.paramHeight.getValue());
            frame.setVisible(true);
        }
    }

    //-------------//
    // ShapeAction //
    //-------------//
    private class ShapeAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public ShapeAction (JMenu menu)
        {
            super("Shape Colors", IconManager.buttonIconOf("general/Properties"));
            menu.add(this).setToolTipText("Manage colors of all shapes");
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            if (shapeColorFrame == null) {
                shapeColorFrame = new JFrame("ShapeColorChooser");

                // Create and set up the content pane.
                JComponent newContentPane
                    = new ShapeColorChooser().getComponent();
                newContentPane.setOpaque(true); //content panes must be opaque
                shapeColorFrame.setContentPane(newContentPane);

                // Realize the window.
                shapeColorFrame.pack();
            }

            // Display the window.
            shapeColorFrame.setVisible(true);
        }
    }

    //----------------//
    // MaterialAction //
    //----------------//
    private class MaterialAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public MaterialAction (JMenu menu)
        {
            super("Training Material",
                  IconManager.buttonIconOf("general/Properties"));
            menu.add(this).setToolTipText("Verify training material");
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            GlyphVerifier.getInstance().getFrame().setVisible(true);
        }
    }

    //-------------//
    // AboutAction //
    //-------------//
    private class AboutAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public AboutAction (JMenu menu)
        {
            super("About", IconManager.buttonIconOf("general/About"));

            final String tiptext = "About " + Main.toolName;
            menu.add(this).setToolTipText(tiptext);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            StringBuffer sb = new StringBuffer();
            sb
                .append("<HTML>")
                .append("<B>").append(Main.toolName).append("</B> ")
                .append("<I>version ")
                .append(Main.toolVersion)
                .append("<BR>")
                .append(" build ")
                .append(Main.toolBuild)
                .append("</I>")
                .append("<BR>")
                .append("Refer to <B>https://audiveris.dev.java.net</B>")
                .append("</HTML>");

            displayMessage(sb.toString());
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
        extends ConstantSet
    {
        //~ Instance variables --------------------------------------------

        Constant.Integer logDivider = new Constant.Integer
                (622,
                 "Where the separation above log pane should be");

        Constant.Integer boardDivider = new Constant.Integer
                (200,
                 "Where the separation on left of board pane should be");

        Constant.Integer frameState = new Constant.Integer
                (Frame.NORMAL,
                 "Initial frame state (0=normal, 1=iconified, 6=maximized");

        Constant.Integer frameX = new Constant.Integer
                (0,
                 "Left position in pixels of the main frame");

        Constant.Integer frameY = new Constant.Integer
                (0,
                 "Top position in pixels of the main frame");

        Constant.Integer frameWidth = new Constant.Integer
                (1024,
                 "Width in pixels of the main frame");

        Constant.Integer frameHeight = new Constant.Integer
                (740,
                 "Height in pixels of the main frame");

        Constant.Integer paramWidth = new Constant.Integer
                (900,
                 "Width in pixels of the param frame");

        Constant.Integer paramHeight = new Constant.Integer
                (500,
                 "Height in pixels of the param frame");

        Constants ()
        {
            initialize();
        }
    }
}
