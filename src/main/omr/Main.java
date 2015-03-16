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

import omr.script.ExportTask;
import omr.script.PrintTask;
import omr.script.ScriptManager;

import omr.sheet.Book;

import omr.step.ProcessingCancellationException;
import omr.step.Step;
import omr.step.Stepping;

import omr.ui.MainGui;
import omr.ui.symbol.MusicFont;

import omr.util.ClassUtil;
import omr.util.Clock;
import omr.util.Dumping;
import omr.util.OmrExecutors;

import org.jdesktop.application.Application;

import org.kohsuke.args4j.CmdLineException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
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
        /** Time stamp */
        Clock.resetTime();
    }

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final Constants constants = new Constants();

    /** Master View */
    private static MainGui gui;

    /** Parameters read from CLI */
    private static Parameters parameters;

    /** The application dumping service */
    public static final Dumping dumping = new Dumping(Main.class.getPackage());

    //~ Constructors -------------------------------------------------------------------------------
    private Main ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // doMain //
    //--------//
    /**
     * Specific starting method for the application.
     *
     * @param args command line parameters
     * @see omr.CLI the possible command line parameters
     */
    public static void doMain (String[] args)
    {
        // Initialize tool parameters
        initialize();

        // Process CLI arguments
        process(args);

        // Locale to be used in the whole application?
        checkLocale();

        // Environment
        showEnvironment();

        // Native libs
        loadNativeLibraries();

        if (!parameters.batchMode) {
            // For interactive mode
            logger.debug("Main. Launching MainGui");
            Application.launch(MainGui.class, args);
        } else {
            // For batch mode

            // Remember if at least one task failed
            boolean failure = false;

            // Check MusicFont is loaded
            MusicFont.checkMusicFont();

            // Launch the required tasks, if any
            List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
            tasks.addAll(getFilesTasks());
            tasks.addAll(getScriptsTasks());

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

    //----------------//
    // getBenchFolder //
    //----------------//
    /**
     * Report the bench folder if present on the CLI
     *
     * @return the CLI bench folder, or null
     */
    public static Path getBenchFolder ()
    {
        if (parameters.benchFolder == null) {
            return null;
        }

        return parameters.benchFolder.toPath();
    }

    //-----------------//
    // getCliConstants //
    //-----------------//
    /**
     * Report the properties set at the CLI level
     *
     * @return the CLI-defined constant values
     */
    public static Properties getCliConstants ()
    {
        if (parameters == null) {
            return null;
        } else {
            return parameters.options;
        }
    }

    //-----------------//
    // getExportFolder //
    //-----------------//
    /**
     * Report the export folder if present on the CLI
     *
     * @return the CLI export path, or null
     */
    public static Path getExportFolder ()
    {
        if (parameters.exportFolder == null) {
            return null;
        }

        return parameters.exportFolder.toPath();
    }

    //---------------//
    // getFilesTasks //
    //---------------//
    /**
     * Prepare the processing of image files listed on command line.
     * On each input file, we apply the actions specified if any via -step, -print, -export.
     *
     * @return the collection of proper callable instances
     */
    public static List<Callable<Void>> getFilesTasks ()
    {
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

        // Launch desired step on each book
        for (final File input : parameters.inputFiles) {
            final Path path = input.toPath();

            tasks.add(
                    new Callable<Void>()
                    {
                        @Override
                        public Void call ()
                        throws Exception
                        {
                            final Set<Step> steps = parameters.getSteps();
                            final SortedSet<Integer> pages = parameters.getPageIds();

                            if (!steps.isEmpty()) {
                                logger.info(
                                        "Launching {} on {} {}",
                                        steps,
                                        input,
                                        (!pages.isEmpty()) ? ("pages " + pages) : "");
                            }

                            if (Files.exists(path)) {
                                final Book book = new Book(path);

                                try {
                                    // Create book sheets and perform desired steps if any
                                    Stepping.processBook(steps, pages, book);

                                    // Book print output?
                                    if (parameters.print || (parameters.printFolder != null)) {
                                        logger.debug("Print output");
                                        new PrintTask(parameters.printFolder).core(
                                                book.getFirstSheet());
                                    }

                                    // Book export output?
                                    if (parameters.export || (parameters.exportFolder != null)) {
                                        logger.debug("Export output");
                                        new ExportTask(parameters.exportFolder).core(
                                                book.getFirstSheet());
                                    }
                                } catch (ProcessingCancellationException pce) {
                                    logger.warn("Cancelled " + book, pce);
                                    book.getBench().recordCancellation();
                                    throw pce;
                                } catch (Throwable ex) {
                                    logger.warn("Exception occurred", ex);
                                    throw ex;
                                } finally {
                                    // Close (when in batch mode only)
                                    if (gui == null) {
                                        book.close();
                                    }
                                }

                                return null;
                            } else {
                                String msg = "Could not find file " + path;
                                logger.warn(msg);
                                throw new RuntimeException(msg);
                            }
                        }

                        @Override
                        public String toString ()
                        {
                            return "Input " + path;
                        }
                    });
        }

        return tasks;
    }

    //--------//
    // getGui //
    //--------//
    /**
     * Points to the single instance of the User Interface, if any.
     *
     * @return MainGui instance, which may be null
     */
    public static MainGui getGui ()
    {
        return gui;
    }

    //-------------//
    // getPagesIds //
    //-------------//
    /**
     * Report the set of page ids if present on the CLI
     *
     * @return the CLI page ids, or null
     */
    public static SortedSet<Integer> getPageIds ()
    {
        return parameters.getPageIds();
    }

    //----------------//
    // getPrintFolder //
    //----------------//
    /**
     * Report the print folder if present on the CLI
     *
     * @return the CLI print path, or null
     */
    public static Path getPrintFolder ()
    {
        if (parameters.printFolder == null) {
            return null;
        }

        return parameters.printFolder.toPath();
    }

    //-----------------//
    // getScriptsTasks //
    //-----------------//
    /**
     * Prepare the processing of scripts listed on command line
     *
     * @return the collection of proper script callables
     */
    public static List<Callable<Void>> getScriptsTasks ()
    {
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

        // Launch desired scripts in parallel
        for (File script : parameters.scriptFiles) {
            tasks.add(
                    new Callable<Void>()
                    {
                        @Override
                        public Void call ()
                        throws Exception
                        {
                            ScriptManager.getInstance().loadAndRun(script);

                            return null;
                        }

                        @Override
                        public String toString ()
                        {
                            return "Script " + script;
                        }
                    });
        }

        return tasks;
    }

    //--------//
    // setGui //
    //--------//
    /**
     * Register the GUI (done by the GUI itself when it is ready)
     *
     * @param gui the MainGui instance
     */
    public static void setGui (MainGui gui)
    {
        Main.gui = gui;
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

            if (Main.getGui() != null) {
                Main.getGui().displayError(msg);
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

    //---------//
    // process //
    //---------//
    private static void process (String[] args)
    {
        try {
            // First get the provided arguments if any
            parameters = new CLI(WellKnowns.TOOL_NAME).getParameters(args);

            // Interactive or Batch mode ?
            if (parameters.batchMode) {
                logger.info("Running in batch mode");

                ///System.setProperty("java.awt.headless", "true");
                //            // Check MIDI output is not asked for
                //            Step midiStep = Steps.valueOf(Steps.MIDI);
                //
                //            if ((midiStep != null) &&
                //                parameters.steps.contains(midiStep)) {
                //                logger.warn(
                //                    "MIDI output is not compatible with -batch mode." +
                //                    " MIDI output is ignored.");
                //                parameters.steps.remove(midiStep);
                //            }
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
                60, //300,
                "Process time-out, specified in seconds");
    }
}
