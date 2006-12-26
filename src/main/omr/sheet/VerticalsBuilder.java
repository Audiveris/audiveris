//----------------------------------------------------------------------------//
//                                                                            //
//                      V e r t i c a l s B u i l d e r                       //
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
import omr.check.Checkable;
import omr.check.FailureResult;
import omr.check.Result;
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
import omr.lag.SectionBoard;

import omr.score.visitor.SheetPainter;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionTag;
import static omr.selection.SelectionTag.*;

import omr.stick.Stick;

import omr.ui.BoardsPane;
import omr.ui.PixelBoard;
import static omr.ui.field.SpinnerUtilities.*;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.*;
import java.util.List;

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
    private static final FailureResult NO_CHUNK = new FailureResult(
        "Stem-NoChunk");

    //~ Instance fields --------------------------------------------------------

    /** Area of vertical runs */
    ///private final VerticalArea verticalsArea;

    /** Related user display if any */
    private GlyphLagView view;

    /** Suite of checks for a stem glyph */
    private StemCheckSuite suite = new StemCheckSuite();

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // VerticalsBuilder //
    //------------------//
    /**
     * Creates a new VerticalsBuilder object.
     *
     * @param sheet the related sheet
     *
     * @throws ProcessingException when processing had to stop
     */
    public VerticalsBuilder (Sheet sheet)
        throws ProcessingException
    {
        // We re-use the same vertical lag (but not the sticks) from Bars.
        super(sheet, sheet.getVerticalLag());

        Scale        scale = sheet.getScale();

        // We cannot reuse the sticks, since thick sticks are allowed for bars
        // but not for stems. So, let's build a new stick area from the initial
        // lag.
        VerticalArea verticalsArea = new VerticalArea(
            sheet,
            lag,
            new MySectionPredicate(),
            scale.toPixels(constants.maxStemThickness));

        // Split these candidates per system
        SystemSplit.splitVerticalSticks(sheet, verticalsArea.getSticks());

        // Now process each system area on turn
        int totalStemNb = 0;

        // Iterate on systems
        for (SystemInfo system : sheet.getSystems()) {
            totalStemNb += retrieveVerticals(system);
        }

        if (constants.displayFrame.getValue() && (Main.getJui() != null)) {
            displayFrame();
        }

        logger.info(totalStemNb + " stem(s) found");
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
    public void deassignSetShape (List<Glyph> glyphs)
    {
        sheet.getSymbolsEditor()
             .deassignSetShape(glyphs);
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
                new MyCheckBoard(
                    unit,
                    suite,
                    sheet.getSelection(VERTICAL_GLYPH))));
    }

    //-------------------//
    // retrieveVerticals //
    //-------------------//
    private int retrieveVerticals (SystemInfo system)
        throws ProcessingException
    {
        double      minResult = constants.minCheckResult.getValue();
        int         stemNb = 0;
        List<Stick> sticks = system.getVerticalSticks();

        if (logger.isFineEnabled()) {
            logger.fine(
                "Searching verticals among " + sticks.size() + " sticks from " +
                system);
        }

        for (Stick stick : sticks) {
            // Run the various Checks
            double res = suite.pass(new Context(stick, system));

            if (logger.isFineEnabled()) {
                logger.fine("suite=> " + res + " for " + stick);
            }

            if (res >= minResult) {
                stick.setResult(STEM);
                stick.setShape(Shape.COMBINING_STEM);
                stick.setInterline(sheet.getScale().interline());
                system.getGlyphs()
                      .add(stick);
                stemNb++;
            } else {
                stick.setResult(TOO_LIMITED);
            }
        }

        // (Re)Sort the modified list of glyphs, by abscissa
        system.sortGlyphs();

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
        Scale.Fraction   chunkHeight = new Scale.Fraction(
            0.33,
            "Height of half area to look for chunks");
        Scale.Fraction   chunkWidth = new Scale.Fraction(
            0.33,
            "Width of half area to look for chunks");
        Constant.Boolean displayFrame = new Constant.Boolean(
            true,
            "Should we display a frame on the stem sticks");
        Constant.Double  maxStemAdjacencyHigh = new Constant.Double(
            0.70,
            "High Maximum adjacency ratio for a stem stick");
        Constant.Double  maxStemAdjacencyLow = new Constant.Double(
            0.60,
            "Low Maximum adjacency ratio for a stem stick");
        Constant.Double  minCheckResult = new Constant.Double(
            0.50,
            "Minimum result for suite of check");
        Constant.Double  minDensityHigh = new Constant.Double(
            0.9,
            "High Minimum density for a stem");
        Constant.Double  minDensityLow = new Constant.Double(
            0.8,
            "Low Minimum density for a stem");
        Constant.Double  minStemAspectHigh = new Constant.Double(
            12.5,
            "High Minimum aspect ratio for a stem stick");
        Constant.Double  minStemAspectLow = new Constant.Double(
            10.0,
            "Low Minimum aspect ratio for a stem stick");
        Scale.Fraction   minStemLengthHigh = new Scale.Fraction(
            3.5,
            "High Minimum length for a stem");
        Scale.Fraction   minStemLengthLow = new Scale.Fraction(
            2.5,
            "Low Minimum length for a stem");
        Scale.Fraction   maxStemThickness = new Scale.Fraction(
            0.4,
            "Maximum thickness of an interesting vertical stick");
    }

    //---------//
    // Context //
    //---------//
    private static class Context
        implements Checkable
    {
        Stick      stick;
        SystemInfo system;

        public Context (Stick      stick,
                        SystemInfo system)
        {
            this.stick = stick;
            this.system = system;
        }

        @Implement(Checkable.class)
        public void setResult (Result result)
        {
            stick.setResult(result);
        }
    }

    //---------------------//
    // FirstAdjacencyCheck //
    //---------------------//
    private static class FirstAdjacencyCheck
        extends Check<Context>
    {
        protected FirstAdjacencyCheck ()
        {
            super(
                "LeftAdj",
                "Check that stick is open on left side (dimension-less)",
                constants.maxStemAdjacencyLow.getValue(),
                constants.maxStemAdjacencyHigh.getValue(),
                false,
                TOO_HIGH_ADJACENCY);
        }

        // Retrieve the adjacency value
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int   length = stick.getLength();

            return (double) stick.getFirstStuck() / (double) length;
        }
    }

    //--------------------//
    // LastAdjacencyCheck //
    //--------------------//
    private static class LastAdjacencyCheck
        extends Check<Context>
    {
        protected LastAdjacencyCheck ()
        {
            super(
                "RightAdj",
                "Check that stick is open on right side (dimension-less)",
                constants.maxStemAdjacencyLow.getValue(),
                constants.maxStemAdjacencyHigh.getValue(),
                false,
                TOO_HIGH_ADJACENCY);
        }

        // Retrieve the adjacency value
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int   length = stick.getLength();

            return (double) stick.getLastStuck() / (double) length;
        }
    }

    //----------------//
    // MinAspectCheck //
    //----------------//
    private static class MinAspectCheck
        extends Check<Context>
    {
        protected MinAspectCheck ()
        {
            super(
                "MinAspect",
                "Check that stick aspect (length/thickness) is" +
                " high enough (dimension-less)",
                constants.minStemAspectLow.getValue(),
                constants.minStemAspectHigh.getValue(),
                true,
                TOO_FAT);
        }

        // Retrieve the ratio length / thickness
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            return context.stick.getAspect();
        }
    }

    //-----------//
    // LeftCheck //
    //-----------//
    private class LeftCheck
        extends Check<Context>
    {
        protected LeftCheck ()
            throws ProcessingException
        {
            super(
                "LeftLimit",
                "Check stick is on right of the system beginning bar",
                0,
                0,
                true,
                OUTSIDE_SYSTEM);
        }

        // Retrieve the stick abscissa
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int   x = stick.getMidPos();

            return sheet.getScale()
                        .pixelsToFrac(x - context.system.getLeft());
        }
    }

    //-----------------//
    // MinDensityCheck //
    //-----------------//
    private static class MinDensityCheck
        extends Check<Context>
    {
        protected MinDensityCheck ()
        {
            super(
                "MinDensity",
                "Check that stick fills its bounding rectangle" +
                " (unit is interline squared)",
                constants.minDensityLow.getValue(),
                constants.minDensityHigh.getValue(),
                true,
                TOO_HOLLOW);
        }

        // Retrieve the density
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick     stick = context.stick;
            Rectangle rect = stick.getBounds();
            double    area = rect.width * rect.height;

            return (double) stick.getWeight() / area;
        }
    }

    //----------------//
    // MinLengthCheck //
    //----------------//
    private class MinLengthCheck
        extends Check<Context>
    {
        protected MinLengthCheck ()
            throws ProcessingException
        {
            super(
                "MinLength",
                "Check that stick is long enough (unit is interline)",
                constants.minStemLengthLow.getValue(),
                constants.minStemLengthHigh.getValue(),
                true,
                TOO_SHORT);
        }

        // Retrieve the length data
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            return sheet.getScale()
                        .pixelsToFrac(context.stick.getLength());
        }
    }

    //--------------//
    // MyCheckBoard //
    //--------------//
    private class MyCheckBoard
        extends CheckBoard<Context>
    {
        public MyCheckBoard (String              unit,
                             CheckSuite<Context> suite,
                             Selection           inputSelection)
        {
            super(unit, suite, inputSelection);
        }

        @Override
        public void update (Selection     selection,
                            SelectionHint hint)
        {
            Context context = null;
            Object  entity = selection.getEntity();

            if (entity instanceof Stick) {
                try {
                    // Get a fresh suite
                    setSuite(new StemCheckSuite());

                    Stick      stick = (Stick) entity;
                    Rectangle  rect = (Rectangle) sheet.getSelection(
                        SelectionTag.PIXEL)
                                                       .getEntity();
                    Point      pt = rect.getLocation();
                    SystemInfo system = sheet.getSystemAtY(pt.y);
                    context = new Context(stick, system);
                } catch (ProcessingException ex) {
                    logger.warning("Glyph cannot be processed");
                }
            }

            tellObject(context);
        }
    }

    //--------//
    // MyView //
    //--------//
    private class MyGlyphLagView
        extends GlyphLagView
    {
        public MyGlyphLagView (GlyphLag lag)
        {
            super(lag, null, null, VerticalsBuilder.this, null);
            setName("VerticalsBuilder-MyView");

            // Pixel
            setLocationSelection(sheet.getSelection(SelectionTag.PIXEL));

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

            final int viewIndex = lag.getViews()
                                     .indexOf(this);

            // All remaining vertical sticks clutter
            //            for (Stick stick : verticalsArea.getSticks()) {
            //                stick.colorize(lag, viewIndex, Color.red);
            //            }
            //            for (Glyph glyph : lag.getGlyphs()) {
            //                glyph.colorize(lag, viewIndex, Color.red);
            //            }

            // Use light gray color for past successful entities
            sheet.colorize(lag, viewIndex, Color.lightGray);

            // Iterate on systems
            for (SystemInfo system : sheet.getSystems()) {
                // Use bright yellow color for recognized stems
                for (Glyph glyph : system.getGlyphs()) {
                    if (glyph.isStem()) {
                        Stick stick = (Stick) glyph;
                        stick.colorize(lag, viewIndex, Color.yellow);
                    }
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
        extends VerticalArea.SectionPredicate
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
        extends Check<Context>
    {
        protected RightCheck ()
            throws ProcessingException
        {
            super(
                "RightLimit",
                "Check stick is on left of the system ending bar",
                0,
                0,
                true,
                OUTSIDE_SYSTEM);
        }

        // Retrieve the stick abscissa
        @Implement(Check.class)
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int   x = stick.getMidPos();

            return sheet.getScale()
                        .pixelsToFrac(
                (context.system.getLeft() + context.system.getWidth()) -
                context.stick.getFirstPos());
        }
    }

    //----------------//
    // StemCheckSuite //
    //----------------//
    private class StemCheckSuite
        extends CheckSuite<Context>
    {
        public StemCheckSuite ()
            throws ProcessingException
        {
            super("Stem", constants.minCheckResult.getValue());
            add(1, new MinLengthCheck());
            add(1, new MinAspectCheck());
            add(1, new FirstAdjacencyCheck());
            add(1, new LastAdjacencyCheck());
            add(0, new LeftCheck());
            add(0, new RightCheck());
            add(2, new MinDensityCheck());

            // This step is much too expensive (and not really useful ...)
            ////add(2, new MinChunkCheck());
            if (logger.isFineEnabled()) {
                dump();
            }
        }
    }
}
