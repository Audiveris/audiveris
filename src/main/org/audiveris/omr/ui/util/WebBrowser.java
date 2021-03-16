//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      W e b B r o w s e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.ui.util;

import org.audiveris.omr.WellKnowns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;

/**
 * Class {@code WebBrowser} gathers functionality to browse a webpage in an external
 * web browser.
 * It uses reflection for compatibility with Java 5 and Mac OS X.
 * <p>
 * Nota: Since using Desktop.browse() on a file under Windows crashes JVM 6, this feature is
 * currently delegated to an external and free utility named BareBonesBrowserLaunch, written by Dem
 * Pilafian.
 * See its web site on http://www.centerkey.com/java/browser/
 *
 * @author Brenton Partridge
 * @author Hervé Bitteur (for delegation to BareBonesBrowserLaunch)
 */
public class WebBrowser
{

    private static final Logger logger = LoggerFactory.getLogger(WebBrowser.class);

    /** Major browsers. */
    private static final String[] browsers = {
        "firefox",
        "opera",
        "konqueror",
        "epiphany",
        "mozilla",
        "netscape"};

    /** Singleton instance, initially null. */
    private static WebBrowser instance;

    private WebBrowser ()
    {
    }

    //-------------//
    // isSupported //
    //-------------//
    /**
     * Checks if web browsing is supported by this implementation.
     *
     * @return false
     */
    public boolean isSupported ()
    {
        return false;
    }

    //--------//
    // launch //
    //--------//
    /**
     * Launches a web browser to browse a site.
     *
     * @param uri URI the browser should open.
     */
    public void launch (URI uri)
    {
        String osName = System.getProperty("os.name");

        if (true) {
            logger.info("Desktop.browse {} with {} on {}", uri, this, osName);

            try {
                Desktop desktop = Desktop.getDesktop();
                desktop.browse(uri);
            } catch (IOException ex) {
                logger.warn("Could not launch browser " + uri, ex);
            }
        } else {
            // Delegate to BareBonesBrowserLaunch-like code
            logger.info("openURL {} with {} on {}", uri, this, osName);
            openURL(uri.toString());
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "WebBrowser(unimplemented fallback)";
    }

    //------------//
    // getBrowser //
    //------------//
    /**
     * Get the singleton WebBrowser implementation.
     *
     * @return a WebBrowser implementation, not null in normal operation
     */
    public static synchronized WebBrowser getBrowser ()
    {
        if (instance == null) {
            instance = setupBrowser();
        }

        return instance;
    }

    //---------//
    // openURL //
    //---------//
    /**
     * Workaround copied from BareBonesBrowserLaunch.
     *
     * @param url
     */
    @SuppressWarnings("unchecked")
    private static void openURL (String url)
    {
        try {
            if (WellKnowns.MAC_OS_X) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[]{String.class});
                openURL.invoke(null, new Object[]{url});
            } else if (WellKnowns.WINDOWS) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else { //assume Unix or Linux

                for (String browser : browsers) {
                    if (Runtime.getRuntime().exec(new String[]{"which", browser})
                            .waitFor() == 0) {
                        Runtime.getRuntime().exec(new String[]{browser, url});

                        return;
                    }
                }

                logger.warn("Could not find any suitable web browser");
            }
        } catch (IOException |
                 ClassNotFoundException |
                 IllegalAccessException |
                 IllegalArgumentException |
                 InterruptedException |
                 NoSuchMethodException |
                 SecurityException |
                 InvocationTargetException ex) {
            logger.warn("Could not launch browser", ex);
        }
    }

    //--------------//
    // setupBrowser //
    //--------------//
    @SuppressWarnings("unchecked")
    private static WebBrowser setupBrowser ()
    {
        //First, try java.awt.Desktop
        try {
            final Class<?> desktopClass = Class.forName("java.awt.Desktop");

            return new WebBrowser()
            {
                @Override
                public boolean isSupported ()
                {
                    try {
                        Method supported = desktopClass.getMethod("isDesktopSupported");

                        return (Boolean) supported.invoke(null);
                    } catch (IllegalAccessException |
                             IllegalArgumentException |
                             NoSuchMethodException |
                             SecurityException |
                             InvocationTargetException e) {
                        return false;
                    }
                }

                @Override
                public String toString ()
                {
                    return "WebBrowser(java.awt.Desktop)";
                }
            };
        } catch (ClassNotFoundException e) {
            logger.debug("java.awt.Desktop unsupported or error initializing");
        }

        //If it's not supported, see if we have the Mac FileManager
        if (WellKnowns.MAC_OS_X) {
            try {
                Class.forName("com.apple.eio.FileManager");

                return new WebBrowser()
                {
                    @Override
                    public boolean isSupported ()
                    {
                        return true;
                    }

                    @Override
                    public String toString ()
                    {
                        return "WebBrowser(com.apple.eio.FileManager)";
                    }
                };
            } catch (ClassNotFoundException e) {
                logger.debug("Apple EIO FileManager unsupported");
            }
        }

        //Otherwise, return the no-op fallback
        return new WebBrowser();
    }
}
