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

import omr.log.Logger;

import com.centerkey.utils.BareBonesBrowserLaunch;

import java.lang.reflect.*;

/**
 * Class <code>WebBrowser</code> gathers functionality to
 * browse a webpage in an external web browser. Uses
 * reflection for compatibility with Java 5 and Mac OS X.
 *
 * <p>Nota: Since using Desktop.browse() on a file under Windows crashes JVM 6,
 * this feature is currently delegated to an external and free utility named
 * BareBonesBrowserLaunch, written by Dem Pilafian.
 * See its web site on http://www.centerkey.com/java/browser/
 *
 * @author Brenton Partridge
 * @author Herv&eacute Bitteur (for delegation to BareBonesBrowserLaunch)
 *
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

    //-------------//
    // isSupported //
    //-------------//
    /**
     * Checks if web browsing is supported by this implementation.
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
     * @param urlString Location to which the browser should browse.
     */
    public void launch (String urlString)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Browsing " + urlString + " using " + toString());
        }

        // Delegate to BareBonesBrowserLaunch
        BareBonesBrowserLaunch.openURL(urlString);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "WebBrowser(unimplemented fallback)";
    }

    //--------------//
    // setupBrowser //
    //--------------//
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
        if (WellKnowns.MAC_OS_X) {
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
