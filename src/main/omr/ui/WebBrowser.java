//----------------------------------------------------------------------------//
//                                                                            //
//                            W e b B r o w s e r                             //
//                                                                            //
//  Copyright (C) Brenton Partridge 2007.  All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.ui;

import omr.Main;

import omr.log.Logger;

import java.lang.reflect.*;
import java.net.URI;

/**
 * Class <code>WebBrowser</code> gathers functionality to
 * browse a webpage in an external web browser. Uses
 * reflection for compatibility with Java 5 and Mac OS X.
 *
 * @author Brenton Partridge
 * @version $Id$
 */
public class WebBrowser
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(WebBrowser.class);

    /** Singleton instance, initially null */
    private static WebBrowser instance;

    //~ Constructors -----------------------------------------------------------

    private WebBrowser ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Checks if web browsing is supported by this implementation.
     */
    public boolean isSupported ()
    {
        return false;
    }

    /**
     * Launches a web browser to browse a site.
     * @param urlString Location to which the browser should browse.
     */
    public void launch (String urlString)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Browsing " + urlString + " using " + toString());
        }
    }

    @Override
    public String toString ()
    {
        return "WebBrowser(unimplemented fallback)";
    }

    /**
     * Get the singleton WebBrowser implementation.
     * @return a WebBrowser implementation, not null
     * in normal operation
     */
    public static synchronized WebBrowser getBrowser ()
    {
        if (instance == null) {
            instance = setupBrowser();
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    private static WebBrowser setupBrowser ()
    {
        //First, try java.awt.Desktop
        try {
            final Class desktopClass = Class.forName("java.awt.Desktop");

            return new WebBrowser() {
                    @Override
                    public boolean isSupported ()
                    {
                        try {
                            Method supported = desktopClass.getMethod(
                                "isDesktopSupported");

                            return (Boolean) supported.invoke(null);
                        } catch (Exception e) {
                            return false;
                        }
                    }

                    @Override
                    public void launch (String urlString)
                    {
                        super.launch(urlString);

                        try {
                            URI    uri = URI.create(urlString);
                            Method getDesktop = desktopClass.getMethod(
                                "getDesktop");
                            Object desktop = getDesktop.invoke(null);
                            desktopClass.getMethod("browse", URI.class)
                                        .invoke(desktop, uri);
                        } catch (Exception ex) {
                            logger.warning(
                                "Could not launch the browser on " + urlString,
                                (ex instanceof InvocationTargetException)
                                                                ? ex.getCause()
                                                                : ex);
                        }
                    }

                    @Override
                    public String toString ()
                    {
                        return "WebBrowser(java.awt.Desktop)";
                    }
                };
        } catch (Exception e) {
            logger.fine("java.awt.Desktop unsupported or error initializing");
        }

        //If it's not supported, see if we have the Mac FileManager
        if (Main.MAC_OS_X) {
            try {
                final Class fileMgr = Class.forName(
                    "com.apple.eio.FileManager");

                return new WebBrowser() {
                        @Override
                        public boolean isSupported ()
                        {
                            return true;
                        }

                        @Override
                        public void launch (String urlString)
                        {
                            super.launch(urlString);

                            try {
                                Method openURL = fileMgr.getDeclaredMethod(
                                    "openURL",
                                    String.class);
                                openURL.invoke(null, urlString);
                            } catch (Exception ex) {
                                logger.warning(
                                    "Could not launch the browser on " +
                                    urlString,
                                    (ex instanceof InvocationTargetException)
                                                                        ? ex.getCause()
                                                                        : ex);
                            }
                        }

                        @Override
                        public String toString ()
                        {
                            return "WebBrowser(com.apple.eio.FileManager)";
                        }
                    };
            } catch (Exception e) {
                logger.fine("Apple EIO FileManager unsupported");
            }
        }

        //Otherwise, return the no-op fallback
        return new WebBrowser();
    }
}
