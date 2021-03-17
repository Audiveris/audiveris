//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T e x t W o r d                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import org.audiveris.omr.ui.symbol.OmrFont;
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
 * Class {@code TextWord} defines a structure to report all information about an
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
    private static final char[] ABNORMAL_CHARS = new char[]{'\\'};

    /** Regexp for abnormal words. */
    private static final Pattern ABNORMAL_WORDS = getAbnormalWordPattern();

    /** Regexp for dashed words. */
    private static final Pattern DASH_WORDS = getDashWordPattern();

    /** Comparator based on word size. */
    public static final Comparator<TextWord> bySize = new Comparator<TextWord>()
    {
        @Override
        public int compare (TextWord o1,
                            TextWord o2)
        {
            return Integer.compare(o1.getValue().length(), o2.getValue().length());
        }
    };

    /** Comparator based on word starting abscissa. */
    public static final Comparator<TextWord> byAbscissa = new Comparator<TextWord>()
    {
        @Override
        public int compare (TextWord o1,
                            TextWord o2)
        {
            return Integer.compare(o1.getBounds().x, o2.getBounds().x);
        }
    };

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

    /** Precise font size, lazily computed. */
    private Float preciseFontSize;

    /** Has this word been adjusted?. */
    private boolean adjusted;

    //~ Constructors -------------------------------------------------------------------------------
    //----------//
    // TextWord //
    //----------//
    /**
     * Creates a new TextWord object, built from its contained TextChar instances.
     *
     * @param baseline   word baseline
     * @param value      UTF-8 content for this word
     * @param fontInfo   Font information for this word
     * @param confidence confidence level in the word value
     * @param chars      The sequence of chars descriptors
     * @param textLine   the containing TextLine instance
     */
    public TextWord (Line2D baseline,
                     String value,
                     FontInfo fontInfo,
                     Double confidence,
                     List<TextChar> chars,
                     TextLine textLine)
    {
        this(boundsOf(chars), value, baseline, confidence, fontInfo, textLine);

        for (TextChar ch : chars) {
            this.chars.add(ch);
        }
    }

    //----------//
    // TextWord //
    //----------//
    /**
     * Creates a new TextWord object, with all OCR information.
     * TextChar instances are meant to be added later, as detailed information
     * <p>
     * FontInfo data is directly copied from OCR, however its pointSize is often too low, varying
     * around -10% or -20%, so we have to refine this value, based on word bounds.
     *
     * @param bounds     Bounding box
     * @param value      UTF-8 content for this word
     * @param baseline   word baseline
     * @param confidence OCR confidence in this word content
     * @param fontInfo   Font information for this word
     * @param textLine   the containing TextLine instance
     */
    public TextWord (Rectangle bounds,
                     String value,
                     Line2D baseline,
                     Double confidence,
                     FontInfo fontInfo,
                     TextLine textLine)
    {
        super(bounds, value, baseline, confidence);
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
     * @return true if OK, false if abnormal font modification
     */
    public boolean adjustFontSize ()
    {
        double size = TextFont.computeFontSize(value, fontInfo, bounds.getSize());
        double ratio = size / fontInfo.pointsize;

        if (ratio < constants.minFontRatio.getSourceValue()
                    || ratio > constants.maxFontRatio.getSourceValue()) {
            logger.info("      abnormal font ratio {} {}", String.format("%.2f", ratio), this);

            return false;
        }

        fontInfo = new FontInfo(fontInfo, (int) Math.rint(size));
        textLine.invalidateCache();

        return true;
    }

    /**
     * @return the adjusted
     */
    public boolean isAdjusted ()
    {
        return adjusted;
    }

    /**
     * @param adjusted the adjusted to set
     */
    public void setAdjusted (boolean adjusted)
    {
        this.adjusted = adjusted;
    }

    //----------//
    // isDashed //
    //----------//
    /**
     * Report whether contains a dash-family character.
     *
     * @return true if there is a dash in word
     */
    public boolean isDashed ()
    {
        return DASH_WORDS.matcher(value).matches();
    }

    //---------------//
    // checkValidity //
    //---------------//
    /**
     * Check the provided OCR'ed word.(the word is not modified)
     *
     * @param scale         scale of containing sheet
     * @param inSheetHeader true if word is located above first system
     * @return reason for invalidity if any, otherwise null
     */
    public String checkValidity (Scale scale,
                                 boolean inSheetHeader)
    {
        // Check for abnormal characters
        for (char ch : ABNORMAL_CHARS) {
            if (value.indexOf(ch) != -1) {
                logger.debug("      abnormal char {} in {}", ch, this);

                return "abnormal-chars";
            }
        }

        // Check for invalid XML characters
        WrappedBoolean stripped = new WrappedBoolean(false);
        XmlUtil.stripNonValidXMLCharacters(value, stripped);

        if (stripped.isSet()) {
            logger.debug("      invalid XML chars in {}", this);

            return "invalid-xml-chars";
        }

        // Check for invalid word values
        if (ABNORMAL_WORDS != null) {
            Matcher matcher = ABNORMAL_WORDS.matcher(value);

            if (matcher.matches()) {
                logger.debug("      abnormal word value {}", this);

                return "abnormal-word-value";
            }
        }

        //
        //        final int minWidthForCheck = scale.toPixels(constants.minWidthForCheck);
        //        final int minHeightForCheck = scale.toPixels(constants.minHeightForCheck);
        //
        //        final double minAspectRatio = constants.minAspectRatio.getValue();
        //        final double maxAspectRatio = constants.maxAspectRatio.getValue();
        //
        //        final double minDiagRatio = constants.minDiagRatio.getValue();
        //        final double maxDiagRatio = constants.maxDiagRatio.getValue();
        //
        //        // Check for non-consistent aspect
        //        // (Applicable only for non-tiny width or height)
        //        //
        //        // Warning: a lyric syllable may contain a long horizontal line,
        //        // that OCR sometimes translates as a too short sequence of '-' or '_' chars
        //        // So, it's safer to skip test in that case
        //        if (DASH_WORDS != null) {
        //            Matcher matcher = DASH_WORDS.matcher(value);
        //
        //            if (matcher.matches()) {
        //                logger.debug("      dash word value {}", this);
        //
        //                return null; // OK
        //            }
        //        }
        //
        //        Rectangle box = getBounds();
        //
        //        if ((box.width >= minWidthForCheck) && (box.height >= minHeightForCheck)) {
        //            Font font = new TextFont(getFontInfo());
        //            TextLayout layout = new TextLayout(value, font, frc);
        //            Rectangle2D rect = layout.getBounds();
        //            double xRatio = box.width / rect.getWidth();
        //            double yRatio = box.height / rect.getHeight();
        //            double aRatio = yRatio / xRatio;
        //
        //            if ((aRatio < minAspectRatio) || (aRatio > maxAspectRatio)) {
        //                ///logger.debug("      invalid aspect {} vs [{}-{}] for {}",
        //                ///             aRatio, minAspectRatio, maxAspectRatio, this);
        //
        //                ///return "invalid aspect";
        //            }
        //
        //            // Check size
        //            double boxDiag = Math.hypot(box.width, box.height);
        //            double rectDiag = Math.hypot(rect.getWidth(), rect.getHeight());
        //            double diagRatio = boxDiag / rectDiag;
        //
        //            if ((diagRatio < minDiagRatio) || (diagRatio > maxDiagRatio)) {
        //                ///logger.debug("      invalid diagonal {} vs [{}-{}] for {}",
        //                ///             diagRatio, minDiagRatio, maxDiagRatio, this);
        //                ///return "invalid diagonal";
        //            }
        //        }
        //
        return null; // OK
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
            logger.debug("      word at {} updated from '{}' [{}]  to '{}' [{}]",
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
        return new TextWord(getBaseline(), getValue(), fontInfo, getConfidence(), chars, null);
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

    //--------------------//
    // getPreciseFontSize //
    //--------------------//
    /**
     * Report the best computed font size for this word, likely to precisely match the
     * word bounds.
     * <p>
     * The size appears to be a bit larger than OCR detected side, by a factor in the range 1.1 -
     * 1.2. TODO: To be improved, using font attributes for better font selection
     * </p>
     *
     * @return the computed font size
     */
    public float getPreciseFontSize ()
    {
        if (preciseFontSize == null) {
            preciseFontSize = TextFont.computeFontSize(getValue(), fontInfo, getBounds().getSize());
        }

        return preciseFontSize;
    }

    //--------------------//
    // setPreciseFontSize //
    //--------------------//
    /**
     * Assign a font size.
     * (to enforce consistent font size across all words of the same sentence)
     *
     * @param preciseFontSize the enforced font size, or null
     */
    public void setPreciseFontSize (Float preciseFontSize)
    {
        this.preciseFontSize = preciseFontSize;
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

    //-------------//
    // isConfident //
    //-------------//
    public boolean isConfident ()
    {
        return getConfidence() >= constants.lowConfidence.getValue();
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

        // Translate contained descriptorrs
        for (TextChar ch : chars) {
            ch.translate(dx, dy);
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(" ").append(getFontInfo());

        if (glyph != null) {
            sb.append(" ").append(glyph.idString());
        }

        return sb.toString();
    }

    //------------------//
    // createManualWord //
    //------------------//
    /**
     * Create a TextWord instance manually, out of a given glyph and value.
     * <p>
     * TODO: Perhaps we could improve the baseline, according to the precise string value provided.
     *
     * @param glyph the underlying glyph
     * @param value the provided string value
     * @return the TextWord created
     */
    public static TextWord createManualWord (Glyph glyph,
                                             String value)
    {
        Rectangle box = glyph.getBounds();
        int fontSize = (int) Math.rint(
                TextFont.computeFontSize(value, FontInfo.DEFAULT, box.getSize()));
        TextWord word = new TextWord(
                box,
                value,
                new Line2D.Double(box.x, box.y + box.height, box.x + box.width, box.y + box.height),
                1.0, // Confidence
                FontInfo.createDefault(fontSize),
                null);

        word.setGlyph(glyph);

        return word;
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

        return new TextWord(
                baselineOf(Arrays.asList(words)),
                sb.toString(),
                fontInfo,
                confidenceOf(Arrays.asList(words)),
                chars,
                line);
    }

    //------------------------//
    // getAbnormalWordPattern //
    //------------------------//
    /**
     * Compile the provided regexp to detect abnormal words
     *
     * @return the pattern for abnormal words, if successful
     */
    private static Pattern getAbnormalWordPattern ()
    {
        try {
            return Pattern.compile(constants.abnormalWordRegexp.getValue());
        } catch (PatternSyntaxException pse) {
            logger.warn("Error in regexp for abnormal words", pse);

            return null;
        }
    }

    //----------------//
    // getDashPattern //
    //----------------//
    /**
     * Compile the provided regexp to detect words with embedded dashes.
     *
     * @return the pattern for dashed words, if successful
     */
    private static Pattern getDashWordPattern ()
    {
        try {
            return Pattern.compile(constants.dashWordRegexp.getValue());
        } catch (PatternSyntaxException pse) {
            logger.warn("Error in regexp for dash words", pse);

            return null;
        }
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

        private final Constant.String abnormalWordRegexp = new Constant.String(
                "^[°<>']$",
                "Regular expression to detect abnormal words");

        private final Constant.String dashWordRegexp = new Constant.String(
                "^.*[-_\u2014].*$",
                "Regular expression to detect words with embedded dashes");

        private final Constant.Ratio maxFontRatio = new Constant.Ratio(
                2.0,
                "Maximum ratio between ocr and glyph font sizes");

        private final Constant.Ratio minFontRatio = new Constant.Ratio(
                0.5,
                "Minimum ratio between ocr and glyph font sizes");

        //
        //        private final Constant.Ratio maxDiagRatio = new Constant.Ratio(
        //                1.5,
        //                "Maximum ratio between ocr and glyph diagonals");
        //
        //        private final Constant.Ratio minDiagRatio = new Constant.Ratio(
        //                0.5,
        //                "Minimum ratio between ocr and glyph diagonals");
        //
        //        private final Scale.Fraction minWidthForCheck = new Scale.Fraction(
        //                0.15,
        //                "Minimum width to check word aspect");
        //
        //        private final Scale.Fraction minHeightForCheck = new Scale.Fraction(
        //                0.15,
        //                "Minimum height to check word aspect");
        //
        //        private final Constant.Ratio maxAspectRatio = new Constant.Ratio(
        //                1.75,
        //                "Maximum ratio between ocr and glyph aspects");
        //
        //        private final Constant.Ratio minAspectRatio = new Constant.Ratio(
        //                0.4,
        //                "Minimum ratio between ocr and glyph aspects");
        //
        private final Scale.Fraction standardFontSize = new Scale.Fraction(
                2.5,
                "Standard font size");
    }
}
