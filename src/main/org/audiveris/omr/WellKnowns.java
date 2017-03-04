//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      W e l l K n o w n s                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import org.audiveris.omr.log.LogUtil;
import static org.audiveris.omr.util.UriUtil.toURI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarFile;

/**
 * Class {@code WellKnowns} gathers top public static final data to be shared within
 * Audiveris application.
 * <p>
 * Note that a few initial operations are performed here, because they need to take place before any
 * other class is loaded.
 *
 * @author Hervé Bitteur
 */
public abstract class WellKnowns
{
    //~ Static fields/initializers -----------------------------------------------------------------

    //----------//
    // IDENTITY //
    //----------//
    //
    /** Application company name: {@value}. */
    public static final String COMPANY_NAME = ProgramId.COMPANY_NAME;

    /** Application company id: {@value}. */
    public static final String COMPANY_ID = ProgramId.COMPANY_ID;

    /** Application name: {@value}. */
    public static final String TOOL_NAME = ProgramId.PROGRAM_NAME;

    /** Application reference: {@value}. */
    public static final String TOOL_REF = ProgramId.PROGRAM_VERSION;

    /** Application build: {@value}. */
    public static final String TOOL_BUILD = ProgramId.PROGRAM_BUILD;

    /** Specific prefix for application folders: {@value}. */
    private static final String TOOL_PREFIX = "/" + COMPANY_ID + "/" + TOOL_NAME;

    //----------//
    // PLATFORM //
    //----------//
    //
    /** Name of operating system. */
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    /** Are we using a Linux OS?. */
    public static final boolean LINUX = OS_NAME.startsWith("linux");

    /** Are we using a Mac OS?. */
    public static final boolean MAC_OS_X = OS_NAME.startsWith("mac os x");

    /** Are we using a Windows OS?. */
    public static final boolean WINDOWS = OS_NAME.startsWith("windows");

    /** Precise OS architecture. */
    public static final String OS_ARCH = System.getProperty("os.arch");

    /** Are we using Windows on 64 bit architecture?. */
    public static final boolean WINDOWS_64 = WINDOWS
                                             && (System.getenv("ProgramFiles(x86)") != null);

    /** File character encoding. */
    public static final String FILE_ENCODING = getFileEncoding();

    /** File separator for the current platform. */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    /** Line separator for the current platform. */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

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
            ? toURI(WellKnowns.class.getClassLoader().getResource("res"))
            : Paths.get("res").toUri();

    /** The folder where Tesseract OCR material is stored. */
    public static final Path OCR_FOLDER = getOcrFolder();

    //-------------// read-write area
    // USER CONFIG // Configuration files the user can edit on his own
    //-------------//
    //
    /** The config folder where global configuration data is stored. */
    public static final Path CONFIG_FOLDER = RUNNING_FROM_JAR ? getConfigFolder()
            : Paths.get("config");

    /** The folder where plugin scripts are found. */
    public static final Path PLUGINS_FOLDER = CONFIG_FOLDER.resolve("plugins");

    //-----------// read-write area
    // USER DATA // User-specific data, except configuration stuff
    //-----------//
    //
    /** Base folder for data. */
    public static final Path DATA_FOLDER = RUNNING_FROM_JAR ? getDataFolder() : Paths.get("data");

    /**
     * The folder where documentations files are installed.
     * Installation takes place when .jar is run for the first time
     */
    public static final Path DOC_FOLDER = DATA_FOLDER.resolve("www");

    /**
     * The folder where examples files are installed.
     * Installation takes place when .jar is run for the first time
     */
    public static final Path EXAMPLES_FOLDER = DATA_FOLDER.resolve("examples");

    /** The folder where temporary data can be stored. */
    public static final Path TEMP_FOLDER = DATA_FOLDER.resolve("temp");

    /** The folder where training material is stored. */
    public static final Path TRAIN_FOLDER = DATA_FOLDER.resolve("train");

    /** The default base for output folders. */
    public static final Path DEFAULT_BASE_FOLDER = DATA_FOLDER.resolve("output");

    static {
        /** Logging configuration. */
        LogUtil.initialize(CONFIG_FOLDER);

        /** Log declared data (debug). */
        logDeclaredData();
    }

    static {
        /** Make sure TEMP_FOLDER exists. */
        createTempFolder();
    }

    static {
        /** Disable DirecDraw by default. */
        disableDirectDraw();
    }

    static {
        /** Disable use of JAI native MediaLib, by default. */
        disableMediaLib();
    }

    //~ Constructors -------------------------------------------------------------------------------
    /** Not meant to be instantiated. */
    private WellKnowns ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // ensureLoaded //
    //--------------//
    /**
     * Make sure this class is loaded.
     */
    public static void ensureLoaded ()
    {
    }

