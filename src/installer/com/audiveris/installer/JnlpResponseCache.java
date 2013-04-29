//----------------------------------------------------------------------------//
//                                                                            //
//                      J n l p R e s p o n s e C a c h e                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import java.io.IOException;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import javax.jnlp.DownloadService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

/**
 * Class {@code JnlpResponseCache} enables the Java Cache for JNLP.
 * You need to call once JnlpResponseCache.init() in your code to enable it.
 *
 * @author horcrux7 http://stackoverflow.com/users/12631/horcrux7
 */
public class JnlpResponseCache
    extends ResponseCache
{
    //~ Instance fields --------------------------------------------------------

    private final DownloadService service;

    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // JnlpResponseCache //
    //-------------------//
    private JnlpResponseCache ()
    {
        try {
            service = (DownloadService) ServiceManager.lookup(
                "javax.jnlp.DownloadService");
        } catch (UnavailableServiceException ex) {
            throw new NoClassDefFoundError(ex.toString());
        }
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // CacheResponse //
    //---------------//
    @Override
    public CacheResponse get (URI                       uri,
                              String                    rqstMethod,
                              Map<String, List<String>> rqstHeaders)
        throws IOException
    {
        return null;
    }

    //--------------//
    // CacheRequest //
    //--------------//
    @Override
    public CacheRequest put (URI           uri,
                             URLConnection conn)
        throws IOException
    {
        URL url = uri.toURL();
        service.loadResource(url, null, service.getDefaultProgressWindow());

        return null;
    }

    //------//
    // init //
    //------//
    static void init ()
    {
        if (ResponseCache.getDefault() == null) {
            ResponseCache.setDefault(new JnlpResponseCache());
        }
    }
}
