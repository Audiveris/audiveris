//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                F i l a m e n t s F a c t o r y                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;
import omr.glyph.Glyphs;
import omr.glyph.facets.BasicAlignment;
import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphComposition;

import omr.lag.Section;
import omr.lag.Sections;

import omr.math.Line;
import omr.math.PointsCollector;

import omr.run.Orientation;
import static omr.run.Orientation.*;

import omr.sheet.Scale;

import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code FilamentsFactory} builds filaments (long series of sections) out of a
 * collection of sections.
 * <p>
 * These filaments are meant to represent good candidates for (horizontal) staff lines or (vertical)
 * bar lines.
 * The factory aims at a given orientation, though the collection of input sections may exhibit
 * mixed orientations.
 * <p>
 * The factory works in two phases:<ol>
 * <li>The first phase, by default, discovers skeletons lines using the long input sections and
 * merges them as much as possible.
 * This strategy fits well the case of a population of sections with no organization known a priori.
 * Another strategy is to explicitly provide the set of skeletons lines, and thus make the factory
 * focus on them only.
 * </li>
 * <li>The second phase completes these skeletons whenever possible by short sections left over, and
 * merges them again.</li></ol>
 * <p>
 * Customization: Default parameters values are defined via a ConstantSet.
 * Before launching filaments retrieval by {@link #retrieveFilaments}, parameters can be modified
 * individually by calling proper setXXX() methods.
 *
 * @author Hervé Bitteur
 */
public class FilamentsFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(FilamentsFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Related scale. */
    private final Scale scale;

    /** Where filaments are to be stored. */
    private final GlyphNest nest;

    /** Which related layer. */
    private final GlyphLayer layer;

    /** Factory orientation. */
    private final Orientation orientation;

    /** Precise constructor for filaments. */
    private Constructor<?> glyphConstructor;

    /** Scale-dependent constants. */
    private final Parameters params;

    //~ Constructors -------------------------------------------------------------------------------
    //------------------//
    // FilamentsFactory //
    //------------------//
    /**
     * Create a factory of filaments.
     *
     * @param scale       the related scale
     * @param nest        the nest to host created filaments
     * @param layer       precise glyph layer
     * @param orientation the target orientation
     * @param glyphClass  precise class to be used for glyph creation,
     *                    typically {@link BasicGlyph} (for straight lines)
     *                    or {@link Filament} / {@link LineFilament} (for spline lines)
     */
    public FilamentsFactory (Scale scale,
                             GlyphNest nest,
                             GlyphLayer layer,
                             Orientation orientation,
                             Class<? extends Glyph> glyphClass)
    {
        this.scale = scale;
        this.nest = nest;
        this.layer = layer;
        this.orientation = orientation;

        try {
            glyphConstructor = glyphClass.getConstructor(
                    new Class<?>[]{Scale.class, GlyphLayer.class});
        } catch (Exception ex) {
            logger.error(null, ex);
        }

        params = new Parameters();
        params.initialize();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // dump //
    //------//
    public void dump (String title)
    {
        if (constants.printParameters.isSet()) {
            params.dump(title);
        }
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
                Line line = orientation.switchRef(section.getAbsoluteLine());

                if (Math.abs(line.getSlope()) < (Math.PI / 4)) {
                    // Measure mean thickness on each half
                    int startCoord = bounds.x + (bounds.width / 4);
                    int startPos = line.yAtX(startCoord);
                    int stopCoord = bounds.x + ((3 * bounds.width) / 4);
                    int stopPos = line.yAtX(stopCoord);

                    // Start side
                    Rectangle oRoi = new Rectangle(startCoord, startPos, 0, 0);
                    final int halfWidth = Math.min(params.probeWidth / 2, bounds.width / 4);
                    oRoi.grow(halfWidth, params.maxThickness);

                    PointsCollector collector = new PointsCollector(orientation.absolute(oRoi));
                    section.cumulate(collector);

                    int startThickness = (int) Math.rint((double) collector.getSize() / oRoi.width);

                    // Stop side
                    oRoi.translate(stopCoord - startCoord, stopPos - startPos);
                    collector = new PointsCollector(orientation.absolute(oRoi));
                    section.cumulate(collector);

                    int stopThickness = (int) Math.rint((double) collector.getSize() / oRoi.width);

                    section.setFat(
                            (startThickness > params.maxThickness)
                            || (stopThickness > params.maxThickness));
                } else {
                    section.setFat(bounds.height > params.maxThickness);
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
     * Aggregate the long and thin sections into filaments.
     *
     * @param source the collection of input sections
     * @return the collection of retrieved filaments
     */
    public List<Glyph> retrieveFilaments (Collection<Section> source)
    {
        StopWatch watch = new StopWatch("FilamentsFactory " + orientation);
        List<Glyph> filaments = new ArrayList<Glyph>();

        try {
            // Create a filament for each section long & slim
            watch.start("createInitialFilaments");
            createInitialFilaments(filaments, source);
            logger.debug("{} filaments created.", filaments.size());

            // Merge filaments into larger ones
            watch.start("mergeFilaments");
            mergeFilaments(filaments);

            // Expand with short sections left over
            watch.start("expandFilaments");
            expandFilaments(filaments, source);

            // Merge filaments into larger filaments
            watch.start("mergeFilaments #2");
            mergeFilaments(filaments);

            // Re-register every filament with its (updated) signature
            return reRegisterFilaments(filaments);
        } catch (Exception ex) {
            logger.warn("FilamentsFactory cannot retrieveFilaments", ex);

            return null;
        } finally {
            if (constants.printWatch.getValue()) {
                watch.print();
            }
        }
    }

    //----------------------//
    // retrieveLineFilament //
    //----------------------//
    /**
     * Aggregate sections into one filament along the provided skeleton line.
     *
     * @param source the collection of candidate input sections
     * @param line   the skeleton line
     * @return the retrieved filament, or null
     */
    public Glyph retrieveLineFilament (Collection<Section> source,
                                       Line line)
    {
        StopWatch watch = new StopWatch("retrieveLineFilament " + orientation);

        try {
            // Aggregate long sections onto provided lines
            watch.start("populateLines");

            Glyph fil = populateLine(source, line);

            if (fil == null) {
                return null;
            }

            // Expand with short sections left over
            watch.start("expandFilaments");
            expandFilaments(Arrays.asList(fil), source);

            // (Re)register filament with its (final) signature
            return reRegisterFilaments(Arrays.asList(fil)).get(0);
        } catch (Exception ex) {
            logger.warn("FilamentsFactory cannot retrieveLineFilament", ex);

            return null;
        } finally {
            if (constants.printWatch.getValue()) {
                watch.print();
            }
        }
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

    //    //------------------------//
    //    // setMaxSectionThickness //
    //    //------------------------//
    //    public void setMaxSectionThickness (Scale.LineFraction lineFrac)
    //    {
    //        params.maxSectionThickness = scale.toPixels(lineFrac);
    //    }
    //
    //    //------------------------//
    //    // setMaxSectionThickness //
    //    //------------------------//
    //    public void setMaxSectionThickness (Scale.Fraction frac)
    //    {
    //        params.maxSectionThickness = scale.toPixels(frac);
    //    }
    //--------------------//
    // setMaxOverlapSpace //
    //--------------------//
    public void setMaxOverlapSpace (Scale.LineFraction lfrac)
    {
        params.maxOverlapSpace = scale.toPixels(lfrac);
    }

    //--------------------//
    // setMaxOverlapSpace //
    //--------------------//
    public void setMaxOverlapSpace (Scale.Fraction frac)
    {
        params.maxOverlapSpace = scale.toPixels(frac);
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

    //-----------------//
    // setMaxThickness //
    //-----------------//
    public void setMaxThickness (Scale.LineFraction lineFrac)
    {
        params.maxThickness = scale.toPixels(lineFrac);
    }

    //-----------------//
    // setMaxThickness //
    //-----------------//
    public void setMaxThickness (Scale.Fraction frac)
    {
        params.maxThickness = scale.toPixels(frac);
    }

    //-----------------//
    // setMaxThickness //
    //-----------------//
    public void setMaxThickness (int value)
    {
        params.maxThickness = value;
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
            Point2D oneStart = orientation.oriented(one.getStartPoint(orientation));
            Point2D oneStop = orientation.oriented(one.getStopPoint(orientation));
            Point2D twoStart = orientation.oriented(two.getStartPoint(orientation));
            Point2D twoStop = orientation.oriented(two.getStopPoint(orientation));

            // coord gap?
            double overlapStart = Math.max(oneStart.getX(), twoStart.getX());
            double overlapStop = Math.min(oneStop.getX(), twoStop.getX());
            double coordGap = (overlapStart - overlapStop) - 1;

            if (coordGap > params.maxCoordGap) {
                if (logger.isDebugEnabled() || areVips) {
                    logger.info("{}Gap too long: {} vs {}", vips, coordGap, params.maxCoordGap);
                }

                return false;
            }

            // pos gap?
            if (coordGap < 0) {
                // There is an overlap between the two filaments
                // Determine maximum consistent resulting thickness
                double maxConsistentThickness = maxConsistentThickness(one);
                double maxSpace = expanding ? params.maxExpansionSpace : params.maxOverlapSpace;

                // Measure thickness at various coord values of overlap
                // Provided that the overlap is long enough
                int valNb = (int) Math.min(3, 1 - (coordGap / 10));

                for (int iq = 1; iq <= valNb; iq++) {
                    double midCoord = overlapStart - ((iq * coordGap) / (valNb + 1));
                    double onePos = one.getPositionAt(midCoord, orientation);
                    double twoPos = two.getPositionAt(midCoord, orientation);
                    double posGap = Math.abs(onePos - twoPos);

                    if (posGap > params.maxOverlapDeltaPos) {
                        if (logger.isDebugEnabled() || areVips) {
                            logger.info(
                                    "{}Delta pos too high for overlap: {} vs {}",
                                    vips,
                                    String.format("%.2f", posGap),
                                    params.maxOverlapDeltaPos);
                        }

                        return false;
                    }

                    // Check resulting thickness at middle of overlap
                    double thickness = Glyphs.getThicknessAt(midCoord, orientation, one, two);

                    if (thickness > params.maxThickness) {
                        if (logger.isDebugEnabled() || areVips) {
                            logger.info(
                                    "{}Too thick: {} vs {} {} {}",
                                    vips,
                                    (float) thickness,
                                    params.maxThickness,
                                    one,
                                    two);
                        }

                        return false;
                    }

                    // Check thickness consistency
                    if ((-coordGap <= params.maxInvolvingLength)
                        && (thickness > maxConsistentThickness)) {
                        if (logger.isDebugEnabled() || areVips) {
                            logger.info(
                                    "{}Non consistent thickness: {} vs {} {} {}",
                                    vips,
                                    (float) thickness,
                                    (float) maxConsistentThickness,
                                    one,
                                    two);
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
                                    vips,
                                    (float) space,
                                    maxSpace,
                                    one,
                                    two);
                        }

                        return false;
                    } else if (expanding && (maxSpace == 0)) {
                        // Check there is a real contact between filament (one) and section (two)
                        if (!contact(one, two.getFirstSection())) {
                            if (logger.isDebugEnabled() || areVips) {
                                logger.info("{}No contact {} {}", vips, one, two);
                            }

                            return false;
                        }
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
                double oneThickness = one.getWeight() / one.getLength(orientation);
                double twoThickness = two.getWeight() / two.getLength(orientation);
                int posMargin = (int) Math.rint(Math.max(oneThickness, twoThickness) / 2);
                double posGap = Math.abs(stop.getY() - start.getY()) - posMargin;

                if (posGap > params.maxPosGap) {
                    if (logger.isDebugEnabled() || areVips) {
                        logger.info(
                                "{}Delta pos too high for gap: {} vs {}",
                                vips,
                                (float) posGap,
                                params.maxPosGap);
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
                                    vips,
                                    (float) gapSlope,
                                    params.maxGapSlope);
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

    //---------//
    // contact //
    //---------//
    /**
     * Check whether there is contact between provided glyph and section.
     *
     * @param glyph   provided filament
     * @param section section to check for contact with glyph
     * @return true if contact
     */
    private boolean contact (Glyph glyph,
                             Section section)
    {
        for (Section s : glyph.getMembers()) {
            if (s.touches(section)) {
                return true;
            }
        }

        return false;
    }

    //----------------//
    // createFilament //
    //----------------//
    private Glyph createFilament (Section section)
    {
        try {
            final Glyph fil = (Glyph) glyphConstructor.newInstance(new Object[]{scale, layer});

            if (section != null) {
                fil.addSection(section, GlyphComposition.Linking.NO_LINK);
                section.setProcessed(true);

                if (constants.registerEachAndEveryGlyph.isSet()) {
                    return nest.registerGlyph(fil); // Not really useful, even harmful if section is null
                }
            }

            return fil;
        } catch (Exception ex) {
            logger.error(null, ex);

            return null;
        }
    }

    //------------------------//
    // createInitialFilaments //
    //------------------------//
    /**
     * Create initial filaments, one per long input section.
     *
     * @param filaments     (output) list to be populated by created filaments
     * @param inputSections the collection of input sections
     */
    private void createInitialFilaments (List<Glyph> filaments,
                                         Collection<Section> inputSections)
            throws Exception
    {
        for (Section section : inputSections) {
            // Reset section cached data
            section.setProcessed(false);
            section.resetFat();

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
                if (section.isVip() || nest.isVip(fil)) {
                    fil.setVip();
                }
            }
        }

        logger.debug("createFilaments: {}/{}", filaments.size(), inputSections.size());
    }

    //-----------------//
    // expandFilaments //
    //-----------------//
    /**
     * Expand as much as possible the existing filaments with the provided sections.
     *
     * @param source the source of available sections
     * @return the collection of expanded filaments
     */
    private List<Glyph> expandFilaments (List<Glyph> filaments,
                                         Collection<Section> source)
    {
        try {
            // Sort sections by first position
            List<Section> sections = new ArrayList<Section>();

            for (Section section : source) {
                if (!section.isProcessed() && !isSectionFat(section)) {
                    sections.add(section);
                }
            }

            logger.debug("expandFilaments: {}/{}", sections.size(), source.size());

            Collections.sort(sections, Section.posComparator);

            // We allocate one glyph per candidate section
            // (simply to be able to reuse the canMerge() method !!!!!!!)
            List<Glyph> sectionGlyphs = new ArrayList<Glyph>(sections.size());

            for (Section section : sections) {
                Glyph sectionGlyph = createFilament(section);
                sectionGlyphs.add(sectionGlyph);

                if (section.isVip() || nest.isVip(sectionGlyph)) {
                    logger.info("VIP created {} from {}", sectionGlyph, section);
                    sectionGlyph.setVip();
                }
            }

            // List of filaments, sorted by decreasing length
            Collections.sort(filaments, Glyphs.byReverseLength(orientation));

            // Process each filament on turn
            for (Glyph fil : filaments) {
                // Build filament fat box
                final Rectangle filBounds = orientation.oriented(fil.getBounds());
                filBounds.grow(params.maxCoordGap, params.maxPosGap);

                boolean expanding = true;

                do {
                    expanding = false;

                    for (Iterator<Glyph> it = sectionGlyphs.iterator(); it.hasNext();) {
                        Glyph sectionGlyph = it.next();
                        Rectangle glyphBounds = orientation.oriented(sectionGlyph.getBounds());

                        if (filBounds.intersects(glyphBounds)) {
                            // Check more closely
                            if (canMerge(fil, sectionGlyph, true)) {
                                if (logger.isDebugEnabled() || fil.isVip() || sectionGlyph.isVip()) {
                                    logger.info(
                                            "VIP merging {} w/ {}",
                                            fil,
                                            Sections.toString(sectionGlyph.getMembers()));

                                    if (sectionGlyph.isVip()) {
                                        fil.setVip();
                                    }
                                }

                                fil.stealSections(sectionGlyph);
                                it.remove();
                                expanding = true;

                                break;
                            }
                        } else {
                            if (fil.isVip() && sectionGlyph.isVip()) {
                                logger.info("No intersection between {} and {}", fil, sectionGlyph);
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
     * Aggregate filaments into longer ones.
     */
    private void mergeFilaments (List<Glyph> filaments)
    {
        Collections.sort(filaments, Glyphs.byReverseLength(orientation));

        // Browse by decreasing filament length
        for (Glyph current : filaments) {
            Glyph candidate = current;

            // Keep on working while we do have a candidate to check for merge
            CandidateLoop:
            while (true) {
                final Rectangle candidateBounds = orientation.oriented(candidate.getBounds());
                candidateBounds.grow(params.maxCoordGap, params.maxPosGap);

                // Check the candidate vs all filaments until current excluded
                HeadsLoop:
                for (Glyph head : filaments) {
                    if (head == current) {
                        break CandidateLoop; // Actual end of sub-list
                    }

                    if ((head != candidate) && (head.getPartOf() == null)) {
                        Rectangle headBounds = orientation.oriented(head.getBounds());

                        if (headBounds.intersects(candidateBounds)) {
                            // Check for a possible merge
                            if (canMerge(head, candidate, false)) {
                                if (logger.isDebugEnabled() || head.isVip() || candidate.isVip()) {
                                    logger.info("VIP merged {} into {}", candidate, head);

                                    if (candidate.isVip()) {
                                        head.setVip();
                                    }
                                }

                                head.stealSections(candidate);
                                candidate = head; // This is the new candidate

                                break HeadsLoop;
                            }
                        } else {
                            if (head.isVip() && candidate.isVip()) {
                                logger.info(
                                        "VIP no fat intersection between {} and {}",
                                        candidate,
                                        head);
                            }
                        }
                    }
                }
            }
        }

        // Discard the merged filaments
        removeMergedFilaments(filaments);
    }

    //--------------//
    // populateLine //
    //--------------//
    /**
     * Use the long source sections to stick to the provided skeleton line and return
     * the resulting filament.
     *
     * @param source the input sections
     * @param lines  the imposed skeleton lines
     */
    private Glyph populateLine (Collection<Section> source,
                                Line line)
    {
        List<Section> longSections = new ArrayList<Section>();

        for (Section section : source) {
            section.setProcessed(false);
            section.resetFat(); // ????????????

            // Limit to main sections
            if (section.getLength(orientation) < params.minCoreSectionLength) {
                if (section.isVip()) {
                    logger.info("Too short {}", section);
                }
            } else {
                longSections.add(section);
            }
        }

        // Sort sections by decreasing length
        Collections.sort(longSections, Sections.byReverseLength(orientation));

        Glyph fil = createFilament(null);
        final Rectangle lineBox = orientation.oriented(line.getBounds());
        lineBox.grow(params.maxCoordGap, params.maxPosGap);

        for (Section section : longSections) {
            if (section.isProcessed()) {
                continue;
            }

            Rectangle sectionBox = orientation.oriented(section.getBounds());

            if (sectionBox.intersects(lineBox)) {
                Point centroid = section.getCentroid();

                // Closer look: check distance to line
                double gap = (orientation == HORIZONTAL) ? (line.yAtXExt(centroid.x) - centroid.y)
                        : (line.xAtYExt(centroid.y) - centroid.x);

                if (Math.abs(gap) <= params.maxPosGap) {
                    fil.addSection(section, GlyphComposition.Linking.NO_LINK);
                    section.setProcessed(true);
                }
            }
        }

        if (!fil.getMembers().isEmpty()) {
            if (constants.registerEachAndEveryGlyph.isSet()) {
                return nest.registerGlyph(fil); // Not really useful
            } else {
                return fil;
            }
        } else {
            return null;
        }
    }

    //---------------------//
    // reRegisterFilaments //
    //---------------------//
    private List<Glyph> reRegisterFilaments (List<Glyph> filaments)
    {
        List<Glyph> updated = new ArrayList<Glyph>(filaments.size());

        for (Glyph fil : filaments) {
            //            if (fil.isVip()) {
            //                logger.warn("About to re-register {}", fil);
            //            }
            Glyph regGlyph = nest.registerGlyph(fil); // Really useful
            regGlyph.linkAllSections();
            updated.add(regGlyph);
        }

        return updated;
    }

    //-----------------------//
    // removeMergedFilaments //
    //-----------------------//
    private void removeMergedFilaments (List<Glyph> filaments)
    {
        for (Iterator<Glyph> it = filaments.iterator(); it.hasNext();) {
            Glyph fil = it.next();

            if (fil.getPartOf() != null) {
                it.remove();
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Constant.Boolean registerEachAndEveryGlyph = new Constant.Boolean(
                false,
                "(Debug) should we register each and every glyph?");

        Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        final Constant.Boolean printParameters = new Constant.Boolean(
                false,
                "Should we print out the factory parameters?");

        Constant.Double maxGapSlope = new Constant.Double(
                "tangent",
                0.5,
                "Maximum absolute slope for a gap");

        Constant.Ratio minSectionAspect = new Constant.Ratio(
                3,
                "Minimum section aspect (length / thickness)");

        Constant.Ratio maxConsistentRatio = new Constant.Ratio(
                1.7,
                "Maximum thickness ratio for consistent merge");

        // Constants specified WRT mean line thickness
        // -------------------------------------------
        //
        Scale.LineFraction maxFilamentThickness = new Scale.LineFraction(
                1.5,
                "Maximum filament thickness WRT mean line height");

        Scale.LineFraction maxSectionThickness = new Scale.LineFraction(
                1.5,
                "Maximum section thickness WRT mean line height");

        Scale.LineFraction maxPosGap = new Scale.LineFraction(
                0.75,
                "Maximum delta position for a gap between filaments");

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

        Scale.Fraction maxOverlapSpace = new Scale.Fraction(
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
        //~ Instance fields ------------------------------------------------------------------------

        /** Maximum thickness for filaments */
        public int maxThickness;

        /** Minimum length for core sections */
        public int minCoreSectionLength;

        /** Maximum delta coordinate for real gap */
        public int maxCoordGap;

        /** Maximum delta position for real gaps */
        public int maxPosGap;

        /** Maximum delta position between overlapping filaments */
        public int maxOverlapDeltaPos;

        /** Maximum space between overlapping filaments */
        public int maxOverlapSpace;

        /** Maximum space for expansion */
        public int maxExpansionSpace;

        /** Maximum filament length to apply thickness test */
        public int maxInvolvingLength;

        /** Maximum dy for slope check on real gap */
        public int maxPosGapForSlope;

        /** Minimum aspect for sections */
        public double minSectionAspect;

        /** Maximum slope for real gaps */
        public double maxGapSlope;

        /** Probe width */
        public int probeWidth;

        //~ Methods --------------------------------------------------------------------------------
        public void dump (String title)
        {
            Main.dumping.dump(this, title);
        }

        /**
         * Initialize with default values
         */
        public void initialize ()
        {
            setMaxThickness(constants.maxFilamentThickness);
            setMinCoreSectionLength(constants.minCoreSectionLength);
            setMaxCoordGap(constants.maxCoordGap);
            setMaxPosGap(constants.maxPosGap);
            setMaxOverlapSpace(constants.maxOverlapSpace);
            setMaxExpansionSpace(constants.maxExpansionSpace);
            setMaxInvolvingLength(constants.maxInvolvingLength);
            setMaxPosGapForSlope(constants.maxPosGapForSlope);
            setMaxOverlapDeltaPos(constants.maxOverlapDeltaPos);
            setMaxGapSlope(constants.maxGapSlope.getValue());
            setMinSectionAspect(constants.minSectionAspect.getValue());

            probeWidth = scale.toPixels(BasicAlignment.getProbeWidth());

            if (logger.isDebugEnabled()) {
                dump(null);
            }
        }
    }
}
