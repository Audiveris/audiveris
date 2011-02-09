//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t I n f o                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphSection;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.text.TextRole.RoleInfo;

import omr.lag.HorizontalOrientation;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.entity.Text.CreatorText.CreatorType;

import omr.sheet.SystemInfo;

import omr.util.ClassUtil;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>TextInfo</code> handles the textual aspects of a glyph.
 *
 * <p>It handles several text contents, by decreasing priority: <ol>
 * <li>manual content (entered manually by the user)</li>
 * <li>ocr content (as computed by the OCR engine)</li>
 * <li>pseudo content, meant to be used as a placeholder based on text type</li>
 * </ol>
 *
 * <p>The {@link #getContent} method returns the manual content if any, otherwise
 * the ocr content. Access to the pseudo content is done only through the {@link
 * #getPseudoContent} method.</p>
 *
 * <p>The font size is taken from OCR if available, otherwise it is computed
 * from physical characteristics.</p>
 *
 * @author Hervé Bitteur
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
    public Float getFontSize ()
    {
        if (fontSize == null) {
            //            if ((ocrLine != null) && (ocrLine.isFontSizeValid())) {
            //                fontSize = ocrLine.fontSize;
            //            } else {
            fontSize = TextFont.computeFontSize(
                getContent(),
                glyph.getContourBox().width);
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

        fontSize = null;
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
                if (getTextRole() != null) {
                    final int textHeight = sentence.getTextHeight();

                    if (textHeight > 0) {
                        double width = glyph.getContourBox().width;
                        int    nbChar = (int) Math.rint(width / textHeight);

                        pseudoContent = getTextRole()
                                            .getStringHolder(nbChar);
                    } else {
                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "glyph#" + glyph.getId() +
                                " text with no height");
                        }

                        return null;
                    }
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
                RoleInfo info = TextRole.guessRole(glyph, sentence.getSystem());
                setTextRole(info.role);

                // Additional info for creator
                if (role == TextRole.Creator) {
                    setCreatorType(info.creatorType);
                }
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

    //----------------------//
    // retrieveSectionsFrom //
    //----------------------//
    /**
     * Retrieve the glyph sections that correspond to the collection of OCR
     * char descriptors
     * @param chars the char descriptors for each word character
     * @return the set of word-enclosed sections
     */
    public SortedSet<GlyphSection> retrieveSectionsFrom (List<OcrChar> chars)
    {
        SortedSet<GlyphSection> sections = new TreeSet<GlyphSection>();

        for (OcrChar charDesc : chars) {
            Rectangle charBox = charDesc.getBox();

            for (GlyphSection section : glyph.getMembers()) {
                // Do we intersect a section not (yet) assigned?
                if (charBox.intersects(section.getContourBox())) {
                    sections.add(section);
                }
            }
        }

        return sections;
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
            String msg = "Trying to split glyph#" + glyph.getId() +
                         " with no OCR value";
            sentence.getSystemPart()
                    .addError(glyph, msg);

            return null;
        }

        // Parse the content string, to extract words
        List<OcrChar> glyphChars = ocrLine.getChars();
        WordScanner   scanner = new OcrScanner(glyphChars);
        List<Word>    words = new ArrayList<Word>();

        while (scanner.hasNext()) {
            String        wordText = scanner.next();
            List<OcrChar> wordChars = glyphChars.subList(
                scanner.getWordStart(),
                scanner.getWordStop() + 1);
            words.add(new Word(wordText, wordChars));
        }

        // Further split words if needed, and assign a glyph to each word
        assignWordGlyphs(words);

        // Return the collection of glyphs
        Collection<Glyph> wordGlyphs = new ArrayList<Glyph>();

        for (Word word : words) {
            if (word.glyph != null) {
                wordGlyphs.add(word.glyph);
            }

            if (logger.isFineEnabled()) {
                logger.fine(word.toString());
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

    //-------------//
    // isSeparator //
    //-------------//
    private static boolean isSeparator (String str)
    {
        return str.equals(EXTENSION_STRING) || str.equals(ELISION_STRING) ||
               str.equals(HYPHEN_STRING);
    }

    //------------------//
    // assignWordGlyphs //
    //------------------//
    /**
     * Assign a glyph to each word, while further splitting words if they
     * contain a space or other separator.
     * @param words the initial collection of words. The collection may get
     * modified, because of addition of new (sub)words and removal of words
     * getting split. At the end, each word of the collection should have a
     * glyph assigned.
     */
    private void assignWordGlyphs (List<Word> words)
    {
        SystemInfo system = sentence.getSystem();

        while (true) {
            // Sort words, so that shorter words come first
            Collections.sort(words);

            Collection<Word> toAdd = new ArrayList<Word>();
            Collection<Word> toRemove = new ArrayList<Word>();

            WordLoop: 
            for (Word word : words) {
                ///logger.info("Word: '" + word.text + "'");
                if (word.glyph == null) {
                    // Isolate proper word glyph from its enclosed sections
                    SortedSet<GlyphSection> sections = retrieveSectionsFrom(
                        word.chars);

                    if (!sections.isEmpty()) {
                        word.glyph = system.addGlyph(
                            system.buildGlyph(sections));

                        // Perhaps, we have a user-provided content which
                        // might contain a word separator
                        TextInfo ti = word.glyph.getTextInfo();
                        String   man = ti.getManualContent();

                        if (((man != null) && (man.length() > 1)) &&
                            (man.contains(" ") || man.contains(ELISION_STRING) ||
                            man.contains(EXTENSION_STRING) ||
                            man.contains(HYPHEN_STRING))) {
                            toRemove.add(word);
                            toAdd.addAll(manualSplit(word.glyph, word.chars));

                            // Reset sections->glyph link
                            for (GlyphSection section : sections) {
                                section.setGlyph(this.glyph);
                            }

                            break WordLoop; // Immediately
                        } else {
                            word.glyph.setShape(Shape.TEXT);

                            // Build the TextInfo for this glyph
                            ti.setOcrInfo(
                                this.ocrLanguage,
                                new OcrLine(
                                    getFontSize(),
                                    word.chars,
                                    word.text));
                            ti.setSentence(this.sentence);
                            ti.role = this.role;

                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    "LyricsItem \"" + word.text + "\" " +
                                    word.glyph);
                            }
                        }
                    } else {
                        system.getScoreSystem()
                              .addError(
                            glyph,
                            "No section for word '" + word.text + "'");
                    }
                }
            }

            if (!toRemove.isEmpty()) {
                words.removeAll(toRemove);
                words.addAll(toAdd);
            } else {
                return; // Normal exit
            }
        }
    }

    //-------------//
    // manualSplit //
    //-------------//
    /**
     * Further split the provided text glyph, based on its manual content
     * @param glyph the provided glyph
     * @param chars the underlying OCR chars
     * @return the sequence of (sub)words
     */
    private List<Word> manualSplit (Glyph         glyph,
                                    List<OcrChar> chars)
    {
        final List<Word> words = new ArrayList<Word>();
        final String     man = glyph.getTextInfo()
                                    .getManualContent();

        if (logger.isFineEnabled()) {
            logger.fine(
                "Forced word split of '" + man + "' in glyph#" + glyph.getId());

            for (OcrChar ch : chars) {
                logger.fine(ch.toString());
            }
        }

        final WordScanner scanner = new ManScanner(chars, man);

        while (scanner.hasNext()) {
            // The difficulty is to determine a precise cut for ocr chars
            String        wordText = scanner.next();
            List<OcrChar> wordChars = chars.subList(
                scanner.getWordStart(),
                scanner.getWordStop() + 1);

            Word          word = new Word(wordText, wordChars);

            if (logger.isFineEnabled()) {
                logger.fine(word.toString());
            }

            words.add(word);
        }

        return words;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Ratio minExtensionAspect = new Constant.Ratio(
            10d,
            "Minimum width/height ratio for an extension character");
    }

    //-------------//
    // WordScanner //
    //-------------//
    /**
     * An abstract scanner to retrieve words.
     */
    private abstract static class WordScanner
    {
        //~ Instance fields ----------------------------------------------------

        /** Precise description of each (non blank) character */
        protected List<OcrChar> chars;

        /** Position in the sequence of chars */
        protected int pos = -1;

        /** Current word and its parameters*/
        private String currentWord = null;
        private int    currentWordStart = 0;
        private int    currentWordStop = 0;

        /** Next word and its parameters */
        private String nextWord = null;
        protected int  nextWordStart = -1;
        protected int  nextWordStop = -1;

        //~ Constructors -------------------------------------------------------

        public WordScanner (List<OcrChar> chars)
        {
            this.chars = chars;
        }

        //~ Methods ------------------------------------------------------------

        /** Return the start position of the current word (as returned by next) */
        public int getWordStart ()
        {
            return currentWordStart;
        }

        /** Return the stop position of the current word (as returned by next) */
        public int getWordStop ()
        {
            return currentWordStop;
        }

        /** Tell whether there is a next word */
        public boolean hasNext ()
        {
            return nextWord != null;
        }

        /** Make the next word current, and return it */
        public String next ()
        {
            currentWord = nextWord;
            currentWordStart = nextWordStart;
            currentWordStop = nextWordStop;

            /** Look ahead */
            nextWord = getNextWord();

            return currentWord;
        }

        protected abstract String getNextWord ();

        protected void lookAhead ()
        {
            nextWord = getNextWord();
        }
    }

    //------------//
    // ManScanner //
    //------------//
    /**
     * A specific scanner using manual text content
     */
    private static class ManScanner
        extends WordScanner
    {
        //~ Instance fields ----------------------------------------------------

        /** The string content ant related parameters */
        private final String content;
        private final int    length;
        private final int    charNb;
        private final double ratio;

        /** The current index in the content string */
        private int index = -1;

        //~ Constructors -------------------------------------------------------

        public ManScanner (List<OcrChar> chars,
                           String        content)
        {
            super(chars);
            this.content = content;
            length = content.length();
            charNb = chars.size();
            ratio = charNb / (double) length;

            lookAhead();
        }

        //~ Methods ------------------------------------------------------------

        protected String getNextWord ()
        {
            StringBuilder word = new StringBuilder();

            for (index = index + 1; index < content.length(); index++) {
                pos = (int) Math.rint(index * ratio);

                String str = content.substring(index, index + 1);

                // White space?
                if (str.equals(" ")) {
                    if (word.length() > 0) {
                        return word.toString();
                    }
                }

                // Special characters? (to be returned as stand-alone words)
                if (isSeparator(str)) {
                    if (word.length() > 0) {
                    } else {
                        nextWordStart = nextWordStop + 1; // First unused pos
                        nextWordStop = pos;
                        word.append(str);
                    }

                    return word.toString();
                } else {
                    // Standard word content
                    if (word.length() == 0) { // Start of a word?
                        nextWordStart = nextWordStop + 1; // First unused pos
                    }

                    nextWordStop = pos;
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

    //------------//
    // OcrScanner //
    //------------//
    /**
     * A specific scanner to scan content, since we need to know the current
     * position within the lineDesc, to infer proper word location.
     */
    private static class OcrScanner
        extends WordScanner
    {
        //~ Constructors -------------------------------------------------------

        public OcrScanner (List<OcrChar> chars)
        {
            super(chars);

            lookAhead();
        }

        //~ Methods ------------------------------------------------------------

        protected String getNextWord ()
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
                if (isSeparator(str)) {
                    if (word.length() > 0) {
                        pos--;
                    } else {
                        nextWordStart = pos;
                        nextWordStop = pos;
                        word.append(str);
                    }

                    return word.toString();
                } else {
                    // Standard word content
                    if (word.length() == 0) {
                        nextWordStart = pos;
                    }

                    nextWordStop = pos;
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

        final String        text; // String content
        final List<OcrChar> chars; // OCR chars descriptors
        Glyph               glyph; // Underlying glyph

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

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{Word");
            sb.append(" text:")
              .append(text);

            if (glyph != null) {
                sb.append(" glyph#")
                  .append(glyph.getId());
            }

            for (OcrChar ch : chars) {
                sb.append("\n")
                  .append(ch);
            }

            sb.append("\n}");

            return sb.toString();
        }
    }
}
