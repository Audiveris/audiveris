//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t I n f o                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphSection;
import omr.glyph.Shape;

import omr.lag.HorizontalOrientation;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.entity.Text.CreatorText.CreatorType;

import omr.sheet.SystemInfo;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

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
 * <p>The font size is taken from OCR if available, otherwise it is computed
 * from physical characteristics.</p>
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

    /** String equivalent of Character used for elision (undertie) */
    public static final String ELISION_STRING = new String(
        Character.toChars(0x203F));

    /** String equivalent of Character used for extension (underscore) */
    public static final String EXTENSION_STRING = "_";

    /** String equivalent of Character used for hyphen */
    public static final String HYPHEN_STRING = "-";

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

    /** (So far empirical) ratio between width and point values */
    public static final float FONT_WIDTH_POINT_RATIO = 4.4f;

    /**
     * Ratio applied to a font point size to get a correct display.
     * TODO: If someone could explain why this ratio is not 1
     */
    public static final float FONT_DISPLAY_RATIO = 4.0f;

    //~ Instance fields --------------------------------------------------------

    /** The glyph this text info belongs to */
    private final Glyph glyph;

    /** Related text area parameters */
    private TextArea textArea;

    /** Manual content if any */
    private String manualContent;

    /** Detailed OCR info about this line */
    private OcrLine ocrLine;

    /** Language used for OCR */
    private String ocrLanguage;

    /** Dummy text content as placeholder, if any */
    private String pseudoContent;

    /** Containing text sentence if any */
    private Sentence sentence;

    /** Role of this text item */
    private TextRole role;

    /** Creator type, if relevant */
    private CreatorType creatorType;

    /** Font size */
    private Integer fontSize;

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

    //----------------//
    // setCreatorType //
    //----------------//
    /**
     * @param creatorType the creatorType to set
     */
    public void setCreatorType (CreatorType creatorType)
    {
        this.creatorType = creatorType;
    }

    //----------------//
    // getCreatorType //
    //----------------//
    /**
     * @return the creatorType
     */
    public CreatorType getCreatorType ()
    {
        return creatorType;
    }

    //-----------//
    // isElision //
    //-----------//
    public boolean isElision ()
    {
        return getContent()
                   .equals(ELISION_STRING);
    }

    //-------------//
    // isExtension //
    //-------------//
    public boolean isExtension ()
    {
        return getContent()
                   .equals(EXTENSION_STRING);
    }

    //----------//
    // isHyphen //
    //----------//
    public boolean isHyphen ()
    {
        return getContent()
                   .equals(HYPHEN_STRING);
    }

    //-----------------------//
    // getMinExtensionAspect //
    //-----------------------//
    public static double getMinExtensionAspect ()
    {
        return constants.minExtensionAspect.getValue();
    }

    //-----------------//
    // computeFontSize //
    //-----------------//
    /**
     * Convenient method to compute a font size using a string content and width
     * @param content the string value
     * @param width the string width in pixels
     * @return the computed font size
     */
    public static Float computeFontSize (String content,
                                         int    width)
    {
        if (content == null) {
            return null;
        }

        Rectangle2D rect = basicFont.getStringBounds(content, frc);
        float       ratio = width / (float) rect.getWidth() / FONT_WIDTH_POINT_RATIO;

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
                   .getWidth() * FONT_WIDTH_POINT_RATIO;
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
            return getOcrContent();
        }
    }

    //-------------//
    // getFontSize //
    //-------------//
    /**
     * Report the proper font size for the textual glyph
     * @return the fontSize
     */
    public Integer getFontSize ()
    {
        if (fontSize == null) {
            if ((ocrLine != null) && (ocrLine.isFontSizeValid())) {
                fontSize = ocrLine.fontSize;
            } else {
                Float fs = computeFontSize(
                    getContent(),
                    glyph.getContourBox().width);

                if (fs != null) {
                    fontSize = (int) Math.rint(fs);
                }
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
    // getOcrContent //
    //---------------//
    /**
     * Report what the OCR has provided for this glyph
     * @return the text provided by the OCR engine, if any
     */
    public String getOcrContent ()
    {
        if (ocrLine == null) {
            return null;
        } else {
            return ocrLine.value;
        }
    }

    //------------//
    // setOcrInfo //
    //------------//
    /**
     * Remember the information as provided by the OCR engine
     * @param ocrLanguage the language provided to OCR engine for recognition
     * @param ocrLine the detailed OCR line about this glyph
     */
    public void setOcrInfo (String  ocrLanguage,
                            OcrLine ocrLine)
    {
        this.ocrLanguage = ocrLanguage;
        this.ocrLine = ocrLine;

        if (ocrLine.isFontSizeValid()) {
            fontSize = ocrLine.fontSize;
        } else {
            fontSize = null;
        }
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

    //------------//
    // getOcrLine //
    //------------//
    /**
     * Report the detailed OCR information
     * @return the ocrLine
     */
    public OcrLine getOcrLine ()
    {
        return ocrLine;
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
                    null, // TODO: NO SYSTEM
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

    //------//
    // dump //
    //------//
    /**
     * Write detailed text information on the standard output
     */
    public void dump ()
    {
        logger.info(this.toString());

        if (ocrLine != null) {
            ocrLine.dump();
        }
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

    //----------------//
    // splitIntoWords //
    //----------------//
    /**
     * Split this (supposedly long lyrics line) sentence into separate words,
     * one word for each lyrics item.
     * @return The collection of (word) glyphs created
     */
    public Collection<Glyph> splitIntoWords ()
    {
        if (ocrLine == null) {
            logger.severe("Trying to split a glyph with no OCR value");

            return null;
        }

        // Parse the content string, to extract words
        SystemInfo    system = sentence.getSystem();
        List<OcrChar> glyphChars = ocrLine.getChars();
        MyScanner     scanner = new MyScanner(glyphChars);
        List<Word>    words = new ArrayList<Word>();

        while (scanner.hasNext()) {
            int           start = scanner.getWordStart();
            int           stop = scanner.getWordStop();
            String        wordText = scanner.next();
            List<OcrChar> wordChars = glyphChars.subList(start, stop + 1);
            words.add(new Word(wordText, wordChars));
        }

        // Sort words, so that shorter words come first
        Collections.sort(words);

        Collection<Glyph> wordGlyphs = new ArrayList<Glyph>();

        for (Word word : words) {
            ///logger.info("Word: '" + word.text + "'");

            // Isolate proper word glyph from its enclosed sections
            SortedSet<GlyphSection> sections = retrieveSectionsFrom(word.chars);

            if (!sections.isEmpty()) {
                Glyph wordGlyph = system.buildGlyph(sections);
                wordGlyph = system.addGlyph(wordGlyph);

                // Perhaps, we have a user-provided content which
                // might contain a word separator
                TextInfo ti = wordGlyph.getTextInfo();

                // TODO: Disabled for the time being
                if (false) {
                    String man = ti.getManualContent();

                    if ((man != null) && man.contains(" ")) {
                        logger.warning(
                            "We should split '" + man + "' in glyph#" +
                            wordGlyph.getId());

                        for (OcrChar ch : word.chars) {
                            logger.info(ch.toString());
                        }
                    }
                }

                wordGlyph.setShape(Shape.TEXT);

                // Build the TextInfo for this glyph
                ti.setOcrInfo(
                    this.ocrLanguage,
                    new OcrLine(getFontSize(), word.chars, word.text));
                ti.setSentence(this.sentence);
                ti.role = this.role;

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "LyricsItem \"" + word.text + "\" " + wordGlyph);
                }

                wordGlyphs.add(wordGlyph);
            } else {
                logger.warning(
                    "Text Glyph#" + glyph.getId() +
                    " has no section for word '" + word.text +
                    "' beginning at " + word.chars.get(0).getBox());
            }
        }

        return wordGlyphs;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{TextInfo");

        if (fontSize != null) {
            sb.append(" fontSize:")
              .append(fontSize);
        }

        if (manualContent != null) {
            sb.append(" manual:")
              .append("\"")
              .append(manualContent)
              .append("\"");
        }

        if (getOcrContent() != null) {
            sb.append(" ocr(")
              .append(ocrLanguage)
              .append("):")
              .append("\"")
              .append(getOcrContent())
              .append("\"");
        }

        if (pseudoContent != null) {
            sb.append(" pseudo:")
              .append("\"")
              .append(pseudoContent)
              .append("\"");
        }

        sb.append("}");

        return sb.toString();
    }

    //----------------------//
    // retrieveSectionsFrom //
    //----------------------//
    /**
     * Retrieve the glyph sections that correspond to the collection of OCR
     * char descriptors
     * @param chars the char descriptors for each word character
     * @return the set of word-enclosed sections
     */
    SortedSet<GlyphSection> retrieveSectionsFrom (List<OcrChar> chars)
    {
        SortedSet<GlyphSection> sections = new TreeSet<GlyphSection>();

        for (OcrChar charDesc : chars) {
            Rectangle charBox = charDesc.getBox();

            for (GlyphSection section : glyph.getMembers()) {
                // Do we intersect a section not (yet) assigned?
                if (charBox.intersects(section.getContourBox()) &&
                    (section.getGlyph() == this.glyph)) {
                    sections.add(section);
                }
            }
        }

        return sections;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Integer basicFontSize = new Constant.Integer(
            "points",
            10,
            "Standard font point size for texts");
        Constant.String  basicFontName = new Constant.String(
            "Serif", //"Sans Serif",
            "Standard font name for texts");
        Constant.Ratio   minExtensionAspect = new Constant.Ratio(
            10d,
            "Minimum width/height ratio for an extension character");
    }

    //-----------//
    // MyScanner //
    //-----------//
    /**
     * A specific scanner to scan content, since we need to know the current
     * position within the lineDesc, to infer proper word location.
     */
    private static class MyScanner
    {
        //~ Instance fields ----------------------------------------------------

        /** Precise description of each (non blank) character */
        private List<OcrChar> chars;

        /** Current position in the sequence of chars */
        private int pos = -1;

        /** Position of first char of current word */
        private int wordStart = 0;

        /** Position of last char of current word */
        private int wordStop = 0;

        /** Current word */
        private String currentWord;

        //~ Constructors -------------------------------------------------------

        public MyScanner (List<OcrChar> chars)
        {
            this.chars = chars;
        }

        //~ Methods ------------------------------------------------------------

        public int getWordStart ()
        {
            return wordStart;
        }

        public int getWordStop ()
        {
            return wordStop;
        }

        public boolean hasNext ()
        {
            if (currentWord == null) {
                currentWord = getNextWord();
            }

            return currentWord != null;
        }

        public String next ()
        {
            String tempWord = currentWord;
            currentWord = null;

            return tempWord;
        }

        private String getNextWord ()
        {
            StringBuilder word = new StringBuilder();

            for (pos = pos + 1; pos < chars.size(); pos++) {
                OcrChar charDesc = chars.get(pos);

                // White space
                if (charDesc.hasSpacesBefore()) {
                    if (word.length() > 0) {
                        pos--;

                        return word.toString();
                    }
                }

                String str = charDesc.content;

                // Special characters (returned as stand-alone words)
                if (str.equals(EXTENSION_STRING) ||
                    str.equals(ELISION_STRING) ||
                    str.equals(HYPHEN_STRING)) {
                    if (word.length() > 0) {
                        pos--;
                    } else {
                        wordStart = pos;
                        wordStop = pos;
                        word.append(str);
                    }

                    return word.toString();
                } else {
                    // Standard word content
                    if (word.length() == 0) {
                        wordStart = pos;
                    }

                    wordStop = pos;

                    word.append(str);
                }
            }

            // We have reached the end
            if (word.length() > 0) {
                return word.toString();
            } else {
                return null;
            }
        }
    }

    //------//
    // Word //
    //------//
    private static class Word
        implements Comparable<Word>
    {
        //~ Instance fields ----------------------------------------------------

        String        text; // String content
        List<OcrChar> chars; // OCR chars descriptors

        //~ Constructors -------------------------------------------------------

        public Word (String        text,
                     List<OcrChar> chars)
        {
            this.text = text;
            this.chars = chars;
        }

        //~ Methods ------------------------------------------------------------

        // Order by ascending text length
        public int compareTo (Word other)
        {
            if (this == other) {
                return 0;
            }

            if (this.text.length() <= other.text.length()) {
                return -1;
            }

            return +1;
        }
    }
}
