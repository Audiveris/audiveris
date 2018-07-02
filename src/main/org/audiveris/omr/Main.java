//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            M a i n                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr;

import org.audiveris.omr.CLI.CliTask;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantManager;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.text.tesseract.TesseractOCR;
import org.audiveris.omr.ui.MainGui;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.util.OmrExecutors;

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
 * It launches the User Interface, unless batch mode is selected.
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
     * @see org.audiveris.omr.CLI the possible command line parameters
     */
    public static void main (String[] args)
    {
        // Log files
        LogUtil.addFileAppender();

        // Locale to be used in the whole application?
        checkLocale();

        // Environment
        showEnvironment();

        // Process CLI parameters
        processCli(args);

        // Help?
        if (cli.isHelpMode()) {
            cli.printUsage();

            return;
        }

        // Initialize tool parameters
        initialize();

        // Engine
        OMR.engine = BookManager.getInstance();

        if (!cli.isBatchMode()) {
            logger.debug("Running in interactive mode");
            LogUtil.addGuiAppender();
            logger.debug("Main. Launching MainGui");
            Application.launch(MainGui.class, args);
        } else {
            ///System.setProperty("java.awt.headless", "true"); //TODO: Useful?
            logger.info("Running in batch mode");

            // Check MusicFont is loaded
            MusicFont.checkMusicFont();

            // Run the required tasks, if any (and remember if at least one task failed)
            boolean failure = runBatchTasks();

            // At this point all tasks have completed (except timeout...)
            // So shutdown gracefully the executors
            boolean timeout = !OmrExecutors.shutdown();

            // Save global sample repository if modified
            if (SampleRepository.hasInstance()) {
                SampleRepository repository = SampleRepository.getGlobalInstance(false);

                if (repository.isModified()) {
                    repository.storeRepository();
                }
            }

            // Store latest constant values on disk?
            if (constants.persistBatchCliConstants.getValue()) {
                ConstantManager.getInstance().storeResource();
            }

            // Force JVM exit?
            if (failure || timeout) {
                String msg = "Exit forced.";
                int status = 0;

                if (failure) {
                    status -= 1;
                    msg += " Failure";
                }

                if (timeout) {
                    status -= 2;
                    msg += " Timeout";
                }

                logger.warn(msg);
                System.exit(status);
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
            cli.parseParameters(args);
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
                    + "- Architecture: {}\n" + "- Java VM:      {}\n" + "- OCR Engine:   {}",
                    WellKnowns.TOOL_REF + ":" + WellKnowns.TOOL_BUILD,
                    System.getProperty("os.name") + " " + System.getProperty("os.version"),
                    System.getProperty("os.arch"),
                    System.getProperty("java.vm.name") + " (build "
                    + System.getProperty("java.vm.version") + ", " + System.getProperty("java.vm.info")
                    + ")",
                    TesseractOCR.getInstance().identify());
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
    }
}
