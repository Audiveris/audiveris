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

import omr.Main;

import omr.check.Check;
import omr.check.CheckBoard;
import omr.check.CheckResult;
import omr.check.CheckSuite;
import omr.check.Checkable;
import omr.check.Failure;
import omr.check.SuiteImpacts;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.Filament;
import omr.grid.StaffInfo;
import omr.grid.StaffManager;

import omr.lag.Lags;
import omr.lag.Section;

import omr.run.Orientation;
import static omr.run.Orientation.*;

import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.sig.BarlineInter;
import omr.sig.SIGraph;

import omr.step.Step;
import static omr.util.HorizontalSide.*;
import omr.util.Predicate;

import net.jcip.annotations.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Collection;

/**
 * Class {@code BarsChecker} is dedicated to physical checks of
 * vertical sticks that are candidates for bar lines.
 * <p>
 * Two instances are used during the processing of a given sheet:<ol>
 * <li>The first one focuses on system retrieval and uses only rough
 * checks.</li>
 * <li>The second one focuses on measure bars and uses more precise checks.</li>
 * </ol>
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
    private static final Logger logger = LoggerFactory.getLogger(
            BarsChecker.class);

    /** Failure, since bar is shorter than staff height */
    private static final Failure TOO_SHORT_BAR = new Failure("Bar-TooShort");

    /** Failure, since bar is on left or right side of the staff area */
    private static final Failure OUTSIDE_STAFF_WIDTH = new Failure(
            "Bar-OutsideStaffWidth");

    /** Failure, since bar goes too high above first staff */
    private static final Failure TOP_EXCESS = new Failure("Bar-TooHigh");

    /** Failure, since bar top is too low below first staff */
    private static final Failure LOW_TOP = new Failure("Bar-LowTop");

    /** Failure, since bar bottom is too high above last staff */
    private static final Failure HIGH_BOTTOM = new Failure("Bar-HighBottom");

    /** Failure, since bar goes too low under last staff */
    private static final Failure BOTTOM_EXCESS = new Failure("Bar-TooLow");

    /** Failure, since bar has no end aligned with a staff */
    private static final Failure NOT_STAFF_ANCHORED = new Failure(
            "Bar-NotStaffAnchored");

    /** Failure, since bar has a large chunk on the top left */
    private static final Failure TOP_LEFT_CHUNK = new Failure(
            "Bar-TopLeftChunk");

    /** Failure, since bar has a large chunk on the top right */
    private static final Failure TOP_RIGHT_CHUNK = new Failure(
            "Bar-TopRightChunk");

    /** Failure, since bar has a large chunk on the bottom left */
    private static final Failure BOTTOM_LEFT_CHUNK = new Failure(
            "Bar-BottomLeftChunk");

    /** Failure, since bar has a large chunk on the bottom right */
    private static final Failure BOTTOM_RIGHT_CHUNK = new Failure(
            "Bar-BottomRightChunk");

    /** Failure, since bar is too far from vertical */
    private static final Failure NON_VERTICAL = new Failure("Bar-NonVertical");

    /** Failure, since bar is too far from straight line */
    private static final Failure NON_STRAIGHT = new Failure("Bar-NonStraight");

    //~ Instance fields --------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** Related scale. */
    private final Scale scale;

    /** Related staves. */
    private final StaffManager staffManager;

    /**
     * Rough checks flag.
     * true for rough tests (when retrieving staff grid),
     * false for precise tests (when retrieving measure bar lines)
     */
    private final boolean rough;

    /** Suite of checks to be performed. (depends on 'rough' flag) */
    private final BarCheckSuite suite;

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

        if (Main.getGui() != null) {
            // Barline check board
            sheet.getAssembly()
                    .addBoard(
                            Step.DATA_TAB,
                            new BarCheckBoard(
                            suite,
                            sheet.getNest().getGlyphService(),
                            new Class<?>[]{GlyphEvent.class}));
        }
    }

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // checkCandidates //
    //-----------------//
    /**
     * From a list of vertical sticks, this method uses several tests
     * to provide the initial collection of good bar lines candidates.
     *
     * @param sticks the provided collection of candidate sticks
     */
    public void checkCandidates (Collection<? extends Glyph> sticks)
    {
        // Check each candidate stick in turn
        for (Glyph stick : sticks) {
            // Allocate the candidate context, and pass the whole check suite
            GlyphContext context = new GlyphContext(stick);
            SuiteImpacts impacts = suite.getImpacts(context);
            double grade = impacts.getGrade();

            if (logger.isDebugEnabled() || stick.isVip()) {
                logger.info("VIP check {}", impacts.getDump());
            }

            if ((stick.isBar() && stick.isManualShape())
                || (grade >= suite.getMinThreshold())) {
                // OK, we flag this candidate with proper barline shape
                // Check for an existing barline inter for this stick
                BarlineInter inter = (BarlineInter) SIGraph.getInter(
                        stick,
                        BarlineInter.class);

                if (inter == null) {
                    if ((!stick.isBar() || !stick.isManualShape())) {
                        stick.setShape(
                                isThickBar(stick) ? Shape.THICK_BARLINE
                                : Shape.THIN_BARLINE);
                    }

                    inter = new BarlineInter(
                            stick,
                            stick.getShape(),
                            impacts,
                            stick.getLine(),
                            stick.getMeanThickness(Orientation.VERTICAL));

                    SystemInfo system = sheet.getSystemOf(stick);
                    SIGraph sig = (system != null) ? system.getSig()
                            : sheet.getSheetSig();
                    sig.addVertex(inter);
                } else {
                    // Update assignment quality with more precise results
                    inter.setImpacts(impacts);
                }

                // Additional processing for Bars that define a system or a part
                // (they start AND end with precise staves horizontal limits)
                if ((context.topStaff != -1) && (context.botStaff != -1)) {
                    // Here, we have both part & system defining bars
                    // System bars occur first
                    // (since glyphs are sorted by increasing abscissa)
                    ///stick.setResult(BAR_PART_DEFINING);
                    inter.setPartDefining(true);

                    logger.debug(
                            "Part-defining Barline from staff {} to staff {} {}",
                            context.topStaff,
                            context.botStaff,
                            stick);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                                "Non-Part-defining Bar line {}{}",
                                (context.topStaff != -1)
                                ? (" topIdx="
                                   + context.topStaff) : "",
                                (context.botStaff != -1)
                                ? (" botIdx="
                                   + context.botStaff) : "");
                    }

                    ///stick.setResult(BAR_NOT_PART_DEFINING);
                    inter.setPartDefining(false);
                }
            } else {
                if (stick.isBar()) {
                    if (logger.isDebugEnabled() || stick.isVip()) {
                        logger.info(
                                "Purged {} {}",
                                stick.idString(),
                                stick.getShape());
                    }

                    stick.setShape(null);

                    // Remove barline inter if any
                    BarlineInter inter = (BarlineInter) SIGraph.getInter(
                            stick,
                            BarlineInter.class);

                    if (inter != null) {
                        inter.delete();
                    }
                }
            }
        }
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
                       || (section.getGlyph()
                        .getShape() != Shape.STAFF_LINE);
            }
        };

        int total = 0;

        for (String key : Arrays.asList(Lags.VLAG, Lags.HLAG)) {
            total += glyph.getAlienPixelsFrom(
                    sheet.getLag(key),
                    absRoi,
                    predicate);
        }

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
                stick.getMeanThickness(Orientation.VERTICAL));

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
        public void addFailure (Failure failure)
        {
            stick.addFailure(failure);
        }

        @Override
        public boolean isVip ()
        {
            return stick.isVip();
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
                    Constant.Double.HALF,
                    Constant.Double.HALF,
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
     * A specific board dedicated to interactive physical checks of
     * bar line sticks.
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
                    GlyphEvent glyphEvent = (GlyphEvent) event;
                    Glyph glyph = glyphEvent.getData();

                    if (glyph != null) {
                        // Make sure this is a rather vertical stick
                        if (Math.abs(glyph.getInvertedSlope()) <= constants.maxCoTangentForCheck.getValue()) {
                            // Apply a fresh suite
                            GlyphContext context = new GlyphContext(glyph);
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
                    "BotInter",
                    "Check that bottom of stick reaches last staff",
                    Scale.Fraction.ZERO,
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
            double staffBottom = staff.getLastLine()
                    .yAt(stop.getX());
            double dy = sheet.getScale()
                    .pixelsToFrac(Math.abs(staffBottom - stop.getY()));

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
                "High ratio of alien pixels to detect chunks (in 1/1000)");

        Scale.Fraction maxDyToStaffLine = new Scale.Fraction(
                0.4,
                "High vertical distance between bar edge and staff line");

        Scale.Fraction maxDyToStaffLine_Rough = new Scale.Fraction(
                4.0,
                "Rough high vertical distance between bar edge and staff line");

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

        Constant.Double slopeHigh = new Constant.Double(
                "tangent",
                0.1,
                "Maximum difference with global slope");

        Scale.Fraction radiusLow = new Scale.Fraction(
                25,
                "Low minimum radius");

        Scale.Fraction radiusHigh = new Scale.Fraction(
                60,
                "High minimum radius");

        Scale.Fraction maxThinWidth = new Scale.Fraction(
                0.3,
                "Maximum width of a normal bar, versus a thick bar");

        Constant.Ratio minStaffCountForLongBar = new Constant.Ratio(
                2,
                "Minimum length for long bars, stated in number of staff heights");

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
                              Failure redResult)
        {
            super(
                    name,
                    description,
                    Constant.Ratio.ZERO,
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

            // Normalize the ratio with stick length. TODO: WHY?????????
            double ratio = (1000 * aliens) / ((double) area * stick.getLength(
                    Orientation.VERTICAL));

            logger.debug(
                    "{} {} aliens:{} area:{} ratio:{}",
                    stick.idString(),
                    getName(),
                    aliens,
                    area,
                    (float) ratio);

            return ratio;
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
                          Failure redResult)
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
                result.grade = 1; //TODO: check this!

                return result;
            } else {
                return super.pass(context, result, update);
            }
        }
    }

    //---------------//
    // BarCheckSuite //
    //---------------//
    private class BarCheckSuite
            extends CheckSuite<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        public BarCheckSuite ()
        {
            super("Bar");

            // Be very careful with check order, because of side-effects
            // topArea, bottomArea, isPartDefining, isThick are already set
            //
            add(1, new SlopeCheck());
            add(0, new LeftCheck());
            add(0, new RightCheck());
            add(1, new HeightCheck());
            add(1, new RadiusCheck());

            add(1, new TopLeftChunkCheck());
            add(1, new TopRightChunkCheck());
            add(1, new BottomLeftChunkCheck());
            add(1, new BottomRightChunkCheck());

            add(1, new TopInterCheck());
            add(1, new BottomInterCheck());

            if (!rough) {
                add(1, new TopLineCheck()); // May set topStaff
                add(1, new BottomLineCheck()); // May set botStaff
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
                    "BL",
                    "Check for no big chunk on bottom left side",
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

    //-----------------//
    // BottomLineCheck //
    //-----------------//
    private class BottomLineCheck
            extends LongCheck
    {
        //~ Constructors -------------------------------------------------------

        protected BottomLineCheck ()
        {
            super(
                    "BotLine",
                    "Check that bottom of stick is close to bottom of staff",
                    Scale.Fraction.ZERO,
                    constants.maxDyToStaffLine,
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
            double staffBottom = staff.getLastLine()
                    .yAt(stop.getX());
            double dy = sheet.getScale()
                    .pixelsToFrac(Math.abs(staffBottom - stop.getY()));

            // Change limits according to rough & partDefining
            if (rough && context.isPartDefining) {
                setLowHigh(
                        Scale.Fraction.ZERO,
                        constants.maxDyToStaffLine_Rough);
            } else {
                setLowHigh(Scale.Fraction.ZERO, constants.maxDyToStaffLine);
            }

            // Side-effect
            if (dy <= getHigh()) {
                context.botStaff = context.bottomArea;
            }

            return dy;
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
                    "BR",
                    "Check for no big chunk on bottom right side",
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

    //-------------//
    // HeightCheck //
    //-------------//
    private class HeightCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        protected HeightCheck ()
        {
            super(
                    "Height",
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

            // Check wrt every staff in the stick range
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
        @Override
        protected double getValue (GlyphContext context)
        {
            Glyph stick = context.stick;
            double dist = Double.MAX_VALUE;

            // Check wrt every staff in the stick range
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaffInfo staff = staffManager.getStaff(i);
                Point2D top = staff.getFirstLine()
                        .getEndPoint(LEFT);
                Point2D bot = staff.getLastLine()
                        .getEndPoint(LEFT);
                double y = (top.getY() + bot.getY()) / 2;
                double x = stick.getPositionAt(y, Orientation.VERTICAL);
                double dx = x - staff.getAbscissa(LEFT);
                dist = Math.min(dist, dx);
            }

            return sheet.getScale()
                    .pixelsToFrac(dist);
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
                    constants.radiusLow,
                    constants.radiusHigh,
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

                double radius = fil.getMeanCurvature();

                if (radius != Double.POSITIVE_INFINITY) {
                    return sheet.getScale()
                            .pixelsToFrac(radius);
                }
            }

            // Straight line
            return Double.POSITIVE_INFINITY;
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
                Point2D top = staff.getFirstLine()
                        .getEndPoint(RIGHT);
                Point2D bot = staff.getLastLine()
                        .getEndPoint(RIGHT);
                double y = (top.getY() + bot.getY()) / 2;
                double x = (stick instanceof Filament)
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

    //------------//
    // SlopeCheck //
    //------------//
    /**
     * Check if stick is aligned with vertical.
     * (taking global sheet slope into account)
     */
    private class SlopeCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        protected SlopeCheck ()
        {
            super(
                    "Slope",
                    "Check that stick is vertical, according to global slope",
                    Constant.Double.ZERO,
                    constants.slopeHigh,
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
                    "Check that top of stick reaches first staff",
                    Scale.Fraction.ZERO,
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
            double staffTop = staff.getFirstLine()
                    .yAt(start.getX());
            double dy = sheet.getScale()
                    .pixelsToFrac(Math.abs(staffTop - start.getY()));

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
                    "TL",
                    "Check for no big chunk on top left side",
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

    //--------------//
    // TopLineCheck //
    //--------------//
    private class TopLineCheck
            extends LongCheck
    {
        //~ Constructors -------------------------------------------------------

        protected TopLineCheck ()
        {
            super(
                    "TopLine",
                    "Check that top of stick is close to top of staff",
                    Scale.Fraction.ZERO,
                    constants.maxDyToStaffLine,
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
            double staffTop = staff.getFirstLine()
                    .yAt(start.getX());
            double dy = sheet.getScale()
                    .pixelsToFrac(Math.abs(staffTop - start.getY()));

            // Change limits according to rough & partDefining
            if (rough && context.isPartDefining) {
                setLowHigh(
                        Scale.Fraction.ZERO,
                        constants.maxDyToStaffLine_Rough);
            } else {
                setLowHigh(Scale.Fraction.ZERO, constants.maxDyToStaffLine);
            }

            // Side-effect
            if (dy <= getHigh()) {
                context.topStaff = context.topArea;
            }

            return dy;
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
                    "TR",
                    "Check for no big chunk on top right side",
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
}
