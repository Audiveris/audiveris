//----------------------------------------------------------------------------//
//                                                                            //
//                           T e x t B u i l d e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.math.LineUtil;

import omr.score.entity.Page;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.text.tesseract.TesseractOCR;

import omr.util.GeoUtil;
import omr.util.LiveParam;
import omr.util.WrappedBoolean;
import omr.util.XmlUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Class {@code TextBuilder} provide features to check, build and
 * reorganize text items, including interacting with the OCR engine.
 *
 * @author Hervé Bitteur
 */
public class TextBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters. */
    private static final Constants constants = new Constants();

    /** Usual logger utility. */
    private static final Logger logger = LoggerFactory.getLogger(TextBuilder.class);

    /** The related OCR. */
    private static final OCR ocr = TesseractOCR.getInstance();

    /** Abnormal characters. */
    private static final char[] ABNORMAL_CHARS = new char[]{'\\'};

    /** Regexp for abnormal words. */
    private static final Pattern ABNORMAL_WORDS = getAbnormalWords();

    //~ Instance fields --------------------------------------------------------
    //
    /** Related system. */
    private final SystemInfo system;

    /** Scale-dependent parameters. */
    private final Parameters params;

    //~ Constructors -----------------------------------------------------------
    //
    //-------------//
    // TextBuilder //
    //-------------//
    /**
     * Creates a new TextBuilder object.
     *
     * @param system the dedicated system
     */
    public TextBuilder (SystemInfo system)
    {
        this.system = system;

        params = new Parameters(system.getSheet().getScale());
    }

    //~ Methods ----------------------------------------------------------------
    //
    //--------//
    // getOcr //
    //--------//
    /**
     * Report the related OCR engine, if one is available.
     *
     * @return the available OCR engine, or null
     */
    public static OCR getOcr ()
    {
        return ocr;
    }

    //----------------//
    // isMainlyItalic //
    //----------------//
    /**
     * Check whether the (majority of) line is in italic font.
     *
     * @param line the line to check
     * @return true if mainly italics
     */
    public boolean isMainlyItalic (TextLine line)
    {
        int reliableWords = 0;
        int italicWords = 0;

        for (TextWord word : line.getWords()) {
            if (word.getConfidence() >= constants.minConfidence.getValue()
                && word.getLength() > 1) {
                reliableWords++;
                if (word.getFontInfo().isItalic) {
                    italicWords++;
                }
            }
        }

        // Check for majority among reliable words
        if (reliableWords != 0) {
            return italicWords * 2 >= reliableWords;
        } else {
            return false;
        }
    }

    //---------//
    // isValid //
    //---------//
    /**
     * Check the ocr line.
     *
     * @param textLine the ocr output
     * @return true if valid, false otherwise
     */
    public boolean isValid (TextLine textLine)
    {
        // Check confidence
        Integer conf = textLine.getConfidence();
        int minConf = constants.minConfidence.getValue();
        if (conf == null || conf < minConf) {
            logger.debug("      Too low confidence {} vs {} for {}",
                    conf, minConf, textLine);
            return false;
        }

        // Check font size
        if (!isValidFontSize(textLine)) {
            return false;
        }

        // Check ratio of invalid words in the line
        int invalidCount = 0;
        for (TextWord word : textLine.getWords()) {
            if (!isValid(word)) {
                invalidCount++;
            }
        }
        double invalidRatio = (double) invalidCount / textLine.getWords().size();

        return invalidRatio <= constants.maxInvalidRatio.getValue();
    }

    //---------//
    // isValid //
    //---------//
    public boolean isValid (TextWord word)
    {
        final String value = word.getValue();

        // Check for abnormal characters
        for (char ch : ABNORMAL_CHARS) {
            if (value.indexOf(ch) != -1) {
                logger.debug("Abnormal char {} in {}", ch, word);
                return false;
            }
        }

        // Check for invalid XML characters
        WrappedBoolean stripped = new WrappedBoolean(false);
        XmlUtil.stripNonValidXMLCharacters(value, stripped);

        if (stripped.isSet()) {
            logger.warn("Invalid XML chars in {}", word);
            return false;
        }

        // Check for invalid word values
        if (ABNORMAL_WORDS != null) {
            Matcher matcher = ABNORMAL_WORDS.matcher(value);
            if (matcher.matches()) {
                logger.debug("Abnormal word value {}", word);
                return false;
            }
        }

//        Rectangle box = word.getBounds();
//        String str = word.getValue();
//        Font font = new TextFont(word.getFontInfo());
//        TextLayout layout = new TextLayout(str, font, frc);
//        Rectangle2D rect = layout.getBounds();
//        double xRatio = box.width / rect.getWidth();
//        double yRatio = box.height / rect.getHeight();
//        double aRatio = yRatio / xRatio;
////        logger.debug("{} xRatio:{} yRatio:{} aRatio:{}", textLine,
////                    (float) xRatio, (float) yRatio, aRatio);
//
//        // Sign of something wrong
//        if ((aRatio < constants.minAspectRatio.getValue())
//                || (aRatio > constants.maxAspectRatio.getValue())) {
//            logger.debug("      Invalid ratio {} vs [{}-{}] for {}",
//                        aRatio,
//                        constants.minAspectRatio.getValue(),
//                        constants.maxAspectRatio.getValue(), word);
//            return false;
//        }
//
        return true;
    }

    //-----------------//
    // isValidFontSize //
    //-----------------//
    public boolean isValidFontSize (TextLine textLine)
    {
        for (TextWord word : textLine.getWords()) {
            FontInfo fontInfo = word.getFontInfo();

            if (fontInfo.pointsize > params.maxFontSize) {
                logger.debug("Too big font {} vs {} on {}",
                        fontInfo.pointsize, params.maxFontSize, textLine);
                return false;
            }
        }

        return true;
    }

    //-----------//
    // mapGlyphs //
    //-----------//
    /**
     * By searching through the provided sections, build one glyph for
     * each word and one sentence for each line.
     *
     * @param lines       the lines (and contained words) to be mapped
     * @param allSections the population of sections to browse
     * @param language    the OCR language specification
     */
    public void mapGlyphs (List<TextLine> lines,
                           Collection<Section> allSections,
                           String language)
    {
        if (logger.isDebugEnabled()) {
            logger.info("{} mapGlyphs", system.idString());
        }

        // To make sure that the same section is not assigned to several words
        for (Section section : allSections) {
            section.setProcessed(false);
        }

        for (TextLine line : lines) {
            logger.debug("   mapping {}", line);
            // Browse all words, starting by shorter ones
            List<TextWord> sortedWords = new ArrayList<>(line.getWords());
            Collections.sort(sortedWords, TextWord.bySize);
            List<TextWord> toRemove = new ArrayList<>();

            for (TextWord word : sortedWords) {
                // Isolate proper word glyph from its enclosed sections
                SortedSet<Section> wordSections = retrieveSections(
                        word.getChars(),
                        allSections);

                if (!wordSections.isEmpty()) {
                    Glyph wordGlyph = system.addGlyph(system.buildGlyph(
                            wordSections));

                    // Link TextWord -> Glyph
                    word.setGlyph(wordGlyph);
                    if (word.isVip()) {
                        line.setVip();
                    }

                    if (word.isVip() || logger.isDebugEnabled()) {
                        logger.info("      mapped {}", word);
                    }

                    // Link Glyph -> TextWord
                    wordGlyph.setTextWord(language, word);
                } else {
                    logger.debug("No section found for {}", word);
                    toRemove.add(word);
                }
            }

            // Purge words if any
            line.removeWords(toRemove);

            // Assign proper shape to each word glyph
            for (TextWord word : line.getWords()) {
                Glyph g = word.getGlyph();

                if (g != null) {
                    boolean many = word.getValue().length() > 1;
                    g.setShape(many ? Shape.TEXT : Shape.CHARACTER);
                }
            }

            logger.debug("  mapGlyphs adding {}", line);
            system.getSentences().add(line);
        }

        // Purge duplications, if any, in system sentences
        purgeSentences();
    }

    //----------------//
    // purgeSentences //
    //----------------//
    /**
     * Remove words whose glyphs no longer point back to them,
     * and finally remove sentences which have no word left.
     */
    public void purgeSentences ()
    {
        for (Iterator<TextLine> itLine = system.getSentences().iterator();
                itLine.hasNext();) {
            TextLine line = itLine.next();

            List<TextWord> toRemove = new ArrayList<>();
            for (TextWord word : line.getWords()) {
                Glyph glyph = word.getGlyph();

                if (glyph == null || glyph.getTextWord() != word) {
                    logger.debug("{} purging old {}", system.idString(), word);
                    toRemove.add(word);
                }
            }

            if (!toRemove.isEmpty()) {
                line.removeWords(toRemove);
            }

            if (line.getWords().isEmpty()) {
                logger.debug("{} purging empty {}", system.idString(), line);
                itLine.remove();
            }
        }
    }

    //---------------//
    // dumpSentences //
    //---------------//
    /**
     * Debug method to list current system sentences.
     */
    public void dumpSentences (String title)
    {
        Set<TextLine> sentences = system.getSentences();
        logger.info("{} {} sentences: {}",
                title, system.idString(), sentences.size());

        for (TextLine sentence : sentences) {
            logger.info("   {}", sentence);
        }
    }

    //------------//
    // mergeLines //
    //------------//
    /**
     * Merge a sequence of TextLine instances into a single instance.
     *
     * @param lines the lines to merge
     * @return a single TextLine
     */
    public TextLine mergeLines (List<TextLine> lines)
    {
        List<TextWord> words = new ArrayList<>();

        for (TextLine line : lines) {
            line.setProcessed(true);
            words.addAll(line.getWords());
        }

        Collections.sort(words, TextWord.byAbscissa);

        return new TextLine(system, words);
    }

    //----------------//
    // recomposeLines //
    //----------------//
    /**
     * Check and modify the provided raw TextLine instances for correct
     * composition.
     *
     * <ul>
     * <li>Except for lyrics line, a too large inter-word gap triggers a line
     * split</li>
     * <li>A too small inter-word gap triggers a word merge</li>
     * <li>For lyrics, separate lines with similar ordinate trigger a line
     * merge</li>
     * <li>For lyrics, a separation character triggers a word split into
     * syllables</li>
     * </ul>
     *
     * @param oldLines the lines to process
     * @return the new sequence of recomposed lines
     */
    public List<TextLine> recomposeLines (Collection<TextLine> oldLines)
    {
        if (logger.isDebugEnabled()) {
            logger.info("{} recomposeLines", system.idString());
        }

        // Separate lyrics and standard populations
        List<TextLine> standards = new ArrayList<>();
        List<TextLine> lyrics = new ArrayList<>();
        separatePopulations(oldLines, standards, lyrics);

        lyrics = purgeInvalidLines(lyrics);
        lyrics = mergeLyricsLines(lyrics);

        if (logger.isDebugEnabled()) {
            logger.info("{} splitWords for lyrics", system.idString());
        }
        for (TextLine line : lyrics) {
            splitWords(line.getWords(), line);
        }

        // Reject invalid standard lines
        standards = splitStandardLines(standards);
        standards = purgeInvalidLines(standards);

        // Recut standard lines
        standards = mergeStandardLines(standards);
        standards = splitStandardLines(standards);

        // Recut standard words
        for (TextLine line : standards) {
            recutStandardWords(line);
        }

        // Gather and sort all lines (standard & lyrics)
        List<TextLine> allLines = new ArrayList<>();
        allLines.addAll(lyrics);
        allLines.addAll(standards);
        Collections.sort(allLines, TextLine.byOrdinate);

        return allLines;
    }

    //---------------------//
    // separatePopulations //
    //---------------------//
    /**
     * Separate the provided lines into lyrics lines and standard
     * (non-lyrics) lines.
     *
     * @param lines     the global population
     * @param standards the non-lyrics population
     * @param lyrics    the lyrics population
     */
    private void separatePopulations (Collection<TextLine> lines,
                                      List<TextLine> standards,
                                      List<TextLine> lyrics)
    {
        for (TextLine line : lines) {
            if (line.getValue().trim().isEmpty()) {
                logger.debug("Empty line {}", line);
                line.setProcessed(true);
            } else {
                line.setProcessed(false);
                if (line.isLyrics()) {
                    lyrics.add(line);
                } else {
                    line.setRole(null); // To force role recomputing later
                    standards.add(line);
                }

                if (logger.isDebugEnabled()) {
                    logger.info("   Initial {}", line);
                    for (TextWord word : line.getWords()) {
                        logger.debug("      {}", word);
                    }
                }
            }
        }
    }

    //-----------------//
    // retrieveOcrLine //
    //-----------------//
    /**
     * Launch the OCR on the provided glyph, to retrieve the TextLine
     * instance(s) this glyph represents.
     *
     * @param glyph    the glyph to OCR
     * @param language the probable language
     * @return a list, not null but perhaps empty, of TextLine instances with
     *         absolute coordinates.
     */
    public List<TextLine> retrieveOcrLine (Glyph glyph,
                                           String language)
    {
        final String label = "s" + glyph.getSystem()
                .getId() + "-g" + glyph.getId();

        return getOcr()
                .recognize(glyph.getImage(),
                glyph.getBounds().getLocation(),
                language,
                OCR.LayoutMode.SINGLE_BLOCK,
                system,
                label);
    }

    //------------------//
    // retrieveSections //
    //------------------//
    /**
     * Report the set of sections that relate to the provided collection
     * of TextChar instances.
     *
     * @param chars       the OCR char descriptors
     * @param allSections the candidate sections
     * @return the corresponding set of sections
     */
    public SortedSet<Section> retrieveSections (List<TextChar> chars,
                                                Collection<Section> allSections)
    {
        SortedSet<Section> set = new TreeSet<>();

        for (TextChar charDesc : chars) {
            Rectangle charBox = charDesc.getBounds();

            for (Section section : allSections) {
                // Do we contain a section not (yet) assigned?
                if (!section.isProcessed()
                    && charBox.contains(section.getBounds())) {
                    set.add(section);
                    section.setProcessed(true);
                }
            }
        }

        return set;
    }

    //-------------//
    // mergeChunks //
    //-------------//
    /**
     * Merge line chunks horizontally
     *
     * @param chunks the (sub) lines to merge
     * @return the resulting merged line
     */
    private TextLine mergeChunks (List<TextLine> chunks)
    {
        TextLine line;
        Collections.sort(chunks, TextLine.byAbscissa);

        if (chunks.size() == 1) {
            line = chunks.get(0);
        } else {
            if (logger.isDebugEnabled()) {
                for (TextLine chunk : chunks) {
                    logger.debug("   chunk {}", chunk);
                }
            }
            line = mergeLines(chunks);
            if (line.isVip() || logger.isDebugEnabled()) {
                logger.info("      merge result {}", line);
            }
        }

        return line;
    }

    //------------------//
    // mergeLyricsLines //
    //------------------//
    /**
     * For lyrics, separate lines with similar ordinate trigger a
     * line merge.
     *
     * @param oldLyrics collection of lyrics chunks
     * @return resulting lyrics lines
     */
    private List<TextLine> mergeLyricsLines (List<TextLine> oldLyrics)
    {
        if (logger.isDebugEnabled()) {
            logger.info("{} mergeLyricsLines", system.idString());
        }
        List<TextLine> newLyrics = new ArrayList<>();
        Collections.sort(oldLyrics, TextLine.byOrdinate);

        List<TextLine> chunks = new ArrayList<>();
        double lastY = 0;

        for (TextLine line : oldLyrics) {
            double y = line.getDskOrigin().getY();

            if (chunks.isEmpty()) {
                chunks.add(line);
                lastY = y;
            } else if ((y - lastY) <= params.maxLyricsDy) {
                // Compatible line
                chunks.add(line);
                lastY = y;
            } else {
                // Non compatible line

                // Complete pending chunks, if any
                if (!chunks.isEmpty()) {
                    newLyrics.add(mergeChunks(chunks));
                }

                // Start a new collection of chunks
                chunks.clear();
                chunks.add(line);
                lastY = y;
            }
        }

        // Complete pending chunks, if any
        if (!chunks.isEmpty()) {
            newLyrics.add(mergeChunks(chunks));
        }

        return newLyrics;
    }

    //--------------------//
    // mergeStandardLines //
    //--------------------//
    /**
     * For standards, separate lines with similar ordinate and small
     * abscissa gap trigger a line merge.
     *
     * @param oldStandards collection of standard candidates
     * @return resulting standard lines
     */
    private List<TextLine> mergeStandardLines (List<TextLine> oldStandards)
    {
        if (logger.isDebugEnabled()) {
            logger.info("{} mergeStandardLines", system.idString());
        }
        Collections.sort(oldStandards, TextLine.byOrdinate);

        for (TextLine current : oldStandards) {
            current.setProcessed(false);
            TextLine candidate = current;

            CandidateLoop:
            while (true) {
                final Rectangle candidateBounds = getDeskewedCore(candidate);
                final Rectangle candidateFatBox = new Rectangle(candidateBounds);
                candidateFatBox.grow(getWordGap(candidate), params.maxLyricsDy);

                HeadsLoop:
                for (TextLine head : oldStandards) {
                    if (head == current) {
                        break CandidateLoop;
                    }
                    if (head != candidate && !head.isProcessed()) {
                        Rectangle headBounds = getDeskewedCore(head);
                        if (headBounds.intersects(candidateFatBox)) {
                            if (head.isChord()) {
                                // Check actual dx between head & candidate
                                int gap = GeoUtil.xGap(headBounds, candidateBounds);
                                if (gap <= params.maxChordDx) {
                                    continue;
                                }
                            }

                            if (candidate.isVip() || head.isVip()
                                || logger.isDebugEnabled()) {
                                logger.info("   merging {} into {}",
                                        candidate, head);
                            }

                            head.addWords(candidate.getWords());
                            candidate.setProcessed(true);
                            candidate = head;
                            break HeadsLoop;
                        }
                    }
                }

            }
        }

        // Remove unavailable lines
        List<TextLine> newStandards = new ArrayList<>();
        for (TextLine line : oldStandards) {
            if (!line.isProcessed()) {
                newStandards.add(line);
            }
        }

        return newStandards;
    }

    //-----------------//
    // getDeskewedCore //
    //-----------------//
    /**
     * Build a rectangle using deskewed baseline and min 1 pixel high.
     *
     * @param line the TextLine entity
     * @return the deskewed core
     */
    private Rectangle getDeskewedCore (TextLine line)
    {
        Point2D P1 = line.getDskOrigin();
        Point p1 = new Point((int) Math.rint(P1.getX()),
                (int) Math.rint(P1.getY()));
        Point2D P2 = system.getSkew().deskewed(line.getBaseline().getP2());
        Point p2 = new Point((int) Math.rint(P2.getX()),
                (int) Math.rint(P2.getY()));
        Rectangle rect = new Rectangle(p1);
        rect.add(p2);

        rect.height = Math.max(1, rect.height); // To allow containment test

        return rect;
    }

    //--------------------//
    // recutStandardWords //
    //--------------------//
    /**
     * Recut (merge & split) words within a standard TextLine.
     *
     * @param line the line to recut words
     */
    public void recutStandardWords (TextLine line)
    {
        mergeStandardWords(line);
        splitWords(line.getWords(), line);
    }

    //--------------------//
    // mergeStandardWords //
    //--------------------//
    private void mergeStandardWords (TextLine line)
    {
        logger.debug("   mergeLineWords for {}", line);

        List<TextWord> toAdd = new ArrayList<>();
        List<TextWord> toRemove = new ArrayList<>();
        TextWord prevWord = null;

        for (TextWord word : line.getWords()) {
            // Look for tiny inter-word gap
            if (prevWord != null) {
                Rectangle prevBounds = prevWord.getBounds();
                int prevStop = prevBounds.x + prevBounds.width;
                int gap = word.getBounds().x - prevStop;
                logger.debug("      gap {} vs {} to {}",
                        gap, params.minWordDx, word);

                if (gap < params.minWordDx) {
                    toRemove.add(prevWord);
                    toRemove.add(word);
                    TextWord bigWord = TextWord.mergeOf(prevWord, word);
                    logger.debug("         merged into {}", bigWord);
                    toAdd.add(bigWord);
                    word = bigWord;
                }
            }

            prevWord = word;
        }

        if (!toAdd.isEmpty()) {
            // No use to add & remove the same words
            List<TextWord> common = new ArrayList<>(toAdd);
            common.retainAll(toRemove);
            toAdd.removeAll(common);
            toRemove.removeAll(common);

            // Perform the modifications
            line.addWords(toAdd);
            line.removeWords(toRemove);
        }
    }

    //------------//
    // splitWords //
    //------------//
    /**
     * Check each word in the provided collection and split it in place
     * according to separating characters ('-' etc).
     * The line sequence of words may get modified, because of the addition of
     * new (sub)words and the removal of words that got split.
     * The line sequence of words remains sorted.
     *
     * @param words the collection of words to check and split
     * @param line  the containing TextLine instance
     */
    public void splitWords (Collection<TextWord> words,
                            TextLine line)
    {
        // To avoid concurrent modification errors
        Collection<TextWord> toAdd = new ArrayList<>();
        Collection<TextWord> toRemove = new ArrayList<>();

        for (TextWord word : words) {
            List<TextWord> subWords = null; // Results of split
            Glyph wordGlyph = word.getGlyph();

            if (wordGlyph != null) {
                if (!wordGlyph.getTextValue().equals(word.getInternalValue())) {
                    // A manual text modification has occurred
                    // Check for a separator in the new manual value
                    if (!word.getChars().isEmpty()) {
                        logger.debug("Manual modif for {}",
                                wordGlyph.idString());
                        subWords = getSubWords(word,
                                line,
                                new WordScanner.ManualScanner(
                                wordGlyph.getTextValue(),
                                line.isLyrics(),
                                word.getChars()));

                        // If no subdivision was made, allocate a new TextWord
                        // just to match the new manual value
                        if (subWords.isEmpty()) {
                            TextWord newWord = new TextWord(
                                    word.getBaseline(),
                                    wordGlyph.getTextValue(),
                                    word.getFontInfo(),
                                    word.getConfidence(),
                                    word.getChars(),
                                    line);
                            newWord.setGlyph(wordGlyph);
                            subWords.add(newWord);
                            wordGlyph.setTextWord(wordGlyph.getOcrLanguage(),
                                    newWord);
                        }
                    }
                }
            } else {
                subWords = getSubWords(word,
                        line,
                        new WordScanner.OcrScanner(
                        word.getValue(),
                        line.isLyrics(),
                        word.getChars()));
            }

            if (subWords != null && !subWords.isEmpty()) {
                toRemove.add(word);
                toAdd.addAll(subWords);
            }
        }

        // Now perform modification on the line sequence of words, if so needed
        if (!toRemove.isEmpty()) {
            line.addWords(toAdd);
            line.removeWords(toRemove);
        }
    }

    //--------------------//
    // splitStandardLines //
    //--------------------//
    /**
     * For standard (non-lyrics) lines, a really wide gap between two
     * words indicate the need to split the line in two.
     *
     * @param oldStandards collection of initial standard lines
     * @return resulting standard lines
     */
    private List<TextLine> splitStandardLines (List<TextLine> oldStandards)
    {
        if (logger.isDebugEnabled()) {
            logger.info("{} splitStandardLines", system.idString());
        }
        Collections.sort(oldStandards, TextLine.byOrdinate);

        List<TextLine> newStandards = new ArrayList<>();

        for (TextLine line : oldStandards) {
            if (line.isVip() || logger.isDebugEnabled()) {
                logger.info("split checking {}", line);
            }

            final int maxAbscissaGap = getWordGap(line);
            List<TextWord> words = line.getWords();
            boolean splitting = true;

            while (splitting) {
                splitting = false;

                // Look for huge inter-word gap
                Integer stop = null;

                for (TextWord word : words) {
                    Rectangle bounds = word.getBounds();

                    if (stop != null) {
                        int gap = bounds.x - stop;

                        if (gap > maxAbscissaGap) {
                            int splitPos = words.indexOf(word);
                            List<TextWord> lineWords = words.subList(0,
                                    splitPos);
                            TextLine newLine = new TextLine(system, lineWords);
                            newLine.setRole(line.getRole());
                            if (line.isVip() || logger.isDebugEnabled()) {
                                logger.info("      subLine {}", newLine);
                            }
                            newStandards.add(newLine);

                            words = words.subList(splitPos, words.size());
                            splitting = true;

                            break;
                        }
                    }

                    stop = bounds.x + bounds.width;
                }
            }

            // Pending words?
            if (words.size() < line.getWords().size()) {
                TextLine newLine = new TextLine(system, words);
                newLine.setRole(line.getRole());
                if (line.isVip() || logger.isDebugEnabled()) {
                    logger.info("      subLine {}", newLine);
                }
                newStandards.add(newLine);
            } else {
                newStandards.add(line);
            }
        }

        return newStandards;
    }

    //-------------//
    // getSubWords //
    //-------------//
    /**
     * Report the potential subwords of the provided word, based on the
     * provided scanner to adapt to Ocr or Manual values.
     *
     * @param word    the word to process
     * @param line    the containing line
     * @param scanner how to scan the word
     * @return the sequence of created (sub)words, perhaps empty
     */
    private List<TextWord> getSubWords (TextWord word,
                                        TextLine line,
                                        WordScanner scanner)
    {
        final List<TextWord> subWords = new ArrayList<>();
        final int contentLength = word.getValue().length();

        while (scanner.hasNext()) {
            String subValue = scanner.next();

            if (subValue.length() < contentLength) {
                // We have a real subword
                List<TextChar> wordChars = scanner.getWordChars();

                // Compute (sub) baseline parameters
                Line2D base = word.getBaseline();
                int x1 = wordChars.get(0).getBounds().x;
                Point2D p1 = LineUtil.intersection(
                        base.getP1(), base.getP2(),
                        new Point2D.Double(x1, 0), new Point2D.Double(x1, 100));

                Rectangle box = wordChars.get(wordChars.size() - 1).getBounds();
                int x2 = box.x + box.width - 1;
                Point2D p2 = LineUtil.intersection(
                        base.getP1(), base.getP2(),
                        new Point2D.Double(x2, 0), new Point2D.Double(x2, 100));
                Line2D subBase = new Line2D.Double(p1, p2);

                // Allocate sub word
                TextWord newWord = new TextWord(
                        subBase,
                        subValue,
                        word.getFontInfo(),
                        word.getConfidence(),
                        wordChars,
                        line);

                logger.debug("      subWord ''{}'' from ''{}''",
                        newWord.getValue(), word.getValue());
                subWords.add(newWord);
            }
        }

        return subWords;
    }

    //-------------------//
    // purgeInvalidLines //
    //-------------------//
    /**
     * Purge lines whose validity is not confirmed.
     *
     * @param lines the lines to purge
     * @return the remaining lines
     */
    private List<TextLine> purgeInvalidLines (List<TextLine> lines)
    {
        if (logger.isDebugEnabled()) {
            logger.info("{} purgeInvalidLines", system.idString());
        }
        List<TextLine> newLines = new ArrayList<>();

        for (TextLine line : lines) {
            logger.debug("   checking {}", line);
            if (isValid(line)) {
                newLines.add(line);
            } else {
                line.setProcessed(true);
                if (logger.isDebugEnabled()) {
                    for (TextWord word : line.getWords()) {
                        logger.debug("      {}", word);
                    }
                }
            }
        }

        return newLines;
    }

    //---------------------//
    // switchLanguageTexts //
    //---------------------//
    /**
     * Use a new language to update existing words when a better OCR
     * result has been found.
     */
    public void switchLanguageTexts ()
    {
        final Page page = system.getSheet().getPage();
        final LiveParam<String> textParam = page.getTextParam();
        final String language = textParam.getTarget();
        if (logger.isDebugEnabled()) {
            logger.info("{} switchLanguageTexts lan:{}",
                    system.idString(), language);
        }
        textParam.setActual(language);

        for (TextLine oldLine : new ArrayList<>(system.getSentences())) {
            // Launch OCR on the whole line image
            List<Glyph> glyphs = oldLine.getWordGlyphs();
            Glyph compound = glyphs.size() == 1
                    ? glyphs.get(0)
                    : system.
                    registerGlyph(system.buildTransientCompound(glyphs));

            List<TextLine> lines = retrieveOcrLine(compound, language);
            if (lines == null || lines.size() != 1) {
                logger.debug("{} No valid replacement for {}",
                        system.idString(), oldLine);
            } else {
                TextLine newLine = lines.get(0);
                recutStandardWords(newLine);

                if (logger.isDebugEnabled()) {
                    logger.info("{} refreshing {} by {}",
                            system.idString(), oldLine, newLine);
                    oldLine.dump();
                    newLine.dump();
                }
                List<TextWord> toRemove = new ArrayList<>();
                List<TextWord> toAdd = new ArrayList<>();
                for (TextWord oldWord : oldLine.getWords()) {
                    TextWord newWord = findNewWord(oldWord, newLine);
                    if (newWord != null) {
                        if (newWord.getConfidence() >= oldWord.getConfidence()) {
                            newWord.setGlyph(oldWord.getGlyph());
                            newWord.getGlyph().setTextWord(language, newWord);
                            toRemove.add(oldWord);
                            toAdd.add(newWord);
                        }
                    } else {
                        logger.debug("{} no word for {} in {}",
                                system.idString(), oldWord, newLine);
                    }
                }

                // Update words in place
                if (!toAdd.isEmpty()) {
                    oldLine.addWords(toAdd);
                    oldLine.removeWords(toRemove);
                }
            }
        }
    }

    //-------------//
    // findNewWord //
    //-------------//
    /**
     * Try to find in the provided new line the word that corresponds
     * to the provided old word.
     *
     * @param oldWord old word
     * @param newLine the line to search
     * @return the corresponding new word, or null if not found
     */
    private TextWord findNewWord (TextWord oldWord,
                                  TextLine newLine)
    {
        Rectangle oldBounds = oldWord.getBounds();

        for (TextWord word : newLine.getWords()) {
            if (word.getBounds().equals(oldBounds)) {
                return word;
            }
        }

        return null;
    }

    //------------------//
    // getAbnormalWords //
    //------------------//
    /**
     * Compile the provided regexp to detect abnormal words
     *
     * @return the pattern for abnormal words, if successful
     */
    private static Pattern getAbnormalWords ()
    {
        try {
            return Pattern.compile(constants.abnormalWordRegexp.getValue());
        } catch (PatternSyntaxException pse) {
            logger.warn("Error in regexp for abnormal words", pse);
            return null;
        }
    }

    //------------//
    // getWordGap //
    //------------//
    /**
     * Report the maximum abscissa gap between two consecutive words
     * of the provided line.
     * We use a smaller horizontal gap between chord names than between words
     * of ordinary standard lines.
     *
     * @param line the line provided
     * @return the maximum abscissa gap to use
     */
    private int getWordGap (TextLine line)
    {
        return line.isChord() ? params.maxChordDx : params.maxWordDx;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.String abnormalWordRegexp = new Constant.String(
                "^[\\.°>]$",
                "Regular expression to detect abnormal words");

        Constant.Integer minConfidence = new Constant.Integer(
                "0..100",
                70,
                "Minimum confidence for OCR validity");

        Constant.Integer maxCharCountForAspectCheck = new Constant.Integer(
                "CharCount",
                3,
                "Maximum character count to apply aspect check");

        Constant.Ratio minAspectRatio = new Constant.Ratio(
                0.35,
                "Minimum ratio between ocr aspect and glyph aspect");

        Constant.Ratio maxAspectRatio = new Constant.Ratio(
                2.0,
                "Maximum ratio between ocr aspect and glyph aspect");

        Scale.Fraction maxFontSize = new Scale.Fraction(
                7.0,
                "Max font size wrt interline");

        Scale.Fraction maxLyricsDy = new Scale.Fraction(
                1.0,
                "Max vertical gap between two lyrics chunks");

        Scale.Fraction maxWordDx = new Scale.Fraction(
                5.0,
                "Max horizontal gap between two non-lyrics words");

        Scale.Fraction minWordDx = new Scale.Fraction(
                0.25,
                "Min horizontal gap between two non-lyrics words");

        Scale.Fraction maxChordDx = new Scale.Fraction(
                1.0,
                "Max horizontal gap between two chord words");

        Constant.Ratio maxInvalidRatio = new Constant.Ratio(
                0.33,
                "Maximum ratio of invalid words in a line");

    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        final int maxFontSize;

        final int maxLyricsDy;

        final int maxWordDx;

        final int minWordDx;

        final int maxChordDx;

        //~ Constructors -------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxFontSize = scale.toPixels(constants.maxFontSize);
            maxLyricsDy = scale.toPixels(constants.maxLyricsDy);
            maxWordDx = scale.toPixels(constants.maxWordDx);
            minWordDx = scale.toPixels(constants.minWordDx);
            maxChordDx = scale.toPixels(constants.maxChordDx);
        }
    }
}
