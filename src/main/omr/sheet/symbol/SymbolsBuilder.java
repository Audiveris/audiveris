//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S y m b o l s B u i l d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.symbol;

import omr.Main;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphClassifier;
import omr.glyph.GlyphCluster;
import omr.glyph.GlyphLayer;
import omr.glyph.GlyphLink;
import omr.glyph.GlyphNest;
import omr.glyph.Glyphs;
import omr.glyph.Grades;
import omr.glyph.ShapeEvaluator;
import omr.glyph.facets.Glyph;

import omr.math.GeoUtil;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;

import omr.util.Navigable;

import org.jgrapht.Graphs;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
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
    private final ShapeEvaluator evaluator = GlyphClassifier.getInstance();

    /** Companion factory for symbols inters. */
    private final SymbolFactory factory;

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
     * - getSymbolsGlyphs()                             // Retrieve all glyphs usable for symbols
     * - buildGlyphsGraph()                             // Build graph with distances
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
        if (glyph.isVip()) {
            logger.info("VIP evaluateGlyph on glyph#{}", glyph.getId());
        }

        final Point center = glyph.getLocation();
        final Staff closestStaff = system.getClosestStaff(center); // Just an indication!

        if (closestStaff == null) {
            return;
        }

        glyph.setPitchPosition(closestStaff.pitchPositionOf(center));

        Evaluation[] evals = evaluator.evaluate(
                glyph,
                system,
                10, // Or any high number...
                Grades.symbolMinGrade,
                EnumSet.of(ShapeEvaluator.Condition.CHECKED),
                null);

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

    //-----------------//
    // getClusterGraph //
    //-----------------//
    private SimpleGraph<Glyph, GlyphLink> getClusterGraph (Set<Glyph> set,
                                                           SimpleGraph<Glyph, GlyphLink> systemGraph)
    {
        // Make a copy of just the subgraph for this set
        SimpleGraph<Glyph, GlyphLink> clusterGraph = new SimpleGraph<Glyph, GlyphLink>(
                GlyphLink.class);
        Set<GlyphLink> edges = new HashSet<GlyphLink>();

        for (Glyph glyph : set) {
            edges.addAll(systemGraph.edgesOf(glyph));
        }

        Graphs.addAllEdges(clusterGraph, systemGraph, edges);

        return clusterGraph;
    }

    //------------------//
    // getSymbolsGlyphs //
    //------------------//
    /**
     * Report the collection of glyphs as symbols candidates.
     *
     * @return the candidates (ordered by abscissa)
     */
    private List<Glyph> getSymbolsGlyphs (Map<SystemInfo, List<Glyph>> optionalsMap)
    {
        List<Glyph> glyphs = new ArrayList<Glyph>(); // Sorted by abscissa, ordinate, id

        for (Glyph glyph : system.getGlyphs()) {
            if ((glyph.getLayer() == GlyphLayer.SYMBOL) && (glyph.getWeight() >= params.minWeight)) {
                glyphs.add(glyph);
            }
        }

        // Include optional glyphs as well
        List<Glyph> optionals = optionalsMap.get(system);

        if ((optionals != null) && !optionals.isEmpty()) {
            for (Glyph glyph : optionals) {
                if (glyph.getWeight() >= params.minWeight) {
                    glyphs.add(glyph);
                }
            }
        }

        return glyphs;
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

        for (Set<Glyph> set : sets) {
            if (set.size() > 1) {
                // Use just the subgraph for this set
                SimpleGraph<Glyph, GlyphLink> clusterGraph = getClusterGraph(set, systemGraph);

                new GlyphCluster(new SymbolAdapter(clusterGraph)).decompose();
            } else {
                // The set is just an isolated glyph, to be evaluated directly
                Glyph glyph = set.iterator().next();

                if (evaluator.isBigEnough(glyph)) {
                    evaluateGlyph(glyph);
                }
            }
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
                0.03,
                "Minimum weight for glyph consideration");

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

        final int minWeight;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxGap = scale.toPixelsDouble(constants.maxGap);
            maxSymbolWidth = scale.toPixels(constants.maxSymbolWidth);
            maxSymbolHeight = scale.toPixels(constants.maxSymbolHeight);
            minWeight = scale.toPixels(constants.minWeight);

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }

    //---------------//
    // SymbolAdapter //
    //---------------//
    private class SymbolAdapter
            implements GlyphCluster.Adapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Graph of the connected glyphs, with their distance edges if any. */
        private final SimpleGraph<Glyph, GlyphLink> graph;

        private final Scale scale = sheet.getScale();

        //~ Constructors ---------------------------------------------------------------------------
        public SymbolAdapter (SimpleGraph<Glyph, GlyphLink> graph)
        {
            this.graph = graph;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void evaluateGlyph (Glyph glyph)
        {
            SymbolsBuilder.this.evaluateGlyph(glyph);
        }

        @Override
        public List<Glyph> getNeighbors (Glyph part)
        {
            return Graphs.neighborListOf(graph, part);
        }

        @Override
        public GlyphNest getNest ()
        {
            return sheet.getNest();
        }

        @Override
        public List<Glyph> getParts ()
        {
            return new ArrayList<Glyph>(graph.vertexSet());
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

            return evaluator.isBigEnough(normed);
        }
    }
}
