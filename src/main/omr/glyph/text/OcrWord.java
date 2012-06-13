//----------------------------------------------------------------------------//
//                                                                            //
//                               O c r W o r d                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.glyph.facets.Glyph;

import omr.score.common.PixelRectangle;

import omr.ui.symbol.TextFont;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code OcrWord} defines a structure to report all information
 * about an OCR-decoded (or manual) word.
 *
 * @author Hervé Bitteur
 */
public class OcrWord
{
    //~ Static fields/initializers ---------------------------------------------

    /** Comparator based on word size. */
    public static final Comparator<OcrWord> sizeComparator = new Comparator<OcrWord>()
    {

        @Override
        public int compare (OcrWord o1,
                            OcrWord o2)
        {
            return Integer.compare(
                    o1.getValue().length(),
                    o2.getValue().length());
        }
    };

    /** Comparator based on word starting abscissa. */
    public static final Comparator<OcrWord> abscissaComparator = new Comparator<OcrWord>()
    {

        @Override
        public int compare (OcrWord o1,
                            OcrWord o2)
        {
            return Integer.compare(
                    o1.bounds.x,
                    o2.bounds.x);
        }
    };

    //~ Instance fields --------------------------------------------------------
    //
    /** Detected word content. */
    private final String value;
    
    /** Detected font attributes. */
    private final FontInfo fontInfo;

    /** Word bounds. */
    private final PixelRectangle bounds;

    /** Baseline (left) ordinate. */
    private int baseline;

    /** Chars that compose this word. */
    private final List<OcrChar> chars = new ArrayList<>();

    /** Precise font size, lazily computed. */
    private Float preciseFontSize;

    /** Underlying glyph, if known. */
    private Glyph glyph;

    //~ Constructors -----------------------------------------------------------
    //
    //---------//
    // OcrWord //
    //---------//
    /**
     * Creates a new OcrWord object, built from its contained OcrChar 
     * instances.
     *
     * @param baseline left ordinate of word baseline
     * @param value    UTF8 content for this word
     * @param fontInfo Font information for this word
     * @param chars    The sequence of chars descriptors
     */
    public OcrWord (int baseline,
                    String value,
                    FontInfo fontInfo,
                    List<OcrChar> chars)
    {
        this(boundsOf(chars), baseline, value, fontInfo);

        for (OcrChar ch : chars) {
            this.chars.add(ch);
        }
    }

    //---------//
    // OcrWord //
    //---------//
    /**
     * Creates a new OcrWord object, with all OCR information.
     * OcrChar instances can be added later, as detailed information
     *
     * @param bounds   Bounding box
     * @param baseline left ordinate of word baseline
     * @param value    UTF8 content for this word
     * @param fontInfo Font information for this word
     */
    public OcrWord (Rectangle bounds,
                    int baseline,
                    String value,
                    FontInfo fontInfo)
    {
        this.bounds = new PixelRectangle(bounds);
        this.value = value;
        this.fontInfo = fontInfo;
        this.baseline = baseline;
    }

    //~ Methods ----------------------------------------------------------------
    //
    //---------//
    // addChar //
    //---------//
    /**
     * Append a char descriptor
     * @param ch the char descriptor
     */
    public void addChar (OcrChar ch)
    {
        chars.add(ch);
    }

    //
    //----------//
    // boundsOf //
    //----------//
    /**
     * Compute the boundiong box of a collection of OcrChar
     *
     * @param chars the provided collection of OcrChar instances
     * @return the bounding box
     */
    public static PixelRectangle boundsOf (Collection<OcrChar> chars)
    {
        PixelRectangle bounds = null;

        for (OcrChar ch : chars) {
            if (bounds == null) {
                bounds = ch.getBounds();
            } else {
                bounds.add(ch.getBounds());
            }
        }

        return bounds;
    }

    //-------------//
    // getBaseline //
    //-------------//
    /**
     * Report the (left) baseline ordinate
     * @return the left ordinate of word baseline
     */
    public int getBaseline ()
    {
        return baseline;
    }

    //
    //-----------//
    // getBounds //
    //-----------//
    /**
     * Report the word bounding box.
     *
     * @return a COPY of the bounds
     */
    public PixelRectangle getBounds ()
    {
        return new PixelRectangle(bounds);
    }

    //----------//
    // getChars //
    //----------//
    /**
     * Report the sequence of char descriptors.
     *
     * @return the chars
     */
    public List<OcrChar> getChars ()
    {
        return chars;
    }

    //--------------------//
    // getPreciseFontSize //
    //--------------------//
    /**
     * Report the best computed font size for this word, likely to
     * precisely match the word bounds.
     * The size appears to be a bit larger than OCR detected side, by a factor
     * in the range 1.1 - 1.2.
     * TODO: To be improved, using font attributes for better font selection
     * @return the computed font size
     */
    public float getPreciseFontSize ()
    {
        if (preciseFontSize == null) {
            preciseFontSize = TextFont.computeFontSize(value, bounds.width);
        }

        return preciseFontSize;
    }
    
    //--------------------//
    // getPreciseFontSize //
    //--------------------//
    /**
     * Assign a font size.
     * (to enforce consistent font size across all words of the same sentence)
     * @param preciseFontSize the enforced font size
     */
    public void setPreciseFontSize (float preciseFontSize)
    {
        this.preciseFontSize = preciseFontSize;
    }

    //-------------//
    // getFontInfo //
    //-------------//
    /**
     * Report the related font attributes.
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
     * Report the underlying glyph, if any
     *
     * @return the glyph, perhaps null
     */
    public Glyph getGlyph ()
    {
        return glyph;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the string content of this word.
     * 
     * @return the value
     */
    public String getValue ()
    {
        return value;
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{OcrWord");
        sb.append(" \"").append(getValue()).append("\"");

        if (getGlyph() != null) {
            sb.append(" ").append(getGlyph().idString());
        }

        sb.append(" ").append(getFontInfo());

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a translation to this word.
     * This applies to internal data (bounds, baseline) and is forwarded to 
     * contained OcrChar instances.
     *
     * @param dx abscissa translation
     * @param dy ordinate translation
     */
    public void translate (int dx,
                           int dy)
    {
        // Translate word bounds
        bounds.x += dx;
        bounds.y += dy;

        // Translate baseline
        baseline += dy;

        // Translate chars descriptorrs
        for (OcrChar ch : chars) {
            ch.translate(dx, dy);
        }
    }
}
