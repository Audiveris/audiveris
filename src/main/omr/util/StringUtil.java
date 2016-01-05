//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S t r i n g U t i l                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code StringUtil}
 *
 * @author Hervé Bitteur
 */
public abstract class StringUtil
{
    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // compare //
    //---------//
    public static int compare (String s1,
                               String s2)
    {
        if (s1 == null) {
            if (s2 == null) {
                return 0;
            }

            return -1;
        }

        if (s2 == null) {
            return +1;
        }

        return s1.compareTo(s2);
    }

    //-----------//
    // parseInts //
    //-----------//
    /**
     * Parse a string of strings, separated by comma.
     *
     * @param str the string to parse
     * @return the sequence of strings
     */
    public static List<String> parseStrings (String str)
    {
        final List<String> strList = new ArrayList<String>();
        final String[] tokens = str.split("\\s*,\\s*");

        for (String token : tokens) {
            String trimmedToken = token.trim();

            if (!trimmedToken.isEmpty()) {
                strList.add(trimmedToken);
            }
        }

        return strList;
    }
}
