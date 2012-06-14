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

import omr.glyph.Shape;
import omr.glyph.facets.BasicFacet;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphContent;
import omr.glyph.text.OCR.LayoutMode;
import omr.glyph.text.TextRole.RoleInfo;

import omr.lag.BasicRoi;
import omr.lag.Section;

import omr.log.Logger;

import omr.run.Orientation;

import omr.score.common.PixelPoint;
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
 * <p>It handles several text values, by decreasing priority: <ol>
 * <li>manual value (entered manually by the user)</li>
 * <li>ocr value (as computed by the OCR engine)</li>
 * <li>pseudo value, meant to be used as a placeholder based on text role
 * when no other source (manual or OCR) is available</li>
 * </ol>
 *
 * <p>The {@link #getTextValue} method returns the manual value if any,
 * otherwise the ocr value. Access to the pseudo value is done only through
 * the {@link #getPseudoValue} method.</p>
 *
 * <p>The font size is taken from OCR if available, otherwise it is computed
 * from physical characteristics. TODO: check this statement!</p>
 *
 * @author Hervé Bitteur
 */
public class BasicContent
        extends BasicFacet
        implements GlyphContent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BasicContent.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Related text area parameters. */
    private TextArea textArea;

    /** Manual value if any. */
    private String manualValue;

    /** OCR data about this line. lang -> ocrLines */
    private final Map<String, List<OcrLine>> ocrLines = new TreeMap<>();

    /** Language used for OCR. */
    private String ocrLanguage;

    /** Role of this text item. */
    private TextRole role;

    /** Dummy text value as placeholder, if any, depending on role. */
    private String pseudoValue;

    /** Creator type. (relevant only if role == Creator) */
    private CreatorType creatorType;

    /** Sentence, if any, this textual glyph belongs to. */
    private Sentence sentence;

    /** Font size. */
    private Float fontSize;

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
        for (Map.Entry<String, List<OcrLine>> entry : ocrLines.entrySet()) {
            StringBuilder sb = new StringBuilder();

            for (OcrLine line : entry.getValue()) {
                sb.append("   (").append(entry.getKey()).append(") \"").append(
                        line.getValue()).append("\"");
                System.out.println(sb.toString());
            }
        }

        if (manualValue != null) {
            System.out.println("   manual=\"" + manualValue + "\"");
        }

        if (sentence != null) {
            System.out.println("   sentence=" + sentence);
        }
    }

    //-------------//
    // getSentence //
    //-------------//
    @Override
    public Sentence getSentence ()
    {
        return sentence;
    }

    //-------------//
    // setSentence //
    //-------------//
    @Override
    public void setSentence (Sentence sentence)
    {
        this.sentence = sentence;
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
            String value = getTextValue();

            if (value != null) {
                fontSize = TextFont.computeFontSize(
                        value,
                        glyph.getBounds().width);
            }
        }

        return fontSize;
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

    //------------//
    // getOcrLine //
    //------------//
    @Override
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
            return ocrLine.getValue();
        } else {
            return null;
        }
    }

    //----------------//
    // getPseudovalue //
    //----------------//
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

    //------------------//
    // retrieveOcrLines //
    //------------------//
    @Override
    public List<OcrLine> retrieveOcrLines (String language)
    {
        final String label = "s" + glyph.getSystem().getId()
                + "-g" + glyph.getId();

        final List<OcrLine> lines = Language.getOcr().recognize(
                glyph.getImage(),
                glyph.getBounds().getLocation(),
                language,
                LayoutMode.SINGLE_BLOCK,
                label);

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

        for (OcrChar charDesc : chars) {
            Rectangle charBox = charDesc.getBounds();

            for (Section section : glyph.getMembers()) {
                // Do we intersect a section not (yet) assigned?
                if (!section.isProcessed()
                        && charBox.intersects(section.getBounds())) {
                    sections.add(section);
                    section.setProcessed(true);

                }
            }
        }

        return sections;
    }

    //--------------------//
    // retrieveSubGlyphs //
    //--------------------//
    @Override
    public List<Glyph> retrieveSubGlyphs (boolean bySyllable)
    {
        List<Glyph> wordGlyphs = new ArrayList<>();
        OcrLine ocrLine = getOcrLine();

        if (ocrLine == null) {
            String msg = "Trying to split " + glyph.idString()
                    + " with no OCR data";
            glyph.getSystem().getScoreSystem().addError(glyph, msg);

            return wordGlyphs; // Empty collection
        }

        // Further split words if needed, and assign a glyph to each word
        splitAllWords(ocrLine.getWords(), bySyllable);
        assignAllWords(ocrLine.getWords());

        // Return the collection of glyphs
        for (OcrWord word : ocrLine.getWords()) {
            if (word.getGlyph() != null) {
                wordGlyphs.add(word.getGlyph());
            }
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

    //----------------//
    // setManualvalue //
    //----------------//
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

    //~ Private Methods --------------------------------------------------------
    //
    //----------------//
    // assignAllWords //
    //----------------//
    /**
     * Assign a glyph to each word.
     *
     * @param words the collection of words to assign
     *              At the end, each word of the collection should have a glyph assigned.
     */
    private void assignAllWords (List<OcrWord> words)
    {
        SystemInfo system = glyph.getSystem();

        // To make sure that the same section is not assigned to several words
        for (Section section : glyph.getMembers()) {
            section.setProcessed(false);
        }

        // Browse all words, starting by shorter ones
        Collections.sort(words, OcrWord.sizeComparator);

        for (OcrWord word : words) {
            // Isolate proper word glyph from its enclosed sections
            SortedSet<Section> sections = retrieveSections(word.getChars());

            if (sections.isEmpty()) {
                system.getScoreSystem().addError(
                        glyph,
                        "No section for word '" + word.getValue() + "'");
                continue;
            }

            Glyph wordGlyph = system.addGlyph(system.buildGlyph(sections));
            word.setGlyph(wordGlyph);
            logger.fine("word {0} as ''{1}''",
                        wordGlyph.idString(), word.getValue());

            wordGlyph.setOcrLines(
                    this.ocrLanguage,
                    Arrays.asList(
                    new OcrLine(glyph.getBounds(),
                                word.getValue(),
                                Arrays.asList(word))));
            wordGlyph.setTextRole(glyph.getTextRole());
        }

        // Assign proper shape to each word glyph
        for (OcrWord word : words) {
            Glyph g = word.getGlyph();
            if (g != null) {
                boolean many = word.getValue().length() > 1;
                g.setShape(many ? Shape.TEXT : Shape.CHARACTER);

            }
        }

        // Sort words on abscissa
        Collections.sort(words, OcrWord.abscissaComparator);
    }

    //---------------//
    // splitAllWords //
    //---------------//
    /**
     * Check each word in the sequence and split it in place
     * according to separating characters ('-' etc).
     *
     * @param words      the collection of words to check and split
     * @param bySyllable true for syllable, false for plain words
     *                   The collection of words may get modified, because of the addition of new
     *                   (sub)words and the removal of words that got split.
     */
    private void splitAllWords (List<OcrWord> words,
                                boolean bySyllable)
    {
        // To avoid concurrent modification errors
        Collection<OcrWord> toAdd = new ArrayList<>();
        Collection<OcrWord> toRemove = new ArrayList<>();

        for (OcrWord word : words) {
            List<OcrWord> subWords; // Results of split
            Glyph wordGlyph = word.getGlyph();

            if (wordGlyph != null) {
                if (wordGlyph.getTextValue().equals(word.getValue())) {
                    continue;
                } else {
                    // A manual text modification has occurred
                    // Check for a separator in the new manual value
                    logger.fine("Manual modif for {0}", wordGlyph.idString());
                    subWords = splitWord(word,
                                         new WordScanner.ManualScanner(
                            wordGlyph.getTextValue(),
                            bySyllable,
                            word.getChars()));
                    // If no subdivision was made, allocate a new OcrWord
                    // to match the new manual value
                    if (subWords.isEmpty()) {
                        subWords.add(new OcrWord(
                                word.getBaseline(),
                                wordGlyph.getTextValue(),
                                word.getFontInfo(),
                                word.getChars()));
                    }
                }
            } else {
                subWords = splitWord(
                        word,
                        new WordScanner.OcrScanner(word.getValue(),
                                                   bySyllable,
                                                   word.getChars()));
            }

            if (!subWords.isEmpty()) {
                toRemove.add(word);
                toAdd.addAll(subWords);
            }
        }

        // Now perform modification on the list of words, if so needed
        if (!toRemove.isEmpty()) {
            words.removeAll(toRemove);
            words.addAll(toAdd);
        }
    }

    //-----------//
    // splitWord //
    //-----------//
    /**
     * Further split the provided word, based on the provided scanner
     * to adapt to Ocr or Manual values.
     *
     * @param word    the word to split
     * @param scanner how to scan the word
     * @return the sequence of created (sub)words, if any
     */
    private List<OcrWord> splitWord (OcrWord word,
                                     WordScanner scanner)
    {
        final List<OcrWord> subWords = new ArrayList<>();
        final int contentLength = word.getValue().length();

        while (scanner.hasNext()) {
            String wordValue = scanner.next();

            if (wordValue.length() < contentLength) {
                // We have a real subword
                List<OcrChar> wordChars = scanner.getWordChars();
                OcrWord newWord = new OcrWord(word.getBaseline(), wordValue,
                                              word.getFontInfo(), wordChars);

                logger.fine("subword ''{0}''", newWord.getValue());
                subWords.add(newWord);
            }
        }

        return subWords;
    }
}
