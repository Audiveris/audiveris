//----------------------------------------------------------------------------//
//                                                                            //
//                      F i l a m e n t s F a c t o r y                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.stick;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;

import omr.log.Logger;

import omr.math.Line;
import omr.math.PointsCollector;

import omr.run.Orientation;

import omr.sheet.Scale;

import omr.step.StepException;

import omr.util.StopWatch;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.List;

/**
 * Class <code>FilamentsFactory</code> builds filaments out of sections
 * provided by a {@link SticksSource}.
 *
 * @author Hervé Bitteur
 */
public class FilamentsFactory
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        FilamentsFactory.class);

    //~ Instance fields --------------------------------------------------------

    /** Related scale */
    private final Scale scale;

    /** Underlying lag */
    private final GlyphLag lag;

    /** Precise constructor for filaments */
    private final Constructor filamentConstructor;
    private final Object[]       scaleArgs;

    /** Scale-dependent constants for horizontal stuff */
    private final Parameters params;

    /** Long filaments found, non sorted */
    private final List<Filament> filaments = new ArrayList<Filament>();

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // FilamentsFactory //
    //------------------//
    /**
     * Retrieve the frames of all staff lines
     *
     * @param scale the related scale
     * @param lag the containing lag
     * @param filamentClass precise Filament class to be use for creation
     * @throws Exception
     */
    public FilamentsFactory (Scale                    scale,
                             GlyphLag                 lag,
                             Class<?extends Filament> filamentClass)
        throws Exception
    {
        this.scale = scale;
        this.lag = lag;

        scaleArgs = new Object[] { scale };
        filamentConstructor = filamentClass.getConstructor(
            new Class[] { Scale.class });

        params = new Parameters();
        params.initialize();
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // setMaxCoordGap //
    //----------------//
    public void setMaxCoordGap (Scale.Fraction frac)
    {
        params.maxCoordGap = scale.toPixels(frac);
    }

    //-------------------------//
    // setMaxFilamentThickness //
    //-------------------------//
    public void setMaxFilamentThickness (Scale.LineFraction lineFrac)
    {
        params.maxFilamentThickness = scale.toPixels(lineFrac);
    }

    //-------------------------//
    // setMaxFilamentThickness //
    //-------------------------//
    public void setMaxFilamentThickness (Scale.Fraction frac)
    {
        params.maxFilamentThickness = scale.toPixels(frac);
    }

    //----------------//
    // setMaxGapSlope //
    //----------------//
    public void setMaxGapSlope (double value)
    {
        params.maxGapSlope = value;
    }

    //-----------------------//
    // setMaxOverlapDeltaPos //
    //-----------------------//
    public void setMaxOverlapDeltaPos (Scale.Fraction frac)
    {
        params.maxOverlapDeltaPos = scale.toPixels(frac);
    }

    //--------------//
    // setMaxPosGap //
    //--------------//
    public void setMaxPosGap (Scale.Fraction frac)
    {
        params.maxPosGap = scale.toPixels(frac);
    }

    //----------------------//
    // setMaxPosGapForSlope //
    //----------------------//
    public void setMaxPosGapForSlope (Scale.Fraction frac)
    {
        params.maxPosGapForSlope = scale.toPixels(frac);
    }

    //------------------------//
    // setMaxSectionThickness //
    //------------------------//
    public void setMaxSectionThickness (Scale.LineFraction lineFrac)
    {
        params.maxSectionThickness = scale.toPixels(lineFrac);
    }

    //------------------------//
    // setMaxSectionThickness //
    //------------------------//
    public void setMaxSectionThickness (Scale.Fraction frac)
    {
        params.maxSectionThickness = scale.toPixels(frac);
    }

    //-------------//
    // setMaxSpace //
    //-------------//
    public void setMaxSpace (Scale.Fraction frac)
    {
        params.maxSpace = scale.toPixels(frac);
    }

    //---------------------//
    // setMinSectionLength //
    //---------------------//
    public void setMinSectionLength (Scale.Fraction frac)
    {
        setMinSectionLength(scale.toPixels(frac));
    }

    //---------------------//
    // setMinSectionLength //
    //---------------------//
    public void setMinSectionLength (int value)
    {
        params.minSectionLength = value;
    }

    //---------------------//
    // getMinSectionLength //
    //---------------------//
    public int getMinSectionLength ()
    {
        return params.minSectionLength;
    }

    //--------------//
    // isSectionFat //
    //--------------//
    /**
     * Detect if the provided section is a thick one
     * @param section the section to check
     * @return true if fat
     */
    public boolean isSectionFat (GlyphSection section)
    {
        if (section.isFat() == null) {
            try {
                // Measure mean thickness on each half
                Rectangle bounds = section.getOrientedBounds();

                // Determine where to measure thickness
                Line      line = ((StickSection) section).getLine();
                int       startCoord = bounds.x + (bounds.width / 4);
                int       startPos = line.yAt(startCoord);
                int       stopCoord = bounds.x + ((3 * bounds.width) / 4);
                int       stopPos = line.yAt(stopCoord);

                // Start side
                Rectangle roi = new Rectangle(startCoord, startPos, 0, 0);
                final int halfWidth = Math.min(
                    params.probeWidth / 2,
                    bounds.width / 4);
                roi.grow(halfWidth, params.maxSectionThickness);

                PointsCollector collector = new PointsCollector(roi);
                section.cumulate(collector);

                int startThickness = (int) Math.rint(
                    (double) collector.getCount() / roi.width);

                // Stop side
                roi.translate(stopCoord - startCoord, stopPos - startPos);
                collector = new PointsCollector(roi);
                section.cumulate(collector);

                int stopThickness = (int) Math.rint(
                    (double) collector.getCount() / roi.width);

                section.setFat(
                    (startThickness > params.maxSectionThickness) ||
                    (stopThickness > params.maxSectionThickness));
            } catch (Exception ignored) {
                section.setFat(true);
            }
        }

        return section.isFat();
    }

    //------//
    // dump //
    //------//
    public void dump ()
    {
        params.dump();
    }

    //-------------------//
    // retrieveFilaments //
    //-------------------//
    /**
     * Organize the long and thin horizontal sections into filaments (glyphs)
     * that will be good candidates for staff lines.
     * <p>First phase consists in retrieving long horizontal sections and
     * merging them in filaments.</p>
     * <p>Second phase consists in detecting patterns of filaments regularly
     * spaced and aggregating them into clusters of lines. </p>
     *
     * @param source the section source for sticks
     */
    public List<Filament> retrieveFilaments (SticksSource source)
        throws StepException
    {
        StopWatch watch = new StopWatch("FilamentsFactory");

        try {
            // Create a filament for each section long & slim
            watch.start("createFilaments");
            createFilaments(source);

            if (logger.isFineEnabled()) {
                logger.info(
                    lag.getOrientation() + " " + filaments.size() +
                    " filaments created.");
            }

            // Merge into long filaments
            watch.start("mergeFilaments");
            mergeFilaments();

            if (logger.isFineEnabled()) {
                logger.info(
                    lag.getOrientation() + " " + filaments.size() +
                    " filaments after merge.");
            }
        } catch (Exception ex) {
            logger.warning("FilamentsFactory cannot retrieveFilaments", ex);
        } finally {
            ///watch.print();
        }

        return filaments;
    }

    //----------//
    // canMerge //
    //----------//
    /**
     * Check whether the two provided filaments could be merged
     * @param one a filament
     * @param two another filament
     * @return true if test is positive
     */
    private boolean canMerge (Filament one,
                              Filament two)
    {
        if (logger.isFineEnabled()) {
            logger.info("testMerge " + one + " & " + two);
        }

        Orientation orientation = lag.getOrientation();

        // Start & Stop points for each filament
        Point oneStart = orientation.switchRef(one.getStartPoint(), null);
        Point oneStop = orientation.switchRef(one.getStopPoint(), null);
        Point twoStart = orientation.switchRef(two.getStartPoint(), null);
        Point twoStop = orientation.switchRef(two.getStopPoint(), null);

        // coord gap?
        int overlapStart = Math.max(oneStart.x, twoStart.x);
        int overlapStop = Math.min(oneStop.x, twoStop.x);
        int coordGap = (overlapStart - overlapStop) - 1;

        if (coordGap > params.maxCoordGap) {
            if (logger.isFineEnabled()) {
                logger.info(
                    "Gap too long: " + coordGap + " vs " + params.maxCoordGap);
            }

            return false;
        }

        // pos gap?
        if (coordGap < 0) {
            // Overlap between the two filaments
            // Measure dy at middle of overlap
            int    midCoord = (overlapStart + overlapStop) / 2;
            double onePos = one.positionAt(midCoord);
            double twoPos = two.positionAt(midCoord);
            double posGap = Math.abs(onePos - twoPos);

            if (posGap > params.maxOverlapDeltaPos) {
                if (logger.isFineEnabled()) {
                    logger.info(
                        "Delta pos too high for overlap: " + posGap + " vs " +
                        params.maxOverlapDeltaPos);
                }

                return false;
            }

            // Check resulting thickness at middle of overlap
            double oneThickness = one.getThicknessAt(midCoord);
            double twoThickness = two.getThicknessAt(midCoord);

            double thickness = posGap + ((oneThickness + twoThickness) / 2);

            if (thickness > params.maxFilamentThickness) {
                if (logger.isFineEnabled()) {
                    logger.info(
                        "Too thick: " + (float) thickness + " vs " +
                        params.maxFilamentThickness + " " + one + " " + two);
                }

                return false;
            }

            // Check space between overlapped filaments
            double space = posGap - ((oneThickness + twoThickness) / 2);

            if (space > params.maxSpace) {
                if (logger.isFineEnabled()) {
                    logger.info(
                        "Space too large: " + (float) space + " vs " +
                        params.maxSpace + " " + one + " " + two);
                }

                return false;
            }
        } else {
            // No overlap, it's a true gap
            Point start;
            Point stop;

            if (oneStart.x < twoStart.x) {
                // one - two
                start = oneStop;
                stop = twoStart;
            } else {
                // two - one
                start = twoStop;
                stop = oneStart;
            }

            // Compute position gap, taking thickness into account
            double oneThickness = one.getWeight() / one.getLength();
            double twoThickness = two.getWeight() / two.getLength();
            int    posMargin = (int) Math.rint(
                Math.max(oneThickness, twoThickness) / 2);
            int    posGap = Math.abs(stop.y - start.y) - posMargin;

            if (posGap > params.maxPosGap) {
                if (logger.isFineEnabled()) {
                    logger.info(
                        "Delta pos too high for gap: " + posGap + " vs " +
                        params.maxPosGap);
                }

                return false;
            }

            // Check slope (relevant only for significant dy)
            if (posGap > params.maxPosGapForSlope) {
                double gapSlope = (double) posGap / coordGap;

                if (gapSlope > params.maxGapSlope) {
                    if (logger.isFineEnabled()) {
                        logger.info(
                            "Slope too high for gap: " + (float) gapSlope +
                            " vs " + params.maxGapSlope);
                    }

                    return false;
                }
            }
        }

        if (logger.isFineEnabled()) {
            logger.info("Compatible!");
        }

        return true;
    }

    //----------------//
    // createFilament //
    //----------------//
    private Filament createFilament (GlyphSection section)
        throws Exception
    {
        Filament fil = (Filament) filamentConstructor.newInstance(scaleArgs);
        fil.addSection(section);

        return (Filament) lag.addGlyph(fil);
    }

    //-----------------//
    // createFilaments //
    //-----------------//
    /**
     *   Aggregate long sections into initial filaments
     */
    private void createFilaments (SticksSource source)
        throws Exception
    {
        // Sort sections by decreasing length
        List<GlyphSection> sections = new ArrayList<GlyphSection>();

        while (source.hasNext()) {
            sections.add(source.next());
        }

        Collections.sort(sections, GlyphSection.reverseLengthComparator);

        for (GlyphSection section : sections) {
            // Limit to main sections for the time being
            if ((section.getLength() >= params.minSectionLength) &&
                !isSectionFat(section)) {
                Filament fil = createFilament(section);
                filaments.add(fil);

                if (logger.isFineEnabled()) {
                    logger.fine("Created " + fil + " with " + section);
                }
            }
        }
    }

    //----------------//
    // mergeFilaments //
    //----------------//
    /**
     * Aggregate single-section filaments into long multi-section filaments
     */
    private void mergeFilaments ()
    {
        // List of filaments, sorted by decreasing length
        Collections.sort(filaments, Filament.reverseLengthComparator);

        // Browse by decreasing filament length
        for (Filament current : filaments) {
            Filament candidate = current;

            // Keep on working while we do have a candidate to check for merge
            CandidateLoop: 
            while (true) {
                final Rectangle candidateBounds = candidate.getOrientedBounds();
                candidateBounds.grow(params.maxCoordGap, params.maxPosGap);

                // Check the candidate vs all filaments until current excluded
                HeadsLoop: 
                for (Filament head : filaments) {
                    if (head == current) {
                        break CandidateLoop; // Actual end of sub-list
                    }

                    if ((head != candidate) && (head.getParent() == null)) {
                        Rectangle headBounds = head.getOrientedBounds();

                        if (headBounds.intersects(candidateBounds)) {
                            // Check for a possible merge
                            if (canMerge(head, candidate)) {
                                if (logger.isFineEnabled()) {
                                    logger.info(
                                        "Merged " + candidate + " into " +
                                        head);
                                }

                                head.include(candidate);
                                candidate = head; // This is a new candidate

                                break HeadsLoop;
                            }
                        }
                    }
                }
            }
        }

        // Discard the merged filaments
        removeMergedFilaments();
    }

    //-----------------------//
    // removeMergedFilaments //
    //-----------------------//
    private void removeMergedFilaments ()
    {
        for (Iterator<Filament> it = filaments.iterator(); it.hasNext();) {
            Filament fil = it.next();

            if (fil.getParent() != null) {
                it.remove();
            }
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

        Constant.Double    maxGapSlope = new Constant.Double(
            "tangent",
            0.5,
            "Maximum absolute slope for a gap");

        // Constants specified WRT mean line thickness
        // -------------------------------------------
        Scale.LineFraction maxSectionThickness = new Scale.LineFraction(
            1.5,
            "Maximum horizontal section thickness WRT mean line height");
        Scale.LineFraction maxFilamentThickness = new Scale.LineFraction(
            1.5,
            "Maximum filament thickness WRT mean line height");

        // Constants specified WRT mean interline
        // --------------------------------------
        Scale.Fraction minSectionLength = new Scale.Fraction(
            1,
            "Minimum length for a horizontal section to be considered in frames computation");
        Scale.Fraction maxOverlapDeltaPos = new Scale.Fraction(
            0.5,
            "Maximum delta position between two overlapping filaments");
        Scale.Fraction maxCoordGap = new Scale.Fraction(
            1,
            "Maximum delta coordinate for a gap between filaments");
        Scale.Fraction maxPosGap = new Scale.Fraction(
            0.2,
            "Maximum delta position for a gap between filaments");
        Scale.Fraction maxSpace = new Scale.Fraction(
            0.2,
            "Maximum space between overlapping filaments");
        Scale.Fraction maxPosGapForSlope = new Scale.Fraction(
            0.1,
            "Maximum delta Y to check slope for a gap between filaments");
        Scale.Fraction maxInvolvingLength = new Scale.Fraction(
            6,
            "Maximum filament length to apply thickness test");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all constants related to horizontal frames
     */
    private class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        /** Probe width */
        public int probeWidth;

        /** Maximum acceptable thickness for horizontal sections */
        public int maxSectionThickness;

        /** Maximum acceptable thickness for horizontal filaments */
        public int maxFilamentThickness;

        /** Minimum acceptable length for horizontal sections */
        public int minSectionLength;

        /** Maximum acceptable delta position */
        public int maxOverlapDeltaPos;

        /** Maximum delta coordinate for real gap */
        public int maxCoordGap;

        /** Maximum delta position for real gaps */
        public int maxPosGap;

        /** Maximum space between overlapping filaments */
        public int maxSpace;

        /** Maximum dy for slope check on real gap */
        public int maxPosGapForSlope;

        /** Maximum slope for real gaps */
        public double maxGapSlope;

        //~ Methods ------------------------------------------------------------

        public void dump ()
        {
            Main.dumping.dump(this);
        }

        /**
         * Initialize with default values
         */
        public void initialize ()
        {
            setMinSectionLength(constants.minSectionLength);
            setMaxSectionThickness(constants.maxSectionThickness);
            setMaxFilamentThickness(constants.maxFilamentThickness);
            setMaxCoordGap(constants.maxCoordGap);
            setMaxPosGap(constants.maxPosGap);
            setMaxSpace(constants.maxSpace);
            setMaxPosGapForSlope(constants.maxPosGapForSlope);
            setMaxOverlapDeltaPos(constants.maxOverlapDeltaPos);
            setMaxGapSlope(constants.maxGapSlope.getValue());

            probeWidth = scale.toPixels(Filament.getProbeWidth());

            if (logger.isFineEnabled()) {
                dump();
            }
        }
    }
}
