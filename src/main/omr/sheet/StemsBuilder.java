//----------------------------------------------------------------------------//
//                                                                            //
//                           S t e m s B u i l d e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphsBuilder;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.math.GeoOrder;
import omr.math.GeoUtil;
import omr.math.LineUtil;

import omr.run.Orientation;
import omr.run.Run;

import omr.sig.BasicExclusion;
import omr.sig.BeamInter;
import omr.sig.BeamPortion;
import omr.sig.BeamStemRelation;
import omr.sig.Exclusion;
import omr.sig.Exclusion.Cause;
import omr.sig.Grades;
import omr.sig.HeadStemRelation;
import omr.sig.Inter;
import omr.sig.Relation;
import omr.sig.SIGraph;
import omr.sig.StemInter;
import omr.sig.StemPortion;
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
 * At this point, both black heads and beams have been identified thanks to
 * spots processing, and void heads thanks to chamfer matching.
 * We don't have flags yet at this time.
 *
 * <p>
 * A stem is expected to be horizontally connected on the left or right
 * side of the head and vertically connected as well.
 * Such connections are looked up in the 4 corners of every head.
 * In poor-quality scores, stems can lack many pixels, resulting in vertical
 * gaps between stem parts and between head and nearest stem part, so we
 * must accept such potential gaps (even if we lower the resulting
 * interpretation grade).
 * However we can be much more strict for the horizontal gap of the connection.
 *
 * <p>
 * A stem can be the aggregation of several items: stem seeds (built from
 * long vertical sticks) and chunks (glyphs built from sections found in the
 * specific corner), all being separated by vertical gaps.
 * Up to which point should we try to accept vertical gaps and increase a stem
 * length starting from a head?<ol>
 * <li>If there is a beam in the corner, try a stem that at least reaches the
 * beam.</li>
 * <li>Use a similar approach for the case of flag (if the flag is in the right
 * direction), except that we don't have identified flags yet!</li>
 * <li>If no obvious limit exists, accept all gaps in sequence until a too large
 * gap is encountered.</li>
 * </ol>
 *
 * <p>
 * Every sequence of stem items built from the head should be evaluated and
 * recorded as a separate stem interpretation in the SIG.
 * Stem evaluation could use criteria such as straightness, width (compared
 * with others), etc plus the amount of white space of all gaps and the size of
 * the largest gap.
 * We could also analyze in the whole page the population of "good" stems to
 * come up with most common stem lengths according to stem configurations,
 * and support stem interpretations that match these most common lengths.
 *
 * <p>
 * Stem-head connection uses criteria based on xGap and yGap at reference point.
 * Stem-beam connection uses yGap (and xGap in the case of beam side
 * connection).
 *
 * <p>
 * TODO: In a beam group, should we create a relation with Beams #2, #3, etc?
 * Relation between stem & beam (with which grade?) or between beams (support)
 *
 * <p>
 * TODO: What if a beam inter is a false beam? We should not limit search to
 * beam limit, but always use system limit.
 *
 * @author Hervé Bitteur
 */
