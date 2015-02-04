//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S t a c k B u i l d e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.math.Rational;

import omr.score.entity.StaffBarline;
import omr.score.entity.TimeSignature;

import omr.sig.SIGraph;
import omr.sig.SigAttic;
import omr.sig.SigReducer;
import omr.sig.inter.AugmentationDotInter;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.ChordInter;
import omr.sig.inter.FlagInter;
import omr.sig.inter.Inter;
import omr.sig.inter.InterEnsemble;
import omr.sig.inter.RepeatDotInter;
import omr.sig.inter.TimeInter;
import omr.sig.inter.TupletInter;
import omr.sig.relation.Relation;
import omr.sig.relation.RepeatDotBarRelation;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.LEFT;
import static omr.util.HorizontalSide.RIGHT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code StackBuilder} builds the rhythm content of a MeasureStack.
 * <ol>
 * <li>Rhythm data brought by head-based chords and beam groups are considered as solid.</li>
 * <li>Rhythm data brought by symbol-based items, such as rest-based chords, flags, augmentation
 * dots, tuplets are used as adjustment variables.</li>
 * <li>Time signatures have to be considered differently, since their value may be called into
 * question, based on intrinsic measure rhythm. So, a two-pass approach may be needed.</li>
 * </ol>
 * Once a good configuration has been chosen, we should care about conflicts between rhythm data and
 * other symbol-based items (for example a tuplet sign may conflict with a dynamic sign).
 * Perhaps give priority (frozen inter) to rhythm data over non-rhythm data?
 *
 * @author Hervé Bitteur
 */
