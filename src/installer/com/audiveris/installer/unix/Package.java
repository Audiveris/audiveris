//----------------------------------------------------------------------------//
//                                                                            //
//                                P a c k a g e                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer.unix;

import com.audiveris.installer.Installer;
import com.audiveris.installer.RegexUtil;
import com.audiveris.installer.Utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class {@code Package} handles a Linux package requirement and
 * installation, using the underlying "apt" and dpkg" utilities.
 *
 * @author Hervé Bitteur
 */
public class Package
{
    //~ Static fields/initializers ---------------------------------------------

    /**
     * Usual logger utility
     */
    private static final Logger logger = LoggerFactory.getLogger(Package.class);

    //~ Instance fields --------------------------------------------------------
    /**
     * Name of the package.
     */
    public final String name;

    /**
     * Minimum version required.
     */
    public final VersionNumber minVersion;

    //~ Constructors -----------------------------------------------------------
    //---------//
    // Package //
    //---------//
    /**
     * Create a Package instance.
     *
     * @param name       the package name
     * @param minVersion the minimum required version
     */
    public Package (String name,
                    String minVersion)
    {
        if ((name == null) || name.isEmpty()) {
            throw new IllegalArgumentException("Null or empty package name");
        }

        if ((minVersion == null) || minVersion.isEmpty()) {
            throw new IllegalArgumentException(
                    "Null or empty package minimum version");
        }

        this.name = name;
        this.minVersion = new VersionNumber(minVersion);
    }

    //~ Methods ----------------------------------------------------------------
    //---------------------//
    // getInstalledVersion //
    //---------------------//
    /**
     * Report the version number of this package, if installed.
     *
     * @return the version number, or null if not found
     */
    public VersionNumber getInstalledVersion ()
    {
        final String VERSION = "version";
        final Pattern versionPattern = Pattern.compile(
                "^Version: " + RegexUtil.group(VERSION, ".*") + "$");

        final String STATUS = "status";
        final Pattern statusPattern = Pattern.compile(
                "^Status: " + RegexUtil.group(STATUS, ".*") + "$");

        try {
            List<String> output = new ArrayList<String>();
            int res = Utilities.runProcess(
                    output,
                    "bash",
                    "-c",
                    "LANG=en_US dpkg -s " + name);

            if (res != 0) {
                // If we get a non-zero exit code no info can be retrieved
                if (!output.isEmpty()) {
                    final String lines = Utilities.dumpOfLines(output);
                    logger.info(lines);
                } else {
                    logger.debug("No command output");
                }

                return null;
            } else {
                // Check status
                boolean installed = false;

                for (String line : output) {
                    Matcher matcher = statusPattern.matcher(line);

                    if (matcher.matches()) {
                        String statusStr = RegexUtil.getGroup(matcher, STATUS);

                        if (statusStr.equals("install ok installed")) {
                            installed = true;

                            break;
                        }
                    }
                }

                if (!installed) {
                    return null;
                }

                // Return installed version
                for (String line : output) {
                    Matcher matcher = versionPattern.matcher(line);

                    if (matcher.matches()) {
                        String versionStr = RegexUtil.getGroup(
                                matcher,
                                VERSION);

                        return new VersionNumber(versionStr);
                    }
                }
            }
        } catch (Throwable ex) {
            logger.warn("Could not get package version", ex);
        }

        return null;
    }

    //---------//
    // install //
    //---------//
    /**
     * Install the latest version of this package
     *
     * @throws Exception
     */
    public void install ()
            throws Exception
    {
        String cmd = "apt-get install -y " + name;
        Installer.getBundle()
                .appendCommand(cmd);
    }

    //-------------//
    // isInstalled //
    //-------------//
    public boolean isInstalled ()
    {
        VersionNumber installedVersion = getInstalledVersion();

        if (installedVersion == null) {
            return false;
        }

        return installedVersion.compareTo(minVersion) >= 0;
    }
}
