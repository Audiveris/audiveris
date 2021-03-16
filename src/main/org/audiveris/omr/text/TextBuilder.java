//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     T e x t B u i l d e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.ProcessingSwitches;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.SystemManager;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.ChordNameInter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.text.tesseract.TesseractOCR;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Pair;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code TextBuilder} works at system level, providing features to check, build
 * and reorganize text items, including interacting with the OCR engine.
 * <p>
 * This builder can operate in 3 different modes:
 * <ol>
 * <li><b>Free mode</b>: Engine mode, text role can be any role, determined by heuristics.
 * <br>{@code manualLyrics == null;}
 * <li><b>Manual as lyrics</b>: Manual mode, for which text role is imposed as lyrics.
 * <br>{@code manualLyrics == true;}
 * <li><b>Manual as non-lyrics</b>: Manual mode, for which text role is imposed as non lyrics.
 * <br>{@code manualLyrics == false;}
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class TextBuilder
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TextBuilder.class);

    /** Related system. */
    @Navigable(false)
    private final SystemInfo system;

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Global scale. */
    private final Scale scale;

    /** Global skew. */
    private final Skew skew;

    /** Set of text lines. */
    private final Set<TextLine> textLines = new LinkedHashSet<>();

    /** Manual mode. */
    private final Boolean manualLyrics;

    /** Maximum acceptable vertical shift between line chunks. */
    private final int maxLineDy;

    /** Map with key as staff pair and value as lines partition. */
    private final Map<Pair<Staff>, Pair<List<TextLine>>> gutterMap = new HashMap<>();

    /**
     * Creates a new TextBuilder object in engine mode (TEXTS step).
     *
     * @param system the related system
     */
    public TextBuilder (SystemInfo system)
    {
        this(system, null);
    }

    /**
     * Creates a new TextBuilder object, in either engine or manual mode.
     * <p>
     * In engine mode, manualLyrics is null, leaving lines roles fully open.
     * <p>
     * In manual mode, the user has selected either "lyrics" (which forces lyrics mode) or "text"
     * (which leaves the role open to anything but lyrics).
     *
     * @param system       the related system
     * @param manualLyrics null for any role, true for lyrics, false for any role but lyrics
     */
    public TextBuilder (SystemInfo system,
                        Boolean manualLyrics)
    {
        this.system = system;
        this.manualLyrics = manualLyrics;

        sheet = system.getSheet();
        scale = sheet.getScale();
        skew = sheet.getSkew();
        maxLineDy = scale.toPixels(constants.maxLineDy);
    }

    //-------------//
    // adjustLines //
    //-------------//
    private void adjustLines (List<TextLine> lines)
    {
        for (TextLine line : lines) {
            for (TextWord word : line.getWords()) {
                word.adjust(scale);
            }
        }
    }

    //----------------//
    // adjustFontSize //
    //----------------//
    private void adjustFontSize (List<TextLine> lines)
    {
        for (Iterator<TextLine> it = lines.iterator(); it.hasNext();) {
            TextLine line = it.next();

            for (Iterator<TextWord> itw = line.getWords().iterator(); itw.hasNext();) {
                TextWord word = itw.next();

                if (!word.isDashed()) {
                    boolean ok = word.adjustFontSize();

                    if (!ok) {
                        itw.remove();
                    }
                }
            }

            if (line.getWords().isEmpty()) {
                it.remove();
            }
        }
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
        final double minConfidence = TesseractOCR.getInstance().getMinConfidence();
        int reliableWords = 0;
        int italicWords = 0;

        for (TextWord word : line.getWords()) {
            if ((word.getConfidence() >= minConfidence) && (word.getLength() > 1)) {
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
    // lookupLyricLine //
    //-----------------//
    /**
     * Look for an existing lyric line that a lyric item at provided baseline location
     * should join.
     * <p>
     * This method is called in manual mode only.
     *
     * @param baseLocation baseline location for a new text line
     * @return the lyric line to join, if any
     */
    public LyricLineInter lookupLyricLine (Point2D baseLocation)
    {
        final double newY = skew.deskewed(baseLocation).getY();
        final List<LyricLineInter> lines = system.getLyricLines();

        // Find closest line, ordinatewise
        Double bestDy = Double.MAX_VALUE;
        LyricLineInter bestLine = null;

        for (LyricLineInter line : lines) {
            final double y = skew.deskewed(line.getLocation()).getY();
            final double dy = Math.abs(y - newY);

            if (bestDy > dy) {
                bestDy = dy;
                bestLine = line;
            }
        }

        // Check we are compatible with this closest line
        if (bestDy <= maxLineDy) {
            return bestLine;
        }

        return null;
    }

    //--------------//
    // processGlyph //
    //--------------//
    /**
     * Retrieve the glyph lines, among the lines OCR'ed from the glyph buffer.
     * <p>
     * This method is called in manual mode only.
     * Boolean 'manualLyrics' is not null and is either true for lyrics imposed or false for lyrics
     * forbidden.
     *
     * @param buffer     the (glyph) pixel buffer
     * @param glyphLines the glyph raw OCR lines, relative to buffer origin
     * @param offset     glyph top left corner (with respect to sheet origin)
     * @return the final absolute text lines, ready to be inserted in sig
     */
    public List<TextLine> processGlyph (ByteProcessor buffer,
                                        List<TextLine> glyphLines,
                                        Point offset)
    {
        // Pre-assign text role as lyrics?
        if (isManual() && manualLyrics) {
            for (TextLine line : glyphLines) {
                line.setRole(TextRole.Lyrics); // Here, lyrics role is certain!
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

    //---------------//
    // processSystem //
    //---------------//
    /**
     * Retrieve the system-relevant lines, among all the lines OCR'ed at sheet level.
     * <p>
     * We may have lines that belong to the system above and lines that belong to the system below.
     * We try to filter them out immediately.
     *
     * @param buffer     the (sheet) pixel buffer
     * @param sheetLines the sheet raw OCR'ed lines
     */
    public void processSystem (ByteProcessor buffer,
                               List<TextLine> sheetLines)
    {
        logger.debug("processSystem #{}", system.getId());

        StopWatch watch = new StopWatch("Texts processSystem #" + system.getId());
        watch.start("retrieveRawLines");
        List<TextLine> rawLines = getSystemRawLines(sheetLines);

        // Gather rawLines into long lines
        watch.start("longLines");
        List<TextLine> longLines = mergeRawLines(rawLines);

        if (logger.isDebugEnabled()) {
            dump("longLines", longLines, false);
        }

        // Discard lines that pertain to a system above or to a system below
        purgeExternalLines(longLines);

        // Partition lines between parts of the system
        partitionPartLines(longLines);

        // Assign a role to each long line
        guessLongRoles(longLines);

        // Assign each lyric line to proper staff
        mapLyricLines(longLines);

        // Separate additional work on lyric lines and on standard lines
        watch.start("recomposeLines");
        List<TextLine> lines = recomposeLines(longLines);

        // Retrieve candidate sections (not the usual horizontal or vertical sheet sections)
        watch.start("getSections");
        List<Section> relSections = getSections(buffer, lines);

        // Map words to section-based glyphs
        watch.start("mapGlyphs");
        mapGlyphs(lines, relSections, null);

        // Allocate corresponding inters based on role
        // - Sentences of Words (or of one ChordName)
        // - LyricLines of LyricItems
        watch.start("createInters");
        createInters();

        watch.start("numberLyricLines()");
        system.numberLyricLines();

        if (constants.printWatch.isSet()) {
            watch.print();
        }

        if (logger.isDebugEnabled()) {
            dump("Retrieved lines", lines, true);
        }
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
                    : ((role == TextRole.ChordName) ? ChordNameInter.create(line)
                            : SentenceInter.create(line));

            // Related staff (can still be modified later)
            Staff staff = line.getStaff();

            if (staff != null) {
                sentence.setStaff(staff);
            } else {
                staff = sentence.assignStaff(system, line.getLocation());
            }

            if (staff != null) {
                // Populate sig
                sig.addVertex(sentence);

                // Link sentence and words
                for (TextWord word : line.getWords()) {
                    final WordInter wordInter = (role == TextRole.Lyrics) ? new LyricItemInter(word)
                            : ((role == TextRole.ChordName) ? ChordNameInter.createValid(word)
                                    : new WordInter(word));

                    if (wordInter != null) {
                        wordInter.setStaff(staff);
                        sig.addVertex(wordInter);
                        sentence.addMember(wordInter);
                    }
                }
            }
        }
    }

    //------//
    // dump //
    //------//
    private void dump (String title,
                       List<TextLine> lines,
                       boolean withWords)
    {
        TextLine.dump("System#" + system.getId() + " --- " + title, lines, withWords);
    }

    //-----------//
    // findBreak //
    //-----------//
    /**
     * Find the index of the breaking line, if any, between upper and lower systems.
     *
     * @param line1 last line of upper staff
     * @param dy1   ordinate extension of core musical items in upper system
     * @param line2 first line of lower staff
     * @param dy2   ordinate extension of core musical items in lower system
     * @param lines the sequence of gutter lines to check
     * @return the index of breaking line or -1 if not found
     */
    private int findBreak (LineInfo line1,
                           int dy1,
                           LineInfo line2,
                           int dy2,
                           List<TextLine> lines)
    {
        if (lines.isEmpty()) {
            return -1;
        }

        // Compute the sequence of vertical gaps
        final double[] gaps = new double[lines.size() + 1];
        Point2D prevBottom = null;
        int i;

        for (i = 0; i < lines.size(); i++) {
            TextLine line = lines.get(i);
            Point2D top = line.getDeskewedExtremum(TOP, skew);
            Point2D bottom = line.getDeskewedExtremum(BOTTOM, skew);

            if (i == 0) {
                // Gap from upper system core limit
                Point2D p1 = skew.skewed(top);
                gaps[i] = p1.getY() - (line1.yAt(p1.getX()) + dy1);
            } else {
                // Gap from previous sentence
                gaps[i] = top.getY() - prevBottom.getY();
            }

            prevBottom = bottom;
        }

        // Gap to lower system core limit
        Point2D p2 = skew.skewed(prevBottom);
        gaps[i] = line2.yAt(p2.getX()) - dy2 - p2.getY();

        // Now pickup the largest gap
        double breakDy = Double.MIN_VALUE;
        int breakIndex = -1;

        for (i = 0; i < gaps.length; i++) {
            double dy = gaps[i];
            if (breakDy <= dy) {
                breakDy = dy;
                breakIndex = i;
            }
        }

        logger.debug("gaps:{} breakIndex:{}", gaps, breakIndex);

        return breakIndex;
    }

    //----------------//
    // getGutterLines //
    //----------------//
    /**
     * Among the provided lines, retrieve the ones that are located in gutter area.
     *
     * @param lines  the provided lines
     * @param gutter the gutter area
     * @return the lines in gutter
     */
    private List<TextLine> getGutterLines (List<TextLine> lines,
                                           Area gutter)
    {
        final List<TextLine> found = new ArrayList<>();

        for (TextLine line : lines) {
            final Point2D center = line.getCenter2D();

            if (gutter.contains(center)) {
                found.add(line);
            }
        }

        return found;
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
        List<Section> allSections = new ArrayList<>();

        for (TextLine line : lines) {
            for (TextWord word : line.getWords()) {
                Rectangle roi = word.getBounds();
                List<Section> wordSections = factory.createSections(buffer, roi);
                allSections.addAll(wordSections);
            }
        }

        return allSections;
    }

    //-------------------//
    // getSystemRawLines //
    //-------------------//
    /**
     * Among the lines OCR'ed from whole sheet, select the ones that could belong to our
     * system.
     * <p>
     * Sheet lines are deep-copied to system lines.
     *
     * @param sheetLines sheet collection of OCR'ed lines
     * @return the relevant sheet lines
     */
    private List<TextLine> getSystemRawLines (List<TextLine> sheetLines)
    {
        List<TextLine> rawLines = new ArrayList<>();

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
                    rawLines.add(line);
                }
            }
        }

        adjustLines(rawLines);
        rawLines = purgeInvalidLines(rawLines);
        Collections.sort(rawLines, TextLine.byOrdinate(skew));

        return rawLines;
    }

    //----------------//
    // guessLongRoles //
    //----------------//
    /**
     * Assign suitable TextRole to each of the provided long lines.
     *
     * @param longLines the long lines to process
     */
    private void guessLongRoles (List<TextLine> longLines)
    {
        for (TextLine line : longLines) {
            guessRole(line);

            if (logger.isDebugEnabled()) {
                logger.info(String.format("long %16s %s", line.getRole(), line));
            }
        }
    }

    //-----------//
    // guessRole //
    //-----------//
    /**
     * Try to assign a role to the provided line.
     *
     * @param line the line to check
     */
    private void guessRole (TextLine line)
    {
        try {
            TextRole role = (isManual() && manualLyrics) ? TextRole.Lyrics
                    : TextRole.guessRole(line, system, manualLyrics == null);
            line.setRole(role);
        } catch (Exception ex) {
            logger.warn("Error in guessRole for {} {}", line, ex.toString(), ex);
        }
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
            List<TextWord> toRemove = new ArrayList<>();
            List<TextWord> sortedWords = new ArrayList<>(line.getWords());
            Collections.sort(sortedWords, TextWord.bySize);

            for (TextWord word : sortedWords) {
                // Isolate proper word glyph from its enclosed sections
                SortedSet<Section> wordSections = retrieveSections(
                        word.getChars(),
                        allSections,
                        offset);

                if (!wordSections.isEmpty()) {
                    SectionCompound compound = CompoundFactory.buildCompound(
                            wordSections,
                            constructor);
                    Glyph rel = compound.toGlyph(null);
                    Glyph wordGlyph = glyphIndex.registerOriginal(
                            new Glyph(rel.getLeft() + dx, rel.getTop() + dy, rel.getRunTable()));

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
                } else if (!word.isAdjusted()) {
                    logger.debug("No section found for {}", word);
                    toRemove.add(word);
                }
            }

            // Purge words if any
            line.removeWords(toRemove);

            if (!line.getWords().isEmpty()) {
                textLines.add(line);
            }
        }
    }

    //---------------//
    // mapLyricLines //
    //---------------//
    /**
     * Map each lyric line to proper staff.
     *
     * @param longLines the long lines (both lyric and standard)
     */
    private void mapLyricLines (List<TextLine> longLines)
    {
        final ProcessingSwitches switches = sheet.getStub().getProcessingSwitches();
        final boolean aboveAllowed = switches.getValue(ProcessingSwitches.Switch.lyricsAboveStaff);

        for (TextLine line : longLines) {
            if (line.getRole() != TextRole.Lyrics) {
                continue;
            }

            final Point2D center = line.getCenter2D();
            Staff staff = null;

            final Staff staffAbove = system.getStaffAtOrAbove(center);
            final Staff staffBelow = system.getStaffAtOrBelow(center);

            if (staffAbove == null) {
                staff = staffBelow;
            }

            if (staffBelow == null) {
                staff = staffAbove;
            }

            if ((staffAbove != null) && (staffBelow != null)) {
                if (!aboveAllowed || (staffAbove.getPart() == staffBelow.getPart())) {
                    staff = staffAbove;
                } else {
                    // Use partition of the gutter between the 2 parts
                    final Pair<Staff> staffPair = new Pair<>(staffAbove, staffBelow);
                    final Pair<List<TextLine>> linePartition = gutterMap.get(staffPair);

                    if (linePartition.two.contains(line)) {
                        staff = staffBelow;
                    } else {
                        staff = staffAbove;
                    }
                }

            }

            line.setStaff(staff);
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
        List<TextWord> words = new ArrayList<>();

        for (TextLine line : lines) {
            line.setProcessed(true);
            words.addAll(line.getWords());
        }

        Collections.sort(words, TextWord.byAbscissa);

        return new TextLine(words);
    }

    //---------------//
    // mergeRawLines //
    //---------------//
    /**
     * Gather the provided raw lines into long lines, based on their ordinate.
     *
     * @param rawLines collection of raw OCR'ed lines
     * @return resulting long lines
     */
    private List<TextLine> mergeRawLines (List<TextLine> rawLines)
    {
        final List<TextLine> longLines = new ArrayList<>();
        Collections.sort(rawLines, TextLine.byOrdinate(skew));

        final List<TextLine> chunks = new ArrayList<>();
        double lastY = 0;

        for (TextLine line : rawLines) {
            double y = line.getDskOrigin(skew).getY();

            if (chunks.isEmpty()) {
                chunks.add(line);
            } else if ((y - lastY) <= maxLineDy) {
                // Compatible line
                chunks.add(line);
            } else {
                // Non compatible line

                // Complete pending chunks, if any
                if (!chunks.isEmpty()) {
                    longLines.add(mergeChunks(chunks));
                }

                // Start a new collection of chunks
                chunks.clear();
                chunks.add(line);
            }

            lastY = y;
        }

        // Complete pending chunks, if any
        if (!chunks.isEmpty()) {
            longLines.add(mergeChunks(chunks));
        }

        return longLines;
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
                final Rectangle candidateBounds = candidate.getDeskewedCore(skew);

                final Rectangle candidateFatBox = new Rectangle(candidateBounds);
                candidateFatBox.grow(candidate.selectWordGap(scale), maxLineDy);

                TrunksLoop:
                for (TextLine trunk : oldStandards) {
                    if (trunk == current) {
                        break CandidateLoop;
                    }

                    if ((trunk != candidate) && !trunk.isProcessed()) {
                        Rectangle trunkBounds = trunk.getDeskewedCore(skew);

                        if (trunkBounds.intersects(candidateFatBox)) {
                            // Check actual dx between trunk & candidate (trunk may be a chordName)
                            int gap = GeoUtil.xGap(trunkBounds, candidateBounds);

                            if (gap > trunk.selectWordGap(scale)) {
                                continue;
                            }

                            if (candidate.isVip() || trunk.isVip() || logger.isDebugEnabled()) {
                                logger.info("  merging {} into {}", candidate, trunk);
                            }

                            trunk.addWords(candidate.getWords());
                            guessRole(trunk);
                            candidate.setProcessed(true);
                            candidate = trunk;

                            break;
                        }
                    }
                }
            }
        }

        // Remove unavailable lines
        List<TextLine> newStandards = new ArrayList<>();

        for (TextLine line : oldStandards) {
            if (!line.isProcessed()) {
                guessRole(line);
                newStandards.add(line);
            }
        }

        return newStandards;
    }

    //----------------//
    // partitionLines //
    //----------------//
    /**
     * Given two consecutive staves, partition the provided list of lines between those
     * that relate to upper staff and those that relate to lower staff.
     *
     * @param staff1 upper staff
     * @param staff2 lower staff
     * @param lines  list of lines to partition
     * @return the 2 sub-lists
     */
    private Pair<List<TextLine>> partitionLines (Staff staff1,
                                                 Staff staff2,
                                                 List<TextLine> lines)
    {
        final LineInfo staffLine1 = staff1.getLastLine();
        final LineInfo staffLine2 = staff2.getFirstLine();
        final List<TextLine> gutterLines = new ArrayList<>();

        // Pick up only the lines located below upper staff and above lower staff
        for (TextLine line : lines) {
            Point2D center = line.getCenter2D();
            double y1 = staffLine1.yAt(center.getX());
            if (y1 > center.getY()) {
                continue;
            }

            double y2 = staffLine2.yAt(center.getX());
            if (y2 < center.getY()) {
                continue;
            }

            gutterLines.add(line);
        }

        final int margin1 = staff1.getPart().getCoreMargin(BOTTOM);
        final int margin2 = staff2.getPart().getCoreMargin(TOP);
        int bi = findBreak(staffLine1, margin1, staffLine2, margin2, gutterLines);

        if (bi == -1) {
            return null;
        }

        return new Pair<>(gutterLines.subList(0, bi),
                          gutterLines.subList(bi, gutterLines.size()));
    }

    //--------------------//
    // partitionPartLines //
    //--------------------//
    /**
     * Partition lines for all inter-part gutters in the system
     *
     * @param longLines the lines to partition
     */
    private void partitionPartLines (List<TextLine> longLines)
    {
        final List<Part> parts = system.getParts();

        for (int ip = 1; ip < parts.size(); ip++) {
            final Part part1 = parts.get(ip - 1);
            final Staff staff1 = part1.getLastStaff();

            final Part part2 = parts.get(ip);
            final Staff staff2 = part2.getFirstStaff();

            final Pair<Staff> staffPair = new Pair<>(staff1, staff2);
            final Pair<List<TextLine>> linePartitions = partitionLines(staff1, staff2, longLines);

            gutterMap.put(staffPair, linePartitions);
        }
    }

    //-------------//
    // purgeGutter //
    //-------------//
    /**
     * Purge the gutter area located between provided upper system1 and lower system2,
     * of the external lines on provided side.
     *
     * @param lines   the lines to purge
     * @param side    TOP to purge above, BOTTOM to purge below
     * @param system1 upper system
     * @param system2 lower system
     */
    private void purgeGutter (List<TextLine> lines,
                              VerticalSide side,
                              SystemInfo system1,
                              SystemInfo system2)
    {
        // Retrieve lines located in the gutter area
        final Area gutter = new Area(system1.getArea());
        gutter.intersect(system2.getArea());
        final List<TextLine> gutterLines = getGutterLines(lines, gutter);

        final Staff staff1 = system1.getLastStaff();
        final Staff staff2 = system2.getFirstStaff();

        final Pair<List<TextLine>> linesPair = partitionLines(staff1, staff2, gutterLines);

        if (linesPair != null) {
            final List<TextLine> toRemove = (side == TOP) ? linesPair.one : linesPair.two;

            if (logger.isDebugEnabled()) {
                dump("Purged " + side, toRemove, false);
            }

            lines.removeAll(toRemove);
        }
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
        final List<TextLine> validLines = new ArrayList<>();

        for (TextLine line : lines) {
            final Point2D origin = line.getBaseline().getP1();
            boolean inSheetHeader = (system.getId() == 1) && (origin.getY() < system.getFirstStaff()
                    .getFirstLine().yAt(origin.getX()));

            String reason = line.checkValidity(scale, inSheetHeader);

            if (reason != null) {
                line.setProcessed(true);

                if (logger.isDebugEnabled()) {
                    logger.info("  {} {}", reason, line);

                    for (TextWord word : line.getWords()) {
                        logger.debug("    {}", word);
                    }
                }
            } else {
                validLines.add(line);
            }
        }

        return validLines;
    }

    //--------------------//
    // purgeExternalLines //
    //--------------------//
    /**
     * Purge the provided sequence of long lines of the lines that pertain to system(s)
     * above or system(s) below the current system.
     *
     * @param lines the list of lines to purge
     */
    private void purgeExternalLines (List<TextLine> lines)
    {
        final SystemManager systemMgr = sheet.getSystemManager();

        for (VerticalSide side : VerticalSide.values()) {
            for (SystemInfo alien : systemMgr.verticalNeighbors(system, side)) {
                purgeGutter(lines,
                            side,
                            (side == TOP) ? alien : system, // Upper
                            (side == TOP) ? system : alien);// Lower
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
     * <li>For lyrics, a separation character triggers a word split into syllables</li>
     * </ul>
     *
     * @param longLines the lines to process
     * @return the sequence of re-composed lines
     */
    private List<TextLine> recomposeLines (Collection<TextLine> longLines)
    {
        logger.debug("System#{} recomposeLines", system.getId());

        // Separate lyrics and standard (perhaps long) lines, based on their roles
        List<TextLine> standards = new ArrayList<>();
        List<TextLine> lyrics = new ArrayList<>();
        separatePopulations(longLines, standards, lyrics);

        // Process lyrics
        if (!lyrics.isEmpty()) {
            if (!isManual()) {
                lyrics = purgeInvalidLines(lyrics);
            }

            logger.debug("splitWords for lyrics");
            for (TextLine line : lyrics) {
                line.splitWords();
            }
        }

        // Process standards
        if (!standards.isEmpty()) {
            standards = splitStandardLines(standards);

            // Reject invalid standard lines
            if (!isManual()) {
                standards = purgeInvalidLines(standards);
            }

            // Recut standard lines
            standards = mergeStandardLines(standards);
            standards = splitStandardLines(standards);

            // Recut standard words
            logger.debug("recutStandardWords");

            for (TextLine line : standards) {
                line.recutStandardWords();
            }
        }

        // Gather and sort all lines (standard & lyrics)
        List<TextLine> allLines = new ArrayList<>();
        allLines.addAll(lyrics);
        allLines.addAll(standards);
        Collections.sort(allLines, TextLine.byOrdinate(skew));

        // Precisely adjust font size for words (dashes excepted)
        adjustFontSize(allLines);

        return allLines;
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
        final SortedSet<Section> wordSections = new TreeSet<>(Section.byFullAbscissa);
        final CompoundConstructor constructor = new SectionCompound.Constructor(
                sheet.getInterline());

        final int dx = (offset != null) ? offset.x : 0;
        final int dy = (offset != null) ? offset.y : 0;

        for (TextChar charDesc : chars) {
            final Rectangle charBox = charDesc.getBounds();
            final SortedSet<Section> charSections = new TreeSet<>(Section.byFullAbscissa);

            for (Iterator<Section> it = allSections.iterator(); it.hasNext();) {
                // Do we contain a section not (yet) assigned?
                Section section = it.next();

                if (charBox.contains(section.getBounds())) {
                    charSections.add(section);
                    it.remove();
                }
            }

            if (!charSections.isEmpty()) {
                wordSections.addAll(charSections);

                // Register char underlying glyph
                SectionCompound compound = CompoundFactory.buildCompound(charSections, constructor);
                Glyph relGlyph = compound.toGlyph(null);
                Glyph absGlyph = new Glyph(
                        relGlyph.getLeft() + dx,
                        relGlyph.getTop() + dy,
                        relGlyph.getRunTable());
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

                if (line.getRole() == null) {
                    guessRole(line);
                }

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

        final List<TextLine> newStandards = new ArrayList<>();

        for (TextLine line : oldStandards) {
            final int maxAbscissaGap = line.selectWordGap(scale); // TODO: should gap depend on font size?
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
                            guessRole(newLine);

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
                guessRole(newLine);

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

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Scale.Fraction maxLineDy = new Scale.Fraction(
                1.0,
                "Max vertical gap between two line chunks");
    }
}
