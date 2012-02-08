//----------------------------------------------------------------------------//
//                                                                            //
//                            W e l l K n o w n s                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.log.Logger;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Class {@code WellKnowns} gathers top public static final data to be
 * shared within omr application.
 *
 * <p>Note that a few initial operations are performed here, because they need
 * to be done before any other class is loaded.
 *
 * @author Hervé Bitteur
 */
public class WellKnowns
{
    //~ Static fields/initializers ---------------------------------------------

    /** Are we using a Linux OS? */
    public static final boolean LINUX = System.getProperty("os.name")
                                              .toLowerCase()
                                              .startsWith("linux");

    /** Are we using a Mac OS? */
    public static final boolean MAC_OS_X = System.getProperty("os.name")
                                                 .toLowerCase()
                                                 .startsWith("mac os x");

    /** Are we using a Windows OS? */
    public static final boolean WINDOWS = System.getProperty("os.name")
                                                .toLowerCase()
                                                .startsWith("windows");

    /** The container from which the application classes were loaded */
    public static final File CLASS_CONTAINER;

    static {
        try {
            /** Classes container, beware of escaped blanks */
            CLASS_CONTAINER = new File(
                WellKnowns.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException ex) {
            System.err.println("Cannot decode container, " + ex);
            throw new RuntimeException(ex);
        }
    }

    /** Installation folder is 2 folders higher than the container */
    // .../build/classes when running from classes files
    // .../dist/audiveris.jar when running from the jar archive
    public static final File HOME_FOLDER = CLASS_CONTAINER.getParentFile()
                                                          .getParentFile();

    /** Specific folder name for configuration data */
    public static final String CONFIG_FOLDER_NAME = "config";

    /** The folder where configuration data is stored */
    public static final File CONFIG_FOLDER = new File(
        HOME_FOLDER,
        CONFIG_FOLDER_NAME);

    /** Specific folder name for symbols */
    public static final String SYMBOLS_FOLDER_NAME = "symbols";

    /** The folder where custom-defined symbols are stored */
    public static final File SYMBOLS_FOLDER = new File(
        HOME_FOLDER,
        SYMBOLS_FOLDER_NAME);

    /** Specific folder name for OCR utility */
    public static final String OCR_FOLDER_NAME = "ocr";

    /** The folder where Tesseract OCR material is stored */
    public static final File OCR_FOLDER = new File(
        HOME_FOLDER,
        OCR_FOLDER_NAME);

    /** Specific folder name for plugins */
    public static final String PLUGINS_FOLDER_NAME = "plugins";

    /** The folder where plugin scripts are found */
    public static final File PLUGINS_FOLDER = new File(
        HOME_FOLDER,
        PLUGINS_FOLDER_NAME);

    /** Specific folder name for documentation */
    public static final String DOC_FOLDER_NAME = "www";

    /** The folder where documentations files are stored */
    public static final File DOC_FOLDER = new File(
        HOME_FOLDER,
        DOC_FOLDER_NAME);

    /** Specific folder name for training data */
    public static final String TRAIN_FOLDER_NAME = "train";

    /** The folder where training material is stored */
    public static final File TRAIN_FOLDER = new File(
        HOME_FOLDER,
        TRAIN_FOLDER_NAME);

    /** Default name for the logging configuration file */
    public static final String LOGGING_CONFIG_NAME = "logging.properties";

    static {
        /** Workaround for Swing performance problem in java 1.6.0 u10 et al. */
        System.setProperty("sun.java2d.d3d", "false");

        /** Turn off JAI native acceleration */
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");

        /** Default logging configuration file (if none already defined) */
        String LOGGING_KEY = "java.util.logging.config.file";

        if (System.getProperty(LOGGING_KEY) == null) {
            System.setProperty(
                LOGGING_KEY,
                new File(CONFIG_FOLDER, LOGGING_CONFIG_NAME).toString());
        }

        /** Set up logger mechanism */
        Logger.getLogger(WellKnowns.class);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // ensureLoaded //
    //--------------//
    /**
     * Make sure this class is loaded
     */
    public static void ensureLoaded ()
    {
    }
}
