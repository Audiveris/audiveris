//----------------------------------------------------------------------------//
//                                                                            //
//                             A u d i v e r i s                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
import java.io.File;
import java.net.URISyntaxException;

/**
 * Class <code>Audiveris</code> is simply the entry point to OMR, which
 * delegates the call to {@link omr.Main#main}.
 * <p>Note that a few initial operations are performed here, because they need
 * to be done before any other class is loaded.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public final class Audiveris
{
    //~ Static fields/initializers ---------------------------------------------

    /** Property key used by java.util.logging to find out a property file */
    private static final String LOGGING_KEY = "java.util.logging.config.file";

    /** Name of the config folder */
    private static final String CONFIG_FOLDER_NAME = "config";

    /** Default name for the logging configuration file */
    private static final String LOGGING_CONFIG_NAME = "logging.properties";

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // Audiveris //
    //-----------//
    /** To avoid instantiation */
    private Audiveris ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // main //
    //------//
    /**
     * The main entry point, which just calls {@link omr.Main#main}
     *
     * @param args These args are simply passed to Main
     */
    public static void main (final String[] args)
    {
        try {
            /** Workaround for Swing performance problem in java 1.6.0 u10&11 */
            System.setProperty("sun.java2d.d3d", "false");

            /** Turn off JAI native acceleration */
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");

            /** Classes container, beware of escaped blanks */
            final File classContainer = new File(
                Audiveris.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            /** Installation folder is 2 folders higher than the container */
            // .../build/classes when running from classes files
            // .../dist/audiveris.jar when running from the jar archive
            final File homeFolder = classContainer.getParentFile()
                                                  .getParentFile();

            /** Config folder */
            final File configFolder = new File(homeFolder, CONFIG_FOLDER_NAME);

            /** Default logging configuration file (if none already defined) */
            if (System.getProperty(LOGGING_KEY) == null) {
                System.setProperty(
                    LOGGING_KEY,
                    new File(configFolder, LOGGING_CONFIG_NAME).toString());
            }

            /** And finally, the real start ... */
            omr.Main.main(classContainer, homeFolder, configFolder, args);
        } catch (URISyntaxException ex) {
            System.err.println("Cannot decode container, " + ex);
        }
    }
}
