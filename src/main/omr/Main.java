//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            M a i n                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.CLI.Parameters;

import omr.constant.Constant;
import omr.constant.ConstantManager;
import omr.constant.ConstantSet;

import omr.sheet.BookManager;

import omr.ui.MainGui;
import omr.ui.symbol.MusicFont;

import omr.util.ClassUtil;
import omr.util.OmrExecutors;

import org.jdesktop.application.Application;

import org.kohsuke.args4j.CmdLineException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Class {@code Main} is the main class for OMR application.
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

    /** Parameters read from CLI. */
    private static Parameters parameters;

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
        // Process CLI arguments
        processCli(args);

        // Initialize tool parameters
        initialize();

        // Locale to be used in the whole application?
        checkLocale();

        // Environment
        showEnvironment();

        // Native libs
        loadNativeLibraries();

        // Engine
        OMR.setEngine(BookManager.getInstance());

        if (!parameters.batchMode) {
            // Here we are in interactive mode
            logger.debug("Main. Launching MainGui");
            Application.launch(MainGui.class, args);
        } else {
            // Here we are in batch mode

            // Check MusicFont is loaded
            MusicFont.checkMusicFont();

            // Run the required tasks, if any (and remember if at least one task failed)
            boolean failure = runBatchTasks();

            // At this point all tasks have completed (normally or not)
            // So shutdown immediately the executors
            OmrExecutors.shutdown(true);

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

            if (OMR.getGui() != null) {
                OMR.getGui().displayError(msg);
            } else {
                logger.warn(msg);
            }
        }
    }

    //----------//
    // logTasks //
    //----------//
    private static void logTasks (List<Callable<Void>> tasks,
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
            // First get the provided arguments if any
            cli = new CLI(WellKnowns.TOOL_NAME);
            parameters = cli.getParameters(args);

            // Interactive or Batch mode ?
            if (parameters.batchMode) {
                logger.info("Running in batch mode");

                ///System.setProperty("java.awt.headless", "true"); //TODO: Useful?
            } else {
                logger.debug("Running in interactive mode");
            }
        } catch (CmdLineException ex) {
            logger.warn("Error in command line: " + ex.getLocalizedMessage(), ex);
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
        final List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
        tasks.addAll(cli.getProjectsTasks());
        tasks.addAll(cli.getInputsTasks());
        tasks.addAll(cli.getScriptsTasks());

        if (!tasks.isEmpty()) {
            // Run all tasks in parallel or one task at a time
            if (constants.batchTasksInParallel.isSet()) {
                try {
                    logTasks(tasks, true);

                    List<Future<Void>> futures = OmrExecutors.getCachedLowExecutor().invokeAll(
                            tasks,
                            constants.processTimeOut.getValue(),
                            TimeUnit.SECONDS);
                    logger.info("Checking {} task(s)", tasks.size());

                    // Check for time-out
                    for (Future<Void> future : futures) {
                        try {
                            future.get();
                        } catch (Exception ex) {
                            logger.warn("Future exception", ex);
                            failure = true;
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("Error in processing tasks", ex);
                    failure = true;
                }
            } else {
                logTasks(tasks, false);

                for (Callable<Void> task : tasks) {
                    try {
                        Future<Void> future = OmrExecutors.getCachedLowExecutor().submit(task);

                        // Check for time-out
                        try {
                            future.get(constants.processTimeOut.getValue(), TimeUnit.SECONDS);
                        } catch (Exception ex) {
                            logger.warn("Future exception", ex);
                            failure = true;
                        }
                    } catch (Exception ex) {
                        logger.warn("Error in processing task " + task, ex);
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

        private final Constant.Boolean batchTasksInParallel = new Constant.Boolean(
                false,
                "Should we process all tasks in parallel when running in batch?");

        private final Constant.Integer processTimeOut = new Constant.Integer(
                "Seconds",
                300,
                "Process time-out, specified in seconds");

        private final Constant.Boolean closeBookOnEnd = new Constant.Boolean(
                true,
                "Should we close a book when it has been processed in batch?");
    }
}
