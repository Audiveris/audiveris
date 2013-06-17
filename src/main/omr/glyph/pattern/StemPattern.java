//----------------------------------------------------------------------------//
//                                                                            //
//                           S t e m P a t t e r n                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.glyph.Evaluation;
import omr.glyph.ShapeEvaluator;
import omr.glyph.GlyphNetwork;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.sheet.SystemInfo;

import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class {@code StemPattern} is a GlyphInspector dedicated to the
 * inspection of Stems at System level
 *
 * @author Hervé Bitteur
 */
public class StemPattern
        extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(StemPattern.class);

    /** Predicate to filter reliable symbols attached to a stem. */
    public static final Predicate<Glyph> reliableStemSymbols = new Predicate<Glyph>()
    {
        @Override
        public boolean check (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            boolean res = glyph.isWellKnown()
                          && ShapeSet.StemSymbols.contains(shape)
                          && (shape != Shape.BEAM_HOOK);

            return res;
        }
    };

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new StemPattern object.
     *
     * @param system the dedicated system
     */
    public StemPattern (SystemInfo system)
    {
        super("Stem", system);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    /**
     * In a specified system, look for all stems that should not be kept,
     * rebuild surrounding glyphs and try to recognize them.
     * If this action does not lead to some recognized symbol, then we restore
     * the stems.
     *
     * @return the number of symbols recognized
     */
    @Override
    public int runPattern ()
    {
        int nb = 0;

        Map<Glyph, Set<Glyph>> badsMap = new HashMap<>();

        // Collect all undue stems
        List<Glyph> SuspectedStems = new ArrayList<>();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isStem() && !glyph.isManualShape() && glyph.isActive()) {
                Set<Glyph> goods = new HashSet<>();
                Set<Glyph> bads = new HashSet<>();
                glyph.getSymbolsBefore(reliableStemSymbols, goods, bads);
                glyph.getSymbolsAfter(reliableStemSymbols, goods, bads);

                if (goods.isEmpty()) {
                    logger.debug("Suspected Stem {}", glyph);

                    SuspectedStems.add(glyph);

                    // Discard "bad" ones
                    badsMap.put(glyph, bads);

                    for (Glyph g : bads) {
                        logger.debug("Deassigning bad glyph {}", g);
                        g.setShape((Shape) null);
                    }
                }
            }
        }

        // Remove these stem glyphs since nearby stems are used for recognition
        for (Glyph glyph : SuspectedStems) {
            system.removeGlyph(glyph);
        }

        // Extract brand new glyphs (removeInactiveGlyphs + retrieveGlyphs)
        system.extractNewGlyphs();

        // Try to recognize each glyph in turn
        List<Glyph> symbols = new ArrayList<>();
        final ShapeEvaluator evaluator = GlyphNetwork.getInstance();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == null) {
                Evaluation vote = evaluator.vote(
                        glyph,
                        system,
                        Grades.patternsMinGrade);

                if (vote != null) {
                    glyph.setEvaluation(vote);

                    if (glyph.isWellKnown()) {
                        logger.debug("New symbol {}", glyph);
                        symbols.add(glyph);
                        nb++;
                    }
                }
            }
        }

        // Keep stems that have not been replaced by symbols, definitively
        // remove the others
        for (Glyph stem : SuspectedStems) {
            // Check if one of its section is now part of a symbol
            boolean known = false;
            Glyph glyph = null;

            for (Section section : stem.getMembers()) {
                glyph = section.getGlyph();

                if ((glyph != null) && glyph.isWellKnown()) {
                    known = true;

                    break;
                }
            }

            if (!known) {
                // Remove the newly created glyph
                if (glyph != null) {
                    system.removeGlyph(glyph);
                }

                // Restore the stem
                system.addGlyph(stem);

                // Deassign the nearby glyphs that cannot accept a stem
                Set<Glyph> bads = badsMap.get(stem);

                if (bads != null) {
                    for (Glyph g : bads) {
                        Shape shape = g.getShape();

                        if ((shape != null)
                            && !g.isManualShape()
                            && !ShapeSet.StemSymbols.contains(shape)) {
                            g.setShape(null);
                        }
                    }
                }
            } else if (stem.isVip()) {
                logger.info("StemPattern deassigned stem#{}", stem.getId());
            }
        }

        return nb;
    }
}
