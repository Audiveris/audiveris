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

import omr.score.midi.MidiAgent;
import omr.score.visitor.ScoreExporter;

import omr.script.Script;
import omr.script.ScriptManager;

import omr.ui.MainGui;
import omr.ui.OmrUIDefaults;

import omr.util.Clock;
import omr.util.JaiLoader;
import omr.util.Logger;
import omr.util.OmrExecutors;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

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
    extends SingleFrameApplication
{
    //~ Static fields/initializers ---------------------------------------------

    static {
        /** Time stamp */
        Clock.resetTime();
    }

    /** Classes container, beware of escaped blanks */
    private static File container;

    static {
        try {
            container = new File(
                Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception ex) {
            System.err.println("Cannot decode container, " + ex);
        }
    }

    /** Installation folder (needs to be initialized before logger) */
    // .../build/classes
    // .../dist/audiveris.jar
    private static final File homeFolder = container.getParentFile()
                                                    .getParentFile();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Main.class);

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Specific folder name for icons */
    public static final String ICONS_NAME = "icons";

    /** Tells if using Mac OS X for special GUI functionality */
    public static final boolean MAC_OS_X = System.getProperty("os.name")
                                                 .toLowerCase()
                                                 .startsWith("mac os x");

    /** Build reference of the application as displayed to the user */
    private static String toolBuild;

    /** Name of the application as displayed to the user */
    private static String toolName;

    /** Version of the application as displayed to the user */
    private static String toolVersion;

    /** Master View */
    private static MainGui gui;

    //~ Instance fields --------------------------------------------------------

    /** Parameters read from CLI */
    private CLI.Parameters parameters;

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
        return new File(homeFolder, "config");
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
        return new File(homeFolder, "www");
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
        return new File(homeFolder, ICONS_NAME);
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this application
     *
     * @return the SingleFrameApplication instance
     */
    public static SingleFrameApplication getInstance ()
    {
        return (SingleFrameApplication) Application.getInstance();
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
        return new File(homeFolder, "train");
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
        launch(Main.class, args);
    }

    //------------//
    // initialize //
    //------------//
    /** {@inheritDoc} */
    @Override
    protected void initialize (String[] args)
    {
        if (logger.isFineEnabled()) {
            logger.fine("homeFolder=" + homeFolder);
            logger.fine("container=" + container);
            logger.fine("container.isDirectory=" + container.isDirectory());
        }

        // Locale to be used in the whole application ?
        checkLocale();

        // Tool name
        final Package thisPackage = Main.class.getPackage();
        toolName = thisPackage.getSpecificationTitle();

        if (toolName == null) {
            toolName = getContext()
                           .getResourceMap()
                           .getString("Application.id");
        }

        // Tool version
        toolVersion = thisPackage.getSpecificationVersion();

        if (toolVersion == null) {
            toolVersion = getContext()
                              .getResourceMap()
                              .getString("Application.version");
        }

        // Tool build
        toolBuild = thisPackage.getImplementationVersion();

        if (toolBuild == null) {
            toolBuild = getContext()
                            .getResourceMap()
                            .getString("Application.build");
        }

        // Process CLI arguments
        process(args);
    }

    //-------//
    // ready //
    //-------//
    @Override
    protected void ready ()
    {
        // Launch background pre-loading tasks
        JaiLoader.preload();
        ScoreExporter.preload();
        MidiAgent.preload();

        // Launch desired step on each sheet in parallel
        for (String name : parameters.sheetNames) {
            final File file = new File(name);
            OmrExecutors.getLowExecutor()
                        .submit(
                new Runnable() {
                        public void run ()
                        {
                            parameters.targetStep.perform(null, file);
                        }
                    });
        }

        // Launch desired scripts in parallel
        for (String name : parameters.scriptNames) {
            final String scriptName = name;
            OmrExecutors.getLowExecutor()
                        .submit(
                new Runnable() {
                        public void run ()
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
                                logger.warning(
                                    "Cannot find script file " + file);
                            }
                        }
                    });
        }
    }

    //---------//
    // startup //
    //---------//
    @Override
    protected void startup ()
    {
        gui = new MainGui(this.getMainFrame());
        show(gui.getFrame());
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

    //---------//
    // process //
    //---------//
    private void process (String[] args)
    {
        // First get the provided arguments if any
        parameters = new CLI(toolName, args).getParameters();

        if (parameters == null) {
            logger.warning("Invalid CLI parameters, exiting ...");
            exit();
        }

        // Interactive or Batch mode ?
        if (parameters.batchMode) {
            logger.info("Running in batch mode");
            System.setProperty("java.awt.headless", "true");
        } else {
            logger.fine("Running in interactive mode");

            // UI Look and Feel
            ///UILookAndFeel.setUI(null);

            // Make sure we have nice window decorations.
            JFrame.setDefaultLookAndFeelDecorated(true);

            // Application UI defaults
            if (!MainGui.useSwingApplicationFramework()) {
                OmrUIDefaults defaults = OmrUIDefaults.getInstance();
                defaults.addResourceBundle("config/ui");

                if (defaults.isEmpty()) {
                    logger.fine(
                        "No UI defaults as resource bundle, loading from config folder");

                    try {
                        defaults.loadFrom(new File(getConfigFolder(), "ui"));
                    } catch (Exception ex) {
                        logger.warning(
                            "Unable to load resources from config folder",
                            ex);
                    }
                }
            }
        }

        // Batch closing
        if (parameters.batchMode) {
            OmrExecutors.shutdown();
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
