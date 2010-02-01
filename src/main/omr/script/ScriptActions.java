//----------------------------------------------------------------------------//
//                                                                            //
//                         S c r i p t A c t i o n s                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.Score;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetActions;
import omr.sheet.ui.SheetsController;

import omr.ui.util.OmrFileFilter;
import omr.ui.util.UIUtilities;

import omr.util.BasicTask;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;

/**
 * Class <code>ScriptActions</code> gathers UI actions related to script
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
    private static final Logger logger = Logger.getLogger(ScriptActions.class);

    /** Singleton */
    private static ScriptActions INSTANCE;

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

    //-------------------//
    // setConfirmOnClose //
    //-------------------//
    public static void setConfirmOnClose (boolean bool)
    {
        if (bool != isConfirmOnClose()) {
            if (bool) {
                logger.info(
                    "You will now be prompted for Script saving on close");
            } else {
                logger.info(
                    "You will no longer be prompted for Script saving on close");
            }

            constants.closeConfirmation.setValue(bool);
        }
    }

    //------------------//
    // isConfirmOnClose //
    //------------------//
    public static boolean isConfirmOnClose ()
    {
        return constants.closeConfirmation.getValue();
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

    //-------------//
    // checkStored //
    //-------------//
    public static boolean checkStored (Script script)
    {
        if (script.isModified() && isConfirmOnClose()) {
            int answer = JOptionPane.showConfirmDialog(
                null,
                "Save script for sheet " + script.getSheet().getRadix() + "?");

            if (answer == JOptionPane.YES_OPTION) {
                Task task = getInstance()
                                .storeScript(null);

                if (task != null) {
                    task.execute();
                }

                return true;
            }

            if (answer == JOptionPane.NO_OPTION) {
                return true;
            }

            return false;
        } else {
            return true;
        }
    }

    //------------//
    // loadScript //
    //------------//
    @Action
    public Task loadScript (ActionEvent e)
    {
        final File file = UIUtilities.fileChooser(
            false,
            Main.getGui().getFrame(),
            new File(constants.defaultScriptDirectory.getValue()),
            new OmrFileFilter(
                "Score script files",
                new String[] { ScriptManager.SCRIPT_EXTENSION }));

        if (file != null) {
            return new LoadScriptTask(file);
        } else {
            return null;
        }
    }

    //-------------//
    // storeScript //
    //-------------//
    @Action(enabledProperty = "sheetAvailable")
    public Task storeScript (ActionEvent e)
    {
        final Sheet sheet = SheetsController.selectedSheet();

        if (sheet == null) {
            return null;
        }

        final File scriptFile = sheet.getScore()
                                     .getScriptFile();

        if (scriptFile != null) {
            return new StoreScriptTask(sheet.getScript(), scriptFile);
        } else {
            return storeScriptAs(e);
        }
    }

    //---------------//
    // storeScriptAs //
    //---------------//
    @Action(enabledProperty = "sheetAvailable")
    public Task storeScriptAs (ActionEvent e)
    {
        final Sheet sheet = SheetsController.selectedSheet();

        if (sheet == null) {
            return null;
        }

        // Let the user select a script output file
        File scriptFile = UIUtilities.fileChooser(
            true,
            Main.getGui().getFrame(),
            getDefaultScriptFile(sheet.getScore()),
            new OmrFileFilter(
                "Script files",
                new String[] { ScriptManager.SCRIPT_EXTENSION }));

        if (scriptFile != null) {
            return new StoreScriptTask(sheet.getScript(), scriptFile);
        } else {
            return null;
        }
    }

    //----------------------//
    // getDefaultScriptFile //
    //----------------------//
    /**
     * Report the default file where the script should be written to
     * @param score the owning score
     * @return the default file for saving the script
     */
    private File getDefaultScriptFile (Score score)
    {
        return (score.getScriptFile() != null) ? score.getScriptFile()
               : new File(
            constants.defaultScriptDirectory.getValue(),
            score.getSheet().getRadix() + ScriptManager.SCRIPT_EXTENSION);
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
            "",
            "Default directory for saved scripts");

        /** User confirmation for closing unsaved script */
        Constant.Boolean closeConfirmation = new Constant.Boolean(
            true,
            "Should we ask confirmation for closing a sheet with unsaved script?");
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
            // Actually load the script
            logger.info("Loading script file " + file + " ...");

            try {
                final Script script = ScriptManager.getInstance()
                                                   .load(
                    new FileInputStream(file));

                if (logger.isFineEnabled()) {
                    script.dump();
                }

                // Remember (even across runs) the parent directory
                constants.defaultScriptDirectory.setValue(file.getParent());
                script.run();
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find script file " + file);
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
        private File   file;

        //~ Constructors -------------------------------------------------------

        StoreScriptTask (Script script,
                         File   file)
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
                java.io.File folder = new java.io.File(file.getParent());

                if (folder.mkdirs()) {
                    logger.info("Creating folder " + folder);
                }

                fos = new java.io.FileOutputStream(file);
                omr.script.ScriptManager.getInstance()
                                        .store(script, fos);
                logger.info("Script stored as " + file);
                constants.defaultScriptDirectory.setValue(file.getParent());
                script.getSheet()
                      .getScore()
                      .setScriptFile(file);
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find script file " + file, ex);
            } catch (JAXBException ex) {
                logger.warning("Cannot marshal script", ex);
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
