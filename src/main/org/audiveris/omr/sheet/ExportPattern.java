//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    E x p o r t P a t t e r n                                   //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.OMR;
import static org.audiveris.omr.util.RegexUtil.escape;
import static org.audiveris.omr.util.RegexUtil.getGroup;
import static org.audiveris.omr.util.RegexUtil.group;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class {@code ExportPattern} handles the naming of possible exports.
 *
 * @author Hervé Bitteur
 */
public class ExportPattern
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final String OPUS = "opus";

    private static final String opusPat = group(OPUS, escape(OMR.OPUS_EXTENSION));

    private static final String SCORE = "score";

    private static final String scorePat = group(
            SCORE,
            escape(OMR.COMPRESSED_SCORE_EXTENSION) + "|" + escape(OMR.SCORE_EXTENSION));

    private static final String MVT = "mvt";

    private static final String mvtPat = group(MVT, escape(OMR.MOVEMENT_EXTENSION) + "\\d+");

    private static final String simplePat = ".+" + scorePat;

    private static final String doublePat = ".+" + "(" + opusPat + "|" + mvtPat + scorePat + ")";

    private static volatile Pattern simplePattern;

    private static volatile Pattern doublePattern;

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getPathSansExt //
    //----------------//
    /**
     * Extract the path radix of a path, by removing potential extensions.
     *
     * @param path the path to process
     * @return the path without any known extension
     */
    public static Path getPathSansExt (Path path)
    {
        String pathStr = path.toString();

        // Try double first
        if (doublePattern == null) {
            doublePattern = Pattern.compile(doublePat);
        }

        Matcher doubleMatcher = doublePattern.matcher(pathStr);

        if (doubleMatcher.matches()) {
            String opus = getGroup(doubleMatcher, OPUS);
            String mvt = getGroup(doubleMatcher, MVT);
            String score = getGroup(doubleMatcher, SCORE);
            pathStr = pathStr.substring(
                    0,
                    pathStr.length() - (opus.length() + mvt.length() + score.length()));
        } else {
            // Try simple after
            if (simplePattern == null) {
                simplePattern = Pattern.compile(simplePat);
            }

            Matcher simpleMatcher = simplePattern.matcher(pathStr);

            if (simpleMatcher.matches()) {
                String score = getGroup(simpleMatcher, SCORE);
                pathStr = pathStr.substring(0, pathStr.length() - (score.length()));
            }
        }

        return Paths.get(pathStr);
    }
}
