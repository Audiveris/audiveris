//----------------------------------------------------------------------------//
//                                                                            //
//                           B a r s C h e c k e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.check.Check;
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

import omr.lag.Lag;
import omr.lag.Section;

import omr.log.Logger;

import omr.run.Orientation;
import omr.run.Run;

import omr.score.common.PixelRectangle;
import static omr.util.HorizontalSide.*;
import omr.util.Implement;
import omr.util.Predicate;

import net.jcip.annotations.NotThreadSafe;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class <code>BarsChecker</code> is dedicated to physical checks of vertical
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
    private static final Logger logger = Logger.getLogger(BarsChecker.class);

    /** Successful bar that embraces a whole system */
    public static final SuccessResult BAR_PART_DEFINING = new SuccessResult(
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

    /** Failure, since bar goes too high above first staff */
    private static final FailureResult TOP_EXCESS = new FailureResult(
        "Bar-TooHigh");

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
    private final Map<Glyph, GlyphContext> contexts = new HashMap<Glyph, GlyphContext>();

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // BarsChecker //
    //-------------//
    /**
     * Prepare a bar checker on the provided sheet
     *
     * @param sheet the sheet to process
     * @param rough true for rough tests (when retrieving staff frames),
     * false for precise tests
     */
    public BarsChecker (Sheet   sheet,
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
    // getStaffAnchors //
    //-----------------//
    /**
     * Report the staves at the top and at the bottom of the
     * provided stick. (Used by package mate SystemsBuilder)
     * @param stick the stick to lookup
     * @return a pair of staves, top & bottom, or null if the stick is
     * not known
     */
    public StaffAnchors getStaffAnchors (Glyph stick)
    {
        GlyphContext context = contexts.get(stick);

        if (context == null) {
            context = new GlyphContext(stick);
            suite.pass(context);
            contexts.put(stick, context);
        }

        return new StaffAnchors(context.topStaff, context.botStaff);
    }

    //----------//
    // getSuite //
    //----------//
    /**
     * Report the suite currently defined (used by package mate SystemsBuilder)
     *
     * @return the check suite
     */
    public CheckSuite<GlyphContext> getSuite ()
    {
        return suite;
    }

    //-----------------//
    // checkCandidates //
    //-----------------//
    /**
     * From the list of vertical sticks, this method uses several tests to
     * provide the initial collection of good barlines candidates.
     *
     * @param sticks the collection of candidate sticks
     */
    public void checkCandidates (Collection<?extends Glyph> sticks)
    {
        //        // Sort candidates according to their abscissa
        //        List<Glyph> sortedSticks = new ArrayList<Glyph>(sticks);
        //        Collections.sort(sortedSticks, Glyph.midPosComparator);
        double minResult = constants.minCheckResult.getValue();

        // Check each candidate stick in turn
        for (Glyph stick : sticks) {
            // Allocate the candidate context, and pass the whole check suite
            GlyphContext context = new GlyphContext(stick);
            initializeContext(context); // Initialization before any check

            double res = suite.pass(context);

            if (logger.isFineEnabled() || stick.isVip()) {
                logger.info(
                    "suite => " + (float) res +
                    ((stick.getResult() != null) ? (" " + stick.getResult()) : "") +
                    " for " + stick);
            }

            if (res >= minResult) {
                // OK, we flag this candidate with proper barline shape
                contexts.put(stick, context);
                stick.setShape(
                    isThickBar(stick) ? Shape.THICK_BARLINE : Shape.THIN_BARLINE);

                // Additional processing for Bars that define a system or a part
                // (they start AND end with precise staves horizontal limits)
                if ((context.topStaff != -1) && (context.botStaff != -1)) {
                    // Here, we have both part & system defining bars
                    // System bars occur first
                    // (since glyphs are sorted by increasing abscissa)
                    stick.setResult(BAR_PART_DEFINING);

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Part-defining Bar line from staff " +
                            context.topStaff + " to staff " + context.botStaff +
                            " " + stick);
                    }
                } else {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Non-Part-defining Bar line " +
                            ((context.topStaff != -1)
                             ? (" topIdx=" + context.topStaff) : "") +
                            ((context.botStaff != -1)
                             ? (" botIdx=" + context.botStaff) : ""));
                    }

                    stick.setResult(BAR_NOT_PART_DEFINING);
                }
            } else {
                if (stick.isBar()) {
                    if (logger.isFineEnabled() || stick.isVip()) {
                        logger.info(
                            "Purged Glyph#" + stick.getId() + " " +
                            stick.getShape());
                    }

                    stick.setShape(null);
                }
            }
        }
    }

    //------------------//
    // getAlienPixelsIn //
    //------------------//
    private int getAlienPixelsIn (Glyph          glyph,
                                  PixelRectangle absRoi)
    {
        Predicate<Section> predicate = new Predicate<Section>() {
            public boolean check (Section section)
            {
                return (section.getGlyph() == null) ||
                       (section.getGlyph()
                               .getShape() != Shape.STAFF_LINE);
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

    //-------------------//
    // initializeContext //
    //-------------------//
    private void initializeContext (GlyphContext context)
    {
        Glyph     stick = context.stick;
        StaffInfo startStaff = staffManager.getStaffAt(stick.getStartPoint());
        StaffInfo stopStaff = staffManager.getStaffAt(stick.getStopPoint());

        // Remember top & bottom areas
        context.topArea = staffManager.getIndexOf(startStaff);
        context.bottomArea = staffManager.getIndexOf(stopStaff);

        // Check whether this stick embraces more than one staff
        context.isPartDefining = context.topArea != context.bottomArea;

        // Remember if this is a thick stick
        context.isThick = isThickBar(stick);
    }

    //~ Inner Classes ----------------------------------------------------------

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

    //--------------//
    // GlyphContext //
    //--------------//
    static class GlyphContext
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
        }

        //~ Methods ------------------------------------------------------------

        @Implement(Checkable.class)
        public void setResult (Result result)
        {
            stick.setResult(result);
        }

        public void setVip ()
        {
            stick.setVip();
        }

        public boolean isVip ()
        {
            return stick.isVip();
        }

        @Override
        public String toString ()
        {
            return "stick#" + stick.getId();
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

            if (!rough) {
                add(1, new TopCheck()); // May set topStaff
                add(1, new BottomCheck()); // May set botStaff
                add(1, new AnchorCheck()); // Reads topStaff & botStaff
            }

            if (logger.isFineEnabled()) {
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
            //                if (logger.isFineEnabled()) {
            //                    logger.fine("Bar stopping too low");
            //                }
            //
            //                bar.setResult(NOT_WITHIN_SYSTEM);
            //
            //                continue;
            //            }
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
        @Implement(Check.class)
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
        @Implement(Check.class)
        protected double getValue (GlyphContext context)
        {
            Glyph     stick = context.stick;
            Point2D   stop = stick.getStopPoint();

            // Which staff area contains the bottom of the stick?
            StaffInfo staff = staffManager.getStaffAt(stop);

            // How far are we from the stop of the staff?
            double staffBottom = staff.getLastLine()
                                      .yAt(stop.getX());
            double dy = sheet.getScale()
                             .pixelsToFrac(Math.abs(staffBottom - stop.getY()));

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

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction  chunkWidth = new Scale.Fraction(
            0.3,
            "Width of box to look for chunks");
        Scale.Fraction  chunkHalfHeight = new Scale.Fraction(
            0.5,
            "Half height of box to look for chunks");
        Constant.Ratio  chunkRatioHigh = new Constant.Ratio(
            1.8,
            "High Minimum ratio of alien pixels to detect chunks");
        Constant.Ratio  chunkRatioLow = new Constant.Ratio(
            1.0,
            "Low Minimum ratio of alien pixels to detect chunks");
        Scale.Fraction  maxStaffShiftDyHigh = new Scale.Fraction(
            0.4,
            "High Maximum vertical distance between a bar edge and the staff line");
        Scale.Fraction  maxStaffShiftDyHighRough = new Scale.Fraction(
            4.0,
            "Rough high Maximum vertical distance between a bar edge and the staff line");
        Scale.Fraction  maxStaffShiftDyLow = new Scale.Fraction(
            0.2,
            "Low Maximum vertical distance between a bar edge and the staff line");
        Scale.Fraction  maxStaffShiftDyLowRough = new Scale.Fraction(
            2.0,
            "Rough low Maximum vertical distance between a bar edge and the staff line");
        Scale.Fraction  maxStaffDHeightHigh = new Scale.Fraction(
            0.4,
            "High Maximum difference between a bar length and min staff height");
        Scale.Fraction  maxStaffDHeightHighRough = new Scale.Fraction(
            1,
            "Rough high Maximum difference between a bar length and min staff height");
        Scale.Fraction  maxStaffDHeightLow = new Scale.Fraction(
            0.2,
            "Low Maximum difference between a bar length and min staff height");
        Scale.Fraction  maxStaffDHeightLowRough = new Scale.Fraction(
            0.5,
            "Rough low Maximum difference between a bar length and min staff height");
        Scale.Fraction  minStaffDxHigh = new Scale.Fraction(
            0,
            "High Minimum horizontal distance between a bar and a staff edge");
        Scale.Fraction  minStaffDxHighRough = new Scale.Fraction(
            -3,
            "Rough high Minimum horizontal distance between a bar and a staff edge");
        Scale.Fraction  minStaffDxLow = new Scale.Fraction(
            -1,
            "Low Minimum horizontal distance between a bar and a staff edge");
        Scale.Fraction  minStaffDxLowRough = new Scale.Fraction(
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
        Scale.Fraction  minRadiusLow = new Scale.Fraction(
            25,
            "Low minimum radius");
        Scale.Fraction  minRadiusHigh = new Scale.Fraction(
            60,
            "High minimum radius");
        Scale.Fraction  maxThinWidth = new Scale.Fraction(
            0.3,
            "Maximum width of a normal bar, versus a thick bar");
        Check.Grade     minCheckResult = new Check.Grade(
            0.50,
            "Minimum result for suite of check");
        Constant.Ratio  minStaffCountForLongBar = new Constant.Ratio(
            2,
            "Minimum length for long bars, stated in number of staff heights");
        Constant.Ratio  booleanThreshold = new Constant.Ratio(
            0.5,
            "* DO NOT EDIT * - switch between true & false for a boolean");
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

        protected ChunkCheck (String        name,
                              String        description,
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

        protected abstract PixelRectangle getBox (Glyph stick);

        @Implement(Check.class)
        protected double getValue (GlyphContext context)
        {
            Glyph          stick = context.stick;
            PixelRectangle box = getBox(stick);

            int            aliens = getAlienPixelsIn(stick, box);
            int            area = box.width * box.height;

            // Normalize the ratio with stick length
            double ratio = (1000 * aliens) / ((double) area * stick.getLength(
                Orientation.VERTICAL));

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Glyph#" + stick.getId() + " " + getName() + " aliens:" +
                    aliens + " area:" + area + " ratio:" + (float) ratio);
            }

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
                "Check there is no big chunk stuck on lower left side of stick",
                BOTTOM_LEFT_CHUNK);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected PixelRectangle getBox (Glyph stick)
        {
            Point2D        bottom = stick.getStopPoint();
            PixelRectangle box = new PixelRectangle(
                (int) Math.rint(bottom.getX() - nWidth),
                (int) Math.rint(bottom.getY() - (1.5 * nHeight)),
                nWidth,
                2 * nHeight);
            stick.addAttachment("BL", box);

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
                "Check there is no big chunk stuck on lower right side of stick",
                BOTTOM_RIGHT_CHUNK);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected PixelRectangle getBox (Glyph stick)
        {
            Point2D        bottom = stick.getStopPoint();
            PixelRectangle box = new PixelRectangle(
                (int) Math.rint(bottom.getX()),
                (int) Math.rint(bottom.getY() - (1.5 * nHeight)),
                nWidth,
                2 * nHeight);
            stick.addAttachment("BR", box);

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
        @Implement(Check.class)
        protected double getValue (GlyphContext context)
        {
            Glyph stick = context.stick;
            int   height = Integer.MAX_VALUE;

            // Check wrt every staff in the stick getRange
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaffInfo area = staffManager.getStaff(i);
                height = Math.min(height, area.getHeight());
            }

            return sheet.getScale()
                        .pixelsToFrac(
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
        @Implement(Check.class)
        protected double getValue (GlyphContext context)
        {
            Glyph  stick = context.stick;
            double dist = Double.MAX_VALUE;

            // Check wrt every staff in the stick range
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaffInfo staff = staffManager.getStaff(i);
                Point2D   top = staff.getFirstLine()
                                     .getEndPoint(LEFT);
                Point2D   bot = staff.getLastLine()
                                     .getEndPoint(LEFT);
                double    y = (top.getY() + bot.getY()) / 2;
                double    x = stick.getPositionAt(y, Orientation.VERTICAL);
                double    dx = x - staff.getAbscissa(LEFT);
                dist = Math.min(dist, dx);
            }

            return sheet.getScale()
                        .pixelsToFrac(dist);
        }
    }

    //-----------//
    // LongCheck //
    //-----------//
    /**
     * This kind of check allows to force the result in certain circumstances
     */
    private abstract class LongCheck
        extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        public LongCheck (String          name,
                          String          description,
                          Constant.Double low,
                          Constant.Double high,
                          boolean         covariant,
                          FailureResult   redResult)
        {
            super(name, description, low, high, covariant, redResult);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public CheckResult pass (GlyphContext context,
                                 CheckResult  result,
                                 boolean      update)
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
        @Implement(Check.class)
        protected double getValue (GlyphContext context)
        {
            Glyph stick = context.stick;

            if (stick instanceof Filament) {
                Filament fil = (Filament) stick;

                return sheet.getScale()
                            .pixelsToFrac(fil.getMeanCurvature());
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
        @Implement(Check.class)
        protected double getValue (GlyphContext context)
        {
            Glyph  stick = context.stick;
            double dist = Double.MAX_VALUE;

            // Check wrt every staff in the stick getRange
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaffInfo staff = staffManager.getStaff(i);
                Point2D   top = staff.getFirstLine()
                                     .getEndPoint(RIGHT);
                Point2D   bot = staff.getLastLine()
                                     .getEndPoint(RIGHT);
                double    y = (top.getY() + bot.getY()) / 2;
                double    x = (stick instanceof Filament)
                              ? ((Filament) stick).getPositionAt(
                    y,
                    Orientation.VERTICAL) : stick.getLine()
                                                 .xAtY(y);
                dist = Math.min(dist, staff.getAbscissa(RIGHT) - x);
            }

            return sheet.getScale()
                        .pixelsToFrac(dist);
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
        @Implement(Check.class)
        protected double getValue (GlyphContext context)
        {
            Glyph     stick = context.stick;
            Point2D   start = stick.getStartPoint();

            // Which staff area contains the top of the stick?
            StaffInfo staff = staffManager.getStaffAt(start);

            // How far are we from the start of the staff?
            double staffTop = staff.getFirstLine()
                                   .yAt(start.getX());
            double dy = sheet.getScale()
                             .pixelsToFrac(Math.abs(staffTop - start.getY()));

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
                "Check there is no big chunk stuck on upper left side of stick",
                TOP_LEFT_CHUNK);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected PixelRectangle getBox (Glyph stick)
        {
            Point2D        top = stick.getStartPoint();
            PixelRectangle box = new PixelRectangle(
                (int) Math.rint(top.getX() - nWidth),
                (int) Math.rint(top.getY() - (nHeight / 2)),
                nWidth,
                2 * nHeight);
            stick.addAttachment("TL", box);

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
            super(
                "TRChunk",
                "Check there is no big chunk stuck on upper right side of stick",
                TOP_RIGHT_CHUNK);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected PixelRectangle getBox (Glyph stick)
        {
            Point2D        top = stick.getStartPoint();
            PixelRectangle box = new PixelRectangle(
                (int) Math.rint(top.getX()),
                (int) Math.rint(top.getY() - (nHeight / 2)),
                nWidth,
                2 * nHeight);
            stick.addAttachment("TR", box);

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
            super(
                "Vertical",
                "Check that stick is vertical, according to global slope",
                constants.maxSlopeLow,
                constants.maxSlopeHigh,
                false,
                NON_VERTICAL);
        }

        //~ Methods ------------------------------------------------------------

        // Retrieve the difference between stick slope and global slope
        @Implement(Check.class)
        protected double getValue (GlyphContext context)
        {
            Glyph   stick = context.stick;
            Point2D start = stick.getStartPoint();
            Point2D stop = stick.getStopPoint();

            // Beware of sign of stickSlope (it is opposite of globalSlope)
            double stickSlope = -(stop.getX() - start.getX()) / (stop.getY() -
                                                                start.getY());

            return Math.abs(stickSlope - sheet.getSkew().getSlope());
        }
    }
}
