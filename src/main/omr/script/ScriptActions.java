//----------------------------------------------------------------------------//
//                                                                            //
//                         S c r i p t A c t i o n s                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.Main;
import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.Score;
import omr.score.ui.ScoreController;

import omr.sheet.ui.SheetActions;

import omr.ui.util.OmrFileFilter;
import omr.ui.util.UIUtil;

import omr.util.BasicTask;
import omr.util.Param;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;

/**
 * Class {@code ScriptActions} gathers UI actions related to script
 * handling. These static member classes are ready to be picked by the plugins
 * mechanism.
 *
 * @author Hervé Bitteur
 */
public class ScriptActions
        extends SheetActions
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            ScriptActions.class);

    /** Singleton */
    private static ScriptActions INSTANCE;

    /** Default parameter. */
    public static final Param<Boolean> defaultPrompt = new Default();

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // ScriptActions //
    //---------------//
    /**
     * Not meant to be instantiated
     */
    protected ScriptActions ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // checkStored //
    //-------------//
    /**
     * Check whether the provided script has been safely saved if needed
     * (and therefore, if the sheet can be closed)
     *
     * @param script the script to check
     * @return true if close is allowed, false if not
     */
    public static boolean checkStored (Script script)
    {
        if (script.isModified() && defaultPrompt.getSpecific()) {
            int answer = JOptionPane.showConfirmDialog(
                    null,
                    "Save script for score " + script.getScore().getRadix() + "?");

            if (answer == JOptionPane.YES_OPTION) {
                Task<Void, Void> task = getInstance()
                        .storeScript(null);

                if (task != null) {
                    task.execute();
                }

                // Here user has saved the script
                return true;
            }

            if (answer == JOptionPane.NO_OPTION) {
                // Here user specifically chooses NOT to save the script
                return true;
            }

            // // Here user says Oops!, cancelling the current close request
            return false;
        } else {
            return true;
        }
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the singleton
     *
     * @return the unique instance of this class
     */
    public static synchronized ScriptActions getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new ScriptActions();
        }

        return INSTANCE;
    }

    //------------//
    // loadScript //
    //------------//
    @Action
    public Task<Void, Void> loadScript (ActionEvent e)
    {
        final File file = UIUtil.fileChooser(
                false,
                Main.getGui().getFrame(),
                new File(constants.defaultScriptDirectory.getValue()),
                new OmrFileFilter(
                "Score script files",
                new String[]{ScriptManager.SCRIPT_EXTENSION}));

        if (file != null) {
            return new LoadScriptTask(file);
        } else {
            return null;
        }
    }

    //-------------//
    // storeScript //
    //-------------//
    @Action(enabledProperty = SHEET_AVAILABLE)
    public Task<Void, Void> storeScript (ActionEvent e)
    {
        final Score score = ScoreController.getCurrentScore();

        if (score == null) {
            return null;
        }

        final File scriptFile = score.getScriptFile();

        if (scriptFile != null) {
            return new StoreScriptTask(score.getScript(), scriptFile);
        } else {
            return storeScriptAs(e);
        }
    }

    //---------------//
    // storeScriptAs //
    //---------------//
    @Action(enabledProperty = SHEET_AVAILABLE)
    public Task<Void, Void> storeScriptAs (ActionEvent e)
    {
        final Score score = ScoreController.getCurrentScore();

        if (score == null) {
            return null;
        }

        // Let the user select a script output file
        File scriptFile = UIUtil.fileChooser(
                true,
                Main.getGui().getFrame(),
                getDefaultScriptFile(score),
                new OmrFileFilter(
                "Script files",
                new String[]{ScriptManager.SCRIPT_EXTENSION}));

        if (scriptFile != null) {
            return new StoreScriptTask(score.getScript(), scriptFile);
        } else {
            return null;
        }
    }

    //----------------------//
    // getDefaultScriptFile //
    //----------------------//
    /**
     * Report the default file where the script should be written to
     *
     * @param score the owning score
     * @return the default file for saving the script
     */
    private File getDefaultScriptFile (Score score)
    {
        return (score.getScriptFile() != null) ? score.getScriptFile()
                : new File(
                constants.defaultScriptDirectory.getValue(),
                score.getRadix() + ScriptManager.SCRIPT_EXTENSION);
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Default directory for saved scripts */
        Constant.String defaultScriptDirectory = new Constant.String(
                WellKnowns.DEFAULT_SCRIPTS_FOLDER.toString(),
                "Default directory for saved scripts");

        /** User confirmation for closing unsaved script */
        Constant.Boolean closeConfirmation = new Constant.Boolean(
                true,
                "Should we ask confirmation for closing a sheet with unsaved script?");

    }

    //---------//
    // Default //
    //---------//
    private static class Default
            extends Param<Boolean>
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public Boolean getSpecific ()
        {
            return constants.closeConfirmation.getValue();
        }

        @Override
        public boolean setSpecific (Boolean specific)
        {
            if (!getSpecific()
                    .equals(specific)) {
                constants.closeConfirmation.setValue(specific);
                logger.info(
                        "You will {} be prompted to save script when"
                        + " closing score",
                        specific ? "now" : "no longer");

                return true;
            } else {
                return false;
            }
        }
    }

    //----------------//
    // LoadScriptTask //
    //----------------//
    private static class LoadScriptTask
            extends BasicTask
    {
        //~ Instance fields ----------------------------------------------------

        private File file;

        //~ Constructors -------------------------------------------------------
        LoadScriptTask (File file)
        {
            this.file = file;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            // Actually run the script
            logger.info("Running script file {} ...", file);

            try {
                final Script script = ScriptManager.getInstance()
                        .load(
                        new FileInputStream(file));

                if (logger.isDebugEnabled()) {
                    script.dump();
                }

                // Remember (even across runs) the parent directory
                constants.defaultScriptDirectory.setValue(file.getParent());
                script.run();
            } catch (FileNotFoundException ex) {
                logger.warn("Cannot find script file {}", file);
            }

            return null;
        }
    }

    //-----------------//
    // StoreScriptTask //
    //-----------------//
    private static class StoreScriptTask
            extends BasicTask
    {
        //~ Instance fields ----------------------------------------------------

        private Script script;

        private File file;

        //~ Constructors -------------------------------------------------------
        StoreScriptTask (Script script,
                         File file)
        {
            this.script = script;
            this.file = file;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws InterruptedException
        {
            FileOutputStream fos = null;

            try {
                File folder = new File(file.getParent());

                if (folder.mkdirs()) {
                    logger.info("Creating folder {}", folder);
                }

                fos = new FileOutputStream(file);
                omr.script.ScriptManager.getInstance()
                        .store(script, fos);
                logger.info("Script stored as {}", file);
                constants.defaultScriptDirectory.setValue(file.getParent());
                script.getScore()
                        .setScriptFile(file);
            } catch (FileNotFoundException ex) {
                logger.warn("Cannot find script file " + file, ex);
            } catch (JAXBException ex) {
                logger.warn("Cannot marshal script", ex);
            } catch (Throwable ex) {
                logger.warn("Error storing script", ex);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            return null;
        }
    }
}
