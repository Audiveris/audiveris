//----------------------------------------------------------------------------//
//                                                                            //
//                            W e l l K n o w n s                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.log.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Class {@code WellKnowns} gathers top public static final data to be
 * shared within Audiveris application.
 *
 * <p>Note that a few initial operations are performed here, because they need
 * to take place before any other class is loaded.
 *
 * @author Hervé Bitteur
 */
public class WellKnowns
{
    //~ Static fields/initializers ---------------------------------------------

    //----------//
    // IDENTITY //
    //----------//
    //
    /** Application name: {@value}. */
    public static final String TOOL_NAME = "audiveris";

    /** Application company name: {@value}. */
    public static final String TOOL_COMPANY = "AudiverisLtd";

    /** Specific prefix for application folders: {@value}. */
    private static final String TOOL_PREFIX = "/" + TOOL_COMPANY + "/"
                                              + TOOL_NAME;

    //----------//
    // PLATFORM //
    //----------//
    //
    /** File character encoding. */
    public static final String FILE_ENCODING = getFileEncoding();

    /** Are we using a Linux OS?. */
    public static final boolean LINUX = System.getProperty("os.name")
            .toLowerCase(Locale.ENGLISH)
            .startsWith("linux");

    /** Are we using a Mac OS?. */
    public static final boolean MAC_OS_X = System.getProperty("os.name")
            .toLowerCase(Locale.ENGLISH)
            .startsWith("mac os x");

    /** Are we using a Windows OS?. */
    public static final boolean WINDOWS = System.getProperty("os.name")
            .toLowerCase(Locale.ENGLISH)
            .startsWith("windows");

    /** Precise OS architecture. */
    public static final String OS_ARCH = System.getProperty("os.arch");
    
    /** File separator for the current platform. */
    public static final String FILE_SEPARATOR = System.getProperty(
            "file.separator");

    /** Line separator for the current platform. */
    public static final String LINE_SEPARATOR = System.getProperty(
            "line.separator");

    /** Redirection, if any, of standard out and err stream. */
    public static final String STD_OUT_ERROR = System.getProperty("stdouterr");

    //---------//
    // PROGRAM //
    //---------//
    //
    /** The container from which the application classes were loaded. */
    public static final File CLASS_CONTAINER = getClassContainer();

    /** Program installation folder for this application. */
    private static final File PROGRAM_FOLDER = getProgramFolder();

    /** The folder where resource data is stored. */
    public static final File RES_FOLDER = new File(PROGRAM_FOLDER, "res");

    /** The folder where Tesseract OCR material is stored. */
    public static final File OCR_FOLDER = getOcrFolder();

    /** The folder where documentations files are stored. */
    public static final File DOC_FOLDER = new File(PROGRAM_FOLDER, "www");

    /** Trick to detect a development environment rather than a standard one. */
    private static final boolean isProject = isProject();

    //--------//
    // CONFIG //
    //--------//
    //
    /** The folder where user specific data is stored. */
    public static final File USER_FOLDER = getUserConfigFolder();

    /** Base folder for config. */
    private static final File CONFIG_FOLDER = getConfigFolder();

    /** The folder where global configuration data is stored. */
    public static final File SETTINGS_FOLDER = new File(
            CONFIG_FOLDER,
            "settings");

    /** The folder where plugin scripts are found. */
    public static final File PLUGINS_FOLDER = new File(
            CONFIG_FOLDER,
            "plugins");

    //------//
    // DATA //
    //------//
    //
    /** Base folder for data. */
    private static final File DATA_FOLDER = getDataFolder();

    /** The folder where examples are stored. */
    public static final File EXAMPLES_FOLDER = new File(
            DATA_FOLDER,
            "examples");

    /** The folder where temporary data can be stored. */
    public static final File TEMP_FOLDER = new File(DATA_FOLDER, "temp");

    /** The folder where evaluation data is stored. */
    public static final File EVAL_FOLDER = new File(DATA_FOLDER, "eval");

    /** The folder where training material is stored. */
    public static final File TRAIN_FOLDER = new File(DATA_FOLDER, "train");

    /** The folder where symbols information is stored. */
    public static final File SYMBOLS_FOLDER = new File(TRAIN_FOLDER, "symbols");

    /** The default folder where benches data is stored. */
    public static final File DEFAULT_BENCHES_FOLDER = new File(
            DATA_FOLDER,
            "benches");

