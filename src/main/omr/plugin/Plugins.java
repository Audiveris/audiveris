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

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.*;

import javax.swing.Action;

/**
 * Class <code>Plugins</code> handles loading, sorting, and instantiating
 * Audiveris plugins.
 *
 * @author Brenton Partridge
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
    private static final Set<Class<?extends Action>> classes = new LinkedHashSet<Class<?extends Action>>();

    /** Instantiated plugins */
    private static final Collection<Action> actions = new LinkedHashSet<Action>();

    static {
        /** Pre-load all plugin classes, as specified in the user config file */
        try {
            loadClassesFromScanner(
                new Scanner(
                    new File(omr.Main.getConfigFolder(), "omr.plugins")));
        } catch (Exception e) {
            logger.warning("Unable to load plugins from config folder", e);
        }
    }

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
     * Report the whole collection of plugin actions instances, with no
     * filtering
     *
     * @return all the action instances
     */
    public static synchronized Collection<Action> getActions ()
    {
        if (actions.size() != classes.size()) {
            actions.clear();

            for (Class<?extends Action> clazz : classes) {
                try {
                    actions.add(clazz.newInstance());
                } catch (Exception e) {
                    logger.warning(clazz + " not instantiable", e);
                }
            }
        }

        return actions;
    }

    //------------//
    // getActions //
    //------------//
    /**
     * Report only the plugin action instances that are annotated with the
     * provided plugin type
     *
     * @param type the desired plugin type
     * @return all actions instances for the provided type
     */
    public static Collection<Action> getActions (PluginType type)
    {
        Collection<Action> typed = new LinkedHashSet<Action>();

        for (Action plugin : getActions()) {
            Annotation ann = plugin.getClass()
                                   .getAnnotation(Plugin.class);

            if ((ann != null) && ((Plugin) ann).type()
                                  .equals(type)) {
                typed.add(plugin);
            }
        }

        return typed;
    }

    //-----------//
    // loadClass //
    //-----------//
    /**
     * Add an instance of the desired class name to the 'classes' collection,
     * provided that the class in properly annotated as a plugin
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
                    classes.add(clazz);

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
            logger.warning("Plugin " + clazzName + " not found", e);
        }
    }

    //------------------------//
    // loadClassesFromScanner //
    //------------------------//
    /**
     * Load all classes as listed by the provided scanner tokens
     *
     * @param scanner a scanner that wraps the file that lists the various class
     * names
     */
    private static void loadClassesFromScanner (Scanner scanner)
    {
        while (scanner.hasNext()) {
            String clazzName = scanner.next();
            loadClass(clazzName);
        }
    }
}
