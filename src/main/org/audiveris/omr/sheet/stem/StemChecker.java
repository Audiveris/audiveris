//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S t e m C h e c k e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.stem;

import ij.process.ByteProcessor;

import org.audiveris.omr.check.Check;
import org.audiveris.omr.check.CheckBoard;
import org.audiveris.omr.check.CheckSuite;
import org.audiveris.omr.check.Failure;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.NearLine;
import org.audiveris.omr.math.LineUtil;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Profiles;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.SystemManager;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.selection.UserEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code StemChecker} checks a glyph as a suitable stem (or stem seed).
 *
 * @author Hervé Bitteur
 */
public class StemChecker
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StemChecker.class);

    /** Length of seed is too short. */
    private static final Failure TOO_SHORT = new Failure("Stem-TooShort");

    /** Clean portion of seed is too short. */
    private static final Failure TOO_HIGH_ADJACENCY = new Failure("Stem-TooHighAdjacency");

    /** Core of seed contains too many white pixels. */
    private static final Failure TOO_HOLLOW = new Failure("Stem-TooHollow");

    /** Core of seed contains a too large gap. */
    private static final Failure TOO_LARGE_GAP = new Failure("Stem-TooLargeGap");

    /** Seed is not vertical enough. */
    private static final Failure NON_VERTICAL = new Failure("Stem-NonVertical");

    /** Seed is not straight enough. */
    private static final Failure NON_STRAIGHT = new Failure("Stem-NonStraight");

    /** Events this entity is interested in. */
    private static final Class<?>[] eventClasses = new Class<?>[]{EntityListEvent.class};

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** Global sheet scale. */
    private final Scale scale;

    /** Input image. (with staves removed) */
    private final ByteProcessor pixelFilter;

    /** Profile level, defined at class level. */
    private final int classProfile;

    /** Cached suites, per profile. */
    private final Map<Integer, SeedCheckSuite> suiteMap = new TreeMap<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new StemChecker object, with system profile level.
     *
     * @param sheet the related sheet
     */
    public StemChecker (Sheet sheet)
    {
        this.sheet = sheet;

        classProfile = sheet.getStub().getProfile();
        scale = sheet.getScale();
        pixelFilter = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // addCheckBoard //
    //---------------//
    /**
     * Install a board dedicated to interactive stem seed check.
     */
    public void addCheckBoard ()
    {
        sheet.getStub().getAssembly().addBoard(
                SheetTab.DATA_TAB,
                new VertCheckBoard(sheet, sheet.getGlyphIndex().getEntityService(), eventClasses));
    }

    //-----------//
    // checkStem //
    //-----------//
    /**
     * Apply the check suite with desired profile on provided stem candidate
     * and report the resulting impacts.
     *
     * @param stick   the stem candidate
     * @param profile desired profile level
     * @return the resulting impacts
     */
    public GradeImpacts checkStem (NearLine stick,
                                   int profile)
    {
        return getSuite(profile).getImpacts(new StickContext(stick));
    }

    //-----------//
    // checkStem //
    //-----------//
    /**
     * Apply the check suite with class profile on provided stem candidate
     * and report the resulting impacts
     *
     * @param stick the stem candidate
     * @return the resulting impacts
     */
    public GradeImpacts checkStem (NearLine stick)
    {
        return checkStem(stick, classProfile);
    }

    //------------//
    // getMaxYGap //
    //------------//
    /**
     * Report the maximum acceptable vertical gap between stem chunks.
     *
     * @param profile desired profile level
     * @return the maximum acceptable vertical gap between chunks
     */
    public static Scale.Fraction getMaxYGap (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.gapHigh, profile);
    }

    //-----------------//
    // getMinThreshold //
    //-----------------//
    public double getMinThreshold (int profile)
    {
        return getSuite(profile).getMinThreshold();
    }

    //-----------------//
    // getMinThreshold //
    //-----------------//
    public double getMinThreshold ()
    {
        return getSuite(classProfile).getMinThreshold();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('@').append(Integer.toHexString(hashCode())).append('{');

        if (classProfile > 0) {
            sb.append(" profile:").append(classProfile);
        }

        return sb.append('}').toString();
    }

    //----------//
    // getSuite //
    //----------//
    /**
     * Report the check suite for vertical candidates, using profile defined for
     * this class instance.
     *
     * @return the check suite for candidates
     */
    private CheckSuite<StickContext> getSuite ()
    {
        return getSuite(classProfile);
    }

    //----------//
    // getSuite //
    //----------//
    /**
     * Report the check suite for vertical candidates, using desired profile level.
     *
     * @param profile desired profile level
     * @return the check suite for candidates
     */
    private CheckSuite<StickContext> getSuite (int profile)
    {
        SeedCheckSuite s = suiteMap.get(profile);

        if (s == null) {
            suiteMap.put(profile, s = new SeedCheckSuite(profile));
        }

        return s;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction beltMarginDx = new Scale.Fraction(
                0.15,
                "Horizontal belt margin checked around stem");

        private final Check.Grade minCheckResult = new Check.Grade(
                0.2,
                "Minimum result for suite of check");

        private final Check.Grade goodCheckResult = new Check.Grade(
                0.5,
                "Good result for suite of check");

        private final Scale.Fraction lengthHigh = new Scale.Fraction(
                3.5,
                "High minimum length for a stem");

        private final Scale.Fraction lengthLow = new Scale.Fraction(
                1.25,
                "Low minimum length for a stem");

        private final Scale.Fraction blackHigh = new Scale.Fraction(
                2.5,
                "High minimum black length for a stem");

        @SuppressWarnings("unused")
        private final Scale.Fraction blackHigh_p1 = new Scale.Fraction(1.25, "Idem for profile 1");

        private final Scale.Fraction blackLow = new Scale.Fraction(
                1.25,
                "Low minimum black length for a stem");

        @SuppressWarnings("unused")
        private final Scale.Fraction blackLow_p1 = new Scale.Fraction(0.625, "Idem for profile 1");

        private final Constant.Ratio blackRatioLow = new Constant.Ratio(
                0.5,
                "Low minimum ratio of black pixels for a stem");

        @SuppressWarnings("unused")
        private final Constant.Ratio blackRatioLow_p1 = new Constant.Ratio(
                0.3,
                "Idem for profile 1");

        @SuppressWarnings("unused")
        private final Constant.Ratio blackRatioLow_p2 = new Constant.Ratio(
                0.2,
                "Idem for profile 2");

        @SuppressWarnings("unused")
        private final Constant.Ratio blackRatioLow_p3 = new Constant.Ratio(
                0.1,
                "Idem for profile 3");

        private final Scale.Fraction cleanHigh = new Scale.Fraction(
                2.0,
                "High minimum clean length for a stem");

        private final Scale.Fraction cleanLow = new Scale.Fraction(
                0.5,
                "Low minimum clean length for a stem");

        @SuppressWarnings("unused")
        private final Scale.Fraction cleanLow_p1 = new Scale.Fraction(0.3, "Idem for profile 1");

        @SuppressWarnings("unused")
        private final Scale.Fraction cleanLow_p2 = new Scale.Fraction(0.2, "Idem for profile 2");

        @SuppressWarnings("unused")
        private final Scale.Fraction cleanLow_p3 = new Scale.Fraction(0.0, "Idem for profile 3");

        private final Scale.Fraction gapHigh = new Scale.Fraction(
                0,
                "Maximum vertical gap between stem segments");

        @SuppressWarnings("unused")
        private final Scale.Fraction gapHigh_p1 = new Scale.Fraction(0.3, "Idem for profile 1");

        @SuppressWarnings("unused")
        private final Scale.Fraction gapHigh_p2 = new Scale.Fraction(0.6, "Idem for profile 2");

        @SuppressWarnings("unused")
        private final Scale.Fraction gapHigh_p3 = new Scale.Fraction(2.0, "Idem for profile 3");

        @SuppressWarnings("unused")
        private final Scale.Fraction gapHigh_p4 = new Scale.Fraction(4.0, "Idem for profile 4");

        private final Constant.Double slopeHigh = new Constant.Double(
                "tangent",
                0.08,
                "Maximum difference with global slope");

        private final Scale.Fraction straightHigh = new Scale.Fraction(
                0.2,
                "High maximum distance to average stem line");

        private final Constant.Double maxCoTangentForCheck = new Constant.Double(
                "cotangent",
                0.1,
                "Maximum cotangent for visual check");
    }

    //--------------//
    // StickContext //
    //--------------//
    private static class StickContext
    {

        /** The stick being checked. */
        public final NearLine stick;

        /** Length of largest gap found. */
        public int gap;

        /** Total length of black portions of stem. */
        public int black;

        /** Total length of white portions of stem. */
        public int white;

        public StickContext (NearLine stick)
        {
            this.stick = stick;
        }

        @Override
        public String toString ()
        {
            return "stick#" + stick.getId();
        }
    }

    //----------------//
    // VertCheckBoard //
    //----------------//
    /**
     * A board which runs and displays the detailed results of an interactive seed
     * check suite on the current glyph.
     */
    private static class VertCheckBoard
            extends CheckBoard<StickContext>
    {

        private final Sheet sheet;

        VertCheckBoard (Sheet sheet,
                        SelectionService eventService,
                        Class[] eventList)
        {
            super("SeedCheck", null, eventService, eventList);
            this.sheet = sheet;
        }

        @Override
        public void onEvent (UserEvent event)
        {
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                if (event instanceof EntityListEvent) {
                    @SuppressWarnings("unchecked")
                    final EntityListEvent<Glyph> listEvent = (EntityListEvent<Glyph>) event;
                    final Glyph glyph = listEvent.getEntity();

                    if (glyph != null) {
                        // Make sure this is a rather vertical stick
                        if (Math.abs(glyph.getInvertedSlope()) <= constants.maxCoTangentForCheck
                                .getValue()) {
                            // Apply the check suite on each containing system
                            SystemManager systemManager = sheet.getSystemManager();

                            for (SystemInfo system : systemManager.getSystemsOf(glyph)) {
                                applySuite(
                                        new StemChecker(system.getSheet()).getSuite(),
                                        new StickContext(glyph));

                                return;
                            }
                        }
                    }

                    tellObject(null);
                }
            } catch (Exception ex) {
                logger.warn(getClass().getName() + " onEvent error", ex);
            }
        }
    }

    //------------//
    // BlackCheck //
    //------------//
    /**
     * Check stick length of black parts.
     */
    private class BlackCheck
            extends Check<StickContext>
    {

        protected BlackCheck (int profile)
        {
            super("Black",
                  "Check that black part of stick is long enough",
                  (Scale.Fraction) constants.getConstant(constants.blackLow, profile),
                  (Scale.Fraction) constants.getConstant(constants.blackHigh, profile),
                  true,
                  TOO_SHORT);
        }

        // Retrieve the length data
        @Override
        protected double getValue (StickContext context)
        {
            return scale.pixelsToFrac(context.black);
        }
    }

    //-----------------//
    // BlackRatioCheck //
    //-----------------//
    /**
     * Check stick ratio of black over stick length.
     */
    private class BlackRatioCheck
            extends Check<StickContext>
    {

        protected BlackRatioCheck (int profile)
        {
            super("BlackRatio",
                  "Check black ratio part over stick length",
                  (Constant.Ratio) constants.getConstant(constants.blackRatioLow, profile),
                  Constant.Ratio.ONE,
                  true,
                  TOO_HOLLOW);
        }

        // Retrieve ratio of black over whole length
        @Override
        protected double getValue (StickContext context)
        {
            return (double) context.black / (context.black + context.white);
        }
    }

    //------------//
    // CleanCheck //
    //------------//
    /**
     * Check the length of stem portions with no item stuck either on left, right or
     * both sides.
     * <p>
     * As a side-effect, additional data is stored in context: white, black and gap fields.
     */
    private class CleanCheck
            extends Check<StickContext>
    {

        protected CleanCheck (int profile)
        {
            super(
                    "Clean",
                    "Check total clean length",
                    (Scale.Fraction) constants.getConstant(constants.cleanLow, profile),
                    (Scale.Fraction) constants.getConstant(constants.cleanHigh, profile),
                    true,
                    TOO_HIGH_ADJACENCY);
        }

        @Override
        protected double getValue (StickContext context)
        {
            final NearLine stick = context.stick;
            final int dx = scale.toPixels(constants.beltMarginDx);
            final Point2D start = stick.getStartPoint(VERTICAL);
            final Point2D stop = stick.getStopPoint(VERTICAL);
            final double halfWidth = (scale.getMaxStem() - 1) / 2.0;

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
            int leftCount = 0; //  Count of rows where stem has item on left
            int rightCount = 0; // Count of rows where stem has item on right
            int bothCount = 0; //  Count of rows where stem has item on both
            int cleanCount = 0; // Count of rows where stem is bare (no item stuck)

            final int yMin = stickBox.y;
            final int yMax = (stickBox.y + stickBox.height) - 1;

            if (stick.isVip()) {
                logger.info("VIP CleanCheck for {}", stick);
            }

            for (int y = yMin; y <= yMax; y++) {
                final int leftLimit = (int) Math.ceil(LineUtil.xAtY(leftLine, y));
                final int rightLimit = (int) Math.floor(LineUtil.xAtY(rightLine, y));

                // Make sure the stem row is not empty
                // (top & bottom rows cannot be considered as empty)
                if ((y != yMin) && (y != yMax)) {
                    boolean empty = true;

                    for (int x = leftLimit; x <= rightLimit; x++) {
                        if (pixelFilter.get(x, y) == 0) {
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
                    if (pixelFilter.get(x, y) != 0) {
                        onLeft = false;

                        break;
                    }
                }

                // Item on right?
                boolean onRight = true;

                for (int x = rightLimit; x <= (rightLimit + dx); x++) {
                    if (pixelFilter.get(x, y) != 0) {
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
                logger.info("#{} gap:{} white:{} both:{} left:{} right:{} clean:{}", stick.getId(),
                            largestGap, whiteCount, bothCount, leftCount, rightCount, cleanCount);
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
            extends Check<StickContext>
    {

        protected GapCheck (int profile)
        {
            super("Gap",
                  "Check size of largest hole in stick",
                  Scale.Fraction.ZERO,
                  getMaxYGap(profile),
                  false,
                  TOO_LARGE_GAP);
        }

        // Retrieve the length data
        @Override
        protected double getValue (StickContext context)
        {
            return scale.pixelsToFrac(context.gap);
        }
    }

    //-------------//
    // LengthCheck //
    //-------------//
    /**
     * Check stick total length.
     */
    private class LengthCheck
            extends Check<StickContext>
    {

        protected LengthCheck (int profile)
        {
            super("Length",
                  "Check that total length of stick is long enough",
                  (Scale.Fraction) constants.getConstant(constants.lengthLow, profile),
                  (Scale.Fraction) constants.getConstant(constants.lengthHigh, profile),
                  true,
                  TOO_SHORT);
        }

        // Retrieve the length data
        @Override
        protected double getValue (StickContext context)
        {
            if (context.stick.isVip()) {
                logger.info("VIP LengthCheck for {}", context.stick);
            }

            final Line2D line = context.stick.getLine();

            return scale.pixelsToFrac(line.getY2() - line.getY1());
        }
    }

    //----------------//
    // SeedCheckSuite //
    //----------------//
    /**
     * The whole suite of checks meant for vertical seed candidates.
     */
    private class SeedCheckSuite
            extends CheckSuite<StickContext>
    {

        /**
         * Create a new suite using specified profile level.
         *
         * @param profile specified profile level
         */
        SeedCheckSuite (int profile)
        {
            super("Seed",
                  constants.minCheckResult.getValue(),
                  constants.goodCheckResult.getValue());

            add(1, new SlopeCheck(profile));
            add(1, new StraightCheck(profile));
            add(2, new LengthCheck(profile));

            // Specific case for BEAM_SIDE profile:
            // Check is run because it is needed by the following checks
            // But its specific result value is not used
            add((profile >= Profiles.BEAM_SIDE) ? (-1) : 2, new CleanCheck(profile));

            add(1, new BlackCheck(profile)); // Needs CleanCheck side output
            add(1, new BlackRatioCheck(profile)); // Needs CleanCheck side output
            add(5, new GapCheck(profile)); // Needs CleanCheck side output

            if (logger.isDebugEnabled()) {
                dump();
            }
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
            extends Check<StickContext>
    {

        protected SlopeCheck (int profile)
        {
            super("Slope",
                  "Check that stick is vertical, according to global slope",
                  Constant.Double.ZERO,
                  (Constant.Double) constants.getConstant(constants.slopeHigh, profile),
                  false,
                  NON_VERTICAL);
        }

        // Retrieve the difference between stick slope and global slope
        @Override
        protected double getValue (StickContext context)
        {
            final NearLine stick = context.stick;
            final Point2D start = stick.getStartPoint(VERTICAL);
            final Point2D stop = stick.getStopPoint(VERTICAL);

            // Beware of sign of stickSlope (it is the opposite of globalSlope)
            final double stickSlope = -(stop.getX() - start.getX()) / (stop.getY() - start.getY());

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
            extends Check<StickContext>
    {

        protected StraightCheck (int profile)
        {
            super("Straight",
                  "Check that stick is straight",
                  Scale.Fraction.ZERO,
                  (Scale.Fraction) constants.getConstant(constants.straightHigh, profile),
                  false,
                  NON_STRAIGHT);
        }

        @Override
        protected double getValue (StickContext context)
        {
            return scale.pixelsToFrac(context.stick.getMeanDistance());
        }
    }
}
