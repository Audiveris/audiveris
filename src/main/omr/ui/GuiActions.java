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

import omr.glyph.ui.GlyphTrainer;
import omr.glyph.ui.GlyphVerifier;
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

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
    extends AbstractBean
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

    /** Singleton */
    private static GuiActions INSTANCE;

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the singleton
     *
     * @return the unique instance of this class
     */
    public static synchronized GuiActions getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new GuiActions();
        }

        return INSTANCE;
    }

    //--------------------//
    // isBrowserSupported //
    //--------------------//
    /**
     * Report whether the underlying platform can launch a browser
     * @return true if it can
     */
    public boolean isBrowserSupported ()
    {
        return WebBrowser.getBrowser()
                         .isSupported();
    }

    //----------//
    // clearLog //
    //----------//
    /**
     * Action to erase the content of the log display
     * (but not the content of the log itself)
     * @param e the event which triggered this action
     */
    @Action
    public void clearLog (ActionEvent e)
    {
        Main.getGui().logPane.clearLog();
    }

    //---------------//
    // defineOptions //
    //---------------//
    /**
     *  Action that opens a window where units options (logger level, constants)
     * can be managed
     * @param e the event that triggered this action
     */
    @Action
    public void defineOptions (ActionEvent e)
    {
        if (optionsFrame == null) {
            // Preload constant units
            UnitManager.getInstance()
                       .preLoadUnits(Main.class.getName());

            optionsFrame = new JFrame();
            optionsFrame.setName("optionsFrame");
            optionsFrame.setDefaultCloseOperation(
                WindowConstants.DISPOSE_ON_CLOSE);
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

    //-------------------//
    // defineShapeColors //
    //-------------------//
    /**
     * Action that  allows to define the colors of predefined shapes
     * @param e the event which triggered this action
     */
    @Action
    public void defineShapeColors (ActionEvent e)
    {
        ShapeColorChooser.showFrame();
    }

    //------//
    // exit //
    //------//
    /**
     * Action to exit the application
     * @param e the event which triggered this action
     */
    @Action
    public void exit (ActionEvent e)
    {
        Main.getInstance()
            .exit();
    }

    //---------------//
    // launchTrainer //
    //---------------//
    /**
     * Action that launches the window dedicated to the training of the neural
     * network
     *
     * @param e the event which triggered this action
     */
    @Action
    public void launchTrainer (ActionEvent e)
    {
        GlyphTrainer.launch();
    }

    //-----------//
    // showAbout //
    //-----------//
    /**
     * Show the 'about' data
     * @param e the event which triggered this action
     */
    @Action
    public void showAbout (ActionEvent e)
    {
        new AboutAction().actionPerformed(e);
    }

    //------------//
    // showManual //
    //------------//
    /**
     * Action to launch a browser on Audiveris Operation manual
     * @param e the event which triggered this action
     */
    @Action(enabledProperty = "browserSupported")
    public void showManual (ActionEvent e)
    {
        try {
            File file = new File(
                Main.getDocumentationFolder(),
                constants.manualUrl.getValue());
            logger.info("file=" + file);

            URI uri = file.toURI();
            logger.info("uri=" + uri);

            URL url = uri.toURL();
            logger.info("url=" + url);

            String str = url + "?manual=operation";
            ///str = "file:///u:/soft/audiveris/www/docs/manual/index.html?manual=operation";
            ///str = "file:///u:/soft/audiveris";
            str = "u:/soft/audiveris/www/docs/manual/index.html"; // BOF !!!
            logger.info("str=" + str);

            WebBrowser.getBrowser()
                      .launch(str);
        } catch (MalformedURLException ex) {
            logger.warning("Error in documentation URL", ex);
        }
    }

    //------------//
    // showMemory //
    //------------//
    /**
     * Action to desplay the current value of occupied  memory
     * @param e the event that triggered this action
     */
    @Action
    public void showMemory (ActionEvent e)
    {
        logger.info("Occupied memory is " + Memory.getValue() + " bytes");
    }

    //------------------------//
    // verifyTrainingMaterial //
    //------------------------//
    /**
     * Action that opens a windows dedicated to the management of collections
     * of glyphs used as training material for the neural network
     *
     * @param e the event which triggered this action
     */
    @Action
    public void verifyTrainingMaterial (ActionEvent e)
    {
        GlyphVerifier.getInstance()
                     .setVisible(true);
    }

    //--------------//
    // visitWebSite //
    //--------------//
    /**
     * Action to launch a browser on application web site
     * @param e the event which triggered this action
     */
    @Action(enabledProperty = "browserSupported")
    public void visitWebSite (ActionEvent e)
    {
        WebBrowser.getBrowser()
                  .launch(constants.webSiteUrl.getValue());
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
    @Deprecated
    @Plugin(type = PluginType.LOG_VIEW, onToolbar = true)
    public static class ClearLogAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .clearLog(e);
        }
    }

    //------------//
    // ExitAction //
    //------------//
    @Deprecated
    @Plugin(type = PluginType.SHEET_END)
    public static class ExitAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .exit(e);
        }
    }

    //--------------//
    // MemoryAction //
    //--------------//
    @Deprecated
    @Plugin(type = PluginType.TOOL)
    public static class MemoryAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .showMemory(e);
        }
    }

    //-----------------//
    // OperationAction //
    //-----------------//
    @Deprecated
    @Plugin(type = PluginType.HELP, dependency = Dependency.NONE)
    public static class OperationAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public OperationAction ()
        {
            setEnabled(getInstance().isBrowserSupported());
        }

        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .showManual(e);
        }
    }

    //---------------//
    // OptionsAction //
    //---------------//
    @Deprecated
    @Plugin(type = PluginType.TOOL)
    public static class OptionsAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            GuiActions.getInstance()
                      .defineOptions(e);
        }
    }

    //------------------//
    // ShapeColorAction //
    //------------------//
    @Deprecated
    @Plugin(type = PluginType.TOOL)
    public static class ShapeColorAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .defineShapeColors(e);
        }
    }

    //---------------//
    // WebSiteAction //
    //---------------//
    @Deprecated
    @Plugin(type = PluginType.HELP, dependency = Dependency.NONE)
    public static class WebSiteAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public WebSiteAction ()
        {
            setEnabled(getInstance().isBrowserSupported());
        }

        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            getInstance()
                .visitWebSite(e);
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
        Constant.String manualUrl = new Constant.String(
            "/docs/manual/index.html",
            "URL of local Audiveris manual");
    }
}
