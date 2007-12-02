//----------------------------------------------------------------------------//
//                                                                            //
//                               MacApplication                               //
//                                                                            //
//  Copyright (C) Brenton Partridge 2007. All rights reserved.                //
//  This software is released under the GNU General Public License.           //
//  Contact herve.bitteur@laposte.net to report bugs & suggestions.           //
//----------------------------------------------------------------------------//
//

package omr.ui;

import java.lang.reflect.*;

import javax.swing.Action;

import omr.util.Implement;
import omr.util.Logger;

/**
 * Class <code>MacApplication</code> provides dynamic hooks into the 
 * OSX-only eawt package, registering Audiveris actions for the 
 * Preferences, About, and Quit menu items.
 * 
 * @author Brenton Partridge
 * @version $Id$
 */
public class MacApplication implements InvocationHandler
{
    private final Action options;
    private final Action exit;
    private final Action about;
    
    private MacApplication (Action options, Action exit, Action about)
    {
        this.options = options;
        this.exit = exit;
        this.about = about;
    }
    
    @Implement(InvocationHandler.class)
    public Object invoke (Object proxy, Method method, Object[] args)
    throws Throwable
    {
        String name = method.getName();
        if ("handlePreferences".equals(name))
        {
            logger.fine(name);
            options.actionPerformed(null);
        }
        else if ("handleQuit".equals(name))
        {
            logger.fine(name);
            exit.actionPerformed(null);
        }
        else if ("handleAbout".equals(name))
        {
            logger.fine(name);
            about.actionPerformed(null);
        }
        return null;
    }
    
    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MacApplication.class);
    
    /**
     * Registers actions for preferences, about, and quit.
     * @return true if successful, false if platform is not
     * Mac OS X or if an error occurs
     */
    public static boolean setupMacMenus()
    {
        if (!omr.Main.MAC_OS_X) return false;
        try
        {
            Class appClass = Class.forName("com.apple.eawt.Application");
            Class listenerClass = Class.forName("com.apple.eawt.ApplicationListener");
            Object app = appClass.newInstance();
            
            //Enable menus
            for (String methodName : 
                new String[] {"setEnabledAboutMenu", "setEnabledPreferencesMenu"})
            {
                Method method = appClass.getMethod(methodName, boolean.class);
                method.invoke(app, true);
            }
            
            //Add an anonymous listener
            Action about = new GuiActions.AboutAction(),
            options = new GuiActions.OptionsAction(),
            exit = new GuiActions.ExitAction();
            Object listenerProxy = Proxy.newProxyInstance(
                MacApplication.class.getClassLoader(), 
                new Class[] {listenerClass},
                new MacApplication(options, exit, about));
            Method addListener = appClass.getMethod("addApplicationListener", listenerClass);
            addListener.invoke(app, listenerProxy);
            return true;
        }
        catch (Exception ex)
        {
            logger.warning("Unable to setup Mac OS X GUI integration", ex);
            return false;
        }
    }
}
