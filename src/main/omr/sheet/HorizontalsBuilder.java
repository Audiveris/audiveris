//----------------------------------------------------------------------------//
//                                                                            //
//                    H o r i z o n t a l s B u i l d e r                     //
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
import omr.check.Failure;
import omr.check.SuiteImpacts;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.Filament;
import omr.grid.FilamentsFactory;
import omr.grid.LineInfo;
import omr.grid.StaffInfo;

import omr.lag.Section;

import omr.math.GeoUtil;
import omr.math.LineUtil;
import static omr.run.Orientation.*;

import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.UserEvent;

import omr.sig.AbstractBeamInter;
import omr.sig.Exclusion;
import omr.sig.Inter;
import omr.sig.LedgerInter;
import omr.sig.LedgerRelation;
import omr.sig.SIGraph;

import omr.step.Step;

import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code HorizontalsBuilder} is in charge of retrieving
 * ledgers and endings for a system.
 * <p>
 * Ledgers are built incrementally.
 * First, all suitable ledger glyph instances in the system are identified and
 * collected in a system-wide 'ledgerCandidates' collection.
 * <p>
 * Then, each virtual line of ledgers is processed, one after the other, going
 * away from the reference staff, above and below:
 * <ol>
 * <li>All relevant candidate glyph instances for the current virtual line are
 * translated into LedgerInter instances with proper intrinsic grade.</li>
 * <li>Ledger relations can be inserted to represent either exclusion or
 * support:<ul>
 * <li>Exclusions occur because of abscissa overlap or because of too different
 * ordinate values between abscissa-close ledgers.</li>
 * <li>Supports occur when close ledgers (abscissa-wise) have similar
 * ordinates.</li>
 * </ul>
 * <li>Finally, using contextual grades, the collection of ledgers
 * interpretations is reduced and only the remaining ones are recorded as such
 * in staff map.
 * They will be used as ordinate references when processing the next virtual
 * line.</li>
 * </ol>
 * <p>
 * Nota: Endings are currently disabled.
 *
 * @author Hervé Bitteur
 */
