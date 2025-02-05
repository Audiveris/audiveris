//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T e x t W o r d                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.symbol.OmrFont;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.StringUtil;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.XmlUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Class <code>TextWord</code> defines a structure to report all information about an
 * OCR-decoded (or manual) word.
 *
 * @author Hervé Bitteur
 */
public class TextWord
        extends TextBasedItem
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TextWord.class);

    /** Abnormal characters. */
    private static final char[] ABNORMAL_CHARS = new char[] { '\\' };

    /** Regexp for one-letter words. */
    private static final Pattern ONE_LETTER_WORDS = compileRegexp(constants.oneLetterWordRegexp);

    /** Regexp for abnormal words. */
    private static final Pattern ABNORMAL_WORDS = compileRegexp(constants.abnormalWordRegexp);

    /** Regexp for tuplets. */
    private static final Pattern TUPLET_WORDS = compileRegexp(constants.tupletWordRegexp);

    /** Regexp for words with a dash. */
    private static final Pattern DASHED_WORDS = compileRegexp(constants.dashedWordRegexp);

    /** Regexp for dash-only words. */
    private static final Pattern LONG_DASH_WORDS = compileRegexp(constants.longDashWordRegexp);

    /** Regexp for part-name words. */
    public static final Pattern PART_NAME_WORDS = compileRegexp(constants.partNameRegexp);

    /** Comparator based on word size. */
    public static final Comparator<TextWord> bySize = (TextWord o1,
                                                       TextWord o2) -> Integer.compare(
                                                               o1.getValue().length(),
                                                               o2.getValue().length());

    /** Comparator based on word starting abscissa. */
    public static final Comparator<TextWord> byAbscissa = (TextWord o1,
                                                           TextWord o2) -> Integer.compare(
                                                                   o1.getBounds().x,
                                                                   o2.getBounds().x);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Containing TextLine. */
    @Navigable(false)
    private TextLine textLine;

    /** Detected font attributes. */
    private FontInfo fontInfo;

    /** Chars that compose this word. */
    private final List<TextChar> chars = new ArrayList<>();

    /** Underlying glyph, if known. */
    private Glyph glyph;

    /** Has this word been adjusted?. */
    private boolean adjusted;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new TextWord object, built from its contained TextChar instances.
     *
     * @param sheet      the related sheet
     * @param baseline   word baseline
     * @param value      UTF-8 content for this word
     * @param fontInfo   Font information for this word
     * @param confidence confidence level in the word value
     * @param chars      The sequence of chars descriptors
     * @param textLine   the containing TextLine instance
     */
    public TextWord (Sheet sheet,
                     Line2D baseline,
                     String value,
                     FontInfo fontInfo,
                     Double confidence,
                     List<TextChar> chars,
                     TextLine textLine)
    {
        this(sheet, boundsOf(chars), value, baseline, confidence, fontInfo, textLine);

        for (TextChar ch : chars) {
            this.chars.add(ch);
        }
    }

    /**
     * Creates a new TextWord object, with all OCR information.
     * TextChar instances are meant to be added later, as detailed information
     * <p>
     * FontInfo data is directly copied from OCR, however its pointSize is often too low, varying
     * around -10% or -20%, so we have to refine this value, based on word bounds.
     *
     * @param sheet      the related sheet
     * @param bounds     Bounding box
     * @param value      UTF-8 content for this word
     * @param baseline   word baseline
     * @param confidence OCR confidence in this word content
     * @param fontInfo   Font information for this word
     * @param textLine   the containing TextLine instance
     */
    public TextWord (Sheet sheet,
                     Rectangle bounds,
                     String value,
                     Line2D baseline,
                     Double confidence,
                     FontInfo fontInfo,
                     TextLine textLine)
    {
        super(sheet, bounds, value, baseline, confidence);
        this.textLine = textLine;
        this.fontInfo = fontInfo;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // addChar //
    //---------//
    /**
     * Append a char descriptor.
     *
     * @param ch the char descriptor
     */
    public void addChar (TextChar ch)
    {
        chars.add(ch);
    }

    //--------//
    // adjust //
    //--------//
    /**
     * Adjust word value and font according to actual bounds.
     * <p>
     * This is a work around for poor OCR output on long dash items, especially in lyrics.
     * <ul>
     * <li>Bounds are generally OK.
     * <li>Value is often just a space character (x20) or a mix of hyphen (x2d), low line (x5f)
     * and m dash (x2014).
     * <li>Font size is not reliable.
     * </ul>
     * Therefore, we artificially inject dashes (underscores) and forge a font size.
     *
     * @param scale global sheet scale
     */
    public void adjust (Scale scale)
    {
        if (value.equals(" ")) {
            final String dash = "_";

            // fontinfo
            int size = scale.toPixels(constants.standardFontSize);
            fontInfo = FontInfo.createDefault(size);

            Font font = new TextFont(fontInfo);
            TextLayout layout = new TextLayout(dash, font, OmrFont.frc);
            Rectangle2D rect = layout.getBounds();

            // chars
            chars.clear();

            double meanCharWidth = rect.getWidth();
            int len = (int) Math.rint(bounds.width / rect.getWidth());

            for (int i = 0; i < len; i++) {
                Rectangle cb = new Rectangle2D.Double(
                        bounds.x + (i * meanCharWidth),
                        bounds.y,
                        meanCharWidth,
                        bounds.height).getBounds();
                addChar(new TextChar(cb, dash));
            }

            checkValue();
            setAdjusted(true);
            textLine.invalidateCache();
            logger.debug("adjusted {}", this);
        }
    }

    //----------------//
    // adjustFontSize //
    //----------------//
    /**
     * Adjust font size precisely according to underlying bounds.
     *
     * @param family the text family selected for the related sheet
     * @return true if OK, false if no font modification was performed
     */
    public boolean adjustFontSize (TextFamily family)
    {
        // Discard one-char words, they are not reliable
        if (getLength() <= 1) {
            return false;
        }

        final int style = (fontInfo.isBold ? Font.BOLD : 0) | (fontInfo.isItalic ? Font.ITALIC : 0);
        final TextFont font = new TextFont(family.getFontName(), null, style, fontInfo.pointsize);

        final int fontSize = font.computeSize(getValue(), getBounds().getSize());
        final double ratio = (double) fontSize / fontInfo.pointsize;

        if (ratio < constants.minFontRatio.getSourceValue() //
                || ratio > constants.maxFontRatio.getSourceValue()) {
            logger.info("   Abnormal font ratio {} {}", String.format("%.2f", ratio), this);

            return false;
        }

        fontInfo = new FontInfo(fontInfo, fontSize);
        textLine.invalidateCache();

        return true;
    }

    //---------------//
    // checkValidity //
    //---------------//
    /**
     * Check the provided OCR'd word (the word is not modified).
     *
     * @return reason for invalidity if any, otherwise null
     */
    public String checkValidity ()
    {
        if (isVip()) {
            logger.info("VIP TextWord.checkValidity {}", this);
        }

        // Remove word with too low confidence
        if (getConfidence() < constants.lowConfidence.getValue()) {
            logger.debug("      low confident word {}", this);
            return "low-confident-word";
        }

        // Remove word with abnormal characters
        for (char ch : ABNORMAL_CHARS) {
            if (value.indexOf(ch) != -1) {
                logger.debug("      abnormal char {} in {}", ch, this);
                return "abnormal-chars";
            }
        }

        // Remove word with invalid XML characters
        WrappedBoolean stripped = new WrappedBoolean(false);
        XmlUtil.stripNonValidXMLCharacters(value, stripped);

        if (stripped.isSet()) {
            logger.debug("      invalid XML chars in {}", this);
            return "invalid-xml-chars";
        }

        // Remove abnormal word
        if (ABNORMAL_WORDS != null) {
            Matcher matcher = ABNORMAL_WORDS.matcher(value);

            if (matcher.matches()) {
                logger.debug("      abnormal word value {}", this);
                return "abnormal-word-value";
            }
        }

        // Remove tuplet
        if (TUPLET_WORDS != null) {
            Matcher matcher = TUPLET_WORDS.matcher(value);

            if (matcher.matches()) {
                logger.debug("      tuplet word value {}", this);
                return "tuplet-word-value";
            }
        }

        //        // Remove one-letter word, except in some cases
        //        if (ONE_LETTER_WORDS != null) {
        //            final ProcessingSwitches switches = sheet.getStub().getProcessingSwitches();
        //            if (!switches.getValue(chordNames) //
        //                    && !switches.getValue(lyrics) //
        //                    && !switches.getValue(lyricsAboveStaff)) {
        //                final Matcher matcher = ONE_LETTER_WORDS.matcher(value);
        //
        //                if (matcher.matches()) {
        //                    // Accept only specific char
        //                    final String mid = matcher.group(1);
        //                    logger.info("mid: {} in {}", mid, this);
        //                    ///logger.debug("      one-letter word {}", this);
        //                    ///return "one-letter-word";
        //                }
        //            }
        //        }

        // Remove long dash word (because they might well be ending horizontal lines)
        if (LONG_DASH_WORDS != null) {
            Matcher matcher = LONG_DASH_WORDS.matcher(value);

            if (matcher.matches()) {
                logger.debug("      long dash word {}", this);
                return "long-dash-word";
            }
        }

        return null; // No invalidity detected, word is considered as OK
    }

    //------------//
    // checkValue //
    //------------//
    /**
     * Make sure word value is consistent with its sequence of chars values.
     */
    public void checkValue ()
    {
        StringBuilder sb = new StringBuilder();

        for (TextChar ch : chars) {
            sb.append(ch.getValue());
        }

        String sbValue = sb.toString();

        if (!getInternalValue().equals(sbValue)) {
            logger.debug(
                    "      word at {} updated from '{}' [{}]  to '{}' [{}]",
                    getBounds(),
                    getInternalValue(),
                    StringUtil.codesOf(getInternalValue(), false),
                    sbValue,
                    StringUtil.codesOf(sbValue, false));
            setValue(sbValue);
        }
    }

    //------//
    // copy //
    //------//
    /**
     * Make a copy of this TextWord, that will share the same TextChar instances.
     *
     * @return a shallow copy
     */
    public TextWord copy ()
    {
        return new TextWord(
                sheet,
                getBaseline(),
                getValue(),
                fontInfo,
                getConfidence(),
                chars,
                null);
    }

    //----------//
    // getChars //
    //----------//
    /**
     * Report the sequence of char descriptors.
     *
     * @return the chars
     */
    public List<TextChar> getChars ()
    {
        return chars;
    }

    //-------------//
    // getFontInfo //
    //-------------//
    /**
     * Report the related font attributes.
     *
     * @return the fontInfo
     */
    public FontInfo getFontInfo ()
    {
        return fontInfo;
    }

    //----------//
    // getGlyph //
    //----------//
    /**
     * Report the underlying glyph, if any.
     *
     * @return the glyph, perhaps null
     */
    public Glyph getGlyph ()
    {
        return glyph;
    }

    //------------------//
    // getInternalValue //
    //------------------//
    /**
     * Report the word content.
     *
     * @return text value
     */
    public String getInternalValue ()
    {
        return super.getValue();
    }

    /**
     * Report the length of this word value, expressed in a number of chars.
     *
     * @return the length of the manual value of the associated glyph if any,
     *         otherwise the length of the internal value.
     */
    public int getLength ()
    {
        return getValue().length();
    }

    //-------------//
    // getSubWords //
    //-------------//
    /**
     * Report the potential sub-words of the provided word, based on the provided
     * scanner to adapt to OCR or Manual values.
     *
     * @param line    the containing line
     * @param scanner how to scan the word
     * @return the sequence of created (sub)words, perhaps empty
     */
    public List<TextWord> getSubWords (TextLine line,
                                       WordScanner scanner)
    {
        final List<TextWord> subWords = new ArrayList<>();
        final int contentLength = value.length();

        while (scanner.hasNext()) {
            String subValue = scanner.next();

            if (subValue.length() < contentLength) {
                // We have a real subword
                List<TextChar> wordChars = scanner.getWordChars();

                // Compute (sub) baseline parameters
                Line2D base = getBaseline();
                int x1 = wordChars.get(0).getBounds().x;
                Point2D p1 = LineUtil.intersection(
                        base.getP1(),
                        base.getP2(),
                        new Point2D.Double(x1, 0),
                        new Point2D.Double(x1, 100));

                Rectangle box = wordChars.get(wordChars.size() - 1).getBounds();
                int x2 = (box.x + box.width) - 1;
                Point2D p2 = LineUtil.intersection(
                        base.getP1(),
                        base.getP2(),
                        new Point2D.Double(x2, 0),
                        new Point2D.Double(x2, 100));
                Line2D subBase = new Line2D.Double(p1, p2);

                // Allocate sub-word
                TextWord newWord = new TextWord(
                        sheet,
                        subBase,
                        subValue,
                        getFontInfo(),
                        getConfidence(),
                        wordChars,
                        line);

                if (logger.isDebugEnabled()) {
                    logger.info("      subWord '{}' out of '{}'", newWord.getValue(), value);
                }

                subWords.add(newWord);
            }
        }

        return subWords;
    }

    //-------------//
    // getTextLine //
    //-------------//
    /**
     * Report the containing TextLine instance
     *
     * @return the containing line
     */
    public TextLine getTextLine ()
    {
        return textLine;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Overridden to use glyph manual value, if any, or fall back using initial value.
     *
     * @return the value to be used
     */
    @Override
    public String getValue ()
    {
        return getInternalValue();
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(" codes[").append(StringUtil.codesOf(getValue(), false)).append(']');

        sb.append(" ").append(getFontInfo());

        if (glyph != null) {
            sb.append(" ").append(glyph.idString());
        }

        return sb.toString();
    }

    /**
     * @return the adjusted
     */
    public boolean isAdjusted ()
    {
        return adjusted;
    }

    //----------//
    // isDashed //
    //----------//
    /**
     * Report whether the word contains a dash-family character.
     *
     * @return true if there is a dash in word
     */
    public boolean isDashed ()
    {
        return DASHED_WORDS.matcher(value).matches();
    }

    /**
     * @param adjusted the adjusted to set
     */
    public void setAdjusted (boolean adjusted)
    {
        this.adjusted = adjusted;
    }

    //----------//
    // setGlyph //
    //----------//
    /**
     * Assign the underlying glyph.
     *
     * @param glyph the glyph to set
     */
    public void setGlyph (Glyph glyph)
    {
        this.glyph = glyph;

        if (glyph.isVip()) {
            setVip(true);
        }
    }

    //-------------//
    // setTextLine //
    //-------------//
    /**
     * Assign a new containing TextLine instance
     *
     * @param textLine the new containing line
     */
    public void setTextLine (TextLine textLine)
    {
        this.textLine = textLine;

        if (isVip() && (textLine != null)) {
            textLine.setVip(true);
        }
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a translation to this word.
     * This applies to internal data (bounds, baseline) and is forwarded to contained TextChar's.
     *
     * @param dx abscissa translation
     * @param dy ordinate translation
     */
    @Override
    public void translate (int dx,
                           int dy)
    {
        // Translate word bounds and baseline
        super.translate(dx, dy);

        // Translate contained descriptors
        for (TextChar ch : chars) {
            ch.translate(dx, dy);
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //---------------//
    // compileRegexp //
    //---------------//
    /**
     * Compile the provided regexp into a pattern.
     *
     * @param str the regexp constant string to compile
     * @return the created pattern
     */
    private static Pattern compileRegexp (Constant.String str)
    {
        try {
            return Pattern.compile(str.getValue());
        } catch (PatternSyntaxException pse) {
            logger.error("Error in regexp {}", str, pse);
            return null;
        }
    }

    //---------//
    // mergeOf //
    //---------//
    /**
     * Return a word which results from the merge of the provided words.
     *
     * @param words the words to merge
     * @return the compound word
     */
    public static TextWord mergeOf (TextWord... words)
    {
        List<TextChar> chars = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (TextWord word : words) {
            chars.addAll(word.getChars());
            sb.append(word.getValue());
        }

        // Use font info of first word. What else?
        FontInfo fontInfo = words[0].getFontInfo();
        TextLine line = words[0].textLine;
        Sheet sheet = words[0].getSheet();

        return new TextWord(
                sheet,
                baselineOf(Arrays.asList(words)),
                sb.toString(),
                fontInfo,
                confidenceOf(Arrays.asList(words)),
                chars,
                line);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Double lowConfidence = new Constant.Double(
                "0..1",
                0.30,
                "Really low confidence to exclude words");

        private final Constant.String oneLetterWordRegexp = new Constant.String(
                "^[\\W]*([\\w])[\\W]*$",
                "Regular expression to detect one-letter words");

        private final Constant.String abnormalWordRegexp = new Constant.String(
                "^[^a-zA-Z_0-9-.,&=©\\?}]+$",
                "Regular expression to detect abnormal words");

        private final Constant.String tupletWordRegexp = new Constant.String(
                "^.*[-_\u2014][36][-_\u2014].*$",
                "Regular expression to detect tuplet-like words");

        private final Constant.String longDashWordRegexp = new Constant.String(
                "^[-_\u2014]{2,}$",
                "Regular expression to detect long (2+) dash-only words");

        private final Constant.String dashedWordRegexp = new Constant.String(
                "^.*[-_\u2014].*$",
                "Regular expression to detect words with embedded dashes");

        private final Constant.String partNameRegexp = new Constant.String(
                ".*[\\w]+.*$",
                "Regular expression to validate a part name");

        private final Constant.Ratio maxFontRatio = new Constant.Ratio(
                2.0,
                "Maximum ratio between ocr and glyph font sizes");

        private final Constant.Ratio minFontRatio = new Constant.Ratio(
                0.3,
                "Minimum ratio between ocr and glyph font sizes");

        private final Scale.Fraction standardFontSize = new Scale.Fraction(
                2.5,
                "Standard font size");
    }
}
