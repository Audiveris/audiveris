//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S t r i n g U t i l                                      //
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
package org.audiveris.omr.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code StringUtil} provides String utilities.
 *
 * @author Hervé Bitteur
 */
public abstract class StringUtil
{

    /** Elision. (undertie) */
    public static final char ELISION_CHAR = '\u203f';

    public static final String ELISION_STRING = new String(new char[]{ELISION_CHAR});

    /** Extension. (underscore and others) */
    public static final char LOW_LINE = '_'; // '\u005f'

    public static final char EN_DASH = '\u2013'; // short dash

    public static final char EM_DASH = '\u2014'; // long dash

    public static final char EXTENSION_CHAR = LOW_LINE;

    public static final String EXTENSIONS = new String(new char[]{LOW_LINE, EN_DASH, EM_DASH});

    /** Hyphen. */
    public static final char HYPHEN = '-'; // '\u002d'

    public static final String HYPHEN_STRING = new String(new char[]{HYPHEN});

    /** Not meant to be instantiated. */
    private StringUtil ()
    {
    }

    //---------//
    // compare //
    //---------//
    /**
     * Compare two Strings, handling null cases.
     *
     * @param s1 a string
     * @param s2 another string
     * @return comparison result (-1, 0, +1)
     */
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
     * Parse a string of integer tokens, separated by comma.
     *
     * @param str the string to parse
     * @return the sequence of strings
     */
    public static List<String> parseStrings (String str)
    {
        final List<String> strList = new ArrayList<>();
        final String[] tokens = str.split("\\s*,\\s*");

        for (String token : tokens) {
            String trimmedToken = token.trim();

            if (!trimmedToken.isEmpty()) {
                strList.add(trimmedToken);
            }
        }

        return strList;
    }

    //---------//
    // codesOf //
    //---------//
    /**
     * Report the sequence of codePoint values for the provided string.
     *
     * @param s        provided string
     * @param withChar if true, char value is displayed before its codePoint
     * @return the sequence of codes
     */
    public static String codesOf (String s,
                                  boolean withChar)
    {
        final StringBuilder sb = new StringBuilder();
        final int len = s.length();

        for (int i = 0; i < len; i++) {
            if (i > 0) {
                sb.append(',');
            }

            if (withChar) {
                char c = s.charAt(i);
                sb.append(String.format("'%c'", c));
            }

            int cp = s.codePointAt(i);
            sb.append(String.format("%x", cp));
        }

        return sb.toString();
    }
}
