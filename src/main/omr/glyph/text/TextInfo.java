//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t I n f o                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;

import omr.lag.HorizontalOrientation;

import omr.log.Logger;

import omr.score.common.PixelPoint;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>TextInfo</code> handles the textual aspects of a glyph. It
 * handles several text contents, by decreasing priority: <ol><li>manual content
 * (entered manually by the user)</li><li>ocr content (as computed by the OCR
 * engine)</li><li>pseudo content, meant to be used as a placeholder, based on
 * the text type</li></ol>
 *
 * <p>The {@link #getContent} method return the manual content if any, otherwise
 * the ocr content. Access to the pseudo content is done only through the {@link
 * #getPseudoContent} method.</p>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class TextInfo
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TextInfo.class);

    /** The basic font used for text entities */
    public static final Font basicFont = new Font(
        constants.basicFontName.getValue(),
        Font.PLAIN,
        constants.basicFontSize.getValue());

    /** Utility font context, used to compute text characteristics */
    private static FontRenderContext frc = new FontRenderContext(
        null,
        false,
        true);

    /**
     * The ratio to be applied to a font size when exporting to XML
     * TODO: try to figure the reason for this value!
     */
    public static final float EXPORT_RATIO = 0.25f;

    //~ Instance fields --------------------------------------------------------

    /** The glyph this text info belongs to */
    private final Glyph glyph;

    /** Related text area parameters */
    private TextArea textArea;

    /** Mabual content if any */
    private String manualContent;

    /** OCR-based content if any */
    private String ocrContent;

    /** Language used for OCR */
    private String ocrLanguage;

    /** Dummy text content as placeholder, if any */
    private String pseudoContent;

    /** Containing text sentence if any */
    private Sentence sentence;

    /** Role of this text item */
    private TextRole role;

    /** Font size */
    private Float fontSize;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new TextInfo object.
     * @param glyph the related glyph
     */
    public TextInfo (Glyph glyph)
    {
        this.glyph = glyph;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // computeFontSize //
    //-----------------//
    /**
     * Convenient method to compute a font size using a string content and width
     * @param content the string value
     * @param width the string width in pixels
     * @return the computed font size
     */
    public static float computeFontSize (String content,
                                         int    width)
    {
        Rectangle2D rect = basicFont.getStringBounds(content, frc);
        float       ratio = width / (float) rect.getWidth();

        return basicFont.getSize2D() * ratio;
    }

    //--------------//
    // computeWidth //
    //--------------//
    /**
     * Convenient method to report the width of a string in a given font
     * @param content the string value
     * @param font the provided font
     * @return the computed width
     */
    public static double computeWidth (String content,
                                       Font   font)
    {
        return font.getStringBounds(content, frc)
                   .getWidth();
    }

    //------------//
    // getContent //
    //------------//
    /**
     * Report the content (the string value) of this text glyph if any
     * @return the text meaning of this glyph if any, either entered manually
     * or via an OCR function
     */
    public String getContent ()
    {
        if (manualContent != null) {
            return manualContent;
        } else {
            return ocrContent;
        }
    }

    //-------------//
    // getFontSize //
    //-------------//
    /**
     * Report the proper font size for the textual glyph
     * @return the fontSize
     */
    public Float getFontSize ()
    {
        if (fontSize == null) {
            if (getContent() != null) {
                fontSize = computeFontSize(
                    getContent(),
                    glyph.getContourBox().width);
            }
        }

        return fontSize;
    }

    //------------------//
    // setManualContent //
    //------------------//
    /**
     * Manually assign a text meaning to the glyph
     * @param manualContent the string value for this text glyph
     */
    public void setManualContent (String manualContent)
    {
        this.manualContent = manualContent;
        fontSize = null;
    }

    //------------------//
    // getManualContent //
    //------------------//
    /**
     * Report the manually assigned text meaning of the glyph
     * @return manualContent the manual string value for this glyph, if any
     */
    public String getManualContent ()
    {
        return manualContent;
    }

    //---------------//
    // setOcrContent //
    //---------------//
    /**
     * Remember the text content as provided by the OCR engine
     * @param ocrLanguage the Language provided to the OCR for recognition
     * @param ocrContent the OCR string value for this text glyph
     */
    public void setOcrContent (String ocrLanguage,
                               String ocrContent)
    {
        this.ocrLanguage = ocrLanguage;
        this.ocrContent = ocrContent;
        fontSize = null;
    }

    //---------------//
    // getOcrContent //
    //---------------//
    /**
     * Report what the OCR has provided for this glyph
     * @return the text provided by the OCR engine, if any
     */
    public String getOcrContent ()
    {
        return ocrContent;
    }

    //----------------//
    // getOcrLanguage //
    //----------------//
    /**
     * Report which language was used to OCR the glyph content
     * @return the language code, if any
     */
    public String getOcrLanguage ()
    {
        return ocrLanguage;
    }

    //------------------//
    // getPseudoContent //
    //------------------//
    /**
     * Report a dummy content for this glyph (for lack of known content)
     * @return an artificial text content, based on the enclosing sentence type
     */
    public String getPseudoContent ()
    {
        if (pseudoContent == null) {
            if (sentence != null) {
                final int nbChar = (int) Math.rint(
                    ((double) glyph.getContourBox().width) / sentence.getTextHeight());

                if (getTextRole() != null) {
                    pseudoContent = getTextRole()
                                        .getStringHolder(nbChar);
                }
            }
        }

        return pseudoContent;
    }

    //-------------//
    // setSentence //
    //-------------//
    /**
     * Define the enclosing sentence for this (text) glyph
     * @param sentence the enclosing sentence
     */
    public void setSentence (Sentence sentence)
    {
        this.sentence = sentence;
    }

    //-------------//
    // getSentence //
    //-------------//
    /**
     * Report the sentence, if any, this (text) glyph is a component of
     * @return the containing sentence, or null
     */
    public Sentence getSentence ()
    {
        return sentence;
    }

    //-------------//
    // getTextArea //
    //-------------//
    /**
     * Report the text area that contains this glyph
     * @return the text area for this glyph
     */
    public TextArea getTextArea ()
    {
        if (textArea == null) {
            try {
                textArea = new TextArea(
                    null, // NO SYSTEM !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    null,
                    glyph.getLag().createAbsoluteRoi(glyph.getContourBox()),
                    new HorizontalOrientation());
            } catch (Exception ex) {
                logger.warning("Cannot create TextArea for glyph " + this);
            }
        }

        return textArea;
    }

    //-------------//
    // setTextRole //
    //-------------//
    /**
     * Force the text type (role) of the textual glyph within the score
     * @param type the role of this textual item
     */
    public void setTextRole (TextRole type)
    {
        this.role = type;

        resetPseudoContent();
    }

    //-------------//
    // getTextRole //
    //-------------//
    /**
     * Report the text type (role) of the textual glyph within the score
     * @return the role of this textual glyph
     */
    public TextRole getTextRole ()
    {
        if (role == null) {
            if (sentence != null) {
                setTextRole(TextRole.guessRole(glyph, sentence.getSystem()));
            }
        }

        return role;
    }

    //--------------//
    // getTextStart //
    //--------------//
    /**
     * Report the starting point of this text glyph, which is the left side
     * abscissa and the baseline ordinate
     * @return the starting point of the text glyph, specified in pixels
     */
    public PixelPoint getTextStart ()
    {
        return new PixelPoint(
            glyph.getContourBox().x,
            getTextArea().getBaseline());
    }

    //--------------------//
    // resetPseudoContent //
    //--------------------//
    /**
     * Invalidate the glyph pseudo content, as a consequence of a sentence type
     * change, to force its re-evaluation later
     */
    public void resetPseudoContent ()
    {
        pseudoContent = null;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Text");

        if (getFontSize() != null) {
            sb.append(" fontSize:")
              .append(getFontSize());
        }

        if (manualContent != null) {
            sb.append(" manual:")
              .append(manualContent);
        }

        if (ocrContent != null) {
            sb.append(" (")
              .append(ocrLanguage)
              .append(")ocr:")
              .append(ocrContent);
        }

        if (pseudoContent != null) {
            sb.append(" pseudo:")
              .append(pseudoContent);
        }

        sb.append("}");

        return sb.toString();
    }

    //-------------//
    // setTextArea //
    //-------------//
    /**
     * Define the related text area for this glyph
     * @param textArea the related text area which can provide horizontal and
     * vertical histograms
     */
    void setTextArea (TextArea textArea)
    {
        this.textArea = textArea;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Integer defaultResolution = new Constant.Integer(
            "dpi",
            300,
            "Default resolution of sheet scan");
        Constant.Integer basicFontSize = new Constant.Integer(
            "points",
            10,
            "Standard font point size for texts");
        Constant.String  basicFontName = new Constant.String(
            "Serif", //"Sans Serif",
            "Standard font name for texts");
    }
}
