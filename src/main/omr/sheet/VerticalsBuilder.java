//----------------------------------------------------------------------------//
//                                                                            //
//                      V e r t i c a l s B u i l d e r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.check.Check;
import omr.check.CheckSuite;
import omr.check.FailureResult;
import omr.check.SuccessResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.Glyphs;
import omr.glyph.Shape;

import omr.lag.Section;

import omr.log.Logger;

import omr.selection.GlyphEvent;
import omr.selection.UserEvent;

import omr.step.StepException;

import omr.stick.Stick;

import omr.util.Implement;
import omr.util.Predicate;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Class <code>VerticalsBuilder</code> is in charge of retrieving all the
 * vertical sticks of a dedicated system. Bars are assumed to have
 * been already recognized, so this accounts for stems, vertical edges of
 * endings, and potentially parts of alterations (sharp, natural, flat).
 *
 * @author Hervé Bitteur
 */
public class VerticalsBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        VerticalsBuilder.class);

    /** Events this entity is interested in */
    private static final Collection<Class<?extends UserEvent>> eventClasses = new ArrayList<Class<?extends UserEvent>>();

    static {
        eventClasses.add(GlyphEvent.class);
    }

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

    /** Related glyph lag */
    private final GlyphLag lag;

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
        this.lag = sheet.getVerticalLag();
        scale = system.getSheet()
                      .getScale();
    }

    //~ Methods ----------------------------------------------------------------

    //----------------------//
    // createStemCheckSuite //
    //----------------------//
    /**
     * Create a brand new check suite for stem glyph candidates
     * @param isShort should we look for short (vs standard) stems?
     * @return the check suite ready for use
     */
    public CheckSuite<Stick> createStemCheckSuite (boolean isShort)
    {
        return new StemCheckSuite(isShort);
    }

    //-------------------//
    // retrieveVerticals //
    //-------------------//
    /**
     * Actually build the new verticals glyphs out of the dedicated system
     * @return the number of stems found
     * @throws omr.step.StepException
     */
    public int retrieveVerticals ()
        throws StepException
    {
        // Get rid of former symbols
        system.removeInactiveGlyphs();

        // We cannot reuse the sticks, since thick sticks are allowed for bars
        // but not for stems.
        VerticalArea verticalsArea = new VerticalArea(
            system.getVerticalSections(),
            sheet,
            lag,
            new MySectionPredicate(),
            scale.toPixels(constants.maxStemThickness));

        return retrieveVerticals(verticalsArea.getSticks(), true);
    }

    //---------------------//
    // segmentGlyphOnStems //
    //---------------------//
    /**
     * Decompose the provided glyph into stems + leaves
     * @param glyph the glyph to decompose
     * @param isShort are we looking for short (vs standard) stems?
     */
    public void segmentGlyphOnStems (Glyph   glyph,
                                     boolean isShort)
    {
        // Gather all sections to be browsed
        Collection<GlyphSection> sections = new ArrayList<GlyphSection>(
            glyph.getMembers());

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
            int nb = retrieveVerticals(verticalsArea.getSticks(), isShort);

            if (logger.isFineEnabled()) {
                if (nb > 0) {
                    logger.fine(nb + " stem" + ((nb > 1) ? "s" : ""));
                } else {
                    logger.fine("No stem found");
                }
            }
        } catch (StepException ex) {
            logger.warning("stemSegment. Error in retrieving verticals");
        }
    }

    //-------------------//
    // retrieveVerticals //
    //-------------------//
    /**
     * This method retrieve compliant vertical entities (stems) within a
     * collection of vertical sticks, and in the context of a system
     *
     * @param sticks the provided collection of vertical sticks
     * @param isShort true for short stems
     * @return the number of stems found
     * @throws StepException
     */
    private int retrieveVerticals (Collection<Stick> sticks,
                                   boolean           isShort)
        throws StepException
    {
        /** Suite of checks for a stem glyph */
        StemCheckSuite suite = new StemCheckSuite(isShort);
        double minResult = constants.minCheckResult.getValue();
        int    stemNb = 0;

        if (logger.isFineEnabled()) {
            logger.fine(
                "Searching verticals among " + sticks.size() + " sticks from " +
                Glyphs.toString(sticks));
        }

        for (Stick stick : sticks) {
            stick = (Stick) system.addGlyph(stick);

            if (!stick.isShapeForbidden(Shape.COMBINING_STEM)) {
                // Run the various Checks
                double res = suite.pass(stick);

                if (logger.isFineEnabled()) {
                    logger.fine("suite=> " + res + " for " + stick);
                }

                if (res >= minResult) {
                    stick.setResult(STEM);
                    stick.setShape(Shape.COMBINING_STEM);
                    stemNb++;
                } else {
                    stick.setResult(TOO_LIMITED);
                }
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine("Found " + stemNb + " stems");
        }

        return stemNb;
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------------//
    // StemCheckSuite //
    //----------------//
    /**
     * A suite of checks meant for stem candidates
     */
    public class StemCheckSuite
        extends CheckSuite<Stick>
    {
        //~ Constructors -------------------------------------------------------

        /**
         * Create a new instance
         * @param isShort for short stems
         */
        public StemCheckSuite (boolean isShort)
        {
            super("Stem", constants.minCheckResult.getValue());
            add(1, new MinLengthCheck(isShort));
            add(1, new MinAspectCheck());
            add(1, new FirstAdjacencyCheck());
            add(1, new LastAdjacencyCheck());
            add(0, new LeftCheck(system));
            add(0, new RightCheck(system));
            add(2, new MinDensityCheck());

            if (logger.isFineEnabled()) {
                dump();
            }
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected void dumpSpecific ()
        {
            System.out.println(system.toString());
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Ratio maxStemAdjacencyHigh = new Constant.Ratio(
            0.75,
            "High Maximum adjacency ratio for a stem stick");
        Constant.Ratio maxStemAdjacencyLow = new Constant.Ratio(
            0.60,
            "Low Maximum adjacency ratio for a stem stick");
        Check.Grade    minCheckResult = new Check.Grade(
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
            3.0,
            "High Minimum length for a stem");
        Scale.Fraction minStemLengthLow = new Scale.Fraction(
            2.4,
            "Low Minimum length for a stem");
        Scale.Fraction minShortStemLengthLow = new Scale.Fraction(
            1.5,
            "Low Minimum length for a short stem");
        Scale.Fraction maxStemThickness = new Scale.Fraction(
            0.4,
            "Maximum thickness of an interesting vertical stick");
        Scale.Fraction minStaffDxHigh = new Scale.Fraction(
            0,
            "HighMinimum horizontal distance between a stem and a staff edge");
        Scale.Fraction minStaffDxLow = new Scale.Fraction(
            0,
            "LowMinimum horizontal distance between a stem and a staff edge");
    }

    //---------------------//
    // FirstAdjacencyCheck //
    //---------------------//
    private static class FirstAdjacencyCheck
        extends Check<Stick>
    {
        //~ Constructors -------------------------------------------------------

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

        //~ Methods ------------------------------------------------------------

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
        //~ Constructors -------------------------------------------------------

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

        //~ Methods ------------------------------------------------------------

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
        //~ Constructors -------------------------------------------------------

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

        //~ Methods ------------------------------------------------------------

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
        //~ Instance fields ----------------------------------------------------

        private final SystemInfo system;

        //~ Constructors -------------------------------------------------------

        protected LeftCheck (SystemInfo system)
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

        //~ Methods ------------------------------------------------------------

        // Retrieve the stick abscissa
        @Implement(Check.class)
        protected double getValue (Stick stick)
        {
            int x = stick.getMidPos();

            return scale.pixelsToFrac(x - system.getLeft());
        }
    }

    //-----------------//
    // MinDensityCheck //
    //-----------------//
    private static class MinDensityCheck
        extends Check<Stick>
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
        //~ Constructors -------------------------------------------------------

        protected MinLengthCheck (boolean isShort)
        {
            super(
                "MinLength",
                "Check that stick is long enough",
                isShort ? constants.minShortStemLengthLow
                                : constants.minStemLengthLow,
                constants.minStemLengthHigh,
                true,
                TOO_SHORT);
        }

        //~ Methods ------------------------------------------------------------

        // Retrieve the length data
        @Implement(Check.class)
        protected double getValue (Stick stick)
        {
            return scale.pixelsToFrac(stick.getLength());
        }
    }

    //--------------------//
    // MySectionPredicate //
    //--------------------//
    private static class MySectionPredicate
        implements Predicate<GlyphSection>
    {
        //~ Methods ------------------------------------------------------------

        public boolean check (GlyphSection section)
        {
            // We process section for which glyph is null
            // or GLYPH_PART, NO_LEGAL_TIME, NOISE, STRUCTURE
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
        //~ Instance fields ----------------------------------------------------

        private final SystemInfo system;

        //~ Constructors -------------------------------------------------------

        protected RightCheck (SystemInfo system)
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

        //~ Methods ------------------------------------------------------------

        // Retrieve the stick abscissa
        @Implement(Check.class)
        protected double getValue (Stick stick)
        {
            return scale.pixelsToFrac(
                (system.getLeft() + system.getWidth()) - stick.getMidPos());
        }
    }
}
