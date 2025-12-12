//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         R e l e a s e s                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.WellKnowns;

import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Class <code>Releases</code> handles features related to Audiveris releases.
 *
 * @author Hervé Bitteur
 */
public abstract class Releases
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Releases.class);

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);

    private static final List<String> relevantExtensions = Arrays.asList(
            ".dmg",
            ".deb",
            ".msi",
            ".exe"); // Historical installers: exe

    //~ Constructors -------------------------------------------------------------------------------

    /** No instance needed for this functional class. */
    @SuppressWarnings("unused")
    private Releases ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------------//
    // getLatestRelease //
    //------------------//
    /**
     * Retrieve the latest release available on Audiveris project site.
     *
     * @return the latest release, or null if something went wrong
     */
    public static GHRelease getLatestRelease ()
    {
        try {
            final GHRepository repository = getRepository();
            final GHRelease latestRelease = repository.getLatestRelease();
            logger.debug("Latest release: {}", latestRelease);

            return latestRelease;
        } catch (IOException ex) {
            logger.warn("Could not connect to Audiveris project.\n{}", ex.toString());

            if (ex.getCause() != null) {
                logger.warn("Cause: {}", ex.getCause().toString());
            }

            return null;
        }
    }

    //----------------//
    // getAllReleases //
    //----------------//
    /**
     * Retrieve all releases available on Audiveris project site.
     *
     * @return the latest release, or null if something went wrong
     */
    public static List<GHRelease> getAllReleases ()
    {
        try {
            final GHRepository repository = getRepository();
            return repository.listReleases().toList();
        } catch (IOException ex) {
            logger.warn("Could not connect to Audiveris project.\n{}", ex.toString());

            if (ex.getCause() != null) {
                logger.warn("Cause: {}", ex.getCause().toString());
            }

            return null;
        }
    }

    //---------------//
    // getRepository //
    //---------------//
    public static GHRepository getRepository ()
        throws IOException
    {
        final GitHub github = GitHub.connectAnonymously();

        final GHOrganization organization = github.getOrganization(WellKnowns.TOOL_NAME);
        logger.debug("{}", organization);

        final GHRepository repository = organization.getRepository(WellKnowns.TOOL_ID);
        logger.debug("{}", repository);

        if (repository == null) {
            logger.warn("Unknown repository: {}", WellKnowns.TOOL_ID);

            return null;
        }

        return repository;
    }

    //--------------------//
    // printLatestRelease //
    //--------------------//
    public static void printLatestRelease ()
    {
        try {
            printRelease(getLatestRelease());
        } catch (IOException ex) {
            logger.warn("Error retrieving release assets", ex);
        }
    }

    //------------------//
    // printAllReleases //
    //------------------//
    public static void printAllReleases ()
    {
        try {
            for (GHRelease release : getAllReleases()) {
                printRelease(release);
            }
        } catch (IOException ex) {
            logger.warn("Error retrieving release assets", ex);
        }
    }

    //--------------//
    // printRelease //
    //--------------//
    public static void printRelease (GHRelease release)
        throws IOException
    {
        System.out.println(
                String.format(
                        "%n%s %s",
                        DATE_FORMAT.format(release.getCreatedAt()),
                        release.getName()));

        int total = 0;
        final List<GHAsset> assets = release.listAssets().toList();

        for (GHAsset asset : assets) {
            final String fileName = asset.getName();
            final int lastDot = fileName.lastIndexOf('.');
            final String ext = fileName.substring(lastDot); // .ext
            final long count = asset.getDownloadCount();
            System.out.println(String.format("%11d %s", count, fileName));

            if (relevantExtensions.contains(ext)) {
                total += count;
            }
        }

        System.out.println(String.format("%11d %s", total, "(all installers)"));
    }
}
