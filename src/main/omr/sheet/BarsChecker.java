//----------------------------------------------------------------------------//
//                                                                            //
//                           B a r s C h e c k e r                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.ProcessingException;

import omr.check.Check;
import omr.check.CheckSuite;
import omr.check.Checkable;
import omr.check.FailureResult;
import omr.check.Result;
import omr.check.SuccessResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;

import omr.score.Barline;
import omr.score.Measure;
import omr.score.Score;
import omr.score.Staff;
import omr.score.System;
import omr.score.UnitRectangle;

import omr.stick.Stick;

import omr.util.Dumper;
import omr.util.Implement;
import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.Rectangle;
import java.util.*;

/**
 * Class <code>BarsChecker</code> is a (package private) companion of class
 * {@link BarsBuilder}, dedicated to physical checks.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
class BarsChecker
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants     constants = new Constants();
    private static final Logger        logger = Logger.getLogger(
        BarsChecker.class);

    /** Successful bar that embraces a whole system */
    private static final SuccessResult BAR_SYSTEM_DEFINING = new SuccessResult(
        "Bar-SystemDefining");

    /** Successful bar that embraces only part of a system */
    private static final SuccessResult BAR_NOT_SYSTEM_DEFINING = new SuccessResult(
        "Bar-NotSystemDefining");

    /** Failure, since bar is shorter than staff height */
    private static final FailureResult TOO_SHORT_BAR = new FailureResult(
        "Bar-TooShort");

    /** Failure, since bar is on left or right side of the staff area */
    private static final FailureResult OUTSIDE_STAFF_WIDTH = new FailureResult(
        "Bar-OutsideStaffWidth");

    /** Failure, since bar has no end aligned with a staff */
    private static final FailureResult NOT_STAFF_ANCHORED = new FailureResult(
        "Bar-NotStaffAnchored");

    /** Failure, since bar goes higher or lower than the system area */
    private static final FailureResult NOT_WITHIN_SYSTEM = new FailureResult(
        "Bar-NotWithinSystem");

    /** Failure, since bar has too many glyphs stuck on it */
    private static final FailureResult TOO_HIGH_ADJACENCY = new FailureResult(
        "Bar-TooHighAdjacency");

    /** Failure, since bar has a large chunk stuck at the top (a hote head?) */
    private static final FailureResult CHUNK_AT_TOP = new FailureResult(
        "Bar-ChunkAtTop");

    /** Failure, since bar has a large chunk stuck at the bottom (a hote head?) */
    private static final FailureResult CHUNK_AT_BOTTOM = new FailureResult(
        "Bar-ChunkAtBottom");

    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final Sheet sheet;

    /** Related scale */
    private final Scale scale;

    /** Related score */
    private final Score score;

    /** Suite of checks to be performed */
    private BarCheckSuite suite;

    /** List of found bar sticks */
    private List<Stick> bars;

    /** Vertical sticks, unused so far */
    private List<Stick> clutter;

    /** Retrieved systems */
    private List<SystemInfo> systems = new ArrayList<SystemInfo>();

    /** Related staff indices (top and bottom of a bar stick) */
    private Map<Stick, Context> contexts = new HashMap<Stick, Context>();

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // BarsChecker //
    //-------------//
    /**
     * Prepare a bar checker on the provided sheet
     *
     * @param sheet the sheet to process
     */
    public BarsChecker (Sheet sheet)
    {
        this.sheet = sheet;

        scale = sheet.getScale();
        score = sheet.getScore();
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // isStaffEmbraced //
    //-----------------//
    /**
     * Check whether the given staff is within the vertical range of the given
     * glyph (bar stick or brace glyph)
     *
     * @param staff the given staff
     * @param bar the given bar
     * @return true if staff is embraced by the bar
     */
    public boolean isStaffEmbraced (Staff staff,
                                    Glyph glyph)
    {
        // Extrema of glyph
        UnitRectangle box = scale.toUnits(glyph.getContourBox());
        int           top = box.y;
        int           bot = box.y + box.height;

        // Check that middle of staff is within bar top & bottom
        final int midStaff = staff.getTopLeft().y + (staff.getSize() / 2);

        return (midStaff > top) && (midStaff < bot);
    }

    //----------//
    // getSuite //
    //----------//
    /**
     * Report the suite currently defined
     *
     * @return the check suite
     */
    public BarCheckSuite getSuite ()
    {
        return suite;
    }

    //-------------//
    // getSystemOf //
    //-------------//
    /**
     * Report the SystemInfo that contains the given bar.
     *
     * @param bar the given bar
     * @param sheet the sheet context
     * @return the containing SystemInfo, null if not found
     */
    public SystemInfo getSystemOf (Stick bar,
                                   Sheet sheet)
    {
        Context context = contexts.get(bar);

        if (context == null) {
            return null;
        }

        int topIdx = context.topIdx;
        int botIdx = context.botIdx;

        if (topIdx == -1) {
            topIdx = botIdx;
        }

        if (botIdx == -1) {
            botIdx = topIdx;
        }

        Score score = sheet.getScore();

        if (score == null) {
            return null;
        }

        for (Iterator it = score.getSystems()
                                .iterator(); it.hasNext();) {
            System     system = (omr.score.System) it.next();
            SystemInfo systemInfo = system.getInfo();

            if ((systemInfo.getStartIdx() <= botIdx) &&
                (systemInfo.getStopIdx() >= topIdx)) {
                return systemInfo;
            }
        }

        // Not found
        return null;
    }

    //------------//
    // isThickBar //
    //------------//
    /**
     * Check if the stick/bar is a thick one
     *
     * @param stick the bar stick to check
     *
     * @return true if thick
     */
    public boolean isThickBar (Stick stick)
    {
        // Max width of a thin bar line, otherwise this must be a thick bar
        final int maxThinWidth = scale.toPixels(constants.maxThinWidth);

        // Average width of the stick
        final int meanWidth = (int) Math.rint(
            (double) stick.getWeight() / (double) stick.getLength());

        return meanWidth > maxThinWidth;
    }

    //------------------//
    // retrieveMeasures //
    //------------------//
    /**
     * Perform the sequence of physical checks to detect bar lines, then
     * systems, staves and measures.
     *
     * @param clutter the initial collection of vertical sticks
     * @param bars the resulting collection of bar sticks
     * @exception omr.ProcessingException raised if processing has been stoppped
     */
    public void retrieveMeasures (List<Stick> clutter,
                                  List<Stick> bars)
        throws omr.ProcessingException
    {
        // Cache parameters
        this.clutter = clutter;
        this.bars = bars;

        // Retrieve true bar lines and thus SystemInfos
        retrieveBarLines();

        // Build score Systems & Staves from SystemInfos
        buildSystemsAndStaves();

        // Build Measures
        buildMeasures();
    }

    //---------------//
    // buildMeasures //
    //---------------//
    /**
     * Bar lines are first sorted according to their abscissa, then we run
     * additional checks on each bar line, since we now know its enclosing
     * system. If OK, then we add a corresponding measure in each staff.
     */
    private void buildMeasures ()
    {
        final int maxDy = scale.toPixels(constants.maxBarOffset);

        // Sort bar lines by increasing abscissa
        Collections.sort(
            bars,
            new Comparator<Stick>() {
                    public int compare (Stick b1,
                                        Stick b2)
                    {
                        return b1.getMidPos() - b2.getMidPos();
                    }
                });

        // Measures building (Bars are already sorted by increasing abscissa)
        for (Iterator<Stick> bit = bars.iterator(); bit.hasNext();) {
            Stick      bar = bit.next();

            // Determine the system this bar line belongs to
            SystemInfo systemInfo = getSystemOf(bar, sheet);

            if (systemInfo == null) { // Should not occur, but that's safer
                logger.warning("Bar not belonging to any system");
                logger.fine("bar = " + bar);
                Dumper.dump(bar);

                continue;
            }

            omr.score.System system = systemInfo.getScoreSystem();

            // We don't check that the bar does not start before first staff,
            // this is too restrictive because of alternate endings.  We however
            // do check that the bar does not end after last staff of the
            // system.
            int barAbscissa = bar.getMidPos();
            int systemBottom = system.getLastStaff()
                                     .getInfo()
                                     .getLastLine()
                                     .getLine()
                                     .yAt(barAbscissa);

            if ((bar.getStop() - systemBottom) > maxDy) {
                if (logger.isFineEnabled()) {
                    logger.fine("Bar stopping too low");
                }

                bar.setResult(NOT_WITHIN_SYSTEM);
                bit.remove();

                continue;
            }

            // We add a measure in each staff of this system, provided that the
            // staff is embraced by the bar line
            for (TreeNode node : system.getStaves()) {
                Staff staff = (Staff) node;

                if (isStaffEmbraced(staff, bar)) {
                    if (logger.isFineEnabled()) {
                        logger.fine("Creating measure for bar-line " + bar);
                    }

                    Measure measure = new Measure(staff, false); // invented ?
                    Barline barline = new Barline(measure, staff, scale);
                    bar.setInterline(scale.interline()); // Safer
                    barline.addStick(bar);
                    measure.setBarline(barline);
                }
            }
        }
    }

    //------------------//
    // buildSystemInfos //
    //------------------//
    /**
     * Knowing the starting staff indice of each staff system, we are able to
     * allocate and describe the proper number of systems in the score.
     *
     * @param starts indexed by any staff, to give the staff index of the
     *               containing system. For a system with just one staff, both
     *               indices are equal. For a system of more than 1 staff, the
     *               indices differ.
     *
     * @throws omr.ProcessingException raised if processing failed
     */
    private void buildSystemInfos (int[] starts)
        throws omr.ProcessingException
    {
        int id = 0; // Id for created SystemInfo's
        int start = -1;

        for (int i = 0; i < starts.length; i++) {
            if (starts[i] != start) {
                if (start != -1) {
                    systems.add(
                        new SystemInfo(++id, sheet, start, starts[i] - 1));
                }

                start = i;
            }
        }

        systems.add(new SystemInfo(++id, sheet, start, starts.length - 1));

        if (logger.isFineEnabled()) {
            for (SystemInfo info : systems) {
                Dumper.dump(info);
            }
        }

        // Finally, store this list into the sheet instance
        sheet.setSystems(systems);
    }

    //-----------------------//
    // buildSystemsAndStaves //
    //-----------------------//
    /**
     * For each SystemInfo, build the corresponding System entity with all its
     * depending Staves
     */
    private void buildSystemsAndStaves ()
    {
        // Systems
        for (SystemInfo info : systems) {
            // Allocate the system
            omr.score.System system = new omr.score.System(
                info,
                score,
                scale.toPagePoint(
                    new PixelPoint(info.getLeft(), info.getTop())),
                scale.toUnits(
                    new PixelDimension(info.getWidth(), info.getDeltaY())));

            // Set the link SystemInfo -> System
            info.setScoreSystem(system);

            // Allocate the staves in this system
            int staffLink = 0;

            for (StaffInfo set : info.getStaves()) {
                LineInfo line = set.getFirstLine();
                new Staff(
                    set,
                    system,
                    scale.toPagePoint(
                        new PixelPoint(
                            set.getLeft(),
                            line.getLine().yAt(line.getLeft()))),
                    scale.pixelsToUnits(set.getRight() - set.getLeft()),
                    64, // Staff vertical size in units
                    staffLink++);
            }
        }
    }

    //-------------//
    // createSuite //
    //-------------//
    /**
     * Create a brand new check suite, with current value of constant parameters
     */
    private void createSuite ()
    {
        suite = new BarCheckSuite();
    }

    //------------------//
    // retrieveBarLines //
    //------------------//
    /**
     * From the list of vertical sticks, this method uses several tests based on
     * stick location, and stick shape (the test is based on adjacency, it
     * should be improved), to detect true bar lines.
     *
     * <p> The output is thus a filled 'bars' list of bar lines, and the list of
     * SystemInfos which describe the parameters of each system.
     *
     * @throws ProcessingException Raised when a sanity check on systems found
     *                             has failed
     */
    private void retrieveBarLines ()
        throws ProcessingException
    {
        // The list of candidate vertical sticks
        if (logger.isFineEnabled()) {
            logger.fine(clutter.size() + " sticks to check");
        }

        // A way to tell the System for each staff, by providing the staff index
        // of the starting staff of the containing system.
        int[] starts = new int[sheet.getStaves()
                                    .size()];

        for (int i = starts.length - 1; i >= 0; i--) {
            starts[i] = -1;
        }

        createSuite();

        double minResult = constants.minCheckResult.getValue();

        // Check each candidate stick in turn
        for (Stick stick : clutter) {
            // Allocate the candidate context, and pass the whole check suite
            Context context = new Context(stick);
            double  res = suite.pass(context);

            if (logger.isFineEnabled()) {
                logger.fine("suite => " + res + " for " + stick);
            }

            if (res >= minResult) {
                // OK, we insert this candidate stick as a true bars member.
                contexts.put(stick, context);
                bars.add(stick);

                // Bars that define a system (they start AND end with staves
                // limits)
                if ((context.topIdx != -1) && (context.botIdx != -1)) {
                    for (int i = context.topIdx; i <= context.botIdx; i++) {
                        if (starts[i] == -1) {
                            starts[i] = context.topIdx;
                        }
                    }

                    stick.setResult(BAR_SYSTEM_DEFINING);

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "System-defining Bar line from staff " +
                            context.topIdx + " to staff " + context.botIdx +
                            " " + stick);
                    }
                } else {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Non-System-defining Bar line " +
                            ((context.topIdx != -1)
                             ? (" topIdx=" + context.topIdx) : "") +
                            ((context.botIdx != -1)
                             ? (" botIdx=" + context.botIdx) : ""));
                    }

                    stick.setResult(BAR_NOT_SYSTEM_DEFINING);
                }
            }
        }

        // Sanity check on the systems found
        for (int i = 0; i < starts.length; i++) {
            if (logger.isFineEnabled()) {
                logger.fine("staff " + i + " system " + starts[i]);
            }

            if (starts[i] == -1) {
                logger.warning("No system found for staff " + i);
                throw new ProcessingException();
            }
        }

        // System retrieval
        buildSystemInfos(starts);
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // Context //
    //---------//
    static class Context
        implements Checkable
    {
        Stick   stick;
        boolean isThick;
        int     botIdx = -1;
        int     bottomArea = -1;
        int     topArea = -1;
        int     topIdx = -1;

        public Context (Stick stick)
        {
            this.stick = stick;
        }

        @Implement(Checkable.class)
        public void setResult (Result result)
        {
            stick.setResult(result);
        }
    }

    //---------------//
    // BarCheckSuite //
    //---------------//
    class BarCheckSuite
        extends CheckSuite<Context>
    {
        public BarCheckSuite ()
        {
            super("Bar", constants.minCheckResult.getValue());

            // Be very careful with check order, because of side-effects
            add(1, new TopCheck());
            add(1, new BottomCheck());
            add(1, new MinLengthCheck());
            add(1, new AnchorCheck());
            add(1, new LeftCheck());
            add(1, new RightCheck());
            add(1, new TopChunkCheck());
            add(1, new BottomChunkCheck());
            add(1, new LeftAdjacencyCheck());
            add(1, new RightAdjacencyCheck());

            if (logger.isFineEnabled()) {
                dump();
            }
        }
    }

    //-------------//
    // AnchorCheck //
    //-------------//
    private class AnchorCheck
        extends Check<Context>
    {
        protected AnchorCheck ()
        {
            super(
                "Anchor",
                "Check that thick bars are top and bottom aligned with staff",
                0.5,
                0.5,
                true,
                NOT_STAFF_ANCHORED);
        }

        // Make sure that at least top or bottom are staff anchors, and that
        // both are staff anchors in the case of thick bars.
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            context.isThick = isThickBar(stick);

            if (context.isThick) {
                if ((context.topIdx != -1) && (context.botIdx != -1)) {
                    return 1;
                }
            } else {
                if ((context.topIdx != -1) || (context.botIdx != -1)) {
                    return 1;
                }
            }

            return 0;
        }
    }

    //-------------//
    // BottomCheck //
    //-------------//
    private class BottomCheck
        extends Check<Context>
    {
        protected BottomCheck ()
        {
            super(
                "Bottom",
                "Check that bottom of stick is close to bottom of staff" +
                " (unit is interline)",
                constants.maxStaveshiftDyLow.getValue(),
                constants.maxStaveshiftDyHigh.getValue(),
                false,
                null);
        }

        // Retrieve the distance with proper staff border
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int   stop = stick.getStop();

            // Which staff area contains the bottom of the stick?
            context.bottomArea = sheet.getStaffIndexAtY(stop);

            StaffInfo area = sheet.getStaves()
                                  .get(context.bottomArea);

            // How far are we from the stop of the staff?
            int    staffBottom = area.getLastLine()
                                     .getLine()
                                     .yAt(stick.getMidPos());

            double dy = sheet.getScale()
                             .pixelsToFrac(Math.abs(staffBottom - stop));

            // Side-effect
            if (dy <= getLow()) {
                context.botIdx = context.bottomArea;
            }

            return dy;
        }
    }

    //------------------//
    // BottomChunkCheck //
    //------------------//
    /**
     * Class <code>BottomChunkCheck</code> checks for lack of chunk at bottom
     */
    private class BottomChunkCheck
        extends Check<Context>
    {
        private final int nHeight;

        // Half-dimensions for window at bottom, checking for chunks
        private final int nWidth;

        protected BottomChunkCheck ()
        {
            super(
                "BotChunk",
                "Check there is no big chunck stuck on bottom of stick" +
                " (unit is interline squared)",
                0,
                0,
                false,
                CHUNK_AT_BOTTOM);

            // Adjust chunk window according to system scale (problem, we have
            // sheet scale and staff scale, not system scale...)
            Scale scale = sheet.getScale();
            nWidth = scale.toPixels(constants.chunkWidth);
            nHeight = scale.toPixels(constants.chunkHeight);

            int area = 4 * nWidth * nHeight;
            setLowHigh(
                area * constants.chunkRatioLow.getValue(),
                area * constants.chunkRatioHigh.getValue());
        }

        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;

            // Retrieve the stick chunk at bottom
            return stick.getAliensAtStop(nHeight, nWidth);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        Scale.Fraction  chunkHeight = new Scale.Fraction(
            0.33,
            "Height of half area to look for chunks");
        Constant.Double chunkRatioHigh = new Constant.Double(
            0.25,
            "HighMinimum ratio of alien pixels to detect chunks");
        Constant.Double chunkRatioLow = new Constant.Double(
            0.25,
            "LowMinimum ratio of alien pixels to detect chunks");
        Scale.Fraction  chunkWidth = new Scale.Fraction(
            0.33,
            "Width of half area to look for chunks");
        Constant.Double maxAdjacencyHigh = new Constant.Double(
            0.25d,
            "HighMaximum adjacency ratio for a bar stick");
        Constant.Double maxAdjacencyLow = new Constant.Double(
            0.25d,
            "LowMaximum adjacency ratio for a bar stick");
        Scale.Fraction  maxBarOffset = new Scale.Fraction(
            1.0,
            "Vertical offset used to detect that a bar extends past a staff");
        Scale.Fraction  maxStaveshiftDyHigh = new Scale.Fraction(
            10,
            "HighMaximum vertical distance between a bar edge and the staff line");
        Scale.Fraction  maxStaveshiftDyLow = new Scale.Fraction(
            0.125,
            "LowMaximum vertical distance between a bar edge and the staff line");
        Scale.Fraction  maxThinWidth = new Scale.Fraction(
            0.3,
            "Maximum width of a normal bar, versus a thick bar");
        Constant.Double minCheckResult = new Constant.Double(
            0.50,
            "Minimum result for suite of check");

        Constants ()
        {
            initialize();
        }
    }

    //--------------------//
    // LeftAdjacencyCheck //
    //--------------------//
    private static class LeftAdjacencyCheck
        extends Check<Context>
    {
        protected LeftAdjacencyCheck ()
        {
            super(
                "LeftAdj",
                "Check that left side of the stick is open enough" +
                " (dimension-less)",
                constants.maxAdjacencyLow.getValue(),
                constants.maxAdjacencyHigh.getValue(),
                false,
                TOO_HIGH_ADJACENCY);
        }

        // Retrieve the adjacency value
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int   length = stick.getLength();

            return (double) stick.getFirstStuck() / (double) length;
        }
    }

    //-----------//
    // LeftCheck //
    //-----------//
    private class LeftCheck
        extends Check<Context>
    {
        protected LeftCheck ()
        {
            super(
                "Left",
                "Check that stick is on the right of staff beginning bar" +
                " (diff is in interline unit)",
                0,
                0,
                true,
                OUTSIDE_STAFF_WIDTH);
        }

        // Retrieve the stick abscissa
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int   x = stick.getMidPos();
            int   dist = Integer.MAX_VALUE;

            // Check wrt every staff in the stick range
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaffInfo area = sheet.getStaves()
                                      .get(i);
                dist = Math.min(dist, x - area.getLeft());
            }

            return sheet.getScale()
                        .pixelsToFrac(dist);
        }
    }

    //----------------//
    // MinLengthCheck //
    //----------------//
    private class MinLengthCheck
        extends Check<Context>
    {
        protected MinLengthCheck ()
        {
            super(
                "MinLength",
                "Check that stick is as long as staff height" +
                " (diff is in interline unit)",
                -constants.maxStaveshiftDyLow.getValue(),
                0,
                true,
                TOO_SHORT_BAR);
        }

        // Retrieve the length data
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int   x = stick.getMidPos();
            int   height = Integer.MAX_VALUE;

            // Check wrt every staff in the stick range
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaffInfo area = sheet.getStaves()
                                      .get(i);
                height = Math.min(height, area.getHeight());
            }

            return sheet.getScale()
                        .pixelsToFrac(stick.getLength() - height);
        }
    }

    //---------------------//
    // RightAdjacencyCheck //
    //---------------------//
    private static class RightAdjacencyCheck
        extends Check<Context>
    {
        protected RightAdjacencyCheck ()
        {
            super(
                "RightAdj",
                "Check that right side of the stick is open enough" +
                " (dimension-less)",
                constants.maxAdjacencyLow.getValue(),
                constants.maxAdjacencyHigh.getValue(),
                false,
                TOO_HIGH_ADJACENCY);
        }

        // Retrieve the adjacency value
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int   length = stick.getLength();

            return (double) stick.getLastStuck() / (double) length;
        }
    }

    //------------//
    // RightCheck //
    //------------//
    private class RightCheck
        extends Check<Context>
    {
        protected RightCheck ()
        {
            super(
                "Right",
                "Check that stick is on the left of staff ending bar" +
                " (diff is in interline unit)",
                0,
                0,
                true,
                OUTSIDE_STAFF_WIDTH);
        }

        // Retrieve the stick abscissa
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int   x = stick.getMidPos();
            int   dist = Integer.MAX_VALUE;

            // Check wrt every staff in the stick range
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaffInfo area = sheet.getStaves()
                                      .get(i);
                dist = Math.min(dist, area.getRight() - x);
            }

            return sheet.getScale()
                        .pixelsToFrac(dist);
        }
    }

    //----------//
    // TopCheck //
    //----------//
    private class TopCheck
        extends Check<Context>
    {
        protected TopCheck ()
        {
            super(
                "Top",
                "Check that top of stick is close to top of staff" +
                " (unit is interline)",
                constants.maxStaveshiftDyLow.getValue(),
                constants.maxStaveshiftDyHigh.getValue(),
                false,
                null);
        }

        // Retrieve the distance with proper staff border
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int   start = stick.getStart();

            // Which staff area contains the top of the stick?
            context.topArea = sheet.getStaffIndexAtY(start);

            StaffInfo area = sheet.getStaves()
                                  .get(context.topArea);

            // How far are we from the start of the staff?
            int    staffTop = area.getFirstLine()
                                  .getLine()
                                  .yAt(stick.getMidPos());

            double dy = sheet.getScale()
                             .pixelsToFrac(Math.abs(staffTop - start));

            // Side-effect
            if (dy <= getLow()) {
                context.topIdx = context.topArea;
            }

            return dy;
        }
    }

    //---------------//
    // TopChunkCheck //
    //---------------//
    /**
     * Class <code>TopChunkCheck</code> checks for lack of chunk at top
     */
    private class TopChunkCheck
        extends Check<Context>
    {
        private final int nHeight;

        // Half-dimensions for window at top, checking for chunks
        private final int nWidth;

        protected TopChunkCheck ()
        {
            super(
                "TopChunk",
                "Check there is no big chunck stuck on top of stick" +
                " (unit is interline squared)",
                0,
                0,
                false,
                CHUNK_AT_TOP);

            // Adjust chunk window according to system scale (problem, we have
            // sheet scale and staff scale, not system scale...)
            Scale scale = sheet.getScale();
            nWidth = scale.toPixels(constants.chunkWidth);
            nHeight = scale.toPixels(constants.chunkHeight);

            int area = 4 * nWidth * nHeight;
            setLowHigh(
                area * constants.chunkRatioLow.getValue(),
                area * constants.chunkRatioHigh.getValue());
        }

        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;

            // Retrieve the stick chunk at top
            return stick.getAliensAtStart(nHeight, nWidth);
        }
    }
}
