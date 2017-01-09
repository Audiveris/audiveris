//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T e x t W o r d                                         //
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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

    private static final Logger logger = LoggerFactory.getLogger(TextWord.class);

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
    //
    /** Containing TextLine. */
    @Navigable(false)
    private TextLine textLine;

    /** Detected font attributes. */
    private final FontInfo fontInfo;

    /** Chars that compose this word. */
    private final List<TextChar> chars = new ArrayList<TextChar>();

    /** Underlying glyph, if known. */
    private Glyph glyph;

    /** Precise font size, lazily computed. */
    private Float preciseFontSize;

    //~ Constructors -------------------------------------------------------------------------------
    //
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

        final float size = TextFont.computeFontSize(value, fontInfo, bounds.getSize());
        this.fontInfo = new FontInfo(fontInfo, (int) Math.rint(size));
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // createManualWord //
    //------------------//
    /**
     * Create a TextWord instance manually, out of a given glyph and value.
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
        List<TextChar> chars = new ArrayList<TextChar>();
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
                    "Word at {} updated from ''{}'' to ''{}''",
                    getBounds(),
                    getInternalValue(),
                    sbValue);
            setValue(sbValue);
        }
    }

    //------//
    // copy //
    //------//
    /**
     * Make a copy of this TextLine, that will share the same TextChar instances.
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

    //------------------//
    // getInternalValue //
    //------------------//
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
     * 1.2. TODO: To be improved, using font attributes for better font selection</p>
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
     * Overridden to use glyph manual value if any or fall back using initial value.
     *
     * @return the value to be used
     */
    @Override
    public String getValue ()
    {
        return getInternalValue();
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

        // Translate contained descriptorrs
        for (TextChar ch : chars) {
            ch.translate(dx, dy);
        }

        // Piggy-back processing: Check word value WRT chars values
        //TODO: NO! NO! NO! NO! NO! NO! NO! NO! NO! NO! NO! NO! NO! NO! NO! NO! NO! NO! NO! NO!
        checkValue();
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
}
