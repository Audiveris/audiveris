//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                V e r t i c a l s B u i l d e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.NearLine;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.glyph.dynamic.StickFactory;
import org.audiveris.omr.glyph.dynamic.StraightFilament;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.run.Orientation;

import static org.audiveris.omr.run.Orientation.*;

import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.SystemManager;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.selection.UserEvent;

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
 * Class {@code VerticalsBuilder} is in charge of retrieving major vertical seeds of a
 * dedicated system.
 * The purpose is to use these major vertical sticks as seeds for stems or vertical edges of
 * endings.
 *
 * @author Hervé Bitteur
 */
public class VerticalsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(VerticalsBuilder.class);

    /** Events this entity is interested in */
    private static final Class<?>[] eventClasses = new Class<?>[]{EntityListEvent.class};

    /** Global seed grade is too low. */
    private static final Failure TOO_LIMITED = new Failure("Stem-TooLimited");

    /** Length of seed is too short. */
    private static final Failure TOO_SHORT = new Failure("Stem-TooShort");

    /** Clean portion of seed is too short. */
    private static final Failure TOO_HIGH_ADJACENCY = new Failure("Stem-TooHighAdjacency");

    /** Seed is located in header at beginning of staff. */
    private static final Failure IN_HEADER = new Failure("Stem-InHeader");

    /** Core of seed contains too many white pixels. */
    private static final Failure TOO_HOLLOW = new Failure("Stem-TooHollow");

    /** Seed is not vertical enough. */
    private static final Failure NON_VERTICAL = new Failure("Stem-NonVertical");

    /** Seed is not straight enough. */
    private static final Failure NON_STRAIGHT = new Failure("Stem-NonStraight");

    //~ Instance fields ----------------------------------------------------------------------------
    /** The system to process. */
    private final SystemInfo system;

    /** Related sheet. */
    private final Sheet sheet;

    /** Global sheet scale. */
    private final Scale scale;

    /** Input image. (with staves removed) */
    private final ByteProcessor pixelFilter;

    /** Suite of checks for a vertical seed. */
    private final SeedCheckSuite suite = new SeedCheckSuite();

    //~ Constructors -------------------------------------------------------------------------------
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

    //----------------//
    // buildVerticals //
    //----------------//
    /**
     * Build the verticals seeds out of the dedicated system.
     *
     * @throws StepException if processing failed at this step
     */
    public void buildVerticals ()
            throws StepException
    {
        // Retrieve candidates
        List<StraightFilament> candidates = retrieveCandidates();

        // Apply seed checks
        checkVerticals(candidates);
    }

    //-----------//
    // checkStem //
    //-----------//
    /**
     * Apply the check suite on provided stem candidate and report the resulting impacts
     *
     * @param stick the stem candidate
     * @return the resulting impacts
     */
    public GradeImpacts checkStem (NearLine stick)
    {
        return suite.getImpacts(new StickContext(stick));
    }

    //------------//
    // getMaxYGap //
    //------------//
    public static Scale.Fraction getMaxYGap ()
    {
        return constants.gapHigh;
    }

    //----------//
    // getSuite //
    //----------//
    public CheckSuite getSuite ()
    {
        return suite;
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
     * This method checks for stem seed glyphs within a collection of vertical filaments
     * in a system.
     *
     * @param sticks the provided collection of vertical filaments
     */
    private void checkVerticals (Collection<StraightFilament> sticks)
    {
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();
        int seedNb = 0;
        logger.debug("S#{} searching verticals on {} sticks", system.getId(), sticks.size());

        for (StraightFilament stick : sticks) {
            if (stick.isVip()) {
                logger.info("VIP checkVerticals for {} in system#{}", stick, system.getId());
            }

            // Check seed is not in header
            Point center = stick.getCenter();
            Staff staff = system.getClosestStaff(center);

            if ((staff == null) || (center.x < staff.getHeaderStop())) {
                continue;
            }

            // Run the suite of checks
            double res = suite.pass(new StickContext(stick), null);

            ///logger.debug("suite=> {} for {}", res, stick);
            if (res >= suite.getMinThreshold()) {
                final Glyph glyph = glyphIndex.registerOriginal(stick.toGlyph(null));
                glyph.addGroup(GlyphGroup.VERTICAL_SEED); // Needed
                system.addFreeGlyph(glyph);
                seedNb++;
            }
        }

        logger.debug("{}verticals: {}", system.getLogPrefix(), seedNb);
    }

    //--------------------//
    // retrieveCandidates //
    //--------------------//
    /**
     * Retrieve all system sticks that could be seed candidates.
     *
     * @return the collection of suitable sticks found in the system
     */
    private List<StraightFilament> retrieveCandidates ()
    {
        // Select suitable (vertical) sections
        // Since we are looking for major seeds, we'll use only vertical sections
        List<Section> vSections = new ArrayList<Section>();

        for (Section section : system.getVerticalSections()) {
            // Check section is within system left and right boundaries
            Point center = section.getAreaCenter();

            if ((center.x > system.getLeft()) && (center.x < system.getRight())) {
                vSections.add(section);
            }
        }

        // Horizontal sections (to contribute to stickers)
        List<Section> hSections = new ArrayList<Section>();

        for (Section section : system.getHorizontalSections()) {
            // Limit width to 1 pixel
            if (section.getLength(HORIZONTAL) == 1) {
                // Check section is within system left and right boundaries
                Point center = section.getAreaCenter();

                if ((center.x > system.getLeft()) && (center.x < system.getRight())) {
                    hSections.add(section);
                }
            }
        }

        final StickFactory factory = new StickFactory(
                Orientation.VERTICAL,
                system,
                sheet.getFilamentIndex(),
                null,
                scale.getMaxStem(),
                scale.toPixels(constants.minCoreSectionLength),
                constants.minSideRatio.getValue());

        return factory.retrieveSticks(vSections, hSections);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // BlackCheck //
    //------------//
    /**
     * Check stick length of black parts.
     */
    private class BlackCheck
            extends Check<StickContext>
    {
        //~ Constructors ---------------------------------------------------------------------------

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

        //~ Methods --------------------------------------------------------------------------------
        // Retrieve the length data
        @Override
        protected double getValue (StickContext context)
        {
            return scale.pixelsToFrac(context.black);
        }
    }

    //------------//
    // CleanCheck //
    //------------//
    /**
     * Check the length of stem portions with no item stuck either on left, right or
     * both sides.
     * As a side-effect, additional data is stored in context: white, black and gap fields.
     */
    private class CleanCheck
            extends Check<StickContext>
    {
        //~ Constructors ---------------------------------------------------------------------------

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

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected double getValue (StickContext context)
        {
            final NearLine stick = context.stick;
            final int dx = scale.toPixels(constants.beltMarginDx);
            final Point2D start = stick.getStartPoint(VERTICAL);
            final Point2D stop = stick.getStopPoint(VERTICAL);
            final double halfWidth = (sheet.getScale().getMaxStem() - 1) / 2.0;

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

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio minSideRatio = new Constant.Ratio(
                0.4,
                "Minimum ratio of filament length to be actually enlarged");

        private final Scale.Fraction minCoreSectionLength = new Scale.Fraction(
                1.5,
                "Minimum length for core sections");

        private final Scale.Fraction beltMarginDx = new Scale.Fraction(
                0.15,
                "Horizontal belt margin checked around stem");

        private final Check.Grade minCheckResult = new Check.Grade(
                0.2,
                "Minimum result for suite of check");

        private final Check.Grade goodCheckResult = new Check.Grade(
                0.5,
                "Good result for suite of check");

        private final Scale.Fraction blackHigh = new Scale.Fraction(
                2.5,
                "High minimum length for a stem");

        private final Scale.Fraction blackLow = new Scale.Fraction(
                1.25,
                "Low minimum length for a stem");

        private final Scale.Fraction cleanHigh = new Scale.Fraction(
                2.0,
                "High minimum clean length for a stem");

        private final Scale.Fraction cleanLow = new Scale.Fraction(
                0.5,
                "Low minimum clean length for a stem");

        private final Scale.Fraction gapHigh = new Scale.Fraction(
                0.3,
                "Maximum vertical gap between stem segments");

        private final Constant.Double slopeHigh = new Constant.Double(
                "tangent",
                0.06,
                "Maximum difference with global slope");

        private final Scale.Fraction straightHigh = new Scale.Fraction(
                0.2,
                "High maximum distance to average stem line");

        private final Constant.Double maxCoTangentForCheck = new Constant.Double(
                "cotangent",
                0.1,
                "Maximum cotangent for visual check");
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
        //~ Constructors ---------------------------------------------------------------------------

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

        //~ Methods --------------------------------------------------------------------------------
        // Retrieve the length data
        @Override
        protected double getValue (StickContext context)
        {
            return scale.pixelsToFrac(context.gap);
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
        //~ Constructors ---------------------------------------------------------------------------

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
            add(1, new BlackCheck()); // Needs CleanCheck side output
            add(5, new GapCheck()); // Needs CleanCheck side output

            if (logger.isDebugEnabled() && (system.getId() == 1)) {
                dump();
            }
        }

        //~ Methods --------------------------------------------------------------------------------
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
            extends Check<StickContext>
    {
        //~ Constructors ---------------------------------------------------------------------------

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

        //~ Methods --------------------------------------------------------------------------------
        // Retrieve the difference between stick slope and global slope
        @Override
        protected double getValue (StickContext context)
        {
            final NearLine stick = context.stick;
            Point2D start = stick.getStartPoint(VERTICAL);
            Point2D stop = stick.getStopPoint(VERTICAL);

            // Beware of sign of stickSlope (it is the opposite of globalSlope)
            double stickSlope = -(stop.getX() - start.getX()) / (stop.getY() - start.getY());

            return Math.abs(stickSlope - sheet.getSkew().getSlope());
        }
    }

    //--------------//
    // StickContext //
    //--------------//
    private static class StickContext
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The stick being checked. */
        NearLine stick;

        /** Length of largest gap found. */
        int gap;

        /** Total length of black portions of stem. */
        int black;

        /** Total length of white portions of stem. */
        int white;

        //~ Constructors ---------------------------------------------------------------------------
        public StickContext (NearLine stick)
        {
            this.stick = stick;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "stick#" + stick.getId();
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
        //~ Constructors ---------------------------------------------------------------------------

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

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected double getValue (StickContext context)
        {
            final NearLine stick = context.stick;

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
    private static class VertCheckBoard
            extends CheckBoard<StickContext>
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Sheet sheet;

        //~ Constructors ---------------------------------------------------------------------------
        public VertCheckBoard (Sheet sheet,
                               SelectionService eventService,
                               Class[] eventList)
        {
            super("SeedCheck", null, eventService, eventList);
            this.sheet = sheet;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void onEvent (UserEvent event)
        {
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                if (event instanceof EntityListEvent) {
                    EntityListEvent<Glyph> listEvent = (EntityListEvent<Glyph>) event;
                    final Glyph glyph = listEvent.getEntity();

                    if (glyph != null) {
                        // Make sure this is a rather vertical stick
                        if (Math.abs(glyph.getInvertedSlope()) <= constants.maxCoTangentForCheck.getValue()) {
                            // Apply the check suite
                            SystemManager systemManager = sheet.getSystemManager();

                            for (SystemInfo system : systemManager.getSystemsOf(glyph)) {
                                applySuite(
                                        new VerticalsBuilder(system).getSuite(),
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
}
