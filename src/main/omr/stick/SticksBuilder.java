//----------------------------------------------------------------------------//
//                                                                            //
//                         S t i c k s B u i l d e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.stick;

import omr.check.FailureResult;
import omr.check.SuccessResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.facets.BasicStick;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;

import omr.lag.Sections;

import omr.log.Logger;

import omr.sheet.Sheet;
import static omr.stick.SectionRole.*;

import java.util.*;

/**
 * Class <code>SticksBuilder</code> introduces the scanning of a source of
 * sections,  to retrieve sticks.
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
 *
 * @author Hervé Bitteur
 */
public class SticksBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SticksBuilder.class);

    /** Unique identifier for debugging */
    private static int globalId = 0;

    /** A stick whose slope is not correct */
    private static final FailureResult NOT_STRAIGHT = new FailureResult(
        "SticksBuilder-NotStraight");

    /** A stick correctly assigned */
    private static final SuccessResult ASSIGNED = new SuccessResult(
        "SticksBuilder-Assigned");

    /** For comparing sections, according to the thickening relevance */
    private static final Comparator<GlyphSection> thickeningComparator = new Comparator<GlyphSection>() {
        public int compare (GlyphSection gs1,
                            GlyphSection gs2)
        {
            StickSection  s1 = (StickSection) gs1;
            StickSection  s2 = (StickSection) gs2;

            // Criteria #1 is layer number (small layer numbers
            // first) Plus section thickness for non layer zero
            // sections
            int           layerDiff;
            StickRelation r1 = s1.getRelation();
            StickRelation r2 = s2.getRelation();

            if ((r1.layer == 0) || (r2.layer == 0)) {
                layerDiff = r1.layer - r2.layer;
            } else {
                layerDiff = (r1.layer + s1.getRunNb()) -
                            (r2.layer + s2.getRunNb());
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

    /** The related sheet */
    protected final Sheet sheet;

    /** The containing lag */
    protected final GlyphLag lag;

    /** The source adapter to retrieve sections from */
    protected final SticksSource source;

    /** Minimum value for length of core sections */
    private final int minCoreLength;

    /** Maximum value for adjacency */
    private final double maxAdjacency;

    /** Maximum thickness value for sticks to be recognized as such */
    protected final int maxThickness;

    /** Maximum value for slope*/
    private final double maxSlope;

    /** A flag used to trigger processing specific to very long (and not totally
       straight) alignments. */
    private final boolean longAlignment;

    /** The <b>sorted</b> collection of sticks found in this area */
    protected List<Stick> sticks = new ArrayList<Stick>();

    /** Sections which are potential future members */
    private List<GlyphSection> candidates = new ArrayList<GlyphSection>();

    /** Sections recognized as members of sticks */
    private List<GlyphSection> members = new ArrayList<GlyphSection>();

    /** Used to flag sections already visited wrt a given stick */
    private Map<GlyphSection, Glyph> visited;

    /** Minimum aspect (length / thickness) for a section */
    private double minSectionAspect = constants.minSectionAspect.getValue();

    /** Instance data for the area */
    private int id = ++globalId;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SticksBuilder object.
     *
     * @param sheet the related sheet
     * @param lag the GlyphLag which hosts the sections and the glyphs
     * @param source An adaptor to get access to participating sections
     * @param minCoreLength Minimum length for a section to be considered as
     *                      stick core
     * @param maxAdjacency Maximum value for section adjacency to be considered
     *                      as open
     * @param maxThickness Maximum thickness value for sticks to be found
     * @param maxSlope      maximum stick slope
     * @param longAlignment specific flag to indicate long filament retrieval
     */
    public SticksBuilder (Sheet        sheet,
                          GlyphLag     lag,
                          SticksSource source,
                          int          minCoreLength,
                          double       maxAdjacency,
                          int          maxThickness,
                          double       maxSlope,
                          boolean      longAlignment)
    {
        // Cache computing parameters
        this.sheet = sheet;
        this.lag = lag;
        this.source = source;
        this.minCoreLength = minCoreLength;
        this.maxAdjacency = maxAdjacency;
        this.maxThickness = maxThickness;
        this.maxSlope = maxSlope;
        this.longAlignment = longAlignment;
    }

    //~ Methods ----------------------------------------------------------------

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
    public static boolean isDiscarded (StickSection section)
    {
        StickRelation relation = section.getRelation();

        return (relation != null) && (relation.role == DISCARDED);
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

    //--------------//
    // createSticks //
    //--------------//
    /**
     * Perform the initialization of the newly allocated instance.  This
     * consists in scanning the given area, using the source provided.  The
     * resulting sticks are available through the getSticks method.
     *
     * @param preCandidates (maybe null) a list of predefined candidate sections
     */
    public void createSticks (List<GlyphSection> preCandidates)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "createSticks StickArea#" + id + " minCoreLength=" +
                minCoreLength + " maxAdjacency=" + maxAdjacency +
                " maxThickness=" + maxThickness + " longAlignment=" +
                longAlignment);
        }
        // Use a brand new glyph map
        visited = new HashMap<GlyphSection, Glyph>();

        // Do we have pre-candidates to start with ?
        if (preCandidates != null) {
            candidates = preCandidates;
        } else {
            // Browse through our sections, to collect the long core sections
            List<GlyphSection> target;

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
                StickSection section = (StickSection) source.next();

                // By vertue of the Source adaptor, all provided sections are
                // entirely within the stick area. So tests for core sections
                // are not already taken, thickness and length, that's all.
                if (!section.isGlyphMember() &&
                    (section.getRunNb() <= maxThickness) &&
                    (section.getMaxRunLength() >= minCoreLength)) {
                    // OK, this section is candidate as core member of stick set
                    mark(section, target, SectionRole.CORE, 0, 0);
                }
            }
        }

        if (longAlignment) {
            // Get rid of candidates too far from the staff line core
            thickenAlignmentCore(candidates, members);
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                members.size() + Sections.toString(" Core sections", members));
        }

        // Collect candidate sections around the core ones
        for (GlyphSection section : members) {
            collectNeighborsOf(section, 1, -1, true); // Before the core section

            collectNeighborsOf(section, 1, +1, true); // After the core section
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

        if (logger.isFineEnabled()) {
            logger.fine(
                members.size() + Sections.toString(" total sections", members));
        }

        // Aggregate member sections into as few sticks as possible. This
        // creates instances in sticks list.
        aggregateMemberSections();

        // Compute stick lines and check the resulting slope. This may result in
        // discarded sticks.
        for (Iterator<Stick> it = sticks.iterator(); it.hasNext();) {
            Stick stick = it.next();

            // Stick slope must be relevant (at least 3 points), and close to 0
            if ((stick.getWeight() < 3) ||
                (Math.abs(stick.getOrientedLine().getSlope()) > maxSlope)) {
                // This is not a stick
                stick.setResult(NOT_STRAIGHT);

                for (GlyphSection section : stick.getMembers()) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Discarding for not straight stick " + section);
                    }

                    discard((StickSection) section);
                }

                it.remove();
            }
        }

        if (logger.isFineEnabled()) {
            dump(true);
        }
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
        System.out.println("StickArea#" + id + " size=" + sticks.size());

        for (Stick stick : sticks) {
            //stick.dump(withContent);
            stick.dump();
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
    protected void merge (int    maxDeltaCoord,
                          int    maxDeltaPos,
                          double maxDeltaSlope)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "merge maxDeltaCoord=" + maxDeltaCoord + " maxDeltaPos=" +
                maxDeltaPos + " maxDeltaSlope=" + maxDeltaSlope);
        }

        final long startTime = System.currentTimeMillis();

        // Sort on stick mid position first
        Collections.sort(sticks, Stick.midPosComparator);

        // Then use position to narrow the tests
        List<Stick> removals = new ArrayList<Stick>();
        int         index = -1;

        for (Stick stick : sticks) {
            index++;

            final int   maxPos = stick.getMidPos() + maxDeltaPos;
            List<Stick> tail = sticks.subList(index + 1, sticks.size());

            for (Iterator<Stick> it = tail.iterator(); it.hasNext();) {
                Stick other = it.next();

                if (other.getMidPos() > maxPos) {
                    break;
                }

                if (stick.isExtensionOf(
                    other,
                    maxDeltaCoord,
                    maxDeltaPos,
                    maxDeltaSlope)) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "merging stick " + stick.getId() + " into stick " +
                            other.getId());
                    }

                    other.addGlyphSections(stick, Glyph.Linking.LINK_BACK);
                    removals.add(stick);

                    break;
                }
            }
        }

        for (Stick stick : removals) {
            sticks.remove(stick);
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                "merged " + removals.size() + " sticks in " +
                (System.currentTimeMillis() - startTime) + " ms");
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
    private boolean isClose (Collection<GlyphSection> members,
                             GlyphSection             section,
                             int                      maxThickness)
    {
        // Just to speed up
        final int start = section.getStart();
        final int stop = section.getStop();
        final int firstPos = section.getFirstPos();
        final int lastPos = section.getLastPos();

        // Check real stick thickness so far
        for (GlyphSection sct : members) {
            // Check overlap in abscissa with section at hand
            if (Math.max(start, sct.getStart()) <= Math.min(
                stop,
                sct.getStop())) {
                // Check global thickness
                int thick;

                if (sct.getFirstPos() > firstPos) {
                    thick = sct.getLastPos() - firstPos + 1;
                } else {
                    thick = lastPos - sct.getFirstPos() + 1;
                }

                if (thick > maxThickness) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Too thick real line (" + thick + ") " + section);
                    }

                    return false;
                }
            }
        }

        return true;
    }

    //--------//
    // isFree //
    //--------//
    private boolean isFree (StickSection section)
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
                            Stick        stick)
    {
        if (visited.get(section) != stick) {
            visited.put(section, stick);

            if (section.isAggregable()) {
                // Check that resulting stick thickness would still be OK
                if (isClose(stick.getMembers(), section, maxThickness + 1)) {
                    stick.addSection(section, Glyph.Linking.LINK_BACK);

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
        final int interline = sheet.getInterline();

        for (GlyphSection s : members) {
            StickSection section = (StickSection) s;

            if (section.isAggregable()) {
                Stick stick = new BasicStick(interline);
                stick.setResult(ASSIGNED); // Needed to flag the stick
                aggregate(section, stick);
                stick = (Stick) lag.addGlyph(stick);
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
     * @param layer the current layer number of the section, regarding the stick
     * @param direction the direction to look into, which is coded as
     *                  <b>+1</b> for looking at outgoing edges, and
     *                  <b>-1</b> for looking at incoming edges.
     */
    private void collectNeighborsOf (GlyphSection section,
                                     int          layer,
                                     int          direction,
                                     boolean      internalAllowed)
    {
        if (direction > 0) {
            for (GlyphSection target : section.getTargets()) {
                collectSection(
                    (StickSection) target,
                    layer,
                    direction,
                    internalAllowed);
            }
        } else {
            for (GlyphSection source : section.getSources()) {
                collectSection(
                    (StickSection) source,
                    layer,
                    direction,
                    internalAllowed);
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
                                 int          layer,
                                 int          direction,
                                 boolean      internalAllowed)
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
            if (!isClose(members, section, maxThickness + 1)) {
                mark(section, null, TOO_FAR, layer, direction);

                return;
            }
        }

        // Include only sections that are slim enough
        if ((section.getRunNb() > 1) &&
            (section.getAspect() < minSectionAspect)) {
            mark(section, null, TOO_FAT, layer, direction);

            return;
        }

        // Check that section is adjacent to open space
        final double adjacency = (direction > 0) ? section.getLastAdjacency()
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
        StickRelation relation = section.getRelation();

        if (relation.isCandidate()) {
            relation.role = DISCARDED;
            section.setGlyph(null);
        }
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
    private void mark (StickSection       section,
                       List<GlyphSection> list,
                       SectionRole        role,
                       int                layer,
                       int                direction)
    {
        section.setParams(role, layer, direction);

        if (list != null) {
            list.add(section);
        }
    }

    //    //----------------//
    //    // purgeInternals //
    //    //----------------//
    //    private void purgeInternals (List<GlyphSection> list)
    //    {
    //        int minLevel = constants.minSectionGrayLevel.getValue();
    //
    //        for (Iterator<GlyphSection> it = list.iterator(); it.hasNext();) {
    //            StickSection  section = (StickSection) it.next();
    //
    //            // We purge internal sections which don't exhibit sufficient
    //            // foreground density (they are too dark)
    //            StickRelation relation = section.getRelation();
    //
    //            if (relation.role == INTERNAL) {
    //                if ((section.getLevel() < minLevel) ||
    //                    (section.getRunNb() > 2)) {
    //                    // We purge this internal section
    //                    it.remove();
    //                    relation.role = PURGED;
    //                }
    //            }
    //        }
    //    }

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
    private void thickenAlignmentCore (List<GlyphSection> ins,
                                       List<GlyphSection> outs)
    {
        // Sort ins according to their relevance
        Collections.sort(ins, thickeningComparator);

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

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Ratio   minSectionAspect = new Constant.Ratio(
            6.7d,
            "Minimum value for section aspect (length / thickness)");
        Constant.Integer minSectionGrayLevel = new Constant.Integer(
            "ByteLevel",
            46,
            "Minimum gray level value for section internal");
    }
}
