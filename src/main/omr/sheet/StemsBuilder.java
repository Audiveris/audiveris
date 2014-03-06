//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S t e m s B u i l d e r                                    //
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

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;

import omr.image.Anchored.Anchor;
import omr.image.ShapeDescriptor;

import omr.lag.Section;

import omr.math.GeoOrder;
import omr.math.GeoUtil;
import omr.math.LineUtil;

import omr.run.Orientation;
import static omr.run.Orientation.*;
import omr.run.Run;

import omr.sig.AbstractBeamInter;
import omr.sig.AbstractNoteInter;
import omr.sig.BeamPortion;
import omr.sig.BeamStemRelation;
import omr.sig.Exclusion.Cause;
import omr.sig.FullBeamInter;
import omr.sig.GradeImpacts;
import omr.sig.HeadStemRelation;
import omr.sig.Inter;
import omr.sig.Relation;
import omr.sig.SIGraph;
import omr.sig.StemInter;
import omr.sig.StemPortion;
import static omr.sig.StemPortion.*;
import omr.sig.WholeInter;

import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;

import omr.util.Corner;
import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Navigable;
import omr.util.StopWatch;
import static omr.util.VerticalSide.*;
import omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code StemsBuilder} processes a system to build stems that
 * connect to note heads and perhaps beams.
 * <p>
 * At this point, beams have been identified as well as void and black heads.
 * We don't have flags yet at this time.
 * <p>
 * A stem is expected to be horizontally connected on the left or right
 * side of a head and vertically connected as well.
 * Such connections are looked up in the 4 corners of every head.
 * In poor-quality scores, stems can lack many pixels, resulting in vertical
 * gaps between stem parts and between head and nearest stem part, so we
 * must accept such potential gaps (even if we lower the resulting
 * interpretation grade).
 * However we can be much more strict for the horizontal gap of the connection.
 * <p>
 * A stem can be the aggregation of several items: stem seeds (built from
 * long vertical sticks) and chunks (built from suitable sections found
 * in the corner), all being separated by vertical gaps.
 * Up to which point should we try to accept vertical gaps and increase a stem
 * length starting from a head?<ol>
 * <li>If there is a beam in the corner, try a stem that at least reaches the
 * beam.</li>
 * <li>Use a similar approach for the case of flag (if the flag is in the right
 * direction), except that we don't have identified flags yet!</li>
 * <li>If no obvious limit exists, accept all gaps in sequence while no too
 * large gap is encountered.</li>
 * </ol>
 * <p>
 * Every sequence of stem items built from the head is evaluated and
 * potentially recorded as a separate stem interpretation in the SIG.
 * <p>
 * TODO: We could analyze in the whole page the population of "good" stems to
 * come up with most common stem lengths according to stem configurations,
 * and boost stem interpretations that match these most common lengths.
 * More precisely, the length that goes from last head to end of stem (if this
 * end is free from beam or flag) should be rather constant between stems.
 * <p>
 * Stem-head connection uses criteria based on xGap and yGap at reference point.
 * Stem-beam connection uses yGap (and xGap in the case of beam side
 * connection).
 *
 * @author Hervé Bitteur
 */