    /** The default folder where MIDI data is stored. */
    public static final File DEFAULT_MIDI_FOLDER = new File(
            DATA_FOLDER,
            "midi");

    /** The default folder where PDF data is stored. */
    public static final File DEFAULT_PRINT_FOLDER = new File(
            DATA_FOLDER,
            "print");

    /** The default folder where scripts data is stored. */
    public static final File DEFAULT_SCRIPTS_FOLDER = new File(
            DATA_FOLDER,
            "scripts");

    /** The default folder where scores data is stored. */
    public static final File DEFAULT_SCORES_FOLDER = new File(
            DATA_FOLDER,
            "scores");

    //---------//
    // LOGGING //
    //---------//
    //
    /** The logging definition file, if any. */
    private static final File LOGGING_FILE = getLoggingFile();

    static {
        /** Log declared data (debug). */
        logDeclaredData();
    }

    // Miscellaneous actions
    static {
        /** Disable DirecDraw by default. */
        disableDirectDraw();
    }

    //~ Constructors -----------------------------------------------------------
    //
    //------------//
    // WellKnowns // Not meant to be instantiated
    //------------//
    private WellKnowns ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //
    //--------------//
    // ensureLoaded //
    //--------------//
    /**
     * Make sure this class is loaded.
     */
    public static void ensureLoaded ()
    {
    }

    //-------------------//
    // disableDirectDraw //
    //-------------------//
    private static void disableDirectDraw ()
    {
        // See http://performance.netbeans.org/howto/jvmswitches/
        // -Dsun.java2d.d3d=false
        // this switch disables DirectDraw and may solve performance problems
        // with some HW configurations.
        final String KEY = "sun.java2d.d3d";

        // Respect user setting if any
        if (System.getProperty(KEY) == null) {
            System.setProperty(KEY, "false");
        }
    }

    //-------------------//
    // getClassContainer //
    //-------------------//
    private static File getClassContainer ()
    {
        try {
            /** Classes container, beware of escaped blanks */
            return new File(
                    WellKnowns.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException ex) {
            System.err.println("Cannot decode container, " + ex);
            throw new RuntimeException(ex);
        }
    }

    //-----------------//
    // getConfigFolder //
    //-----------------//
    private static File getConfigFolder ()
    {
        if (isProject) {
            // For development environment CONFIG = DATA = PROGRAM
            return PROGRAM_FOLDER;
        } else {
            // Standard case: config data is in user appdata
            return USER_FOLDER;
        }
    }

    //---------------------//
    // getUserConfigFolder //
    //---------------------//
    private static File getUserConfigFolder ()
    {
        if (WINDOWS) {
            return getUserDataFolder();
        } else if (MAC_OS_X) {
            String config = System.getenv("XDG_CONFIG_HOME");
            if (config != null) {
                return new File(config + System.getProperty("user.dir"));
            }
            String home = System.getenv("HOME");
            if (home != null) {
                return new File(System.getProperty("user.dir"));
            }
            throw new RuntimeException("HOME environment variable is not set");

        } else if (LINUX) {
            String config = System.getenv("XDG_CONFIG_HOME");

            if (config != null) {
                return new File(config + TOOL_PREFIX);
            }

            String home = System.getenv("HOME");

            if (home != null) {
                return new File(home + "/.config" + TOOL_PREFIX);
            }

            throw new RuntimeException("HOME environment variable is not set");
        } else {
            throw new RuntimeException("Platform unknown");
        }
    }

    //---------------//
    // getDataFolder //
    //---------------//
    private static File getDataFolder ()
    {
        if (isProject) {
            // For development environment CONFIG = DATA = PROGRAM
            return PROGRAM_FOLDER;
        } else {
            return getUserDataFolder();
        }
    }

    //-------------------//
    // getUserDataFolder //
    //-------------------//
    private static File getUserDataFolder ()
    {
        if (WINDOWS) {
            String appdata = System.getenv("APPDATA");

            if (appdata != null) {
                return new File(appdata + TOOL_PREFIX);
            }

            throw new RuntimeException(
                    "APPDATA environment variable is not set");
        } else if (MAC_OS_X) {
            String data = System.getenv("XDG_DATA_HOME");
            if (data != null) {
                return new File(System.getProperty("user.dir"));
            }
            String home = System.getenv("HOME");
            if (home != null) {
                return new File(System.getProperty("user.dir"));
            }
            throw new RuntimeException("HOME environment variable is not set");
        } else if (LINUX) {
            String data = System.getenv("XDG_DATA_HOME");

            if (data != null) {
                return new File(data + TOOL_PREFIX);
            }

            String home = System.getenv("HOME");

            if (home != null) {
                return new File(home + "/.local/share" + TOOL_PREFIX);
            }

            throw new RuntimeException("HOME environment variable is not set");
        } else {
            throw new RuntimeException("Platform unknown");
        }
    }

    //------------------//
    // getProgramFolder //
    //------------------//
    private static File getProgramFolder ()
    {
        // .../build/classes when running from classes files
        // .../dist/audiveris.jar when running from the jar archive
        return CLASS_CONTAINER.getParentFile()
                .getParentFile();
    }

    //--------------//
    // getOcrFolder //
    //--------------//
    private static File getOcrFolder ()
    {
        // First, try to use TESSDATA_PREFIX environment variable
        // which would denote a Tesseract installation
        final String TESSDATA_PREFIX = "TESSDATA_PREFIX";
        final String tessPrefix = System.getenv(TESSDATA_PREFIX);

        if (tessPrefix != null) {
            File dir = new File(tessPrefix);
            if (dir.isDirectory()) {
                // TODO: check directory content?
                return dir;
            }
        }

        // Fallback using ocr subdirectory of Audiveris
        if (LINUX) {
            return new File("/usr/local/share");
        } else {
            return new File(PROGRAM_FOLDER, "ocr");
        }
    }

    //-----------//
    // isProject //
    //-----------//
    private static boolean isProject ()
    {
        // We use the fact that a "src" folder is the evidence that we
        // are running from the development folder (with data & settings as
        // subfolders) rather than from a standard folder (installed with 
        // distinct location for application data & config).
        // There are other possible "src" symbol does not work for example other program invoking audiveris.jar file
        // so added one more condition to check if class_container having .jar extension
        File devFolder = new File(PROGRAM_FOLDER, "src");
        String s = CLASS_CONTAINER.getName();
        String ext = null;
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }

        return devFolder.exists() || "jar".equals(ext);
    }

