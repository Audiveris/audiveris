//----------------------------------------------------------------------------//
//                                                                            //
//                                  M a i n                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr;

import static omr.WellKnowns.*;

import omr.constant.Constant;
import omr.constant.ConstantManager;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.Score;

import omr.script.Script;
import omr.script.ScriptManager;

import omr.step.ProcessingCancellationException;
import omr.step.Step;
import omr.step.Stepping;
import omr.step.Steps;

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
 * Class <code>Main</code> is the main class for OMR application. It deals with
 * the main routine and its command line parameters.  It launches the User
 * Interface, unless a batch mode is selected.
 *
 * <p> The command line parameters can be (order not relevant) : <dl>
 *
 * <dt> <b>-help</b> </dt> <dd> to print a quick usage help and leave the
 * application. </dd>
 *
 * <dt> <b>-batch</b> </dt> <dd> to run in batch mode, with no user
 * interface. </dd>
 *
 * <dt> <b>-bench</b> </dt> <dd> to record bench data and save it to disk.
 * </dd>
 *
 * <dt> <b>-step (STEPNAME | &#64;STEPLIST)+</b> </dt> <dd> to run all the
 * specified steps (including the steps which are mandatory to get to the
 * specified ones). 'STEPNAME' can be any one of the step names (the case is
 * irrelevant) as defined in the {@link omr.step.Step} class. These steps will
 * be performed on each sheet referenced from the command line.</dd>
 *
 * <dt> <b>-option (KEY=VALUE | &#64;OPTIONLIST)+</b> </dt> <dd> to specify
 * the value of some application parameters (that can also be set via the
 * pull-down menu "Tools|Options"), either by stating the key=value pair or by
 * referencing (flagged by a &#64; sign) a file that lists key=value pairs (or
 * even other files list recursively). A list file is a simple text file, with
 * one key=value pair per line. <b>Nota</b>: The syntax used in the Properties
 * syntax, so for example back-slashes must be escaped.</dd>
 *
 * <dt> <b>-sheet (FILENAME | &#64;FILELIST)+</b> </dt> <dd> to specify some
 * image files to be read, either by naming the image file or by referencing
 * (flagged by a &#64; sign) a file that lists image files (or even other files
 * list recursively). A list file is a simple text file, with one image file
 * name per line.</dd>
 *
 * <dt> <b>-script (SCRIPTNAME | &#64;SCRIPTLIST)+</b> </dt> <dd> to specify
 * some scripts to be read, using the same mechanism than sheets. These script
 * files contain actions recorded during a previous run.</dd>
 *
 * </dd> </dl>
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

    /** Name of the application as displayed to the user */
    private static String toolName;

    /** Version of the application as displayed to the user */
    private static String toolVersion;

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

    //-----------------//
    // getCliConstants //
    //-----------------//
    /**
     * Report the properties set at the CLI level
     * @return the CLI-defined constant values
     */
    public static Properties getCliConstants ()
    {
        if (parameters == null) {
            return null;
        } else {
            return parameters.constants;
        }
    }

    //---------------//
    // getFilesTasks //
    //---------------//
    /**
     * Prepare the processing of image files listed on command line
     * @return the collection of proper callables
     */
    public static List<Callable<Void>> getFilesTasks ()
    {
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

        // Launch desired step on each sheet in parallel
        for (final String name : parameters.sheetNames) {
            final File file = new File(name);

            tasks.add(
                new Callable<Void>() {
                        public Void call ()
                            throws Exception
                        {
                            logger.info(
                                "Launching " + parameters.desiredSteps +
                                " on " + name);

                            if (file.exists()) {
                                final Score score = new Score(file);

                                try {
                                    Stepping.processScore(
                                        parameters.desiredSteps,
                                        score);
                                } catch (ProcessingCancellationException pce) {
                                    logger.warning("Cancelled " + score, pce);
                                    score.getBench()
                                         .recordCancellation();
                                } catch (Exception ex) {
                                    logger.warning("Exception occurred", ex);
                                } finally {
                                    // Close (when in batch mode only)
                                    if (gui == null) {
                                        score.close();
                                    }
                                }

                                return null;
                            } else {
                                logger.warning(
                                    "Could not find sheet " +
                                    file.getCanonicalPath());
                            }

                            return null;
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
     * @param gui the MainGui instance
     */
    public static void setGui (MainGui gui)
    {
        Main.gui = gui;
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

    //-----------------//
    // getScriptsTasks //
    //-----------------//
    /**
     * Prepare the processing of scripts listed on command line
     * @return the collection of proper script callables
     */
    public static List<Callable<Void>> getScriptsTasks ()
    {
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

        // Launch desired scripts in parallel
        for (String name : parameters.scriptNames) {
            final String scriptName = name;

            tasks.add(
                new Callable<Void>() {
                        public Void call ()
                            throws Exception
                        {
                            long   start = System.currentTimeMillis();
                            Script script = null;
                            File   file = new File(scriptName);
                            logger.info("Loading script file " + file + " ...");

                            try {
                                FileInputStream fis = new FileInputStream(file);
                                script = ScriptManager.getInstance()
                                                      .load(fis);
                                fis.close();
                                script.run();

                                long stop = System.currentTimeMillis();
                                logger.info(
                                    "Script file " + file + " run in " +
                                    (stop - start) + " ms");
                            } catch (ProcessingCancellationException pce) {
                                Score score = script.getScore();
                                logger.warning("Cancelled " + score, pce);

                                if (score != null) {
                                    score.getBench()
                                         .recordCancellation();
                                }
                            } catch (FileNotFoundException ex) {
                                logger.warning(
                                    "Cannot find script file " + file);
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
    // setToolBuild //
    //--------------//
    public static void setToolBuild (String toolBuild)
    {
        Main.toolBuild = toolBuild;
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

    //-------------//
    // setToolName //
    //-------------//
    public static void setToolName (String toolName)
    {
        Main.toolName = toolName;
    }

    //-------------//
    // getToolName //
    //-------------//
    /**
     * Report the name of the application as displayed to the user
     *
     * @return Name of the application
     */
    public static String getToolName ()
    {
        return toolName;
    }

    //----------------//
    // setToolVersion //
    //----------------//
    public static void setToolVersion (String toolVersion)
    {
        Main.toolVersion = toolVersion;
    }

    //----------------//
    // getToolVersion //
    //----------------//
    /**
     * Report the version of the application as displayed to the user
     *
     * @return version of the application
     */
    public static String getToolVersion ()
    {
        return toolVersion;
    }

    //--------//
    // doMain //
    //--------//
    /**
     * Specific starting method for the application.
     * @param args command line parameters
     * @see omr.Main the possible command line parameters
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
            if (logger.isFineEnabled()) {
                logger.fine("Main. Launching MainGui");
            }

            Application.launch(MainGui.class, args);
        } else {
            // For batch mode

            // Check MusicFont is loaded
            MusicFont.checkMusicFont();

            // Launch the required tasks, if any
            List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
            tasks.addAll(getFilesTasks());
            tasks.addAll(getScriptsTasks());

            if (!tasks.isEmpty()) {
                try {
                    logger.info("Submitting " + tasks.size() + " task(s)");

                    List<Future<Void>> futures = OmrExecutors.getCachedLowExecutor()
                                                             .invokeAll(
                        tasks,
                        constants.processTimeOut.getValue(),
                        TimeUnit.SECONDS);
                    logger.info("Checking " + tasks.size() + " task(s)");

                    // Check for time-out
                    for (Future<Void> future : futures) {
                        try {
                            future.get();
                        } catch (Exception ex) {
                            logger.warning("Future exception", ex);
                        }

                        if (future.isCancelled()) {
                            logger.warning("*** Cancelled future: " + future);
                        }
                    }
                } catch (Exception ex) {
                    logger.warning("Error in processing tasks", ex);
                }
            }

            // At this point all tasks have completed (normally or not)
            // So shutdown immediately the executors
            logger.info("SHUTTING DOWN ...");
            OmrExecutors.shutdown(true);

            // Store latest constant values on disk ?
            if (constants.persistBatchCliConstants.getValue()) {
                ConstantManager.getInstance()
                               .storeResource();
            }

            if (logger.isFineEnabled()) {
                logger.fine("End of main");
            }
        }
    }

    //--------------//
    // hasBenchFlag //
    //--------------//
    /**
     * Report whether the bench flag is present on the CLI
     * @return true if the bench flag appears on CLI
     */
    public static boolean hasBenchFlag ()
    {
        return parameters.benchFlag;
    }

    //-------------//
    // checkLocale //
    //-------------//
    private static void checkLocale ()
    {
        final String country = constants.localeCountry.getValue();

        if (!country.equals("")) {
            for (Locale locale : Locale.getAvailableLocales()) {
                if (locale.getCountry()
                          .equals(country)) {
                    Locale.setDefault(locale);

                    return;
                }
            }

            logger.info("Cannot set locale country to " + country);
        }
    }

    //------------//
    // initialize //
    //------------//
    private static void initialize ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("homeFolder=" + HOME_FOLDER);
            logger.fine("classContainer=" + CLASS_CONTAINER);
            logger.fine(
                "classContainer.isDirectory=" + CLASS_CONTAINER.isDirectory());
        }

        // Tool name
        final Package thisPackage = Main.class.getPackage();
        toolName = thisPackage.getSpecificationTitle();

        // Tool version
        toolVersion = thisPackage.getSpecificationVersion();

        // Tool build
        toolBuild = thisPackage.getImplementationVersion();
    }

    //---------//
    // process //
    //---------//
    private static void process (String[] args)
    {
        // First get the provided arguments if any
        parameters = new CLI(toolName, args).getParameters();

        if (parameters == null) {
            logger.warning("Exiting ...");

            // Stop the JVM ????
            Runtime.getRuntime()
                   .exit(1);
        }

        // Interactive or Batch mode ?
        if (parameters.batchMode) {
            logger.info("Running in batch mode");

            ///System.setProperty("java.awt.headless", "true");

            // Check MIDI output is not asked for
            Step midiStep = Steps.valueOf(Steps.MIDI);

            if ((midiStep != null) &&
                parameters.desiredSteps.contains(midiStep)) {
                logger.warning(
                    "MIDI output is not compatible with -batch mode." +
                    " MIDI output is ignored.");
                parameters.desiredSteps.remove(midiStep);
            }
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

        /** Selection of locale country code (2 letters), or empty */
        private final Constant.String localeCountry = new Constant.String(
            "",
            "Locale country to be used in the whole application (US, FR, ...)");

        /** "Should we persist CLI-defined constants when running in batch? */
        private final Constant.Boolean persistBatchCliConstants = new Constant.Boolean(
            false,
            "Should we persist CLI-defined constants when running in batch?");

        /** Process time-out, specified in seconds */
        private final Constant.Integer processTimeOut = new Constant.Integer(
            "Seconds",
            300,
            "Process time-out, specified in seconds");
    }
}
