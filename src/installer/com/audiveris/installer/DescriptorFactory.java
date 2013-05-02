//----------------------------------------------------------------------------//
//                                                                            //
//                     D e s c r i p t o r F a c t o r y                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import com.audiveris.installer.unix.UnixDescriptor;
import com.audiveris.installer.windows.WindowsDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Class {@code DescriptorFactory} is in charge of returning the
 * suitable Descriptor instance for the current environment (os + arch).
 *
 * @author Hervé Bitteur
 */
public class DescriptorFactory
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            DescriptorFactory.class);

    /** Precise OS name. */
    private static final String OS_NAME = System.getProperty("os.name")
            .toLowerCase(Locale.ENGLISH);

    /** Precise OS architecture. */
    public static final String OS_ARCH = System.getProperty("os.arch")
            .toLowerCase(Locale.ENGLISH);

    /** Are we using a Linux OS?. */
    public static final boolean LINUX = OS_NAME.startsWith("linux");

    /** Are we using a Mac OS?. */
    public static final boolean MAC_OS_X = OS_NAME.startsWith("mac os x");

    /** Are we using a Windows OS?. */
    public static final boolean WINDOWS = OS_NAME.startsWith("windows");

    /** Are we using Windows on 64 bit architecture?. */
    public static final boolean WINDOWS_64 = WINDOWS
                                             && (System.getenv(
            "ProgramFiles(x86)") != null);

    /** THE descriptor instance. */
    private static Descriptor descriptor;

    //~ Constructors -----------------------------------------------------------
    /** Not meant to be instantiated. */
    private DescriptorFactory ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // geteDescriptor //
    //----------------//
    public static Descriptor getDescriptor ()
    {
        if (descriptor == null) {
            descriptor = createDescriptor();
        }

        return descriptor;
    }

    //------------------//
    // createDescriptor //
    //------------------//
    private static Descriptor createDescriptor ()
    {
        if (WINDOWS) {
            logger.debug("Creating a Windows descriptor");

            return new WindowsDescriptor();
        }

        if (LINUX) {
            logger.debug("Creating a Unix descriptor");

            return new UnixDescriptor();
        }

        // For all others...
        throw new UnsupportedEnvironmentException(
                "name: " + OS_NAME + ", arch: " + OS_ARCH);
    }
}
