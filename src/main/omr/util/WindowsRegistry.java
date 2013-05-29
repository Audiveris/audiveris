//----------------------------------------------------------------------------//
//                                                                            //
//                         W i n d o w s R e g i s t r y                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;

/**
 * Class {@code WindowsRegistry} provides a basic interface to
 * Windows registry, implemented to top on CLI "reg" command.
 *
 * @author Hervé Bitteur
 */
public class WindowsRegistry
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            WindowsRegistry.class);

    //~ Methods ----------------------------------------------------------------
    //-------//
    // query //
    //-------//
    /**
     * Send a query to registry and return the output lines.
     *
     * @param args query arguments
     * @return the output lines
     */
    public static List<String> query (String... args)
    {
        // Output lines
        List<String> output = new ArrayList<>();

        // Command arguments
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.addAll(Arrays.asList(
                "cmd.exe", "/c", "reg", "query"));
        cmdArgs.addAll(Arrays.asList(args));
        logger.debug("cmdArgs: {}", cmdArgs);

        try {
            // Spawn cmd process
            ProcessBuilder pb = new ProcessBuilder(cmdArgs);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                output.add(line);
            }

            // Wait for process completion
            int exitValue = process.waitFor();
            logger.debug("Exit value is: {}", exitValue);
        } catch (IOException | InterruptedException ex) {
            logger.warn("Error running " + cmdArgs, ex);
        }

        return output;
    }
}
