//----------------------------------------------------------------------------//
//                                                                            //
//                      V e r t i c a l s B u i l d e r                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2007. All rights reserved.                    //
//  This software is released under the GNU General Public License.           //
//  Please contact author at herve.bitteur@laposte.net for bugs & suggestions //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.Main;

import omr.check.Check;
import omr.check.CheckBoard;
import omr.check.CheckSuite;
import omr.check.FailureResult;
import omr.check.SuccessResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphModel;
import omr.glyph.GlyphSection;
import omr.glyph.Shape;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphLagView;

import omr.lag.RunBoard;
import omr.lag.ScrollLagView;
import omr.lag.Section;
import omr.lag.SectionBoard;

import omr.score.visitor.SheetPainter;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionTag;
import static omr.selection.SelectionTag.*;

import omr.step.StepException;

import omr.stick.Stick;

import omr.ui.BoardsPane;
import omr.ui.PixelBoard;
import static omr.ui.field.SpinnerUtilities.*;

import omr.util.Implement;
import omr.util.Logger;
import omr.util.Predicate;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Class <code>VerticalsBuilder</code> is in charge of retrieving all the
 * vertical sticks of all systems in the sheet at hand. Bars are assumed to have
 * been already recognized, so this accounts for stems, vertical edges of
 * endings, and potentially parts of alterations (sharp, natural, flat).
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class VerticalsBuilder
    extends GlyphModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        VerticalsBuilder.class);

    // Success codes
    private static final SuccessResult STEM = new SuccessResult("Stem");

    // Failure codes
    private static final FailureResult TOO_LIMITED = new FailureResult(
        "Stem-TooLimited");
    private static final FailureResult TOO_SHORT = new FailureResult(
        "Stem-TooShort");
    private static final FailureResult TOO_FAT = new FailureResult(
        "Stem-TooFat");
    private static final FailureResult TOO_HIGH_ADJACENCY = new FailureResult(
        "Stem-TooHighAdjacency");
    private static final FailureResult OUTSIDE_SYSTEM = new FailureResult(
        "Stem-OutsideSystem");
    private static final FailureResult TOO_HOLLOW = new FailureResult(
        "Stem-TooHollow");

    //~ Instance fields --------------------------------------------------------

    /** Related user display if any */
    private GlyphLagView view;

    /** Global sheet scale */
    private final Scale scale;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // VerticalsBuilder //
    //------------------//
    /**
     * Creates a new VerticalsBuilder object.
     *
     * @param sheet the related sheet
     *
     * @throws StepException when processing had to stop
     */
    public VerticalsBuilder (Sheet sheet)
    {
        // We work with the sheet vertical lag
        super(sheet, sheet.getVerticalLag());
        scale = sheet.getScale();
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // deassignGlyphShape //
    //--------------------//
    /**
     * This method is limited to deassignment of stems
     *
     * @param glyph the glyph to deassign
     */
    @Override
    public void deassignGlyphShape (Glyph glyph)
    {
        Shape shape = glyph.getShape();

        switch (shape) {
        case COMBINING_STEM :
            sheet.getSymbolsEditor()
                 .deassignGlyphShape(glyph);

            break;

        default :
        }
    }

    //------------------//
    // deassignSetShape //
    //------------------//
    /**
     * This method is limited to deassignment of stems
     *
     * @param glyphs the collection of glyphs to be de-assigned
     */
    @Override
    public void deassignSetShape (Collection<Glyph> glyphs)
    {
        sheet.getSymbolsEditor()
             .deassignSetShape(glyphs);
    }

    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the display, with proper colors for sections
     */
    public void refresh ()
    {
        if ((view == null) && constants.displayFrame.getValue()) {
            displayFrame();
        } else if (view != null) {
            for (Glyph glyph : sheet.getVerticalLag()
                                    .getActiveGlyphs()) {
                view.colorizeGlyph(glyph, null);
            }

            view.repaint();
        }
    }

    //----------------------//
    // retrieveAllVerticals //
    //----------------------//
    public void retrieveAllVerticals ()
        throws StepException
    {
        // Process each system area on turn
        int nb = 0;

        for (SystemInfo system : sheet.getSystems()) {
            nb += retrieveSystemVerticals(system);
        }

        if (constants.displayFrame.getValue() && (Main.getGui() != null)) {
            displayFrame();
        }

        if (nb > 0) {
            logger.info(nb + " stem" + ((nb > 1) ? "s" : "") + " found");
        } else {
            logger.info("No stem found");
        }
    }

    //-------------------------//
    // retrieveSystemVerticals //
    //-------------------------//
    public int retrieveSystemVerticals (SystemInfo system)
        throws StepException
    {
        // Get rid of former symbols
        sheet.getGlyphsBuilder()
             .removeSystemInactives(system);

        // We cannot reuse the sticks, since thick sticks are allowed for bars
        // but not for stems.
        VerticalArea verticalsArea = new VerticalArea(
            system.getVerticalSections(),
            sheet,
            lag,
            new MySectionPredicate(),
            scale.toPixels(constants.maxStemThickness));

        return retrieveVerticals(verticalsArea.getSticks(), system, true);
    }

    //-------------//
    // stemSegment //
    //-------------//
    public void stemSegment (Collection<Glyph> glyphs,
                             SystemInfo        system,
                             boolean           normal)
    {
        // Gather all sections to be browsed
        Collection<GlyphSection> sections = new ArrayList<GlyphSection>();

        for (Glyph glyph : glyphs) {
            sections.addAll(glyph.getMembers());
        }

        if (logger.isFineEnabled()) {
            logger.fine("Sections browsed: " + Section.toString(sections));
        }

        // Retrieve vertical sticks as stem candidates
        try {
            VerticalArea verticalsArea = new VerticalArea(
                sections,
                sheet,
                lag,
                new MySectionPredicate(),
                scale.toPixels(constants.maxStemThickness));

            // Retrieve stems
            int nb = retrieveVerticals(
                verticalsArea.getSticks(),
                system,
                normal);

            if (nb > 0) {
                logger.info(nb + " stem" + ((nb > 1) ? "s" : ""));
            } else {
                logger.info("No stem found");
            }
        } catch (omr.step.StepException ex) {
            logger.warning("stemSegment. Error in retrieving verticals");
        }
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        // Specific rubber display
        view = new MyGlyphLagView(lag);
        view.colorize();

        // Create a hosting frame for the view
        final String unit = "VerticalsBuilder";

        sheet.getAssembly()
             .addViewTab(
            "Verticals",
            new ScrollLagView(view),
            new BoardsPane(
                sheet,
                view,
                new PixelBoard(unit),
                new RunBoard(unit, sheet.getSelection(VERTICAL_RUN)),
                new SectionBoard(
                    unit,
                    lag.getLastVertexId(),
                    sheet.getSelection(VERTICAL_SECTION),
                    sheet.getSelection(VERTICAL_SECTION_ID)),
                new GlyphBoard(
                    unit,
                    this,
                    null, // TO BE CHECKED : specific glyphs
                    sheet.getSelection(VERTICAL_GLYPH),
                    sheet.getSelection(VERTICAL_GLYPH_ID),
                    sheet.getSelection(GLYPH_SET)),
                new MyCheckBoard(unit, sheet.getSelection(VERTICAL_GLYPH))));
    }

    //-------------------//
    // retrieveVerticals //
    //-------------------//
    /**
     * This method retrieve compliant vertical entities (stems) within a
     * collection of vertical sticks, and in the context of a system
     *
     * @param sticks the provided collection of vertical sticks
     * @param system the containing system whose "glyphs" collection will be
     * augmented by the stems found
     * @param normal true for normal stems, false for short stems
     * @return the number of stems found
     */
    private int retrieveVerticals (Collection<Stick> sticks,
                                   SystemInfo        system,
                                   boolean           normal)
        throws StepException
    {
        /** Suite of checks for a stem glyph */
        StemCheckSuite suite = new StemCheckSuite(system, normal);
        double minResult = constants.minCheckResult.getValue();
        int    stemNb = 0;

        if (logger.isFineEnabled()) {
            logger.fine(
                "Searching verticals among " + sticks.size() + " sticks from " +
                Glyph.toString(sticks));
        }

        for (Stick stick : sticks) {
            // Run the various Checks
            double res = suite.pass(stick);

            if (logger.isFineEnabled()) {
                logger.fine("suite=> " + res + " for " + stick);
            }

            if (res >= minResult) {
                stick.setResult(STEM);
                stick.setShape(Shape.COMBINING_STEM);
                stick.setInterline(sheet.getScale().interline());
                stemNb++;
            } else {
                stick.setResult(TOO_LIMITED);
            }

            sheet.getGlyphsBuilder()
                 .insertGlyph(stick, system);
        }

        if (logger.isFineEnabled()) {
            logger.fine("Found " + stemNb + " stems");
        }

        return stemNb;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        Constant.Boolean displayFrame = new Constant.Boolean(
            true,
            "Should we display a frame on the stem sticks");
        Constant.Ratio   maxStemAdjacencyHigh = new Constant.Ratio(
            0.70,
            "High Maximum adjacency ratio for a stem stick");
        Constant.Ratio   maxStemAdjacencyLow = new Constant.Ratio(
            0.60,
            "Low Maximum adjacency ratio for a stem stick");
        Check.Grade      minCheckResult = new Check.Grade(
            0.50,
            "Minimum result for suite of check");
        Constant.Ratio   minDensityHigh = new Constant.Ratio(
            0.9,
            "High Minimum density for a stem");
        Constant.Ratio   minDensityLow = new Constant.Ratio(
            0.8,
            "Low Minimum density for a stem");
        Constant.Ratio   minStemAspectHigh = new Constant.Ratio(
            12.5,
            "High Minimum aspect ratio for a stem stick");
        Constant.Ratio   minStemAspectLow = new Constant.Ratio(
            10.0,
            "Low Minimum aspect ratio for a stem stick");
        Scale.Fraction   minStemLengthHigh = new Scale.Fraction(
            3.5,
            "High Minimum length for a stem");
        Scale.Fraction   minStemLengthLow = new Scale.Fraction(
            2.5,
            "Low Minimum length for a stem");
        Scale.Fraction   minShortStemLengthLow = new Scale.Fraction(
            1.5,
            "Low Minimum length for a short stem");
        Scale.Fraction   maxStemThickness = new Scale.Fraction(
            0.4,
            "Maximum thickness of an interesting vertical stick");
        Scale.Fraction   minStaffDxHigh = new Scale.Fraction(
            0,
            "HighMinimum horizontal distance between a stem and a staff edge");
        Scale.Fraction   minStaffDxLow = new Scale.Fraction(
            0,
            "LowMinimum horizontal distance between a stem and a staff edge");
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
                "LeftAdj",
                "Check that stick is open on left side",
                constants.maxStemAdjacencyLow,
                constants.maxStemAdjacencyHigh,
                false,
                TOO_HIGH_ADJACENCY);
        }

        // Retrieve the adjacency value
        @Implement(Check.class)
        protected double getValue (Stick stick)
        {
            return (double) stick.getFirstStuck() / stick.getLength();
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
                "RightAdj",
                "Check that stick is open on right side",
                constants.maxStemAdjacencyLow,
                constants.maxStemAdjacencyHigh,
                false,
                TOO_HIGH_ADJACENCY);
        }

        // Retrieve the adjacency value
        @Implement(Check.class)
        protected double getValue (Stick stick)
        {
            return (double) stick.getLastStuck() / stick.getLength();
        }
    }

    //----------------//
    // MinAspectCheck //
    //----------------//
    private static class MinAspectCheck
        extends Check<Stick>
    {
        protected MinAspectCheck ()
        {
            super(
                "MinAspect",
                "Check that stick aspect (length/thickness) is high enough",
                constants.minStemAspectLow,
                constants.minStemAspectHigh,
                true,
                TOO_FAT);
        }

        // Retrieve the ratio length / thickness
        @Implement(Check.class)
        protected double getValue (Stick stick)
        {
            return stick.getAspect();
        }
    }

    //-----------//
    // LeftCheck //
    //-----------//
    private class LeftCheck
        extends Check<Stick>
    {
        private final SystemInfo system;

        protected LeftCheck (SystemInfo system)
            throws StepException
        {
            super(
                "LeftLimit",
                "Check stick is on right of the system beginning bar",
                constants.minStaffDxLow,
                constants.minStaffDxHigh,
                true,
                OUTSIDE_SYSTEM);
            this.system = system;
        }

        // Retrieve the stick abscissa
        @Implement(Check.class)
        protected double getValue (Stick stick)
        {
            int x = stick.getMidPos();

            return sheet.getScale()
                        .pixelsToFrac(x - system.getLeft());
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
                "Check that stick fills its bounding rectangle",
                constants.minDensityLow,
                constants.minDensityHigh,
                true,
                TOO_HOLLOW);
        }

        // Retrieve the density
        @Implement(Check.class)
        protected double getValue (Stick stick)
        {
            Rectangle rect = stick.getBounds();
            double    area = rect.width * rect.height;

            return (double) stick.getWeight() / area;
        }
    }

    //----------------//
    // MinLengthCheck //
    //----------------//
    private class MinLengthCheck
        extends Check<Stick>
    {
        protected MinLengthCheck (boolean normal)
            throws StepException
        {
            super(
                "MinLength",
                "Check that stick is long enough",
                normal ? constants.minStemLengthLow
                                : constants.minShortStemLengthLow,
                constants.minStemLengthHigh,
                true,
                TOO_SHORT);
        }

        // Retrieve the length data
        @Implement(Check.class)
        protected double getValue (Stick stick)
        {
            return sheet.getScale()
                        .pixelsToFrac(stick.getLength());
        }
    }

    //--------------//
    // MyCheckBoard //
    //--------------//
    private class MyCheckBoard
        extends CheckBoard<Stick>
    {
        public MyCheckBoard (String    unit,
                             Selection inputSelection)
        {
            super(unit, null, inputSelection);
        }

        @Override
        public void update (Selection     selection,
                            SelectionHint hint)
        {
            Object entity = selection.getEntity();

            if (entity instanceof Stick) {
                try {
                    Stick      stick = (Stick) entity;
                    SystemInfo system = sheet.getSystemAtY(
                        stick.getContourBox().y);
                    // Get a fresh suite
                    setSuite(new StemCheckSuite(system, true));
                    tellObject(stick);
                } catch (StepException ex) {
                    logger.warning("Glyph cannot be processed");
                }
            }
        }
    }

    //----------------//
    // MyGlyphLagView //
    //----------------//
    private class MyGlyphLagView
        extends GlyphLagView
    {
        public MyGlyphLagView (GlyphLag lag)
        {
            super(lag, null, null, VerticalsBuilder.this, null);
            setName("VerticalsBuilder-MyView");

            // Pixel
            setLocationSelection(
                sheet.getSelection(SelectionTag.SHEET_RECTANGLE));

            // Glyph
            Selection glyphSelection = sheet.getSelection(
                SelectionTag.VERTICAL_GLYPH);
            setGlyphSelection(glyphSelection);
            glyphSelection.addObserver(this);

            // Glyph set
            Selection glyphSetSelection = sheet.getSelection(
                SelectionTag.GLYPH_SET);
            setGlyphSetSelection(glyphSetSelection);
            glyphSetSelection.addObserver(this);
        }

        //----------//
        // colorize //
        //----------//
        @Override
        public void colorize ()
        {
            super.colorize();

            final int viewIndex = lag.viewIndexOf(this);

            // Use light gray color for past successful entities
            sheet.colorize(lag, viewIndex, Color.lightGray);

            // Use bright yellow color for recognized stems
            for (Glyph glyph : sheet.getActiveGlyphs()) {
                if (glyph.isStem()) {
                    Stick stick = (Stick) glyph;
                    stick.colorize(lag, viewIndex, Color.yellow);
                }
            }
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        public void renderItems (Graphics g)
        {
            // Render all physical info known so far
            sheet.accept(new SheetPainter(g, getZoom()));

            super.renderItems(g);
        }
    }

    //--------------------//
    // MySectionPredicate //
    //--------------------//
    private static class MySectionPredicate
        implements Predicate<GlyphSection>
    {
        public boolean check (GlyphSection section)
        {
            // We process section for which glyph is null, NOISE, STRUCTURE
            boolean result = (section.getGlyph() == null) ||
                             !section.getGlyph()
                                     .isWellKnown();

            return result;
        }
    }

    //------------//
    // RightCheck //
    //------------//
    private class RightCheck
        extends Check<Stick>
    {
        private final SystemInfo system;

        protected RightCheck (SystemInfo system)
            throws StepException
        {
            super(
                "RightLimit",
                "Check stick is on left of the system ending bar",
                constants.minStaffDxLow,
                constants.minStaffDxHigh,
                true,
                OUTSIDE_SYSTEM);
            this.system = system;
        }

        // Retrieve the stick abscissa
        @Implement(Check.class)
        protected double getValue (Stick stick)
        {
            return sheet.getScale()
                        .pixelsToFrac(
                (system.getLeft() + system.getWidth()) - stick.getMidPos());
        }
    }

    //----------------//
    // StemCheckSuite //
    //----------------//
    private class StemCheckSuite
        extends CheckSuite<Stick>
    {
        private final SystemInfo system;

        public StemCheckSuite (SystemInfo system,
                               boolean    normal)
            throws StepException
        {
            super("Stem", constants.minCheckResult.getValue());
            add(1, new MinLengthCheck(normal));
            add(1, new MinAspectCheck());
            add(1, new FirstAdjacencyCheck());
            add(1, new LastAdjacencyCheck());
            add(0, new LeftCheck(system));
            add(0, new RightCheck(system));
            add(2, new MinDensityCheck());

            this.system = system;

            if (logger.isFineEnabled()) {
                dump();
            }
        }

        @Override
        protected void dumpSpecific ()
        {
            System.out.println(system.toString());
        }
    }
}
