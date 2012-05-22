//----------------------------------------------------------------------------//
//                                                                            //
//                            O c r S c a n n e r                             //
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
 * Class {@code OcrScanner} is a specific scanner to scan content,
 * since we need to know the current position within the lineDesc,
 * to infer proper word location.
 *
 * @author Hervé Bitteur
 */
public class OcrScanner
    extends WordScanner
{
    //~ Constructors -----------------------------------------------------------

    //------------//
    // OcrScanner //
    //------------//
    /**
     * Creates a new OcrScanner object.
     * @param chars DOCUMENT ME!
     */
    public OcrScanner (List<OcrChar> chars)
    {
        super(chars);
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

        for (pos += 1; pos < chars.size(); pos++) {
            OcrChar charDesc = chars.get(pos);

            // White space
            if (charDesc.hasSpacesBefore()) {
                if (word.length() > 0) {
                    pos--;

                    return word.toString();
                }
            }

            String str = charDesc.content;

            // Special characters (returned as stand-alone words)
            if (BasicContent.isSeparator(str)) {
                if (word.length() > 0) {
                    pos--;
                } else {
                    nextWordStart = pos;
                    nextWordStop = pos;
                    word.append(str);
                }

                return word.toString();
            } else {
                // Standard word content
                if (word.length() == 0) {
                    nextWordStart = pos;
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
