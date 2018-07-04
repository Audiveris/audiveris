//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B e a m s B u i l d e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.beam;

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.BasicGlyph;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.image.AreaMask;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.Population;
import org.audiveris.omr.run.Orientation;

import static org.audiveris.omr.run.Orientation.VERTICAL;

import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.stem.StemsBuilder;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractBeamInter.Impacts;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.SmallBeamInter;
import org.audiveris.omr.sig.relation.Exclusion;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.util.ByteUtil;
import org.audiveris.omr.util.Corner;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.HorizontalSide;

import static org.audiveris.omr.util.HorizontalSide.*;

import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Predicate;
import org.audiveris.omr.util.VerticalSide;

import static org.audiveris.omr.util.VerticalSide.*;

import org.audiveris.omr.util.WrappedInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class {@code BeamsBuilder} is in charge, at system level, of retrieving the possible
 * beam and beam hook interpretations.
 * <p>
 * The retrieval is performed on the collection of spots produced by closing the blurred initial
 * image with a disk-shaped structure element whose diameter is just slightly smaller than the
 * typical beam height.
 * <ol>
 * <li>{@link #buildBeams()} retrieves standard beams.</li>
 * <li>{@link #buildCueBeams(java.util.List)} retrieves cue beams.</li>
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class BeamsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BeamsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** Scale-dependent global constants. */
    private final Parameters params;

    /** Parameters to build items (cue or standard). */
    private ItemParameters itemParams;

    /**
     * Beams (and hooks) for this system.
     * Collection is called raw to remind us it is NOT sorted
     */
    private List<Inter> rawSystemBeams;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** The related SIG. */
    private final SIGraph sig;

    /** Spots already recognized as beams. */
    private final List<Glyph> assignedSpots = new ArrayList<Glyph>();

    /** Remaining beam spots candidates, sorted by abscissa. */
    private List<Glyph> sortedBeamSpots;

    /** Vertical stem seeds, sorted by abscissa. */
    private List<Glyph> sortedSystemSeeds;

    /** Input image. */
    private ByteProcessor pixelFilter;

    /** Population of observed vertical distance between grouped beams. */
    private final Population distances;

    /** Lag of glyph sections. */
    private final Lag spotLag;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BeamsBuilder object.
     *
     * @param system    the dedicated system
     * @param distances (output) population of vertical beam distances within groups
     * @param spotLag   (output) lag, if any, to be populated with glyph sections
     */
    public BeamsBuilder (SystemInfo system,
                         Population distances,
                         Lag spotLag)
    {
        this.system = system;
        this.distances = distances;
        this.spotLag = spotLag;

        sig = system.getSig();
        sheet = system.getSheet();
        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // buildBeams //
    //------------//
    /**
     * Find possible interpretations of beams among system spots.
     *
     */
    public void buildBeams ()
    {
        // Select parameters for standard items
        itemParams = new ItemParameters(sheet.getScale(), 1.0);

        // Cache input image
        pixelFilter = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);

        // First, retrieve beam candidates from spots
        sortedBeamSpots = system.getGroupedGlyphs(GlyphGroup.BEAM_SPOT);

        // Create initial beams by checking spots individually
        createBeams();

        // Then, extend beams as much as possible
        sortedBeamSpots.removeAll(assignedSpots);
        extendBeams();

        // Finally, retrieve beam hooks
        sortedBeamSpots.removeAll(assignedSpots);
        buildHooks();

        // Measure vertical gap
        if (distances.getCardinality() > 0) {
            logger.debug("S#{} beam gaps {}", system.getId(), distances);
        }
    }

    //---------------//
    // buildCueBeams //
    //---------------//
    /**
     * Find possible cue beams interpretations around identified cue notes and stems.
     *
     * @param spots candidate spots for cue beams
     */
    public void buildCueBeams (List<Glyph> spots)
    {
        // Select parameters for cue items
        itemParams = new ItemParameters(sheet.getScale(), constants.cueBeamRatio.getValue());

        // Cache input image
        pixelFilter = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);

        List<CueAggregate> aggregates = getCueAggregates();

        for (CueAggregate aggregate : aggregates) {
            aggregate.process(spots);
        }
    }

    //----------------//
    // isSideInSystem //
    //----------------//
    /**
     * Check whether the (beam) side designated by provided point and height values
     * lies fully in the current system
     *
     * @param pt     side point on median
     * @param height beam height
     * @return true if side is fully within current system, false otherwise
     */
    boolean isSideInSystem (Point2D pt,
                            double height)
    {
        Area area = system.getArea();

        // Check top and bottom points of the beam side
        for (int dir : new int[]{-1, +1}) {
            if (!area.contains(pt.getX(), pt.getY() + (dir * (height / 2)))) {
                return false;
            }
        }

        return true;
    }

    //-------------//
    // browseHooks //
    //-------------//
    /**
     * Look for hooks on a vertical side of a beam.
     *
     * @param beam the base beam
     * @param side which vertical side to browse
     */
    private void browseHooks (BeamInter beam,
                              VerticalSide side)
    {
        if (beam.isVip()) {
            logger.info("VIP browseHooks on {} of {}", side, beam);
        }

        // Look for a parallel beam just above or below
        final Line2D median = beam.getMedian();
        final double height = beam.getHeight();
        final double dy = 1.5 * ((side == TOP) ? (-height) : height);

        Area luArea = AreaUtil.horizontalParallelogram(
                new Point2D.Double(median.getX1(), median.getY1() + dy),
                new Point2D.Double(median.getX2(), median.getY2() + dy),
                height);
        Set<Glyph> glyphs = Glyphs.intersectedGlyphs(sortedBeamSpots, luArea);

        for (Glyph glyph : glyphs) {
            String failure = checkHookGlyph(beam, side, glyph);

            if ((failure != null) && glyph.isVip()) {
                logger.info("VIP hook#{} {}", glyph.getId(), failure);
            }
        }
    }

    //------------//
    // buildHooks //
    //------------//
    /**
     * Retrieve all possible beam hooks interpretations in the system.
     * <p>
     * Since beam hooks can be found only near standard beams, we check each
     * beam and look just above and below for a hook.
     */
    private void buildHooks ()
    {
        for (Inter inter : sig.inters(BeamInter.class)) {
            for (VerticalSide side : VerticalSide.values()) {
                browseHooks((BeamInter) inter, side);
            }
        }
    }

    //----------------//
    // checkBeamGlyph //
    //----------------//
    /**
     * Check the provided glyph as a beam candidate.
     *
     * @param glyph the glyph to check
     * @param isCue true for a small beam candidate
     * @param beams (output) to be appended by created beam inters
     * @return the failure description if not successful, null otherwise
     */
    private String checkBeamGlyph (Glyph glyph,
                                   boolean isCue,
                                   List<Inter> beams)
    {
        final Rectangle box = glyph.getBounds();
        final Line2D glyphLine = glyph.getLine();

        if (glyph.isVip()) {
            logger.info("VIP checkBeamGlyph {} cue:{}", glyph, isCue);
        }

        // Minimum width
        if (box.width < itemParams.minBeamWidthLow) {
            return "too narrow";
        }

        // Minimum mean height
        final double meanHeight = glyph.getMeanThickness(Orientation.HORIZONTAL);

        if (meanHeight < itemParams.minHeightLow) {
            return "too slim";
        }

        if (!isCue) {
            // Maximum slope
            try {
                double absSlope = Math.abs(LineUtil.getSlope(glyphLine));

                if (absSlope > params.maxBeamSlope) {
                    return "too steep";
                }
            } catch (Exception ignored) {
                return "vertical";
            }
        }

        // Check straight lines of all north and south borders
        final BeamStructure structure = new BeamStructure(glyph, spotLag, itemParams);
        final Double meanDist = structure.computeLines();

        if ((meanDist == null) || (meanDist > params.maxDistanceToBorder)) {
            return "wavy or inconsistent borders";
        }

        // Check structure width
        final double structWidth = structure.getWidth();

        if (structWidth < itemParams.minBeamWidthLow) {
            return "too narrow borders";
        }

        // Check that all lines of the glyph are rather parallel
        double lineSlopeGap = structure.compareSlopes();

        if (lineSlopeGap > params.maxBeamSlopeGap) {
            return "diverging beams";
        }

        // Adjust horizontal sides
        structure.adjustSides();

        // Adjust middle lines if necessary
        structure.extendMiddleLines();

        // Check stuck beams and split them if necessary
        structure.splitLines();

        // Compute items grade and create Inter instances when acceptable
        if (isCue) {
            final double distImpact = 1 - (meanDist / params.maxDistanceToBorder);
            List<Inter> cues = createSmallBeamInters(structure, distImpact);

            if (!cues.isEmpty()) {
                beams.addAll(cues);

                return null;
            } else {
                return "no good item";
            }
        } else if (createBeamInters(structure)) {
            assignedSpots.add(glyph);

            return null; // This means no failure
        } else {
            return "no good item";
        }
    }

    //----------------//
    // checkHookGlyph //
    //----------------//
    /**
     * Check the provided glyph as a hook near base beam.
     * We first check that the hook candidate does not overlap a beam.
     * We then use the usual core & belt mask test for the hook candidate, using slope and height
     * values from base beam, and adjusted abscissa limits.
     *
     * @param beam  the base beam
     * @param side  which vertical side of beam
     * @param glyph the candidate hook
     * @return the failure message if any, null otherwise
     */
    private String checkHookGlyph (BeamInter beam,
                                   VerticalSide side,
                                   Glyph glyph)
    {
        if (glyph.isVip()) {
            logger.info("VIP checkHookGlyph {} on {} of {}", glyph, side, beam);
        }

        final Rectangle box = glyph.getBounds();
        final double distImpact = ((Impacts) beam.getImpacts()).getDistImpact();

        // Minimum width
        if (box.width < itemParams.minHookWidthLow) {
            return "too narrow";
        }

        // Minimum & maximum mean hook height
        // TODO: for hooks, since spots are rounded rectangles, the mean thickness is underestimated
        final double meanHeight = glyph.getMeanThickness(Orientation.HORIZONTAL);

        if (meanHeight < itemParams.minHeightLow) {
            return "too slim";
        } else if (meanHeight > itemParams.maxHeightHigh) {
            return "too thick";
        }

        // Define hook item
        Point centroid = glyph.getCentroid();
        double slope = LineUtil.getSlope(beam.getMedian());
        Point2D p1 = LineUtil.intersectionAtX(centroid, slope, box.x);
        Point2D p2 = LineUtil.intersectionAtX(centroid, slope, (box.x + box.width) - 1);
        Line2D median = new Line2D.Double(p1, p2);
        double height = beam.getHeight();
        BeamItem item = new BeamItem(median, height);

        // Check this hook item does not conflict with any existing beam
        if (overlap(item)) {
            return "overlap";
        }

        // Compute core & belt impacts
        Impacts impacts = computeHookImpacts(item, distImpact);

        if ((impacts != null) && (impacts.getGrade() >= BeamHookInter.getMinGrade())) {
            BeamHookInter hook = new BeamHookInter(impacts, median, height);

            if (glyph.isVip()) {
                hook.setVip(true);
            }

            registerBeam(hook);
            rawSystemBeams.add(hook);
            assignedSpots.add(glyph);

            // Measure vertical distance between hook and base beam
            measureVerticalDistance(beam, hook);

            return null; // Meaning: no failure
        } else {
            return "no good item";
        }
    }

    //--------------------//
    // computeBeamImpacts //
    //--------------------//
    /**
     * Compute the grade details for the provided BeamItem, targeting
     * a BeamInter.
     *
     * @param item       the isolated beam item
     * @param above      true to check above beam item
     * @param below      true to check below beam item
     * @param distImpact IMPACT of jitter to border
     * @return the impacts if successful, null otherwise
     */
    private Impacts computeBeamImpacts (BeamItem item,
                                        boolean above,
                                        boolean below,
                                        double distImpact)
    {
        return computeImpacts(
                item,
                above,
                below,
                itemParams.minBeamWidthLow,
                itemParams.minBeamWidthHigh,
                distImpact);
    }

    //--------------------//
    // computeHookImpacts //
    //--------------------//
    /**
     * Compute the grade details for the provided BeamItem, targeting a BeamHookInter.
     *
     * @param item       the isolated beam item
     * @param distImpact IMPACT of jitter to border
     * @return the impacts if successful, null otherwise
     */
    private Impacts computeHookImpacts (BeamItem item,
                                        double distImpact)
    {
        return computeImpacts(
                item,
                true,
                true,
                itemParams.minHookWidthLow,
                itemParams.minHookWidthHigh,
                distImpact);
    }

    //----------------//
    // computeImpacts //
    //----------------//
    /**
     * Compute the grade details for the provided BeamItem.
     *
     * @param item         the isolated beam item
     * @param above        true to check above beam item
     * @param below        true to check below beam item
     * @param minWidthLow  low minimum width
     * @param minWidthHigh high minimum width
     * @param distImpact   IMPACT of jitter to border
     * @return the impacts if successful, null otherwise
     */
    private Impacts computeImpacts (BeamItem item,
                                    boolean above,
                                    boolean below,
                                    double minWidthLow,
                                    double minWidthHigh,
                                    double distImpact)
    {
        Area coreArea = item.getCoreArea();
        AreaMask coreMask = new AreaMask(coreArea);
        WrappedInteger core = new WrappedInteger(0);
        int coreCount = coreMask.fore(core, pixelFilter);
        double coreRatio = (double) core.value / coreCount;

        int dx = params.beltMarginDx;
        int topDy = above ? params.beltMarginDy : 0;
        int botDy = below ? params.beltMarginDy : 0;

        Area beltArea = item.getBeltArea(coreArea, dx, topDy, botDy);
        AreaMask beltMask = new AreaMask(beltArea);
        WrappedInteger belt = new WrappedInteger(0);
        int beltCount = beltMask.fore(belt, pixelFilter);
        double beltRatio = (double) belt.value / beltCount;
        int width = (int) Math.rint(item.median.getX2() - item.median.getX1() + 1);

        if ((width < minWidthLow)
            || (item.height < itemParams.minHeightLow)
            || (item.height > itemParams.maxHeightHigh)
            || (coreRatio < params.minCoreBlackRatio)
            || (beltRatio > params.maxBeltBlackRatio)) {
            if (item.isVip() || logger.isDebugEnabled()) {
                logger.info(
                        "Rejected {} width:{} height:{} %core:{} %belt:{}",
                        item,
                        width,
                        item.height,
                        String.format("%.2f", coreRatio),
                        String.format("%.2f", beltRatio));
            }

            return null;
        }

        double widthImpact = (width - minWidthLow) / (minWidthHigh - minWidthLow);
        double minHeightImpact = (item.height - itemParams.minHeightLow) / (itemParams.typicalHeight
                                                                            - itemParams.minHeightLow);
        double maxHeightImpact = (itemParams.maxHeightHigh - item.height) / (itemParams.maxHeightHigh
                                                                             - itemParams.typicalHeight);
        double coreImpact = (coreRatio - params.minCoreBlackRatio) / (1 - params.minCoreBlackRatio);
        double beltImpact = 1 - (beltRatio / params.maxBeltBlackRatio);

        return new Impacts(
                widthImpact,
                minHeightImpact,
                maxHeightImpact,
                coreImpact,
                beltImpact,
                distImpact);
    }

    //------------------//
    // createBeamInters //
    //------------------//
    /**
     * Create the resulting Inter instances (full beam or beam hook), for each good item.
     * <p>
     * For beams with width between minBeamWidthLow and maxHookWidth, we create both
     * interpretations.
     *
     * @param structure the beam structure retrieved (from a glyph)
     * @return true if at least one good item was found
     */
    private boolean createBeamInters (BeamStructure structure)
    {
        boolean success = false;
        final List<BeamLine> lines = structure.getLines();

        // First, retrieve top and bottom jitter
        final double topJitter = structure.computeJitter(lines.get(0), TOP);
        final double botJitter = structure.computeJitter(lines.get(lines.size() - 1), BOTTOM);
        final double meanJitter = 0.5 * (topJitter + botJitter);
        final double distImpact = 1 - (meanJitter / params.maxJitterRatio);

        ///logger.info("meanJitter:{} for {}", String.format("%.4f", meanJitter), structure);
        //
        for (int idx = 0; idx < lines.size(); idx++) {
            final BeamLine line = lines.get(idx);

            for (BeamItem item : line.getItems()) {
                double itemWidth = item.median.getX2() - item.median.getX1();
                BeamHookInter hook = null;

                if (itemWidth <= itemParams.maxHookWidth) {
                    logger.debug("Create hook with {}", line);

                    Impacts impacts = computeHookImpacts(item, distImpact);

                    if ((impacts != null) && (impacts.getGrade() >= BeamHookInter.getMinGrade())) {
                        success = true;
                        hook = new BeamHookInter(impacts, item.median, item.height);

                        if (line.isVip()) {
                            hook.setVip(true);
                        }

                        registerBeam(hook);
                    }
                }

                try {
                    //TODO: test for detecting top/bottom items is not correct
                    Impacts impacts = computeBeamImpacts(
                            item,
                            idx == 0, // Check above only for first item
                            idx == (lines.size() - 1), // Check below only for last item
                            distImpact);

                    if ((impacts != null) && (impacts.getGrade() >= BeamInter.getMinGrade())) {
                        success = true;

                        BeamInter beam = new BeamInter(impacts, item.median, item.height);

                        if (line.isVip()) {
                            beam.setVip(true);
                        }

                        registerBeam(beam);

                        // Exclusion between beam and hook, if any
                        if (hook != null) {
                            sig.insertExclusion(hook, beam, Exclusion.Cause.OVERLAP);
                        }
                    }
                } catch (Exception ex) {
                    // This is often due to pseudo beam candidate stuck on image border
                    // resulting in ArrayIndexOutOfBoundsException when checking beam white belt
                    // We can skip such candidates!
                    logger.warn("Could not compute impacts for beam " + item + " ex: " + ex, ex);
                }
            }
        }

        return success;
    }

    //-------------//
    // createBeams //
    //-------------//
    /**
     * Create initial beams, by checking each spot glyph individually.
     */
    private void createBeams ()
    {
        for (Glyph glyph : sortedBeamSpots) {
            final String failure = checkBeamGlyph(glyph, false, null);

            if ((failure != null) && glyph.isVip()) {
                logger.info("VIP beam#{} {}", glyph.getId(), failure);
            }
        }
    }

    //-----------------------//
    // createSmallBeamInters //
    //-----------------------//
    /**
     * Create the resulting SmallBeamInter instances, one for each good item.
     * TODO: Should we handle small beam hooks as well?
     *
     * @param structure  the items retrieved (from a glyph)
     * @param distImpact impact of border jitter
     * @return the list of inter instances created
     */
    private List<Inter> createSmallBeamInters (BeamStructure structure,
                                               double distImpact)
    {
        final List<Inter> beams = new ArrayList<Inter>();
        final List<BeamLine> lines = structure.getLines();

        for (BeamLine line : lines) {
            final int idx = lines.indexOf(line);

            for (BeamItem item : line.getItems()) {
                Impacts impacts = computeBeamImpacts(
                        item,
                        idx == 0, // Check above only for top items
                        idx == (lines.size() - 1), // Check below only for bottom items
                        distImpact);

                if ((impacts != null) && (impacts.getGrade() >= SmallBeamInter.getMinGrade())) {
                    SmallBeamInter beam = new SmallBeamInter(impacts, item.median, item.height);

                    if (item.isVip()) {
                        beam.setVip(true);
                    }

                    registerBeam(beam);
                    beams.add(beam);
                }
            }
        }

        return beams;
    }

    //-------------//
    // extendBeams //
    //-------------//
    /**
     * Now that individual beams candidates have been extracted, try to improve beam
     * geometry (merge, extension).
     * Try to extend each beam to either another beam (merge) or a stem seed (extension) or in
     * parallel with a sibling beam (extension) or to another spot (extension).
     */
    private void extendBeams ()
    {
        // All stem seeds for this system, sorted by abscissa
        sortedSystemSeeds = system.getGroupedGlyphs(GlyphGroup.VERTICAL_SEED);

        // The beam & hook inters for this system, NOT sorted by abscissa.
        // We may add to this list, but not remove elements (they are simply logically 'deleted').
        // Later, buildHooks() will add hooks to this list.
        rawSystemBeams = sig.inters(AbstractBeamInter.class);

        // Extend each orphan beam as much as possible
        for (Inter inter : new ArrayList<Inter>(rawSystemBeams)) {
            if (inter.isRemoved()) {
                continue;
            }

            final AbstractBeamInter beam = (AbstractBeamInter) inter;

            if (beam.isVip()) {
                logger.info("VIP extendBeams for {}", beam);
            }

            for (HorizontalSide side : HorizontalSide.values()) {
                // If successful, this appends a new beam instance to the list.
                // The new beam will later be tested for further extension.

                // Is there a compatible beam near by?
                AbstractBeamInter sideBeam = getSideBeam(beam, side, null);
                Integer maxDx = null;

                if (sideBeam != null) {
                    // Try to merge the 2 beams (avoid double testing, so check on one side only)
                    if ((side == LEFT) && extendToBeam(beam, side, sideBeam)) {
                        break;
                    }

                    Line2D median = beam.getMedian();
                    Line2D sideMedian = sideBeam.getMedian();
                    double dx = (side == LEFT) ? (median.getX1() - sideMedian.getX2())
                            : (sideMedian.getX1() - median.getX2());
                    maxDx = (int) Math.rint(Math.max(0, dx - params.beamsXMargin));
                }

                // Try the other extension modes (limited by side beam if any)
                if (extendToStem(beam, side, maxDx) || extendToSpot(beam, side, maxDx)) {
                    break;
                }

                // Try parallel extension (only when there is no side beam)
                if ((sideBeam == null) && extendInParallel(beam, side)) {
                    break;
                }
            }
        }
    }

    //------------------//
    // extendInParallel //
    //------------------//
    /**
     * Try to extend the provided beam in parallel with a another beam (in the same
     * group of beams).
     *
     * @param beam the beam to extend
     * @param side the horizontal side
     * @return true if extension was done, false otherwise
     */
    private boolean extendInParallel (AbstractBeamInter beam,
                                      HorizontalSide side)
    {
        final boolean logging = beam.isVip() || logger.isDebugEnabled();

        // Look for a parallel beam just above or below
        final Line2D median = beam.getMedian();
        final double height = beam.getHeight();
        final double slope = LineUtil.getSlope(median);

        Area luArea = AreaUtil.horizontalParallelogram(
                median.getP1(),
                median.getP2(),
                3 * height);
        beam.addAttachment("=", luArea);

        List<Inter> others = Inters.intersectedInters(rawSystemBeams, GeoOrder.NONE, luArea);

        others.remove(beam); // Safer

        if (!others.isEmpty()) {
            // Use a closer look
            final Point2D endPt = (side == LEFT) ? median.getP1() : median.getP2();

            for (Inter ib : others) {
                AbstractBeamInter other = (AbstractBeamInter) ib;

                if (logging) {
                    logger.info("VIP {} found parallel {}", beam, other);
                }

                // Check concrete intersection (using areas rather than rectangles)
                if (!AreaUtil.intersection(other.getArea(), luArea)) {
                    if (logging) {
                        logger.info("VIP too distant beams {} and {}", beam, other);
                    }

                    continue;
                }

                // Check they are really parallel?
                final Line2D otherMedian = other.getMedian();
                final double otherSlope = LineUtil.getSlope(otherMedian);

                if (Math.abs(otherSlope - slope) > params.maxBeamSlopeGap) {
                    if (logging) {
                        logger.info("VIP {} not parallel with {}", beam, other);
                    }

                    continue;
                }

                // Side-effect: measure the actual vertical gap between such parallel beams
                // (avoiding a given pair to be counted 4 times...)
                if ((side == LEFT) && (beam.getId() < other.getId())) {
                    measureVerticalDistance(beam, other);
                }

                // Check the other beam can really extend the current beam
                final Point2D otherEndPt = (side == LEFT) ? otherMedian.getP1() : otherMedian.getP2();
                double extDx = (side == LEFT) ? (endPt.getX() - otherEndPt.getX())
                        : (otherEndPt.getX() - endPt.getX());

                if (extDx < (2 * params.maxStemBeamGapX)) {
                    if (logging) {
                        logger.info("VIP {} no increment with {}", beam, other);
                    }

                    continue;
                }

                // Make sure the end side is fully in the same system as the current one
                Point2D extPt = LineUtil.intersectionAtX(median, otherEndPt.getX());

                if (!isSideInSystem(extPt, height)) {
                    continue;
                }

                // Make sure this does not include another beam
                AbstractBeamInter includedBeam = getSideBeam(beam, side, extDx);

                if (includedBeam != null) {
                    continue;
                }

                return extendToPoint(beam, side, extPt);
            }
        }

        return false;
    }

    //--------------//
    // extendToBeam //
    //--------------//
    /**
     * Try to extend the provided beam on the desired side to another beam within reach.
     * The other beam must be compatible in terms of gap (abscissa and ordinate) and beam slope.
     * <p>
     * If such a compatible beam is found, but the middle area between them is not correctly filled,
     * we must remember this information to avoid any other extension attempt on this side.
     *
     * @param beam  the beam to extend
     * @param side  the horizontal side
     * @param other the side beam found
     * @return true if extension was done, false otherwise
     */
    private boolean extendToBeam (AbstractBeamInter beam,
                                  HorizontalSide side,
                                  AbstractBeamInter other)
    {
        // Check black ratio in the middle area (if its width is significant)
        final Line2D beamMedian = beam.getMedian();
        final Line2D otherMedian = other.getMedian();

        double gap = (beamMedian.getX1() < otherMedian.getX1())
                ? (otherMedian.getX1() - beamMedian.getX2())
                : (beamMedian.getX1() - otherMedian.getX2());

        if (gap >= params.minBeamsGapX) {
            Area middleArea = middleArea(beam, other);
            AreaMask coreMask = new AreaMask(middleArea);
            WrappedInteger core = new WrappedInteger(0);
            int coreCount = coreMask.fore(core, pixelFilter);
            double coreRatio = (double) core.value / coreCount;

            if (coreRatio < params.minExtBlackRatio) {
                return false;
            }
        }

        BeamInter newBeam = mergeOf(beam, other);

        if (newBeam == null) {
            return false;
        }

        registerBeam(newBeam);
        rawSystemBeams.add(newBeam);

        if (beam.isVip() || other.isVip()) {
            newBeam.setVip(true);
        }

        beam.remove();
        other.remove();

        if (newBeam.isVip() || logger.isDebugEnabled()) {
            logger.info("VIP Merged {} & {} into {}", beam, other, newBeam);
        }

        return true;
    }

    //---------------//
    // extendToPoint //
    //---------------//
    /**
     * Try to extend the beam on provided side until the target extension point.
     *
     * @param beam  the beam to extend
     * @param side  the horizontal side
     * @param extPt the targeted extension point
     * @return true if extension was done, false otherwise
     */
    private boolean extendToPoint (AbstractBeamInter beam,
                                   HorizontalSide side,
                                   Point2D extPt)
    {
        final boolean logging = beam.isVip() || logger.isDebugEnabled();

        if (logging) {
            logger.info("VIP extendToPoint for {}", beam);
        }

        final Line2D median = beam.getMedian();
        final double height = beam.getHeight();

        // Check we have a concrete extension
        Point2D endPt = (side == LEFT) ? median.getP1() : median.getP2();
        double extDx = (side == LEFT) ? (endPt.getX() - extPt.getX()) : (extPt.getX()
                                                                         - endPt.getX());

        // (to cope with rounding we use 1 instead of 0)
        if (extDx <= 1) {
            return false;
        }

        // Check we have a high enough black ratio in the extension zone
        Area extArea = sideAreaOf("+", beam, side, 0, extDx, 0);
        AreaMask extMask = new AreaMask(extArea);
        WrappedInteger extCore = new WrappedInteger(0);
        int extCoreCount = extMask.fore(extCore, pixelFilter);
        double extCoreRatio = (double) extCore.value / extCoreCount;

        if (extCoreRatio < params.minExtBlackRatio) {
            if (logging) {
                logger.info("VIP {} lacks pixels in stem extension", beam);
            }

            return false;
        }

        // Resulting median
        final Line2D newMedian;

        if (side == LEFT) {
            newMedian = new Line2D.Double(extPt, median.getP2());
        } else {
            newMedian = new Line2D.Double(median.getP1(), extPt);
        }

        // Impacts
        final double distImpact = ((Impacts) beam.getImpacts()).getDistImpact();
        BeamItem newItem = new BeamItem(newMedian, height);

        if (beam.isVip()) {
            newItem.setVip(true);
        }

        Impacts impacts = computeBeamImpacts(newItem, true, true, distImpact);

        if ((impacts != null) && (impacts.getGrade() >= BeamInter.getMinGrade())) {
            BeamInter newBeam = new BeamInter(impacts, newMedian, height);

            if (beam.isVip()) {
                newBeam.setVip(true);
            }

            registerBeam(newBeam);
            rawSystemBeams.add(newBeam);
            beam.remove();

            if (logging) {
                logger.info("VIP {} extended as {} {}", beam, newBeam, newBeam.getImpacts());
            }

            return true;
        } else {
            if (logging) {
                logger.info("VIP {} extension failed", beam);
            }

            return false;
        }
    }

    //--------------//
    // extendToSpot //
    //--------------//
    /**
     * Try to extend the provided beam on the desired side to a spot within reach.
     *
     * @param beam  the beam to extend
     * @param side  the horizontal side
     * @param maxDx limit on x extension (due to side beam if any)
     * @return true if extension was done, false otherwise
     */
    private boolean extendToSpot (AbstractBeamInter beam,
                                  HorizontalSide side,
                                  Integer maxDx)
    {
        final boolean logging = beam.isVip() || logger.isDebugEnabled();
        final int dx = (maxDx == null) ? params.maxExtensionToSpot
                : Math.min(params.maxExtensionToSpot, maxDx);
        final Area luArea = sideAreaOf("O", beam, side, 0, dx, 0);
        final List<Glyph> spots = new ArrayList<Glyph>(
                Glyphs.intersectedGlyphs(sortedBeamSpots, luArea));
        Collections.sort(spots, Glyphs.byAbscissa);

        if (beam.getGlyph() != null) {
            spots.remove(beam.getGlyph()); // Safer
        }

        if (!spots.isEmpty()) {
            // Pick up the nearest spot
            Glyph spot = (side == LEFT) ? spots.get(spots.size() - 1) : spots.get(0);

            if (logging) {
                logger.info("VIP {} found spot#{} on {}", beam, spot.getId(), side);
            }

            // Try to extend the beam to this spot, inclusive
            Line2D median = beam.getMedian();
            Rectangle spotBox = spot.getBounds();
            int x = (side == LEFT) ? spotBox.x : ((spotBox.x + spotBox.width) - 1);
            Point2D extPt = LineUtil.intersectionAtX(median, x);

            return extendToPoint(beam, side, extPt);
        }

        return false;
    }

    //--------------//
    // extendToStem //
    //--------------//
    /**
     * Try to extend the provided beam on the desired side to a stem seed within reach.
     *
     * @param beam  the beam to extend
     * @param side  the horizontal side
     * @param maxDx limit on x extension (due to side beam if any)
     * @return true if extension was done, false otherwise
     */
    private boolean extendToStem (AbstractBeamInter beam,
                                  HorizontalSide side,
                                  Integer maxDx)
    {
        final boolean logging = beam.isVip() || logger.isDebugEnabled();
        final int dx = (maxDx == null) ? params.maxExtensionToStem
                : Math.min(params.maxExtensionToStem, maxDx);
        final int dy = params.maxStemBeamGapY;
        final Area luArea = sideAreaOf("|", beam, side, dy, dx, 0);
        List<Glyph> seeds = new ArrayList<Glyph>(
                Glyphs.intersectedGlyphs(sortedSystemSeeds, luArea));
        Collections.sort(seeds, Glyphs.byAbscissa);

        // We should remove seeds already 'embraced' by the beam
        // It's easier to proceed and simply check for concrete extension.
        if (!seeds.isEmpty()) {
            // Pick up the nearest stem seed
            Glyph seed = (side == LEFT) ? seeds.get(seeds.size() - 1) : seeds.get(0);

            if (logging) {
                logger.info("{} found stem#{} on {}", beam, seed.getId(), side);
            }

            // Try to extend the beam to this stem seed
            Line2D seedLine = new Line2D.Double(
                    seed.getStartPoint(Orientation.VERTICAL),
                    seed.getStopPoint(Orientation.VERTICAL));
            Point2D extPt = LineUtil.intersection(beam.getMedian(), seedLine);

            return extendToPoint(beam, side, extPt);
        }

        return false;
    }

    //------------------//
    // getCueAggregates //
    //------------------//
    /**
     * Gather the cue heads and stems regions into aggregates of at least two heads
     * (and stems).
     *
     * @return the aggregates retrieved
     */
    private List<CueAggregate> getCueAggregates ()
    {
        List<CueAggregate> aggregates = new ArrayList<CueAggregate>();

        // We look for collections of good cue black heads + stem, close enough
        // to be able to be connected by a cue beam.
        List<Inter> smallBlacks = sig.inters(
                new Predicate<Inter>()
        {
            @Override
            public boolean check (Inter inter)
            {
                if (inter.isRemoved() || (inter.getShape() != Shape.NOTEHEAD_BLACK_SMALL)) {
                    return false;
                }

                if (inter.getContextualGrade() == null) {
                    sig.computeContextualGrade(inter);
                }

                return inter.getContextualGrade() >= Grades.minContextualGrade;
            }
        });

        if (smallBlacks.isEmpty()) {
            return aggregates;
        }

        Collections.sort(smallBlacks, Inters.byAbscissa);
        logger.debug("S#{} cues:{}", system.getId(), smallBlacks);

        // Look for aggregates of close instances
        for (Inter head : smallBlacks) {
            Inter stem = stemOf(head);
            Rectangle headBox = head.getBounds();
            headBox.grow(params.cueXMargin, params.cueYMargin);

            // Check among existing aggregates
            CueAggregate aggregate = null;

            for (CueAggregate ag : aggregates) {
                if (ag.bounds.intersects(headBox)) {
                    aggregate = ag;

                    break;
                }
            }

            if (aggregate == null) {
                aggregate = new CueAggregate();
                aggregates.add(aggregate);
            }

            aggregate.add(head, stem);
        }

        // Purge aggregates with a single head
        for (Iterator<CueAggregate> it = aggregates.iterator(); it.hasNext();) {
            CueAggregate ag = it.next();

            if (ag.heads.size() == 1) {
                it.remove();
            }
        }

        if (!aggregates.isEmpty()) {
            for (int i = 0; i < aggregates.size(); i++) {
                CueAggregate aggregate = aggregates.get(i);
                aggregate.identify(i);
                logger.debug("{}", aggregate);
            }
        }

        return aggregates;
    }

    //-------------//
    // getSideBeam //
    //-------------//
    /**
     * Look for a compatible beam inter next to the provided one (in a same beam line).
     * They either can be merged or give a limit to other extension modes.
     *
     * @param beam     the provided beam
     * @param side     which side to look on
     * @param maxGapDx max gap width between the two beams, or default value if null
     * @return the sibling beam found if any
     */
    private AbstractBeamInter getSideBeam (AbstractBeamInter beam,
                                           final HorizontalSide side,
                                           Double maxGapDx)
    {
        Area luArea = (maxGapDx != null) ? sideAreaOf(null, beam, side, 0, maxGapDx, 0)
                : sideAreaOf("-", beam, side, 0, params.maxSideBeamDx, 0);

        List<Inter> others = Inters.intersectedInters(rawSystemBeams, GeoOrder.NONE, luArea);
        others.remove(beam); // Safer

        if (!others.isEmpty()) {
            // Use a closer look, using colinearity
            final Line2D median = beam.getMedian();
            final Point2D endPt = (side == LEFT) ? median.getP1() : median.getP2();
            final double slope = LineUtil.getSlope(median);

            for (Iterator<Inter> it = others.iterator(); it.hasNext();) {
                AbstractBeamInter other = (AbstractBeamInter) it.next();

                // Check connection point & beam slopes are OK
                Line2D otherMedian = other.getMedian();

                if ((Math.abs(LineUtil.getSlope(otherMedian) - slope) > params.maxBeamSlopeGap)
                    || (otherMedian.ptLineDist(endPt) > params.maxBeamsGapY)) {
                    it.remove();
                }
            }

            // Keep just the closest one to current beam (abscissa-wise)
            if (others.size() > 1) {
                final double endX = endPt.getX();
                Collections.sort(
                        others,
                        new Comparator<Inter>()
                {
                    @Override
                    public int compare (Inter o1,
                                        Inter o2)
                    {
                        AbstractBeamInter b1 = (AbstractBeamInter) o1;
                        AbstractBeamInter b2 = (AbstractBeamInter) o2;

                        if (side == LEFT) {
                            return Double.compare(
                                    endX - b1.getMedian().getX2(),
                                    endX - b2.getMedian().getX2());
                        } else {
                            return Double.compare(
                                    b1.getMedian().getX1() - endX,
                                    b2.getMedian().getX1() - endX);
                        }
                    }
                });
            }

            if (!others.isEmpty()) {
                return (AbstractBeamInter) others.get(0);
            }
        }

        return null;
    }

    //-------------------------//
    // measureVerticalDistance //
    //-------------------------//
    /**
     * Measure vertical distance (center to center) between the two provided beams.
     * (These beams are very likely to be in a single group)
     *
     * @param one a beam
     * @param two another beam
     */
    private void measureVerticalDistance (AbstractBeamInter one,
                                          AbstractBeamInter two)
    {
        if (distances != null) {
            Line2D m1 = one.getMedian();
            Line2D m2 = two.getMedian();

            // Determine a suitable abscissa
            double maxLeft = Math.max(m1.getX1(), m2.getX1());
            double minRight = Math.min(m1.getX2(), m2.getX2());
            double x = (maxLeft + minRight) / 2;

            // Measure actual vertical distance at this abscissa
            double y1 = LineUtil.yAtX(m1, x);
            double y2 = LineUtil.yAtX(m2, x);
            double distance = Math.abs(y2 - y1);
            distances.includeValue(distance);
        }
    }

    //---------//
    // mergeOf //
    //---------//
    /**
     * (Try to) create a new BeamInter instance that represents a merge of the provided
     * beams.
     *
     * @param one a beam
     * @param two another beam
     * @return the resulting beam, or null if failed
     */
    private BeamInter mergeOf (AbstractBeamInter one,
                               AbstractBeamInter two)
    {
        final Line2D oneMedian = one.getMedian();
        final Line2D twoMedian = two.getMedian();

        // Mean dist
        double distImpact = (((Impacts) one.getImpacts()).getDistImpact()
                             + ((Impacts) two.getImpacts()).getDistImpact()) / 2;

        // Height
        double oneWidth = oneMedian.getX2() - oneMedian.getX1();
        double twoWidth = twoMedian.getX2() - twoMedian.getX1();
        double height = ((one.getHeight() * oneWidth) + (two.getHeight() * twoWidth)) / (oneWidth
                                                                                         + twoWidth);

        // Median & width
        final Line2D median;

        if (oneMedian.getX1() < twoMedian.getX1()) {
            median = new Line2D.Double(oneMedian.getP1(), twoMedian.getP2());
        } else {
            median = new Line2D.Double(twoMedian.getP1(), oneMedian.getP2());
        }

        BeamItem newItem = new BeamItem(median, height);

        if (one.isVip() || two.isVip()) {
            newItem.setVip(true);
        }

        Impacts impacts = computeBeamImpacts(newItem, true, true, distImpact);

        if ((impacts != null) && (impacts.getGrade() >= BeamInter.getMinGrade())) {
            return new BeamInter(impacts, median, height);
        } else {
            return null;
        }
    }

    //------------//
    // middleArea //
    //------------//
    /**
     * Report the gap area between two beams.
     * (The beams are merge candidates assumed to be co-linear)
     *
     * @param one a beam
     * @param two another beam
     * @return the area between them
     */
    private Area middleArea (AbstractBeamInter one,
                             AbstractBeamInter two)
    {
        final Line2D oneMedian = one.getMedian();
        final Line2D twoMedian = two.getMedian();

        // Height
        double oneWidth = oneMedian.getX2() - oneMedian.getX1();
        double twoWidth = twoMedian.getX2() - twoMedian.getX1();
        double height = ((one.getHeight() * oneWidth) + (two.getHeight() * twoWidth)) / (oneWidth
                                                                                         + twoWidth);

        // Median
        final Line2D median;

        if (oneMedian.getX1() < twoMedian.getX1()) {
            median = new Line2D.Double(oneMedian.getP2(), twoMedian.getP1());
        } else {
            median = new Line2D.Double(twoMedian.getP2(), oneMedian.getP1());
        }

        return AreaUtil.horizontalParallelogram(median.getP1(), median.getP2(), height);
    }

    //---------//
    // overlap //
    //---------//
    /**
     * Check whether the provided (hook) item overlaps any existing
     * beam inter (in rawSystemBeams collection).
     * This is mainly to detect if a hook candidate is not actually "part of"
     * any existing beam.
     *
     * @param item the item to check
     * @return true if any overlap was found
     */
    private boolean overlap (BeamItem item)
    {
        // First filtering using rough intersection (area / rectangle)
        Area itemCore = item.getCoreArea();
        List<Inter> beams = Inters.intersectedInters(rawSystemBeams, GeoOrder.NONE, itemCore);

        if (beams.isEmpty()) {
            return false;
        }

        // More precise look, checking that item center lies within beam area
        Point itemCenter = GeoUtil.centerOf(itemCore.getBounds());

        for (Inter inter : beams) {
            AbstractBeamInter beam = (AbstractBeamInter) inter;

            if (beam.getArea().contains(itemCenter)) {
                return true;
            }
        }

        return false;
    }

    //--------------//
    // registerBeam //
    //--------------//
    /**
     * Add the provided beam to sig and link it with its underlying glyph.
     *
     * @param beam the provided beam
     */
    private void registerBeam (AbstractBeamInter beam)
    {
        sig.addVertex(beam);

        Glyph glyph = retrieveGlyph(beam);
        beam.setGlyph(glyph);

        // Make this glyph survive the beam removal if any
        system.addFreeGlyph(glyph);
    }

    //---------------//
    // retrieveGlyph //
    //---------------//
    /**
     * Given a beam (with its area), build the underlying glyph.
     *
     * @param beam the provided beam
     * @return the glyph built
     */
    private Glyph retrieveGlyph (AbstractBeamInter beam)
    {
        final Rectangle box = beam.getBounds();
        final ByteProcessor buf = new ByteProcessor(box.width, box.height);
        final int filterWidth = pixelFilter.getWidth();
        final int filterHeight = pixelFilter.getHeight();
        ByteUtil.raz(buf);

        final Point p = new Point(0, 0);

        for (int dy = 0; dy < box.height; dy++) {
            p.y = box.y + dy;

            for (int dx = 0; dx < box.width; dx++) {
                p.x = box.x + dx;

                if (p.x < filterWidth && p.y < filterHeight) {
                    final int val = pixelFilter.get(p.x, p.y);

                    if ((val == 0) && beam.contains(p)) {
                        buf.set(dx, dy, 0);
                    }
                }
            }
        }

        // Runs
        RunTable runTable = new RunTableFactory(VERTICAL).createTable(buf);

        // Glyph
        Glyph glyph = sheet.getGlyphIndex().registerOriginal(
                new BasicGlyph(box.x, box.y, runTable));

        if (glyph.getWeight() == 0) {
            logger.warn("No pixels for {}", beam);
        }

        return glyph;
    }

    //------------//
    // sideAreaOf //
    //------------//
    /**
     * Define an area on desired horizontal side of the beam.
     *
     * @param kind   kind of area (meant for attachment debug)
     * @param beam   the beam inter
     * @param side   desired side
     * @param double extDy ordinate extension
     * @param double extDx abscissa extension
     * @param double intDx abscissa offset towards beam interior
     * @return the area
     */
    private Area sideAreaOf (String kind,
                             AbstractBeamInter beam,
                             HorizontalSide side,
                             double extDy,
                             double extDx,
                             double intDx)
    {
        final Line2D median = beam.getMedian();
        final double height = beam.getHeight() + (2 * extDy);
        final double intX = (side == LEFT) ? (median.getX1() - 1 + intDx)
                : ((median.getX2() + 1) - intDx);
        final Point2D intPt = LineUtil.intersectionAtX(median, intX);
        final double extX = (side == LEFT) ? (median.getX1() - extDx) : (median.getX2() + extDx);
        final Point2D extPt = LineUtil.intersectionAtX(median, extX);
        Area area = (side == LEFT)
                ? AreaUtil.horizontalParallelogram(extPt, intPt, height)
                : AreaUtil.horizontalParallelogram(intPt, extPt, height);

        if (kind != null) {
            beam.addAttachment(kind + ((side == LEFT) ? "L" : "R"), area);
        }

        return area;
    }

    //--------//
    // stemOf //
    //--------//
    private Inter stemOf (Inter head)
    {
        for (Relation rel : sig.edgesOf(head)) {
            if (rel instanceof HeadStemRelation) {
                return sig.getEdgeTarget(rel);
            }
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------------//
    // ItemParameters //
    //----------------//
    /** Parameters that govern beam/hook items, sometime dependent on cue/standard. */
    public static class ItemParameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final double minBeamWidthLow;

        final double minBeamWidthHigh;

        final double minHookWidthLow;

        final double minHookWidthHigh;

        final double maxHookWidth;

        final double minHeightLow;

        final double typicalHeight;

        final double maxHeightHigh;

        final double cornerMargin;

        final double maxItemXGap;

        final int coreSectionWidth;

        //~ Constructors ---------------------------------------------------------------------------
        public ItemParameters (Scale scale,
                               double ratio)
        {
            minBeamWidthLow = scale.toPixelsDouble(constants.minBeamWidthLow);
            minBeamWidthHigh = scale.toPixelsDouble(constants.minBeamWidthHigh);
            minHookWidthLow = scale.toPixelsDouble(constants.minHookWidthLow);
            minHookWidthHigh = scale.toPixelsDouble(constants.minHookWidthHigh);
            maxHookWidth = scale.toPixelsDouble(constants.maxHookWidth);

            typicalHeight = scale.getBeamThickness() * ratio;
            minHeightLow = typicalHeight * constants.minHeightRatioLow.getValue();
            maxHeightHigh = typicalHeight * constants.maxHeightRatioHigh.getValue();
            cornerMargin = typicalHeight * constants.cornerMarginRatio.getValue();

            maxItemXGap = scale.toPixelsDouble(constants.maxItemXGap);
            coreSectionWidth = scale.toPixels(constants.coreSectionWidth);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        // Item parameters
        //----------------
        private final Scale.Fraction minBeamWidthLow = new Scale.Fraction(
                1.0, // 1.5,
                "Low minimum width for a beam");

        private final Scale.Fraction minBeamWidthHigh = new Scale.Fraction(
                4.0,
                "High minimum width for a beam");

        private final Scale.Fraction minHookWidthLow = new Scale.Fraction(
                0.7,
                "Low minimum width for a hook");

        private final Scale.Fraction minHookWidthHigh = new Scale.Fraction(
                1.0,
                "High minimum width for a hook");

        private final Scale.Fraction maxHookWidth = new Scale.Fraction(
                2.0,
                "Maximum width for a hook");

        private final Constant.Ratio minHeightRatioLow = new Constant.Ratio(
                0.7,
                "Low minimum height for a beam or hook, specified as ratio of typical beam");

        private final Constant.Ratio maxHeightRatioHigh = new Constant.Ratio(
                1.3,
                "High maximum height for a beam or hook, specified as ratio of typical beam");

        private final Constant.Ratio cornerMarginRatio = new Constant.Ratio(
                0.2,
                "Corner margin for a beam or hook, specified as ratio of typical beam");

        private final Scale.Fraction maxItemXGap = new Scale.Fraction(
                0.5,
                "Acceptable abscissa gap within a beam item");

        private final Scale.Fraction coreSectionWidth = new Scale.Fraction(
                0.15,
                "Minimum width for a core section to define borders");

        // Global parameters
        //------------------
        private final Scale.Fraction maxSideBeamDx = new Scale.Fraction(
                4.0,
                "Maximum abscissa gap to detect side beams");

        private final Scale.Fraction maxBeamsGapX = new Scale.Fraction(
                1.0,
                "Maximum abscissa gap to merge aligned beams");

        private final Scale.Fraction minBeamsGapX = new Scale.Fraction(
                0.2,
                "Minimum abscissa gap to check inner area");

        private final Scale.Fraction maxBeamsGapY = new Scale.Fraction(
                0.25,
                "Maximum ordinate mismatch to merge aligned beams");

        private final Scale.Fraction beamsXMargin = new Scale.Fraction(
                0.25,
                "Abscissa margin around beams to exclude beam stems");

        private final Scale.Fraction maxStemBeamGapX = new Scale.Fraction(
                0.2,
                "Maximum abscissa gap between stem and beam");

        private final Scale.Fraction maxStemBeamGapY = new Scale.Fraction(
                0.8,
                "Maximum ordinate gap between stem and beam");

        private final Scale.Fraction maxExtensionToStem = new Scale.Fraction(
                4.0,
                "Maximum beam horizontal extension to stem seed");

        private final Scale.Fraction maxExtensionToSpot = new Scale.Fraction(
                2.0,
                "Maximum beam horizontal extension to spot");

        private final Scale.Fraction beltMarginDx = new Scale.Fraction(
                0.25,
                "Horizontal belt margin checked around beam");

        private final Scale.Fraction beltMarginDy = new Scale.Fraction(
                0.15,
                "Vertical belt margin checked around beam");

        private final Constant.Double maxBeamSlope = new Constant.Double(
                "tangent",
                1.0,
                "Maximum absolute tangent value for a beam angle");

        private final Constant.Double maxBorderSlopeGap = new Constant.Double(
                "tangent",
                0.15,
                "Maximum delta slope between top and bottom borders of a beam");

        private final Constant.Double maxBeamSlopeGap = new Constant.Double(
                "tangent",
                0.07,
                "Maximum delta slope between beams of a group");

        private final Scale.Fraction maxDistanceToBorder = new Scale.Fraction(
                0.15,
                "Maximum mean distance to average beam border");

        private final Constant.Ratio maxJitterRatio = new Constant.Ratio(
                0.02,
                "Maximum border jitter ratio");

        private final Constant.Ratio maxBeltBlackRatio = new Constant.Ratio(
                0.4,
                "Maximum ratio of black pixels around beam");

        private final Constant.Ratio minCoreBlackRatio = new Constant.Ratio(
                0.7,
                "Minimum ratio of black pixels inside beam");

        private final Constant.Ratio minExtBlackRatio = new Constant.Ratio(
                0.6,
                "Minimum ratio of black pixels inside beam extension");

        private final Scale.Fraction cueXMargin = new Scale.Fraction(
                2.0,
                "Abscissa margin to aggregate cues");

        private final Scale.Fraction cueYMargin = new Scale.Fraction(
                3.0,
                "Ordinate margin to aggregate cues");

        private final Scale.Fraction cueBoxDx = new Scale.Fraction(
                0.25,
                "Abscissa expansion of aggregate box");

        private final Scale.Fraction cueBoxDy = new Scale.Fraction(
                1.0,
                "Ordinate shift of aggregate box");

        private final Constant.Ratio cueBeamRatio = new Constant.Ratio(
                0.6,
                "Ratio applied for cue beams height");
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

        final int maxSideBeamDx;

        final int minBeamsGapX;

        final int maxBeamsGapX;

        final int maxBeamsGapY;

        final int beamsXMargin;

        final int maxStemBeamGapX;

        final int maxStemBeamGapY;

        final int maxExtensionToStem;

        final int maxExtensionToSpot;

        final int beltMarginDx;

        final int beltMarginDy;

        final double maxBeamSlope;

        final double maxBorderSlopeGap;

        final double maxBeamSlopeGap;

        final double maxDistanceToBorder;

        final double maxJitterRatio;

        final double maxBeltBlackRatio;

        final double minCoreBlackRatio;

        final double minExtBlackRatio;

        final int cueXMargin;

        final int cueYMargin;

        final int cueBoxDx;

        final int cueBoxDy;

        final double cueBeamRatio;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            maxSideBeamDx = scale.toPixels(constants.maxSideBeamDx);
            minBeamsGapX = scale.toPixels(constants.minBeamsGapX);
            maxBeamsGapX = scale.toPixels(constants.maxBeamsGapX);
            maxBeamsGapY = scale.toPixels(constants.maxBeamsGapY);
            beamsXMargin = scale.toPixels(constants.beamsXMargin);
            maxStemBeamGapX = scale.toPixels(constants.maxStemBeamGapX);
            maxStemBeamGapY = scale.toPixels(constants.maxStemBeamGapY);
            maxExtensionToStem = scale.toPixels(constants.maxExtensionToStem);
            maxExtensionToSpot = scale.toPixels(constants.maxExtensionToSpot);
            beltMarginDx = scale.toPixels(constants.beltMarginDx);
            beltMarginDy = scale.toPixels(constants.beltMarginDy);
            maxBeamSlope = constants.maxBeamSlope.getValue();
            maxBorderSlopeGap = constants.maxBorderSlopeGap.getValue();
            maxBeamSlopeGap = constants.maxBeamSlopeGap.getValue();
            maxDistanceToBorder = scale.toPixelsDouble(constants.maxDistanceToBorder);
            maxJitterRatio = constants.maxJitterRatio.getValue();
            maxBeltBlackRatio = constants.maxBeltBlackRatio.getValue();
            minCoreBlackRatio = constants.minCoreBlackRatio.getValue();
            minExtBlackRatio = constants.minExtBlackRatio.getValue();
            cueXMargin = scale.toPixels(constants.cueXMargin);
            cueYMargin = scale.toPixels(constants.cueYMargin);
            cueBoxDx = scale.toPixels(constants.cueBoxDx);
            cueBoxDy = scale.toPixels(constants.cueBoxDy);
            cueBeamRatio = constants.cueBeamRatio.getValue();

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }

    //--------------//
    // CueAggregate //
    //--------------//
    /**
     * Describes an aggregate of cue notes.
     * We assume that within a cue aggregate, the layout is rather simple:
     * - all stems have the same direction.
     */
    private class CueAggregate
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Unique Id. (in page) */
        private String id = "";

        /** Bounds of the aggregate. */
        private Rectangle bounds;

        /** Sequence of cue heads. */
        private final List<Inter> heads = new ArrayList<Inter>();

        /** Sequence of stems. (parallel to heads list) */
        private final List<Inter> stems = new ArrayList<Inter>();

        /** Global stem direction. (up:-1, down:+1, mixed/unknown:0) */
        private int globalDir = 0;

        //~ Methods --------------------------------------------------------------------------------
        public void add (Inter head,
                         Inter stem)
        {
            // Head/Stem box
            Rectangle hsBox = head.getBounds().union(stem.getBounds());

            if (bounds == null) {
                bounds = hsBox;
            } else {
                bounds.add(hsBox);
            }

            heads.add(head);
            stems.add(stem);
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append("{");
            sb.append(id);

            if (bounds != null) {
                sb.append(" bounds:").append(bounds);
            }

            for (int i = 0; i < heads.size(); i++) {
                sb.append(" ").append(heads.get(i)).append("+").append(stems.get(i));
            }

            sb.append("}");

            return sb.toString();
        }

        /**
         * (Try to) connect the provided stem to compatible beams.
         *
         * @param stem     the provided stem
         * @param allBeams the collection of beams identified
         */
        private void connectStemToBeams (Inter stem,
                                         List<Inter> allBeams,
                                         Inter head)
        {
            // Which head corner?
            if (globalDir == 0) {
                return;
            }

            final Corner corner;
            int headX = GeoUtil.centerOf(head.getBounds()).x;
            int stemX = GeoUtil.centerOf(stem.getBounds()).x;

            if (headX <= stemX) {
                corner = (globalDir < 0) ? Corner.TOP_LEFT : Corner.BOTTOM_LEFT;
            } else {
                corner = (globalDir < 0) ? Corner.TOP_RIGHT : Corner.BOTTOM_RIGHT;
            }

            // Limit to beams that cross stem vertical line
            List<Inter> beams = new ArrayList<Inter>();
            Rectangle fatStemBox = stem.getBounds();
            fatStemBox.grow(params.cueBoxDx, 0);
            fatStemBox.y = bounds.y;
            fatStemBox.height = bounds.height;

            for (Inter beam : allBeams) {
                if (fatStemBox.intersects(beam.getBounds())) {
                    beams.add(beam);
                }
            }

            new StemsBuilder(system).linkCueBeams(head, corner, stem, beams);
        }

        /**
         * Retrieve cue glyph instances out of an aggregate snapshot.
         *
         * @return the list of glyph instances found
         */
        private List<Glyph> getCueGlyphs ()
        {
            // Expand aggregate bounds using global direction
            Rectangle box = new Rectangle(bounds);
            box.grow(params.cueBoxDx, 0);

            if (globalDir != 0) {
                box.y += (globalDir * params.cueBoxDy);
            } else {
                box.grow(0, params.cueBoxDy);
            }

            // Take a small *COPY* of binary image and apply morphology
            Picture picture = sheet.getPicture();
            ByteProcessor whole = picture.getSource(Picture.SourceKey.BINARY);
            ByteProcessor buf = new ByteProcessor(box.width, box.height);

            for (int y = 0; y < box.height; y++) {
                for (int x = 0; x < box.width; x++) {
                    int val = whole.get(box.x + x, box.y + y);
                    buf.set(x, y, val);
                }
            }

            double beam = params.cueBeamRatio * sheet.getScale().getBeamThickness();

            return new SpotsBuilder(sheet).buildSpots(buf, box.getLocation(), beam, id);
        }

        /**
         * Retrieve the global stem direction in the aggregate.
         *
         * @return the global direction found, 0 otherwise
         */
        private int getDirection ()
        {
            Integer dir = null;

            for (int i = 0; i < heads.size(); i++) {
                final Inter head = heads.get(i);
                final int headY = GeoUtil.centerOf(head.getBounds()).y;
                final Inter stem = stems.get(i);
                final Rectangle stemBox = stem.getBounds();

                // Consider relative position is reliable only if head center
                // is found in upper quarter or lower quarter of stem height
                final int quarter = (int) Math.rint(stemBox.height / 4.0);

                if (headY >= ((stemBox.y + stemBox.height) - quarter)) {
                    if (dir == null) {
                        dir = -1;
                    } else if (dir > 0) {
                        return 0;
                    }
                } else if (headY <= (stemBox.y + quarter)) {
                    if (dir == null) {
                        dir = 1;
                    } else if (dir < 0) {
                        return 0;
                    }
                }
            }

            return (dir != null) ? dir : 0;
        }

        private void identify (int index)
        {
            id = "S" + system.getId() + "A" + (index + 1);
        }

        private void process (List<Glyph> spots)
        {
            // Determine stem direction in the aggregate
            globalDir = getDirection();

            if (globalDir == 0) {
                logger.info("Mixed or unknown direction in cue area {}", this);

                return;
            }

            // Retrieve candidate glyphs from spots
            List<Glyph> glyphs = getCueGlyphs();

            // Retrieve beams from candidate glyphs
            List<Inter> beams = new ArrayList<Inter>();

            for (Glyph glyph : glyphs) {
                glyph = system.registerGlyph(glyph, GlyphGroup.BEAM_SPOT);
                spots.add(glyph);

                List<Inter> glyphBeams = new ArrayList<Inter>();
                final String failure = checkBeamGlyph(glyph, true, glyphBeams);

                if (failure != null) {
                    if (glyph.isVip()) {
                        logger.info("VIP cue#{} {}", glyph.getId(), failure);
                    }
                } else {
                    if (glyph.isVip()) {
                        logger.debug("{} -> {}", glyph.idString(), glyphBeams);
                    }

                    beams.addAll(glyphBeams);
                }
            }

            // Link stems & beams as possible
            if (!beams.isEmpty()) {
                for (int i = 0; i < heads.size(); i++) {
                    final Inter head = heads.get(i);
                    final Inter stem = stems.get(i);
                    connectStemToBeams(stem, beams, head);
                }
            }
        }
    }
}