    //------------------//
    // createTempFolder //
    //------------------//
    /**
     * Make sure TEMP_FOLDER exists.
     */
    private static void createTempFolder ()
    {
        final Logger logger = LoggerFactory.getLogger(WellKnowns.class);

        if (!Files.exists(TEMP_FOLDER)) {
            try {
                Files.createDirectories(TEMP_FOLDER);
            } catch (IOException ex) {
                logger.warn("Error creating " + TEMP_FOLDER, ex);
            }
        }
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

    //-----------------//
    // disableMediaLib //
    //-----------------//
    private static void disableMediaLib ()
    {
        // Avoid "Could not find mediaLib accelerator wrapper classes. Continuing in pure Java mode"
        final String KEY = "com.sun.media.jai.disableMediaLib";

        // Respect user setting if any
        if (System.getProperty(KEY) == null) {
            System.setProperty(KEY, "true");
        }
    }

    //-------------------//
    // getClassContainer //
    //-------------------//
    private static URI getClassContainer ()
    {
        return toURI(WellKnowns.class.getProtectionDomain().getCodeSource().getLocation());
    }

    //-----------------//
    // getConfigFolder //
    //-----------------//
    private static Path getConfigFolder ()
    {
        if (WINDOWS) {
            String appdata = System.getenv("APPDATA");

            if (appdata != null) {
                return Paths.get(appdata + TOOL_PREFIX + "/config");
            }

            throw new RuntimeException("APPDATA environment variable is not set");
        } else if (MAC_OS_X) {
            String config = System.getenv("XDG_CONFIG_HOME");

            if (config != null) {
                return Paths.get(config + System.getProperty("user.dir"));
            }

            String home = System.getenv("HOME");

            if (home != null) {
                return Paths.get(System.getProperty("user.dir"));
            }

            throw new RuntimeException("HOME environment variable is not set");
        } else if (LINUX) {
            String config = System.getenv("XDG_CONFIG_HOME");

            if (config != null) {
                return Paths.get(config + TOOL_PREFIX);
            }

            String home = System.getenv("HOME");

            if (home != null) {
                return Paths.get(home + "/.config" + TOOL_PREFIX);
            }

            throw new RuntimeException("HOME environment variable is not set");
        } else {
            throw new RuntimeException("Platform unknown");
        }
    }

    //---------------//
    // getDataFolder //
    //---------------//
    private static Path getDataFolder ()
    {
        if (WINDOWS) {
            String appdata = System.getenv("APPDATA");

            if (appdata != null) {
                return Paths.get(appdata + TOOL_PREFIX + "/data");
            }

            throw new RuntimeException("APPDATA environment variable is not set");
        } else if (MAC_OS_X) {
            String data = System.getenv("XDG_DATA_HOME");

            if (data != null) {
                return Paths.get(System.getProperty("user.dir"));
            }

            String home = System.getenv("HOME");

            if (home != null) {
                return Paths.get(System.getProperty("user.dir"));
            }

            throw new RuntimeException("HOME environment variable is not set");
        } else if (LINUX) {
            String data = System.getenv("XDG_DATA_HOME");

            if (data != null) {
                return Paths.get(data + TOOL_PREFIX);
            }

            String home = System.getenv("HOME");

            if (home != null) {
                return Paths.get(home + "/.local/share" + TOOL_PREFIX);
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
            String rn = WellKnowns.class.getName().replace('.', '/') + ".class";
            Enumeration<URL> en = WellKnowns.class.getClassLoader().getResources(rn);

            if (en.hasMoreElements()) {
                URL url = en.nextElement();

                // url = jar:http://audiveris.kenai.com/jnlp/audiveris.jar!/omr/WellKnowns.class
                JarURLConnection urlcon = (JarURLConnection) (url.openConnection());
                JarFile jarFile = urlcon.getJarFile();

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
        long millis = JAR_FILE.getEntry("META-INF/MANIFEST.MF").getTime();

        return FileTime.fromMillis(millis);
    }

    //--------------//
    // getOcrFolder //
    //--------------//
    private static Path getOcrFolder ()
    {
        // First, try to use TESSDATA_PREFIX environment variable
        // which might denote a Tesseract installation
        final String TESSDATA_PREFIX = "TESSDATA_PREFIX";
        final String tessPrefix = System.getenv(TESSDATA_PREFIX);

        if (tessPrefix != null) {
            Path dir = Paths.get(tessPrefix);

            if (Files.isDirectory(dir)) {
                return dir;
            }
        }

        // Fallback to default directory
        if (LINUX) {
            return Paths.get("/usr/share/tesseract-ocr");
        } else if (WINDOWS) {
            final String pf32 = OS_ARCH.equals("x86") ? "ProgramFiles" : "ProgramFiles(x86)";

            return Paths.get(System.getenv(pf32)).resolve("tesseract-ocr");
        } else if (MAC_OS_X) {
            // Default path when installed with homebrew
            final Path brewPath = Paths.get("/usr/local/opt/tesseract/share");
            // Default path when installed with macports
            Path portPath = Paths.get("/opt/local/share");
            if (Files.exists(brewPath.resolve("tessdata"))) {
                return brewPath;
            } else if (Files.exists(portPath.resolve("tessdata"))) {
                return portPath;
            }
        }

        throw new InstallationException("Tesseract-OCR is not installed");
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
            logger.info("Not running from jar.");
        } else {
            // Debug, to identify this jar
            logger.debug("JarTime: {} JarFile: {}", JAR_TIME, JAR_FILE.getName());
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
        return CLASS_CONTAINER.toString().toLowerCase().endsWith(".jar");
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------------------//
    // InstallationException //
    //-----------------------//
    /**
     * Exception used to signal an installation error.
     */
    public static class InstallationException
            extends RuntimeException
    {
        //~ Constructors ---------------------------------------------------------------------------

        public InstallationException (String message)
        {
            super(message);
        }
    }
}
