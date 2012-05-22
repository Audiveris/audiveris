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
package omr.glyph.text;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.BasicFacet;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphContent;
import omr.glyph.text.TextRole.RoleInfo;

import omr.lag.BasicRoi;
import omr.lag.Section;

import omr.log.Logger;

import omr.run.Orientation;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.Text.CreatorText.CreatorType;

import omr.sheet.SystemInfo;

import omr.ui.symbol.TextFont;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class {@code BasicContent} handles the textual aspects of a glyph.
 *
 * <p>It handles several text contents, by decreasing priority: <ol>
 * <li>manual content (entered manually by the user)</li>
 * <li>ocr content (as computed by the OCR engine)</li>
 * <li>pseudo content, meant to be used as a placeholder based on text type</li>
 * </ol>
 *
 * <p>The {@link #getTextValue} method returns the manual content if any,
 * otherwise the ocr content. Access to the pseudo content is done only through
 * the {@link #getPseudoValue} method.</p>
 *
 * <p>The font size is taken from OCR if available, otherwise it is computed
 * from physical characteristics.</p>
 *
 * @author Hervé Bitteur
 */
public class BasicContent
        extends BasicFacet
        implements GlyphContent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BasicContent.class);

    //~ Instance fields --------------------------------------------------------
    /** Related text area parameters. */
    private TextArea textArea;

    /** Manual content if any. */
    private String manualValue;

    /** OCR data about this line. lang -> ocrLines */
    private final Map<String, List<OcrLine>> ocrLines = new TreeMap<>();

    /** Language used for OCR. */
    private String ocrLanguage;

    /** Role of this text item. */
    private TextRole role;

    /** Dummy text content as placeholder, if any, depending on role. */
    private String pseudoValue;

    /** Creator type. (relevant only if role == Creator) */
    private CreatorType creatorType;

    /** Font size. */
    private Float fontSize;

    //~ Constructors -----------------------------------------------------------
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
    //------//
    // dump //
    //------//
    /**
     * Write detailed text information on the standard output.
     */
    @Override
    public void dump ()
    {
        for (Map.Entry<String, List<OcrLine>> entry : ocrLines.entrySet()) {
            StringBuilder sb = new StringBuilder();

            for (OcrLine line : entry.getValue()) {
                sb.append("   (").append(entry.getKey()).append(") \"").append(
                        line.value).append("\"");
                System.out.println(sb.toString());
            }
        }
    }

    //----------------//
    // getCreatorType //
    //----------------//
    @Override
    public CreatorType getCreatorType ()
    {
        return creatorType;
    }

    //-------------//
    // getFontSize //
    //-------------//
    @Override
    public Float getFontSize ()
    {
        if (fontSize == null) {
            //            if ((ocrLine != null) && (ocrLine.isFontSizeValid())) {
            //                fontSize = ocrLine.fontSize;
            //            } else {
            String value = getTextValue();

            if (value != null) {
                fontSize = TextFont.computeFontSize(
                        value,
                        glyph.getBounds().width);
            }
        }

        return fontSize;
    }

    //------------------//
    // getManualContent //
    //------------------//
    @Override
    public String getManualValue ()
    {
        return manualValue;
    }

    //-----------------------//
    // getMinExtensionAspect //
    //-----------------------//
    public static double getMinExtensionAspect ()
    {
        return constants.minExtensionAspect.getValue();
    }

    //----------------//
    // getOcrLanguage //
    //----------------//
    @Override
    public String getOcrLanguage ()
    {
        return ocrLanguage;
    }

    //------------//
    // getOcrLine //
    //------------//
    public OcrLine getOcrLine ()
    {
        if (ocrLanguage == null) {
            return null;
        }

        List<OcrLine> lines = getOcrLines(ocrLanguage);

        if ((lines == null) || lines.isEmpty() || (lines.size() > 1)) {
            return null;
        }

        return lines.get(0);
    }

    //-------------//
    // getOcrLines //
    //-------------//
    @Override
    public List<OcrLine> getOcrLines (String language)
    {
        return ocrLines.get(language);
    }

    //-------------//
    // getOcrValue //
    //-------------//
    @Override
    public String getOcrValue ()
    {
        OcrLine ocrLine = getOcrLine();

        if (ocrLine != null) {
            return ocrLine.value;
        } else {
            return null;
        }
    }

    //------------------//
    // getPseudoContent //
    //------------------//
    @Override
    public String getPseudoValue ()
    {
        if (pseudoValue == null) {
            if (getTextRole() != null) {
                final int textHeight = getTextHeight();

                if (textHeight > 0) {
                    double width = glyph.getBounds().width;
                    int nbChar = (int) Math.rint(width / textHeight);

                    pseudoValue = getTextRole().getStringHolder(nbChar);
                } else {
                    logger.fine("{0} text with no height", glyph.idString());

                    return null;
                }
            }
        }

        return pseudoValue;
    }

    //-------------//
    // getTextArea //
    //-------------//
    @Override
    public TextArea getTextArea ()
    {
        if (textArea == null) {
            textArea = new TextArea(
                    glyph.getSystem(),
                    null,
                    new BasicRoi(glyph.getBounds()),
                    Orientation.HORIZONTAL);
        }

        return textArea;
    }

    //---------------//
    // getTextHeight //
    //---------------//
    @Override
    public int getTextHeight ()
    {
        return getTextArea().getBaseline() - getTextArea().getMedianLine();
    }

    //-------------//
    // getTextRole //
    //-------------//
    @Override
    public TextRole getTextRole ()
    {
        if (role == null) {
            SystemInfo system = glyph.getSystem();

            if (system != null) {
                RoleInfo info = TextRole.guessRole(glyph, system);
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
    @Override
    public PixelPoint getTextStart ()
    {
        return new PixelPoint(glyph.getBounds().x, getTextArea().getBaseline());
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
            return getOcrValue();
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

    //-----------//
    // isElision //
    //-----------//
    @Override
    public boolean isElision ()
    {
        return getTextValue().equals(ELISION_STRING);
    }

    //-------------//
    // isExtension //
    //-------------//
    @Override
    public boolean isExtension ()
    {
        return getTextValue().equals(EXTENSION_STRING);
    }

    //----------//
    // isHyphen //
    //----------//
    @Override
    public boolean isHyphen ()
    {
        return getTextValue().equals(HYPHEN_STRING);
    }

    //-------------//
    // isSeparator //
    //-------------//
    public static boolean isSeparator (String str)
    {
        return str.equals(EXTENSION_STRING) || str.equals(ELISION_STRING)
                || str.equals(HYPHEN_STRING);
    }

    //------------------//
    // retrieveOcrLines //
    //------------------//
    @Override
    public List<OcrLine> retrieveOcrLines (String language)
    {
        final String label = "s" + glyph.getSystem().getId() + "-g" + glyph.
                getId();

        final List<OcrLine> lines = Language.getOcr().recognize(
                glyph.getImage(),
                language,
                label);

        // Convert from glyph-based coordinates to absolute coordinates
        if (lines != null) {
            PixelRectangle box = glyph.getBounds();

            for (OcrLine ol : lines) {
                ol.translate(box.x, box.y);

                ///logger.info("OCR " + glyph.idString() + " '" + ol.value + "'");
            }
        }

        if (lines != null) {
            // Remember OCR results
            setOcrLines(language, lines);

            return lines;
        } else {
            return Collections.emptyList();
        }
    }

    //------------------//
    // retrieveSections //
    //------------------//
    @Override
    public SortedSet<Section> retrieveSections (List<OcrChar> chars)
    {
        SortedSet<Section> sections = new TreeSet<>();
        PixelRectangle glyphBox = glyph.getBounds();

        for (OcrChar charDesc : chars) {
            /*
             * Nota: in Tesseract 2.04, ordinates of char boxes are not always
             * reliable, so use only abscissae for basic checks.
             */
            Rectangle charBox = charDesc.getBox();
            charBox.y = glyphBox.y;
            charBox.height = glyphBox.height;

            for (Section section : glyph.getMembers()) {
                if (!section.isProcessed()) {
                    // Do we intersect a section not (yet) assigned?
                    if (charBox.intersects(section.getBounds())) {
                        sections.add(section);
                        section.setProcessed(true);
                    }
                }
            }
        }

        return sections;
    }

    //--------------------//
    // retrieveWordGlyphs //
    //--------------------//
    @Override
    public List<Glyph> retrieveWordGlyphs ()
    {
        OcrLine ocrLine = getOcrLine();

        if (ocrLine == null) {
            String msg = "Trying to split " + glyph.idString()
                    + " with no OCR data";
            glyph.getSystem().getScoreSystem().addError(glyph, msg);

            return null;
        }

        // Parse the content string, to extract words
        List<OcrChar> glyphChars = ocrLine.getChars();
        WordScanner scanner = new OcrScanner(glyphChars);
        List<Word> words = new ArrayList<>();

        while (scanner.hasNext()) {
            String wordText = scanner.next();
            List<OcrChar> wordChars = glyphChars.subList(
                    scanner.getWordStart(),
                    scanner.getWordStop() + 1);
            words.add(new Word(wordText, wordChars));
        }

        // Further split words if needed, and assign a glyph to each word
        assignWordGlyphs(words);

        // Return the collection of glyphs
        List<Glyph> wordGlyphs = new ArrayList<>();

        for (Word word : words) {
            if (word.glyph != null) {
                wordGlyphs.add(word.glyph);
            }

            logger.fine(word.toString());
        }

        return wordGlyphs;
    }

    //----------------//
    // setCreatorType //
    //----------------//
    @Override
    public void setCreatorType (CreatorType creatorType)
    {
        this.creatorType = creatorType;
    }

    //------------------//
    // setManualContent //
    //------------------//
    @Override
    public void setManualValue (String manualValue)
    {
        this.manualValue = manualValue;

        fontSize = null;
    }

    //-------------//
    // setOcrLines //
    //-------------//
    @Override
    public void setOcrLines (String ocrLanguage,
                             List<OcrLine> ocrLines)
    {
        this.ocrLines.put(ocrLanguage, ocrLines);

        // Consider this is the current language for this glyph
        this.ocrLanguage = ocrLanguage;

        fontSize = null;
    }

    //-------------//
    // setTextRole //
    //-------------//
    @Override
    public void setTextRole (TextRole type)
    {
        this.role = type;

        pseudoValue = null;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Content");

        if (fontSize != null) {
            sb.append(" fontSize:").append(fontSize);
        }

        if (manualValue != null) {
            sb.append(" manual:").append("\"").append(manualValue).append("\"");
        }

        if (getOcrValue() != null) {
            sb.append(" ocr(").append(ocrLanguage).append("):").append("\"").
                    append(getOcrValue()).append("\"");
        }

        if (pseudoValue != null) {
            sb.append(" pseudo:").append("\"").append(pseudoValue).append("\"");
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
            sb.append(" \"").append(value).append("\"");
        }

        return sb.toString();
    }

    //------------------//
    // assignWordGlyphs //
    //------------------//
    /**
     * Assign a glyph to each word, while further splitting words if
     * they contain a space or other separator.
     *
     * @param words the initial collection of words. The collection may get
     * modified, because of addition of new (sub)words and removal of words
     * getting split. At the end, each word of the collection should have a
     * glyph assigned.
     */
    private void assignWordGlyphs (List<Word> words)
    {
        SystemInfo system = glyph.getSystem();

        for (Section section : glyph.getMembers()) {
            section.setProcessed(false);
        }

        while (true) {
            // Sort words, so that shorter words come first
            Collections.sort(words);

            Collection<Word> toAdd = new ArrayList<>();
            Collection<Word> toRemove = new ArrayList<>();

            WordLoop:
            for (Word word : words) {
                ///logger.info("Word: '" + word.text + "'");
                if (word.glyph == null) {
                    // Isolate proper word glyph from its enclosed sections
                    SortedSet<Section> sections = retrieveSections(
                            word.getChars());

                    if (!sections.isEmpty()) {
                        word.glyph = system.addGlyph(
                                system.buildGlyph(sections));

                        // Perhaps, we have a user-provided content which
                        // might contain a word separator
                        String man = word.glyph.getManualValue();

                        if (((man != null) && (man.length() > 1))
                                && (man.contains(" ") || man.contains(
                                    ELISION_STRING)
                                    || man.contains(EXTENSION_STRING)
                                    || man.contains(HYPHEN_STRING))) {
                            toRemove.add(word);
                            toAdd.addAll(
                                    manualSplit(word.glyph, word.getChars()));

                            // Reset sections->glyph link
                            for (Section section : sections) {
                                section.setGlyph(this.glyph);
                            }

                            break WordLoop; // Immediately
                        } else {
                            word.glyph.setShape(Shape.TEXT);

                            // Build the BasicContent for this word glyph
                            word.glyph.setOcrLines(
                                    this.ocrLanguage,
                                    Arrays.asList(
                                    new OcrLine(
                                    getFontSize(),
                                    word.getChars(),
                                    word.getText())));
                            word.glyph.setTextRole(this.role);

                            logger.fine("LyricsItem \"{0}\" {1}",
                                        new Object[]{word.getText(), word.glyph});
                        }
                    } else {
                        system.getScoreSystem().addError(
                                glyph,
                                "No section for word '" + word.getText() + "'");
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
     * Further split the provided text glyph, based on its manual
     * content.
     *
     * @param glyph the provided glyph
     * @param chars the underlying OCR chars
     * @return the sequence of (sub)words
     */
    private List<Word> manualSplit (Glyph glyph,
                                    List<OcrChar> chars)
    {
        final List<Word> words = new ArrayList<>();
        final String man = glyph.getManualValue();

        if (logger.isFineEnabled()) {
            logger.fine("Manual split of ''{0}'' in {1}", new Object[]{man,
                                                                       glyph.
                        idString()});

            for (OcrChar ch : chars) {
                logger.fine(ch.toString());
            }
        }

        final WordScanner scanner = new ManualScanner(chars, man);

        while (scanner.hasNext()) {
            // The difficulty is to determine a precise cut for ocr chars
            String wordText = scanner.next();
            List<OcrChar> wordChars = chars.subList(
                    scanner.getWordStart(),
                    scanner.getWordStop() + 1);

            Word word = new Word(wordText, wordChars);

            logger.fine(word.toString());

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
}
