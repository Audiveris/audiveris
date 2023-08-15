//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S y m b o l s B u i l d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sheet.symbol;

import org.audiveris.omr.classifier.Classifier;
import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.classifier.ShapeClassifier;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphCluster;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.glyph.GlyphLink;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.relation.Exclusion;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.StopWatch;

import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class <code>SymbolsBuilder</code> is in charge, at system level, of retrieving all
 * possible symbols interpretations.
 *
 * @author Hervé Bitteur
 */
public class SymbolsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SymbolsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Shape classifier to use. */
    private final Classifier classifier = ShapeClassifier.getInstance();

    //    /** Shape second classifier to use. */
    //    private final Classifier classifier2 = ShapeClassifier.getSecondInstance();
    //
    /** Companion factory for symbols inters. */
    private final InterFactory factory;

    /** Areas where fine glyphs may be needed. */
    private final List<Rectangle> fineBoxes = new ArrayList<>();

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
                           InterFactory factory)
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
     * <ol>
     * <li>retrieveFineBoxes() // Retrieve areas around small chords
     * <li>getSymbolsGlyphs() // Retrieve all glyphs usable for symbols
     * <li>buildLinks() // Build graph with distances
     * <li>processClusters(): // Group connected glyphs into clusters
     * <ol>
     * <li>FOREACH cluster of connected glyphs:
     * <ol>
     * <li>cluster.decompose() // Decompose cluster into all subsets
     * <li>FOREACH subset process(subset):
     * <ol>
     * <li>build compound glyph // Build one compound glyph per subset
     * <li>evaluateGlyph(compound) // Run shape classifier on compound
     * <li>FOREACH acceptable evaluation:
     * <ol>
     * <li>interFactory.create(eval, glyph) // Create inter(s) related to evaluation
     * </ol>
     * </ol>
     * </ol>
     * </ol>
     * </ol>
     *
     * @param optionalsMap the optional (weak) glyphs per system
     */
    public void buildSymbols (Map<SystemInfo, List<Glyph>> optionalsMap)
    {
        final StopWatch watch = new StopWatch("buildSymbols system #" + system.getId());
        logger.debug("System#{} buildSymbols", system.getId());

        // Identify areas for fine glyphs
        watch.start("retrieveFineBoxes");
        retrieveFineBoxes();

        // Retrieve all candidate glyphs
        watch.start("getSymbolsGlyphs");

        final List<Glyph> glyphs = getSymbolsGlyphs(optionalsMap);

        // Formalize glyphs relationships in a system-level graph
        watch.start("buildLinks");

        final SimpleGraph<Glyph, GlyphLink> systemGraph = Glyphs.buildLinks(glyphs, params.maxGap);

        // Process all sets of connected glyphs
        watch.start("processClusters");
        processClusters(systemGraph);

        if (constants.printWatch.isSet()) {
            watch.print();
        }
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

        final Point2D center = glyph.getCenter2D();
        final Staff closestStaff = system.getClosestStaff(center); // Just an indication!

        if (closestStaff == null) {
            return;
        }

        final Evaluation[] evals = classifier.evaluate(
                glyph,
                system,
                constants.maxEvaluationCount.getValue(),
                Grades.symbolMinGrade,
                EnumSet.of(Classifier.Condition.CHECKED));

        // Create one interpretation for each acceptable evaluation
        final SIGraph sig = system.getSig();
        final List<Inter> createdInters = new ArrayList<>();

        for (Evaluation eval : evals) {
            try {
                final Inter created = factory.create(eval, glyph, closestStaff);

                if (created != null) {
                    // Set exclusion with all competitors already created for the same glyph
                    for (Inter other : createdInters) {
                        sig.insertExclusion(other, created, Exclusion.ExclusionCause.OVERLAP);
                    }

                    createdInters.add(created);
                }
            } catch (Exception ex) {
                logger.warn("Error in glyph evaluation " + ex, ex);
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
        List<Glyph> glyphs = new ArrayList<>();

        for (Glyph glyph : system.getGroupedGlyphs(GlyphGroup.SYMBOL)) {
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
        final ConnectivityInspector<Glyph, GlyphLink> inspector = new ConnectivityInspector<>(
                systemGraph);
        final List<Set<Glyph>> sets = inspector.connectedSets();
        logger.debug("symbols sets: {}", sets.size());

        final int interline = sheet.getInterline();
        final int maxPartCount = constants.maxPartCount.getValue();

        for (Set<Glyph> set : sets) {
            final int setSize = set.size();
            logger.debug("set size: {}", setSize);

            if (setSize > 1) {
                final Set<Glyph> subSet; // Use an upper limit for set size

                if (setSize <= maxPartCount) {
                    subSet = set;
                } else {
                    List<Glyph> list = new ArrayList<>(set);
                    Collections.sort(list, Glyphs.byReverseWeight);
                    list = list.subList(0, Math.min(list.size(), maxPartCount));
                    subSet = new LinkedHashSet<>(list);
                    logger.debug("Symbol parts shrunk from {} to {}", setSize, maxPartCount);
                }

                // Use just the subgraph for this (sub)set
                final SimpleGraph<Glyph, GlyphLink> subGraph;
                subGraph = GlyphCluster.getSubGraph(subSet, systemGraph, true);
                new GlyphCluster(new SymbolAdapter(subGraph), GlyphGroup.SYMBOL).decompose();
            } else {
                // The set is just an isolated glyph, to be evaluated directly
                final Glyph glyph = set.iterator().next();

                if (classifier.isBigEnough(glyph, interline)) {
                    evaluateGlyph(glyph);
                }
            }
        }
    }

    //-------------------//
    // retrieveFineBoxes //
    //-------------------//
    /**
     * Define a fine box on the right side of every small chord, specifically meant for
     * detecting a potential small flag there.
     */
    private void retrieveFineBoxes ()
    {
        final List<Inter> smallChords = system.getSig().inters(SmallChordInter.class);

        for (Inter inter : smallChords) {
            final Rectangle box = inter.getBounds();
            final Rectangle fineBox = new Rectangle(
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
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Integer maxPartCount = new Constant.Integer(
                "Glyphs",
                7,
                "Maximum number of parts considered for a symbol");

        private final Constant.Integer maxEvaluationCount = new Constant.Integer(
                "Evaluations",
                2,
                "Maximum number of evaluations kept for a symbol");

        private final Scale.Fraction maxGap = new Scale.Fraction(
                0.6, // Was 0.5, but 0.55 is minimum needed for measure repeat sign
                "Maximum distance between two compound parts");

        private final Scale.AreaFraction minWeight = new Scale.AreaFraction(
                0.03,
                "Minimum weight for glyph consideration");

        private final Scale.AreaFraction minFineWeight = new Scale.AreaFraction(
                0.006,
                "Minimum weight for glyph consideration in a fine area");

        private final Scale.Fraction smallChordMargin = new Scale.Fraction(
                1,
                "Margin on right side of small chords to extend fine boxes");

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
     * Class <code>Parameters</code> gathers all pre-scaled constants.
     */
    private static class Parameters
    {

        final double maxGap;

        final int maxSymbolWidth;

        final int maxSymbolHeight;

        final int smallChordMargin;

        final int minWeight;

        final int minFineWeight;

        Parameters (Scale scale)
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

        private final Scale scale = sheet.getScale();

        SymbolAdapter (SimpleGraph<Glyph, GlyphLink> graph)
        {
            super(graph);
        }

        @Override
        public void evaluateGlyph (Glyph glyph,
                                   Set<Glyph> parts)
        {
            SymbolsBuilder.this.evaluateGlyph(glyph);
        }

        @Override
        public boolean isTooLarge (Rectangle symBox)
        {
            // Check width
            if (symBox.width > params.maxSymbolWidth) {
                return true;
            }

            // Check height (not limited if on left of system: braces / brackets)
            if (GeoUtil.center2D(symBox).getX() < system.getLeft()) {
                return false;
            } else {
                return symBox.height > params.maxSymbolHeight;
            }
        }

        @Override
        public boolean isTooLight (int weight)
        {
            double normed = scale.pixelsToAreaFrac(weight);

            return !classifier.isBigEnough(normed);
        }
    }
}
