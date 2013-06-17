//----------------------------------------------------------------------------//
//                                                                            //
//                           T e x t P a t t e r n                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.glyph.facets.Glyph;
import omr.glyph.pattern.GlyphPattern;

import omr.lag.Section;

import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code TextPattern} is in charge of updating the structure of
 * text sentences.
 *
 * @author Hervé Bitteur
 */
public class TextPattern
        extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(TextPattern.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Text utility for this system */
    TextBuilder textBuilder = system.getTextBuilder();

    //~ Constructors -----------------------------------------------------------
    //
    //-------------//
    // TextPattern //
    //-------------//
    public TextPattern (
            SystemInfo system)
    {
        super("Text", system);
    }

    //~ Methods ----------------------------------------------------------------
    //
    //------------//
    // runPattern //
    //------------//
    /**
     * Update sentences and text glyphs.
     *
     * @return nothing
     */
    @Override
    public int runPattern ()
    {
        List<TextLine> toRemove = new ArrayList<>();
        // Check each sentence for inactive, non-text glyph, modified value
        // Use a input copy of sentences collection, since it can get modified
        for (TextLine line : new LinkedHashSet<>(system.getSentences())) {
            checkModifiedValue(line);

            purgeWords(line);

            if (line.getWords().isEmpty()) {
                logger.debug("Removing empty line");
                toRemove.add(line);
            }
        }

        system.getSentences().removeAll(toRemove);

        // Look for active text glyphs left over (no word, or no line)
        checkOrphanGlyphs();

        // Global recomposition of all lines
        List<TextLine> lines = textBuilder.recomposeLines(system.getSentences());
        system.getSentences().clear();
        system.getSentences().addAll(lines);

        // Purge lines
        purgeLines(system.getSentences());
        textBuilder.purgeSentences();

        return 0; // Useless
    }

    //-------------------//
    // checkOrphanGlyphs //
    //-------------------//
    /**
     * Look for orphan text glyphs and create the proper TextLine
     * structure.
     * Strategy: create a TextLine for each of these orphan glyphs then
     * look for potential merge with other TextLine instances.
     */
    private void checkOrphanGlyphs ()
    {
        String language = system.getSheet().getPage().getTextParam().getTarget();

        for (Glyph glyph : system.getGlyphs()) {
            if (!isOrphan(glyph)) {
                continue;
            }

            if (glyph.getManualValue() != null) {
                // Build a TextLine/TextWord manually
                TextWord word = TextWord.createManualWord(
                        glyph, glyph.getManualValue());
                glyph.setTextWord(language, word);
                TextLine line = new TextLine(system, Arrays.asList(word));
                List<TextLine> lines = Arrays.asList(line);
                lines = textBuilder.recomposeLines(lines);
                system.getSentences().addAll(lines);
                textBuilder.purgeSentences();
            } else {
                // Use OCR on this glyph
                logger.debug("Orphan text {}", glyph.idString());
                List<TextLine> lines = textBuilder.retrieveOcrLine(glyph,
                        language);
                if (lines != null && !lines.isEmpty()) {
                    lines = textBuilder.recomposeLines(lines);
                    if (!lines.isEmpty()) {
                        textBuilder.mapGlyphs(lines,
                                glyph.getMembers(),
                                language);
                    }
                }
                if (lines == null || lines.isEmpty()) {
                    logger.debug("{} No valid text in {}",
                            system.idString(), glyph.idString());
                    if (!glyph.isManualShape()) {
                        glyph.setShape(null);
                    }
                }
            }
        }
    }

    //----------//
    // isOrphan //
    //----------//
    /**
     * Check whether the provided glyph is a text orphan.
     *
     * @param glyph the glyph to check
     * @return true if orphan
     */
    private boolean isOrphan (Glyph glyph)
    {
        if (glyph == null || !glyph.isText() || !glyph.isActive()) {
            return false;
        }

        TextWord word = glyph.getTextWord();
        if (word == null) {
            return true;
        }

        TextLine line = word.getTextLine();
        if (line == null) {
            return true;
        }

        if (!line.getWords().contains(word)) {
            return true;
        }

        return false;
    }

    //--------------------//
    // checkModifiedValue //
    //--------------------//
    /**
     * Check a TextLine for a modified (manual) value and align the
     * internal structure accordingly.
     *
     * @param line the TextLine to check and update if needed
     */
    private void checkModifiedValue (TextLine line)
    {
        String language = system.getSheet().getPage().getTextParam().getTarget();
        boolean altered = false;
        List<Section> lineSections = new ArrayList<>();

        // Use a copy to avoid concurrent modifs
        List<TextWord> words = new ArrayList<>(line.getWords());
        for (TextWord word : words) {
            Glyph glyph = word.getGlyph();

            if (glyph == null) {
                continue;
            } else {
                lineSections.addAll(glyph.getMembers());
            }

            if (!glyph.isActive() || !glyph.isText()) {
                continue;
            }

            if (!glyph.getTextValue().equals(word.getInternalValue())) {
                // Here the glyph (manual) value has been modified
                altered = true;
                textBuilder.splitWords(Arrays.asList(word), line);
            }
        }

        // Remap glyphs if line has been altered
        if (altered) {
            textBuilder.mapGlyphs(Arrays.asList(line), lineSections, language);
        }
    }

    //------------//
    // purgeWords //
    //------------//
    /**
     * Purge a TextLine of its former words no longer linked to an
     * active text glyph.
     *
     * @param line the TextLine to purge
     */
    private void purgeWords (TextLine line)
    {
        List<TextWord> toRemove = new ArrayList<>();

        for (TextWord word : line.getWords()) {
            Glyph glyph = word.getGlyph();

            if (glyph == null || !glyph.isActive() || !glyph.isText()) {
                logger.debug("Purging word {}", word);
                toRemove.add(word);
            }
        }

        if (!toRemove.isEmpty()) {
            line.removeWords(toRemove);
        }
    }

    //------------//
    // purgeLines //
    //------------//
    /**
     * Remove the merged lines from the provided collection.
     *
     * @param sentences the collection to purge
     */
    private void purgeLines (Set<TextLine> lines)
    {
        for (Iterator<TextLine> it = lines.iterator(); it.hasNext();) {
            TextLine line = it.next();
            if (line.isProcessed()) {
                logger.debug("Purging line {}", line);
                it.remove();
            }
        }
    }
}
