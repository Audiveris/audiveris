//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         V e r s i o n s                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import org.audiveris.omr.util.Version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code Versions} gathers key versions for upgrade checks.
 *
 * @author Hervé Bitteur
 */
public abstract class Versions
{

    /** Version of current Audiveris software. */
    public final static Version CURRENT_SOFTWARE = new Version(WellKnowns.TOOL_REF);

    /**
     * Better precision in inter geometry.
     * <ul>
     * <li>Migration from Point to Point2D for many inter segments (horizontal or vertical).
     * <li>Barline and Bracket now include staff line height.
     * <li>Related BarConnector and BracketConnector are shortened accordingly.
     * <li>Stem now uses thickness and vertical median line in lieu of top/bottom points.
     * <li>Ledger now uses thickness and horizontal median line.
     * </ul>
     */
    public final static Version INTER_GEOMETRY = new Version("5.2.1");

    /** Sequence of upgrade versions to check. */
    public final static List<Version> UPGRADE_VERSIONS = Arrays.asList(INTER_GEOMETRY);

    /** Latest upgrade version. */
    public final static Version LATEST_UPGRADE = UPGRADE_VERSIONS.get(UPGRADE_VERSIONS.size() - 1);

    // No instance needed for this functional class
    private Versions ()
    {
    }

    //-------//
    // check //
    //-------//
    /**
     * Check a provided version against current software version.
     *
     * @param version version to check
     * @return check result
     */
    public static CheckResult check (Version version)
    {
        if (version.major < CURRENT_SOFTWARE.major) {
            // Non compatible, reprocess from binary?
            return CheckResult.BOOK_TOO_OLD;
        }

        if (version.major > CURRENT_SOFTWARE.major) {
            // Non compatible, use more recent program
            return CheckResult.PROGRAM_TOO_OLD;
        }

        if (version.minor > CURRENT_SOFTWARE.minor) {
            // Non compatible, use more recent program
            return CheckResult.PROGRAM_TOO_OLD;
        }

        // Compatible (though book file may be upgraded automatically)
        return CheckResult.COMPATIBLE;
    }

    //-------------//
    // getUpgrades //
    //-------------//
    /**
     * Report the sequence of upgrade versions to apply on the provided sheet.
     *
     * @param sheetVersion current version of sheet
     * @return sequence of upgrades to perform
     */
    public static List<Version> getUpgrades (Version sheetVersion)
    {
        List<Version> list = null;

        for (Version v : UPGRADE_VERSIONS) {
            if (sheetVersion.compareTo(v) < 0) {
                if (list == null) {
                    list = new ArrayList<>();
                }

                list.add(v);
            }
        }

        return (list == null) ? Collections.EMPTY_LIST : list;
    }

    //-------------//
    // CheckResult //
    //-------------//
    public enum CheckResult
    {
        COMPATIBLE,
        BOOK_TOO_OLD,
        PROGRAM_TOO_OLD;
    }
}
