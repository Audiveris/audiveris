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
import omr.glyph.Shape;
import omr.glyph.ShapeEvaluator;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.image.ChamferDistance;
import omr.image.DistanceTable;

import omr.lag.Section;

import omr.math.GeoOrder;
import omr.math.LineUtil;

import omr.run.Orientation;
import static omr.run.Orientation.VERTICAL;
import omr.run.Run;

import omr.sig.AccidentalInter;
import omr.sig.BraceInter;
import omr.sig.ClefInter;
import omr.sig.FingeringInter;
import omr.sig.FlagInter;
import omr.sig.FlagStemRelation;
import omr.sig.Inter;
import omr.sig.NumberInter;
import omr.sig.RestInter;
import omr.sig.SIGraph;
import omr.sig.StemPortion;

import omr.util.Navigable;

import org.jgrapht.Graphs;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
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

    /** The related SIG. */
    private final SIGraph sig;

    /** Shape classifier to use. */
    private final ShapeEvaluator evaluator = GlyphClassifier.getInstance();

    /** Scale-dependent global constants. */
    private final Parameters params;

    /** All system stems, ordered by abscissa. */
    private final List<Inter> systemStems;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BeamsBuilder object.
     *
     * @param system the dedicated system
     */
    public SymbolsBuilder (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();
        sheet = system.getSheet();
        params = new Parameters(sheet.getScale());

        systemStems = sig.inters(Shape.STEM);
        Collections.sort(systemStems, Inter.byAbscissa);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // buildSymbols //
    //--------------//
    /**
     * Find possible interpretations of symbols among system glyphs.
     */
    public void buildSymbols ()
    {
        logger.info("System#{} symbols", system.getId());

        List<Glyph> glyphs = getSymbolsGlyphs();

        buildClusters(glyphs);
    }

    //---------------//
    // buildClusters //
    //---------------//
    /**
     * Build all clusters of connectable glyphs, using glyph-to-glyph distance.
     *
     * @param glyphs the list of leaf glyph instances (sorted by abscissa)
     */
    private void buildClusters (List<Glyph> glyphs)
    {
        /** Graph of glyphs, linked by their distance. */
        SimpleGraph<Glyph, Distance> systemGraph = new SimpleGraph<Glyph, Distance>(Distance.class);
        final int gapInt = (int) Math.ceil(params.maxGap);

        for (Glyph glyph : glyphs) {
            systemGraph.addVertex(glyph);
        }

        for (int i = 0; i < glyphs.size(); i++) {
            Glyph glyph = glyphs.get(i);
            Rectangle fatBox = glyph.getBounds();
            fatBox.grow(gapInt, gapInt);

            final int xBreak = fatBox.x + fatBox.width;
            DistanceTable distTable = null;

            for (Glyph other : glyphs.subList(i + 1, glyphs.size())) {
                Rectangle otherBox = other.getBounds();

                // Rough filtering, using fat box intersection
                if (!fatBox.intersects(otherBox)) {
                    continue;
                } else if (otherBox.x > xBreak) {
                    break;
                }

                // We need a distance table around the glyph
                if (distTable == null) {
                    distTable = new GlyphDistance().computeDistances(fatBox, glyph);
                }

                // Precise distance from glyph to other
                double dist = measureDistance(fatBox, distTable, other);

                if (dist <= params.maxGap) {
                    systemGraph.addEdge(glyph, other, new Distance(dist));

                    //                    logger.info(
                    //                            "#{} to #{} = {}",
                    //                            glyph.getId(),
                    //                            other.getId(),
                    //                            String.format("%.1f", dist));
                }
            }
        }

        // Retrieve the components (sets of connected vertices)
        ConnectivityInspector inspector = new ConnectivityInspector(systemGraph);
        List<Set<Glyph>> sets = inspector.connectedSets();

        logger.info("sets: {}", sets.size());

        for (Set<Glyph> set : sets) {
            List<Glyph> list = new ArrayList<Glyph>(set);
            Collections.sort(list, Glyph.byId);

            if (set.size() > 1) {
                // Consider decompositions of this set
                SimpleGraph<Glyph, Distance> compGraph = new SimpleGraph<Glyph, Distance>(
                        Distance.class);
                Set<Distance> edges = new HashSet<Distance>();

                for (Glyph glyph : set) {
                    edges.addAll(systemGraph.edgesOf(glyph));
                }

                Graphs.addAllEdges(compGraph, systemGraph, edges);
                new Cluster(compGraph).decompose();
            } else {
                // Isolated glyph, to be evaluated directly
                evaluateGlyph(set.iterator().next());
            }
        }
    }

    //--------------//
    // createInters //
    //--------------//
    /**
     * Create the proper inter instance(s) for the provided evaluated glyph.
     *
     * @param eval  evaluation detail
     * @param glyph evaluated glyph
     * @param staff related staff
     * @return the collection of created instance(s)
     */
    private List<? extends Inter> createInters (Evaluation eval,
                                                Glyph glyph,
                                                StaffInfo staff)
    {
        final Shape shape = eval.shape;
        final double grade = Inter.intrinsicRatio * eval.grade;

        if (glyph.isVip()) {
            logger.info("glyph#{} {}", glyph.getId(), eval.shape);
        }

        if (ShapeSet.Clefs.contains(shape)) {
            return Arrays.asList(ClefInter.create(shape, glyph, grade, staff));
        } else if (ShapeSet.Rests.contains(shape)) {
            return Arrays.asList(RestInter.create(shape, glyph, grade));
        } else if (ShapeSet.Accidentals.contains(shape)) {
            return Arrays.asList(AccidentalInter.create(shape, glyph, grade));
        } else if (ShapeSet.Flags.contains(shape)) {
            FlagInter flagInter = FlagInter.create(shape, glyph, grade);
            detectFlagStemRelation(flagInter);

            return Arrays.asList(flagInter);
        } else if ((shape == Shape.BRACE) || (shape == Shape.BRACKET)) {
            return Arrays.asList(BraceInter.create(shape, glyph, grade));
        } else if (ShapeSet.PartialTimes.contains(shape)) {
            return Arrays.asList(NumberInter.create(shape, glyph, grade));
        } else if (ShapeSet.FullTimes.contains(shape)) {
            //            List<Inter> nd = TimeInter.create(shape, glyph, grade);
            //
            //            if (nd.size() > 1) {
            //                for (Inter inter : nd) {
            //                    sig.addVertex(inter);
            //                }
            //
            //                for (int i = 0; i < nd.size(); i++) {
            //                    Inter inter = nd.get(i);
            //
            //                    for (Inter other : nd.subList(i + 1, nd.size())) {
            //                        sig.addEdge(inter, other, new BasicSupport());
            //                    }
            //                }
            //            }
        } else if (ShapeSet.Digits.contains(shape)) {
            return Arrays.asList(FingeringInter.create(shape, glyph, grade));
        }

        return null;
    }

    //------------------------//
    // detectFlagStemRelation //
    //------------------------//
    /**
     * Detect Flag/Stem adjacency for the provided flag and thus mutual support
     * (rather than exclusion).
     *
     * @param flag the provided flag
     */
    private void detectFlagStemRelation (FlagInter flag)
    {
        // Look for stems nearby, using the lowest (for up) or highest (for down) third of height
        Shape shape = flag.getShape();
        boolean isUp = ShapeSet.FlagsUp.contains(shape);
        int stemWidth = sheet.getScale().getMainStem();
        Rectangle flagBox = flag.getBounds();
        int height = (int) Math.rint(flagBox.height / 3.0);
        int y = isUp ? ((flagBox.y + flagBox.height) - height - params.maxStemFlagGapY)
                : (flagBox.y + params.maxStemFlagGapY);

        //TODO: -1 is used to cope with stem margin when erased (To be improved)
        Rectangle box = new Rectangle((flagBox.x - 1) - stemWidth, y, stemWidth, height);

        // We need a flag ref point to compute x and y distances to stem
        Glyph glyph = flag.getGlyph();
        Section section = glyph.getFirstSection();
        Point refPt = new Point(
                flagBox.x,
                isUp ? section.getStartCoord() : section.getStopCoord());
        int midFlagY = (section.getStartCoord() + section.getStopCoord()) / 2;
        glyph.addAttachment("fs", box);

        Scale scale = sheet.getScale();
        List<Inter> stems = sig.intersectedInters(systemStems, GeoOrder.BY_ABSCISSA, box);

        for (Inter stem : stems) {
            Glyph stemGlyph = stem.getGlyph();
            Point2D start = stemGlyph.getStartPoint(VERTICAL);
            Point2D stop = stemGlyph.getStopPoint(VERTICAL);
            Point2D crossPt = LineUtil.intersectionAtY(start, stop, refPt.getY());
            double xGap = refPt.getX() - crossPt.getX();
            double yGap;

            if (refPt.getY() < start.getY()) {
                yGap = start.getY() - refPt.getY();
            } else if (refPt.getY() > stop.getY()) {
                yGap = refPt.getY() - stop.getY();
            } else {
                yGap = 0;
            }

            FlagStemRelation fRel = new FlagStemRelation();
            fRel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

            if (fRel.getGrade() >= fRel.getMinGrade()) {
                // Determine and check stem portion
                //TODO: this may be too strict, STEM_MIDDLE can happen with stack of flags?
                double midStemY = (start.getY() + stop.getY()) / 2;

                if (isUp) {
                    if (midFlagY > midStemY) {
                        fRel.setStemPortion(StemPortion.STEM_BOTTOM);
                    } else {
                        continue;
                    }
                } else {
                    if (midFlagY < midStemY) {
                        fRel.setStemPortion(StemPortion.STEM_TOP);
                    } else {
                        continue;
                    }
                }

                sig.addVertex(flag);
                sig.addEdge(flag, stem, fRel);
            }
        }
    }

    //---------------//
    // evaluateGlyph //
    //---------------//
    /**
     * Evaluate a provided glyph and create all acceptable inter instances.
     *
     * @param glyph the glyph to evaluate
     * @return the collection of inters created
     */
    private List<Inter> evaluateGlyph (Glyph glyph)
    {
        if (glyph.isVip()) {
            logger.info("VIP buildSymbols on glyph#{}", glyph.getId());
        }

        final List<Inter> allInters = new ArrayList<Inter>();
        final Point center = glyph.getLocation();
        final StaffInfo staff = system.getStaffAt(center);
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
                List<? extends Inter> inters = createInters(eval, glyph, staff);

                if ((inters != null) && !inters.isEmpty()) {
                    allInters.addAll(inters);

                    for (Inter inter : inters) {
                        sig.addVertex(inter);
                    }
                }
            }

            //            // Mutual exclusions between inters of the same glyph
            //            if (allInters.size() > 1) {
            //                sig.insertExclusions(allInters, Exclusion.Cause.OVERLAP);
            //            }
        }

        return allInters;
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
     * Any two subsets are mutually exclusive if they have a glyph in common.
     */
    private class Cluster
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The graph of the connected glyphs, with their distance edges. */
        private final SimpleGraph<Glyph, Distance> graph;

        /** Map (seed -> inters that involve this seed). */
        private final Map<Glyph, Set<Inter>> seedInters = new HashMap<Glyph, Set<Inter>>();

        //~ Constructors ---------------------------------------------------------------------------
        public Cluster (SimpleGraph<Glyph, Distance> graph)
        {
            this.graph = graph;
        }

        //~ Methods --------------------------------------------------------------------------------
        public void decompose ()
        {
            List<Glyph> seeds = new ArrayList<Glyph>(graph.vertexSet());
            Collections.sort(seeds, Glyph.byId);

            logger.info("Decomposing {}", Glyphs.toString("cluster", seeds));

            Set<Glyph> forbidden = new HashSet<Glyph>();

            for (Glyph seed : seeds) {
                if (seed.isVip()) {
                    logger.info("   Seed #{}", seed.getId());
                }

                process(Collections.singleton(seed), forbidden);
                forbidden.add(seed);
            }

            // Formalized exclusion between subsets that have a glyph in common
            //            for (Glyph seed : seeds) {
            //                sig.insertExclusions(
            //                        new ArrayList<Inter>(seedInters.get(seed)),
            //                        Exclusion.Cause.OVERLAP);
            //            }
        }

        private Set<Glyph> getOutliers (Set<Glyph> set)
        {
            Set<Glyph> outliers = new HashSet<Glyph>();

            for (Glyph glyph : set) {
                outliers.addAll(Graphs.neighborListOf(graph, glyph));
            }

            outliers.removeAll(set);

            return outliers;
        }

        private void process (Set<Glyph> set,
                              Set<Glyph> forbidden)
        {
            ///logger.info("      Processing {}", Glyphs.toString(set));

            // Build compound and get acceptable evaluations for the compound
            Glyph compound = (set.size() == 1) ? set.iterator().next()
                    : sheet.getNest()
                    .buildGlyph(set, true, Glyph.Linking.NO_LINK);
            Collection<Inter> inters = evaluateGlyph(compound);

            // Flag exclusions
            for (Glyph glyph : set) {
                Set<Inter> involved = seedInters.get(glyph);

                if (involved == null) {
                    seedInters.put(glyph, involved = new HashSet<Inter>());
                }

                involved.addAll(inters);
            }

            // Reachable outliers
            Set<Glyph> outliers = getOutliers(set);
            outliers.removeAll(forbidden);

            if (outliers.isEmpty()) {
                return;
            }

            Set<Glyph> newForbidden = new HashSet<Glyph>();
            newForbidden.addAll(forbidden);

            for (Glyph outlier : outliers) {
                Set<Glyph> larger = new HashSet<Glyph>();
                larger.addAll(set);
                larger.add(outlier);
                process(larger, newForbidden);
                newForbidden.add(outlier);
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
         * Typical gap is given by F-key, often segmented by upper staff line into left and right
         * parts, but perhaps this case alone could be handled specifically thanks to the two dots.
         */
        private final Scale.Fraction maxGap = new Scale.Fraction(
                0.5, //1.0,
                "Maximum distance between two compound parts");

        private final Scale.AreaFraction minWeight = new Scale.AreaFraction(
                0.03,
                "Minimum weight for glyph consideration");
    }

    //----------//
    // Distance //
    //----------//
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
    private static class GlyphDistance
            extends ChamferDistance.Short
    {
        //~ Constructors ---------------------------------------------------------------------------

        ///private int normalizer;
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

        final int minWeight;

        final int maxStemFlagGapY;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxGap = scale.toPixelsDouble(constants.maxGap);
            minWeight = scale.toPixels(constants.minWeight);
            maxStemFlagGapY = scale.toPixels(FlagStemRelation.getYGapMaximum());

            Main.dumping.dump(this);
        }
    }
}
