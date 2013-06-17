//----------------------------------------------------------------------------//
//                                                                            //
//                              U r i U t i l                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 *
 * @author Hervé Bitteur
 */
public class UriUtil
{
    //~ Methods ----------------------------------------------------------------

    //--------//
    // toFile //
    //--------//
    /**
     * Convenient method to get a File access from a URL
     *
     * @param url the provided URL
     * @return the corresponding File entity
     */
    public static File toFile (URL url)
    {
        if (url == null) {
            return null;
        }

        try {
            return new File(url.toURI());
        } catch (URISyntaxException ex) {
            return new File(url.getPath());
        }
    }

    //-------//
    // toURI //
    //-------//
    /**
     * Convenient method to avoid exception burden
     *
     * @param url the initial URL
     * @return the equivalent URI
     */
    public static URI toURI (URL url)
    {
        if (url == null) {
            return null;
        }

        try {
            return url.toURI();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    //-------//
    // toURI //
    //-------//
    /**
     * Convenient method to simulate a parent/child composition
     *
     * @param parent the URI to parent directory
     * @param child  the child name
     * @return the resulting URI
     */
    public static URI toURI (URI parent,
                             String child)
    {
        try {
            // Make sure parent ends with a '/'
            if (parent == null) {
                throw new IllegalArgumentException("Parent is null");
            }

            StringBuilder dirName = new StringBuilder(parent.toString());

            if (dirName.charAt(dirName.length() - 1) != '/') {
                dirName.append('/');
            }

            // Make sure child does not start with a '/'
            if ((child == null) || child.isEmpty()) {
                throw new IllegalArgumentException("Child is null or empty");
            }

            if (child.startsWith("/")) {
                throw new IllegalArgumentException(
                        "Child is absolute: " + child);
            }

            return new URI(dirName.append(child).toString());
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    //-------//
    // toURL //
    //-------//
    /**
     * Convenient method to avoid exception burden
     *
     * @param uri the initial URI
     * @return the equivalent URL
     */
    public static URL toURL (URI uri)
    {
        if (uri == null) {
            return null;
        }

        try {
            return uri.toURL();
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }
}
