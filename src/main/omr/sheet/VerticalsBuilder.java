//----------------------------------------------------------------------------//
//                                                                            //
//                      V e r t i c a l s B u i l d e r                       //
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
import omr.check.CheckSuite;
import omr.check.Checkable;
import omr.check.Failure;
import omr.check.SuiteImpacts;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.Shape;
import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;

import omr.grid.FilamentsFactory;
import omr.grid.StaffInfo;

import omr.image.PixelFilter;

import omr.lag.Section;

import omr.math.LineUtil;
import static omr.run.Orientation.*;

import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.step.Step;
import omr.step.StepException;

import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code VerticalsBuilder} is in charge of retrieving major
 * vertical seeds of a dedicated system.
 *
 * The purpose is to use these major vertical sticks as seeds for stems,
 * vertical edges of endings, and potential parts of alteration signs
 * (sharp, natural, flat).
 * <p>
 * Since bar lines are significantly longer that stems, they require to be based
 * on filaments (natural splines) rather than the straight lines used here.
 *
 * @author Hervé Bitteur
 */
public class VerticalsBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            VerticalsBuilder.class);

    /** Events this entity is interested in */
    private static final Class<?>[] eventClasses = new Class<?>[]{
        GlyphEvent.class
    };

    /** Global seed grade is too low. */
    private static final Failure TOO_LIMITED = new Failure("Stem-TooLimited");

    /** Length of seed is too short. */
    private static final Failure TOO_SHORT = new Failure("Stem-TooShort");

    /** Clean portion of seed is too short. */
    private static final Failure TOO_HIGH_ADJACENCY = new Failure(
            "Stem-TooHighAdjacency");

    /** Seed is located in 'DMZ' at beginning of staff. */
    private static final Failure IN_DMZ = new Failure("Stem-InDMZ");

    /** Core of seed contains too many white pixels. */
    private static final Failure TOO_HOLLOW = new Failure("Stem-TooHollow");

    /** Seed is not vertical enough. */
    private static final Failure NON_VERTICAL = new Failure("Stem-NonVertical");

    /** Seed is not straight enough. */
    private static final Failure NON_STRAIGHT = new Failure("Stem-NonStraight");

    //~ Instance fields --------------------------------------------------------
    /** The system to process. */
    private final SystemInfo system;

    /** Related sheet. */
    private final Sheet sheet;

    /** Global sheet scale. */
    private final Scale scale;

    /** Typical stem thickness, as read from scale. */
    private final double typicalWidth;

    /** Input image. (with staves removed) */
    private PixelFilter pixelFilter;

    /** Suite of checks for a vertical seed. */
    private final SeedCheckSuite suite = new SeedCheckSuite();

    //~ Constructors -----------------------------------------------------------
    //------------------//
    // VerticalsBuilder //
    //------------------//
    /**
     * Creates a new VerticalsBuilder object.
     *
     * @param system the underlying system
     */
    public VerticalsBuilder (SystemInfo system)
    {
        this.system = system;

        sheet = system.getSheet();
        scale = sheet.getScale();
        typicalWidth = scale.getMainStem();
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // addCheckBoard //
    //---------------//
    /**
     * Install a board dedicated to interactive stem seed check.
     */
    public void addCheckBoard ()
    {
        sheet.getAssembly()
                .addBoard(
                        Step.DATA_TAB,
                        new VertCheckBoard(sheet.getNest().getGlyphService(), eventClasses));
    }

    //----------------//
    // buildVerticals //
    //----------------//
    /**
     * Build the verticals seeds out of the dedicated system.
     *
     * @throws omr.step.StepException
     */
    public void buildVerticals ()
            throws StepException
    {
        // Cache input image
        Picture picture = sheet.getPicture();
        pixelFilter = (PixelFilter) picture.getSource(
                Picture.SourceKey.STAFF_LINE_FREE);

        // Retrieve candidates
        List<Glyph> candidates = retrieveCandidates();

        // Apply seed checks
        checkVerticals(candidates);
    }

    //-----------//
    // checkStem //
    //-----------//
    /**
     * Apply the check suite on provided stem candidate and report
     * the resulting impacts
     *
     * @param stick the stem candidate
     * @return the resulting impacts
     */
    public SuiteImpacts checkStem (Glyph stick)
    {
        return suite.getImpacts(new GlyphContext(stick));
    }

    //------------//
    // getMaxYGap //
    //------------//
    public int getMaxYGap ()
    {
        return scale.toPixels(constants.gapHigh);
    }

    //---------------------//
    // segmentGlyphOnStems //
    //---------------------//
    /**
     * Decompose the provided glyph into stems + leaves
     *
     * @param glyph the glyph to decompose
     */
    @Deprecated
    public void segmentGlyphOnStems (Glyph glyph)
    {
        logger.warn("Not implemented. Deprecated");
    }

    //----------------//
    // checkVerticals //
    //----------------//
    /**
     * This method checks for compliant vertical entities (stems)
     * within a collection of vertical sticks, and in the context of a
     * system.
     *
     * @param sticks the provided collection of vertical sticks
     */
    private void checkVerticals (Collection<Glyph> sticks)
    {
        int seedNb = 0;
        logger.debug("Searching verticals on {} sticks", sticks.size());

        for (Glyph stick : sticks) {
            stick = system.registerGlyph(stick);

            if (stick.isVip()) {
                logger.info("VIP checkVerticals for {}", stick);
            }

            // Check seed is not in DMZ
            Point center = stick.getAreaCenter();
            StaffInfo staff = system.getStaffAt(center);

            if (center.x < staff.getDmzEnd()) {
                stick.addFailure(IN_DMZ);

                continue;
            }

            // Run the suite of checks
            double res = suite.pass(new GlyphContext(stick), null);

            ///logger.debug("suite=> {} for {}", res, stick);
            if (res >= suite.getMinThreshold()) {
                stick.setShape(Shape.VERTICAL_SEED);
                seedNb++;
            } else {
                stick.addFailure(TOO_LIMITED);
            }
        }

        logger.debug("{}verticals: {}", system.getLogPrefix(), seedNb);
    }

    //---------------//
    // getCleanValue //
    //---------------//
    /**
     * Retrieve the cumulated length of stick portions without items
     * on left or right side.
     *
     * @param stick the stick to check
     * @return the clean length of the stick
     */
    private int getCleanValue (Glyph stick)
    {
        final int dx = scale.toPixels(constants.beltMarginDx);
        final Point2D start = stick.getStartPoint(VERTICAL);
        final Point2D stop = stick.getStopPoint(VERTICAL);
        final double halfWidth = (typicalWidth - 1) / 2;

        // Theoretical stem vertical lines on left and right
        final Line2D leftLine = new Line2D.Double(
                new Point2D.Double(start.getX() - halfWidth, start.getY()),
                new Point2D.Double(stop.getX() - halfWidth, stop.getY()));
        final Line2D rightLine = new Line2D.Double(
                new Point2D.Double(start.getX() + halfWidth, start.getY()),
                new Point2D.Double(stop.getX() + halfWidth, stop.getY()));
        final Rectangle stickBox = stick.getBounds();

        // Inspect each horizontal row
        int emptyCount = 0; // Count of rows where stem is white (broken)
        int leftCount = 0; // Count of rows where stem has item on left
        int rightCount = 0; // Count of rows where stem has item on right
        int bothCount = 0; // Count of rows where stem has item on both
        int cleanCount = 0; // Count of rows where stem is bare (no item stuck)

        if (stick.isVip()) {
            logger.info("VIP getCleanValue for {}", stick);
        }

        for (int y = stickBox.y; y < (stickBox.y + stickBox.height); y++) {
            final int leftLimit = (int) Math.rint(
                    LineUtil.intersectionAtY(leftLine, y).getX());
            final int rightLimit = (int) Math.rint(
                    LineUtil.intersectionAtY(rightLine, y).getX());

            // Make sure the stem row is not empty
            boolean empty = true;

            for (int x = leftLimit; x <= rightLimit; x++) {
                if (pixelFilter.isFore(x, y)) {
                    empty = false;

                    break;
                }
            }

            if (empty) {
                emptyCount++;

                continue;
            }

            // Item on left?
            boolean onLeft = true;

            for (int x = leftLimit; x >= (leftLimit - dx); x--) {
                if (!pixelFilter.isFore(x, y)) {
                    onLeft = false;

                    break;
                }
            }

            // Item on right?
            boolean onRight = true;

            for (int x = rightLimit; x <= (rightLimit + dx); x++) {
                if (!pixelFilter.isFore(x, y)) {
                    onRight = false;

                    break;
                }
            }

            if (onLeft && onRight) {
                bothCount++;
            } else if (onLeft) {
                leftCount++;
            } else if (onRight) {
                rightCount++;
            } else {
                cleanCount++;
            }
        }

        if (stick.isVip()) {
            logger.info(
                    "#{} empty:{} both:{} left:{} right:{} clean:{}",
                    stick.getId(),
                    emptyCount,
                    bothCount,
                    leftCount,
                    rightCount,
                    cleanCount);
        }

        return cleanCount;
    }

    //--------------------//
    // retrieveCandidates //
    //--------------------//
    /**
     * Retrieve all system sticks that could be seed candidates.
     *
     * @return the collection of sticks found in the system
     */
    private List<Glyph> retrieveCandidates ()
    {
        // Select suitable sections
        // Since we are looking for major seeds, we'll use only vertical sections
        List<Section> sections = new ArrayList<Section>();
        Predicate<Section> predicate = new MySectionPredicate();

        for (Section section : system.getVerticalSections()) {
            if (predicate.check(section)) {
                sections.add(section);
            }
        }

        // Use filament factory with straight lines as default
        final FilamentsFactory factory = new FilamentsFactory(
                scale,
                sheet.getNest(),
                GlyphLayer.DEFAULT,
                VERTICAL,
                BasicGlyph.class);

        // Adjust factory parameters
        factory.setMaxThickness(scale.getMainStem());
        factory.setMaxOverlapDeltaPos(constants.maxOverlapDeltaPos);
        factory.setMaxOverlapSpace(constants.maxOverlapSpace);
        factory.setMaxCoordGap(constants.maxCoordGap);

        if (system.getId() == 1) {
            factory.dump("VerticalsBuilder factory");
        }

        // Retrieve candidates
        return factory.retrieveFilaments(sections);
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.LineFraction maxOverlapDeltaPos = new Scale.LineFraction(
                1.0,
                "Maximum delta position between two overlapping filaments");

        Scale.LineFraction maxOverlapSpace = new Scale.LineFraction(
                0.3,
                "Maximum space between overlapping filaments");

        Scale.Fraction maxCoordGap = new Scale.Fraction(
                0,
                "Maximum delta coordinate for a gap between filaments");

        Scale.Fraction beltMarginDx = new Scale.Fraction(
                0.15,
                "Horizontal belt margin checked around stem");

        Check.Grade minCheckResult = new Check.Grade(
                0.2,
                "Minimum result for suite of check");

        Check.Grade goodCheckResult = new Check.Grade(
                0.5,
                "Good result for suite of check");

        Scale.Fraction blackHigh = new Scale.Fraction(
                2.5,
                "High length for a stem");

        Scale.Fraction blackLow = new Scale.Fraction(
                1.5,
                "Low length for a stem");

        Scale.Fraction cleanHigh = new Scale.Fraction(
                2.0,
                "High clean length for a stem");

        Scale.Fraction cleanLow = new Scale.Fraction(
                0.75,
                "Low clean length for a stem");

        Scale.Fraction gapHigh = new Scale.Fraction(
                2.0,
                "High vertical gap between stem segments");

        Constant.Double slopeHigh = new Constant.Double(
                "tangent",
                0.2,
                "High difference with global slope");

        Scale.Fraction straightHigh = new Scale.Fraction(
                0.2,
                "High mean distance to average stem line");

        Constant.Double maxCoTangentForCheck = new Constant.Double(
                "cotangent",
                0.1,
                "Maximum cotangent for interactive check of a stem candidate");

    }

    //--------------//
    // GlyphContext //
    //--------------//
    private static class GlyphContext
            implements Checkable
    {
        //~ Instance fields ----------------------------------------------------

        /** The stick being checked. */
        Glyph stick;

        /** Length of largest gap found. */
        int gap;

        /** Total length of black portions of stem. */
        int black;

        /** Total length of white portions of stem. */
        int white;

        //~ Constructors -------------------------------------------------------
        public GlyphContext (Glyph stick)
        {
            this.stick = stick;
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

    //------------//
    // BlackCheck //
    //------------//
    /**
     * Check stick length of black parts.
     */
    private class BlackCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        protected BlackCheck ()
        {
            super(
                    "Black",
                    "Check that black part of stick is long enough",
                    constants.blackLow,
                    constants.blackHigh,
                    true,
                    TOO_SHORT);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the length data
        @Override
        protected double getValue (GlyphContext context)
        {
            return scale.pixelsToFrac(context.black);
        }
    }

    //------------//
    // CleanCheck //
    //------------//
    /**
     * Check the length of stem portions with no item stuck either on
     * left, right or both sides.
     * As a side-effect, additional data is stored in context: white, black and
     * gap fields.
     */
    private class CleanCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        protected CleanCheck ()
        {
            super(
                    "Clean",
                    "Check total clean length",
                    constants.cleanLow,
                    constants.cleanHigh,
                    true,
                    TOO_HIGH_ADJACENCY);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected double getValue (GlyphContext context)
        {
            final Glyph stick = context.stick;
            final int dx = scale.toPixels(constants.beltMarginDx);
            final Point2D start = stick.getStartPoint(VERTICAL);
            final Point2D stop = stick.getStopPoint(VERTICAL);
            final double halfWidth = (typicalWidth - 1) / 2;

            {
                // Sanity check
                double invSlope = LineUtil.getInvertedSlope(start, stop);

                if (Double.isNaN(invSlope)
                    || Double.isInfinite(invSlope)
                    || (Math.abs(invSlope) > 0.5)) {
                    if (stick.isVip()) {
                        logger.info("VIP too far from vertical {}", stick);
                    }

                    return 0;
                }
            }

            // Theoretical stem vertical lines on left and right sides
            final Line2D leftLine = new Line2D.Double(
                    new Point2D.Double(start.getX() - halfWidth, start.getY()),
                    new Point2D.Double(stop.getX() - halfWidth, stop.getY()));
            final Line2D rightLine = new Line2D.Double(
                    new Point2D.Double(start.getX() + halfWidth, start.getY()),
                    new Point2D.Double(stop.getX() + halfWidth, stop.getY()));
            final Rectangle stickBox = stick.getBounds();

            // Inspect each horizontal row of the stick
            int largestGap = 0; // Largest gap so far
            int lastBlackY = -1; // Ordinate of last black row
            int lastWhiteY = -1; // Ordinate of last white row
            int whiteCount = 0; // Count of rows where stem is white (broken)
            int leftCount = 0; // Count of rows where stem has item on left
            int rightCount = 0; // Count of rows where stem has item on right
            int bothCount = 0; // Count of rows where stem has item on both
            int cleanCount = 0; // Count of rows where stem is bare (no item stuck)

            final int yMin = stickBox.y;
            final int yMax = (stickBox.y + stickBox.height) - 1;

            if (stick.isVip()) {
                logger.info("VIP CleanCheck for {}", stick);
            }

            for (int y = yMin; y <= yMax; y++) {
                final int leftLimit = (int) Math.rint(
                        LineUtil.intersectionAtY(leftLine, y).getX());
                final int rightLimit = (int) Math.rint(
                        LineUtil.intersectionAtY(rightLine, y).getX());

                // Make sure the stem row is not empty
                // (top & bottom rows cannot be considered as empty)
                if ((y != yMin) && (y != yMax)) {
                    boolean empty = true;

                    for (int x = leftLimit; x <= rightLimit; x++) {
                        if (pixelFilter.isFore(x, y)) {
                            empty = false;

                            break;
                        }
                    }

                    if (empty) {
                        whiteCount++;
                        lastWhiteY = y;

                        continue;
                    }
                }

                // End of gap?
                if ((lastWhiteY != -1) && (lastBlackY != -1)) {
                    largestGap = Math.max(largestGap, lastWhiteY - lastBlackY);
                    lastWhiteY = -1;
                }

                lastBlackY = y;

                // Item on left?
                boolean onLeft = true;

                for (int x = leftLimit; x >= (leftLimit - dx); x--) {
                    if (!pixelFilter.isFore(x, y)) {
                        onLeft = false;

                        break;
                    }
                }

                // Item on right?
                boolean onRight = true;

                for (int x = rightLimit; x <= (rightLimit + dx); x++) {
                    if (!pixelFilter.isFore(x, y)) {
                        onRight = false;

                        break;
                    }
                }

                if (onLeft && onRight) {
                    bothCount++;
                } else if (onLeft) {
                    leftCount++;
                } else if (onRight) {
                    rightCount++;
                } else {
                    cleanCount++;
                }
            }

            if (stick.isVip()) {
                logger.info(
                        "#{} gap:{} white:{} both:{} left:{} right:{} clean:{}",
                        stick.getId(),
                        largestGap,
                        whiteCount,
                        bothCount,
                        leftCount,
                        rightCount,
                        cleanCount);
            }

            // Side effect: update context data
            context.white = whiteCount;
            context.black = bothCount + leftCount + rightCount + cleanCount;
            context.gap = largestGap;

            return scale.pixelsToFrac(cleanCount);
        }
    }

    //----------//
    // GapCheck //
    //----------//
    /**
     * Check largest gap in stick.
     */
    private class GapCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        protected GapCheck ()
        {
            super(
                    "Gap",
                    "Check size of largest hole in stick",
                    Scale.Fraction.ZERO,
                    constants.gapHigh,
                    false,
                    TOO_HOLLOW);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the length data
        @Override
        protected double getValue (GlyphContext context)
        {
            return scale.pixelsToFrac(context.gap);
        }
    }

    //--------------------//
    // MySectionPredicate //
    //--------------------//
    /**
     * Predicate to filter relevant sections for seed candidates.
     */
    private class MySectionPredicate
            implements Predicate<Section>
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public boolean check (Section section)
        {
            // Check section is within system left and right boundaries
            Point center = section.getAreaCenter();

            return (center.x > system.getLeft())
                   && (center.x < system.getRight());
        }
    }

    //----------------//
    // SeedCheckSuite //
    //----------------//
    /**
     * The whole suite of checks meant for vertical seed candidates.
     */
    private class SeedCheckSuite
            extends CheckSuite<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        /**
         * Create a new instance
         */
        public SeedCheckSuite ()
        {
            super(
                    "Seed",
                    constants.minCheckResult.getValue(),
                    constants.goodCheckResult.getValue());

            add(1, new SlopeCheck());
            add(1, new StraightCheck());
            add(2, new CleanCheck());
            add(1, new BlackCheck()); // Needs CleanCheck output
            add(1, new GapCheck()); // Needs CleanCheck output

            if (logger.isDebugEnabled() && (system.getId() == 1)) {
                dump();
            }
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected void dumpSpecific (StringBuilder sb)
        {
            sb.append(String.format("%s%n", system));
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
            final Glyph stick = context.stick;
            Point2D start = stick.getStartPoint(VERTICAL);
            Point2D stop = stick.getStopPoint(VERTICAL);

            // Beware of sign of stickSlope (it is the opposite of globalSlope)
            double stickSlope = -(stop.getX() - start.getX()) / (stop.getY()
                                                                 - start.getY());

            return Math.abs(stickSlope - sheet.getSkew().getSlope());
        }
    }

    //---------------//
    // StraightCheck //
    //---------------//
    /**
     * Check if stick is straight.
     */
    private class StraightCheck
            extends Check<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        protected StraightCheck ()
        {
            super(
                    "Straight",
                    "Check that stick is straight",
                    Scale.Fraction.ZERO,
                    constants.straightHigh,
                    false,
                    NON_STRAIGHT);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected double getValue (GlyphContext context)
        {
            final Glyph stick = context.stick;

            return scale.pixelsToFrac(stick.getMeanDistance());
        }
    }

    //----------------//
    // VertCheckBoard //
    //----------------//
    /**
     * A board which runs and displays the detailed results of an
     * interactive seed check suite on the current glyph.
     */
    private class VertCheckBoard
            extends CheckBoard<GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        public VertCheckBoard (SelectionService eventService,
                               Class[] eventList)
        {
            super("SeedCheck", null, eventService, eventList);
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

                    if (glyph instanceof Glyph) {
                        // Make sure this is a rather vertical stick
                        if (Math.abs(glyph.getInvertedSlope()) <= constants.maxCoTangentForCheck.getValue()) {
                            // Apply the check suite
                            applySuite(suite, new GlyphContext(glyph));

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
}