    //----------------//
    // getLoggingFile //
    //----------------//
    private static File getLoggingFile ()
    {
        final String JAVA_LOGGING_KEY = "java.util.logging.config.file";
        final String APACHE_LOGGING_KEY = "log4j.configuration";

        final String LOGGING_NAME = "logging.properties";
        File loggingFile = null;

        // Set logging configuration file (if none already defined)
        final String loggingProp = System.getProperty(JAVA_LOGGING_KEY);
        if (loggingProp == null) {
            // Check for a user file
            loggingFile = new File(USER_FOLDER, LOGGING_NAME);

            if (loggingFile.exists()) {

                // Set property for java.util.logging
                System.setProperty(JAVA_LOGGING_KEY, loggingFile.toString());

                // Set property for log4j
                try {
                    final String url = loggingFile.toURI().toURL().toString();
                    System.setProperty(APACHE_LOGGING_KEY, url);
                } catch (MalformedURLException ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            ///System.out.println("Logging already defined by " + loggingProp);
        }

//        System.out.println("prop " + JAVA_LOGGING_KEY + "= " + System.getProperty(JAVA_LOGGING_KEY));
//        System.out.println("prop " + APACHE_LOGGING_KEY + "= " + System.getProperty(APACHE_LOGGING_KEY));

        return loggingFile;
    }

    //-----------------//
    // getFileEncoding //
    //-----------------//
    private static String getFileEncoding ()
    {
        final String ENCODING_KEY = "file.encoding";
        final String ENCODING_VALUE = "UTF-8";

        System.setProperty(ENCODING_KEY, ENCODING_VALUE);
        return ENCODING_VALUE;
    }

    //-----------------//
    // logDeclaredData //
    //-----------------//
    private static void logDeclaredData ()
    {
        final Logger logger = Logger.getLogger(WellKnowns.class);

        if (logger.isFineEnabled()) {
            for (Field field : WellKnowns.class.getDeclaredFields()) {
                try {
                    logger.fine("{0}= {1}", field.getName(), field.get(null));
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
