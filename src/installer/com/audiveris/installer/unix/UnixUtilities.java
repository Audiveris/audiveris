//----------------------------------------------------------------------------//
//                                                                            //
//                          U n i x U t i l i t i e s                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer.unix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Class {@code UnixUtilities} gathers basic utilities for Unix.
 *
 * @author Hervé Bitteur
 */
public class UnixUtilities
{
    //~ Static fields/initializers ---------------------------------------------

    
    private static final Logger logger = LoggerFactory.getLogger(
            UnixUtilities.class);

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // getCommandLine //
    //----------------//
    /**
     * Report the command line for the current process.
     *
     * @return the command line, or null if failed
     */
    public static String getCommandLine ()
    {
        try {
            // Retrieve my command line
            Path cmd = Paths.get("/proc/" + getPid() + "/cmdline");

            if (Files.exists(cmd)) {
                List<String> lines = Files.readAllLines(
                        cmd,
                        StandardCharsets.US_ASCII);

                if (!lines.isEmpty()) {
                    // BEWARE: spaces between arguments have been converted to 0
                    String cmdLine = lines.get(0)
                            .replace((char) 0, ' ')
                            .trim();
                    logger.debug("cmdline: {}", cmdLine);

                    return cmdLine;
                }
            }
        } catch (IOException ex) {
            logger.warn("Error in getCommandLine()", ex);
        }

        return null;
    }

    //--------//
    // getPid //
    //--------//
    /**
     * Report the pid of the current process.
     *
     * @return the process id, as a string
     */
    public static String getPid ()
            throws IOException
    {
        String pid = new File("/proc/self").getCanonicalFile()
                .getName();
        logger.debug("pid: {}", pid);

        return pid;
    }
}
