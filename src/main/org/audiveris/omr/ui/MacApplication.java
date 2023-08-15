//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  M a c A p p l i c a t i o n                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.ui;

import org.audiveris.omr.OMR;
import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.util.UriUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Image;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Paths;

import javax.swing.ImageIcon;

/**
 * Class <code>MacApplication</code> provides dynamic hooks into the
 * OSX-only eawt package, registering Audiveris actions for the
 * Preferences, About, and Quit menu items.
 *
 * @author Brenton Partridge
 */
public class MacApplication
        implements InvocationHandler
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MacApplication.class);

    /** Cached ApplicationEvent class */
    private static Class<?> eventClass;

    static {
        try {
            eventClass = Class.forName("com.apple.eawt.ApplicationEvent");
        } catch (ClassNotFoundException e) {
            eventClass = null;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Invocation handler for
     * <code>
     * com.apple.eawt.ApplicationListener</code>.
     * This method should not be manually called;
     * it is used by the proxy to forward calls.
     *
     * @throws Throwable if something goes wrong
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
            GuiActions.getInstance().defineOptions(null);

            break;

        case "handleQuit":
            GuiActions.getInstance().exit(null);

            break;

        case "handleAbout":
            GuiActions.getInstance().showAbout(null);

            break;

        case "handleOpenFile":
            logger.debug(filename);

            // Actually load the book
            Book book = OMR.engine.loadInput(Paths.get(filename));
            book.createStubs();
            if (OMR.gui != null) {
                StubsController.getInstance().displayStubs(book, null);
            }

            break;

        default:
            break;
        }

        return null;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    private static Object getEvent (Object[] args)
    {
        if (args.length > 0) {
            Object arg = args[0];

            if (arg != null) {
                try {
                    if ((eventClass != null) && eventClass.isAssignableFrom(arg.getClass())) {
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
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
            return null;
        }
    }

    private static void setHandled (Object event)
    {
        try {
            Method handled = eventClass.getMethod("setHandled", boolean.class);
            handled.invoke(event, true);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException
                | SecurityException | InvocationTargetException e) {
        }
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
            for (String methodName : new String[]
            { "setEnabledAboutMenu", "setEnabledPreferencesMenu" }) {
                Method method = appClass.getMethod(methodName, boolean.class);
                method.invoke(app, true);
            }

            //The interface used to register hooks
            Class<?> listenerClass = Class.forName("com.apple.eawt.ApplicationListener");

            //Using the current class loader,
            //generate, load, and instantiate a class implementing listenerClass,
            //providing an instance of this class as a callback for any method invocation
            Object listenerProxy = Proxy.newProxyInstance(
                    MacApplication.class.getClassLoader(),
                    new Class<?>[]
                    { listenerClass },
                    new MacApplication());

            //Add the generated class as a hook
            Method addListener = appClass.getMethod("addApplicationListener", listenerClass);
            addListener.invoke(app, listenerProxy);

            // display audiveris icon in the dock instead of default java one
            Method getApplication = appClass.getMethod("getApplication");
            Object application = getApplication.invoke(app);

            URI uri = UriUtil.toURI(WellKnowns.RES_URI, "icon-256.png");
            Image icon = new ImageIcon(uri.toURL()).getImage();

            Method setDockImage = application.getClass().getMethod("setDockIconImage", Image.class);
            setDockImage.invoke(application, icon);

            return true;
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException
                | InstantiationException | NoSuchMethodException | SecurityException
                | InvocationTargetException | MalformedURLException ex) {
            logger.warn("Unable to setup Mac OS X GUI integration", ex);

            return false;
        }
    }
}
