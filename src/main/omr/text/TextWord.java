//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t W o r d                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.glyph.facets.Glyph;

import omr.score.common.PixelRectangle;

import omr.ui.symbol.TextFont;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code TextWord} defines a structure to report all information
 * about an OCR-decoded (or manual) word.
 *
 * @author Hervé Bitteur
 */
public class TextWord
        extends TextBasedItem
{
    //~ Static fields/initializers ---------------------------------------------

    /** Comparator based on word size. */
    public static final Comparator<TextWord> bySize = new Comparator<TextWord>()
    {
        @Override
        public int compare (TextWord o1,
                            TextWord o2)
        {
            return Integer.compare(o1.getValue().length(),
                                   o2.getValue().length());
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

    //~ Instance fields --------------------------------------------------------
    //
    /** Containing TextLine. */
    private TextLine textLine;

    /** Detected font attributes. */
    private final FontInfo fontInfo;

    /** Chars that compose this word. */
    private final List<TextChar> chars = new ArrayList<>();

    /** Underlying glyph, if known. */
    private Glyph glyph;

    /** Precise font size, lazily computed. */
    private Float preciseFontSize;

    //~ Constructors -----------------------------------------------------------
    //
    //----------//
    // TextWord //
    //----------//
    /**
     * Creates a new TextWord object, built from its contained TextChar
     * instances.
     *
     * @param baseline   word baseline
     * @param value      UTF8 content for this word
     * @param fontInfo   Font information for this word
     * @param confidence confidence level in the word value
     * @param chars      The sequence of chars descriptors
     * @param textLine   the containing TextLine instance
     */
    public TextWord (Line2D baseline,
                     String value,
                     FontInfo fontInfo,
                     Integer confidence,
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
     *
     * @param bounds     Bounding box
     * @param value      UTF8 content for this word
     * @param baseline   word baseline
     * @param confidence OCR confidence in this word content
     * @param fontInfo   Font information for this word
     * @param textLine   the containing TextLine instance
     */
    public TextWord (Rectangle bounds,
                     String value,
                     Line2D baseline,
                     Integer confidence,
                     FontInfo fontInfo,
                     TextLine textLine)
    {
        super(bounds, value, baseline, confidence);
        this.textLine = textLine;
        this.fontInfo = fontInfo;
    }

    //~ Methods ----------------------------------------------------------------
    //
    //------------------//
    // createManualWord //
    //------------------//
    /**
     * Create a TextWord instance manually, out of a given glyph and
     * value.
     * TODO: Perhaps we could improve the baseline, according to the precise
     * string value provided.
     *
     * @param glyph the underlying glyph
     * @param value the provided string value
     */
    public static TextWord createManualWord (Glyph glyph,
                                             String value)
    {
        PixelRectangle box = glyph.getBounds();
        int fontSize = (int) Math.
                rint(
                TextFont.computeFontSize(value, FontInfo.DEFAULT, box.getSize()));
        TextWord word = new TextWord(
                box,
                value,
                new Line2D.Double(box.x,
                                  box.y + box.height,
                                  box.x + box.width,
                                  box.y + box.height),
                100, // Confidence
                FontInfo.createDefault(fontSize),
                null);

        word.setGlyph(glyph);

        return word;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Overridden to use glyph manual value if any, and fall back using
     * initial value otherwise.
     *
     * @return the value to be used
     */
    @Override
    public String getValue ()
    {
        if (glyph != null && glyph.getManualValue() != null) {
            return glyph.getManualValue();
        } else {
            return getInternalValue();
        }
    }

    //------------------//
    // getInternalValue //
    //------------------//
    public String getInternalValue ()
    {
        return super.getValue();
    }
    
    /**
     * Report the length of this word value, expressed in a number of 
     * chars.
     * @return the length of the manual value of the associated glyph if any,
     * otherwise the length of the internal value.
     */
    public int getLength()
    {
        return getValue().length();
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
        FontInfo fontInfo = words[0]
                .getFontInfo();
        TextLine line = words[0].textLine;

        return new TextWord(baselineOf(Arrays.asList(words)),
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

    //--------------------//
    // getPreciseFontSize //
    //--------------------//
    /**
     * Report the best computed font size for this word, likely to
     * precisely match the word bounds.
     *
     * <p>The size appears to be a bit larger than OCR detected side, by a
     * factor in the range 1.1 - 1.2. TODO: To be improved, using font
     * attributes for better font selection</p>
     *
     * @return the computed font size
     */
    public float getPreciseFontSize ()
    {
        if (preciseFontSize == null) {
            preciseFontSize = TextFont.computeFontSize(getValue(),
                                                       fontInfo,
                                                       getBounds().getSize());
        }

        return preciseFontSize;
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

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a translation to this word.
     * This applies to internal data (bounds, baseline) and is forwarded to
     * contained TextChar instances.
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

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * {@inheritDoc }
     */
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());

        sb.append(" ").append(getFontInfo());

        if (getGlyph() != null) {
            sb.append(" ").append(getGlyph().idString());
        }

        return sb.toString();
    }
}
