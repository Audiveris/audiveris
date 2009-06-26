//----------------------------------------------------------------------------//
//                                                                            //
//                         S t e m I n s p e c t o r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import omr.util.Predicate;

import java.util.*;

/**
 * Class <code>StemInspector</code> is a GlyphInspector dedicated to the
 * inspection of Stems at System level
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class StemInspector
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(StemInspector.class);

    //~ Instance fields --------------------------------------------------------

    /** Dedicated system */
    private final SystemInfo system;

    /** Predicate to filter only reliable symbols attached to a stem */
    private final Predicate<Glyph> reliableStemSymbols = new Predicate<Glyph>() {
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
     * Creates a new StemInspector object.
     *
     * @param system the dedicated system
     */
    public StemInspector (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // runStemPattern //
    //----------------//
    /**
     * In a specified system, look for all stems that should not be kept,
     * rebuild surrounding glyphs and try to recognize them. If this action does
     * not lead to some recognized symbol, then we restore the stems.
     *
     * @return the number of symbols recognized
     */
    public int runStemPattern ()
    {
        int         nb = 0;

        // Collect all undue stems
        List<Glyph> SuspectedStems = new ArrayList<Glyph>();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isStem() && glyph.isActive()) {
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
        List<Glyph>     symbols = new ArrayList<Glyph>();
        final GlyphEvaluator evaluator = GlyphNetwork.getInstance();
        final double    maxDoubt = GlyphInspector.getPatternsMaxDoubt();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == null) {
                Evaluation vote = evaluator.vote(glyph, maxDoubt);

                if (vote != null) {
                    glyph.setShape(vote.shape, vote.doubt);

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

            for (GlyphSection section : stem.getMembers()) {
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
            }
        }

        return nb;
    }
}
