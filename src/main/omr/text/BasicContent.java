//----------------------------------------------------------------------------//
//                                                                            //
//                          B a s i c C o n t e n t                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.glyph.facets.BasicFacet;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphContent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

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
    private static final Logger logger = LoggerFactory.getLogger(
            BasicContent.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Manual value if any. */
    private String manualValue;

    /** Manual role if any. */
    private TextRoleInfo manualRole;

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
    //--------//
    // dumpOf //
    //--------//
    /**
     * Write detailed text information on the standard output.
     */
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        if (manualValue != null) {
            sb.append(String.format("   manual=\"%s\"%n", manualValue));
        }

        if (textWord != null) {
            sb.append(
                    String.format(
                    "   textWord=%s textLine=%s%n",
                    textWord,
                    textWord.getTextLine()));
        }

        return sb.toString();
    }

    //---------------//
    // getManualRole //
    //---------------//
    @Override
    public TextRoleInfo getManualRole ()
    {
        return manualRole;
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

    //-----------------//
    // getTextLocation //
    //-----------------//
    @Override
    public Point getTextLocation ()
    {
        if (textWord != null) {
            return textWord.getLocation();
        } else {
            return null;
        }
    }

    //-------------//
    // getTextRole //
    //-------------//
    @Override
    public TextRoleInfo getTextRole ()
    {
        if (manualRole != null) {
            return manualRole;
        }

        if (textWord != null) {
            TextLine line = textWord.getTextLine();

            if (line != null) {
                return line.getRole();
            }
        }

        return null;
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

    //-------------//
    // getTextWord //
    //-------------//
    @Override
    public TextWord getTextWord ()
    {
        return textWord;
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        // TBD
    }

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

    //---------------//
    // setManualRole //
    //---------------//
    @Override
    public void setManualRole (TextRoleInfo manualRole)
    {
        this.manualRole = manualRole;
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
