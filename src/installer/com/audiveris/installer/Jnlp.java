//----------------------------------------------------------------------------//
//                                                                            //
//                                  J n l p                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.jnlp.BasicService;
import javax.jnlp.DownloadService;
import javax.jnlp.DownloadServiceListener;
import javax.jnlp.ExtensionInstallerService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

/**
 * Class {@code Jnlp} is a facade to JNLP services to allow
 * running with and without real JNLP services.
 *
 * @author Hervé Bitteur
 */
public class Jnlp
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Jnlp.class);

    /** Extension installer service. */
    public static final ExtensionInstallerService extensionInstallerService = new ExtensionInstallerServiceFacade();

    /** Basic service. */
    public static final BasicService basicService = new BasicServiceFacade();

    /** Download service. */
    public static final DownloadService downloadService = new DownloadServiceFacade();

    //~ Inner Classes ----------------------------------------------------------

    //--------------------//
    // BasicServiceFacade //
    //--------------------//
    private static class BasicServiceFacade
        implements BasicService
    {
        //~ Instance fields ----------------------------------------------------

        private BasicService service;

        //~ Constructors -------------------------------------------------------

        public BasicServiceFacade ()
        {
            try {
                service = (BasicService) ServiceManager.lookup(
                    "javax.jnlp.BasicService");
                logger.debug("Real BasicService available");

                // Use JNLP cache for all downloads
                JnlpResponseCache.init();
            } catch (UnavailableServiceException ex) {
                logger.debug("No BasicService available");
            }
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public URL getCodeBase ()
        {
            URL res = null;

            if (service != null) {
                res = service.getCodeBase();
            } else {
                try {
                    res = new File(".").toURI()
                                       .toURL();
                } catch (MalformedURLException ex) {
                    logger.error("URL error", ex);
                }
            }

            logger.debug("getCodeBase => {}", res);

            return res;
        }

        @Override
        public boolean isOffline ()
        {
            boolean res = false;

            if (service != null) {
                res = service.isOffline();
            }

            logger.debug("isOffline => {}", res);

            return res;
        }

        @Override
        public boolean isWebBrowserSupported ()
        {
            boolean res = false;

            if (service != null) {
                res = service.isWebBrowserSupported();
            }

            logger.debug("isWebBrowserSupported => {}", res);

            return res;
        }

        @Override
        public boolean showDocument (URL url)
        {
            boolean res = false;

            if (service != null) {
                res = service.showDocument(url);
            }

            logger.debug("showDocument url: {}, => {}", url, res);

            return res;
        }
    }

    //-----------------------//
    // DownloadServiceFacade //
    //-----------------------//
    private static class DownloadServiceFacade
        implements DownloadService
    {
        //~ Instance fields ----------------------------------------------------

        private DownloadService service;

        //~ Constructors -------------------------------------------------------

        public DownloadServiceFacade ()
        {
            try {
                service = (DownloadService) ServiceManager.lookup(
                    "javax.jnlp.DownloadService");
                logger.debug("Real DownloadService available");
            } catch (UnavailableServiceException ex) {
                logger.debug("No DownloadService available");
            }
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public DownloadServiceListener getDefaultProgressWindow ()
        {
            DownloadServiceListener res = null;

            if (service != null) {
                res = service.getDefaultProgressWindow();
            }

            logger.debug("getDefaultProgressWindow => {}", res);

            return res;
        }

        @Override
        public boolean isExtensionPartCached (URL    url,
                                              String string,
                                              String string1)
        {
            boolean res = false;

            if (service != null) {
                res = service.isExtensionPartCached(url, string, string1);
            }

            logger.debug(
                "isExtensionPartCached url: {}, string: {}, string1: {} => {}",
                url,
                string,
                string1,
                res);

            return res;
        }

        @Override
        public boolean isExtensionPartCached (URL      url,
                                              String   string,
                                              String[] strings)
        {
            boolean res = false;

            if (service != null) {
                res = service.isExtensionPartCached(url, string, strings);
            }

            logger.debug(
                "isExtensionPartCached url: {}, string: {}, strings: {} => {}",
                url,
                string,
                strings,
                res);

            return res;
        }

        @Override
        public boolean isPartCached (String string)
        {
            boolean res = false;

            if (service != null) {
                res = service.isPartCached(string);
            }

            logger.debug("isPartCached string: {} => {}", string, res);

            return res;
        }

        @Override
        public boolean isPartCached (String[] strings)
        {
            boolean res = false;

            if (service != null) {
                res = service.isPartCached(strings);
            }

            logger.debug("isPartCached strings: {} => {}", strings, res);

            return res;
        }

        @Override
        public boolean isResourceCached (URL    url,
                                         String string)
        {
            boolean res = false;

            if (service != null) {
                res = service.isResourceCached(url, string);
            }

            logger.debug(
                "isResourceCached url: {}, string: {} => {}",
                url,
                string,
                res);

            return res;
        }

        @Override
        public void loadExtensionPart (URL                     url,
                                       String                  string,
                                       String                  string1,
                                       DownloadServiceListener dl)
            throws IOException
        {
            logger.debug(
                "loadExtensionPart url: {}, string: {}, string1: {}, dl: {}",
                url,
                string,
                string1,
                dl);

            if (service != null) {
                service.loadExtensionPart(url, string, string1, dl);
            }
        }

        @Override
        public void loadExtensionPart (URL                     url,
                                       String                  string,
                                       String[]                strings,
                                       DownloadServiceListener dl)
            throws IOException
        {
            logger.debug(
                "loadExtensionPart url: {}, string: {}, strings: {}, dl: {}",
                url,
                string,
                strings,
                dl);

            if (service != null) {
                service.loadExtensionPart(url, string, strings, dl);
            }
        }

        @Override
        public void loadPart (String                  string,
                              DownloadServiceListener dl)
            throws IOException
        {
            logger.debug("loadPart string: {}, dl: {}", string, dl);

            if (service != null) {
                service.loadPart(string, dl);
            }
        }

        @Override
        public void loadPart (String[]                strings,
                              DownloadServiceListener dl)
            throws IOException
        {
            logger.debug("loadPart strings: {}, dl: {}", strings, dl);

            if (service != null) {
                service.loadPart(strings, dl);
            }
        }

        @Override
        public void loadResource (URL                     url,
                                  String                  string,
                                  DownloadServiceListener dl)
            throws IOException
        {
            logger.debug(
                "loadResource url: {}, string: {}, dl: {}",
                url,
                string,
                dl);

            if (service != null) {
                service.loadResource(url, string, dl);
            }
        }

        @Override
        public void removeExtensionPart (URL    url,
                                         String string,
                                         String string1)
            throws IOException
        {
            logger.debug(
                "removeExtensionPart url: {}, string: {}, string1: {}",
                url,
                string,
                string1);

            if (service != null) {
                service.removeExtensionPart(url, string, string1);
            }
        }

        @Override
        public void removeExtensionPart (URL      url,
                                         String   string,
                                         String[] strings)
            throws IOException
        {
            logger.debug(
                "removeExtensionPart url: {}, string: {}, strings: {}",
                url,
                string,
                strings);

            if (service != null) {
                service.removeExtensionPart(url, string, strings);
            }
        }

        @Override
        public void removePart (String string)
            throws IOException
        {
            logger.debug("removePart string: {}", string);

            if (service != null) {
                service.removePart(string);
            }
        }

        @Override
        public void removePart (String[] strings)
            throws IOException
        {
            logger.debug("removePart strings: {}", strings.toString());

            if (service != null) {
                service.removePart(strings);
            }
        }

        @Override
        public void removeResource (URL    url,
                                    String string)
            throws IOException
        {
            logger.debug("removeResource url: {}, string: {}", url, string);

            if (service != null) {
                service.removeResource(url, string);
            }
        }
    }

    //---------------------------------//
    // ExtensionInstallerServiceFacade //
    //---------------------------------//
    private static class ExtensionInstallerServiceFacade
        implements ExtensionInstallerService
    {
        //~ Instance fields ----------------------------------------------------

        /** Real service, if any. */
        private ExtensionInstallerService service;

        //~ Constructors -------------------------------------------------------

        public ExtensionInstallerServiceFacade ()
        {
            try {
                service = (ExtensionInstallerService) ServiceManager.lookup(
                    "javax.jnlp.ExtensionInstallerService");
                logger.debug("Real ExtensionInstallerService available");

                logger.debug("ExtensionLocation = {}", getExtensionLocation());
                logger.debug("InstallPath = {}", getInstallPath());
            } catch (UnavailableServiceException ex) {
                logger.debug("No ExtensionInstallerService available");
            }
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public URL getExtensionLocation ()
        {
            URL res = null;

            if (service != null) {
                res = service.getExtensionLocation();
            }

            logger.debug("getExtensionLocation => {}", res);

            return res;
        }

        @Override
        public String getExtensionVersion ()
        {
            String res = null;

            if (service != null) {
                res = service.getExtensionVersion();
            }

            logger.debug("getExtensionVersion => {}", res);

            return res;
        }

        @Override
        public String getInstallPath ()
        {
            String res = null;

            if (service != null) {
                res = service.getInstallPath();
            }

            logger.debug("getInstallPath => {}", res);

            return res;
        }

        @Override
        public String getInstalledJRE (URL    url,
                                       String string)
        {
            logger.debug("getInstalledJRE url: {} string: {}", url, string);

            String res = null;

            if (service != null) {
                res = service.getInstalledJRE(url, string);
            }

            return res;
        }

        @Override
        public void hideProgressBar ()
        {
            logger.debug("hideProgressBar");

            if (service != null) {
                service.hideProgressBar();
            }
        }

        @Override
        public void hideStatusWindow ()
        {
            logger.debug("hideStatusWindow");

            if (service != null) {
                service.hideStatusWindow();
            }
        }

        @Override
        public void installFailed ()
        {
            logger.debug("installFailed");

            if (service != null) {
                service.installFailed();
            }
        }

        @Override
        public void installSucceeded (boolean reboot)
        {
            logger.debug("installSucceeded reboot: {}", reboot);

            if (service != null) {
                service.installSucceeded(reboot);
            }
        }

        @Override
        public void setHeading (String string)
        {
            logger.debug("setHeading string: {}", string);

            if (service != null) {
                service.setHeading(string);
            }
        }

        @Override
        public void setJREInfo (String string,
                                String string1)
        {
            logger.debug("setJREInfo string: {} string1: {}", string, string1);

            if (service != null) {
                service.setJREInfo(string, string1);
            }
        }

        @Override
        public void setNativeLibraryInfo (String string)
        {
            logger.debug("setNativeLibraryInfo string: {}", string);

            if (service != null) {
                service.setNativeLibraryInfo(string);
            }
        }

        @Override
        public void setStatus (String string)
        {
            logger.debug("setStatus string: {}", string);

            if (service != null) {
                service.setStatus(string);
            }
        }

        @Override
        public void updateProgress (int i)
        {
            logger.debug("updateProgress i: {}", i);

            if (service != null) {
                service.updateProgress(i);
            }
        }
    }
}
