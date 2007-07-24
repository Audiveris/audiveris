//  Plugins
//
//  Copyright (C) Brenton Partridge 2007.
//  All rights reserved.
//  This software is released under the GNU General Public License.
//  Contact herve.bitteur@laposte.net to report bugs & suggestions.
//----------------------------------------------------------------------------//

package omr.plugin;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import javax.swing.Action;
import omr.util.Logger;

/**
 * Handles loading, sorting, and instantiating Audiveris plugins.
 * 
 * @author Brenton Partridge
 * @version $Id$
 */
public class Plugins
{
	/** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Plugins.class);
    
    /** Class loader for plugin classes */
    private static ClassLoader classLoader = Plugins.class.getClassLoader();
    
    private static final Set<Class<? extends Action>> classes = 
    	new LinkedHashSet<Class<? extends Action>>();
    
	private static final Collection<Action> actions = new LinkedList<Action>();
	
	private static void loadClassesFromScanner(Scanner scanner)
	{
		while (scanner.hasNext()) {
			String clazzName = scanner.next();
			loadClass(clazzName);
		}
	}
	
	private static void loadClassesFromServiceProviders()
	{
		try
		{
			Enumeration<URL> urls = classLoader.getResources("META-INF/omr.plugins");
			while (urls.hasMoreElements())
			{
				URL url = urls.nextElement();
				Scanner scanner = new Scanner(url.openStream());
				loadClassesFromScanner(scanner);
			}
		}
		catch (Exception e)
		{
			logger.warning("Unable to load plugins from service providers", e);
		}
	}

	@SuppressWarnings("unchecked")
	private static void loadClass(String clazzName)
	{
		try
		{
			Class<? extends Action> clazz = 
				(Class<? extends Action>)classLoader.loadClass(clazzName);
			if (Action.class.isAssignableFrom(clazz))
			{
				if (clazz.isAnnotationPresent(Plugin.class))
				{
					classes.add(clazz);
					if (logger.isFineEnabled())
					{
						logger.fine("Loaded plugin " + clazzName);
					}					
				}
				else
				{
					logger.warning(clazzName + " not annotated as Plugin");
				}
			}
			else
			{
				logger.warning(clazzName + " not subclass of Action");
			}
		}
		catch (Exception e)
		{
			logger.warning("Plugin " + clazzName + " not found", e);
		}
	}
	
	public synchronized static Collection<Action> getActions()
	{
		if (classes.isEmpty())
		{
			loadClassesFromServiceProviders();
		}
		
		if (actions.size() != classes.size())
		{
			actions.retainAll(Collections.<Action>emptySet());
			for (Class<? extends Action> clazz : classes)
			{
				try
				{
					actions.add(clazz.newInstance());
				}
				catch (Exception e)
				{
					logger.warning(clazz + " not instantiable", e);
				}
			}
		}
		return actions;
	}
	
	public static Collection<Action> getActions(PluginType type)
	{
		Collection<Action> actions = getActions();
		Set<Action> typed = new LinkedHashSet<Action>();
		
		for (Action plugin : actions)
        {
        	Annotation ann = plugin.getClass().getAnnotation(Plugin.class);
        	if (ann != null && ((Plugin)ann).type().equals(type))
        		typed.add(plugin);
        }
		
		return typed;
	}
}
