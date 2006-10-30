//----------------------------------------------------------------------------//
//                                                                            //
//                    H o r i z o n t a l s B u i l d e r                     //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.Main;
import omr.ProcessingException;

import omr.check.Check;
import omr.check.CheckBoard;
import omr.check.CheckSuite;
import omr.check.FailureResult;
import omr.check.SuccessResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphModel;
import omr.glyph.GlyphSection;
import omr.glyph.Shape;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphLagView;

import omr.lag.RunBoard;
import omr.lag.ScrollLagView;
import omr.lag.SectionBoard;

import omr.score.visitor.SheetPainter;

import omr.selection.Selection;
import omr.selection.SelectionTag;
import static omr.selection.SelectionTag.*;

import omr.stick.Stick;
import omr.stick.StickUtil;

import omr.ui.BoardsPane;
import omr.ui.PixelBoard;
import omr.ui.ToggleHandler;
import static omr.ui.field.SpinnerUtilities.*;
import omr.ui.view.Zoom;

import omr.util.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>HorizontalsBuilder</code> is in charge of retrieving horizontal
 * dashes in the given sheet.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class HorizontalsBuilder
    extends GlyphModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        HorizontalsBuilder.class);

    /** Success codes */
    private static final SuccessResult LEDGER = new SuccessResult("Ledger");
    private static final SuccessResult   ENDING = new SuccessResult("Ending");

    /** Failure codes */
    private static final FailureResult TOO_SHORT = new FailureResult(
        "Hori-TooShort");
    private static final FailureResult   TOO_LONG = new FailureResult(
        "Hori-TooLong");
    private static final FailureResult   TOO_THIN = new FailureResult(
        "Hori-TooThin");
    private static final FailureResult   TOO_THICK = new FailureResult(
        "Hori-TooThick");
    private static final FailureResult   TOO_FAT = new FailureResult(
        "Hori-TooFat");
    private static final FailureResult   TOO_HOLLOW = new FailureResult(
        "Hori-TooHollow");
    private static final FailureResult   IN_STAFF = new FailureResult(
        "Hori-InStaff");
    private static final FailureResult   TOO_FAR = new FailureResult(
        "Hori-TooFar");
    private static final FailureResult   TOO_ADJA = new FailureResult(
        "Hori-TooHighAdjacency");
    private static final FailureResult   BI_CHUNK = new FailureResult(
        "Hori-BiChunk");

    //~ Instance fields --------------------------------------------------------

    /** Check suite for common tests */
    private CheckSuite<Stick> commonSuite;

    /** Check suite for Additional tests for endings */
    private CheckSuite<Stick> endingSuite;

    /** Check suite for Additional tests for ledgers */
    private CheckSuite<Stick> ledgerSuite;

    /** Total check suite for ending */
    private ArrayList<CheckSuite<Stick>> endingList;

    /** Total check suite for ledger */
    private ArrayList<CheckSuite<Stick>> ledgerList;

    /** The related view if any */
    private GlyphLagView lagView;

    /** Horizontals area, with retrieved horizontal sticks */
    private HorizontalArea horizontalsArea;

    /** The whole list of horizontals (ledgers, legato signs, endings) found */
    private final Horizontals info;

    /** The collection of all horizontal items */
    private final List<Dash> allDashes = new ArrayList<Dash>();

    /** The containing sheet */
    private Sheet sheet;

    //~ Constructors -----------------------------------------------------------

    //--------------------//
    // HorizontalsBuilder //
    //--------------------//
    /**
     * @param sheet the related sheet
     */
    public HorizontalsBuilder (Sheet sheet)
    {
        // Reuse the horizontal lag of runs (from staff lines)
        super(sheet, sheet.getHorizontalLag());

        this.sheet = sheet;
        info = new Horizontals();
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Run the Horizontals step, searching all horizontal sticks for typical
     * things like ledgers, endings and legato signs.
     *
     * @return the built Horizontals info
     * @throws ProcessingException raised is process gets stopped
     */
    public Horizontals buildInfo ()
        throws ProcessingException
    {
        // Purge small sections
        Scale scale = sheet.getScale();
        ///lag.purgeTinySections(scale.fracToSquarePixels(constants.minForeWeight));

        // Retrieve (horizontal) sticks
        horizontalsArea = new HorizontalArea(
            sheet,
            lag,
            scale.toPixels(constants.maxThicknessHigh) - 1);

        // Recognize horizontals -> ledgers, endings
        retrieveHorizontals();

        // Cleanup the ledgers (and the endings)
        cleanup(info.getLedgers());
        cleanup(info.getEndings());

        // Display the resulting rubber is so asked for
        if (constants.displayFrame.getValue() && (Main.getJui() != null)) {
            displayFrame();
        }

        logger.info(
            info.getLedgers().size() + " ledger(s), " +
            info.getEndings().size() + " ending(s)");

        return info;
    }

    //---------//
    // cleanup //
    //---------//
    private void cleanup (List<?extends Dash> dashes)
    {
        for (Dash dash : dashes) {
            StickUtil.cleanup(
                dash.getStick(),
                lag,
                constants.extensionMinPointNb.getValue(),
                sheet.getPicture());
        }
    }

    //--------------//
    // createSuites //
    //--------------//
    private void createSuites ()
    {
        // Common horizontal suite
        commonSuite = new CheckSuite<Stick>(
            "Common",
            constants.minCheckResult.getValue());
        commonSuite.add(1, new MinThicknessCheck()); // Minimum thickness
        commonSuite.add(1, new MaxThicknessCheck());
        commonSuite.add(1, new MinDistCheck()); // Not within staves
        commonSuite.add(1, new MaxDistCheck()); // Not too far from staves

        // ledgerSuite
        ledgerSuite = new CheckSuite<Stick>(
            "Ledger",
            constants.minCheckResult.getValue());
        ledgerSuite.add(
            1,
            new MinLengthCheck(
                constants.minLedgerLengthLow.getValue(),
                constants.minLedgerLengthHigh.getValue())); // Minimum length
        ledgerSuite.add(1, new MaxLengthCheck()); // Maximum length
        ledgerSuite.add(1, new MinDensityCheck());
        ledgerSuite.add(1, new ChunkCheck()); // At least one edge WITHOUT a chunk

        // Ledger collection
        ledgerList = new ArrayList<CheckSuite<Stick>>();
        ledgerList.add(commonSuite);
        ledgerList.add(ledgerSuite);

        // endingSuite
        endingSuite = new CheckSuite<Stick>(
            "Ending",
            constants.minCheckResult.getValue());
        endingSuite.add(
            1,
            new MinLengthCheck(
                constants.minEndingLengthLow.getValue(),
                constants.minEndingLengthHigh.getValue())); // Minimum length
        endingSuite.add(1, new FirstAdjacencyCheck());
        endingSuite.add(1, new LastAdjacencyCheck());

        // Ending collection
        endingList = new ArrayList<CheckSuite<Stick>>();
        endingList.add(commonSuite);
        endingList.add(endingSuite);

        if (logger.isFineEnabled()) {
            commonSuite.dump();
            ledgerSuite.dump();
            endingSuite.dump();
        }
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        // Sections that, as members of horizontals, will be treated as specific
        List<GlyphSection> members = new ArrayList<GlyphSection>();

        for (Dash dash : allDashes) {
            members.addAll(dash.getStick().getMembers());
        }

        // Specific rubber display
        lagView = new MyView(lag, members);

        final String  unit = "HorizontalsBuilder";
        BoardsPane    boardsPane = new BoardsPane(
            sheet,
            lagView,
            new PixelBoard(unit),
            new RunBoard(unit, sheet.getSelection(HORIZONTAL_RUN)),
            new SectionBoard(
                unit,
                lag.getLastVertexId(),
                sheet.getSelection(HORIZONTAL_SECTION),
                sheet.getSelection(HORIZONTAL_SECTION_ID)),
            new GlyphBoard(
                unit,
                this,
                sheet.getSelection(HORIZONTAL_GLYPH),
                sheet.getSelection(HORIZONTAL_GLYPH_ID),
                null),
            new CheckBoard<Stick>(
                unit + "-Common",
                commonSuite,
                sheet.getSelection(HORIZONTAL_GLYPH)),
            new CheckBoard<Stick>(
                unit + "-Ledger",
                ledgerSuite,
                sheet.getSelection(HORIZONTAL_GLYPH)),
            new CheckBoard<Stick>(
                unit + "-Ending",
                endingSuite,
                sheet.getSelection(HORIZONTAL_GLYPH)));

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(lagView);
        sheet.getAssembly()
             .addViewTab("Horizontals", slv, boardsPane);
        slv.getComponent()
           .addAncestorListener(
            new ToggleHandler(
                "Horizontals",
                lagView,
                "Toggle between before & after horizontal cleanup"));
    }

    //---------------------//
    // retrieveHorizontals //
    //---------------------//
    private void retrieveHorizontals ()
    {
        // Define the suites of Checks
        double minResult = constants.minCheckResult.getValue();

        // Create suites and collections
        createSuites();

        for (Stick stick : horizontalsArea.getSticks()) {
            if (logger.isFineEnabled()) {
                logger.fine("Checking " + stick);
            }

            // Run the Ledger Checks
            if (CheckSuite.passCollection(stick, ledgerList) >= minResult) {
                stick.setResult(LEDGER);
                stick.setShape(Shape.LEDGER);
                info.getLedgers()
                    .add(new Ledger(stick));
            } else {
                // Then, if failed, the Ending Checks
                if (CheckSuite.passCollection(stick, endingList) >= minResult) {
                    stick.setResult(ENDING);
                    stick.setShape(Shape.ENDING_HORIZONTAL);
                    info.getEndings()
                        .add(new Ending(stick));
                }
            }
        }

        // Update lists
        allDashes.addAll(info.getLedgers());
        allDashes.addAll(info.getEndings());

        if (logger.isFineEnabled()) {
            logger.fine(
                "Found " + info.getLedgers().size() + " ledgers and " +
                info.getEndings().size() + " endings");
        }
    }

    //---------------//
    // staffDistance //
    //---------------//
    private static double staffDistance (Sheet sheet,
                                         Stick stick)
    {
        // Compute the (algebraic) distance from the stick to the nearest
        // staff. Distance is negative if the stick is within the staff,
        // positive outside.
        final int y = stick.getMidPos();
        final int x = (stick.getStart() + stick.getStop()) / 2;
        final int idx = sheet.getStaffIndexAtY(y);
        StaffInfo area = sheet.getStaves()
                              .get(idx);
        final int top = area.getFirstLine()
                            .getLine()
                            .yAt(x);
        final int bottom = area.getLastLine()
                               .getLine()
                               .yAt(x);
        final int dist = Math.max(top - y, y - bottom);

        return sheet.getScale()
                    .pixelsToFrac(dist);
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // ChunkCheck //
    //------------//
    /**
     * Class <code>ChunkCheck</code> checks for absence of a chunk either at
     * start or stop
     */
    private class ChunkCheck
        extends Check<Stick>
    {
        private final int nHeight;

        // Half-dimensions for window at top and bottom, checking for chunks
        private final int nWidth;

        protected ChunkCheck ()
        {
            super(
                "Chunk",
                "Check not chunk is stuck on either side of the stick" +
                " (unit is interline squared)",
                0,
                0,
                false,
                BI_CHUNK);

            // Adjust chunk window according to system scale (problem, we have
            // sheet scale and staff scale, not system scale...)
            Scale scale = sheet.getScale();
            nWidth = scale.toPixels(constants.chunkWidth);
            nHeight = scale.toPixels(constants.chunkHeight);

            int area = 4 * nWidth * nHeight;
            setLowHigh(
                area * constants.chunkRatioLow.getValue(),
                area * constants.chunkRatioHigh.getValue());

            if (logger.isFineEnabled()) {
                logger.fine(
                    "MaxPixLow=" + getLow() + ", MaxPixHigh=" + getHigh());
            }
        }

        protected double getValue (Stick stick)
        {
            // Retrieve the smallest stick chunk either at top or bottom
            int res = Math.min(
                stick.getAliensAtStart(nHeight, nWidth),
                stick.getAliensAtStop(nHeight, nWidth));

            if (logger.isFineEnabled()) {
                logger.fine("MaxAliens= " + res + " for " + stick);
            }

            return res;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        Scale.Fraction   chunkHeight = new Scale.Fraction(
            0.33,
            "Height of half area to look for chunks");
        Constant.Double  chunkRatioHigh = new Constant.Double(
            0.25,
            "HighMaximum ratio of alien pixels to detect chunks");
        Constant.Double  chunkRatioLow = new Constant.Double(
            0.25,
            "LowMaximum ratio of alien pixels to detect chunks");
        Scale.Fraction   chunkWidth = new Scale.Fraction(
            0.33,
            "Width of half area to look for chunks");
        Constant.Boolean displayFrame = new Constant.Boolean(
            false,
            "Should we display a frame on the horizontal sticks");
        Constant.Integer extensionMinPointNb = new Constant.Integer(
            4,
            "Minimum number of points to compute extension of" +
            " crossing objects during cleanup");
        Constant.Double  maxAdjacencyHigh = new Constant.Double(
            0.70,
            "High Maximum adjacency ratio for an ending");
        Constant.Double  maxAdjacencyLow = new Constant.Double(
            0.60,
            "Low Maximum adjacency ratio for an ending");
        Scale.Fraction   maxLengthHigh = new Scale.Fraction(
            3.5,
            "High Maximum length for a horizontal");
        Scale.Fraction   maxLengthLow = new Scale.Fraction(
            2.5,
            "Low Maximum length for a horizontal");
        Scale.Fraction   maxStaffDistanceHigh = new Scale.Fraction(
            7,
            "High Maximum staff distance for a horizontal");
        Scale.Fraction   maxStaffDistanceLow = new Scale.Fraction(
            5,
            "Low Maximum staff distance for a horizontal");
        Scale.Fraction   maxThicknessHigh = new Scale.Fraction(
            0.3,
            " High Maximum thickness of an interesting stick");
        Scale.Fraction   maxThicknessLow = new Scale.Fraction(
            0.3,
            " Low Maximum thickness of an interesting stick");
        Constant.Double  minCheckResult = new Constant.Double(
            0.50,
            "Minimum result for suite of check");
        Constant.Double  minDensityHigh = new Constant.Double(
            0.9,
            "High Minimum density for a horizontal");
        Constant.Double  minDensityLow = new Constant.Double(
            0.8,
            "Low Minimum density for a horizontal");
        Scale.Fraction   minEndingLengthHigh = new Scale.Fraction(
            10,
            "High Minimum length for an ending");
        Scale.Fraction   minEndingLengthLow = new Scale.Fraction(
            5,
            "Low Minimum length for an ending");
        Scale.Fraction   minLedgerLengthHigh = new Scale.Fraction(
            3.5,
            "High Minimum length for a ledger");
        Scale.Fraction   minLedgerLengthLow = new Scale.Fraction(
            2.5,
            "Low Minimum length for a ledger");
        Scale.Fraction   minStaffDistanceHigh = new Scale.Fraction(
            0.8,
            "High Minimum staff distance for a horizontal");
        Scale.Fraction   minStaffDistanceLow = new Scale.Fraction(
            0.6,
            "Low Minimum staff distance for a horizontal");
        Scale.Fraction   minThicknessHigh = new Scale.Fraction(
            0.3,
            " High Minimum thickness of an interesting stick");
        Scale.Fraction   minThicknessLow = new Scale.Fraction(
            0.3,
            " Low Minimum thickness of an interesting stick");
    }

    //---------------------//
    // FirstAdjacencyCheck //
    //---------------------//
    private static class FirstAdjacencyCheck
        extends Check<Stick>
    {
        protected FirstAdjacencyCheck ()
        {
            super(
                "TopAdj",
                "Check that stick is open on top side (dimension-less)",
                constants.maxAdjacencyLow.getValue(),
                constants.maxAdjacencyHigh.getValue(),
                false,
                TOO_ADJA);
        }

        // Retrieve the adjacency value
        protected double getValue (Stick stick)
        {
            int length = stick.getLength();

            return (double) stick.getFirstStuck() / (double) length;
        }
    }

    //--------------------//
    // LastAdjacencyCheck //
    //--------------------//
    private static class LastAdjacencyCheck
        extends Check<Stick>
    {
        protected LastAdjacencyCheck ()
        {
            super(
                "BottomAdj",
                "Check that stick is open on bottom side" +
                " (dimension-less)",
                constants.maxAdjacencyLow.getValue(),
                constants.maxAdjacencyHigh.getValue(),
                false,
                TOO_ADJA);
        }

        // Retrieve the adjacency value
        protected double getValue (Stick stick)
        {
            int length = stick.getLength();

            return (double) stick.getLastStuck() / (double) length;
        }
    }

    //--------------//
    // MaxDistCheck //
    //--------------//
    private class MaxDistCheck
        extends Check<Stick>
    {
        protected MaxDistCheck ()
        {
            super(
                "MaxDist",
                "Check that stick is not too far from staff" +
                " (unit is interline)",
                0,
                0,
                false,
                TOO_FAR);
            setLowHigh(
                constants.maxStaffDistanceLow.getValue(),
                constants.maxStaffDistanceHigh.getValue());
        }

        // Retrieve the position with respect to the various staves of the
        // system being checked.
        protected double getValue (Stick stick)
        {
            return staffDistance(sheet, stick);
        }
    }

    //----------------//
    // MaxLengthCheck //
    //----------------//
    private class MaxLengthCheck
        extends Check<Stick>
    {
        protected MaxLengthCheck ()
        {
            super(
                "MaxLength",
                "Check that stick is not too long (unit is interline)",
                constants.maxLengthLow.getValue(),
                constants.maxLengthHigh.getValue(),
                false,
                TOO_LONG);
        }

        // Retrieve the length data
        protected double getValue (Stick stick)
        {
            return sheet.getScale()
                        .pixelsToFrac(stick.getLength());
        }
    }

    //-------------------//
    // MaxThicknessCheck //
    //-------------------//
    private class MaxThicknessCheck
        extends Check<Stick>
    {
        protected MaxThicknessCheck ()
        {
            super(
                "MaxThickness",
                "Check that stick is not too thick (unit is interline)",
                constants.maxThicknessLow.getValue(),
                constants.maxThicknessHigh.getValue(),
                false,
                TOO_THICK);
        }

        // Retrieve the thickness data
        protected double getValue (Stick stick)
        {
            return sheet.getScale()
                        .pixelsToFrac(stick.getThickness());
        }
    }

    //-----------------//
    // MinDensityCheck //
    //-----------------//
    private static class MinDensityCheck
        extends Check<Stick>
    {
        protected MinDensityCheck ()
        {
            super(
                "MinDensity",
                "Check that stick fills its bounding rectangle" +
                " (dimension-less)",
                constants.minDensityLow.getValue(),
                constants.minDensityHigh.getValue(),
                true,
                TOO_HOLLOW);
        }

        // Retrieve the density
        protected double getValue (Stick stick)
        {
            Rectangle rect = stick.getBounds();
            double    area = rect.width * rect.height;

            return (double) stick.getWeight() / area;
        }
    }

    //--------------//
    // MinDistCheck //
    //--------------//
    private class MinDistCheck
        extends Check<Stick>
    {
        protected MinDistCheck ()
        {
            super(
                "MinDist",
                "Check that stick is not within staff height" +
                " (unit is interline)",
                0,
                0,
                true,
                IN_STAFF);
            setLowHigh(
                constants.minStaffDistanceLow.getValue(),
                constants.minStaffDistanceHigh.getValue());
        }

        // Retrieve the position with respect to the various staves of the
        // system being checked.
        protected double getValue (Stick stick)
        {
            return staffDistance(sheet, stick);
        }
    }

    //----------------//
    // MinLengthCheck //
    //----------------//
    private class MinLengthCheck
        extends Check<Stick>
    {
        protected MinLengthCheck (double low,
                                  double high)
        {
            super(
                "MinLength",
                "Check that stick is long enough (unit is interline)",
                low,
                high,
                true,
                TOO_SHORT);
        }

        // Retrieve the length data
        protected double getValue (Stick stick)
        {
            return sheet.getScale()
                        .pixelsToFrac(stick.getLength());
        }
    }

    //-------------------//
    // MinThicknessCheck //
    //-------------------//
    private class MinThicknessCheck
        extends Check<Stick>
    {
        protected MinThicknessCheck ()
        {
            super(
                "MinThickness",
                "Check that stick is thick enough (unit is interline)",
                constants.minThicknessLow.getValue(),
                constants.minThicknessHigh.getValue(),
                true,
                TOO_THIN);
        }

        // Retrieve the thickness data
        protected double getValue (Stick stick)
        {
            return sheet.getScale()
                        .pixelsToFrac(stick.getThickness());
        }
    }

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends GlyphLagView
    {
        public MyView (GlyphLag           lag,
                       List<GlyphSection> members)
        {
            super(lag, members, HorizontalsBuilder.this);
            setName("HorizontalsBuilder-View");

            setLocationSelection(sheet.getSelection(SelectionTag.PIXEL));

            setSpecificSelections(
                sheet.getSelection(SelectionTag.HORIZONTAL_RUN),
                sheet.getSelection(SelectionTag.HORIZONTAL_SECTION));

            Selection glyphSelection = sheet.getSelection(
                SelectionTag.HORIZONTAL_GLYPH);
            setGlyphSelection(glyphSelection);
            glyphSelection.addObserver(this);
        }

        //----------//
        // colorize //
        //----------//
        @Override
        public void colorize ()
        {
            super.colorize();

            final int viewIndex = lag.getViews()
                                     .indexOf(this);

            // All checked sticks. Perhaps, recognized sticks should no longer
            // be part of the list, since they'll be colorized a few lines
            // below... TBD
            for (Stick stick : horizontalsArea.getSticks()) {
                if ((stick.getResult() != LEDGER) &&
                    (stick.getResult() != ENDING)) {
                    stick.colorize(lag, viewIndex, Color.red);
                }
            }

            // Use light gray color for past successful entities
            // If relevant to the current lag...
            sheet.colorize(lag, viewIndex, Color.lightGray);
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        public void renderItems (Graphics g)
        {
            Zoom z = getZoom();

            // Render all physical info known so far (staff lines)
            sheet.accept(new SheetPainter(g, z));

            // Render the dashes found
            for (Dash dash : allDashes) {
                dash.render(g, z);
                dash.renderContour(g, z);
            }

            super.renderItems(g);
        }
    }
}
