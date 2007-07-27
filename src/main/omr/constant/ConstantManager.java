//----------------------------------------------------------------------------//
//                                                                            //
//                       C o n s t a n t M a n a g e r                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.constant;

import omr.Main;

import omr.util.Logger;

import java.io.*;
import java.util.*;

/**
 * Class <code>ConstantManager</code> manages the whole population of Constants,
 * including their mapping to properties, their storing on disk and their
 * reloading from disk.
 *
 * <p/> The actual value of an application "constant", as returned by the method
 * {@link Constant#currentString}, is determined in the following order, any
 * definition overriding the previous ones:
 *
 * <ol> <li> First, <b>SOURCE</b> values are always provided within
 * <em><b>source declaration</b></em> of the constants in the Java source file
 * itself. For example, in the "omr/sheet/ScaleBuilder.java" file, we can find
 * the following declaration which defines the minimum value for sheet
 * interline, here specified in pixels (we reject scans with lower values).
 *
 * <pre>
 * Constant.Integer minInterline = new Constant.Integer(
 *    "Pixels",
 *    15,
 *    "Minimum number of pixels per interline");
 * </pre>
 * </li>
 *
 * <li> Then, <b>DEFAULT</b> values, contained in a property file named
 * <em><b>"config/run.default.properties"</b></em> can assign overriding values
 * to some constants. For example, the <code>minInterline</code> constant above
 * could be altered by the following line in this default file: <pre>
 * omr.sheet.ScaleBuilder.minInterline=12</pre> This file is mandatory, although
 * it can be empty, and must be located in the <u>config</u> folder (either in
 * the jar file, or in the distribution file hierarchy). If this file is not
 * found at start-up the application is stopped.  Typically, these DEFAULT
 * values define values for a distribution of the application, for example Linux
 * and Windows binaries might need different values for some constants. The only
 * way to modify the content of this file is to manually edit it, and this
 * should be reserved to omr developer.</li> <br/>
 *
 * <li> Finally, <b>USER</b> values, may be contained in another property file
 * named <em><b>"run.properties"</b></em>. This file is modified every time the
 * user updates the value of a constant by means of the provided Constant user
 * interface at run-time. The file is not mandatory, the user's home directory
 * (which corresponds to java property <b>user.home</b>) is searched for a
 * <b>.audiveris</b> folder to contain such file. Its values override the Source
 * (and Default if any) corresponding constants. Typically, these USER values
 * represent some modification made by the end user at run-time and thus saved
 * from one run to the other. The format of the user file is the same as the
 * default file, and it is not meant to be edited manually, but rather through
 * the provided GUI tool. </li> </ol>
 *
 * <p> The whole set of constant values is stored on disk when the application
 * is closed. Doing so, the disk values are always kept in synch with the
 * program values, <b>provided the application is closed rather than
 * killed</b>. It can also be stored by manually calling the method
 * <code>storeResource</code>.
 *
 * <p> Only the user property file is written, the source value in the source
 * code, or the potential overriding default values, are not altered.
 *
 * </p> Moreover, if the user has modified a value in such a way that the final
 * value is the same as found in the default stage, the value is simply
 * discarded from the user property file. Doing so, the user property file
 * really contains only the additions of the user. </p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ConstantManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        ConstantManager.class);

    /** Default properties */
    private static Properties defaultProperties = new Properties();

    /** Default properties file (or resource) name */
    private static String DEFAULT_FILE_NAME = "/config/run.default.properties";

    /** User properties */
    private static Properties userProperties = null;

    /** User properties file name */
    private static String USER_FILE_NAME = System.getProperty("user.home") +
                                           "/.audiveris/run.properties";

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // ConstantManager //
    //-----------------//
    // Not meant to be instantiated, we stay static
    private ConstantManager ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // storeResource //
    //---------------//
    /**
     * Stores the current content of the whole property set to disk.  More
     * specifically, only the values set OUTSIDE the original Default parts are
     * stored, and they are stored in the user property file.
     */
    public static void storeResource ()
    {
        // First purge USER properties that are equal to DEFAULT properties
        purgeUserProperties();

        // Then, save the remaining USER values
        try {
            if (logger.isFineEnabled()) {
                logger.fine("Store constants into " + USER_FILE_NAME);
            }

            // First make sure the directory exists (Brenton patch)
            if (new File(USER_FILE_NAME).getParentFile()
                                        .mkdirs()) {
                logger.info("Creating " + USER_FILE_NAME);
            }

            // Then write down the user properties
            FileOutputStream out = new FileOutputStream(USER_FILE_NAME);
            userProperties.store(
                out,
                " User Audiveris run properties file. Do not edit");
            out.close();
        } catch (FileNotFoundException ex) {
            logger.warning(
                "User property file " + USER_FILE_NAME +
                " not found or not writable");
        } catch (IOException ex) {
            logger.warning(
                "Error while storing the User property file " + USER_FILE_NAME);
        }
    }

    //-------------//
    // setProperty //
    //-------------//
    /**
     * Meant to be called by <code>Constant</code> class, to update the property
     * value that relates to the given constant.
     *
     * @param qualifiedName name of the constant
     * @param val           the new value
     */
    static void setProperty (String qualifiedName,
                             String val)
    {
        getUserProperties()
            .setProperty(qualifiedName, val);
    }

    //-------------//
    // getProperty //
    //-------------//
    /**
     * Meant to be called by <code>Constant</code> class, to retrieve the
     * property value that relates to the constant.
     *
     * @param qualifiedName name of the constant
     *
     * @return the property value as a string
     */
    static String getProperty (String qualifiedName)
    {
        return getUserProperties()
                   .getProperty(qualifiedName);
    }

    //----------------//
    // removeProperty //
    //----------------//
    static void removeProperty (String qualifiedName)
    {
        getUserProperties()
            .remove(qualifiedName);
    }

    //-------------------//
    // getUserProperties //
    //-------------------//
    /**
     * Make sure the properties have been loaded
     *
     * @return The global properties
     */
    private static Properties getUserProperties ()
    {
        if (userProperties == null) {
            loadResource();
        }

        return userProperties;
    }

    //--------------//
    // loadResource //
    //--------------//
    /**
     * Triggers the loading of property files, first the default, then the user
     * values if any. Any modification made at run-time will be saved in the
     * user part.
     */
    private static void loadResource ()
    {
        // Load DEFAULT properties from the distribution
        try {
            InputStream in = Main.class.getResourceAsStream(DEFAULT_FILE_NAME);
            defaultProperties.load(in);
            in.close();

            ///logger.info("Loaded default constants from resource " + DEFAULT_FILE_NAME);
        } catch (Exception ex) {
            logger.severe(
                "Error while loading DEFAULT resource as " + DEFAULT_FILE_NAME);
        }

        // Create program properties with default properties as default
        userProperties = new Properties(defaultProperties);

        // Now load properties from last invocation, if any
        try {
            FileInputStream in = new FileInputStream(USER_FILE_NAME);
            userProperties.load(in);
            in.close();

            ///logger.info("Loaded user constants from file " + USER_FILE_NAME);
        } catch (FileNotFoundException ex) {
            // This is not at all a fatal error, let the user know this.
            logger.info(
                "[" + ConstantManager.class.getName() + "]" +
                " No User property file " + USER_FILE_NAME);
        } catch (IOException ex) {
            logger.severe(
                "Error while loading the User property file " + USER_FILE_NAME);
        }
    }

    //---------------------//
    // purgeUserProperties //
    //---------------------//
    private static void purgeUserProperties ()
    {
        // Browse the content of userProperties
        for (Enumeration keys = userProperties.propertyNames();
             keys.hasMoreElements();) {
            java.lang.String key = (java.lang.String) keys.nextElement();
            java.lang.String value = userProperties.getProperty(key);

            // Does this key exist and with same value in the DEFAULT part
            java.lang.String defaultValue = defaultProperties.getProperty(key);

            if (defaultValue != null) {
                if (value.equals(defaultValue)) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Removing identical User" +
                            " and Default value for key : " + key + " = " +
                            value);
                    }

                    userProperties.remove(key);
                }
            }
        }
    }
}
