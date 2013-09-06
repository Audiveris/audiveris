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
import omr.check.FailureResult;
import omr.check.SuccessResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.ShapeChecker;
import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;

import omr.grid.FilamentsFactory;

import omr.lag.Section;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code VerticalsBuilder} is in charge of retrieving major
 * vertical seeds of a dedicated system.
 *
 * The purpose is to use these major vertical sticks as seeds for (bar-lines?),
 * stems, vertical edges of endings, and potential parts of alteration signs
 * (sharp, natural, flat).
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
    /** Related sheet */
    private final Sheet sheet;

    /** Dedicated system */
    private final SystemInfo system;

    /** Global sheet scale */
    private final Scale scale;

    //~ Constructors -----------------------------------------------------------
    //------------------//
    // VerticalsBuilder //
    //------------------//
    /**
     * Creates a new VerticalsBuilder object.
     *
     * @param system the related system
     */
    public VerticalsBuilder (SystemInfo system)
    {
        // We work with the sheet vertical lag
        this.system = system;
        this.sheet = system.getSheet();
        scale = system.getSheet()
                .getScale();
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // addCheckBoard //
    //---------------//
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
     * @return the number of seeds found
     * @throws omr.step.StepException
     */
    public int buildVerticals ()
            throws StepException
    {
        // Get rid of former symbols
        system.removeInactiveGlyphs(); // ????? what for ?????

        // Select suitable sections
        // Since we are looking for major seeds, we'll use only vertical sections
        List<Section> sections = new ArrayList<Section>();
        Predicate<Section> predicate = new MySectionPredicate();

        for (Section section : system.getVerticalSections()) {
            if (predicate.check(section)) {
                sections.add(section);
            }
        }

        final FilamentsFactory factory;

        try {
            // Use filament factory
            factory = new FilamentsFactory(
                    scale,
                    sheet.getNest(),
                    GlyphLayer.DEFAULT,
                    VERTICAL,
                    BasicGlyph.class);
        } catch (Exception ex) {
            logger.warn("Error creating verticals factory", ex);

            return 0;
        }

        // Adjust factory parameters
        factory.setMaxOverlapDeltaPos(constants.maxOverlapDeltaPos);
        factory.setMaxOverlapSpace(constants.maxOverlapSpace);
        factory.setMaxCoordGap(constants.maxCoordGap);

        if (system.getId() == 1) {
            factory.dump("VerticalsBuilder factory");
        }

        // Retrieve raw filaments
        List<Glyph> candidates = factory.retrieveFilaments(sections, true);

        // Apply seed checks
        int seeds = checkVerticals(candidates, false);

        logger.info(
                "{}S#{} verticals: {}",
                sheet.getLogPrefix(),
                system.getId(),
                seeds);

        return seeds;
    }

    //------------------//
    // createCheckSuite //
    //------------------//
    /**
     * Create a brand new check suite for vertical seed candidates
     *
     * @param isShort should we look for short (vs standard) stems?
     * @return the check suite ready for use
     */
    public CheckSuite<Glyph> createCheckSuite (boolean isShort)
    {
        return new VertCheckSuite(isShort);
    }

    //---------------------//
    // segmentGlyphOnStems //
    //---------------------//
    /**
     * Decompose the provided glyph into stems + leaves
     *
     * @param glyph   the glyph to decompose
     * @param isShort are we looking for short (vs standard) stems?
     */
    public void segmentGlyphOnStems (Glyph glyph,
                                     boolean isShort)
    {
        //        // Gather all sections to be browsed
        //        Collection<Section> sections = new ArrayList<Section>(
        //                glyph.getMembers());
        //
        //        logger.debug("Sections browsed: {}", Sections.toString(sections));
        //
        //        // Retrieve vertical sticks as stem candidates
        //        try {
        //            SticksBuilder verticalsArea = new SticksBuilder(
        //                    Orientation.VERTICAL,
        //                    scale,
        //                    sheet.getNest(),
        //                    new SectionsSource(sections, new MySectionPredicate()),
        //                    false);
        //            verticalsArea.setMaxThickness(constants.maxStemThickness);
        //
        //            // Retrieve stems
        //            int nb = checkVerticals(verticalsArea.retrieveSticks(), isShort);
        //
        //            if (nb > 0) {
        //                logger.debug("{} stem{}", nb, (nb > 1) ? "s" : "");
        //            } else {
        //                logger.debug("No stem found");
        //            }
        //        } catch (StepException ex) {
        //            logger.warn("stemSegment. Error in retrieving verticals");
        //        }
    }

    //----------------//
    // checkVerticals //
    //----------------//
    /**
     * This method checks for compliant vertical entities (stems)
     * within a collection of vertical sticks, and in the context of a
     * system.
     *
     * @param sticks  the provided collection of vertical sticks
     * @param isShort true for short stems
     * @return the number of stems found
     * @throws StepException
     */
    private int checkVerticals (Collection<Glyph> sticks,
                                boolean isShort)
            throws StepException
    {
        /** Suite of checks for a vertical seed */
        VertCheckSuite suite = new VertCheckSuite(isShort);
        double minResult = constants.minCheckResult.getValue();
        int seedNb = 0;

        logger.debug(
                "Searching verticals among {} sticks from {}",
                sticks.size(),
                Glyphs.toString(sticks));

        for (Glyph stick : sticks) {
            stick = system.addGlyph(stick);

            if (stick.isKnown()) {
                continue;
            }

            // Check seed is not too far from nearest staff
            if (!ShapeChecker.getInstance()
                    .checkStem(system, stick)) {
                logger.debug("Too distant seed {}", stick.idString());

                continue;
            }

            // Run the various Checks
            double res = suite.pass(stick);
            logger.debug("suite=> {} for {}", res, stick);

            if (res >= minResult) {
                stick.setResult(STEM);
                stick.setShape(Shape.VERTICAL_SEED);
                seedNb++;
            } else {
                stick.setResult(TOO_LIMITED);
            }
        }

        logger.debug("Found {} vertical seeds", seedNb);

        // Status of SIG
        logger.debug("SIG {}", system.getSig());

        return seedNb;
    }

    //~ Inner Classes ----------------------------------------------------------
    //----------------//
    // VertCheckSuite //
    //----------------//
    /**
     * A suite of checks meant for vertical seed candidates
     */
    public class VertCheckSuite
            extends CheckSuite<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        /**
         * Create a new instance
         *
         * @param isShort for short verticals
         */
        public VertCheckSuite (boolean isShort)
        {
            super("Vert", constants.minCheckResult.getValue());
            add(1, new MinLengthCheck(isShort));
            add(1, new MinAspectCheck());
            add(1, new LeftAdjacencyCheck());
            add(1, new RightAdjacencyCheck());
            add(2, new MinDensityCheck());

            if (logger.isDebugEnabled()) {
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

        Scale.Fraction minAbscissaGap = new Scale.Fraction(
                1.5,
                "Minimum abscissa gap between two stems");

        Constant.Ratio maxStemAdjacencyHigh = new Constant.Ratio(
                0.75,
                "High Maximum adjacency ratio for a stem");

        Constant.Ratio maxStemAdjacencyLow = new Constant.Ratio(
                0.60,
                "Low Maximum adjacency ratio for a stem");

        Check.Grade minCheckResult = new Check.Grade(
                0.4,
                "Minimum result for suite of check");

        Constant.Ratio minDensityHigh = new Constant.Ratio(
                0.75,
                "High Minimum density for a stem");

        Constant.Ratio minDensityLow = new Constant.Ratio(
                0.35,
                "Low Minimum density for a stem");

        Constant.Ratio minStemAspectHigh = new Constant.Ratio(
                8.33,
                "High Minimum aspect ratio for a stem stick");

        Constant.Ratio minStemAspectLow = new Constant.Ratio(
                6.67,
                "Low Minimum aspect ratio for a stem stick");

        Scale.Fraction minStemLengthHigh = new Scale.Fraction(
                2.5,
                "High Minimum length for a stem");

        Scale.Fraction minStemLengthLow = new Scale.Fraction(
                1.5,
                "Low Minimum length for a stem");

        Scale.Fraction minShortStemLengthHigh = new Scale.Fraction(
                2.5,
                "Low Minimum length for a short stem");

        Scale.Fraction minShortStemLengthLow = new Scale.Fraction(
                2.0,
                "Low Minimum length for a short stem");

        Scale.Fraction maxStemThickness = new Scale.Fraction(
                0.3,
                "Maximum thickness of an interesting vertical stick");

        Scale.Fraction minStaffDxHigh = new Scale.Fraction(
                0,
                "High Minimum horizontal distance between a stem and a staff edge");

        Scale.Fraction minStaffDxLow = new Scale.Fraction(
                0,
                "Low Minimum horizontal distance between a stem and a staff edge");

        Constant.Double maxCoTangentForCheck = new Constant.Double(
                "cotangent",
                0.1,
                "Maximum cotangent for interactive check of a stem candidate");

    }

    //--------------------//
    // LeftAdjacencyCheck //
    //--------------------//
    private static class LeftAdjacencyCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected LeftAdjacencyCheck ()
        {
            super(
                    "LeftAdj",
                    "Check that stick is open on left side",
                    constants.maxStemAdjacencyLow,
                    constants.maxStemAdjacencyHigh,
                    false,
                    TOO_HIGH_ADJACENCY);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the adjacency value
        @Override
        protected double getValue (Glyph stick)
        {
            return (double) stick.getFirstStuck() / stick.getLength(VERTICAL);
        }
    }

    //----------------//
    // MinAspectCheck //
    //----------------//
    private static class MinAspectCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected MinAspectCheck ()
        {
            super(
                    "Aspect",
                    "Check that stick aspect (length/thickness) is high enough",
                    constants.minStemAspectLow,
                    constants.minStemAspectHigh,
                    true,
                    TOO_FAT);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the ratio length / thickness
        @Override
        protected double getValue (Glyph stick)
        {
            return stick.getAspect(VERTICAL);
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
                    "Density",
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

    //----------------//
    // MinLengthCheck //
    //----------------//
    private class MinLengthCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected MinLengthCheck (boolean isShort)
        {
            super(
                    "Length",
                    "Check that stick is long enough",
                    isShort ? constants.minShortStemLengthLow
                    : constants.minStemLengthLow,
                    isShort ? constants.minShortStemLengthHigh
                    : constants.minStemLengthHigh,
                    true,
                    TOO_SHORT);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the length data
        @Override
        protected double getValue (Glyph stick)
        {
            return scale.pixelsToFrac(stick.getLength(VERTICAL));
        }
    }

    //--------------------//
    // MySectionPredicate //
    //--------------------//
    private class MySectionPredicate
            implements Predicate<Section>
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public boolean check (Section section)
        {
            // We process section for which glyph is null
            // or GLYPH_PART, NO_LEGAL_TIME, NOISE, STRUCTURE
            boolean result = (section.getGlyph() == null)
                             || !section.getGlyph()
                    .isWellKnown();

            if (!result) {
                return false;
            }

            // Check section is within system left and right boundaries
            Point center = section.getAreaCenter();

            return (center.x > system.getLeft())
                   && (center.x < system.getRight());
        }
    }

    //---------------------//
    // RightAdjacencyCheck //
    //---------------------//
    private static class RightAdjacencyCheck
            extends Check<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        protected RightAdjacencyCheck ()
        {
            super(
                    "RightAdj",
                    "Check that stick is open on right side",
                    constants.maxStemAdjacencyLow,
                    constants.maxStemAdjacencyHigh,
                    false,
                    TOO_HIGH_ADJACENCY);
        }

        //~ Methods ------------------------------------------------------------
        // Retrieve the adjacency value
        @Override
        protected double getValue (Glyph stick)
        {
            return (double) stick.getLastStuck() / stick.getLength(VERTICAL);
        }
    }

    //----------------//
    // VertCheckBoard //
    //----------------//
    private class VertCheckBoard
            extends CheckBoard<Glyph>
    {
        //~ Constructors -------------------------------------------------------

        public VertCheckBoard (SelectionService eventService,
                               Class[] eventList)
        {
            super("VertSeed", null, eventService, eventList);
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
                        SystemInfo system = sheet.getSystemOf(glyph);

                        // Make sure this is a rather vertical stick
                        if (Math.abs(glyph.getInvertedSlope()) <= constants.maxCoTangentForCheck.getValue()) {
                            // Get a fresh suite
                            applySuite(
                                    system.createStemCheckSuite(false),
                                    glyph);

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
