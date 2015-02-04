//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     T e x t B u i l d e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;
import omr.glyph.facets.Glyph;

import omr.lag.BasicLag;
import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.SectionFactory;

import omr.math.GeoUtil;
import omr.math.LineUtil;

import static omr.run.Orientation.VERTICAL;

import omr.sheet.Part;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;
import omr.sheet.SystemInfo;

import omr.sig.SIGraph;
import omr.sig.inter.Inter;
import omr.sig.inter.SentenceInter;

import static omr.text.TextRole.PartName;

import omr.text.tesseract.TesseractOCR;

import omr.ui.symbol.TextFont;

import omr.util.LiveParam;
import omr.util.Navigable;
import omr.util.StopWatch;
import omr.util.WrappedBoolean;
import omr.util.XmlUtil;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
import omr.sheet.Staff;

/**
 * Class {@code TextBuilder} works at system level, to provide features to check, build
 * and reorganize text items, including interacting with the OCR engine.
 *
 * @author Hervé Bitteur
 */
public class TextBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

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

    /** Needed for font size computation */
    protected static final FontRenderContext frc = new FontRenderContext(null, true, true);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related system. */
    @Navigable(false)
    private final SystemInfo system;

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Global skew. */
    private final Skew skew;

    /** Scale-dependent parameters. */
    private final Parameters params;

    //~ Constructors -------------------------------------------------------------------------------
    //
    //-------------//
    // TextBuilder //
    //-------------//
    /**
     * Creates a new TextBuilder object.
     *
     * @param system the related system
     */
    public TextBuilder (SystemInfo system)
    {
        this.system = system;

        sheet = system.getSheet();
        skew = sheet.getSkew();
        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // checkValidity //
    //---------------//
    /**
     * Check the OCR line, which may get some of its words removed.
     * <p>
     * First remove any really invalid word from the line.
     * Then use average of remaining words confidence value.
     * Use font size validity.
     * Use ratio of invalid words.
     *
     * @param line the OCR output
     * @return reason for invalidity if any, otherwise null
     */
    public String checkValidity (TextLine line)
    {
        {
            // Discard really invalid words
            final double lowConf = constants.lowConfidence.getValue();
            final List<TextWord> toRemove = new ArrayList<TextWord>();

            for (TextWord word : line.getWords()) {
                if (word.getConfidence() < lowConf) {
                    if (logger.isDebugEnabled()) {
                        logger.info("    low-conf word {} vs {}", word, lowConf);
                    }

                    toRemove.add(word);
                } else {
                    String reason = checkValidity(word);

                    if (reason != null) {
                        if (logger.isDebugEnabled()) {
                            logger.info("    invalid word {} {}", word, reason);
                        }

                        toRemove.add(word);
                    }
                }
            }

            if (!toRemove.isEmpty()) {
                line.removeWords(toRemove);
                checkRole(line);
            }
        }

        // Check global line confidence
        Double conf = line.getConfidence();
        double minConf = constants.minConfidence.getValue();

        if ((conf == null) || conf.isNaN() || (conf < minConf)) {
            return "low-confidence";
        }

        // Check font size
        if (!isValidFontSize(line)) {
            return "invalid-font-size";
        }

        //        // Check ratio of invalid words in the line
        //        int invalidCount = 0;
        //
        //        for (TextWord word : line.getWords()) {
        //            String reason = checkValidity(word);
        //
        //            if (reason != null) {
        //                invalidCount++;
        //            }
        //        }
        //
        //        double invalidRatio = (double) invalidCount / line.getWords().size();
        //
        //        if (invalidRatio > constants.maxInvalidRatio.getValue()) {
        //            return "many-invalid-words";
        //        }
        //
        return null; // OK
    }

    //---------------//
    // checkValidity //
    //---------------//
    /**
     * Check the provided OCR'ed word. (the word is not modified)
     *
     * @param word the word to check
     * @return reason for invalidity if any, otherwise null
     */
    public String checkValidity (TextWord word)
    {
        final String value = word.getValue();

        // Check for abnormal characters
        for (char ch : ABNORMAL_CHARS) {
            if (value.indexOf(ch) != -1) {
                logger.debug("Abnormal char {} in {}", ch, word);

                return "abnormal-chars";
            }
        }

        // Check for invalid XML characters
        WrappedBoolean stripped = new WrappedBoolean(false);
        XmlUtil.stripNonValidXMLCharacters(value, stripped);

        if (stripped.isSet()) {
            logger.debug("Invalid XML chars in {}", word);

            return "invalid-xml-chars";
        }

        // Check for invalid word values
        if (ABNORMAL_WORDS != null) {
            Matcher matcher = ABNORMAL_WORDS.matcher(value);

            if (matcher.matches()) {
                logger.debug("Abnormal word value {}", word);

                return "abnormal-word-value";
            }
        }

        // (Applicable only for non-tiny width or height)
        Rectangle box = word.getBounds();

        if ((box.width >= params.minWidthForCheck) && (box.height >= params.minHeightForCheck)) {
            // Check for non-consistent aspect
            String str = word.getValue();
            Font font = new TextFont(word.getFontInfo());
            TextLayout layout = new TextLayout(str, font, frc);
            Rectangle2D rect = layout.getBounds();
            double xRatio = box.width / rect.getWidth();
            double yRatio = box.height / rect.getHeight();
            double aRatio = yRatio / xRatio;

            if ((aRatio < params.minAspectRatio) || (aRatio > params.maxAspectRatio)) {
                logger.debug(
                        "   Invalid aspect {} vs [{}-{}] for {}",
                        aRatio,
                        params.minAspectRatio,
                        params.maxAspectRatio,
                        word);

                return "invalid aspect";
            }

            // Check size
            double boxDiag = Math.hypot(box.width, box.height);
            double rectDiag = Math.hypot(rect.getWidth(), rect.getHeight());
            double diagRatio = boxDiag / rectDiag;

            ///logger.info(String.format("aspect:%.2f diag:%.2f %s", aRatio, diagRatio, word));
            if ((diagRatio < params.minDiagRatio) || (diagRatio > params.maxDiagRatio)) {
                logger.debug(
                        "   Invalid diagonal {} vs [{}-{}] for {}",
                        diagRatio,
                        params.minDiagRatio,
                        params.maxDiagRatio,
                        word);

                return "invalid diagonal";
            }
        }

        // OK
        return null;
    }

    //---------------//
    // dumpSentences //
    //---------------//
    /**
     * Debug method to list current system sentences.
     *
     * @param title a title for the dump
     */
    public void dumpSentences (String title)
    {
        Set<TextLine> sentences = system.getSentences();
        logger.info("{} {} sentences: {}", title, sheet.getLogPrefix(), sentences.size());

        for (TextLine sentence : sentences) {
            logger.info("   {}", sentence);
        }
    }

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
    public static boolean isMainlyItalic (TextLine line)
    {
        int reliableWords = 0;
        int italicWords = 0;

        for (TextWord word : line.getWords()) {
            if ((word.getConfidence() >= constants.minConfidence.getValue())
                && (word.getLength() > 1)) {
                reliableWords++;

                if (word.getFontInfo().isItalic) {
                    italicWords++;
                }
            }
        }

        // Check for majority among reliable words
        if (reliableWords != 0) {
            return (italicWords * 2) >= reliableWords;
        } else {
            return false;
        }
    }

    //-----------------//
    // isValidFontSize //
    //-----------------//
    /**
     * Check whether all words in the provided line have a valid font size.
     *
     * @param line the OCR'ed line to check
     * @return true if valid
     */
    public boolean isValidFontSize (TextLine line)
    {
        for (TextWord word : line.getWords()) {
            FontInfo fontInfo = word.getFontInfo();

            if (fontInfo.pointsize > params.maxFontSize) {
                logger.debug(
                        "Too big font {} vs {} on {}",
                        fontInfo.pointsize,
                        params.maxFontSize,
                        line);

                return false;
            }
        }

        return true;
    }

    //-----------//
    // mapGlyphs //
    //-----------//
    /**
     * By searching through the provided sections, build one glyph for each word and
     * one sentence for each line.
     *
     * @param lines       the lines (and contained words) to be mapped
     * @param allSections the population of sections to browse
     * @param language    the OCR language specification
     */
    public void mapGlyphs (List<TextLine> lines,
                           Collection<Section> allSections,
                           String language)
    {
        logger.debug("mapGlyphs");

        GlyphNest nest = system.getSheet().getNest();

        // To make sure that the same section is not assigned to several words
        for (Section section : allSections) {
            section.setProcessed(false);
        }

        for (TextLine line : lines) {
            logger.debug("  mapping {}", line);

            // Browse all words, starting by shorter ones
            List<TextWord> sortedWords = new ArrayList<TextWord>(line.getWords());
            Collections.sort(sortedWords, TextWord.bySize);

            List<TextWord> toRemove = new ArrayList<TextWord>();

            for (TextWord word : sortedWords) {
                // Isolate proper word glyph from its enclosed sections
                Rectangle roi = word.getBounds();
                SortedSet<Section> wordSections = retrieveSections(word.getChars(), allSections);

                if (!wordSections.isEmpty()) {
                    Glyph wordGlyph = system.registerGlyph(
                            nest.buildGlyph(wordSections, GlyphLayer.DEFAULT, true, Glyph.Linking.LINK));

                    // Link TextWord -> Glyph
                    word.setGlyph(wordGlyph);

                    if (word.isVip()) {
                        line.setVip();
                    }

                    if (word.isVip()) {
                        logger.info("    mapped {}", word);
                    } else {
                        logger.debug("    mapped {}", word);
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

            //            // Assign proper shape to each word glyph
            //            for (TextWord word : line.getWords()) {
            //                Glyph g = word.getGlyph();
            //
            //                if (g != null) {
            //                    boolean many = word.getValue().length() > 1;
            //                    g.setShape(many ? Shape.TEXT : Shape.CHARACTER);
            //                }
            //            }
            //
            system.getSentences().add(line);
        }

        // Purge duplications, if any, in system sentences
        purgeSentences();
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
        List<TextWord> words = new ArrayList<TextWord>();

        for (TextLine line : lines) {
            line.setProcessed(true);
            words.addAll(line.getWords());
        }

        Collections.sort(words, TextWord.byAbscissa);

        return new TextLine(words);
    }

    //----------------//
    // purgeSentences //
    //----------------//
    /**
     * Remove words whose glyphs no longer point back to them,
     * and finally remove sentences which have no word left.
     * <p>
     * TODO: is this still useful?
     */
    public void purgeSentences ()
    {
        for (Iterator<TextLine> itLine = system.getSentences().iterator(); itLine.hasNext();) {
            TextLine line = itLine.next();
            List<TextWord> toRemove = new ArrayList<TextWord>();

            for (TextWord word : line.getWords()) {
                Glyph glyph = word.getGlyph();

                if ((glyph == null) || (glyph.getTextWord() != word)) {
                    logger.debug("{} purging old {}", sheet.getLogPrefix(), word);
                    toRemove.add(word);
                }
            }

            if (!toRemove.isEmpty()) {
                line.removeWords(toRemove);
            }

            if (line.getWords().isEmpty()) {
                logger.debug("{} purging empty {}", sheet.getLogPrefix(), line);
                itLine.remove();
            }
        }
    }

    //----------------//
    // recomposeLines //
    //----------------//
    /**
     * Check and modify the provided raw TextLine instances for correct composition.
     * <ul>
     * <li>Except for lyrics line, a too large inter-word gap triggers a line split</li>
     * <li>A too large inter-char gap triggers a word split</li>
     * <li>A too small inter-word gap triggers a word merge</li>
     * <li>For lyrics, separate lines with similar ordinate trigger a line merge</li>
     * <li>For lyrics, a separation character triggers a word split into syllables</li>
     * </ul>
     *
     * @param rawLines the lines to process
     * @return the sequence of re-composed lines
     */
    public List<TextLine> recomposeLines (Collection<TextLine> rawLines)
    {
        logger.debug("System#{} recomposeLines", system.getId());

        // Separate lyrics and standard lines, based on their roles
        List<TextLine> standards = new ArrayList<TextLine>();
        List<TextLine> lyrics = new ArrayList<TextLine>();
        separatePopulations(rawLines, standards, lyrics);

        // Process lyrics
        if (!lyrics.isEmpty()) {
            lyrics = purgeInvalidLines("lyrics", lyrics);
            lyrics = mergeLyricsLines(lyrics);

            logger.debug("splitWords for lyrics");

            for (TextLine line : lyrics) {
                splitWords(line.getWords(), line);
            }
        }

        // Process standards
        if (!standards.isEmpty()) {
            // Reject invalid standard lines
            standards = splitStandardLines(standards);
            standards = purgeInvalidLines("standards", standards);

            // Recut standard lines
            standards = mergeStandardLines(standards);
            standards = splitStandardLines(standards);

            // Recut standard words
            logger.debug("recutStandardWords");

            for (TextLine line : standards) {
                recutStandardWords(line);
            }
        }

        // Gather and sort all lines (standard & lyrics)
        List<TextLine> allLines = new ArrayList<TextLine>();
        allLines.addAll(lyrics);
        allLines.addAll(standards);
        Collections.sort(allLines, TextLine.byOrdinate(skew));

        return allLines;
    }

    //--------------------//
    // recutStandardWords //
    //--------------------//
    /**
     * Re-cut (merge & split) words within a standard TextLine.
     *
     * @param line the line to re-cut words
     */
    public void recutStandardWords (TextLine line)
    {
        mergeStandardWords(line);
        splitWords(line.getWords(), line);
    }

    //---------------//
    // retrieveLines //
    //---------------//
    /**
     * Retrieve the system-relevant lines, among all the lines OCR'ed at sheet level.
     *
     * @param buffer     the pixel buffer
     * @param sheetLines the sheet raw OCR lines
     */
    public void retrieveLines (ByteProcessor buffer,
                               List<TextLine> sheetLines)
    {
        StopWatch watch = new StopWatch("Texts retrieveLines system#" + system.getId());
        watch.start("Pickup system lines");

        List<TextLine> systemLines = new ArrayList<TextLine>();

        // We pick up the words that are contained by system area
        Area area = system.getArea();

        for (TextLine sheetLine : sheetLines) {
            TextLine line = new TextLine();

            for (TextWord sheetWord : sheetLine.getWords()) {
                if (area.contains(sheetWord.getBounds())) {
                    TextWord word = sheetWord.copy();
                    line.appendWord(word);
                }
            }

            if (!line.getWords().isEmpty()) {
                systemLines.add(line);
            }
        }

        Collections.sort(systemLines, TextLine.byOrdinate(skew));

        watch.start("recomposeLines");
        systemLines = recomposeLines(systemLines);

        // Retrieve candidate sections and map words to glyphs
        watch.start("getSections");

        List<Section> allSections = getSections(buffer, systemLines);
        watch.start("mapGlyphs");
        mapGlyphs(systemLines, allSections, sheet.getTextParam().getActual());

        // Allocate corresponding inters
        watch.start("createInters");

        SIGraph sig = system.getSig();

        for (TextLine line : system.getSentences()) {
            SentenceInter sentenceInter = SentenceInter.create(line);

            for (Inter wordInter : sentenceInter.getMembers()) {
                sig.addVertex(wordInter);
            }

            sig.addVertex(sentenceInter);
            assignSentence(sentenceInter);
        }

        if (constants.printWatch.isSet()) {
            watch.print();
        }

        if (logger.isDebugEnabled()) {
            dump("Retrieved lines", systemLines);
        }
    }

    //-----------------//
    // retrieveOcrLine //
    //-----------------//
    /**
     * Launch the OCR on the provided glyph, to retrieve the TextLine instance(s)
     * this glyph represents.
     *
     * @param glyph    the glyph to OCR
     * @param language the probable language
     * @return a list, not null but perhaps empty, of TextLine instances with absolute coordinates.
     */
    public List<TextLine> retrieveOcrLine (Glyph glyph,
                                           String language)
    {
        return getOcr().recognize(
                sheet.getScale(),
                glyph.getImage().getBufferedImage(),
                glyph.getBounds().getLocation(),
                language,
                OCR.LayoutMode.SINGLE_BLOCK,
                sheet.getId() + "-g" + glyph.getId());
    }

    //------------------//
    // retrieveSections //
    //------------------//
    /**
     * Report the set of sections that relate to the provided collection of TextChar
     * instances.
     *
     * @param chars       the OCR char descriptors
     * @param allSections the candidate sections
     * @return the corresponding set of sections
     */
    public SortedSet<Section> retrieveSections (List<TextChar> chars,
                                                Collection<Section> allSections)
    {
        SortedSet<Section> set = new TreeSet<Section>();

        for (TextChar charDesc : chars) {
            Rectangle charBox = charDesc.getBounds();

            for (Section section : allSections) {
                // Do we contain a section not (yet) assigned?
                if (!section.isProcessed() && charBox.contains(section.getBounds())) {
                    set.add(section);
                    section.setProcessed(true);
                }
            }
        }

        return set;
    }

    //------------//
    // splitWords //
    //------------//
    /**
     * Check each word in the provided collection and split it in place according to
     * separating characters ('-' etc).
     * <p>
     * The line sequence of words may get modified, because of the addition of new (sub)words and
     * the removal of words that got split.
     * The line sequence of words remains sorted.
     *
     * @param words the collection of words to check and split
     * @param line  the containing TextLine instance
     */
    public void splitWords (Collection<TextWord> words,
                            TextLine line)
    {
        // To avoid concurrent modification errors
        Collection<TextWord> toAdd = new ArrayList<TextWord>();
        Collection<TextWord> toRemove = new ArrayList<TextWord>();

        for (TextWord word : words) {
            List<TextWord> subWords = null; // Results of split
            Glyph wordGlyph = word.getGlyph();

            if (wordGlyph != null) {
                if (!wordGlyph.getTextValue().equals(word.getInternalValue())) {
                    // A manual text modification has occurred
                    // Check for a separator in the new manual value
                    if (!word.getChars().isEmpty()) {
                        logger.debug("Manual modif for {}", wordGlyph.idString());
                        subWords = getSubWords(
                                word,
                                line,
                                new WordScanner.ManualScanner(
                                        wordGlyph.getTextValue(),
                                        line.isLyrics(),
                                        params.maxCharDx,
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
                            wordGlyph.setTextWord(wordGlyph.getOcrLanguage(), newWord);
                        }
                    }
                }
            } else {
                subWords = getSubWords(
                        word,
                        line,
                        new WordScanner.OcrScanner(
                                word.getValue(),
                                line.isLyrics(),
                                params.maxCharDx,
                                word.getChars()));
            }

            if ((subWords != null) && !subWords.isEmpty()) {
                toRemove.add(word);
                toAdd.addAll(subWords);
            }
        }

        // Now perform modification on the line sequence of words, if so needed
        if (!toRemove.isEmpty()) {
            line.addWords(toAdd);
            line.removeWords(toRemove);
            checkRole(line);
        }
    }

    //---------------------//
    // switchLanguageTexts //
    //---------------------//
    /**
     * Use a new language to update existing words when a better OCR result has been found.
     */
    public void switchLanguageTexts ()
    {
        final GlyphNest nest = sheet.getNest();
        final LiveParam<String> textParam = sheet.getTextParam();
        final String language = textParam.getTarget();

        logger.debug("switchLanguageTexts lan:{}", language);

        textParam.setActual(language);

        for (TextLine oldLine : new ArrayList<TextLine>(system.getSentences())) {
            // Launch OCR on the whole line image
            List<Glyph> glyphs = oldLine.getWordGlyphs();
            Glyph compound = (glyphs.size() == 1) ? glyphs.get(0)
                    : system.registerGlyph(nest.buildGlyph(glyphs, false));

            List<TextLine> lines = retrieveOcrLine(compound, language);

            if ((lines == null) || (lines.size() != 1)) {
                logger.debug("{} No valid replacement for {}", sheet.getLogPrefix(), oldLine);
            } else {
                TextLine newLine = lines.get(0);
                recutStandardWords(newLine);

                if (logger.isDebugEnabled()) {
                    logger.debug("refreshing {} by {}", oldLine, newLine);
                    oldLine.dump();
                    newLine.dump();
                }

                List<TextWord> toRemove = new ArrayList<TextWord>();
                List<TextWord> toAdd = new ArrayList<TextWord>();

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
                        logger.debug(
                                "{} no word for {} in {}",
                                sheet.getLogPrefix(),
                                oldWord,
                                newLine);
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

    //----------------//
    // assignSentence //
    //----------------//
    /**
     * Additional processing depending on sentence role.
     * TODO: complete with all relevant roles (see old Text.populate method)
     *
     * @param sentence the created sentence
     */
    private void assignSentence (SentenceInter sentence)
    {
        switch (sentence.getRole().role) {
        case PartName:

            // Assign the sentence as part name
            Staff staff = system.getClosestStaff(sentence.getCenter());
            Part part = staff.getPart();
            part.setName(sentence.getValue());

            break;

        default:
        }
    }

    //-----------//
    // checkRole //
    //-----------//
    /**
     * Try to assign a role to the provided line, if none is already assigned.
     *
     * @param line the line to check for role
     */
    private void checkRole (TextLine line)
    {
        if (line.getRole() == null) {
            TextRoleInfo roleInfo = TextRole.guessRole(line, system);

            if (roleInfo != null) {
                line.setRole(roleInfo);
            }
        }
    }

    //------//
    // dump //
    //------//
    private void dump (String title,
                       List<TextLine> lines)
    {
        logger.info("System#{} --- {}", system.getId(), title);

        for (TextLine line : lines) {
            logger.info("   {}", line);

            for (TextWord word : line.getWords()) {
                logger.info("      {}", word);
            }
        }
    }

    //-------------//
    // findNewWord //
    //-------------//
    /**
     * Try to find in the provided new line the word that corresponds to the provided
     * old word.
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

    //-----------------//
    // getDeskewedCore //
    //-----------------//
    /**
     * Build a rectangle using de-skewed baseline and min 1 pixel high.
     *
     * @param line the TextLine entity
     * @return the de-skewed core
     */
    private Rectangle getDeskewedCore (TextLine line)
    {
        Point2D P1 = line.getDskOrigin(skew);
        Point p1 = new Point((int) Math.rint(P1.getX()), (int) Math.rint(P1.getY()));
        Point2D P2 = skew.deskewed(line.getBaseline().getP2());
        Point p2 = new Point((int) Math.rint(P2.getX()), (int) Math.rint(P2.getY()));
        Rectangle rect = new Rectangle(p1);
        rect.add(p2);

        rect.height = Math.max(1, rect.height); // To allow containment test

        return rect;
    }

    //-------------//
    // getSections //
    //-------------//
    /**
     * Build all the system sections that could be part of OCR'ed items.
     *
     * @param buffer the pixel buffer used by OCR
     * @param lines  the text lines kept
     * @return the collection of candidate sections
     */
    private List<Section> getSections (ByteProcessor buffer,
                                       List<TextLine> lines)
    {
        Lag tLag = new BasicLag("tLag", VERTICAL);
        SectionFactory factory = new SectionFactory(tLag, JunctionRatioPolicy.DEFAULT);
        List<Section> allSections = new ArrayList<Section>();

        for (TextLine line : lines) {
            for (TextWord word : line.getWords()) {
                Rectangle roi = word.getBounds();
                List<Section> wordSections = factory.createSections(buffer, null, roi);
                allSections.addAll(wordSections);
            }
        }

        return allSections;
    }

    //-------------//
    // getSubWords //
    //-------------//
    /**
     * Report the potential sub-words of the provided word, based on the provided
     * scanner to adapt to OCR or Manual values.
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
        final List<TextWord> subWords = new ArrayList<TextWord>();
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
                        base.getP1(),
                        base.getP2(),
                        new Point2D.Double(x1, 0),
                        new Point2D.Double(x1, 100));

                Rectangle box = wordChars.get(wordChars.size() - 1).getBounds();
                int x2 = (box.x + box.width) - 1;
                Point2D p2 = LineUtil.intersection(
                        base.getP1(),
                        base.getP2(),
                        new Point2D.Double(x2, 0),
                        new Point2D.Double(x2, 100));
                Line2D subBase = new Line2D.Double(p1, p2);

                // Allocate sub-word
                TextWord newWord = new TextWord(
                        subBase,
                        subValue,
                        word.getFontInfo(),
                        word.getConfidence(),
                        wordChars,
                        line);

                if (logger.isDebugEnabled()) {
                    logger.info("  subWord '{}' out of '{}'", newWord.getValue(), word.getValue());
                }

                subWords.add(newWord);
            }
        }

        return subWords;
    }

    //------------//
    // getWordGap //
    //------------//
    /**
     * Report the maximum abscissa gap between two consecutive words of the provided line.
     * <p>
     * We use a smaller horizontal gap between chord names than between words of ordinary standard
     * lines.
     *
     * @param line the line provided
     * @return the maximum abscissa gap to use
     */
    private int getWordGap (TextLine line)
    {
        return line.isChordName() ? params.maxChordDx : params.maxWordDx;
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
        Collections.sort(chunks, TextLine.byAbscissa(skew));

        if (chunks.size() == 1) {
            line = chunks.get(0);
        } else {
            if (logger.isDebugEnabled()) {
                for (TextLine chunk : chunks) {
                    logger.debug("   chunk {}", chunk);
                }
            }

            line = mergeLines(chunks);
            checkRole(line);

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
     * For lyrics, separate lines with similar ordinate trigger a line merge.
     *
     * @param oldLyrics collection of lyrics chunks
     * @return resulting lyrics lines
     */
    private List<TextLine> mergeLyricsLines (List<TextLine> oldLyrics)
    {
        logger.debug("mergeLyricsLines");

        List<TextLine> newLyrics = new ArrayList<TextLine>();
        Collections.sort(oldLyrics, TextLine.byOrdinate(skew));

        List<TextLine> chunks = new ArrayList<TextLine>();
        double lastY = 0;

        for (TextLine line : oldLyrics) {
            double y = line.getDskOrigin(skew).getY();

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
     * For standard lines, separate lines with similar ordinate and small abscissa gap
     * trigger a line merge.
     *
     * @param oldStandards collection of standard candidates
     * @return resulting standard lines
     */
    private List<TextLine> mergeStandardLines (List<TextLine> oldStandards)
    {
        logger.debug("mergeStandardLines");

        Collections.sort(oldStandards, TextLine.byOrdinate(skew));

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

                    if ((head != candidate) && !head.isProcessed()) {
                        Rectangle headBounds = getDeskewedCore(head);

                        if (headBounds.intersects(candidateFatBox)) {
                            if (head.isChordName()) {
                                // Check actual dx between head & candidate
                                int gap = GeoUtil.xGap(headBounds, candidateBounds);

                                if (gap > params.maxChordDx) {
                                    continue;
                                }
                            }

                            if (candidate.isVip() || head.isVip() || logger.isDebugEnabled()) {
                                logger.info("  merging {} into {}", candidate, head);
                            }

                            head.addWords(candidate.getWords());
                            checkRole(head);
                            candidate.setProcessed(true);
                            candidate = head;

                            break HeadsLoop;
                        }
                    }
                }
            }
        }

        // Remove unavailable lines
        List<TextLine> newStandards = new ArrayList<TextLine>();

        for (TextLine line : oldStandards) {
            if (!line.isProcessed()) {
                checkRole(line);
                newStandards.add(line);
            }
        }

        return newStandards;
    }

    //--------------------//
    // mergeStandardWords //
    //--------------------//
    private void mergeStandardWords (TextLine line)
    {
        ///logger.debug("  mergeStandardWords for {}", line);
        List<TextWord> toAdd = new ArrayList<TextWord>();
        List<TextWord> toRemove = new ArrayList<TextWord>();
        TextWord prevWord = null;

        for (TextWord word : line.getWords()) {
            // Look for tiny inter-word gap
            if (prevWord != null) {
                Rectangle prevBounds = prevWord.getBounds();
                int prevStop = prevBounds.x + prevBounds.width;
                int gap = word.getBounds().x - prevStop;

                if (gap < params.minWordDx) {
                    toRemove.add(prevWord);
                    toRemove.add(word);

                    TextWord bigWord = TextWord.mergeOf(prevWord, word);

                    if (logger.isDebugEnabled()) {
                        logger.info("    merged {} & {} into {}", prevWord, word, bigWord);
                    }

                    toAdd.add(bigWord);
                    word = bigWord;
                }
            }

            prevWord = word;
        }

        if (!toAdd.isEmpty()) {
            // No use to add & remove the same words
            List<TextWord> common = new ArrayList<TextWord>(toAdd);
            common.retainAll(toRemove);
            toAdd.removeAll(common);
            toRemove.removeAll(common);

            // Perform the modifications
            line.addWords(toAdd);
            line.removeWords(toRemove);
            checkRole(line);
        }
    }

    //-------------------//
    // purgeInvalidLines //
    //-------------------//
    /**
     * Purge lines whose validity is not confirmed.
     *
     * @param kind  lyrics or standards
     * @param lines the lines to purge
     * @return the remaining lines
     */
    private List<TextLine> purgeInvalidLines (String kind,
                                              List<TextLine> lines)
    {
        logger.debug("purgeInvalidLines for {}", kind);

        List<TextLine> newLines = new ArrayList<TextLine>();

        for (TextLine line : lines) {
            String reason = checkValidity(line);

            if (reason != null) {
                line.setProcessed(true);

                if (logger.isDebugEnabled()) {
                    logger.info("  {} {}", reason, line);

                    for (TextWord word : line.getWords()) {
                        logger.debug("    {}", word);
                    }
                }
            } else {
                newLines.add(line);
            }
        }

        return newLines;
    }

    //---------------------//
    // separatePopulations //
    //---------------------//
    /**
     * Separate the provided lines into lyrics lines and standard (non-lyrics) lines.
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
                checkRole(line);

                if (line.isLyrics()) {
                    lyrics.add(line);
                } else {
                    standards.add(line);
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("  raw {}", line);

                    for (TextWord word : line.getWords()) {
                        logger.debug("    {}", word);
                    }
                }
            }
        }
    }

    //--------------------//
    // splitStandardLines //
    //--------------------//
    /**
     * For standard (non-lyrics) lines, a really wide gap between two words indicate
     * the need to split the line in two.
     *
     * @param oldStandards collection of initial standard lines
     * @return resulting standard lines
     */
    private List<TextLine> splitStandardLines (List<TextLine> oldStandards)
    {
        logger.debug("splitStandardLines");

        Collections.sort(oldStandards, TextLine.byOrdinate(skew));

        List<TextLine> newStandards = new ArrayList<TextLine>();

        for (TextLine line : oldStandards) {
            final int maxAbscissaGap = getWordGap(line);
            List<TextWord> words = line.getWords();
            boolean splitting = true;

            while (splitting) {
                splitting = false;

                // Look for huge inter-word gap
                Integer stop = null; // Abscissa at end of previous word

                for (TextWord word : words) {
                    Rectangle bounds = word.getBounds();

                    if (stop != null) {
                        int gap = bounds.x - stop;

                        if (gap > maxAbscissaGap) {
                            int splitPos = words.indexOf(word);
                            List<TextWord> lineWords = words.subList(0, splitPos);
                            TextLine newLine = new TextLine(lineWords);
                            checkRole(newLine);

                            if (line.isVip() || logger.isDebugEnabled()) {
                                logger.info("  subLine {}", newLine);
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
                TextLine newLine = new TextLine(words);
                checkRole(newLine);

                if (line.isVip() || logger.isDebugEnabled()) {
                    logger.info("  subLine {}", newLine);
                }

                newStandards.add(newLine);
            } else {
                newStandards.add(line);
            }
        }

        return newStandards;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        Constant.String abnormalWordRegexp = new Constant.String(
                "^[\\.°>]$",
                "Regular expression to detect abnormal words");

        Constant.Double minConfidence = new Constant.Double(
                "0..1",
                0.70,
                "Minimum confidence for OCR validity");

        Constant.Double lowConfidence = new Constant.Double(
                "0..1",
                0.30,
                "Really low confidence to exclude words");

        Constant.Integer maxCharCountForAspectCheck = new Constant.Integer(
                "CharCount",
                3,
                "Maximum character count to apply aspect check");

        Scale.Fraction minWidthForCheck = new Scale.Fraction(
                0.15,
                "Minimum width to check word aspect");

        Scale.Fraction minHeightForCheck = new Scale.Fraction(
                0.15,
                "Minimum height to check word aspect");

        Constant.Ratio minAspectRatio = new Constant.Ratio(
                0.5,
                "Minimum ratio between ocr and glyph aspects");

        Constant.Ratio maxAspectRatio = new Constant.Ratio(
                1.5,
                "Maximum ratio between ocr and glyph aspects");

        Constant.Ratio minDiagRatio = new Constant.Ratio(
                0.5,
                "Minimum ratio between ocr and glyph diagonals");

        Constant.Ratio maxDiagRatio = new Constant.Ratio(
                1.5,
                "Maximum ratio between ocr and glyph diagonals");

        Scale.Fraction maxFontSize = new Scale.Fraction(7.0, "Max font size wrt interline");

        Scale.Fraction maxLyricsDy = new Scale.Fraction(
                1.0,
                "Max vertical gap between two lyrics chunks");

        Scale.Fraction maxCharDx = new Scale.Fraction(
                1.0,
                "Max horizontal gap between two chars in a word");

        Scale.Fraction maxWordDx = new Scale.Fraction(
                3.0,
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
        //~ Instance fields ------------------------------------------------------------------------

        final int maxFontSize;

        final int maxLyricsDy;

        final int maxCharDx;

        final int maxWordDx;

        final int minWordDx;

        final int maxChordDx;

        final int minWidthForCheck;

        final int minHeightForCheck;

        final double minAspectRatio;

        final double maxAspectRatio;

        final double minDiagRatio;

        final double maxDiagRatio;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxFontSize = scale.toPixels(constants.maxFontSize);
            maxLyricsDy = scale.toPixels(constants.maxLyricsDy);
            maxCharDx = scale.toPixels(constants.maxCharDx);
            maxWordDx = scale.toPixels(constants.maxWordDx);
            minWordDx = scale.toPixels(constants.minWordDx);
            maxChordDx = scale.toPixels(constants.maxChordDx);
            minWidthForCheck = scale.toPixels(constants.minWidthForCheck);
            minHeightForCheck = scale.toPixels(constants.minHeightForCheck);

            minAspectRatio = constants.minAspectRatio.getValue();
            maxAspectRatio = constants.maxAspectRatio.getValue();
            minDiagRatio = constants.minDiagRatio.getValue();
            maxDiagRatio = constants.maxDiagRatio.getValue();
        }
    }
}
