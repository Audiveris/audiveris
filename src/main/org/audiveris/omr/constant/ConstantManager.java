//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 C o n s t a n t M a n a g e r                                  //
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
package org.audiveris.omr.constant;

import net.jcip.annotations.ThreadSafe;

import org.audiveris.omr.CLI;
import org.audiveris.omr.Main;
import org.audiveris.omr.WellKnowns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class {@code ConstantManager} manages the persistency of the whole population of
 * Constants, including their mapping to properties, their storing on disk and their
 * reloading from disk.
 * <p>
 * The actual value of an application "constant", as returned by the method
 * {@link Constant#getCurrentString}, is determined in the following order, any definition
 * overriding the previous ones:
 * <ol> <li> First, <b>SOURCE</b> values are always provided within <em><b>source
 * declaration</b></em> of the constants in the Java source file itself.
 * For example, in the <em><b>"omr/sheet/ScaleBuilder.java"</b></em> file, we can find the following
 * declaration which defines the minimum value for sheet resolution, here specified in pixels (the
 * application has difficulties with scans of lower resolution).
 * <pre>
 * Constant.Integer minResolution = new Constant.Integer(
 *    "Pixels",
 *    11,
 *    "Minimum resolution, expressed as number of pixels per interline");
 * </pre>This declaration must be read as follows:<ul>
 *
 * <li>{@code minResolution} is the Java object used in the application.
 * It is defined as a Constant.Integer, a subtype of Constant meant to host Integer values</li>
 *
 * <li>{@code "Pixels"} specifies the unit used. Here we are counting in pixels.</li>
 *
 * <li>{@code 11} is the constant value. This is the value used by the application, provided it is
 * not overridden in the USER properties file or later via a dedicated GUI tool.</li>
 *
 * <li><code>"Minimum resolution, expressed as number of pixels per interline" </code> is the
 * constant description, which will be used as a tool tip in the GUI interface in charge of editing
 * these constants.</li></ul>
 *
 * </li><br>
 *
 * <li>Then, <b>USER</b> values, contained in a property file named <em><b>"run.properties"</b></em>
 * can assign overriding values to some constants. For example, the {@code minInterline} constant
 * above could be altered by the following line in this user file:
 * <pre>
 * omr.sheet.ScaleBuilder.minInterline=12</pre>
 * This file is modified every time the user updates the value of a constant by means of the
 * provided Constant user interface at run-time.
 * The file is not mandatory, and is located in the user application data {@code config} folder.
 * Its values override the SOURCE corresponding constants.
 * Typically, these USER values represent some modification made by the end user at run-time and
 * thus saved from one run to the other.
 * The file is not meant to be edited manually, but rather through the provided GUI tool.</li>
 * <br>
 *
 * <li>Then, <b>CLI</b> values, as set on the command line interface, by means of the
 * <em><b>"-option"</b> key=value</em> command. For further details on this command, refer to the
 * {@link org.audiveris.omr.CLI} class documentation.
 * <br>Persistency here depends on the way Audiveris is running:<ul>
 * <li>When running in <i>batch</i> mode, these CLI-defined constant values <b>are not</b> persisted
 * in the USER file, unless the constant {@code omr.Main.persistBatchCliConstants} is set to
 * true.</li>
 * <li>When running in <i>interactive</i> mode, these CLI-defined constant values <b>are</b> always
 * persisted in the USER file.</li></ul></li> <br>
 *
 * <li>Finally, <b>UI Options Menu</b> values, as set online through the graphical user interface.
 * These constant values defined at the GUI level are persisted in the USER file.</li> </ol>
 *
 * <p>
 * The whole set of constant values is stored on disk when the application is closed. Doing so, the
 * disk values are always kept in synch with the program values, <b>provided the application is
 * normally closed rather than killed</b>. It can also be stored programmatically by calling the
 * {@link #storeResource} method.
 *
 * <p>
 * Only the USER property file is written, the SOURCE values in the source code are not altered.
 * Moreover, if the user has modified a value in such a way that the final value is the same as in
 * the source, the value is simply discarded from the USER property file.
 * Doing so, the USER property file really contains only the additions of this particular user.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class ConstantManager
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            ConstantManager.class);

    /** User properties file name */
    private static final String USER_FILE_NAME = "run.properties";

    /** The singleton */
    private static final ConstantManager INSTANCE = new ConstantManager();

    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * Map of all constants created in the application, regardless whether these
     * constants are enclosed in a ConstantSet or defined as standalone entities
     */
    protected final ConcurrentHashMap<String, Constant> constants = new ConcurrentHashMap<String, Constant>();

    /** User properties */
    private final UserHolder userHolder = new UserHolder(
            WellKnowns.CONFIG_FOLDER.resolve(USER_FILE_NAME));

    //~ Constructors -------------------------------------------------------------------------------
    private ConstantManager ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // addConstant //
    //-------------//
    /**
     * Register a brand new constant with a provided name to retrieve
     * a predefined value loaded from disk backup if any.
     *
     * @param qName    the constant qualified name
     * @param constant the Constant instance to register
     * @return the loaded value if any, otherwise null
     */
    public String addConstant (String qName,
                               Constant constant)
    {
        if (qName == null) {
            throw new IllegalArgumentException("Attempt to add a constant with no qualified name");
        }

        Constant old = constants.putIfAbsent(qName, constant);

        if ((old != null) && (old != constant)) {
            throw new IllegalArgumentException("Attempt to duplicate constant " + qName);
        }

        // Value set at CLI level?
        CLI cli = Main.getCli();

        if (cli != null) {
            Properties cliConstants = cli.getOptions();

            if (cliConstants != null) {
                String cliValue = cliConstants.getProperty(qName);

                if (cliValue != null) {
                    return cliValue;
                }
            }
        }

        // Fallback on using user value
        return userHolder.getProperty(qName);
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the singleton of this class.
     *
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
     * Report the whole collection of properties (coming from USER
     * sources) backed up on disk.
     *
     * @return the collection of constant properties
     */
    public Collection<String> getAllProperties ()
    {
        SortedSet<String> props = new TreeSet<String>(userHolder.getKeys());

        return props;
    }

    //-------------------------//
    // getUnusedUserProperties //
    //-------------------------//
    /**
     * Report the collection of USER properties that do not relate to
     * any known application Constant.
     *
     * @return the potential old stuff in USER properties
     */
    public Collection<String> getUnusedUserProperties ()
    {
        return userHolder.getUnusedKeys();
    }

    //----------------//
    // removeConstant //
    //----------------//
    /**
     * Remove a constant.
     *
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
     * Stores the current content of the whole property set to disk.
     * More specifically, only the values set OUTSIDE the original Default
     * parts are stored, and they are stored in the user property file.
     */
    public void storeResource ()
    {
        userHolder.store();
    }

    //----------------------//
    // getConstantUserValue //
    //----------------------//
    String getConstantUserValue (String qName)
    {
        return userHolder.getProperty(qName);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------------//
    // AbstractHolder //
    //----------------//
    private class AbstractHolder
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Related file. */
        protected final Path path;

        /** The handled properties. */
        protected Properties properties;

        //~ Constructors ---------------------------------------------------------------------------
        public AbstractHolder (Path path)
        {
            this.path = path;
        }

        //~ Methods --------------------------------------------------------------------------------
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

                if ((constant != null) && constant.getSourceString().equals(entry.getValue())) {
                    props.add((String) entry.getKey());
                }
            }

            return props;
        }

        public void load ()
        {
            // Load from local file
            if (path != null) {
                loadFromFile();
            }
        }

        private void loadFromFile ()
        {
            try {
                InputStream in = null;

                try {
                    in = new FileInputStream(path.toFile());
                    properties.load(in);
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (FileNotFoundException ignored) {
                // This is not at all an error
                logger.debug(
                        "[{}" + "]" + " No property file {}",
                        ConstantManager.class.getName(),
                        path.toAbsolutePath());
            } catch (IOException ex) {
                logger.error("Error loading constants file {}", path.toAbsolutePath(), ex);
            }
        }
    }

    //------------//
    // UserHolder //
    //------------//
    /**
     * Triggers the loading of user property file.
     * Any modification made at run-time will be saved in the user part.
     */
    private class UserHolder
            extends AbstractHolder
    {
        //~ Constructors ---------------------------------------------------------------------------

        public UserHolder (Path path)
        {
            super(path);
            properties = new Properties();
            load();
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Remove from the USER collection the properties that are
         * already in the source with identical value,
         * and insert properties that need to reflect the current values
         * which differ from source.
         */
        public void cleanup ()
        {
            // Browse all constant entries
            for (Entry<String, Constant> entry : constants.entrySet()) {
                final String key = entry.getKey();
                final Constant constant = entry.getValue();

                final String current = constant.getCurrentString();
                final String source = constant.getSourceString();

                if (!current.equals(source)) {
                    logger.debug("Writing User value for key: {} = {}", key, current);

                    properties.setProperty(key, current);
                } else if (properties.remove(key) != null) {
                    logger.debug("Removing User value for key: {} = {}", key, current);
                }
            }
        }

        public void store ()
        {
            // First purge properties
            cleanup();

            // Then, save the remaining values
            logger.debug("Store constants into {}", path);

            try {
                // First make sure the directory exists (Brenton patch)
                Files.createDirectories(path.getParent());

                // Then write down the properties
                FileOutputStream out = null;

                try {
                    out = new FileOutputStream(path.toFile());
                    properties.store(out, " Audiveris user properties file. Do not edit");
                } catch (FileNotFoundException ex) {
                    logger.warn(
                            "Property file {} not found or not writable",
                            path.toAbsolutePath());
                } catch (IOException ex) {
                    logger.warn("Error while storing the property file {}", path.toAbsolutePath());
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            } catch (IOException ex) {
                logger.warn("Could not create folder " + path.getParent(), ex);
            }
        }
    }
}
