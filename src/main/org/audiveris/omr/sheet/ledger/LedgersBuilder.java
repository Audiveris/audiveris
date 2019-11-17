//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   L e d g e r s B u i l d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sheet.ledger;

import ij.process.ByteProcessor;

import org.audiveris.omr.check.Check;
import org.audiveris.omr.check.CheckBoard;
import org.audiveris.omr.check.CheckSuite;
import org.audiveris.omr.check.Failure;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.dynamic.Filament;
import org.audiveris.omr.glyph.dynamic.StickFactory;
import org.audiveris.omr.glyph.dynamic.StraightFilament;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.run.Orientation;
import static org.audiveris.omr.run.Orientation.*;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.InterlineScale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.ui.SheetAssembly;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.relation.Exclusion;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.NamedDouble;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Predicate;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class {@code LedgersBuilder} retrieves ledgers for a system.
 * <p>
 * Each virtual line of ledgers is processed, one after the other, going away from the reference
 * staff, above and below:
 * <ol>
 * <li>All acceptable candidate glyph instances for the current virtual line are translated into
 * LedgerInter instances with proper intrinsic grade.</li>
 * <li>Exclusions can be inserted because of abscissa overlap.</li>
 * <li>Finally, using grades, the collection of ledgers interpretations is reduced and only the
 * remaining ones are recorded as such in staff map.
 * They will be used as ordinate references when processing the next virtual line.</li>
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class LedgersBuilder
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LedgersBuilder.class);

    /** Events this entity is interested in. */
    private static final Class<?>[] eventClasses = new Class<?>[]{EntityListEvent.class};

    /** Failure codes. */
    private static final Failure TOO_SHORT = new Failure("Hori-TooShort");

    private static final Failure TOO_THIN = new Failure("Hori-TooThin");

    private static final Failure TOO_THICK = new Failure("Hori-TooThick");

    private static final Failure TOO_CONCAVE = new Failure("Hori-TooConcave");

    private static final Failure TOO_BENDED = new Failure("Hori-TooBended");

    private static final Failure TOO_SHIFTED = new Failure("Hori-TooShifted");

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Dedicated system. */
    private final SystemInfo system;

    /** Related sig. */
    private final SIGraph sig;

    /** Global sheet scale. */
    private final Scale scale;

    /** Large sheet scale. */
    private final InterlineScale largeScale;

    /** Check suites. */
    private final Suites suites;

    /** The system-wide collection of ledger candidates. */
    private List<StraightFilament> ledgerCandidates;

    /** The (good) system-wide beams and hooks, sorted by left abscissa. */
    private List<Inter> sortedSystemBeams;

    /** Minimum x overlap between successive ledgers. */
    private final int minAbscissaOverlap;

    // Intermediate constants for width check within ledger suite
    private final NamedDouble minLengthLow;

    private final NamedDouble minLengthHigh;

    /**
     * @param system the related system to process
     */
    public LedgersBuilder (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();
        sheet = system.getSheet();
        scale = sheet.getScale();
        largeScale = scale.getInterlineScale();

        minLengthLow = new NamedDouble("minLengthLow", "fraction", 0, "low length");
        minLengthHigh = new NamedDouble("minLengthHigh", "fraction", 0, "high length");

        suites = new Suites(scale);

        minAbscissaOverlap = largeScale.toPixels(constants.minAbscissaOverlap);
    }

    //---------------//
    // addCheckBoard //
    //---------------//
    /**
     * Add a user board dedicated to ledger check.
     */
    public void addCheckBoard ()
    {
        SheetAssembly assembly = sheet.getStub().getAssembly();
        assembly.addBoard(SheetTab.DATA_TAB, new LedgerCheckBoard(sheet));
        assembly.addBoard(SheetTab.LEDGER_TAB, new LedgerCheckBoard(sheet));
    }

    //--------------//
    // buildLedgers //
    //--------------//
    /**
     * Search horizontal sticks for ledgers and build ledgers incrementally.
     *
     * @param sections candidate sections for this system
     */
    public void buildLedgers (List<Section> sections)
    {
        try {
            // Put apart the (good) system beams, they can't intersect ledgers.
            StopWatch watch = new StopWatch("buildLedgers S#" + system.getId());
            watch.start("getGoodBeams");
            sortedSystemBeams = getGoodBeams();

            // Retrieve system candidate glyphs out of candidate sections
            watch.start("getCandidateFilaments among " + sections.size());
            ledgerCandidates = getCandidateFilaments(sections);

            // Filter candidates accurately, line by line
            watch.start("filterLedgers");
            filterLedgers();

            if (constants.printWatch.isSet()) {
                watch.print();
            }
        } catch (Throwable ex) {
            logger.warn("Error retrieving ledgers. " + ex, ex);
        }
    }

    //-------------//
    // beamOverlap //
    //-------------//
    /**
     * Check whether stick middle point is contained by a good beam.
     *
     * @param stick the candidate to check
     * @return true if a beam overlap was detected
     */
    private boolean beamOverlap (Filament stick)
    {
        Point2D middle = getMiddle(stick);

        for (Inter inter : sortedSystemBeams) {
            AbstractBeamInter beam = (AbstractBeamInter) inter;

            if (beam.getArea().contains(middle)) {
                if (stick.isVip() || logger.isDebugEnabled()) {
                    logger.info("ledger stick#{} overlaps beam#{}", stick.getId(), beam.getId());
                }

                return true;
            } else if (beam.getBounds().getLocation().x > middle.getX()) {
                return false;
            }
        }

        return false;
    }

    //---------------//
    // filterLedgers //
    //---------------//
    /**
     * Use smart tests on ledger candidates.
     * Starting from each staff, check one interline higher (and lower) for candidates, etc.
     * <p>
     * For merged grand staff, there is only one middle ledger (C4), related to the upper staff.
     * Therefore, upper staff is limited downward to +1, and lower staff limited upward to 0.
     */
    private void filterLedgers ()
    {
        for (Staff staff : system.getStaves()) {
            final Part part = staff.getPart();
            logger.debug("Staff#{}", staff.getId());

            // Above staff (-1,-2,-3, ...)
            final int minIndex = (part.isMerged() && (staff == part.getLastStaff())) ? 0
                    : Integer.MIN_VALUE;

            for (int index = -1; index >= minIndex; index--) {
                if (0 == lookupLine(staff, index)) {
                    break;
                }
            }

            // Below staff (+1,+2,+3, ...)
            final int maxIndex = (part.isMerged() && (staff == part.getFirstStaff())) ? 1
                    : Integer.MAX_VALUE;

            for (int index = 1; index <= maxIndex; index++) {
                if (0 == lookupLine(staff, index)) {
                    break;
                }
            }
        }
    }

    //-----------------------//
    // getCandidateFilaments //
    //-----------------------//
    /**
     * Retrieve possible candidate filaments built from provided sections.
     *
     * @param sections the section population to build filaments from
     * @return a collection of candidate filaments
     */
    private List<StraightFilament> getCandidateFilaments (List<Section> sections)
    {
        final int maxThickness = Math.min(
                scale.toPixels(constants.maxThicknessHigh),
                scale.toPixels(constants.maxThicknessHigh2));

        // Use stick factory
        final StickFactory factory = new StickFactory(
                Orientation.HORIZONTAL,
                system,
                sheet.getFilamentIndex(),
                null, // predicate, // Miss many ledgers when significantly thicker than staff line!
                maxThickness,
                largeScale.toPixels(constants.minCoreSectionLength),
                constants.minSideRatio.getValue());
        final List<StraightFilament> filaments = factory.retrieveSticks(sections, null);

        // Purge candidates that overlap good beams
        purgeBeamOverlaps(filaments);

        return filaments;
    }

    //--------------//
    // getGoodBeams //
    //--------------//
    /**
     * Retrieve the list of beam / hook interpretations in the system, ordered by
     * abscissa.
     *
     * @return the sequence of system beams (full beams and hooks)
     */
    private List<Inter> getGoodBeams ()
    {
        List<Inter> beams = sig.inters(new Predicate<Inter>()
        {
            @Override
            public boolean check (Inter inter)
            {
                return (inter instanceof AbstractBeamInter) && inter.isGood();
            }
        });

        Collections.sort(beams, Inters.byAbscissa);

        return beams;
    }

    //---------------//
    // getYReference //
    //---------------//
    /**
     * Look for an ordinate reference suitable with provided stick.
     * This may be a ledger on the previous line or the staff line itself
     *
     * @param staff           the staff being processed
     * @param index           the position WRT to staff
     * @param stick           the candidate stick to check
     * @param previousWrapper (output) wrapper around a previous ledger
     * @return the ordinate reference found, or null if not found
     */
    private Double getYReference (Staff staff,
                                  int index,
                                  Filament stick,
                                  Wrapper<LedgerInter> previousWrapper)
    {
        final int prevIndex = (index < 0) ? (index + 1) : (index - 1);

        if (prevIndex != 0) {
            final List<LedgerInter> prevLedgers = staff.getLedgers(prevIndex);

            // If no previous ledger for reference, give up
            if ((prevLedgers == null) || prevLedgers.isEmpty()) {
                if (stick.isVip()) {
                    logger.info("Ledger candidate {} orphan", stick);
                }

                return null;
            }

            // Check abscissa compatibility
            Rectangle stickBox = stick.getBounds();

            for (LedgerInter ledger : prevLedgers) {
                Rectangle ledgerBox = ledger.getBounds();

                if (GeoUtil.xOverlap(stickBox, ledgerBox) > minAbscissaOverlap) {
                    // Use this previous ledger as ordinate reference
                    double xMid = stick.getCenter().x;
                    Glyph ledgerGlyph = ledger.getGlyph();
                    previousWrapper.value = ledger;

                    // Middle of stick may fall outside of ledger width
                    if (GeoUtil.xEmbraces(ledgerBox, xMid)) {
                        return LineUtil.intersectionAtX(ledgerGlyph.getCenterLine(), xMid).y;
                    } else {
                        return LineUtil.yAtX(
                                ledgerGlyph.getStartPoint(HORIZONTAL),
                                ledgerGlyph.getStopPoint(HORIZONTAL),
                                xMid);
                    }
                }
            }

            if (stick.isVip()) {
                logger.info("Ledger candidate {} local orphan", stick);
            }

            return null;
        } else {
            // Use staff line as reference
            LineInfo staffLine = (index < 0) ? staff.getFirstLine() : staff.getLastLine();

            return staffLine.yAt(stick.getCenter().getX());
        }
    }

    //---------------------//
    // intersectHorizontal //
    //---------------------//
    /**
     * Check whether the provided section intersects at least one horizontal section of
     * the system.
     *
     * @param section the provided (ledger) section
     * @return true if intersects a horizontal section
     */
    private boolean intersectHorizontal (Section section)
    {
        Rectangle sectionBox = section.getBounds();

        // Check this section intersects a horizontal section
        for (Section hs : system.getHorizontalSections()) {
            if (hs.getBounds().intersects(sectionBox)) {
                if (hs.intersects(section)) {
                    return true;
                }
            }
        }

        return false;
    }

    //------------//
    // lookupLine //
    //------------//
    /**
     * This is the heart of ledger retrieval, which looks for ledgers on a specific
     * "virtual line".
     * <p>
     * We use a very rough height for the region of interest, relying on pitch check WRT yTarget to
     * discard the too distant candidates.
     * However there is a risk that a ledger be found "acceptable" on two line indices.
     * Moreover, a conflict on line #2 could remove the ledger from SIG while it is still accepted
     * on line #1.
     *
     * @param staff the staff being processed
     * @param index index of line relative to staff
     * @return the number of ledgers found on this virtual line
     */
    private int lookupLine (Staff staff,
                            int index)
    {
        logger.debug("Checking staff: {} line: {}", staff.getId(), index);

        // Choose which suite to apply
        final int interline = staff.getSpecificInterline(); // Staff specific
        final CheckSuite<StickContext> suite = suites.getSuite(interline);
        final InterlineScale staffScale = scale.getInterlineScale(interline);
        final int yMargin = staffScale.toPixels(constants.ledgerMarginY);
        final LineInfo staffLine = (index < 0) ? staff.getFirstLine() : staff.getLastLine();
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();
        final int minWide = staffScale.toPixels(constants.minWideLedgerLength);

        // Define bounds for the virtual line, properly shifted and enlarged
        Rectangle virtualLineBox = staffLine.getBounds();
        virtualLineBox.y += (index * interline);
        virtualLineBox.grow(0, 2 * yMargin);

        final List<LedgerInter> ledgers = new ArrayList<>();

        // Filter enclosed candidates and populate acceptable ledgers
        for (StraightFilament stick : ledgerCandidates) {
            // Rough containment
            final Point2D middle = getMiddle(stick);

            if (!virtualLineBox.contains(middle)) {
                continue;
            }

            if (stick.isVip()) {
                logger.info("VIP lookupLine for {}", stick);
            }

            // Check for presence of ledger on previous line
            // and definition of a reference ordinate (staff line or ledger)
            final Wrapper<LedgerInter> prevWrapper = new Wrapper<>(null);
            final Double yRef = getYReference(staff, index, stick, prevWrapper);

            if (yRef == null) {
                if (stick.isVip()) {
                    logger.info("VIP no line ref for {}", stick);
                }

                continue;
            }

            // Check precise vertical distance WRT the target ordinate
            final double yTarget = yRef + (Integer.signum(index) * interline);

            if ((prevWrapper.value != null) && (prevWrapper.value.getBounds().width >= minWide)) {
                // A wide reference ledger raises width check for the current candidate ledger
                minLengthLow.setValue(constants.minLedgerLengthLow2.getValue());
                minLengthHigh.setValue(constants.minLedgerLengthHigh2.getValue());
            } else {
                minLengthLow.setValue(constants.minLedgerLengthLow.getValue());
                minLengthHigh.setValue(constants.minLedgerLengthHigh.getValue());
            }

            GradeImpacts impacts = suite.getImpacts(new StickContext(stick, yTarget));

            if (impacts != null) {
                double grade = impacts.getGrade();

                if (stick.isVip()) {
                    logger.info("VIP staff#{} at {} {}", staff.getId(), index, impacts);
                }

                if (grade >= suite.getMinThreshold()) {
                    Glyph glyph = glyphIndex.registerOriginal(stick.toGlyph(null));
                    LedgerInter ledger = new LedgerInter(glyph, impacts);
                    ledger.setIndex(index);
                    sig.addVertex(ledger);
                    ledgers.add(ledger);
                }
            }
        }

        if (!ledgers.isEmpty()) {
            // Now, check for collision within line population and reduce accordingly.
            reduceLedgers(staff, index, ledgers);

            // Populate staff with ledgers kept
            for (LedgerInter ledger : ledgers) {
                staff.addLedger(ledger, index);
                ledger.setStaff(staff);

                if (ledger.isVip()) {
                    logger.info(
                            "VIP {} in staff#{} at {} for {}",
                            ledger,
                            staff.getId(),
                            index,
                            ledger.getDetails());
                }
            }
        }

        return ledgers.size();
    }

    //-------------------//
    // purgeBeamOverlaps //
    //-------------------//
    /**
     * Purge the filaments that overlap a (good) beam.
     *
     * @param filaments (updated) the collection of filaments to purge
     */
    private void purgeBeamOverlaps (List<StraightFilament> filaments)
    {
        List<Filament> toRemove = new ArrayList<>();

        for (Filament fil : filaments) {
            if (beamOverlap(fil)) {
                toRemove.add(fil);
            }
        }

        if (!toRemove.isEmpty()) {
            filaments.removeAll(toRemove);
        }
    }

    //---------------//
    // reduceLedgers //
    //---------------//
    /**
     * Check for collision within line population of ledgers and reduce the population
     * accordingly.
     *
     * @param staff   staff being processed
     * @param index   index of virtual line around staff
     * @param ledgers population of ledger interpretations for the line
     */
    private void reduceLedgers (Staff staff,
                                int index,
                                List<LedgerInter> ledgers)
    {
        final int interline = staff.getSpecificInterline();
        int maxDx = largeScale.toPixels(constants.maxInterLedgerDx);
        Set<Exclusion> exclusions = new LinkedHashSet<>();
        Collections.sort(ledgers, Inters.byAbscissa);

        for (int i = 0; i < ledgers.size(); i++) {
            final LedgerInter ledger = ledgers.get(i);
            final Rectangle ledgerBox = ledger.getBounds();
            final Rectangle fatBox = ledger.getBounds();
            fatBox.grow(maxDx, interline);

            // Check neighbors on the right only (since we are browsing a sorted list)
            for (LedgerInter other : ledgers.subList(i + 1, ledgers.size())) {
                if (GeoUtil.xOverlap(ledgerBox, other.getBounds()) > 0) {
                    // Abscissa overlap
                    exclusions.add(sig.insertExclusion(ledger, other, Exclusion.Cause.OVERLAP));
                } else {
                    break; // End of reachable neighbors
                }
            }
        }

        if (!exclusions.isEmpty()) {
            Set<Inter> deletions = sig.reduceExclusions(exclusions);
            logger.debug(
                    "Staff: {} index: {} deletions: {} {}",
                    staff.getId(),
                    index,
                    deletions.size(),
                    deletions);
            ledgers.removeAll(deletions);
        }
    }

    //-----------//
    // getMiddle //
    //-----------//
    /**
     * Retrieve the middle point of a stick, assumed rather horizontal.
     *
     * @param stick the stick to process
     * @return the middle point
     */
    private static Point2D getMiddle (Filament stick)
    {
        final Point2D startPoint = stick.getStartPoint();
        final Point2D stopPoint = stick.getStopPoint();

        return new Point2D.Double(
                (startPoint.getX() + stopPoint.getX()) / 2,
                (startPoint.getY() + stopPoint.getY()) / 2);

    }

    //-------------//
    // LedgerSuite //
    //-------------//
    /**
     * A suite of checks, with scaling that may depend on specific staff interline.
     */
    private class LedgerSuite
            extends CheckSuite<StickContext>
    {

        /**
         * Staff 'specific' interline scale.
         * NOTA: we also can use 'largeScale' and 'scale' where applicable.
         */
        private final InterlineScale specific;

        /**
         * Create a check suite.
         */
        LedgerSuite (InterlineScale interlineScale)
        {
            super("Ledger");
            this.specific = interlineScale;

            add(0.5, new MinThicknessCheck());
            add(0, new MaxThicknessCheck());
            add(4, new MinLengthCheck());
            add(2, new ConvexityCheck());
            add(1, new StraightCheck());
            add(0.5, new LeftPitchCheck());
            add(0.5, new RightPitchCheck());
        }

        private class ConvexityCheck
                extends Check<StickContext>
        {

            ConvexityCheck ()
            {
                super(
                        "Convex",
                        "Check number of convex stick ends",
                        constants.convexityLow,
                        Constant.Double.TWO,
                        true,
                        TOO_CONCAVE);
            }

            @Override
            protected double getValue (StickContext context)
            {
                ByteProcessor pixelFilter = sheet.getPicture().getSource(
                        Picture.SourceKey.NO_STAFF);

                Filament stick = context.stick;
                Rectangle box = stick.getBounds();
                int convexities = 0;

                // On each end of stick, we check that pixels just above and just below are white,
                // so that stick slightly stands out.
                // We use the stick bounds, regardless of the precise geometry inside.
                //
                //  X                                                         X
                //  +---------------------------------------------------------+
                //  |                                                         |
                //  |                                                         |
                //  +---------------------------------------------------------+
                //  X                                                         X
                //
                for (HorizontalSide hSide : HorizontalSide.values()) {
                    int x = (hSide == LEFT) ? box.x : ((box.x + box.width) - 1);
                    boolean topFore = pixelFilter.get(x, box.y - 1) == 0;
                    boolean bottomFore = pixelFilter.get(x, box.y + box.height) == 0;
                    boolean isConvex = !(topFore || bottomFore);

                    if (isConvex) {
                        convexities++;
                    }
                }

                return convexities;
            }
        }

        private class LeftPitchCheck
                extends Check<StickContext>
        {

            protected LeftPitchCheck ()
            {
                super(
                        "LPitch",
                        "Check that left ordinate is close to theoretical value",
                        Constant.Double.ZERO,
                        constants.ledgerMarginY,
                        false,
                        TOO_SHIFTED);
            }

            @Override
            protected double getValue (StickContext context)
            {
                Filament stick = context.stick;
                double yTarget = context.yTarget;
                double y = stick.getStartPoint().getY();

                return specific.pixelsToFrac(Math.abs(y - yTarget));
            }
        }

        private class MaxThicknessCheck
                extends Check<StickContext>
        {

            protected MaxThicknessCheck ()
            {
                super(
                        "MaxTh.",
                        "Check that stick is not too thick",
                        constants.maxThicknessLow,
                        constants.maxThicknessHigh,
                        false,
                        TOO_THICK);
            }

            // Retrieve the thickness data
            @Override
            protected double getValue (StickContext context)
            {
                Filament stick = context.stick;

                return scale.pixelsToLineFrac(stick.getMeanThickness(HORIZONTAL));
            }
        }

        private class MinLengthCheck
                extends Check<StickContext>
        {

            protected MinLengthCheck ()
            {
                super(
                        "Length",
                        "Check that stick is long enough",
                        minLengthLow, // Intermediate value
                        minLengthHigh, // Intermediate value
                        true,
                        TOO_SHORT);
            }

            // Retrieve the length data
            @Override
            protected double getValue (StickContext context)
            {
                Filament stick = context.stick;

                return specific.pixelsToFrac(stick.getLength(HORIZONTAL));
            }
        }

        private class MinThicknessCheck
                extends Check<StickContext>
        {

            protected MinThicknessCheck ()
            {
                super(
                        "MinTh.",
                        "Check that stick is thick enough",
                        Constant.Double.ZERO,
                        constants.minThicknessHigh,
                        true,
                        TOO_THIN);
            }

            // Retrieve the thickness data
            @Override
            protected double getValue (StickContext context)
            {
                Filament stick = context.stick;

                return largeScale.pixelsToFrac(stick.getMeanThickness(HORIZONTAL));
            }
        }

        private class RightPitchCheck
                extends Check<StickContext>
        {

            protected RightPitchCheck ()
            {
                super(
                        "RPitch",
                        "Check that right ordinate is close to theoretical value",
                        Constant.Double.ZERO,
                        constants.ledgerMarginY,
                        false,
                        TOO_SHIFTED);
            }

            @Override
            protected double getValue (StickContext context)
            {
                Filament stick = context.stick;
                double yTarget = context.yTarget;
                double y = stick.getStopPoint().getY();

                return specific.pixelsToFrac(Math.abs(y - yTarget));
            }
        }

        private class StraightCheck
                extends Check<StickContext>
        {

            protected StraightCheck ()
            {
                super(
                        "Straight",
                        "Check that stick is rather straight",
                        Constant.Double.ZERO,
                        constants.maxDistanceHigh,
                        false,
                        TOO_BENDED);
            }

            @Override
            protected double getValue (StickContext context)
            {
                StraightFilament stick = context.stick;

                return largeScale.pixelsToFrac(stick.getMeanDistance());
            }
        }
    }

