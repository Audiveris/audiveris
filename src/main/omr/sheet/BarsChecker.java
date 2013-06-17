//----------------------------------------------------------------------------//
//                                                                            //
//                           B a r s C h e c k e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.check.Check;
import omr.check.CheckBoard;
import omr.check.CheckResult;
import omr.check.CheckSuite;
import omr.check.Checkable;
import omr.check.FailureResult;
import omr.check.Result;
import omr.check.SuccessResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.Filament;
import omr.grid.StaffInfo;
import omr.grid.StaffManager;

import omr.lag.Section;

import omr.run.Orientation;
import static omr.run.Orientation.*;


import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.sheet.BarsChecker.BarCheckSuite;
import static omr.util.HorizontalSide.*;
import omr.util.Predicate;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.jcip.annotations.NotThreadSafe;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code BarsChecker} is dedicated to physical checks of vertical
 * sticks that are candidates for barlines.
 *
 * @author Hervé Bitteur
 */
@NotThreadSafe
public class BarsChecker
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(BarsChecker.class);

    /** Successful bar that embraces a whole system or part */
    public static final SuccessResult BAR_PART_DEFINING = new SuccessResult(
            "Bar-PartDefining");

    /** Successful bar that embraces only a portion of a part */
    private static final SuccessResult BAR_NOT_PART_DEFINING = new SuccessResult(
            "Bar-NotPartDefining");

    /** Failure, since bar is shorter than staff height */
    private static final FailureResult TOO_SHORT_BAR = new FailureResult(
            "Bar-TooShort");

    /** Failure, since bar is on left or right side of the staff area */
    private static final FailureResult OUTSIDE_STAFF_WIDTH = new FailureResult(
            "Bar-OutsideStaffWidth");

    /** Failure, since bar goes too high above first staff */
    private static final FailureResult TOP_EXCESS = new FailureResult(
            "Bar-TooHigh");

    /** Failure, since bar top is too low below first staff */
    private static final FailureResult LOW_TOP = new FailureResult(
            "Bar-LowTop");

    /** Failure, since bar bottom is too high above last staff */
    private static final FailureResult HIGH_BOTTOM = new FailureResult(
            "Bar-HighBottom");

    /** Failure, since bar goes too low under last staff */
    private static final FailureResult BOTTOM_EXCESS = new FailureResult(
            "Bar-TooLow");

    /** Failure, since bar has no end aligned with a staff */
    private static final FailureResult NOT_STAFF_ANCHORED = new FailureResult(
            "Bar-NotStaffAnchored");

    /** Failure, since bar has a large chunk stuck on the top left */
    private static final FailureResult TOP_LEFT_CHUNK = new FailureResult(
            "Bar-TopLeftChunk");

    /** Failure, since bar has a large chunk stuck on the top right */
    private static final FailureResult TOP_RIGHT_CHUNK = new FailureResult(
            "Bar-TopRightChunk");

    /** Failure, since bar has a large chunk stuck on the bottom left */
    private static final FailureResult BOTTOM_LEFT_CHUNK = new FailureResult(
            "Bar-BottomLeftChunk");

    /** Failure, since bar has a large chunk stuck on the bottom right */
    private static final FailureResult BOTTOM_RIGHT_CHUNK = new FailureResult(
            "Bar-BottomRightChunk");

    /** Failure, since bar is too far from vertical */
    private static final FailureResult NON_VERTICAL = new FailureResult(
            "Bar-NonVertical");

    /** Failure, since bar is too far from straight line */
    private static final FailureResult NON_STRAIGHT = new FailureResult(
            "Bar-NonStraight");

    //~ Instance fields --------------------------------------------------------
    /**
     * true for rough tests (when retrieving staff grid),
     * false for precise tests (when retrieving measures)
     */
    private boolean rough;

    /** Related sheet */
    private final Sheet sheet;

    /** Related scale */
    private final Scale scale;

    /** Related staves */
    private final StaffManager staffManager;

    /** Suite of checks to be performed */
    private final BarCheckSuite suite;

    /** Related context of a bar stick */
    private final Map<Glyph, GlyphContext> contexts = new HashMap<>();

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // BarsChecker //
    //-------------//
    /**
     * Prepare a bar checker on the provided sheet
     *
     * @param sheet the sheet to process
     * @param rough true for rough tests (when retrieving staff frames),
     *              false for precise tests
     */
    public BarsChecker (Sheet sheet,
                        boolean rough)
    {
        this.sheet = sheet;
        this.rough = rough;

        scale = sheet.getScale();
        suite = new BarCheckSuite();
        staffManager = sheet.getStaffManager();
    }

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // checkCandidates //
    //-----------------//
    /**
     * From the list of vertical sticks, this method uses several tests
     * to provide the initial collection of good barlines candidates.
     *
     * @param sticks the collection of candidate sticks
     */
    public void checkCandidates (Collection<? extends Glyph> sticks)
    {
        //        // Sort candidates according to their abscissa
        //        List<Glyph> sortedSticks = new ArrayList<Glyph>(sticks);
        //        Collections.sort(sortedSticks, Glyph.midPosComparator);
        double minResult = constants.minCheckResult.getValue();

        // Check each candidate stick in turn
        for (Glyph stick : sticks) {
            // Allocate the candidate context, and pass the whole check suite
            GlyphContext context = new GlyphContext(stick);

            double res = suite.pass(context);

            if (logger.isDebugEnabled() || stick.isVip()) {
                logger.info("suite => {}{} for {}",
                        (float) res,
                        (stick.getResult() != null)
                        ? (" " + stick.getResult()) : "",
                        stick);
            }

            if ((stick.isBar() && stick.isManualShape()) || res >= minResult) {
                // OK, we flag this candidate with proper barline shape
                contexts.put(stick, context);
                if ((!stick.isBar() || !stick.isManualShape())) {
                    stick.setShape(
                            isThickBar(stick)
                            ? Shape.THICK_BARLINE : Shape.THIN_BARLINE);
                }

                // Additional processing for Bars that define a system or a part
                // (they start AND end with precise staves horizontal limits)
                if ((context.topStaff != -1) && (context.botStaff != -1)) {
                    // Here, we have both part & system defining bars
                    // System bars occur first
                    // (since glyphs are sorted by increasing abscissa)
                    stick.setResult(BAR_PART_DEFINING);

                    logger.debug(
                            "Part-defining Barline from staff {} to staff {} {}",
                            context.topStaff, context.botStaff, stick);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Non-Part-defining Bar line {}{}",
                                (context.topStaff != -1)
                                ? (" topIdx=" + context.topStaff) : "",
                                (context.botStaff != -1)
                                ? (" botIdx=" + context.botStaff) : "");
                    }

                    stick.setResult(BAR_NOT_PART_DEFINING);
                }
            } else {
                if (stick.isBar()) {
                    if (logger.isDebugEnabled() || stick.isVip()) {
                        logger.info("Purged {} {}",
                                stick.idString(), stick.getShape());
                    }

                    stick.setShape(null);
                }
            }
        }
    }

    //---------------//
    // getCheckBoard //
    //---------------//
    public CheckBoard<GlyphContext> getCheckBoard ()
    {
        return new BarCheckBoard(
                getSuite(),
                sheet.getNest().getGlyphService(),
                new Class<?>[]{GlyphEvent.class});
    }

    //----------//
    // getSuite //
    //----------//
    /**
     * Report the suite currently defined.
     * (used by package mate SystemsBuilder)
     *
     * @return the check suite
     */
    public CheckSuite<GlyphContext> getSuite ()
    {
        return suite;
    }

    //------------------//
    // getAlienPixelsIn //
    //------------------//
    private int getAlienPixelsIn (Glyph glyph,
                                  Rectangle absRoi)
    {
        Predicate<Section> predicate = new Predicate<Section>()
        {
            @Override
            public boolean check (Section section)
            {
                return (section.getGlyph() == null)
                       || (section.getGlyph().getShape() != Shape.STAFF_LINE);
            }
        };

        int total = 0;
        total += glyph.getAlienPixelsFrom(
                sheet.getVerticalLag(),
                absRoi,
                predicate);
        total += glyph.getAlienPixelsFrom(
                sheet.getHorizontalLag(),
                absRoi,
                predicate);

        return total;
    }

    //-------------------//
    // initializeContext //
    //-------------------//
    private void initializeContext (GlyphContext context)
    {
        Glyph stick = context.stick;
        StaffInfo startStaff = staffManager.getStaffAt(
                stick.getStartPoint(VERTICAL));
        StaffInfo stopStaff = staffManager.getStaffAt(
                stick.getStopPoint(VERTICAL));

        // Remember top & bottom areas
        context.topArea = staffManager.getIndexOf(startStaff);
        context.bottomArea = staffManager.getIndexOf(stopStaff);

        // Check whether this stick embraces more than one staff
        context.isPartDefining = context.topArea != context.bottomArea;

        // Remember if this is a thick stick
        context.isThick = isThickBar(stick);
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
    private boolean isThickBar (Glyph stick)
    {
        // Max width of a thin bar line, otherwise this must be a thick bar
        final int maxThinWidth = scale.toPixels(constants.maxThinWidth);

        // Average width of the stick
        final int meanWidth = (int) Math.rint(
                (double) stick.getWeight() / (double) stick.getLength(
                Orientation.VERTICAL));

        return meanWidth > maxThinWidth;
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //--------------//
    // StaffAnchors //
    //--------------//
    /**
     * Record which staves the top and the bottom of a stick are anchored to
     */
    public static class StaffAnchors
    {
        //~ Instance fields ----------------------------------------------------

        public final int top;

        public final int bot;

        //~ Constructors -------------------------------------------------------
        public StaffAnchors (int topIdx,
                             int botIdx)
        {
            this.top = topIdx;
            this.bot = botIdx;
        }
    }

    //---------------//
    // BarCheckSuite //
    //---------------//
    class BarCheckSuite
            extends CheckSuite<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        public BarCheckSuite ()
        {
            super("Bar", constants.minCheckResult.getValue());

            // Be very careful with check order, because of side-effects
            // topArea, bottomArea, isPartDefining, isThick are already set
            //
            add(1, new VerticalCheck());
            add(1, new LeftCheck());
            add(1, new RightCheck());
            add(1, new HeightDiffCheck());
            add(1, new RadiusCheck());

            add(1, new TopLeftChunkCheck());
            add(1, new TopRightChunkCheck());
            add(1, new BottomLeftChunkCheck());
            add(1, new BottomRightChunkCheck());

            add(1, new TopInterCheck());
            add(1, new BottomInterCheck());

            if (!rough) {
                add(1, new TopCheck()); // May set topStaff
                add(1, new BottomCheck()); // May set botStaff
                add(1, new AnchorCheck()); // Reads topStaff & botStaff
            }

            if (logger.isDebugEnabled()) {
                dump();
            }

            //            // We don't check that the bar does not start before first staff,
            //            // this is too restrictive because of alternate endings.  We however
            //            // do check that the bar does not end after last staff of the
            //            // last part of the system.
            //            int barAbscissa = bar.getMidPos();
            //            int systemBottom = scoreSystem.getLastPart()
            //                                          .getLastStaff()
            //                                          .getInfo()
            //                                          .getLastLine()
            //                                          .yAt(barAbscissa);
            //
            //            if ((bar.getStop() - systemBottom) > maxDy) {
            //                if (logger.isDebugEnabled()) {
            //                    logger.debug("Bar stopping too low");
            //                }
            //
            //                bar.setResult(NOT_WITHIN_SYSTEM);
            //
            //                continue;
            //            }
        }
    }

    //--------------//
    // GlyphContext //
    //--------------//
    class GlyphContext
            implements Checkable
    {
        //~ Instance fields ----------------------------------------------------

        /** The stick being checked */
        Glyph stick;

        /** Indicates a part-defining stick (embracing more than one staff) */
        boolean isPartDefining;

        /** Indicates a thick bar stick */
        boolean isThick;

        /** Nearest staff for top of bar stick */
        int topArea = -1;

        /** Nearest staff for bottom of bar stick */
        int bottomArea = -1;

        /** Precise staff for top of bar stick, assigned when OK */
        int topStaff = -1;

        /** Precise staff for bottom of bar stick, assigned when OK */
        int botStaff = -1;

        //~ Constructors -------------------------------------------------------
        public GlyphContext (Glyph stick)
        {
            this.stick = stick;

            initializeContext(this); // Initialization before any check
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public boolean isVip ()
        {
            return stick.isVip();
        }

        @Override
        public void setResult (Result result)
        {
            stick.setResult(result);
        }

        @Override
        public void setVip ()
        {
            stick.setVip();
        }

        @Override
        public String toString ()
        {
            return "stick#" + stick.getId();
        }
    }

    //-------------//
    // AnchorCheck //
    //-------------//
    private class AnchorCheck
            extends Check<GlyphContext>
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
        @Override
        protected double getValue (GlyphContext context)
        {
            if (rough) {
                if (context.isPartDefining) {
                    return 1;
                } else {
                    if ((context.topStaff != -1) && (context.botStaff != -1)) {
                        return 1;
                    }
                }

                return 0;
            } else {
                if (context.isThick) {
                    if ((context.topStaff != -1) && (context.botStaff != -1)) {
                        return 1;
                    }
                } else {
                    if ((context.topStaff != -1) || (context.botStaff != -1)) {
                        return 1;
                    }
                }

                return 0;
            }
        }
    }

    //---------------//
    // BarCheckBoard //
    //---------------//
    /**
     * A specific board dedicated to physical checks of barline sticks
     */
    private class BarCheckBoard
            extends CheckBoard<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        public BarCheckBoard (CheckSuite<GlyphContext> suite,
                              SelectionService eventService,
                              Class[] eventList)
        {
            super("BarlineCheck", suite, eventService, eventList);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void onEvent (UserEvent event)
        {
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                if (event instanceof GlyphEvent) {
                    BarsChecker.GlyphContext context = null;
                    GlyphEvent glyphEvent = (GlyphEvent) event;
                    Glyph glyph = glyphEvent.getData();

                    if (glyph != null) {
                        // Make sure this is a rather vertical stick
                        if (Math.abs(glyph.getInvertedSlope()) <= constants.maxCoTangentForCheck.
                                getValue()) {
                            // Apply a fresh suite
                            context = new BarsChecker.GlyphContext(glyph);
                            applySuite(new BarCheckSuite(), context);

                            return;
                        }
                    }

                    tellObject(null);
                }
            } catch (Exception ex) {
                logger.warn(getClass().getName() + " onEvent error", ex);
            }
        }
    }

    //-------------//
    // BottomCheck //
    //-------------//
    private class BottomCheck
            extends LongCheck
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
                    BOTTOM_EXCESS);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the distance with proper staff border
        @Override
        protected double getValue (GlyphContext context)
        {
            Glyph stick = context.stick;
            Point2D stop = stick.getStopPoint(VERTICAL);

            // Which staff area contains the bottom of the stick?
            StaffInfo staff = staffManager.getStaffAt(stop);

            // How far are we from the stop of the staff?
            double staffBottom = staff.getLastLine().yAt(stop.getX());
            double dy = sheet.getScale().pixelsToFrac(Math.
                    abs(staffBottom - stop.
                    getY()));

            // Change limits according to rough & partDefining
            if (rough && context.isPartDefining) {
                setLowHigh(
                        constants.maxStaffShiftDyLowRough,
                        constants.maxStaffShiftDyHighRough);
            } else {
                setLowHigh(
                        constants.maxStaffShiftDyLow,
                        constants.maxStaffShiftDyHigh);
            }

            // Side-effect
            if (dy <= getLow()) {
                context.botStaff = context.bottomArea;
            }

            return dy;
        }
    }

    //------------------//
    // BottomInterCheck //
    //------------------//
    private class BottomInterCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        protected BottomInterCheck ()
        {
            super(
                    "Bottom",
                    "Check that bottom of stick reaches last staff",
                    constants.maxStaffYGapLow,
                    constants.maxStaffYGapHigh,
                    false,
                    HIGH_BOTTOM);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the distance with proper staff border
        @Override
        protected double getValue (GlyphContext context)
        {
            Glyph stick = context.stick;
            Point2D stop = stick.getStopPoint(VERTICAL);

            // Which staff area contains the bottom of the stick?
            StaffInfo staff = staffManager.getStaffAt(stop);

            // How far are we from the stop of the staff?
            double staffBottom = staff.getLastLine().yAt(stop.getX());
            double dy = sheet.getScale().pixelsToFrac(Math.
                    abs(staffBottom - stop.
                    getY()));

            return dy;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction chunkWidth = new Scale.Fraction(
                0.3,
                "Width of box to look for chunks");

        Scale.Fraction chunkHalfHeight = new Scale.Fraction(
                0.5,
                "Half height of box to look for chunks");

        Constant.Ratio chunkRatioHigh = new Constant.Ratio(
                4.0,
                "High Minimum ratio of alien pixels to detect chunks");

        Constant.Ratio chunkRatioLow = new Constant.Ratio(
                2.0,
                "Low Minimum ratio of alien pixels to detect chunks");

        Scale.Fraction maxStaffShiftDyHigh = new Scale.Fraction(
                0.4,
                "High Maximum vertical distance between a bar edge and the staff line");

        Scale.Fraction maxStaffShiftDyHighRough = new Scale.Fraction(
                4.0,
                "Rough high Maximum vertical distance between a bar edge and the staff line");

        Scale.Fraction maxStaffShiftDyLow = new Scale.Fraction(
                0.3,
                "Low Maximum vertical distance between a bar edge and the staff line");

        Scale.Fraction maxStaffShiftDyLowRough = new Scale.Fraction(
                2.0,
                "Rough low Maximum vertical distance between a bar edge and the staff line");

        Scale.Fraction maxStaffDHeightHigh = new Scale.Fraction(
                0.4,
                "High Maximum difference between a bar length and min staff height");

        Scale.Fraction maxStaffDHeightHighRough = new Scale.Fraction(
                1,
                "Rough high Maximum difference between a bar length and min staff height");

        Scale.Fraction maxStaffDHeightLow = new Scale.Fraction(
                0.2,
                "Low Maximum difference between a bar length and min staff height");

        Scale.Fraction maxStaffDHeightLowRough = new Scale.Fraction(
                0.5,
                "Rough low Maximum difference between a bar length and min staff height");

        Scale.Fraction maxStaffYGapLow = new Scale.Fraction(
                2,
                "Low Maximum dy between a bar edge and target staff line");

        Scale.Fraction maxStaffYGapHigh = new Scale.Fraction(
                3,
                "High Maximum dy between a bar edge and target staff line");

        Scale.Fraction minStaffDxHigh = new Scale.Fraction(
                0,
                "High Minimum horizontal distance between a bar and a staff edge");

        Scale.Fraction minStaffDxHighRough = new Scale.Fraction(
                -3,
                "Rough high Minimum horizontal distance between a bar and a staff edge");

        Scale.Fraction minStaffDxLow = new Scale.Fraction(
                -1,
                "Low Minimum horizontal distance between a bar and a staff edge");

        Scale.Fraction minStaffDxLowRough = new Scale.Fraction(
                -5,
                "Rough low Minimum horizontal distance between a bar and a staff edge");

        Constant.Double maxSlopeLow = new Constant.Double(
                "slope",
                0.1,
                "Low maximum difference with global slope");

        Constant.Double maxSlopeHigh = new Constant.Double(
                "slope",
                0.2,
                "High maximum difference with global slope");

        Scale.Fraction minRadiusLow = new Scale.Fraction(
                25,
                "Low minimum radius");

        Scale.Fraction minRadiusHigh = new Scale.Fraction(
                60,
                "High minimum radius");

        Scale.Fraction maxThinWidth = new Scale.Fraction(
                0.3,
                "Maximum width of a normal bar, versus a thick bar");

        Check.Grade minCheckResult = new Check.Grade(
                0.50,
                "Minimum result for suite of check");

        Constant.Ratio minStaffCountForLongBar = new Constant.Ratio(
                2,
                "Minimum length for long bars, stated in number of staff heights");

        Constant.Ratio booleanThreshold = new Constant.Ratio(
                0.5,
                "* DO NOT EDIT * - switch between true & false for a boolean");

        Constant.Double maxCoTangentForCheck = new Constant.Double(
                "cotangent",
                0.1,
                "Maximum cotangent for checking a barline candidate");

    }

    //------------//
    // ChunkCheck //
    //------------//
    private abstract class ChunkCheck
            extends Check<GlyphContext>
    {
        //~ Instance fields ----------------------------------------------------

        // Width for chunk window
        protected final int nWidth;

        // Height for chunk window
        protected final int nHeight;

        //~ Constructors -------------------------------------------------------
        protected ChunkCheck (String name,
                              String description,
                              FailureResult redResult)
        {
            super(
                    name,
                    description,
                    constants.chunkRatioLow,
                    constants.chunkRatioHigh,
                    false,
                    redResult);

            // Adjust chunk window according to system scale
            nWidth = scale.toPixels(constants.chunkWidth);
            nHeight = scale.toPixels(constants.chunkHalfHeight);
        }

        //~ Methods ------------------------------------------------------------
        protected abstract Rectangle getBox (Glyph stick);

        @Override
        protected double getValue (GlyphContext context)
        {
            Glyph stick = context.stick;
            Rectangle box = getBox(stick);

            int aliens = getAlienPixelsIn(stick, box);
            int area = box.width * box.height;

            // Normalize the ratio with stick length
            double ratio = (1000 * aliens) / ((double) area * stick.getLength(
                    Orientation.VERTICAL));

            logger.debug("{} {} aliens:{} area:{} ratio:{}",
                    stick.idString(), getName(), aliens, area,
                    (float) ratio);

            return ratio;
        }
    }

    //----------------------//
    // BottomLeftChunkCheck //
    //----------------------//
    /**
     * Check for lack of chunk on lower left side of the bar stick
     */
    private class BottomLeftChunkCheck
            extends ChunkCheck
    {
        //~ Constructors -------------------------------------------------------

        protected BottomLeftChunkCheck ()
        {
            super(
                    "BLChunk",
                    "Check for no big chunk stuck on lower left side of stick",
                    BOTTOM_LEFT_CHUNK);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected Rectangle getBox (Glyph stick)
        {
            Point2D bottom = stick.getStopPoint(VERTICAL);
            Rectangle box = new Rectangle(
                    (int) Math.rint(bottom.getX() - nWidth),
                    (int) Math.rint(bottom.getY() - (1.5 * nHeight)),
                    nWidth,
                    2 * nHeight);
            stick.addAttachment("bl", box);

            return box;
        }
    }

    //-----------------------//
    // BottomRightChunkCheck //
    //-----------------------//
    /**
     * Check for lack of chunk on lower right side of the bar stick
     */
    private class BottomRightChunkCheck
            extends ChunkCheck
    {
        //~ Constructors -------------------------------------------------------

        protected BottomRightChunkCheck ()
        {
            super(
                    "BRChunk",
                    "Check for no big chunk stuck on lower right side of stick",
                    BOTTOM_RIGHT_CHUNK);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected Rectangle getBox (Glyph stick)
        {
            Point2D bottom = stick.getStopPoint(VERTICAL);
            Rectangle box = new Rectangle(
                    (int) Math.rint(bottom.getX()),
                    (int) Math.rint(bottom.getY() - (1.5 * nHeight)),
                    nWidth,
                    2 * nHeight);
            stick.addAttachment("br", box);

            return box;
        }
    }

    //-----------------//
    // HeightDiffCheck //
    //-----------------//
    private class HeightDiffCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        protected HeightDiffCheck ()
        {
            super(
                    "HeightDiff",
                    "Check that stick is as long as minimum staff height",
                    rough ? constants.maxStaffDHeightLowRough
                    : constants.maxStaffDHeightLow,
                    rough ? constants.maxStaffDHeightHighRough
                    : constants.maxStaffDHeightHigh,
                    false,
                    TOO_SHORT_BAR);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the length data
        @Override
        protected double getValue (GlyphContext context)
        {
            Glyph stick = context.stick;
            int height = Integer.MAX_VALUE;

            // Check wrt every staff in the stick getRange
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaffInfo area = staffManager.getStaff(i);
                height = Math.min(height, area.getHeight());
            }

            return sheet.getScale().pixelsToFrac(
                    height - stick.getLength(Orientation.VERTICAL));
        }
    }

    //-----------//
    // LeftCheck //
    //-----------//
    private class LeftCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        protected LeftCheck ()
        {
            super(
                    "Left",
                    "Check that stick is on the right of staff beginning bar",
                    rough ? constants.minStaffDxLowRough : constants.minStaffDxLow,
                    rough ? constants.minStaffDxHighRough : constants.minStaffDxHigh,
                    true,
                    OUTSIDE_STAFF_WIDTH);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the stick abscissa
        @Override
        protected double getValue (GlyphContext context)
        {
            Glyph stick = context.stick;
            double dist = Double.MAX_VALUE;

            // Check wrt every staff in the stick range
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaffInfo staff = staffManager.getStaff(i);
                Point2D top = staff.getFirstLine().getEndPoint(LEFT);
                Point2D bot = staff.getLastLine().getEndPoint(LEFT);
                double y = (top.getY() + bot.getY()) / 2;
                double x = stick.getPositionAt(y, Orientation.VERTICAL);
                double dx = x - staff.getAbscissa(LEFT);
                dist = Math.min(dist, dx);
            }

            return sheet.getScale().pixelsToFrac(dist);
        }
    }

    //-----------//
    // LongCheck //
    //-----------//
    /**
     * This kind of check allows to force the result in certain cases.
     */
    private abstract class LongCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        public LongCheck (String name,
                          String description,
                          Constant.Double low,
                          Constant.Double high,
                          boolean covariant,
                          FailureResult redResult)
        {
            super(name, description, low, high, covariant, redResult);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public CheckResult pass (GlyphContext context,
                                 CheckResult result,
                                 boolean update)
        {
            if (rough && context.isPartDefining) {
                // Since this stick is a long one, embracing several staves,
                // this check is not relevant
                result.value = getValue(context); // For possible side-effect
                result.flag = GREEN;

                return result;
            } else {
                return super.pass(context, result, update);
            }
        }
    }

    //-------------//
    // RadiusCheck //
    //-------------//
    private class RadiusCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        protected RadiusCheck ()
        {
            super(
                    "Radius",
                    "Check mean stick radius of curvature",
                    constants.minRadiusLow,
                    constants.minRadiusHigh,
                    true,
                    NON_STRAIGHT);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve a measure of x variance
        @Override
        protected double getValue (GlyphContext context)
        {
            Glyph stick = context.stick;

            if (stick instanceof Filament) {
                Filament fil = (Filament) stick;

                return sheet.getScale().pixelsToFrac(fil.getMeanCurvature());
            } else {
                return getHigh(); // A pass-through check
            }
        }
    }

    //------------//
    // RightCheck //
    //------------//
    private class RightCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        protected RightCheck ()
        {
            super(
                    "Right",
                    "Check that stick is on the left of staff ending bar",
                    rough ? constants.minStaffDxLowRough : constants.minStaffDxLow,
                    rough ? constants.minStaffDxHighRough : constants.minStaffDxHigh,
                    true,
                    OUTSIDE_STAFF_WIDTH);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the stick abscissa
        @Override
        protected double getValue (GlyphContext context)
        {
            Glyph stick = context.stick;
            double dist = Double.MAX_VALUE;

            // Check wrt every staff in the stick getRange
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaffInfo staff = staffManager.getStaff(i);
                Point2D top = staff.getFirstLine().getEndPoint(RIGHT);
                Point2D bot = staff.getLastLine().getEndPoint(RIGHT);
                double y = (top.getY() + bot.getY()) / 2;
                double x = (stick instanceof Filament)
                        ? ((Filament) stick).getPositionAt(
                        y,
                        Orientation.VERTICAL) : stick.getLine().xAtY(y);
                dist = Math.min(dist, staff.getAbscissa(RIGHT) - x);
            }

            return sheet.getScale().pixelsToFrac(dist);
        }
    }

    //----------//
    // TopCheck //
    //----------//
    private class TopCheck
            extends LongCheck
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
                    TOP_EXCESS);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the distance with proper staff border
        @Override
        protected double getValue (GlyphContext context)
        {
            Glyph stick = context.stick;
            Point2D start = stick.getStartPoint(VERTICAL);

            // Which staff area contains the top of the stick?
            StaffInfo staff = staffManager.getStaffAt(start);

            // How far are we from the start of the staff?
            double staffTop = staff.getFirstLine().yAt(start.getX());
            double dy = sheet.getScale().pixelsToFrac(Math.abs(staffTop - start.
                    getY()));

            // Change limits according to rough & partDefining
            if (rough && context.isPartDefining) {
                setLowHigh(
                        constants.maxStaffShiftDyLowRough,
                        constants.maxStaffShiftDyHighRough);
            } else {
                setLowHigh(
                        constants.maxStaffShiftDyLow,
                        constants.maxStaffShiftDyHigh);
            }

            // Side-effect
            if (dy <= getLow()) {
                context.topStaff = context.topArea;
            }

            return dy;
        }
    }

    //---------------//
    // TopInterCheck //
    //---------------//
    private class TopInterCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        protected TopInterCheck ()
        {
            super(
                    "TopInter",
                    "Check that top of stick intersects a staff",
                    constants.maxStaffYGapLow,
                    constants.maxStaffYGapHigh,
                    false,
                    LOW_TOP);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the distance with proper staff line
        @Override
        protected double getValue (GlyphContext context)
        {
            Glyph stick = context.stick;
            Point2D start = stick.getStartPoint(VERTICAL);

            // Which staff area contains the top of the stick?
            StaffInfo staff = staffManager.getStaffAt(start);

            // How far are we from the start of the staff?
            double staffTop = staff.getFirstLine().yAt(start.getX());
            double dy = sheet.getScale().pixelsToFrac(Math.abs(staffTop - start.
                    getY()));

            return dy;
        }
    }

    //-------------------//
    // TopLeftChunkCheck //
    //-------------------//
    /**
     * Check for lack of chunk on upper left side of the bar stick
     */
    private class TopLeftChunkCheck
            extends ChunkCheck
    {
        //~ Constructors -------------------------------------------------------

        protected TopLeftChunkCheck ()
        {
            super(
                    "TLChunk",
                    "Check for no big chunk stuck on upper left side of stick",
                    TOP_LEFT_CHUNK);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected Rectangle getBox (Glyph stick)
        {
            Point2D top = stick.getStartPoint(VERTICAL);
            Rectangle box = new Rectangle(
                    (int) Math.rint(top.getX() - nWidth),
                    (int) Math.rint(top.getY() - (nHeight / 2)),
                    nWidth,
                    2 * nHeight);
            stick.addAttachment("tl", box);

            return box;
        }
    }

    //--------------------//
    // TopRightChunkCheck //
    //--------------------//
    /**
     * Check for lack of chunk on upper right side of the bar stick
     */
    private class TopRightChunkCheck
            extends ChunkCheck
    {
        //~ Constructors -------------------------------------------------------

        protected TopRightChunkCheck ()
        {
            super("TRChunk",
                    "Check for no big chunk stuck on upper right side of stick",
                    TOP_RIGHT_CHUNK);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected Rectangle getBox (Glyph stick)
        {
            Point2D top = stick.getStartPoint(VERTICAL);
            Rectangle box = new Rectangle(
                    (int) Math.rint(top.getX()),
                    (int) Math.rint(top.getY() - (nHeight / 2)),
                    nWidth,
                    2 * nHeight);
            stick.addAttachment("tr", box);

            return box;
        }
    }

    //---------------//
    // VerticalCheck //
    //---------------//
    private class VerticalCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        protected VerticalCheck ()
        {
            super("Vertical",
                    "Check that stick is vertical, according to global slope",
                    constants.maxSlopeLow,
                    constants.maxSlopeHigh,
                    false,
                    NON_VERTICAL);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the difference between stick slope and global slope
        @Override
        protected double getValue (GlyphContext context)
        {
            Glyph stick = context.stick;
            Point2D start = stick.getStartPoint(VERTICAL);
            Point2D stop = stick.getStopPoint(VERTICAL);

            // Beware of sign of stickSlope (it is opposite of globalSlope)
            double stickSlope = -(stop.getX() - start.getX()) / (stop.getY()
                                                                 - start.getY());

            return Math.abs(stickSlope - sheet.getSkew().getSlope());
        }
    }
}
