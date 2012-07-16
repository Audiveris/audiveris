//----------------------------------------------------------------------------//
//                                                                            //
//                                  M a i n                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.constant.Constant;
import omr.constant.ConstantManager;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.Score;

import omr.script.Script;
import omr.script.ScriptManager;

import omr.step.ProcessingCancellationException;
import omr.step.Stepping;

import omr.ui.MainGui;
import omr.ui.symbol.MusicFont;

import omr.util.Clock;
import omr.util.Dumping;
import omr.util.OmrExecutors;

import org.jdesktop.application.Application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

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
    //~ Static fields/initializers ---------------------------------------------

    static {
        /** Time stamp */
        Clock.resetTime();
    }

    /** Build reference of the application as displayed to the user */
    private static String toolBuild;

    /** Master View */
    private static MainGui gui;

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Main.class);

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Parameters read from CLI */
    private static CLI.Parameters parameters;

    /** The application dumping service */
    public static final Dumping dumping = new Dumping(Main.class.getPackage());

    //~ Constructors -----------------------------------------------------------
    //------//
    // Main //
    //------//
    private Main ()
    {
    }

    //~ Methods ----------------------------------------------------------------
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

        // Locale to be used in the whole application ?
        checkLocale();

        if (!parameters.batchMode) {
            // For interactive mode
            logger.fine("Main. Launching MainGui");
            Application.launch(MainGui.class, args);
        } else {
            // For batch mode

            // Remember if at least one task has failed
            boolean failure = false;

            // Check MusicFont is loaded
            MusicFont.checkMusicFont();

            // Launch the required tasks, if any
            List<Callable<Void>> tasks = new ArrayList<>();
            tasks.addAll(getFilesTasks());
            tasks.addAll(getScriptsTasks());

            if (!tasks.isEmpty()) {
                try {
                    logger.info("Submitting {0} task(s)", tasks.size());

                    List<Future<Void>> futures = OmrExecutors.
                            getCachedLowExecutor().invokeAll(
                            tasks,
                            constants.processTimeOut.getValue(),
                            TimeUnit.SECONDS);
                    logger.info("Checking {0} task(s)", tasks.size());

                    // Check for time-out
                    for (Future<Void> future : futures) {
                        try {
                            future.get();
                        } catch (Exception ex) {
                            logger.warning("Future exception", ex);
                            failure = true;
                        }
                    }
                } catch (Exception ex) {
                    logger.warning("Error in processing tasks", ex);
                    failure = true;
                }
            }

            // At this point all tasks have completed (normally or not)
            // So shutdown immediately the executors
            logger.info("SHUTTING DOWN ...");
//            OmrExecutors.shutdown(true);

            // Store latest constant values on disk?
            if (constants.persistBatchCliConstants.getValue()) {
                ConstantManager.getInstance().storeResource();
            }

            // Stop the JVM with failure status?
            if (failure) {
                logger.warning("Exit with failure status");
                System.exit(-1);
            }
        }
    }

    //--------------//
    // getBenchPath //
    //--------------//
    /**
     * Report the bench path if present on the CLI
     *
     * @return the CLI bench path, or null
     */
    public static String getBenchPath ()
    {
        return parameters.benchPath;
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

    //---------------//
    // getExportPath //
    //---------------//
    /**
     * Report the export path if present on the CLI
     *
     * @return the CLI export path, or null
     */
    public static String getExportPath ()
    {
        return parameters.exportPath;
    }

    //---------------//
    // getFilesTasks //
    //---------------//
    /**
     * Prepare the processing of image files listed on command line
     *
     * @return the collection of proper callables
     */
    public static List<Callable<Void>> getFilesTasks ()
    {
        List<Callable<Void>> tasks = new ArrayList<>();

        // Launch desired step on each score in parallel
        for (final String name : parameters.inputNames) {
            final File file = new File(name);

            tasks.add(
                    new Callable<Void>()
                    {

                        @Override
                        public Void call ()
                                throws Exception
                        {
                            logger.info(
                                    "Launching {0} on {1}",
                                    new Object[]{parameters.desiredSteps, name});

                            if (file.exists()) {
                                final Score score = new Score(file);

                                try {
                                    Stepping.processScore(
                                            parameters.desiredSteps,
                                            score);
                                } catch (ProcessingCancellationException pce) {
                                    logger.warning("Cancelled " + score, pce);
                                    score.getBench().recordCancellation();
                                    throw pce;
                                } catch (Exception ex) {
                                    logger.warning("Exception occurred", ex);
                                    throw ex;
                                } finally {
                                    // Close (when in batch mode only)
                                    if (gui == null) {
                                        score.close();
                                    }

                                    return null;
                                }
                            } else {
                                String msg = "Could not find file "
                                        + file.getCanonicalPath();
                                logger.warning(msg);
                                throw new RuntimeException(msg);
                            }
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
    // getMidiPath //
    //-------------//
    /**
     * Report the midi path if present on the CLI
     *
     * @return the CLI midi path, or null
     */
    public static String getMidiPath ()
    {
        return parameters.midiPath;
    }

    //--------------//
    // getPrintPath //
    //--------------//
    /**
     * Report the print path if present on the CLI
     *
     * @return the CLI print path, or null
     */
    public static String getPrintPath ()
    {
        return parameters.printPath;
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
        List<Callable<Void>> tasks = new ArrayList<>();

        // Launch desired scripts in parallel
        for (String name : parameters.scriptNames) {
            final String scriptName = name;

            tasks.add(
                    new Callable<Void>()
                    {

                        @Override
                        public Void call ()
                                throws Exception
                        {
                            long start = System.currentTimeMillis();
                            Script script = null;
                            File file = new File(scriptName);
                            logger.info("Loading script file {0} ...", file);

                            try {
                                FileInputStream fis = new FileInputStream(file);
                                script = ScriptManager.getInstance().load(fis);
                                fis.close();
                                script.run();

                                long stop = System.currentTimeMillis();
                                logger.info(
                                        "Script file {0} run in {1} ms",
                                        new Object[]{file, stop - start});
                            } catch (ProcessingCancellationException pce) {
                                Score score = script.getScore();
                                logger.warning("Cancelled " + score, pce);

                                if (score != null) {
                                    score.getBench().recordCancellation();
                                }
                            } catch (FileNotFoundException ex) {
                                logger.warning(
                                        "Cannot find script file {0}",
                                        file);
                            } catch (Exception ex) {
                                logger.warning("Exception occurred", ex);
                            } finally {
                                // Close when in batch mode
                                if ((gui == null) && (script != null)) {
                                    Score score = script.getScore();

                                    if (score != null) {
                                        score.close();
                                    }
                                }
                            }

                            return null;
                        }
                    });
        }

        return tasks;
    }

    //--------------//
    // getToolBuild //
    //--------------//
    /**
     * Report the build reference of the application as displayed to the user
     *
     * @return Build reference of the application
     */
    public static String getToolBuild ()
    {
        return toolBuild;
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

    //--------------//
    // setToolBuild //
    //--------------//
    public static void setToolBuild (String toolBuild)
    {
        Main.toolBuild = toolBuild;
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
                    logger.fine("Locale set to {0}", locale);
                    return;
                }
            }

            logger.warning("Cannot set locale to {0}", localeStr);
        }
    }

    //------------//
    // initialize //
    //------------//
    private static void initialize ()
    {
        // Tool build
        final Package thisPackage = Main.class.getPackage();
        toolBuild = thisPackage.getImplementationVersion();
    }

    //---------//
    // process //
    //---------//
    private static void process (String[] args)
    {
        // First get the provided arguments if any
        parameters = new CLI(WellKnowns.TOOL_NAME, args).getParameters();

        if (parameters == null) {
            logger.warning("Exiting ...");

            // Stop the JVM, with failure status (1)
            Runtime.getRuntime().exit(1);
        }

        // Interactive or Batch mode ?
        if (parameters.batchMode) {
            logger.info("Running in batch mode");

            ///System.setProperty("java.awt.headless", "true");

            //            // Check MIDI output is not asked for
            //            Step midiStep = Steps.valueOf(Steps.MIDI);
            //
            //            if ((midiStep != null) &&
            //                parameters.desiredSteps.contains(midiStep)) {
            //                logger.warning(
            //                    "MIDI output is not compatible with -batch mode." +
            //                    " MIDI output is ignored.");
            //                parameters.desiredSteps.remove(midiStep);
            //            }
        } else {
            logger.fine("Running in interactive mode");

            // Make sure we have nice window decorations.
            JFrame.setDefaultLookAndFeelDecorated(true);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        private final Constant.String locale = new Constant.String(
                "en",
                "Locale language to be used in the whole application (en, fr)");

        private final Constant.Boolean persistBatchCliConstants = new Constant.Boolean(
                false,
                "Should we persist CLI-defined constants when running in batch?");

        private final Constant.Integer processTimeOut = new Constant.Integer(
                "Seconds",
                300,
                "Process time-out, specified in seconds");
    }
}
