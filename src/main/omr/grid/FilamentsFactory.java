//----------------------------------------------------------------------------//
//                                                                            //
//                      F i l a m e n t s F a c t o r y                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.Nest;
import omr.glyph.facets.BasicAlignment;
import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphComposition;

import omr.lag.Section;
import omr.lag.Sections;

import omr.math.Line;
import omr.math.PointsCollector;

import omr.run.Orientation;

import omr.sheet.Scale;

import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code FilamentsFactory} builds filaments (long series of
 * sections) out of a collection of sections.
 *
 * <p>These filaments are meant to represent good candidates for (horizontal)
 * staff lines or (vertical) bar lines. The factory aims at a given orientation,
 * though the input sections may exhibit mixed orientations.</p>
 *
 * <p>Internal parameters have default values defined via a ConstantSet. Before
 * launching filaments retrieval by {@link #retrieveFilaments}, parameters can
 * be modified individually by calling some setXXX() methods.</p>
 *
 * @author Hervé Bitteur
 */
public class FilamentsFactory
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            FilamentsFactory.class);

    //~ Instance fields --------------------------------------------------------
    /** Related scale */
    private final Scale scale;

    /** Where filaments are to be stored */
    private final Nest nest;

    /** Factory orientation */
    private final Orientation orientation;

    /** Precise constructor for filaments */
    private final Constructor<?> filamentConstructor;

    private final Object[] scaleArgs;

    /** Scale-dependent constants for horizontal stuff */
    private final Parameters params;

    /** Long filaments found, non sorted */
    private final List<Glyph> filaments = new ArrayList<>();

    //~ Constructors -----------------------------------------------------------
    //------------------//
    // FilamentsFactory //
    //------------------//
    /**
     * Create a factory of filaments.
     *
     * @param scale         the related scale
     * @param nest          the nest to host created filaments
     * @param orientation   the target orientation
     * @param filamentClass precise Filament class to be use for creation
     * @throws Exception
     */
    public FilamentsFactory (Scale scale,
                             Nest nest,
                             Orientation orientation,
                             Class<? extends Glyph> filamentClass)
            throws Exception
    {
        this.scale = scale;
        this.nest = nest;
        this.orientation = orientation;

        scaleArgs = new Object[]{scale};
        filamentConstructor = filamentClass.getConstructor(
                new Class<?>[]{Scale.class});

        params = new Parameters();
        params.initialize();
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // dump //
    //------//
    public void dump ()
    {
        params.dump();
    }

    //--------------//
    // isSectionFat //
    //--------------//
    /**
     * Detect if the provided section is a thick one.
     * (as seen in the context of the factory orientation)
     *
     * @param section the section to check
     * @return true if fat
     */
    public boolean isSectionFat (Section section)
    {
        if (section.isFat() == null) {
            try {
                if (section.getMeanThickness(orientation) <= 1) {
                    section.setFat(false);

                    return section.isFat();
                }

                // Check global slimness
                if (section.getMeanAspect(orientation) < params.minSectionAspect) {
                    section.setFat(true);

                    return section.isFat();
                }

                // Check thickness
                Rectangle bounds = orientation.oriented(section.getBounds());
                Line line = orientation.switchRef(
                        section.getAbsoluteLine());

                if (Math.abs(line.getSlope()) < (Math.PI / 4)) {
                    // Measure mean thickness on each half
                    int startCoord = bounds.x + (bounds.width / 4);
                    int startPos = line.yAtX(startCoord);
                    int stopCoord = bounds.x + ((3 * bounds.width) / 4);
                    int stopPos = line.yAtX(stopCoord);

                    // Start side
                    Rectangle oRoi = new Rectangle(startCoord, startPos, 0, 0);
                    final int halfWidth = Math.min(
                            params.probeWidth / 2,
                            bounds.width / 4);
                    oRoi.grow(halfWidth, params.maxSectionThickness);

                    PointsCollector collector = new PointsCollector(
                            orientation.absolute(oRoi));
                    section.cumulate(collector);

                    int startThickness = (int) Math.rint(
                            (double) collector.getSize() / oRoi.width);

                    // Stop side
                    oRoi.translate(stopCoord - startCoord, stopPos - startPos);
                    collector = new PointsCollector(orientation.absolute(oRoi));
                    section.cumulate(collector);

                    int stopThickness = (int) Math.rint(
                            (double) collector.getSize() / oRoi.width);

                    section.setFat(
                            (startThickness > params.maxSectionThickness)
                            || (stopThickness > params.maxSectionThickness));
                } else {
                    section.setFat(bounds.height > params.maxSectionThickness);
                }
            } catch (Exception ex) {
                logger.warn("Error in checking fatness of " + section, ex);
                section.setFat(true);
            }
        }

        return section.isFat();
    }

    //-------------------//
    // retrieveFilaments //
    //-------------------//
    /**
     * Aggregate the long and thin sections into filaments (glyphs).
     *
     * @param source       the section source for filaments
     * @param useExpansion true to expand filaments with short sections left
     *                     over
     * @return the collection of retrieved filaments
     */
    public List<Glyph> retrieveFilaments (Collection<Section> source,
                                          boolean useExpansion)
    {
        StopWatch watch = new StopWatch("FilamentsFactory");

        try {
            // Create a filament for each section long & slim
            watch.start("createFilaments");
            createFilaments(source);

            logger.debug("{} {} filaments created.",
                    orientation, filaments.size());

            // Merge filaments into larger filaments
            watch.start("mergeFilaments");
            mergeFilaments();

            // Expand with short sections left over?
            if (useExpansion) {
                watch.start("expandFilaments");
                expandFilaments(source);

                // Merge filaments into larger filaments
                watch.start("mergeFilaments #2");
                mergeFilaments();
            }
        } catch (Exception ex) {
            logger.warn("FilamentsFactory cannot retrieveFilaments", ex);
        } finally {
            if (constants.printWatch.getValue()) {
                watch.print();
            }
        }

        return filaments;
    }

    //----------------//
    // setMaxCoordGap //
    //----------------//
    public void setMaxCoordGap (Scale.Fraction frac)
    {
        params.maxCoordGap = scale.toPixels(frac);
    }

    //----------------------//
    // setMaxExpansionSpace //
    //----------------------//
    public void setMaxExpansionSpace (Scale.Fraction frac)
    {
        params.maxExpansionSpace = scale.toPixels(frac);
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
    // setMaxInvolvingLength //
    //-----------------------//
    public void setMaxInvolvingLength (Scale.Fraction frac)
    {
        params.maxInvolvingLength = scale.toPixels(frac);
    }

    //-----------------------//
    // setMaxOverlapDeltaPos //
    //-----------------------//
    public void setMaxOverlapDeltaPos (Scale.Fraction frac)
    {
        params.maxOverlapDeltaPos = scale.toPixels(frac);
    }

    //-----------------------//
    // setMaxOverlapDeltaPos //
    //-----------------------//
    public void setMaxOverlapDeltaPos (Scale.LineFraction lFrac)
    {
        params.maxOverlapDeltaPos = scale.toPixels(lFrac);
    }

    //--------------//
    // setMaxPosGap //
    //--------------//
    public void setMaxPosGap (Scale.LineFraction lineFrac)
    {
        params.maxPosGap = scale.toPixels(lineFrac);
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

    //-------------------------//
    // setMinCoreSectionLength //
    //-------------------------//
    public void setMinCoreSectionLength (Scale.Fraction frac)
    {
        setMinCoreSectionLength(scale.toPixels(frac));
    }

    //-------------------------//
    // setMinCoreSectionLength //
    //-------------------------//
    public void setMinCoreSectionLength (int value)
    {
        params.minCoreSectionLength = value;
    }

    //---------------------//
    // setMinSectionAspect //
    //---------------------//
    public void setMinSectionAspect (double value)
    {
        params.minSectionAspect = value;
    }

    //----------//
    // canMerge //
    //----------//
    /**
     * Check whether the two provided filaments could be merged.
     *
     * @param one       a filament
     * @param two       another filament
     * @param expanding true when expanding filaments with sections left over
     * @return true if test is positive
     */
    private boolean canMerge (Glyph one,
                              Glyph two,
                              boolean expanding)
    {
        // For VIP debugging
        final boolean areVips = one.isVip() && two.isVip();
        String vips = null;

        if (areVips) {
            vips = one.getId() + "&" + two.getId() + ": "; // BP here!
        }

        try {
            // Start & Stop points for each filament
            Point2D oneStart = orientation.oriented(
                    one.getStartPoint(orientation));
            Point2D oneStop = orientation.oriented(
                    one.getStopPoint(orientation));
            Point2D twoStart = orientation.oriented(
                    two.getStartPoint(orientation));
            Point2D twoStop = orientation.oriented(
                    two.getStopPoint(orientation));

            // coord gap?
            double overlapStart = Math.max(oneStart.getX(), twoStart.getX());
            double overlapStop = Math.min(oneStop.getX(), twoStop.getX());
            double coordGap = (overlapStart - overlapStop) - 1;

            if (coordGap > params.maxCoordGap) {
                if (logger.isDebugEnabled() || areVips) {
                    logger.info(
                            "{}Gap too long: {} vs {}",
                            vips, coordGap, params.maxCoordGap);
                }

                return false;
            }

            // pos gap?
            if (coordGap < 0) {
                // Overlap between the two filaments
                // Determine maximum consistent resulting thickness
                double maxConsistentThickness = maxConsistentThickness(one);
                double maxSpace = expanding ? params.maxExpansionSpace
                        : params.maxSpace;

                // Measure thickness at various coord values of overlap
                // Provided that the overlap is long enough
                int valNb = (int) Math.min(3, 1 - (coordGap / 10));

                for (int iq = 1; iq <= valNb; iq++) {
                    double midCoord = overlapStart
                                      - ((iq * coordGap) / (valNb + 1));
                    double onePos = one.getPositionAt(midCoord, orientation);
                    double twoPos = two.getPositionAt(midCoord, orientation);
                    double posGap = Math.abs(onePos - twoPos);

                    if (posGap > params.maxOverlapDeltaPos) {
                        if (logger.isDebugEnabled() || areVips) {
                            logger.info(
                                    "{}Delta pos too high for overlap: {} vs {}",
                                    vips, posGap, params.maxOverlapDeltaPos);
                        }

                        return false;
                    }

                    // Check resulting thickness at middle of overlap
                    double thickness = Glyphs.getThicknessAt(
                            midCoord,
                            orientation,
                            one,
                            two);

                    if (thickness > params.maxFilamentThickness) {
                        if (logger.isDebugEnabled() || areVips) {
                            logger.info(
                                    "{}Too thick: {} vs {} {} {}",
                                    vips, (float) thickness,
                                    params.maxFilamentThickness, one, two);
                        }

                        return false;
                    }

                    // Check thickness consistency
                    if ((-coordGap <= params.maxInvolvingLength)
                        && (thickness > maxConsistentThickness)) {
                        if (logger.isDebugEnabled() || areVips) {
                            logger.info(
                                    "{}Non consistent thickness: {} vs {} {} {}",
                                    vips, (float) thickness,
                                    (float) maxConsistentThickness, one, two);
                        }

                        return false;
                    }

                    // Check space between overlapped filaments
                    double space = thickness
                                   - (one.getThicknessAt(midCoord, orientation)
                                      + two.getThicknessAt(midCoord, orientation));

                    if (space > maxSpace) {
                        if (logger.isDebugEnabled() || areVips) {
                            logger.info(
                                    "{}Space too large: {} vs {} {} {}",
                                    vips, (float) space, maxSpace, one, two);
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
                double oneThickness = one.getWeight() / one.getLength(
                        orientation);
                double twoThickness = two.getWeight() / two.getLength(
                        orientation);
                int posMargin = (int) Math.rint(
                        Math.max(oneThickness, twoThickness) / 2);
                double posGap = Math.abs(stop.getY() - start.getY())
                                - posMargin;

                if (posGap > params.maxPosGap) {
                    if (logger.isDebugEnabled() || areVips) {
                        logger.info(
                                "{}Delta pos too high for gap: {} vs {}",
                                vips, (float) posGap, params.maxPosGap);
                    }

                    return false;
                }

                // Check slope (relevant only for significant dy)
                if (posGap > params.maxPosGapForSlope) {
                    double gapSlope = posGap / coordGap;

                    if (gapSlope > params.maxGapSlope) {
                        if (logger.isDebugEnabled() || areVips) {
                            logger.info(
                                    "{}Slope too high for gap: {} vs {}",
                                    vips, (float) gapSlope, params.maxGapSlope);
                        }

                        return false;
                    }
                }
            }

            if (logger.isDebugEnabled() || areVips) {
                logger.info("{}Compatible!", vips);
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
    private Glyph createFilament (Section section)
            throws Exception
    {
        Filament fil = (Filament) filamentConstructor.newInstance(scaleArgs);
        fil.addSection(section);

        return nest.addGlyph(fil);
    }

    //-----------------//
    // createFilaments //
    //-----------------//
    /**
     * Aggregate long sections into initial filaments.
     */
    private void createFilaments (Collection<Section> source)
            throws Exception
    {
        // Sort sections by decreasing length in the desired orientation
        List<Section> sections = new ArrayList<>(source);
        Collections.sort(
                sections,
                Sections.getReverseLengthComparator(orientation));

        for (Section section : sections) {
            // Limit to main sections
            if (section.getLength(orientation) < params.minCoreSectionLength) {
                if (section.isVip()) {
                    logger.info("Too short {}", section);
                }

                continue;
            }

            if (isSectionFat(section)) {
                if (section.isVip()) {
                    logger.info("Too fat {}", section);
                }

                continue;
            }

            Glyph fil = createFilament(section);
            filaments.add(fil);

            if (logger.isDebugEnabled() || section.isVip() || nest.isVip(fil)) {
                logger.info(
                        "Created {} with {}", fil, section);

                if (section.isVip() || nest.isVip(fil)) {
                    fil.setVip();
                }
            }
        }

        logger.debug("createFilaments: {}/{}", filaments.size(), source.size());
    }

    //-----------------//
    // expandFilaments //
    //-----------------//
    /**
     * Expand as much as possible the existing filaments with the
     * provided sections.
     *
     * @param source the source of available sections
     * @return the collection of expanded filaments
     */
    private List<Glyph> expandFilaments (Collection<Section> source)
    {
        try {
            // Sort sections by first position 
            List<Section> sections = new ArrayList<>();

            for (Section section : source) {
                if (!section.isGlyphMember() && !isSectionFat(section)) {
                    sections.add(section);
                }
            }

            logger.debug("expandFilaments: {}/{}",
                    sections.size(), source.size());

            Collections.sort(sections, Section.posComparator);

            List<Glyph> glyphs = new ArrayList<>(sections.size());

            for (Section section : sections) {
                Glyph glyph = new BasicGlyph(scale.getInterline());
                glyph.addSection(
                        section,
                        GlyphComposition.Linking.NO_LINK_BACK);
                glyph = nest.addGlyph(glyph);
                glyphs.add(glyph);

                if (section.isVip() || nest.isVip(glyph)) {
                    logger.info("VIP created {} from {}", glyph, section);
                    glyph.setVip();
                }
            }

            // List of filaments, sorted by decreasing length
            Collections.sort(
                    filaments,
                    Glyphs.getReverseLengthComparator(orientation));

            // Process each filament on turn
            for (Glyph fil : filaments) {
                // Build filament fat box
                final Rectangle filBounds = orientation.oriented(
                        fil.getBounds());
                filBounds.grow(params.maxCoordGap, params.maxPosGap);

                boolean expanding = true;

                do {
                    expanding = false;

                    for (Iterator<Glyph> it = glyphs.iterator(); it.hasNext();) {
                        Glyph glyph = it.next();
                        Rectangle glyphBounds = orientation.oriented(
                                glyph.getBounds());

                        if (filBounds.intersects(glyphBounds)) {
                            // Check more closely
                            if (canMerge(fil, glyph, true)) {
                                if (logger.isDebugEnabled()
                                    || fil.isVip()
                                    || glyph.isVip()) {
                                    logger.info("Merging {} w/ {}",
                                            fil,
                                            Sections.toString(glyph.getMembers()));

                                    if (glyph.isVip()) {
                                        fil.setVip();
                                    }
                                }

                                fil.stealSections(glyph);
                                it.remove();
                                expanding = true;

                                break;
                            }
                        } else {
                            if (fil.isVip() && glyph.isVip()) {
                                logger.info("No intersection between {} and {}",
                                        fil, glyph);
                            }
                        }
                    }
                } while (expanding);
            }
        } catch (Exception ex) {
            logger.warn("FilamentsFactory cannot expandFilaments", ex);
        }

        return filaments;
    }

    //------------------------//
    // maxConsistentThickness //
    //------------------------//
    private double maxConsistentThickness (Glyph stick)
    {
        double mean = stick.getWeight() / (double) stick.getLength(orientation);

        if (mean < 2) {
            return 2 * constants.maxConsistentRatio.getValue() * mean;
        } else {
            return constants.maxConsistentRatio.getValue() * mean;
        }
    }

    //----------------//
    // mergeFilaments //
    //----------------//
    /**
     * Aggregate single-section filaments into long multi-section
     * filaments.
     */
    private void mergeFilaments ()
    {
        // List of filaments, sorted by decreasing length
        Collections.sort(
                filaments,
                Glyphs.getReverseLengthComparator(orientation));

        // Browse by decreasing filament length
        for (Glyph current : filaments) {
            Glyph candidate = current;

            // Keep on working while we do have a candidate to check for merge
            CandidateLoop:
            while (true) {
                final Rectangle candidateBounds = orientation.oriented(
                        candidate.getBounds());
                candidateBounds.grow(params.maxCoordGap, params.maxPosGap);

                // Check the candidate vs all filaments until current excluded
                HeadsLoop:
                for (Glyph head : filaments) {
                    if (head == current) {
                        break CandidateLoop; // Actual end of sub-list
                    }

                    if ((head != candidate) && (head.getPartOf() == null)) {
                        Rectangle headBounds = orientation.oriented(
                                head.getBounds());

                        if (headBounds.intersects(candidateBounds)) {
                            // Check for a possible merge
                            if (canMerge(head, candidate, false)) {
                                if (logger.isDebugEnabled()
                                    || head.isVip()
                                    || candidate.isVip()) {
                                    logger.info(
                                            "Merged {} into {}",
                                            candidate, head);

                                    if (candidate.isVip()) {
                                        head.setVip();
                                    }
                                }

                                head.stealSections(candidate);
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
                                        "No intersection between {} and {}",
                                        candidate, head);
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
        for (Iterator<Glyph> it = filaments.iterator(); it.hasNext();) {
            Glyph fil = it.next();

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

        Constant.Double maxGapSlope = new Constant.Double(
                "tangent",
                0.5,
                "Maximum absolute slope for a gap");

        Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        Constant.Ratio minSectionAspect = new Constant.Ratio(
                3,
                "Minimum section aspect (length / thixkness)");

        Constant.Ratio maxConsistentRatio = new Constant.Ratio(
                1.7,
                "Maximum thickness ratio for consistent merge");

        //
        // Constants specified WRT mean line thickness
        // -------------------------------------------
        //
        Scale.LineFraction maxSectionThickness = new Scale.LineFraction(
                1.5,
                "Maximum horizontal section thickness WRT mean line height");

        Scale.LineFraction maxFilamentThickness = new Scale.LineFraction(
                1.5,
                "Maximum filament thickness WRT mean line height");

        Scale.LineFraction maxPosGap = new Scale.LineFraction(
                0.75,
                "Maximum delta position for a gap between filaments");

        //
        // Constants specified WRT mean interline
        // --------------------------------------
        //
        Scale.Fraction minCoreSectionLength = new Scale.Fraction(
                1,
                "Minimum length for a section to be considered as core");

        Scale.Fraction maxOverlapDeltaPos = new Scale.Fraction(
                0.5,
                "Maximum delta position between two overlapping filaments");

        Scale.Fraction maxCoordGap = new Scale.Fraction(
                1,
                "Maximum delta coordinate for a gap between filaments");

        Scale.Fraction maxSpace = new Scale.Fraction(
                0.16,
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

    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all scale-dependent parameters.
     */
    private class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        /** Probe width */
        public int probeWidth;

        /** Maximum acceptable thickness for sections */
        public int maxSectionThickness;

        /** Maximum acceptable thickness for filaments */
        public int maxFilamentThickness;

        /** Minimum acceptable length for core sections */
        public int minCoreSectionLength;

        /** Maximum acceptable delta position */
        public int maxOverlapDeltaPos;

        /** Maximum delta coordinate for real gap */
        public int maxCoordGap;

        /** Maximum delta position for real gaps */
        public int maxPosGap;

        /** Maximum space between overlapping filaments */
        public int maxSpace;

        /** Maximum space for expansion */
        public int maxExpansionSpace;

        /** Maximum filament length to apply thickness test */
        public int maxInvolvingLength;

        /** Maximum dy for slope check on real gap */
        public int maxPosGapForSlope;

        /** Minimum acceptable aspect for sections */
        public double minSectionAspect;

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
            setMinCoreSectionLength(constants.minCoreSectionLength);
            setMaxSectionThickness(constants.maxSectionThickness);
            setMaxFilamentThickness(constants.maxFilamentThickness);
            setMaxCoordGap(constants.maxCoordGap);
            setMaxPosGap(constants.maxPosGap);
            setMaxSpace(constants.maxSpace);
            setMaxExpansionSpace(constants.maxExpansionSpace);
            setMaxInvolvingLength(constants.maxInvolvingLength);
            setMaxPosGapForSlope(constants.maxPosGapForSlope);
            setMaxOverlapDeltaPos(constants.maxOverlapDeltaPos);
            setMaxGapSlope(constants.maxGapSlope.getValue());
            setMinSectionAspect(constants.minSectionAspect.getValue());

            probeWidth = scale.toPixels(BasicAlignment.getProbeWidth());

            if (logger.isDebugEnabled()) {
                dump();
            }
        }
    }
}
