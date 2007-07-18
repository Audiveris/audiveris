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
import omr.ui.util.FileFilter;
import omr.ui.util.SwingWorker;

import omr.util.Logger;
import omr.util.OmrExecutors;

import java.awt.event.ActionEvent;
import java.io.*;
import java.util.concurrent.Executor;

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

    //--------------//
    // selectScript //
    //--------------//
    /**
     * User dialog, to allow the selection and load of a script file.
     */
    private void selectScript ()
    {
        // Let the user select a script file
        final JFileChooser fc = new JFileChooser(
            constants.initScriptDir.getValue());
        fc.addChoosableFileFilter(
            new FileFilter(
                "Score script files",
                new String[] { ScriptManager.SCRIPT_EXTENSION }));

        if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            final File file = fc.getSelectedFile();

            // Actually load the script
            logger.info("Loading script file " + file + " ...");

            try {
                final Script script = ScriptManager.getInstance()
                                                   .load(
                    new FileInputStream(file));
                script.dump();

                // Remember (even across runs) the parent directory
                constants.initScriptDir.setValue(file.getParent());

                // Run the script in parallel
                Executor executor = OmrExecutors.getLowExecutor();

                executor.execute(
                    new Runnable() {
                            @Override
                            public void run ()
                            {
                                script.run();
                            }
                        });
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find script file " + file);
            }
        }
    }

    //-------------//
    // storeScript //
    //-------------//
    /**
     * Store the script ot the current sheet
     */
    private void storeScript ()
    {
        Sheet sheet = Main.getGui().sheetController.getCurrentSheet();

        if (sheet == null) {
            return;
        }

        final Script script = sheet.getScript();

        if (script == null) {
            return;
        }

        // Where do we write the script file?
        File         xmlFile = new File(
            Main.getOutputFolder(),
            script.getSheet().getRadix() + ScriptManager.SCRIPT_EXTENSION);

        // Ask user confirmation: let the user select a script output file
        JFileChooser fc = new JFileChooser(Main.getOutputFolder());
        fc.addChoosableFileFilter(
            new FileFilter(
                "Script files",
                new String[] { ScriptManager.SCRIPT_EXTENSION }));
        fc.setSelectedFile(xmlFile);

        // Let the user play with the dialog
        int res = fc.showSaveDialog(Main.getGui().getFrame());

        if (res == JFileChooser.APPROVE_OPTION) {
            xmlFile = fc.getSelectedFile();

            // Remember (even across runs) the selected directory
            Main.setOutputFolder(xmlFile.getParent());
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

        @Override
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

        @Override
        public void actionPerformed (ActionEvent e)
        {
            storeScript();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /** Default directory for selection of script files */
        Constant.String initScriptDir = new Constant.String(
            "",
            "Default directory for selection of script files");
    }
}
