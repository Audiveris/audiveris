//----------------------------------------------------------------------------//
//                                                                            //
//                            G u i A c t i o n s                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.ui;

import omr.Main;

import omr.constant.ConstantSet;
import omr.constant.UnitManager;
import omr.constant.UnitModel;
import omr.constant.UnitTreeTable;

import omr.glyph.ui.ShapeColorChooser;

import omr.plugin.Dependency;
import omr.plugin.Plugin;
import omr.plugin.PluginType;

import omr.ui.treetable.JTreeTable;

import omr.util.Implement;
import omr.util.Logger;
import omr.util.Memory;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

/**
 * Class <code>GuiActions</code> gathers individual actions trigerred from the
 * main Gui interface.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class GuiActions
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GuiActions.class);

    /** Color chooser for shapes */
    private static JFrame shapeColorFrame;

    /** Options UI */
    private static JFrame optionsFrame;

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // displayMessage //
    //----------------//
    /**
     * Allow to display a modal dialog with an html content
     *
     * @param htmlStr the HTML string
     */
    public static void displayMessage (String htmlStr)
    {
        JEditorPane htmlPane = new JEditorPane("text/html", htmlStr);
        htmlPane.setEditable(false);
        JOptionPane.showMessageDialog(Main.getGui().getFrame(), htmlPane);
    }

    //----------------//
    // displayWarning //
    //----------------//
    /**
     * Allow to display a modal dialog with an html content
     *
     * @param htmlStr the HTML string
     */
    public static void displayWarning (String htmlStr)
    {
        JEditorPane htmlPane = new JEditorPane("text/html", htmlStr);
        htmlPane.setEditable(false);

        JOptionPane.showMessageDialog(
            Main.getGui().getFrame(),
            htmlPane,
            "Warning",
            JOptionPane.WARNING_MESSAGE);
    }

    //-------------//
    // addTableRow //
    //-------------//
    private static void addTableRow (StringBuilder sb,
                                     String        name,
                                     Object        value)
    {
        sb.append("<TR><TH>")
          .append(name)
          .append("<TH><TD>")
          .append(value)
          .append("</TD></TR>");
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------------//
    // AboutAction //
    //-------------//
    /**
     * Class <code>AboutAction</code> opens an 'About' dialog with some
     * information about the application.
     *
     */
    @Plugin(type = PluginType.HELP, dependency = Dependency.NONE, onToolbar = true)
    public static class AboutAction
        extends AbstractAction
    {
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("<HTML><TABLE BORDER='0'>");

            // Application information
            addTableRow(sb, "Application", Main.getToolName());

            // Version information
            addTableRow(sb, "Version", Main.getToolVersion());

            // Build information, if available
            addTableRow(
                sb,
                "Build",
                (Main.getToolBuild() != null) ? Main.getToolBuild() : "");

            // Launch information
            addTableRow(sb, "Classes", Main.getClassesContainer());

            // Web site
            addTableRow(sb, "WebSite", "https://audiveris.dev.java.net");

            sb.append("</TABLE></HTML>");

            displayMessage(sb.toString());
        }
    }

    //----------------//
    // ClearLogAction //
    //----------------//
    /**
     * Class <code>ClearLogAction</code> erases the content of the log display
     * (but not the content of the log itself)
     *
     */
    @Plugin(type = PluginType.LOG_VIEW, onToolbar = true)
    public static class ClearLogAction
        extends AbstractAction
    {
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Main.getGui().logPane.clearLog();
        }
    }

    //------------//
    // ExitAction //
    //------------//
    /**
     * Class <code>ExitAction</code> allows to exit the application
     *
     */
    @Plugin(type = PluginType.GENERAL_END, dependency = Dependency.NONE, onToolbar = false)
    public static class ExitAction
        extends AbstractAction
    {
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Main.getGui()
                .exit();
        }
    }

    //------------//
    // FineAction //
    //------------//
    /**
     * Class <code>FineAction</code> allows to set looger level to FINE in the
     * Selection mechanism
     *
     */
    @Plugin(type = PluginType.TEST, onToolbar = true)
    public static class FineAction
        extends AbstractAction
    {
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
    /**
     * Class <code>MemoryAction</code> desplays the current value of occupied
     * memory
     *
     */
    @Plugin(type = PluginType.TOOL)
    public static class MemoryAction
        extends AbstractAction
    {
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            logger.info("Occupied memory is " + Memory.getValue() + " bytes");
        }
    }

    //---------------//
    // OptionsAction //
    //---------------//
    /**
     * Class <code>OptionsAction</code> opens a window where units options
     * (logger level, constants) can be managed
     *
     */
    @Plugin(type = PluginType.TOOL)
    public static class OptionsAction
        extends AbstractAction
    {
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            if (optionsFrame == null) {
                // Preload constant units
                UnitManager.getInstance(Main.class.getName());

                optionsFrame = new JFrame("Units Options");
                optionsFrame.getContentPane()
                            .setLayout(new BorderLayout());

                JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
                optionsFrame.getContentPane()
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
                optionsFrame.getContentPane()
                            .add(new JScrollPane(jtt));

                optionsFrame.pack();
                optionsFrame.setSize(
                    constants.paramWidth.getValue(),
                    constants.paramHeight.getValue());
            }

            optionsFrame.setVisible(true);
        }
    }

    //------------------//
    // ShapeColorAction //
    //------------------//
    /**
     * Class <code>ShapeColorAction</code> allows to define the colors of
     * predefined shapes
     *
     */
    @Plugin(type = PluginType.TOOL)
    public static class ShapeColorAction
        extends AbstractAction
    {
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

            shapeColorFrame.setVisible(true);
        }
    }

    //------------//
    // TestAction //
    //------------//
    /**
     * Class <code>TestAction</code> triggers a generic test methody
     *
     */
    @Plugin(type = PluginType.TEST, onToolbar = true)
    public static class TestAction
        extends AbstractAction
    {
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            UITest.test();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        PixelCount paramHeight = new PixelCount(
            500,
            "Height of the param frame");
        PixelCount paramWidth = new PixelCount(900, "Width of the param frame");
    }
}
