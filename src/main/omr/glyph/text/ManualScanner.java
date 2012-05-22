//----------------------------------------------------------------------------//
//                                                                            //
//                         M a n u a l S c a n n e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2012. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import java.util.List;

/**
 * Class {@code ManualScanner} is a specific scanner using manual
 * text content.
 *
 * @author Hervé Bitteur
 */
public class ManualScanner
    extends WordScanner
{
    //~ Instance fields --------------------------------------------------------

    /** The string content ant related parameters */
    final String content;
    private final int    length;
    private final int    charNb;
    private final double ratio;

    /** The current index in the content string */
    private int index = -1;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // ManualScanner //
    //---------------//
    /**
     * Creates a new ManualScanner object.
     * @param chars DOCUMENT ME!
     * @param content DOCUMENT ME!
     */
    public ManualScanner (List<OcrChar> chars,
                          String        content)
    {
        super(chars);
        this.content = content;
        length = content.length();
        charNb = chars.size();
        ratio = charNb / (double) length;
        lookAhead();
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getNextWord //
    //-------------//
    @Override
    protected String getNextWord ()
    {
        StringBuilder word = new StringBuilder();

        for (index += 1; index < content.length(); index++) {
            pos = (int) Math.rint(index * ratio);

            String str = content.substring(index, index + 1);

            // White space?
            if (str.equals(" ")) {
                if (word.length() > 0) {
                    return word.toString();
                }
            }

            // Special characters? (to be returned as stand-alone words)
            if (BasicContent.isSeparator(str)) {
                if (word.length() > 0) {
                } else {
                    nextWordStart = nextWordStop + 1; // First unused pos
                    nextWordStop = pos;
                    word.append(str);
                }

                return word.toString();
            } else {
                // Standard word content
                if (word.length() == 0) {
                    // Start of a word?
                    nextWordStart = nextWordStop + 1; // First unused pos
                }

                nextWordStop = pos;
                word.append(str);
            }
        }

        // We have reached the end
        if (word.length() > 0) {
            return word.toString();
        } else {
            return null;
        }
    }
}
