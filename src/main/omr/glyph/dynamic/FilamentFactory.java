//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 F i l a m e n t F a c t o r y                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.dynamic;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.lag.Section;

import omr.math.GeoUtil;
import omr.math.Line;
import omr.math.LineUtil;
import omr.math.PointsCollector;

import omr.run.Orientation;
import static omr.run.Orientation.*;

import omr.sheet.Scale;
import omr.sheet.grid.BarFilamentFactory;
import omr.sheet.grid.StaffFilament;

import omr.util.Dumping;
import omr.util.Entities;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class {@code FilamentFactory} builds filaments (long series of sections) out of a
 * collection of sections.
 * <p>
 * These filaments are meant to represent good candidates for (horizontal) staff lines and ledgers
 * or (vertical) stems and ending legs, for which lines have to be discovered and built.
 * For bar lines candidates, a different {@link BarFilamentFactory} class is used because in that
 * case the bar core rectangle is a very strong guide.
 * <p>
 * The factory aims at a given filaments orientation, though the collection of input sections may
 * exhibit mixed orientations.
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
 * @param <F> precise filament type
 *
 * @author Hervé Bitteur
 */
public class FilamentFactory<F extends Filament>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(FilamentFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Related scale. */
    private final Scale scale;

    /** Where filaments are to be stored. */
    private final FilamentIndex index;

    /** Factory orientation. */
    private final Orientation orientation;

    /** Precise constructor for filaments. */
    private Constructor<?> filamentConstructor;

    /** Scale-dependent constants. */
    private final Parameters params;

    /** Processed sections. true/false */
    private final Set<Section> processedSections = new HashSet<Section>();

    /** Fat sections. unknown/true/false */
    private final Map<Section, Boolean> fatSections = new HashMap<Section, Boolean>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a factory of filaments.
     *
     * @param scale         the related scale
     * @param index         the index to host created filaments
     * @param orientation   the target orientation
     * @param filamentClass precise class to be used for filament creation,
     *                      typically {@link StraightFilament} (for straight lines)
     *                      or {@link CurvedFilament} / {@link StaffFilament} (for wavy lines)
     */
    public FilamentFactory (Scale scale,
                            FilamentIndex index,
                            Orientation orientation,
                            Class<? extends Filament> filamentClass)
    {
        this.scale = scale;
        this.index = index;
        this.orientation = orientation;

        try {
            filamentConstructor = filamentClass.getConstructor(int.class);
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
        final Boolean fat = fatSections.get(section);

        if (fat != null) {
            return fat;
        }

        try {
            if (section.getMeanThickness(orientation) <= 1) {
                return setFat(section, false);
            }

            // Check global slimness
            if (section.getMeanAspect(orientation) < params.minSectionAspect) {
                return setFat(section, true);
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

                return setFat(
                        section,
                        (startThickness > params.maxThickness)
                        || (stopThickness > params.maxThickness));
            } else {
                return setFat(section, bounds.height > params.maxThickness);
            }
        } catch (Exception ex) {
            logger.warn("Error in checking fatness of " + section, ex);

            return setFat(section, true);
        }
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
    public List<F> retrieveFilaments (Collection<Section> source)
    {
        StopWatch watch = new StopWatch("FilamentsFactory " + orientation);
        List<F> filaments = new ArrayList<F>();

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

            return filaments;
        } catch (Exception ex) {
            logger.warn("FilamentsFactory cannot retrieveFilaments", ex);

            return null;
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //----------------------//
    // retrieveLineFilament //
    //----------------------//
    /**
     * Aggregate sections into one filament along the provided skeleton line.
     * <p>
     * This method is used to retrieve the underlying glyph of a bar line peak.
     * It allows to detect if a bar goes beyond staff height.
     * It also allows to evaluate glyph straightness and thus discard peaks due to braces.
     * <p>
     * Perhaps we could also use the abscissa range of the peak rectangle?
     *
     * @param source the collection of candidate input sections
     * @param line   the skeleton line
     * @return the retrieved filament, or null
     */
    public Filament retrieveLineFilament (Collection<Section> source,
                                          Line line)
    {
        StopWatch watch = new StopWatch("retrieveLineFilament " + orientation);

        try {
            // Aggregate long sections that intersect line core onto skeleton line
            watch.start("populateLines");

            F fil = populateLine(source, line);

            if (fil == null) {
                return null;
            }

            // Expand with short sections left over, when they touch already included ones
            watch.start("expandFilaments");
            expandFilaments(Arrays.asList(fil), source);

            return fil;
        } catch (Exception ex) {
            logger.warn("FilamentsFactory cannot retrieveLineFilament", ex);

            return null;
        } finally {
            if (constants.printWatch.isSet()) {
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
    private boolean canMerge (Filament one,
                              Filament two,
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
            Point2D oneStart = orientation.oriented(one.getStartPoint());
            Point2D oneStop = orientation.oriented(one.getStopPoint());
            Point2D twoStart = orientation.oriented(two.getStartPoint());
            Point2D twoStop = orientation.oriented(two.getStopPoint());

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
                    double thickness = Compounds.getThicknessAt(
                            midCoord,
                            orientation,
                            scale,
                            one,
                            two);

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
                                   - (Compounds.getThicknessAt(midCoord, orientation, scale, one)
                                      + Compounds.getThicknessAt(midCoord, orientation, scale, two));

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
                Point2D gapStart;
                Point2D gapStop;

                if (oneStart.getX() < twoStart.getX()) {
                    // one - two
                    gapStart = oneStop;
                    gapStop = twoStart;
                } else {
                    // two - one
                    gapStart = twoStop;
                    gapStop = oneStart;
                }

                // Compute position gap, taking thickness into account
                double oneThickness = one.getWeight() / one.getLength(orientation);
                double twoThickness = two.getWeight() / two.getLength(orientation);
                int posMargin = (int) Math.rint(Math.max(oneThickness, twoThickness) / 2);
                double posGap = Math.abs(gapStop.getY() - gapStart.getY()) - posMargin;

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

                // Check gap slope (relevant only for significant dy)
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

            // Check slope compatibility for filaments of significant lengths
            double oneLength = oneStop.getX() - oneStart.getX() + 1;
            double twoLength = twoStop.getX() - twoStart.getX() + 1;

            if ((oneLength >= params.minLengthForDeltaSlope)
                && (twoLength >= params.minLengthForDeltaSlope)) {
                double oneSlope = LineUtil.getSlope(oneStart, oneStop);
                double twoSlope = LineUtil.getSlope(twoStart, twoStop);
                double deltaSlope = Math.abs(twoSlope - oneSlope);

                if (deltaSlope > params.maxDeltaSlope) {
                    if (logger.isDebugEnabled() || areVips) {
                        logger.info(
                                "{}DeltaSlope too high: {} vs {}",
                                vips,
                                (float) deltaSlope,
                                params.maxDeltaSlope);
                    }

                    return false;
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
     * Check whether there is contact between provided filament and section.
     *
     * @param filament provided filament
     * @param section  section to check for contact with
     * @return true if contact
     */
    private boolean contact (Filament filament,
                             Section section)
    {
        for (Section s : filament.getMembers()) {
            if (s.touches(section)) {
                return true;
            }
        }

        return false;
    }

    //----------------//
    // createFilament //
    //----------------//
    private F createFilament (Section section)
    {
        try {
            final F fil = (F) filamentConstructor.newInstance(
                    new Object[]{scale.getInterline()});

            if (section != null) {
                fil.addSection(section);
                setProcessed(section);

                index.register(fil);
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
    private void createInitialFilaments (List<F> filaments,
                                         Collection<Section> inputSections)
            throws Exception
    {
        for (Section section : inputSections) {
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

            F fil = createFilament(section);
            filaments.add(fil);

            if (logger.isDebugEnabled() || section.isVip() || index.isVipId(fil.getId())) {
                if (section.isVip() || index.isVipId(fil.getId())) {
                    fil.setVip(true);
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
    private List<F> expandFilaments (List<F> filaments,
                                     Collection<Section> source)
    {
        try {
            // Sort sections by first position
            List<Section> sections = new ArrayList<Section>();

            for (Section section : source) {
                if (!isProcessed(section) && !isSectionFat(section)) {
                    sections.add(section);
                }
            }

            logger.debug("expandFilaments: {}/{}", sections.size(), source.size());

            Collections.sort(sections, Section.byPosition);

            // We allocate one glyph per candidate section
            // (simply to be able to reuse the canMerge() method !!!!!!!)
            List<Filament> sectionGlyphs = new ArrayList<Filament>(sections.size());

            for (Section section : sections) {
                Filament sectionFil = createFilament(section);
                sectionGlyphs.add(sectionFil);

                if (section.isVip() || index.isVipId(sectionFil.getId())) {
                    logger.info("VIP created {} from {}", sectionFil, section);
                    sectionFil.setVip(true);
                }
            }

            // List of filaments, sorted by decreasing length
            Collections.sort(filaments, Compounds.byReverseLength(orientation));

            // Process each filament on turn
            for (Filament fil : filaments) {
                // Build filament fat box
                final Rectangle filBounds = orientation.oriented(fil.getBounds());
                filBounds.grow(params.maxCoordGap, params.maxPosGap);

                boolean expanding;

                do {
                    expanding = false;

                    for (Iterator<Filament> it = sectionGlyphs.iterator(); it.hasNext();) {
                        Filament sectionFil = it.next();
                        Rectangle glyphBounds = orientation.oriented(sectionFil.getBounds());

                        if (filBounds.intersects(glyphBounds)) {
                            // Check more closely
                            if (canMerge(fil, sectionFil, true)) {
                                if (logger.isDebugEnabled() || fil.isVip() || sectionFil.isVip()) {
                                    logger.info(
                                            "VIP merging {} w/ sections{}",
                                            fil,
                                            Entities.ids(sectionFil.getMembers()));

                                    if (sectionFil.isVip()) {
                                        fil.setVip(true);
                                    }
                                }

                                fil.stealSections(sectionFil);
                                it.remove();
                                expanding = true;

                                break;
                            }
                        } else if (fil.isVip() && sectionFil.isVip()) {
                            logger.info("No intersection between {} and {}", fil, sectionFil);
                        }
                    }
                } while (expanding);
            }
        } catch (Exception ex) {
            logger.warn("FilamentsFactory cannot expandFilaments", ex);
        }

        return filaments;
    }

    //-------------//
    // isProcessed //
    //-------------//
    private boolean isProcessed (Section section)
    {
        return processedSections.contains(section);
    }

    //------------------------//
    // maxConsistentThickness //
    //------------------------//
    private double maxConsistentThickness (Filament stick)
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
    private void mergeFilaments (List<F> filaments)
    {
        Collections.sort(filaments, Compounds.byReverseLength(orientation));

        // Browse by decreasing filament length
        for (Filament current : filaments) {
            Filament candidate = current;

            // Keep on working while we do have a candidate to check for merge
            CandidateLoop:
            while (true) {
                final Rectangle candidateBounds = orientation.oriented(candidate.getBounds());
                candidateBounds.grow(params.maxCoordGap, params.maxPosGap);

                // Check the candidate vs all filaments until current excluded
                HeadsLoop:
                for (Filament head : filaments) {
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
                                        head.setVip(true);
                                    }
                                }

                                head.stealSections(candidate);
                                candidate = head; // This is the new candidate

                                break HeadsLoop;
                            }
                        } else if (head.isVip() && candidate.isVip()) {
                            logger.info(
                                    "VIP no fat intersection between {} and {}",
                                    candidate,
                                    head);
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
     * <p>
     * Strategy: We use only the long sections that intersect line core and are close enough to the
     * target line.
     *
     * @param source the input sections
     * @param lines  the imposed skeleton lines
     */
    private F populateLine (Collection<Section> source,
                            Line line)
    {
        Rectangle lineRect = orientation.oriented(line.getBounds());
        F fil = createFilament(null);

        for (Section section : source) {
            Rectangle sectRect = orientation.oriented(section.getBounds());

            if (sectRect.width < params.minCoreSectionLength) {
                if (section.isVip()) {
                    logger.info("Too short {}", section);
                }
            } else {
                int overlap = GeoUtil.xOverlap(lineRect, sectRect);

                if (overlap <= 0) {
                    if (section.isVip()) {
                        logger.info("Not in core {}", section);
                    }
                } else {
                    Point centroid = section.getCentroid();
                    double gap = (orientation == HORIZONTAL)
                            ? (line.yAtXExt(centroid.x) - centroid.y)
                            : (line.xAtYExt(centroid.y) - centroid.x);

                    if (Math.abs(gap) <= params.maxPosGap) {
                        fil.addSection(section);
                        setProcessed(section);
                    }
                }
            }
        }

        if (!fil.getMembers().isEmpty()) {
            return fil;
        } else {
            return null;
        }
    }

    //-----------------------//
    // removeMergedFilaments //
    //-----------------------//
    private void removeMergedFilaments (List<F> filaments)
    {
        for (Iterator<F> it = filaments.iterator(); it.hasNext();) {
            Filament fil = it.next();

            if (fil.getPartOf() != null) {
                it.remove();
            }
        }
    }

    //--------//
    // setFat //
    //--------//
    private boolean setFat (Section section,
                            boolean bool)
    {
        fatSections.put(section, bool);

        return bool;
    }

    //--------------//
    // setProcessed //
    //--------------//
    private void setProcessed (Section section)
    {
        processedSections.add(section);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Boolean printParameters = new Constant.Boolean(
                false,
                "Should we print out the factory parameters?");

        private final Constant.Double maxGapSlope = new Constant.Double(
                "tangent",
                0.5,
                "Maximum absolute slope for a gap");

        private final Constant.Ratio minSectionAspect = new Constant.Ratio(
                3,
                "Minimum section aspect (length / thickness)");

        private final Constant.Ratio maxConsistentRatio = new Constant.Ratio(
                1.7,
                "Maximum thickness ratio for consistent merge");

        private final Constant.Ratio maxDeltaSlope = new Constant.Ratio(
                0.01,
                "Maximum slope difference between long filaments");

        // Constants specified WRT mean line thickness
        // -------------------------------------------
        //
        private final Scale.LineFraction maxFilamentThickness = new Scale.LineFraction(
                1.5,
                "Maximum filament thickness WRT mean line height");

        private final Scale.LineFraction maxPosGap = new Scale.LineFraction(
                0.75,
                "Maximum delta position for a gap between filaments");

        // Constants specified WRT mean interline
        // --------------------------------------
        //
        private final Scale.Fraction minCoreSectionLength = new Scale.Fraction(
                1,
                "Minimum length for a section to be considered as core");

        private final Scale.Fraction maxOverlapDeltaPos = new Scale.Fraction(
                0.5,
                "Maximum delta position between two overlapping filaments");

        private final Scale.Fraction maxCoordGap = new Scale.Fraction(
                1,
                "Maximum delta coordinate for a gap between filaments");

        private final Scale.Fraction maxOverlapSpace = new Scale.Fraction(
                0.16,
                "Maximum space between overlapping filaments");

        private final Scale.Fraction maxExpansionSpace = new Scale.Fraction(
                0.02,
                "Maximum space when expanding filaments");

        private final Scale.Fraction maxPosGapForSlope = new Scale.Fraction(
                0.1,
                "Maximum delta Y to check slope for a gap between filaments");

        private final Scale.Fraction maxInvolvingLength = new Scale.Fraction(
                2,
                "Maximum filament length to apply thickness test");

        private final Scale.Fraction minLengthForDeltaSlope = new Scale.Fraction(
                10,
                "Minimum filament length to apply delta slope test");
    }

    //----------------//
    // DistantSection //
    //----------------//
    /**
     * Meant to ease sorting of sections according to their distance to line.
     */
    private static class DistantSection
            implements Comparable<DistantSection>
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Underlying section. */
        final Section section;

        /** Distance to line. */
        final double dist;

        //~ Constructors ---------------------------------------------------------------------------
        public DistantSection (Section section,
                               double dist)
        {
            this.section = section;
            this.dist = dist;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int compareTo (DistantSection that)
        {
            return Double.compare(dist, that.dist);
        }

        @Override
        public String toString ()
        {
            return dist + "/" + section;
        }
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

        public int minLengthForDeltaSlope;

        public double maxDeltaSlope;

        //~ Methods --------------------------------------------------------------------------------
        public void dump (String title)
        {
            new Dumping().dump(this, title);
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

            minLengthForDeltaSlope = scale.toPixels(constants.minLengthForDeltaSlope);
            maxDeltaSlope = constants.maxDeltaSlope.getValue();

            probeWidth = scale.toPixels(Filament.getProbeWidth());

            if (logger.isDebugEnabled()) {
                dump(null);
            }
        }
    }
}
