//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S y m b o l s B u i l d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.symbol;

import omr.classifier.Classifier;
import omr.classifier.Evaluation;
import omr.classifier.GlyphClassifier;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphCluster;
import omr.glyph.GlyphLink;
import omr.glyph.Glyphs;
import omr.glyph.Grades;
import omr.glyph.Symbol.Group;

import omr.math.GeoUtil;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;

import omr.sig.inter.Inter;
import omr.sig.inter.SmallChordInter;

import omr.util.Dumping;
import omr.util.Navigable;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class {@code SymbolsBuilder} is in charge, at system level, of retrieving all
 * possible symbols interpretations.
 *
 * @author Hervé Bitteur
 */
public class SymbolsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SymbolsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Shape classifier to use. */
    private final Classifier classifier = GlyphClassifier.getInstance();

    /** Companion factory for symbols inters. */
    private final SymbolFactory factory;

    /** Aras where fine glyphs may be needed. */
    private final List<Rectangle> fineBoxes = new ArrayList<Rectangle>();

    /** Scale-dependent global constants. */
    private final Parameters params;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SymbolsBuilder object.
     *
     * @param system  the dedicated system
     * @param factory the dedicated symbol factory
     */
    public SymbolsBuilder (SystemInfo system,
                           SymbolFactory factory)
    {
        this.system = system;
        this.factory = factory;

        sheet = system.getSheet();

        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // buildSymbols //
    //--------------//
    /**
     * Find all possible interpretations of symbols composed from available system glyphs.
     * <p>
     * <b>Synopsis:</b>
     * <pre>
     * - retrieveFineBoxes()                            // Retrieve areas around small chords
     * - getSymbolsGlyphs()                             // Retrieve all glyphs usable for symbols
     * - buildLinks()                                   // Build graph with distances
     * - processClusters():                             // Group connected glyphs into clusters
     *    + FOREACH cluster of connected glyphs:
     *       + cluster.decompose()                      // Decompose cluster into all subsets
     *       + FOREACH subset process(subset):
     *          - build compound glyph                  // Build one compound glyph per subset
     *          - evaluateGlyph(compound)               // Run shape classifier on compound
     *          - FOREACH acceptable evaluation
     *             + symbolFactory.create(eval, glyph) // Create inter(s) related to evaluation
     * </pre>
     *
     * @param optionalsMap the optional (weak) glyphs per system
     */
    public void buildSymbols (Map<SystemInfo, List<Glyph>> optionalsMap)
    {
        logger.debug("System#{} buildSymbols", system.getId());

        // Identify areas for fine glyphs
        retrieveFineBoxes();

        // Retrieve all candidate glyphs
        List<Glyph> glyphs = getSymbolsGlyphs(optionalsMap);

        // Formalize glyphs relationships in a system-level graph
        final SimpleGraph<Glyph, GlyphLink> systemGraph = Glyphs.buildLinks(glyphs, params.maxGap);

        // Process all sets of connected glyphs
        processClusters(systemGraph);
    }

    //---------------//
    // evaluateGlyph //
    //---------------//
    /**
     * Evaluate a provided glyph and create all acceptable inter instances.
     *
     * @param glyph the glyph to evaluate
     */
    private void evaluateGlyph (Glyph glyph)
    {
        if (glyph.getId() == 0) {
            glyph = sheet.getGlyphIndex().registerOriginal(glyph);
        }

        logger.debug("evaluateGlyph on {}", glyph);

        if (glyph.isVip()) {
            logger.info("VIP evaluateGlyph on {}", glyph);
        }

        final Point center = glyph.getCenter();
        final Staff closestStaff = system.getClosestStaff(center); // Just an indication!

        if (closestStaff == null) {
            return;
        }

        Evaluation[] evals = classifier.evaluate(
                glyph,
                system,
                10, // Or any high number...
                Grades.symbolMinGrade,
                EnumSet.of(Classifier.Condition.CHECKED));

        if (evals.length > 0) {
            // Create one interpretation for each acceptable evaluation
            for (Evaluation eval : evals) {
                try {
                    factory.create(eval, glyph, closestStaff);
                } catch (Exception ex) {
                    logger.warn("Error in glyph evaluation " + ex, ex);
                }
            }
        }
    }

    //------------------//
    // getSymbolsGlyphs //
    //------------------//
    /**
     * Report the collection of glyphs as symbols candidates.
     * <p>
     * Using a too low weight threshold would result in explosion of symbols (and inter-symbol
     * relations), which would be a disaster in terms of resources. However, we need really fine
     * glyphs to detect small flags (slashed or not).
     * <p>
     * So, we define a list of "fine boxes" at system level, which represents the boxes of system
     * small head-chords. And we use a different threshold depending on whether a glyph candidate
     * intersects or not a fine box.
     *
     * @return the candidates (ordered by abscissa)
     */
    private List<Glyph> getSymbolsGlyphs (Map<SystemInfo, List<Glyph>> optionalsMap)
    {
        // Sorted by abscissa, ordinate, id
        List<Glyph> glyphs = new ArrayList<Glyph>();

        for (Glyph glyph : system.getGroupedGlyphs(Group.SYMBOL)) {
            final int weight = glyph.getWeight();

            if (weight >= params.minWeight) {
                glyphs.add(glyph);
            } else if ((weight >= params.minFineWeight) && hitFineBox(glyph)) {
                glyphs.add(glyph);
            }
        }

        // Include optional glyphs as well
        List<Glyph> optionals = optionalsMap.get(system);

        if ((optionals != null) && !optionals.isEmpty()) {
            for (Glyph glyph : optionals) {
                final int weight = glyph.getWeight();

                if (weight >= params.minWeight) {
                    glyphs.add(glyph);
                } else if ((weight >= params.minFineWeight) && hitFineBox(glyph)) {
                    glyphs.add(glyph);
                }
            }
        }

        return glyphs;
    }

    //------------//
    // hitFineBox //
    //------------//
    /**
     * Check whether the provided glyph intersects a fine box.
     *
     * @param glyph the glyph to check
     * @return true if in fine area
     */
    private boolean hitFineBox (Glyph glyph)
    {
        final Rectangle glyphBounds = glyph.getBounds();

        for (Rectangle box : fineBoxes) {
            if (box.intersects(glyphBounds)) {
                return true;
            }
        }

        return false;
    }

    //-----------------//
    // processClusters //
    //-----------------//
    /**
     * Process all clusters of connected glyphs, based on the glyphs graph.
     *
     * @param systemGraph the graph of candidate glyphs, with their mutual distances
     */
    private void processClusters (SimpleGraph<Glyph, GlyphLink> systemGraph)
    {
        // Retrieve all the clusters of glyphs (sets of connected glyphs)
        ConnectivityInspector inspector = new ConnectivityInspector(systemGraph);
        List<Set<Glyph>> sets = inspector.connectedSets();
        logger.debug("sets: {}", sets.size());

        final int interline = sheet.getInterline();

        for (Set<Glyph> set : sets) {
            if (set.size() > 1) {
                // Use just the subgraph for this set
                SimpleGraph<Glyph, GlyphLink> subGraph = GlyphCluster.getSubGraph(set, systemGraph);
                new GlyphCluster(new SymbolAdapter(subGraph), Group.SYMBOL).decompose();
            } else {
                // The set is just an isolated glyph, to be evaluated directly
                Glyph glyph = set.iterator().next();

                if (classifier.isBigEnough(glyph, interline)) {
                    evaluateGlyph(glyph);
                }
            }
        }
    }

    //-------------------//
    // retrieveFineBoxes //
    //-------------------//
    private void retrieveFineBoxes ()
    {
        List<Inter> smallChords = system.getSig().inters(SmallChordInter.class);

        for (Inter inter : smallChords) {
            Rectangle box = inter.getBounds();
            Rectangle fineBox = new Rectangle(
                    box.x + box.width,
                    box.y,
                    params.smallChordMargin,
                    box.height);
            fineBoxes.add(fineBox);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction maxGap = new Scale.Fraction(
                0.75, // 0.5 is a bit too small for fermata - dot distance
                "Maximum distance between two compound parts");

        private final Scale.AreaFraction minWeight = new Scale.AreaFraction(
                0.03, //0.03,
                "Minimum weight for glyph consideration");

        private final Scale.AreaFraction minFineWeight = new Scale.AreaFraction(
                0.006,
                "Minimum weight for glyph consideration in a fine area");

        private final Scale.Fraction smallChordMargin = new Scale.Fraction(
                1,
                "Margin to right side of small chords to ");

        private final Scale.Fraction maxSymbolWidth = new Scale.Fraction(
                4.0,
                "Maximum width for a symbol");

        private final Scale.Fraction maxSymbolHeight = new Scale.Fraction(
                10.0,
                "Maximum height for a symbol (when found within staff abscissa range)");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all pre-scaled constants.
     */
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final double maxGap;

        final int maxSymbolWidth;

        final int maxSymbolHeight;

        final int smallChordMargin;

        final int minWeight;

        final int minFineWeight;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxGap = scale.toPixelsDouble(constants.maxGap);
            maxSymbolWidth = scale.toPixels(constants.maxSymbolWidth);
            maxSymbolHeight = scale.toPixels(constants.maxSymbolHeight);
            smallChordMargin = scale.toPixels(constants.smallChordMargin);
            minWeight = scale.toPixels(constants.minWeight);
            minFineWeight = scale.toPixels(constants.minFineWeight);

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }

    //---------------//
    // SymbolAdapter //
    //---------------//
    private class SymbolAdapter
            extends GlyphCluster.AbstractAdapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale scale = sheet.getScale();

        //~ Constructors ---------------------------------------------------------------------------
        public SymbolAdapter (SimpleGraph<Glyph, GlyphLink> graph)
        {
            super(graph);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void evaluateGlyph (Glyph glyph)
        {
            SymbolsBuilder.this.evaluateGlyph(glyph);
        }

        @Override
        public boolean isSizeAcceptable (Rectangle symBox)
        {
            // Check width
            if (symBox.width > params.maxSymbolWidth) {
                return false;
            }

            // Check height (not limited if on left of system: braces / brackets)
            if (GeoUtil.centerOf(symBox).x < system.getLeft()) {
                return true;
            } else {
                return symBox.height <= params.maxSymbolHeight;
            }
        }

        @Override
        public boolean isWeightAcceptable (int weight)
        {
            double normed = scale.pixelsToAreaFrac(weight);

            return classifier.isBigEnough(normed);
        }
    }
}
