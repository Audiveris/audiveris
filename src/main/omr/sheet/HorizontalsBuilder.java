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
import omr.check.Checkable;
import omr.check.Failure;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.GlyphsController;

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

import omr.sig.FullBeamInter;
import omr.sig.Inter;
import omr.sig.LedgerInter;
import omr.sig.SIGraph;

import omr.step.Step;
import omr.step.StepException;

import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Class {@code HorizontalsBuilder} is in charge of retrieving
 * ledgers and endings in a system.
 *
 * <p>
 * Nota: Endings are currently disabled.
 *
 * @author Hervé Bitteur
 */
public class HorizontalsBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            HorizontalsBuilder.class);

    /** Events this entity is interested in */
    private static final Class<?>[] eventClasses = new Class<?>[]{
        GlyphEvent.class
    };

    /** Failure codes */
    private static final Failure TOO_SHORT = new Failure("Hori-TooShort");

    private static final Failure TOO_LONG = new Failure("Hori-TooLong");

    private static final Failure TOO_THIN = new Failure("Hori-TooThin");

    private static final Failure TOO_THICK = new Failure(
            "Hori-TooThick");

    private static final Failure TOO_HOLLOW = new Failure(
            "Hori-TooHollow");

    private static final Failure TOO_SLOPED = new Failure(
            "Hori-TooSloped");

    //~ Instance fields --------------------------------------------------------
    //
    /** Related sheet. */
    private final Sheet sheet;

    /** Dedicated system. */
    private final SystemInfo system;

    /** Global sheet scale. */
    private final Scale scale;

    /** Check suite for endings. */
    private CheckSuite<Glyph> endingSuite;

    /** Check suite for ledgers. */
    private CheckSuite<Glyph> ledgerSuite;

    /** The current collection of ledger candidates. */
    private final List<Glyph> ledgerCandidates = new ArrayList<Glyph>();

    /** Glyphs controller, if any. */
    private GlyphsController controller;

    /** Minimum length for a full ledger. */
    private final int minFullLedgerLength;

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

        sheet = system.getSheet();
        scale = sheet.getScale();

        minFullLedgerLength = scale.toPixels(constants.minFullLedgerLength);
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // addCheckBoard //
    //---------------//
    public void addCheckBoard ()
    {
        sheet.getAssembly()
                .addBoard(Step.DATA_TAB, new LedgerCheckBoard());
    }

    //--------------//
    // buildLedgers //
    //--------------//
    /**
     * Search horizontal sticks for ledgers and endings.
     *
     * @throws StepException raised if process gets stopped
     */
    public void buildLedgers ()
    {
        try {
            // Filter which sections to provide to factory
            List<Section> sections = getCandidateSections();

            // Retrieve candidate glyphs out of candidate sections
            List<Glyph> sticks = getCandidateGlyphs(sections);

            // Apply basic checks for ledgers candidates, tenutos, endings
            checkHorizontals(sticks);

            // Discard candidates that overlap good beams
            discardBeamOverlaps();

            // Filter ledgers more accurately
            filterLedgers();
        } catch (Throwable ex) {
            logger.warn("Error retrieving horizontals. ", ex);
        } finally {
            // User feedback
            feedback();
        }
    }

    //---------------//
    // getController //
    //---------------//
    /**
     * @return the controller
     */
    public GlyphsController getController ()
    {
        return controller;
    }

    //-------------//
    // getMaxShift //
    //-------------//
    public static Scale.Fraction getMaxShift ()
    {
        return constants.maxShift;
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
    public static Point2D getMiddle (Glyph stick)
    {
        final Point2D startPoint = stick.getStartPoint(HORIZONTAL);
        final Point2D stopPoint = stick.getStopPoint(HORIZONTAL);

        return new Point2D.Double(
                (startPoint.getX() + stopPoint.getX()) / 2,
                (startPoint.getY() + stopPoint.getY()) / 2);
    }

    //------------------------//
    // getMinFullLedgerLength //
    //------------------------//
    public static Scale.Fraction getMinFullLedgerLength ()
    {
        return constants.minFullLedgerLength;
    }

    //--------------//
    // isFullLedger //
    //--------------//
    public boolean isFullLedger (Glyph glyph)
    {
        return glyph.getLength(HORIZONTAL) >= minFullLedgerLength;
    }

    //----------------//
    // checkCollision //
    //----------------//
    /**
     * Check for a potential collision with another ledger.
     * Within the sequence of ledgers already defined for current line index,
     * we check for abscissa overlap or for strong ordinate variation.
     *
     * @param staff   the staff being processed
     * @param index   the position WRT to staff
     * @param stick   the candidate stick to check
     * @param yTarget the target ordinate for current stick
     * @param delta   the gap between stick ordinate and target ordinate
     * @return true if OK, false if not OK
     */
    private boolean checkCollision (StaffInfo staff,
                                    int index,
                                    Glyph stick,
                                    double yTarget,
                                    double delta)
    {
        final Set<Glyph> siblings = staff.getLedgers(index);

        if ((siblings != null) && !siblings.isEmpty()) {
            if (stick.isVip()) {
                logger.info("VIP checkCollision for {}", stick);
            }

            final List<Glyph> concurrents = new ArrayList<Glyph>();
            final Rectangle box = stick.getBounds();

            // Check for abscissa compatibility
            for (Glyph ledger : siblings) {
                if (GeoUtil.xOverlap(box, ledger.getBounds()) > 0) {
                    concurrents.add(ledger);
                }
            }

            // Check for ordinate compatibility with close neighbors
            int maxDx = scale.toPixels(constants.maxInterLedgerDxForTest);
            int maxDy = scale.toPixels(constants.maxInterLedgerDy);
            Rectangle fatBox = stick.getBounds();
            fatBox.grow(maxDx, scale.getInterline());

            for (Glyph ledger : siblings) {
                if (concurrents.contains(ledger)) {
                    continue;
                }

                Rectangle ledgerBox = ledger.getBounds();

                if (GeoUtil.xOverlap(fatBox, ledgerBox) > 0) {
                    // This is a neighbor, check dy at mid abscissa
                    Point2D left = stick.getStartPoint(HORIZONTAL);
                    Point2D right = stick.getStopPoint(HORIZONTAL);
                    Point2D ledgerLeft = ledger.getStartPoint(HORIZONTAL);
                    Point2D ledgerRight = ledger.getStopPoint(HORIZONTAL);
                    final double xMid;

                    if (ledgerBox.x < box.x) {
                        xMid = (ledgerRight.getX() + left.getX()) / 2.0;
                    } else {
                        xMid = (right.getX() + ledgerLeft.getX()) / 2.0;
                    }

                    double ledgerY = LineUtil.intersectionAtX(
                            ledgerLeft,
                            ledgerRight,
                            xMid)
                            .getY();
                    double stickY = LineUtil.intersectionAtX(left, right, xMid)
                            .getY();

                    if (Math.abs(ledgerY - stickY) > maxDy) {
                        concurrents.add(ledger);
                    }
                }
            }

            // Decide between candidate and concurrents
            for (Glyph other : concurrents) {
                // Keep the one with smallest delta
                final double yOther = getMiddle(other)
                        .getY();
                final double otherDelta = Math.abs(yOther - yTarget);

                if (delta >= otherDelta) {
                    // Discard this candidate ledger
                    if (stick.isVip()) {
                        logger.info(
                                "Candidate ledger {} collides with {}",
                                stick,
                                other);
                    }

                    return false;
                } else {
                    // Remove the other ledger
                    if (other.isVip()) {
                        logger.info(
                                "Ledger {} collides with candidate {}",
                                other,
                                stick);
                    }

                    other.setShape(null);
                    staff.removeLedger(other);

                    // Remove interpretation as well
                    for (Inter inter : other.getInterpretations()) {
                        if (inter.getShape() == Shape.LEDGER) {
                            inter.delete();

                            break;
                        }
                    }
                }
            }
        }

        return true;
    }

    //------------------//
    // checkHorizontals //
    //------------------//
    /**
     * Run checks on raw horizontal candidates and copy good candidates
     * to ledgerCandidates collection (and to endingcandidates).
     *
     * @param sticks the raw horizontal glyph instances
     */
    private void checkHorizontals (List<Glyph> sticks)
    {
        ledgerCandidates.clear();

        // Define the suites of Checks
        double minResult = constants.minCheckResult.getValue();

        // Create suites and collections
        createSuites();

        for (Glyph stick : sticks) {
            if (stick.isVip()) {
                logger.info("VIP checkHorizontals for {}", stick);
            }

            // Run the Ledger Checks
            if (ledgerSuite.pass(stick, null) >= minResult) {
                //                stick.setResult(LEDGER);
                //                stick.setShape(Shape.LEDGER);
                //                ledgers.add(stick);
                ledgerCandidates.add(stick);

                //                if (logger.isDebugEnabled()) {
                //                    logger.debug("Ledger candidate " + stick);
                //                }
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
    }

    //--------------//
    // createSuites //
    //--------------//
    private void createSuites ()
    {
        // ledgerSuite
        ledgerSuite = new CheckSuite<Glyph>(
                "Ledger",
                constants.minCheckResult.getValue());
        ledgerSuite.add(1, new MinThicknessCheck());
        ledgerSuite.add(1, new MaxThicknessCheck());
        ledgerSuite.add(
                1,
                new MinLengthCheck(
                        constants.minLedgerLengthLow,
                        constants.minLedgerLengthHigh));
        ledgerSuite.add(1, new MaxLengthCheck());
        ledgerSuite.add(1, new MinDensityCheck());

        // endingSuite
        endingSuite = new CheckSuite<Glyph>(
                "Ending",
                constants.minCheckResult.getValue());
        endingSuite.add(1, new MinThicknessCheck());
        endingSuite.add(1, new MaxThicknessCheck());
        endingSuite.add(
                1,
                new MinLengthCheck(
                        constants.minEndingLengthLow,
                        constants.minEndingLengthHigh));
        endingSuite.add(1, new SlopeCheck());

        if (logger.isDebugEnabled()) {
            ledgerSuite.dump();
            endingSuite.dump();
        }
    }

    //---------------------//
    // discardBeamOverlaps //
    //---------------------//
    /**
     * Discard the ledger candidates that overlap a (good) beam.
     */
    private void discardBeamOverlaps ()
    {
        List<Glyph> toRemove = new ArrayList<Glyph>();
        List<Inter> beams = getBeams();

        for (Glyph stick : ledgerCandidates) {
            // Check whether stick middle point is contained by a beam glyph
            Point2D middle = getMiddle(stick);

            for (Inter inter : beams) {
                FullBeamInter beam = (FullBeamInter) inter;

                if (beam.getArea()
                        .contains(middle)) {
                    if (stick.isVip() || logger.isDebugEnabled()) {
                        logger.info(
                                "ledger#{} overlaps beam#{}",
                                stick.getId(),
                                beam.getId());
                    }

                    toRemove.add(stick);

                    break;
                } else {
                    // Speedup, since beams are sorted by abscissa
                    if (beam.getBounds()
                            .getLocation().x > middle.getX()) {
                        break;
                    }
                }
            }
        }

        if (!toRemove.isEmpty()) {
            logger.debug(
                    "S#{} {} ledger/beam overlaps",
                    system.getId(),
                    toRemove.size());
            ledgerCandidates.removeAll(toRemove);
        }
    }

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
                int count = lookupLine(i, staff);

                if (count == 0) {
                    break;
                }
            }

            // Below staff
            for (int i = 1;; i++) {
                int count = lookupLine(i, staff);

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
     * Retrieve the list of beam-shaped glyph instances in the system,
     * ordered by abscissa.
     *
     * @return the sequence of system beams
     */
    private List<Inter> getBeams ()
    {
        SIGraph sig = system.getSig();
        List<Inter> beams = sig.inters(
                new Predicate<Inter>()
                {
                    @Override
                    public boolean check (Inter inter)
                    {
                        return (inter.getShape() == Shape.BEAM)
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
            final Set<Glyph> prevLedgers = staff.getLedgers(prevIndex);

            // Check abscissa compatibility
            if (prevLedgers == null) {
                if (stick.isVip()) {
                    logger.info("Ledger candidate {} orphan", stick);
                }

                return null;
            }

            for (Glyph ledger : prevLedgers) {
                if (GeoUtil.xOverlap(stick.getBounds(), ledger.getBounds()) > 0) {
                    // Use this previous ledger as ordinate reference
                    return getMiddle(ledger)
                            .getY();
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
     * Look up for ledgers on a specific line above or below the
     * provided staff line.
     *
     * @param index index of line, above staff if negative, below staff if
     *              positive
     * @param staff the staff being checked
     * @return the number of ledgers found on this "virtual line"
     */
    private int lookupLine (int index,
                            StaffInfo staff)
    {
        logger.debug("Checking line {}", index);

        final int yMargin = scale.toPixels(constants.ledgerMarginY);
        final LineInfo staffLine = (index < 0) ? staff.getFirstLine()
                : staff.getLastLine();
        final SIGraph sig = system.getSig();
        int found = 0; // Number of ledgers found on this line

        // Define bounds for the virtual line, properly shifted and enlarged
        Rectangle staffLineBox = staffLine.getBounds();
        staffLineBox.y += (index * scale.getInterline());
        staffLineBox.grow(0, 2 * yMargin); // Roughly

        // Filter enclosed candidates
        for (Glyph stick : ledgerCandidates) {
            // Rough containment
            if (!staffLineBox.contains(stick.getAreaCenter())) {
                continue;
            }

            // Check for presence of ledger on previous line
            // and definition of a reference ordinate (staff line or ledger)
            final Point2D middle = getMiddle(stick);
            final Double yRef = getYReference(staff, index, stick);

            if (yRef == null) {
                continue;
            }

            // Check precise vertical distance WRT the target ordinate
            final double yTarget = yRef
                                   + (Integer.signum(index) * scale.getInterline());
            final int delta = (int) Math.rint(
                    Math.abs(middle.getY() - yTarget));

            if (delta > yMargin) {
                continue;
            }

            // Check for overlapping ledger candidate at same level
            if (!checkCollision(staff, index, stick, yTarget, delta)) {
                continue;
            }

            // OK!
            Glyph glyph = system.addGlyph(stick); // Useful???
            glyph.setShape(Shape.LEDGER);
            sig.addVertex(new LedgerInter(glyph, 1.0)); // TODO: grade computation
            staff.addLedger(glyph, index);
            found++;

            if (stick.isVip() || logger.isDebugEnabled()) {
                logger.info("Ledger at {} for {}", index, stick);
            }
        }

        return found;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Ratio maxConsistentRatio = new Constant.Ratio(
                1.7,
                "Maximum thickness ratio for consistent merge");

        // Constants specified WRT mean line thickness
        // -------------------------------------------
        Scale.LineFraction maxFilamentThickness = new Scale.LineFraction(
                1.85,
                "Maximum filament thickness WRT mean line height");

        Scale.LineFraction maxOverlapDeltaPos = new Scale.LineFraction(
                1.0,
                "Maximum delta position between two overlapping filaments");

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
                0.25,
                "Margin on ledger ordinate");

        Scale.Fraction maxStemDx = new Scale.Fraction(
                1.5,
                "Maximum horizontal distance between ledger and stem");

        Scale.Fraction maxStemDy = new Scale.Fraction(
                0.5,
                "Maximum vertical distance between ledger and stem");

        Scale.Fraction minFullLedgerLength = new Scale.Fraction(
                1.0,
                "Minimum length for a full ledger with no stem");

        Scale.Fraction maxShift = new Scale.Fraction(
                0.1,
                "Max shift between two runs of ledger sections");

        Scale.Fraction extensionMinPointNb = new Scale.Fraction(
                0.2,
                "Minimum number of points to compute extension of crossing objects");

        Scale.Fraction maxLengthHigh = new Scale.Fraction(
                20,
                "High Maximum length for a ledger");

        Scale.Fraction maxLengthLow = new Scale.Fraction(
                5,
                "Low Maximum length for a ledger");

        Scale.LineFraction maxThicknessHigh = new Scale.LineFraction(
                1.2,
                "High Maximum thickness of an interesting stick");

        Scale.LineFraction maxThicknessLow = new Scale.LineFraction(
                1.0,
                "Low Maximum thickness of an interesting stick");

        Check.Grade minCheckResult = new Check.Grade(
                0.50,
                "Minimum result for suite of check");

        Constant.Ratio densityHigh = new Constant.Ratio(
                1,
                "High density for a horizontal");

        Constant.Ratio densityLow = new Constant.Ratio(
                0.6,
                "Low density for a horizontal");

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

        Constant.Double maxSlopeLow = new Constant.Double(
                "slope",
                0.01,
                "Low Maximum slope for ending");

        Constant.Double maxSlopeHigh = new Constant.Double(
                "slope",
                0.02,
                "High Maximum slope for ending");

        Constant.Double maxSlopeForCheck = new Constant.Double(
                "slope",
                0.1,
                "Maximum slope for checking a ledger candidate");

        Scale.Fraction maxInterLedgerDxForTest = new Scale.Fraction(
                2.0,
                "Maximum inter-ledger abscissa gap for ordinate compatibility test");

        Scale.Fraction maxInterLedgerDy = new Scale.Fraction(
                0.2,
                "Maximum inter-ledger ordinate gap");

    }

    //-----------------//
    // MinDensityCheck //
    //-----------------//
    private static class MinDensityCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected MinDensityCheck ()
        {
            super(
                    "Density",
                    "Check that stick fills its bounding rectangle",
                    constants.densityLow,
                    constants.densityHigh,
                    true,
                    TOO_HOLLOW);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the density
        @Override
        protected double getValue (Glyph stick)
        {
            Rectangle rect = stick.getBounds();
            double area = rect.width * rect.height;

            return (double) stick.getWeight() / area;
        }
    }

    //--------------//
    // GlyphContext //
    //--------------//
    /**
     * Class to handle the context of checks performed on a stick.
     */
    private class GlyphContext
            implements Checkable
    {
        //~ Instance fields ----------------------------------------------------

        /** The stick being checked. */
        final Glyph stick;

        /** Stick middle point. */
        final Point2D mid;

        //~ Constructors -------------------------------------------------------
        public GlyphContext (Glyph stick)
        {
            this.stick = stick;
            mid = getMiddle(stick);
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
            StringBuilder sb = new StringBuilder();
            sb.append("stick#")
                    .append(stick.getId());

            return sb.toString();
        }
    }

    //------------------//
    // LedgerCheckBoard //
    //------------------//
    /**
     * A specific board dedicated to physical checks of ledger sticks
     */
    private class LedgerCheckBoard
            extends CheckBoard<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        public LedgerCheckBoard ()
        {
            super(
                    "LedgerCheck",
                    ledgerSuite,
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

                    if (glyph != null) {
                        // Make sure this is a rather horizontal stick
                        if (Math.abs(glyph.getSlope()) <= constants.maxSlopeForCheck.getValue()) {
                            // Get a fresh suite
                            //setSuite(system.createLedgerCheckSuite(true));
                            tellObject(glyph);

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

    //----------------//
    // MaxLengthCheck //
    //----------------//
    private class MaxLengthCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected MaxLengthCheck ()
        {
            super(
                    "MaxLength",
                    "Check that stick is not too long",
                    constants.maxLengthLow,
                    constants.maxLengthHigh,
                    false,
                    TOO_LONG);
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
    // MaxThicknessCheck //
    //-------------------//
    private class MaxThicknessCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected MaxThicknessCheck ()
        {
            super(
                    "MaxThickness",
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
                    .pixelsToFrac(stick.getThickness(HORIZONTAL));
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
                    "MinLength",
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
                    "MinThickness",
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
                    .pixelsToFrac(stick.getThickness(HORIZONTAL));
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
                    constants.maxSlopeLow,
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
