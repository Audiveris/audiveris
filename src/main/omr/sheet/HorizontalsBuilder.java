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
import omr.check.FailureResult;
import omr.check.Result;
import omr.check.SuccessResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.GlyphsController;

import omr.grid.Filament;
import omr.grid.FilamentsFactory;
import omr.grid.LineInfo;
import omr.grid.StaffInfo;

import omr.lag.Section;

import static omr.run.Orientation.*;

import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.UserEvent;

import omr.step.Step;
import omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Class {@code HorizontalsBuilder} is in charge of retrieving
 * horizontal dashes (ledgers, legato signs and endings) in a system.
 *
 * <p>Nota: Endings and legato signs are currently disabled.
 *
 * <p>TODO: This class is a monster, without any compelling reason!
 * Serious simplification is needed.
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
        GlyphEvent.class};

    /** Success codes */
    private static final SuccessResult LEDGER = new SuccessResult("Ledger");

    private static final SuccessResult ENDING = new SuccessResult("Ending");

    /** Failure codes */
    private static final FailureResult TOO_SHORT = new FailureResult(
            "Hori-TooShort");

    private static final FailureResult TOO_LONG = new FailureResult(
            "Hori-TooLong");

    private static final FailureResult TOO_THIN = new FailureResult(
            "Hori-TooThin");

    private static final FailureResult TOO_THICK = new FailureResult(
            "Hori-TooThick");

    private static final FailureResult TOO_HOLLOW = new FailureResult(
            "Hori-TooHollow");

    private static final FailureResult IN_STAFF = new FailureResult(
            "Hori-InStaff");

    private static final FailureResult TOO_FAR = new FailureResult(
            "Hori-TooFar");

    private static final FailureResult TOO_ADJA = new FailureResult(
            "Hori-TooHighAdjacency");

    private static final FailureResult BI_CHUNK = new FailureResult(
            "Hori-BiChunk");

    private static final FailureResult TOO_SLOPED = new FailureResult(
            "Hori-TooSloped");

    //~ Instance fields --------------------------------------------------------
    /** Related sheet */
    private final Sheet sheet;

    /** Dedicated system */
    private final SystemInfo system;

    /** Global sheet scale */
    private final Scale scale;

    /** Check suite for common tests */
    private CheckSuite<Glyph> commonSuite;

    /** Check suite for Additional tests for endings */
    private CheckSuite<Glyph> endingSuite;

    /** Check suite for Additional tests for ledgers */
    private CheckSuite<Glyph> ledgerSuite;

    /** Total check suite for ending */
    private ArrayList<CheckSuite<Glyph>> endingList;

    /** Total check suite for ledger */
    private ArrayList<CheckSuite<Glyph>> ledgerList;

    /** The current collection of ledger candidates */
    private final List<GlyphContext> ledgerCandidates = new ArrayList<>();

    /** Tenuto signs found */
    private final List<Glyph> tenutos;

    /** Endings found */
    private final List<Glyph> endings;

    /** Glyphs controller, if any */
    private GlyphsController controller;

    /** Minimum length for a full ledger */
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
        scale = system.getSheet().getScale();

        tenutos = system.getTenutos();
        endings = system.getEndings();

        minFullLedgerLength = scale.toPixels(constants.minFullLedgerLength);
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // addCheckBoard //
    //---------------//
    public void addCheckBoard ()
    {
        sheet.getAssembly().addBoard(Step.DATA_TAB, new LedgerCheckBoard());
    }

    //    //--------------//
    //    // assignGlyphs //
    //    //--------------//
    //    /**
    //     * Assign a collection of glyphs to a Ledger or Ending shape
    //     * @param glyphs the collection of glyphs to be assigned
    //     * @param shape the shape to be assigned
    //     * @param compound flag to build one compound, rather than assign each
    //     *                 individual glyph
    //     * @param grade the grade we have wrt the assigned shape
    //     */
    //    @Override
    //    public void assignGlyphs (Collection<Glyph> glyphs,
    //                              Shape             shape,
    //                              boolean           compound,
    //                              double            grade)
    //    {
    //        super.assignGlyphs(glyphs, shape, compound, grade);
    //
    //        lagView.colorizeAllSections();
    //    }
    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Run the Horizontals step, searching all horizontal sticks for
     * typical things like ledgers, endings and legato signs.
     *
     * @throws StepException raised if process gets stopped
     */
    public void buildInfo ()
            throws Exception
    {
        try {
            // Filter which sections to provide to factory
            List<Section> sections = getCandidateSections(
                    system.getHorizontalSections());

            /// NO: sections.addAll(system.getVerticalSections());

            // Retrieve candidate glyphs out of candidate sections
            List<Glyph> sticks = getCandidateGlyphs(sections);

            // Apply basic checks for ledgers candidates, tenutos, endings
            checkHorizontals(sticks);

            // Filter ledgers more accurately
            filterLedgers();
        } catch (Throwable ex) {
            logger.warn("Error retrieving horizontals", ex);
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

    //--------------//
    // isFullLedger //
    //--------------//
    public boolean isFullLedger (Glyph glyph)
    {
        return glyph.getLength(HORIZONTAL) >= minFullLedgerLength;
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
                int count = lookupLine(i, staff.getFirstLine());

                if (count == 0) {
                    break;
                }
            }

            // Below staff
            for (int i = 1;; i++) {
                int count = lookupLine(i, staff.getLastLine());

                if (count == 0) {
                    break;
                }
            }
        }
    }

    //----------------------//
    // getCandidateSections //
    //----------------------//
    private List<Section> getCandidateSections (Collection<Section> allSections)
    {
        List<Section> keptSections = new ArrayList<>();

        for (Section section : allSections) {
            if ((section.getGlyph() == null)
                || !section.getGlyph().isWellKnown()) {
                keptSections.add(section);
            }
        }

        return keptSections;
    }

    //---------------//
    // staffDistance //
    //---------------//
    private double staffDistance (Glyph stick)
    {
        // Compute the (algebraic) distance from the stick to the nearest
        // staff. Distance is negative if the stick is within the staff,
        // positive outside.
        final Point2D mid = new Point2D.Double(
                (stick.getStartPoint(HORIZONTAL).getX() + stick.getStopPoint(
                HORIZONTAL).getX()) / 2,
                (stick.getStartPoint(HORIZONTAL).getY() + stick.getStopPoint(
                HORIZONTAL).getY()) / 2);
        final StaffInfo staff = system.getStaffAt(mid);
        final double top = staff.getFirstLine().yAt(mid.getX());
        final double bottom = staff.getLastLine().yAt(mid.getX());
        final double dist = Math.max(top - mid.getY(), mid.getY() - bottom);

        return system.getSheet().getScale().pixelsToFrac(dist);
    }

    //------------------//
    // checkHorizontals //
    //------------------//
    private void checkHorizontals (List<Glyph> sticks)
    {
        ledgerCandidates.clear();

        // Define the suites of Checks
        double minResult = constants.minCheckResult.getValue();

        // Create suites and collections
        createSuites();

        for (Glyph stick : sticks) {
            // Allocate the candidate context, and pass the whole check suite
            GlyphContext context = new GlyphContext(stick);

            // Run the Ledger Checks
            if (CheckSuite.passCollection(stick, ledgerList) >= minResult) {
                //                stick.setResult(LEDGER);
                //                stick.setShape(Shape.LEDGER);
                //                ledgers.add(stick);
                ledgerCandidates.add(context);

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

    //    //----------------//
    //    // deassignGlyphs //
    //    //----------------//
    //    /**
    //     * De-Assign a collection of glyphs.
    //     *
    //     * @param glyphs the collection of glyphs to be de-assigned
    //     */
    //    @Override
    //    public void deassignGlyphs (Collection<Glyph> glyphs)
    //    {
    //        //        super.deassignGlyphs(glyphs);
    //        //
    //        //        lagView.colorizeAllSections();
    //    }
    //    //-------------//
    //    // assignGlyph //
    //    //-------------//
    //    @Override
    //    protected Glyph assignGlyph (Glyph  glyph,
    //                                 Shape  shape,
    //                                 double grade)
    //    {
    //        //        if (shape != null) {
    //        //            // Assign
    //        //            if (logger.isDebugEnabled()) {
    //        //                logger.debug(
    //        //                    "Assign horizontal glyph#" + glyph.getId() + " to " +
    //        //                    shape);
    //        //            }
    //        //
    //        //            Dash dash = null;
    //        //
    //        //            switch (shape) {
    //        //            case LEDGER :
    //        //                dash = new Ledger((Glyph) glyph);
    //        //                info.getLedgers()
    //        //                    .add((Ledger) dash);
    //        //
    //        //                break;
    //        //
    //        //            case ENDING_HORIZONTAL :
    //        //                dash = new Ending((Glyph) glyph);
    //        //                info.getEndings()
    //        //                    .add((Ending) dash);
    //        //
    //        //                break;
    //        //
    //        //            default :
    //        //                assert false;
    //        //            }
    //        //
    //        //            cleanup(Collections.singletonList(dash));
    //        //            allDashes.add(dash);
    //        //            dashSections.addAll(glyph.getMembers());
    //        //        } else {
    //        //            // Deassign
    //        //            if (logger.isDebugEnabled()) {
    //        //                logger.debug("Deassign horizontal glyph#" + glyph.getId());
    //        //            }
    //        //
    //        //            Dash dash = null;
    //        //
    //        //            switch (glyph.getShape()) {
    //        //            case LEDGER :
    //        //                dash = info.getLedgerOf(glyph);
    //        //                info.getLedgers()
    //        //                    .remove((Ledger) dash);
    //        //
    //        //                break;
    //        //
    //        //            case ENDING_HORIZONTAL :
    //        //                dash = info.getEndingOf(glyph);
    //        //                info.getEndings()
    //        //                    .remove((Ending) dash);
    //        //
    //        //                break;
    //        //
    //        //            default :
    //        //                assert false;
    //        //            }
    //        //
    //        //            // Remove the patches and restore the glyph
    //        //            lineCleaner.restoreStick((Glyph) glyph, dash.getPatches());
    //        //            allDashes.remove(dash);
    //        //            dashSections.removeAll(glyph.getMembers());
    //        //        }
    //        //
    //        //        return super.assignGlyph(glyph, shape, grade);
    //        return null;
    //    }
    //    //---------//
    //    // cleanup //
    //    //---------//
    //    private void cleanup (List<?extends Dash> dashes)
    //    {
    //        for (Dash dash : dashes) {
    //            dash.setPatches(lineCleaner.cleanupStick(dash.getStick()));
    //        }
    //    }
    //--------------//
    // createSuites //
    //--------------//
    private void createSuites ()
    {
        // Common horizontal suite
        commonSuite = new CheckSuite<>(
                "Common",
                constants.minCheckResult.getValue());
        commonSuite.add(1, new MinThicknessCheck()); // Minimum thickness
        commonSuite.add(1, new MaxThicknessCheck());
        commonSuite.add(1, new MinDistCheck()); // Not within staves
        commonSuite.add(1, new MaxDistCheck()); // Not too far from staves
        commonSuite.add(1, new FirstAdjacencyCheck());
        commonSuite.add(1, new LastAdjacencyCheck());

        // ledgerSuite
        ledgerSuite = new CheckSuite<>(
                "Ledger",
                constants.minCheckResult.getValue());
        ledgerSuite.add(
                1,
                new MinLengthCheck(
                constants.minLedgerLengthLow,
                constants.minLedgerLengthHigh)); // Minimum length
        ledgerSuite.add(1, new MaxLengthCheck()); // Maximum length
        ledgerSuite.add(1, new MinDensityCheck());
        ///ledgerSuite.add(1, new ChunkCheck()); // At least one edge WITHOUT a chunk

        // Ledger collection
        ledgerList = new ArrayList<>();
        ledgerList.add(commonSuite);
        ledgerList.add(ledgerSuite);

        // endingSuite
        endingSuite = new CheckSuite<>(
                "Ending",
                constants.minCheckResult.getValue());
        endingSuite.add(
                1,
                new MinLengthCheck(
                constants.minEndingLengthLow,
                constants.minEndingLengthHigh)); // Minimum length
        endingSuite.add(1, new SlopeCheck());

        // Ending collection
        endingList = new ArrayList<>();
        endingList.add(commonSuite);
        endingList.add(endingSuite);

        if (logger.isDebugEnabled()) {
            commonSuite.dump();
            ledgerSuite.dump();
            endingSuite.dump();
        }
    }

    //----------//
    // feedback //
    //----------//
    private void feedback ()
    {
        int nl = 0;
        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == Shape.LEDGER) {
                nl++;
            }
        }
        int nt = tenutos.size();
        int ne = endings.size();

        // A bit tedious
        StringBuilder sb = new StringBuilder();
        sb.append("S#").append(system.getId());
        sb.append(" ");

        if (nl > 0) {
            sb.append(nl).append(" ledger").append((nl > 1) ? "s" : "");
        } else if (logger.isDebugEnabled()) {
            sb.append("No ledger");
        }

        if (nt > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append(nt).append(" tenuto").append((nt > 1) ? "s" : "");
        } else if (logger.isDebugEnabled()) {
            sb.append("No tenuto");
        }

        if (ne > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append(ne).append(" ending").append((ne > 1) ? "s" : "");
        } else if (logger.isDebugEnabled()) {
            sb.append("No ending");
        }

        logger.debug("{}{}", sheet.getLogPrefix(), sb.toString());
    }

    //    //------------------//
    //    // getAliensAtStart //
    //    //------------------//
    //    /**
    //     * Count alien pixels in the following rectangle...
    //     * <pre>
    //     * +-------+
    //     * |       |
    //     * +=======+==================================+
    //     * |       |
    //     * +-------+
    //     * </pre>
    //     *
    //     * @param glyph the glyph at stake
    //     * @param halfHeight half rectangle size along stick thickness
    //     * @param width rectangle size along stick length
    //     * @return the number of alien pixels found
    //     */
    //    private int getAliensAtStart (Glyph glyph,
    //                                  int   halfHeight,
    //                                  int   width)
    //    {
    //        Point2D        start = glyph.getStartPoint();
    //        Rectangle roi = new Rectangle(
    //            (int) Math.rint(start.getX()),
    //            (int) Math.rint(start.getY() - halfHeight),
    //            width,
    //            2 * halfHeight);
    //            glyph.addAttachment("ll", roi);
    //        
    //        int            count = 0;
    //        count += glyph.getAlienPixelsFrom(sheet.getHorizontalLag(), roi, null);
    //        count += glyph.getAlienPixelsFrom(sheet.getVerticalLag(), roi, null);
    //
    //        return count;
    //    }
    //
    //    //-----------------//
    //    // getAliensAtStop //
    //    //-----------------//
    //    /**
    //     * Count alien pixels in the following rectangle...
    //     * <pre>
    //     *                                    +-------+
    //     *                                    |       |
    //     * +==================================+=======+
    //     *                                    |       |
    //     *                                    +-------+
    //     * </pre>
    //     *
    //     * @param glyph the glyph at stake
    //     * @param halfHeight half rectangle size along stick thickness
    //     * @param width rectangle size along stick length
    //     * @return the number of alien pixels found
    //     */
    //    private int getAliensAtStop (Glyph glyph,
    //                                 int   halfHeight,
    //                                 int   width)
    //    {
    //        Point2D        stop = glyph.getStopPoint();
    //        Rectangle roi = new Rectangle(
    //            (int) Math.rint(stop.getX() - width),
    //            (int) Math.rint(stop.getY() - halfHeight),
    //            width,
    //            2 * halfHeight);
    //            glyph.addAttachment("lr", roi);
    //        int            count = 0;
    //        count += glyph.getAlienPixelsFrom(sheet.getHorizontalLag(), roi, null);
    //        count += glyph.getAlienPixelsFrom(sheet.getVerticalLag(), roi, null);
    //
    //        return count;
    //    }
    //--------------------//
    // getCandidateGlyphs //
    //--------------------//
    private List<Glyph> getCandidateGlyphs (List<Section> sections)
            throws Exception
    {
        // Use filament factory
        FilamentsFactory factory = new FilamentsFactory(
                scale,
                sheet.getNest(),
                HORIZONTAL,
                Filament.class);
        // Adjust factory parameters
        factory.setMaxCoordGap(constants.maxCoordGap);
        factory.setMaxExpansionSpace(constants.maxExpansionSpace);
        factory.setMaxFilamentThickness(constants.maxFilamentThickness);
        factory.setMaxGapSlope(constants.maxGapSlope.getValue());
        factory.setMaxInvolvingLength(constants.maxInvolvingLength);
        factory.setMaxOverlapDeltaPos(constants.maxOverlapDeltaPos);
        factory.setMaxPosGap(constants.maxPosGap);
        factory.setMaxPosGapForSlope(constants.maxPosGapForSlope);
        factory.setMaxSectionThickness(constants.maxSectionThickness);
        factory.setMaxSpace(constants.maxSpace);
        factory.setMinCoreSectionLength(constants.minCoreSectionLength);
        factory.setMinSectionAspect(constants.minSectionAspect.getValue());

        ///factory.dump();

        // Reset the "fat" attribute of the sections, since this depends on
        // max thickness parameters
        for (Section section : sections) {
            section.resetFat();
            section.setGlyph(null);
        }

        return factory.retrieveFilaments(sections, true);
    }

    //------------//
    // lookupLine //
    //------------//
    /**
     * Look up for ledgers on a specific line above or below the
     * provided staff line.
     *
     * @param index     index of line, above if positive, below if negative
     * @param staffLine the staff line used as reference
     * @return the number of ledgers found on this "virtual line"
     */
    private int lookupLine (int index,
                            LineInfo staffLine)
    {
        logger.debug("Checking line {}", index);

        int ledgerMarginX = scale.toPixels(constants.ledgerMarginX);
        int found = 0; // Number of ledgers found on this line

        // Define bounds for the virtual line, properly shifted and enlarged
        Rectangle box = staffLine.getBounds();
        box.y += (index * scale.getInterline());
        box.grow(0, scale.toPixels(constants.ledgerMarginY));

        // Filter enclosed candidates
        for (GlyphContext context : ledgerCandidates) {
            // Rough containment
            if (!box.contains(context.mid)) {
                continue;
            }

            // Check precise distance
            double dist = Math.abs(index - context.dist);
            logger.debug("{} {}", dist, context);

            if (dist > constants.ledgerMarginY.getValue()) {
                if (context.stick.isVip()) {
                    logger.info("Ledger candidate {} dy:{} vs {}",
                            context, dist, constants.ledgerMarginY.getValue());
                }
                continue;
            }

            // Check for presence of ledger on previous line
            int prevIndex = (index > 0) ? (index - 1) : (index + 1);

            if (prevIndex != 0) {
                Set<Glyph> prevLedgers = context.staff.getLedgers(prevIndex);

                // Check abscissa compatibility
                if (prevLedgers == null) {
                    if (context.stick.isVip()) {
                        logger.info("Ledger candidate {} orphan", context);
                    }
                    continue;
                }

                boolean foundPrevious = false;

                for (Glyph ledger : prevLedgers) {
                    Point mid = ledger.getAreaCenter();
                    double dx = Math.abs(mid.x - context.mid.getX());

                    if (dx <= ledgerMarginX) {
                        foundPrevious = true;

                        break;
                    }
                }

                if (!foundPrevious) {
                    if (context.stick.isVip()) {
                        logger.info("Ledger candidate {} local orphan", context);
                    }
                    continue;
                }

                //            } else if (context.stick.getLength(HORIZONTAL) < minFullLedgerLength) {
                //                // If ledger is short, check for presence of stem
                //                // This test discards ledgers of whole notes!
                //                Rectangle stickBox = context.stick.getBounds();
                //                stickBox.grow(maxStemDx, maxStemDy);
                //
                //                List<Glyph> others = system.lookupIntersectedGlyphs(stickBox);
                //                boolean     foundStem = false;
                //
                //                for (Glyph glyph : others) {
                //                    if (glyph.isStem()) {
                //                        foundStem = true;
                //
                //                        break;
                //                    }
                //                }
                //
                //                if (!foundStem) {
                //                    continue;
                //                }
            }

            // OK!
            Glyph glyph = system.addGlyph(context.stick);
//            Ledger ledger = new Ledger(glyph, context.staff, index);
//            glyph.setTranslation(ledger);
            glyph.setShape(Shape.LEDGER);
            context.staff.addLedger(glyph, index);
            found++;

            if (context.stick.isVip()) {
                logger.info("Ledger at {}", context);
            } else {
                logger.debug("Ledger at {}", context);
            }
        }

        return found;
    }

    //~ Inner Classes ----------------------------------------------------------
    //    //------------//
    //    // ChunkCheck //
    //    //------------//
    //    /**
    //     * Class {@code ChunkCheck} checks for absence of a chunk either at
    //     * start or stop
    //     */
    //    private class ChunkCheck
    //        extends Check<Glyph>
    //    {
    //        //~ Instance fields ----------------------------------------------------
    //
    //        // Half width for chunk window at top and bottom
    //        private final int    nWidth;
    //
    //        // Half height for chunk window at top and bottom
    //        private final int    nHeight;
    //
    //        // Total area for chunk window
    //        private final double area;
    //
    //        //~ Constructors -------------------------------------------------------
    //
    //        protected ChunkCheck ()
    //        {
    //            super(
    //                "Chunk",
    //                "Check no chunk is stuck on either side of the stick",
    //                constants.chunkRatioLow,
    //                constants.chunkRatioHigh,
    //                false,
    //                BI_CHUNK);
    //
    //            // Adjust chunk window according to system scale
    //            nWidth = scale.toPixels(constants.chunkWidth);
    //            nHeight = scale.toPixels(constants.chunkHeight);
    //            area = 2 * nWidth * nHeight;
    //
    //            if (logger.isDebugEnabled()) {
    //                logger.debug(
    //                    "MaxPixLow=" + getLow() + ", MaxPixHigh=" + getHigh());
    //            }
    //        }
    //
    //        //~ Methods ------------------------------------------------------------
    //
    //        protected double getValue (Glyph stick)
    //        {
    //            // Retrieve the smallest stick chunk either at top or bottom
    //            double res = Math.min(
    //                getAliensAtStart(stick, nHeight, nWidth),
    //                getAliensAtStop(stick, nHeight, nWidth));
    //            res /= area;
    //
    //            if (logger.isDebugEnabled()) {
    //                logger.info("MinAliensRatio= " + res + " for " + stick);
    //            }
    //
    //            return res;
    //        }
    //    }
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        // For factory
        Constant.Double maxGapSlope = new Constant.Double(
                "tangent",
                0.5,
                "Maximum absolute slope for a gap");

        Constant.Ratio minSectionAspect = new Constant.Ratio(
                1.0,
                "Minimum section aspect (length / thickness)");

        Constant.Ratio maxConsistentRatio = new Constant.Ratio(
                1.7,
                "Maximum thickness ratio for consistent merge");

        // Constants specified WRT mean line thickness
        // -------------------------------------------
        Scale.LineFraction maxSectionThickness = new Scale.LineFraction(
                1.85,
                "Maximum horizontal section thickness WRT mean line height");

        Scale.LineFraction maxFilamentThickness = new Scale.LineFraction(
                1.75,
                "Maximum filament thickness WRT mean line height");

        Scale.LineFraction maxOverlapDeltaPos = new Scale.LineFraction(
                1.0,
                "Maximum delta position between two overlapping filaments");

        // Constants specified WRT mean interline
        // --------------------------------------
        Scale.Fraction minCoreSectionLength = new Scale.Fraction(
                0.25,
                "Minimum length for a section to be considered as core");

        Scale.Fraction maxCoordGap = new Scale.Fraction(
                0,
                "Maximum delta coordinate for a gap between filaments");

        Scale.Fraction maxPosGap = new Scale.Fraction(
                0.2,
                "Maximum delta position for a gap between filaments");

        Scale.Fraction maxSpace = new Scale.Fraction(
                0.0,
                "Maximum space between overlapping filaments");

        Scale.Fraction maxExpansionSpace = new Scale.Fraction(
                0.02,
                "Maximum space when expanding filaments");

        Scale.Fraction maxPosGapForSlope = new Scale.Fraction(
                0.1,
                "Maximum delta Y to check slope for a gap between filaments");

        Scale.Fraction maxInvolvingLength = new Scale.Fraction(
                2,
                "Maximum filament length to apply thickness test");

        ///
        Scale.Fraction ledgerMarginY = new Scale.Fraction(
                0.4,
                "Margin on ledger ordinate");

        Scale.Fraction ledgerMarginX = new Scale.Fraction(
                2.0,
                "Margin on ledger abscissa across lines");

        Scale.Fraction maxStemDx = new Scale.Fraction(
                1.5,
                "Maximum horizontal distance between ledger and stem");

        Scale.Fraction maxStemDy = new Scale.Fraction(
                0.5,
                "Maximum vertical distance between ledger and stem");

        Scale.Fraction minFullLedgerLength = new Scale.Fraction(
                1.5,
                "Minimum length for a full ledger with no stem");

        ///
        Scale.Fraction minCoreLength = new Scale.Fraction(
                0.2,
                "Minimum length for ledger core");

        //        Scale.Fraction     chunkHeight = new Scale.Fraction(
        //            0.33,
        //            "Height of half area to look for chunks");
        //        Constant.Ratio     chunkRatioHigh = new Constant.Ratio(
        //            0.05,
        //            "HighMaximum ratio of alien pixels to detect chunks");
        //        Constant.Ratio     chunkRatioLow = new Constant.Ratio(
        //            0.02,
        //            "LowMaximum ratio of alien pixels to detect chunks");
        //        Scale.Fraction     chunkWidth = new Scale.Fraction(
        //            0.33,
        //            "Width of half area to look for chunks");
        Scale.Fraction extensionMinPointNb = new Scale.Fraction(
                0.2,
                "Minimum number of points to compute extension of crossing objects");

        Constant.Ratio maxAdjacencyHigh = new Constant.Ratio(
                0.3,
                "High Maximum adjacency ratio for an ending");

        Constant.Ratio maxAdjacencyLow = new Constant.Ratio(
                0.2,
                "Low Maximum adjacency ratio for an ending");

        Scale.Fraction maxLengthHigh = new Scale.Fraction(
                3.1,
                "High Maximum length for a horizontal");

        Scale.Fraction maxLengthLow = new Scale.Fraction(
                2.2,
                "Low Maximum length for a horizontal");

        Scale.Fraction maxStaffDistanceHigh = new Scale.Fraction(
                6,
                "High Maximum staff distance for a horizontal");

        Scale.Fraction maxStaffDistanceLow = new Scale.Fraction(
                4,
                "Low Maximum staff distance for a horizontal");

        Scale.LineFraction maxThicknessHigh = new Scale.LineFraction(
                1.2,
                "High Maximum thickness of an interesting stick");

        Scale.LineFraction maxThicknessLow = new Scale.LineFraction(
                1.0,
                "Low Maximum thickness of an interesting stick");

        Check.Grade minCheckResult = new Check.Grade(
                0.50,
                "Minimum result for suite of check");

        Constant.Ratio minDensityHigh = new Constant.Ratio(
                0.7,
                "High Minimum density for a horizontal");

        Constant.Ratio minDensityLow = new Constant.Ratio(
                0.6,
                "Low Minimum density for a horizontal");

        Scale.Fraction minEndingLengthHigh = new Scale.Fraction(
                15,
                "High Minimum length for an ending");

        Scale.Fraction minEndingLengthLow = new Scale.Fraction(
                10,
                "Low Minimum length for an ending");

        Scale.Fraction minLedgerLengthHigh = new Scale.Fraction(
                0.3,
                "High Minimum length for a ledger");

        Scale.Fraction minLedgerLengthLow = new Scale.Fraction(
                0.2,
                "Low Minimum length for a ledger");

        Scale.Fraction minStaffDistanceHigh = new Scale.Fraction(
                0.8,
                "High Minimum staff distance for a horizontal");

        Scale.Fraction minStaffDistanceLow = new Scale.Fraction(
                0.6,
                "Low Minimum staff distance for a horizontal");

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

        //
        Constant.Double maxSlopeForCheck = new Constant.Double(
                "slope",
                1.5,
                "Maximum slope for checking a ledger candidate");

    }

    //---------------------//
    // FirstAdjacencyCheck //
    //---------------------//
    private static class FirstAdjacencyCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected FirstAdjacencyCheck ()
        {
            super(
                    "TopAdj",
                    "Check that stick is open on top side",
                    constants.maxAdjacencyLow,
                    constants.maxAdjacencyHigh,
                    false,
                    TOO_ADJA);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the adjacency value
        @Override
        protected double getValue (Glyph stick)
        {
            int length = stick.getLength(HORIZONTAL);

            return (double) stick.getFirstStuck() / (double) length;
        }
    }

    //--------------------//
    // LastAdjacencyCheck //
    //--------------------//
    private static class LastAdjacencyCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected LastAdjacencyCheck ()
        {
            super(
                    "BottomAdj",
                    "Check that stick is open on bottom side",
                    constants.maxAdjacencyLow,
                    constants.maxAdjacencyHigh,
                    false,
                    TOO_ADJA);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the adjacency value
        @Override
        protected double getValue (Glyph stick)
        {
            int length = stick.getLength(HORIZONTAL);

            return (double) stick.getLastStuck() / (double) length;
        }
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
                    "MinDensity",
                    "Check that stick fills its bounding rectangle",
                    constants.minDensityLow,
                    constants.minDensityHigh,
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
    private class GlyphContext
            implements Checkable
    {
        //~ Instance fields ----------------------------------------------------

        /** The stick being checked */
        final Glyph stick;

        /** Stick middle point */
        final Point2D mid;

        /** Nearest staff */
        final StaffInfo staff;

        /** Absolute normalized distance from staff */
        final double absDist;

        /** Algebraic normalized distance from closest staff line */
        final double dist;

        //~ Constructors -------------------------------------------------------
        public GlyphContext (Glyph stick)
        {
            this.stick = stick;

            mid = new Point2D.Double(
                    (stick.getStartPoint(HORIZONTAL).getX() + stick.
                    getStopPoint(
                    HORIZONTAL).getX()) / 2,
                    (stick.getStartPoint(HORIZONTAL).getY() + stick.
                    getStopPoint(
                    HORIZONTAL).getY()) / 2);
            staff = system.getStaffAt(mid);

            double toTop = scale.pixelsToFrac(
                    staff.getFirstLine().yAt(mid.getX()) - mid.getY());
            double fromBottom = scale.pixelsToFrac(
                    mid.getY() - staff.getLastLine().yAt(mid.getX()));

            if (toTop > fromBottom) {
                absDist = toTop;
                dist = -toTop;
            } else {
                absDist = fromBottom;
                dist = fromBottom;
            }
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
                        if (Math.abs(glyph.getSlope()) <= constants.maxSlopeForCheck.
                                getValue()) {
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

    //--------------//
    // MaxDistCheck //
    //--------------//
    private class MaxDistCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected MaxDistCheck ()
        {
            super(
                    "MaxDist",
                    "Check that stick is not too far from staff",
                    constants.maxStaffDistanceLow,
                    constants.maxStaffDistanceHigh,
                    false,
                    TOO_FAR);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the position with respect to the various staves of the
        // system being checked.
        @Override
        protected double getValue (Glyph stick)
        {
            return staffDistance(stick);
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
            return sheet.getScale().pixelsToFrac(stick.getLength(HORIZONTAL));
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
            return sheet.getScale().pixelsToFrac(stick.getThickness(HORIZONTAL));
        }
    }

    //--------------//
    // MinDistCheck //
    //--------------//
    private class MinDistCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected MinDistCheck ()
        {
            super(
                    "MinDist",
                    "Check that stick is not within staff height",
                    constants.minStaffDistanceLow,
                    constants.minStaffDistanceHigh,
                    true,
                    IN_STAFF);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the position with respect to the various staves of the
        // system being checked.
        @Override
        protected double getValue (Glyph stick)
        {
            return staffDistance(stick);
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
            return sheet.getScale().pixelsToFrac(stick.getLength(HORIZONTAL));
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
            return sheet.getScale().pixelsToFrac(stick.getThickness(HORIZONTAL));
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