//--------//
// Suites //
//--------//
    /**
     * Management of check suites, based on staff interline.
     */
    private class Suites
    {

        final Map<Integer, LedgerSuite> map = new HashMap<>();

        Suites (Scale sheetScale)
        {
            final Integer large = sheetScale.getInterline();
            map.put(large, new LedgerSuite(sheetScale.getInterlineScale()));

            final Integer small = sheetScale.getSmallInterline();

            if (small != null) {
                map.put(small, new LedgerSuite(sheetScale.getSmallInterlineScale()));
            }
        }

        LedgerSuite getSuite (int interline)
        {
            return map.get(interline);
        }
    }

//-----------//
// Constants //
//-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Ratio minSideRatio = new Constant.Ratio(
                0.8,
                "Minimum ratio of filament length to be actually enlarged");

        private final Constant.Double convexityLow = new Constant.Double(
                "end number",
                -0.5,
                "Minimum convexity ends");

        private final Constant.Double maxSlopeForCheck = new Constant.Double(
                "tangent",
                0.1,
                "Maximum slope for visual check");

        // Constants specified WRT mean line thickness
        // -------------------------------------------
        private final Scale.LineFraction maxThicknessHigh = new Scale.LineFraction(
                3.25, // 3.0 is just too low for a synthetic score with 1-pixel staff lines
                "High Maximum thickness of an interesting stick (WRT staff line)");

        private final Scale.LineFraction maxThicknessLow = new Scale.LineFraction(
                1.0,
                "Low Maximum thickness of an interesting stick (WRT staff line)");

        // Constants specified WRT mean interline
        // --------------------------------------
        private final Scale.Fraction minCoreSectionLength = new Scale.Fraction(
                1.0,
                "Minimum length for a section to be considered as core");

        private final Scale.Fraction maxThicknessHigh2 = new Scale.Fraction(
                0.4,
                "High Maximum thickness of an interesting stick (WRT interline)");

        private final Scale.Fraction ledgerMarginY = new Scale.Fraction(
                0.35,
                "Margin on ledger ordinate WRT theoretical ordinate");

        private final Scale.Fraction minAbscissaOverlap = new Scale.Fraction(
                0.75,
                "Minimum abscissa overlap of a ledger with the previous one");

        private final Scale.Fraction minLedgerLengthHigh = new Scale.Fraction(
                1.5,
                "High Minimum length for a ledger");

        private final Scale.Fraction minLedgerLengthLow = new Scale.Fraction(
                1.0,
                "Low Minimum length for a ledger");

        private final Scale.Fraction minWideLedgerLength = new Scale.Fraction(
                1.5,
                "Minimum length for a wide ledger");

        private final Scale.Fraction minLedgerLengthHigh2 = new Scale.Fraction(
                2.0,
                "High Minimum long length for a ledger");

        private final Scale.Fraction minLedgerLengthLow2 = new Scale.Fraction(
                1.4,
                "Low Minimum long length for a ledger");

        private final Scale.Fraction minThicknessHigh = new Scale.Fraction(
                0.25,
                "High Minimum thickness of an interesting stick");

        private final Scale.Fraction maxInterLedgerDx = new Scale.Fraction(
                2.5,
                "Maximum inter-ledger abscissa gap for ordinate compatibility test");

        private final Scale.Fraction maxDistanceHigh = new Scale.Fraction(
                0.3,
                "Low Minimum radius for ledger");
    }

