//----------------------------------------------------------------------------//
//                                                                            //
//                            W e l l K n o w n s                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.log.LogUtilities;
import static omr.util.UriUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarFile;

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
    /** Application company name: {@value}. */
    public static final String COMPANY_NAME = ProgramId.COMPANY_NAME;

    /** Application company id: {@value}. */
    public static final String COMPANY_ID = ProgramId.COMPANY_ID;

    /** Application name: {@value}. */
    public static final String TOOL_NAME = ProgramId.NAME;

    /** Application reference: {@value}. */
    public static final String TOOL_REF = ProgramId.VERSION + "." +
                                          ProgramId.REVISION;

    /** Specific prefix for application folders: {@value} */
    private static final String TOOL_PREFIX = "/" + COMPANY_ID + "/" +
                                              TOOL_NAME;

    //----------//
    // PLATFORM //
    //----------//
    //
    /** Name of operating system */
    private static final String OS_NAME = System.getProperty("os.name")
                                                .toLowerCase(Locale.ENGLISH);

    /** Are we using a Linux OS?. */
    public static final boolean LINUX = OS_NAME.startsWith("linux");

    /** Are we using a Mac OS?. */
    public static final boolean MAC_OS_X = OS_NAME.startsWith("mac os x");

    /** Are we using a Windows OS?. */
    public static final boolean WINDOWS = OS_NAME.startsWith("windows");

    /** Precise OS architecture. */
    public static final String OS_ARCH = System.getProperty("os.arch");

    /** Are we using Windows on 64 bit architecture?. */
    public static final boolean WINDOWS_64 = WINDOWS &&
                                             (System.getenv(
        "ProgramFiles(x86)") != null);

    /** File character encoding. */
    public static final String FILE_ENCODING = getFileEncoding();

    /** File separator for the current platform. */
    public static final String FILE_SEPARATOR = System.getProperty(
        "file.separator");

    /** Line separator for the current platform. */
    public static final String LINE_SEPARATOR = System.getProperty(
        "line.separator");

    /** Redirection, if any, of standard out and err streams. */
    public static final String STD_OUT_ERR = System.getProperty("stdouterr");

    //---------//
    // PROGRAM // This is a read-only area
    //---------//
    //    
    /** The container from which the application classes were loaded. */
    public static final URI CLASS_CONTAINER = getClassContainer();

    /**
     * Running from normal jar archive rather than class files? .
     * When running directly from .class files, we are in development mode and
     * want to have very short cycles, data is retrieved from local files.
     * When running from .jar archive, we are in standard mode whereby most data
     * is meant to be retrieved from the .jar archive itself.
     */
    public static final boolean RUNNING_FROM_JAR = runningFromJar();

    /** Containing jar file, if any. */
    public static final JarFile JAR_FILE = RUNNING_FROM_JAR ? getJarFile() : null;

    /** Time of last modification of jar file, if any. */
    public static final FileTime JAR_TIME = RUNNING_FROM_JAR ? getJarTime() : null;

    /** The uri where resource data is stored. */
    public static final URI RES_URI = RUNNING_FROM_JAR
                                      ? toURI(
        WellKnowns.class.getClassLoader().getResource("res"))
                                      : Paths.get("res")
                                             .toUri();

    /** The folder where Tesseract OCR material is stored. */
    public static final File OCR_FOLDER = getOcrFolder();

    //-------------// read-write area
    // USER CONFIG // Configuration files the user can edit on his own
    //-------------// 
    //
    /** The config folder where global configuration data is stored. */
    public static final File CONFIG_FOLDER = RUNNING_FROM_JAR
                                             ? getConfigFolder()
                                             : new File("config");

    /** The folder where plugin scripts are found. */
    public static final File PLUGINS_FOLDER = new File(
        CONFIG_FOLDER,
        "plugins");

    //-----------// read-write area
    // USER DATA // User-specific data, except configuration stuff
    //-----------//
    //
    /** Base folder for data */
    public static final File DATA_FOLDER = RUNNING_FROM_JAR ? getDataFolder()
                                           : new File("data");

    /**
     * The folder where documentations files are installed.
     * Installation takes place when .jar is run for the first time
     */
    public static final File DOC_FOLDER = new File(DATA_FOLDER, "www");

    /**
     * The folder where examples files are installed.
     * Installation takes place when .jar is run for the first time
     */
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

    static {
        /** Logging configuration. */
        LogUtilities.initialize(CONFIG_FOLDER, TEMP_FOLDER);
        /** Log declared data (debug). */
        logDeclaredData();
    }

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

    //--------------//
    // ensureLoaded //
    //--------------//
    /**
     * Make sure this class is loaded.
     */
    public static void ensureLoaded ()
    {
    }

    //
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
    private static URI getClassContainer ()
    {
        return toURI(
            WellKnowns.class.getProtectionDomain().getCodeSource().getLocation());
    }

    //-----------------//
    // getConfigFolder //
    //-----------------//
    private static File getConfigFolder ()
    {
        if (WINDOWS) {
            String appdata = System.getenv("APPDATA");

            if (appdata != null) {
                return new File(appdata + TOOL_PREFIX + "/config");
            }

            throw new RuntimeException(
                "APPDATA environment variable is not set");
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
        if (WINDOWS) {
            String appdata = System.getenv("APPDATA");

            if (appdata != null) {
                return new File(appdata + TOOL_PREFIX + "/data");
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

    //------------//
    // getJarFile //
    //------------//
    private static JarFile getJarFile ()
    {
        try {
            String           rn = WellKnowns.class.getName()
                                                  .replace('.', '/') +
                                  ".class";
            Enumeration<URL> en = WellKnowns.class.getClassLoader()
                                                  .getResources(rn);

            if (en.hasMoreElements()) {
                URL              url = en.nextElement();

                // url = jar:http://audiveris.kenai.com/jnlp/audiveris.jar!/omr/WellKnowns.class
                JarURLConnection urlcon = (JarURLConnection) (url.openConnection());
                JarFile          jarFile = urlcon.getJarFile();

                return jarFile;
            }
        } catch (Exception ex) {
            System.out.print("Error getting jar file " + ex);
        }

        return null;
    }

    //------------//
    // getJarTime //
    //------------//
    private static FileTime getJarTime ()
    {
        long millis = JAR_FILE.getEntry("META-INF/MANIFEST.MF")
                              .getTime();

        return FileTime.fromMillis(millis);
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
                return dir;
            }
        }

        // Fallback to default directory
        if (LINUX) {
            return new File("/usr/share/tesseract-ocr");
        } else {
            throw new InstallationException("Tesseract-OCR is not installed");
        }
    }

    //-----------------//
    // logDeclaredData //
    //-----------------//
    private static void logDeclaredData ()
    {
        final Logger logger = LoggerFactory.getLogger(WellKnowns.class);

        // To check updates
        ///logger.info("Token #{}", 2);

        if (!RUNNING_FROM_JAR) {
            // Just to remind the developer we are NOT running in normal mode
            logger.info("[Not running from jar]");
        } else {
            // Debug, to identify this jar
            logger.debug(
                "JarTime: {} JarFile: {}",
                JAR_TIME,
                JAR_FILE.getName());
        }

        if (logger.isDebugEnabled()) {
            for (Field field : WellKnowns.class.getDeclaredFields()) {
                try {
                    logger.debug("{}= {}", field.getName(), field.get(null));
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    //----------------//
    // runningFromJar //
    //----------------//
    private static boolean runningFromJar ()
    {
        return CLASS_CONTAINER.toString()
                              .toLowerCase()
                              .endsWith(".jar");
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------------------//
    // InstallationException //
    //-----------------------//
    /**
     * Exception used to signal an installation error.
     */
    public static class InstallationException
        extends RuntimeException
    {
        //~ Constructors -------------------------------------------------------

        public InstallationException (String message)
        {
            super(message);
        }
    }
}
