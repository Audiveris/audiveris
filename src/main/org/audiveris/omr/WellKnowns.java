//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      W e l l K n o w n s                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarFile;

import javax.swing.filechooser.FileSystemView;

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

    /** Application id: {@value}. */
    public static final String TOOL_ID = ProgramId.PROGRAM_ID;

    /** Application reference: {@value}. */
    public static final String TOOL_REF = ProgramId.PROGRAM_VERSION;

    /** Application build: {@value}. */
    public static final String TOOL_BUILD = ProgramId.PROGRAM_BUILD;

    /** Specific prefix for application folders: {@value}. */
    private static final String TOOL_PREFIX = "/" + COMPANY_ID + "/" + TOOL_ID;

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
    public static final boolean WINDOWS_64 = WINDOWS && (System.getenv(
            "ProgramFiles(x86)") != null);

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

    /** The uri where read-only resources are stored. */
    public static final URI RES_URI = RUNNING_FROM_JAR ? toURI(
            WellKnowns.class.getClassLoader().getResource("res")) : Paths.get("res").toUri();

    //-------------// read-write area
    // USER CONFIG // Configuration files the user can edit on his own
    //-------------//
    //
    /** The folder where global configuration data is stored. */
    public static final Path CONFIG_FOLDER = getFolder(FolderKind.CONFIG);

    //-----------// read-write area
    // USER DATA // User-specific data, except configuration stuff
    //-----------//
    //
    /** Base folder for data. */
    public static final Path DATA_FOLDER = getFolder(FolderKind.DATA);

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

    /** The folder where training material is stored. */
    public static final Path TRAIN_FOLDER = CONFIG_FOLDER.resolve("train");

    /** The default base for output folders. */
    public static final Path DEFAULT_BASE_FOLDER = DATA_FOLDER; // BHT: skip "output"

    //----------//
    // USER LOG //
    //----------//
    //
    /** The folder where log files are stored. */
    public static final Path LOG_FOLDER = getFolder(FolderKind.LOG);

    /** The folder where temporary data can be stored. */
    public static final Path TEMP_FOLDER = LOG_FOLDER.resolve("temp");

    static {
        /** Logging configuration. */
        LogUtil.initialize(CONFIG_FOLDER, RES_URI);

        /** Make sure LOG_FOLDER exists. */
        createFolder(LOG_FOLDER);

        /** Log declared data (debug). */
        logDeclaredData();

        /** Make sure TEMP_FOLDER exists. */
        createFolder(TEMP_FOLDER);
    }

    static {
        /** Disable DirectDraw by default. */
        disableDirectDraw();
    }

    static {
        /** Disable use of JAI native MediaLib, by default. */
        disableMediaLib();
    }

    private static enum FolderKind
    {
        DATA,
        CONFIG,
        LOG;
    }

    /** Not meant to be instantiated. */
    private WellKnowns ()
    {
    }

    //--------------//
    // ensureLoaded //
    //--------------//
    /**
     * Make sure this class is loaded.
     */
    public static void ensureLoaded ()
    {
    }

    //--------------//
    // createFolder //
    //--------------//
    /**
     * Make sure the provided folder exists.
     *
     * @param folder the folder to create if needed
     */
    private static void createFolder (Path folder)
    {
        if (!Files.exists(folder)) {
            try {
                Files.createDirectories(folder);
            } catch (IOException ex) {
                final Logger logger = LoggerFactory.getLogger(WellKnowns.class);
                logger.warn("Error creating " + folder, ex);
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
    // getFileEncoding //
    //-----------------//
    private static String getFileEncoding ()
    {
        final String ENCODING_KEY = "file.encoding";
        final String ENCODING_VALUE = "UTF-8";

        System.setProperty(ENCODING_KEY, ENCODING_VALUE);

        return ENCODING_VALUE;
    }

    //-----------//
    // getFolder //
    //-----------//
    private static Path getFolder (FolderKind kind)
    {
        if (WINDOWS) {
            return getFolderForWindows(kind);
        } else if (MAC_OS_X) {
            return getFolderForMac(kind);
        } else if (LINUX) {
            return getFolderForLinux(kind);
        } else {
            printError("Platform unknown: " + kind);

            return null;
        }
    }

    //-------------------//
    // getFolderForLinux //
    //-------------------//
    private static Path getFolderForLinux (FolderKind kind)
    {
        // First try XDG specification
        final String xdg = System.getenv(xdgProperty(kind));

        if (xdg != null) {
            final Path audiverisPath = Paths.get(xdg + TOOL_PREFIX);

            switch (kind) {
            case DATA:
                return audiverisPath;

            case CONFIG:
                return audiverisPath;

            default:
            case LOG:
                return audiverisPath.resolve("log");
            }
        }

        // Fall back using home
        final String home = System.getenv("HOME");

        if (home == null) {
            printError("HOME environment variable is not set");

            return null;
        }

        switch (kind) {
        case DATA:
            return Paths.get(home + "/.local/share" + TOOL_PREFIX);

        case CONFIG:
            return Paths.get(home + "/.config" + TOOL_PREFIX);

        default:
        case LOG:
            return Paths.get(home + "/.cache" + TOOL_PREFIX + "/log");
        }
    }

    //-----------------//
    // getFolderForMac //
    //-----------------//
    private static Path getFolderForMac (FolderKind kind)
    {
        final String home = System.getenv("HOME");

        if (home == null) {
            printError("HOME environment variable is not set");

            return null;
        }

        switch (kind) {
        case DATA:
            return Paths.get(home + "/Library/" + TOOL_PREFIX + "/data");

        case CONFIG:
            return Paths.get(home + "/Library/Application Support/" + TOOL_PREFIX);

        default:
        case LOG:
            return Paths.get(home + "/Library/" + TOOL_PREFIX + "/log");
        }
    }

    //---------------------//
    // getFolderForWindows //
    //---------------------//
    private static Path getFolderForWindows (FolderKind kind)
    {
        // User Application Data
        final String appdata = System.getenv("APPDATA");

        if (appdata == null) {
            printError("APPDATA environment variable is not set");

            return null;
        }

        final Path audiverisPath = Paths.get(appdata + TOOL_PREFIX);

        // User Documents
        final String userDocs = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();

        switch (kind) {
        case DATA:
            return Paths.get(userDocs + "/" + TOOL_NAME);

        case CONFIG:
            return audiverisPath.resolve("config");

        default:
        case LOG:
            return audiverisPath.resolve("log");
        }
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
        } catch (IOException ex) {
            System.out.print("Error getting jar file " + ex);
        }

        return null;
    }

    //-----------------//
    // logDeclaredData //
    //-----------------//
    private static void logDeclaredData ()
    {
        // Note: Logger initialization has been differed until now
        final Logger logger = LoggerFactory.getLogger(WellKnowns.class);

        if (logger.isTraceEnabled()) {
            for (Field field : WellKnowns.class.getDeclaredFields()) {
                try {
                    logger.trace("{}= {}", field.getName(), field.get(null));
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    //------------//
    // printError //
    //------------//
    /**
     * Fallback solution, since we cannot reliably use exception in static initializer.
     *
     * @param msg the error message
     */
    private static void printError (String msg)
    {
        System.err.println("*** INIT_ERROR occurred in class WellKnowns: " + msg);
    }

    //----------------//
    // runningFromJar //
    //----------------//
    private static boolean runningFromJar ()
    {
        return CLASS_CONTAINER.toString().toLowerCase().endsWith(".jar");
    }

    //-------------//
    // xdgProperty //
    //-------------//
    private static String xdgProperty (FolderKind kind)
    {
        switch (kind) {
        case DATA:
            return "XDG_DATA_HOME";

        case CONFIG:
            return "XDG_CONFIG_HOME";

        default:
        case LOG:
            return "XDG_CACHE_HOME";
        }
    }
}
