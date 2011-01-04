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
import omr.check.CheckSuite;
import omr.check.Checkable;
import omr.check.FailureResult;
import omr.check.Result;
import omr.check.SuccessResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.Shape;
import omr.glyph.facets.Stick;

import omr.lag.JunctionDeltaPolicy;
import omr.lag.SectionsBuilder;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.step.StepException;

import omr.util.Implement;

import net.jcip.annotations.NotThreadSafe;

import java.util.*;

/**
 * Class <code>BarsChecker</code> is a (package private) companion of class
 * {@link SystemsBuilder}, dedicated to physical checks of vertical sticks that
 * are candidates for barlines.
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

    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final Sheet sheet;

    /** Related scale */
    private final Scale scale;

    /** Related vertical lag */
    private final GlyphLag lag;

    /** Suite of checks to be performed */
    private final BarCheckSuite suite;

    /** Related context of a bar stick */
    private final Map<Stick, GlyphContext> contexts = new HashMap<Stick, GlyphContext>();

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // BarsChecker //
    //-------------//
    /**
     * Prepare a bar checker on the provided sheet
     *
     * @param sheet the sheet to process
     * @param lag the sheet vertical lag
     */
    public BarsChecker (Sheet    sheet,
                        GlyphLag lag)
    {
        this.sheet = sheet;
        this.lag = lag;

        scale = sheet.getScale();
        suite = new BarCheckSuite();
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // retrieveCandidates //
    //--------------------//
    /**
     * From the list of vertical sticks, this method uses several tests based on
     * stick location, and stick shape (the test is based on adjacency, it
     * should be improved), to provide the initial collection of good barlines
     * candidates.
     *
     * @throws StepException Raised when processing goes wrong
     */
    public void retrieveCandidates ()
        throws StepException
    {
        // Populate the vertical lag of runs
        SectionsBuilder<GlyphLag, GlyphSection> lagBuilder;
        lagBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
            lag,
            new JunctionDeltaPolicy(scale.toPixels(constants.maxDeltaLength)));
        lagBuilder.createSections(sheet.getPicture(), 0); // 0 = minRunLength

        // Retrieve (vertical) sticks
        VerticalArea barsArea = new VerticalArea(
            sheet,
            lag,
            scale.toPixels(constants.maxBarThickness));

        // Register these sticks as standard lag glyphs
        for (Stick stick : barsArea.getSticks()) {
            lag.addGlyph(stick);
        }

        // Sort bar candidates according to their abscissa
        Collections.sort(barsArea.getSticks(), Stick.midPosComparator);

        double minResult = constants.minCheckResult.getValue();

        // Check each candidate stick in turn
        for (Stick stick : barsArea.getSticks()) {
            // Allocate the candidate context, and pass the whole check suite
            GlyphContext context = new GlyphContext(stick);
            double       res = suite.pass(context);

            if (logger.isFineEnabled()) {
                logger.fine("suite => " + res + " for " + stick);
            }

            if (res >= minResult) {
                // OK, we flag this candidate with proper barline shape
                contexts.put(stick, context);
                stick.setShape(
                    isThickBar(stick) ? Shape.THICK_BARLINE : Shape.THIN_BARLINE);

                // Additional processing for Bars that define a system or a part
                // (they start AND end with precise staves horizontal limits)
                if ((context.topIdx != -1) && (context.botIdx != -1)) {
                    // Here, we have both part & system defining bars
                    // System bars occur first
                    // (since glyphs are sorted by increasing abscissa)
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
    }

    //-----------------//
    // getStaffAnchors //
    //-----------------//
    /**
     * Report the indices of the staves at the top and at the bottom of the
     * provided stick. (Used by package mate SystemsBuilder)
     * @param stick the stick to lookup
     * @return a pair of staff indices, top & bottom, or null if the stick is
     * not known
     */
    StaffAnchors getStaffAnchors (Stick stick)
    {
        GlyphContext context = contexts.get(stick);

        if (context == null) {
            context = new GlyphContext(stick);
            suite.pass(context);
            contexts.put(stick, context);
        }

        return new StaffAnchors(context.topIdx, context.botIdx);
    }

    //----------//
    // getSuite //
    //----------//
    /**
     * Report the suite currently defined (used by package mate SystemsBuilder)
     *
     * @return the check suite
     */
    CheckSuite<GlyphContext> getSuite ()
    {
        return suite;
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

    //~ Inner Classes ----------------------------------------------------------

    //--------------//
    // StaffAnchors //
    //--------------//
    public static class StaffAnchors
    {
        //~ Instance fields ----------------------------------------------------

        final int top;
        final int bot;

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

        public GlyphContext (Stick stick)
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
        extends CheckSuite<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        public BarCheckSuite ()
        {
            super("Bar", constants.minCheckResult.getValue());

            // Be very careful with check order, because of side-effects
            add(1, new TopCheck()); // Side-effect: may set topIdx
            add(1, new BottomCheck()); // Side-effect: may set botIdx
            add(1, new HeightDiffCheck());
            add(1, new AnchorCheck());
            add(1, new LeftCheck());
            add(1, new RightCheck());
            add(1, new TopLeftChunkCheck());
            add(1, new TopRightChunkCheck());
            add(1, new BottomLeftChunkCheck());
            add(1, new BottomRightChunkCheck());

            if (logger.isFineEnabled()) {
                dump();
            }
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
        extends Check<GlyphContext>
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
        protected double getValue (GlyphContext context)
        {
            Stick stick = context.stick;
            int   stop = stick.getStop();

            // Which staff area contains the bottom of the stick?
            context.bottomArea = sheet.getStaffIndexAtY(stop);

            StaffInfo area = sheet.getStaves()
                                  .get(context.bottomArea);

            // How far are we from the stop of the staff?
            int    staffBottom = area.getLastLine()
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

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxBarThickness = new Scale.Fraction(
            1.0,
            "Maximum thickness of an interesting vertical stick");
        Scale.Fraction maxDeltaLength = new Scale.Fraction(
            0.2,
            "Maximum difference in run length to be part of the same section");
        Scale.Fraction chunkWidth = new Scale.Fraction(
            0.3,
            "Width of box to look for chunks");
        Scale.Fraction chunkHalfHeight = new Scale.Fraction(
            0.5,
            "Half height of box to look for chunks");
        Constant.Ratio chunkRatioHigh = new Constant.Ratio(
            1.8,
            "High Minimum ratio of alien pixels to detect chunks");
        Constant.Ratio chunkRatioLow = new Constant.Ratio(
            1.0,
            "Low Minimum ratio of alien pixels to detect chunks");
        Scale.Fraction maxStaffShiftDyHigh = new Scale.Fraction(
            4.0,
            "High Maximum vertical distance between a bar edge and the staff line");
        Scale.Fraction maxStaffShiftDyLow = new Scale.Fraction(
            0.27,
            "Low Maximum vertical distance between a bar edge and the staff line");
        Scale.Fraction maxStaffDHeightHigh = new Scale.Fraction(
            0.4,
            "High Maximum difference between a bar length and min staff height");
        Scale.Fraction maxStaffDHeightLow = new Scale.Fraction(
            0.2,
            "Low Maximum difference between a bar length and min staff height");
        Scale.Fraction minStaffDxHigh = new Scale.Fraction(
            0,
            "High Minimum horizontal distance between a bar and a staff edge");
        Scale.Fraction minStaffDxLow = new Scale.Fraction(
            0,
            "Low Minimum horizontal distance between a bar and a staff edge");
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

        protected abstract PixelRectangle getBox (Stick stick);

        @Implement(Check.class)
        protected double getValue (GlyphContext context)
        {
            Stick          stick = context.stick;
            PixelRectangle box = getBox(stick);

            int            aliens = stick.getAlienPixelsIn(
                lag.switchRef(box, null));
            int            area = box.width * box.height;

            // Normalize the ratio with stick length
            double ratio = (1000 * aliens) / ((double) area * stick.getLength());

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
        protected PixelRectangle getBox (Stick stick)
        {
            PixelPoint     bottom = stick.getStopPoint();
            PixelRectangle box = new PixelRectangle(
                bottom.x - nWidth,
                bottom.y - ((3 * nHeight) / 2),
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
        protected PixelRectangle getBox (Stick stick)
        {
            PixelPoint     bottom = stick.getStopPoint();
            PixelRectangle box = new PixelRectangle(
                bottom.x,
                bottom.y - ((3 * nHeight) / 2),
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
                constants.maxStaffDHeightLow,
                constants.maxStaffDHeightHigh,
                false,
                TOO_SHORT_BAR);
        }

        //~ Methods ------------------------------------------------------------

        // Retrieve the length data
        @Implement(Check.class)
        protected double getValue (GlyphContext context)
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
        extends Check<GlyphContext>
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
        protected double getValue (GlyphContext context)
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
                constants.minStaffDxLow,
                constants.minStaffDxHigh,
                true,
                OUTSIDE_STAFF_WIDTH);
        }

        //~ Methods ------------------------------------------------------------

        // Retrieve the stick abscissa
        @Implement(Check.class)
        protected double getValue (GlyphContext context)
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
        extends Check<GlyphContext>
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
        protected double getValue (GlyphContext context)
        {
            Stick stick = context.stick;
            int   start = stick.getStart();

            // Which staff area contains the top of the stick?
            context.topArea = sheet.getStaffIndexAtY(start);

            StaffInfo area = sheet.getStaves()
                                  .get(context.topArea);

            // How far are we from the start of the staff?
            int    staffTop = area.getFirstLine()
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
        protected PixelRectangle getBox (Stick stick)
        {
            PixelPoint     top = stick.getStartPoint();
            PixelRectangle box = new PixelRectangle(
                top.x - nWidth,
                top.y - (nHeight / 2),
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
        protected PixelRectangle getBox (Stick stick)
        {
            PixelPoint     top = stick.getStartPoint();
            PixelRectangle box = new PixelRectangle(
                top.x,
                top.y - (nHeight / 2),
                nWidth,
                2 * nHeight);
            stick.addAttachment("TR", box);

            return box;
        }
    }
}
