//----------------------------------------------------------------------------//
//                                                                            //
//                       C o n s t a n t M a n a g e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.constant;

import omr.Main;

import omr.log.Logger;

import net.jcip.annotations.ThreadSafe;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class <code>ConstantManager</code> manages the persistency of the whole
 * population of Constants, including their mapping to properties, their storing
 * on disk and their reloading from disk.
 *
 * <p/> The actual value of an application "constant", as returned by the method
 * {@link Constant#getCurrentString}, is determined in the following order, any
 * definition overriding the previous ones:
 *
 * <ol> <li> First, <b>SOURCE</b> values are always provided within
 * <em><b>source declaration</b></em> of the constants in the Java source file
 * itself. For example, in the <em><b>"omr/sheet/ScaleBuilder.java"</b></em>
 * file, we can find the following declaration which defines the minimum value
 * for sheet interline, here specified in pixels (the application reject scans
 * with lower values).
 *
 * <pre>
 * Constant.Integer minInterline = new Constant.Integer(
 *    "Pixels",
 *    15,
 *    "Minimum number of pixels per interline");
 * </pre>This declaration must be read as follows:<ul>
 *
 * <li><code>minInterline</code> is the Java object used in the application. It
 * is defined as a Constant.Integer, a subtype of Constant meant to host Integer
 * values</li>
 *
 * <li><code>"Pixels"</code> specifies the unit used. Here we are counting in
 * pixels.</li>
 *
 * <li><code>15</code> is the constant value. This is the value used by the
 * application, provided it is not overridden in the DEFAULT or USER properties
 * files, or later via a dedicated GUI tool.</li>
 *
 * <li><code>"Minimum number of pixels per interline"</code> is the constant
 * description, which will be used as a tool tip in the GUI interface in charge
 * of editing these constants.</li></ul>
 *
 * </li><br/>
 *
 * <li> Then, <b>DEFAULT</b> values, contained in a property file named
 * <em><b>"config/run.default.properties"</b></em> can assign overriding values
 * to some constants. For example, the <code>minInterline</code> constant above
 * could be altered by the following line in this default file: <pre>
 * omr.sheet.ScaleBuilder.minInterline=12</pre> This file is mandatory, although
 * it can be empty, and must be located in the <u>config</u> subfolder of the
 * distribution file hierarchy. If this file is not found at start-up the
 * application is stopped.  Typically, this file defines values for a
 * distribution of the application, for example Linux and Windows binaries might
 * need different values for some constants. The only way to modify the content
 * of this file is to manually edit it, and this should be reserved to a
 * knowledgeable person.</li> <br/>
 *
 * <li> Finally, <b>USER</b> values, may be contained in another property file
 * named <em><b>"run.properties"</b></em>. This file is modified every time the
 * user updates the value of a constant by means of the provided Constant user
 * interface at run-time. The file is not mandatory, the user's home directory
 * (which corresponds to java property <b>user.home</b>) is searched for a
 * <b>.audiveris</b> folder to contain such file. Its values override the SOURCE
 * (and DEFAULT if any) corresponding constants. Typically, these USER values
 * represent some modification made by the end user at run-time and thus saved
 * from one run to the other. The format of the user file is the same as the
 * default file, and it is not meant to be edited manually, but rather through
 * the provided GUI tool.</li> </ol>
 *
 * <p>The difference between DEFAULT and USER, besides the fact that USER values
 * override DEFAULT values, is that there is exactly one DEFAULT file but there
 * may be zero or several USER files. They address different purposes.
 * Different users on the same machine may want to have some common Audiveris
 * technical values, while allowing separate user-related values for each
 * user. The common (shared) values should go to the DEFAULT file, while the
 * user specific values should go to the USER file.
 *
 * <p> The whole set of constant values is stored on disk when the application
 * is closed. Doing so, the disk values are always kept in synch with the
 * program values, <b>provided the application is normally closed rather than
 * killed</b>. It can also be stored programmatically by calling the
 * {@link #storeResource} method.
 *
 * <p> Only the USER property file is written, the SOURCE values in the source
 * code, or the potential overriding DEFAULT values, are not altered. Moreover,
 * if the user has modified a value in such a way that the final value is the
 * same as found in the DEFAULT file, the value is simply discarded from the
 * USER property file. Doing so, the USER property file really contains only the
 * additions of this particular user.</p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
@ThreadSafe
public class ConstantManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        ConstantManager.class);

    /** Default properties file name */
    private static final String DEFAULT_FILE_NAME = "run.default.properties";

    /** User properties file name */
    private static final String USER_FILE_NAME = "run.properties";

    /** User properties file folder */
    private static final File USER_FILE_FOLDER = new File(
        System.getProperty("user.home") +
        (Main.MAC_OS_X ? "/Library/Preferences/Audiveris" : "/.audiveris"));

    /** The singleton */
    private static final ConstantManager INSTANCE = new ConstantManager();

    //~ Instance fields --------------------------------------------------------

    /**
     * Map of all constants created in the application, regardless whether these
     * constants are enclosed in a ConstantSet or defined as standalone entities
     */
    protected final ConcurrentHashMap<String, Constant> constants = new ConcurrentHashMap<String, Constant>();

    /** Default properties */
    private final DefaultHolder defaultHolder = new DefaultHolder(
        "/" + Main.getConfigFolder().getName() + "/" + DEFAULT_FILE_NAME,
        new File(Main.getConfigFolder(), DEFAULT_FILE_NAME));

    /** User properties */
    private final UserHolder userHolder = new UserHolder(
        null,
        new File(USER_FILE_FOLDER, USER_FILE_NAME),
        defaultHolder);

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // ConstantManager //
    //-----------------//
    private ConstantManager ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the singleton of this class
     * @return the only ConstantManager instance
     */
    public static ConstantManager getInstance ()
    {
        return INSTANCE;
    }

    //------------------//
    // getAllProperties //
    //------------------//
    /**
     * Report the whole collection of properties (coming from DEFAULT and USER
     * sources) backed up on disk
     * @return the collection of constant properties
     */
    public Collection<String> getAllProperties ()
    {
        SortedSet<String> props = new TreeSet<String>(defaultHolder.getKeys());
        props.addAll(userHolder.getKeys());

        return props;
    }

    //----------------------------//
    // getUnusedDefaultProperties //
    //----------------------------//
    /**
     * Report the collection of DEFAULT properties that do not relate to any
     * known application Constant
     * @return the potential old stuff in DEFAULT properties
     */
    public Collection<String> getUnusedDefaultProperties ()
    {
        return defaultHolder.getUnusedKeys();
    }

    //-------------------------//
    // getUnusedUserProperties //
    //-------------------------//
    /**
     * Report the collection of USER properties that do not relate to any
     * known application Constant
     * @return the potential old stuff in USER properties
     */
    public Collection<String> getUnusedUserProperties ()
    {
        return userHolder.getUnusedKeys();
    }

    //-----------------------------//
    // getUselessDefaultProperties //
    //-----------------------------//
    /**
     * Report the collection of used DEFAULT properties but whose content is
     * equal to the source value (and are thus useless)
     * @return the useless items in DEFAULT properties
     */
    public Collection<String> getUselessDefaultProperties ()
    {
        return defaultHolder.getUselessKeys();
    }

    //-------------//
    // addConstant //
    //-------------//
    /**
     * Register a brand new constant with a provided name to retrieve
     * a predefined value loaded from disk backup if any
     * @param qName the constant qualified name
     * @param constant the Constant instance to register
     * @return the loaded value if any, otherwise null
     */
    public String addConstant (String   qName,
                               Constant constant)
    {
        if (qName == null) {
            throw new IllegalArgumentException(
                "Attempt to add a constant with no qualified name");
        }

        Constant old = constants.putIfAbsent(qName, constant);

        if ((old != null) && (old != constant)) {
            throw new IllegalArgumentException(
                "Attempt to duplicate constant " + qName);
        }

        return userHolder.getProperty(qName);
    }

    //----------------//
    // removeConstant //
    //----------------//
    /**
     * Remove a constant
     * @param constant the constant to remove
     * @return the removed Constant, or null if not found
     */
    public Constant removeConstant (Constant constant)
    {
        if (constant.getQualifiedName() == null) {
            throw new IllegalArgumentException(
                "Attempt to remove a constant with no qualified name defined");
        }

        return constants.remove(constant.getQualifiedName());
    }

    //---------------//
    // storeResource //
    //---------------//
    /**
     * Stores the current content of the whole property set to disk.  More
     * specifically, only the values set OUTSIDE the original Default parts are
     * stored, and they are stored in the user property file.
     */
    public void storeResource ()
    {
        userHolder.store();
    }

    //-------------------------//
    // getConstantDefaultValue //
    //-------------------------//
    String getConstantDefaultValue (String qName)
    {
        return defaultHolder.getProperty(qName);
    }

    //----------------------//
    // getConstantUserValue //
    //----------------------//
    String getConstantUserValue (String qName)
    {
        return userHolder.getProperty(qName);
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------------//
    // AbstractHolder //
    //----------------//
    private class AbstractHolder
    {
        //~ Instance fields ----------------------------------------------------

        /** Null if no related file */
        protected final File file;

        /** Null if no related resource*/
        protected final String resourceName;

        /** The handled properties */
        protected Properties properties;

        //~ Constructors -------------------------------------------------------

        public AbstractHolder (String resourceName,
                               File   file)
        {
            this.resourceName = resourceName;
            this.file = file;
        }

        //~ Methods ------------------------------------------------------------

        public Collection<String> getKeys ()
        {
            Collection<String> strings = new ArrayList<String>();

            for (Object obj : properties.keySet()) {
                strings.add((String) obj);
            }

            return strings;
        }

        public String getProperty (String key)
        {
            return properties.getProperty(key);
        }

        public Collection<String> getUnusedKeys ()
        {
            SortedSet<String> props = new TreeSet<String>();

            for (Object obj : properties.keySet()) {
                if (!constants.containsKey((String) obj)) {
                    props.add((String) obj);
                }
            }

            return props;
        }

        public Collection<String> getUselessKeys ()
        {
            SortedSet<String> props = new TreeSet<String>();

            for (Entry<Object, Object> entry : properties.entrySet()) {
                Constant constant = constants.get((String) entry.getKey());

                if ((constant != null) &&
                    constant.getSourceString()
                            .equals(entry.getValue())) {
                    props.add((String) entry.getKey());
                }
            }

            return props;
        }

        public void load ()
        {
            // First, load from resource
            if (resourceName != null) {
                loadFromResource();
            }

            // Second, load from local file
            if (file != null) {
                loadFromFile();
            }
        }

        private void loadFromFile ()
        {
            try {
                InputStream in = new FileInputStream(file);
                properties.load(in);
                in.close();
            } catch (FileNotFoundException ex) {
                // This is not at all an error
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "[" + ConstantManager.class.getName() + "]" +
                        " No property file " + file.getAbsolutePath());
                }
            } catch (IOException ex) {
                logger.severe(
                    "Error loading constants file " + file.getAbsolutePath());
            }
        }

        private void loadFromResource ()
        {
            try {
                InputStream in = Main.class.getResourceAsStream(resourceName);

                if (in != null) {
                    properties.load(in);
                    in.close();
                } else {
                    // We should have a resource available
                    logger.warning(
                        "[" + ConstantManager.class.getName() + "]" +
                        " No property resource " + resourceName);
                }
            } catch (IOException ex) {
                logger.severe(
                    "Error loading constants resource " + resourceName);
            }
        }
    }

    //---------------//
    // DefaultHolder //
    //---------------//
    private class DefaultHolder
        extends AbstractHolder
    {
        //~ Constructors -------------------------------------------------------

        public DefaultHolder (String resourceName,
                              File   file)
        {
            super(resourceName, file);
            properties = new Properties();
            load();
        }
    }

    //------------//
    // UserHolder //
    //------------//
    /**
     * Triggers the loading of property files, first the default, then the user
     * values if any. Any modification made at run-time will be saved in the
     * user part.
     */
    private class UserHolder
        extends AbstractHolder
    {
        //~ Instance fields ----------------------------------------------------

        private final AbstractHolder defaultHolder;

        //~ Constructors -------------------------------------------------------

        public UserHolder (String         resourceName,
                           File           file,
                           AbstractHolder defaultHolder)
        {
            super(resourceName, file);
            this.defaultHolder = defaultHolder;
            properties = new Properties(defaultHolder.properties);
            load();
        }

        //~ Methods ------------------------------------------------------------

        /**
         * Remove from the USER collection the properties that are already in
         * the DEFAULT collection with identical value, and insert properties
         * that need to reflect the current values which differ from DEFAULT or
         * from source.
         */
        public void cleanup ()
        {
            // Browse all constant entries
            for (Entry<String, Constant> entry : constants.entrySet()) {
                final String   key = entry.getKey();
                final Constant constant = entry.getValue();

                final String   current = constant.getCurrentString();
                final String   def = defaultHolder.getProperty(key);
                final String   source = constant.getSourceString();

                if ((def != null) && current.equals(def)) {
                    if (properties.remove(key) != null) {
                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "Removing User value for key: " + key + " = " +
                                current);
                        }
                    }
                } else if (!current.equals(source)) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Writing User value for key: " + key + " = " +
                            current);
                    }

                    properties.setProperty(key, current);
                } else {
                    if (properties.remove(key) != null) {
                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "Removing User value for key: " + key + " = " +
                                current);
                        }
                    }
                }
            }
        }

        public void store ()
        {
            // First purge properties
            cleanup();

            // Then, save the remaining values
            try {
                if (logger.isFineEnabled()) {
                    logger.fine("Store constants into " + file);
                }

                // First make sure the directory exists (Brenton patch)
                if (file.getParentFile()
                        .mkdirs()) {
                    logger.info("Creating " + file);
                }

                // Then write down the properties
                FileOutputStream out = new FileOutputStream(file);
                properties.store(
                    out,
                    " Audiveris user properties file. Do not edit");
                out.close();
            } catch (FileNotFoundException ex) {
                logger.warning(
                    "Property file " + file.getAbsolutePath() +
                    " not found or not writable");
            } catch (IOException ex) {
                logger.warning(
                    "Error while storing the property file " +
                    file.getAbsolutePath());
            }
        }
    }
}
