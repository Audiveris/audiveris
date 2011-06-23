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
import omr.run.Run;
import omr.run.RunsTable;

import omr.score.common.PixelPoint;
import omr.score.ui.PagePainter;

import omr.sheet.BarsChecker;
import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.step.StepException;

import omr.stick.Filament;
import omr.stick.FilamentsFactory;
import omr.stick.StickSection;
import omr.stick.SticksSource;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Predicate;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;

/**
 * Class <code>BarsRetriever</code> focuses on the retrieval of vertical bars
 * to determine the horizontal limits of staves and the gathering of staves
 * into system frames.
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

    /** Sequence of systems */
    private List<SystemFrame> systems;

    /** Related staff manager */
    private final StaffManager staffManager;

    /** Companion in charge of physical bar checking */
    private BarsChecker barsChecker;

    /** Collection of bar sticks that intersect a staff */
    private final Map<StaffInfo, List<StickX>> barSticks = new HashMap<StaffInfo, List<StickX>>();

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
            // Filaments factory
            FilamentsFactory factory = new FilamentsFactory(
                scale,
                vLag,
                Filament.class,
                null);

            // Factory parameters adjustment
            factory.setMaxSectionThickness(constants.maxSectionThickness);
            factory.setMaxFilamentThickness(constants.maxFilamentThickness);
            factory.setMaxCoordGap(constants.maxCoordGap);
            factory.dump();

            // Create filaments out of vertical sections
            for (Filament fil : factory.retrieveFilaments(
                new SticksSource(vLag.getVertices()))) {
                filaments.add(fil);
            }

            // Retrieve barline candidates
            barsChecker = new BarsChecker(sheet, vLag, -globalSlope, true); // Rough=true
            barsChecker.retrieveCandidates(filaments);

            // Connect bars and staves into systems
            crossConnect();
        } catch (Exception ex) {
            logger.warning("BarsRetriever cannot buildInfo", ex);
        }
    }

    //----------//
    // buildLag //
    //----------//
    /**
     * Build the underlying lag, out of the provided runs table.
     * This method must be called before building info.
     * @param wholeVertTable the provided table of runs
     * @param showRuns true to create views on runs
     */
    public void buildLag (RunsTable wholeVertTable,
                          boolean   showRuns)
    {
        vLag = new GlyphLag("vLag", StickSection.class, Orientation.VERTICAL);

        RunsTable longVertTable = wholeVertTable.clone("long-vert")
                                                .purge(
            new Predicate<Run>() {
                    public final boolean check (Run run)
                    {
                        return run.getLength() < params.minRunLength;
                    }
                });

        if (showRuns) {
            // Add a view on runs table
            sheet.getAssembly()
                 .addRunsTab(longVertTable);
        }

        SectionsBuilder sectionsBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
            vLag,
            new JunctionRatioPolicy(params.maxLengthRatio));
        sectionsBuilder.createSections(longVertTable);
    }

    //-----------------//
    // staffHasLongBar //
    //-----------------//
    public boolean staffHasLongBar (StaffInfo      staff,
                                    HorizontalSide side)
    {
        BarInfo bar = staff.getBar(side);

        return (bar != null) && isLongBar(bar.getStick(RIGHT));
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

        // Draw tangent at each ending point
        g.setColor(Color.BLACK);

        double dy = sheet.getScale()
                         .toPixels(constants.tangentLg);

        for (Filament filament : filaments) {
            PixelPoint p = filament.getStartPoint();
            double     der = filament.slopeAt(p.y);
            g.draw(new Line2D.Double(p.x, p.y, p.x - (der * dy), p.y - dy));
            p = filament.getStopPoint();
            der = filament.slopeAt(p.y);
            g.draw(new Line2D.Double(p.x, p.y, p.x + (der * dy), p.y + dy));
        }

        g.setStroke(oldStroke);
    }

    //---------//
    // getBars //
    //---------//
    private List<Stick> getBars ()
    {
        List<Stick> bars = new ArrayList<Stick>();

        for (Glyph glyph : vLag.getActiveGlyphs()) {
            Shape shape = glyph.getShape();

            if ((shape == Shape.THICK_BARLINE) ||
                (shape == Shape.THIN_BARLINE)) {
                bars.add((Stick) glyph);
            }
        }

        return bars;
    }

    //--------------//
    // getBestStick //
    //--------------//
    private StickX getBestStick (StaffInfo staff,
                                 int       x,
                                 int       maxDx)
    {
        StickX best = null;
        int    bestDx = Integer.MAX_VALUE;

        for (StickX sx : barSticks.get(staff)) {
            int dx = Math.abs(x - sx.x);

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
    private PixelPoint getLineEnding (StaffInfo      staff,
                                      FilamentLine   line,
                                      HorizontalSide side)
    {
        double     slope = staff.getEndingSlope(side);
        Stick      stick = (staff.getBar(side) == null) ? null
                           : staff.getBar(side)
                                  .getStick(RIGHT);
        PixelPoint linePt = line.getEndPoint(side);
        int        staffX = staff.getAbscissa(side);
        double     y = linePt.y - ((linePt.x - staffX) * slope);
        double     x = (stick == null) ? staffX : stick.getAbsoluteLine()
                                                       .xAt(y);

        return new PixelPoint((int) Math.rint(x), (int) Math.rint(y));
    }

    //----------------------//
    // adjustLongSystemSide //
    //----------------------//
    /**
     * Adjust the precise side of a system, for which we have some staves with
     * long reliable bar (and other staves without such bars)
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

            int staffY = staff.getMidOrdinate(side);
            int staffX;

            if ((prevLong != null) && (nextLong != null)) {
                // Interpolate
                PixelPoint prev = prevLong.intersection(
                    prevLong.getBar(side).getStick(RIGHT));
                PixelPoint next = nextLong.intersection(
                    nextLong.getBar(side).getStick(RIGHT));
                staffX = prev.x +
                         ((staffY - prev.y) * ((next.x - prev.x) / (next.y -
                                                                   prev.y)));
            } else {
                // Extrapolate using global slope
                PixelPoint pt = (prevLong != null)
                                ? prevLong.intersection(
                    prevLong.getBar(side).getStick(RIGHT))
                                : nextLong.intersection(
                    nextLong.getBar(side).getStick(RIGHT));
                staffX = pt.x -
                         (int) Math.rint(((staffY - pt.y) * globalSlope));
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
     * Adjust the precise side of a system, for which we have some staves with
     * short (unreliable) bar
     * @param system the system to process
     * @param side the desired side
     */
    private void adjustShortSystemSide (SystemFrame    system,
                                        HorizontalSide side)
    {
        final int       dir = (side == LEFT) ? 1 : (-1);
        List<StaffInfo> staves = system.getStaves();

        for (int idx = 0; idx < staves.size(); idx++) {
            StaffInfo staff = staves.get(idx);

            // Check that staff bar, if any, is not passed by lines
            BarInfo bar = staff.getBar(side);

            if (bar != null) {
                int linesX = staff.getLinesEnd(side);
                int barX = staff.intersection(bar.getStick(RIGHT)).x;

                if ((dir * (barX - linesX)) > params.maxLineExtension) {
                    staff.setBar(side, null);
                    staff.setAbscissa(side, linesX);

                    if (logger.isFineEnabled()) {
                        logger.info(side + " extended " + staff);
                    }
                }
            }
        }
    }

    //---------------//
    // createSystems //
    //---------------//
    /**
     * Build the frame of each system
     * @param tops the starting staff for each system
     * @return the sequence of system physical frames
     */
    private List<SystemFrame> createSystems (Integer[] tops)
    {
        List<SystemFrame> systems = new ArrayList<SystemFrame>();
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
                systems.add(systemFrame);
            } else {
                // Continuing current system
                systemFrame.setStaves(
                    staffManager.getRange(systemFrame.getFirstStaff(), staff));
            }

            if (logger.isFineEnabled()) {
                logger.fine("i:" + i + " tops:" + tops[i]);
            }
        }

        return systems;
    }

    //--------------//
    // crossConnect //
    //--------------//
    /**
     * Connect bars and staves, using the skeletons of vertical glyphs (bars)
     * and non-finished staves.
     * - Candidate bars are available as vertical glyphs.
     * - Staves are available through the StaffManager.
     *
     */
    private void crossConnect ()
    {
        // Each bar can connect several staves as a system (or just a part)
        List<Stick> bars = getBars();

        // Retrieve the staves that start systems
        Integer[] tops = retrieveSystemTops(bars);

        // Create system frames
        systems = createSystems(tops);

        // Retrieve precise left and right sides of each system / staff
        for (SystemFrame system : systems) {
            retrieveSystemSides(system);
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

        int          staffX = staff.getAbscissa(side);
        BarInfo      bar = null;
        Integer      barX = null;
        final int    xBreak = staffX + (dir * params.maxDistanceFromStaffSide);

        // Browsing bar sticks using 'dir' direction
        for (int i = firstIdx; i != breakIdx; i += dir) {
            StickX sx = staffSticks.get(i);
            int    x = sx.x;

            if ((dir * (xBreak - x)) < 0) {
                break; // Speed up
            }

            if (isLongBar(sx.stick)) {
                // Very good
            } else if (takeAllSticks) {
                // Be very careful with short sticks
            } else {
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
            barX = stick.getAbsoluteLine()
                        .xAt(staff.getMidOrdinate(side));

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
                    side + " no  bar for staff#" + staff.getId() + " " +
                    Glyphs.toString(StickX.sticksOf(staffSticks)));
            }
        }

        staff.setAbscissa(side, staffX);

        return bar;
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

        final boolean[] options = new boolean[] { false, true };

        for (HorizontalSide side : HorizontalSide.values()) {
            // 1st pass w/ long bars, 2nd pass w/ shorter bars if needed
            for (boolean takeAllSticks : options) {
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

        for (StaffInfo staff : staves) {
            if (logger.isFineEnabled()) {
                logger.info(
                    "staff#" + staff.getId() + " left:" +
                    staff.getAbscissa(LEFT) + " leftBar:" + staff.getBar(LEFT) +
                    " right:" + staff.getAbscissa(RIGHT) + " rightBar:" +
                    staff.getBar(RIGHT));
            }

            // Adjust left and right endings of each line in the staff
            for (LineInfo l : staff.getLines()) {
                FilamentLine line = (FilamentLine) l;
                line.setEndingPoints(
                    getLineEnding(staff, line, LEFT),
                    getLineEnding(staff, line, RIGHT));
            }
        }

        system.getLeftBar();
    }

    //--------------------//
    // retrieveSystemTops //
    //--------------------//
    /**
     * Retrieve for each staff the staff that starts its containing system
     * @param bars (input) the collection of bar candidates
     * @return the (index of) system starting staff for each staff
     */
    private Integer[] retrieveSystemTops (List<Stick> bars)
    {
        Collections.sort(bars, Stick.reverseLengthComparator);

        Integer[] tops = new Integer[staffManager.getStaffCount()];

        for (Stick stick : bars) {
            PixelPoint start = stick.getStartPoint();
            StaffInfo  topStaff = staffManager.getStaffAt(start);
            PixelPoint stop = stick.getStopPoint();
            StaffInfo  botStaff = staffManager.getStaffAt(stop);

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Bar#" + stick.getId() + " top:" + topStaff.getId() +
                    " bot:" + botStaff.getId());
            }

            int top = topStaff.getId() - 1;
            int bot = botStaff.getId() - 1;

            for (int i = top; i <= bot; i++) {
                StaffInfo    staff = staffManager.getStaff(i);
                List<StickX> staffSticks = barSticks.get(staff);

                if (staffSticks == null) {
                    staffSticks = new ArrayList<StickX>();
                    barSticks.put(staff, staffSticks);
                }

                PixelPoint inter = staff.intersection(stick);
                staffSticks.add(new StickX(inter.x, stick));

                if ((tops[i] == null) || (top < tops[i])) {
                    tops[i] = top;
                }
            }
        }

        if (logger.isFineEnabled()) {
            logger.info("tops:" + Arrays.toString(tops));
        }

        return tops;
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
            0.8,
            "Maximum filament thickness WRT mean line height");
        Scale.Fraction  maxCoordGap = new Scale.Fraction(
            0.5,
            "Maximum delta coordinate for a gap between filaments");
        Scale.Fraction  minRunLength = new Scale.Fraction(
            1.5, // 1.5,
            "Minimum length for a vertical run to be considered");
        Scale.Fraction  minLongLength = new Scale.Fraction(
            8,
            "Minimum length for a long vertical bar");
        Scale.Fraction  maxDistanceFromStaffSide = new Scale.Fraction(
            3, // 2
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
        Scale.Fraction  tangentLg = new Scale.Fraction(
            1,
            "Typical length to display tangents at ending points");
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
        final int x;

        /** The (bar) stick */
        final Stick stick;

        //~ Constructors -------------------------------------------------------

        public StickX (int   x,
                       Stick stick)
        {
            this.x = x;
            this.stick = stick;
        }

        //~ Methods ------------------------------------------------------------

        /** For sorting sticks on abscissa, for a given staff */
        public int compareTo (StickX that)
        {
            return x - that.x;
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
