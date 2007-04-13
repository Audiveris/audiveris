//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h I n s p e c t o r                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.ProcessingException;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.math.Circle;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.Implement;
import omr.util.Logger;
import omr.util.Predicate;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>GlyphInspector</code> is dedicated to the inspection of retrieved
 * glyphs, their recognition being usually based on features used by a neural
 * network evaluator.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphInspector
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphInspector.class);

    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final Sheet sheet;

    /** Related glyph builder */
    private final GlyphsBuilder builder;

    /** Underlying lag */
    private final GlyphLag vLag;

    /** Predicate to filter only reliable symbols attached to a stem */
    private final Predicate<Glyph> reliableStemSymbols = new Predicate<Glyph>() {
        public boolean check (Glyph glyph)
        {
            Shape   shape = glyph.getShape();

            boolean res = glyph.isWellKnown() &&
                          Shape.StemSymbols.contains(shape) &&
                          (shape != Shape.BEAM_HOOK);

            return res;
        }
    };


    //~ Constructors -----------------------------------------------------------

    //----------------//
    // GlyphInspector //
    //----------------//
    /**
     * Create an GlyphInspector instance.
     *
     * @param sheet the sheet to inspect
     * @param builder the related glyph builder
     */
    public GlyphInspector (Sheet         sheet,
                           GlyphsBuilder builder)
    {
        this.sheet = sheet;
        this.builder = builder;

        vLag = sheet.getVerticalLag();
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // getCleanupMaxDoubt //
    //--------------------//
    /**
     * Report the maximum doubt for a cleanup
     *
     *
     * @return maximum acceptable doubt value
     */
    public static double getCleanupMaxDoubt ()
    {
        return constants.cleanupMaxDoubt.getValue();
    }

    //-----------------//
    // getLeafMaxDoubt //
    //-----------------//
    /**
     * Report the maximum doubt for a leaf
     *
     *
     * @return maximum acceptable doubt value
     */
    public static double getLeafMaxDoubt ()
    {
        return constants.leafMaxDoubt.getValue();
    }

    //-------------------------//
    // getMinCompoundPartDoubt //
    //-------------------------//
    public static double getMinCompoundPartDoubt ()
    {
        return constants.minCompoundPartDoubt.getValue();
    }

    //-------------------//
    // getSymbolMaxDoubt //
    //-------------------//
    /**
     * Report the maximum doubt for a symbol
     *
     * @return maximum acceptable doubt value
     */
    public static double getSymbolMaxDoubt ()
    {
        return constants.symbolMaxDoubt.getValue();
    }

    //----------------//
    // evaluateGlyphs //
    //----------------//
    /**
     * All new symbol glyphs of the sheet, for which we can get a positive vote
     * of the evaluator, are assigned the voted shape.
     *
     * @param maxDoubt maximum value for acceptable doubt
     */
    public void evaluateGlyphs (double maxDoubt)
    {
        // For temporary use only ...
        if (constants.inspectorDisabled.getValue()) {
            logger.warning(
                "GlyphInspector is disabled. Check Tools|Options menu");

            return;
        }

        Counters counters = new Counters();

        for (SystemInfo system : sheet.getSystems()) {
            evaluateSystemGlyphs(system, maxDoubt, counters);
        }

        if (counters.acceptNb > 0) {
            logger.info(
                counters.acceptNb + " glyph" +
                ((counters.acceptNb > 1) ? "s" : "") + " assigned (" +
                counters.noiseNb + " as noise, " + counters.structureNb +
                " as structure, " + counters.clutterNb + " as clutter, " +
                counters.knownNb + " as known)");
        }
    }

    //----------------------//
    // evaluateSystemGlyphs //
    //----------------------//
    /**
     * All new symbol glyphs of a given system, for which we can get a positive
     * vote of the evaluator, are assigned the voted shape.
     *
     * @param system the system to consider
     * @param maxDoubt maximum value for acceptable doubt
     * @param counters counters to increment, or null
     */
    public void evaluateSystemGlyphs (SystemInfo system,
                                      double     maxDoubt,
                                      Counters   counters)
    {
        Evaluator evaluator = GlyphNetwork.getInstance();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == null) {
                glyph.setInterline(sheet.getScale().interline());

                // Get vote
                Evaluation vote = evaluator.vote(glyph, maxDoubt);

                if (vote != null) {
                    glyph.setShape(vote.shape, vote.doubt);

                    // Increment counters if any
                    if (counters != null) {
                        counters.acceptNb++;

                        if (vote.shape == Shape.NOISE) {
                            counters.noiseNb++;
                        } else if (vote.shape == Shape.CLUTTER) {
                            counters.clutterNb++;
                        } else if (vote.shape == Shape.STRUCTURE) {
                            counters.structureNb++;
                        } else {
                            counters.knownNb++;
                        }
                    }
                }
            }
        }
    }

    //    //---------------//
    //    // processBraces //
    //    //---------------//
    //    /**
    //     * Look for braces and get back to the BarsBuilder to organize the score
    //     * parts (TBD: is it used anymore?)
    //     */
    //    public void processBraces ()
    //    {
    //        // Collect braces recognized, system per system
    //        List<List<Glyph>> braceLists = new ArrayList<List<Glyph>>();
    //
    //        int               nb = 0;
    //
    //        for (SystemInfo system : sheet.getSystems()) {
    //            List<Glyph> braces = new ArrayList<Glyph>();
    //
    //            for (Glyph glyph : system.getGlyphs()) {
    //                if (glyph.getShape() == Shape.BRACE) {
    //                    braces.add(glyph);
    //                    nb++;
    //                }
    //            }
    //
    //            braceLists.add(braces);
    //        }
    //
    //        if (logger.isFineEnabled()) {
    //            logger.fine("Found " + nb + " brace(s)");
    //        }
    //
    //        // Pass this data to the glyph inspector
    //        //        sheet.getBarsBuilder()
    //        //             .setBraces(braceLists);
    //    }

    //-------------------//
    // retrieveCompounds //
    //-------------------//
    /**
     * Look for glyphs portions that should be considered as parts of compound
     * glyphs
     *
     * @param maxDoubt maximum doubt for a compound glyph to be accepted
     */
    public void retrieveCompounds (double maxDoubt)
    {
        List<Glyph> compounds = new ArrayList<Glyph>();

        for (SystemInfo system : sheet.getSystems()) {
            retrieveSystemCompounds(system, compounds, maxDoubt);
        }

        // Feedback to the user
        if (compounds.size() > 1) {
            logger.info(
                "Built " + compounds.size() + " compounds #" +
                compounds.get(0).getId() + " .. #" +
                compounds.get(compounds.size() - 1).getId());
        } else if (compounds.size() == 1) {
            logger.info(
                "Built compound #" + compounds.get(0).getId() + " as " +
                compounds.get(0).getShape());
        } else {
            logger.fine("No compound built");
        }
    }

    //-------------------//
    // retrieveVerticals //
    //-------------------//
    /**
     * Look for vertical sticks (stems actually, though we could have endings
     * verticals as well), and rebuild glyphs after the stem extraction
     */
    public void retrieveVerticals ()
        throws ProcessingException
    {
        sheet.getVerticalsBuilder()
             .retrieveAllVerticals();
    }

    //-------------//
    // tryCompound //
    //-------------//
    /**
     * Try to build a compound, starting from given seed and looking into the
     * collection of suitable glyphs.
     *
     * @param seed the initial glyph around which the compound is built
     * @param suitables collection of potential glyphs
     * @param adapter the specific behavior of the compound tests
     * @return the compound built if successful, null otherwise
     */
    public Glyph tryCompound (Glyph           seed,
                              List<Glyph>     suitables,
                              CompoundAdapter adapter)
    {
        // Build box extended around the seed
        Rectangle   rect = seed.getContourBox();
        Rectangle   box = new Rectangle(
            rect.x - adapter.getBoxDx(),
            rect.y - adapter.getBoxDy(),
            rect.width + (2 * adapter.getBoxDx()),
            rect.height + (2 * adapter.getBoxDy()));

        // Retrieve good neighbors among the suitable glyphs
        List<Glyph> neighbors = new ArrayList<Glyph>();

        // Include the seed in the compound glyphs
        neighbors.add(seed);

        for (Glyph g : suitables) {
            if (!adapter.isSuitable(g)) {
                continue;
            }

            if (box.intersects(g.getContourBox())) {
                neighbors.add(g);
            }
        }

        if (neighbors.size() > 1) {
            if (logger.isFineEnabled()) {
                logger.finest(
                    "neighbors=" + Glyph.toString(neighbors) + " seed=" + seed);
            }

            Glyph compound = builder.buildCompound(neighbors);

            if (adapter.isValid(compound)) {
                builder.insertCompound(compound, neighbors);

                if (logger.isFineEnabled()) {
                    logger.fine("Inserted compound " + compound);
                }

                return compound;
            }
        }

        return null;
    }

    //-------------//
    // verifySlurs //
    //-------------//
    /**
     * Browse through all slur glyphs, run additional checks, and correct
     * spurious glyphs if any
     */
    public void verifySlurs ()
    {
        int fixedNb = 0;

        for (SystemInfo system : sheet.getSystems()) {
            fixedNb += verifySystemSlurs(system);
        }

        // Feedback
        if (fixedNb > 1) {
            logger.info(fixedNb + " slurs fixed");
        } else if (fixedNb > 0) {
            logger.info(fixedNb + " slur fixed");
        } else {
            logger.fine("No slur fixed");
        }
    }

    //-------------//
    // verifyStems //
    //-------------//
    /**
     * Look for all stems that should not be kept, rebuild surrounding glyphs
     * and try to recognize them
     */
    public void verifyStems ()
    {
        int symbolNb = 0;

        for (SystemInfo system : sheet.getSystems()) {
            symbolNb += verifySystemStems(system);
        }

        // User feedback
        if (symbolNb > 1) {
            logger.info(symbolNb + " new symbols from stem cancellation");
        } else if (symbolNb > 0) {
            logger.info(symbolNb + " new symbol from stem cancellation");
        } else if (logger.isFineEnabled()) {
            logger.fine("No new symbol from stem cancellation");
        }
    }

    //-------------------------//
    // retrieveSystemCompounds //
    //-------------------------//
    /**
     * In the specified system, look for glyphs portions that should be
     * considered as parts of compound glyphs
     *
     * @param system the system where splitted glyphs are looked for
     * @param compounds resulting global list of compounds to expend
     * @param maxDoubt maximum doubt value for a compound
     */
    private void retrieveSystemCompounds (SystemInfo  system,
                                          List<Glyph> compounds,
                                          double      maxDoubt)
    {
        builder.removeSystemInactives(system);

        BasicAdapter adapter = new BasicAdapter(maxDoubt);

        // Collect glyphs suitable for participating in compound building
        List<Glyph>  suitables = new ArrayList<Glyph>(
            system.getGlyphs().size());

        for (Glyph glyph : system.getGlyphs()) {
            if (adapter.isSuitable(glyph)) {
                suitables.add(glyph);
            }
        }

        // Sort suitable glyphs by decreasing weight
        Collections.sort(
            suitables,
            new Comparator<Glyph>() {
                    public int compare (Glyph o1,
                                        Glyph o2)
                    {
                        return o2.getWeight() - o1.getWeight();
                    }
                });

        // Now process each seed in turn, by looking at smaller ones
        for (int index = 0; index < suitables.size(); index++) {
            Glyph seed = suitables.get(index);
            adapter.setSeed(seed);

            Glyph compound = tryCompound(
                seed,
                suitables.subList(index + 1, suitables.size()),
                adapter);

            //                if (logger.isFineEnabled()) {
            //                    logger.finest(
            //                        seed.getId() + " " + seed.getShape() + "(" +
            //                        String.format("%.3f", seed.getDoubt()) + ") : " +
            //                        Glyph.idsOf(neighbors) + " -> " + vote);
            //                }
            if (compound != null) {
                compound.setShape(
                    adapter.getVote().shape,
                    adapter.getVote().doubt);
                compounds.add(compound);
            }
        }
    }

    //-------------------//
    // verifySystemSlurs //
    //-------------------//
    /**
     * Process all the slur glyphs in the given system, and try to correct the
     * spurious ones if any
     *
     * @param system the system at hand
     * @return the number of slurs fixed in this system
     */
    private int verifySystemSlurs (SystemInfo system)
    {
        int         fixedNb = 0;
        List<Glyph> oldGlyphs = new ArrayList<Glyph>();
        List<Glyph> newGlyphs = new ArrayList<Glyph>();

        // First, make up a list of all slur glyphs in this system
        // (So as to free the system glyph list for on-the-fly modifications)
        List<Glyph> slurs = new ArrayList<Glyph>();
        builder.removeSystemInactives(system);

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == Shape.SLUR) {
                slurs.add(glyph);
            }
        }

        // Then verify each slur seed in turn
        for (Glyph seed : slurs) {
            // Check this slur has not just been 'merged' with another one
            if (seed.getFirstSection()
                    .getGlyph() != seed) {
                continue;
            }

            Circle circle = SlurGlyph.computeCircle(seed);

            if (!circle.isValid(SlurGlyph.getMaxCircleDistance())) {
                if (SlurGlyph.fixSpuriousSlur(seed, system)) {
                    fixedNb++;
                }
            } else if (logger.isFineEnabled()) {
                logger.finest("Valid slur " + seed.getId());
            }
        }

        // Extract & evaluate brand new glyphs
        builder.extractNewSystemGlyphs(system);

        return fixedNb;
    }

    //-------------------//
    // verifySystemStems //
    //-------------------//
    /**
     * In a specified system, look for all stems that should not be kept,
     * rebuild surrounding glyphs and try to recognize them. If this action does
     * not lead to some recognized symbol, then we restore the stems.
     *
     * @param system the specified system
     * @return the number of symbols recognized
     */
    private int verifySystemStems (SystemInfo system)
    {
        int         nb = 0;

        // Use very close stems to detect sharps and naturals ?
        // TBD
        //
        // Collect all undue stems
        List<Glyph> SuspectedStems = new ArrayList<Glyph>();
        builder.removeSystemInactives(system);

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isStem() && glyph.isActive()) {
                Set<Glyph> goods = new HashSet<Glyph>();
                Set<Glyph> bads = new HashSet<Glyph>();
                glyph.getSymbolsBefore(reliableStemSymbols, goods, bads);
                glyph.getSymbolsAfter(reliableStemSymbols, goods, bads);

                if (goods.size() == 0) {
                    if (logger.isFineEnabled()) {
                        logger.finest("Suspected Stem " + glyph);
                    }

                    SuspectedStems.add(glyph);

                    // Discard "bad" ones
                    for (Glyph g : bads) {
                        g.setShape((Shape) null);
                    }
                }
            }
        }

        // Remove these stems since nearby stems are used for recognition
        for (Glyph glyph : SuspectedStems) {
            builder.removeGlyph(glyph, system, /*cutSections=>*/
                                true);
        }

        // Extract brand new glyphs
        builder.extractNewSystemGlyphs(system);

        // Try to recognize each glyph in turn
        List<Glyph>     symbols = new ArrayList<Glyph>();

        final Evaluator evaluator = GlyphNetwork.getInstance();
        final double    maxDoubt = getCleanupMaxDoubt();

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
                    builder.removeGlyph(
                        glyph,
                        system, /* cutSections => */
                        true);
                }

                // Restore the stem
                builder.insertGlyph(stem, system);
            }
        }

        // Extract brand new glyphs
        builder.extractNewSystemGlyphs(system);

        return nb;
    }

    //~ Inner Interfaces -------------------------------------------------------

    //-----------------//
    // CompoundAdapter //
    //-----------------//
    /**
     * Interface <code>CompoundAdapter</code> provides the needed features for a
     * generic compound building.
     */
    public static interface CompoundAdapter
    {
        /** Extension in abscissa to look for neighbors */
        int getBoxDx ();

        /** Extension in ordinate to look for neighbors */
        int getBoxDy ();

        /**
         * Predicate for a glyph to be a potential part of the building (the
         * location criteria is handled separately)
         */
        boolean isSuitable (Glyph glyph);

        /** Predicate to check the success of the newly built compound */
        boolean isValid (Glyph compound);
    }

    //~ Inner Classes ----------------------------------------------------------

    /** To pass a set of counters back & forth */
    private static class Counters
    {
        int acceptNb = 0;
        int knownNb = 0;
        int noiseNb = 0;
        int clutterNb = 0;
        int structureNb = 0;
    }

    //--------------//
    // BasicAdapter //
    //--------------//
    /**
     * Class <code>BasicAdapter</code> is a CompoundAdapter meant to retrieve
     * all compounds (in a system). It is reusable from one candidate to the
     * other, by using the setSeed() method.
     */
    private class BasicAdapter
        implements CompoundAdapter
    {
        /** Maximum doubt for a compound */
        private final double maxDoubt;

        /** The seed being considered */
        private Glyph seed;

        /** The result of compound evaluation */
        private Evaluation vote;

        public BasicAdapter (double maxDoubt)
        {
            this.maxDoubt = maxDoubt;
        }

        @Implement(CompoundAdapter.class)
        public int getBoxDx ()
        {
            return sheet.getScale()
                        .toPixels(constants.boxWiden);
        }

        @Implement(CompoundAdapter.class)
        public int getBoxDy ()
        {
            return sheet.getScale()
                        .toPixels(constants.boxWiden);
        }

        public void setSeed (Glyph seed)
        {
            this.seed = seed;
        }

        @Implement(CompoundAdapter.class)
        public boolean isSuitable (Glyph glyph)
        {
            return glyph.isActive() &&
                   (!glyph.isKnown() ||
                   (!glyph.isManualShape() &&
                   ((glyph.getShape() == Shape.DOT) ||
                   (glyph.getShape() == Shape.SLUR) ||
                   (glyph.getShape() == Shape.CLUTTER) ||
                   (glyph.getDoubt() >= getMinCompoundPartDoubt()))));
        }

        @Implement(CompoundAdapter.class)
        public boolean isValid (Glyph compound)
        {
            vote = GlyphNetwork.getInstance()
                               .vote(compound, maxDoubt);

            if (vote != null) {
                compound.setShape(vote.shape, vote.doubt);
            }

            return (vote != null) && vote.shape.isWellKnown() &&
                   (vote.shape != Shape.CLUTTER) &&
                   (!seed.isKnown() || (vote.doubt < seed.getDoubt()));
        }

        public Evaluation getVote ()
        {
            return vote;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        Constant.Boolean inspectorDisabled = new Constant.Boolean(
            false,
            "Should we (temporarily) disable glyph recognition?");
        Scale.Fraction   boxWiden = new Scale.Fraction(
            0.15,
            "Box widening to check intersection with compound");
        Evaluation.Doubt cleanupMaxDoubt = new Evaluation.Doubt(
            1.2,
            "Maximum doubt for cleanup phase");
        Evaluation.Doubt leafMaxDoubt = new Evaluation.Doubt(
            1.01,
            "Maximum acceptance doubt for a leaf");
        Evaluation.Doubt symbolMaxDoubt = new Evaluation.Doubt(
            1.0001,
            "Maximum doubt for a symbol");
        Evaluation.Doubt minCompoundPartDoubt = new Evaluation.Doubt(
            1.020,
            "Minimum doubt for a suitable compound part");
    }
}
