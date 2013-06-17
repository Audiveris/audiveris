//----------------------------------------------------------------------------//
//                                                                            //
//                            W e b B r o w s e r                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Brenton Partridge 2007.  All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import omr.WellKnowns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;

/**
 * Class {@code WebBrowser} gathers functionality to
 * browse a webpage in an external web browser. Uses
 * reflection for compatibility with Java 5 and Mac OS X.
 *
 * <p>Nota: Since using Desktop.browse() on a file under Windows crashes JVM 6,
 * this feature is currently delegated to an external and free utility named
 * BareBonesBrowserLaunch, written by Dem Pilafian.
 * See its web site on http://www.centerkey.com/java/browser/
 *
 * @author Brenton Partridge
 * @author Hervé Bitteur (for delegation to BareBonesBrowserLaunch)
 *
 */
public class WebBrowser
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(WebBrowser.class);

    /** Major browsers. */
    private static final String[] browsers = {
        "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape"
    };

    /** Singleton instance, initially null. */
    private static WebBrowser instance;

    //~ Constructors -----------------------------------------------------------
    //
    //------------//
    // WebBrowser //
    //------------//
    private WebBrowser ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // getBrowser //
    //------------//
    /**
     * Get the singleton WebBrowser implementation.
     *
     * @return a WebBrowser implementation, not null
     *         in normal operation
     */
    public static synchronized WebBrowser getBrowser ()
    {
        if (instance == null) {
            instance = setupBrowser();
        }

        return instance;
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
            logger.info(
                    "Desktop.browse {} with {} on {}", uri, this, osName);

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

    //---------//
    // openURL //
    //---------//
    /**
     * Workaround copied from BareBonesBrowserLaunch.
     *
     * @param url
     */
    private static void openURL (String url)
    {
        try {
            if (WellKnowns.MAC_OS_X) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod(
                        "openURL",
                        new Class[]{String.class});
                openURL.invoke(null, new Object[]{url});
            } else if (WellKnowns.WINDOWS) {
                Runtime.getRuntime()
                        .exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else { //assume Unix or Linux

                for (String browser : browsers) {
                    if (Runtime.getRuntime()
                            .exec(new String[]{"which", browser})
                            .waitFor() == 0) {
                        Runtime.getRuntime()
                                .exec(new String[]{browser, url});

                        return;
                    }
                }

                logger.warn("Could not find any suitable web browser");
            }
        } catch (ClassNotFoundException | NoSuchMethodException |
                SecurityException | IllegalAccessException |
                IllegalArgumentException | InvocationTargetException |
                IOException | InterruptedException ex) {
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
                        Method supported = desktopClass.getMethod(
                                "isDesktopSupported");

                        return (Boolean) supported.invoke(null);
                    } catch (Exception e) {
                        return false;
                    }
                }

                @Override
                public String toString ()
                {
                    return "WebBrowser(java.awt.Desktop)";
                }
            };
        } catch (Exception e) {
            logger.debug("java.awt.Desktop unsupported or error initializing");
        }

        //If it's not supported, see if we have the Mac FileManager
        if (WellKnowns.MAC_OS_X) {
            try {
                final Class<?> fileMgr = Class.forName(
                        "com.apple.eio.FileManager");

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
            } catch (Exception e) {
                logger.debug("Apple EIO FileManager unsupported");
            }
        }

        //Otherwise, return the no-op fallback
        return new WebBrowser();
    }
}
