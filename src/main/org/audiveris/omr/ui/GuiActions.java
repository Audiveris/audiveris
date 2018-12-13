//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      G u i A c t i o n s                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.OMR;
import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.classifier.ui.SampleBrowser;
import org.audiveris.omr.classifier.ui.Trainer;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.ui.ShapeColorChooser;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.text.tesseract.TesseractOCR;
import org.audiveris.omr.ui.action.AdvancedTopics;
import org.audiveris.omr.ui.symbol.SymbolRipper;
import org.audiveris.omr.ui.util.CursorController;
import org.audiveris.omr.ui.util.OmrFileFilter;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.ui.util.WebBrowser;
import org.audiveris.omr.util.Memory;
import org.audiveris.omr.util.UriUtil;

import org.jdesktop.application.AbstractBean;
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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

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
 * Class {@code GuiActions} gathers general actions triggered from the main GUI.
 *
 * @author Hervé Bitteur
 */
public class GuiActions
        extends AbstractBean
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(GuiActions.class);

    /** Should the errors window be displayed. */
    public static final String ERRORS_WINDOW_DISPLAYED = "errorsWindowDisplayed";

    /** Should the log window be displayed. */
    public static final String LOG_WINDOW_DISPLAYED = "logWindowDisplayed";

    /** Should the boards window be displayed. */
    public static final String BOARDS_WINDOW_DISPLAYED = "boardsWindowDisplayed";

    /** Options UI */
    private static Options options;

    // Resource injection
    private static ResourceMap resource = Application.getInstance().getContext().getResourceMap(
            GuiActions.class);

    /** Create this action just once */
    private static volatile AboutAction aboutAction;

    //---------------------//
    // browseGlobalSamples //
    //---------------------//
    /**
     * Launch browser on the global repository.
     *
     * @param e the event which triggered this action
     */
    @Action
    public void browseGlobalSamples (ActionEvent e)
    {
        CursorController.launchWithDelayedMessage(
                "Launching global sample browser...",
                new Runnable()
        {
            @Override
            public void run ()
            {
                try {
                    SampleBrowser.getInstance().setVisible();
                } catch (Throwable ex) {
                    logger.warn("Could not launch samples verifier. " + ex, ex);
                }
            }
        });
    }

    //--------------------//
    // browseLocalSamples //
    //--------------------//
    /**
     * Launch browser on a local sample repository.
     *
     * @param e the event which triggered this action
     */
    @Action
    public void browseLocalSamples (ActionEvent e)
    {
        // Select local samples repository
        final String ext = SampleRepository.SAMPLES_FILE_NAME;
        final Path repoPath = UIUtil.pathChooser(
                false,
                OMR.gui.getFrame(),
                BookManager.getBaseFolder(),
                new OmrFileFilter(ext, new String[]{ext}));

        if (repoPath != null) {
            CursorController.launchWithDelayedMessage(
                    "Launching local sample browser...",
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    try {
                        new SampleBrowser(SampleRepository.getInstance(repoPath, true))
                                .setVisible();
                    } catch (Throwable ex) {
                        logger.warn("Could not launch samples browser. " + ex, ex);
                    }
                }
            });
        }
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
        OMR.gui.clearLog();
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

    //--------------//
    // defineTopics //
    //--------------//
    /**
     * Action that opens the dialog where topics can be enabled/disabled.
     *
     * @param e the event that triggered this action
     */
    @Action
    public void defineTopics (ActionEvent e)
    {
        OmrGui.getApplication().show(AdvancedTopics.getComponent());
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
        OmrGui.getApplication().exit();
    }

    //-------------------------//
    // isBoardsWindowDisplayed //
    //-------------------------//
    public boolean isBoardsWindowDisplayed ()
    {
        return constants.boardsWindowDisplayed.getValue();
    }

    //--------------------------//
    // setBoardsWindowDisplayed //
    //--------------------------//
    public void setBoardsWindowDisplayed (boolean value)
    {
        boolean oldValue = constants.boardsWindowDisplayed.getValue();
        constants.boardsWindowDisplayed.setValue(value);
        firePropertyChange(BOARDS_WINDOW_DISPLAYED, oldValue, value);
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
        return WebBrowser.getBrowser().isSupported();
    }

    //-------------------------//
    // isErrorsWindowDisplayed //
    //-------------------------//
    public boolean isErrorsWindowDisplayed ()
    {
        return constants.errorsWindowDisplayed.getValue();
    }

    //--------------------------//
    // setErrorsWindowDisplayed //
    //--------------------------//
    public void setErrorsWindowDisplayed (boolean value)
    {
        boolean oldValue = constants.errorsWindowDisplayed.getValue();
        constants.errorsWindowDisplayed.setValue(value);
        firePropertyChange(ERRORS_WINDOW_DISPLAYED, oldValue, value);
    }

    //----------------------//
    // isLogWindowDisplayed //
    //----------------------//
    public boolean isLogWindowDisplayed ()
    {
        return constants.logWindowDisplayed.getValue();
    }

    //-----------------------//
    // setLogWindowDisplayed //
    //-----------------------//
    public void setLogWindowDisplayed (boolean value)
    {
        boolean oldValue = constants.logWindowDisplayed.getValue();
        constants.logWindowDisplayed.setValue(value);
        firePropertyChange(LOG_WINDOW_DISPLAYED, oldValue, value);
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
     * Action that launches the window dedicated to the training of the neural network
     *
     * @param e the event which triggered this action
     */
    @Action
    public void launchTrainer (ActionEvent e)
    {
        CursorController.launchWithDelayedMessage("Launching trainer...", new Runnable()
                                          {
                                              @Override
                                              public void run ()
                                              {
                                                  Trainer.launch();
                                              }
                                          });
    }

    //-------------------//
    // saveGlobalSamples //
    //-------------------//
    /**
     * Action that saves the global sample repository.
     *
     * @param e the event which triggered this action
     */
    @Action
    public void saveGlobalSamples (ActionEvent e)
    {
        SampleRepository.getGlobalInstance().checkForSave();
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
     * Action to launch a browser on Audiveris manual
     *
     * @param e the event which triggered this action
     */
    @Action(enabledProperty = "browserSupported")
    public void showManual (ActionEvent e)
    {
        //        Path path = WellKnowns.DOC_FOLDER.resolve(constants.manualUrl.getValue());
        //
        //        if (!Files.exists(path)) {
        //            logger.warn("Cannot find file {}", path);
        //        } else {
        //            URI uri = path.toUri();
        //            WebBrowser.getBrowser().launch(uri);
        //        }
        String str = constants.manualUrl.getValue();

        try {
            URI uri = new URI(str);
            WebBrowser.getBrowser().launch(uri);
        } catch (URISyntaxException ex) {
            logger.warn("Illegal manual uri " + str, ex);
        }
    }

    //------------//
    // showMemory //
    //------------//
    /**
     * Action to display the current value of occupied memory
     *
     * @param e the event that triggered this action
     */
    @Action
    public void showMemory (ActionEvent e)
    {
        logger.info("\n----- Occupied memory is {} bytes -----\n", Memory.getValue());
    }

    //--------------//
    // toggleBoards //
    //--------------//
    /**
     * Action that toggles the display of boards window
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = BOARDS_WINDOW_DISPLAYED)
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
    @Action(selectedProperty = ERRORS_WINDOW_DISPLAYED)
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
    @Action(selectedProperty = LOG_WINDOW_DISPLAYED)
    public void toggleLog (ActionEvent e)
    {
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
            WebBrowser.getBrowser().launch(uri);
        } catch (URISyntaxException ex) {
            logger.warn("Illegal site uri " + str, ex);
        }
    }

    //-----------//
    // visitWiki //
    //-----------//
    /**
     * Action to launch a browser on application wiki
     *
     * @param e the event which triggered this action
     */
    @Action(enabledProperty = "browserSupported")
    public void visitWiki (ActionEvent e)
    {
        String str = constants.wikiUrl.getValue();

        try {
            URI uri = new URI(str);
            WebBrowser.getBrowser().launch(uri);
        } catch (URISyntaxException ex) {
            logger.warn("Illegal wiki uri " + str, ex);
        }
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class in application.
     *
     * @return the instance
     */
    public static GuiActions getInstance ()
    {
        return LazySingleton.INSTANCE;
    }

    //---------------//
    // LazySingleton //
    //---------------//
    private static class LazySingleton
    {

        static final GuiActions INSTANCE = new GuiActions();
    }

    public static class AboutAction
    {

        // Dialog
        private JDialog aboutBox = null;

        private HyperlinkListener linkListener = new LinkListener();

        public void actionPerformed (ActionEvent e)
        {
            if (aboutBox == null) {
                aboutBox = createAboutBox();
            }

            OmrGui.getApplication().show(aboutBox);
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

            ///builder.setDefaultDialogBorder();
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
                    ((JEditorPane) topic.comp).addHyperlinkListener(linkListener);
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
            Topic.version.comp.setText(WellKnowns.TOOL_REF + ":" + WellKnowns.TOOL_BUILD);
            Topic.classes.comp.setText(WellKnowns.CLASS_CONTAINER.toString());
            Topic.license.comp.setText("GNU Affero GPL v3");

            Topic.ocr.comp.setText(TesseractOCR.getInstance().identify());

            Topic.javaVendor.comp.setText(System.getProperty("java.vendor"));
            Topic.javaVersion.comp.setText(System.getProperty("java.version"));
            Topic.javaRuntime.comp.setText(
                    System.getProperty("java.runtime.name") + " (build "
                            + System.getProperty("java.runtime.version")
                            + ")");
            Topic.javaVm.comp.setText(
                    System.getProperty("java.vm.name") + " (build "
                            + System.getProperty("java.vm.version")
                            + ", "
                            + System.getProperty("java.vm.info")
                            + ")");
            Topic.os.comp.setText(
                    System.getProperty("os.name") + " " + System.getProperty("os.version"));
            Topic.osArch.comp.setText(System.getProperty("os.arch"));

            return dialog;
        }

        private static enum Topic
        {
            /** Longer application description */
            description(new JTextField()),
            /** Current version */
            version(new JTextField()),
            /** Precise classes */
            classes(new JTextField()),
            /** Link to web site */
            home(new JEditorPane("text/html", "")),
            /** Link to book site */
            book(new JEditorPane("text/html", "")),
            /** License */
            license(new JTextField()),
            /** OCR version */
            ocr(new JTextField()),
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

            public final JTextComponent comp;

            Topic (JTextComponent comp)
            {
                this.comp = comp;
            }
        }

        //------------//
        // ImagePanel //
        //------------//
        private static class ImagePanel
                extends JPanel
        {

            private Image img;

            ImagePanel (Image img)
            {
                this.img = img;

                Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
                setPreferredSize(size);
                setMinimumSize(size);
                setMaximumSize(size);
                setSize(size);
                setLayout(null);
            }

            ImagePanel (URI uri)
                    throws MalformedURLException
            {
                this(new ImageIcon(uri.toURL()).getImage());
            }

            @Override
            public void paintComponent (Graphics g)
            {
                g.drawImage(img, 0, 0, null);
            }
        }

        private static class LinkListener
                implements HyperlinkListener
        {

            @Override
            public void hyperlinkUpdate (HyperlinkEvent event)
            {
                HyperlinkEvent.EventType type = event.getEventType();
                final URL url = event.getURL();

                if (type == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        //System.out.println("Activated URL " + url);
                        URI uri = new URI(url.toString());
                        WebBrowser.getBrowser().launch(uri);
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
    private static class Constants
            extends ConstantSet
    {

        private final Constant.String webSiteUrl = new Constant.String(
                "http://www.audiveris.org",
                "URL of Audiveris home page");

        private final Constant.String wikiUrl = new Constant.String(
                "https://github.com/Audiveris/audiveris/wiki",
                "URL of Audiveris wiki");

        private final Constant.String manualUrl = new Constant.String( //"docs/manual/handbook.html",
                "https://bacchushlg.gitbooks.io/audiveris-5-1/content/",
                "URL of Audiveris manual");

        private final Constant.Boolean boardsWindowDisplayed = new Constant.Boolean(
                true,
                "Should the boards window be displayed");

        private final Constant.Boolean logWindowDisplayed = new Constant.Boolean(
                true,
                "Should the log window be displayed");

        private final Constant.Boolean errorsWindowDisplayed = new Constant.Boolean(
                false,
                "Should the errors window be displayed");
    }

    //-------------//
    // OptionsTask //
    //-------------//
    private static class OptionsTask
            extends Task<Options, Void>
    {

        final Timer timer = new Timer();

        OptionsTask ()
        {
            super(OmrGui.getApplication());

            timer.schedule(new TimerTask()
            {
                @Override
                public void run ()
                {
                    logger.info("Building options window...");
                }
            }, CursorController.delay);
        }

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
        protected void finished ()
        {
            timer.cancel();
        }

        @Override
        protected void succeeded (Options options)
        {
            if (options != null) {
                OmrGui.getApplication().show(options.getComponent());
            }
        }
    }
}
