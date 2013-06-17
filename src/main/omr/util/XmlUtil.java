//----------------------------------------------------------------------------//
//                                                                            //
//                                X m l U t i l                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

/**
 * Class {@code XmlUtil} gathers methods about XML data
 *
 * @author Hervé Bitteur
 */
public class XmlUtil
{
    //~ Constructors -----------------------------------------------------------

    private XmlUtil ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //----------------------------//
    // stripNonValidXMLCharacters //
    //----------------------------//
    /**
     * Copied from Mark Mclaren blog:
     * http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
     *
     * This method ensures that the output String has only valid XML
     * unicode characters as specified by the XML 1.0 standard.
     *
     * For reference, please
     * see <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">
     * the standard</a>.
     *
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
            if ((c == 0x9)
                || (c == 0xA)
                || (c == 0xD)
                || ((c >= 0x20) && (c <= 0xD7FF))
                || ((c >= 0xE000) && (c <= 0xFFFD))
                || ((c >= 0x10000) && (c <= 0x10FFFF))) {
                sb.append(c);
            } else {
                if (stripped != null) {
                    stripped.set(true);
                }
            }
        }

        return sb.toString();
    }
}
