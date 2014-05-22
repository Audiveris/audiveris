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
package omr.sheet;

import omr.Main;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphClassifier;
import omr.glyph.GlyphLayer;
import omr.glyph.Glyphs;
import omr.glyph.Grades;
import omr.glyph.ShapeEvaluator;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.image.ChamferDistance;
import omr.image.DistanceTable;

import omr.lag.Section;

import omr.math.GeoUtil;

import omr.run.Orientation;
import omr.run.Run;

import omr.util.Navigable;

import org.jgrapht.Graphs;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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

    /** Just for feedback. */
    int processCount = 0;

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
     */
    public void buildSymbols ()
    {
        logger.debug("System#{} buildSymbols", system.getId());

        // Retrieve all candidate glyphs
        List<Glyph> glyphs = getSymbolsGlyphs();

        // Formalize glyphs relationships in a system-level graph
        SimpleGraph<Glyph, Distance> systemGraph = buildGlyphsGraph(glyphs);

        // Process all sets of connected glyphs
        processClusters(systemGraph);

        logger.debug("System#{} symbols processed: {}", system.getId(), processCount);
    }

    //------------------//
    // buildGlyphsGraph //
    //------------------//
    /**
     * Build the graph of glyphs candidates, linked using glyph-to-glyph distances.
     *
     * @param glyphs the list of leaf glyph instances (sorted by abscissa)
     * @return the graph of candidate glyphs
     */
    private SimpleGraph<Glyph, Distance> buildGlyphsGraph (List<Glyph> glyphs)
    {
        final int dmzEnd = system.getFirstStaff().getDmzEnd();

        /** Graph of glyphs, linked by their distance. */
        SimpleGraph<Glyph, Distance> systemGraph = new SimpleGraph<Glyph, Distance>(Distance.class);

        // Populate all glyphs as graph vertices
        for (Glyph glyph : glyphs) {
            systemGraph.addVertex(glyph);
        }

        // Populate edges (glyph to glyph distances) when applicable
        for (int i = 0; i < glyphs.size(); i++) {
            final Glyph glyph = glyphs.get(i);

            // Choose appropriate maxGap depending on whether glyph is in DMZ or not
            DistanceTable distTable = null; // Glyph-centered distance table
            final double maxGap = (glyph.getLocation().x <= dmzEnd) ? params.maxDmzGap
                    : params.maxGap;
            final int gapInt = (int) Math.ceil(maxGap);
            final Rectangle fatBox = glyph.getBounds();
            fatBox.grow(gapInt, gapInt);

            final int xBreak = fatBox.x + fatBox.width; // Glyphs are sorted by abscissa

            for (Glyph other : glyphs.subList(i + 1, glyphs.size())) {
                Rectangle otherBox = other.getBounds();

                // Rough filtering, using fat box intersection
                if (!fatBox.intersects(otherBox)) {
                    continue;
                } else if (otherBox.x > xBreak) {
                    break;
                }

                // We now need the glyph distance table, if not yet computed
                if (distTable == null) {
                    distTable = new GlyphDistance().computeDistances(fatBox, glyph);
                }

                // Precise distance from glyph to other
                double dist = measureDistance(fatBox, distTable, other);

                if (dist <= maxGap) {
                    systemGraph.addEdge(glyph, other, new Distance(dist));
                }
            }
        }

        return systemGraph;
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
            logger.info("VIP buildSymbols on glyph#{}", glyph.getId());
        }

        final Point center = glyph.getLocation();
        final StaffInfo staff = system.getStaffAt(center);

        if (staff == null) {
            return;
        }

        glyph.setPitchPosition(staff.pitchPositionOf(center));

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
                    factory.create(eval, glyph, staff);
                } catch (Exception ex) {
                    logger.warn("Error in glyph evaluation " + ex, ex);
                }
            }
        }
    }

    //-----------------//
    // getClusterGraph //
    //-----------------//
    private SimpleGraph<Glyph, Distance> getClusterGraph (Set<Glyph> set,
                                                          SimpleGraph<Glyph, Distance> systemGraph)
    {
        // Make a copy of just the subgraph for this set
        SimpleGraph<Glyph, Distance> clusterGraph = new SimpleGraph<Glyph, Distance>(
                Distance.class);
        Set<Distance> edges = new HashSet<Distance>();

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
    private List<Glyph> getSymbolsGlyphs ()
    {
        List<Glyph> glyphs = new ArrayList<Glyph>(); // Sorted by abscissa, ordinate, id

        for (Glyph glyph : system.getGlyphs()) {
            if ((glyph.getLayer() == GlyphLayer.SYMBOL)
                && (glyph.getShape() == null)
                && (glyph.getWeight() >= params.minWeight)) {
                glyphs.add(glyph);
            }
        }

        // Include optional glyphs as well
        List<Glyph> optionals = system.getOptionalGlyphs();

        if ((optionals != null) && !optionals.isEmpty()) {
            for (Glyph glyph : optionals) {
                if (glyph.getWeight() >= params.minWeight) {
                    glyphs.add(glyph);
                }
            }

            Collections.sort(glyphs, Glyph.byAbscissa);
        }

        return glyphs;
    }

    //-----------------//
    // measureDistance //
    //-----------------//
    /**
     * Measure the minimum distance between glyph and other glyph.
     *
     * @param box       table bounds
     * @param distTable distance table around the glyph
     * @param other     the other glyph
     * @return minimum distance
     */
    private double measureDistance (Rectangle box,
                                    DistanceTable distTable,
                                    Glyph other)
    {
        int bestDist = Integer.MAX_VALUE;

        for (Section section : other.getMembers()) {
            Orientation orientation = section.getOrientation();
            int p = section.getFirstPos();

            for (Run run : section.getRuns()) {
                final int start = run.getStart();

                for (int ic = run.getLength() - 1; ic >= 0; ic--) {
                    final int x = (orientation == Orientation.HORIZONTAL) ? (start + ic) : p;
                    final int y = (orientation == Orientation.HORIZONTAL) ? p : (start + ic);

                    if (box.contains(x, y)) {
                        int dist = distTable.getValue(x - box.x, y - box.y);

                        if (dist < bestDist) {
                            bestDist = dist;
                        }
                    }
                }

                p++;
            }
        }

        return (double) bestDist / distTable.getNormalizer();
    }

    //-----------------//
    // processClusters //
    //-----------------//
    /**
     * Process all clusters of connected glyphs, based on the glyphs graph.
     *
     * @param systemGraph the graph of candidate glyphs, with their mutual distances
     */
    private void processClusters (SimpleGraph<Glyph, Distance> systemGraph)
    {
        // Retrieve all the clusters of glyphs (sets of connected glyphs)
        ConnectivityInspector inspector = new ConnectivityInspector(systemGraph);
        List<Set<Glyph>> sets = inspector.connectedSets();
        logger.debug("sets: {}", sets.size());

        for (Set<Glyph> set : sets) {
            if (set.size() > 1) {
                // Use just the subgraph for this set
                SimpleGraph<Glyph, Distance> clusterGraph = getClusterGraph(set, systemGraph);

                new Cluster(clusterGraph).decompose();
            } else {
                // The set is just an isolated glyph, to be evaluated directly
                evaluateGlyph(set.iterator().next());
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Cluster //
    //---------//
    /**
     * Handles a cluster of connected glyphs, to retrieve all acceptable compounds built
     * on subsets of these glyphs.
     * <p>
     * The processing of any given subset consists in the following:<ol>
     * <li>Build the compound of chosen vertices, and record acceptable evaluations.</li>
     * <li>Build the set of new reachable vertices.</li>
     * <li>For each reachable vertex, recursively process the new set composed of current set + the
     * reachable vertex.</li></ol>
     */
    private class Cluster
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Graph of the connected glyphs, with their distance edges if any. */
        private final SimpleGraph<Glyph, Distance> graph;

        //~ Constructors ---------------------------------------------------------------------------
        public Cluster (SimpleGraph<Glyph, Distance> graph)
        {
            this.graph = graph;
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Identify all acceptable compounds within the cluster and evaluate them.
         */
        public void decompose ()
        {
            final Set<Glyph> used = new HashSet<Glyph>(); // Glyphs used so far
            final List<Glyph> seeds = new ArrayList<Glyph>(graph.vertexSet());
            Collections.sort(seeds, Glyph.byId); // It's easier to debug
            logger.debug("Decomposing {}", Glyphs.toString("cluster", seeds));

            for (Glyph seed : seeds) {
                if (seed.isVip()) {
                    logger.info("   Seed #{}", seed.getId());
                }

                used.add(seed);
                process(Collections.singleton(seed), used);
            }
        }

        /**
         * Retrieve all glyphs at acceptable distance from at least one member of the
         * provided set.
         *
         * @param set the provided set
         * @return all the glyphs reachable from the set
         */
        private Set<Glyph> getOutliers (Set<Glyph> set)
        {
            Set<Glyph> outliers = new HashSet<Glyph>();

            for (Glyph glyph : set) {
                outliers.addAll(Graphs.neighborListOf(graph, glyph));
            }

            outliers.removeAll(set);

            return outliers;
        }

        /**
         * Check whether symbol size is acceptable.
         *
         * @param symBox symbol bounding box
         * @return true if OK
         */
        private boolean isSizeAcceptable (Rectangle symBox)
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

        /**
         * Process the provided set of glyphs.
         *
         * @param set  the current glyphs
         * @param used
         */
        private void process (Set<Glyph> set,
                              Set<Glyph> used)
        {
            processCount++;

            // Build compound and get acceptable evaluations for the compound
            Glyph compound = (set.size() == 1) ? set.iterator().next()
                    : sheet.getNest().buildGlyph(set, true, Glyph.Linking.NO_LINK);

            // Create all acceptable inters, if any, for the compound
            evaluateGlyph(compound);

            // Identify all outliers immediately reachable from the compound
            Set<Glyph> outliers = getOutliers(set);
            outliers.removeAll(used);

            if (outliers.isEmpty()) {
                return; // No further growth is possible
            }

            Rectangle setBox = Glyphs.getBounds(set);
            Set<Glyph> newUsed = new HashSet<Glyph>(used);

            for (Glyph outlier : outliers) {
                newUsed.add(outlier);

                // Check appending this atom does not make the resulting symbol too wide or too high
                Rectangle symBox = outlier.getBounds().union(setBox);

                if (isSizeAcceptable(symBox)) {
                    Set<Glyph> largerSet = new HashSet<Glyph>(set);
                    largerSet.add(outlier);
                    process(largerSet, newUsed);
                }
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        /**
         * Value for this maximum distance is key.
         * Accepting large gaps would quickly turn into an explosion of subset possibilities.
         * Use DMZ for relaxed constraints (case of F clefs split by top staff line).
         * Use distance from staff for more text-oriented distances?
         */
        private final Scale.Fraction maxGap = new Scale.Fraction(
                0.5,
                "Maximum distance between two compound parts");

        private final Scale.Fraction maxDmzGap = new Scale.Fraction(
                1.0,
                "Maximum distance between two compound parts, when in DMZ");

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

    //----------//
    // Distance //
    //----------//
    /**
     * Class to formalize an acceptable distance between two glyphs.
     * A simple Double could not be used, because auto-boxing may try to reuse instances.
     */
    private static class Distance
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final double value;

        //~ Constructors ---------------------------------------------------------------------------
        public Distance (double value)
        {
            this.value = value;
        }
    }

    //---------------//
    // GlyphDistance //
    //---------------//
    /**
     * Handles the distances in the vicinity of a glyph.
     */
    private static class GlyphDistance
            extends ChamferDistance.Short
    {
        //~ Constructors ---------------------------------------------------------------------------

        public GlyphDistance ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        public DistanceTable computeDistances (Rectangle box,
                                               Glyph glyph)
        {
            DistanceTable output = allocateOutput(box.width, box.height, 3);

            // Initialize with glyph data (0 for glyph, -1 for other pixels)
            output.fill(-1);

            for (Section section : glyph.getMembers()) {
                Orientation orientation = section.getOrientation();
                int p = section.getFirstPos();

                for (Run run : section.getRuns()) {
                    final int start = run.getStart();

                    for (int ic = run.getLength() - 1; ic >= 0; ic--) {
                        if (orientation == Orientation.HORIZONTAL) {
                            output.setValue((start + ic) - box.x, p - box.y, 0);
                        } else {
                            output.setValue(p - box.x, (start + ic) - box.y, 0);
                        }
                    }

                    p++;
                }
            }

            process(output);

            return output;
        }
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

        final double maxDmzGap;

        final int maxSymbolWidth;

        final int maxSymbolHeight;

        final int minWeight;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxGap = scale.toPixelsDouble(constants.maxGap);
            maxDmzGap = scale.toPixelsDouble(constants.maxDmzGap);
            maxSymbolWidth = scale.toPixels(constants.maxSymbolWidth);
            maxSymbolHeight = scale.toPixels(constants.maxSymbolHeight);
            minWeight = scale.toPixels(constants.minWeight);

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }
}
