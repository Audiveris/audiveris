//----------------------------------------------------------------------------//
//                                                                            //
//                         B a r s R e t r i e v e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.GlyphSectionsBuilder;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;

import omr.lag.JunctionRatioPolicy;

import omr.log.Logger;

import omr.math.Barycenter;
import omr.math.BasicLine;
import omr.math.Line;

import omr.run.Orientation;
import omr.run.RunsTable;

import omr.score.ui.PagePainter;

import omr.sheet.BarsChecker;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;

import omr.step.StepException;

import omr.stick.StickSection;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * Class <code>BarsRetriever</code> focuses on the retrieval of vertical bars
 * to determine the side limits of staves and, most importantly, the gathering
 * of staves into systems.
 *
 * <pre>
 * buildInfo()
 *    +  retrieveBars()
 *       +  retrieveBarCandidates()        // Retrieve initial barline candidates
 *       +  populateStaffSticks()          // Assign bar candidates to intersected staves
 *       +  retrieveStaffSideBars()        // Retrieve left staff bars and right staff bars
 *
 *    +  retrieveSystems()
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
 * @author Hervé Bitteur
 */
public class BarsRetriever
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
    private GlyphLag vLag;

    /** Long filaments found, non sorted */
    private final List<Filament> filaments = new ArrayList<Filament>();

    /** Related staff manager */
    private final StaffManager staffManager;

    /** Related system manager */
    private final SystemManager systemManager;

    /** Candidate bar sticks */
    private final List<Stick> bars = new ArrayList<Stick>();

    /** Collection of bar sticks that intersect a staff */
    private final Map<StaffInfo, StickComb> barSticks = new HashMap<StaffInfo, StickComb>();

    /** System tops */
    private Integer[] systemTops;

    //~ Constructors -----------------------------------------------------------

    /** Sequence of systems */
    ////////////////////////////////////////////private List<SystemFrame> systems;

    //---------------//
    // BarsRetriever //
    //---------------//
    /**
     * Retrieve the frames of all staff lines
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

    //--------//
    // getLag //
    //--------//
    /**
     * Report the horizontal lag
     * @return the horizontal lag
     */
    public GlyphLag getLag ()
    {
        return vLag;
    }

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Use the long vertical sections to retrieve the barlines that limit staves
     */
    public void buildInfo ()
        throws StepException
    {
        try {
            // Retrieve bars
            retrieveBars();
        } catch (Exception ex) {
            logger.warning("BarsRetriever cannot retrieveBars", ex);
        }

        try {
            // Detect systems of staves aggregated via barlines
            retrieveSystems();
        } catch (Exception ex) {
            logger.warning("BarsRetriever cannot retrieveSystems", ex);
        }

        // Adjust precise sides for systems, staves & lines
        adjustSides();
    }

    //----------//
    // buildLag //
    //----------//
    /**
     * Build the underlying lag, out of the provided runs table.
     * This method must be called before building info.
     * @param vertTable the provided table of vertical runs
     * @param showRuns true to create views on runs
     */
    public void buildLag (RunsTable vertTable,
                          boolean   showRuns)
    {
        vLag = new GlyphLag("vLag", StickSection.class, Orientation.VERTICAL);

        GlyphSectionsBuilder sectionsBuilder = new GlyphSectionsBuilder(
            vLag,
            new JunctionRatioPolicy(params.maxLengthRatio));
        sectionsBuilder.createSections(vertTable);

        // Debug sections VIPs
        for (int id : params.vipSections) {
            GlyphSection sect = vLag.getVertexById(id);

            if (sect != null) {
                sect.setVip();
            }
        }
    }

    //------------------//
    // getBarlineGlyphs //
    //------------------//
    /**
     * Report the collection of all sticks that are actually part of the staff
     * bar lines (left or right side)
     * @return the collection of used barline sticks
     */
    List<Stick> getBarlineGlyphs ()
    {
        List<Stick> sticks = new ArrayList<Stick>();

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
    boolean isLong (Stick stick)
    {
        return (stick != null) && (stick.getLength() >= params.minLongLength);
    }

    //--------//
    // isLong //
    //--------//
    boolean isLong (BarInfo bar)
    {
        if (bar != null) {
            for (Stick stick : bar.getSticksAncestors()) {
                if (isLong(stick)) {
                    return true;
                }
            }
        }

        return false;
    }

    //------------------//
    // adjustStaffLines //
    //------------------//
    /**
     * Staff by staff, align the lines endings with the system limits, and
     * check the intermediate line points.
     * Package access meant for LinesRetriever companion.
     * @param system the system to process
     */
    void adjustStaffLines (SystemFrame system)
    {
        for (StaffInfo staff : system.getStaves()) {
            if (logger.isFineEnabled()) {
                logger.info(staff.toString());
            }

            // Adjust left and right endings of each line in the staff
            for (LineInfo l : staff.getLines()) {
                FilamentLine line = (FilamentLine) l;
                line.setEndingPoints(
                    getLineEnding(system, staff, line, LEFT),
                    getLineEnding(system, staff, line, RIGHT));
            }

            // Insert line intermediate points, if so needed
            List<LineFilament> fils = new ArrayList<LineFilament>();

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

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Render the filaments, their ending tangents, their patterns
     * @param g graphics context
     * @param showTangents true for showing ending tangents
     */
    void renderItems (Graphics2D g,
                      boolean    showTangents)
    {
        // Draw filaments
        g.setColor(PagePainter.musicColor);

        Stroke oldStroke = g.getStroke();
        g.setStroke(splineStroke);

        for (Filament filament : filaments) {
            filament.renderLine(g);
        }

        // Draw tangent at each ending point (using max coord gap)?
        if (showTangents) {
            g.setColor(Color.BLACK);

            double dy = sheet.getScale()
                             .toPixels(constants.maxCoordGap);

            for (Filament filament : filaments) {
                Point2D p = filament.getStartPoint();
                double  der = filament.slopeAt(p.getY());
                g.draw(
                    new Line2D.Double(
                        p.getX(),
                        p.getY(),
                        p.getX() - (der * dy),
                        p.getY() - dy));
                p = filament.getStopPoint();
                der = filament.slopeAt(p.getY());
                g.draw(
                    new Line2D.Double(
                        p.getX(),
                        p.getY(),
                        p.getX() + (der * dy),
                        p.getY() + dy));
            }
        }

        g.setStroke(oldStroke);
    }

    //---------------//
    // getLineEnding //
    //---------------//
    /**
     * Report the precise point where a given line should end
     * @param staff containing staff
     * @param line the line at hand
     * @param side the desired ending
     * @return the computed ending point
     */
    private Point2D getLineEnding (SystemFrame    system,
                                   StaffInfo      staff,
                                   LineInfo       line,
                                   HorizontalSide side)
    {
        double  slope = staff.getEndingSlope(side);
        Object  limit = system.getLimit(side);
        Point2D linePt = line.getEndPoint(side);
        double  staffX = staff.getAbscissa(side);
        double  y = linePt.getY() - ((linePt.getX() - staffX) * slope);
        double  x;

        // Dirty programming, sorry
        if (limit instanceof Filament) {
            x = ((Filament) limit).getPositionAt(y);
        } else if (limit instanceof Line) {
            x = ((Line) limit).xAtY(y);
        } else {
            throw new RuntimeException("Illegal system limit: " + limit);
        }

        return new Point2D.Double(x, y);
    }

    //-------------//
    // adjustSides //
    //-------------//
    /**
     * Adjust precise sides for systems, staves & lines
     */
    private void adjustSides ()
    {
        for (SystemFrame system : systemManager.getSystems()) {
            try {
                for (HorizontalSide side : HorizontalSide.values()) {
                    // Determine the side limit of the system
                    adjustSystemLimit(system, side);
                }

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "System#" + system.getId() + " left:" +
                        system.getLimit(LEFT).getClass().getSimpleName() +
                        " right:" +
                        system.getLimit(RIGHT).getClass().getSimpleName());
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
     * Adjust the limit on the desired side of the provided system and store the
     * chosen limit (whatever it is) in the system itself.
     * @param system the system to process
     * @param side the desired side
     * @see SystemFrame#getLimit(HorizontalSide)
     */
    private void adjustSystemLimit (SystemFrame    system,
                                    HorizontalSide side)
    {
        Stick   drivingStick = null;

        // Do we have a bar embracing the whole system?
        BarInfo bar = retrieveSystemBar(system, side);

        if (bar != null) {
            // We use the heaviest stick among the system-embracing bar sticks
            SortedSet<Stick> allSticks = new TreeSet<Stick>(
                Glyph.reverseWeightComparator);

            for (Stick stick : bar.getSticksAncestors()) {
                Point2D   start = stick.getStartPoint();
                StaffInfo topStaff = staffManager.getStaffAt(start);
                Point2D   stop = stick.getStopPoint();
                StaffInfo botStaff = staffManager.getStaffAt(stop);

                if ((topStaff == system.getFirstStaff()) &&
                    (botStaff == system.getLastStaff())) {
                    allSticks.add(stick);
                }
            }

            if (!allSticks.isEmpty()) {
                drivingStick = allSticks.first();

                if (logger.isFineEnabled()) {
                    logger.info(
                        "System#" + system.getId() + " " + side +
                        " drivingStick: " + drivingStick);
                }

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

            if (logger.isFineEnabled()) {
                logger.info(
                    "System#" + system.getId() + " " + side + " barycenter: " +
                    bary);
            }

            double    slope = sheet.getSkew()
                                   .getSlope();
            BasicLine line = new BasicLine();
            line.includePoint(bary.getX(), bary.getY());
            line.includePoint(bary.getX() - (100 * slope), bary.getY() + 100);
            system.setLimit(side, line);
        }
    }

    //-------------------//
    // canConnectSystems //
    //-------------------//
    /**
     * Try to merge the two provided systems into a single one
     * @param prevSystem the system above
     * @param nextSystem the system below
     * @return true if left bars have been merged
     */
    private boolean canConnectSystems (SystemFrame prevSystem,
                                       SystemFrame nextSystem)
    {
        List<SystemFrame> systems = systemManager.getSystems();
        int               maxBarPosGap = scale.toPixels(constants.maxBarPosGap);
        int               maxBarCoordGap = scale.toPixels(
            constants.maxBarCoordGap);
        Skew              skew = sheet.getSkew();

        if (logger.isFineEnabled()) {
            logger.info(
                "Checking S#" + prevSystem.getId() + "(" +
                prevSystem.getStaves().size() + ") - S#" + nextSystem.getId() +
                "(" + nextSystem.getStaves().size() + ")");
        }

        StaffInfo prevStaff = prevSystem.getLastStaff();
        Point2D   prevStaffPt = skew.deskewed(
            prevStaff.getLastLine().getEndPoint(LEFT));
        double    prevY = prevStaffPt.getY();
        BarInfo   prevBar = prevStaff.getBar(LEFT); // Perhaps null

        StaffInfo nextStaff = nextSystem.getFirstStaff();
        Point2D   nextStaffPt = skew.deskewed(
            nextStaff.getFirstLine().getEndPoint(LEFT));
        double    nextY = nextStaffPt.getY();
        BarInfo   nextBar = nextStaff.getBar(LEFT); // Perhaps null

        // Check vertical connections between bars
        if ((prevBar != null) && (nextBar != null)) {
            // case: Bar - Bar
            for (Stick prevStick : prevBar.getSticksAncestors()) {
                Point2D prevPoint = skew.deskewed(prevStick.getStopPoint());

                for (Stick nextStick : nextBar.getSticksAncestors()) {
                    Point2D nextPoint = skew.deskewed(
                        nextStick.getStartPoint());

                    // Check dx
                    double dx = Math.abs(nextPoint.getX() - prevPoint.getX());

                    // Check dy
                    double dy = Math.abs(
                        Math.min(nextY, nextPoint.getY()) -
                        Math.max(prevY, prevPoint.getY()));

                    if (logger.isFineEnabled()) {
                        logger.info(
                            "F" + prevStick.getId() + "-F" + nextStick.getId() +
                            " dx:" + (float) dx + " vs " + maxBarPosGap +
                            ", dy:" + (float) dy + " vs " + maxBarCoordGap);
                    }

                    if ((dx <= maxBarPosGap) && (dy <= maxBarCoordGap)) {
                        logger.info(
                            "Merging systems S#" + prevSystem.getId() + "(" +
                            prevSystem.getStaves().size() + ") - S#" +
                            nextSystem.getId() + "(" +
                            nextSystem.getStaves().size() + ")");

                        Filament pf = (Filament) prevStick;
                        Filament nf = (Filament) nextStick;
                        pf.include(nf);

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

            for (Stick prevStick : prevBar.getSticksAncestors()) {
                Point2D point = skew.deskewed(prevStick.getStopPoint());

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
        } else {
            // case: NoBar - Bar
            Point2D nextPoint = null;

            for (Stick nextStick : nextBar.getSticksAncestors()) {
                Point2D point = skew.deskewed(nextStick.getStartPoint());

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

    //---------------//
    // createSystems //
    //---------------//
    /**
     * Build the frame of each system
     * @param tops the starting staff id for each system
     * @return the sequence of system physical frames
     */
    private List<SystemFrame> createSystems (Integer[] tops)
    {
        List<SystemFrame> newSystems = new ArrayList<SystemFrame>();
        Integer           staffTop = null;
        int               systemId = 0;
        SystemFrame       systemFrame = null;

        for (int i = 0; i < staffManager.getStaffCount(); i++) {
            StaffInfo staff = staffManager.getStaff(i);

            // System break?
            if ((staffTop == null) || (staffTop < tops[i])) {
                // Start of a new system
                staffTop = tops[i];

                systemFrame = new SystemFrame(
                    ++systemId,
                    staffManager.getRange(staff, staff));
                newSystems.add(systemFrame);
            } else {
                // Continuing current system
                systemFrame.setStaves(
                    staffManager.getRange(systemFrame.getFirstStaff(), staff));
            }
        }

        return newSystems;
    }

    //---------------------//
    // extendStaffAbscissa //
    //---------------------//
    /**
     * Determine a not-too-bad abscissa for staff end, extending the abscissa
     * beyond the bar limit if staff lines so require.
     * @param staff the staff to process
     * @param side the desired side
     * @return the staff abscissa
     */
    private double extendStaffAbscissa (StaffInfo      staff,
                                        HorizontalSide side)
    {
        // Check that staff bar, if any, is not passed by lines
        BarInfo bar = staff.getBar(side);

        if (bar != null) {
            Stick            drivingStick = null;
            double           linesX = staff.getLinesEnd(side);

            // Pick up the heaviest bar stick
            SortedSet<Stick> allSticks = new TreeSet<Stick>(
                Glyph.reverseWeightComparator);

            // Use long sticks first
            for (Stick stick : bar.getSticksAncestors()) {
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
                Point2D   inter = staff.intersection(drivingStick);
                double    barX = inter.getX();
                final int dir = (side == LEFT) ? 1 : (-1);

                if ((dir * (barX - linesX)) > params.maxLineExtension) {
                    staff.setBar(side, null);
                    staff.setAbscissa(side, linesX);

                    if (logger.isFineEnabled()) {
                        logger.fine(side + " extended " + staff);
                    }
                }
            }
        }

        return staff.getAbscissa(side);
    }

    //--------------//
    // mergeSystems //
    //--------------//
    private boolean mergeSystems ()
    {
        List<SystemFrame> systems = systemManager.getSystems();
        boolean           modified = false;

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
     * Retrieve the sticks that intersect each staff
     * @return the sequence of staff intersections
     */
    private void populateStaffSticks ()
    {
        for (Stick stick : bars) {
            if (stick.getPartOf() != null) {
                continue;
            }

            Point2D   start = stick.getStartPoint();
            StaffInfo topStaff = staffManager.getStaffAt(start);
            Point2D   stop = stick.getStopPoint();
            StaffInfo botStaff = staffManager.getStaffAt(stop);

            if (logger.isFineEnabled() || stick.isVip()) {
                logger.info(
                    "Bar#" + stick.getId() + " top:" + topStaff.getId() +
                    " bot:" + botStaff.getId());
            }

            int top = topStaff.getId();
            int bot = botStaff.getId();

            for (int id = top; id <= bot; id++) {
                StaffInfo staff = staffManager.getStaff(id - 1);
                StickComb staffSticks = barSticks.get(staff);

                if (staffSticks == null) {
                    staffSticks = new StickComb();
                    barSticks.put(staff, staffSticks);
                }

                Point2D inter = staff.intersection(stick);
                staffSticks.add(new StickPos(inter.getX(), stick));
            }
        }
    }

    //-----------------------//
    // retrieveBarCandidates //
    //-----------------------//
    /**
     * Retrieve initial barline candidates
     * @return the collection of candidates barlines
     * @throws Exception
     */
    private void retrieveBarCandidates ()
        throws Exception
    {
        // Filaments factory
        FilamentsFactory factory = new FilamentsFactory(
            scale,
            vLag,
            Filament.class);

        // Debug VIP sticks
        factory.setVipGlyphs(params.vipSticks);

        // Factory parameters adjustment
        factory.setMaxSectionThickness(constants.maxSectionThickness);
        factory.setMaxFilamentThickness(constants.maxFilamentThickness);
        factory.setMaxCoordGap(constants.maxCoordGap);
        factory.setMaxPosGap(constants.maxPosGap);

        ///factory.setMaxOverlapDeltaPos(constants.maxOverlapDeltaPos);

        // Retrieve filaments out of vertical sections
        for (Filament fil : factory.retrieveFilaments(vLag.getVertices(), true)) {
            filaments.add(fil);
        }

        BarsChecker barsChecker = new BarsChecker(sheet, vLag, true);
        barsChecker.retrieveCandidates(filaments);

        // Consider only sticks with a barline shape
        for (Glyph glyph : vLag.getActiveGlyphs()) {
            Shape shape = glyph.getShape();

            if ((shape == Shape.THICK_BARLINE) ||
                (shape == Shape.THIN_BARLINE)) {
                Filament fil = (Filament) glyph;
                bars.add(fil);
            }
        }
    }

    //--------------//
    // retrieveBars //
    //--------------//
    private void retrieveBars ()
        throws Exception
    {
        // Retrieve initial barline candidates
        retrieveBarCandidates();

        // Assign bar candidates to intersected staves
        populateStaffSticks();

        // Retrieve left staff bars, they define systems
        retrieveStaffSideBars();
    }

    //------------------//
    // retrieveStaffBar //
    //------------------//
    /**
     * Retrieve the (perhaps multi-stick) complete side bar, if any, of a given
     * staff and record it within the staff
     * @param staff the given staff
     * @param side proper horizontal side
     */
    private void retrieveStaffBar (StaffInfo      staff,
                                   HorizontalSide side)
    {
        StickComb    staffSticks = barSticks.get(staff);
        final int    dir = (side == LEFT) ? 1 : (-1);
        final double staffX = staff.getAbscissa(side);
        final double xBreak = staffX + (dir * params.maxDistanceFromStaffSide);
        StickComb    comb = new StickComb();
        BarInfo      bar = null;
        staff.setBar(side, null); // Reset

        // Give first importance to long (inter-staff) sticks
        for (boolean takeAllSticks : new boolean[] { false, true }) {
            // Browse bar sticks from outside to inside of staff
            for (StickPos sp : (dir > 0) ? staffSticks
                               : staffSticks.descendingSet()) {
                double x = sp.x;

                if ((dir * (xBreak - x)) < 0) {
                    break; // Speed up
                }

                if (takeAllSticks || isLong(sp.getStickAncestor())) {
                    if (comb.isEmpty()) {
                        comb.add(sp);
                    } else {
                        // Perhaps a pack of bars, check total width
                        double width = Math.max(
                            Math.abs(x - comb.first().x),
                            Math.abs(x - comb.last().x));

                        if (((side == LEFT) &&
                            (width <= params.maxLeftBarPackWidth)) ||
                            ((side == RIGHT) &&
                            (width <= params.maxRightBarPackWidth))) {
                            comb.add(sp);
                        }
                    }
                }
            }
        }

        if (!comb.isEmpty()) {
            comb.reduce(params.maxPosGap); // Merge sticks vertically

            double barX = comb.last().x;

            if ((dir * (barX - staffX)) <= params.maxBarOffset) {
                bar = new BarInfo(comb.getSticks());
                staff.setBar(side, bar);
                staff.setAbscissa(side, barX);
            } else {
                if (logger.isFineEnabled()) {
                    logger.info(
                        "Staff#" + staff.getId() + " " + side +
                        " discarded stick#" +
                        comb.last().getStickAncestor().getId());
                }
            }
        } else {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "Staff#" + staff.getId() + " no " + side + " bar " +
                    Glyphs.toString(StickPos.sticksOf(staffSticks)));
            }
        }

        if (logger.isFineEnabled()) {
            logger.info("Staff#" + staff.getId() + " " + side + " bar: " + bar);
        }
    }

    //-----------------------//
    // retrieveStaffSideBars //
    //-----------------------//
    /**
     * Retrieve staffs left bar (which define systems) and right bar
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
     * Merge bar sticks across staves of the same system, as lons as they can be
     * connected even indirectly via other sticks
     * @param system the system to process
     * @param side the side to process
     * @return the bar info for the system side, or null if no consistency
     * could be ensured
     */
    private BarInfo retrieveSystemBar (SystemFrame    system,
                                       HorizontalSide side)
    {
        // Horizontal sequence of bar sticks applied to the whole system side
        Stick[] seq = null;

        for (StaffInfo staff : system.getStaves()) {
            BarInfo bar = staff.getBar(side);

            if (bar == null) {
                return null; // We cannot ensure consistency
            }

            List<Stick> barFils = bar.getSticksAncestors();
            Integer     delta = null;
            int         ib = 0;

            if (seq == null) {
                seq = barFils.toArray(new Stick[barFils.size()]);
            } else {
                // Loop on bar sticks
                BarLoop: 
                for (ib = 0; ib < barFils.size(); ib++) {
                    Stick barFil = barFils.get(ib);

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
                        Stick[] newSeq = new Stick[isBrk - isMin];
                        System.arraycopy(seq, 0, newSeq, -isMin, seq.length);
                        seq = newSeq;
                    }

                    for (ib = 0; ib < barFils.size(); ib++) {
                        Stick    barFil = barFils.get(ib);
                        int      is = (ib + delta) - isMin;
                        Filament seqFil = (Filament) seq[is];

                        if (seqFil != null) {
                            seqFil = seqFil.getAncestor();

                            if (seqFil != barFil) {
                                if (logger.isFineEnabled()) {
                                    logger.info(
                                        "Including F" + barFil.getId() +
                                        " to F" + seqFil.getId());
                                }

                                seqFil.include(barFil);
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
     * Retrieve for each staff the staff that starts its containing system
     * @return the (id of) system starting staff for each staff
     */
    private Integer[] retrieveSystemTops ()
    {
        systemTops = new Integer[staffManager.getStaffCount()];

        for (StaffInfo staff : staffManager.getStaves()) {
            int     bot = staff.getId();
            BarInfo bar = staff.getBar(LEFT);

            for (Stick stick : bar.getSticksAncestors()) {
                Point2D   start = stick.getStartPoint();
                StaffInfo topStaff = staffManager.getStaffAt(start);
                int       top = topStaff.getId();

                for (int id = top; id <= bot; id++) {
                    if ((systemTops[id - 1] == null) ||
                        (top < systemTops[id - 1])) {
                        systemTops[id - 1] = top;
                    }
                }
            }
        }

        return systemTops;
    }

    //-----------------//
    // retrieveSystems //
    //-----------------//
    /**
     * Detect systems of staves aggregated via connecting barlines.
     * This method creates the 'systems' member.
     */
    private void retrieveSystems ()
    {
        do {
            // Retrieve the staves that start systems
            if (systemTops == null) {
                systemTops = retrieveSystemTops();
            }

            logger.info("top staff ids: " + Arrays.toString(systemTops));

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

    //--------------------//
    // tryRangeConnection //
    //--------------------//
    /**
     * Try to connect all systems in the provided range
     * @param range sublist of systems
     * @return true if OK
     */
    private boolean tryRangeConnection (List<SystemFrame> range)
    {
        final SystemFrame firstSystem = range.get(0);
        final StaffInfo   firstStaff = firstSystem.getFirstStaff();
        final int         topId = firstStaff.getId();
        int               idx = staffManager.getStaves()
                                            .indexOf(firstStaff);

        for (SystemFrame system : range) {
            for (StaffInfo staff : system.getStaves()) {
                systemTops[idx++] = topId;
            }
        }

        logger.info(
            "Staves connection from " + topId + " to " +
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
    //        for (SystemFrame system : systems) {
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

        Constant.Ratio   maxLengthRatio = new Constant.Ratio(
            1.5,
            "Maximum ratio in length for a run to be combined with an existing section");

        // Constants specified WRT mean interline
        // --------------------------------------
        Scale.Fraction   maxSectionThickness = new Scale.Fraction(
            0.8,
            "Maximum horizontal section thickness WRT interline");
        Scale.Fraction   maxFilamentThickness = new Scale.Fraction(
            0.8,
            "Maximum horizontal filament thickness WRT interline");
        Scale.Fraction   maxOverlapDeltaPos = new Scale.Fraction(
            1.0,
            "Maximum delta position between two overlapping filaments");
        Scale.Fraction   maxCoordGap = new Scale.Fraction(
            0.5,
            "Maximum delta coordinate for a gap between filaments");
        Scale.Fraction   maxPosGap = new Scale.Fraction(
            0.2,
            "Maximum delta abscissa for a gap between filaments");
        Scale.Fraction   maxBarCoordGap = new Scale.Fraction(
            2,
            "Maximum delta coordinate for a vertical gap between bars");
        Scale.Fraction   maxBarPosGap = new Scale.Fraction(
            0.3,
            "Maximum delta position for a vertical gap between bars");
        Scale.Fraction   minRunLength = new Scale.Fraction(
            1.5,
            "Minimum length for a vertical run to be considered");
        Scale.Fraction   minLongLength = new Scale.Fraction(
            8,
            "Minimum length for a long vertical bar");
        Scale.Fraction   maxDistanceFromStaffSide = new Scale.Fraction(
            4,
            "Max abscissa delta when looking for left or right side bars");
        Scale.Fraction   maxLeftBarPackWidth = new Scale.Fraction(
            1.5,
            "Max width of a pack of vertical barlines");
        Scale.Fraction   maxRightBarPackWidth = new Scale.Fraction(
            0.5,
            "Max width of a pack of vertical barlines");
        Scale.Fraction   maxBarOffset = new Scale.Fraction(
            4,
            "Max abscissa offset of a bar candidate within staff width");
        Scale.Fraction   maxSideDx = new Scale.Fraction(
            .5,
            "Max difference on theoretical bar abscissa");
        Scale.Fraction   maxLineExtension = new Scale.Fraction(
            .5,
            "Max extension of line beyond staff bar");
        Scale.Fraction   minBarChunkHeight = new Scale.Fraction(
            1,
            "Min height of a bar chunk past system boundaries");

        // Constants for display
        //
        Constant.Double  splineThickness = new Constant.Double(
            "thickness",
            0.5,
            "Stroke thickness to draw filaments curves");

        //
        Constant.Boolean smartStavesConnections = new Constant.Boolean(
            true,
            "(beta) Should we try smart staves connections into systems?");

        // Constants for debugging
        //
        Constant.String verticalVipSections = new Constant.String(
            "",
            "(Debug) Comma-separated list of VIP sections");
        Constant.String verticalVipSticks = new Constant.String(
            "",
            "(Debug) Comma-separated list of VIP sticks");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all constants related to vertical frames
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

        /** Maximum width for a pack of bars on right side*/
        final int maxRightBarPackWidth;

        /** Max abscissa offset of a bar candidate within staff width*/
        final int maxBarOffset;

        /** Max difference on theoretical bar abscissa */
        final int maxSideDx;

        /** Max extension of line beyond staff bar */
        final int maxLineExtension;

        /** Min height to detect a bar going past a staff */
        final int minBarChunkHeight;

        // Debug
        final List<Integer> vipSections;
        final List<Integer> vipSticks;

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

            // VIPs
            vipSections = decode(constants.verticalVipSections.getValue());
            vipSticks = decode(constants.verticalVipSticks.getValue());

            if (logger.isFineEnabled()) {
                Main.dumping.dump(this);
            }

            if (!vipSections.isEmpty()) {
                logger.info("Vertical VIP sections: " + vipSections);
            }

            if (!vipSticks.isEmpty()) {
                logger.info("Vertical VIP sticks: " + vipSticks);
            }
        }

        //~ Methods ------------------------------------------------------------

        private List<Integer> decode (String str)
        {
            List<Integer>   ids = new ArrayList<Integer>();

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

    //-----------//
    // StickComb //
    //-----------//
    /**
     * A horizontal sequence of vertical sticks, kept sorted on horizontal position
     */
    private static class StickComb
        extends TreeSet<StickPos>
    {
        //~ Methods ------------------------------------------------------------

        public List<Stick> getSticks ()
        {
            return StickPos.sticksOf(this);
        }

        public void reduce (double maxDeltaPos)
        {
            // If 2 sticks are close in position, simply merge them
            for (Iterator<StickPos> headIt = iterator(); headIt.hasNext();) {
                StickPos head = headIt.next();

                for (StickPos tail : tailSet(head, false)) {
                    if (tail.getStickAncestor() == head.getStickAncestor()) {
                        continue;
                    }

                    if ((tail.x - head.x) <= maxDeltaPos) {
                        if (logger.isFineEnabled() ||
                            head.stick.isVip() ||
                            tail.stick.isVip()) {
                            logger.info(
                                "Merging verticals " + head + " & " + tail);
                        }

                        Filament fil = (Filament) tail.getStickAncestor();
                        fil.include(head.getStickAncestor());
                        headIt.remove();

                        break;
                    }
                }
            }
        }
    }

    //----------//
    // StickPos //
    //----------//
    private static class StickPos
        implements Comparable<StickPos>
    {
        //~ Instance fields ----------------------------------------------------

        /** Abscissa where the stick intersects the related staff */
        final double x;

        /** The (bar) stick */
        private final Stick stick;

        //~ Constructors -------------------------------------------------------

        public StickPos (double x,
                         Stick  stick)
        {
            this.x = x;
            this.stick = stick;
        }

        //~ Methods ------------------------------------------------------------

        /** For sorting sticks on abscissa, for a given staff */
        public int compareTo (StickPos that)
        {
            int dx = Double.compare(x, that.x);

            if (dx != 0) {
                return dx;
            } else {
                // Just to disambiguate
                return Double.compare(
                    getStickAncestor()
                        .getAreaCenter().y,
                    that.getStickAncestor()
                        .getAreaCenter().y);
            }
        }

        /** Conversion to a sequence of sticks */
        public static List<Stick> sticksOf (Collection<StickPos> sps)
        {
            List<Stick> sticks = new ArrayList<Stick>();

            for (StickPos sp : sps) {
                sticks.add(sp.getStickAncestor());
            }

            return sticks;
        }

        /**
         * @return the stick, in fact its ancestor
         */
        public Stick getStickAncestor ()
        {
            return (Stick) stick.getAncestor();
        }

        @Override
        public String toString ()
        {
            return "Stick#" + getStickAncestor()
                                  .getId() + "@" + (float) x;
        }
    }
}
