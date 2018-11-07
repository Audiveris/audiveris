//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          X m l U t i l                                         //
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
package org.audiveris.omr.util;

/**
 * Class {@code XmlUtil} gathers methods about XML data
 *
 * @author Hervé Bitteur
 */
public abstract class XmlUtil
{

    //----------------------------//
    // stripNonValidXMLCharacters //
    //----------------------------//
    /**
     * Copied from Mark Mclaren blog:
     * http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
     * <p>
     * This method ensures that the output String has only valid XML
     * unicode characters as specified by the XML 1.0 standard.
     * <p>
     * For reference, please
     * see <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">
     * the standard</a>.
     * <p>
     * Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] |
     * [#x10000-#x10FFFF]
     * (any Unicode character, excluding the surrogate blocks, FFFE, and FFFF)
     *
     * @param input    The String whose non-valid characters we want to remove.
     * @param stripped if non null, its value will be set to true if one or
     *                 more characters are stripped
     * @return The input String, stripped of non-valid characters.
     */
    public static String stripNonValidXMLCharacters (String input,
                                                     WrappedBoolean stripped)
    {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        for (char c : input.toCharArray()) {
            if ((c == 0x9) || (c == 0xA) || (c == 0xD) || ((c >= 0x20) && (c <= 0xD7FF)) || ((c
                                                                                                      >= 0xE000)
                                                                                             && (c
                                                                                                         <= 0xFFFD))
                || ((c >= 0x10000) && (c <= 0x10FFFF))) {
                sb.append(c);
            } else if (stripped != null) {
                stripped.set(true);
            }
        }

        return sb.toString();
    }
}
