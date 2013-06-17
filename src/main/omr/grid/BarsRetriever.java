//----------------------------------------------------------------------------//
//                                                                            //
//                         B a r s R e t r i e v e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.NestView;

import omr.lag.BasicLag;
import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.SectionsBuilder;

import omr.math.Barycenter;
import omr.math.BasicLine;
import omr.math.Line;
import omr.math.LineUtil;

import static omr.run.Orientation.*;
import omr.run.RunsTable;

import omr.sheet.BarsChecker;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;
import omr.sheet.SystemInfo;

import omr.step.StepException;

import omr.ui.Colors;
import omr.ui.util.UIUtil;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.VipUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class {@code BarsRetriever} focuses on the retrieval of vertical
 * barlines.
 * Barlines are used to determine the side limits of staves and, most
 * importantly, the gathering of staves into systems.
 * The other barlines are used to determine parts and measures.
 *
 * @author Hervé Bitteur
 */
public class BarsRetriever
        implements NestView.ItemRenderer
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters. */
    private static final Constants constants = new Constants();

    /** Usual logger utility. */
    private static final Logger logger = LoggerFactory.getLogger(BarsRetriever.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Related sheet. */
    private final Sheet sheet;

    /** Scale-dependent constants for vertical stuff. */
    private final Parameters params;

    /** Lag of vertical runs. */
    private Lag vLag;

    /** Related staff manager. */
    private final StaffManager staffManager;

    /** Long vertical filaments found, non sorted. */
    private final List<Glyph> filaments = new ArrayList<>();

    /** Intersections between staves and bar sticks. */
    private final Map<StaffInfo, IntersectionSequence> crossings = new TreeMap<>(StaffInfo.byId);

    /**
     * System tops.
     * For each staff, gives the staff that starts the containing system
     */
    private Integer[] systemTops;

    /**
     * Part tops.
     * For each staff, gives the staff that starts the containing part
     */
    private Integer[] partTops;

    //~ Constructors -----------------------------------------------------------
    //
    //---------------//
    // BarsRetriever //
    //---------------//
    /**
     * Retrieve the frames of all staff lines.
     *
     * @param sheet the sheet to process
     */
    public BarsRetriever (Sheet sheet)
    {
        this.sheet = sheet;

        // Scale-dependent parameters
        params = new Parameters(sheet.getScale());

        // Companions
        staffManager = sheet.getStaffManager();
    }

    //~ Methods ----------------------------------------------------------------
    //
    //----------//
    // buildLag //
    //----------//
    /**
     * Build the underlying lag, out of the provided runs table.
     * This method must be called before building info.
     *
     * @param vertTable the provided table of vertical runs
     */
    public void buildLag (RunsTable vertTable)
    {
        vLag = new BasicLag("vLag", VERTICAL);

        SectionsBuilder sectionsBuilder = new SectionsBuilder(
                vLag,
                new JunctionRatioPolicy(params.maxLengthRatio));
        sectionsBuilder.createSections(vertTable);

        sheet.setVerticalLag(vLag);

        // Debug sections VIPs
        for (int id : params.vipSections) {
            Section sect = vLag.getVertexById(id);

            if (sect != null) {
                sect.setVip();
            }
        }
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Render the filaments and their ending tangents if so desired.
     *
     * @param g graphics context
     */
    @Override
    public void renderItems (Graphics2D g)
    {
        if (!constants.showVerticalLines.isSet()) {
            return;
        }

        final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
        final Color oldColor = g.getColor();
        g.setColor(Colors.ENTITY_MINOR);

        // Draw filaments
        for (Glyph filament : filaments) {
            filament.renderLine(g);
        }

        // Draw tangent at each ending point (using max coord gap)?
        if (constants.showTangents.isSet()) {
            g.setColor(Colors.TANGENT);

            double dy = sheet.getScale()
                    .toPixels(constants.maxCoordGap);

            for (Glyph glyph : filaments) {
                Point2D p = glyph.getStartPoint(VERTICAL);
                double derivative = (glyph instanceof Filament)
                        ? ((Filament) glyph).slopeAt(p.getY(),
                        VERTICAL)
                        : glyph.getInvertedSlope();
                g.draw(new Line2D.Double(p.getX(), p.getY(),
                        p.getX() - (derivative * dy),
                        p.getY() - dy));
                p = glyph.getStopPoint(VERTICAL);
                derivative = (glyph instanceof Filament)
                        ? ((Filament) glyph).slopeAt(p.getY(), VERTICAL)
                        : glyph.getInvertedSlope();
                g.draw(new Line2D.Double(p.getX(), p.getY(),
                        p.getX() + (derivative * dy),
                        p.getY() + dy));
            }
        }

        g.setStroke(oldStroke);
        g.setColor(oldColor);
    }

    //---------------------//
    // retrieveMeasureBars //
    //---------------------//
    /**
     * Use long vertical sections to retrieve the barlines that define
     * measures.
     * We first filter the bar candidates in a less rough manner now that we
     * have precise values for staff lines coordinates.
     * Then, we use consistency checks within the same system, where measure
     * barlines should be aligned vertically.
     *
     * <pre>
     * retrieveMeasureBars()
     *    +  recheckBarCandidates()       // Strict barSticks checking
     *
     *    +  (per system)
     *       + checkBarsAlignment(system) // Check bars consistency w/i system
     *
     *    +  retrievePartTops()           // Retrieve parts top staves
     *
     *    +  (per system)
     *       + refineBarsEndings(system)  // Refine endings for each bar line
     * </pre>
     */
    public void retrieveMeasureBars ()
    {
        // Strict barSticks checking
        recheckBarCandidates();

        // Check bars consistency within the same system
        for (SystemInfo system : sheet.getSystems()) {
            checkBarsAlignment(system);
        }

        // We already have systemTop for each staff
        // let's try to define partTop for each staff
        partTops = retrievePartTops();

        logger.info("{}Parts   top staff ids: {}",
                sheet.getLogPrefix(), Arrays.toString(partTops));

        // Refine ending points for each bar line
        for (SystemInfo system : sheet.getSystems()) {
            refineBarsEndings(system);
        }
    }

    //--------------------//
    // retrieveSystemBars //
    //--------------------//
    /**
     * Use the long vertical sections to retrieve the barlines that
     * define the limits of systems and staves.
     *
     * <pre>
     * retrieveSystemBars()
     *    +  retrieveMajorBars()
     *       +  buildVerticalFilaments()       // Build vertical filaments
     *       +  retrieveBarCandidates()        // Retrieve initial barline candidates
     *       +  populateCrossings()            // Assign bar candidates to intersected staves
     *       +  retrieveStaffSideBars()        // Retrieve left staff bars and right staff bars
     *
     *    +  buildSystems()
     *       +  retrieveSystemTops()           // Retrieve top staves (they start systems)
     *       +  createSystems()                // Create system frames using top staves
     *       +  mergeSystems()                 // Merge of systems as much as possible
     *
     *    +  adjustSides()                     // Adjust sides for systems, staves & lines
     *       +  (per system)
     *          +  (per side)
     *             + adjustSystemLimit(system, side) // Adjust system limit
     *          +  adjustStaffLines(system)    // Adjust staff lines to system limits
     * </pre>
     *
     * @throws StepException raised if processing must stop
     */
    public void retrieveSystemBars (Collection<Glyph> oldGlyphs,
                                    Collection<Glyph> newGlyphs)
            throws StepException
    {
        try {
            // Retrieve major barSticks (for system & staves limits)
            retrieveMajorBars(oldGlyphs, newGlyphs);
        } catch (Exception ex) {
            logger.warn(sheet.getLogPrefix()
                        + "BarsRetriever cannot retrieveBars", ex);
        }

        try {
            // Detect systems of staves aggregated via barlines
            buildSystems();
        } catch (Exception ex) {
            logger.warn(sheet.getLogPrefix()
                        + "BarsRetriever cannot retrieveSystems", ex);
        }

        // Adjust precise horizontal sides for systems, staves & lines
        adjustSides();
    }

    //------------------//
    // adjustStaffLines //
    //------------------//
    /**
     * Staff by staff, align the lines endings with the system limits,
     * and check the intermediate line points.
     * Package access meant for LinesRetriever companion.
     *
     * @param system the system to process
     */
    void adjustStaffLines (SystemInfo system)
    {
        for (StaffInfo staff : system.getStaves()) {
            logger.debug("{}", staff);

            // Adjust left and right endings of each line in the staff
            for (LineInfo l : staff.getLines()) {
                FilamentLine line = (FilamentLine) l;
                line.setEndingPoints(getLineEnding(system, staff, line, LEFT),
                        getLineEnding(system, staff, line, RIGHT));
            }

            // Insert line intermediate points, if so needed
            List<LineFilament> fils = new ArrayList<>();

            for (LineInfo l : staff.getLines()) {
                FilamentLine line = (FilamentLine) l;
                fils.add(line.fil);
            }

            for (LineInfo l : staff.getLines()) {
                FilamentLine line = (FilamentLine) l;
                line.fil.fillHoles(fils);
            }
        }
    }

    //------------------//
    // adjustSystemBars //
    //------------------//
    /**
     * Adjust start and stop points of system side barSticks.
     */
    void adjustSystemBars ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            try {
                for (HorizontalSide side : HorizontalSide.values()) {
                    Object limit = system.getLimit(side);

                    if (limit != null) {
                        // Determine first point of barline
                        StaffInfo firstStaff = system.getFirstStaff();
                        Point2D pStart = firstStaff.getFirstLine().
                                getEndPoint(side);

                        // Determine last point of barline
                        StaffInfo lastStaff = system.getLastStaff();
                        Point2D pStop = lastStaff.getLastLine().
                                getEndPoint(side);

                        // [Dirty programming, sorry]
                        if (limit instanceof Glyph) {
                            ((Glyph) limit).setEndingPoints(pStart, pStop);
                        }
                    }
                }
            } catch (Exception ex) {
                logger.warn("BarsRetriever can't adjust side bars of "
                            + system.idString(), ex);
            }
        }
    }

    //----------------------//
    // getSideBarlineGlyphs //
    //----------------------//
    /**
     * Report the set of all sticks that are actually part of the staff
     * side barlines (left or right side).
     *
     * @return the collection of used barline sticks
     */
    Set<Glyph> getSideBarlineGlyphs ()
    {
        Set<Glyph> sticks = new HashSet<>();

        for (StaffInfo staff : staffManager.getStaves()) {
            for (HorizontalSide side : HorizontalSide.values()) {
                BarInfo bar = staff.getBar(side);

                if (bar != null) {
                    sticks.addAll(bar.getSticksAncestors());
                }
            }
        }

        return sticks;
    }

    //--------//
    // isLong //
    //--------//
    boolean isLong (Glyph stick)
    {
        return (stick != null)
               && (stick.getLength(VERTICAL) >= params.minLongLength);
    }

    //--------//
    // isLong //
    //--------//
    boolean isLong (BarInfo bar)
    {
        if (bar != null) {
            for (Glyph stick : bar.getSticksAncestors()) {
                if (isLong(stick)) {
                    return true;
                }
            }
        }

        return false;
    }

    //-------------//
    // adjustSides //
    //-------------//
    /**
     * Adjust precise sides for systems, staves & lines.
     */
    private void adjustSides ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            try {
                for (HorizontalSide side : HorizontalSide.values()) {
                    // Determine the side limit of the system
                    adjustSystemLimit(system, side);
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("System#{} left:{} right:{}", system.getId(),
                            system.getLimit(LEFT).getClass()
                            .getSimpleName(),
                            system.getLimit(RIGHT).getClass()
                            .getSimpleName());
                }

                // Use system limits to adjust staff lines
                adjustStaffLines(system);
            } catch (Exception ex) {
                logger.warn("BarsRetriever cannot adjust system#"
                            + system.getId(), ex);
            }
        }
    }

    //-------------------//
    // adjustSystemLimit //
    //-------------------//
    /**
     * Adjust the limit on the desired side of the provided system and
     * store the chosen limit (whatever it is) in the system itself.
     *
     * @param system the system to process
     * @param side   the desired side
     *
     * @see SystemInfo#getLimit(HorizontalSide)
     */
    private void adjustSystemLimit (SystemInfo system,
                                    HorizontalSide side)
    {
        Glyph drivingStick = null;

        // Do we have a bar embracing the whole system?
        BarInfo bar = retrieveSystemBar(system, side);

        if (bar != null) {
            // We use the heaviest stick among the system-embracing bar sticks
            SortedSet<Glyph> allSticks = new TreeSet<>(
                    Glyph.byReverseWeight);

            for (Glyph stick : bar.getSticksAncestors()) {
                Point2D start = stick.getStartPoint(VERTICAL);
                StaffInfo topStaff = staffManager.getStaffAt(start);
                Point2D stop = stick.getStopPoint(VERTICAL);
                StaffInfo botStaff = staffManager.getStaffAt(stop);

                if ((topStaff == system.getFirstStaff())
                    && (botStaff == system.getLastStaff())) {
                    allSticks.add(stick);
                }
            }

            if (!allSticks.isEmpty()) {
                drivingStick = allSticks.first();

                logger.debug("System#{} {} drivingStick: {}",
                        system.getId(), side, drivingStick);

                // Polish long driving stick, if needed
                if (isLong(drivingStick) && drivingStick instanceof Filament) {
                    ((Filament) drivingStick).polishCurvature();
                }

                // Remember approximate limit abscissa for each staff
                for (StaffInfo staff : system.getStaves()) {
                    Point2D inter = staff.intersection(drivingStick);
                    staff.setAbscissa(side, inter.getX());
                }

                system.setLimit(side, drivingStick);
            }
        }

        if (drivingStick == null) {
            // We fall back using some centroid & slope
            // TODO: This algorithm could be refined
            Barycenter bary = new Barycenter();

            for (StaffInfo staff : system.getStaves()) {
                double x = extendStaffAbscissa(staff, side);
                double y = staff.getMidOrdinate(side);
                bary.include(x, y);
            }

            logger.debug("System#{} {} barycenter: {}",
                    system.getId(), side, bary);

            double slope = sheet.getSkew().getSlope();
            BasicLine line = new BasicLine();
            line.includePoint(bary.getX(), bary.getY());
            line.includePoint(bary.getX() - (100 * slope), bary.getY() + 100);
            system.setLimit(side, line);
        }
    }

    //--------------//
    // buildSystems //
    //--------------//
    /**
     * Detect systems of staves aggregated via connecting barlines.
     */
    private void buildSystems ()
    {
        do {
            // Retrieve the staves that start systems
            if (systemTops == null) {
                systemTops = retrieveSystemTops();
            }

            logger.info("{}Systems top staff ids: {}", sheet.getLogPrefix(),
                    Arrays.toString(systemTops));

            // Create system frames using staves tops
            sheet.setSystems(createSystems(systemTops));

            // Merge of systems as much as possible
            if (mergeSystems()) {
                logger.info("Systems modified, rebuilding...");
            } else {
                break;
            }
        } while (true);
    }

    //------------------------//
    // buildVerticalFilaments //
    //------------------------//
    /**
     * With vertical lag sections, build vertical filaments.
     *
     * @throws Exception
     */
    private void buildVerticalFilaments ()
            throws Exception
    {
        // Filaments factory
        FilamentsFactory factory = new FilamentsFactory(sheet.getScale(),
                sheet.getNest(),
                VERTICAL,
                Filament.class);

        // Factory parameters adjustment
        factory.setMaxSectionThickness(constants.maxSectionThickness);
        factory.setMaxFilamentThickness(constants.maxFilamentThickness);
        factory.setMaxCoordGap(constants.maxCoordGap);
        factory.setMaxPosGap(constants.maxPosGap);
        factory.setMaxSpace(constants.maxSpace);
        factory.setMaxOverlapDeltaPos(constants.maxOverlapDeltaPos);

        // Retrieve filaments out of vertical sections
        filaments.addAll(factory.retrieveFilaments(vLag.getVertices(), true));
    }

    //-------------------//
    // canConnectSystems //
    //-------------------//
    /**
     * Try to merge the two provided systems into a single one.
     *
     * @param prevSystem the system above
     * @param nextSystem the system below
     *
     * @return true if left barSticks have been merged
     */
    private boolean canConnectSystems (SystemInfo prevSystem,
                                       SystemInfo nextSystem)
    {
        List<SystemInfo> systems = sheet.getSystems();
        Skew skew = sheet.getSkew();

        if (logger.isDebugEnabled()) {
            logger.info("Checking S#{}({}) - S#{}({})", prevSystem.getId(),
                    prevSystem.getStaves().size(), nextSystem.getId(),
                    nextSystem.getStaves().size());
        }

        StaffInfo prevStaff = prevSystem.getLastStaff();
        Point2D prevStaffPt = skew.deskewed(prevStaff.getLastLine()
                .getEndPoint(LEFT));
        double prevY = prevStaffPt.getY();
        BarInfo prevBar = prevStaff.getBar(LEFT); // Perhaps null

        StaffInfo nextStaff = nextSystem.getFirstStaff();
        Point2D nextStaffPt = skew.deskewed(nextStaff.getFirstLine()
                .getEndPoint(LEFT));
        double nextY = nextStaffPt.getY();
        BarInfo nextBar = nextStaff.getBar(LEFT); // Perhaps null

        // Check vertical connections between barSticks
        if ((prevBar != null) && (nextBar != null)) {
            // case: Bar - Bar
            for (Glyph prevStick : prevBar.getSticksAncestors()) {
                Point2D prevPoint = skew.deskewed(prevStick.getStopPoint(
                        VERTICAL));

                for (Glyph nextStick : nextBar.getSticksAncestors()) {
                    Point2D nextPoint = skew.deskewed(nextStick.getStartPoint(
                            VERTICAL));

                    // Check dx
                    double dx = Math.abs(nextPoint.getX() - prevPoint.getX());

                    // Check dy
                    double dy = Math.abs(Math.min(nextY, nextPoint.getY())
                                         - Math.max(prevY, prevPoint.getY()));

                    logger.debug("F{}-F{} dx:{} vs {}, dy:{} vs {}",
                            prevStick.getId(), nextStick.getId(),
                            (float) dx, params.maxBarPosGap, (float) dy,
                            params.maxBarCoordGap);

                    if ((dx <= params.maxBarPosGap)
                        && (dy <= params.maxBarCoordGap)) {
                        logger.info("Merging systems S#{}({}) - S#{}({})",
                                prevSystem.getId(),
                                prevSystem.getStaves().size(),
                                nextSystem.getId(),
                                nextSystem.getStaves().size());
                        prevStick.stealSections(nextStick);

                        return tryRangeConnection(
                                systems.subList(systems.indexOf(prevSystem),
                                1 + systems.indexOf(nextSystem)));
                    }
                }
            }
        } else if (prevBar != null) {
            // case: Bar - noBar
            Point2D prevPoint = null;

            for (Glyph prevStick : prevBar.getSticksAncestors()) {
                Point2D point = skew.deskewed(prevStick.getStopPoint(VERTICAL));

                if ((prevPoint == null) || (prevPoint.getY() < point.getY())) {
                    prevPoint = point;
                }
            }

            if ((prevPoint.getY() - prevY) > params.minBarChunkHeight) {
                double dy = nextY - prevPoint.getY();

                if (dy <= params.maxBarCoordGap) {
                    return tryRangeConnection(systems.subList(systems.indexOf(
                            prevSystem),
                            1
                            + systems.indexOf(nextSystem)));
                }
            }
        } else if (nextBar != null) {
            // case: NoBar - Bar
            Point2D nextPoint = null;

            for (Glyph nextStick : nextBar.getSticksAncestors()) {
                Point2D point = skew.deskewed(nextStick.getStartPoint(VERTICAL));

                if ((nextPoint == null) || (nextPoint.getY() > point.getY())) {
                    nextPoint = point;
                }
            }

            if ((nextY - nextPoint.getY()) > params.minBarChunkHeight) {
                double dy = nextPoint.getY() - prevY;

                if (dy <= params.maxBarCoordGap) {
                    return tryRangeConnection(systems.subList(systems.indexOf(
                            prevSystem),
                            1
                            + systems.indexOf(nextSystem)));
                }
            }
        }

        return false;
    }

    //--------------------//
    // checkBarsAlignment //
    //--------------------//
    /**
     * Check that, within the same system, barSticks are vertically
     * aligned across all staves.
     * We first build BarAlignment instances to record the bar locations and
     * finally check these alignments for correctness.
     * Resulting alignments are stored in the SystemInfo instance.
     *
     * @param system the system to process
     */
    private void checkBarsAlignment (SystemInfo system)
    {
        List<StaffInfo> staves = system.getStaves();
        int staffCount = staves.size();

        // Bar alignments for the system
        List<BarAlignment> alignments = null;

        for (int iStaff = 0; iStaff < staves.size(); iStaff++) {
            StaffInfo staff = staves.get(iStaff);

            IntersectionSequence staffCrossings = crossings.get(staff);

            //            logger.info("System#" + system.getId() +" Staff#" + staff.getId());
            //            for (StickIntersection inter : staffBars) {
            //                logger.info(inter.toString());
            //            }
            //
            if (alignments == null) {
                // Initialize the alignments
                alignments = new ArrayList<>();
                system.setBarAlignments(alignments);

                for (StickIntersection crossing : staffCrossings) {
                    BarAlignment align = new BarAlignment(sheet, staffCount);
                    align.addInter(iStaff, crossing);
                    alignments.add(align);
                }
            } else {
                // Do we have a bar around each abscissa?
                for (StickIntersection loc : staffCrossings) {
                    // Find closest alignment
                    Double[] dists = new Double[alignments.size()];
                    Integer bestIdx = null;

                    for (int ia = 0; ia < alignments.size(); ia++) {
                        BarAlignment align = alignments.get(ia);
                        Double dist = align.distance(iStaff, loc);

                        if (dist != null) {
                            dists[ia] = dist;
                            dist = Math.abs(dist);

                            if (bestIdx != null) {
                                if (Math.abs(dists[bestIdx]) > dist) {
                                    dists[bestIdx] = dist;
                                    bestIdx = ia;
                                }
                            } else {
                                bestIdx = ia;
                            }
                        }
                    }

                    if ((bestIdx != null)
                        && (Math.abs(dists[bestIdx])
                            <= params.maxAlignmentDistance)) {
                        alignments.get(bestIdx)
                                .addInter(iStaff, loc);
                    } else {
                        // Insert a new alignment at proper index
                        BarAlignment align = new BarAlignment(sheet,
                                staffCount);
                        align.addInter(iStaff, loc);

                        if (bestIdx == null) {
                            alignments.add(align);
                        } else {
                            if (dists[bestIdx] < 0) {
                                alignments.add(bestIdx, align);
                            } else {
                                alignments.add(bestIdx + 1, align);
                            }
                        }
                    }
                }
            }
        }

        // Check the bar alignments
        for (Iterator<BarAlignment> it = alignments.iterator(); it.hasNext();) {
            BarAlignment align = it.next();

            // Don't call manual alignments into question
            if (align.isManual()) {
                continue;
            }

            // If alignment is almost empty, remove it
            // otherwise, try to fill the holes
            int filled = align.getFilledCount();

            //            double ratio = (double) filled / staffCount;
            ////            if (ratio < constants.minAlignmentRatio.getValue()) {
            //                // We remove this alignment and deassign its sticks
            //                logger.debug("{}Removing {}", sheet.getLogPrefix(), align);
            //                it.remove();
            //
            //                for (StickIntersection inter : align.getIntersections()) {
            //                    if (inter != null) {
            //                        inter.getStickAncestor().setShape(null);
            //                    }
            //                }
            //            } else if (filled != staffCount) {
            //                // TODO: Should implement driven recognition here...
            //                logger.info("{}Should fill {}", sheet.getLogPrefix(), align);
            //            }

            // Strict: we require all staves to have a barline in this alignment
            if (filled < staffCount) {
                // We remove this alignment and deassign its sticks
                logger.debug("{}Removing {}", sheet.getLogPrefix(), align);
                it.remove();

                for (StickIntersection inter : align.getIntersections()) {
                    if (inter != null) {
                        inter.getStickAncestor()
                                .setShape(null);
                    }
                }
            }
        }
    }

    //---------------//
    // createSystems //
    //---------------//
    /**
     * Build the frame of each system.
     *
     * @param tops the starting staff id for each system
     * @return the sequence of system physical frames
     */
    private List<SystemInfo> createSystems (Integer[] tops)
    {
        List<SystemInfo> newSystems = new ArrayList<>();
        Integer staffTop = null;
        int systemId = 0;
        SystemInfo system = null;

        for (int i = 0; i < staffManager.getStaffCount(); i++) {
            StaffInfo staff = staffManager.getStaff(i);

            // System break?
            if ((staffTop == null) || (staffTop < tops[i])) {
                // Start of a new system
                staffTop = tops[i];

                system = new SystemInfo(++systemId, sheet,
                        staffManager.getRange(staff, staff));
                newSystems.add(system);
            } else {
                // Continuing current system
                system.setStaves(staffManager.getRange(system.getFirstStaff(),
                        staff));
            }
        }

        return newSystems;
    }

    //---------------------//
    // extendStaffAbscissa //
    //---------------------//
    /**
     * Determine a not-too-bad abscissa for staff end, extending the
     * abscissa beyond the bar limit if staff lines so require.
     *
     * @param staff the staff to process
     * @param side  the desired side
     * @return the staff abscissa
     */
    private double extendStaffAbscissa (StaffInfo staff,
                                        HorizontalSide side)
    {
        // Check that staff bar, if any, is not passed by lines
        BarInfo bar = staff.getBar(side);

        if (bar != null) {
            Glyph drivingStick = null;
            double linesX = staff.getLinesEnd(side);

            // Pick up the heaviest bar stick
            SortedSet<Glyph> allSticks = new TreeSet<>(
                    Glyph.byReverseWeight);

            // Use long sticks first
            for (Glyph stick : bar.getSticksAncestors()) {
                if (isLong(stick)) {
                    allSticks.add(stick);
                }
            }

            // use local sticks if needed
            if (allSticks.isEmpty()) {
                allSticks.addAll(bar.getSticksAncestors());
            }

            if (!allSticks.isEmpty()) {
                drivingStick = allSticks.first();

                // Extend approximate limit abscissa?
                Point2D inter = staff.intersection(drivingStick);
                double barX = inter.getX();
                final int dir = (side == LEFT) ? 1 : (-1);

                if ((dir * (barX - linesX)) > params.maxLineExtension) {
                    staff.setBar(side, null);
                    staff.setAbscissa(side, linesX);
                    logger.debug("{} extended {}", side, staff);
                }
            }
        }

        return staff.getAbscissa(side);
    }

    //---------------//
    // getLineEnding //
    //---------------//
    /**
     * Report the precise point where a given line should end.
     *
     * @param system the system to process
     * @param staff  containing staff
     * @param line   the line at hand
     * @param side   the desired ending
     * @return the computed ending point
     *
     * @throws RuntimeException DOCUMENT ME!
     */
    private Point2D getLineEnding (SystemInfo system,
                                   StaffInfo staff,
                                   LineInfo line,
                                   HorizontalSide side)
    {
        double slope = staff.getEndingSlope(side);
        Object limit = system.getLimit(side);
        Point2D linePt = line.getEndPoint(side);
        double staffX = staff.getAbscissa(side);
        double y = linePt.getY() - ((linePt.getX() - staffX) * slope);
        double x;

        // Dirty programming, sorry
        if (limit instanceof Glyph) {
            x = ((Glyph) limit).getPositionAt(y, VERTICAL);
        } else if (limit instanceof Line) {
            x = ((Line) limit).xAtY(y);
        } else {
            throw new RuntimeException("Illegal system limit: " + limit);
        }

        return new Point2D.Double(x, y);
    }

    //--------------//
    // mergeSystems //
    //--------------//
    private boolean mergeSystems ()
    {
        List<SystemInfo> systems = sheet.getSystems();
        boolean modified = false;

        // Check connection of left barSticks across systems
        for (int i = 1; i < systems.size(); i++) {
            if (canConnectSystems(systems.get(i - 1), systems.get(i))) {
                modified = true;
            }
        }

        // More touchy decisions here
        //        if (!modified) {
        //            if (constants.smartStavesConnections.getValue()) {
        //                return trySmartConnections();
        //            }
        //        } else {
        systemTops = null; // To force recomputation

        //        }

        return modified;
    }

    //-------------------//
    // populateCrossings //
    //-------------------//
    /**
     * Retrieve the sticks that intersect each staff, the results being
     * kept as sequences of staff intersections in crossings structure.
     */
    private void populateCrossings ()
    {
        // Global reset
        crossings.clear();

        for (Glyph stick : filaments) {
            // Skip merged sticks
            if (stick.getPartOf() != null) {
                continue;
            }

            Point2D start = stick.getStartPoint(VERTICAL);
            StaffInfo topStaff = staffManager.getStaffAt(start);
            int top = topStaff.getId();

            Point2D stop = stick.getStopPoint(VERTICAL);
            StaffInfo botStaff = staffManager.getStaffAt(stop);
            int bot = botStaff.getId();

            if (logger.isDebugEnabled() || stick.isVip()) {
                logger.info("Bar#{} top:{} bot:{}", stick.getId(), top, bot);
            }

            for (int id = top; id <= bot; id++) {
                StaffInfo staff = staffManager.getStaff(id - 1);
                IntersectionSequence staffCrossings = crossings.get(staff);

                if (staffCrossings == null) {
                    staffCrossings = new IntersectionSequence(
                            StickIntersection.byAbscissa);
                    crossings.put(staff, staffCrossings);
                }

                staffCrossings.add(new StickIntersection(
                        staff.intersection(stick), stick));
            }
        }
    }

    //---------------------//
    // preciseIntersection //
    //---------------------//
    /**
     * Compute the precise point where the vertical bar stick intersects
     * the horizontal (staff) line.
     *
     * @param stick the vertical (bar) stick
     * @param line  the horizontal (staff) line
     * @return the precise intersection point
     */
    private Point2D preciseIntersection (Glyph stick,
                                         LineInfo line)
    {
        Point2D startPoint = stick.getStartPoint(VERTICAL);
        Point2D stopPoint = stick.getStopPoint(VERTICAL);

        // First, get a rough intersection
        Point2D pt = LineUtil.intersection(line.getEndPoint(LEFT),
                line.getEndPoint(RIGHT),
                startPoint, stopPoint);

        // Second, get a precise ordinate
        double y = line.yAt(pt.getX());

        // Third, get a precise abscissa
        double x;

        if (y < startPoint.getY()) { // Above stick
            double invSlope = stick.getInvertedSlope();
            x = startPoint.getX() + ((y - startPoint.getY()) * invSlope);
        } else if (y > stopPoint.getY()) { // Below stick
            double invSlope = stick.getInvertedSlope();
            x = stopPoint.getX() + ((y - stopPoint.getY()) * invSlope);
        } else { // Within stick height
            x = stick.getLine().xAtY(y);
        }

        return new Point2D.Double(x, y);
    }

    //----------------------//
    // recheckBarCandidates //
    //----------------------//
    /**
     * Have a closer look at so-called barSticks, now that the grid of
     * staves has been fully defined, to get rid of spurious barline
     * sticks.
     */
    private void recheckBarCandidates ()
    {
        // Do not check barSticks already involved in side barlines
        Set<Glyph> sideBars = getSideBarlineGlyphs();
        filaments.removeAll(sideBars);

        // Check the others, now using more strict checks
        sheet.setBarsChecker(new BarsChecker(sheet, false));
        sheet.getBarsChecker().checkCandidates(filaments);

        // Purge barSticks collection
        for (Iterator<Glyph> it = filaments.iterator(); it.hasNext();) {
            Glyph stick = it.next();

            if (!stick.isBar()) {
                it.remove();
            }
        }

        filaments.addAll(sideBars);

        // Purge crossings accordingly
        for (IntersectionSequence seq : crossings.values()) {
            for (Iterator<StickIntersection> it = seq.iterator();
                    it.hasNext();) {
                StickIntersection crossing = it.next();

                if (!crossing.getStickAncestor().isBar()) {
                    ///logger.info("Purging " + stickPos);
                    it.remove();
                }
            }
        }
    }

    //-------------------//
    // refineBarsEndings //
    //-------------------//
    /**
     * If we have reliable bar lines, refine their ending points to the
     * staff lines.
     *
     * @param system the system to process
     */
    private void refineBarsEndings (SystemInfo system)
    {
        if (system.getBarAlignments() == null) {
            return;
        }

        List<StaffInfo> staves = system.getStaves();

        for (BarAlignment alignment : system.getBarAlignments()) {
            int iStaff = -1;

            for (StickIntersection crossing : alignment.getIntersections()) {
                iStaff++;

                if (crossing != null) {
                    StaffInfo staff = staves.get(iStaff);
                    Glyph stick = crossing.getStickAncestor();

                    // Left bar items have already been adjusted
                    BarInfo leftBar = staff.getBar(LEFT);

                    if ((leftBar != null)
                        && leftBar.getSticksAncestors().contains(stick)) {
                        continue;
                    }

                    // Perform adjustment only when on bottom staff
                    Point2D stop = stick.getStopPoint(VERTICAL);
                    StaffInfo botStaff = staffManager.getStaffAt(stop);

                    if (botStaff == staff) {
                        Point2D start = stick.getStartPoint(VERTICAL);
                        StaffInfo topStaff = staffManager.getStaffAt(start);
                        stick.setEndingPoints(
                                preciseIntersection(stick,
                                topStaff.getFirstLine()),
                                preciseIntersection(stick,
                                botStaff.getLastLine()));
                    }
                }
            }
        }
    }

    //-----------------------//
    // retrieveBarCandidates //
    //-----------------------//
    /**
     * Retrieve initial barline candidates.
     *
     * @throws Exception
     */
    private void retrieveBarCandidates ()
            throws Exception
    {
        BarsChecker barsChecker = new BarsChecker(sheet, true); // Rough
        barsChecker.checkCandidates(filaments);

        // Consider only sticks with a barline shape
        for (Iterator<Glyph> it = filaments.iterator(); it.hasNext();) {
            Glyph glyph = it.next();
            if (glyph.getPartOf() != null || !glyph.isBar()) {
                it.remove();
            }
        }
    }

    //-------------------//
    // retrieveMajorBars //
    //-------------------//
    private void retrieveMajorBars (Collection<Glyph> oldGlyphs,
                                    Collection<Glyph> newGlyphs)
            throws Exception
    {
        // Build vertical filaments
        if (oldGlyphs.isEmpty() && newGlyphs.isEmpty()) {
            // Build filaments from scratch
            buildVerticalFilaments();
        } else {
            // Apply modifications to filaments

            // Removal
            filaments.removeAll(oldGlyphs);

            // Addition
            filaments.addAll(newGlyphs);

            // Purge non-active glyphs
            for (Iterator<Glyph> it = filaments.iterator(); it.hasNext();) {
                Glyph glyph = it.next();
                if (!glyph.isActive()) {
                    logger.debug("Purging non-active {}", glyph);
                    it.remove();
                }
            }
        }

        // Retrieve rough barline candidates
        retrieveBarCandidates();

        // Assign bar candidates to intersected staves
        populateCrossings();

        // Retrieve left staff bars (they define systems) and right staff bars
        retrieveStaffSideBars();
    }

    //------------------//
    // retrievePartTops //
    //------------------//
    /**
     * Retrieve, for each staff, the staff that begins its containing
     * part.
     *
     * @return the (id of) part-starting staff for each staff
     */
    private Integer[] retrievePartTops ()
    {
        staffManager.setPartTops(partTops =
                new Integer[staffManager.getStaffCount()]);

        for (StaffInfo staff : staffManager.getStaves()) {
            ///logger.info("Staff#" + staff.getId());
            int bot = staff.getId();
            BarInfo leftBar = staff.getBar(LEFT);
            BarInfo rightBar = staff.getBar(RIGHT);
            double staffLeft = staff.getAbscissa(LEFT);
            IntersectionSequence staffCrossings = crossings.get(staff);

            for (StickIntersection stickCrossing : staffCrossings) {
                Glyph stick = stickCrossing.getStickAncestor();

                if (!stick.isBar()) {
                    continue;
                }

                // Do not use left bar items (they define systems, not parts)
                if ((leftBar != null)
                    && leftBar.getSticksAncestors().contains(stick)) {
                    ///logger.info("Skipping left side  stick#" + stick.getId());
                    continue;
                }

                if (stickCrossing.x < staffLeft) {
                    // logger.info(
                    //   "Too left " + stickPos.x + " vs " + staffLeft +
                    //   " stick#" + stick.getId());
                    continue;
                }

                // Use right bar, if any, even if not anchored ...
                // Or use a plain bar stick provided it is anchored on both sides
                if ((rightBar != null && rightBar.getSticksAncestors().contains(
                        stick)) || (stick.getResult() == BarsChecker.BAR_PART_DEFINING)) {
                    Point2D start = stick.getStartPoint(VERTICAL);
                    StaffInfo topStaff = staffManager.getStaffAt(start);
                    int top = topStaff.getId();

                    for (int id = top; id <= bot; id++) {
                        if ((partTops[id - 1] == null) || (top < partTops[id - 1])) {
                            partTops[id - 1] = top;
                        }
                    }
                }
            }
        }

        return partTops;
    }

    //------------------//
    // retrieveStaffBar //
    //------------------//
    /**
     * Retrieve the (perhaps multi-bar) complete side bar, if any, of a
     * given staff and record it within the staff.
     *
     * @param staff the given staff
     * @param side  proper horizontal side
     */
    private void retrieveStaffBar (StaffInfo staff,
                                   HorizontalSide side)
    {
        final IntersectionSequence staffCrossings = crossings.get(staff);
        final int dir = (side == LEFT) ? 1 : (-1);
        final double staffX = staff.getAbscissa(side);
        final double xBreak = staffX + (dir * params.maxDistanceFromStaffSide);
        final IntersectionSequence seq = new IntersectionSequence(
                StickIntersection.byAbscissa);
        BarInfo bar = null;
        staff.setBar(side, null); // Reset

        // Give first importance to long (inter-staff) sticks
        for (boolean takeAllSticks : new boolean[]{false, true}) {
            // Browse bar sticks from outside to inside of staff
            for (StickIntersection crossing
                    : (dir > 0) ? staffCrossings : staffCrossings.descendingSet()) {
                double x = crossing.x;

                if ((dir * (xBreak - x)) < 0) {
                    break; // Speed up
                }

                if (takeAllSticks || isLong(crossing.getStickAncestor())) {
                    if (seq.isEmpty()) {
                        seq.add(crossing);
                    } else {
                        // Perhaps a pack of bars, check total width
                        double width = Math.max(Math.abs(x - seq.first().x),
                                Math.abs(x - seq.last().x));

                        if (((side == LEFT)
                             && (width <= params.maxLeftBarPackWidth))
                            || ((side == RIGHT)
                                && (width <= params.maxRightBarPackWidth))) {
                            seq.add(crossing);
                        }
                    }
                }
            }
        }

        if (!seq.isEmpty()) {
            seq.reduce(params.maxPosGap); // Merge sticks vertically

            double barX = seq.last().x;

            if ((dir * (barX - staffX)) <= params.maxDistanceFromStaffSide) {
                bar = new BarInfo(seq.getSticks());
                staff.setBar(side, bar);
                staff.setAbscissa(side, barX);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.info("Staff#{} {} discarded stick#{}",
                            staff.getId(), side,
                            seq.last().getStickAncestor().getId());
                }
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Staff#{} no {} bar {}", staff.getId(), side,
                        Glyphs.toString(StickIntersection.sticksOf(
                        staffCrossings)));
            }
        }

        logger.debug("Staff#{} {} bar: {}", staff.getId(), side, bar);
    }

    //-----------------------//
    // retrieveStaffSideBars //
    //-----------------------//
    /**
     * Retrieve staffs left bar (which define systems) and right bar.
     */
    private void retrieveStaffSideBars ()
    {
        for (HorizontalSide side : HorizontalSide.values()) {
            for (StaffInfo staff : staffManager.getStaves()) {
                retrieveStaffBar(staff, side);
            }
        }
    }

    //-------------------//
    // retrieveSystemBar //
    //-------------------//
    /**
     * Merge bar sticks across staves of the same system, as long as
     * they can be connected even indirectly via other sticks.
     *
     * @param system the system to process
     * @param side   the side to process
     *
     * @return the bar info for the system side, or null if no consistency could
     *         be ensured
     */
    private BarInfo retrieveSystemBar (SystemInfo system,
                                       HorizontalSide side)
    {
        // Horizontal sequence of bar sticks applied to the whole system side
        Glyph[] seq = null;

        for (StaffInfo staff : system.getStaves()) {
            BarInfo bar = staff.getBar(side);

            if (bar == null) {
                return null; // We cannot ensure consistency
            }

            List<Glyph> barSticks = bar.getSticksAncestors();
            Integer delta = null; // Delta on stick indices between 2 staves
            int ib = 0; // Index on sticks

            if (seq == null) {
                seq = barSticks.toArray(new Glyph[barSticks.size()]);
            } else {
                // Loop on bar sticks
                BarLoop:
                for (ib = 0; ib < barSticks.size(); ib++) {
                    Glyph barFil = barSticks.get(ib);

                    // Check with sequence
                    for (int is = 0; is < seq.length; is++) {
                        if (seq[is].getAncestor() == barFil) {
                            // We have a pivot stick!
                            delta = is - ib;

                            break BarLoop;
                        }
                    }
                }

                if (delta != null) {
                    // Update sequence accordingly
                    int isMin = Math.min(0, delta);
                    int isBrk = Math.max(seq.length, barSticks.size() + delta);

                    if ((isMin < 0) || (isBrk > seq.length)) {
                        // Allocate new sequence (shifted and/or enlarged)
                        Glyph[] newSeq = new Glyph[isBrk - isMin];
                        System.arraycopy(seq, 0, newSeq, -isMin, seq.length);
                        seq = newSeq;
                    }

                    for (ib = 0; ib < barSticks.size(); ib++) {
                        Glyph barStick = barSticks.get(ib);
                        int is = (ib + delta) - isMin;
                        Glyph seqStick = seq[is];

                        if (seqStick != null) {
                            seqStick = seqStick.getAncestor();

                            if (seqStick != barStick) {
                                logger.debug("Including F{} to F{}",
                                        barStick.getId(), seqStick.getId());
                                seqStick.stealSections(barStick);
                            }
                        } else {
                            seq[is] = barStick;
                        }
                    }
                } else {
                    return null;
                }
            }
        }

        BarInfo systemBar = new BarInfo(seq);
        system.setBar(side, systemBar);

        return systemBar;
    }

    //--------------------//
    // retrieveSystemTops //
    //--------------------//
    /**
     * Retrieve for each staff the staff that begins its containing
     * system.
     *
     * @return the (id of) system-starting staff for each staff
     */
    private Integer[] retrieveSystemTops ()
    {
        staffManager.setSystemTops(systemTops =
                new Integer[staffManager.getStaffCount()]);

        for (StaffInfo staff : staffManager.getStaves()) {
            int bot = staff.getId();
            BarInfo bar = staff.getBar(LEFT);

            if (bar != null) {
                // We have a starting bar line
                for (Glyph stick : bar.getSticksAncestors()) {
                    Point2D start = stick.getStartPoint(VERTICAL);
                    StaffInfo topStaff = staffManager.getStaffAt(start);
                    int top = topStaff.getId();

                    for (int id = top; id <= bot; id++) {
                        if ((systemTops[id - 1] == null)
                            || (top < systemTops[id - 1])) {
                            systemTops[id - 1] = top;
                        }
                    }
                }
            } else {
                // We have no starting bar line, so staff = part = system
                systemTops[bot - 1] = bot;
            }
        }

        return systemTops;
    }

    //--------------------//
    // tryRangeConnection //
    //--------------------//
    /**
     * Try to connect all systems in the provided range.
     *
     * @param range sublist of systems
     *
     * @return true if OK
     */
    private boolean tryRangeConnection (List<SystemInfo> range)
    {
        final SystemInfo firstSystem = range.get(0);
        final StaffInfo firstStaff = firstSystem.getFirstStaff();
        final int topId = firstStaff.getId();
        int idx = staffManager.getStaves()
                .indexOf(firstStaff);

        for (SystemInfo system : range) {
            for (StaffInfo staff : system.getStaves()) {
                systemTops[idx++] = topId;
            }
        }

        logger.info("Staves connection from {} to {}", topId,
                range.get(range.size() - 1).getLastStaff().getId());

        return true;
    }

    //~ Inner Classes ----------------------------------------------------------
    //    //---------------------//
    //    // trySmartConnections //
    //    //---------------------//
    //    /**
    //     * Method to try system connections not based on local bar connections
    //     * @return true if connection decided
    //     */
    //    private boolean trySmartConnections ()
    //    {
    //        // Valuable hints:
    //        // - Significant differences in systems lengths
    //        // - System w/o left bar, while others have some
    //        // - Bar that significantly departs from a staff
    //
    //        // Systems lengths
    //        List<Integer> lengths = new ArrayList<Integer>(systems.size());
    //
    //        // Extrema
    //        int smallestLength = Integer.MAX_VALUE;
    //        int largestLength = Integer.MIN_VALUE;
    //
    //        for (SystemInfo system : systems) {
    //            int length = system.getStaves()
    //                               .size();
    //            lengths.add(length);
    //            smallestLength = Math.min(smallestLength, length);
    //            largestLength = Math.max(largestLength, length);
    //        }
    //
    //        // If all systems are equal in length, there is nothing to try
    //        if (smallestLength == largestLength) {
    //            return false;
    //        }
    //
    //        if ((2 * largestLength) == staffManager.getStaffCount()) {
    //            // Check that we can add up to largest size
    //            if (lengths.get(0) == largestLength) {
    //                return tryRangeConnection(systems.subList(1, lengths.size()));
    //            } else if (lengths.get(lengths.size() - 1) == largestLength) {
    //                return tryRangeConnection(
    //                    systems.subList(0, lengths.size() - 1));
    //            }
    //        } else if ((3 * largestLength) == staffManager.getStaffCount()) {
    //            // Check that we can add up to largest size
    //            // TBD
    //        }
    //
    //        return false;
    //    }
    //-----------//
    // Constants //
    //-----------//
    /**
     * TODO: This collection of parameters is way too long! It should be
     * carefully reduced!!!
     */
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Ratio maxLengthRatio = new Constant.Ratio(
                1.4,
                "Maximum ratio in length for a run to be combined with an existing section");

        // Constants specified WRT mean interline
        // --------------------------------------
        Scale.Fraction maxSectionThickness = new Scale.Fraction(
                0.8,
                "Maximum horizontal section thickness WRT interline");

        Scale.Fraction maxFilamentThickness = new Scale.Fraction(
                0.8,
                "Maximum horizontal filament thickness WRT interline");

        Scale.Fraction maxOverlapDeltaPos = new Scale.Fraction(
                0.4,
                "Maximum delta position between two overlapping filaments");

        Scale.Fraction maxCoordGap = new Scale.Fraction(
                0.5,
                "Maximum delta coordinate for a gap between filaments");

        Scale.Fraction maxPosGap = new Scale.Fraction(
                0.2,
                "Maximum delta abscissa for a gap between filaments");

        Scale.Fraction maxSpace = new Scale.Fraction(
                0.1,
                "Maximum space between overlapping bar filaments");

        Scale.Fraction maxBarCoordGap = new Scale.Fraction(
                2,
                "Maximum delta coordinate for a vertical gap between bars");

        Scale.Fraction maxBarPosGap = new Scale.Fraction(
                0.3,
                "Maximum delta position for a vertical gap between bars");

        Scale.Fraction minRunLength = new Scale.Fraction(
                1.5,
                "Minimum length for a vertical run to be considered");

        Scale.Fraction minLongLength = new Scale.Fraction(
                8,
                "Minimum length for a long vertical bar");

        Scale.Fraction maxDistanceFromStaffSide = new Scale.Fraction(
                2,
                "Max abscissa delta when looking for left or right side bars");

        Scale.Fraction maxLeftBarPackWidth = new Scale.Fraction(
                1.5,
                "Max width of a pack of vertical barlines");

        Scale.Fraction maxRightBarPackWidth = new Scale.Fraction(
                1,
                "Max width of a pack of vertical barlines");

        Scale.Fraction maxSideDx = new Scale.Fraction(
                .5,
                "Max difference on theoretical bar abscissa");

        Scale.Fraction maxLineExtension = new Scale.Fraction(
                .5,
                "Max extension of line beyond staff bar");

        Scale.Fraction minBarChunkHeight = new Scale.Fraction(
                1,
                "Min height of a bar chunk past system boundaries");

        Scale.Fraction maxAlignmentDistance = new Scale.Fraction(
                0.5,
                "Max horizontal shift between aligned bars");

        Constant.Ratio minAlignmentRatio = new Constant.Ratio(
                0.5,
                "Minimum percentage of mapped staves in a bar alignment ");

        // Constants for display
        //
        Constant.Boolean showVerticalLines = new Constant.Boolean(
                false,
                "Should we display the vertical lines?");

        Constant.Boolean showTangents = new Constant.Boolean(
                false,
                "Should we show filament ending tangents?");

        Constant.Double splineThickness = new Constant.Double(
                "thickness", 0.5,
                "Stroke thickness to draw filaments curves");

        Constant.Boolean smartStavesConnections = new Constant.Boolean(
                true,
                "(beta) Should we try smart staves connections into systems?");

        // Constants for debugging
        //
        Constant.String verticalVipSections = new Constant.String(
                "",
                "(Debug) Comma-separated list of VIP sections");

    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all constants related to vertical
     * frames.
     */
    private static class Parameters
    {
        //~ Static fields/initializers -----------------------------------------

        /** Usual logger utility. */
        private static final Logger logger = LoggerFactory.getLogger(Parameters.class);

        //~ Instance fields ----------------------------------------------------
        /** Maximum delta abscissa for a gap between filaments. */
        final int maxPosGap;

        /** Minimum run length for vertical lag. */
        final int minRunLength;

        /** Used for section junction policy. */
        final double maxLengthRatio;

        /** Minimum for long vertical stick bars. */
        final int minLongLength;

        /** Maximum distance between a bar and the staff side. */
        final int maxDistanceFromStaffSide;

        /** Maximum width for a pack of bars on left side. */
        final int maxLeftBarPackWidth;

        /** Maximum width for a pack of bars on right side. */
        final int maxRightBarPackWidth;

        /** Max difference on theoretical bar abscissa. */
        final int maxSideDx;

        /** Max extension of line beyond staff bar. */
        final int maxLineExtension;

        /** Min height to detect a bar going past a staff. */
        final int minBarChunkHeight;

        /** Maximum abscissa shift for bar alignments. */
        final double maxAlignmentDistance;

        /** Maximum delta position for a vertical gap between bars. */
        final int maxBarPosGap;

        /** Maximum delta coordinate for a vertical gap between bars. */
        final int maxBarCoordGap;

        // Debug
        final List<Integer> vipSections;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            maxPosGap = scale.toPixels(constants.maxPosGap);
            minRunLength = scale.toPixels(constants.minRunLength);
            maxLengthRatio = constants.maxLengthRatio.getValue();
            minLongLength = scale.toPixels(constants.minLongLength);
            maxDistanceFromStaffSide = scale.toPixels(
                    constants.maxDistanceFromStaffSide);
            maxLeftBarPackWidth = scale.toPixels(constants.maxLeftBarPackWidth);
            maxRightBarPackWidth = scale.
                    toPixels(constants.maxRightBarPackWidth);
            maxSideDx = scale.toPixels(constants.maxSideDx);
            maxLineExtension = scale.toPixels(constants.maxLineExtension);
            minBarChunkHeight = scale.toPixels(constants.minBarChunkHeight);
            maxAlignmentDistance = scale.
                    toPixels(constants.maxAlignmentDistance);
            maxBarPosGap = scale.toPixels(constants.maxBarPosGap);
            maxBarCoordGap = scale.toPixels(constants.maxBarCoordGap);

            // VIPs
            vipSections = VipUtil.decodeIds(
                    constants.verticalVipSections.getValue());

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }

            if (!vipSections.isEmpty()) {
                logger.info("Vertical VIP sections: {}", vipSections);
            }
        }
    }
}
