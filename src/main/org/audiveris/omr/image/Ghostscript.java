//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      G h o s t s c r i p t                                     //
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
package org.audiveris.omr.image;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import static org.audiveris.omr.util.RegexUtil.*;
import org.audiveris.omr.util.WindowsRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class {@code Ghostscript} provides the path to a suitable Ghostscript executable,
 * which may depend on underlying OS and architecture, and on versions found.
 *
 * @author Hervé Bitteur
 */
public class Ghostscript
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Ghostscript.class);

    /** Path to exec. */
    private static volatile String path;

    //---------//
    // getPath //
    //---------//
    /**
     * Report the path to suitable ghostscript executable.
     *
     * @return the best path found, or an empty string if nothing found
     */
    public static String getPath ()
    {
        if (path == null) {
            if (WellKnowns.LINUX || WellKnowns.MAC_OS_X) {
                path = "gs";
            } else if (WellKnowns.WINDOWS) {
                path = getWindowsPath();
            } else {
                logger.error("OS not supported");
            }

            // To avoid endless initialization attempts
            if (path == null) {
                path = "";
            }
        }

        return path;
    }

    //--------------------//
    // getRegistryOutputs //
    //--------------------//
    /**
     * Collect the output lines from registry queries.
     *
     * @return the output lines
     */
    private static List<String> getRegistryOutputs ()
    {
        /** Radices used in registry search (32, 64 or Wow). */
        final String[] radices = new String[]{
            "HKLM\\SOFTWARE\\GPL Ghostscript", // Pure 32/32 or 64/64
            "HKLM\\SOFTWARE\\Wow6432Node\\GPL Ghostscript" // Wow (64/32)
        };

        List<String> outputs = new ArrayList<>();

        // Access registry twice, one for win32 & win64 and one for Wow
        for (String radix : radices) {
            logger.debug("Radix: {}", radix);
            outputs.addAll(WindowsRegistry.query(radix, "/s"));
        }

        return outputs;
    }

    //----------------//
    // getWindowsPath //
    //----------------//
    /**
     * Retrieve the path to suitable ghostscript executable on Windows
     * environments.
     * <p>
     * This is implemented on registry informations, using CLI "reg" command:
     * reg query "HKLM\SOFTWARE\GPL Ghostscript" /s
     *
     * @return the best suitable path, or null if nothing found
     */
    private static String getWindowsPath ()
    {
        // Group names
        final String VERSION = "version";
        final String PATH = "path";
        final String ARCH = "arch";

        /** Regex for registry key line. */
        final Pattern keyPattern = Pattern.compile(
                "^HKEY_LOCAL_MACHINE\\\\SOFTWARE\\\\(Wow6432Node\\\\)?GPL Ghostscript\\\\" + group(
                        VERSION,
                        "\\d+\\.\\d+") + "$");

        /** Regex for registry value line. */
        final Pattern valPattern = Pattern.compile(
                "^\\s+GS_DLL\\s+REG_SZ\\s+" + group(PATH, ".+") + "$");

        /** Regex for dll name. */
        final Pattern dllPattern = Pattern.compile("gsdll" + group(ARCH, "\\d+") + "\\.dll$");

        Double bestVersion = null; // Best version found so far
        String bestPath = null; // Best path found so far
        boolean relevant = false; // Is current registry info interesting?
        int index = 0; // Line number in registry outputs

        // Browse registry output lines in sequence
        for (String line : getRegistryOutputs()) {
            logger.debug("Line#{}:{}", ++index, line);

            Matcher keyMatcher = keyPattern.matcher(line);

            if (keyMatcher.matches()) {
                relevant = false;

                // Check version information
                String versionStr = getGroup(keyMatcher, VERSION);
                logger.debug("Version read as: {}", versionStr);

                Double version = Double.valueOf(versionStr);

                if ((version >= constants.minVersion.getValue()) && (version <= constants.maxVersion
                        .getValue())) {
                    // We have an acceptable version
                    if ((bestVersion == null) || (bestVersion < version)) {
                        bestVersion = version;
                        logger.debug("Best version is: {}", versionStr);
                        relevant = true;
                    } else {
                        logger.debug("Version discarded: {}", versionStr);
                    }
                } else {
                    logger.debug("Version unacceptable: {}", versionStr);
                }
            } else if (relevant) {
                Matcher valMatcher = valPattern.matcher(line);

                if (valMatcher.matches()) {
                    // Read path information
                    bestPath = getGroup(valMatcher, PATH);
                    logger.debug("Best path is: {}", bestPath);
                }
            }
        }

        // Extract prefix and dll from best path found, regardless of arch
        if (bestPath != null) {
            int lastSep = bestPath.lastIndexOf('\\');
            String prefix = bestPath.substring(0, lastSep);
            logger.debug("Prefix is: {}", prefix);

            String dll = bestPath.substring(lastSep + 1);
            logger.debug("Dll is: {}", dll);

            Matcher dllMatcher = dllPattern.matcher(dll);

            if (dllMatcher.matches()) {
                String arch = getGroup(dllMatcher, ARCH);
                String result = prefix + "\\gswin" + arch + "c.exe";
                logger.debug("Final path is: {}", result);

                return result; // Normal exit
            }
        }

        logger.warn("Could not find suitable Ghostscript software");

        return null; // Abnormal exit
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Double minVersion = new Constant.Double(
                "version",
                9.06,
                "Minimum Ghostscript acceptable version");

        private final Constant.Double maxVersion = new Constant.Double(
                "version",
                9_999,
                "Maximum Ghostscript acceptable version");
    }

    private Ghostscript ()
    {
    }
}
