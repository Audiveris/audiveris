//----------------------------------------------------------------------------//
//                                                                            //
//                            G u i A c t i o n s                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.Main;
import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.ui.SampleVerifier;
import omr.glyph.ui.ShapeColorChooser;
import omr.glyph.ui.panel.GlyphTrainer;

import omr.score.ui.ScoreDependent;

import omr.sheet.ui.SheetsController;

import omr.ui.symbol.SymbolRipper;
import omr.ui.util.WebBrowser;

import omr.util.Memory;
import omr.util.UriUtil;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.JTextComponent;

/**
 * Class {@code GuiActions} gathers individual actions triggered from
 * the main Gui interface.
 *
 * @author Hervé Bitteur
 */
public class GuiActions
        extends ScoreDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            GuiActions.class);

    /** Options UI */
    private static Options options;

    // Resource injection
    private static ResourceMap resource = Application.getInstance()
            .getContext()
            .getResourceMap(
            GuiActions.class);

    /** Singleton */
    private static GuiActions INSTANCE;

    /** Create this action just once */
    private static volatile AboutAction aboutAction;

    /** Should the errors window be displayed */
    public static final String ERRORS_DISPLAYED = "errorsDisplayed";

    /** Should the log window be displayed */
    public static final String LOG_DISPLAYED = "logDisplayed";

    /** Should the boards window be displayed */
    public static final String BOARDS_DISPLAYED = "boardsDisplayed";

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

    //----------//
    // clearLog //
    //----------//
    /**
     * Action to erase the content of the log display
     *
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
     * Action that opens a window where units options (logger level,
     * constants) can be managed.
     *
     * @param e the event that triggered this action
     * @return the SAF task
     */
    @Action
    public Task<Options, Void> defineOptions (ActionEvent e)
    {
        return new OptionsTask();
    }

    //-------------------//
    // defineShapeColors //
    //-------------------//
    /**
     * Action that allows to define the colors of predefined shapes
     *
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
     *
     * @param e the event which triggered this action
     */
    @Action(enabledProperty = SHEET_AVAILABLE)
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
     *
     * @param e the event which triggered this action
     */
    @Action
    public void exit (ActionEvent e)
    {
        MainGui.getInstance()
                .exit();
    }

    //-------------------//
    // isBoardsDisplayed //
    //-------------------//
    public boolean isBoardsDisplayed ()
    {
        return constants.boardsDisplayed.getValue();
    }

    //--------------------//
    // isBrowserSupported //
    //--------------------//
    /**
     * Report whether the underlying platform can launch a browser
     *
     * @return true if it can
     */
    public boolean isBrowserSupported ()
    {
        return WebBrowser.getBrowser()
                .isSupported();
    }

    //-------------------//
    // isErrorsDisplayed //
    //-------------------//
    public boolean isErrorsDisplayed ()
    {
        return constants.errorsDisplayed.getValue();
    }

    //----------------//
    // isLogDisplayed //
    //----------------//
    public boolean isLogDisplayed ()
    {
        return constants.logDisplayed.getValue();
    }

    //--------------------//
    // launchSymbolRipper //
    //--------------------//
    /**
     * Launch the utility to rip a symbol
     */
    @Action
    public void launchSymbolRipper ()
    {
        SymbolRipper.main();
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

    //--------------------//
    // setBoardsDisplayed //
    //--------------------//
    public void setBoardsDisplayed (boolean value)
    {
        boolean oldValue = constants.boardsDisplayed.getValue();
        constants.boardsDisplayed.setValue(value);
        firePropertyChange(BOARDS_DISPLAYED, oldValue, value);
    }

    //--------------------//
    // setErrorsDisplayed //
    //--------------------//
    public void setErrorsDisplayed (boolean value)
    {
        boolean oldValue = constants.errorsDisplayed.getValue();
        constants.errorsDisplayed.setValue(value);
        firePropertyChange(ERRORS_DISPLAYED, oldValue, value);
    }

    //-----------------//
    // setLogDisplayed //
    //-----------------//
    public void setLogDisplayed (boolean value)
    {
        boolean oldValue = constants.logDisplayed.getValue();
        constants.logDisplayed.setValue(value);
        firePropertyChange(LOG_DISPLAYED, oldValue, value);
    }

    //-----------//
    // showAbout //
    //-----------//
    /**
     * Show the 'about' data
     *
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
     * Action to launch a browser on (local) Audiveris handbook
     *
     * @param e the event which triggered this action
     */
    @Action(enabledProperty = "browserSupported")
    public void showManual (ActionEvent e)
    {
        File file = new File(
                WellKnowns.DOC_FOLDER,
                constants.manualUrl.getValue());

        if (!file.exists()) {
            logger.warn("Cannot find file {}", file);
        } else {
            URI uri = file.toURI();
            WebBrowser.getBrowser()
                    .launch(uri);
        }
    }

    //------------//
    // showMemory //
    //------------//
    /**
     * Action to desplay the current value of occupied memory
     *
     * @param e the event that triggered this action
     */
    @Action
    public void showMemory (ActionEvent e)
    {
        logger.info("Occupied memory is {} bytes", Memory.getValue());
    }

    //--------------//
    // toggleBoards //
    //--------------//
    /**
     * Action that toggles the display of baords window
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = BOARDS_DISPLAYED)
    public void toggleBoards (ActionEvent e)
    {
    }

    //--------------//
    // toggleErrors //
    //--------------//
    /**
     * Action that toggles the display of errors window
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = ERRORS_DISPLAYED)
    public void toggleErrors (ActionEvent e)
    {
    }

    //-----------//
    // toggleLog //
    //-----------//
    /**
     * Action that toggles the display of log window
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = LOG_DISPLAYED)
    public void toggleLog (ActionEvent e)
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
        SampleVerifier.getInstance()
                .setVisible(true);
    }

    //--------------//
    // visitWebSite //
    //--------------//
    /**
     * Action to launch a browser on application web site
     *
     * @param e the event which triggered this action
     */
    @Action(enabledProperty = "browserSupported")
    public void visitWebSite (ActionEvent e)
    {
        String str = constants.webSiteUrl.getValue();

        try {
            URI uri = new URI(str);
            WebBrowser.getBrowser()
                    .launch(uri);
        } catch (URISyntaxException ex) {
            logger.warn("Illegal site uri " + str, ex);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-------------//
    // AboutAction //
    //-------------//
    /**
     * Class {@code AboutAction} opens an 'About' dialog with some
     * information about the application.
     *
     */
    public static class AboutAction
    {
        //~ Enumerations -------------------------------------------------------

        private static enum Topic
        {
            //~ Enumeration constant initializers ------------------------------

            /** Longer application description */
            description(new JTextField()),
            /** Current version */
            version(new JTextField()),
            /** Precise classes */
            classes(new JTextField()),
            /** Link to web site */
            home(new JEditorPane("text/html", "")),
            /** Link to project site */
            project(new JEditorPane("text/html", "")),
            /** License */
            license(new JTextField()),
            /** Java vendor */
            javaVendor(new JTextField()),
            /** Java version */
            javaVersion(new JTextField()),
            /** Java runtime */
            javaRuntime(new JTextField()),
            /** Java VM */
            javaVm(new JTextField()),
            /** OS */
            os(new JTextField()),
            /** Arch */
            osArch(new JTextField());
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
        private JDialog aboutBox = null;

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
            StringBuilder rows = new StringBuilder("pref,10dlu,pref,5dlu");

            for (int i = 0; i < (Topic.values().length); i++) {
                rows.append(",pref,3dlu");
            }

            // Layout
            final FormLayout layout = new FormLayout(
                    "right:pref, 5dlu, pref, 200dlu",
                    rows.toString());
            final PanelBuilder builder = new PanelBuilder(layout);
            final CellConstraints cst = new CellConstraints();

            builder.setDefaultDialogBorder();

            int iRow = 1;

            URI uri = UriUtil.toURI(WellKnowns.RES_URI, "splash.png");

            try {
                JPanel logoPanel = new ImagePanel(uri);
                builder.add(logoPanel, cst.xyw(1, iRow, 4));
            } catch (MalformedURLException ex) {
                logger.warn("Error on " + uri, ex);
            }

            iRow += 2;

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
                topic.comp.setBackground(Color.WHITE);

                if (topic.comp instanceof JEditorPane) {
                    ((JEditorPane) topic.comp).addHyperlinkListener(
                            linkListener);
                }

                builder.add(topic.comp, cst.xy(3, iRow));
            }

            JPanel panel = builder.getPanel();
            panel.setOpaque(true);
            panel.setBackground(Color.WHITE);
            panel.setName("panel");

            JDialog dialog = new JDialog();
            dialog.setName("aboutDialog");
            dialog.add(panel, BorderLayout.CENTER);

            // Manual injection
            resource.injectComponents(dialog);
            Topic.version.comp.setText(
                    WellKnowns.TOOL_REF + ":" + WellKnowns.TOOL_BUILD);
            Topic.classes.comp.setText(WellKnowns.CLASS_CONTAINER.toString());
            Topic.license.comp.setText("GNU GPL V2");

            Topic.javaVendor.comp.setText(System.getProperty("java.vendor"));
            Topic.javaVersion.comp.setText(System.getProperty("java.version"));
            Topic.javaRuntime.comp.setText(
                    System.getProperty("java.runtime.name") + " (build "
                    + System.getProperty("java.runtime.version") + ")");
            Topic.javaVm.comp.setText(
                    System.getProperty("java.vm.name") + " (build "
                    + System.getProperty("java.vm.version") + ", "
                    + System.getProperty("java.vm.info") + ")");
            Topic.os.comp.setText(
                    System.getProperty("os.name") + " "
                    + System.getProperty("os.version"));
            Topic.osArch.comp.setText(System.getProperty("os.arch"));

            return dialog;
        }

        //~ Inner Classes ------------------------------------------------------
        //------------//
        // ImagePanel //
        //------------//
        private static class ImagePanel
                extends JPanel
        {
            //~ Instance fields ------------------------------------------------

            private Image img;

            //~ Constructors ---------------------------------------------------
            public ImagePanel (Image img)
            {
                this.img = img;

                Dimension size = new Dimension(
                        img.getWidth(null),
                        img.getHeight(null));
                setPreferredSize(size);
                setMinimumSize(size);
                setMaximumSize(size);
                setSize(size);
                setLayout(null);
            }

            public ImagePanel (URI uri)
                    throws MalformedURLException
            {
                this(new ImageIcon(uri.toURL()).getImage());
            }

            //~ Methods --------------------------------------------------------
            @Override
            public void paintComponent (Graphics g)
            {
                g.drawImage(img, 0, 0, null);
            }
        }

        private static class LinkListener
                implements HyperlinkListener
        {
            //~ Methods --------------------------------------------------------

            @Override
            public void hyperlinkUpdate (HyperlinkEvent event)
            {
                HyperlinkEvent.EventType type = event.getEventType();
                final URL url = event.getURL();

                if (type == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        //System.out.println("Activated URL " + url);
                        URI uri = new URI(url.toString());
                        WebBrowser.getBrowser()
                                .launch(uri);
                    } catch (URISyntaxException ex) {
                        logger.warn("Illegal URI " + url, ex);
                    }
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

        Constant.String webSiteUrl = new Constant.String(
                "http://www.audiveris.org",
                "URL of Audiveris home page");

        Constant.String manualUrl = new Constant.String(
                "docs/manual/handbook.html",
                "URL of local Audiveris manual");

        final Constant.Boolean boardsDisplayed = new Constant.Boolean(
                true,
                "Should the boards window be displayed");

        final Constant.Boolean logDisplayed = new Constant.Boolean(
                true,
                "Should the log window be displayed");

        final Constant.Boolean errorsDisplayed = new Constant.Boolean(
                true,
                "Should the errors window be displayed");

    }

    //-------------//
    // OptionsTask //
    //-------------//
    private static class OptionsTask
            extends Task<Options, Void>
    {
        //~ Constructors -------------------------------------------------------

        public OptionsTask ()
        {
            super(MainGui.getInstance());
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected Options doInBackground ()
                throws Exception
        {
            if (options == null) {
                options = new Options();
            }

            return options;
        }

        @Override
        protected void succeeded (Options options)
        {
            if (options != null) {
                MainGui.getInstance()
                        .show(options.getComponent());
            }
        }
    }
}
