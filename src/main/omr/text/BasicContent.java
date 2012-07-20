//----------------------------------------------------------------------------//
//                                                                            //
//                          B a s i c C o n t e n t                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2012. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.glyph.facets.BasicFacet;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphContent;

import omr.log.Logger;

import omr.score.common.PixelPoint;

/**
 * Class {@code BasicContent} handles the textual aspects of a glyph.
 *
 * <p>It handles several text values, by decreasing priority:</p>
 * <ol>
 * <li>manual value (entered manually by the user)</li>
 * <li>ocr value (as computed by the OCR engine)</li>
 * </ol>
 *
 * <p>The {@link #getTextValue} method returns the manual value if any,
 * otherwise the ocr value.</p>
 *
 * @author Hervé Bitteur
 */
public class BasicContent
        extends BasicFacet
        implements GlyphContent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility. */
    private static final Logger logger = Logger.getLogger(BasicContent.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Manual value if any. */
    private String manualValue;

    /** Language used for OCR. */
    private String ocrLanguage;

    /** Related TextWord, if any. */
    private TextWord textWord;

    //~ Constructors -----------------------------------------------------------
    //
    //--------------//
    // BasicContent //
    //--------------//
    /**
     * Creates a new BasicContent object.
     *
     * @param glyph the related glyph
     */
    public BasicContent (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-------------//
    // isSeparator //
    //-------------//
    /**
     * Predicate to detect a separator.
     *
     * @param str the character to check
     *
     * @return true if this is a separator
     */
    public static boolean isSeparator (String str)
    {
        return str.equals(EXTENSION_STRING) || str.equals(ELISION_STRING)
                || str.equals(HYPHEN_STRING);
    }

    //------//
    // dump //
    //------//
    /**
     * Write detailed text information on the standard output.
     */
    @Override
    public void dump ()
    {
        if (manualValue != null) {
            System.out.println("   manual=\"" + manualValue + "\"");
        }

        if (textWord != null) {
            System.out.println("   textWord=" + textWord
                    + " textLine=" + textWord.getTextLine());
        }
    }

    //----------------//
    // getManualvalue //
    //----------------//
    @Override
    public String getManualValue ()
    {
        return manualValue;
    }

    //----------------//
    // getOcrLanguage //
    //----------------//
    @Override
    public String getOcrLanguage ()
    {
        return ocrLanguage;
    }

    //-------------//
    // getTextWord //
    //-------------//
    @Override
    public TextWord getTextWord ()
    {
        return textWord;
    }

    //-------------//
    // getTextRole //
    //-------------//
    @Override
    public TextRoleInfo getTextRole ()
    {
        if (textWord != null) {
            return textWord.getTextLine().getRole();
        } else {
            return null;
        }
    }

    //-----------------//
    // getTextLocation //
    //-----------------//
    @Override
    public PixelPoint getTextLocation ()
    {
        if (textWord != null) {
            return textWord.getLocation();
        } else {
            return null;
        }
    }

    //--------------//
    // getTextValue //
    //--------------//
    @Override
    public String getTextValue ()
    {
        if (manualValue != null) {
            return manualValue;
        } else {
            if (textWord != null) {
                return textWord.getValue();
            } else {
                return null;
            }
        }
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        // TBD
    }

    //----------------//
    // setManualvalue //
    //----------------//
    @Override
    public void setManualValue (String manualValue)
    {
        this.manualValue = manualValue;

        if (textWord != null) {
            textWord.setPreciseFontSize(null);
        }
    }

    //-------------//
    // setTextWord //
    //-------------//
    @Override
    public void setTextWord (String ocrLanguage,
                             TextWord textWord)
    {
        this.textWord = textWord;

        // Consider this is the current language for this glyph
        this.ocrLanguage = ocrLanguage;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Content");

        if (manualValue != null) {
            sb.append(" manual:")
                    .append("\"")
                    .append(manualValue)
                    .append("\"");
        } else if (textWord != null) {
            sb.append(" ocr(")
                    .append(ocrLanguage)
                    .append("):")
                    .append("\"")
                    .append(textWord.getValue())
                    .append("\"");
        }

        sb.append("}");

        return sb.toString();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(25);
        String value = getTextValue();

        if (value != null) {
            sb.append(" \"")
                    .append(value)
                    .append("\"");
        }

        return sb.toString();
    }
}
