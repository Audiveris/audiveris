//----------------------------------------------------------------------------//
//                                                                            //
//                           B a r s C h e c k e r                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.score.common.PixelDimension;
import omr.score.common.PixelPoint;
import omr.check.Check;
import omr.check.CheckSuite;
import omr.check.Checkable;
import omr.check.FailureResult;
import omr.check.Result;
import omr.check.SuccessResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.Score;
import omr.score.common.PageRectangle;
import omr.score.entity.Barline;
import omr.score.entity.Measure;
import omr.score.entity.Staff;
import omr.score.entity.System;
import omr.score.entity.SystemPart;

import omr.step.StepException;

import omr.stick.Stick;

import omr.util.Dumper;
import omr.util.Implement;
import omr.util.Logger;
import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>BarsChecker</code> is a (package private) companion of class
 * {@link BarsBuilder}, dedicated to physical checks.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class BarsChecker
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BarsChecker.class);

    /** Successful bar that embraces a whole system */
    private static final SuccessResult BAR_PART_DEFINING = new SuccessResult(
        "Bar-PartDefining");

    /** Successful bar that embraces only part of a part */
    private static final SuccessResult BAR_NOT_PART_DEFINING = new SuccessResult(
        "Bar-NotPartDefining");

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

    //----------------//
    // isPartEmbraced //
    //----------------//
    /**
     * Check whether the given part is within the vertical range of the given
     * glyph (bar stick or brace glyph)
     *
     * @param part the given part
     * @param glyph the given glyph
     * @return true if part is embraced by the bar
     */
    public boolean isPartEmbraced (SystemPart part,
                                   Glyph      glyph)
    {
        // Extrema of glyph
        PageRectangle box = scale.toUnits(glyph.getContourBox());
        int           top = box.y;
        int           bot = box.y + box.height;

        // Check that part and glyph overlap vertically
        final int topPart = part.getFirstStaff()
                                .getTopLeft().y;
        final int botPart = part.getLastStaff()
                                .getTopLeft().y +
                            part.getLastStaff()
                                .getHeight();

        return Math.max(topPart, top) < Math.min(botPart, bot);
    }

    //    //-----------------//
    //    // isStaffEmbraced //
    //    //-----------------//
    //    /**
    //     * Check whether the given staff is within the vertical range of the given
    //     * glyph (bar stick or brace glyph)
    //     *
    //     * @param staff the given staff
    //     * @param glyph the given glyph
    //     * @return true if staff is embraced by the bar
    //     */
    //    public boolean isStaffEmbraced (Staff staff,
    //                                    Glyph glyph)
    //    {
    //        // Extrema of glyph
    //        PageRectangle box = scale.toUnits(glyph.getContourBox());
    //        int           top = box.y;
    //        int           bot = box.y + box.height;
    //
    //        // Check that middle of staff is within bar top & bottom
    //        final int midStaff = staff.getTopLeft().y + (staff.getHeight() / 2);
    //
    //        return (midStaff > top) && (midStaff < bot);
    //    }

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
            System     system = (omr.score.entity.System) it.next();
            SystemInfo systemInfo = system.getInfo();

            if ((systemInfo.getStartIdx() <= botIdx) &&
                (systemInfo.getStopIdx() >= topIdx)) {
                return systemInfo;
            }
        }

        // Not found
        return null;
    }

    //------------------//
    // retrieveMeasures //
    //------------------//
    /**
     * Perform the sequence of physical checks to detect bar lines, then
     * systems, parts, staves and measures.
     *
     * @param clutter the initial collection of vertical sticks, where
     *                recognized bar sticks are removed
     * @param bars the resulting collection of bar sticks
     * @exception StepException raised if processing has been stoppped
     */
    public void retrieveMeasures (List<Stick> clutter,
                                  List<Stick> bars)
        throws StepException
    {
        // Cache parameters
        this.clutter = clutter;
        this.bars = bars;

        // Retrieve true bar lines and thus SystemInfos
        retrieveBarLines();

        // Build score Systems, Parts & Staves from SystemInfos
        buildScoreSystemsAndStaves();

        // Build Measures
        buildMeasures();
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
    private boolean isThickBar (Stick stick)
    {
        // Max width of a thin bar line, otherwise this must be a thick bar
        final int maxThinWidth = scale.toPixels(constants.maxThinWidth);

        // Average width of the stick
        final int meanWidth = (int) Math.rint(
            (double) stick.getWeight() / (double) stick.getLength());

        return meanWidth > maxThinWidth;
    }

    //---------------//
    // buildMeasures //
    //---------------//
    /**
     * Bar lines are first sorted according to their abscissa, then we run
     * additional checks on each bar line, since we now know its enclosing
     * system. If OK, then we add the corresponding measures in their parts.
     */
    private void buildMeasures ()
    {
        final int maxDy = scale.toPixels(constants.maxBarOffset);

        // Sort bar sticks by increasing abscissa
        Collections.sort(
            bars,
            new Comparator<Stick>() {
                    public int compare (Stick b1,
                                        Stick b2)
                    {
                        return b1.getMidPos() - b2.getMidPos();
                    }
                });

        // Measures building (Sticks are already sorted by increasing abscissa)
        for (Iterator<Stick> bit = bars.iterator(); bit.hasNext();) {
            Stick bar = bit.next();

            // Determine the system this bar line belongs to
            SystemInfo systemInfo = getSystemOf(bar, sheet);

            if (systemInfo == null) { // Should not occur, but that's safer
                logger.warning("Bar not belonging to any system");

                continue;
            }

            omr.score.entity.System system = systemInfo.getScoreSystem();

            // We don't check that the bar does not start before first staff,
            // this is too restrictive because of alternate endings.  We however
            // do check that the bar does not end after last staff of the
            // last part of the system.
            int barAbscissa = bar.getMidPos();
            int systemBottom = system.getLastPart()
                                     .getLastStaff()
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

            // We add a measure in each relevant part of this system, provided
            // that the part is embraced by the bar line
            for (TreeNode node : system.getParts()) {
                SystemPart part = (SystemPart) node;

                if (isPartEmbraced(part, bar)) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            part + " - Creating measure for bar-line " + bar);
                    }

                    Measure measure = new Measure(part);
                    Barline barline = new Barline(measure);
                    bar.setShape(
                        isThickBar(bar) ? Shape.THICK_BAR_LINE
                                                : Shape.THIN_BAR_LINE);
                    barline.addStick(bar);
                }
            }
        }
    }

    //----------------------------//
    // buildScoreSystemsAndStaves //
    //----------------------------//
    /**
     * For each SystemInfo, build the corresponding System entity with all its
     * depending Parts and Staves
     */
    private void buildScoreSystemsAndStaves ()
    {
        // Systems
        for (SystemInfo info : systems) {
            // Allocate the system
            omr.score.entity.System system = new omr.score.entity.System(
                info,
                score,
                scale.toPagePoint(
                    new PixelPoint(info.getLeft(), info.getTop())),
                scale.toUnits(
                    new PixelDimension(info.getWidth(), info.getDeltaY())));

            // Set the link SystemInfo -> System
            info.setScoreSystem(system);

            // Allocate the parts in the system
            for (PartInfo partInfo : info.getParts()) {
                SystemPart part = new SystemPart(system);

                // Allocate the staves in this part
                for (StaffInfo staffInfo : partInfo.getStaves()) {
                    LineInfo line = staffInfo.getFirstLine();
                    new Staff(
                        staffInfo,
                        part,
                        scale.toPagePoint(
                            new PixelPoint(
                                staffInfo.getLeft(),
                                line.getLine().yAt(line.getLeft()))),
                        scale.pixelsToUnits(
                            staffInfo.getRight() - staffInfo.getLeft()),
                        64); // Staff vertical size in units);
                }
            }
        }
    }

    //----------------------//
    // buildSystemsAndParts //
    //----------------------//
    /**
     * Knowing the starting staff indice of each staff system, we are able to
     * allocate and describe the proper number of systems & parts in the score.
     *
     * @param systemStarts indexed by any staff, to give the staff index of the
     *                     containing system. For a system with just one staff,
     *                     both indices are equal. For a system of more than 1
     *                     staff, the indices differ.
     * @param partStarts indexed by any staff, to give the staff index of the
     *                   containing part. For a part with just one staff, both
     *                   indices are equal. For a part of more than 1 staff, the
     *                   indices differ.
     * @throws StepException raised if processing failed
     */
    private void buildSystemsAndParts (int[] systemStarts,
                                       int[] partStarts)
        throws StepException
    {
        int        id = 0; // Id for created SystemInfo's
        int        sStart = -1; // Current system start
        SystemInfo system = null; // Current system info
        int        pStart = -1; // Current part start
        PartInfo   part = null; // Current part info

        for (int i = 0; i < systemStarts.length; i++) {
            // System break ?
            if (systemStarts[i] != sStart) {
                system = new SystemInfo(++id, sheet);
                systems.add(system);
                sStart = i;
            }

            system.addStaff(i);

            // Part break ?
            if (partStarts[i] != pStart) {
                part = new PartInfo();
                system.addPart(part);
                pStart = i;
            }

            part.addStaff(sheet.getStaves().get(i));
        }

        if (logger.isFineEnabled()) {
            for (SystemInfo systemInfo : systems) {
                Dumper.dump(systemInfo);

                int i = 0;

                for (PartInfo partInfo : systemInfo.getParts()) {
                    Dumper.dump(partInfo, "Part #" + ++i, 1);
                }
            }
        }

        // Finally, store this list into the sheet instance
        sheet.setSystems(systems);
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
     * @throws StepException Raised when a sanity check on systems found
     *                             has failed
     */
    private void retrieveBarLines ()
        throws StepException
    {
        // Sort vertical sticks according to their abscissa
        Collections.sort(
            clutter,
            new Comparator<Stick>() {
                    public int compare (Stick o1,
                                        Stick o2)
                    {
                        return Integer.signum(o1.getMidPos() - o2.getMidPos());
                    }
                });

        if (logger.isFineEnabled()) {
            logger.fine(
                clutter.size() + " sticks to check: " +
                Glyph.toString(clutter));

            for (Stick stick : clutter) {
                logger.fine(stick.toString());
            }
        }

        // Total number of staves in the sheet
        final int staffNb = sheet.getStaves()
                                 .size();

        // A way to tell the containing System for each staff, by providing the
        // staff index of the starting staff of the containing system.
        int[] systemStarts = new int[staffNb];
        Arrays.fill(systemStarts, -1);

        // A way to tell the containing Part for each staff, by providing the
        // staff index of the starting staff of the containing part.
        int[] partStarts = new int[staffNb];
        Arrays.fill(partStarts, -1);

        createSuite();

        double minResult = constants.minCheckResult.getValue();

        // Check each candidate stick in turn, leaving in clutter the unlucky
        // candidates
        for (Iterator<Stick> it = clutter.iterator(); it.hasNext();) {
            Stick   stick = it.next();

            // Allocate the candidate context, and pass the whole check suite
            Context context = new Context(stick);
            double  res = suite.pass(context);

            if (logger.isFineEnabled()) {
                logger.fine("suite => " + res + " for " + stick);
            }

            if (res >= minResult) {
                // OK, we insert this candidate stick as a true bars member,
                // and remove it from clutter
                contexts.put(stick, context);
                bars.add(stick);
                it.remove();

                // Bars that define a system or a part (they start AND end with
                // precise staves limits)
                if ((context.topIdx != -1) && (context.botIdx != -1)) {
                    // Here, we have both part & system defining bars
                    // System bars occur first (increasing abscissa of glyphs)
                    for (int i = context.topIdx; i <= context.botIdx; i++) {
                        if (systemStarts[i] == -1) {
                            systemStarts[i] = context.topIdx;
                        }

                        partStarts[i] = context.topIdx;
                    }

                    stick.setResult(BAR_PART_DEFINING);

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Part-defining Bar line from staff " +
                            context.topIdx + " to staff " + context.botIdx +
                            " " + stick);
                    }
                } else {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Non-Part-defining Bar line " +
                            ((context.topIdx != -1)
                             ? (" topIdx=" + context.topIdx) : "") +
                            ((context.botIdx != -1)
                             ? (" botIdx=" + context.botIdx) : ""));
                    }

                    stick.setResult(BAR_NOT_PART_DEFINING);
                }
            }
        }

        // Print all bars found
        if (logger.isFineEnabled()) {
            logger.fine("Bars found:");

            for (Stick bar : bars) {
                logger.fine(bar.toString());
            }
        }

        // Sanity check on the systems found
        for (int i = 0; i < systemStarts.length; i++) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "staff=" + i + " systemStart=" + systemStarts[i] +
                    " partStart=" + partStarts[i]);
            }

            if (systemStarts[i] == -1) {
                logger.warning("No system found for staff " + i);
                throw new StepException();
            }
        }

        // System/Part retrieval
        buildSystemsAndParts(systemStarts, partStarts);
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // Context //
    //---------//
    static class Context
        implements Checkable
    {
        //~ Instance fields ----------------------------------------------------

        /** The stick being checked */
        Stick stick;

        /** Indicates a thick bar stick */
        boolean isThick;

        /** Staff area index for top of bar stick */
        int topArea = -1;

        /** Staff area index for bottom of bar stick */
        int bottomArea = -1;

        /** Precise staff index for top of bar stick, assigned when OK */
        int topIdx = -1;

        /** Precise staff index for bottom of bar stick, assigned when OK */
        int botIdx = -1;

        //~ Constructors -------------------------------------------------------

        public Context (Stick stick)
        {
            this.stick = stick;
        }

        //~ Methods ------------------------------------------------------------

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
        //~ Constructors -------------------------------------------------------

        public BarCheckSuite ()
        {
            super("Bar", constants.minCheckResult.getValue());

            // Be very careful with check order, because of side-effects
            add(1, new TopCheck());
            add(1, new BottomCheck());
            add(1, new HeightDiffCheck());
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
        //~ Constructors -------------------------------------------------------

        protected AnchorCheck ()
        {
            super(
                "Anchor",
                "Check top and bottom alignment of bars (thick/thin) with staff",
                constants.booleanThreshold,
                constants.booleanThreshold,
                true,
                NOT_STAFF_ANCHORED);
        }

        //~ Methods ------------------------------------------------------------

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
        //~ Constructors -------------------------------------------------------

        protected BottomCheck ()
        {
            super(
                "Bottom",
                "Check that bottom of stick is close to bottom of staff",
                constants.maxStaffShiftDyLow,
                constants.maxStaffShiftDyHigh,
                false,
                null);
        }

        //~ Methods ------------------------------------------------------------

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
        //~ Instance fields ----------------------------------------------------

        // Half width for chunk window at bottom
        private final int    nWidth;

        // Half height for chunk window at bottom
        private final int    nHeight;

        // Total area for chunk window
        private final double area;

        //~ Constructors -------------------------------------------------------

        protected BottomChunkCheck ()
        {
            super(
                "BotChunk",
                "Check there is no big chunck stuck on bottom of stick",
                constants.chunkRatioLow,
                constants.chunkRatioHigh,
                false,
                CHUNK_AT_BOTTOM);

            // Adjust chunk window according to system scale (problem, we have
            // sheet scale and staff scale, not system scale...)
            Scale scale = sheet.getScale();
            nWidth = scale.toPixels(constants.chunkWidth);
            nHeight = scale.toPixels(constants.chunkHeight);
            area = 4 * nWidth * nHeight;
        }

        //~ Methods ------------------------------------------------------------

        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;

            // Retrieve the stick chunk ratio at bottom
            return stick.getAliensAtStop(nHeight, nWidth) / area;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction chunkHeight = new Scale.Fraction(
            0.33,
            "Height of half area to look for chunks");
        Constant.Ratio chunkRatioHigh = new Constant.Ratio(
            0.25,
            "HighMinimum ratio of alien pixels to detect chunks");
        Constant.Ratio chunkRatioLow = new Constant.Ratio(
            0.25,
            "LowMinimum ratio of alien pixels to detect chunks");
        Scale.Fraction chunkWidth = new Scale.Fraction(
            0.33,
            "Width of half area to look for chunks");
        Constant.Ratio maxAdjacencyHigh = new Constant.Ratio(
            0.25d,
            "HighMaximum adjacency ratio for a bar stick");
        Constant.Ratio maxAdjacencyLow = new Constant.Ratio(
            0.25d,
            "LowMaximum adjacency ratio for a bar stick");
        Scale.Fraction maxBarOffset = new Scale.Fraction(
            1.0,
            "Vertical offset used to detect that a bar extends past a staff");
        Scale.Fraction maxStaffShiftDyHigh = new Scale.Fraction(
            10,
            "HighMaximum vertical distance between a bar edge and the staff line");
        Scale.Fraction maxStaffShiftDyLow = new Scale.Fraction(
            0.125,
            "LowMaximum vertical distance between a bar edge and the staff line");
        Scale.Fraction maxStaffDHeightHigh = new Scale.Fraction(
            0.4,
            "HighMaximum difference between a bar length and min staff height");
        Scale.Fraction maxStaffDHeightLow = new Scale.Fraction(
            0.2,
            "LowMaximum difference between a bar length and min staff height");
        Scale.Fraction minStaffDxHigh = new Scale.Fraction(
            0,
            "HighMinimum horizontal distance between a bar and a staff edge");
        Scale.Fraction minStaffDxLow = new Scale.Fraction(
            0,
            "LowMinimum horizontal distance between a bar and a staff edge");
        Scale.Fraction maxThinWidth = new Scale.Fraction(
            0.3,
            "Maximum width of a normal bar, versus a thick bar");
        Check.Grade    minCheckResult = new Check.Grade(
            0.50,
            "Minimum result for suite of check");
        Constant.Ratio booleanThreshold = new Constant.Ratio(
            0.5,
            "* DO NOT EDIT * - switch between true & false for a boolean");
    }

    //--------------------//
    // LeftAdjacencyCheck //
    //--------------------//
    private static class LeftAdjacencyCheck
        extends Check<Context>
    {
        //~ Constructors -------------------------------------------------------

        protected LeftAdjacencyCheck ()
        {
            super(
                "LeftAdj",
                "Check that left side of the stick is open enough",
                constants.maxAdjacencyLow,
                constants.maxAdjacencyHigh,
                false,
                TOO_HIGH_ADJACENCY);
        }

        //~ Methods ------------------------------------------------------------

        // Retrieve the adjacency value
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int   length = stick.getLength();

            return (double) stick.getFirstStuck() / (double) length;
        }
    }

    //-----------------//
    // HeightDiffCheck //
    //-----------------//
    private class HeightDiffCheck
        extends Check<Context>
    {
        //~ Constructors -------------------------------------------------------

        protected HeightDiffCheck ()
        {
            super(
                "HeightDiff",
                "Check that stick is as long as minimum staff height",
                constants.maxStaffDHeightLow,
                constants.maxStaffDHeightHigh,
                false,
                TOO_SHORT_BAR);
        }

        //~ Methods ------------------------------------------------------------

        // Retrieve the length data
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int   height = Integer.MAX_VALUE;

            // Check wrt every staff in the stick range
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaffInfo area = sheet.getStaves()
                                      .get(i);
                height = Math.min(height, area.getHeight());
            }

            return sheet.getScale()
                        .pixelsToFrac(height - stick.getLength());
        }
    }

    //-----------//
    // LeftCheck //
    //-----------//
    private class LeftCheck
        extends Check<Context>
    {
        //~ Constructors -------------------------------------------------------

        protected LeftCheck ()
        {
            super(
                "Left",
                "Check that stick is on the right of staff beginning bar",
                constants.minStaffDxLow,
                constants.minStaffDxHigh,
                true,
                OUTSIDE_STAFF_WIDTH);
        }

        //~ Methods ------------------------------------------------------------

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

    //---------------------//
    // RightAdjacencyCheck //
    //---------------------//
    private static class RightAdjacencyCheck
        extends Check<Context>
    {
        //~ Constructors -------------------------------------------------------

        protected RightAdjacencyCheck ()
        {
            super(
                "RightAdj",
                "Check that right side of the stick is open enough",
                constants.maxAdjacencyLow,
                constants.maxAdjacencyHigh,
                false,
                TOO_HIGH_ADJACENCY);
        }

        //~ Methods ------------------------------------------------------------

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
        //~ Constructors -------------------------------------------------------

        protected RightCheck ()
        {
            super(
                "Right",
                "Check that stick is on the left of staff ending bar",
                constants.minStaffDxLow,
                constants.minStaffDxHigh,
                true,
                OUTSIDE_STAFF_WIDTH);
        }

        //~ Methods ------------------------------------------------------------

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
        //~ Constructors -------------------------------------------------------

        protected TopCheck ()
        {
            super(
                "Top",
                "Check that top of stick is close to top of staff",
                constants.maxStaffShiftDyLow,
                constants.maxStaffShiftDyHigh,
                false,
                null);
        }

        //~ Methods ------------------------------------------------------------

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
        //~ Instance fields ----------------------------------------------------

        // Half width for chunk window at top
        private final int    nWidth;

        // Half height for chunk window at top
        private final int    nHeight;

        // Total area for chunk window
        private final double area;

        //~ Constructors -------------------------------------------------------

        protected TopChunkCheck ()
        {
            super(
                "TopChunk",
                "Check there is no big chunck stuck on top of stick",
                constants.chunkRatioLow,
                constants.chunkRatioHigh,
                false,
                CHUNK_AT_TOP);

            // Adjust chunk window according to system scale (problem, we have
            // sheet scale and staff scale, not system scale...)
            Scale scale = sheet.getScale();
            nWidth = scale.toPixels(constants.chunkWidth);
            nHeight = scale.toPixels(constants.chunkHeight);
            area = 4 * nWidth * nHeight;
        }

        //~ Methods ------------------------------------------------------------

        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;

            // Retrieve the stick chunk ratio at top
            return stick.getAliensAtStart(nHeight, nWidth) / area;
        }
    }
}
