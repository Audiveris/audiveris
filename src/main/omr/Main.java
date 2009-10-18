//----------------------------------------------------------------------------//
//                                                                            //
//                                  M a i n                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.script.Script;
import omr.script.ScriptManager;

import omr.sheet.Sheet;

import omr.ui.MainGui;

import omr.util.Clock;
import omr.util.OmrExecutors;
import omr.util.Worker;

import org.jdesktop.application.Application;

import java.io.*;
import java.util.*;

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
 * <dt> <b>-script (SCRIPTNAME | &#64;SCRIPTLIST)+</b> </dt> <dd> to specify
 * some scripts to be read, using the same mechanism than sheets. These script
 * files contain actions recorded during a previous run.</dd>
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
        /** Time stamp */
        Clock.resetTime();
    }

    /** Classes container (either classes directory or jar archive */
    private static File classContainer;

    /** Installation folder */
    private static File homeFolder;

    /** Config folder */
    private static File configFolder;

    /** Specific folder name for icons */
    private static final String ICONS_FOLDER_NAME = "icons";

    /** Specific folder name for OCR utility */
    private static final String OCR_FOLDER_NAME = "ocr";

    /** Specific folder name for documentation */
    private static final String DOC_FOLDER_NAME = "www";

    /** Specific folder name for training data */
    private static final String TRAIN_FOLDER_NAME = "train";

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

    /** Tells if using Linux */
    public static final boolean LINUX = isLinux();

    /** Tells if using Mac OS X */
    public static final boolean MAC_OS_X = isMac();

    /** Tells if using Windows */
    public static final boolean WINDOWS = isWindows();

    /** Parameters read from CLI */
    private static CLI.Parameters parameters;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Main //
    //------//
    private Main ()
    {
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
        return classContainer;
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
        return configFolder;
    }

    //------------------------//
    // getDocumentationFolder //
    //------------------------//
    /**
     * Report the folder where documentations files are stored
     *
     * @return the directory for documentation files
     */
    public static File getDocumentationFolder ()
    {
        return new File(homeFolder, DOC_FOLDER_NAME);
    }

    //--------//
    // setGui //
    //--------//
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

    //---------------//
    // getHomeFolder //
    //---------------//
    /**
     * Report the location where Audiveris tool is installed
     * @return Audiveris home folder
     */
    public static File getHomeFolder ()
    {
        return homeFolder;
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
        return new File(homeFolder, ICONS_FOLDER_NAME);
    }

    //--------------//
    // getOcrFolder //
    //--------------//
    /**
     * Report the folder where Tesseract OCR material is stored
     *
     * @return the directory for tesseract / tesjeract files
     */
    public static File getOcrFolder ()
    {
        return new File(homeFolder, OCR_FOLDER_NAME);
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
        return new File(homeFolder, TRAIN_FOLDER_NAME);
    }

    //---------------//
    // launchScripts //
    //---------------//
    /**
     * Launch the processing of scripts listed on command line
     */
    public static void launchScripts ()
    {
        // Launch desired scripts in parallel
        for (String name : parameters.scriptNames) {
            final String scriptName = name;

            try {
                Worker<Void> task = new Worker<Void>() {
                    @Override
                    public Void construct ()
                    {
                        long start = System.currentTimeMillis();
                        File file = new File(scriptName);
                        logger.info("Loading script file " + file + " ...");

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
                            logger.warning("Cannot find script file " + file);
                        }

                        return null;
                    }
                };

                task.start();
            } catch (Exception ex) {
                logger.warning("Error in processing script " + name, ex);
            }
        }
    }

    //--------------//
    // launchSheets //
    //--------------//
    /**
     * Launch the processing of sheets listed on command line
     */
    public static void launchSheets ()
    {
        // Launch desired step on each sheet in parallel
        for (String name : parameters.sheetNames) {
            final File file = new File(name);

            try {
                Worker<Void> task = new Worker<Void>() {
                    @Override
                    public Void construct ()
                    {
                        if (file.exists()) {
                            final Sheet sheet = new Sheet(file);
                            parameters.targetStep.performUntil(sheet);
                        } else {
                            logger.warning("Cannot find " + file);
                        }

                        return null;
                    }
                };

                logger.info(
                    "Submitting " + parameters.targetStep + " on " + file);
                task.start();
            } catch (Exception ex) {
                logger.warning("Error in processing sheet " + file, ex);
            }
        }
    }

    //------//
    // main //
    //------//
    /**
     * Specific starting method for the application.
     *
     * @param classContainer the class container (a directory or a jar file)
     * @param homeFolder the folder where Audiveris is installed
     * @param configFolder the subfolder for configuration data
     * @param args the command line parameters
     *
     * @see omr.Main the possible command line parameters
     */
    public static void main (File     classContainer,
                             File     homeFolder,
                             File     configFolder,
                             String[] args)
    {
        Main.classContainer = classContainer;
        Main.homeFolder = homeFolder;
        Main.configFolder = configFolder;

        // Locale to be used in the whole application ?
        checkLocale();

        // Initialize tool parameters
        initialize();

        // Process CLI arguments
        process(args);

        // For non-batch mode
        if (!parameters.batchMode) {
            if (logger.isFineEnabled()) {
                logger.fine("Main. Launching MainGui");
            }

            Application.launch(MainGui.class, args);
        } else {
            // Launch the required tasks
            launchSheets();
            launchScripts();

            // Wait for batch completion and exit
            OmrExecutors.shutdown();

            if (logger.isFineEnabled()) {
                logger.fine("End of main");
            }
        }
    }

    //---------//
    // isLinux //
    //---------//
    /**
     * Are we using a Linux OS
     * @return true if so
     */
    private static boolean isLinux ()
    {
        return System.getProperty("os.name")
                     .toLowerCase()
                     .startsWith("Linux");
    }

    //-------//
    // isMac //
    //-------//
    /**
     * Are we using a Mac OS
     * @return true if so
     */
    private static boolean isMac ()
    {
        return System.getProperty("os.name")
                     .toLowerCase()
                     .startsWith("mac os x");
    }

    //-----------//
    // isWindows //
    //-----------//
    /**
     * Are we using a Windows OS
     * @return true if so
     */
    private static boolean isWindows ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("OS is '" + System.getProperty("os.name") + "'");
        }

        return System.getProperty("os.name")
                     .toLowerCase()
                     .startsWith("windows");
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
            logger.fine("homeFolder=" + homeFolder);
            logger.fine("classContainer=" + classContainer);
            logger.fine(
                "classContainer.isDirectory=" + classContainer.isDirectory());
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
            logger.warning("Invalid CLI parameters, exiting ...");

            // Stop the JVM ????
            Runtime.getRuntime()
                   .exit(1);
        }

        // Interactive or Batch mode ?
        if (parameters.batchMode) {
            logger.info("Running in batch mode");
            System.setProperty("java.awt.headless", "true");
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
    }
}