public class StemsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StemsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
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

    /** Global skew. */
    private final Skew skew;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Vertical seeds for this system. */
    private List<Glyph> systemSeeds;

    /** Heads (void & black) for this system. */
    private List<Inter> systemHeads;

    /** Beams and beam hooks for this system. */
    private List<Inter> systemBeams;

    /** Stems interpretations for this system. */
    private List<Inter> systemStems;

    //~ Constructors -------------------------------------------------------------------------------
    //
    //--------------//
    // StemsBuilder //
    //--------------//
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
        skew = sheet.getSkew();
        params = new Parameters(system, scale);

        ShapeSymbol symbol = Shape.NOTEHEAD_BLACK.getSymbol();
        headSymbolDim = symbol.getDimension(MusicFont.getFont(scale.getInterline()));
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // linkCueBeams //
    //--------------//
    /**
     * We reuse this class to connect a stem to potential cue beams.
     * Logic is rather simple for cue configurations. To be refined with a
     * stem reuse to support multiple cue heads on one stem.
     *
     * @param head   cue head
     * @param corner head corner for connection
     * @param stem   cue stem
     * @param beams  cue beams candidates
     */
    public void linkCueBeams (Inter head,
                              Corner corner,
                              Inter stem,
                              List<Inter> beams)
    {
        new HeadLinker(head).linkCueCorner(corner, beams, (StemInter) stem);
    }

    //
    //-----------//
    // linkStems //
    //-----------//
    /**
     * Link stems to suitable heads and beams in the system.
     * <pre>
     * Synopsis:
     *
     * - retrieve systemSeeds, systemBeams, systemHeads
     *
     * FOREACH head in systemHeads:
     *      FOREACH corner:
     *          - getReferencePoint()
     *          - getLookupArea()
     *          - link()
     *              - lookupBeams()
     *              - lookupSeeds()
     *              - lookupChunks()
     *              - includeItems()
     *                  - createStemInter() for relevant items
     *                  - connectHeadStem() for relevant items
     *              - connectBeamStem() for relevant beams
     *
     * - retrieve systemStems
     *
     * FOREACH head in systemHeads:
     *      FOREACH corner:
     *          - reuse()
     *              - connectHeadStem() for relevant stems
     *
     * - performMutualExclusions
     * </pre>
     */
    public void linkStems ()
    {
        StopWatch watch = new StopWatch("StemsBuilder S#" + system.getId());
        watch.start("collections");
        // The sorted stem seeds for this system
        systemSeeds = getSystemSeeds();

        // The sorted beam and beam hook interpretations for this system
        systemBeams = sig.inters(AbstractBeamInter.class);
        Collections.sort(systemBeams, Inter.byAbscissa);

        // The sorted head interpretations for this system
        systemHeads = sig.inters(ShapeSet.StemTemplateNotes);
        Collections.sort(systemHeads, Inter.byAbscissa);

        // First phase, look around heads for stems (and beams if any)
        watch.start("phase #1");

        for (Inter head : systemHeads) {
            new HeadLinker(head).linkAllCorners();
        }

        // Second phase, look for reuse of existing stems interpretations
        watch.start("phase #2");
        systemStems = sig.inters(Shape.STEM);
        Collections.sort(systemStems, Inter.byAbscissa);

        for (Inter head : systemHeads) {
            new HeadLinker(head).reuseAllCorners();
        }

        // Handle stems mutual exclusions
        watch.start("stem exclusions");
        performMutualExclusions();

        // Check stems horizontal gap on each beam
        watch.start("checkBeamStems");

        for (Inter beam : sig.inters(FullBeamInter.class)) {
            checkBeamStems(beam);
        }

        // Check carefully multiple stem links on same head
        watch.start("checkHeadStems");

        for (Inter head : systemHeads) {
            checkHeadStems(head);
        }

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //----------------//
    // checkBeamStems //
    //----------------//
    /**
     * Check whether the beam does not have a stem relation too close to another.
     * If one of the stems is a side stem, we discard the other connection.
     * If none of the stems is a side stem, we discard the worst connection.
     *
     * @param beam the beam to check
     */
    private void checkBeamStems (Inter beam)
    {
        List<BeamStemRelation> rels = new ArrayList<BeamStemRelation>();

        for (Relation rel : sig.edgesOf(beam)) {
            if (rel instanceof BeamStemRelation) {
                rels.add((BeamStemRelation) rel);
            }
        }

        // Sort by abscissae
        Collections.sort(
                rels,
                new Comparator<BeamStemRelation>()
        {
            @Override
            public int compare (BeamStemRelation o1,
                                BeamStemRelation o2)
            {
                return Double.compare(o1.getCrossPoint().getX(), o2.getCrossPoint().getX());
            }
                });

        BeamStemRelation prevRel = null;

        for (BeamStemRelation rel : rels) {
            if (prevRel != null) {
                double dx = rel.getCrossPoint().getX() - prevRel.getCrossPoint().getX();

                if (dx < params.minBeamStemsGap) {
                    // Check if there is already an exclusion between the two stems
                    if (sig.getExclusion(sig.getEdgeTarget(prevRel), sig.getEdgeTarget(rel)) != null) {
                        prevRel = rel;
                    } else if (prevRel.getBeamPortion() == BeamPortion.LEFT) {
                        // Keep side connection
                        sig.removeEdge(rel);
                    } else if (rel.getBeamPortion() == BeamPortion.RIGHT) {
                        // Keep side connection
                        sig.removeEdge(prevRel);
                        prevRel = rel;
                    } else {
                        // Discard the weaker of these two CENTER connections
                        if (rel.getGrade() <= prevRel.getGrade()) {
                            sig.removeEdge(rel);
                        } else {
                            sig.removeEdge(prevRel);
                            prevRel = rel;
                        }
                    }
                }
            } else {
                prevRel = rel;
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
    private void checkHeadStems (Inter head)
    {
        // Retrieve all nearby stems
        List<Inter> allStems = new ArrayList<Inter>();

        for (Relation rel : sig.edgesOf(head)) {
            if (rel instanceof HeadStemRelation) {
                allStems.add(sig.getEdgeTarget(rel));
            }
        }

        // List of non-conflicting stems ensembles
        List<List<Inter>> partners = sig.getPartners(null, allStems);
        ShareChecker checker = null;

        for (List<Inter> ensemble : partners) {
            if (ensemble.size() <= 1) {
                continue;
            }

            if (checker == null) {
                checker = new ShareChecker(head);
            }

            checker.check(ensemble);
        }
    }

    //----------------//
    // getSystemSeeds //
    //----------------//
    /**
     * Retrieves the vertical stem seeds for the system
     *
     * @return the collection of system stem seeds
     */
    private List<Glyph> getSystemSeeds ()
    {
        List<Glyph> seeds = new ArrayList<Glyph>();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == Shape.VERTICAL_SEED) {
                seeds.add(glyph);
            }
        }

        return seeds;
    }

    //-------------------------//
    // performMutualExclusions //
    //-------------------------//
    /**
     * Browse the system interpretations to insert mutual exclusions
     * wherever possible.
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

            for (int i = 0; i < (size - 1); i++) {
                Inter one = stems.get(i);
                Rectangle oneBox = one.getGlyph().getBounds();

                for (Inter two : stems.subList(i + 1, size)) {
                    Rectangle twoBox = two.getGlyph().getBounds();

                    // Is there an overlap between stems one & two?
                    if (oneBox.intersects(twoBox)) {
                        sig.insertExclusion(one, two, Cause.OVERLAP);
                        count++;
                    }
                }
            }

            performStemNoteExclusions(stems);
        } finally {
            logger.debug("S#{} stems: {} exclusions: {}", system.getId(), size, count);
        }
    }

    //---------------------------//
    // performStemNoteExclusions //
    //---------------------------//
    private void performStemNoteExclusions (List<Inter> stems)
    {
        List<Inter> notes = sig.inters(
                ShapeSet.shapesOf(ShapeSet.NoteHeads.getShapes(), ShapeSet.Notes.getShapes()));

        for (Inter inter : stems) {
            StemInter stem = (StemInter) inter;

            NoteLoop:
            for (Inter note : notes) {
                if (note.overlaps(stem)) {
                    // Is there a support connection?
                    if (!(note instanceof WholeInter)) {
                        for (Relation rel : sig.getAllEdges(note, stem)) {
                            if (rel instanceof HeadStemRelation) {
                                continue NoteLoop;
                            }
                        }
                    }

                    sig.insertExclusion(note, stem, Cause.OVERLAP);
                    logger.debug("Overlap between {} & {}", note, stem);
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

        final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        final Scale.Fraction vicinityMargin = new Scale.Fraction(
                1.0,
                "Rough abscissa margin when looking for neighbors");

        final Constant.Double slopeMargin = new Constant.Double(
                "tangent",
                0.02,
                "Margin around slope to define corner lookup area");

        final Scale.Fraction minHeadSectionContribution = new Scale.Fraction(
                0.2,
                "Minimum stem contribution for a section near head");

        final Scale.Fraction minStemExtension = new Scale.Fraction(
                0.8,
                "Minimum vertical distance from head to end of stem");

        final Scale.Fraction minHeadBeamDistance = new Scale.Fraction(
                0.5,
                "Minimum vertical distance between head and beam");

        final Scale.Fraction maxInterBeamGap = new Scale.Fraction(
                1.0,
                "Maximum vertical gap between two consecutive beams of the same group");

        final Scale.Fraction minBeamStemsGap = new Scale.Fraction(
                1.0,
                "Minimum x gap between two stems on the same beam");

        final Constant.Ratio maxSeedJitter = new Constant.Ratio(
                2.0,
                "Maximum distance from stem seed to theoretical line,"
                + " as ratio of typical stem width");

        final Constant.Ratio maxSectionJitter = new Constant.Ratio(
                0.5,
                "Maximum distance from section center to target line,"
                + " as ratio of typical stem width");

        final Scale.Fraction yGapTiny = new Scale.Fraction(
                0.1,
                "Maximum vertical tiny gap between stem & head");
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

        final double slopeMargin;

        final int maxHeadOutDx;

        final int maxBeamInDx;

        final int maxHeadInDx;

        final int vicinityMargin;

        final int maxStemHeadGapY;

        final int maxYGap;

        final int maxStemThickness;

        final int minHeadSectionContribution;

        final int minStemExtension;

        final int minHeadBeamDistance;

        final int maxInterBeamGap;

        final int minBeamStemsGap;

        final double maxSeedJitter;

        final double maxSectionJitter;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (SystemInfo system,
                           Scale scale)
        {
            slopeMargin = constants.slopeMargin.getValue();
            maxHeadOutDx = scale.toPixels(HeadStemRelation.getXOutGapMaximum());
            maxBeamInDx = scale.toPixels(BeamStemRelation.getXInGapMaximum());
            maxHeadInDx = scale.toPixels(HeadStemRelation.getXInGapMaximum());
            vicinityMargin = scale.toPixels(constants.vicinityMargin);
            maxStemHeadGapY = scale.toPixels(HeadStemRelation.getYGapMaximum());
            maxYGap = system.verticalsBuilder.getMaxYGap();
            maxStemThickness = scale.getMainStem();
            minHeadSectionContribution = scale.toPixels(constants.minHeadSectionContribution);
            minStemExtension = scale.toPixels(constants.minStemExtension);
            minHeadBeamDistance = scale.toPixels(constants.minHeadBeamDistance);
            maxInterBeamGap = scale.toPixels(constants.maxInterBeamGap);
            minBeamStemsGap = scale.toPixels(constants.minBeamStemsGap);
            maxSeedJitter = constants.maxSeedJitter.getValue() * scale.getMainStem();
            maxSectionJitter = constants.maxSectionJitter.getValue() * scale.getMainStem();

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }

    //------------//
    // HeadLinker //
    //------------//
    /**
     * A HeadLinker tries to establish links from a head to nearby
     * stem interpretations, processing all 4 corners.
     */
    private class HeadLinker
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The head interpretation being processed. */
        private final Inter head;

        /** Head bounding box. */
        private final Rectangle headBox;

        /** All beams and hooks interpretations in head vicinity. */
        private List<Inter> neighborBeams;

        /** All stems seeds in head vicinity. */
        private List<Glyph> neighborSeeds;

        /** All stems interpretations in head vicinity. */
        private List<Inter> neighborStems;

        //~ Constructors ---------------------------------------------------------------------------
        public HeadLinker (Inter head)
        {
            this.head = head;
            headBox = head.getBounds();
        }

        //~ Methods --------------------------------------------------------------------------------
        //----------------//
        // linkAllCorners //
        //----------------//
        public void linkAllCorners ()
        {
            neighborBeams = getNeighboringInters(head, systemBeams);
            neighborSeeds = getNeighboringSeeds(head);

            for (Corner corner : Corner.values) {
                new CornerLinker(corner).link();
            }
        }

        //---------------//
        // linkCueCorner //
        //---------------//
        public void linkCueCorner (Corner corner,
                                   List<Inter> beams,
                                   StemInter stem)
        {
            new CornerLinker(corner).linkCue(beams, stem);
        }

        //-----------------//
        // reuseAllCorners //
        //-----------------//
        public void reuseAllCorners ()
        {
            neighborStems = getNeighboringInters(head, systemStems);

            for (Corner corner : Corner.values) {
                new CornerLinker(corner).reuse();
            }
        }

        //----------------------//
        // getNeighboringInters //
        //----------------------//
        /**
         * From the provided collection of interpretations, retrieve all those located
         * in the vicinity of the provided central interpretation.
         *
         * @param inter  the central interpretation
         * @param inters the collection of interpretations to search
         * @return the set of neighboring interpretations
         */
        private List<Inter> getNeighboringInters (Inter inter,
                                                  List<Inter> inters)
        {
            // Retrieve neighboring inters, using a box of system height and sufficiently wide,
            // just to play with a limited number of inters.
            Rectangle interBox = inter.getBounds();
            Rectangle systemBox = system.getBounds();
            Rectangle fatBox = new Rectangle(
                    interBox.x,
                    systemBox.y,
                    interBox.width,
                    systemBox.height);
            fatBox.grow(params.vicinityMargin, 0);

            return sig.intersectedInters(inters, GeoOrder.BY_ABSCISSA, fatBox);
        }

        //---------------------//
        // getNeighboringSeeds //
        //---------------------//
        /**
         * Retrieve all vertical seeds in the vicinity of the provided
         * (head) interpretation.
         *
         * @param inter the head interpretation
         * @return the set of neighboring seeds
         */
        private List<Glyph> getNeighboringSeeds (Inter inter)
        {
            // Retrieve neighboring stem seeds, using a box of system height and
            // sufficiently wide, just to play with a limited number of seeds.
            Rectangle interBox = inter.getBounds();
            Rectangle systemBox = system.getBounds();
            Rectangle fatBox = new Rectangle(
                    interBox.x,
                    systemBox.y,
                    interBox.width,
                    systemBox.height);
            fatBox.grow(params.vicinityMargin, 0);

            return sig.intersectedGlyphs(systemSeeds, true, fatBox);
        }

        //~ Inner Classes --------------------------------------------------------------------------
        //
        //--------------//
        // CornerLinker //
        //--------------//
        /**
         * A CornerLinker searches for all acceptable head -> stem
         * links in a given corner.
         */
        protected class CornerLinker
        {
            //~ Instance fields --------------------------------------------------------------------

            /** The corner being processed. */
            private final Corner corner;

            /** Direction of abscissae when going away from head. */
            private final int xDir;

            /** Direction of ordinates when going away from head. */
            private final int yDir;

            /** The head reference point for the corner. */
            private final Point2D refPt;

            /** The distant target point for the stem. */
            private Point2D targetPt;

            /** The look up area for the corner. */
            private Area area;

            /** The stems seeds found in the corner. */
            private List<Glyph> seeds;

            /** The most probable stem target line. */
            private Line2D targetLine;

            /** Ordinate range between refPt & limit. */
            private Rectangle yRange;

            //~ Constructors -----------------------------------------------------------------------
            public CornerLinker (Corner corner)
            {
                this.corner = corner;

                xDir = (corner.hSide == RIGHT) ? 1 : (-1);
                yDir = (corner.vSide == BOTTOM) ? 1 : (-1);
                refPt = getReferencePoint();
            }

            //~ Methods ----------------------------------------------------------------------------
            //------//
            // link //
            //------//
            /**
             * Look for all acceptable stems interpretations that can be connected to
             * the head in the desired corner.
             * Stop the search at the first good beam found or at the first non acceptable yGap,
             * whichever comes first.
             */
            public void link ()
            {
                if (head.isVip()) {
                    logger.info("VIP link {} {}", head, corner);
                }

                area = getLuArea();

                // Compute target end of stem
                Rectangle systemBox = system.getBounds();
                int sysY = (yDir > 0) ? (systemBox.y + systemBox.height) : systemBox.y;
                targetPt = getTargetPt(new Line2D.Double(0, sysY, 100, sysY));

                // Look for beams and beam hooks in the corner
                List<Inter> beamCandidates = sig.intersectedInters(
                        neighborBeams,
                        GeoOrder.BY_ABSCISSA,
                        area);

                List<AbstractBeamInter> beams = new ArrayList<AbstractBeamInter>();
                int groupStart = lookupBeams(beams, beamCandidates);

                // If we have a good beam, stop at the end of beam group
                // using the good beam for the target point
                if (groupStart != -1) {
                    targetPt = getTargetPt(getLimit(beams.get(groupStart)));
                }

                Line2D line = new Line2D.Double(refPt, targetPt);
                head.addAttachment("t" + corner.getId(), line);

                yRange = getYRange(targetPt.getY());

                // Define the best target line (and collect suitable seeds)
                targetLine = getTargetLine();

                // Look for additional chunks built out of sections found.
                // Assign special role to a fat section part of head (if any)
                Wrapper<Section> fatHeadSection = new Wrapper<Section>();
                List<Glyph> chunks = lookupChunks(fatHeadSection);

                // Aggregate seeds and chunks up to the limit
                List<Glyph> items = new ArrayList<Glyph>(seeds);

                if (!chunks.isEmpty()) {
                    items.addAll(chunks);
                    sortByDistance(items);
                }

                double refY = refPt.getY(); // Reference ordinate

                if (fatHeadSection.value != null) {
                    // Shift the reference ordinate accordingly
                    Rectangle runBox = getRunBox(fatHeadSection.value, corner.hSide);
                    int contrib = getContrib(runBox);

                    if (contrib > 0) {
                        refY += (yDir * contrib);
                    }
                }

                // Include each item (seed / chunk) until limit is reached
                List<StemInter> stems = includeItems(items, refY, fatHeadSection.value);

                // Beam - Stem connection(s)?
                if (!beams.isEmpty() && !stems.isEmpty()) {
                    linkBeamsAndStems(beams, groupStart, stems);
                }
            }

            /**
             * Specific link for cue (head & beam).
             */
            public void linkCue (List<Inter> candidates,
                                 StemInter stem)
            {
                // Look for beams in the corner
                List<AbstractBeamInter> beams = new ArrayList<AbstractBeamInter>();
                int groupStart = lookupBeams(beams, candidates);
                linkBeamsAndStems(beams, groupStart, Collections.singletonList(stem));
            }

            //-------//
            // reuse //
            //-------//
            /**
             * Check the stems interpretations in the vicinity and try
             * to connect the head to them, if not already done.
             */
            public void reuse ()
            {
                area = getLuArea();

                // Look for stems inters that intersect the lookup area
                List<Inter> stems = sig.intersectedInters(
                        neighborStems,
                        GeoOrder.BY_ABSCISSA,
                        area);

                for (Inter inter : stems) {
                    StemInter stemInter = (StemInter) inter;
                    // (try to) connect
                    connectHeadStem(null, stemInter);
                }
            }

            //--------------------//
            // areGroupCompatible //
            //--------------------//
            /**
             * Check whether the two beams can be consecutive beams
             * in the same beam group, using ordinate gap.
             *
             * @param one current beam
             * @param two following beam, in 'dir' direction
             * @return true if OK
             */
            private boolean areGroupCompatible (AbstractBeamInter one,
                                                AbstractBeamInter two)
            {
                // Vertical gap?
                Point2D onePt = getTargetPt(one.getMedian());
                Point2D twoPt = getTargetPt(two.getMedian());
                final double yGap = Math.abs(onePt.getY() - twoPt.getY())
                                    - ((one.getHeight() + two.getHeight()) / 2);

                if (yGap > params.maxInterBeamGap) {
                    logger.debug("{} & {} are too distant", one, two);

                    return false;
                }

                return true;
            }

            //-----------------//
            // connectBeamStem //
            //-----------------//
            /**
             * (Try to) connect beam and stem.
             *
             * @param beam      the beam or hook interpretation
             * @param stemInter the stem interpretation
             * @return the beam stem relation if successful, null otherwise
             */
            private BeamStemRelation connectBeamStem (AbstractBeamInter beam,
                                                      StemInter stemInter)
            {
                if (beam.isVip() && stemInter.isVip()) {
                    logger.info("VIP connectBeamStem {} & {}", beam, stemInter);
                }

                // Relation beam -> stem (if not yet present)
                BeamStemRelation bRel = (BeamStemRelation) sig.getRelation(
                        beam,
                        stemInter,
                        BeamStemRelation.class);

                if (bRel == null) {
                    final Glyph stemGlyph = stemInter.getGlyph();
                    final Line2D beamLimit = getLimit(beam);
                    bRel = new BeamStemRelation();
                    bRel.setStemPortion(
                            (yDir > 0) ? StemPortion.STEM_BOTTOM : StemPortion.STEM_TOP);

                    // Precise cross point
                    Point2D start = stemGlyph.getStartPoint(Orientation.VERTICAL);
                    Point2D stop = stemGlyph.getStopPoint(Orientation.VERTICAL);
                    Point2D crossPt = crossing(stemGlyph, beam);
                    bRel.setCrossPoint(crossPt);

                    // Abscissa -> beamPortion
                    // toLeft & toRight are >0 if within beam, <0 otherwise
                    double toLeft = crossPt.getX() - beamLimit.getX1();
                    double toRight = beamLimit.getX2() - crossPt.getX();
                    final double xGap;

                    if (beam instanceof FullBeamInter
                        && (Math.min(toLeft, toRight) > params.maxBeamInDx)) {
                        // It's a beam center connection
                        bRel.setBeamPortion(BeamPortion.CENTER);
                        xGap = 0;
                    } else {
                        // It's a beam side connection, which one?
                        if (toLeft < toRight) {
                            bRel.setBeamPortion(BeamPortion.LEFT);
                            xGap = Math.max(0, -toLeft);
                        } else {
                            bRel.setBeamPortion(BeamPortion.RIGHT);
                            xGap = Math.max(0, -toRight);
                        }
                    }

                    // Ordinate
                    final double yGap = (yDir > 0) ? Math.max(0, crossPt.getY() - stop.getY())
                            : Math.max(0, start.getY() - crossPt.getY());

                    bRel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

                    if (bRel.getGrade() >= bRel.getMinGrade()) {
                        sig.addEdge(beam, stemInter, bRel);
                        logger.debug("{} {} {}", head, corner, bRel);
                    } else {
                        bRel = null;
                    }
                }

                return bRel;
            }

            //-----------------//
            // connectHeadStem //
            //-----------------//
            /**
             * (Try to) connect head and stem.
             *
             * @param headSection the head section found, if any
             * @param stemInter   the stem interpretation to connect
             * @return the head-stem relation or null
             */
            private HeadStemRelation connectHeadStem (Section headSection,
                                                      StemInter stemInter)
            {
                // New relation head -> stem (if not yet present)
                HeadStemRelation hRel = (HeadStemRelation) sig.getRelation(
                        head,
                        stemInter,
                        HeadStemRelation.class);

                if (hRel == null) {
                    hRel = new HeadStemRelation();
                    hRel.setHeadSide(corner.hSide);

                    if (head.isVip() && stemInter.isVip()) {
                        logger.info("VIP connectHeadStem {} & {}", head, stemInter);
                    }

                    final Glyph stemGlyph = stemInter.getGlyph();
                    final Rectangle stemBox = stemGlyph.getBounds();
                    final double xGap;
                    final double yGap;

                    if (headSection != null) {
                        // xGap computed on head section
                        // yGap measured between head section and stem glyph
                        Rectangle runBox = getRunBox(headSection, corner.hSide);
                        xGap = xDir * (runBox.x - refPt.getX());

                        int overlap = GeoUtil.yOverlap(runBox, stemBox);
                        yGap = Math.abs(Math.min(overlap, 0));
                    } else {
                        // Use stem line to compute both xGap and yGap
                        Point2D start = stemGlyph.getStartPoint(VERTICAL);
                        Point2D stop = stemGlyph.getStopPoint(VERTICAL);
                        Point2D crossPt = LineUtil.intersectionAtY(start, stop, refPt.getY());
                        xGap = xDir * (crossPt.getX() - refPt.getX());

                        if (refPt.getY() < start.getY()) {
                            yGap = start.getY() - refPt.getY();
                        } else if (refPt.getY() > stop.getY()) {
                            yGap = refPt.getY() - stop.getY();
                        } else {
                            yGap = 0;
                        }
                    }

                    hRel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

                    if (hRel.getGrade() >= hRel.getMinGrade()) {
                        // Determine stem portion (with 1/3 head margin)
                        if (yDir > 0) {
                            if (stemBox.y >= (headBox.y - (headBox.height / 3))) {
                                hRel.setStemPortion(StemPortion.STEM_TOP);
                            } else {
                                hRel.setStemPortion(StemPortion.STEM_MIDDLE);
                            }
                        } else {
                            if ((stemBox.y + stemBox.height) <= (headBox.y + headBox.height
                                                                 + (headBox.height / 3))) {
                                hRel.setStemPortion(StemPortion.STEM_BOTTOM);
                            } else {
                                hRel.setStemPortion(StemPortion.STEM_MIDDLE);
                            }
                        }

                        sig.addEdge(head, stemInter, hRel);

                        if (stemInter.isVip()) {
                            logger.info("VIP {} {} {}", head, corner, hRel);
                        }
                    } else {
                        if (stemInter.isVip()) {
                            logger.info(
                                    "VIP failed connection {} to {} {} {}",
                                    stemInter,
                                    head,
                                    corner,
                                    hRel.getDetails());
                        }

                        hRel = null;
                    }
                }

                return hRel;
            }

            //-----------------//
            // createStemInter //
            //-----------------//
            /**
             * (Try to) create stem interpretation with proper grade.
             *
             * @param items the sequence of items (seeds / chunks) that
             *              compose the stem
             * @return the proper stem interpretation or null if too weak
             */
            private StemInter createStemInter (List<Glyph> items)
            {
                GlyphNest nest = system.getSheet().getNest();
                final Glyph stem = (items.size() == 1) ? items.get(0)
                        : nest.buildGlyph(items, true, Glyph.Linking.NO_LINK);

                if (stem.isVip()) {
                    logger.info("VIP createStemInter for {}", stem);
                }

                // Stem interpretation (if not yet present)
                StemInter stemInter = (StemInter) sig.getInter(stem, StemInter.class);

                if (stemInter == null) {
                    GradeImpacts impacts = system.verticalsBuilder.checkStem(stem);
                    double grade = impacts.getGrade();

                    if (grade >= StemInter.getMinGrade()) {
                        stemInter = new StemInter(stem, impacts);
                        sig.addVertex(stemInter);
                    }
                }

                return stemInter;
            }

            //----------//
            // crossing //
            //----------//
            /**
             * Compute the crossing point between a stem and a beam.
             *
             * @param stemGlyph the stem (glyph)
             * @param beam      the beam
             * @return the precise crossing point
             */
            private Point2D crossing (Glyph stemGlyph,
                                      AbstractBeamInter beam)
            {
                Point2D start = stemGlyph.getStartPoint(Orientation.VERTICAL);
                Point2D stop = stemGlyph.getStopPoint(Orientation.VERTICAL);
                Line2D beamLimit = getLimit(beam);

                return LineUtil.intersection(start, stop, beamLimit.getP1(), beamLimit.getP2());
            }

            //------------//
            // getContrib //
            //------------//
            /**
             * Report the (vertical) contribution of a rectangle to the
             * filling of white space above or below the head.
             *
             * @param box the rectangle to check
             * @return the corresponding height within white space
             */
            private int getContrib (Rectangle box)
            {
                return Math.max(0, GeoUtil.yOverlap(yRange, box));
            }

            //------------//
            // getInPoint //
            //------------//
            /**
             * Report the reference point slightly translated to the
             * interior of the head, to catch stem candidates.
             *
             * @return the inner refPt
             */
            private Point2D getInPoint ()
            {
                return new Point2D.Double(refPt.getX() - (xDir * params.maxHeadInDx), refPt.getY());
            }

            //----------//
            // getLimit //
            //----------//
            /**
             * Report proper beam limit, according to corner direction
             *
             * @param beam the beam or hook of interest
             * @return the top or bottom beam limit, according to dir
             */
            private Line2D getLimit (AbstractBeamInter beam)
            {
                return beam.getBorder(corner.vSide.opposite());
            }

            //-----------//
            // getLuArea //
            //-----------//
            /**
             * Define the lookup area on given corner, knowing the
             * reference point of the entity (head).
             * Global slope is used (plus and minus slopeMargin).
             *
             * @return the lookup area
             */
            private Area getLuArea ()
            {
                final double slope = skew.getSlope();
                final double dSlope = -xDir * yDir * params.slopeMargin;

                final Point2D outPt = getOutPoint();
                final Point2D inPt = getInPoint();

                // Look Up path, start by head horizontal segment
                final Path2D lu = new Path2D.Double();
                lu.moveTo(outPt.getX(), outPt.getY());
                lu.lineTo(inPt.getX(), inPt.getY());

                // Then segment away from head (system limit)
                final Rectangle systemBox = system.getBounds();
                final double yLimit = (yDir > 0) ? systemBox.getMaxY() : systemBox.getMinY();
                final double dy = yLimit - outPt.getY();
                lu.lineTo(inPt.getX() + ((slope + dSlope) * dy), yLimit);
                lu.lineTo(outPt.getX() + ((slope - dSlope) * dy), yLimit);
                lu.closePath();

                // Attachment
                StringBuilder sb = new StringBuilder();
                sb.append((corner.vSide == TOP) ? "T" : "B");
                sb.append((corner.hSide == LEFT) ? "L" : "R");
                head.addAttachment(sb.toString(), lu);

                return new Area(lu);
            }

            //-------------//
            // getOutPoint //
            //-------------//
            /**
             * Report the reference point slightly translated to the
             * exterior of the head, to catch stem candidates.
             *
             * @return the outer refPt
             */
            private Point2D getOutPoint ()
            {
                return new Point2D.Double(
                        refPt.getX() + (xDir * params.maxHeadOutDx),
                        refPt.getY());
            }

            //-------------------//
            // getReferencePoint //
            //-------------------//
            /**
             * Compute head reference point for this corner (the point
             * where a stem could be connected).
             * For best precision, we use the related shape descriptor.
             *
             * @return the refPt
             */
            private Point2D getReferencePoint ()
            {
                AbstractNoteInter note = (AbstractNoteInter) head;
                ShapeDescriptor desc = note.getDescriptor();
                Anchor anchor = corner.stemAnchor();
                Point offset = desc.getOffset(anchor);

                final Point ref = headBox.getLocation();
                ref.translate(offset.x, offset.y);

                return ref;
            }

            //-----------//
            // getRunBox //
            //-----------//
            /**
             * Report the run box of the first or last run of the
             * provided section according to the desired side.
             *
             * @param section the section for which the side run is retrieved
             * @param side    the desired side
             * @return the run bounding box
             */
            private Rectangle getRunBox (Section section,
                                         HorizontalSide side)
            {
                final Run run = (xDir < 0) ? section.getFirstRun() : section.getLastRun();
                final int pos = (xDir < 0) ? section.getFirstPos() : section.getLastPos();

                return new Rectangle(pos, run.getStart(), 1, run.getLength());
            }

            //---------------//
            // getTargetLine //
            //---------------//
            /**
             * Build the best possible target line.
             * First, we use (head) refPt and (distant) targetPt to define a
             * theoretical line.
             * Then, we look for suitable seeds if any to refine the line.
             * The non-suitable seeds are removed from the collection.
             *
             * @return the best target line for stem
             */
            private Line2D getTargetLine ()
            {
                // Theoretical line
                Line2D theory = new Line2D.Double(refPt, targetPt);

                // Look for stems seeds
                seeds = sig.intersectedGlyphs(neighborSeeds, true, area);

                if (!seeds.isEmpty()) {
                    // Purge seeds that are too distant from theoretical line
                    for (Iterator<Glyph> it = seeds.iterator(); it.hasNext();) {
                        Glyph seed = it.next();
                        Point2D seedCenter = seed.getCentroid();
                        double dist = theory.ptLineDist(seedCenter);

                        if (dist > params.maxSeedJitter) {
                            it.remove();
                        }
                    }

                    // In case of overlap, simply keep the most contributive
                    List<Glyph> kept = new ArrayList<Glyph>();
                    sortByContrib(seeds);

                    StemLoop:
                    for (Glyph seed : seeds) {
                        Rectangle stemBox = seed.getBounds();

                        for (Glyph k : kept) {
                            if (GeoUtil.yOverlap(stemBox, k.getBounds()) > 0) {
                                continue StemLoop;
                            }
                        }

                        // No overlap
                        kept.add(seed);
                    }

                    seeds.retainAll(kept);

                    // Finally, define line based on seed(s) kept if any
                    if (!seeds.isEmpty()) {
                        sortByDistance(seeds);

                        Point2D start = seeds.get(0).getStartPoint(VERTICAL);
                        Point2D stop = seeds.get(seeds.size() - 1).getStopPoint(VERTICAL);

                        return new Line2D.Double(start, stop);
                    }
                }

                return theory;
            }

            //-------------//
            // getTargetPt //
            //-------------//
            /**
             * Compute the point where the (skewed) vertical from head
             * reference point crosses the provided limit.
             *
             * @param limit the end of the white space
             * @return the crossing point
             */
            private Point2D getTargetPt (Line2D limit)
            {
                final Point2D refPt2 = new Point2D.Double(
                        refPt.getX() - (100 * skew.getSlope()),
                        refPt.getY() + 100);

                return LineUtil.intersection(refPt, refPt2, limit.getP1(), limit.getP2());
            }

            //-----------//
            // getYRange //
            //-----------//
            /**
             * Compute the range to be covered by stem items
             *
             * @param yLimit the limit farthest from head
             * @return a range rectangle
             */
            private Rectangle getYRange (double yLimit)
            {
                return new Rectangle(
                        0, // x is irrelevant
                        (int) Math.rint((yDir > 0) ? refPt.getY() : yLimit),
                        0, // width is irrelevant
                        (int) Math.rint(Math.abs(yLimit - refPt.getY())));
            }

            //--------------//
            // includeItems //
            //--------------//
            /**
             * Include the stem items, one after the other.
             * We may have insufficient clean value for first items (resulting
             * in no intermediate StemInter created) but we must go on.
             *
             * @param items          the sequence of stem items
             * @param refY           the ordinate of head ref point
             * @param fatHeadSection the fat head section if any
             * @return the list of StemInter instances built
             */
            private List<StemInter> includeItems (List<Glyph> items,
                                                  double refY,
                                                  Section fatHeadSection)
            {
                double lastY = refY; // Current end of stem
                List<StemInter> allStemInters = new ArrayList<StemInter>();

                for (int i = 0; i < items.size(); i++) {
                    Glyph item = items.get(i);
                    Rectangle itemBox = item.getBounds();

                    // Are we past the beam limit (if any)?
                    if (getContrib(itemBox) == 0) {
                        break;
                    }

                    // Is gap with previous item acceptable?
                    final int itemY = (yDir > 0) ? itemBox.y : ((itemBox.y + itemBox.height)
                                                                - 1);
                    final double itemStart = (yDir > 0) ? Math.max(itemY, refY)
                            : Math.min(itemY, refY);
                    final double yGap = yDir * (itemStart - lastY);

                    if (yGap > params.maxYGap) {
                        break; // Too large gap
                    }

                    if ((i == 0) && (yGap > params.maxStemHeadGapY)) {
                        break; // Initial item too far from head
                    }

                    // Check minimum stem extension from head
                    double itemStop = itemY + (yDir * (itemBox.height - 1));
                    lastY = (yDir > 0) ? Math.max(lastY, itemStop) : Math.min(lastY, itemStop);

                    final double extension = Math.abs(lastY - refY);

                    if (extension < params.minStemExtension) {
                        continue;
                    }

                    // OK, build a stem interpretation with all items so far
                    List<Glyph> stemItems = items.subList(0, i + 1);
                    StemInter stemInter = createStemInter(stemItems);

                    if (stemInter != null) {
                        allStemInters.add(stemInter);
                        connectHeadStem(fatHeadSection, stemInter);
                    }
                }

                return allStemInters;
            }

            private void linkBeamsAndStems (List<AbstractBeamInter> beams,
                                            int groupStart,
                                            List<StemInter> stems)
            {
                for (int i = 0; i < beams.size(); i++) {
                    AbstractBeamInter beam = beams.get(i);

                    for (StemInter stem : stems) {
                        BeamStemRelation rel = connectBeamStem(beam, stem);

                        if (rel == null) {
                            continue;
                        }

                        if ((i == groupStart) && (groupStart < (beams.size() - 1))) {
                            // Extend stem connection till end of beam group
                            for (AbstractBeamInter next : beams.subList(
                                    groupStart + 1,
                                    beams.size())) {
                                BeamStemRelation r = (BeamStemRelation) sig.getRelation(
                                        next,
                                        stem,
                                        BeamStemRelation.class);

                                if (r == null) {
                                    r = new BeamStemRelation();
                                    r.setStemPortion(rel.getStemPortion());
                                    r.setBeamPortion(rel.getBeamPortion());
                                    r.setCrossPoint(crossing(stem.getGlyph(), beam));
                                    r.setGrade(rel.getGrade());
                                    sig.addEdge(next, stem, r);
                                }
                            }
                        }
                    }

                    if (i == groupStart) {
                        break;
                    }
                }
            }

            //-------------//
            // lookupBeams //
            //-------------//
            /**
             * Look for beam interpretations in the lookup area.
             * We stop at (group of) first good beam interpretation, if any.
             *
             * @param beams      (output) list to be populated, ordered by distance from head
             * @param candidates (input) collection of candidate beams
             * @return index of first good beam in the beams list
             */
            private int lookupBeams (List<AbstractBeamInter> beams,
                                     List<Inter> candidates)
            {
                // Reject beam candidates which are not in corner direction
                // (this can happen because of beam bounding rectangle)
                for (Iterator<Inter> it = candidates.iterator(); it.hasNext();) {
                    AbstractBeamInter b = (AbstractBeamInter) it.next();

                    if ((yDir * (getTargetPt(getLimit(b)).getY() - refPt.getY())) <= 0) {
                        it.remove();
                    }
                }

                // Sort candidates by distance from head
                Collections.sort(
                        candidates,
                        new Comparator<Inter>()
                {
                    @Override
                    public int compare (Inter i1,
                                        Inter i2)
                    {
                        AbstractBeamInter b1 = (AbstractBeamInter) i1;
                        AbstractBeamInter b2 = (AbstractBeamInter) i2;

                        return Double.compare(
                                yDir * (getTargetPt(getLimit(b1)).getY() - refPt.getY()),
                                yDir * (getTargetPt(getLimit(b2)).getY() - refPt.getY()));
                    }
                        });

                // Build the list of beams
                AbstractBeamInter goodBeam = null;

                for (Inter inter : candidates) {
                    AbstractBeamInter beam = (AbstractBeamInter) inter;

                    if (goodBeam == null) {
                        // Check if beam is far enough from head
                        final Point2D beamPt = getTargetPt(getLimit(beam));
                        final double distToBeam = yDir * (beamPt.getY() - refPt.getY());

                        if (distToBeam < params.minHeadBeamDistance) {
                            continue;
                        }

                        beams.add(beam);

                        // Truncate at first good encountered beam, if any, taken with its group.
                        // Nota: We could shrink the lu area accordingly, however we impose area
                        // containment for stem sections, so let's stay with the system limit.
                        if (beam.isGood()) {
                            goodBeam = beam;
                        }
                    } else {
                        // We are within good beam group, check end of it
                        AbstractBeamInter lastBeam = beams.get(beams.size() - 1);

                        if (areGroupCompatible(lastBeam, beam)) {
                            beams.add(beam);
                        } else {
                            break;
                        }
                    }
                }

                return beams.indexOf(goodBeam);
            }

            //--------------//
            // lookupChunks //
            //--------------//
            /**
             * Retrieve chunks of stems out of additional compatible sections (not part
             * of stem seeds) found in the corner.
             *
             * @param fatHeadSection (output) specific section, part of head rather than stem
             * @return the collection of chunks found
             */
            private List<Glyph> lookupChunks (Wrapper<Section> fatHeadSection)
            {
                // Look up suitable sections
                List<Section> sections = lookupSections(fatHeadSection);

                // Aggregate these sections into glyphs & check them
                GlyphNest nest = system.getSheet().getNest();
                List<Glyph> chunks = nest.retrieveGlyphs(
                        sections,
                        GlyphLayer.DEFAULT,
                        false,
                        Glyph.Linking.NO_LINK);

                // Remove useless glyphs and put wide glyphs apart
                List<Glyph> wides = new ArrayList<Glyph>();

                for (Iterator<Glyph> it = chunks.iterator(); it.hasNext();) {
                    Glyph chunk = it.next();
                    Rectangle chunkBox = chunk.getBounds();

                    if (getContrib(chunkBox) == 0) {
                        it.remove();
                    } else {
                        int meanWidth = (int) Math.rint(
                                chunk.getMeanThickness(Orientation.VERTICAL));

                        if (meanWidth > params.maxStemThickness) {
                            wides.add(chunk);
                            it.remove();
                        }
                    }
                }

                // For too wide chunks we just keep the biggest section
                if (!wides.isEmpty()) {
                    int interline = scale.getInterline();
                    GlyphLayer layer = GlyphLayer.DEFAULT;

                    for (Glyph wide : wides) {
                        List<Section> members = new ArrayList<Section>(wide.getMembers());
                        Collections.sort(members, Section.reverseWeightComparator);

                        Glyph glyph = new BasicGlyph(interline, layer);
                        glyph.addSection(members.get(0), Glyph.Linking.NO_LINK);
                        chunks.add(glyph);
                    }
                }

                return chunks;
            }

            //----------------//
            // lookupSections //
            //----------------//
            /**
             * To complement stem seeds, look up for relevant sections
             * in the lookup area that could be part of a global stem.
             *
             * @param fatHeadSection (potential output) a thick section, part of
             *                       head, that accounts for stem range
             * @return the collection of additional sections found
             */
            private List<Section> lookupSections (Wrapper<Section> fatHeadSection)
            {
                // Horizontal line around refPt
                final Point2D outPt = getOutPoint();
                final Point2D inPt = getInPoint();
                final Line2D hLine = (corner.hSide == LEFT) ? new Line2D.Double(outPt, inPt)
                        : new Line2D.Double(inPt, outPt);
                final int refY = (int) Math.rint(refPt.getY());
                final List<Section> sections = new ArrayList<Section>();
                final List<Section> headSections = new ArrayList<Section>();

                // Widen head box with typical stem width
                final Rectangle wideHeadBox = head.getBounds();
                wideHeadBox.grow(scale.getMainStem(), 0);

                // Browse both vertical and horizontal sections in the system
                for (Collection<Section> collection : Arrays.asList(
                        system.getVerticalSections(),
                        system.getHorizontalSections())) {
                    SectionLoop:
                    for (Section section : collection) {
                        Rectangle sectBox = section.getBounds();

                        if (section.isVip()) {
                            logger.info("VIP {}", section);
                        }

                        // Check intersection at least
                        if (!area.intersects(sectBox)) {
                            continue SectionLoop;
                        }

                        // Containment is mandatory except for a head section
                        // (a section that intersects head glyph)
                        if (!area.contains(sectBox)) {
                            if (!sectBox.intersects(wideHeadBox)
                                || !GeoUtil.yEmbraces(sectBox, refY)) {
                                continue SectionLoop;
                            }

                            // Section is likely to be part of head itself.
                            // Even if too thick, use part of its length as stem
                            // portion (if it does not overlap stem seeds)
                            if (section.isVertical() && (sectBox.width > params.maxStemThickness)) {
                                // Consider the touching run
                                Rectangle runBox = getRunBox(section, corner.hSide);

                                for (Glyph seed : seeds) {
                                    if (GeoUtil.yOverlap(runBox, seed.getBounds()) > 0) {
                                        continue SectionLoop;
                                    }
                                }

                                // Make sure this run is within area width
                                if (GeoUtil.xEmbraces(hLine, runBox.x)) {
                                    // Use head section that brings best contrib
                                    if (fatHeadSection.value != null) {
                                        Rectangle otherBox = getRunBox(
                                                fatHeadSection.value,
                                                corner.hSide);

                                        if (getContrib(runBox) > getContrib(otherBox)) {
                                            fatHeadSection.value = section;
                                        }
                                    } else {
                                        fatHeadSection.value = section;
                                    }
                                }

                                continue SectionLoop;
                            }

                            // A headSection must provide significant contribution
                            // otherwise it belongs to the head, not to the stem.
                            int sectContrib = getContrib(sectBox);

                            if (sectContrib < params.minHeadSectionContribution) {
                                logger.debug("Discarding tiny headSection {}", section);

                                continue SectionLoop;
                            }

                            headSections.add(section);
                        }

                        // Contraint section width <= stem width
                        if (sectBox.width > params.maxStemThickness) {
                            continue SectionLoop;
                        }

                        // A section which overlaps an existing seed is useless
                        for (Glyph seed : seeds) {
                            if (GeoUtil.yOverlap(sectBox, seed.getBounds()) > 0) {
                                continue SectionLoop;
                            }
                        }

                        // Check section distance to target line
                        Point center = section.getCentroid();
                        double dist = targetLine.ptLineDist(center);

                        if (dist <= params.maxSectionJitter) {
                            sections.add(section);
                        }
                    }
                }

                // Handle overlap between standard sections and fatHeadSection
                // if any, by keeping the most contributive one
                if (fatHeadSection.value != null) {
                    final Rectangle runBox = getRunBox(fatHeadSection.value, corner.hSide);
                    final int runContrib = getContrib(runBox);

                    for (Iterator<Section> it = sections.iterator(); it.hasNext();) {
                        final Section section = it.next();
                        final Rectangle sctBox = section.getBounds();

                        if (GeoUtil.yOverlap(runBox, sctBox) > 0) {
                            if (getContrib(sctBox) <= runContrib) {
                                it.remove();
                            } else {
                                logger.debug("Cancelling fatHeadSection {}", fatHeadSection);
                                fatHeadSection.value = null;

                                break;
                            }
                        }
                    }
                }

                // Handle the case of several head sections that might result
                // in a too thick glyph
                headSections.retainAll(sections);

                if (headSections.size() > 1) {
                    // Keep only the most contributive section
                    Section bestSection = null;
                    int bestContrib = Integer.MIN_VALUE;

                    for (Section section : headSections) {
                        int contrib = getContrib(section.getBounds());

                        if (contrib > bestContrib) {
                            bestContrib = contrib;
                            bestSection = section;
                        }
                    }

                    sections.removeAll(headSections);
                    headSections.clear();
                    headSections.add(bestSection);
                    sections.addAll(headSections);
                }

                return sections;
            }

            //---------------//
            // sortByContrib //
            //---------------//
            /**
             * Sort stem items by their decreasing contribution.
             */
            private void sortByContrib (List<Glyph> glyphs)
            {
                Collections.sort(
                        glyphs,
                        new Comparator<Glyph>()
                {
                    @Override
                    public int compare (Glyph o1,
                                        Glyph o2)
                    {
                        // Sort by decreasing contribution
                        int c1 = getContrib(o1.getBounds());
                        int c2 = getContrib(o2.getBounds());

                        return Integer.signum(c2 - c1);
                    }
                        });
            }

            //----------------//
            // sortByDistance //
            //----------------//
            /**
             * Sort stem items by their increasing vertical distance
             * from head.
             */
            private void sortByDistance (List<Glyph> glyphs)
            {
                Collections.sort(glyphs, (yDir > 0) ? Glyph.byOrdinate : Glyph.byReverseOrdinate);
            }
        }
    }

    //--------------//
    // ShareChecker //
    //--------------//
    /**
     * Checks and cleans up an ensemble of stems around a head.
     */
    private class ShareChecker
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Inter head;

        private final List<HeadStemRelation> rels = new ArrayList<HeadStemRelation>();

        //~ Constructors ---------------------------------------------------------------------------
        public ShareChecker (Inter head)
        {
            this.head = head;
        }

        //~ Methods --------------------------------------------------------------------------------
        public void check (List<Inter> stems)
        {
            // Retrieve stem relations
            rels.clear();

            for (Inter stem : stems) {
                HeadStemRelation rel = (HeadStemRelation) sig.getRelation(
                        head,
                        stem,
                        HeadStemRelation.class);

                if (rel != null) {
                    rels.add(rel);
                }
            }

            if (head.isVip() && (rels.size() >= 2)) {
                logger.info("VIP {} with multiple stems {}", head, stems);
            }

            // Get down to a maximum of 2 stems
            while (rels.size() > 2) {
                if (!discardLargeGap()) {
                    discardWeakerStem();
                }
            }

            // Do we still have a conflict to solve?
            if (rels.size() == 2) {
                // If not canonical, discard one of the stem link
                if (!isCanonicalShare()) {
                    if (!discardLargeGap()) {
                        discardWeakerStem();
                    }
                }
            }
        }

        /**
         * Discard the stem link with largest y gap, if any
         *
         * @return true if such stem link was found
         */
        private boolean discardLargeGap ()
        {
            double worstGap = 0;
            HeadStemRelation worstRel = null;

            for (HeadStemRelation rel : rels) {
                double yGap = rel.getYDistance();

                if (worstGap < yGap) {
                    worstGap = yGap;
                    worstRel = rel;
                }
            }

            if (worstGap > constants.yGapTiny.getValue()) {
                if (head.isVip()) {
                    logger.info("VIP {} discarding gap {}", head, sig.getEdgeTarget(worstRel));
                }

                rels.remove(worstRel);
                sig.removeEdge(worstRel);

                return true;
            } else {
                return false;
            }
        }

        /**
         * Discard the link to the stem with lower intrinsic grade.
         */
        private void discardWeakerStem ()
        {
            double worstGrade = Double.MAX_VALUE;
            HeadStemRelation worstRel = null;

            for (HeadStemRelation rel : rels) {
                double grade = sig.getEdgeTarget(rel).getGrade();

                if (worstGrade > grade) {
                    worstGrade = grade;
                    worstRel = rel;
                }
            }

            if (head.isVip()) {
                logger.info("VIP {} discarding weaker {}", head, sig.getEdgeTarget(worstRel));
            }

            rels.remove(worstRel);
            sig.removeEdge(worstRel);
        }

        /**
         * Check whether this is the canonical "shared" config.
         * (STEM_TOP on head LEFT and STEM_BOTTOM on head RIGHT)
         *
         * @return true if canonical
         */
        private boolean isCanonicalShare ()
        {
            boolean left = false;
            boolean right = false;

            for (HeadStemRelation rel : rels) {
                StemPortion portion = rel.getStemPortion();
                HorizontalSide side = rel.getHeadSide();
                double yGap = rel.getYDistance();

                if (yGap <= constants.yGapTiny.getValue()) {
                    if (portion == STEM_TOP) {
                        if (side == LEFT) {
                            left = true;
                        }
                    } else if (portion == STEM_BOTTOM) {
                        if (side == RIGHT) {
                            right = true;
                        }
                    }
                }
            }

            return left && right;
        }
    }
}
