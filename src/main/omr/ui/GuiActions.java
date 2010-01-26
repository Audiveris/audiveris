//----------------------------------------------------------------------------//
//                                                                            //
//                            G u i A c t i o n s                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.Main;
import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.constant.UnitManager;
import omr.constant.UnitModel;
import omr.constant.UnitTreeTable;

import omr.glyph.ui.GlyphVerifier;
import omr.glyph.ui.ShapeColorChooser;
import omr.glyph.ui.panel.GlyphTrainer;

import omr.log.Logger;

import omr.score.ui.ScoreDependent;

import omr.sheet.ui.SheetsController;

import omr.ui.treetable.JTreeTable;
import omr.ui.util.WebBrowser;

import omr.util.Memory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Task;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.JTextComponent;

/**
 * Class <code>GuiActions</code> gathers individual actions trigerred from the
 * main Gui interface.
 *
 * @author Herv&eacute Bitteur
 */
public class GuiActions
    extends ScoreDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GuiActions.class);

    /** Options UI */
    private static JFrame optionsFrame;

    // Resource injection
    private static ResourceMap          resource = Application.getInstance()
                                                              .getContext()
                                                              .getResourceMap(
        GuiActions.class);

    /** Singleton */
    private static GuiActions INSTANCE;

    /** Create this action just once */
    private static volatile AboutAction aboutAction;

    /** Should the errors window be displayed */
    public static final String ERRORS_DISPLAYED = "errorsDisplayed";

    //~ Methods ----------------------------------------------------------------

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

    //--------------------//
    // setErrorsDisplayed //
    //--------------------//
    public void setErrorsDisplayed (boolean value)
    {
        boolean oldValue = constants.errorsDisplayed.getValue();
        constants.errorsDisplayed.setValue(value);
        firePropertyChange(
            ERRORS_DISPLAYED,
            oldValue,
            constants.errorsDisplayed.getValue());
    }

    //-------------------//
    // isErrorsDisplayed //
    //-------------------//
    public boolean isErrorsDisplayed ()
    {
        return constants.errorsDisplayed.getValue();
    }

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

    //----------//
    // clearLog //
    //----------//
    /**
     * Action to erase the content of the log display
     * @param e the event which triggered this action
     */
    @Action
    public void clearLog (ActionEvent e)
    {
        Main.getGui()
            .clearLog();
    }

    //---------------//
    // defineOptions //
    //---------------//
    /**
     *  Action that opens a window where units options (logger level, constants)
     * can be managed
     * @param e the event that triggered this action
     * @return the SAF task
     */
    @Action
    public Task defineOptions (ActionEvent e)
    {
        return new OptionsTask();
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

    //-------------------//
    // dumpEventServices //
    //-------------------//
    /**
     * Action to erase the dump the content of all event services
     * @param e the event which triggered this action
     */
    @Action(enabledProperty = "sheetAvailable")
    public void dumpEventServices (ActionEvent e)
    {
        SheetsController.getInstance()
                        .dumpCurrentSheetServices();
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
        MainGui.getInstance()
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
        if (aboutAction == null) {
            aboutAction = new AboutAction();
        }

        aboutAction.actionPerformed(e);
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
            File   file = new File(
                WellKnowns.DOC_FOLDER,
                constants.manualUrl.getValue());
            URI    uri = file.toURI();
            URL    url = uri.toURL();
            String str = "\"" + url + "?manual=operation" + "\"";

            logger.info("Launching browser on " + str);
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

    //--------------//
    // toggleErrors //
    //--------------//
    /**
     * Action that toggles the display of errors window
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = ERRORS_DISPLAYED)
    public void toggleErrors (ActionEvent e)
    {
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
        String str = constants.webSiteUrl.getValue();

        logger.info("Launching browser on " + str);
        WebBrowser.getBrowser()
                  .launch(str);
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
    public static class AboutAction
    {
        //~ Enumerations -------------------------------------------------------

        private static enum Topic {
            //~ Enumeration constant initializers ------------------------------


            /** Application name */
            application(new JTextField()),
            /** Longer application description */
            description(new JTextField()), 
            /** Current version */
            version(new JTextField()), 
            /** Current build */
            build(new JTextField()), 
            /** Precise classes */
            classes(new JTextField()), 
            /** Link to web site */
            home(new JEditorPane("text/html", "")), 

            /** Link to project site */
            project(new JEditorPane("text/html", ""));
            //~ Instance fields ------------------------------------------------

            public final JTextComponent comp;

            //~ Constructors ---------------------------------------------------

            Topic (JTextComponent comp)
            {
                this.comp = comp;
            }
        }

        //~ Instance fields ----------------------------------------------------

        // Dialog
        private JDialog           aboutBox = null;
        private HyperlinkListener linkListener = new LinkListener();

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            if (aboutBox == null) {
                aboutBox = createAboutBox();
            }

            MainGui.getInstance()
                   .show(aboutBox);
        }

        private JDialog createAboutBox ()
        {
            StringBuilder rows = new StringBuilder("pref,5dlu");

            for (int i = 0; i < Topic.values().length; i++) {
                rows.append(",pref,3dlu");
            }

            // Layout
            final FormLayout      layout = new FormLayout(
                "right:pref, 5dlu, pref",
                rows.toString());
            final PanelBuilder    builder = new PanelBuilder(layout);
            final CellConstraints cst = new CellConstraints();

            int                   iRow = 1;
            builder.setDefaultDialogBorder();

            JLabel titleLabel = new JLabel();
            titleLabel.setName("aboutTitleLabel");
            builder.add(titleLabel, cst.xyw(1, iRow, 3));

            for (Topic topic : Topic.values()) {
                iRow += 2;

                JLabel label = new JLabel();
                label.setName(topic + "Label");
                builder.add(label, cst.xy(1, iRow));

                topic.comp.setName(topic + "TextField");
                topic.comp.setEditable(false);
                topic.comp.setBorder(null);

                if (topic.comp instanceof JEditorPane) {
                    ((JEditorPane) topic.comp).addHyperlinkListener(
                        linkListener);
                }

                builder.add(topic.comp, cst.xy(3, iRow));
            }

            JPanel panel = builder.getPanel();
            panel.setName("panel");

            JDialog dialog = new JDialog();
            dialog.setName("aboutDialog");
            dialog.add(panel, BorderLayout.CENTER);

            // Manual injection
            resource.injectComponents(dialog);
            Topic.build.comp.setText(Main.getToolBuild());
            Topic.classes.comp.setText(WellKnowns.CLASS_CONTAINER.toString());

            return dialog;
        }

        //~ Inner Classes ------------------------------------------------------

        //            sb.append("<A HREF=\"http://kenai.com/projects/audiveris\">http://kenai.com/projects/audiveris</A>");
        //        htmlPane.addHyperlinkListener(new ActivatedHyperlinkListener());
        //
        private static class LinkListener
            implements HyperlinkListener
        {
            //~ Methods --------------------------------------------------------

            public void hyperlinkUpdate (HyperlinkEvent event)
            {
                HyperlinkEvent.EventType type = event.getEventType();
                final URL                url = event.getURL();

                if (type == HyperlinkEvent.EventType.ACTIVATED) {
                    //System.out.println("Activated URL " + url);
                    WebBrowser.getBrowser()
                              .launch(url.toString());
                }
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.String        webSiteUrl = new Constant.String(
            "http://kenai.com/projects/audiveris",
            "URL of Audiveris home page");
        Constant.String        manualUrl = new Constant.String(
            "/docs/manual/index.html",
            "URL of local Audiveris manual");

        /** Should the errors window be displayed */
        final Constant.Boolean errorsDisplayed = new Constant.Boolean(
            true,
            "Should the errors window be displayed");
    }

    //-------------//
    // OptionsTask //
    //-------------//
    private static class OptionsTask
        extends Task<JFrame, Void>
    {
        //~ Static fields/initializers -----------------------------------------

        private static final AbstractAction dumping = new AbstractAction() {
            public void actionPerformed (ActionEvent e)
            {
                UnitManager.getInstance()
                           .dumpAllUnits();
            }
        };

        private static final AbstractAction checking = new AbstractAction() {
            public void actionPerformed (ActionEvent e)
            {
                UnitManager.getInstance()
                           .checkAllUnits();
            }
        };


        //~ Constructors -------------------------------------------------------

        public OptionsTask ()
        {
            super(MainGui.getInstance());
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected JFrame doInBackground ()
            throws Exception
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

                JButton dumpButton = new JButton(dumping);
                dumpButton.setName("optionsDumpButton");
                toolBar.add(dumpButton);

                JButton checkButton = new JButton(checking);
                checkButton.setName("optionsCheckButton");
                toolBar.add(checkButton);

                UnitModel  cm = new UnitModel();
                JTreeTable jtt = new UnitTreeTable(cm);
                optionsFrame.getContentPane()
                            .add(new JScrollPane(jtt));

                // Resources injection
                resource.injectComponents(optionsFrame);
            }

            return optionsFrame;
        }

        @Override
        protected void succeeded (JFrame frame)
        {
            if (frame != null) {
                MainGui.getInstance()
                       .show(frame);
            }
        }
    }
}
