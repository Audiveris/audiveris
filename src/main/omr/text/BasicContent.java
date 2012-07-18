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

import omr.lag.BasicRoi;
import omr.lag.Section;

import omr.log.Logger;

import omr.run.Orientation;

import omr.score.common.PixelPoint;

import omr.text.OCR.LayoutMode;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code BasicContent} handles the textual aspects of a glyph.
 *
 * <p>It handles several text values, by decreasing priority:</p>
 *
 * <ol>
 * <li>manual value (entered manually by the user)</li>
 * <li>ocr value (as computed by the OCR engine)</li>
 * <li>pseudo value, meant to be used as a placeholder based on text role when
 * no other source (manual or OCR) is available</li>
 * </ol>
 *
 * <p>The {@link #getTextValue} method returns the manual value if any,
 * otherwise the ocr value. Access to the pseudo value is done only through the
 * {@link #getPseudoValue} method.</p>
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

    /** Usual logger utility. */
    private static final Logger logger = Logger.getLogger(BasicContent.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Related text area parameters. */
    private TextArea textArea;

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

    //----------------//
    // getPseudovalue //
    //----------------//
    @Override
    public String getPseudoValue ()
    {
        if (getTextRole() != null) {
            final int textHeight = getTextHeight();

            if (textHeight > 0) {
                double width = glyph.getBounds().width;
                int nbChar = (int) Math.rint(width / textHeight);

                return getTextRole().role.getStringHolder(nbChar);
            }
        }

        return null;
    }

    //-------------//
    // getTextArea //
    //-------------//
    @Override
    public TextArea getTextArea ()
    {
        if (textArea == null) {
            textArea = new TextArea(glyph.getSystem(), null,
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

    //------------------//
    // retrieveOcrLines //
    //------------------//
    @Override
    public List<TextLine> retrieveOcrLines (String language)
    {
        final String label = "s" + glyph.getSystem()
                .getId() + "-g" + glyph.getId();

        return TextBuilder.getOcr()
                .recognize(glyph.getImage(),
                           glyph.getBounds().getLocation(),
                           language,
                           LayoutMode.SINGLE_BLOCK,
                           glyph.getSystem(),
                           label);
    }

    //------------------//
    // retrieveSections //
    //------------------//
    @Override
    public SortedSet<Section> retrieveSections (List<TextChar> chars)
    {
        SortedSet<Section> sections = new TreeSet<>();

        for (TextChar charDesc : chars) {
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

//    //-------------------//
//    // retrieveSubGlyphs //
//    //-------------------//
//    @Override
//    public List<Glyph> retrieveSubGlyphs (boolean bySyllable)
//    {
//        List<Glyph> wordGlyphs = new ArrayList<>();
//        TextLine textLine = getTextLine();
//
//        if (textLine == null) {
//            String msg = "Trying to split " + glyph.idString()
//                    + " with no OCR data";
//            glyph.getSystem()
//                    .getScoreSystem()
//                    .addError(glyph, msg);
//
//            return wordGlyphs; // Empty collection
//        }
//
//        // Further split words if needed
//        splitWords(textLine.getWords(), bySyllable);
//
//        // Assign a glyph to each word
//        assignAllWords(textLine.getWords());
//
//        // Return the collection of glyphs
//        for (TextWord word : textLine.getWords()) {
//            if (word.getGlyph() != null) {
//                wordGlyphs.add(word.getGlyph());
//            }
//        }
//
//        return wordGlyphs;
//    }
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
//    //----------------//
//    // assignAllWords //
//    //----------------//
//    /**
//     * Assign a glyph to each word.
//     *
//     * @param words the collection of words to assign At the end, each word
//     *              should have a glyph assigned.
//     */
//    private void assignAllWords (List<OcrWord> words)
//    {
//        TextBuilder builder = glyph.getSystem()
//                .getTextBuilder();
//        builder.mapGlyphsToWords(words, glyph.getMembers(), ocrLanguage);
//
//        for (TextWord word : words) {
//            Glyph wordGlyph = word.getGlyph();
//
//            if (wordGlyph != null) {
//                wordGlyph.setTextRole(glyph.getTextRole());
//            }
//        }
//    }
//    //---------------//
//    // splitWords //
//    //---------------//
//    /**
//     * Check each word in the sequence and split it in place according to
//     * separating characters ('-' etc). The collection of words may get
//     * modified, because of the addition of new (sub)words and the removal of
//     * words that got split.
//     *
//     * @param words      the collection of words to check and split
//     * @param bySyllable true for syllable, false for plain words.
//     */
//    private void splitWords (List<OcrWord> words,
//                                boolean bySyllable)
//    {
//        // To avoid concurrent modification errors
//        Collection<OcrWord> toAdd = new ArrayList<>();
//        Collection<OcrWord> toRemove = new ArrayList<>();
//
//        for (TextWord word : words) {
//            List<OcrWord> subWords; // Results of split
//            Glyph wordGlyph = word.getGlyph();
//
//            if (wordGlyph != null) {
//                if (wordGlyph.getTextValue().equals(word.getValue())) {
//                    continue;
//                } else {
//                    // A manual text modification has occurred
//                    // Check for a separator in the new manual value
//                    logger.fine("Manual modif for {0}", wordGlyph.idString());
//                    subWords = splitWord(word,
//                                         new WordScanner.ManualScanner(wordGlyph
//                            .getTextValue(),
//                                                                       bySyllable,
//                                                                       word.
//                            getChars()));
//
//                    // If no subdivision was made, allocate a new TextWord
//                    // to match the new manual value
//                    if (subWords.isEmpty()) {
//                        subWords.add(new TextWord(word.getBaseline(),
//                                                 wordGlyph.getTextValue(),
//                                                 word.getFontInfo(),
//                                                 word.getConfidence(),
//                                                 word.getChars()));
//                    }
//                }
//            } else {
//                subWords = splitWord(word,
//                                     new WordScanner.OcrScanner(word.getValue(),
//                                                                bySyllable,
//                                                                word.getChars()));
//            }
//
//            if (!subWords.isEmpty()) {
//                toRemove.add(word);
//                toAdd.addAll(subWords);
//            }
//        }
//
//        // Now perform modification on the list of words, if so needed
//        if (!toRemove.isEmpty()) {
//            words.removeAll(toRemove);
//            words.addAll(toAdd);
//        }
//    }
//    //-----------//
//    // splitWord //
//    //-----------//
//    /**
//     * Further split the provided word, based on the provided scanner to adapt
//     * to Ocr or Manual values.
//     *
//     * @param word    the word to split
//     * @param scanner how to scan the word
//     *
//     * @return the sequence of created (sub)words, if any
//     */
//    private List<OcrWord> splitWord (TextWord word,
//                                     WordScanner scanner)
//    {
//        final List<OcrWord> subWords = new ArrayList<>();
//        final int contentLength = word.getValue()
//                .length();
//
//        while (scanner.hasNext()) {
//            String wordValue = scanner.next();
//
//            if (wordValue.length() < contentLength) {
//                // We have a real subword
//                List<OcrChar> wordChars = scanner.getWordChars();
//                TextWord newWord = new TextWord(word.getBaseline(),
//                                              wordValue,
//                                              word.getFontInfo(),
//                                              word.getConfidence(),
//                                              wordChars);
//
//                logger.fine("subword ''{0}''", newWord.getValue());
//                subWords.add(newWord);
//            }
//        }
//
//        return subWords;
//    }
}
