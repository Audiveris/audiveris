//----------------------------------------------------------------------------//
//                                                                            //
//                               P l u g i n s                                //
//                                                                            //
//  Copyright (C) Brenton Partridge 2007. All rights reserved.                //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.plugin;

import omr.util.Logger;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.*;

import javax.swing.Action;

/**
 * Class <code>Plugins</code> handles the loading and sorting of a stream of
 * plugin classes.
 *
 * @author Brenton Partridge
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Plugins
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Plugins.class);

    /** Class loader for plugin classes */
    private static final ClassLoader classLoader = Plugins.class.getClassLoader();

    /** Set of plugin classes */
    private static final Set<Class<?extends Action>> actions = new LinkedHashSet<Class<?extends Action>>();

    //~ Constructors -----------------------------------------------------------

    //---------//
    // Plugins //
    //---------//
    /**
     * Not meant to be instantiated
     */
    private Plugins ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // getActions //
    //------------//
    /**
     * Report only the plugin action classes that are annotated with the
     * provided plugin type
     *
     * @param type the desired plugin type
     * @return all actions classes for the provided type
     */
    public static Collection<Class<?extends Action>> getActions (PluginType type)
    {
        Collection<Class<?extends Action>> typed = new LinkedHashSet<Class<?extends Action>>();

        for (Class<?extends Action> plugin : actions) {
            Annotation ann = plugin.getAnnotation(Plugin.class);

            if ((ann != null) && ((Plugin) ann).type()
                                  .equals(type)) {
                typed.add(plugin);
            }
        }

        return typed;
    }

    //-------------//
    // loadClasses //
    //-------------//
    /**
     * Load all classes whose names are listed in the provided input stream
     *
     * @param is the provided input stream
     */
    public static void loadClasses (InputStream is)
    {
        Scanner scanner = new Scanner(is);
        
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();

            if (line.length() > 0 && !line.startsWith("#")) {
                loadClass(line);
            }
        }
    }

    //-----------//
    // loadClass //
    //-----------//
    /**
     * Add the class of the desired name to the 'classes' collection, provided
     * that the class implements the Action interface and is properly annotated
     * as a plugin.
     *
     * @param clazzName the name of the desired class
     */
    @SuppressWarnings("unchecked")
    private static void loadClass (String clazzName)
    {
        try {
            Class<?extends Action> clazz = (Class<?extends Action>) classLoader.loadClass(
                clazzName);

            if (Action.class.isAssignableFrom(clazz)) {
                if (clazz.isAnnotationPresent(Plugin.class)) {
                    actions.add(clazz);

                    if (logger.isFineEnabled()) {
                        logger.fine("Loaded plugin " + clazzName);
                    }
                } else {
                    logger.warning(clazzName + " not annotated as Plugin");
                }
            } else {
                logger.warning(clazzName + " not subclass of Action");
            }
        } catch (Exception e) {
            logger.warning("Plugin '" + clazzName + "' not found", e);
        }
    }
}
