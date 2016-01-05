//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    E x p o r t P a t t e r n                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.OMR;
import static omr.util.RegexUtil.escape;
import static omr.util.RegexUtil.getGroup;
import static omr.util.RegexUtil.group;

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

    private static Pattern simplePattern;

    private static Pattern doublePattern;

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
