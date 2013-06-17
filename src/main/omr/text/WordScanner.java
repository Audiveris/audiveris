//----------------------------------------------------------------------------//
//                                                                            //
//                           W o r d S c a n n e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Class {@code WordScanner} is a scanner to retrieve words out of
 * a string content, while mapping each word to a sequence of TextChar
 * instances.
 *
 * @author Hervé Bitteur
 */
public abstract class WordScanner
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            WordScanner.class);

    //~ Instance fields --------------------------------------------------------
    private final boolean bySyllable;

    /** The content string */
    private final String content;

    /** The current index in the content string */
    private int strIndex = -1;

    /** Precise description of each (non blank) character */
    private List<TextChar> chars;

    /** Current word and its positions in chars sequence */
    private String currentWord = null;

    private int currentWordStart = 0;

    private int currentWordStop = 0;

    /** Next word and its positions in chars sequence */
    private String nextWord = null;

    private int nextWordStart = -1;

    private int nextWordStop = -1;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // WordScanner //
    //-------------//
    /**
     * Creates a new WordScanner object.
     *
     * @param content the string value to scan
     * @param chars   the sequence of chars descriptors
     */
    public WordScanner (String content,
                        boolean bySyllable,
                        List<TextChar> chars)
    {
        this.content = content;
        this.bySyllable = bySyllable;
        this.chars = chars;
    }

    //~ Methods ----------------------------------------------------------------
    //
    //--------------//
    // getWordChars //
    //--------------//
    /**
     * Report the sequence of TextChar instances that correspond to
     * the current word.
     *
     * @return the word sequence of TextChar's
     */
    public List<TextChar> getWordChars ()
    {
        return chars.subList(currentWordStart, currentWordStop + 1);
    }

    //---------//
    // hasNext //
    //---------//
    /**
     * Tell whether there is a next word.
     *
     * @return true if not finished, false otherwise
     */
    public boolean hasNext ()
    {
        return nextWord != null;
    }

    //------//
    // next //
    //------//
    /**
     * Make the next word current, and return it.
     *
     * @return the next word content
     */
    public String next ()
    {
        // Promote 'next' as 'current'
        currentWord = nextWord;
        currentWordStart = nextWordStart;
        currentWordStop = nextWordStop;

        // ¨Prepare the new 'next' if any
        lookAhead();

        return currentWord;
    }

    //--------------//
    // stringToDesc //
    //--------------//
    /**
     * Knowing the char strIndex in string content, determine the
     * related position in the sequence of TextChar instances
     *
     * @param strIndex strIndex in contant
     * @return position in sequence of TextChar instances
     */
    protected abstract int stringToDesc (int strIndex);

    //-------------//
    // getNextWord //
    //-------------//
    /**
     * Retrieve positions for the next word, whose content is returned.
     * The related TextChar instances can now be retrieved through their range
     * [getWordStart() .. getWordStop()].
     *
     * @return the next word content
     */
    protected String getNextWord ()
    {
        StringBuilder WordSb = new StringBuilder();

        for (strIndex += 1; strIndex < content.length(); strIndex++) {
            String charValue = content.substring(strIndex, strIndex + 1);

            // Position in sequence of TextChar instances
            int charPos = stringToDesc(strIndex);

            if (charValue.equals(" ")) {
                // White space
                if (WordSb.length() > 0) {
                    return WordSb.toString();
                }
            } else if (bySyllable && BasicContent.isSeparator(charValue)) {
                // Special characters (returned as stand-alone words)
                if (WordSb.length() > 0) {
                    strIndex--; // To get back to this index, next time
                } else {
                    nextWordStart = charPos;
                    nextWordStop = charPos;
                    WordSb.append(charValue);
                }

                return WordSb.toString();
            } else {
                // Standard word character
                if (WordSb.length() == 0) {
                    nextWordStart = charPos;
                }

                nextWordStop = charPos;
                WordSb.append(charValue);
            }
        }

        // We have reached the end
        if (WordSb.length() > 0) {
            return WordSb.toString();
        } else {
            return null;
        }
    }

    //-----------//
    // lookAhead //
    //-----------//
    /**
     * Prepare positions for the next word.
     */
    protected void lookAhead ()
    {
        nextWord = getNextWord();
    }

    //~ Inner Classes ----------------------------------------------------------
    //---------------//
    // ManualScanner //
    //---------------//
    /**
     * Class {@code ManualScanner} is a specific scanner using manual
     * text content, whose length may be different from the sequence of
     * TextChar instances.
     */
    public static class ManualScanner
            extends WordScanner
    {
        //~ Instance fields ----------------------------------------------------

        /** Ratio of number of TextChar instances / content length. */
        private final double ratio;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new ManualScanner object.
         *
         * @param content the string value to scan
         * @param chars   the sequence of chars descriptors
         */
        public ManualScanner (String content,
                              boolean bySyllable,
                              List<TextChar> chars)
        {
            super(content, bySyllable, chars);

            ratio = chars.size() / (double) content.length();

            lookAhead();
            logger.debug("ManualScanner on ''{}''", content);
        }

        //~ Methods ------------------------------------------------------------
        /**
         * Compute charPos proportionally to strIndex.
         */
        @Override
        protected int stringToDesc (int strIndex)
        {
            return (int) Math.rint(strIndex * ratio);
        }
    }

    //------------//
    // OcrScanner //
    //------------//
    /**
     * Class {@code OcrScanner} is a basic scanner for which
     * the sequence of TextChar's is parallel to String content.
     *
     * @author Hervé Bitteur
     */
    public static class OcrScanner
            extends WordScanner
    {
        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new OcrScanner object.
         *
         * @param content the string value to scan
         * @param chars   the sequence of chars descriptors
         */
        public OcrScanner (String content,
                           boolean bySyllable,
                           List<TextChar> chars)
        {
            super(content, bySyllable, chars);

            lookAhead();
            logger.debug("OcrScanner on ''{}''", content);
        }

        //~ Methods ------------------------------------------------------------
        /**
         * CharPos and strIndex are always equal.
         */
        @Override
        protected int stringToDesc (int strIndex)
        {
            return strIndex;
        }
    }
}
