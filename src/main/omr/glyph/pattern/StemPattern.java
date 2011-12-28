//----------------------------------------------------------------------------//
//                                                                            //
//                           S t e m P a t t e r n                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.glyph.Evaluation;
import omr.glyph.GlyphEvaluator;
import omr.glyph.GlyphNetwork;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import omr.util.Implement;
import omr.util.Predicate;

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
    private static final Logger logger = Logger.getLogger(StemPattern.class);

    /** Predicate to filter only reliable symbols attached to a stem */
    private static final Predicate<Glyph> reliableStemSymbols = new Predicate<Glyph>() {
        public boolean check (Glyph glyph)
        {
            Shape   shape = glyph.getShape();

            boolean res = glyph.isWellKnown() &&
                          ShapeRange.StemSymbols.contains(shape) &&
                          (shape != Shape.BEAM_HOOK);

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
     * rebuild surrounding glyphs and try to recognize them. If this action does
     * not lead to some recognized symbol, then we restore the stems.
     *
     * @return the number of symbols recognized
     */
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        int                    nb = 0;

        Map<Glyph, Set<Glyph>> badsMap = new HashMap<Glyph, Set<Glyph>>();

        // Collect all undue stems
        List<Glyph>            SuspectedStems = new ArrayList<Glyph>();

        for (Glyph glyph : system.getGlyphs()) {
            if (!glyph.isStem() || glyph.isManualShape() || !glyph.isActive()) {
                Set<Glyph> goods = new HashSet<Glyph>();
                Set<Glyph> bads = new HashSet<Glyph>();
                glyph.getSymbolsBefore(reliableStemSymbols, goods, bads);
                glyph.getSymbolsAfter(reliableStemSymbols, goods, bads);

                if (goods.isEmpty()) {
                    if (logger.isFineEnabled()) {
                        logger.finest("Suspected Stem " + glyph);
                    }

                    SuspectedStems.add(glyph);

                    // Discard "bad" ones
                    badsMap.put(glyph, bads);

                    for (Glyph g : bads) {
                        if (logger.isFineEnabled()) {
                            logger.finest("Deassigning bad glyph " + g);
                        }

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
        List<Glyph>          symbols = new ArrayList<Glyph>();
        final GlyphEvaluator evaluator = GlyphNetwork.getInstance();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == null) {
                Evaluation vote = evaluator.vote(
                    glyph,
                    Grades.patternsMinGrade,
                    system);

                if (vote != null) {
                    glyph.setEvaluation(vote);

                    if (glyph.isWellKnown()) {
                        if (logger.isFineEnabled()) {
                            logger.finest("New symbol " + glyph);
                        }

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
            Glyph   glyph = null;

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

                        if ((shape != null) &&
                            !g.isManualShape() &&
                            !ShapeRange.StemSymbols.contains(shape)) {
                            g.setShape(null);
                        }
                    }
                }
            }
        }

        return nb;
    }
}
