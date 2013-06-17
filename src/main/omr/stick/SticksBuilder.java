//----------------------------------------------------------------------------//
//                                                                            //
//                         S t i c k s B u i l d e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.stick;

import omr.Main;

import omr.check.FailureResult;
import omr.check.SuccessResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.Nest;
import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;

import omr.lag.BasicSection;
import omr.lag.Section;
import omr.lag.Sections;

import omr.run.Orientation;

import omr.sheet.Scale;
import static omr.stick.SectionRole.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class {@code SticksBuilder} introduces the scanning of a source of
 * sections, to retrieve sticks.
 *
 * <p> The same algorithms are used for all kinds of sticks with only one
 * difference, depending on length of sticks we are looking for, since long
 * alignments (this applies to staff lines only) can exhibit not very straight
 * lines as opposed to bar lines, stems or ledgers.
 *
 * <ul> <li> <b>Horizontal sticks</b> can be (chunks of) staff lines, alternate
 * ending, or ledgers. </li>
 *
 * <li> <b>Vertical sticks</b> can be bar lines, or stems. </li> </ul> </p>
 *
 * @author Hervé Bitteur
 */
public class SticksBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(SticksBuilder.class);

    /** Unique identifier for debugging */
    private static int globalId = 0;

    /** A too small stick */
    private static final FailureResult TOO_SMALL = new FailureResult(
            "SticksBuilder-TooSmall");

    /** A stick whose slope is not correct */
    private static final FailureResult NOT_STRAIGHT = new FailureResult(
            "SticksBuilder-NotStraight");

    /** A stick correctly assigned */
    private static final SuccessResult ASSIGNED = new SuccessResult(
            "SticksBuilder-Assigned");

    /** For comparing sections, according to the thickening relevance */
    private static final Comparator<Section> thickeningComparator = new Comparator<Section>()
    {
        @Override
        public int compare (Section s1,
                            Section s2)
        {
            // Criteria #1 is layer number (small layer numbers
            // first) Plus section thickness for non layer zero
            // sections
            int layerDiff;
            StickRelation r1 = s1.getRelation();
            StickRelation r2 = s2.getRelation();

            if ((r1.layer == 0) || (r2.layer == 0)) {
                layerDiff = r1.layer - r2.layer;
            } else {
                layerDiff = (r1.layer + s1.getRunCount())
                            - (r2.layer + s2.getRunCount());
            }

            if (layerDiff != 0) {
                return layerDiff;
            } else {
                // Criteria #2 is section length (large lengths
                // first)
                return s2.getMaxRunLength() - s1.getMaxRunLength();
            }
        }
    };

    //~ Instance fields --------------------------------------------------------
    /** Desired orientation of sticks */
    protected final Orientation orientation;

    /** The related scale */
    protected final Scale scale;

    /** The containing nest */
    protected final Nest nest;

    /** Class parameters */
    protected final Parameters params;

    /** The source adapter to retrieve sections from */
    protected final SectionsSource source;

    /** A flag used to trigger processing specific to very long (and not totally
     * straight) alignments */
    private final boolean longAlignment;

    /** The <b>sorted</b> collection of sticks found in this area */
    protected List<Glyph> sticks = new ArrayList<>();

    /** Sections which are potential future members */
    private List<Section> candidates = new ArrayList<>();

    /** Sections recognized as members of sticks */
    private List<Section> members = new ArrayList<>();

    /** Used to flag sections already visited wrt a given stick */
    private Map<Section, Glyph> visited;

    /** Instance data for the area */
    private int id = ++globalId;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new SticksBuilder object.
     *
     * @param orientation   general orientation of desired sticks
     * @param scale         the related scale
     * @param nest          the nest which hosts the glyphs
     * @param source        An adaptor to get access to participating sections
     * @param longAlignment specific flag to indicate long filament retrieval
     */
    public SticksBuilder (Orientation orientation,
                          Scale scale,
                          Nest nest,
                          SectionsSource source,
                          boolean longAlignment)
    {
        // Cache computing parameters
        this.orientation = orientation;
        this.scale = scale;
        this.nest = nest;
        this.source = source;
        this.longAlignment = longAlignment;

        // Default parameters values
        params = new Parameters();
        params.initialize();
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // canConnect //
    //------------//
    public boolean canConnect (Glyph one,
                               Glyph two,
                               int maxDeltaCoord,
                               int maxDeltaPos)
    {
        Point2D oneStart = orientation.oriented(one.getStartPoint(orientation));
        Point2D oneStop = orientation.oriented(one.getStopPoint(orientation));
        Point2D twoStart = orientation.oriented(two.getStartPoint(orientation));
        Point2D twoStop = orientation.oriented(two.getStopPoint(orientation));

        if (Math.abs(oneStop.getX() - twoStart.getX()) <= maxDeltaCoord) {
            // Case: this ... that
            if (Math.abs(twoStart.getY() - oneStop.getY()) <= maxDeltaPos) {
                return true;
            }
        }

        if (Math.abs(twoStop.getX() - oneStart.getX()) <= maxDeltaCoord) {
            // Case: that ... this
            if (Math.abs(twoStop.getY() - oneStart.getY()) <= maxDeltaPos) {
                return true;
            }
        }

        return false;
    }

    //--------------//
    // createSticks //
    //--------------//
    /**
     * Perform the initialization of the newly allocated instance. This
     * consists in scanning the given area, using the source provided. The
     * resulting sticks are available through the getSticks method.
     *
     * @param preCandidates (maybe null) a list of predefined candidate sections
     */
    public void createSticks (List<Section> preCandidates)
    {
        // Use a brand new glyph map
        visited = new HashMap<>();

        // Do we have pre-candidates to start with ?
        if (preCandidates != null) {
            candidates = preCandidates;
        } else {
            // Browse through our sections, to collect the long core sections
            List<Section> target;

            if (longAlignment) {
                // For long alignments (staff lines), we use an intermediate
                // list of candidates
                target = candidates;
            } else {
                // For short alignment (such as stems and others), we fill
                // members directly
                target = members;
            }

            while (source.hasNext()) {
                Section section = source.next();

                // By vertue of the Source adaptor, all provided sections are
                // entirely within the stick area. So tests for core sections
                // are not already taken, thickness and length, that's all.
                if (!section.isGlyphMember()
                    && (section.getRunCount() <= params.maxThickness)
                    && (section.getMaxRunLength() >= params.minCoreLength)) {
                    // OK, this section is candidate as core member of stick set
                    mark(section, target, SectionRole.CORE, 0, 0);
                }
            }
        }

        if (longAlignment) {
            // Get rid of candidates too far from the staff line core
            thickenAlignmentCore(candidates, members);
        }

        logger.debug("{}{}",
                members.size(), Sections.toString(" Core sections", members));

        // Collect candidate sections around the core ones
        for (Section section : members) {
            collectNeighborsOf(section, 1, -1, true); // Before the core section

            collectNeighborsOf(section, 1, +1, true); // After the core section
        }

        // Purge some internal sections
        ///purgeInternals(candidates);
        //
        if (longAlignment) {
            // Thicken the core sections with the candidates, still paying
            // attention to the resulting thickness of the sticks.
            thickenAlignmentCore(candidates, members);

            ///purgeInternals(members);
        } else {
            members.addAll(candidates);
        }

        logger.debug("{}{}",
                members.size(), Sections.toString(" total sections", members));

        // Aggregate member sections into as few sticks as possible.
        // This creates instances in sticks list.
        aggregateMemberSections();

        // Compute stick lines and check the resulting slope. This may result in
        // discarded sticks.
        for (Iterator<Glyph> it = sticks.iterator(); it.hasNext();) {
            Glyph stick = it.next();

            // Glyph size (at least 3 points)
            if (stick.getWeight() < 3) {
                stick.setResult(TOO_SMALL);

                for (Section section : stick.getMembers()) {
                    logger.debug("Discarding too small stick {}", section);
                    discard(section);
                }

                it.remove();
            } else {
                // Glyph slope must be close to expected value
                double stickSlope = (orientation == Orientation.VERTICAL)
                        ? stick.getInvertedSlope() : stick.getSlope();

                if (Math.abs(stickSlope - params.expectedSlope) > params.slopeMargin) {
                    stick.setResult(NOT_STRAIGHT);

                    for (Section section : stick.getMembers()) {
                        logger.debug("Discarding not straight stick {}", section);
                        discard(section);
                    }

                    it.remove();
                }
            }
        }

        //        if (logger.isDebugEnabled()) {
        //            dump(true);
        //        }
    }

    //------//
    // dump //
    //------//
    /**
     * A debugging routine, which dumps any pertinent info to standard output.
     *
     * @param withContent flag to specify that sticks are to be dumped with
     *                    their content
     */
    public void dump (boolean withContent)
    {
        params.dump();
        System.out.println("StickArea#" + id + " size=" + sticks.size());

        for (Glyph stick : sticks) {
            System.out.println(stick.dumpOf());
        }
    }

    //-----------//
    // getSticks //
    //-----------//
    /**
     * Returns the collection of sticks found in this area
     *
     * @return the sticks found
     */
    public List<Glyph> getSticks ()
    {
        return sticks;
    }

    //-------------//
    // isDiscarded //
    //-------------//
    /**
     * Checks whether a given section has been discarded
     *
     * @param section the section to check
     *
     * @return true if actually discarded
     */
    public static boolean isDiscarded (Section section)
    {
        StickRelation relation = section.getRelation();

        return (relation != null) && (relation.role == DISCARDED);
    }

    //-------//
    // reset //
    //-------//
    /**
     * Used to reset the ids of stick areas (for debugging mainly)
     */
    public static void reset ()
    {
        globalId = 0;
    }

    //----------------//
    // retrieveSticks //
    //----------------//
    /**
     * Perform the sticks retrieval (creation & merge) based on the parameters
     * defined for this area
     *
     * @return the sticks retrieved
     */
    public List<Glyph> retrieveSticks ()
    {
        // Retrieve the stick(s)
        createSticks(null);

        // Merge aligned verticals
        merge();

        // Sort sticks found
        Collections.sort(sticks, Glyph.byId);

        logger.debug("End of scanning area, found {} stick(s): {}",
                sticks.size(), Glyphs.toString(sticks));

        return sticks;
    }

    //------------------//
    // setExpectedSlope //
    //------------------//
    public void setExpectedSlope (double value)
    {
        params.expectedSlope = value;
    }

    //-----------------//
    // setMaxAdjacency //
    //-----------------//
    public void setMaxAdjacency (double value)
    {
        params.maxAdjacency = value;
    }

    //------------------//
    // setMaxDeltaCoord //
    //------------------//
    public void setMaxDeltaCoord (Scale.Fraction frac)
    {
        params.maxDeltaCoord = scale.toPixels(frac);
    }

    //----------------//
    // setMaxDeltaPos //
    //----------------//
    public void setMaxDeltaPos (Scale.Fraction frac)
    {
        params.maxDeltaPos = scale.toPixels(frac);
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
    public void setMaxThickness (Scale.LineFraction lFrac)
    {
        params.maxThickness = scale.toPixels(lFrac);
    }

    //------------------//
    // setMinCoreLength //
    //------------------//
    public void setMinCoreLength (Scale.Fraction frac)
    {
        setMinCoreLength(scale.toPixels(frac));
    }

    //------------------//
    // setMinCoreLength //
    //------------------//
    public void setMinCoreLength (int value)
    {
        params.minCoreLength = value;
    }

    //---------------------//
    // setMinSectionAspect //
    //---------------------//
    public void setMinSectionAspect (double value)
    {
        params.minSectionAspect = value;
    }

    //----------------//
    // setSlopeMargin //
    //----------------//
    public void setSlopeMargin (double value)
    {
        params.slopeMargin = value;
    }

    //-------//
    // merge //
    //-------//
    /**
     * Merge all sticks found in the area, provided they can be considered
     * extensions of one another, according to the current proximity parameters
     */
    protected void merge ()
    {
        final long startTime = System.currentTimeMillis();

        // Sort on stick mid position first
        Collections.sort(
                sticks,
                new Comparator<Glyph>()
        {
            @Override
            public int compare (Glyph s1,
                                Glyph s2)
            {
                return s1.getMidPos(orientation)
                       - s2.getMidPos(orientation);
            }
        });

        // Then use position to narrow the tests
        List<Glyph> removals = new ArrayList<>();
        int index = -1;

        for (Glyph stick : sticks) {
            index++;

            List<Glyph> tail = sticks.subList(index + 1, sticks.size());
            boolean merging = true;

            while (merging) {
                Rectangle stickBounds = orientation.oriented(
                        stick.getBounds());
                stickBounds.grow(params.maxDeltaCoord, params.maxDeltaPos);

                Rectangle stickBox = orientation.absolute(stickBounds);

                // final int maxPos = stick.getMidPos() + (20 * maxDeltaPos);
                merging = false;

                for (Iterator<Glyph> it = tail.iterator(); it.hasNext();) {
                    Glyph other = it.next();

                    // Check other has not been removed yet
                    if (removals.contains(other)) {
                        continue;
                    }

                    if (other.getBounds().intersects(stickBox)) {
                        if (canConnect(
                                stick,
                                other,
                                params.maxDeltaCoord,
                                params.maxDeltaPos)) {
                            int oldId = stick.getId();

                            stick.stealSections(other);
                            stick = nest.addGlyph(stick);

                            if (logger.isDebugEnabled()
                                && (stick.getId() != oldId)) {
                                logger.debug("Merged sticks #{} & #{} => #{}",
                                        oldId, other.getId(), stick.getId());
                            }

                            removals.add(other);
                            merging = true;

                            break;
                        }
                    }
                }
            }
        }

        sticks.removeAll(removals);

        logger.debug("merged {} sticks in {} ms",
                removals.size(), System.currentTimeMillis() - startTime);
    }

    //-----------//
    // aggregate //
    //-----------//
    /**
     * Try to aggregate this section (and its neighbors) to the stick
     *
     * @param section the section to aggregate
     * @param stick   the stick to which section is to be aggregated
     */
    private void aggregate (Section section,
                            Glyph stick)
    {
        if (visited.get(section) != stick) {
            visited.put(section, stick);

            if (section.isAggregable()) {
                // Check that resulting stick thickness would still be OK
                if (isClose(
                        stick.getMembers(),
                        section,
                        params.maxThickness + 1)) {
                    stick.addSection(section, Glyph.Linking.LINK_BACK);

                    // Aggregate recursively other sections
                    for (Section source : section.getSources()) {
                        aggregate(source, stick);
                    }

                    for (Section target : section.getTargets()) {
                        aggregate(target, stick);
                    }
                }
            }
        }
    }

    //-------------------------//
    // aggregateMemberSections //
    //-------------------------//
    /**
     * Aggregate member sections into sticks. We start with no stick at all,
     * then consider each member section on its turn. If the section is not
     * aggregated it begins a new stick. Otherwise we try to aggregate the
     * section to one of the sticks identified so far.
     */
    private void aggregateMemberSections ()
    {
        final int interline = scale.getInterline();

        for (Section section : members) {
            if (section.isAggregable()) {
                Glyph stick = new BasicGlyph(interline);
                stick.setResult(ASSIGNED); // Needed to flag the stick
                aggregate(section, stick);
                stick = nest.addGlyph(stick);
                stick.setResult(ASSIGNED); // In case we are reusing a glyph
                sticks.add(stick);
            }
        }
    }

    //--------------------//
    // collectNeighborsOf //
    //--------------------//
    /**
     * Look for neighbors of this section, in the given direction. If a
     * neighboring sections qualifies, it is assigned the provided layer level.
     *
     * @param section   the section from which neighbors are considered
     * @param layer     the current layer number of the section, regarding the
     *                  stick
     * @param direction the direction to look into, which is coded as
     * <b>+1</b> for looking at outgoing edges, and
     * <b>-1</b> for looking at incoming edges.
     */
    private void collectNeighborsOf (Section section,
                                     int layer,
                                     int direction,
                                     boolean internalAllowed)
    {
        if (direction > 0) {
            for (Section target : section.getTargets()) {
                collectSection(target, layer, direction, internalAllowed);
            }
        } else {
            for (Section source : section.getSources()) {
                collectSection(source, layer, direction, internalAllowed);
            }
        }
    }

    //----------------//
    // collectSection //
    //----------------//
    /**
     * Try to collect this section as a candidate.
     *
     * @param section         the condidate section
     * @param direction       which direction we are moving to
     * @param internalAllowed false if we are reaching the stick border
     */
    private void collectSection (Section section,
                                 int layer,
                                 int direction,
                                 boolean internalAllowed)
    {
        // We consider only free (not yet assigned) sections, which are
        // located within the given area.
        if (!isFree(section) || !source.isInArea(section)) {
            return;
        }

        // If the section being checked is already too thick compared with the
        // stick we are looking at, then we discard the section.
        if (isTooThick(section)) {
            // Mark the section, so that we don't retry the same one via another
            // path
            mark(section, null, TOO_THICK, layer, direction);

            return;
        }

        if (longAlignment) {
            // We don't collect sections that are too far from the center of
            // member sections, since this would result in a too thick line.
            if (!isClose(members, section, params.maxThickness + 1)) {
                mark(section, null, TOO_FAR, layer, direction);

                return;
            }
        }

        // Include only sections that are slim enough
        if ((section.getRunCount() > 1)
            && (section.getAspect(orientation) < params.minSectionAspect)) {
            mark(section, null, TOO_FAT, layer, direction);

            return;
        }

        // Check that section is adjacent to open space
        final double adjacency = (direction > 0) ? section.getLastAdjacency()
                : section.getFirstAdjacency();

        if (adjacency <= params.maxAdjacency) {
            mark(section, candidates, PERIPHERAL, layer, direction);
            ///collectNeighborsOf(section, layer + 1, direction, false);
            collectNeighborsOf(section, layer - 1, -direction, true);
        } else {
            if (internalAllowed) {
                mark(section, candidates, INTERNAL, layer, direction);
                collectNeighborsOf(section, layer + 1, direction, false);
            } else {
                mark(section, null, TOO_FAR, layer, direction);
            }
        }
    }

    //---------//
    // discard //
    //---------//
    /**
     * Flag a section (using its related data) as discarded.
     *
     * @param section the section to discard
     */
    private void discard (Section section)
    {
        StickRelation relation = section.getRelation();

        if ((relation != null) && relation.isCandidate()) {
            relation.role = DISCARDED;
            section.setGlyph(null);
        }
    }

    //---------//
    // isClose //
    //---------//
    /**
     * Check that the section would not thicken too much the stick being built,
     * and whose members are passed as parameter
     *
     * @param members      the members to compute distance to
     * @param section      the section to check
     * @param maxThickness the maximum resulting stick thickness
     *
     * @return true if OK
     */
    private boolean isClose (Collection<Section> members,
                             Section section,
                             int maxThickness)
    {
        // Just to speed up
        final int start = section.getStartCoord();
        final int stop = section.getStopCoord();
        final int firstPos = section.getFirstPos();
        final int lastPos = section.getLastPos();

        // Check real stick thickness so far
        for (Section sct : members) {
            // Check overlap in abscissa with section at hand
            if (Math.max(start, sct.getStartCoord()) <= Math.min(
                    stop,
                    sct.getStopCoord())) {
                // Check global thickness
                int thick;

                if (sct.getFirstPos() > firstPos) {
                    thick = sct.getLastPos() - firstPos + 1;
                } else {
                    thick = lastPos - sct.getFirstPos() + 1;
                }

                if (thick > maxThickness) {
                    logger.debug("Too thick real line ({}) {}", thick, section);
                    return false;
                }
            }
        }

        return true;
    }

    //--------//
    // isFree //
    //--------//
    private boolean isFree (Section section)
    {
        StickRelation relation = section.getRelation();

        return (relation == null) || (relation.role == null);
    }

    //------------//
    // isTooThick //
    //------------//
    /**
     * Check whether the given section is too thick (thicker than the allowed
     * maxThickness).
     *
     * @param section the section to check
     *
     * @return true if section is too thick
     */
    private boolean isTooThick (Section section)
    {
        return section.getRunCount() > params.maxThickness;
    }

    //------//
    // mark //
    //------//
    /**
     * Mark the section with the given flag, and insert it in the provided list
     * if any
     *
     * @param section   the section to mark
     * @param list      the list (if any) to add the section to
     * @param role      section role
     * @param layer     section layer in the stick
     * @param direction section direction in the stick
     */
    private void mark (Section section,
                       List<Section> list,
                       SectionRole role,
                       int layer,
                       int direction)
    {
        ((BasicSection) section).setParams(role, layer, direction);

        if (list != null) {
            list.add(section);
        }
    }

    //----------------------//
    // thickenAlignmentCore //
    //----------------------//
    /**
     * This routine is used only in the special case of long alignment, meaning
     * staff line detection. The routine uses and then clears the candidate
     * list. The sections that successfully pass the tests are added to the
     * members list.
     *
     * @param ins  input list of candidates (consumed)
     * @param outs output list of members (appended)
     */
    private void thickenAlignmentCore (List<Section> ins,
                                       List<Section> outs)
    {
        // Sort ins according to their relevance
        Collections.sort(ins, thickeningComparator);

        // Using the priority order, let's thicken the stick core
        for (Section section : ins) {
            if (!isDiscarded(section)) {
                if (isClose(outs, section, params.maxThickness)) {
                    // OK, it is now a member of the stick
                    outs.add(section);
                } else {
                    // Get rid of this one
                    discard(section);
                }
            }
        }

        // Get rid of the ins list
        ins.clear();
    }

    //~ Inner Classes ----------------------------------------------------------
    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all parameters for SticksBuilder
     */
    protected class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        /** Expected (oriented) slope for sticks */
        public double expectedSlope;

        /** Margin around expected slope */
        public double slopeMargin;

        /** Minimum value for length of core sections */
        public int minCoreLength;

        /** Maximum value for adjacency */
        public double maxAdjacency;

        /** Maximum thickness value for sticks */
        public int maxThickness;

        /** Minimum aspect (length / thickness) for a section */
        public double minSectionAspect;

        /** Maximum gap in coordinate when merging sticks */
        public int maxDeltaCoord;

        /** Maximum gap in position when merging sticks */
        public int maxDeltaPos;

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
            setExpectedSlope(0d);
            setSlopeMargin(constants.slopeMargin.getValue());
            setMinCoreLength(constants.coreSectionLength);
            setMaxAdjacency(constants.maxAdjacency.getValue());
            setMaxThickness(constants.maxThickness);
            setMinSectionAspect(constants.minSectionAspect.getValue());
            setMaxDeltaCoord(constants.maxDeltaCoord);
            setMaxDeltaPos(constants.maxDeltaPos);

            if (logger.isDebugEnabled()) {
                dump();
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Double slopeMargin = new Constant.Double(
                "tangent",
                0.04d,
                "Maximum slope value for a stick");

        Scale.Fraction coreSectionLength = new Scale.Fraction(
                1.5, // 2.0
                "Minimum length of a section to be processed");

        Constant.Ratio maxAdjacency = new Constant.Ratio(
                0.8d,
                "Maximum adjacency ratio to be a true vertical line");

        Scale.Fraction maxThickness = new Scale.Fraction(
                0.3,
                "Maximum thickness for resulting stick");

        Constant.Ratio minSectionAspect = new Constant.Ratio(
                6.7d,
                "Minimum value for section aspect (length / thickness)");

        Scale.Fraction maxDeltaCoord = new Scale.Fraction(
                0.175,
                "Maximum difference of ordinates when merging two sticks");

        Scale.Fraction maxDeltaPos = new Scale.Fraction(
                0.1,
                "Maximum difference of abscissa when merging two sticks");

        Constant.Angle maxSlope = new Constant.Angle(
                0.04d,
                "Maximum slope value for a stick to be vertical");

    }
}
