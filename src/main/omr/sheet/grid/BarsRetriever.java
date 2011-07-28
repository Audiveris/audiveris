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
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;

import omr.lag.JunctionRatioPolicy;
import omr.lag.SectionsBuilder;

import omr.log.Logger;

import omr.run.Orientation;
import omr.run.RunsTable;

import omr.score.ui.PagePainter;

import omr.sheet.BarsChecker;
import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.step.StepException;

import omr.stick.StickSection;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * Class <code>BarsRetriever</code> focuses on the retrieval of vertical bars
 * to determine the horizontal limits of staves and the gathering of staves
 * into system.
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

    /** Global slope of the sheet */
    private double globalSlope;

    /** Related staff manager */
    private final StaffManager staffManager;

    /** Candidate bar sticks */
    private List<Stick> bars;

    /** Collection of bar sticks that intersect a staff */
    private Map<StaffInfo, List<StickX>> barSticks;

    /** Sequence of systems */
    private List<SystemFrame> systems;

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

    //------------//
    // getSystems //
    //------------//
    /**
     * Report the sequence of detected systems
     * @return the systems detected
     */
    public List<SystemFrame> getSystems ()
    {
        return systems;
    }

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Use the long vertical sections to retrieve the barlines that limit the
     * staves
     * @param globalSlope the sheet global slope
     */
    public void buildInfo (double globalSlope)
        throws StepException
    {
        this.globalSlope = globalSlope;

        try {
            // Retrieve initial barline candidates
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

        try {
            // Adjust precise sides for systems, staves & lines
            adjustSides();
        } catch (Exception ex) {
            logger.warning("BarsRetriever cannot adjustSides", ex);
        }
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

        SectionsBuilder sectionsBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
            vLag,
            new JunctionRatioPolicy(params.maxLengthRatio));
        sectionsBuilder.createSections(vertTable);

        //                vLag.getVertexById(942)
        //                    .setVip(); // BINGO
        //                vLag.getVertexById(923)
        //                    .setVip(); // BINGO
    }

    //-----------//
    // isLongBar //
    //-----------//
    boolean isLongBar (Stick stick)
    {
        return stick.getLength() >= params.minLongLength;
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Render the filaments, their ending tangents, their patterns
     * @param g graphics context
     */
    void renderItems (Graphics2D g)
    {
        // Draw filaments
        g.setColor(PagePainter.musicColor);

        Stroke oldStroke = g.getStroke();
        g.setStroke(splineStroke);

        for (Filament filament : filaments) {
            filament.renderLine(g);
        }

        // Draw tangent at each ending point (using max coord gap)
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

        g.setStroke(oldStroke);
    }

    //--------------//
    // getBestStick //
    //--------------//
    private StickX getBestStick (StaffInfo staff,
                                 double    x,
                                 double    maxDx)
    {
        StickX best = null;
        double bestDx = Integer.MAX_VALUE;

        for (StickX sx : barSticks.get(staff)) {
            double dx = Math.abs(x - sx.x);

            if ((dx < maxDx) && (dx < bestDx)) {
                bestDx = dx;
                best = sx;
            }
        }

        return best;
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
    private Point2D getLineEnding (StaffInfo      staff,
                                   LineInfo       line,
                                   HorizontalSide side)
    {
        double  slope = staff.getEndingSlope(side);
        Stick   stick = (staff.getBar(side) == null) ? null
                        : staff.getBar(side)
                               .getStick(RIGHT);
        Point2D linePt = line.getEndPoint(side);
        double  staffX = staff.getAbscissa(side);
        double  y = linePt.getY() - ((linePt.getX() - staffX) * slope);
        double  x = (stick == null) ? staffX : stick.getPositionAt(y);

        return new Point2D.Double(x, y);
    }

    //----------------------//
    // adjustLineSystemSide //
    //----------------------//
    /**
     * Adjust the precise side of a system, for which we align line endings
     * @param system the system to process
     * @param side the desired side
     */
    private void adjustLineSystemSide (SystemFrame    system,
                                       HorizontalSide side)
    {
        // Compute theoretical bar line equation x = -y*slope + b;
        double deltas = 0;

        for (StaffInfo staff : system.getStaves()) {
            double x = staff.getLinesEnd(side);
            double y = staff.getMidOrdinate(side);
            deltas += (x + (y * globalSlope));
        }

        double b = deltas / system.getStaves()
                                  .size();

        // Enforce this pseudo-vertical line
        for (StaffInfo staff : system.getStaves()) {
            double y = staff.getMidOrdinate(side);
            double x = b - (y * globalSlope);
            staff.setBar(side, null);
            staff.setAbscissa(side, x);
        }
    }

    //----------------------//
    // adjustLongSystemSide //
    //----------------------//
    /**
     * Adjust the precise side of a system, for which we have some staves with
     * long reliable bar (and perhaps other staves without such bars)
     * @param system the system to process
     * @param side the desired side
     */
    private void adjustLongSystemSide (SystemFrame    system,
                                       HorizontalSide side)
    {
        List<StaffInfo> staves = system.getStaves();
        StaffInfo       prevLong = null;

        for (int idx = 0; idx < staves.size(); idx++) {
            StaffInfo staff = staves.get(idx);

            // Skip staves with long bar
            if (staffHasLongBar(staff, side)) {
                prevLong = staff;

                continue;
            }

            // Is there a staff w/ long bar after this one?
            StaffInfo nextLong = null;

            for (int i = idx + 1; i < staves.size(); i++) {
                StaffInfo s = staves.get(i);

                if (staffHasLongBar(s, side)) {
                    nextLong = s;

                    break;
                }
            }

            double staffY = staff.getMidOrdinate(side);
            double staffX;

            if ((prevLong != null) && (nextLong != null)) {
                // Interpolate
                Point2D prev = prevLong.intersection(
                    prevLong.getBar(side).getStick(RIGHT));
                Point2D next = nextLong.intersection(
                    nextLong.getBar(side).getStick(RIGHT));
                staffX = prev.getX() +
                         ((staffY - prev.getY()) * ((next.getX() - prev.getX()) / (next.getY() -
                                                                                  prev.getY())));
            } else {
                // Extrapolate using global slope
                Point2D pt = (prevLong != null)
                             ? prevLong.intersection(
                    prevLong.getBar(side).getStick(RIGHT))
                             : nextLong.intersection(
                    nextLong.getBar(side).getStick(RIGHT));
                staffX = pt.getX() - ((staffY - pt.getY()) * globalSlope);
            }

            // Use staffX to check existing bar stick
            StickX sx = getBestStick(staff, staffX, params.maxSideDx);

            if (sx != null) { // Use the precise stick abscissa
                staff.setBar(side, new BarInfo(sx.stick));
                staff.setAbscissa(side, sx.x);
            } else { // Use the theoretical abscissa
                staff.setBar(side, null);
                staff.setAbscissa(side, staffX);
            }

            if (logger.isFineEnabled()) {
                logger.fine(side + " adjusted " + staff);
            }
        }
    }

    //-----------------------//
    // adjustShortSystemSide //
    //-----------------------//
    /**
     * Adjust the precise side of a system, for which we have all staves with
     * short (unreliable) bar
     * @param system the system to process
     * @param side the desired side
     */
    private void adjustShortSystemSide (SystemFrame    system,
                                        HorizontalSide side)
    {
        //        final int       dir = (side == LEFT) ? 1 : (-1);
        //        List<StaffInfo> staves = system.getStaves();
        //
        //        for (int idx = 0; idx < staves.size(); idx++) {
        //            StaffInfo staff = staves.get(idx);
        //
        //            // Check that staff bar, if any, is not passed by lines
        //            BarInfo bar = staff.getBar(side);
        //
        ////            if (bar != null) {
        ////                int linesX = staff.getLinesEnd(side);
        ////                int barX = staff.intersection(bar.getStick(RIGHT)).x;
        ////
        ////                if ((dir * (barX - linesX)) > params.maxLineExtension) {
        ////                    staff.setBar(side, null);
        ////                    staff.setAbscissa(side, linesX);
        ////
        ////                    if (logger.isFineEnabled()) {
        ////                        logger.info(side + " extended " + staff);
        ////                    }
        ////                }
        ////            }
        //        }
    }

    //-------------//
    // adjustSides //
    //-------------//
    /**
     * Adjust precise sides for systems, staves & lines
     */
    private void adjustSides ()
    {
        // Systems
        for (SystemFrame system : systems) {
            retrieveSystemSides(system);
            adjustSystemSides(system);
        }

        // Staff lines
        adjustStaffLines();
    }

    //------------------//
    // adjustStaffLines //
    //------------------//
    /**
     * Staff by staff, align the lines endings with the staff endings, and
     * check the intermediate line points
     */
    private void adjustStaffLines ()
    {
        for (StaffInfo staff : staffManager.getStaves()) {
            if (logger.isFineEnabled()) {
                logger.info(staff.toString());
            }

            // Adjust left and right endings of each line in the staff
            for (LineInfo l : staff.getLines()) {
                FilamentLine line = (FilamentLine) l;
                line.setEndingPoints(
                    getLineEnding(staff, line, LEFT),
                    getLineEnding(staff, line, RIGHT));
            }

            // Check line intermediate points
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

    //-------------------//
    // adjustSystemSides //
    //-------------------//
    /**
     * Adjust the precise abscissa of each side of the system staves.
     *
     * Retrieve the left and right side of each system (& staff),
     * to adjust precise ending points of each staff line.
     * We need a precise point in x (from barline) and in y (from staff line).
     *
     * <p>Nota: We have to make sure that all staves of a given system exhibit
     * consistent sides, otherwise the dewarping will strongly degrade the
     * image.</p>
     * @param system the system to process
     */
    private void adjustSystemSides (SystemFrame system)
    {
        final List<StaffInfo> staves = system.getStaves();
        final int             staffCount = staves.size();

        for (HorizontalSide side : HorizontalSide.values()) {
            int longs = 0;
            int shorts = 0;

            for (StaffInfo staff : staves) {
                BarInfo bar = staff.getBar(side);

                if (bar != null) {
                    if (isLongBar(bar.getStick(RIGHT))) {
                        longs++;
                    } else {
                        shorts++;
                    }
                }
            }

            // Check consistency of staves within the system
            if (longs > 0) {
                if (logger.isFineEnabled()) {
                    logger.info(
                        "System#" + system.getId() + " " + side +
                        " long bars: " + longs + "/" + staffCount);
                }

                // Align on long bars
                adjustLongSystemSide(system, side);
            } else {
                if (logger.isFineEnabled()) {
                    logger.info(
                        "System#" + system.getId() + " " + side +
                        " short bars: " + shorts + "/" + staffCount);
                }

                // Double-check system consistency
                if (shorts == staffCount) {
                    // (Do nothing)
                    adjustShortSystemSide(system, side);
                } else {
                    // Use line ends, adjusted for slope
                    adjustLineSystemSide(system, side);
                }
            }
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
        int maxBarPosGap = scale.toPixels(constants.maxBarPosGap);
        int maxBarCoordGap = scale.toPixels(constants.maxBarCoordGap);

        logger.info(
            "Checking S#" + prevSystem.getId() + "(" +
            prevSystem.getStaves().size() + ") - S#" + nextSystem.getId() +
            "(" + nextSystem.getStaves().size() + ")");

        StaffInfo prevStaff = prevSystem.getLastStaff();
        BarInfo   prevBar = prevStaff.getBar(LEFT); // Perhaps null

        if (prevBar == null) {
            logger.info("No left bar for system#" + prevSystem.getId());

            return false;
        }

        StaffInfo nextStaff = nextSystem.getFirstStaff();
        BarInfo   nextBar = nextStaff.getBar(LEFT); // Perhaps null

        if (nextBar == null) {
            logger.info("No left bar for system#" + nextSystem.getId());

            return false;
        }

        // Check vertical connections
        for (Stick prevStick : prevBar.getSticks()) {
            Point2D prevPoint = prevStick.getStopPoint();

            for (Stick nextStick : nextBar.getSticks()) {
                // Special case
                //                if (nextStick == prevStick) {
                //                    // Force the merge, find out the proper other stick
                //                    int dx = Integer.MAX_VALUE;
                //                    Stick other = null;
                //                    for (Stick p : prevBar.getSticks()) {
                //                        if (p != prevStick) {
                //                            
                //                        }
                //                    }
                //                    
                //                }
                Point2D nextPoint = nextStick.getStartPoint();

                // Check dx
                double dx = Math.abs(nextPoint.getX() - prevPoint.getX());

                // Check dy
                double prevY = prevStaff.getLastLine()
                                        .getEndPoint(LEFT)
                                        .getY();
                double nextY = nextStaff.getFirstLine()
                                        .getEndPoint(LEFT)
                                        .getY();
                double dy = Math.abs(
                    Math.min(nextY, nextPoint.getY()) -
                    Math.max(prevY, prevPoint.getY()));
                logger.info(
                    "F" + prevStick.getId() + "-F" + nextStick.getId() +
                    " dx:" + (float) dx + " dy:" + (float) dy);

                if ((dx <= maxBarPosGap) && (dy <= maxBarCoordGap)) {
                    logger.warning(
                        "Merging S#" + prevSystem.getId() + "(" +
                        prevSystem.getStaves().size() + ") - S#" +
                        nextSystem.getId() + "(" +
                        nextSystem.getStaves().size() + ")");

                    Filament pf = (Filament) prevStick;
                    Filament nf = (Filament) nextStick;
                    pf.include(nf);

                    return true;
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

    //--------------//
    // retrieveBars //
    //--------------//
    /**
     * Retrieve initial barline candidates
     * @return the collection of candidates barlines
     * @throws Exception
     */
    private void retrieveBars ()
        throws Exception
    {
        // Filaments factory
        FilamentsFactory factory = new FilamentsFactory(
            scale,
            vLag,
            Filament.class);

        //factory.setVipGlyphs(9, 2); // BINGO

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

        BarsChecker barsChecker = new BarsChecker(
            sheet,
            vLag,
            globalSlope,
            true);
        barsChecker.retrieveCandidates(filaments);

        bars = new ArrayList<Stick>();

        // Consider only sticks with a barline shape
        for (Glyph glyph : vLag.getActiveGlyphs()) {
            Shape shape = glyph.getShape();

            if ((shape == Shape.THICK_BARLINE) ||
                (shape == Shape.THIN_BARLINE)) {
                bars.add((Stick) glyph);
            }
        }
    }

    //-------------------//
    // retrieveStaffSide //
    //-------------------//
    /**
     * Determine the precise side of a given staff
     * @param staff the given staff
     * @param side proper horizontal side
     * @param staffSticks the ordered sequence of intersecting bar sticks
     * @param takeAllSticks false if focused on long sticks only
     * @return the retrieved bar, if any
     */
    private BarInfo retrieveStaffSide (StaffInfo      staff,
                                       HorizontalSide side,
                                       boolean        takeAllSticks)
    {
        List<StickX> staffSticks = barSticks.get(staff);
        final int    dir = (side == LEFT) ? 1 : (-1);
        final int    firstIdx = (dir > 0) ? 0 : (staffSticks.size() - 1);
        final int    breakIdx = (dir > 0) ? staffSticks.size() : (-1);

        double       staffX = staff.getAbscissa(side);
        final double xBreak = staffX + (dir * params.maxDistanceFromStaffSide);
        BarInfo      bar = null;
        Double       barX = null;

        // Reset
        staff.setBar(side, null);

        // Browsing bar sticks using 'dir' direction
        for (int i = firstIdx; i != breakIdx; i += dir) {
            StickX sx = staffSticks.get(i);
            double x = sx.x;

            if ((dir * (xBreak - x)) < 0) {
                break; // Speed up
            }

            if (!(isLongBar(sx.stick) || takeAllSticks)) {
                continue;
            }

            if (bar == null) {
                bar = new BarInfo(sx.stick);
                barX = x;
            } else {
                // Perhaps a pack of bars
                if (side == LEFT) {
                    if ((x - barX) <= params.maxLeftBarPackWidth) {
                        bar.appendStick(sx.stick);
                    }
                } else {
                    if ((barX - x) <= params.maxRightBarPackWidth) {
                        bar.prependStick(sx.stick);
                    }
                }
            }
        }

        if (bar != null) {
            Stick stick = bar.getStick(RIGHT);
            barX = stick.getPositionAt(staff.getMidOrdinate(side));

            if ((dir * (barX - staffX)) <= params.maxBarOffset) {
                staffX = barX;
                staff.setBar(side, bar);
            } else {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        side + " stick#" + stick.getId() +
                        " discarded for staff#" + staff.getId());
                }
            }
        } else {
            if (logger.isFineEnabled()) {
                logger.fine(
                    side + " no " + (takeAllSticks ? "" : "long") +
                    " bar for staff#" + staff.getId() + " " +
                    Glyphs.toString(StickX.sticksOf(staffSticks)));
            }
        }

        staff.setAbscissa(side, staffX);

        return bar;
    }

    //-------------------//
    // retrieveSystemBar //
    //-------------------//
    /**
     * Retrieve the left side of each system (& staff)
     * @param system the system to process
     */
    private void retrieveSystemBar (SystemFrame system)
    {
        // Sort staff-related sticks, using abscissa at staff intersection
        for (StaffInfo staff : system.getStaves()) {
            Collections.sort(barSticks.get(staff));
        }

        // 1st pass w/ long bars, 2nd pass w/ shorter bars if needed
        for (boolean takeAllSticks : new boolean[] { false, true }) {
            boolean hasLongBar = false;

            for (StaffInfo staff : system.getStaves()) {
                BarInfo bar = retrieveStaffSide(staff, LEFT, takeAllSticks);

                if ((bar != null) && isLongBar(bar.getStick(RIGHT))) {
                    hasLongBar = true;
                }
            }

            if (hasLongBar) {
                break;
            }
        }
    }

    //---------------------//
    // retrieveSystemSides //
    //---------------------//
    /**
     * Retrieve the left and right side of each system (& staff),
     * to adjust precise ending points of each staff line.
     * We need a precise point in x (from barline) and in y (from staff line).
     *
     * <p>Nota: We have to make sure that all staves of a given system exhibit
     * consistent sides, otherwise the dewarping will strongly degrade the
     * image.</p>
     *
     * @param system the system to process
     */
    private void retrieveSystemSides (SystemFrame system)
    {
        final List<StaffInfo> staves = system.getStaves();
        final int             staffCount = staves.size();

        // Sort sticks, using abscissa at staff intersection
        for (StaffInfo staff : staves) {
            Collections.sort(barSticks.get(staff));
        }

        for (HorizontalSide side : HorizontalSide.values()) {
            // 1st pass w/ long bars, 2nd pass w/ shorter bars if needed
            for (boolean takeAllSticks : new boolean[] { false, true }) {
                int longs = 0;
                int shorts = 0;

                for (StaffInfo staff : staves) {
                    BarInfo bar = retrieveStaffSide(staff, side, takeAllSticks);

                    if (bar != null) {
                        if (isLongBar(bar.getStick(RIGHT))) {
                            longs++;
                        } else {
                            shorts++;
                        }
                    }
                }

                // Check consistency of staves within the system
                if (longs > 0) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "System#" + system.getId() + " " + side +
                            " long bars: " + longs + "/" + staffCount);
                    }

                    // Align on long bars
                    adjustLongSystemSide(system, side);

                    break;
                } else if (takeAllSticks) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "System#" + system.getId() + " " + side +
                            " short bars: " + shorts + "/" + staffCount);
                    }

                    // Double-check system consistency
                    if (shorts > 0) {
                        // If sections go beyond bar, use line ends
                        adjustShortSystemSide(system, side);
                    } else {
                        // Use line ends directly (already done by default)
                    }
                }
            }
        }

        // Print out
        logger.info(sheet.getLogPrefix() + system);
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
        Collections.sort(bars, Stick.reverseLengthComparator);
        barSticks = new HashMap<StaffInfo, List<StickX>>();

        Integer[] tops = new Integer[staffManager.getStaffCount()];

        for (Stick stick : bars) {
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
                StaffInfo    staff = staffManager.getStaff(id - 1);
                List<StickX> staffSticks = barSticks.get(staff);

                if (staffSticks == null) {
                    staffSticks = new ArrayList<StickX>();
                    barSticks.put(staff, staffSticks);
                }

                Point2D inter = staff.intersection(stick);
                staffSticks.add(new StickX(inter.getX(), stick));

                ///if ((tops[id - 1] == null) || (top < tops[id - 1])) {
                if (tops[id - 1] == null) {
                    tops[id - 1] = top;
                }
            }
        }

        ///if (logger.isFineEnabled()) {
        logger.info("top staff ids: " + Arrays.toString(tops));

        ///}
        return tops;
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
            Integer[] tops = retrieveSystemTops();

            // Create system frames using staves tops
            systems = createSystems(tops);

            // Retrieve left bar of each system
            for (SystemFrame system : systems) {
                retrieveSystemBar(system);
            }

            // Smart checking of systems
            if (systemsModified()) {
                logger.info("Systems modified, rebuilding...");
            } else {
                break;
            }
        } while (true);
    }

    //-----------------//
    // staffHasLongBar //
    //-----------------//
    private boolean staffHasLongBar (StaffInfo      staff,
                                     HorizontalSide side)
    {
        BarInfo bar = staff.getBar(side);

        return (bar != null) && isLongBar(bar.getStick(RIGHT));
    }

    //-----------------//
    // systemsModified //
    //-----------------//
    private boolean systemsModified ()
    {
        boolean modified = false;

        // Consistency of system lengths
        // TBD

        // Check connection of left bars across systems
        for (int i = 1; i < systems.size(); i++) {
            if (canConnectSystems(systems.get(i - 1), systems.get(i))) {
                modified = true;
            }
        }

        return modified;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Ratio  maxLengthRatio = new Constant.Ratio(
            1.5,
            "Maximum ratio in length for a run to be combined with an existing section");

        // Constants specified WRT mean interline
        // --------------------------------------
        Scale.Fraction  maxSectionThickness = new Scale.Fraction(
            0.8,
            "Maximum horizontal section thickness WRT mean line height");
        Scale.Fraction  maxFilamentThickness = new Scale.Fraction(
            1.0,
            "Maximum filament thickness WRT mean line height");
        Scale.Fraction  maxOverlapDeltaPos = new Scale.Fraction(
            1.0,
            "Maximum delta position between two overlapping filaments");
        Scale.Fraction  maxCoordGap = new Scale.Fraction(
            0.5,
            "Maximum delta coordinate for a gap between filaments");
        Scale.Fraction  maxPosGap = new Scale.Fraction(
            0.3,
            "Maximum delta abscissa for a gap between filaments");
        Scale.Fraction  maxBarCoordGap = new Scale.Fraction(
            2, // 2.5
            "Maximum delta coordinate for a vertical gap between bars");
        Scale.Fraction  maxBarPosGap = new Scale.Fraction(
            0.2,
            "Maximum delta position for a vertical gap between bars");
        Scale.Fraction  minRunLength = new Scale.Fraction(
            1.5,
            "Minimum length for a vertical run to be considered");
        Scale.Fraction  minLongLength = new Scale.Fraction(
            8,
            "Minimum length for a long vertical bar");
        Scale.Fraction  maxDistanceFromStaffSide = new Scale.Fraction(
            4,
            "Max abscissa delta when looking for left or right side bars");
        Scale.Fraction  maxLeftBarPackWidth = new Scale.Fraction(
            1.5,
            "Max width of a pack of vertical barlines");
        Scale.Fraction  maxRightBarPackWidth = new Scale.Fraction(
            0.5,
            "Max width of a pack of vertical barlines");
        Scale.Fraction  maxBarOffset = new Scale.Fraction(
            4,
            "Max abscissa offset of a bar candidate within staff width");
        Scale.Fraction  maxSideDx = new Scale.Fraction(
            .5,
            "Max difference on theoretical bar abscissa");
        Scale.Fraction  maxLineExtension = new Scale.Fraction(
            .5,
            "Max extension of line beyond staff bar");

        // Constants for display
        //
        Constant.Double splineThickness = new Constant.Double(
            "thickness",
            0.5,
            "Stroke thickness to draw filaments curves");
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

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
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

            if (logger.isFineEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }

    //--------//
    // StickX //
    //--------//
    private static class StickX
        implements Comparable<StickX>
    {
        //~ Instance fields ----------------------------------------------------

        /** Abscissa where the stick intersects a staff */
        final double x;

        /** The (bar) stick */
        final Stick stick;

        //~ Constructors -------------------------------------------------------

        public StickX (double x,
                       Stick  stick)
        {
            this.x = x;
            this.stick = stick;
        }

        //~ Methods ------------------------------------------------------------

        /** For sorting sticks on abscissa, for a given staff */
        public int compareTo (StickX that)
        {
            return Double.compare(x, that.x);
        }

        /** Conversion to a sequence of sticks */
        public static List<Stick> sticksOf (Collection<StickX> sxs)
        {
            List<Stick> sticks = new ArrayList<Stick>();

            for (StickX sx : sxs) {
                sticks.add(sx.stick);
            }

            return sticks;
        }
    }
}
