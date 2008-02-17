//----------------------------------------------------------------------------//
//                                                                            //
//                         S c r i p t A c t i o n s                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import static omr.plugin.Dependency.*;
import omr.plugin.Plugin;
import static omr.plugin.PluginType.*;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.ui.util.FileFilter;
import omr.ui.util.UIUtilities;

import omr.util.Implement;
import omr.util.Logger;

import org.jdesktop.swingworker.SwingWorker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/**
 * Class <code>ScriptActions</code> gathers UI actions related to script
 * handling. These static member classes are ready to be picked by the plugins
 * mechanism.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScriptActions
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScriptActions.class);

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // ScriptActions //
    //---------------//
    /**
     * Not meant to be instantiated
     */
    private ScriptActions ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // checkStored //
    //-------------//
    public static boolean checkStored (Script script)
    {
        if (!script.isStored() && constants.closeConfirmation.getValue()) {
            int answer = JOptionPane.showConfirmDialog(
                null,
                "Save script for sheet " + script.getSheet().getRadix() + "?");

            if (answer == JOptionPane.YES_OPTION) {
                ScriptActions.storeScript(script);

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

    //-------------//
    // storeScript //
    //-------------//
    static void storeScript (final Script script)
    {
        // Where do we write the script file?
        File xmlFile = new File(
            constants.defaultScriptDirectory.getValue(),
            script.getSheet().getRadix() + ScriptManager.SCRIPT_EXTENSION);

        // Ask user confirmation: let the user select a script output file
        xmlFile = UIUtilities.fileChooser(
            true,
            Main.getGui().getFrame(),
            xmlFile,
            new FileFilter(
                "Script files",
                new String[] { ScriptManager.SCRIPT_EXTENSION }));

        if (xmlFile != null) {
            constants.defaultScriptDirectory.setValue(xmlFile.getParent());
        } else {
            return;
        }

        if (xmlFile != null) {
            final File                        file = xmlFile;
            final SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground ()
                {
                    try {
                        java.io.File folder = new java.io.File(
                            file.getParent());

                        if (folder.mkdirs()) {
                            logger.info("Creating folder " + folder);
                        }

                        omr.script.ScriptManager.getInstance()
                                                .store(
                            script,
                            new java.io.FileOutputStream(file));
                        logger.info("Script stored as " + file);
                    } catch (FileNotFoundException ex) {
                        logger.warning("Cannot find script file " + file, ex);
                    }

                    return null;
                }
            };

            worker.execute();
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // LoadAction //
    //------------//
    /**
     * Class <code>LoadAction</code> let the user select and load a script file
     */
    @Plugin(type = SCRIPT, dependency = NONE, onToolbar = false)
    public static class LoadAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            final File                        file = UIUtilities.fileChooser(
                false,
                Main.getGui().getFrame(),
                new File(constants.defaultScriptDirectory.getValue()),
                new FileFilter(
                    "Score script files",
                    new String[] { ScriptManager.SCRIPT_EXTENSION }));

            final SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground ()
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
                        constants.defaultScriptDirectory.setValue(
                            file.getParent());
                        script.run();
                    } catch (FileNotFoundException ex) {
                        logger.warning("Cannot find script file " + file);
                    }

                    return null;
                }
            };

            worker.execute();
        }
    }

    //-------------//
    // StoreAction //
    //-------------//
    /**
     * Class <code>StoreAction</code> handles the storing of the currently
     * selected script.
     */
    @Plugin(type = SCRIPT, dependency = SHEET_AVAILABLE, onToolbar = false)
    public static class StoreAction
        extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            final Sheet sheet = SheetManager.getSelectedSheet();

            if (sheet == null) {
                return;
            }

            final Script script = sheet.getScript();

            if (script == null) {
                return;
            }

            storeScript(script);
        }
    }

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
}
