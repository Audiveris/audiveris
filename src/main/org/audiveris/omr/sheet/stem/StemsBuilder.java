//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S t e m s B u i l d e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sheet.stem;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.glyph.dynamic.CompoundFactory;
import org.audiveris.omr.glyph.dynamic.StraightFilament;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.Fraction;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.BarConnectionRelation;
import org.audiveris.omr.sig.relation.BeamPortion;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.Exclusion.Cause;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.Corner;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class {@code StemsBuilder} processes a system to build stems that connect to note
 * heads and perhaps beams.
 * <p>
 * At this point, beams have been identified as well as void and black heads, but no flags yet.
 * <p>
 * A stem is expected to be horizontally connected on the left or right side of a head and
 * vertically connected as well.
 * Such connections are looked up in the 4 corners of every head.
 * In poor-quality scores, stems can lack many pixels, resulting in vertical gaps between stem parts
 * and between head and nearest stem part, so we must accept such potential gaps (even if we lower
 * the resulting interpretation grade).
 * However we can be much more strict for the horizontal gap of the connection.
 * <p>
 * A stem can aggregate several items: stem seeds (built from long vertical sticks) and chunks
 * (built from suitable sections found in the corner), all being separated by vertical gaps.
 * Up to which point should we try to accept vertical gaps and increase a stem length starting from
 * a head?
 * <ol>
 * <li>If there is a beam in the corner, try a stem that at least reaches the beam.</li>
 * <li>Use a similar approach for the case of flag (if the flag is in the right direction), except
 * that we don't have identified flags yet!</li>
 * <li>If no obvious limit exists, accept all gaps in sequence while no too large gap is
 * encountered.</li>
 * </ol>
 * <p>
 * Stem-head connection uses criteria based on xGap and yGap at reference point.
 * Stem-beam connection uses yGap (and xGap in the case of beam side connection).
 * <p>
 * Every sequence of stem items built from the head is evaluated and potentially recorded as a
 * separate stem interpretation in the SIG.
 * <p>
 * TODO: We could analyze in the whole page the population of "good" stems to come up with most
 * common stem lengths according to stem configurations, and boost stem interpretations that match
 * these most common lengths.
 * More precisely, the length that goes from last head to end of stem (if this end is free from beam
 * or flag) should be rather constant between stems.
 * <p>
 * TODO: We could be more strict on verticality (modulo sheet slope) when retrieving stem seeds and
 * when building stems. We could thus expect a better split between the vertical runs that belong to
 * the stem and those that belong to the head.
 * This would imply a specific version of filament factory.
 *
 * @author Hervé Bitteur
 */
