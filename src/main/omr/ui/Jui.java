//-----------------------------------------------------------------------//
//                                                                       //
//                                 J u i                                 //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.ui;

import omr.Main;
import omr.Step;
import omr.constant.*;
import omr.glyph.ui.GlyphTrainer;
import omr.glyph.ui.ShapeColorChooser;
import omr.glyph.ui.GlyphVerifier;
import omr.sheet.Sheet;
import omr.ui.treetable.JTreeTable;
import omr.util.Logger;
import omr.util.Memory;

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
 */
public class Jui
    extends JFrame
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(Jui.class);

    /**
     * A tool border to use consistently in all UI components
     */
    static final Border toolBorder = BorderFactory.createRaisedBevelBorder();

    //~ Instance variables ------------------------------------------------

    // Used to remember the current user desired target
    private Object target;

    // Menus & tools in the frame
    private final JMenu    fileMenu = new JMenu("File");
    private final JMenu    stepMenu = new StepMenu("Step");
    private final JMenu    toolMenu = new JMenu("Tool");
    private final JMenu    helpMenu = new JMenu("Help");
    private final JToolBar toolBar;

    /**
     * Progress bar for actions performed on sheet
     */
    final JProgressBar progressBar = new JProgressBar();

    /**
     * Score pane, which may contain several score views
     */
    public final ScorePane scorePane;

    /**
     * Sheet tabbed pane, which may contain several views
     */
    public final SheetPane sheetPane;

    /**
     * Log pane, which displays logging info
     */
    public final LogPane logPane;

    /**
     * Boards pane, which displays several boards
     */
    private JPanel boardsHolder;

    // The splitted panes
    private final JSplitPane splitPane;
    private final JSplitPane bigSplitPane;

    // Color chooser for shapes
    private JFrame shapeColorFrame;

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
        addWindowListener(new WindowAdapter()
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
        sheetPane = new SheetPane(this, toolBar);

        // Score actions
        toolBar.addSeparator();
        scorePane = new ScorePane(this, toolBar);

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
        setJMenuBar(menuBar);
        menuBar.add(fileMenu);
        menuBar.add(sheetPane.getMenu());
        menuBar.add(stepMenu);
        menuBar.add(scorePane.getMenu());
        menuBar.add(toolMenu);
        menuBar.add(Box.createHorizontalStrut(30));
        menuBar.add(helpMenu);

        /*
           +==============================================================+
           | toolKeyPanel                                                 |
           | +=============================+============================+ |
           | | toolBar                     | progressBar                | |
           | +=============================+============================+ |
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
        getContentPane().setLayout(new BorderLayout());

        logPane = new LogPane();
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                   sheetPane, logPane);
        splitPane.setBorder(null);
        splitPane.setDividerSize(2);
        splitPane.setDividerLocation(constants.logDivider.getValue());
        splitPane.setResizeWeight(1d);  // Give extra space to left part

        progressBar.setBorder(toolBorder);

        JPanel toolKeyPanel = new JPanel();
        toolKeyPanel.setLayout(new BorderLayout());
        toolKeyPanel.add(toolBar, BorderLayout.WEST);
        toolKeyPanel.add(progressBar, BorderLayout.CENTER);
        toolKeyPanel.add(new MemoryMeter(), BorderLayout.EAST);

        // Boards
        boardsHolder = new JPanel();

        bigSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                      splitPane, boardsHolder);
        bigSplitPane.setBorder(null);
        bigSplitPane.setDividerSize(2);
        bigSplitPane.setDividerLocation(constants.boardDivider.getValue());
        bigSplitPane.setResizeWeight(1d); // Give extra space to left part

        // Global layout
        getContentPane().add(toolKeyPanel, BorderLayout.NORTH);
        getContentPane().add(bigSplitPane, BorderLayout.CENTER);

        // Differ realization
        EventQueue.invokeLater(new FrameShower(this));
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

    //---------------//
    // setBoardsPane //
    //---------------//
    /**
     * Dynamically modify the content of the boardspane (according to the
     * sheet view at hand)
     *
     * @param boards the boards pane to be displayed
     */
    public void setBoardsPane (BoardsPane boards)
    {
        boardsHolder.removeAll();

        if (boards != null) {
            boardsHolder.add(boards);
        }

        // Trigger display update
        boardsHolder.invalidate();
        boardsHolder.revalidate();
        boardsHolder.repaint();
    }

    //-----------//
    // setTarget //
    //-----------//
    /**
     * Specify what the current interest of the user is, by means of the
     * current score. Thus, when for example a shhet image is loaded
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

        if (logger.isDebugEnabled()) {
            logger.debug("isTarget this=" + target +
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
        if (logger.isDebugEnabled()) {
            logger.debug("updateTitle");
        }

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

            setTitle(sb.toString());
        } else {
            // Look for score second
            omr.score.Score score = scorePane.getCurrentScore();

            if (score != null) {
                setTitle(score.getRadix() + " - " + toolInfo);
            } else {
                setTitle(toolInfo);
            }
        }
    }

    //---------------//
    // enableActions //
    //---------------//
    /**
     * Given a list of actions, set all these actions (whether they descend
     * from AbstractAction or AbstractButton) enabled or not, according to
     * the bool parameter provided.
     *
     * @param actions list of actions to enable/disable as a whole
     * @param bool    true for enable, false for disable
     */
    static void enableActions (Collection actions,
                               boolean bool)
    {
        for (Iterator it = actions.iterator(); it.hasNext();) {
            Object next = it.next();

            if (next instanceof AbstractAction) {
                ((AbstractAction) next).setEnabled(bool);
            } else if (next instanceof AbstractButton) {
                ((AbstractButton) next).setEnabled(bool);
            } else {
                logger.warning("Neither Button nor Action : " + next);
            }
        }
    }

    //-----------------//
    // setObjectTarget //
    //-----------------//
    private synchronized void setObjectTarget (Object target)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("setObjectTarget " + target);
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
        final int state = getExtendedState();
        constants.frameState.setValue(state);


        if (state == Frame.NORMAL) {
            Rectangle bounds = getBounds();
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
            super("Exit", IconUtil.buttonIconOf("general/Stop"));

            final String tiptext = "Exit the program";
            menu.add(this).setToolTipText(tiptext);

            final JButton button = toolBar.add(this);
            button.setBorder(toolBorder);
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
            super("Memory", IconUtil.buttonIconOf("general/Find"));

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
            super("Trainer", IconUtil.buttonIconOf("media/Movie"));

            final String tiptext = "Launch trainer interface";
            menu.add(this).setToolTipText(tiptext);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            GlyphTrainer glyphTrainer = GlyphTrainer.getInstance();
            glyphTrainer.setVisible(true);
            glyphTrainer.toFront();
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
            super("Options", IconUtil.buttonIconOf("general/Properties"));
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
            super("Shape Colors", IconUtil.buttonIconOf("general/Properties"));
            menu.add(this).setToolTipText("Manage colors of all shapes");
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            if (shapeColorFrame == null) {
                shapeColorFrame = new JFrame("ShapeColorChooser");

                // Create and set up the content pane.
                JComponent newContentPane = new ShapeColorChooser();
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
                  IconUtil.buttonIconOf("general/Properties"));
            menu.add(this).setToolTipText("Verify training material");
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            GlyphVerifier.getInstance().setVisible(true);
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
            super("About", IconUtil.buttonIconOf("general/About"));

            final String tiptext = "About Audiveris";
            menu.add(this).setToolTipText(tiptext);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            StringBuffer sb = new StringBuffer();
            sb
                .append("<HTML>")
                .append("<B>Audiveris</B> ")
                .append("<I>version ").append(Main.toolVersion).append("</I>")
                .append("<BR>")
                .append("Refer to <B>https://audiveris.dev.java.net</B>")
                .append("</HTML>");

            JEditorPane htmlPane = new JEditorPane("text/html",
                                                   sb.toString());
            htmlPane.setEditable(false);

            JOptionPane.showMessageDialog
                (Jui.this, htmlPane);
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
