//----------------------------------------------------------------------------//
//                                                                            //
//                              P l u g i n s 2                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.plugin;

import omr.util.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

/**
 * Class <code>Plugins2</code> gathers the descriptors related to a
 * collection of action plugins.
 *
 * @author Brenton Partridge
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "plugins")
public class Plugins2
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Plugins2.class);

    /** COntext for JAXB unmarshalling */
    private static JAXBContext jaxbContext;

    /** Class loader for plugin classes */
    private static final ClassLoader classLoader = Plugins2.class.getClassLoader();

    /** The collection of all plugins */
    private static final Set<Descriptor> allDescriptors = new LinkedHashSet<Descriptor>();

    //~ Instance fields --------------------------------------------------------

    @XmlElement(name = "plugin")
    public List<Descriptor> descriptors = new ArrayList<Descriptor>();

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // getAllDescriptors //
    //-------------------//
    public static Set<Descriptor> getAllDescriptors ()
    {
        return allDescriptors;
    }

    //--------------//
    // loadInstance //
    //--------------//
    /**
     *
     *
     * @param className the name of the desired class
     * @return the loaded class
     */
    @SuppressWarnings("unchecked")
    public static Object loadInstance (String className)
    {
        try {
            if (logger.isFineEnabled()) {
                logger.fine("Entering loadClass for " + className);
            }

            Class classe = classLoader.loadClass(className);

            return classe.newInstance();
        } catch (Exception ex) {
            logger.warning("Cannot get instance of " + className, ex);
        }

        return null;
    }

    //-------------//
    // loadPlugins //
    //-------------//
    /**
     * Unmarshal the provided XML stream to allocate the corresponding
     * collection of plugin descriptors
     *
     * @param in the input stream that contains the collection of plugin
     * descriptors in XML format. The stream is not closed by this method
     *
     * @return the collection of plugin descriptors, or null if failed
     */
    public static boolean loadPlugins (InputStream in)
    {
        try {
            if (jaxbContext == null) {
                jaxbContext = JAXBContext.newInstance(Plugins2.class);
            }

            Unmarshaller um = jaxbContext.createUnmarshaller();
            Plugins2     pluginSet = (Plugins2) um.unmarshal(in);

            for (Descriptor desc : pluginSet.descriptors) {
                logger.info("Descriptor unmarshalled " + desc);
            }

            allDescriptors.addAll(pluginSet.descriptors);

            return true;
        } catch (Exception ex) {
            logger.warning("Error unmarshalling plugins stream", ex);

            return false;
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // Descriptor //
    //------------//
    /**
     * Gathers all parameters for one plugin
     */
    public static class Descriptor
    {
        //~ Instance fields ----------------------------------------------------

        /** Class name */
        @XmlAttribute(name = "class")
        public final String className;

        /** Name of method within class */
        @XmlAttribute(name = "method")
        public final String methodName;

        /** Which UI section should host this action */
        @XmlElement(name = "type")
        public final PluginType type;

        /** Which kind of menu item should be generated for this action */
        @XmlElement(name = "item")
        public final String itemClassName;

        /** Should a toolbar button be generated for this action */
        @XmlElement(name = "toolbar")
        public final Boolean onToolbar;

        //~ Constructors -------------------------------------------------------

        //------------//
        // Descriptor //
        //------------//
        public Descriptor ()
        {
            className = null;
            methodName = null;
            type = null;
            itemClassName = null;
            onToolbar = null;
        }

        //~ Methods ------------------------------------------------------------

        //----------//
        // toString //
        //----------//
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{plugin");
            sb.append(" class:")
              .append(className);
            sb.append(" method:")
              .append(methodName);

            if (type != null) {
                sb.append(" type:")
                  .append(type);
            }

            if (itemClassName != null) {
                sb.append(" item:")
                  .append(itemClassName);
            }

            if (onToolbar != null) {
                sb.append(" toolbar:")
                  .append(onToolbar);
            }

            sb.append("}");

            return sb.toString();
        }
    }
}