public class StemsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StemsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    @Navigable(false)
    private final SIGraph sig;

    /** Sheet scale. */
    @Navigable(false)
    private final Scale scale;

    /** Dimension for black head symbol. */
    private final Dimension headSymbolDim;

    /** Scale-dependent parameters. */
    private Parameters params;

    /** Vertical seeds for this system. */
    private List<Glyph> systemSeeds;

    /** Beams and beam hooks for this system. */
    private List<Inter> systemBeams;

    /** Stems interpretations for this system. */
    private final List<StemInter> systemStems = new ArrayList<>();

    /** Areas forbidden to stem candidates. */
    private final List<Area> noStemAreas;

    private VerticalsBuilder verticalsBuilder;

    /** Constructor for stem compound. */
    private final CompoundFactory.CompoundConstructor stemConstructor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new StemsBuilder object.
     *
     * @param system the dedicated system
     */
    public StemsBuilder (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();

        Sheet sheet = system.getSheet();
        scale = sheet.getScale();

        ShapeSymbol symbol = Shape.NOTEHEAD_BLACK.getSymbol();
        headSymbolDim = symbol.getDimension(MusicFont.getHeadFont(scale, scale.getInterline()));

        noStemAreas = retrieveNoStemAreas();

        stemConstructor = new StraightFilament.Constructor(scale.getInterline());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // linkCueBeams //
    //--------------//
    /**
     * We reuse this class to connect a stem to potential cue beams.
     * Logic is rather simple for cue configurations. To be refined with a stem reuse to support
     * multiple cue heads on one stem.
     *
     * @param head   cue head
     * @param corner head corner for connection
     * @param stem   cue stem
     * @param beams  cue beams candidates
     */
    public void linkCueBeams (HeadInter head,
                              Corner corner,
                              Inter stem,
                              List<Inter> beams)
    {
        if (params == null) {
            params = new Parameters(system, scale);
        }

        new HeadLinker(head, this).linkCueCorner(corner, beams, (StemInter) stem);
    }

    //-----------//
    // linkStems //
    //-----------//
    /**
     * Link stems to suitable heads and beams in the system.
     * Retrieval is driven by heads (since a stem always needs a head), and can use beams.
     *
     * <pre>
     * Synopsis:
     *
     * - retrieve systemSeeds, systemBeams, systemHeads
     *
     * FOREACH head in systemHeads:
     *      FOREACH stem corner of the head:
     *          - getReferencePoint()
     *          - getLookupArea()
     *          - link()
     *              - lookupBeams()
     *              - lookupSeeds()
     *              - lookupChunks()
     *              - includeItems()
     *                  - createStemInter() for each relevant item
     *                  - connectHeadStem() for each relevant item
     *              - connectBeamStem() for relevant beams
     *
     * - retrieve systemStems
     *
     * FOREACH head in systemHeads:
     *      FOREACH stem corner of the head:
     *          - reuse()
     *              - connectHeadStem() for relevant stems
     *
     * - performMutualExclusions
     * </pre>
     */
    public void linkStems ()
    {
        if (params == null) {
            params = new Parameters(system, scale);
        }

        verticalsBuilder = new VerticalsBuilder(system);

        StopWatch watch = new StopWatch("StemsBuilder S#" + system.getId());
        watch.start("collections");
        // The abscissa-sorted stem seeds for this system
        systemSeeds = system.getGroupedGlyphs(GlyphGroup.VERTICAL_SEED);
        purgeNoStemSeeds(systemSeeds);

        // The abscissa-sorted beam (and beam hook) interpretations for this system
        systemBeams = sig.inters(AbstractBeamInter.class);
        Collections.sort(systemBeams, Inters.byAbscissa);

        // The abscissa-sorted head interpretations for this system
        final List<Inter> systemHeads = sig.inters(
                ShapeSet.getStemTemplateNotes(system.getSheet()));
        Collections.sort(systemHeads, Inters.byAbscissa);

        // First phase, look around heads for stems (and beams if any)
        watch.start("phase #1");

        for (Inter head : systemHeads) {
            new HeadLinker((HeadInter) head, this).linkStemCorners();
        }

        // Second phase, look for reuse of existing stems interpretations
        watch.start("phase #2");
        Collections.sort(systemStems, Inters.byAbscissa);

        for (Inter head : systemHeads) {
            new HeadLinker((HeadInter) head, this).reuseStemCorners();
        }

        // Handle stems mutual exclusions
        watch.start("stem exclusions");
        performMutualExclusions();

        // Check stems horizontal gap on each beam
        watch.start("checkBeamStems");

        for (Inter beam : sig.inters(BeamInter.class)) {
            checkBeamStems(beam);
            boostBeamSides(beam);
        }

        // Check carefully multiple stem links on same head
        watch.start("checkHeadStems");

        for (Inter head : systemHeads) {
            checkHeadStems((HeadInter) head);
        }

        // Discard heads with no stem link
        watch.start("checkNeededStems");
        checkNeededStems(systemHeads);

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    /**
     * @return the noStemAreas
     */
    List<Area> getNoStemAreas ()
    {
        return noStemAreas;
    }

    /**
     * @return the params
     */
    Parameters getParams ()
    {
        return params;
    }

    /**
     * @return the stemConstructor
     */
    CompoundFactory.CompoundConstructor getStemConstructor ()
    {
        return stemConstructor;
    }

    /**
     * @return the systemBeams
     */
    List<Inter> getSystemBeams ()
    {
        return systemBeams;
    }

    /**
     * @return the systemSeeds
     */
    List<Glyph> getSystemSeeds ()
    {
        return systemSeeds;
    }

    /**
     * @return the systemStems
     */
    List<StemInter> getSystemStems ()
    {
        return systemStems;
    }

    /**
     * @return the verticalsBuilder
     */
    VerticalsBuilder getVerticalsBuilder ()
    {
        return verticalsBuilder;
    }

    //----------------//
    // boostBeamSides //
    //----------------//
    private void boostBeamSides (Inter beam)
    {
        if (!beam.isGood()) {
            return;
        }

        List<BeamStemRelation> rels = new ArrayList<>();

        for (Relation rel : sig.edgesOf(beam)) {
            if (rel instanceof BeamStemRelation) {
                rels.add((BeamStemRelation) rel);
            }
        }

        for (BeamStemRelation rel : rels) {
            if (rel.getBeamPortion() != BeamPortion.CENTER) {
                StemInter stem = (StemInter) sig.getEdgeTarget(rel);
                stem.increase(constants.sideStemBoost.getValue());
            }
        }
    }

    //----------------//
    // checkBeamStems //
    //----------------//
    /**
     * Check whether the beam does not have a stem relation too close to another.
     * Too close stems are mutually exclusive.
     *
     * @param beam the beam to check
     */
    private void checkBeamStems (Inter beam)
    {
        if (beam.isVip()) {
            logger.info("VIP checkBeamStems? {}", beam);
        }

        List<BeamStemRelation> rels = new ArrayList<>();

        for (Relation rel : sig.edgesOf(beam)) {
            if (rel instanceof BeamStemRelation) {
                rels.add((BeamStemRelation) rel);
            }
        }

        // Sort by abscissae
        Collections.sort(rels, new Comparator<BeamStemRelation>()
                 {
                     @Override
                     public int compare (BeamStemRelation o1,
                                         BeamStemRelation o2)
                     {
                         return Double.compare(o1.getExtensionPoint().getX(), o2.getExtensionPoint()
                                               .getX());
                     }
                 });

        final int size = rels.size();

        for (int i = 0; i < size; i++) {
            BeamStemRelation rel = rels.get(i);
            StemInter stem = (StemInter) sig.getEdgeTarget(rel);

            for (BeamStemRelation r : rels.subList(i + 1, size)) {
                double dx = r.getExtensionPoint().getX() - rel.getExtensionPoint().getX();

                if (dx < params.minBeamStemsGap) {
                    StemInter s = (StemInter) sig.getEdgeTarget(r);
                    sig.insertExclusion(stem, s, Cause.INCOMPATIBLE);
                } else {
                    break;
                }
            }
        }
    }

    //----------------//
    // checkHeadStems //
    //----------------//
    /**
     * A head can have links to two stems (non mutually exclusive) only when these stems
     * are compatible (head is on stem ends with one stem on bottom left and one stem on
     * top right).
     * <p>
     * Otherwise, we must clean up the configuration.
     * If there is a link with zero yGap, it has priority over non-zero yGap that are generally due
     * to stem extension. So cut the non-zero links.
     * If there are two zero yGaps, if they are opposed on normal sides, it's OK.
     * If not, cut the link to the one not on normal side.
     * If there are two non-zero yGaps, cut the one with larger yGap.
     *
     * @param head the note head to check
     */
    private void checkHeadStems (HeadInter head)
    {
        // Retrieve all stems connected to this head
        List<Inter> allStems = new ArrayList<>();

        for (Relation rel : sig.getRelations(head, HeadStemRelation.class)) {
            allStems.add(sig.getEdgeTarget(rel));
        }

        // List of non-conflicting stems ensembles
        List<List<Inter>> partners = sig.getPartitions(null, allStems);
        HeadStemsCleaner checker = null;

        for (List<Inter> ensemble : partners) {
            if (ensemble.size() <= 1) {
                continue;
            }

            if (checker == null) {
                checker = new HeadStemsCleaner(head);
            }

            checker.check(ensemble);
        }
    }

    //------------------//
    // checkNeededStems //
    //------------------//
    /**
     * For heads that need a stem, check there is at least one.
     *
     * @param systemHeads heads to check
     */
    private void checkNeededStems (List<Inter> systemHeads)
    {
        for (Inter head : systemHeads) {
            if (ShapeSet.StemHeads.contains(head.getShape())) {
                if (!sig.hasRelation(head, HeadStemRelation.class)) {
                    head.remove();
                }
            }
        }
    }

    //-------------------------//
    // performMutualExclusions //
    //-------------------------//
    /**
     * Browse the system interpretations to insert mutual exclusions wherever possible.
     * This is done for stems.
     */
    private void performMutualExclusions ()
    {
        final List<Inter> stems = sig.inters(Shape.STEM);
        final int size = stems.size();
        int count = 0;

        try {
            if (size < 2) {
                return;
            }

            Collections.sort(stems, Inters.byAbscissa);

            for (int i = 0; i < (size - 1); i++) {
                final Inter one = stems.get(i);
                final Rectangle oneBox = one.getGlyph().getBounds();
                final int xBreak = oneBox.x + oneBox.width;

                for (Inter two : stems.subList(i + 1, size)) {
                    Rectangle twoBox = two.getGlyph().getBounds();

                    // Is there an overlap between stems one & two?
                    if (oneBox.intersects(twoBox)) {
                        sig.insertExclusion(one, two, Cause.OVERLAP);
                        count++;
                    } else if (twoBox.x >= xBreak) {
                        break;
                    }
                }
            }
        } finally {
            logger.debug("S#{} stems: {} exclusions: {}", system.getId(), size, count);
        }
    }

    //------------------//
    // purgeNoStemSeeds //
    //------------------//
    /**
     * In the provided seeds collection, remove the ones that are located in so-called
     * noStemAreas.
     *
     * @param seeds the collection to purge
     */
    private void purgeNoStemSeeds (List<Glyph> seeds)
    {
        SeedLoop:
        for (Iterator<Glyph> it = seeds.iterator(); it.hasNext();) {
            final Glyph seed = it.next();
            final Rectangle seedBox = seed.getBounds();
            final double seedSize = seedBox.width * seedBox.height;

            for (Area noStem : noStemAreas) {
                if (noStem.intersects(seedBox)) {
                    // Compute intersection over noStem area
                    Area intersection = new Area(seedBox);
                    intersection.intersect(noStem);
                    Rectangle2D interBox = intersection.getBounds2D();
                    double interSize = interBox.getWidth() * interBox.getHeight();

                    Rectangle2D barBox = noStem.getBounds2D();
                    double barSize = barBox.getWidth() * barBox.getHeight();

                    final double ratio = interSize / Math.min(barSize, seedSize);

                    if (ratio > params.maxBarOverlap) {
                        it.remove();

                        continue SeedLoop;
                    }
                }
            }
        }
    }

    //---------------------//
    // retrieveNoStemAreas //
    //---------------------//
    /**
     * Remember those inters that candidate stems cannot compete with.
     * Typically, these are 2-staff barlines with their connector.
     *
     * @return list of no-stem areas, sorted by abscissa
     */
    private List<Area> retrieveNoStemAreas ()
    {
        final List<Area> areas = new ArrayList<>();

        for (Inter barline : sig.inters(BarlineInter.class)) {
            Set<Relation> connections = sig.getRelations(barline, BarConnectionRelation.class);
            for (Relation connection : connections) {
                BarlineInter source = (BarlineInter) sig.getEdgeSource(connection);

                if (source == barline) {
                    // Top area
                    areas.add(barline.getArea());

                    // Bottom area
                    BarlineInter target = (BarlineInter) sig.getEdgeTarget(connection);
                    areas.add(target.getArea());

                    // Middle area
                    Line2D median = new Line2D.Double(
                            source.getMedian().getP2(),
                            target.getMedian().getP1());
                    double width = 0.5 * (source.getWidth() + target.getWidth());
                    Area middle = AreaUtil.verticalRibbon(median, width);
                    areas.add(middle);
                }
            }
        }

        // Sort by abscissa
        Collections.sort(areas, new Comparator<Area>()
                 {
                     @Override
                     public int compare (Area a1,
                                         Area a2)
                     {
                         return Double.compare(a1.getBounds2D().getMinX(),
                                               a2.getBounds2D().getMinX());
                     }
                 });

        return areas;
    }

    //---------------------//
    // getMinStemExtension //
    //---------------------//
    /**
     * Report the minimum extension that goes from last head to end of stem.
     *
     * @return the defined constant
     */
    public static Fraction getMinStemExtension ()
    {
        return constants.minStemExtension;
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

        private final Scale.Fraction vicinityMargin = new Scale.Fraction(
                1.0,
                "Rough abscissa margin when looking for neighbors");

        private final Constant.Ratio beamRatio = new Constant.Ratio(
                2.0,
                "Ratio applied on acceptable vertical gap when a beam is targetted");

        private final Constant.Double slopeMargin = new Constant.Double(
                "tangent",
                0.02,
                "Margin around slope to define corner lookup area");

        private final Constant.Ratio maxBarOverlap = new Constant.Ratio(
                0.25,
                "Maximum stem overlap ratio on a connected barline");

        private final Scale.Fraction minHeadSectionContribution = new Scale.Fraction(
                0.2,
                "Minimum stem contribution for a section near head");

        private final Scale.Fraction minStemExtension = new Scale.Fraction(
                0.8,
                "Minimum vertical distance from head to end of stem");

        private final Scale.Fraction minHeadBeamDistance = new Scale.Fraction(
                0.125,
                "Minimum vertical distance between head and beam");

        private final Scale.Fraction maxBeamDistance = new Scale.Fraction(
                1.5,
                "Default maximum vertical distance between two consecutive grouped beams");

        private final Scale.Fraction minBeamStemsGap = new Scale.Fraction(
                1.0,
                "Minimum x gap between two stems on the same beam");

        private final Constant.Ratio maxSeedJitter = new Constant.Ratio(
                2.0,
                "Maximum distance from stem seed to theoretical line,"
                        + " as ratio of typical stem width");

        private final Constant.Ratio maxSectionJitter = new Constant.Ratio(
                1.0,
                "Maximum distance from section center to target line,"
                        + " as ratio of typical stem width");

        private final Constant.Ratio sideStemBoost = new Constant.Ratio(
                0.5,
                "How much do we boost beam side stems");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all pre-scaled constants.
     */
    static class Parameters
    {

        final double beamRatio;

        final double slopeMargin;

        final double maxBarOverlap;

        final int maxHeadOutDx;

        final int maxBeamInDx;

        final int maxHeadInDx;

        final int vicinityMargin;

        final int maxStemHeadGapY;

        final int maxYGap;

        final int maxYGapPoor;

        final int maxStemThickness;

        final int minChunkWeight;

        final int minHeadSectionContribution;

        final int minStemExtension;

        final int minHeadBeamDistance;

        final int minBeamStemsGap;

        final double maxSeedJitter;

        final double maxSectionJitter;

        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        Parameters (SystemInfo system,
                    Scale scale)
        {
            beamRatio = constants.beamRatio.getValue();
            slopeMargin = constants.slopeMargin.getValue();
            maxBarOverlap = constants.maxBarOverlap.getValue();
            maxHeadOutDx = scale.toPixels(HeadStemRelation.getXOutGapMaximum(false));
            maxBeamInDx = scale.toPixels(BeamStemRelation.getXInGapMaximum(false));
            maxHeadInDx = scale.toPixels(HeadStemRelation.getXInGapMaximum(false));
            vicinityMargin = scale.toPixels(constants.vicinityMargin);
            maxStemHeadGapY = scale.toPixels(HeadStemRelation.getYGapMaximum(false));
            maxYGap = scale.toPixels(VerticalsBuilder.getMaxYGap());
            maxYGapPoor = scale.toPixels(VerticalsBuilder.getMaxYGapPoor());
            minHeadSectionContribution = scale.toPixels(constants.minHeadSectionContribution);
            minStemExtension = scale.toPixels(getMinStemExtension());
            minHeadBeamDistance = scale.toPixels(constants.minHeadBeamDistance);
            minBeamStemsGap = scale.toPixels(constants.minBeamStemsGap);

            final int stemThickness = scale.getMaxStem();
            maxStemThickness = stemThickness;
            maxSeedJitter = constants.maxSeedJitter.getValue() * stemThickness;
            maxSectionJitter = constants.maxSectionJitter.getValue() * stemThickness;

            minChunkWeight = scale.getStemThickness(); // TODO: check this

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
