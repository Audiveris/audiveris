//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      G u i A c t i o n s                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.OMR;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.classifier.ui.SampleBrowser;
import org.audiveris.omr.classifier.ui.Trainer;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.ui.ShapeColorChooser;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.sheet.Versions;
import org.audiveris.omr.text.tesseract.Languages;
import org.audiveris.omr.ui.action.Preferences;
import org.audiveris.omr.ui.symbol.SymbolRipper;
import org.audiveris.omr.ui.util.CursorController;
import org.audiveris.omr.ui.util.OmrFileFilter;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.ui.util.WaitingTask;
import org.audiveris.omr.ui.util.WebBrowser;
import org.audiveris.omr.util.Memory;
import org.audiveris.omr.util.VoidTask;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * Class <code>GuiActions</code> gathers general actions triggered from the main GUI.
 *
 * @author Hervé Bitteur
 */
public class GuiActions
        extends AbstractBean
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(GuiActions.class);

    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(GuiActions.class);

    /** Constants UI. */
    private static ConstantsUI constantsUI;

    /** Create this action just once. */
    private static volatile AboutAction aboutAction;

    //~ Methods ------------------------------------------------------------------------------------

    //---------------------//
    // browseGlobalSamples //
    //---------------------//
    /**
     * Launch browser on the global repository.
     *
     * @param e the event which triggered this action
     * @return the background task
     */
    @Action
    public Task browseGlobalSamples (ActionEvent e)
    {
        return new SampleBrowser.Waiter(resources.getString("launchingGlobalSampleBrowser"))
        {
            @Override
            protected SampleBrowser doInBackground ()
                throws Exception
            {
                return SampleBrowser.getInstance();
            }
        };
    }

    //--------------------//
    // browseLocalSamples //
    //--------------------//
    /**
     * Launch browser on a local sample repository.
     *
     * @param e the event which triggered this action
     * @return the background task
     */
    @Action
    public Task browseLocalSamples (ActionEvent e)
    {
        // Select local samples repository
        final String ext = SampleRepository.SAMPLES_FILE_NAME;
        final Path repoPath = UIUtil.pathChooser(
                false,
                OMR.gui.getFrame(),
                BookManager.getBaseFolder(),
                new OmrFileFilter(ext, new String[] { ext }));

        if (repoPath == null) {
            return null;
        }

        return new SampleBrowser.Waiter(resources.getString("launchingLocalSampleBrowser"))
        {
            @Override
            protected SampleBrowser doInBackground ()
                throws Exception
            {
                return new SampleBrowser(SampleRepository.getInstance(repoPath, true));
            }
        };
    }

    //-------------//
    // checkUpdate //
    //-------------//
    /**
     * Check current program version against latest release available on GitHub.
     *
     * @param e the event which triggered this action
     * @return the task to launch
     */
    @Action
    public Task checkUpdate (ActionEvent e)
    {
        return new CheckUpdateTask();
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

    //-----------------//
    // defineConstants //
    //-----------------//
    /**
     * Action that opens a window where units constants (logger level,
     * constants) can be managed.
     *
     * @param e the event that triggered this action
     * @return the SAF task
     */
    @Action
    public Task<ConstantsUI, Void> defineConstants (ActionEvent e)
    {
        return new ConstantsUITask();
    }

    //-------------------//
    // definePreferences //
    //-------------------//
    /**
     * Action that opens the dialog where the user preferences are managed.
     *
     * @param e the event that triggered this action
     */
    @Action
    public void definePreferences (ActionEvent e)
    {
        Preferences.show();
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

    //------------------//
    // installLanguages //
    //------------------//
    /**
     * Install OCR languages from github.
     *
     * @param e the event which triggered this action
     * @return the SAF task
     */
    @Action
    public InstallLanguagesTask installLanguages (ActionEvent e)
    {
        return new InstallLanguagesTask();
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

    //--------------------//
    // launchSymbolRipper //
    //--------------------//
    /**
     * Launch the utility to rip a symbol.
     */
    @Action
    public void launchSymbolRipper ()
    {
        new SymbolRipper();
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
        CursorController.launchWithDelayedMessage("Launching trainer...", () -> Trainer.launch());
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

    //~ Static Methods -----------------------------------------------------------------------------

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

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------------//
    // CheckUpdateTask //
    //-----------------//
    private static class CheckUpdateTask
            extends VoidTask
    {
        @Override
        protected Void doInBackground ()
            throws Exception
        {
            Versions.poll(true /* manual */);

            return null;
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

        private final Constant.String manualUrl = new Constant.String(
                "https://audiveris.github.io/audiveris/",
                "URL of Audiveris manual");
    }

    //----------------------//
    // InstallLanguagesTask //
    //----------------------//
    public static class InstallLanguagesTask
            extends VoidTask
    {
        @Override
        protected Void doInBackground ()
            throws Exception
        {
            final Languages.Selector dld = Languages.getInstance().getSelector();

            if (dld != null) {
                OmrGui.getApplication().show(dld.getComponent());
            }

            return null;
        }
    }

    //---------------//
    // LazySingleton //
    //---------------//
    private static class LazySingleton
    {
        static final GuiActions INSTANCE = new GuiActions();
    }

    //-----------------//
    // ConstantsUITask //
    //-----------------//
    private static class ConstantsUITask
            extends WaitingTask<ConstantsUI, Void>
    {
        ConstantsUITask ()
        {
            super(OmrGui.getApplication(), resources.getString("constantsTask.message"));
        }

        @Override
        protected ConstantsUI doInBackground ()
            throws Exception
        {
            if (constantsUI == null) {
                constantsUI = new ConstantsUI();
            }

            return constantsUI;
        }

        @Override
        protected void succeeded (ConstantsUI cui)
        {
            if (cui != null) {
                OmrGui.getApplication().show(cui.getComponent());
            }
        }
    }
}