public class StackBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger    logger = LoggerFactory.getLogger(StackBuilder.class);

    /** Relevant rhythm classes. */
    public static final Class<?>[] timingClasses = new Class<?>[] {
                                                       TimeInter.class, ChordInter.class,
                                                       FlagInter.class, TupletInter.class,
                                                       AugmentationDotInter.class
                                                   };

    //~ Instance fields ----------------------------------------------------------------------------

    /** The dedicated measure stack. */
    private final MeasureStack stack;

    /** To temporarily save inters and their relations, outside of the standard sig. */
    private final StackBackup backup;

    /** All configurations that were tested so far and failed. */
    private final Set<Config> failures = new LinkedHashSet<Config>();

    /** Configurations still to be tested for this stack. */
    private final List<Config> candidates = new ArrayList<Config>();

    /** Current index in candidates. */
    private int candidateIndex;

    /** Current config in stack. */
    private Config config;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code StackBuilder} object.
     *
     * @param stack the measure stack to process
     */
    public StackBuilder (MeasureStack stack)
    {
        this.stack = stack;
        backup = new StackBackup(stack);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // process //
    //---------//
    public void process (List<Inter> systemInters)
    {
        if (stack.getPageId().equals("3")) {
            logger.info("BINGO");
        }

        // Populate stack with relevant rhythm data
        populate(systemInters);

        final SIGraph sig = stack.getSystem().getSig();

        // Variables
        List<Inter>   vars = new ArrayList<Inter>();
        vars.addAll(stack.getRestChords());
        vars.addAll(stack.getTimings());
        SigReducer.detectOverlaps(vars);

        // Backup all stack rhythm data
        backup.save(vars);

        List<List<Inter>> partitions = sig.getPartitions(null, vars);

        for (List<Inter> partition : partitions) {
            candidates.add(new Config(partition));
        }

        Config goodConfig = null; // The very first good config found

        // Process all identified configuration, until a good one is found
        for (candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
            final Config candidate = candidates.get(candidateIndex);

            if (!failures.contains(candidate)) {
                goodConfig = checkConfig(candidate);

                if (goodConfig != null) {
                    break;
                }
            }
        }

        if (goodConfig != null) {
            // Re-install the goodConfig if different from current config, then freeze it
            if (!goodConfig.equals(config)) {
                logger.info("Re-installing good config {}", goodConfig);
                backup.install(goodConfig.inters);
            }

            backup.freeze(goodConfig.inters);
        } else {
            // TODO: in which config should we leave this stack???
            logger.warn("No good config found for {}", stack);
        }
    }

    //-------------//
    // checkConfig //
    //-------------//
    /**
     * Check the provided config.
     *
     * @param config newConfig the config to consider
     * @return the config if successful, null otherwise
     */
    private Config checkConfig (Config newConfig)
    {
        logger.info("checkConfig {}", newConfig);

        if (stack.getPageId().equals("7")) {
            logger.info("BINGO");
        }

        if (!newConfig.equals(config)) {
            backup.install(newConfig.inters);
            config = newConfig;
        }

        if (!checkVoices()) {
            return null;
        }

        return newConfig;
    }

    //-------------//
    // checkVoices //
    //-------------//
    /**
     * Check validity of every voice in stack.
     * At first invalid voice encountered, we may suggest new configs (into candidates) and return
     * false;
     *
     * @return true if all voices are OK, false otherwise (with perhaps additional candidates)
     */
    private boolean checkVoices ()
    {
        try {
            Rational stackDur = stack.getCurrentDuration();

            if (!stackDur.equals(Rational.ZERO)) {
                // Make sure the stack duration is not bigger than limit
                if (stackDur.compareTo(stack.getExpectedDuration()) <= 0) {
                    stack.setActualDuration(stackDur);
                } else {
                    stack.setActualDuration(stack.getExpectedDuration());
                }
            }

            ///stack.printVoices(null);
            Rational actualDuration = stack.getActualDuration();
            logger.info(
                "{} expected:{} actual:{} current:{}",
                stack,
                stack.getExpectedDuration(),
                actualDuration,
                stackDur);

            for (Voice voice : stack.getVoices()) {
                Rational voiceDur = voice.getDuration();
                logger.info("{} ends at {}", voice, voiceDur);

                if (voiceDur != null) {
                    Rational  delta = voiceDur.minus(actualDuration);
                    final int sign = delta.compareTo(Rational.ZERO);

                    if (sign == 0) {
                        // OK for this voice
                    } else if (sign > 0) {
                        // Too long: try to shorten this voice
                        // Removing a rest, removing a dot, inserting a tuplet? Test all!
                        failures.add(config);

                        List<ChordInter> rests = voice.getRests();
                        logger.info("{} Excess {} in {} from:{}", stack, delta, voice, rests);

                        if (!rests.isEmpty()) {
                            postRests(rests, delta);
                        }

                        return false;
                    } else {
                        // Too short
                        // If voice made of only rest(s), delete it
                        if (voice.isOnlyRest()) {
                            failures.add(config);

                            List<ChordInter> rests = voice.getRests();
                            logger.info("{} Abnormal rest-only {} rests:{}", stack, voice, rests);

                            Config newConfig = new Config(config.inters);
                            newConfig.inters.removeAll(rests);

                            if (!failures.contains(newConfig) && !candidates.contains(newConfig)) {
                                candidates.add(candidateIndex + 1, newConfig);
                            }

                            return false;
                        }
                    }
                }

                // TODO: Suggestion?
            }

            return true; // Success!
        } catch (TimeSignature.InvalidTimeSignature ex) {
        } catch (Exception ex) {
            logger.warn(getClass().getSimpleName() + " Error visiting " + stack, ex);
        }

        return false;
    }

    //------------//
    // pickupRest //
    //------------//
    /**
     * Choose a rest within the provided collection.
     *
     * @param rests the (non-empty) collection of rests to select from
     * @param delta the precise duration excess
     * @return the chosen rest
     */
    private ChordInter pickupRest (List<ChordInter> rests,
                                   Rational         delta)
    {
        Collections.sort(rests, Inter.byReverseGrade);

        // Look for the first rest with duration equal to provided delta, if any
        for (ChordInter rest : rests) {
            if (rest.getDuration().equals(delta)) {
                return rest;
            }
        }

        // None found, select the lowest grade
        return rests.get(rests.size() - 1);
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate the measure stack with the relevant rhythm Inter instances.
     *
     * @param systemInters relevant inters at system level
     */
    private void populate (List<Inter> systemInters)
    {
        final SystemInfo system = stack.getSystem();

        // Rough stack abscissa limits
        int left = Integer.MAX_VALUE;
        int right = 0;

        for (Measure measure : stack.getMeasures()) {
            for (Staff staff : measure.getPart().getStaves()) {
                left = Math.min(left, measure.getAbscissa(LEFT, staff));
                right = Math.max(right, measure.getAbscissa(RIGHT, staff));
            }
        }

        // Use staff & location of system inter to determine if it relates to stack at hand
        for (Inter inter : systemInters) {
            Point center = inter.getCenter();

            // Rough limits
            if ((center.x < left) || (center.x > right)) {
                continue;
            }

            List<Staff> stavesArounds = system.getStavesAround(center); // 1 or 2 staves
            Staff       staff1 = stavesArounds.get(0);
            Measure     m1 = stack.getMeasureAt(staff1);

            if ((m1.getAbscissa(LEFT, staff1) <= center.x) &&
                (center.x <= m1.getAbscissa(RIGHT, staff1))) {
                // Populate (system) measure stack
                stack.addInter(inter);

                // Populate (part) measure as well if possible (TODO: what for?)
                Staff staff = inter.getStaff();

                if (staff != null) {
                    Measure measure = staff.getPart().getMeasureAt(center);
                    measure.addInter(inter);
                }
            }
        }

        // Allocate beam groups
        BeamGroup.populate(stack);

        // Check repeat dots on left & right sides
        verifyRepeatDots();
    }

    //-----------//
    // postRests //
    //-----------//
    private void postRests (List<ChordInter> rests,
                            Rational         delta)
    {
        Collections.sort(rests, Inter.byGrade);
//
//        // Try all combinations, starting by discarding weaker ones
//
//        // Look for rests with duration equal to provided delta
//        for (ChordInter rest : rests) {
//            if (rest.getDuration().equals(delta)) {
//                return rest;
//            }
//        }
//
        // Pickup a rest of this delta if any
        ChordInter rest = pickupRest(rests, delta);
        Config     newConfig = new Config(config.inters);
        newConfig.inters.remove(rest);

        if (!failures.contains(newConfig) && !candidates.contains(newConfig)) {
            candidates.add(candidateIndex + 1, newConfig);
        }
    }

    //------------------//
    // verifyRepeatDots //
    //------------------//
    /**
     * Look for repeat sign on each side of the measure stack.
     * If positive, flag the related stack barline(s) as such and delete any other interpretations
     * for these repeat dots.
     */
    private void verifyRepeatDots ()
    {
        final SIGraph sig = stack.getSystem().getSig();

        for (HorizontalSide side : HorizontalSide.values()) {
            final List<RepeatDotInter> dots = new ArrayList<RepeatDotInter>();
            int                        barCount = 0;

            for (Measure measure : stack.getMeasures()) {
                final Part        part = measure.getPart();
                final PartBarline partBarline = measure.getBarline(side);

                if (partBarline == null) {
                    continue;
                }

                for (Staff staff : part.getStaves()) {
                    StaffBarline staffBarline = partBarline.getBarline(part, staff);
                    BarlineInter bar = (side == LEFT) ? staffBarline.getRightBar()
                                       : staffBarline.getLeftBar();

                    if (bar == null) {
                        continue;
                    }

                    barCount++;

                    Set<Relation> dRels = sig.getRelations(bar, RepeatDotBarRelation.class);

                    if (!dRels.isEmpty()) {
                        for (Relation rel : dRels) {
                            RepeatDotInter dot = (RepeatDotInter) sig.getOppositeInter(bar, rel);
                            dots.add(dot);
                            logger.debug("Repeat dot for {}", dot);
                        }
                    }
                }
            }

            int dotCount = dots.size();
            logger.debug("{} {} bars:{} dots:{}", stack, side, barCount, dotCount);

            if ((dotCount != 0) && (dotCount >= (barCount / 2))) {
                // It's a repeat side, enforce it!
                stack.addRepeat(side);

                // Delete inters that conflict with repeat dots
                List<Inter> toDelete = new ArrayList<Inter>();

                for (RepeatDotInter dot : dots) {
                    Rectangle dotBox = dot.getBounds();

                    for (Inter inter : sig.vertexSet()) {
                        if (inter == dot) {
                            continue;
                        }

                        if (dotBox.intersects(inter.getBounds()) && dot.overlaps(inter)) {
                            toDelete.add(inter);
                        }
                    }
                }

                if (!toDelete.isEmpty()) {
                    for (Inter inter : toDelete) {
                        stack.removeInter(inter);
                        inter.delete();
                    }
                }
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------//
    // Config //
    //--------//
    private static class Config
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Rhythm data for this config, always ordered byFullAbscissa. */
        private final List<Inter> inters;

        //~ Constructors ---------------------------------------------------------------------------

        public Config (List<Inter> inters)
        {
            // Make a sorted copy of provided inters
            this.inters = new ArrayList<Inter>(inters);
            Collections.sort(this.inters, Inter.byFullAbscissa);
        }

        //~ Methods --------------------------------------------------------------------------------

        @Override
        public boolean equals (Object obj)
        {
            if (!(obj instanceof Config)) {
                return false;
            }

            Config that = (Config) obj;

            if (this.inters.size() != that.inters.size()) {
                return false;
            }

            for (int i = 0; i < inters.size(); i++) {
                if (this.inters.get(i) != that.inters.get(i)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;

            return hash;
        }

        @Override
        public String toString ()
        {
            return inters.toString();
        }
    }

    //-------------//
    // StackBackup //
    //-------------//
    /**
     * Class {@code StackBackup} manages the rhythm data configuration of a MeasureStack.
     * <ol>
     * <li>It saves the comprehensive initial set of rhythm data, perhaps with conflicting
     * items.</li>
     * <li>It can install a specific configuration of rhythm data for testing.</li>
     * <li>It can freeze the stack when a final good configuration has been chosen.</li>
     * </ol>
     */
    private static class StackBackup
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The managed measure stack. */
        private final MeasureStack stack;

        /** Initial rhythm data. */
        private List<Inter> initials;
        private final SIGraph  sig;
        private final SigAttic attic = new SigAttic();

        //~ Constructors ---------------------------------------------------------------------------

        public StackBackup (MeasureStack stack)
        {
            this.stack = stack;
            sig = stack.getSystem().getSig();
        }

        //~ Methods --------------------------------------------------------------------------------

        public void freeze (List<Inter> keptInters)
        {
            // For those chords that have not been kept, delete the member notes
            List<Inter> discardedInters = new ArrayList<Inter>(initials);
            discardedInters.removeAll(keptInters);

            for (Inter discarded : discardedInters) {
                discarded.delete();
                stack.removeInter(discarded);

                if (discarded instanceof InterEnsemble) {
                    for (Inter member : ((InterEnsemble) discarded).getMembers()) {
                        member.delete();
                    }
                }
            }

            // Freeze the stack rhythm data
            for (Inter kept : keptInters) {
                kept.freeze();
            }

            for (ChordInter chord : stack.getChords()) {
                chord.freeze();
            }
        }

        public void install (List<Inter> config)
        {
            // Clear the stack
            for (Inter inter : initials) {
                stack.removeInter(inter);
                inter.delete();
            }

            // Restore just the partition
            attic.restore(sig, config);

            for (Inter inter : config) {
                stack.addInter(inter);
            }

            // Reset all rhythm data within the stack
            stack.resetRhythm();

            // Count augmentation dots on chords
            countChordDots();

            // Link tuplets
            List<TupletInter> toDelete = new TupletsBuilder(stack).linkTuplets();

            if (!toDelete.isEmpty()) {
                config.removeAll(toDelete);
            }

            // Build slots & voices
            new SlotsBuilder(stack).process();
        }

        public void save (List<Inter> inters)
        {
            // Copy the initial rhythm data
            initials = new ArrayList<Inter>(inters);

            // Save relevant sig inters & relations
            attic.save(sig, inters);
        }

        //----------------//
        // countChordDots //
        //----------------//
        private void countChordDots ()
        {
            List<ChordInter> chords = stack.getChords();

            // Determine augmentation dots for each chord
            for (ChordInter chord : chords) {
                chord.countDots();
            }
        }
    }
}
