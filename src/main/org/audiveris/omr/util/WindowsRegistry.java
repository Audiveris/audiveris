//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   W i n d o w s R e g i s t r y                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.util;

import org.audiveris.omr.WellKnowns;

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
 * Class {@code WindowsRegistry} provides a basic interface to Windows registry,
 * implemented to top on CLI "reg" command.
 *
 * @author Hervé Bitteur
 */
public class WindowsRegistry
{

    private static final Logger logger = LoggerFactory.getLogger(WindowsRegistry.class);

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
        List<String> output = new ArrayList<String>();

        // Command arguments
        List<String> cmdArgs = new ArrayList<String>();
        cmdArgs.addAll(Arrays.asList("cmd.exe", "/c", "reg", "query"));
        cmdArgs.addAll(Arrays.asList(args));
        logger.debug("cmdArgs: {}", cmdArgs);

        BufferedReader br = null;

        try {
            // Spawn cmd process
            ProcessBuilder pb = new ProcessBuilder(cmdArgs);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, WellKnowns.FILE_ENCODING);
            br = new BufferedReader(isr);

            String line;

            while ((line = br.readLine()) != null) {
                output.add(line);
            }

            // Wait for process completion
            int exitValue = process.waitFor();
            logger.debug("Exit value is: {}", exitValue);
        } catch (RuntimeException rex) {
            throw rex;
        } catch (Exception ex) {
            logger.warn("Error running " + cmdArgs, ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    logger.warn("Error closing cmd reader {}", ex.toString(), ex);
                }
            }
        }

        return output;
    }
}
