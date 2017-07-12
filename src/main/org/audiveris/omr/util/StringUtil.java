//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S t r i n g U t i l                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