public class StemsBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            StemsBuilder.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
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

    /** Heads for this system. */
    private List<Inter> systemHeads;

    /** Beams for this system. */
    private List<Inter> systemBeams;

    /** Stems interpretations for this system. */
    private List<Inter> systemStems;

    //~ Constructors -----------------------------------------------------------
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
        headSymbolDim = symbol.getDimension(
                MusicFont.getFont(scale.getInterline()));
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-----------//
    // linkStems //
    //-----------//
    /**
     * Link stems to suitable heads and beams in the system.
     */
    public void linkStems ()
    {
        StopWatch watch = new StopWatch("StemsBuilder S#" + system.getId());
        watch.start("collections");
        // The sorted stem seeds for this system
        systemSeeds = getSystemSeeds();

        // The beam interpretations for this system
        systemBeams = sig.inters(Shape.BEAM);
        Collections.sort(systemBeams, Inter.byAbscissa);

        // The sorted head interpretations for this system
        systemHeads = sig.inters(
                Arrays.asList(Shape.NOTEHEAD_BLACK, Shape.NOTEHEAD_VOID));
        Collections.sort(systemHeads, Inter.byAbscissa);

        // First phase, look around heads for stems (and beams if any)
        watch.start("phase #1");

        for (Inter head : systemHeads) {
            new HeadLinker(head).linkAllCorners();
        }

        // Second phase, look for reuse of existing stems interpretations
        watch.start("phase #2");
        systemStems = sig.inters(Shape.STEM);

        for (Inter head : systemHeads) {
            new HeadLinker(head).reuseAllCorners();
        }

        // Finally, handle stems mutual exclusions
        watch.start("exclusions");
        performMutualExclusions();

        if (constants.printWatch.isSet()) {
            watch.print();
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
                Rectangle oneBox = one.getGlyph()
                        .getBounds();

                for (Inter two : stems.subList(i + 1, size)) {
                    Rectangle twoBox = two.getGlyph()
                            .getBounds();

                    // Is there an overlap between stems one & two?
                    if (oneBox.intersects(twoBox)) {
                        Exclusion exc = new BasicExclusion(Cause.OVERLAP);
                        sig.addEdge(one, two, exc);
                        count++;
                    }
                }
            }

            performStemNoteExclusions(stems);
        } finally {
            logger.debug(
                    "S#{} stems: {} exclusions: {}",
                    system.getId(),
                    size,
                    count);
        }
    }

    //---------------------------//
    // performStemNoteExclusions //
    //---------------------------//
    private void performStemNoteExclusions (List<Inter> stems)
    {
        List<Inter> notes = sig.inters(
                ShapeSet.shapesOf(
                        ShapeSet.NoteHeads.getShapes(),
                        Arrays.asList(Shape.WHOLE_NOTE)));

        for (Inter inter : stems) {
            StemInter stem = (StemInter) inter;
            Rectangle stemBox = stem.getBounds();

            NoteLoop:
            for (Inter note : notes) {
                if (note.getBounds()
                        .intersects(stemBox)) {
                    // Is there a connection?
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

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        final Scale.Fraction maxOutDx = new Scale.Fraction(
                0.1,
                "Maximum horizontal gap between head/beam and stem");

        final Scale.Fraction maxInDx = new Scale.Fraction(
                0.2,
                "Maximum horizontal overlap between head/beam and stem");

        final Scale.Fraction xMargin = new Scale.Fraction(
                1.0,
                "Abscissa margin when looking for neighboring stem seeds");

        final Constant.Double slopeMargin = new Constant.Double(
                "tangent",
                0.02,
                "Margin around slope");

        final Scale.Fraction maxStemHeadGapX = new Scale.Fraction(
                0.2,
                "Maximum horizontal gap between head and nearest stem segment");

        final Scale.Fraction maxStemHeadGapY = new Scale.Fraction(
                0.8,
                "Maximum vertical gap between head and nearest stem segment");

        final Scale.Fraction maxYGap = new Scale.Fraction(
                2.0,
                "Maximum vertical gap between stem segments");

        final Scale.Fraction minHeadSectionContribution = new Scale.Fraction(
                0.2,
                "Minimum stem contribution for a section near head");

        final Scale.Fraction minStemExtension = new Scale.Fraction(
                0.8,
                "Minimum length counted from head to end of stem");

        final Scale.Fraction minHeadBeamDistance = new Scale.Fraction(
                0.5,
                "Minimum vertical distance between head and beam");

        final Scale.Fraction minLongStemLength = new Scale.Fraction(
                3,
                "Minimum length for a long stem");

        final Scale.Fraction maxDistanceToLine = new Scale.Fraction(
                0.2,
                "Maximum mean distance to average stem line");

        final Scale.Fraction maxInterBeamGap = new Scale.Fraction(
                1.0,
                "Maximum vertical gap between two consecutive beams of the same group");

    }

    //------------//
    // HeadLinker //
    //------------//
    /**
     * A HeadLinker tries to establish links from a head to nearby
     * stem interpretations, processing all 4 corners.
     *
     * TODO: Insert exclusion between head and "crossing" stems that intersect
     * to far (abscissa-wise) inside the head.
     */
    private class HeadLinker
    {
        //~ Instance fields ----------------------------------------------------

        /** The head interpretation being processed. */
        private final Inter head;

        /** Underlying glyph. */
        private final Glyph headGlyph;

        /** Head bounding box. */
        private final Rectangle headBox;

        /** All beams interpretations in head vicinity. */
        private List<Inter> neighborBeams;

        /** All stems seeds in head vicinity. */
        private List<Glyph> neighborSeeds;

        /** All stems interpretations in head vicinity. */
        private List<Inter> neighborStems;

        //~ Constructors -------------------------------------------------------
        public HeadLinker (Inter head)
        {
            this.head = head;
            headGlyph = head.getGlyph();
            headBox = head.getBounds();
        }

        //~ Methods ------------------------------------------------------------
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
         * From the provided collection of interpretations, retrieve
         * all those located in the vicinity of the provided central
         * interpretation.
         *
         * @param inter  the central interpretation
         * @param inters the collection of interpretations to search
         * @return the set of neighboring interpretations
         */
        private List<Inter> getNeighboringInters (Inter inter,
                                                  List<Inter> inters)
        {
            // Retrieve neighboring inters, using a box of system height and
            // sufficiently wide, just to play with a limited number of inters.
            Rectangle interBox = inter.getBounds();
            Rectangle systemBox = system.getBounds();
            Rectangle fatBox = new Rectangle(
                    interBox.x,
                    systemBox.y,
                    interBox.width,
                    systemBox.height);
            fatBox.grow(params.xMargin, 0);

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
            fatBox.grow(params.xMargin, 0);

            return sig.intersectedGlyphs(systemSeeds, true, fatBox);
        }

        //~ Inner Classes ------------------------------------------------------
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
            //~ Instance fields ------------------------------------------------

            /** The corner being processed. */
            private final Corner corner;

            /** Direction of ordinates going away from head. */
            private final int dir;

            /** The reference point for the corner. */
            private final Point2D refPt;

            /** The look up area for the corner. */
            private final Area area;

            /** The stems items found in the corner area. */
            private List<Glyph> items;

            /** Ordinate range between refPt & limit. */
            private Rectangle yRange;

            //~ Constructors ---------------------------------------------------
            public CornerLinker (Corner corner)
            {
                this.corner = corner;

                dir = (corner.vSide == BOTTOM) ? 1 : (-1);
                refPt = getReferencePoint();
                area = getLuArea();
            }

            //~ Methods --------------------------------------------------------
            //------//
            // link //
            //------//
            /**
             * Look for all acceptable stems interpretations that can
             * be connected to the head in the desired corner.
             * Stop the search at the first good beam found or at the first non
             * acceptable yGap, whichever comes first.
             */
            public void link ()
            {
                // Compute target end of stem
                Point2D targetPt;

                // Use system limit
                Rectangle systemBox = system.getBounds();
                int sysY = (dir > 0) ? (systemBox.y + systemBox.height)
                        : systemBox.y;
                targetPt = getTargetPt(new Line2D.Double(0, sysY, 100, sysY));

                // Look for beams in the corner
                List<BeamInter> beams = new ArrayList<BeamInter>();
                int goodIndex = lookupBeams(beams);

                // If we have a good beam, stop at the end of beam group
                // using the good beam for the target point
                if (goodIndex != -1) {
                    targetPt = getTargetPt(getLimit(beams.get(goodIndex)));
                }

                ///Line2D line = new Line2D.Double(refPt, targetPt);
                ///headGlyph.addAttachment("t" + corner.getId(), line);
                yRange = getYRange(targetPt.getY());

                // Look for all stems seeds in the corner
                items = lookupSeeds();

                // Look for additional chunks built out of sections found.
                // Assign special role to a fat section part of head (if any)
                Wrapper<Section> fatHeadSection = new Wrapper<Section>();
                List<Glyph> chunks = lookupChunks(fatHeadSection);

                // Aggregate seeds and chunks up to the limit
                if (!chunks.isEmpty()) {
                    items.addAll(chunks);
                    sortByDistance(items);
                }

                double refY = refPt.getY(); // Reference ordinate

                if (fatHeadSection.value != null) {
                    // Shift the reference ordinate accordingly
                    Rectangle runBox = getRunBox(
                            fatHeadSection.value,
                            corner.hSide);
                    int contrib = getContrib(runBox);

                    if (contrib > 0) {
                        refY += (dir * contrib);
                    }
                }

                // Include each item (seed / chunk) until limit is reached
                List<StemInter> stems = includeItems(
                        items,
                        refY,
                        fatHeadSection.value);

                // Beam - Stem connection(s)?
                if (!beams.isEmpty() && !stems.isEmpty()) {
                    for (int i = 0; i < beams.size(); i++) {
                        BeamInter beam = beams.get(i);

                        for (StemInter stem : stems) {
                            BeamStemRelation rel = connectBeamStem(beam, stem);

                            if (rel == null) {
                                continue;
                            }

                            if ((i == goodIndex)
                                && (goodIndex < (beams.size() - 1))) {
                                // Extend stem connection till end of beam group
                                for (BeamInter next : beams.subList(
                                        goodIndex + 1,
                                        beams.size())) {
                                    BeamStemRelation r = (BeamStemRelation) sig.getRelation(
                                            next,
                                            stem,
                                            BeamStemRelation.class);

                                    if (r == null) {
                                        r = new BeamStemRelation();
                                        r.setStemPortion(rel.getStemPortion());
                                        r.setBeamPortion(rel.getBeamPortion());
                                        r.setGrade(rel.getGrade());
                                        sig.addEdge(next, stem, r);
                                    }
                                }
                            }
                        }

                        if (i == goodIndex) {
                            break;
                        }
                    }
                }
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

            //---------------//
            // areCompatible //
            //---------------//
            /**
             * Check whether the two beams can be consecutive beams
             * in the same beam group, using ordinate gap.
             *
             * @param one current beam
             * @param two following beam, in 'dir' direction
             * @return true if OK
             */
            private boolean areCompatible (BeamInter one,
                                           BeamInter two)
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
             * @param beam      the beam interpretation
             * @param stemInter the stem interpretation
             * @return the beam stem relation if successful, null otherwise
             */
            private BeamStemRelation connectBeamStem (BeamInter beam,
                                                      StemInter stemInter)
            {
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
                            (dir > 0) ? StemPortion.STEM_BOTTOM : StemPortion.STEM_TOP);

                    // Precise cross point
                    Point2D start = stemGlyph.getStartPoint(
                            Orientation.VERTICAL);
                    Point2D stop = stemGlyph.getStopPoint(Orientation.VERTICAL);
                    Point2D crossPt = LineUtil.intersection(
                            start,
                            stop,
                            beamLimit.getP1(),
                            beamLimit.getP2());
                    bRel.setCrossPoint(crossPt);

                    // Abscissa
                    double toLeft = beamLimit.getX1() - crossPt.getX();
                    final double xGap;

                    if (Math.abs(toLeft) < scale.getInterline()) {
                        bRel.setBeamPortion(BeamPortion.LEFT);
                        xGap = Math.max(0, toLeft);
                    } else {
                        double toRight = beamLimit.getX2() - crossPt.getX();

                        if (Math.abs(toRight) < scale.getInterline()) {
                            bRel.setBeamPortion(BeamPortion.RIGHT);
                            xGap = Math.max(0, -toRight);
                        } else {
                            bRel.setBeamPortion(BeamPortion.CENTER);
                            xGap = 0;
                        }
                    }

                    // Ordinate
                    final double yGap = (dir > 0)
                            ? Math.max(
                                    0,
                                    crossPt.getY() - stop.getY())
                            : Math.max(
                                    0,
                                    start.getY() - crossPt.getY());

                    bRel.setDistances(
                            scale.pixelsToFrac(xGap),
                            scale.pixelsToFrac(yGap));

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

                    final Glyph stemGlyph = stemInter.getGlyph();
                    final Rectangle stemBox = stemGlyph.getBounds();
                    final double xGap;
                    final double yGap;

                    if (headSection != null) {
                        // xGap computed on head section
                        // yGap measured between head section and stem glyph
                        Rectangle runBox = getRunBox(headSection, corner.hSide);
                        xGap = Math.abs(runBox.x - refPt.getX());

                        int overlap = GeoUtil.yOverlap(runBox, stemBox);
                        yGap = Math.abs(Math.min(overlap, 0));
                    } else {
                        // Use stem line to compute both xGap and yGap
                        Line2D line = new Line2D.Double(
                                stemGlyph.getStartPoint(Orientation.VERTICAL),
                                stemGlyph.getStopPoint(Orientation.VERTICAL));
                        double distSq = line.ptSegDistSq(refPt);
                        double xGapSq = line.ptLineDistSq(refPt);
                        xGap = Math.sqrt(xGapSq);
                        yGap = Math.sqrt(Math.abs(distSq - xGapSq));
                    }

                    hRel.setDistances(
                            scale.pixelsToFrac(xGap),
                            scale.pixelsToFrac(yGap));

                    if (hRel.getGrade() >= hRel.getMinGrade()) {
                        // Determine stem portion (with 1/2 head margin)
                        if (dir > 0) {
                            if (stemBox.y >= (headBox.y - (headBox.height / 2))) {
                                hRel.setStemPortion(StemPortion.STEM_TOP);
                            } else {
                                hRel.setStemPortion(StemPortion.STEM_MIDDLE);
                            }
                        } else {
                            if ((stemBox.y + stemBox.height) <= (headBox.y
                                                                 + headBox.height
                                                                 + (headBox.height / 2))) {
                                hRel.setStemPortion(StemPortion.STEM_BOTTOM);
                            } else {
                                hRel.setStemPortion(StemPortion.STEM_MIDDLE);
                            }
                        }

                        sig.addEdge(head, stemInter, hRel);
                        logger.debug("{} {} {}", head, corner, hRel);
                    } else {
                        hRel = null;
                    }
                }

                //
                //                if (hRel != null) {
                //                    head.setShape(Shape.NOTEHEAD_BLACK);
                //                }
                return hRel;
            }

            //-----------------//
            // createStemInter //
            //-----------------//
            /**
             * (Try to) create stem interpretation with proper grade.
             *
             * @param items     the sequence of items (seeds / chunks) that
             *                  compose
             *                  the stem
             * @param extension distance from head to end os stem
             * @return the proper stem interpretation or null if too weak
             */
            private StemInter createStemInter (List<Glyph> items,
                                               double extension)
            {
                final Glyph stem = (items.size() > 1)
                        ? system.buildCompound(items) : items.get(0);

                if (stem.isVip()) {
                    logger.info("VIP createStemInter {}", stem);
                }

                // Stem interpretation (if not yet present)
                StemInter stemInter = (StemInter) sig.getInter(
                        stem,
                        StemInter.class);

                if (stemInter == null) {
                    // Use the amount of white space to evaluate the stem.
                    double largestGap = 0;
                    int totalLength = 0;
                    double totalGap = 0;
                    Integer lastY = null;

                    for (Glyph item : items) {
                        final Rectangle itemBox = item.getBounds();
                        final int itemY = (dir > 0) ? itemBox.y
                                : ((itemBox.y + itemBox.height)
                                   - 1);
                        totalLength += itemBox.height;

                        if (lastY != null) {
                            int gap = Math.abs(itemY - lastY);
                            totalGap += gap;
                            largestGap = Math.max(largestGap, gap);
                        }

                        lastY = itemY + (dir * (itemBox.height - 1));
                    }

                    // Impact of white ratio
                    double whiteImpact = Grades.clamp(
                            1 - (totalGap / totalLength));

                    // Impact of largest gap
                    double gapImpact = Grades.clamp(
                            1 - (largestGap / params.maxYGap));

                    // Impact of straightness: mean distance to straight line
                    double straightImpact = Grades.clamp(
                            1
                            - (stem.getMeanDistance() / params.maxDistanceToLine));

                    // Impact of verticality (wrt global slope)
                    double vertSlope = -skew.getSlope();
                    double stemSlope = stem.getInvertedSlope();
                    double deltaSlope = Math.abs(stemSlope - vertSlope);
                    double slopeImpact = Grades.clamp(
                            1 - (deltaSlope / params.slopeMargin));

                    // Impact of length
                    double lengthImpact = Grades.clamp(
                            (totalLength - params.minStemExtension) / (params.minLongStemLength
                                                                       - params.minStemExtension));

                    // TODO: adjacency??? (belt ratio)
                    // Weighted value
                    double grade = (whiteImpact + gapImpact + straightImpact
                                    + slopeImpact + lengthImpact) / 5;

                    if (grade >= StemInter.getMinGrade()) {
                        stemInter = new StemInter(stem, grade);
                        sig.addVertex(stemInter);
                    } else {
                        logger.debug(
                                "Too weak stem#{} grade: {}",
                                stem.getId(),
                                String.format("%.2f", grade));
                    }
                }

                return stemInter;
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
                final int xSign = (corner.hSide == LEFT) ? (-1) : 1;

                return new Point2D.Double(
                        refPt.getX() - (xSign * params.maxInDx),
                        refPt.getY());
            }

            //----------//
            // getLimit //
            //----------//
            /**
             * Report proper beam limit, according to corner direction
             *
             * @param beam the beam of interest
             * @return the top or bottom beam limit, according to dir
             */
            private Line2D getLimit (BeamInter beam)
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
                final int xSign = (corner.hSide == LEFT) ? (-1) : 1;

                final double slope = skew.getSlope();
                final double dSlope = -xSign * dir * params.slopeMargin;

                final Point2D outPt = getOutPoint();
                final Point2D inPt = getInPoint();

                // Look Up path, start by head horizontal segment
                final Path2D lu = new Path2D.Double();
                lu.moveTo(outPt.getX(), outPt.getY());
                lu.lineTo(inPt.getX(), inPt.getY());

                // Then segment away from head (system limit)
                final Rectangle systemBox = system.getBounds();
                final double yLimit = (dir > 0) ? systemBox.getMaxY()
                        : systemBox.getMinY();
                final double dy = yLimit - outPt.getY();
                lu.lineTo(inPt.getX() + ((slope + dSlope) * dy), yLimit);
                lu.lineTo(outPt.getX() + ((slope - dSlope) * dy), yLimit);
                lu.closePath();

                // Attachment
                StringBuilder sb = new StringBuilder();
                sb.append((corner.vSide == TOP) ? "T" : "B");
                sb.append((corner.hSide == LEFT) ? "L" : "R");

                if (headGlyph != null) {
                    headGlyph.addAttachment(sb.toString(), lu);
                }

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
                final int xSign = (corner.hSide == LEFT) ? (-1) : 1;

                return new Point2D.Double(
                        refPt.getX() + (xSign * params.maxOutDx),
                        refPt.getY());
            }

            //-------------------//
            // getReferencePoint //
            //-------------------//
            /**
             * Compute head reference point for this corner (the point
             * where a stem could be connected).
             * For best precision, we use the dimensions of MusicFont head
             * symbol for proper scale, rather than the glyph underneath.
             *
             * @return the refPt
             */
            private Point2D getReferencePoint ()
            {
                ///Point center = headGlyph.getCentroid();
                Point center = GeoUtil.centerOf(headBox);

                final double dx = (corner.hSide == LEFT)
                        ? (-headSymbolDim.width * 0.5)
                        : (headSymbolDim.width * 0.5);

                //
                //                final double dy = (corner.hSide == LEFT)
                //                        ? ((corner.vSide == TOP) ? 0
                //                        : (headDim.height * 0.3))
                //                        : ((corner.vSide == TOP)
                //                        ? (-headDim.height * 0.3) : 0);
                final double dy = (dir > 0) ? (headSymbolDim.height * 0.45)
                        : (-headSymbolDim.height * 0.45);

                return new Point2D.Double(center.x + dx, center.y + dy);
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
                final Run run = (side == LEFT) ? section.getFirstRun()
                        : section.getLastRun();
                final int pos = (side == LEFT) ? section.getFirstPos()
                        : section.getLastPos();

                return new Rectangle(pos, run.getStart(), 1, run.getLength());
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
                Point2D refPt2 = new Point2D.Double(
                        refPt.getX() - (100 * skew.getSlope()),
                        refPt.getY() + 100);

                return LineUtil.intersection(
                        refPt,
                        refPt2,
                        limit.getP1(),
                        limit.getP2());
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
                        (int) Math.rint((dir > 0) ? refPt.getY() : yLimit),
                        0, // width is irrelevant
                        (int) Math.rint(Math.abs(yLimit - refPt.getY())));
            }

            //--------------//
            // includeItems //
            //--------------//
            /**
             * Include the stem items, one after the other
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
                StemInter stemInter = null;
                List<StemInter> allInters = new ArrayList<StemInter>();

                for (int i = 0; i < items.size(); i++) {
                    Glyph item = items.get(i);
                    Rectangle itemBox = item.getBounds();

                    // Are we past the beam limit (if any)?
                    if (getContrib(itemBox) == 0) {
                        break;
                    }

                    // Is gap with previous item acceptable?
                    final int itemY = (dir > 0) ? itemBox.y
                            : ((itemBox.y + itemBox.height) - 1);
                    final double itemStart = (dir > 0) ? Math.max(itemY, refY)
                            : Math.min(itemY, refY);
                    final double yGap = Math.abs(itemStart - lastY);

                    if (yGap > params.maxYGap) {
                        break; // Too large gap
                    }

                    if ((i == 0) && (yGap > params.maxStemHeadGapY)) {
                        break; // Initial item too far from head
                    }

                    // Check minimum stem extension from head
                    lastY = itemY + (dir * (itemBox.height - 1));

                    final double extension = Math.abs(lastY - refY);

                    if (extension < params.minStemExtension) {
                        continue;
                    }

                    // OK, build a stem interpretation with all items so far
                    // TODO: some items might be too distant from line and should be skipped!
                    List<Glyph> stemItems = items.subList(0, i + 1);
                    stemInter = createStemInter(stemItems, extension);

                    if (stemInter != null) {
                        allInters.add(stemInter);
                        connectHeadStem(fatHeadSection, stemInter);
                    }
                }

                return allInters;
            }

            //-------------//
            // lookupBeams //
            //-------------//
            /**
             * Look for beam interpretations in the lookup area.
             * We stop at (group of) first good beam interpretation, if any.
             *
             * @param beams (output) list to be populated, ordered by distance
             *              from head
             * @return index of first good beam in the beams list
             */
            private int lookupBeams (List<BeamInter> beams)
            {
                // Look for beams in the corner
                List<Inter> allbeams = sig.intersectedInters(
                        neighborBeams,
                        GeoOrder.BY_ABSCISSA,
                        area);

                // Sort by distance from head
                Collections.sort(
                        allbeams,
                        new Comparator<Inter>()
                        {
                            @Override
                            public int compare (Inter b1,
                                                Inter b2)
                            {
                                double d1 = Math.abs(
                                        refPt.getY()
                                        - getTargetPt(getLimit((BeamInter) b1)).getY());
                                double d2 = Math.abs(
                                        refPt.getY()
                                        - getTargetPt(getLimit((BeamInter) b2)).getY());

                                return Double.compare(d1, d2);
                            }
                        });

                // Build the list of beams
                BeamInter goodBeam = null;

                BeamLoop:
                for (Inter inter : allbeams) {
                    BeamInter beam = (BeamInter) inter;

                    if (goodBeam == null) {
                        // Check if beam is far enough from head
                        final Point2D pt = getTargetPt(getLimit(beam));
                        final double distToBeam = dir * (pt.getY()
                                                         - refPt.getY());

                        if (distToBeam < params.minHeadBeamDistance) {
                            if (beam.isVip() || logger.isDebugEnabled()) {
                                logger.info(
                                        "Beam {} too close to {}",
                                        beam,
                                        head);
                            }

                            sig.insertExclusion(beam, head, Cause.TOO_CLOSE);

                            continue BeamLoop;
                        }

                        beams.add(beam);

                        // Truncate at first good encountered beam, if any, 
                        // taken with its group.
                        // Nota: We could shrink the lu area accordingly, however we
                        // impose area containment for stem sections, so let's
                        // stay with the system limit for area definition.              
                        if (beam.isGood()) {
                            goodBeam = beam;
                        }
                    } else {
                        // We are within good beam group, check end of it
                        BeamInter lastBeam = beams.get(beams.size() - 1);

                        if (areCompatible(lastBeam, beam)) {
                            beams.add(beam);
                        } else {
                            break BeamLoop;
                        }
                    }
                }

                return beams.indexOf(goodBeam);
            }

            //--------------//
            // lookupChunks //
            //--------------//
            /**
             * Build chunks of stems out of additional compatible
             * sections (not part of stem seeds) found in the corner.
             * TODO: Add adjacency test for chunks?
             * TODO: Add distance test WRT theoretical line?
             *
             * @param fatHeadSection (output) a very specific section which is
             *                       part
             *                       of the head.
             * @return the collection of chunks found
             */
            private List<Glyph> lookupChunks (Wrapper<Section> fatHeadSection)
            {
                // Look up suitable sections
                List<Section> sections = lookupSections(fatHeadSection);

                // Aggregate these sections into glyphs & check them
                List<Glyph> chunks = GlyphsBuilder.retrieveGlyphs(
                        sections,
                        system.getSheet().getNest(),
                        GlyphLayer.DEFAULT,
                        scale);

                for (Iterator<Glyph> it = chunks.iterator(); it.hasNext();) {
                    Glyph chunk = it.next();
                    Rectangle chunkBox = chunk.getBounds();

                    if ((chunkBox.width > params.maxStemThickness)
                        || (getContrib(chunkBox) == 0)) {
                        it.remove();
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
             * Perhaps extrapolate a line based on existing seeds and check
             * distance of sections to this line.
             *
             * Also impose a width slightly smaller than full stem max width
             * (2/3?)
             *
             * Can these section(s) really be part of a stem? (check global
             * thickness and whiteness around section(s))
             *
             * Use average stem length or histogram to exclude outliers?
             *
             * Build glyphs out of embraced connected sections and check them
             * (aspect, width, straightness, vertical, distance from line)
             *
             * @param fatHeadSection (potential output) a thick section, part of
             *                       head, that accounts for stem range
             * @return the collection of additional sections found
             */
            private List<Section> lookupSections (
                    Wrapper<Section> fatHeadSection)
            {
                // Horizontal line around refPt
                final Point2D outPt = getOutPoint();
                final Point2D inPt = getInPoint();
                final Line2D hLine = (corner.hSide == LEFT)
                        ? new Line2D.Double(outPt, inPt)
                        : new Line2D.Double(inPt, outPt);
                final int refY = (int) Math.rint(refPt.getY());
                final List<Section> sections = new ArrayList<Section>();
                final List<Section> headSections = new ArrayList<Section>();

                // Browse both vertical and horizontal sections in the system
                for (Collection<Section> collection : Arrays.asList(
                        system.getVerticalSections(),
                        system.getHorizontalSections())) {
                    SectionLoop:
                    for (Section section : collection) {
                        Rectangle sectBox = section.getBounds();

                        // Check intersection at least
                        if (!area.intersects(sectBox)) {
                            continue SectionLoop;
                        }

                        // Containment is mandatory except for a head section
                        // (a section that intersects head glyph)
                        if (!area.contains(sectBox)) {
                            if (!sectBox.intersects(headBox)
                                || !GeoUtil.yEmbraces(sectBox, refY)) {
                                continue SectionLoop;
                            }

                            // Here we have a section likely to be part of head itself
                            // Even if too thick, use part of its length as stem portion
                            // (if it does not overlap stem seeds)
                            if (section.isVertical()
                                && (sectBox.width > params.maxStemThickness)) {
                                // Consider the touching run
                                Rectangle runBox = getRunBox(
                                        section,
                                        corner.hSide);

                                for (Glyph seed : items) {
                                    if (GeoUtil.yOverlap(
                                            runBox,
                                            seed.getBounds()) > 0) {
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

                                        if (getContrib(runBox) > getContrib(
                                                otherBox)) {
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
                                logger.debug(
                                        "Discarding tiny headSection {}",
                                        section);

                                continue SectionLoop;
                            }

                            headSections.add(section);
                        }

                        // Contraint section width <= stem width
                        if (sectBox.width > params.maxStemThickness) {
                            continue SectionLoop;
                        }

                        // A section which overlaps an existing seed is useless
                        for (Glyph seed : items) {
                            if (GeoUtil.yOverlap(sectBox, seed.getBounds()) > 0) {
                                continue SectionLoop;
                            }
                        }

                        sections.add(section);
                    }
                }

                // Handle overlap between standard sections and fatHeadSection
                // if any, by keeping the most contributive one
                if (fatHeadSection.value != null) {
                    final Rectangle runBox = getRunBox(
                            fatHeadSection.value,
                            corner.hSide);
                    final int runContrib = getContrib(runBox);

                    for (Iterator<Section> it = sections.iterator();
                            it.hasNext();) {
                        final Section section = it.next();
                        final Rectangle sctBox = section.getBounds();

                        if (GeoUtil.yOverlap(runBox, sctBox) > 0) {
                            if (getContrib(sctBox) <= runContrib) {
                                it.remove();
                            } else {
                                logger.debug(
                                        "Cancelling fatHeadSection {}",
                                        fatHeadSection);
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

            //-------------//
            // lookupSeeds //
            //-------------//
            /**
             * Retrieve proper collection of stem seeds in the corner.
             * In case of overlapping seeds, we keep the most contributive.
             * Perhaps we could also check distance to the theoretical vertical
             * line.
             *
             * @return the proper stem seeds, sorted by increasing vertical
             *         distance from the head.
             */
            private List<Glyph> lookupSeeds ()
            {
                // Look for stems seeds
                List<Glyph> stems = sig.intersectedGlyphs(
                        neighborSeeds,
                        true,
                        area);

                // In case of overlap, simply keep the most contributive
                Collections.sort(
                        stems,
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

                List<Glyph> kept = new ArrayList<Glyph>();

                StemLoop:
                for (Glyph stem : stems) {
                    Rectangle stemBox = stem.getBounds();

                    for (Glyph k : kept) {
                        if (GeoUtil.yOverlap(stemBox, k.getBounds()) > 0) {
                            continue StemLoop;
                        }
                    }

                    // No overlap
                    kept.add(stem);
                }

                stems.retainAll(kept);

                // Sort seeds by increasing distance from head
                sortByDistance(stems);

                return stems;
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
                Collections.sort(
                        glyphs,
                        (dir > 0) ? Glyph.byOrdinate : Glyph.byReverseOrdinate);
            }
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
        //~ Instance fields ----------------------------------------------------

        final int maxOutDx;

        final int maxInDx;

        final int xMargin;

        final double slopeMargin;

        final int maxStemHeadGapX;

        final int maxStemHeadGapY;

        final int maxStemBeamGapX;

        final int maxStemBeamGapY;

        final int maxYGap;

        final int maxStemThickness;

        final int minHeadSectionContribution;

        final int minStemExtension;

        final int minHeadBeamDistance;

        final int minLongStemLength;

        final double maxDistanceToLine;

        final int maxInterBeamGap;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (SystemInfo system,
                           Scale scale)
        {
            maxOutDx = scale.toPixels(constants.maxOutDx);
            maxInDx = scale.toPixels(constants.maxInDx);
            xMargin = scale.toPixels(constants.xMargin);
            slopeMargin = constants.slopeMargin.getValue();
            maxStemHeadGapX = scale.toPixels(constants.maxStemHeadGapX);
            maxStemHeadGapY = scale.toPixels(constants.maxStemHeadGapY);
            maxStemBeamGapX = system.beamsBuilder.maxStemBeamGapX();
            maxStemBeamGapY = system.beamsBuilder.maxStemBeamGapY();
            maxYGap = scale.toPixels(constants.maxYGap);
            maxStemThickness = scale.toPixels(
                    VerticalsBuilder.getMaxStemThickness());
            minHeadSectionContribution = scale.toPixels(
                    constants.minHeadSectionContribution);
            minStemExtension = scale.toPixels(constants.minStemExtension);
            minHeadBeamDistance = scale.toPixels(constants.minHeadBeamDistance);
            minLongStemLength = scale.toPixels(constants.minLongStemLength);
            maxDistanceToLine = scale.toPixelsDouble(
                    constants.maxDistanceToLine);
            maxInterBeamGap = scale.toPixels(constants.maxInterBeamGap);

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }
}
