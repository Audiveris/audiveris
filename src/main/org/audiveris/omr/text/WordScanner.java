//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     W o r d S c a n n e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.text;

import org.audiveris.omr.sig.inter.LyricItemInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.List;

/**
 * Class {@code WordScanner} is a scanner to retrieve words out of a string content,
 * while mapping each word to a sequence of TextChar instances.
 *
 * @author Hervé Bitteur
 */
public abstract class WordScanner
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(WordScanner.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The content string. */
    private final String content;

    /** Split by syllable. */
    private final boolean bySyllable;

    /** Maximum abscissa gap between two consecutive chars. */
    private final int maxCharGap;

    /** Precise description of each (non blank) character. */
    private final List<TextChar> chars;

    /** The current index in the content string. */
    private int strIndex = -1;

    /** Current word and its positions in chars sequence. */
    private String currentWord = null;

    private int currentWordStart = 0;

    private int currentWordStop = 0;

    /** Next word and its positions in chars sequence. */
    private String nextWord = null;

    private int nextWordStart = -1;

    private int nextWordStop = -1;

    //~ Constructors -------------------------------------------------------------------------------
    //-------------//
    // WordScanner //
    //-------------//
    /**
     * Creates a new WordScanner object.
     *
     * @param content    the string value to scan
     * @param bySyllable to split by syllable
     * @param maxCharGap maximum abscissa gap between two consecutive chars
     * @param chars      the sequence of chars descriptors
     */
    protected WordScanner (String content,
                           boolean bySyllable,
                           int maxCharGap,
                           List<TextChar> chars)
    {
        this.content = content;
        this.bySyllable = bySyllable;
        this.maxCharGap = maxCharGap;
        this.chars = chars;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    //--------------//
    // getWordChars //
    //--------------//
    /**
     * Report the sequence of TextChar instances that correspond to the current word.
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
     * Knowing the char strIndex in string content, determine the related position
     * in the sequence of TextChar instances.
     *
     * @param strIndex strIndex in content
     * @return position in sequence of TextChar instances
     */
    protected abstract int stringToDesc (int strIndex);

    //-------------//
    // getNextWord //
    //-------------//
    /**
     * Retrieve positions for the next word, whose content is returned.
     * The related TextChar instances can then be retrieved through {@link #getWordChars()}.
     *
     * @return the next word content
     */
    protected String getNextWord ()
    {
        final StringBuilder WordSb = new StringBuilder();
        final int contentLength = content.length();

        for (strIndex += 1; strIndex < contentLength; strIndex++) {
            String charValue = content.substring(strIndex, strIndex + 1);

            // Position in sequence of TextChar instances
            int charPos = stringToDesc(strIndex);

            if (charValue.equals(" ")) {
                // White space
                if (WordSb.length() > 0) {
                    return WordSb.toString();
                }
            } else if (bySyllable && LyricItemInter.isSeparator(charValue)) {
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
                } else {
                    // Check abscissa gap with previous character
                    Rectangle prevCharBox = chars.get(charPos - 1).getBounds();
                    Rectangle charBox = chars.get(charPos).getBounds();
                    int xGap = charBox.x - (prevCharBox.x + prevCharBox.width);

                    if (xGap > maxCharGap) {
                        strIndex--; // To get back to this index, next time

                        return WordSb.toString();
                    }
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------------//
    // ManualScanner //
    //---------------//
    /**
     * Class {@code ManualScanner} is a specific scanner using manual text content,
     * whose length may be different from the sequence of TextChar instances.
     */
    public static class ManualScanner
            extends WordScanner
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Ratio of number of TextChar instances / content length. */
        private final double ratio;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new ManualScanner object.
         *
         * @param content    the string value to scan
         * @param bySyllable true for split by syllable
         * @param maxCharGap maximum abscissa gap between two consecutive chars
         * @param chars      the sequence of chars descriptors
         */
        public ManualScanner (String content,
                              boolean bySyllable,
                              int maxCharGap,
                              List<TextChar> chars)
        {
            super(content, bySyllable, maxCharGap, chars);

            ratio = chars.size() / (double) content.length();

            lookAhead();
            logger.debug("ManualScanner on ''{}''", content);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected int stringToDesc (int strIndex)
        {
            // Compute charPos proportionally to strIndex
            return (int) Math.rint(strIndex * ratio);
        }
    }

    //------------//
    // OcrScanner //
    //------------//
    /**
     * Class {@code OcrScanner} is a basic scanner for which the sequence of TextChar's
     * is parallel to String content.
     *
     * @author Hervé Bitteur
     */
    public static class OcrScanner
            extends WordScanner
    {
        //~ Constructors ---------------------------------------------------------------------------

        /**
         * Creates a new OcrScanner object.
         *
         * @param content    the string value to scan
         * @param bySyllable for split by syllable
         * @param maxCharGap maximum abscissa gap between two consecutive chars
         * @param chars      the sequence of chars descriptors
         */
        public OcrScanner (String content,
                           boolean bySyllable,
                           int maxCharGap,
                           List<TextChar> chars)
        {
            super(content, bySyllable, maxCharGap, chars);

            lookAhead();
            logger.debug("OcrScanner on ''{}''", content);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected int stringToDesc (int strIndex)
        {
            // CharPos and strIndex are always equal
            return strIndex;
        }
    }
}
