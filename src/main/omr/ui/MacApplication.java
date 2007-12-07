//----------------------------------------------------------------------------//
//                                                                            //
//                         M a c A p p l i c a t i o n                        //
//                                                                            //
//  Copyright (C) Brenton Partridge 2007. All rights reserved.                //
//  This software is released under the GNU General Public License.           //
//  Contact herve.bitteur@laposte.net to report bugs & suggestions.           //
//----------------------------------------------------------------------------//
//
package omr.ui;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.event.ActionListener;
import java.lang.reflect.*;

/**
 * Class <code>MacApplication</code> provides dynamic hooks into the
 * OSX-only eawt package, registering Audiveris actions for the
 * Preferences, About, and Quit menu items.
 *
 * @author Brenton Partridge
 * @version $Id$
 */
public class MacApplication
    implements InvocationHandler
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MacApplication.class);

    //~ Instance fields --------------------------------------------------------

    private final ActionListener options = new GuiActions.OptionsAction();
    private final ActionListener exit = new GuiActions.ExitAction();
    private final ActionListener about = new GuiActions.AboutAction();

    //~ Methods ----------------------------------------------------------------

    /**
     * Invocation handler for <code>
     * com.apple.eawt.ApplicationListener</code>.
     * This method should not be manually called;
     * it is used by the proxy to forward calls.
     * @throws Throwable
     */
    @Implement(InvocationHandler.class)
    public Object invoke (Object   proxy,
                          Method   method,
                          Object[] args)
        throws Throwable
    {
        String name = method.getName();

        if ("handlePreferences".equals(name)) {
            logger.fine(name);
            options.actionPerformed(null);
        } else if ("handleQuit".equals(name)) {
            logger.fine(name);
            exit.actionPerformed(null);
        } else if ("handleAbout".equals(name)) {
            logger.fine(name);
            about.actionPerformed(null);
        }

        return null;
    }

    /**
     * Registers actions for preferences, about, and quit.
     * @return true if successful, false if platform is not
     * Mac OS X or if an error occurs
     */
    public static boolean setupMacMenus ()
    {
        if (!omr.Main.MAC_OS_X) {
            return false;
        }

        try {
            //The class used to register hooks
            Class  appClass = Class.forName("com.apple.eawt.Application");
            Object app = appClass.newInstance();

            //Enable the about menu item and the preferences menu item
            for (String methodName : new String[] {
                     "setEnabledAboutMenu", "setEnabledPreferencesMenu"
                 }) {
                Method method = appClass.getMethod(methodName, boolean.class);
                method.invoke(app, true);
            }

            //The interface used to register hooks
            Class  listenerClass = Class.forName(
                "com.apple.eawt.ApplicationListener");

            //Using the current class loader,
            //generate, load, and instantiate a class implementing listenerClass,
            //providing an instance of this class as a callback for any method invocation
            Object listenerProxy = Proxy.newProxyInstance(
                MacApplication.class.getClassLoader(),
                new Class[] { listenerClass },
                new MacApplication());

            //Add the generated class as a hook
            Method addListener = appClass.getMethod(
                "addApplicationListener",
                listenerClass);
            addListener.invoke(app, listenerProxy);

            return true;
        } catch (Exception ex) {
            logger.warning("Unable to setup Mac OS X GUI integration", ex);

            return false;
        }
    }
}
