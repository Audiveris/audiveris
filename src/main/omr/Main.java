//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            M a i n                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr;

import omr.CLI.CliTask;

import omr.constant.Constant;
import omr.constant.ConstantManager;
import omr.constant.ConstantSet;

import omr.log.LogUtil;

import omr.sheet.BookManager;

import omr.ui.MainGui;
import omr.ui.symbol.MusicFont;

import omr.util.ClassUtil;
import omr.util.OmrExecutors;

import org.jdesktop.application.Application;

import org.kohsuke.args4j.CmdLineException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Class {@code Main} is the main class for OMR application.
 * <p>
 * It deals with the main routine and its command line parameters.
 * It launches the User Interface, unless a batch mode is selected.
 *
 * @see CLI
 *
 * @author Hervé Bitteur
 */
public class Main
{
    //~ Static fields/initializers -----------------------------------------------------------------

    static {
        // We need class WellKnowns to be elaborated before anything else
        WellKnowns.ensureLoaded();
    }

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final Constants constants = new Constants();

    /** CLI parameters. */
    private static CLI cli;

    //~ Constructors -------------------------------------------------------------------------------
    private Main ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // getCli //
    //--------//
    /**
     * Points to the command line interface parameters
     *
     * @return CLI instance
     */
    public static CLI getCli ()
    {
        return cli;
    }

    //---------------------//
    // getSheetStepTimeOut //
    //---------------------//
    /**
     * Report the timeout value for any step on a sheet.
     *
     * @return the timeout value (in seconds)
     */
    public static int getSheetStepTimeOut ()
    {
        return constants.sheetStepTimeOut.getValue();
    }

    //------//
    // main //
    //------//
    /**
     * Specific starting method for the application.
     *
     * @param args command line parameters
     * @see omr.CLI the possible command line parameters
     */
    public static void main (String[] args)
    {
        // Process CLI parameters
        processCli(args);

        // Initialize tool parameters
        initialize();

        // Locale to be used in the whole application?
        checkLocale();

        // Environment
        showEnvironment();

        // Native libs
        //loadNativeLibraries();
        // Engine
        OMR.engine = BookManager.getInstance();

        if (!cli.isBatchMode()) {
            // Here we are in interactive mode
            logger.debug("Main. Launching MainGui");
            Application.launch(MainGui.class, args);
        } else {
            // Here we are in batch mode

            // Check MusicFont is loaded
            MusicFont.checkMusicFont();

            // Run the required tasks, if any (and remember if at least one task failed)
            boolean failure = runBatchTasks();

            // At this point all tasks have completed (except timeout...)
            // So shutdown gracefully the executors
            OmrExecutors.shutdown(false);

            // Store latest constant values on disk?
            if (constants.persistBatchCliConstants.getValue()) {
                ConstantManager.getInstance().storeResource();
            }

            // Stop the JVM with failure status?
            if (failure) {
                logger.warn("Exit with failure status");
                System.exit(-1);
            }
        }
    }

    //--------------------------//
    // processSystemsInParallel //
    //--------------------------//
    public static boolean processSystemsInParallel ()
    {
        return constants.processSystemsInParallel.isSet();
    }

    //----------------------//
    // saveSheetOnEveryStep //
    //----------------------//
    public static boolean saveSheetOnEveryStep ()
    {
        return constants.saveSheetOnEveryStep.isSet();
    }

    //-------------//
    // checkLocale //
    //-------------//
    private static void checkLocale ()
    {
        final String localeStr = constants.locale.getValue().trim();

        if (!localeStr.isEmpty()) {
            for (Locale locale : Locale.getAvailableLocales()) {
                if (locale.toString().equalsIgnoreCase(localeStr)) {
                    Locale.setDefault(locale);
                    logger.debug("Locale set to {}", locale);

                    return;
                }
            }

            logger.warn("Cannot set locale to {}", localeStr);
        }
    }

    //------------//
    // initialize //
    //------------//
    private static void initialize ()
    {
        // (re) Open the executor services
        OmrExecutors.restart();
    }

    //---------------------//
    // loadNativeLibraries //
    //---------------------//
    /**
     * Explicitly load all the needed native libraries.
     */
    private static void loadNativeLibraries ()
    {
        boolean verbose = constants.showNatives.isSet();

        // Explicitly load all native libs resources and in proper order
        if (verbose) {
            logger.info("Loading native libraries ...");
        }

        boolean success = true;

        if (WellKnowns.WINDOWS) {
            // For Windows, drop only the ".dll" suffix
            success &= ClassUtil.loadLibrary("jniTessBridge", verbose);
            success &= ClassUtil.loadLibrary("libtesseract302", verbose);
            success &= ClassUtil.loadLibrary("liblept168", verbose);
        } else if (WellKnowns.LINUX) {
            // For Linux, drop both the "lib" prefix and the ".so" suffix
            success &= ClassUtil.loadLibrary("jniTessBridge", verbose);
        }

        if (success) {
            logger.info("All native libraries loaded.");
        } else {
            // Inform user of OCR installation problem
            String msg = "Tesseract OCR is not installed properly";

            if (OMR.gui != null) {
                OMR.gui.displayError(msg);
            } else {
                logger.warn(msg);
            }
        }
    }

