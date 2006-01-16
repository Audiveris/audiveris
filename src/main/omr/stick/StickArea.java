//-----------------------------------------------------------------------//
//                                                                       //
//                           S t i c k A r e a                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.stick;

import omr.check.FailureResult;
import omr.check.SuccessResult;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.lag.Lag;
import omr.util.Logger;
import omr.util.Predicate;

import static omr.stick.SectionRole.*;

import java.util.*;

/**
 * Class <code>StickArea</code> introduces the scanning of rectangular
 * areas, to retrieve sticks.
 *
 * <p> The same algorithms are used for all kinds of sticks with only one
 * difference, depending on length of sticks we are looking for, since long
 * alignments (this applies to staff lines only) can exhibit not very
 * straight lines as opposed to bar lines, stems or ledgers.
 *
 * <ul> <li> <b>Horizontal sticks</b> can be (chunks of) staff lines,
 * alternate ending, or ledgers. </li>
 *
 * <li> <b>Vertical sticks</b> can be bar lines, or stems. </li> </ul> </p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class StickArea
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(StickArea.class);

    // Unique identifier for debugging
    private static int globalId = 0;

    // A stick whose slope is not correct
    private static final FailureResult NOT_STRAIGHT = new FailureResult("StickArea-NotStraight");

    // A stick correctly assigned
    private static final SuccessResult ASSIGNED = new SuccessResult("StickArea-Assigned");

    //~ Instance variables ------------------------------------------------

    // Instance data for the area
    private int id = ++globalId;

    // Sections which are potential future members
    private List<GlyphSection> candidates = new ArrayList<GlyphSection>();

    // Sections recognized as members of sticks
    private List<GlyphSection> members = new ArrayList<GlyphSection>();

    /** The <b>sorted</b> collection of sticks found in this area */
    protected List<Stick> sticks = new ArrayList<Stick>();

    // Maximum value for adjacency
    private double maxAdjacency;

    // Maximum thickness value for sticks to be recognized as such
    private int maxThickness;

    // The source adapter to retrieve sections from
    private Source source;

    //  A flag used to trigger processing specific to very long (and not
    //  totally straight) alignments.
    private boolean longAlignment;

    // The slope of a stick, whether vertical or horizontal, must always be
    // close to zero
    private double maxSlope;

    // Maximum fatness (thickness / length) for a section
    private double maxSectionFatness = constants.maxSectionFatness.getValue();

    // Used to flag sections already visited wrt a given stick
    private int[] visited;

    //~ Constructors ------------------------------------------------------

    /**
     * Creates a new StickArea object.
     */
    public StickArea ()
    {
    }

    //~ Methods -----------------------------------------------------------

    //------//
    // dump //
    //------//
    /**
     * A debugging routine, which dumps any pertinent info to standard
     * output.
     *
     * @param withContent flag to specify that sticks are to be dumped with
     *                    their content
     */
    public void dump (boolean withContent)
    {
        System.out.println("StickArea#" + id + " size=" + sticks.size());

        for (Stick stick : sticks) {
            stick.dump(withContent);
        }
    }

    //------------//
    // initialize //
    //------------//
    /**
     * Perform the initialization of the newly allocated instance.  This
     * consists in scanning the given area, using the source provided.  The
     * resulting sticks are available through the getSticks method.
     *
     * @param preCandidates (maybe null) a list of predefined candidate
     *                      sections
     * @param source        An adaptor to get access to participating
     *                      sections
     * @param minCoreLength Minimum length for a section to be considered
     *                      as stick core
     * @param maxAdjacency  Maximum value for section adjacency to be
     *                      considered as open
     * @param maxThickness  Maximum thickness value for sticks to be found
     * @param maxSlope      maximum stick slope
     * @param longAlignment specific flag to indicate long filament
     *                      retrieval
     */
    public void initialize (GlyphLag           lag,
                            List<GlyphSection> preCandidates,
                            Source             source,
                            int                minCoreLength,
                            double             maxAdjacency,
                            int                maxThickness,
                            double             maxSlope,
                            boolean            longAlignment)
    {
        // Cache computing parameters
        this.maxAdjacency = maxAdjacency;
        this.maxThickness = maxThickness;
        this.maxSlope = maxSlope;
        this.source = source;
        this.longAlignment = longAlignment;

        if (logger.isDebugEnabled()) {
            logger.debug("initialize StickArea#" + id + " minCoreLength="
                         + minCoreLength + " maxAdjacency=" + maxAdjacency
                         + " maxThickness=" + maxThickness
                         + " longAlignment=" + longAlignment);
        }

        // Use a sized array
        visited = new int[lag.getLastVertexId() + 1];
        Arrays.fill(visited, 0);

        // Do we have pre-candidates to start with ?
        if (preCandidates != null) {
            candidates = preCandidates;
        } else {
            // Browse through our sections, to collect the long core
            // sections
            List<GlyphSection> target;

            if (longAlignment) {
                // For long alignments (staff lines), we use an
                // intermediate list of candidates
                target = candidates;
            } else {
                // For short alignment (such as stems and others), we fill
                // members directly
                target = members;
            }

            while (source.hasNext()) {
                StickSection section = (StickSection) source.next();

                // By vertue of the Source adaptor, all provided sections
                // are entirely within the stick area. So tests for core
                // sections are thickness and length, that's all.
                if ((section.getRunNb() <= maxThickness)
                    && (section.getMaxRunLength() >= minCoreLength)) {
                    // OK, this section is candidate as core member of
                    // stick set
                    mark(section, target, SectionRole.CORE, 0, 0);
                }
            }
        }

        if (longAlignment) {
            // Get rid of candidates too far from the staff line core
            thickenAlignmentCore(candidates, members);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(members.size() + " Core sections");
        }

        // Collect candidate sections around the core ones
        for (GlyphSection section : members) {
            collectNeighborsOf(section, 1, -1, true); // Before the core
                                                      // section
            collectNeighborsOf(section, 1, +1, true); // After the core
                                                      // section
        }

        // Purge some internal sections
        ///purgeInternals(candidates);

        if (longAlignment) {
            // Thicken the core sections with the candidates, still paying
            // attention to the resulting thickness of the sticks.
            thickenAlignmentCore(candidates, members);
            ///purgeInternals(members);
        } else {
            members.addAll(candidates);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(members.size() + " total sections");
        }

        // Aggregate member sections into as few sticks as possible. This
        // creates instances in sticks list.
        aggregateMemberSections(lag);

        // Compute stick lines and check the resulting slope
        // This may result in discarded sticks.
        for (Iterator<Stick> it = sticks.iterator(); it.hasNext();) {
            Stick stick = it.next();

            // Stick slope must be very close to zero
            if (stick.getWeight() < constants.minStickWeight.getValue() ||
                Math.abs(stick.getLine().getSlope()) > maxSlope) {
                // This is not a stick
                stick.setResult(NOT_STRAIGHT);

                for (GlyphSection section : stick.getMembers()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Discarding for not straight stick "
                                     + section);
                    }

                    discard((StickSection) section);
                }

                it.remove();
            }
        }

        if (logger.isDebugEnabled()) {
            dump(true);
        }
    }

    //-------//
    // merge //
    //-------//
    /**
     * Merge all sticks found in the area, provided they can be considered
     * extensions of one another, according to the proximity parameters
     * provided.
     *
     * @param maxDeltaCoord maximum distance in coordinate
     * @param maxDeltaPos   maximum distance in position
     * @param maxDeltaSlope maximum difference in slope
     */
    public void merge (int maxDeltaCoord,
                       int maxDeltaPos,
                       double maxDeltaSlope)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("merge maxDeltaCoord=" + maxDeltaCoord
                         + " maxDeltaPos=" + maxDeltaPos
                         + " maxDeltaSlope=" + maxDeltaSlope);
        }

        final long startTime = System.currentTimeMillis();

        // Sort on stick mid position first
        Collections.sort(sticks,
                         new Comparator<Stick>()
                         {
                             public int compare (Stick s1,
                                                 Stick s2)
                             {
                                 return s1.getMidPos() - s2.getMidPos();
                             }
                         });

        // Then use position to narrow the tests
        List<Stick> removals = new ArrayList<Stick>();
        int index = -1;
        for (Stick stick : sticks) {
            index++;
            final int maxPos = stick.getMidPos() + maxDeltaPos;
            List<Stick> tail = sticks.subList(index + 1, sticks.size());

            for (Iterator<Stick> it = tail.iterator(); it.hasNext();) {
                Stick other = it.next();
                if (other.getMidPos() > maxPos) {
                    break;
                }

                if (StickUtil.areExtensions(stick, other, maxDeltaCoord,
                                            maxDeltaPos, maxDeltaSlope)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("merging stick " + stick.getId()
                                     + " into stick " + other.getId());
                    }
                    other.addGlyphSections(stick,
                                           /* linkSections => */ true);
                    removals.add(stick);

                    break;
                }
            }
        }

        for (Stick stick : removals) {
            sticks.remove(stick);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("merged " + removals.size() + " sticks in "
                         + (System.currentTimeMillis() - startTime)
                         + " ms");
        }
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

    //-----------//
    // getSticks //
    //-----------//
    /**
     * Returns the collection of sticks found in this area
     *
     * @return the sticks found
     */
    public List<Stick> getSticks ()
    {
        return sticks;
    }

    //---------//
    // isClose //
    //---------//
    /**
     * Check that the section would not thicken too much the stick being
     * built, and whose members are passed as parameter
     *
     * @param members      the members to compute distance to
     * @param section      the section to check
     * @param maxThickness the maximum resulting stick thickness
     *
     * @return true if OK
     */
    private boolean isClose (List<GlyphSection> members,
                             GlyphSection section,
                             int maxThickness)
    {
        // Just to speed up
        final int start = section.getStart();
        final int stop = section.getStop();
        final int firstPos = section.getFirstPos();
        final int lastPos = section.getLastPos();

        // Check real stick thickness so far
        for (GlyphSection sct : members) {
            // Check overlap in abscissa with section at hand
            if (Math.max(start, sct.getStart()) <= Math.min(stop,
                                                            sct.getStop())) {
                // Check global thickness
                int thick;

                if (sct.getFirstPos() > firstPos) {
                    thick = sct.getLastPos() - firstPos + 1;
                } else {
                    thick = lastPos - sct.getFirstPos() + 1;
                }

                if (thick > maxThickness) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Too thick real line " + thick + " "
                                     + section);
                    }

                    return false;
                }
            }
        }

        return true;
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
    private boolean isDiscarded (StickSection section)
    {
        return section.role == DISCARDED;
    }

    //--------//
    // isFree //
    //--------//
    private boolean isFree (StickSection section)
    {
        return section.role == null;
    }

    //------------//
    // isTooThick //
    //------------//
    /**
     * Check whether the given section is too thick (thicker than the
     * allowed maxThickness).
     *
     * @param section the section to check
     *
     * @return true if section is too thick
     */
    private boolean isTooThick (GlyphSection section)
    {
        return section.getRunNb() > maxThickness;
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
    private void aggregate (StickSection section,
                            Stick stick)
    {
        int id = section.getId();

        if (visited[id] == stick.getId()) {
            return;
        }

        visited[id] = stick.getId();

        if (section.isAggregable()) {
            // Check that resulting stick thickness would still be OK
            if (isClose(stick.getMembers(), section, maxThickness + 1)) {
                stick.addSection(section, /* link => */ true);

                // Aggregate recursively other sections
                for (GlyphSection source : section.getSources()) {
                    aggregate((StickSection) source, stick);
                }

                for (GlyphSection target : section.getTargets()) {
                    aggregate((StickSection) target, stick);
                }
            }
        }
    }

    //-------------------------//
    // aggregateMemberSections //
    //-------------------------//
    /**
     * Aggregate member sections into sticks. We start with no stick at
     * all, then consider each member section on its turn. If the section
     * is not aggregated it begins a new stick. Otherwise we try to
     * aggregate the section to one of the sticks identified so far.
     */
    private void aggregateMemberSections (GlyphLag lag)
    {
        for (GlyphSection s : members) {
            StickSection section = (StickSection) s;
            if (section.isAggregable()) {
                Stick stick = (Stick) lag.createGlyph(Stick.class);
                stick.setResult(ASSIGNED);
                sticks.add(stick);
                aggregate(section, stick);
            }
        }
    }

    //--------------------//
    // collectNeighborsOf //
    //--------------------//
    /**
     * Look for neighbors of this section, in the given direction. If a
     * neighboring sections qualifies, it is assigned the provided layer
     * level.
     *
     * @param section   the section from which neighbors are considered
     * @param layer the current layer number of the section, regarding the
     *                  stick
     * @param direction the direction to look into, which is coded as
     *                  <b>+1</b> for looking at outgoing edges, and
     *                  <b>-1</b> for looking at incoming edges.
     */
    private void collectNeighborsOf (GlyphSection section,
                                     int layer,
                                     int direction,
                                     boolean internalAllowed)
    {
        if (direction > 0) {
            for (GlyphSection target : section.getTargets()) {
                collectSection((StickSection) target,
                               layer, direction, internalAllowed);
            }
        } else {
            for (GlyphSection source : section.getSources()) {
                collectSection((StickSection) source,
                               layer, direction, internalAllowed);
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
     * @param layer           current layer number
     * @param direction       which direction we are moving to
     * @param internalAllowed false if we are reaching the stick border
     */
    private void collectSection (StickSection section,
                                 int layer,
                                 int direction,
                                 boolean internalAllowed)
    {
        // We consider only free (not yet assigned) sections, which are
        // located within the given area.
        if (!isFree(section) || !source.isInArea(section)) {
            return;
        }

        // If the section being checked is already too thick compared
        // with the stick we are looking at, then we discard the section.
        if (isTooThick(section)) {
            // Mark the section, so that we don't retry the same one
            // via another path
            mark(section, null, TOO_THICK, layer, direction);

            return;
        }

        if (longAlignment) {
            // We don't collect sections that are too far from the
            // center of member sections, since this would result in a
            // too thick line.
            if (!isClose(members, section, maxThickness + 1)) {
                mark(section, null, TOO_FAR, layer, direction);

                return;
            }
        }

        // Include only sections that are slim enough
        if ((section.getRunNb() > 1) && (section.getAspect() > maxSectionFatness)) {
            mark(section, null, TOO_FAT, layer, direction);

            return;
        }

        // Check that section is adjacent to open space
        final double adjacency = (direction > 0)
                                 ? section.getLastAdjacency()
                                 : section.getFirstAdjacency();

        if (adjacency <= maxAdjacency) {
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
    private void discard (StickSection section)
    {
        if (section.isCandidate()) {
            section.role = DISCARDED;
            section.setGlyph(null);
        }
    }

    //------//
    // mark //
    //------//
    /**
     * Mark the section with the given flag, and insert it in the provided
     * list if any
     *
     * @param section   the section to mark
     * @param list      the list (if any) to add the section to
     * @param role      section role
     * @param layer     section layer in the stick
     * @param direction section direction in the stick
     */
    private void mark (StickSection section,
                       List<GlyphSection> list,
                       SectionRole role,
                       int layer,
                       int direction)
    {
        section.setParams(role, layer, direction);

        if (list != null) {
            list.add(section);
        }
    }

    //----------------//
    // purgeInternals //
    //----------------//
    private void purgeInternals (List<GlyphSection> list)
    {
        int minLevel = constants.minSectionGreyLevel.getValue();

        for (Iterator<GlyphSection> it = list.iterator(); it.hasNext();) {
            StickSection section = (StickSection) it.next();
            // We purge internal sections which don't exhibit sufficient
            // foreground density (they are too dark)
            if (section.role == INTERNAL) {
                if ((section.getLevel() < minLevel)
                    || (section.getRunNb() > 2)) {
                    // We purge this internal section
                    it.remove();
                    section.role = PURGED;
                }
            }
        }
    }

    //----------------------//
    // thickenAlignmentCore //
    //----------------------//
    /**
     * This routine is used only in the special case of long alignment,
     * meaning staff line detection. The routine uses and then clears the
     * candidate list. The sections that successfully pass the tests are
     * added to the members list.
     *
     * @param ins  input list of candidates (consumed)
     * @param outs output list of members (appended)
     */
    private void thickenAlignmentCore (List<GlyphSection> ins,
                                       List<GlyphSection> outs)
    {
        // Sort ins according to their relevance
        Collections.sort
            (ins,
             new Comparator<GlyphSection>()
             {
                 public int compare (GlyphSection gs1,
                                     GlyphSection gs2)
                 {
                     StickSection s1 = (StickSection) gs1;
                     StickSection s2 = (StickSection) gs2;
                     // Criteria #1 is layer number (small layer numbers
                     // first) Plus section thickness for non layer zero
                     // sections
                     int layerDiff;

                     if ((s1.layer == 0) || (s2.layer == 0)) {
                         layerDiff = s1.layer - s2.layer;
                     } else {
                         layerDiff = (s1.layer + s1.getRunNb())
                             - (s2.layer + s2.getRunNb());
                     }

                     if (layerDiff != 0) {
                         return layerDiff;
                     } else {
                         // Criteria #2 is section length (large lengths
                         // first)
                         return s2.getMaxRunLength() - s1.getMaxRunLength();
                     }
                 }
             });

        // Using the priority order, let's thicken the stick core
        for (GlyphSection s : ins) {
            StickSection section = (StickSection) s;
            if (!isDiscarded(section)) {
                if (isClose(outs, section, maxThickness)) {
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

    //~ Classes -----------------------------------------------------------

    /**
     * Abstract class <code>Source</code> allows to formalize the way
     * relevant sections are made available to the area to be scanned.
     */
    public static class Source
    {
        //~ Instance variables --------------------------------------------

        /** the predicate to check whether section is to be processed */
        protected final SectionPredicate predicate;

        /** the section iterator for the source */
        protected ListIterator<GlyphSection> vi;

        /** the section currently visited */
        protected StickSection section;

        //~ Constructors --------------------------------------------------

        //--------//
        // Source //
        //--------//
        /**
         * Default constructor needed for LineBuilder
         */
        public Source ()
        {
            predicate = null;           // Not used in fact
        }

        //--------//
        // Source //
        //--------//
        /**
         * Create a StickArea source on a given collection of glyph
         * sections, with default predicate
         *
         * @param collection the provided sections
         */
        public Source (Collection<GlyphSection> collection)
        {
            this(collection, new SectionPredicate());
        }

        //--------//
        // Source //
        //--------//
        /**
         * Create a StickArea source on a given collection of glyph
         * sections, with a specific predicate for section
         *
         * @param collection the provided sections
         * @param predicate the predicate to check for candidate sections
         */
        public Source (Collection<GlyphSection> collection,
                       SectionPredicate         predicate)
        {
            this.predicate = predicate;
            ArrayList<GlyphSection> list
                = new ArrayList<GlyphSection>(collection);
            vi = list.listIterator();
        }

        //~ Methods -------------------------------------------------------

        //---------//
        // hasNext //
        //---------//
        /**
         * Check whether we have more sections to scan
         *
         * @return the boolean result of the test
         */
        public boolean hasNext ()
        {
            while (vi.hasNext()) {
                // Update cached data
                section = (StickSection) vi.next();

                if (predicate.check(section)) {
                    section.role = null;        // Safer ?
                    section.setGlyph(null);     // Safer ?
                    return true;
                }
            }

            return false;
        }

        //----------//
        // isInArea //
        //----------//
        /**
         * Check whether a given section lies entirely within the scanned
         * area
         *
         * @param section The section to be checked
         *
         * @return The boolean result of the test
         */
        public boolean isInArea (StickSection section)
        {
            return true;
        }

        //------//
        // next //
        //------//
        /**
         * Return the next relevant section in Area, if any
         *
         * @return the next section
         */
        public GlyphSection next ()
        {
            return section;
        }
    }

    //------------------//
    // SectionPredicate //
    //------------------//
    public static class SectionPredicate
        implements Predicate<StickSection>
    {
        public boolean check (StickSection section)
        {
            // Check whether this section is not already assigned to a
            // recognized stick
            boolean result =
                section.getGlyph() == null ||
                section.role == null ||
                !(section.getGlyph().getResult() instanceof SuccessResult);

            //System.out.println(result + " " + section);

            return result;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        Constant.Integer minStickWeight = new Constant.Integer
                (3,
                 "Minimum value for stick number of pixels");

        Constant.Double maxSectionFatness = new Constant.Double
                (0.2d,
                 "Maximum value for section fatness (thickness / length)");

        Constant.Integer minSectionGreyLevel = new Constant.Integer
                (85,
                 "Minimum grey level value for section internal");

        Constants ()
        {
            initialize();
        }
    }
}
