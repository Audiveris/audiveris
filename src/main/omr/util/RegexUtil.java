//----------------------------------------------------------------------------//
//                                                                            //
//                             R e g e x U t i l                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.util.regex.Matcher;

/**
 * Class {@code RegexUtil} gathers utility features related to Regex.
 *
 * @author Hervé Bitteur
 */
public class RegexUtil
{
    //~ Methods ----------------------------------------------------------------

    //----------//
    // getGroup //
    //----------//
    /**
     * Report the input sequence captured by the provided
     * named-capturing group.
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
     * Convenient method to build a named-group like "(?<name>content)"
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
