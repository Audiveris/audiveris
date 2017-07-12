//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T e x t L i n e                                         //
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
package org.audiveris.omr.text;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.sheet.Skew;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code TextLine} defines a non-mutable structure to report all information on
 * one OCR-decoded line.
 *
 * @author Hervé Bitteur
 */
public class TextLine
        extends TextBasedItem
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TextLine.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Words that compose this line. */
    private final List<TextWord> words = new ArrayList<TextWord>();

    /** Average font for the line. */
    private FontInfo meanFont;

    /** Role of this text line. */
    private TextRole role;

    /** Temporary processed flag. */
    private boolean processed;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TextLine object from a sequence of words.
     *
     * @param words the sequence of words
     */
    public TextLine (List<TextWord> words)
    {
        this();

        this.words.addAll(words);

        for (TextWord word : words) {
            word.setTextLine(this);
        }
    }

    /**
     * Creates a new TextLine object, without its contained words which are assumed
     * to be added later.
     */
    public TextLine ()
    {
        super(null, null, null, null);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addWords //
    //----------//
    /**
     * Add a few words.
     *
     * @param words the words to add
     */
    public void addWords (Collection<TextWord> words)
    {
        if ((words != null) && !words.isEmpty()) {
            this.words.addAll(words);

            for (TextWord word : words) {
                word.setTextLine(this);
            }

            Collections.sort(this.words, TextWord.byAbscissa);

            invalidateCache();
        }
    }

    //
    //------------//
    // appendWord //
    //------------//
    /**
     * Append a word at the end of the word sequence of the line.
     *
     * @param word the word to append
     */
    public void appendWord (TextWord word)
    {
        words.add(word);
        word.setTextLine(this);
        invalidateCache();
    }

    /**
     * Give a Line comparator by de-skewed abscissa.
     *
     * @param skew the global sheet skew
     * @return the skew-based abscissa comparator
     */
    public static Comparator<TextLine> byAbscissa (final Skew skew)
    {
        return new Comparator<TextLine>()
        {
            @Override
            public int compare (TextLine line1,
                                TextLine line2)
            {
                return Double.compare(
                        line1.getDskOrigin(skew).getX(),
                        line2.getDskOrigin(skew).getX());
            }
        };
    }

    /**
     * Give a Line comparator by de-skewed ordinate.
     *
     * @param skew the global sheet skew
     * @return the skew-based ordinate comparator
     */
    public static Comparator<TextLine> byOrdinate (final Skew skew)
    {
        return new Comparator<TextLine>()
        {
            @Override
            public int compare (TextLine line1,
                                TextLine line2)
            {
                return Double.compare(
                        line1.getDskOrigin(skew).getY(),
                        line2.getDskOrigin(skew).getY());
            }
        };
    }

    //------//
    // dump //
    //------//
    /**
     * Print out internals.
     */
    public void dump ()
    {
        logger.info("{}", this);

        for (TextWord word : words) {
            logger.info("   {}", word);
        }
    }

    //-------------//
    // getBaseline //
    //-------------//
    /**
     * Overridden to recompute baseline from contained words
     *
     * @return the line baseline
     */
    @Override
    public Line2D getBaseline ()
    {
        if (super.getBaseline() == null) {
            if (words.isEmpty()) {
                return null;
            } else {
                setBaseline(baselineOf(words));
            }
        }

        return super.getBaseline();
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Overridden to recompute the bounds from contained words.
     *
     * @return the line bounds
     */
    @Override
    public Rectangle getBounds ()
    {
        if (super.getBounds() == null) {
            setBounds(boundsOf(getWords()));
        }

        return super.getBounds();
    }

    //----------//
    // getChars //
    //----------//
    /**
     * Report the sequence of chars descriptors (of words).
     *
     * @return the chars
     */
    public List<TextChar> getChars ()
    {
        List<TextChar> chars = new ArrayList<TextChar>();

        for (TextWord word : words) {
            chars.addAll(word.getChars());
        }

        return chars;
    }

    //---------------//
    // getConfidence //
    //---------------//
    /**
     * Overridden to recompute the confidence from contained words.
     *
     * @return the line confidence
     */
    @Override
    public Double getConfidence ()
    {
        if (super.getConfidence() == null) {
            setConfidence(confidenceOf(getWords()));
        }

        return super.getConfidence();
    }

    //--------------//
    // getDskOrigin //
    //--------------//
    /**
     * Report the de-skewed origin of this text line
     *
     * @param skew the sheet global skew
     * @return the de-skewed origin
     */
    public Point2D getDskOrigin (Skew skew)
    {
        Line2D base = getBaseline();

        if (base != null) {
            return skew.deskewed(base.getP1());
        }

        return null;
    }

    //--------------//
    // getFirstWord //
    //--------------//
    /**
     * Report the first word of the sentence.
     *
     * @return the first word
     */
    public TextWord getFirstWord ()
    {
        if (!words.isEmpty()) {
            return words.get(0);
        } else {
            return null;
        }
    }

    //-------------//
    // getMeanFont //
    //-------------//
    /**
     * Build a mean font (size, bold, serif) on representative words.
     *
     * @return the most representative font, or null if not available
     */
    public FontInfo getMeanFont ()
    {
        if (meanFont == null) {
            int charCount = 0; // Number of (representative) characters
            int boldCount = 0; // Number of rep chars with bold attribute
            int italicCount = 0; // Number of rep chars with italic attribute
            int serifCount = 0; // Number of rep chars with serif attribute
            int monospaceCount = 0; // Number of rep chars with monospace attribute
            int smallcapsCount = 0; // Number of rep chars with smallcaps attribute
            int underlinedCount = 0; // Number of rep chars with underlined attribute
            float sizeTotal = 0; // Total of font sizes on rep chars

            for (TextWord word : words) {
                int length = word.getLength();

                // Discard one-char words, they are not reliable
                if (length > 1) {
                    charCount += length;
                    sizeTotal += (word.getPreciseFontSize() * length);

                    FontInfo info = word.getFontInfo();

                    if (info.isBold) {
                        boldCount += length;
                    }

                    if (info.isItalic) {
                        italicCount += length;
                    }

                    if (info.isUnderlined) {
                        underlinedCount += length;
                    }

                    if (info.isMonospace) {
                        monospaceCount += length;
                    }

                    if (info.isSerif) {
                        serifCount += word.getLength();
                    }

                    if (info.isSmallcaps) {
                        smallcapsCount += length;
                    }
                }
            }

            if (charCount > 0) {
                int quorum = charCount / 2;
                meanFont = new FontInfo(
                        boldCount >= quorum, // isBold,
                        italicCount >= quorum, // isItalic,
                        underlinedCount >= quorum, // isUnderlined,
                        monospaceCount >= quorum, // isMonospace,
                        serifCount >= quorum, // isSerif,
                        smallcapsCount >= quorum, // isSmallcaps,
                        (int) Math.rint((double) sizeTotal / charCount),
                        "DummyFont");
            } else // We have no representative data, let's use the first word
            {
                if (getFirstWord() != null) {
                    meanFont = getFirstWord().getFontInfo();
                } else {
                    logger.error("TextLine with no first word {}", this);
                }
            }
        }

        return meanFont;
    }

    //---------//
    // getRole //
    //---------//
    /**
     * Report the line role.
     *
     * @return the role
     */
    public TextRole getRole ()
    {
        return role;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Overridden to return the concatenation of word values.
     *
     * @return the value to be used
     */
    @Override
    public String getValue ()
    {
        StringBuilder sb = null;

        // Use each word value
        for (TextWord word : words) {
            String str = word.getValue();

            if (sb == null) {
                sb = new StringBuilder(str);
            } else {
                sb.append(" ").append(str);
            }
        }

        if (sb == null) {
            return "";
        } else {
            return sb.toString();
        }
    }

    //---------------//
    // getWordGlyphs //
    //---------------//
    /**
     * Report the sequence of glyphs (parallel to the sequence of words)
     *
     * @return the sequence of word glyphs
     */
    public List<Glyph> getWordGlyphs ()
    {
        List<Glyph> glyphs = new ArrayList<Glyph>(words.size());

        for (TextWord word : words) {
            Glyph glyph = word.getGlyph();

            if (glyph != null) {
                glyphs.add(glyph);
            } else {
                logger.warn("Word {} with no related glyph", word);
            }
        }

        return glyphs;
    }

    //----------//
    // getWords //
    //----------//
    /**
     * Report an <b>unmodifiable</b> view of the sequence of words.
     *
     * @return the words view
     */
    public List<TextWord> getWords ()
    {
        return Collections.unmodifiableList(words);
    }

    //-------------//
    // isChordName //
    //-------------//
    /**
     * Report whether this line has the ChordName role
     *
     * @return true for chord line
     */
    public boolean isChordName ()
    {
        return getRole() == TextRole.ChordName;
    }

    //----------//
    // isLyrics //
    //----------//
    /**
     * Report whether this line is flagged as a Lyrics line
     *
     * @return true for lyrics line
     */
    public boolean isLyrics ()
    {
        return getRole() == TextRole.Lyrics;
    }

    //-------------//
    // isProcessed //
    //-------------//
    public boolean isProcessed ()
    {
        return processed;
    }

    //-------------//
    // removeWords //
    //-------------//
    /**
     * Remove a few words
     *
     * @param words the words to remove
     */
    public void removeWords (Collection<TextWord> words)
    {
        if ((words != null) && !words.isEmpty()) {
            this.words.removeAll(words);
            invalidateCache();
        }
    }

    //--------------//
    // setProcessed //
    //--------------//
    public void setProcessed (boolean processed)
    {
        this.processed = processed;
    }

    //---------//
    // setRole //
    //---------//
    /**
     * Assign role information.
     *
     * @param role the role to set
     */
    public void setRole (TextRole role)
    {
        this.role = role;
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a translation to the coordinates of words descriptors.
     *
     * @param dx abscissa translation
     * @param dy ordinate translation
     */
    @Override
    public void translate (int dx,
                           int dy)
    {
        // Translate line bounds and baseline
        super.translate(dx, dy);

        // Translate contained descriptors
        for (TextWord word : words) {
            word.translate(dx, dy);
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (role != null) {
            sb.append(" ").append(role);
        }

        return sb.toString();
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    private void invalidateCache ()
    {
        setBounds(null);

        setBaseline(null);
        setConfidence(null);

        role = null;
        meanFont = null;
    }
}
