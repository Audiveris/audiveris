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
package omr.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.Glyphs;
import omr.glyph.facets.BasicAlignment;
import omr.glyph.facets.BasicStick;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphComposition;
import omr.glyph.facets.Stick;

import omr.lag.Sections;

import omr.log.Logger;

import omr.math.Line;
import omr.math.PointsCollector;

import omr.run.Orientation;

import omr.sheet.Scale;

import omr.stick.SectionsSource;
import omr.stick.StickSection;

import omr.util.StopWatch;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class <code>FilamentsFactory</code> builds filaments out of sections
 * provided by a {@link SectionsSource}.
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

    /** (Debug) Collection of glyphs ids to be supervised, if any */
    private Set<Integer> vipGlyphs;

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
                int       startPos = line.yAtX(startCoord);
                int       stopCoord = bounds.x + ((3 * bounds.width) / 4);
                int       stopPos = line.yAtX(stopCoord);

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

    //--------------//
    // setVipGlyphs //
    //--------------//
    /**
     * Debug method to flag certain glyphs as VIPs
     * @param ids the VIP glyphs ids
     */
    public void setVipGlyphs (Collection<Integer> ids)
    {
        vipGlyphs = new HashSet<Integer>();
        vipGlyphs.addAll(ids);
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
     * Aggregate the long and thin sections into filaments (glyphs)
     *
     * @param source the section source for filaments
     * @param useExpansion true to expand filaments with short sections
     * @return the collection of retrieved filaments
     */
    public List<Filament> retrieveFilaments (Collection<GlyphSection> source,
                                             boolean                  useExpansion)
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

            // Expand with short sections left over?
            if (useExpansion) {
                watch.start("expandFilaments");
                expandFilaments(source);

                // Merge into long filaments
                watch.start("mergeFilaments #2");
                mergeFilaments();
            }
        } catch (Exception ex) {
            logger.warning("FilamentsFactory cannot retrieveFilaments", ex);
        } finally {
            if (constants.printWatch.getValue()) {
                watch.print();
            }
        }

        return filaments;
    }

    //-------//
    // isVip //
    //-------//
    private boolean isVip (Glyph glyph)
    {
        return (vipGlyphs != null) && vipGlyphs.contains(glyph.getId());
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
    private boolean canMerge (Stick one,
                              Stick two)
    {
        // For VIP debugging
        final boolean areVips = one.isVip() && two.isVip();
        String        vips = null;

        if (areVips) {
            vips = one.getId() + "&" + two.getId() + ": "; // BP here!
        }

        Orientation orientation = lag.getOrientation();

        try {
            // Start & Stop points for each filament
            Point2D oneStart = orientation.oriented(one.getStartPoint());
            Point2D oneStop = orientation.oriented(one.getStopPoint());
            Point2D twoStart = orientation.oriented(two.getStartPoint());
            Point2D twoStop = orientation.oriented(two.getStopPoint());

            // coord gap?
            double overlapStart = Math.max(oneStart.getX(), twoStart.getX());
            double overlapStop = Math.min(oneStop.getX(), twoStop.getX());
            double coordGap = (overlapStart - overlapStop) - 1;

            if (coordGap > params.maxCoordGap) {
                if (logger.isFineEnabled() || areVips) {
                    logger.info(
                        vips + "Gap too long: " + coordGap + " vs " +
                        params.maxCoordGap);
                }

                return false;
            }

            // pos gap?
            if (coordGap < 0) {
                // Overlap between the two filaments
                // Measure thickness at various coord values of overlap
                for (int iq = 1; iq < 4; iq++) {
                    double midCoord = overlapStart - ((iq * coordGap) / 4);
                    double onePos = one.getPositionAt(midCoord);
                    double twoPos = two.getPositionAt(midCoord);
                    double posGap = Math.abs(onePos - twoPos);

                    if (posGap > params.maxOverlapDeltaPos) {
                        if (logger.isFineEnabled() || areVips) {
                            logger.info(
                                vips + "Delta pos too high for overlap: " +
                                posGap + " vs " + params.maxOverlapDeltaPos);
                        }

                        return false;
                    }

                    // Check resulting thickness at middle of overlap
                    double thickness = Glyphs.getThicknessAt(
                        midCoord,
                        one,
                        two);

                    if (thickness > params.maxFilamentThickness) {
                        if (logger.isFineEnabled() || areVips) {
                            logger.info(
                                vips + "Too thick: " + (float) thickness +
                                " vs " + params.maxFilamentThickness + " " +
                                one + " " + two);
                        }

                        return false;
                    }

                    // Check space between overlapped filaments
                    double space = thickness -
                                   (one.getThicknessAt(midCoord) +
                                   two.getThicknessAt(midCoord));

                    if (space > params.maxSpace) {
                        if (logger.isFineEnabled() || areVips) {
                            logger.info(
                                vips + "Space too large: " + (float) space +
                                " vs " + params.maxSpace + " " + one + " " +
                                two);
                        }

                        return false;
                    }
                }
            } else {
                // No overlap, it's a true gap
                Point2D start;
                Point2D stop;

                if (oneStart.getX() < twoStart.getX()) {
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
                double posGap = Math.abs(stop.getY() - start.getY()) -
                                posMargin;

                if (posGap > params.maxPosGap) {
                    if (logger.isFineEnabled() || areVips) {
                        logger.info(
                            vips + "Delta pos too high for gap: " +
                            (float) posGap + " vs " + params.maxPosGap);
                    }

                    return false;
                }

                // Check slope (relevant only for significant dy)
                if (posGap > params.maxPosGapForSlope) {
                    double gapSlope = posGap / coordGap;

                    if (gapSlope > params.maxGapSlope) {
                        if (logger.isFineEnabled() || areVips) {
                            logger.info(
                                vips + "Slope too high for gap: " +
                                (float) gapSlope + " vs " + params.maxGapSlope);
                        }

                        return false;
                    }
                }
            }

            if (logger.isFineEnabled() || areVips) {
                logger.info(vips + "Compatible!");
            }

            return true;
        } catch (Exception ex) {
            // Generally a stick for which some parameters cannot be computed
            return false;
        }
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
    private void createFilaments (Collection<GlyphSection> source)
        throws Exception
    {
        // Sort sections by decreasing length
        List<GlyphSection> sections = new ArrayList<GlyphSection>(source);
        Collections.sort(sections, GlyphSection.reverseLengthComparator);

        for (GlyphSection section : sections) {
            // Limit to main sections
            if ((section.getLength() >= params.minSectionLength) &&
                !isSectionFat(section)) {
                Filament fil = createFilament(section);
                filaments.add(fil);

                if (logger.isFineEnabled() || section.isVip() || isVip(fil)) {
                    logger.info("Created " + fil + " with " + section);

                    if (section.isVip() || isVip(fil)) {
                        fil.setVip();
                    }
                }
            }
        }

        if (logger.isFineEnabled()) {
            logger.info(
                "createFilaments: " + filaments.size() + "/" + source.size());
        }
    }

    //-----------------//
    // expandFilaments //
    //-----------------//
    /**
     * Expand as much as possible the existing filaments with the provided
     * sections
     * @param source the source of available sections
     * @return the collection of expanded filaments
     */
    private List<Filament> expandFilaments (Collection<GlyphSection> source)
    {
        try {
            // Sort sections by first position 
            List<GlyphSection> sections = new ArrayList<GlyphSection>();

            for (GlyphSection section : source) {
                if (!section.isGlyphMember()) {
                    sections.add(section);
                }
            }

            if (logger.isFineEnabled()) {
                logger.info(
                    "expandFilaments: " + sections.size() + "/" +
                    source.size());
            }

            Collections.sort(sections, GlyphSection.posComparator);

            List<Stick> sticks = new ArrayList<Stick>(sections.size());

            for (GlyphSection section : sections) {
                Stick stick = new BasicStick(scale.interline());
                stick.addSection(
                    section,
                    GlyphComposition.Linking.NO_LINK_BACK);
                lag.addGlyph(stick);
                sticks.add(stick);

                if (section.isVip() || isVip(stick)) {
                    logger.info("VIP created " + stick + " from " + section);
                    stick.setVip();
                }
            }

            // List of filaments, sorted by decreasing length
            Collections.sort(filaments, Filament.reverseLengthComparator);

            // Process each filament on turn
            for (Filament fil : filaments) {
                // Build filament fat box
                final Rectangle filBounds = fil.getOrientedBounds();
                filBounds.grow(params.maxCoordGap, params.maxPosGap);

                int     maxPos = filBounds.y + filBounds.height;
                boolean expanding = true;

                do {
                    expanding = false;

                    for (Iterator<Stick> it = sticks.iterator(); it.hasNext();) {
                        Stick     stick = it.next();
                        Rectangle stickBounds = stick.getOrientedBounds();

                        if (filBounds.intersects(stickBounds)) {
                            // Check more closely
                            if (canMerge(fil, stick)) {
                                if (logger.isFineEnabled() ||
                                    fil.isVip() ||
                                    stick.isVip()) {
                                    logger.info(
                                        "Merging " + fil + " w/ " +
                                        Sections.toString(stick.getMembers()));

                                    if (stick.isVip()) {
                                        fil.setVip();
                                    }
                                }

                                fil.include(stick);
                                it.remove();
                                expanding = true;

                                break;
                            }
                        } else if (stickBounds.y > maxPos) {
                            break; // Speedup
                        }
                    }
                } while (expanding);
            }
        } catch (Exception ex) {
            logger.warning("FilamentsFactory cannot expandFilaments", ex);
        }

        return filaments;
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

                    if ((head != candidate) && (head.getPartOf() == null)) {
                        Rectangle headBounds = head.getOrientedBounds();

                        if (headBounds.intersects(candidateBounds)) {
                            // Check for a possible merge
                            if (canMerge(head, candidate)) {
                                if (logger.isFineEnabled() ||
                                    head.isVip() ||
                                    candidate.isVip()) {
                                    logger.info(
                                        "Merged " + candidate + " into " +
                                        head);

                                    if (candidate.isVip()) {
                                        head.setVip();
                                    }
                                }

                                head.include(candidate);
                                candidate = head; // This is a new candidate

                                break HeadsLoop;
                            } else {
                                //                                if (head.isVip() || candidate.isVip()) {
                                //                                    logger.info(
                                //                                        "Could not merge " + candidate +
                                //                                        " into " + head);
                                //                                }
                            }
                        } else {
                            if (head.isVip() && candidate.isVip()) {
                                logger.info(
                                    "No intersection between " + candidate +
                                    " and " + head);
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

            if (fil.getPartOf() != null) {
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
        Constant.Boolean   printWatch = new Constant.Boolean(
            false,
            "Should we print out the stop watch?");

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
            0.175,
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

            probeWidth = scale.toPixels(BasicAlignment.getProbeWidth());

            if (logger.isFineEnabled()) {
                dump();
            }
        }
    }
}
