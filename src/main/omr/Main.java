//----------------------------------------------------------------------------//
//                                                                            //
//                                  M a i n                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.visitor.ScoreExporter;

import omr.script.Script;
import omr.script.ScriptManager;

import omr.step.Step;

import omr.ui.MainGui;
import omr.ui.util.UILookAndFeel;

import omr.util.Clock;
import omr.util.Implement;
import omr.util.JaiLoader;
import omr.util.Logger;
import omr.util.OmrExecutors;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;

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
 * <dt> <b>-step STEPNAME</b> </dt> <dd> to run till the specified
 * step. 'STEPNAME' can be any one of the step names (the case is irrelevant) as
 * defined in the {@link omr.step.Step} class. This step will be performed on
 * each sheet referenced from the command line.</dd>
 *
 * <dt> <b>-sheet (SHEETNAME | &#64;SHEETLIST)+</b> </dt> <dd> to specify some
 * sheets to be read, either by naming the image file or by referencing (flagged
 * by a &#64; sign) a file that lists image files (or even other files list
 * recursively). A list file is a simple text file, with one image file name per
 * line.</dd>
 *
 * <dt> <b>-script (SCRIPTNAME | &#64;SCRIPTLIST)+</b> </dt> <dd> to specify some
 * scripts to be read, using the same mechanism than sheets. These script files
 * contain actions recorded during a previous run.</dd>
 *
 * </dd> </dl>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Main
{
    //~ Static fields/initializers ---------------------------------------------

    static {
        // Time stamps
        Clock.resetTime();
    }

    /** Classes container */
    private static File container = new File(
        Main.class.getProtectionDomain().getCodeSource().getLocation().getFile());

    /** Installation folder (needs to be initialized before logger) */
    // .../build/classes
    // .../dist/audiveris.jar
    // .../bin/audiveris.jar
    private static File homeFolder = container.getParentFile()
                                              .getParentFile();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Main.class);

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Singleton */
    private static Main INSTANCE;

    /** Specific folder name for icons */
    public static final String ICONS_NAME = "icons";

    /** Tells if using Mac OS X for special GUI functionality */
    public static final boolean MAC_OS_X = System.getProperty("os.name")
                                                 .toLowerCase()
                                                 .startsWith("mac os x");

    //~ Instance fields --------------------------------------------------------

    /** Build reference of the application as displayed to the user */
    private final String toolBuild;

    /** Name of the application as displayed to the user */
    private final String toolName;

    /** Version of the application as displayed to the user */
    private final String toolVersion;

    /** Master View */
    private MainGui gui;

    /** List of script file names to process */
    private List<String> scriptNames = new ArrayList<String>();

    /** List of sheet file names to process */
    private List<String> sheetNames = new ArrayList<String>();

    /** Target step, LOAD by default */
    private Step targetStep = Step.LOAD;

    /** Batch mode if any */
    private boolean batchMode = false;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Main //
    //------//
    private Main ()
    {
        // Locale  to be used in the whole application ?
        checkLocale();

        // Tool name
        final Package thisPackage = Main.class.getPackage();
        final String  name = thisPackage.getSpecificationTitle();

        if (name != null) {
            toolName = name;
            constants.toolName.setValue(name);
        } else {
            toolName = constants.toolName.getValue();
        }

        // Tool version
        final String version = thisPackage.getSpecificationVersion();

        if (version != null) {
            toolVersion = version;
            constants.toolVersion.setValue(version);
        } else {
            toolVersion = constants.toolVersion.getValue();
        }

        // Tool build
        toolBuild = thisPackage.getImplementationVersion();
    }

    //~ Methods ----------------------------------------------------------------

    //---------------------//
    // getClassesContainer //
    //---------------------//
    /**
     * Report the container from which the application classes were loaded
     * @return either the jar file, or the directory of .class files
     */
    public static File getClassesContainer ()
    {
        return container;
    }

    //-----------------//
    // getConfigFolder //
    //-----------------//
    /**
     * Report the folder where config parameters are stored
     *
     * @return the directory for configuration files
     */
    public static File getConfigFolder ()
    {
        return new File(getHomeFolder(), "config");
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
        if (INSTANCE == null) {
            return null;
        } else {
            return INSTANCE.gui;
        }
    }

    //----------------//
    // getIconsFolder //
    //----------------//
    /**
     * Report the folder where custom-defined icons are stored
     *
     * @return the directory for icon files
     */
    public static File getIconsFolder ()
    {
        return new File(getHomeFolder(), ICONS_NAME);
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
        return INSTANCE.toolBuild;
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
        return INSTANCE.toolName;
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
        return INSTANCE.toolVersion;
    }

    //----------------//
    // getTrainFolder //
    //----------------//
    /**
     * Report the folder defined for training files
     *
     * @return the directory for training material
     */
    public static File getTrainFolder ()
    {
        return new File(getHomeFolder(), "train");
    }

    //------//
    // main //
    //------//
    /**
     * Specific starting method for the application.
     *
     * @param args the command line parameters
     *
     * @see omr.Main the possible command line parameters
     */
    public static void main (String[] args)
    {
        // Problem, from Emacs all args are passed in one string sequence.  We
        // recognize this by detecting a single argument starting with '-'
        if ((args.length == 1) && (args[0].startsWith("-"))) {
            // Redispatch the real args
            StringTokenizer st = new StringTokenizer(args[0]);
            int             argNb = 0;

            // First just count the number of real arguments
            while (st.hasMoreTokens()) {
                argNb++;
                st.nextToken();
            }

            String[] newArgs = new String[argNb];

            // Second copy all real arguments into newly
            // allocated array
            argNb = 0;
            st = new StringTokenizer(args[0]);

            while (st.hasMoreTokens()) {
                newArgs[argNb++] = st.nextToken();
            }

            // Fake the args
            args = newArgs;
        }

        // Launch the processing
        INSTANCE = new Main();

        if (logger.isFineEnabled()) {
            logger.fine("homeFolder=" + homeFolder);
            logger.fine("container=" + container);
            logger.fine("container.isDirectory=" + container.isDirectory());
        }

        try {
            INSTANCE.process(args);
        } catch (Main.StopRequired ex) {
            logger.info("Exiting.");
        }
    }

    //---------------//
    // getHomeFolder //
    //---------------//
    private static File getHomeFolder ()
    {
        return homeFolder;
    }

    //----------//
    // getTasks //
    //----------//
    private Collection getTasks ()
    {
        List<Callable> callables = new ArrayList<Callable>();

        // Browse desired sheets in parallel
        for (String name : sheetNames) {
            final File file = new File(name);

            // Perform desired step on each sheet in parallel
            callables.add(
                Executors.callable(
                    new Runnable() {
                            public void run ()
                            {
                                targetStep.performSerial(null, file);
                            }
                        }));
        }

        // Browse desired scripts in parallel
        for (String name : scriptNames) {
            // Run each script in parallel
            final String scriptName = name;
            callables.add(
                Executors.callable(
                    new Runnable() {
                            public void run ()
                            {
                                long start = System.currentTimeMillis();
                                File file = new File(scriptName);
                                logger.info(
                                    "Loading script file " + file + " ...");

                                try {
                                    final Script script = ScriptManager.getInstance()
                                                                       .load(
                                        new FileInputStream(file));
                                    script.run();

                                    long stop = System.currentTimeMillis();
                                    logger.info(
                                        "Script file " + file + " run in " +
                                        (stop - start) + " ms");
                                } catch (FileNotFoundException ex) {
                                    logger.warning(
                                        "Cannot find script file " + file);
                                }
                            }
                        }));
        }

        return callables;
    }

    //--------//
    // addRef //
    //--------//
    private void addRef (String       ref,
                         List<String> list)
    {
        // The ref may be a plain file name or the name of a pack that lists
        // ref(s). This is signalled by a starting '@' character in ref
        if (ref.startsWith("@")) {
            // File with other refs inside
            String pack = ref.substring(1);

            try {
                BufferedReader br = new BufferedReader(new FileReader(pack));
                String         newRef;

                try {
                    while ((newRef = br.readLine()) != null) {
                        addRef(newRef.trim(), list);
                    }

                    br.close();
                } catch (IOException ex) {
                    logger.warning(
                        "IO error while reading file '" + pack + "'");
                }
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find file '" + pack + "'");
            }
        } else
        // Plain file name
        if (ref.length() > 0) {
            list.add(ref);
        }
    }

    //-------------//
    // checkLocale //
    //-------------//
    private void checkLocale ()
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

    //----------------//
    // parseArguments //
    //----------------//
    private void parseArguments (final String[] args)
        throws StopRequired
    {
        // Status of the finite state machine
        final int STEP = 0;
        final int SHEET = 1;
        final int SCRIPT = 2;
        boolean   paramNeeded = false; // Are we expecting a param?
        int       status = SHEET; // By default
        String    currentCommand = null;

        // Parse all arguments from command line
        for (int i = 0; i < args.length; i++) {
            String token = args[i];

            if (token.startsWith("-")) {
                // This is a command
                // Check that we were not expecting param(s)
                if (paramNeeded) {
                    printCommandLine(args);
                    stopUsage(
                        "Found no parameter after command '" + currentCommand +
                        "'");
                }

                if (token.equalsIgnoreCase("-help")) {
                    stopUsage(null);
                } else if (token.equalsIgnoreCase("-batch")) {
                    batchMode = true;
                    paramNeeded = false;
                } else if (token.equalsIgnoreCase("-step")) {
                    status = STEP;
                    paramNeeded = true;
                } else if (token.equalsIgnoreCase("-sheet")) {
                    status = SHEET;
                    paramNeeded = true;
                } else if (token.equalsIgnoreCase("-script")) {
                    status = SCRIPT;
                    paramNeeded = true;
                } else {
                    printCommandLine(args);
                    stopUsage("Unknown command '" + token + "'");
                }

                // Remember the current command
                currentCommand = token;
            } else {
                // This is a parameter
                switch (status) {
                case STEP :
                    // Read a step name
                    targetStep = Step.valueOf(token.toUpperCase());

                    if (targetStep == null) {
                        printCommandLine(args);
                        stopUsage(
                            "Step name expected, found '" + token +
                            "' instead");
                    }

                    // By default, sheets are now expected
                    status = SHEET;
                    paramNeeded = false;

                    break;

                case SHEET :
                    addRef(token, sheetNames);
                    paramNeeded = false;

                    break;

                case SCRIPT :
                    addRef(token, scriptNames);
                    paramNeeded = false;

                    break;
                }
            }
        }

        // Additional error checking
        if (paramNeeded) {
            printCommandLine(args);
            stopUsage(
                "Expecting a parameter after command '" + currentCommand + "'");
        }

        // Results
        if (logger.isFineEnabled()) {
            logger.fine("batchMode=" + batchMode);
            logger.fine("targetStep=" + targetStep);
            logger.fine("sheetNames=" + sheetNames);
            logger.fine("scriptNames=" + scriptNames);
        }
    }

    //------------------//
    // printCommandLine //
    //------------------//
    private void printCommandLine (String[] args)
    {
        System.out.println("\nCommandParameters:");

        for (String arg : args) {
            System.out.print(" " + arg);
        }

        System.out.println();
    }

    //---------//
    // process //
    //---------//
    private void process (String[] args)
        throws StopRequired
    {
        // First parse the provided arguments if any
        parseArguments(args);

        // Then, preload the JAI class so image operations are ready
        JaiLoader.preload();

        // Interactive or Batch mode ?
        if (batchMode) {
            logger.info("Running in batch mode");
        } else {
            logger.fine("Running in interactive mode");

            // UI Look and Feel
            UILookAndFeel.setUI(null);

            // Make sure we have nice window decorations.
            JFrame.setDefaultLookAndFeelDecorated(true);

            // Launch the GUI
            gui = new MainGui();

            // Background task : JaxbContext
            OmrExecutors.getLowExecutor()
                        .execute(
                new Runnable() {
                        @Implement(Runnable.class)
                        public void run ()
                        {
                            ScoreExporter.preloadJaxbContext();
                        }
                    });
        }

        // Perform sheet and script actions
        if ((sheetNames.size() > 0) || (scriptNames.size() > 0)) {
            try {
                OmrExecutors.getLowExecutor()
                            .invokeAll(getTasks());
            } catch (InterruptedException ex) {
                logger.warning("Error while running sheets & scripts", ex);
            }
        }

        // Batch closing
        if (batchMode) {
            OmrExecutors.shutdown();
        }
    }

    //-----------//
    // stopUsage //
    //-----------//
    private void stopUsage (String msg)
        throws StopRequired
    {
        // Print message if any
        if (msg != null) {
            logger.warning(msg);
        }

        StringBuffer buf = new StringBuffer(1024);

        // Print standard command line syntax
        buf.append("usage: java ")
           .append(getToolName())
           .append(" [-help]")
           .append(" [-batch]")
           .append(" [-step STEPNAME]")
           .append(" [-sheet (SHEETNAME|@SHEETLIST)+]")
           .append(" [-script (SCRIPTNAME|@SCRIPTLIST)+]");

        // Print all allowed step names
        buf.append("\n      Known step names are in order")
           .append(" (non case-sensitive) :");

        for (Step step : Step.values()) {
            buf.append(
                String.format(
                    "%n%-11s : %s",
                    step.toString().toUpperCase(),
                    step.getDescription()));
        }

        logger.info(buf.toString());

        // Stop application immediately
        throw new StopRequired();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /** Selection of locale country code (2 letters), or empty */
        Constant.String localeCountry = new Constant.String(
            "US",
            "Locale country to be used in the whole application (US, FR, ...)");

        /** Utility constant */
        Constant.String toolName = new Constant.String(
            "Audiveris",
            "* DO NOT EDIT * - Name of this application");

        /** Utility constant */
        Constant.String toolVersion = new Constant.String(
            "",
            "* DO NOT EDIT * - Version of this application");
    }

    //--------------//
    // StopRequired //
    //--------------//
    private static class StopRequired
        extends Exception
    {
    }
}
