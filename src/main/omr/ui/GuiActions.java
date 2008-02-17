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

import omr.constant.Constant;
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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

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

    /** Options UI */
    private static JFrame optionsFrame;

    // Resource injection
    private static ResourceMap resource = Application.getInstance()
                                                     .getContext()
                                                     .getResourceMap(
        GuiActions.class);

    //~ Inner Classes ----------------------------------------------------------

    //-------------//
    // AboutAction //
    //-------------//
    /**
     * Class <code>AboutAction</code> opens an 'About' dialog with some
     * information about the application.
     *
     */
    @Plugin(type = PluginType.HELP, dependency = Dependency.NONE)
    public static class AboutAction
        extends AbstractAction
    {
        //~ Instance fields ----------------------------------------------------

        // Dialog        
        private JDialog                 aboutBox = null;
        private Map<String, JTextField> fields = new HashMap<String, JTextField>();

        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            if (aboutBox == null) {
                aboutBox = createAboutBox();
            }

            Main.getInstance()
                .show(aboutBox);
        }

        private JDialog createAboutBox ()
        {
            // Layout
            final FormLayout      layout = new FormLayout(
                "right:pref, 5dlu, pref",
                "pref,5dlu,pref,2dlu,pref,2dlu,pref,2dlu,pref,2dlu,pref,2dlu, pref");
            final PanelBuilder    builder = new PanelBuilder(layout);
            final CellConstraints cst = new CellConstraints();

            int                   iRow = 1;
            builder.setDefaultDialogBorder();

            JLabel titleLabel = new JLabel();
            titleLabel.setName("aboutTitleLabel");
            builder.add(titleLabel, cst.xyw(1, iRow, 3));

            for (String name : new String[] {
                     "application", "description", "home", "version", "build",
                     "classes"
                 }) {
                iRow += 2;

                JLabel label = new JLabel();
                label.setName(name + "Label");
                builder.add(label, cst.xy(1, iRow));

                JTextField textField = new JTextField();
                textField.setName(name + "TextField");
                textField.setEditable(false);
                textField.setBorder(null);
                builder.add(textField, cst.xy(3, iRow));
                fields.put(name, textField);
            }

            JPanel panel = builder.getPanel();
            panel.setName("panel");

            JDialog dialog = new JDialog();
            dialog.setName("aboutDialog");
            dialog.add(panel, BorderLayout.CENTER);

            // Manual injection
            resource.injectComponents(dialog);
            fields.get("build")
                  .setText(Main.getToolBuild());
            fields.get("classes")
                  .setText(Main.getClassesContainer().toString());

            return dialog;
        }
    }

    //----------------//
    // ClearLogAction //
    //----------------//
    /**
     * Class <code>ClearLogAction</code> erases the content of the log display
     * (but not the content of the log itself)
     */
    @Plugin(type = PluginType.LOG_VIEW, onToolbar = true)
    public static class ClearLogAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

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
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Main.getInstance()
                .exit();
        }
    }

    //------------//
    // FineAction //
    //------------//
    /**
     * Class <code>FineAction</code> allows to set logger level to FINE in the
     * Selection mechanism
     *
     */
    @Plugin(type = PluginType.TEST, onToolbar = true)
    public static class FineAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

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
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            logger.info("Occupied memory is " + Memory.getValue() + " bytes");
        }
    }

    //-----------------//
    // OperationAction //
    //-----------------//
    /**
     * Class <code>OperationAction</code> launches a browser on Audiveris
     * Operation manual
     */
    @Plugin(type = PluginType.HELP, dependency = Dependency.NONE)
    public static class OperationAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public OperationAction ()
        {
            setEnabled(WebBrowser.getBrowser().isSupported());
        }

        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            WebBrowser.getBrowser()
                      .launch(constants.operationUrl.getValue());
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
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            if (optionsFrame == null) {
                // Preload constant units
                UnitManager.getInstance(Main.class.getName());

                optionsFrame = new JFrame();
                optionsFrame.setName("optionsFrame");
                optionsFrame.getContentPane()
                            .setLayout(new BorderLayout());

                JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
                optionsFrame.getContentPane()
                            .add(toolBar, BorderLayout.NORTH);

                JButton dumpButton = new JButton(
                    new AbstractAction() {
                            public void actionPerformed (ActionEvent e)
                            {
                                UnitManager.getInstance()
                                           .dumpAllUnits();
                            }
                        });
                dumpButton.setName("optionsDumpButton");
                toolBar.add(dumpButton);

                JButton checkButton = new JButton(
                    new AbstractAction() {
                            public void actionPerformed (ActionEvent e)
                            {
                                UnitManager.getInstance()
                                           .checkAllUnits();
                            }
                        });
                checkButton.setName("optionsCheckButton");
                toolBar.add(checkButton);

                UnitModel  cm = new UnitModel();
                JTreeTable jtt = new UnitTreeTable(cm);
                optionsFrame.getContentPane()
                            .add(new JScrollPane(jtt));

                // Resources injection
                resource.injectComponents(optionsFrame);
            }

            Main.getInstance()
                .show(optionsFrame);
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
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            ShapeColorChooser.showFrame();
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
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            UITest.test();
        }
    }

    //---------------//
    // WebSiteAction //
    //---------------//
    /**
     * Class <code>WebSiteAction</code> launches a browser on Audiveris website
     */
    @Plugin(type = PluginType.HELP, dependency = Dependency.NONE)
    public static class WebSiteAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public WebSiteAction ()
        {
            setEnabled(WebBrowser.getBrowser().isSupported());
        }

        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            WebBrowser.getBrowser()
                      .launch(constants.webSiteUrl.getValue());
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.String webSiteUrl = new Constant.String(
            "https://audiveris.dev.java.net",
            "URL of Audiveris home page");
        Constant.String operationUrl = new Constant.String(
            "https://audiveris.dev.java.net/nonav/docs/manual/index.html?manual=operation",
            "URL of Audiveris operation manual");
    }
}
