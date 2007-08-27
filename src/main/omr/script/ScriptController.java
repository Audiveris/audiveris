//----------------------------------------------------------------------------//
//                                                                            //
//                      S c r i p t C o n t r o l l e r                       //
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

import omr.sheet.Sheet;

import omr.ui.icon.IconManager;
import omr.ui.util.SwingWorker;
import omr.ui.util.UIUtilities;
import omr.ui.util.FileFilter;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import javax.swing.*;

/**
 * Class <code>ScriptController</code> is the part of the user interface dealing
 * with selecting the files for loading and storing scripts.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScriptController
{
        //~ Static fields/initializers ---------------------------------------------

        /** Specific application parameters */
        private static final Constants constants = new Constants();

        /** Usual logger utility */
        private static final Logger logger = Logger.getLogger(
                ScriptController.class);

        //~ Instance fields --------------------------------------------------------

        /** User action to open a script */
        private final Action openAction = new OpenAction();

        /** User action to store a script */
        private final Action storeAction = new StoreAction();

        //~ Constructors -----------------------------------------------------------

        //------------------//
        // ScriptController //
        //------------------//
        /**
         * Create an instance of script controller
         */
        public ScriptController ()
        {
        }

        //~ Methods ----------------------------------------------------------------

        //---------------//
        // getOpenAction //
        //---------------//
        /**
         * Report the action that handles the opening of a script
         *
         * @return the action for inclusion in some UI layout
         */
        public Action getOpenAction ()
        {
                return openAction;
        }

        //----------------//
        // getStoreAction //
        //----------------//
        /**
         * Report the action that handles the storing of a script
         *
         * @return the action for inclusion in some UI layout
         */
        public Action getStoreAction ()
        {
                return storeAction;
        }

        //-------------//
        // checkStored //
        //-------------//
        public void checkStored (Script script)
        {
                if (!script.isStored() && constants.closeConfirmation.getValue()) {
                        int answer = JOptionPane.showConfirmDialog(
                                null,
                                "Save script for sheet " + script.getSheet().getRadix() + "?");

                        if (answer == JOptionPane.YES_OPTION) {
                                storeScript(script);
                        }
                }
        }

        //--------------//
        // selectScript //
        //--------------//
        /**
         * User dialog, to allow the selection and load of a script file.
         */
        private void selectScript ()
        {
                final File file = UIUtilities.fileChooser(
                        false,
                        Main.getGui().getFrame(),
                        new File(constants.defaultScriptDirectory.getValue()),
                        new FileFilter(
                                "Score script files",
                                new String[] { ScriptManager.SCRIPT_EXTENSION }));

                final SwingWorker<Void> worker = new SwingWorker<Void>() {
                        // This runs on worker's thread
                        @Implement(SwingWorker.class)
                        @Override
                        public Void construct ()
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

                worker.start();
        }

        //-------------//
        // storeScript //
        //-------------//
        /**
         * Store a script
         *
         * @param script the script to save on disk
         */
        private void storeScript (final Script script)
        {
                // Where do we write the script file?
                File         xmlFile = new File(
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
                        final File        file = xmlFile;
                        final SwingWorker worker = new SwingWorker() {
                                @Override
                                public Object construct ()
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

                        worker.start();
                }
        }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // OpenAction //
    //------------//
    /**
     * Class <code>OpenAction</code> handles the interactive selection of a
     * script file and its loading.
     */
    public class OpenAction
        extends AbstractAction
    {
        public OpenAction ()
        {
            super(
                "Open script",
                IconManager.getInstance().loadImageIcon("general/Import"));
            putValue(SHORT_DESCRIPTION, "Open a score script file");
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            selectScript();
        }
    }

    //-------------//
    // StoreAction //
    //-------------//
    /**
     * Class <code>StoreAction</code> handles the saving of the currently
     * selected script
     */
    public class StoreAction
        extends AbstractAction
    {
        public StoreAction ()
        {
            super(
                "Store script",
                IconManager.getInstance().loadImageIcon("general/Export"));
            putValue(SHORT_DESCRIPTION, "Store current score script");
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            Sheet sheet = Main.getGui().sheetController.getCurrentSheet();

            if (sheet != null) {
                final Script script = sheet.getScript();

                if (script != null) {
                    storeScript(script);
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