public class HorizontalsBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            HorizontalsBuilder.class);

    /** Events this entity is interested in. */
    private static final Class<?>[] eventClasses = new Class<?>[]{
        GlyphEvent.class
    };

    /** Failure codes */
    private static final Failure TOO_SHORT = new Failure("Hori-TooShort");

    private static final Failure TOO_THIN = new Failure("Hori-TooThin");

    private static final Failure TOO_THICK = new Failure("Hori-TooThick");

    private static final Failure TOO_SLOPED = new Failure("Hori-TooSloped");

    private static final Failure TOO_SHIFTED = new Failure(
            "Hori-TooShifted");

    //~ Instance fields --------------------------------------------------------
    //
    /** Related sheet. */
    private final Sheet sheet;

    /** Dedicated system. */
    private final SystemInfo system;

    /** Related sig. */
    private final SIGraph sig;

    /** Global sheet scale. */
    private final Scale scale;

    /** Rough check suite for ledgers. */
    private final LedgerRoughSuite ledgerRoughSuite = new LedgerRoughSuite();

    /** The system-wide collection of ledger candidates. */
    private List<Glyph> ledgerCandidates;

    /** The (good) system-wide beams and hooks. */
    private List<Inter> systemBeams;

    //~ Constructors -----------------------------------------------------------
    //--------------------//
    // HorizontalsBuilder //
    //--------------------//
    /**
     * @param system the related system to process
     */
    public HorizontalsBuilder (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();
        sheet = system.getSheet();
        scale = sheet.getScale();
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // addCheckBoard //
    //---------------//
    /**
     * Add a user board dedicated to ledger check.
     */
    public void addCheckBoard ()
    {
        sheet.getAssembly()
                .addBoard(Step.DATA_TAB, new LedgerCheckBoard());
    }

    //--------------//
    // buildLedgers //
    //--------------//
    /**
     * Search horizontal sticks for ledgers (and endings).
     */
    public void buildLedgers ()
    {
        try {
            // Filter which sections to provide to factory
            List<Section> sections = getCandidateSections();

            // Retrieve candidate glyphs out of candidate sections
            List<Glyph> sticks = getCandidateGlyphs(sections);

            // Apply basic checks for ledgers candidates, endings
            ledgerCandidates = checkHorizontals(sticks);

            // Filter candidates accurately, line by line
            filterLedgers();
        } catch (Throwable ex) {
            logger.warn("Error retrieving horizontals. ", ex);
        } finally {
            // User feedback
            feedback();
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
    private boolean beamOverlap (Glyph stick)
    {
        Point2D middle = getMiddle(stick);

        for (Inter inter : systemBeams) {
            AbstractBeamInter beam = (AbstractBeamInter) inter;

            if (beam.getArea()
                    .contains(middle)) {
                if (stick.isVip() || logger.isDebugEnabled()) {
                    logger.info(
                            "ledger stick#{} overlaps beam#{}",
                            stick.getId(),
                            beam.getId());
                }

                return true;
            } else {
                // Speedup, since beams are sorted by abscissa
                if (beam.getBounds()
                        .getLocation().x > middle.getX()) {
                    return false;
                }
            }
        }

        return false;
    }

    //------------------//
    // checkHorizontals //
    //------------------//
    /**
     * Run checks on possible sticks and return good candidates
     *
     * @param sticks the raw horizontal glyph instances
     * @return the list of ledger candidates
     */
    private List<Glyph> checkHorizontals (List<Glyph> sticks)
    {
        List<Glyph> candidates = new ArrayList<Glyph>();
        systemBeams = getBeams();

        for (Glyph stick : sticks) {
            if (stick.isVip()) {
                logger.info("VIP checkHorizontals for {}", stick);
            }

            // Run the ledger rough checks
            final double grade;

            if (stick.isVip()) {
                SuiteImpacts impacts = ledgerRoughSuite.getImpacts(stick);
                logger.info("VIP {}", impacts.getDump());
                grade = impacts.getGrade();
            } else {
                grade = ledgerRoughSuite.pass(stick, null);
            }

            if (grade >= ledgerRoughSuite.getThreshold()) {
                // Check potential overlap with a good beam/hook
                //TODO: could this test be part of rough suite?
                if (!beamOverlap(stick)) {
                    candidates.add(stick);
                }

                //TODO Ending are disabled for the time being
                //            } else {
                //                // Then, if failed, the Ending Checks
                //                if (CheckSuite.passCollection(stick, endingList) >= minResult) {
                //                    stick.setResult(ENDING);
                //                    stick.setShape(Shape.ENDING_HORIZONTAL);
                //                    endings.add(stick);
                //
                //                    if (logger.isDebugEnabled()) {
                //                        logger.info("Ending " + stick);
                //                    }
                //                }
            }
        }

        return candidates;
    }

    //    //--------------//
    //    // createSuites //
    //    //--------------//
    //    private void createSuites ()
    //    {
    //        // endingSuite
    //        endingSuite = new CheckSuite<Glyph>(
    //                "Ending",
    //                constants.minCheckResult.getValue());
    //        endingSuite.add(1, new MinThicknessCheck());
    //        endingSuite.add(1, new MaxThicknessCheck());
    //        endingSuite.add(
    //                1,
    //                new MinLengthCheck(
    //                constants.minEndingLengthLow,
    //                constants.minEndingLengthHigh));
    //        endingSuite.add(1, new SlopeCheck());
    //    }
    //
    //----------//
    // feedback //
    //----------//
    private void feedback ()
    {
        int nl = 0;
        int nt = 0;
        int ne = 0;

        for (Glyph glyph : system.getGlyphs()) {
            Shape shape = glyph.getShape();

            if (shape == Shape.LEDGER) {
                nl++;
            } else if (shape == Shape.ENDING_HORIZONTAL) {
                ne++;
            } else if (shape == Shape.TENUTO) {
                nt++;
            }
        }

        // A bit tedious
        StringBuilder sb = new StringBuilder();
        sb.append("S#")
                .append(system.getId());
        sb.append(" ");

        if (nl > 0) {
            sb.append("ledgers: ")
                    .append(nl);
        } else if (logger.isDebugEnabled()) {
            sb.append("No ledger");
        }

        if (nt > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append(nt)
                    .append(" tenuto")
                    .append((nt > 1) ? "s" : "");
        } else if (logger.isDebugEnabled()) {
            sb.append("No tenuto");
        }

        if (ne > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append(ne)
                    .append(" ending")
                    .append((ne > 1) ? "s" : "");
        } else if (logger.isDebugEnabled()) {
            sb.append("No ending");
        }

        logger.debug("{}{}", sheet.getLogPrefix(), sb.toString());
    }

    //---------------//
    // filterLedgers //
    //---------------//
    /**
     * Use smart tests on ledger candidates.
     * Starting from each staff, check one interline higher (and lower) for
     * candidates, etc.
     */
    private void filterLedgers ()
    {
        for (StaffInfo staff : system.getStaves()) {
            logger.debug("Staff#{}", staff.getId());

            // Above staff
            for (int i = -1;; i--) {
                int count = lookupLine(staff, i);

                if (count == 0) {
                    break;
                }
            }

            // Below staff
            for (int i = 1;; i++) {
                int count = lookupLine(staff, i);

                if (count == 0) {
                    break;
                }
            }
        }
    }

    //----------//
    // getBeams //
    //----------//
    /**
     * Retrieve the list of beam / hook interpretations in the system,
     * ordered by abscissa.
     *
     * @return the sequence of system beams (full beams and hooks)
     */
    private List<Inter> getBeams ()
    {
        List<Inter> beams = sig.inters(
                new Predicate<Inter>()
                {
                    @Override
                    public boolean check (Inter inter)
                    {
                        return (inter instanceof AbstractBeamInter)
                               && inter.isGood();
                    }
                });

        Collections.sort(beams, Inter.byAbscissa);

        return beams;
    }

    //--------------------//
    // getCandidateGlyphs //
    //--------------------//
    /**
     * Retrieve possible candidate glyph instances built from provided
     * sections.
     *
     * @param sections the section population to build sticks from
     * @return a collection of candidate glyph instances
     */
    private List<Glyph> getCandidateGlyphs (List<Section> sections)
    {
        // Use filament factory
        FilamentsFactory factory = new FilamentsFactory(
                scale,
                sheet.getNest(),
                GlyphLayer.LEDGER,
                HORIZONTAL,
                Filament.class);

        // Adjust factory parameters
        factory.setMaxThickness(constants.maxFilamentThickness);
        factory.setMinCoreSectionLength(constants.minCoreSectionLength);
        factory.setMaxOverlapDeltaPos(constants.maxOverlapDeltaPos);
        factory.setMaxCoordGap(constants.maxCoordGap);
        factory.setMaxPosGap(constants.maxPosGap);
        factory.setMaxOverlapSpace(constants.maxOverlapSpace);

        if (system.getId() == 1) {
            factory.dump("HorizontalsBuilder factory");
        }

        for (Section section : sections) {
            section.setGlyph(null); // ???????
        }

        return factory.retrieveFilaments(sections, true);
    }

    //----------------------//
    // getCandidateSections //
    //----------------------//
    /**
     * Retrieve good candidate sections.
     * These are sections from a (complete) horizontal lag that do not stand
     * within staves.
     *
     * @return list of sections kept
     */
    private List<Section> getCandidateSections ()
    {
        List<Section> keptSections = new ArrayList<Section>();
        int minWidth = scale.toPixels(constants.minLedgerLengthLow);

        for (Section section : system.getHorizontalFullSections()) {
            // Check minimum length (useless test!!! TODO)
            if (section.getBounds().width < minWidth) {
                continue;
            }

            keptSections.add(section);
        }

        logger.debug(
                "S#{} keptSections: {}",
                system.getId(),
                keptSections.size());

        return keptSections;
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
    private static Point2D getMiddle (Glyph stick)
    {
        final Point2D startPoint = stick.getStartPoint(HORIZONTAL);
        final Point2D stopPoint = stick.getStopPoint(HORIZONTAL);

        return new Point2D.Double(
                (startPoint.getX() + stopPoint.getX()) / 2,
                (startPoint.getY() + stopPoint.getY()) / 2);
    }

    //---------------//
    // getYReference //
    //---------------//
    /**
     * Look for an ordinate reference suitable with provided stick.
     * This may be a ledger on the previous line or the staff line itself
     *
     * @param staff the staff being processed
     * @param index the position WRT to staff
     * @param stick the candidate stick to check
     * @return the ordinate reference found, or null if not found
     */
    private Double getYReference (StaffInfo staff,
                                  int index,
                                  Glyph stick)
    {
        final int prevIndex = (index < 0) ? (index + 1) : (index - 1);
        final LineInfo staffLine = (index < 0) ? staff.getFirstLine()
                : staff.getLastLine();

        if (prevIndex != 0) {
            final Set<LedgerInter> prevLedgers = staff.getLedgers(prevIndex);

            // Check abscissa compatibility
            if (prevLedgers == null) {
                if (stick.isVip()) {
                    logger.info("Ledger candidate {} orphan", stick);
                }

                return null;
            }

            int minOverlap = scale.toPixels(constants.minReferenceOverlap);

            for (LedgerInter ledger : prevLedgers) {
                Rectangle ledgerBox = ledger.getBounds();

                if (GeoUtil.xOverlap(stick.getBounds(), ledgerBox) > 0) {
                    // Use this previous ledger as ordinate reference
                    double xMid = stick.getAreaCenter().x;
                    Glyph ledgerGlyph = ledger.getGlyph();

                    // Middle of stick may fall outside of ledger width
                    if (GeoUtil.xEmbraces(ledgerBox, xMid)) {
                        return ledgerGlyph.getLine()
                                .yAtX(xMid);
                    } else {
                        return LineUtil.intersectionAtX(
                                ledgerGlyph.getStartPoint(HORIZONTAL),
                                ledgerGlyph.getStopPoint(HORIZONTAL),
                                xMid)
                                .getY();
                    }
                }
            }

            if (stick.isVip()) {
                logger.info("Ledger candidate {} local orphan", stick);
            }

            return null;
        } else {
            // Use staff line as reference
            return staffLine.yAt(stick.getAreaCenter().getX());
        }
    }

    //------------//
    // lookupLine //
    //------------//
    /**
     * This is the heart of ledger retrieval, which looks for ledgers
     * on a specific "virtual line".
     *
     * @param staff the staff being processed
     * @param index index of line relative to staff, above staff if negative
     *              (-1, -2, etc), below staff if positive (+1, +2, etc)
     * @return the number of ledgers found on this virtual line
     */
    private int lookupLine (StaffInfo staff,
                            int index)
    {
        logger.debug("Checking staff: {} line: {}", staff.getId(), index);

        final int yMargin = scale.toPixels(constants.ledgerMarginY);
        final LineInfo staffLine = (index < 0) ? staff.getFirstLine()
                : staff.getLastLine();

        // Define bounds for the virtual line, properly shifted and enlarged
        Rectangle staffLineBox = staffLine.getBounds();
        staffLineBox.y += (index * scale.getInterline());
        staffLineBox.grow(0, 2 * yMargin); // Roughly

        final List<LedgerInter> ledgers = new ArrayList<LedgerInter>();

        // Filter enclosed candidates and populate acceptable ledgers
        for (Glyph stick : ledgerCandidates) {
            // Rough containment
            final Point2D middle = getMiddle(stick);

            if (!staffLineBox.contains(middle)) {
                continue;
            }

            if (stick.isVip()) {
                logger.info("VIP lookupLine for {}", stick);
            }

            // Check for presence of ledger on previous line
            // and definition of a reference ordinate (staff line or ledger)
            final Double yRef = getYReference(staff, index, stick);

            if (yRef == null) {
                if (stick.isVip()) {
                    logger.info("VIP no line ref for {}", stick);
                }

                continue;
            }

            // Check precise vertical distance WRT the target ordinate
            final double yTarget = yRef
                                   + (Integer.signum(index) * scale.getInterline());

            LedgerFineSuite fineSuite = new LedgerFineSuite(yTarget);
            SuiteImpacts impacts = fineSuite.getImpacts(stick);
            double grade = impacts.getGrade();

            if (stick.isVip()) {
                logger.info("VIP {}", impacts.getDump());
            }

            if (grade >= constants.minCheckResult.getValue()) {
                stick = system.addGlyph(stick); // Useful???

                LedgerInter ledger = new LedgerInter(stick, impacts);
                sig.addVertex(ledger);
                ledgers.add(ledger);
            }
        }

        // Now, check for collision or support relations within line population
        // and reduce the population accordingly.
        reduceLedgers(staff, index, ledgers);

        // Populate staff with ledgers kept
        for (LedgerInter ledger : ledgers) {
            ledger.getGlyph()
                    .setShape(Shape.LEDGER); // Useful???
            staff.addLedger(ledger, index);

            if (ledger.isVip()) {
                logger.info(
                        "VIP {} in staff#{} at {} for {}",
                        ledger,
                        staff.getId(),
                        index,
                        ledger.getDetails());
            }
        }

        return ledgers.size();
    }

    //---------------//
    // reduceLedgers //
    //---------------//
    /**
     * Check for collision or support relations within line
     * population of ledgers and reduce the population accordingly.
     *
     * @param staff   staff being processed
     * @param index   index of virtual line around staff
     * @param ledgers population of ledger interpretations for the line
     */
    private void reduceLedgers (StaffInfo staff,
                                int index,
                                List<LedgerInter> ledgers)
    {
        int maxDx = scale.toPixels(constants.maxInterLedgerDx);
        int maxDy = scale.toPixels(constants.maxInterLedgerDy);
        Set<Exclusion> exclusions = new LinkedHashSet<Exclusion>();
        Collections.sort(ledgers, Inter.byAbscissa);

        for (int i = 0; i < ledgers.size(); i++) {
            final LedgerInter ledger = ledgers.get(i);
            Point2D ledgerLeft = ledger.getGlyph()
                    .getStartPoint(HORIZONTAL);
            Point2D ledgerRight = ledger.getGlyph()
                    .getStopPoint(HORIZONTAL);
            final Rectangle ledgerBox = ledger.getBounds();
            final Rectangle fatBox = ledger.getBounds();
            fatBox.grow(maxDx, scale.getInterline());

            // Check neighbors on the right only
            for (LedgerInter other : ledgers.subList(i + 1, ledgers.size())) {
                final Rectangle otherBox = other.getBounds();

                if (GeoUtil.xOverlap(ledgerBox, otherBox) > 0) {
                    // Abscissa overlap
                    exclusions.add(
                            sig.insertExclusion(
                                    ledger,
                                    other,
                                    Exclusion.Cause.OVERLAP));
                } else if (GeoUtil.xOverlap(fatBox, otherBox) > 0) {
                    // This is a neighbor, check dy at mid abscissa
                    Point2D otherLeft = other.getGlyph()
                            .getStartPoint(HORIZONTAL);
                    Point2D otherRight = other.getGlyph()
                            .getStopPoint(HORIZONTAL);
                    double xMid = (ledgerRight.getX() + otherLeft.getX()) / 2.0;
                    double xGap = otherLeft.getX() - ledgerRight.getX() - 1;
                    double ledgerY = LineUtil.intersectionAtX(
                            ledgerLeft,
                            ledgerRight,
                            xMid)
                            .getY();
                    double otherY = LineUtil.intersectionAtX(
                            otherLeft,
                            otherRight,
                            xMid)
                            .getY();

                    double yGap = Math.abs(ledgerY - otherY);

                    if (yGap > maxDy) {
                        exclusions.add(
                                sig.insertExclusion(
                                        ledger,
                                        other,
                                        Exclusion.Cause.INCOMPATIBLE));
                    } else {
                        LedgerRelation rel = new LedgerRelation();
                        rel.setDistances(
                                scale.pixelsToFrac(xGap),
                                scale.pixelsToFrac(yGap));
                        sig.addEdge(ledger, other, rel);
                    }
                } else {
                    break; // End of reachable neighbors
                }
            }
        }

        for (Inter inter : ledgers) {
            sig.computeContextualGrade(inter, false);
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

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Check.Grade minCheckResult = new Check.Grade(
                0.4,
                "Minimum result for suite of check");

        Constant.Double maxSlopeHigh = new Constant.Double(
                "slope",
                0.02,
                "High Maximum slope for ending (WRT page slope)");

        Constant.Double maxSlopeForCheck = new Constant.Double(
                "slope",
                0.1,
                "Maximum slope for displaying check board");

        // Constants specified WRT mean line thickness
        // -------------------------------------------
        Scale.LineFraction maxFilamentThickness = new Scale.LineFraction(
                1.5,
                "Maximum filament thickness WRT mean line height");

        Scale.LineFraction maxOverlapDeltaPos = new Scale.LineFraction(
                1.0,
                "Maximum delta position between two overlapping filaments");

        Scale.LineFraction maxThicknessHigh = new Scale.LineFraction(
                2.0,
                "High Maximum thickness of an interesting stick");

        Scale.LineFraction maxThicknessLow = new Scale.LineFraction(
                1.0,
                "Low Maximum thickness of an interesting stick");

        // Constants specified WRT mean interline
        // --------------------------------------
        Scale.Fraction minCoreSectionLength = new Scale.Fraction(
                1.0,
                "Minimum length for a section to be considered as core");

        Scale.Fraction maxCoordGap = new Scale.Fraction(
                0,
                "Maximum delta coordinate for a gap between filaments");

        Scale.Fraction maxPosGap = new Scale.Fraction(
                0.2,
                "Maximum delta position for a gap between filaments");

        Scale.Fraction maxOverlapSpace = new Scale.Fraction(
                0.0,
                "Maximum space between overlapping filaments");

        Scale.Fraction ledgerMarginY = new Scale.Fraction(
                0.3,
                "Margin on ledger ordinate WRT theoretical ordinate");

        Scale.Fraction minEndingLengthHigh = new Scale.Fraction(
                15,
                "High Minimum length for an ending");

        Scale.Fraction minEndingLengthLow = new Scale.Fraction(
                10,
                "Low Minimum length for an ending");

        Scale.Fraction minLedgerLengthHigh = new Scale.Fraction(
                1.5,
                "High Minimum length for a ledger");

        Scale.Fraction minLedgerLengthLow = new Scale.Fraction(
                1.0,
                "Low Minimum length for a ledger");

        Scale.Fraction minThicknessHigh = new Scale.Fraction(
                0.23,
                "High Minimum thickness of an interesting stick");

        Scale.Fraction minThicknessLow = new Scale.Fraction(
                0.06,
                "Low Minimum thickness of an interesting stick");

        Scale.Fraction maxInterLedgerDx = new Scale.Fraction(
                2.0,
                "Maximum inter-ledger abscissa gap for ordinate compatibility test");

        Scale.Fraction maxInterLedgerDy = new Scale.Fraction(
                0.2,
                "Maximum inter-ledger ordinate gap");

        Scale.Fraction minReferenceOverlap = new Scale.Fraction(
                0.5,
                "Minimum abscissa overlap for using ledger as reference");

    }

    //------------------//
    // LedgerCheckBoard //
    //------------------//
    /**
     * A specific board to display intrinsic checks of ledger sticks.
     */
    private class LedgerCheckBoard
            extends CheckBoard<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        public LedgerCheckBoard ()
        {
            super(
                    "LedgerRough",
                    ledgerRoughSuite,
                    sheet.getNest().getGlyphService(),
                    eventClasses);
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

                    // Make sure we have a rather horizontal stick
                    if ((glyph != null)
                        && (Math.abs(glyph.getSlope()) <= constants.maxSlopeForCheck.getValue())) {
                        tellObject(glyph);
                    } else {
                        tellObject(null);
                    }
                }
            } catch (Exception ex) {
                logger.warn(getClass().getName() + " onEvent error", ex);
            }
        }
    }

    //-----------------//
    // LedgerFineSuite //
    //-----------------//
    private class LedgerFineSuite
            extends CheckSuite<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        /**
         * Create a precise check suite.
         *
         * @param yTarget target ordinate (extrapolated from latest ledger or
         *                staff line)
         */
        public LedgerFineSuite (double yTarget)
        {
            super("LedgerFine", constants.minCheckResult.getValue());

            add(1, new MinThicknessCheck());
            add(1, new MaxThicknessCheck());
            add(
                    2,
                    new MinLengthCheck(
                    constants.minLedgerLengthLow,
                    constants.minLedgerLengthHigh));

            add(0.5, new PitchCheck(yTarget));
        }
    }

    //------------------//
    // LedgerRoughSuite //
    //------------------//
    private class LedgerRoughSuite
            extends CheckSuite<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        public LedgerRoughSuite ()
        {
            super("LedgerRough", constants.minCheckResult.getValue());

            add(1, new MinThicknessCheck());
            add(1, new MaxThicknessCheck());
            add(
                    2,
                    new MinLengthCheck(
                    constants.minLedgerLengthLow,
                    constants.minLedgerLengthHigh));
        }
    }

    //-------------------//
    // MaxThicknessCheck //
    //-------------------//
    private class MaxThicknessCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected MaxThicknessCheck ()
        {
            super(
                    "MaxThick",
                    "Check that stick is not too thick",
                    constants.maxThicknessLow,
                    constants.maxThicknessHigh,
                    false,
                    TOO_THICK);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the thickness data
        @Override
        protected double getValue (Glyph stick)
        {
            return sheet.getScale()
                    .pixelsToLineFrac(stick.getMeanThickness(HORIZONTAL));
        }
    }

    //----------------//
    // MinLengthCheck //
    //----------------//
    private class MinLengthCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected MinLengthCheck (Constant.Double low,
                                  Constant.Double high)
        {
            super(
                    "Length",
                    "Check that stick is long enough",
                    low,
                    high,
                    true,
                    TOO_SHORT);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the length data
        @Override
        protected double getValue (Glyph stick)
        {
            return sheet.getScale()
                    .pixelsToFrac(stick.getLength(HORIZONTAL));
        }
    }

    //-------------------//
    // MinThicknessCheck //
    //-------------------//
    private class MinThicknessCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected MinThicknessCheck ()
        {
            super(
                    "MinThick",
                    "Check that stick is thick enough",
                    constants.minThicknessLow,
                    constants.minThicknessHigh,
                    true,
                    TOO_THIN);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the thickness data
        @Override
        protected double getValue (Glyph stick)
        {
            return sheet.getScale()
                    .pixelsToFrac(stick.getMeanThickness(HORIZONTAL));
        }
    }

    //------------//
    // PitchCheck //
    //------------//
    private class PitchCheck
            extends Check<Glyph>
    {
        //~ Instance fields ----------------------------------------------------

        private final double yTarget;

        //~ Constructors -------------------------------------------------------
        protected PitchCheck (double yTarget)
        {
            super(
                    "Pitch",
                    "Check that ledger ordinate is close to theoretical value",
                    Constant.Double.ZERO,
                    constants.ledgerMarginY,
                    false,
                    TOO_SHIFTED);
            this.yTarget = yTarget;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected double getValue (Glyph stick)
        {
            double xMid = getMiddle(stick)
                    .getX();
            double y = stick.getLine()
                    .yAtX(xMid);

            return sheet.getScale()
                    .pixelsToFrac(Math.abs(y - yTarget));
        }
    }

    //------------//
    // SlopeCheck //
    //------------//
    private class SlopeCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected SlopeCheck ()
        {
            super(
                    "Slope",
                    "Check that stick slope is close to global page slope",
                    Constant.Double.ZERO,
                    constants.maxSlopeHigh,
                    false,
                    TOO_SLOPED);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the absolute slope
        @Override
        protected double getValue (Glyph stick)
        {
            return Math.abs(stick.getSlope() - sheet.getSkew().getSlope());
        }
    }
}
