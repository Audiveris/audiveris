//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T e x t L i n e                                         //
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
package org.audiveris.omr.text;

import java.awt.Point;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sig.inter.ChordNameInter;
import org.audiveris.omr.text.WordScanner.OcrScanner;
import org.audiveris.omr.text.tesseract.TesseractOCR;

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

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TextLine.class);

    /** Words that compose this line. */
    private final List<TextWord> words = new ArrayList<>();

    /** Average font for the line. */
    private FontInfo meanFont;

    /** Role of this text line. */
    private TextRole role;

    /** Temporary processed flag. */
    private boolean processed;

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

    //---------------//
    // checkValidity //
    //---------------//
    /**
     * Check the OCR line, which may get some of its words removed.
     * <p>
     * First remove any really invalid word from the line.
     * Then use average of remaining words confidence value.
     * Use font size validity.
     *
     * @param scale         scale of containing sheet
     * @param inSheetHeader true if line is located above first system
     * @return reason for invalidity if any, otherwise null
     */
    public String checkValidity (Scale scale,
                                 boolean inSheetHeader)
    {
        // Discard really invalid words
        final List<TextWord> toRemove = new ArrayList<>();

        for (TextWord word : getWords()) {
            String reason = word.checkValidity(scale, inSheetHeader);

            if (reason != null) {
                toRemove.add(word);
            }
        }

        if (!toRemove.isEmpty()) {
            removeWords(toRemove);
        }

        // Check global line confidence
        final double minConfidence = TesseractOCR.getInstance().getMinConfidence();
        Double conf = getConfidence();

        if ((conf == null) || conf.isNaN() || (conf < minConfidence)) {
            return "low-confidence";
        }

        // Check font size
        if (!isValidFontSize(scale, inSheetHeader)) {
            return "invalid-font-size";
        }

        return null; // OK
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
        List<TextChar> chars = new ArrayList<>();

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

    //-----------------//
    // getDeskewedCore //
    //-----------------//
    /**
     * Build a rectangle using de-skewed baseline and min 1 pixel high.
     *
     * @param skew global sheet skew
     * @return the de-skewed core
     */
    public Rectangle getDeskewedCore (Skew skew)
    {
        Point2D P1 = getDskOrigin(skew);
        Point p1 = new Point((int) Math.rint(P1.getX()), (int) Math.rint(P1.getY()));
        Point2D P2 = skew.deskewed(getBaseline().getP2());
        Point p2 = new Point((int) Math.rint(P2.getX()), (int) Math.rint(P2.getY()));
        Rectangle rect = new Rectangle(p1);
        rect.add(p2);

        rect.height = Math.max(1, rect.height); // To allow containment test

        return rect;
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

    //----------//
    // getGrade //
    //----------//
    /**
     * Compute sentence grade based on OCR-provided confidence and total number of
     * characters.
     *
     * @return the computed grade
     */
    public double getGrade ()
    {
        final int minLg = constants.sentenceLowerLength.getValue();
        final int maxLg = constants.sentenceUpperLength.getValue();
        final int length = getLength();

        double grade = getConfidence() * Grades.intrinsicRatio;

        if (length >= minLg) {
            double ratio = Math.min(1.0, (length - minLg) / (double) (maxLg - minLg));
            grade += (ratio * (constants.maxSentenceGrade.getValue() - grade));
        }

        return grade;
    }

    //-----------//
    // getLength //
    //-----------//
    /**
     * Report the total number of characters in this text line.
     *
     * @return number of characters in this line
     */
    public int getLength ()
    {
        int count = 0;

        for (TextWord word : words) {
            count += word.getLength();
        }

        return count;
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
            } else {
                // We have no representative data, let's use the first word
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
        List<Glyph> glyphs = new ArrayList<>(words.size());

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
     * Report the live sequence of words.
     *
     * @return the words
     */
    public List<TextWord> getWords ()
    {
        return words;
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

    //-----------------//
    // isAllChordNames //
    //-----------------//
    /**
     * Report whether this line is composed only of chord names.
     *
     * @return true if so
     */
    public boolean isAllChordNames ()
    {
        for (TextWord word : getWords()) {
            if (ChordNameInter.createValid(word) == null) {
                return false;
            }
        }

        return true;
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
    /**
     * Tell whether this line has already beet processed.
     *
     * @return true if so
     */
    public boolean isProcessed ()
    {
        return processed;
    }

    //--------------//
    // setProcessed //
    //--------------//
    /**
     * Set the processed flag for this line
     *
     * @param processed true if processed
     */
    public void setProcessed (boolean processed)
    {
        this.processed = processed;
    }

    //--------------------//
    // mergeStandardWords //
    //--------------------//
    public void mergeStandardWords ()
    {
        final int minWordDx = (int) Math.rint(
                getMeanFont().pointsize * constants.minWordDxFontRatio.getValue());
        final List<TextWord> toAdd = new ArrayList<>();
        final List<TextWord> toRemove = new ArrayList<>();

        TextWord prevWord = null;

        for (TextWord word : getWords()) {
            // Look for tiny inter-word gap
            if (prevWord != null) {
                Rectangle prevBounds = prevWord.getBounds();
                int prevStop = prevBounds.x + prevBounds.width;
                int gap = word.getBounds().x - prevStop;

                if (gap < minWordDx) {
                    toRemove.add(prevWord);
                    toRemove.add(word);

                    TextWord bigWord = TextWord.mergeOf(prevWord, word);

                    if (logger.isDebugEnabled()) {
                        logger.info("   merged {} & {} into {}", prevWord, word, bigWord);
                    }

                    toAdd.add(bigWord);
                    word = bigWord;
                }
            }

            prevWord = word;
        }

        if (!toAdd.isEmpty()) {
            // No use to add & remove the same words
            List<TextWord> common = new ArrayList<>(toAdd);
            common.retainAll(toRemove);
            toAdd.removeAll(common);
            toRemove.removeAll(common);

            // Perform the modifications, without modifying line role
            addWords(toAdd);
            removeWords(toRemove);
        }
    }

    //--------------------//
    // recutStandardWords //
    //--------------------//
    /**
     * Re-cut (merge and split) words within a standard TextLine.
     */
    public void recutStandardWords ()
    {
        mergeStandardWords();
        splitWords();
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

    //---------------//
    // selectWordGap //
    //---------------//
    /**
     * Report the maximum acceptable abscissa gap between two consecutive words.
     * <p>
     * We use a smaller horizontal gap between chord names than between words of ordinary standard
     * lines.
     *
     * @param scale scale of containing sheet
     * @return the maximum abscissa gap to use
     */
    public int selectWordGap (Scale scale)
    {
        // Chord name
        if (isChordName()) {
            return scale.toPixels(constants.maxChordDx);
        }

        // Standard line, adapt inter-word gap to font size
        int pointSize = getMeanFont().pointsize;

        return (int) Math.rint(constants.maxWordDxFontRatio.getValue() * pointSize);
    }

    //------------//
    // splitWords //
    //------------//
    /**
     * Check each word in the provided collection and split it in place according to
     * separating characters ('-' etc).
     * <p>
     * The line sequence of words may get modified, because of the addition of new (sub)words and
     * the removal of words that got split.
     * <p>
     * The line sequence of words remains sorted and the line role is not modified.
     */
    public void splitWords ()
    {
        // To avoid concurrent modification errors
        final Collection<TextWord> toAdd = new ArrayList<>();
        final Collection<TextWord> toRemove = new ArrayList<>();

        for (TextWord word : words) {
            final int maxCharGap = getMaxCharGap(word); // Max gap depends on word font size
            final List<TextWord> subWords = word.getSubWords(
                    this,
                    new OcrScanner(word.getValue(), isLyrics(), maxCharGap, word.getChars()));

            if (!subWords.isEmpty()) {
                toRemove.add(word);
                toAdd.addAll(subWords);
            }
        }

        // Now perform modification on the line sequence of words, if so needed
        // Word modification is done "in situ", this does not modify line role
        if (!toRemove.isEmpty()) {
            addWords(toAdd);
            removeWords(toRemove);
        }
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

    //---------------//
    // getMaxCharGap //
    //---------------//
    /**
     * Compute max abscissa gap between two chars in a word.
     *
     * @param word the word at hand
     * @return max number of pixels for abscissa gap, according to the word font size
     */
    private int getMaxCharGap (TextWord word)
    {
        int pointSize = word.getFontInfo().pointsize;

        // TODO: very rough value to be refined and explained!
        int val = (int) Math.rint((constants.maxCharDx.getValue() * pointSize) / 2.0);

        return val;
    }

    //-----------------//
    // isValidFontSize //
    //-----------------//
    /**
     * Check whether all words in the provided line have a valid font size.
     * <p>
     * We just check if no word has a too big font size.
     *
     * @return true if valid
     */
    private boolean isValidFontSize (Scale scale,
                                     boolean inSheetHeader)
    {
        final int minFontSize = scale.toPixels(constants.minFontSize);

        // Heuristic: Allow large font only before sheet first staff
        final int maxFontSize = inSheetHeader ? scale.toPixels(constants.maxTitleFontSize)
                : scale.toPixels(constants.maxFontSize);

        for (TextWord word : getWords()) {
            FontInfo fontInfo = word.getFontInfo();

            if (fontInfo.pointsize > maxFontSize) {
                logger.debug("   too big font {} vs {} on {}",
                             fontInfo.pointsize, maxFontSize, this);

                return false;
//            } else if (fontInfo.pointsize < minFontSize) {
//                logger.debug("   too small font {} vs {} on {}",
//                             fontInfo.pointsize, minFontSize, this);
//
//                return false;
            }
        }

        return true;
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
    public void invalidateCache ()
    {
        setBounds(null);

        setBaseline(null);
        setConfidence(null);

        meanFont = null;
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
    public static void dump (String title,
                             List<TextLine> lines,
                             boolean withWords)
    {
        logger.info(title);

        for (TextLine line : lines) {
            logger.info("   {}", line);

            if (withWords) {
                for (TextWord word : line.getWords()) {
                    logger.info("      {}", word);
                }
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction maxFontSize = new Scale.Fraction(
                5.0,
                "Maximum font size with respect to interline");

        private final Scale.Fraction minFontSize = new Scale.Fraction(
                1.25,
                "Minimum font size with respect to interline");

        private final Scale.Fraction maxTitleFontSize = new Scale.Fraction(
                8.0,
                "Maximum font size for titles with respect to interline");

        private final Constant.Integer sentenceLowerLength = new Constant.Integer(
                "Chars",
                10,
                "Minimum number of characters to boost a sentence grade");

        private final Constant.Integer sentenceUpperLength = new Constant.Integer(
                "Chars",
                50,
                "Maximum number of characters to boost a sentence grade");

        private final Constant.Ratio maxSentenceGrade = new Constant.Ratio(
                0.90,
                "Maximum value for final sentence grade");

        private final Constant.Ratio maxWordDxFontRatio = new Constant.Ratio(
                1.5,
                "Max horizontal gap between two non-lyrics words as font ratio");

        private final Constant.Ratio minWordDxFontRatio = new Constant.Ratio(
                0.125,
                "Min horizontal gap between two non-lyrics words as font ratio");

        private final Scale.Fraction maxChordDx = new Scale.Fraction(
                1.0,
                "Max horizontal gap between two chord words");

        private final Scale.Fraction maxCharDx = new Scale.Fraction(
                1.0,
                "Max horizontal gap between two chars in a word");
    }
}