    //----------//
    // logTasks //
    //----------//
    private static void logTasks (List<CliTask> tasks,
                                  boolean inParallel)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Submitting ").append(tasks.size()).append(" task(s) in ");
        sb.append(inParallel ? "parallel:" : "sequence:");

        for (Callable task : tasks) {
            sb.append("\n    ").append(task);
        }

        logger.info(sb.toString());
    }

    //------------//
    // processCli //
    //------------//
    private static void processCli (String[] args)
    {
        try {
            // First get the provided parameters if any
            cli = new CLI(WellKnowns.TOOL_NAME);
            cli.getParameters(args);

            // Interactive or Batch mode ?
            if (cli.isBatchMode()) {
                logger.info("Running in batch mode");

                ///System.setProperty("java.awt.headless", "true"); //TODO: Useful?
            } else {
                logger.debug("Running in interactive mode");
                LogUtil.addGuiAppender();
            }
        } catch (CmdLineException ex) {
            logger.warn("Error in command line: {}", ex.getLocalizedMessage(), ex);
            logger.warn("Exiting ...");

            // Stop the JVM, with failure status (1)
            Runtime.getRuntime().exit(1);
        }
    }

    //---------------//
    // runBatchTasks //
    //---------------//
    private static boolean runBatchTasks ()
    {
        boolean failure = false;
        final List<CliTask> tasks = cli.getCliTasks();

        if (!tasks.isEmpty()) {
            // Run all tasks in parallel? (or one task at a time)
            if (constants.runBatchTasksInParallel.isSet()) {
                try {
                    logTasks(tasks, true);

                    List<Future<Void>> futures = OmrExecutors.getCachedLowExecutor().invokeAll(
                            tasks);
                    logger.info("Checking {} task(s)", tasks.size());

                    // Check for time-out
                    for (Future<Void> future : futures) {
                        try {
                            future.get();
                        } catch (Exception ex) {
                            CliTask task = tasks.get(futures.indexOf(future));
                            final String radix = task.getRadix();

                            logger.warn("Future exception on {}, {}", radix, ex.toString(), ex);
                            failure = true;
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("Error in processing tasks", ex);
                    failure = true;
                }
            } else {
                logTasks(tasks, false);

                for (CliTask task : tasks) {
                    try {
                        task.call();
                    } catch (Exception ex) {
                        final String radix = task.getRadix();
                        logger.warn("Exception on {}, {}", radix, ex.toString(), ex);
                        failure = true;
                    }
                }
            }
        }

        return failure;
    }

    //-----------------//
    // showEnvironment //
    //-----------------//
    /**
     * Show the application environment to the user.
     */
    private static void showEnvironment ()
    {
        if (constants.showEnvironment.isSet()) {
            logger.info(
                    "Environment:\n" + "- Audiveris:    {}\n" + "- OS:           {}\n"
                    + "- Architecture: {}\n" + "- Java VM:      {}",
                    WellKnowns.TOOL_REF + ":" + WellKnowns.TOOL_BUILD,
                    System.getProperty("os.name") + " " + System.getProperty("os.version"),
                    System.getProperty("os.arch"),
                    System.getProperty("java.vm.name") + " (build "
                    + System.getProperty("java.vm.version") + ", " + System.getProperty("java.vm.info")
                    + ")");
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean showEnvironment = new Constant.Boolean(
                true,
                "Should we show environment?");

        private final Constant.Boolean showNatives = new Constant.Boolean(
                true,
                "Should we show loading of native libraries?");

        private final Constant.String locale = new Constant.String(
                "en",
                "Locale language to be used in the whole application (en, fr)");

        private final Constant.Boolean persistBatchCliConstants = new Constant.Boolean(
                false,
                "Should we persist CLI-defined constants when running in batch?");

        private final Constant.Boolean runBatchTasksInParallel = new Constant.Boolean(
                false,
                "Should we process all tasks in parallel when running in batch?");

        private final Constant.Boolean processSystemsInParallel = new Constant.Boolean(
                false,
                "Should we process all systems in parallel in a sheet?");

        private final Constant.Boolean saveSheetOnEveryStep = new Constant.Boolean(
                true,
                "Should we save sheet after every successful step?");

        private final Constant.Integer sheetStepTimeOut = new Constant.Integer(
                "Seconds",
                120,
                "Time-out for one step on a sheet, specified in seconds");

        private final Constant.Boolean closeBookOnEnd = new Constant.Boolean(
                true,
                "Should we close a book when it has been processed in batch?");
    }
}
