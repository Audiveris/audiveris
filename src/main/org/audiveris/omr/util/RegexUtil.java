//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       R e g e x U t i l                                        //
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

import java.util.regex.Matcher;

/**
 * Class {@code RegexUtil} gathers utility features related to Regex.
 *
 * @author Hervé Bitteur
 */
public abstract class RegexUtil
{
    //~ Constructors -------------------------------------------------------------------------------

    // Not meant to be instantiated
    private RegexUtil ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // escape //
    //--------//
    /**
     * Escape the special characters in the provided content (for the time being, the
     * only escaped characters are: '.')
     *
     * @param content the string to process
     * @return content with special characters escaped
     */
    public static String escape (String content)
    {
        return content.replaceAll("\\.", "\\\\.");
    }

    //----------//
    // getGroup //
    //----------//
    /**
     * Report the input sequence captured by the provided named-capturing group.
     *
     * @param matcher the matcher
     * @param name    the provided name for desired group
     * @return the input sequence, perhaps empty but not null
     */
    public static String getGroup (Matcher matcher,
                                   String name)
    {
        String result = null;

        try {
            result = matcher.group(name);
        } catch (Exception ignored) {
        }

        if (result != null) {
            return result;
        } else {
            return "";
        }
    }

    //-------//
    // group //
    //-------//
    /**
     * Convenient method to build a named-group like "(?&lt;name&gt;content)"
     *
     * @param name    name for the group
     * @param content inner content of the group
     * @return the ready to use group value
     */
    public static String group (String name,
                                String content)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("(?<");
        sb.append(name);
        sb.append(">");

        sb.append(content);

        sb.append(")");

        return sb.toString();
    }
}