//------------------//
// LedgerCheckBoard //
//------------------//
    /**
     * A specific board to display intrinsic checks of ledger sticks.
     */
    private static class LedgerCheckBoard
            extends CheckBoard<StickContext>
    {

        private final Sheet sheet;

        LedgerCheckBoard (Sheet sheet)
        {
            super("LedgerCheck", null, sheet.getFilamentIndex().getEntityService(), eventClasses);
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
                    EntityListEvent<Filament> listEvent = (EntityListEvent<Filament>) event;
                    final Filament fil = listEvent.getEntity();

                    // Make sure we have a rather horizontal stick
                    if ((fil != null) && (fil instanceof StraightFilament) && (Math.abs(
                            fil.getSlope()) <= constants.maxSlopeForCheck.getValue())) {
                        // Use the closest staff
                        Point center = fil.getCenter();
                        StaffManager mgr = sheet.getStaffManager();
                        Staff staff = mgr.getClosestStaff(center);
                        LedgersBuilder builder = new LedgersBuilder(staff.getSystem());
                        int interline = staff.getSpecificInterline();
                        CheckSuite<StickContext> suite = builder.suites.getSuite(interline);

                        // We need a yTarget (which should take previous ledger into account!)
                        // Instead, we use an index value based on distance to staff
                        int pitch = (int) Math.rint(staff.pitchPositionOf(center));
                        int index = (pitch / 2) - (2 * Integer.signum(pitch));
                        Wrapper<LedgerInter> p = new Wrapper<>(null);
                        Double yRef = builder.getYReference(staff, index, fil, p);

                        if (yRef != null) {
                            double yTarget = yRef + (Integer.signum(index) * interline);
                            applySuite(suite, new StickContext((StraightFilament) fil, yTarget));

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

//--------------//
// StickContext //
//--------------//
    private static class StickContext
    {

        /** The stick being checked. */
        final StraightFilament stick;

        /** Target ordinate. */
        final double yTarget;

        StickContext (StraightFilament stick,
                      double yTarget)
        {
            this.stick = stick;
            this.yTarget = yTarget;
        }

        @Override
        public String toString ()
        {
            return "stick#" + stick.getId();
        }
    }

}
