//----------------------------------------------------------------------------//
//                                                                            //
//                        M a c A p p l i c a t i o n                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Brenton Partridge 2007-2008. All rights reserved.           //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.WellKnowns;

import omr.score.Score;

import omr.script.Script;
import omr.script.ScriptManager;

import org.slf4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.swing.SwingWorker;

/**
 * Class {@code MacApplication} provides dynamic hooks into the
 * OSX-only eawt package, registering Audiveris actions for the
 * Preferences, About, and Quit menu items.
 *
 * @author Brenton Partridge
 */
public class MacApplication
        implements InvocationHandler
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            MacApplication.class);

    /** Cached ApplicationEvent class */
    private static Class<?> eventClass;

    static {
        try {
            eventClass = Class.forName("com.apple.eawt.ApplicationEvent");
        } catch (Exception e) {
            eventClass = null;
        }
    }

    //~ Methods ----------------------------------------------------------------
    /**
     * Invocation handler for
     * <code>
     * com.apple.eawt.ApplicationListener</code>.
     * This method should not be manually called;
     * it is used by the proxy to forward calls.
     *
     * @throws Throwable
     */
    @Override
    public Object invoke (Object proxy,
                          Method method,
                          Object[] args)
            throws Throwable
    {
        String name = method.getName();
        String filename = null;

        Object event = getEvent(args);

        if (event != null) {
            setHandled(event);
            filename = getFilename(event);
        }

        logger.debug(name);

        switch (name) {
        case "handlePreferences":
            GuiActions.getInstance()
                    .defineOptions(null);

            break;

        case "handleQuit":
            GuiActions.getInstance()
                    .exit(null);

            break;

        case "handleAbout":
            GuiActions.getInstance()
                    .showAbout(null);

            break;

        case "handleOpenFile":
            logger.debug(filename);

            if (filename.toLowerCase()
                    .endsWith(".script")) {
                final File file = new File(filename);
                final SwingWorker<?, ?> worker = new SwingWorker<Object, Object>()
                {
                    @Override
                    protected Object doInBackground ()
                    {
                        // Actually load the script
                        logger.info("Loading script file {} ...", file);

                        try {
                            final Script script = ScriptManager.getInstance()
                                    .load(
                                    new FileInputStream(file));

                            if (logger.isDebugEnabled()) {
                                script.dump();
                            }

                            script.run();
                        } catch (Exception ex) {
                            logger.warn("Error loading script file {}", file);
                        }

                        return null;
                    }
                };

                worker.execute();
            } else {
                // Actually load the sheet picture
                Score score = new Score(new File(filename));
            }

            break;
        }

        return null;
    }

    /**
     * Registers actions for preferences, about, and quit.
     *
     * @return true if successful, false if platform is not
     *         Mac OS X or if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static boolean setupMacMenus ()
    {
        if (!WellKnowns.MAC_OS_X) {
            return false;
        }

        try {
            //The class used to register hooks
            Class<?> appClass = Class.forName("com.apple.eawt.Application");
            Object app = appClass.newInstance();

            //Enable the about menu item and the preferences menu item
            for (String methodName : new String[]{
                "setEnabledAboutMenu", "setEnabledPreferencesMenu"
            }) {
                Method method = appClass.getMethod(methodName, boolean.class);
                method.invoke(app, true);
            }

            //The interface used to register hooks
            Class<?> listenerClass = Class.forName(
                    "com.apple.eawt.ApplicationListener");

            //Using the current class loader,
            //generate, load, and instantiate a class implementing listenerClass,
            //providing an instance of this class as a callback for any method invocation
            Object listenerProxy = Proxy.newProxyInstance(
                    MacApplication.class.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    new MacApplication());

            //Add the generated class as a hook
            Method addListener = appClass.getMethod(
                    "addApplicationListener",
                    listenerClass);
            addListener.invoke(app, listenerProxy);

            return true;
        } catch (Exception ex) {
            logger.warn("Unable to setup Mac OS X GUI integration", ex);

            return false;
        }
    }

    private static Object getEvent (Object[] args)
    {
        if (args.length > 0) {
            Object arg = args[0];

            if (arg != null) {
                try {
                    if ((eventClass != null)
                        && eventClass.isAssignableFrom(arg.getClass())) {
                        return arg;
                    }
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    private static String getFilename (Object event)
    {
        try {
            Method filename = eventClass.getMethod("getFilename");
            Object rval = filename.invoke(event);

            if (rval == null) {
                return null;
            } else {
                return (String) rval;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static void setHandled (Object event)
    {
        try {
            Method handled = eventClass.getMethod("setHandled", boolean.class);
            handled.invoke(event, true);
        } catch (Exception e) {
        }
    }
}
