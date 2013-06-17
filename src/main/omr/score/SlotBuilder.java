//----------------------------------------------------------------------------//
//                                                                            //
//                            S l o t B u i l d e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;

import omr.math.Rational;

import omr.score.entity.Beam;
import omr.score.entity.BeamGroup;
import omr.score.entity.Chord;
import omr.score.entity.Measure;
import omr.score.entity.Note;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import omr.score.entity.Voice;
import static omr.score.SlotBuilder.Rel.*;

import omr.sheet.Scale;

import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code SlotBuilder} handles the building of the sequence of
 * slots for every measure within a given system.
 *
 * <p>The key point is to determine when two chords should belong or
 * not to the same time slot:
 * <ul>
 * <li>Chords that share a common stem belong to the same slot.</li>
 * <li>Chords that originate from the same physical glyph belong to the same
 * slot. (for example a note head with one stem on left and one stem on right
 * leads to two overlapping logical chords)</li>
 * <li>Chords within the same beam group, but not on the same stem, cannot
 * belong to the same slot.</li>
 * <li>Similar abscissa is only an indication, it is not always reliable.</li>
 * </ul>
 * </p>
 *
 * @author Hervé Bitteur
 */
public class SlotBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(SlotBuilder.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** The containing system. */
    private final ScoreSystem system;

    /** Scale-dependent parameters. */
    private final Parameters params;

    //~ Constructors -----------------------------------------------------------
    //
    //-------------//
    // SlotBuilder //
    //-------------//
    /**
     * Creates a new SlotBuilder object.
     *
     * @param system the containing system
     */
    public SlotBuilder (ScoreSystem system)
    {
        this.system = system;

        params = new Parameters(system.getScale());
    }

    //~ Methods ----------------------------------------------------------------
    //
    //------------//
    // buildSlots //
    //------------//
    /**
     * Allocate the proper sequence of slots for the chords of the
     * provided measure.
     *
     * @param measure the provided measure
     */
    public void buildSlots (Measure measure)
    {
        MeasureChecker checker = new MeasureChecker(measure);

        // We work on the population of chords, using inter-chords constraints
        checker.buildRelationships();
        checker.buildSlots();
        checker.refineVoices();
    }

    //-----//
    // Rel //
    //-----//
    /**
     * Describes the oriented relationship between two chords.
     */
    protected static enum Rel
    {

        /**
         * Strongly before.
         * Stem-located before in the same beam group.
         * Abscissa-located before the vertically overlapping chord.
         * Important abscissa difference in different staves.
         */
        BEFORE("B"),
        //
        /** Strongly after.
         * Stem-located after in the same beam group.
         * Abscissa-located after the vertically overlapping chord.
         * Important abscissa difference in different staves.
         */
        AFTER("A"),
        //
        /**
         * Strongly equal.
         * Identical thanks to an originating glyph in common.
         * Adjacency detected in same staff.
         */
        EQUAL("="),
        //
        /**
         * Weakly close.
         * No important difference, use other separation criteria.
         */
        CLOSE("?");

        private final String mnemo;

        Rel (String mnemo)
        {
            this.mnemo = mnemo;
        }

        @Override
        public String toString ()
        {
            return mnemo;
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //----------------//
    // MeasureChecker //
    //----------------//
    private class MeasureChecker
    {

        /** The measure at hand. */
        private final Measure measure;

        /** Inter-chord relationships for the current measure. */
        private Rel[][] matrix;

        /**
         * A chord comparator based on inter-chord relationships
         * and then on startTime when known.
         */
        private Comparator<Chord> byRel = new Comparator<Chord>()
        {
            @Override
            public int compare (Chord c1,
                                Chord c2)
            {
                Rel rel = getRel(c1, c2);
                if (rel == null) {
                    return 0;
                } else {
                    switch (rel) {
                    case BEFORE:
                        return -1;

                    case AFTER:
                        return 1;

                    default:
                        // Use start time difference when known
                        if (c1.getStartTime() != null
                            && c2.getStartTime() != null) {
                            return c1.getStartTime().compareTo(c2.getStartTime());
                        } else {
                            return 0;
                        }
                    }
                }
            }
        };

        //----------------//
        // MeasureChecker //
        //----------------//
        public MeasureChecker (Measure measure)
        {
            this.measure = measure;
        }

        //--------------------//
        // buildRelationships //
        //--------------------//
        /**
         * Compute the matrix of inter-chords relationships.
         */
        public void buildRelationships ()
        {
            // Sort measure chords by abscissa
            Collections.sort(measure.getChords(), Chord.byNodeAbscissa);

            // Allocate matrix of inter-chord relartionships
            int chordCount = measure.getChords().size();
            matrix = new Rel[chordCount][chordCount];

            // BeamGroup-based relationships
            inspectBeams();

            // Seed-based relationships
            inspectSeeds();

            // Location-based relationships
            inspectLocations();

            if (logger.isDebugEnabled()) {
                dumpRelationships();
            }
        }

        //--------------//
        // inspectBeams //
        //--------------//
        /**
         * Derive some inter-chord relationships from BeamGroup's.
         */
        private void inspectBeams ()
        {
            // BeamGroup-based relationships
            for (BeamGroup group : measure.getBeamGroups()) {
                Set<Chord> chordSet = new HashSet<>();
                for (Beam beam : group.getBeams()) {
                    chordSet.addAll(beam.getChords());
                }
                List<Chord> groupChords = new ArrayList<>(chordSet);
                Collections.sort(groupChords, Chord.byAbscissa);
                Chord prevChord = null;
                for (Chord chord : groupChords) {
                    if (prevChord != null) {
                        setRel(prevChord, chord, BEFORE);
                        setRel(chord, prevChord, AFTER);
                    }
                    prevChord = chord;
                }
            }
        }

        //--------------//
        // inspectSeeds //
        //--------------//
        /**
         * Derive some inter-chords relationships from shared seeds.
         */
        private void inspectSeeds ()
        {
            final int chordCount = measure.getChords().size();
            int index = 0;
            for (TreeNode pn : measure.getChords()) {
                index++;
                Chord ch1 = (Chord) pn;
                if (ch1.isWholeDuration()) {
                    continue;
                }
                for (TreeNode n : measure.getChords().subList(index, chordCount)) {
                    Chord ch2 = (Chord) n;
                    if (ch2.isWholeDuration() || getRel(ch1, ch2) != null) {
                        continue;
                    }

                    // Check for common note glyph
                    if (haveCommonSeed(ch1, ch2)) {
                        setRel(ch1, ch2, EQUAL);
                        setRel(ch2, ch1, EQUAL);
                    }
                }
            }
        }

        //------------------//
        // inspectLocations //
        //------------------//
        /**
         * Derive the missing inter-chord relationships from chords
         * relative locations.
         */
        private void inspectLocations ()
        {
            final int chordCount = measure.getChords().size();
            List<ChordPair> adjacencies = new ArrayList<>();
            int index = 0;
            for (TreeNode pn : measure.getChords()) {
                index++;
                Chord ch1 = (Chord) pn;
                if (ch1.isWholeDuration()) {
                    continue;
                }
                Rectangle box1 = ch1.getBox();

                for (TreeNode n : measure.getChords().subList(index, chordCount)) {
                    Chord ch2 = (Chord) n;
                    if (ch2.isWholeDuration() || getRel(ch1, ch2) != null) {
                        continue;
                    }

                    // Check y overlap
                    Rectangle box2 = ch2.getBox();
                    int yOverlap = Math.min(box1.y + box1.height, box2.y + box2.height)
                                   - Math.max(box1.y, box2.y);
                    if (yOverlap > 0) {
                        // Boxes overlap vertically
                        if (areAdjacent(ch1, ch2)) {
                            setRel(ch1, ch2, EQUAL);
                            setRel(ch2, ch1, EQUAL);
                            adjacencies.add(new ChordPair(ch1, ch2));
                        } else {
                            if (ch1.getCenter().x <= ch2.getCenter().x) {
                                setRel(ch1, ch2, BEFORE);
                                setRel(ch2, ch1, AFTER);
                            } else {
                                setRel(ch2, ch1, BEFORE);
                                setRel(ch1, ch2, AFTER);
                            }
                        }
                    } else {
                        // Boxes do not overlap vertically
                        int dx = Math.abs(ch1.getCenter().x - ch2.getCenter().x);
                        if (dx <= params.maxSlotDx) {
                            setRel(ch1, ch2, CLOSE);
                            setRel(ch2, ch1, CLOSE);
                        } else {
                            if (ch1.getCenter().x <= ch2.getCenter().x) {
                                setRel(ch1, ch2, BEFORE);
                                setRel(ch2, ch1, AFTER);
                            } else {
                                setRel(ch2, ch1, BEFORE);
                                setRel(ch1, ch2, AFTER);
                            }
                        }
                    }
                }
            }

            // Process detected adjacencies
            if (!adjacencies.isEmpty()) {
                logger.debug("Adjacencies: {}", adjacencies);
                for (ChordPair pair : adjacencies) {
                    // Since ch1 ~ ch2, all neighbors of ch1 ~ neighbors of ch2
                    Set<Chord> n1 = getClosure(pair.one);
                    Set<Chord> n2 = getClosure(pair.two);
                    for (Chord ch1 : n1) {
                        for (Chord ch2 : n2) {
                            if (ch1 != ch2) {
                                if (getRel(ch1, ch2) != EQUAL) {
                                    setRel(ch1, ch2, CLOSE);
                                }
                                if (getRel(ch2, ch1) != EQUAL) {
                                    setRel(ch2, ch1, CLOSE);
                                }
                            }
                        }
                    }
                }
            }
        }

        //-------------//
        // areAdjacent //
        //-------------//
        private boolean areAdjacent (Chord ch1,
                                     Chord ch2)
        {
            final Rectangle box1 = ch1.getBox();
            final Rectangle box2 = ch2.getBox();

            // Check horizontal void gap
            final int xGap = Math.max(box1.x, box2.x)
                             - Math.min(box1.x + box1.width, box2.x + box2.width);

            if (xGap > params.maxAdjacencyGap) {
                return false;
            }
            if (xGap < 0) {
                return true;
            }

            if (ch1.getStem() != null
                && ch2.getStem() != null) {

                // If they share the same stem -> true
                if (ch1.getStem() == ch2.getStem()) {
                    return true;
                }

                // If stem directions are identical -> false
                if (ch1.getStemDir() == ch2.getStemDir()) {
                    return false;
                }

                // If beam on each side -> false (different groups!)
                if (!ch1.getBeams().isEmpty()
                    && !ch2.getBeams().isEmpty()) {
                    return false;
                }

                // Check abscissa gap between stems
                if (Math.abs(ch1.getHeadLocation().x - ch2.getHeadLocation().x)
                    > params.maxSlotDx) {
                    return false;
                }

                // All tests are OK
                return true;
            } else {
                return false;
            }
        }

        //-------------------//
        // dumpRelationships //
        //-------------------//
        private void dumpRelationships ()
        {
            logger.info(measure.getContextString());

            // List BeamGroups
            for (BeamGroup group : measure.getBeamGroups()) {
                logger.info("  {}", group);
            }

            // List chords relationships
            StringBuilder sb = new StringBuilder("  ");
            for (int ix = 0; ix < matrix.length; ix++) {
                sb.append(String.format(" %2d", ix + 1));
            }

            for (int iy = 0; iy < matrix.length; iy++) {
                Rel[] line = matrix[iy];
                sb.append("\n");
                sb.append(String.format("%2d", iy + 1));
                for (int ix = 0; ix < matrix.length; ix++) {
                    sb.append("  ");
                    Rel rel = line[ix];
                    if (rel != null) {
                        sb.append(rel);
                    } else {
                        sb.append(".");
                    }
                }

                // Append chord description
                sb.append("  ").append(getChord(iy + 1));

            }
            logger.info("\n{}", sb);
        }

        //------------//
        // getClosure //
        //------------//
        private Set<Chord> getClosure (Chord chord)
        {
            Set<Chord> closes = new LinkedHashSet<>();
            closes.add(chord);

            for (TreeNode cn : measure.getChords()) {
                Chord ch = (Chord) cn;
                Rel rel1 = getRel(chord, ch);
                Rel rel2 = getRel(ch, chord);
                if (rel1 == CLOSE || rel1 == EQUAL
                    || rel2 == CLOSE || rel2 == EQUAL) {
                    closes.add(ch);
                }
            }

            return closes;
        }

        //----------//
        // getChord //
        //----------//
        /**
         * Retrieve a chord instance, knowing its id (in the measure).
         *
         * @param id the chord id
         * @return the chord instance, or null
         */
        private Chord getChord (int id)
        {
            for (TreeNode cn : measure.getChords()) {
                Chord chord = (Chord) cn;
                if (chord.getId() == id) {
                    return chord;
                }
            }
            return null;
        }

        //----------------//
        // haveCommonSeed //
        //----------------//
        /**
         * Check whether the provided chords have a common seed glyph.
         * If so, they must share the same time slot and must be in separate
         * voices.
         *
         * @param ch1 first provided chord
         * @param ch2 second provided chord
         * @return true if there is a common seed
         */
        private boolean haveCommonSeed (Chord ch1,
                                        Chord ch2)
        {
            // use Chord -> Notes -> Glyph(s) and check for common glyph
            Set<Glyph> seeds = new HashSet<>();

            for (TreeNode nn : ch1.getNotes()) {
                Note note = (Note) nn;
                seeds.addAll(note.getGlyphs());
            }

            for (TreeNode nn : ch2.getNotes()) {
                Note note = (Note) nn;
                for (Glyph glyph : note.getGlyphs()) {
                    if (seeds.contains(glyph)) {
                        return true;
                    }
                }
            }

            return false;
        }

        //--------//
        // setRel //
        //--------//
        private void setRel (Chord from,
                             Chord to,
                             Rel rel)
        {
            matrix[from.getId() - 1][to.getId() - 1] = rel;
        }

        //--------//
        // getRel //
        //--------//
        private Rel getRel (Chord from,
                            Chord to)
        {
            return matrix[from.getId() - 1][to.getId() - 1];
        }

        //------------//
        // buildSlots //
        //------------//
        /**
         * Build the measure time slots, using the inter-chord
         * relationships and the chords durations.
         */
        private void buildSlots ()
        {
            // The 'actives' collection gathers the chords that are "active"
            // (not terminated) at the time slot being considered. Initially, it
            // contains just the whole chords.
            List<Chord> actives = new ArrayList<>(measure.getWholeChords());
            Collections.sort(actives, Chord.byAbscissa);

            // Create voices for whole chords
            for (Chord chord : actives) {
                chord.setStartTime(Rational.ZERO);
                Voice.createWholeVoice(chord);
            }

            // List of chords assignable, but not yet assigned to a slot
            List<Chord> pendings = new ArrayList<>();
            for (TreeNode pn : measure.getChords()) {
                Chord chord = (Chord) pn;
                if (!chord.isWholeDuration()) {
                    pendings.add(chord);
                }
            }

            // Assign chords to time slots
            while (!pendings.isEmpty()) {
                // Hosting time slot
                Rational startTime = computeStartTime(actives);

                dump("actives", actives);

                // Chords ending here, with their voice available for the slot
                List<Chord> freeEndings = new ArrayList<>();

                // Chords ending here, with voice not available (beam group)
                List<Chord> endings = new ArrayList<>();

                for (Chord chord : actives) {
                    // Look for chord that finishes at the slot at hand
                    if (!chord.isWholeDuration()
                        && (chord.getEndTime().compareTo(startTime) <= 0)) {
                        // Make sure voice is really available
                        BeamGroup group = chord.getBeamGroup();

                        if ((group != null) && (chord != group.getLastChord())) {
                            // Group continuation
                            endings.add(chord);
                        } else if (!chord.getFollowingTiedChords().isEmpty()) {
                            // Tie continuation
                            endings.add(chord);
                        } else {
                            freeEndings.add(chord);
                        }
                    }
                }
                dump("freeEndings", freeEndings);
                dump("endings", endings);

                // Which pending chords should join the slot?
                dump("pendings", pendings);
                List<Chord> incomings = retrieveIncomingChords(pendings);
                dump("incomings", incomings);

                Slot slot = new Slot(measure);
                measure.getSlots().add(slot);
                slot.setChords(incomings);
                slot.setStartTime(startTime);

                // Determine the voice of each chord of the slot
                slot.buildVoices(endings);

                // Update for next iteration
                pendings.removeAll(incomings);
                actives.removeAll(freeEndings);
                actives.removeAll(endings);
                actives.addAll(incomings);
            }
        }

        //------//
        // dump //
        //------//
        private void dump (String label,
                           Collection<Chord> chords)
        {
            if (logger.isDebugEnabled()) {

                StringBuilder sb = new StringBuilder();
                sb.append(label)
                        .append("[");

                for (Chord chord : chords) {
                    sb.append("#").append(chord.getId());
                }

                sb.append("]");
                logger.debug(sb.toString());
            }
        }

        //------------------------//
        // retrieveIncomingChords //
        //------------------------//
        /**
         * Among the pendings chords, select the ones that would fit
         * into the next time slot.
         *
         * @param pendings the sequence of noy yet assigned chords
         * @return the collection of chords for next slot
         */
        private List<Chord> retrieveIncomingChords (List<Chord> pendings)
        {
            Collections.sort(pendings, byRel);
            List<Chord> incomings = new ArrayList<>();
            Chord firstChord = pendings.get(0);

            for (Chord chord : pendings) {
                if (byRel.compare(firstChord, chord) == 0) {
                    incomings.add(chord);
                } else {
                    break;
                }
            }

            return incomings;
        }

        //------------------//
        // computeStartTime //
        //------------------//
        /**
         * Based on the provided active chords, determine the next
         * expiration time.
         *
         * @param activeChords
         */
        private Rational computeStartTime (Collection<Chord> activeChords)
        {
            Rational startTime = Rational.MAX_VALUE;

            for (Chord chord : activeChords) {
                if (!chord.isWholeDuration()) { // Skip the "whole" chords
                    Rational endTime = chord.getEndTime();

                    if (endTime.compareTo(startTime) < 0) {
                        startTime = endTime;
                    }
                }
            }

            if (startTime.equals(Rational.MAX_VALUE)) {
                startTime = Rational.ZERO;
            }

            logger.debug("startTime={}", startTime);

            return startTime;
        }

        //--------------//
        // refineVoices //
        //--------------//
        /**
         * Slight improvements to voices in the current measure.
         */
        private void refineVoices ()
        {
            // Preserve vertical voice order at beginning of measure
            // Use chords from first time slot + whole chords
            List<Chord> firsts = new ArrayList<>();
            if (!measure.getSlots().isEmpty()) {
                firsts.addAll(measure.getSlots().get(0).getChords());
            }
            if (measure.getWholeChords() != null) {
                firsts.addAll(measure.getWholeChords());
            }
            Collections.sort(firsts, Chord.byOrdinate);

            // Rename voices accordingly
            for (int i = 0; i < firsts.size(); i++) {
                Chord chord = firsts.get(i);
                Voice voice = chord.getVoice();
                measure.swapVoiceId(voice, i + 1);
            }
        }
    }

    //-----------//
    // ChordPair //
    //-----------//
    private static class ChordPair
    {

        final Chord one;

        final Chord two;

        public ChordPair (Chord one,
                          Chord two)
        {
            this.one = one;
            this.two = two;
            logger.debug("Adjacent {}@{} & {}@{}",
                    one, one.getHeadLocation(), two, two.getHeadLocation());
        }

        @Override
        public String toString ()
        {
            return "{ch#" + one.getId() + ",ch#" + two.getId() + "}";
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {

        Scale.Fraction maxSlotDx = new Scale.Fraction(
                1,
                "Maximum horizontal delta between a slot and a chord");

        Scale.Fraction maxAdjacencyGap = new Scale.Fraction(
                0.5,
                "Maximum horizontal gap between adjacent chords bounds");

    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        private final int maxSlotDx;

        private final int maxAdjacencyGap;

        //~ Constructors -------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxSlotDx = scale.toPixels(constants.maxSlotDx);
            maxAdjacencyGap = scale.toPixels(constants.maxAdjacencyGap);
        }
    }
}
