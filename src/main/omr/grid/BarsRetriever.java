//----------------------------------------------------------------------------//
//                                                                            //
//                         B a r s R e t r i e v e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
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

import omr.log.Logger;

import omr.math.Barycenter;
import omr.math.BasicLine;
import omr.math.Line;
import omr.math.LineUtilities;
import static omr.run.Orientation.*;
import omr.run.RunsTable;

import omr.sheet.BarsChecker;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;
import omr.sheet.SystemInfo;

import omr.step.StepException;

import omr.ui.Colors;
import omr.ui.util.UIUtilities;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * Class {@code BarsRetriever} focuses on the retrieval of vertical
 * bar lines.
 * Bar lines are used to determine the side limits of staves and, most
 * importantly, the gathering of staves into systems.
 * The other bar lines are used to determine parts and measures.
 *
 * @author Hervé Bitteur
 */
public class BarsRetriever
        implements NestView.ItemRenderer
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BarsRetriever.class);

    /** Stroke for drawing filaments curves */
    private static final Stroke splineStroke = new BasicStroke(
            (float) constants.splineThickness.getValue(),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND);

    //~ Instance fields --------------------------------------------------------
    /** Related sheet */
    private final Sheet sheet;

    /** Related scale */
    private final Scale scale;

    /** Scale-dependent constants for vertical stuff */
    private final Parameters params;

    /** Lag of vertical runs */
    private Lag vLag;

    /** Long filaments found, non sorted */
    private final List<Glyph> filaments = new ArrayList<>();

    /** Related staff manager */
    private final StaffManager staffManager;

    /** Related system manager */
    private final SystemManager systemManager;

    /** Candidate bar sticks */
    private List<Glyph> bars = new ArrayList<>();

    /** Sequence of all candidate bar sticks that intersect a staff */
    private final Map<StaffInfo, IntersectionSequence> barSticks = new HashMap<>();

    /** System tops */
    private Integer[] systemTops;

    /** Part tops */
    private Integer[] partTops;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // BarsRetriever //
    //---------------//
    /**
     * Retrieve the frames of all staff lines
     *
     * @param sheet the sheet to process
     */
    public BarsRetriever (Sheet sheet)
    {
        this.sheet = sheet;

        scale = sheet.getScale();
        params = new Parameters(scale);
        staffManager = sheet.getStaffManager();
        systemManager = sheet.getSystemManager();
    }

    //~ Methods ----------------------------------------------------------------
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
     * Render the filaments and their ending tangents.
     *
     * @param g graphics context
     */
    @Override
    public void renderItems (Graphics2D g)
    {
        if (!constants.showVerticalLines.isSet()) {
            return;
        }

        final Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1f);
        final Color oldColor = g.getColor();
        g.setColor(Colors.ENTITY_MINOR);

        // Draw filaments
        for (Glyph filament : filaments) {
            filament.renderLine(g);
        }

        // Draw tangent at each ending point (using max coord gap)?
        if (constants.showTangents.isSet()) {
            g.setColor(Colors.TANGENT);

            double dy = sheet.getScale().toPixels(constants.maxCoordGap);

            for (Glyph glyph : filaments) {
                Point2D p = glyph.getStartPoint(VERTICAL);
                double derivative = (glyph instanceof Filament)
                                    ? ((Filament) glyph).slopeAt(
                        p.getY(),
                        VERTICAL) : glyph.getInvertedSlope();
                g.draw(
                        new Line2D.Double(
                        p.getX(),
                        p.getY(),
                        p.getX() - (derivative * dy),
                        p.getY() - dy));
                p = glyph.getStopPoint(VERTICAL);
                derivative = (glyph instanceof Filament)
                             ? ((Filament) glyph).slopeAt(p.getY(), VERTICAL)
                             : glyph.getInvertedSlope();
                g.draw(
                        new Line2D.Double(
                        p.getX(),
                        p.getY(),
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
     * Then, we use consistency checks within the same system, where measure bar
     * lines should be aligned vertically.
     */
    public void retrieveMeasureBars ()
    {
        // Strict bars checking
        recheckBarCandidates();

        // Check bars consistency within the same system
        for (SystemInfo system : systemManager.getSystems()) {
            checkBarsAlignment(system);
        }

        // We already have systemStart for each staff
        // let's try to define partStart for each staff
        partTops = retrievePartTops();

        logger.info("{0}Parts   top staff ids: {1}",
                    new Object[]{sheet.getLogPrefix(), Arrays.toString(partTops)});

        // Refine ending points for each bar line
        for (SystemInfo system : systemManager.getSystems()) {
            refineBarsEndings(system);
        }
    }

    //--------------------//
    // retrieveSystemBars //
    //--------------------//
    /**
     * Use the long vertical sections to retrieve the barlines that limit
     * systems and staves.
     *
     * <pre>
     * retrieveSystemBars()
     *    +  retrieveMajorBars()
     *       +  buildVerticalFilaments()       // Build vertical filaments
     *       +  retrieveBarCandidates()        // Retrieve initial barline candidates
     *       +  populateStaffSticks()          // Assign bar candidates to intersected staves
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
     */
    public void retrieveSystemBars ()
            throws StepException
    {
        try {
            // Retrieve major bars (for system & staves limits)
            retrieveMajorBars();
        } catch (Exception ex) {
            logger.warning(
                    sheet.getLogPrefix() + "BarsRetriever cannot retrieveBars",
                    ex);
        }

        try {
            // Detect systems of staves aggregated via barlines
            buildSystems();
        } catch (Exception ex) {
            logger.warning(
                    sheet.getLogPrefix() + "BarsRetriever cannot retrieveSystems",
                    ex);
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
            logger.fine("{0}", staff);

            // Adjust left and right endings of each line in the staff
            for (LineInfo l : staff.getLines()) {
                FilamentLine line = (FilamentLine) l;
                line.setEndingPoints(
                        getLineEnding(system, staff, line, LEFT),
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
     * Adjust start and stop points of system side bars.
     */
    void adjustSystemBars ()
    {
        for (SystemInfo system : systemManager.getSystems()) {
            try {
                for (HorizontalSide side : HorizontalSide.values()) {
                    Object limit = system.getLimit(side);

                    if (limit != null) {
                        StaffInfo firstStaff = system.getFirstStaff();
                        Point2D pStart = firstStaff.getFirstLine().getEndPoint(
                                side);
                        StaffInfo lastStaff = system.getLastStaff();
                        Point2D pStop = lastStaff.getLastLine().getEndPoint(side);

                        // Dirty programming, sorry
                        if (limit instanceof Filament) {
                            ((Filament) limit).setEndingPoints(pStart, pStop);
                        }
                    }
                }
            } catch (Exception ex) {
                logger.warning(
                        "BarsRetriever cannot adjust side bars of system#"
                        + system.getId(),
                        ex);
            }
        }
    }

    //------------------//
    // getBarlineGlyphs //
    //------------------//
    /**
     * Report the set of all sticks that are actually part of the staff
     * bar lines (left or right side).
     *
     * @return the collection of used barline sticks
     */
    Set<Glyph> getBarlineGlyphs ()
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
        for (SystemInfo system : systemManager.getSystems()) {
            try {
                for (HorizontalSide side : HorizontalSide.values()) {
                    // Determine the side limit of the system
                    adjustSystemLimit(system, side);
                }

                if (logger.isFineEnabled()) {
                    logger.fine(
                            "System#{0} left:{1} right:{2}",
                            new Object[]{
                                system.getId(),
                                system.getLimit(LEFT).getClass().getSimpleName(),
                                system.getLimit(RIGHT).getClass().getSimpleName()
                            });
                }

                // Use system limits to adjust staff lines
                adjustStaffLines(system);
            } catch (Exception ex) {
                logger.warning(
                        "BarsRetriever cannot adjust system#" + system.getId(),
                        ex);
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
                    Glyph.reverseWeightComparator);

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

                logger.fine(
                        "System#{0} {1} drivingStick: {2}",
                        new Object[]{system.getId(), side, drivingStick});

                // Polish long driving stick, if needed
                if (isLong(drivingStick)) {
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

            logger.fine(
                    "System#{0} {1} barycenter: {2}",
                    new Object[]{system.getId(), side, bary});

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
     * This method creates the 'systems' member.
     */
    private void buildSystems ()
    {
        do {
            // Retrieve the staves that start systems
            if (systemTops == null) {
                systemTops = retrieveSystemTops();
            }

            logger.info(
                    "{0}Systems top staff ids: {1}",
                    new Object[]{sheet.getLogPrefix(), Arrays.toString(
                        systemTops)});

            // Create system frames using staves tops
            systemManager.setSystems(createSystems(systemTops));

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
        FilamentsFactory factory = new FilamentsFactory(
                scale,
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
     * @return true if left bars have been merged
     */
    private boolean canConnectSystems (SystemInfo prevSystem,
                                       SystemInfo nextSystem)
    {
        List<SystemInfo> systems = systemManager.getSystems();
        int maxBarPosGap = scale.toPixels(constants.maxBarPosGap);
        int maxBarCoordGap = scale.toPixels(
                constants.maxBarCoordGap);
        Skew skew = sheet.getSkew();

        if (logger.isFineEnabled()) {
            logger.info(
                    "Checking S#{0}({1}) - S#{2}({3})",
                    new Object[]{
                        prevSystem.getId(), prevSystem.getStaves().size(),
                        nextSystem.getId(),
                        nextSystem.getStaves().size()
                    });
        }

        StaffInfo prevStaff = prevSystem.getLastStaff();
        Point2D prevStaffPt = skew.deskewed(
                prevStaff.getLastLine().getEndPoint(LEFT));
        double prevY = prevStaffPt.getY();
        BarInfo prevBar = prevStaff.getBar(LEFT); // Perhaps null

        StaffInfo nextStaff = nextSystem.getFirstStaff();
        Point2D nextStaffPt = skew.deskewed(
                nextStaff.getFirstLine().getEndPoint(LEFT));
        double nextY = nextStaffPt.getY();
        BarInfo nextBar = nextStaff.getBar(LEFT); // Perhaps null

        // Check vertical connections between bars
        if ((prevBar != null) && (nextBar != null)) {
            // case: Bar - Bar
            for (Glyph prevStick : prevBar.getSticksAncestors()) {
                Point2D prevPoint = skew.deskewed(
                        prevStick.getStopPoint(VERTICAL));

                for (Glyph nextStick : nextBar.getSticksAncestors()) {
                    Point2D nextPoint = skew.deskewed(
                            nextStick.getStartPoint(VERTICAL));

                    // Check dx
                    double dx = Math.abs(nextPoint.getX() - prevPoint.getX());

                    // Check dy
                    double dy = Math.abs(
                            Math.min(nextY, nextPoint.getY())
                            - Math.max(prevY, prevPoint.getY()));

                    logger.fine(
                            "F{0}-F{1} dx:{2} vs {3}, dy:{4} vs {5}",
                            new Object[]{
                                prevStick.getId(), nextStick.getId(), (float) dx,
                                maxBarPosGap, (float) dy, maxBarCoordGap
                            });

                    if ((dx <= maxBarPosGap) && (dy <= maxBarCoordGap)) {
                        logger.info(
                                "Merging systems S#{0}({1}) - S#{2}({3})",
                                new Object[]{
                                    prevSystem.getId(),
                                    prevSystem.getStaves().size(), nextSystem.
                                    getId(),
                                    nextSystem.getStaves().size()
                                });

                        Filament pf = (Filament) prevStick;
                        Filament nf = (Filament) nextStick;
                        pf.stealSections(nf);

                        return tryRangeConnection(
                                systems.subList(
                                systems.indexOf(prevSystem),
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

                if (dy <= maxBarCoordGap) {
                    return tryRangeConnection(
                            systems.subList(
                            systems.indexOf(prevSystem),
                            1 + systems.indexOf(nextSystem)));
                }
            }
        } else if (nextBar != null) {
            // case: NoBar - Bar
            Point2D nextPoint = null;

            for (Glyph nextStick : nextBar.getSticksAncestors()) {
                Point2D point = skew.deskewed(
                        nextStick.getStartPoint(VERTICAL));

                if ((nextPoint == null) || (nextPoint.getY() > point.getY())) {
                    nextPoint = point;
                }
            }

            if ((nextY - nextPoint.getY()) > params.minBarChunkHeight) {
                double dy = nextPoint.getY() - prevY;

                if (dy <= maxBarCoordGap) {
                    return tryRangeConnection(
                            systems.subList(
                            systems.indexOf(prevSystem),
                            1 + systems.indexOf(nextSystem)));
                }
            }
        }

        return false;
    }

    //--------------------//
    // checkBarsAlignment //
    //--------------------//
    /**
     * Check that, within the same system, bars are vertically aligned
     * across all staves.
     * We first build BarAlignment instances to record the bar locations
     * and finally check these alignments for correctness.
     * Resulting alignments are stored in the SystemInfo instance.
     */
    private void checkBarsAlignment (SystemInfo system)
    {
        List<StaffInfo> staves = system.getStaves();
        int staffCount = staves.size();

        // Bar alignments for the system
        List<BarAlignment> alignments = null;

        for (int iStaff = 0; iStaff < staves.size(); iStaff++) {
            StaffInfo staff = staves.get(iStaff);

            IntersectionSequence staffBars = barSticks.get(staff);

            //            logger.info("System#" + system.getId() +" Staff#" + staff.getId());
            //            for (StickIntersection inter : staffBars) {
            //                logger.info(inter.toString());
            //            }
            //
            if (alignments == null) {
                // Initialize the alignments
                alignments = new ArrayList<>();
                system.setBarAlignments(alignments);

                for (StickIntersection loc : staffBars) {
                    BarAlignment align = new BarAlignment(sheet, staffCount);
                    align.addInter(iStaff, loc);
                    alignments.add(align);
                }
            } else {
                // Do we have a bar around each abscissa?
                for (StickIntersection stickLoc : staffBars) {
                    // Find closest alignment
                    Double[] dists = new Double[alignments.size()];
                    Integer bestIdx = null;

                    for (int ia = 0; ia < alignments.size(); ia++) {
                        BarAlignment align = alignments.get(ia);
                        Double dist = align.distance(iStaff, stickLoc);

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
                            && (Math.abs(dists[bestIdx]) <= params.maxAlignmentDistance)) {
                        alignments.get(bestIdx).addInter(iStaff, stickLoc);
                    } else {
                        // Insert a new alignment
                        BarAlignment align = new BarAlignment(
                                sheet,
                                staffCount);
                        align.addInter(iStaff, stickLoc);

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

            // If alignment is almost empty, remove it
            // otherwise, try to fill the holes
            int filled = align.getFilledCount();
            double ratio = (double) filled / staffCount;

            if (ratio < constants.minAlignmentRatio.getValue()) {
                // We remove this alignment and deassign its sticks
                logger.info(
                        "{0}Removing {1}",
                        new Object[]{sheet.getLogPrefix(), align});
                it.remove();

                for (StickIntersection inter : align.getIntersections()) {
                    if (inter != null) {
                        inter.getStickAncestor().setShape(null);
                    }
                }
            } else if (filled != staffCount) {
                // TODO: Should implement driven recognition here...
                logger.info(
                        "{0}Should fill {1}",
                        new Object[]{sheet.getLogPrefix(), align});
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

                system = new SystemInfo(
                        ++systemId,
                        sheet,
                        staffManager.getRange(staff, staff));
                newSystems.add(system);
            } else {
                // Continuing current system
                system.setStaves(
                        staffManager.getRange(system.getFirstStaff(), staff));
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
                    Glyph.reverseWeightComparator);

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

                    logger.fine(
                            "{0} extended {1}",
                            new Object[]{side, staff});
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
     * @param staff containing staff
     * @param line  the line at hand
     * @param side  the desired ending
     * @return the computed ending point
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
        if (limit instanceof Filament) {
            x = ((Filament) limit).getPositionAt(y, VERTICAL);
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
        List<SystemInfo> systems = systemManager.getSystems();
        boolean modified = false;

        // Check connection of left bars across systems
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

    //---------------------//
    // populateStaffSticks //
    //---------------------//
    /**
     * Retrieve the sticks that intersect each staff, the results being
     * kept as sequences of staff intersections in barSticks structure.
     */
    private void populateStaffSticks ()
    {
        for (Glyph stick : bars) {
            if (stick.getPartOf() != null) {
                continue;
            }

            Point2D start = stick.getStartPoint(VERTICAL);
            StaffInfo topStaff = staffManager.getStaffAt(start);
            Point2D stop = stick.getStopPoint(VERTICAL);
            StaffInfo botStaff = staffManager.getStaffAt(stop);

            if (logger.isFineEnabled() || stick.isVip()) {
                logger.info(
                        "Bar#{0} top:{1} bot:{2}",
                        new Object[]{
                            stick.getId(), topStaff.getId(), botStaff.getId()
                        });
            }

            int top = topStaff.getId();
            int bot = botStaff.getId();

            for (int id = top; id <= bot; id++) {
                StaffInfo staff = staffManager.getStaff(id - 1);
                IntersectionSequence staffSticks = barSticks.get(staff);

                if (staffSticks == null) {
                    staffSticks = new IntersectionSequence(
                            StickIntersection.horiComparator);
                    barSticks.put(staff, staffSticks);
                }

                staffSticks.add(
                        new StickIntersection(staff.intersection(stick), stick));
            }
        }
    }

    //---------------------//
    // preciseIntersection //
    //---------------------//
    private Point2D preciseIntersection (Glyph stick,
                                         LineInfo line)
    {
        Point2D startPoint = stick.getStartPoint(VERTICAL);
        Point2D stopPoint = stick.getStopPoint(VERTICAL);
        Point2D pt = LineUtilities.intersection(
                line.getEndPoint(LEFT),
                line.getEndPoint(RIGHT),
                startPoint,
                stopPoint);

        double y = line.yAt(pt.getX());
        double x;

        if (y < startPoint.getY()) {
            double invSlope = stick.getInvertedSlope();
            x = startPoint.getX() + ((y - startPoint.getY()) * invSlope);
        } else if (y > stopPoint.getY()) {
            double invSlope = stick.getInvertedSlope();
            x = stopPoint.getX() + ((y - stopPoint.getY()) * invSlope);
        } else {
            x = stick.getLine().xAtY(y);
        }

        return new Point2D.Double(x, y);
    }

    //----------------------//
    // recheckBarCandidates //
    //----------------------//
    /**
     * Have a closer look at so-called bars, now that the grid of staves
     * has been fully defined, to get rid of spurious bar line sticks.
     */
    private void recheckBarCandidates ()
    {
        // Do not check bars already involved in side barlines
        Set<Glyph> sideBars = getBarlineGlyphs();
        bars.removeAll(sideBars);

        // Check the others, now using more strict checks
        sheet.setBarsChecker(new BarsChecker(sheet, false));
        sheet.getBarsChecker().checkCandidates(bars);

        // Purge bars collection
        for (Iterator<Glyph> it = bars.iterator(); it.hasNext();) {
            Glyph stick = it.next();

            if (!stick.isBar()) {
                it.remove();
            }
        }

        bars.addAll(sideBars);

        // Purge barSticks accordingly
        for (IntersectionSequence seq : barSticks.values()) {
            for (Iterator<StickIntersection> it = seq.iterator(); it.hasNext();) {
                StickIntersection stickPos = it.next();

                if (!stickPos.getStickAncestor().isBar()) {
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

            for (StickIntersection inter : alignment.getIntersections()) {
                iStaff++;

                if (inter != null) {
                    StaffInfo staff = staves.get(iStaff);
                    Glyph stick = inter.getStickAncestor();

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
     * @return the collection of candidates barlines
     * @throws Exception
     */
    private void retrieveBarCandidates ()
            throws Exception
    {
        BarsChecker barsChecker = new BarsChecker(sheet, true); // Rough
        barsChecker.checkCandidates(filaments);

        // Consider only sticks with a barline shape
        for (Glyph glyph : sheet.getNest().getActiveGlyphs()) {
            if (glyph.isBar()) {
                Filament fil = (Filament) glyph;
                bars.add(fil);
            }
        }
    }

    //-------------------//
    // retrieveMajorBars //
    //-------------------//
    private void retrieveMajorBars ()
            throws Exception
    {
        // Build vertical filaments
        buildVerticalFilaments();

        // Retrieve rough barline candidates
        retrieveBarCandidates();

        // Assign bar candidates to intersected staves
        populateStaffSticks();

        // Retrieve left staff bars (they define systems) and right staff bars
        retrieveStaffSideBars();
    }

    //------------------//
    // retrievePartTops //
    //------------------//
    /**
     * Retrieve, for each staff, the staff that starts its containing
     * part.
     *
     * @return the (id of) part-starting staff for each staff
     */
    private Integer[] retrievePartTops ()
    {
        systemManager.setPartTops(
                partTops = new Integer[staffManager.getStaffCount()]);

        for (StaffInfo staff : staffManager.getStaves()) {
            ///logger.info("Staff#" + staff.getId());
            int bot = staff.getId();
            BarInfo leftBar = staff.getBar(LEFT);
            BarInfo rightBar = staff.getBar(RIGHT);
            double staffLeft = staff.getAbscissa(LEFT);
            IntersectionSequence staffSticks = barSticks.get(staff);

            for (StickIntersection stickPos : staffSticks) {
                Glyph stick = stickPos.getStickAncestor();

                if (!stick.isBar()) {
                    continue;
                }

                // Do not use left bar items
                if ((leftBar != null)
                        && leftBar.getSticksAncestors().contains(stick)) {
                    ///logger.info("Skipping left side  stick#" + stick.getId());
                    continue;
                }

                if (stickPos.x < staffLeft) {
                    // logger.info(
                    //   "Too left " + stickPos.x + " vs " + staffLeft +
                    //   " stick#" + stick.getId());
                    continue;
                }

                // Use right bar, if any, even if not anchored
                if ((rightBar != null)
                        && rightBar.getSticksAncestors().contains(stick)) {
                    ///logger.info("Using right side  stick#" + stick.getId());
                } else if (stick.getResult() != BarsChecker.BAR_PART_DEFINING) {
                    ///logger.info("Not anchored  stick#" + stick.getId());
                    continue;
                }

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
        IntersectionSequence staffSticks = barSticks.get(staff);
        final int dir = (side == LEFT) ? 1 : (-1);
        final double staffX = staff.getAbscissa(side);
        final double xBreak = staffX
                + (dir * params.maxDistanceFromStaffSide);
        IntersectionSequence seq = new IntersectionSequence(
                StickIntersection.horiComparator);
        BarInfo bar = null;
        staff.setBar(side, null); // Reset

        // Give first importance to long (inter-staff) sticks
        for (boolean takeAllSticks : new boolean[]{false, true}) {
            // Browse bar sticks from outside to inside of staff
            for (StickIntersection sp : (dir > 0) ? staffSticks
                                        : staffSticks.descendingSet()) {
                double x = sp.x;

                if ((dir * (xBreak - x)) < 0) {
                    break; // Speed up
                }

                if (takeAllSticks || isLong(sp.getStickAncestor())) {
                    if (seq.isEmpty()) {
                        seq.add(sp);
                    } else {
                        // Perhaps a pack of bars, check total width
                        double width = Math.max(
                                Math.abs(x - seq.first().x),
                                Math.abs(x - seq.last().x));

                        if (((side == LEFT)
                             && (width <= params.maxLeftBarPackWidth))
                                || ((side == RIGHT)
                                    && (width <= params.maxRightBarPackWidth))) {
                            seq.add(sp);
                        }
                    }
                }
            }
        }

        if (!seq.isEmpty()) {
            seq.reduce(params.maxPosGap); // Merge sticks vertically

            double barX = seq.last().x;

            if ((dir * (barX - staffX)) <= params.maxBarOffset) {
                bar = new BarInfo(seq.getSticks());
                staff.setBar(side, bar);
                staff.setAbscissa(side, barX);
            } else {
                if (logger.isFineEnabled()) {
                    logger.info(
                            "Staff#{0} {1} discarded stick#{2}",
                            new Object[]{
                                staff.getId(), side,
                                seq.last().getStickAncestor().getId()
                            });
                }
            }
        } else {
            if (logger.isFineEnabled()) {
                logger.fine(
                        "Staff#{0} no {1} bar {2}",
                        new Object[]{
                            staff.getId(), side,
                            Glyphs.toString(
                            StickIntersection.sticksOf(staffSticks))
                        });
            }
        }

        logger.fine("Staff#{0} {1} bar: {2}",
                    new Object[]{staff.getId(), side, bar});
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
     * @return the bar info for the system side, or null if no consistency
     * could be ensured
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

            List<Glyph> barFils = bar.getSticksAncestors();
            Integer delta = null; // Delta on stick indices between 2 staves
            int ib = 0; // Index on sticks

            if (seq == null) {
                seq = barFils.toArray(new Glyph[barFils.size()]);
            } else {
                // Loop on bar sticks
                BarLoop:
                for (ib = 0; ib < barFils.size(); ib++) {
                    Glyph barFil = barFils.get(ib);

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
                    int isBrk = Math.max(seq.length, barFils.size() + delta);

                    if ((isMin < 0) || (isBrk > seq.length)) {
                        // Allocate new sequence (shifted and/or enlarged)
                        Glyph[] newSeq = new Glyph[isBrk - isMin];
                        System.arraycopy(seq, 0, newSeq, -isMin, seq.length);
                        seq = newSeq;
                    }

                    for (ib = 0; ib < barFils.size(); ib++) {
                        Glyph barFil = barFils.get(ib);
                        int is = (ib + delta) - isMin;
                        Filament seqFil = (Filament) seq[is];

                        if (seqFil != null) {
                            seqFil = (Filament) seqFil.getAncestor();

                            if (seqFil != barFil) {
                                logger.fine(
                                        "Including F{0} to F{1}",
                                        new Object[]{
                                            barFil.getId(), seqFil.getId()
                                        });
                                seqFil.stealSections(barFil);
                            }
                        } else {
                            seq[is] = barFil;
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
     * Retrieve for each staff the staff that starts its containing
     * system.
     *
     * @return the (id of) system-starting staff for each staff
     */
    private Integer[] retrieveSystemTops ()
    {
        systemManager.setSystemTops(
                systemTops = new Integer[staffManager.getStaffCount()]);

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
     * @return true if OK
     */
    private boolean tryRangeConnection (List<SystemInfo> range)
    {
        final SystemInfo firstSystem = range.get(0);
        final StaffInfo firstStaff = firstSystem.getFirstStaff();
        final int topId = firstStaff.getId();
        int idx = staffManager.getStaves().indexOf(firstStaff);

        for (SystemInfo system : range) {
            for (StaffInfo staff : system.getStaves()) {
                systemTops[idx++] = topId;
            }
        }

        logger.info(
                "Staves connection from {0} to {1}",
                new Object[]{
                    topId, range.get(range.size() - 1).getLastStaff().getId()
                });

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
                0.25,
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
                4,
                "Max abscissa delta when looking for left or right side bars");

        Scale.Fraction maxLeftBarPackWidth = new Scale.Fraction(
                1.5,
                "Max width of a pack of vertical barlines");

        Scale.Fraction maxRightBarPackWidth = new Scale.Fraction(
                1,
                "Max width of a pack of vertical barlines");

        Scale.Fraction maxBarOffset = new Scale.Fraction(
                4,
                "Max abscissa offset of a bar candidate within staff width");

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
                "thickness",
                0.5,
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
     * Class {@code Parameters} gathers all constants related to
     * vertical frames.
     */
    private static class Parameters
    {
        //~ Static fields/initializers -----------------------------------------

        /** Usual logger utility */
        private static final Logger logger = Logger.getLogger(Parameters.class);

        //~ Instance fields ----------------------------------------------------
        /** Maximum delta abscissa for a gap between filaments */
        final int maxPosGap;

        /** Minimum run length for vertical lag */
        final int minRunLength;

        /** Used for section junction policy */
        final double maxLengthRatio;

        /** Minimum for long vertical stick bars */
        final int minLongLength;

        /** Maximum distance between a bar and the staff side */
        final int maxDistanceFromStaffSide;

        /** Maximum width for a pack of bars on left side */
        final int maxLeftBarPackWidth;

        /** Maximum width for a pack of bars on right side */
        final int maxRightBarPackWidth;

        /** Max abscissa offset of a bar candidate within staff width */
        final int maxBarOffset;

        /** Max difference on theoretical bar abscissa */
        final int maxSideDx;

        /** Max extension of line beyond staff bar */
        final int maxLineExtension;

        /** Min height to detect a bar going past a staff */
        final int minBarChunkHeight;

        /** Maximum abscissa shift for bar alignments */
        final double maxAlignmentDistance;

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
            maxRightBarPackWidth = scale.toPixels(
                    constants.maxRightBarPackWidth);
            maxBarOffset = scale.toPixels(constants.maxBarOffset);
            maxSideDx = scale.toPixels(constants.maxSideDx);
            maxLineExtension = scale.toPixels(constants.maxLineExtension);
            minBarChunkHeight = scale.toPixels(constants.minBarChunkHeight);
            maxAlignmentDistance = scale.toPixels(
                    constants.maxAlignmentDistance);

            // VIPs
            vipSections = decode(constants.verticalVipSections.getValue());

            if (logger.isFineEnabled()) {
                Main.dumping.dump(this);
            }

            if (!vipSections.isEmpty()) {
                logger.info("Vertical VIP sections: {0}", vipSections);
            }
        }

        //~ Methods ------------------------------------------------------------
        private List<Integer> decode (String str)
        {
            List<Integer> ids = new ArrayList<>();

            // Retrieve the list of ids
            StringTokenizer st = new StringTokenizer(str, ",");

            while (st.hasMoreTokens()) {
                try {
                    ids.add(Integer.decode(st.nextToken().trim()));
                } catch (Exception ex) {
                    logger.warning("Illegal id", ex);
                }
            }

            return ids;
        }
    }
}
