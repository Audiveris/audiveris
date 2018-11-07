//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     T e x t B u i l d e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.text;

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.dynamic.CompoundFactory;
import org.audiveris.omr.glyph.dynamic.CompoundFactory.CompoundConstructor;
import org.audiveris.omr.glyph.dynamic.SectionCompound;
import org.audiveris.omr.lag.JunctionRatioPolicy;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.lag.SectionFactory;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.LineUtil;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.ChordNameInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.XmlUtil;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Class {@code TextBuilder} works at system level, to provide features to check, build
 * and reorganize text items, including interacting with the OCR engine.
 * <p>
 * This builder can operate in 3 different modes: <ol>
 * <li><b>Free mode</b>: Engine mode, text role can be any role, determined by heuristics.
 * manualLyrics == null;
 * <li><b>Manual as lyrics</b>: Manual mode, for which text role is imposed as lyrics.
 * manualLyrics == true;
 * <li><b>Manual as non-lyrics</b>: Manual mode, for which text role is imposed as non lyrics.
 * manualLyrics == false;
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class TextBuilder
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TextBuilder.class);

    /** Abnormal characters. */
    private static final char[] ABNORMAL_CHARS = new char[]{'\\'};

    /** Regexp for abnormal words. */
    private static final Pattern ABNORMAL_WORDS = getAbnormalWords();

    /** Needed for font size computation. */
    protected static final FontRenderContext frc = new FontRenderContext(null, true, true);

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

    /** Set of text lines. */
    private final Set<TextLine> textLines = new LinkedHashSet<TextLine>();

    /** Processed sections. true/false */
    private final Set<Section> processedSections = new LinkedHashSet<Section>();

    /** Manual mode. */
    private final Boolean manualLyrics;

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
        manualLyrics = null;
        params = new Parameters(sheet.getScale(), false);
    }

    /**
     * Creates a new TextBuilder object in manual mode.
     *
     * @param system       the related system
     * @param manualLyrics true for lyrics, false for any role but lyrics
     */
    public TextBuilder (SystemInfo system,
                        boolean manualLyrics)
    {
        this.system = system;
        this.manualLyrics = manualLyrics;

        sheet = system.getSheet();
        skew = sheet.getSkew();
        params = new Parameters(sheet.getScale(), true);
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
            if ((word.getConfidence() >= constants.minConfidence.getValue()) && (word.getLength()
                                                                                         > 1)) {
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

    //--------------------//
    // retrieveGlyphLines //
    //--------------------//
    /**
     * Retrieve the glyph lines, among the lines OCR'ed from the glyph buffer.
     *
     * @param buffer     the (glyph) pixel buffer
     * @param glyphLines the glyph raw OCR lines, relative to buffer origin
     * @param offset     glyph top left corner (with respect to sheet origin)
     * @return the final absolute text lines, ready to be inserted in sig
     */
    public List<TextLine> retrieveGlyphLines (ByteProcessor buffer,
                                              List<TextLine> glyphLines,
                                              Point offset)
    {
        // Pre-assign text roleas lyrics?
        if (isManual() && manualLyrics) {
            for (TextLine line : glyphLines) {
                line.setRole(TextRole.Lyrics);
            }
        }

        List<Section> relativeSections = getSections(buffer, glyphLines);
        mapGlyphs(glyphLines, relativeSections, offset);

        // Translate to absolute coordinates
        for (TextLine glyphLine : glyphLines) {
            glyphLine.translate(offset.x, offset.y);
        }

        glyphLines = recomposeLines(glyphLines);

        return glyphLines;
    }

    //---------------------//
    // retrieveSystemLines //
    //---------------------//
    /**
     * Retrieve the system-relevant lines, among all the lines OCR'ed at sheet level.
     *
     * @param buffer     the (sheet) pixel buffer
     * @param sheetLines the sheet raw OCR lines
     */
    public void retrieveSystemLines (ByteProcessor buffer,
                                     List<TextLine> sheetLines)
    {
        StopWatch watch = new StopWatch("Texts retrieveLines system#" + system.getId());
        watch.start("Pickup system lines");

        List<TextLine> systemLines = new ArrayList<TextLine>();

        // We pick up the words that are contained by system area
        // Beware: a text located between two systems must be deep copied to each system!
        final Area area = system.getArea();
        final Rectangle areaBounds = area.getBounds();

        for (TextLine sheetLine : sheetLines) {
            if (areaBounds.intersects(sheetLine.getBounds())) {
                TextLine line = new TextLine();

                for (TextWord sheetWord : sheetLine.getWords()) {
                    final Rectangle wordBox = sheetWord.getBounds();

                    if (area.contains(wordBox)) {
                        TextWord word = sheetWord.copy();
                        line.appendWord(word);
                    }
                }

                if (!line.getWords().isEmpty()) {
                    systemLines.add(line);
                }
            }
        }

        Collections.sort(systemLines, TextLine.byOrdinate(skew));

        watch.start("recomposeLines");
        systemLines = recomposeLines(systemLines);

        // Retrieve candidate sections and map words to glyphs
        watch.start("getSections");

        // Brand new sections (not the usual horizontal or vertical sheet sections)
        List<Section> relSections = getSections(buffer, systemLines);
        watch.start("mapGlyphs");
        mapGlyphs(systemLines, relSections, null);

        // Allocate corresponding inters based on role (Sentences or LyricLines of LyricItems)
        watch.start("createInters");
        createInters();

        watch.start("numberLyricLines()");
        numberLyricLines();

        if (constants.printWatch.isSet()) {
            watch.print();
        }

        if (logger.isDebugEnabled()) {
            dump("Retrieved lines", systemLines);
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
            TextRole role = (isManual() && manualLyrics) ? TextRole.Lyrics : TextRole
                    .guessRole(line, system, manualLyrics == null);

            if (role != null) {
                line.setRole(role);
            }
        }
    }

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
    private String checkValidity (TextLine line)
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
    private String checkValidity (TextWord word)
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
                logger
                        .debug("   Invalid aspect {} vs [{}-{}] for {}", aRatio,
                               params.minAspectRatio, params.maxAspectRatio, word);

                return "invalid aspect";
            }

            // Check size
            double boxDiag = Math.hypot(box.width, box.height);
            double rectDiag = Math.hypot(rect.getWidth(), rect.getHeight());
            double diagRatio = boxDiag / rectDiag;

            ///logger.info(String.format("aspect:%.2f diag:%.2f %s", aRatio, diagRatio, word));
            if ((diagRatio < params.minDiagRatio) || (diagRatio > params.maxDiagRatio)) {
                logger.debug("   Invalid diagonal {} vs [{}-{}] for {}", diagRatio,
                             params.minDiagRatio, params.maxDiagRatio, word);

                return "invalid diagonal";
            }
        }

        return null; // OK
    }

    //--------------//
    // createInters //
    //--------------//
    /**
     * Allocate corresponding inters based on text role.
     * <ul>
     * <li>For any role other than Lyrics, a plain Sentence is created for each text line.</li>
     * <li>For Lyrics role, a specific LyricLine (sub-class of Sentence) is created.</li>
     * </ul>
     */
    private void createInters ()
    {
        final SIGraph sig = system.getSig();

        for (TextLine line : textLines) {
            final TextRole role = line.getRole();
            final SentenceInter sentence = (role == TextRole.Lyrics) ? LyricLineInter.create(line)
                    : ((role == TextRole.ChordName) ? ChordNameInter.create(line) : SentenceInter
                            .create(line));

            // Related staff (can still be modified later)
            Staff staff = sentence.assignStaff(system, line.getLocation());

            if (staff != null) {
                // Populate sig
                sig.addVertex(sentence);

                // Link sentence and words
                for (TextWord word : line.getWords()) {
                    WordInter wordInter = (role == TextRole.Lyrics) ? new LyricItemInter(word)
                            : new WordInter(word);
                    wordInter.setStaff(staff);
                    sig.addVertex(wordInter);
                    sentence.addMember(wordInter);
                }
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

    //---------------//
    // dumpTextLines //
    //---------------//
    /**
     * Debug method to list current system text lines.
     *
     * @param title a title for the dump
     */
    private void dumpTextLines (String title)
    {
        logger.info("{} lines: {}", title, textLines.size());

        for (TextLine line : textLines) {
            logger.info("   {}", line);
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

    //---------------//
    // getMaxCharGap //
    //---------------//
    /**
     * Compute max abscissa gap between two chars in a word.
     *
     * @param word the word at hand
     * @return max number of pixels for abscissa gap, according to the word font size
     */
    private int getMaxCharGap (TextWord word)
    {
        int pointSize = word.getFontInfo().pointsize;

        // TODO: very rough value to be refined and explained!
        int val = (int) Math.rint((constants.maxCharDx.getValue() * pointSize) / 2.0);

        return val;
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
        SectionFactory factory = new SectionFactory(VERTICAL, JunctionRatioPolicy.DEFAULT);
        List<Section> allSections = new ArrayList<Section>();

        for (TextLine line : lines) {
            for (TextWord word : line.getWords()) {
                Rectangle roi = word.getBounds();
                List<Section> wordSections = factory.createSections(buffer, roi);
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
                Point2D p1 = LineUtil.intersection(base.getP1(), base.getP2(),
                                                   new Point2D.Double(x1, 0), new Point2D.Double(x1,
                                                                                                 100));

                Rectangle box = wordChars.get(wordChars.size() - 1).getBounds();
                int x2 = (box.x + box.width) - 1;
                Point2D p2 = LineUtil.intersection(base.getP1(), base.getP2(),
                                                   new Point2D.Double(x2, 0), new Point2D.Double(x2,
                                                                                                 100));
                Line2D subBase = new Line2D.Double(p1, p2);

                // Allocate sub-word
                TextWord newWord = new TextWord(subBase, subValue, word.getFontInfo(), word
                                                .getConfidence(), wordChars, line);

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
        // Chord name
        if (line.isChordName()) {
            return params.maxChordDx;
        }

        // Standard line, adapt inter-word gap to font size
        int pointSize = line.getMeanFont().pointsize;

        return (int) Math.rint(params.maxWordDxFontRatio * pointSize);
    }

    //----------//
    // isManual //
    //----------//
    /**
     * Report whether TextBuilder is working in manual mode.
     *
     * @return true if manual mode
     */
    private boolean isManual ()
    {
        return manualLyrics != null;
    }

    //-------------//
    // isProcessed //
    //-------------//
    private boolean isProcessed (Section section)
    {
        return processedSections.contains(section);
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
    private boolean isValidFontSize (TextLine line)
    {
        for (TextWord word : line.getWords()) {
            FontInfo fontInfo = word.getFontInfo();

            if (fontInfo.pointsize > params.maxFontSize) {
                logger.debug("Too big font {} vs {} on {}", fontInfo.pointsize, params.maxFontSize,
                             line);

                return false;
            } else if (fontInfo.pointsize < params.minFontSize) {
                logger
                        .debug("Too small font {} vs {} on {}", fontInfo.pointsize,
                               params.minFontSize, line);

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
     * Build also one glyph per word char, to allow manual re-assignment. TODO: still useful?
     *
     * @param lines       the (relative) lines (and contained words) to be mapped
     * @param allSections the population of (relative) sections to browse
     * @param offset      offset to be applied on sections
     */
    private void mapGlyphs (List<TextLine> lines,
                            Collection<Section> allSections,
                            Point offset)
    {
        logger.debug("mapGlyphs");

        final int dx = (offset != null) ? offset.x : 0;
        final int dy = (offset != null) ? offset.y : 0;
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();
        final int interline = sheet.getInterline();
        final CompoundConstructor constructor = new SectionCompound.Constructor(interline);

        for (TextLine line : lines) {
            logger.debug("  mapping {}", line);

            // Browse all words, starting by shorter ones
            List<TextWord> toRemove = new ArrayList<TextWord>();
            List<TextWord> sortedWords = new ArrayList<TextWord>(line.getWords());
            Collections.sort(sortedWords, TextWord.bySize);

            for (TextWord word : sortedWords) {
                // Isolate proper word glyph from its enclosed sections
                SortedSet<Section> wordSections = retrieveSections(word.getChars(), allSections,
                                                                   offset);

                if (!wordSections.isEmpty()) {
                    SectionCompound compound = CompoundFactory.buildCompound(wordSections,
                                                                             constructor);
                    Glyph rel = compound.toGlyph(null);
                    Glyph wordGlyph = glyphIndex.registerOriginal(new Glyph(rel.getLeft() + dx, rel
                                                                            .getTop() + dy, rel
                                                                                    .getRunTable()));

                    // Link TextWord -> Glyph
                    word.setGlyph(wordGlyph);

                    if (word.isVip()) {
                        line.setVip(true);
                    }

                    if (word.isVip()) {
                        logger.info("    mapped {}", word);
                    } else {
                        logger.debug("    mapped {}", word);
                    }
                } else {
                    logger.debug("No section found for {}", word);
                    toRemove.add(word);
                }
            }

            // Purge words if any
            line.removeWords(toRemove);

            textLines.add(line);
        }
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

    //------------//
    // mergeLines //
    //------------//
    /**
     * Merge a sequence of TextLine instances into a single instance.
     *
     * @param lines the lines to merge
     * @return a single TextLine
     */
    private TextLine mergeLines (List<TextLine> lines)
    {
        List<TextWord> words = new ArrayList<TextWord>();

        for (TextLine line : lines) {
            line.setProcessed(true);
            words.addAll(line.getWords());
        }

        Collections.sort(words, TextWord.byAbscissa);

        return new TextLine(words);
    }

    //-----------------//
    // mergeLyricLines //
    //-----------------//
    /**
     * For lyrics, separate lines with similar ordinate trigger a line merge.
     *
     * @param oldLyrics collection of lyrics chunks
     * @return resulting lyrics lines
     */
    private List<TextLine> mergeLyricLines (List<TextLine> oldLyrics)
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
            } else if ((y - lastY) <= params.maxLyricsDy) {
                // Compatible line
                chunks.add(line);
            } else {
                // Non compatible line

                // Complete pending chunks, if any
                if (!chunks.isEmpty()) {
                    newLyrics.add(mergeChunks(chunks));
                }

                // Start a new collection of chunks
                chunks.clear();
                chunks.add(line);
            }

            lastY = y;
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
        final int minWordDx = (int) Math.rint(line.getMeanFont().pointsize
                                                      * params.minWordDxFontRatio);
        List<TextWord> toAdd = new ArrayList<TextWord>();
        List<TextWord> toRemove = new ArrayList<TextWord>();
        TextWord prevWord = null;

        for (TextWord word : line.getWords()) {
            // Look for tiny inter-word gap
            if (prevWord != null) {
                Rectangle prevBounds = prevWord.getBounds();
                int prevStop = prevBounds.x + prevBounds.width;
                int gap = word.getBounds().x - prevStop;

                if (gap < minWordDx) {
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

    //------------------//
    // numberLyricLines //
    //------------------//
    /**
     * Order and number the lyric lines per part.
     */
    private void numberLyricLines ()
    {
        // Sort lyric lines by (deskewed) ordinate
        final SIGraph sig = system.getSig();
        List<Inter> lyricInters = sig.inters(LyricLineInter.class);

        if (lyricInters.isEmpty()) {
            return;
        }

        List<LyricLineInter> lines = new ArrayList<LyricLineInter>();

        for (Inter inter : lyricInters) {
            lines.add((LyricLineInter) inter);
        }

        Collections.sort(lines, SentenceInter.byOrdinate);

        // Assign sequential number to lyric line in its part
        int lyricNumber = 0;
        Part part = null;

        for (LyricLineInter line : lines) {
            Staff staff = line.getStaff();
            Part newPart = staff.getPart();

            if (newPart != part) {
                lyricNumber = 0;
                part = newPart;
            }

            line.setNumber(++lyricNumber);
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
    private List<TextLine> recomposeLines (Collection<TextLine> rawLines)
    {
        logger.debug("System#{} recomposeLines", system.getId());

        // Separate lyrics and standard lines, based on their roles
        List<TextLine> standards = new ArrayList<TextLine>();
        List<TextLine> lyrics = new ArrayList<TextLine>();
        separatePopulations(rawLines, standards, lyrics);

        // Process lyrics
        if (!lyrics.isEmpty()) {
            if (!isManual()) {
                lyrics = purgeInvalidLines("lyrics", lyrics);
            }

            lyrics = mergeLyricLines(lyrics);

            logger.debug("splitWords for lyrics");

            for (TextLine line : lyrics) {
                splitWords(line.getWords(), line);
            }
        }

        // Process standards
        if (!standards.isEmpty()) {
            standards = splitStandardLines(standards);

            // Reject invalid standard lines
            if (!isManual()) {
                standards = purgeInvalidLines("standards", standards);
            }

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
     * Re-cut (merge &amp; split) words within a standard TextLine.
     *
     * @param line the line to re-cut words
     */
    private void recutStandardWords (TextLine line)
    {
        mergeStandardWords(line);
        splitWords(line.getWords(), line);
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
     * @param offset      offset to apply to char glyphs before being registered
     * @return the corresponding set of sections
     */
    private SortedSet<Section> retrieveSections (List<TextChar> chars,
                                                 Collection<Section> allSections,
                                                 Point offset)
    {
        final SortedSet<Section> wordSections = new TreeSet<Section>(Section.byFullAbscissa);
        final CompoundConstructor constructor
                = new SectionCompound.Constructor(sheet.getInterline());

        final int dx = (offset != null) ? offset.x : 0;
        final int dy = (offset != null) ? offset.y : 0;

        for (TextChar charDesc : chars) {
            final Rectangle charBox = charDesc.getBounds();
            final SortedSet<Section> charSections = new TreeSet<Section>(Section.byFullAbscissa);

            for (Section section : allSections) {
                // Do we contain a section not (yet) assigned?
                if (!isProcessed(section) && charBox.contains(section.getBounds())) {
                    charSections.add(section);
                    setProcessed(section);
                }
            }

            if (!charSections.isEmpty()) {
                wordSections.addAll(charSections);

                // Register char underlying glyph
                SectionCompound compound = CompoundFactory.buildCompound(charSections, constructor);
                Glyph relGlyph = compound.toGlyph(null);
                Glyph absGlyph = new Glyph(relGlyph.getLeft() + dx, relGlyph.getTop() + dy, relGlyph
                                           .getRunTable());
                Glyph charGlyph = sheet.getGlyphIndex().registerOriginal(absGlyph);
                system.addFreeGlyph(charGlyph);
            }
        }

        return wordSections;
    }

    //---------------------//
    // separatePopulations //
    //---------------------//
    /**
     * Separate the provided lines into lyrics lines and standard (non-lyrics) lines.
     *
     * @param lines     (input) the global population
     * @param standards (output) the non-lyrics population
     * @param lyrics    (output) the lyrics population
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

    //--------------//
    // setProcessed //
    //--------------//
    private void setProcessed (Section section)
    {
        processedSections.add(section);
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
            final int maxAbscissaGap = getWordGap(line); // TODO: should gap depend on font size?
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
    private void splitWords (Collection<TextWord> words,
                             TextLine line)
    {
        // To avoid concurrent modification errors
        Collection<TextWord> toAdd = new ArrayList<TextWord>();
        Collection<TextWord> toRemove = new ArrayList<TextWord>();

        for (TextWord word : words) {
            List<TextWord> subWords = null; // Results of split
            final int maxCharGap = getMaxCharGap(word); // Max gap depends on word font size

            //            final Glyph wordGlyph = word.getCompound();
            //            if (wordGlyph != null) {
            //                if (!wordGlyph.getTextValue().equals(word.getInternalValue())) {
            //                    // A manual text modification has occurred
            //                    // Check for a separator in the new manual value
            //                    if (!word.getChars().isEmpty()) {
            //                        logger.debug("Manual modif for {}", wordGlyph.idString());
            //                        subWords = getSubWords(
            //                                word,
            //                                line,
            //                                new WordScanner.ManualScanner(
            //                                        wordGlyph.getTextValue(),
            //                                        line.isLyrics(),
            //                                        maxCharGap,
            //                                        word.getChars()));
            //
            //                        // If no subdivision was made, allocate a new TextWord
            //                        // just to match the new manual value
            //                        if (subWords.isEmpty()) {
            //                            TextWord newWord = new TextWord(
            //                                    word.getBaseline(),
            //                                    wordGlyph.getTextValue(),
            //                                    word.getFontInfo(),
            //                                    word.getConfidence(),
            //                                    word.getChars(),
            //                                    line);
            //                            newWord.setCompound(wordGlyph);
            //                            subWords.add(newWord);
            //                            wordGlyph.setTextWord(wordGlyph.getOcrLanguage(), newWord);
            //                        }
            //                    }
            //                }
            //            } else {
            subWords = getSubWords(word, line, new WordScanner.OcrScanner(word.getValue(), line
                                                                          .isLyrics(), maxCharGap,
                                                                          word.getChars()));

            //            }
            if (!subWords.isEmpty()) {
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

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(false,
                                                                         "Should we print out the stop watch?");

        private final Constant.String abnormalWordRegexp = new Constant.String("^[°>']$",
                                                                               "Regular expression to detect abnormal words");

        private final Constant.Double minConfidence = new Constant.Double("0..1", 0.65,
                                                                          "Minimum confidence for OCR validity");

        private final Constant.Double lowConfidence = new Constant.Double("0..1", 0.30,
                                                                          "Really low confidence to exclude words");

        private final Scale.Fraction minWidthForCheck = new Scale.Fraction(0.15,
                                                                           "Minimum width to check word aspect");

        private final Scale.Fraction minHeightForCheck = new Scale.Fraction(0.15,
                                                                            "Minimum height to check word aspect");

        private final Constant.Ratio minAspectRatio = new Constant.Ratio(0.5,
                                                                         "Minimum ratio between ocr and glyph aspects");

        private final Constant.Ratio maxAspectRatio = new Constant.Ratio(1.5,
                                                                         "Maximum ratio between ocr and glyph aspects");

        private final Constant.Ratio minDiagRatio = new Constant.Ratio(0.5,
                                                                       "Minimum ratio between ocr and glyph diagonals");

        private final Constant.Ratio maxDiagRatio = new Constant.Ratio(1.5,
                                                                       "Maximum ratio between ocr and glyph diagonals");

        private final Scale.Fraction minFontSize = new Scale.Fraction(1.25,
                                                                      "Minimum font size with respect to interline");

        private final Scale.Fraction maxFontSize = new Scale.Fraction(8.0,
                                                                      "Maximum font size with respect to interline");

        private final Scale.Fraction maxLyricsDy = new Scale.Fraction(1.0,
                                                                      "Max vertical gap between two lyrics chunks");

        private final Scale.Fraction maxCharDx = new Scale.Fraction(1.0,
                                                                    "Max horizontal gap between two chars in a word");

        private final Constant.Ratio maxWordDxFontRatio = new Constant.Ratio(1.5,
                                                                             "Max horizontal gap between two non-lyrics words as font ratio");

        private final Constant.Ratio minWordDxFontRatio = new Constant.Ratio(0.125,
                                                                             "Min horizontal gap between two non-lyrics words as font ratio");

        private final Scale.Fraction maxChordDx = new Scale.Fraction(1.0,
                                                                     "Max horizontal gap between two chord words");
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {

        final int minFontSize;

        final int maxFontSize;

        final int maxLyricsDy;

        final int maxCharDx;

        final double maxWordDxFontRatio;

        final double minWordDxFontRatio;

        final int maxChordDx;

        final int minWidthForCheck;

        final int minHeightForCheck;

        final double minAspectRatio;

        final double maxAspectRatio;

        final double minDiagRatio;

        final double maxDiagRatio;

        public Parameters (Scale scale,
                           boolean isManual)
        {
            // TODO: check all these constant for specific manual work...
            minFontSize = scale.toPixels(constants.minFontSize);
            maxFontSize = scale.toPixels(constants.maxFontSize);
            maxLyricsDy = scale.toPixels(constants.maxLyricsDy);
            maxCharDx = scale.toPixels(constants.maxCharDx);
            maxWordDxFontRatio = constants.maxWordDxFontRatio.getValue();
            minWordDxFontRatio = constants.minWordDxFontRatio.getValue();
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
