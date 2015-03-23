//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S t a c k T u n e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.math.Combinations;
import omr.math.Rational;

import omr.score.entity.StaffBarline;

import omr.sig.SIGraph;
import omr.sig.SigAttic;
import omr.sig.SigReducer;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.AugmentationDotInter;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.ChordInter;
import omr.sig.inter.FlagInter;
import omr.sig.inter.Inter;
import omr.sig.inter.InterEnsemble;
import omr.sig.inter.Inters;
import omr.sig.inter.RepeatDotInter;
import omr.sig.inter.RestChordInter;
import omr.sig.inter.RestInter;
import omr.sig.inter.TupletInter;
import omr.sig.relation.AugmentationRelation;
import omr.sig.relation.DoubleDotRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.RepeatDotBarRelation;

import omr.step.Step;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class {@code StackTuner} adjusts the rhythm content of a given MeasureStack.
 * <ol>
 * <li>Rhythm data brought by head-based chords and beam groups are considered as solid.</li>
 * <li>Rhythm data brought by symbol-based items (rest-based chords, flags, augmentation dots,
 * tuplets) are used as adjustment variables.</li>
 * <li>Time signatures have to be considered differently, since their value may be called into
 * question, based on intrinsic measure rhythm. Moreover, a two-pass approach is needed when the
 * current page does not start with a time signature.</li>
 * </ol>
 * Once a good configuration has been chosen, we should care about conflicts between rhythm data and
 * other symbol-based items (for example a tuplet sign may conflict with a dynamic sign).
 * Perhaps give priority (frozen inter) to rhythm data over non-rhythm data?
 *
 * @author Hervé Bitteur
 */
public class StackTuner
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StackTuner.class);

    /** Relevant rhythm classes. */
    public static final Class<?>[] rhythmClasses = new Class<?>[]{
        ChordInter.class, // Chords (heads & rests)
        FlagInter.class, // Flags
        TupletInter.class, // Tuplet signs
        AugmentationDotInter.class // Augmentation dots
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated measure stack. */
    private final MeasureStack stack;

    /** Full mode (or raw mode: 1st phase meant to just guess expected duration). */
    private final boolean fullMode;

    /** True when trying to improve a good configuration. */
    private boolean improveMode = false;

    /** To temporarily save inters and their relations, outside of the standard sig. */
    private final StackBackup backup;

    /** All configurations that were tested so far and failed. */
    private final Set<Config> failures = new LinkedHashSet<Config>();

    /** Configurations still to be tested for this stack. */
    private final List<Config> candidates = new ArrayList<Config>();

    /** Current index in candidates. */
    private int candidateIndex;

    /** Current configuration in stack. */
    private Config config;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StackTuner} object.
     *
     * @param stack    the measure stack to process
     * @param fullMode true for full processing, false for raw processing
     */
    public StackTuner (MeasureStack stack,
                       boolean fullMode)
    {
        this.stack = stack;
        this.fullMode = fullMode;

        backup = new StackBackup();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Process the stack to find out a suitable configuration of rhythm data.
     *
     * @param systemInters    The collection of relevant rhythm inters in containing system
     * @param initialDuration The expected duration for this stack, or null
     */
    public void process (List<Inter> systemInters,
                         Rational initialDuration)
    {
        stack.setExpectedDuration(initialDuration);
        stack.clearInters();

        // Populate stack with relevant rhythm data
        populate(systemInters);

        final SIGraph sig = stack.getSystem().getSig();

        // Adjustment variables (no head-based chords)
        final List<ChordInter> allRestChords = new ArrayList<ChordInter>(stack.getRestChords());
        List<Inter> vars = new ArrayList<Inter>();
        vars.addAll(allRestChords);
        vars.addAll(stack.getRhythms());

        // Backup all stack rhythm data (except the head-based chords)
        backup.save(vars);

        // Do we have potential exclusions between adjustment variables?
        if (Step.RHYTHMS.compareTo(Step.SYMBOL_REDUCTION) < 0) {
            // Use this branch if RHYTHMS is run before SYMBOL_REDUCTION
            // Determine possible partitions of rhythm data
            SigReducer.detectOverlaps(vars);

            List<List<Inter>> partitions = sig.getPartitions(null, vars);

            for (List<Inter> partition : partitions) {
                // Make sure that all inters are relevant in this partition
                filterPartition(partition);

                Config cfg = new Config(partition);

                if (!candidates.contains(cfg)) {
                    candidates.add(cfg);
                }
            }
        } else {
            // Use this branch if symbols have already been reduced
            candidates.add(new Config(vars));
        }

        Config goodConfig = null; // The very first good config found, if any

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
                logger.debug("Re-installing good config {}", goodConfig);
                backup.install(goodConfig);
            }

            // Try to re-insert removed rests chords?
            List<Inter> keptRestChords = sig.inters(goodConfig.inters, RestChordInter.class);
            List<Inter> removedRestChords = new ArrayList<Inter>(allRestChords);
            removedRestChords.removeAll(keptRestChords);

            if (!removedRestChords.isEmpty()) {
                Config improved = improveConfig(goodConfig, removedRestChords);

                if (improved != null) {
                    logger.debug("Installing better config {}", improved);
                    backup.install(improved);
                    goodConfig = improved;
                }
            }

            backup.freeze(goodConfig.inters);
        } else {
            // TODO: in which config should we leave this stack???
            if (fullMode) {
                logger.warn("No good config found for {}", stack);
            }
        }
    }

    //---------------//
    // resetInitials //
    //---------------//
    public void resetInitials ()
    {
        backup.resetInitials();
    }

    //-------------//
    // removeChord //
    //-------------//
    /**
     * Post a candidate, based on current config with the provided chord removed
     *
     * @param chord the chord to "remove" before posting a new candidate
     */
    void removeChord (ChordInter chord)
    {
        if (logger.isDebugEnabled() || chord.isVip()) {
            logger.info("VIP removing {} causing close slots", chord);
        }

        Config newConfig = config.copy();
        newConfig.inters.remove(chord);

        if (!failures.contains(newConfig) && !candidates.contains(newConfig)) {
            candidates.add(candidateIndex + 1, newConfig);
        }
    }

    //-------------//
    // checkConfig //
    //-------------//
    /**
     * Check the provided config.
     * <p>
     * If OK we return the config.
     * If not OK we return null, perhaps after posting additional config candidates.
     *
     * @param config newConfig the config to consider
     * @return the config if successful, null otherwise
     */
    private Config checkConfig (Config newConfig)
    {
        if (logger.isDebugEnabled()) {
            logger.info("Chk{} {}", newConfig.ids(), newConfig);
        }

        if (!newConfig.equals(config)) {
            config = newConfig;

            // Installation computes the time slots, and may fail
            if (!backup.install(config)) {
                failures.add(config);

                if (fullMode && !improveMode) {
                    postAlternatives();
                }

                return null;
            }
        }

        // Check that each voice looks correct
        if ((fullMode || improveMode) && checkVoices()) {
            return newConfig;
        } else {
            return null;
        }
    }

    //-------------//
    // checkVoices //
    //-------------//
    /**
     * Check validity of every voice in stack.
     * At first invalid voice encountered, we may suggest new configs (inserted into candidates
     * list for later check) and return false;
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

            // Compute voices terminations
            stack.checkDuration();

            if (logger.isDebugEnabled()) {
                stack.printVoices(null);
            }

            Rational actualDuration = stack.getActualDuration();
            logger.debug(
                    "{} expected:{} actual:{} current:{}",
                    stack,
                    stack.getExpectedDuration(),
                    actualDuration,
                    stackDur);

            for (Voice voice : stack.getVoices()) {
                Rational voiceDur = voice.getDuration();

                logger.debug(
                        "{} ends at {} ts: {}",
                        voice,
                        voiceDur,
                        voice.getInferredTimeSignature());

                if (voiceDur != null) {
                    Rational delta = voiceDur.minus(actualDuration);
                    final int sign = delta.compareTo(Rational.ZERO);

                    if (sign == 0) {
                        // OK for this voice
                        ///logger.info("OK {}", config.ids());
                    } else if (sign > 0) {
                        failures.add(config);

                        // Voice is too long: try to shorten this voice
                        // Removing a rest
                        // Removing a dot (TODO)
                        // Inserting a tuplet (TODO)
                        List<ChordInter> rests = voice.getRests();
                        Collections.sort(rests, Inter.byReverseGrade);

                        logger.info("{} Excess {} in {} from:{}", stack, delta, voice, rests);

                        if (!rests.isEmpty()) {
                            postRests(rests, delta);
                        }

                        return false;
                    } else {
                        // Voice is too short, this does not necessarily invalidate the config.
                        // If voice is made of only rest(s), delete it.
                        if (voice.isOnlyRest()) {
                            failures.add(config);

                            List<ChordInter> rests = voice.getRests();
                            logger.info("{} Abnormal rest-only {} rests:{}", stack, voice, rests);

                            Config newConfig = config.copy();
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
        } catch (Exception ex) {
            logger.warn(getClass().getSimpleName() + " Error visiting " + stack, ex);
        }

        return false;
    }

    //-----------------//
    // filterPartition //
    //-----------------//
    /**
     * Filter the content of a partition for irrelevant inters.
     * Augmentation dots need their augmented entity (head/rest or first dot)
     *
     * @param partition the partition to check and reduce if needed
     */
    private void filterPartition (List<Inter> partition)
    {
        final SIGraph sig = stack.getSystem().getSig();
        final List<Inter> allDots = sig.inters(partition, AugmentationDotInter.class);
        final List<Inter> secondDots = new ArrayList<Inter>();
        logger.debug("filterPartition on {}", allDots);

        // Pass #1 for simple augmentation of a rest
        // Make sure the rest augmented by a dot is contained by the partition
        for (Inter dot : allDots) {
            Set<Relation> simpleRels = sig.getRelations(dot, AugmentationRelation.class);

            if (simpleRels.isEmpty()) {
                secondDots.add(dot); // Since this dot must be a second dot
            } else {
                boolean augFound = false;

                for (Relation rel : simpleRels) {
                    Inter augInter = sig.getOppositeInter(dot, rel);

                    // If the augmented entity is a note head, it's OK
                    if (augInter instanceof AbstractHeadInter) {
                        augFound = true;

                        break;
                    }

                    // Here, the augmented entity is a rest, make sure its chord is in the partition
                    Inter restChord = augInter.getEnsemble();

                    if (partition.contains(restChord)) {
                        augFound = true;

                        break;
                    }
                }

                if (!augFound) {
                    logger.debug("Isolated first {} removed from partition", dot);
                    partition.remove(dot);
                }
            }
        }

        // Pass #2 for double augmentation
        // Make sure the first dot (augmented by a second dot) is contained by the partition
        // If not, remove the second dot.
        for (Inter dot : secondDots) {
            Set<Relation> doubleRels = sig.getRelations(dot, DoubleDotRelation.class);
            boolean augFound = false;

            for (Relation rel : doubleRels) {
                Inter firstDot = sig.getOppositeInter(dot, rel);

                if (partition.contains(firstDot)) {
                    augFound = true;
                }
            }

            if (!augFound) {
                logger.debug("Isolated second {} removed from partition", dot);
                partition.remove(dot);
            }
        }
    }

    //---------------//
    // improveConfig //
    //---------------//
    private Config improveConfig (Config goodConfig,
                                  List<Inter> vars)
    {
        improveMode = true;
        if (logger.isDebugEnabled()) {
            logger.info("Removed rest chords: {}", Inters.ids(vars));
        }
        Collections.sort(vars, Inter.byCenterAbscissa);
        candidates.clear();
        candidateIndex = -1;

        final int n = vars.size();

        // This should be used only for rather small sizes ...
        final boolean[][] bools = Combinations.getVectors(n);
        int targetIdx = candidateIndex + 1;

        for (boolean[] vector : bools) {
            Config newConfig = config.copy();

            for (int i = 0; i < n; i++) {
                if (vector[i]) {
                    newConfig.add(vars.get(i));
                }
            }

            if (!failures.contains(newConfig)) {
                if (!goodConfig.equals(newConfig)) {
                    logger.debug("Inc{}", newConfig.ids());
                    candidates.add(targetIdx++, newConfig);
                }
            }
        }

        // Try all combinations
        List<Config> betterConfigs = new ArrayList<Config>();

        for (candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
            final Config candidate = candidates.get(candidateIndex);

            if (!failures.contains(candidate)) {
                Config betterConfig = checkConfig(candidate);

                if (betterConfig != null) {
                    betterConfigs.add(betterConfig);
                }
            }
        }

        improveMode = false;
        logger.debug("{} better configs: {}", stack, betterConfigs.size());

        if (!betterConfigs.isEmpty()) {
            // Pick up the longest one?
            if (betterConfigs.size() > 1) {
                Collections.sort(betterConfigs, Config.byReverseSize);
            }

            return betterConfigs.get(0);
        } else {
            return null;
        }
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
        // Populate (system) measure stack
        final List<Inter> stackInters = stack.filter(systemInters);

        for (Inter inter : stackInters) {
            stack.addInter(inter);
        }

        // Allocate beam groups
        BeamGroup.populate(stack);

        // Check repeat dots on left & right sides
        verifyRepeatDots();
    }

    //------------------//
    // postAlternatives //
    //------------------//
    /**
     * Try to post relevant alternatives to the current config with incorrect slots.
     * Slots were not correctly built, building stopped at wrong slot.
     * Determine the possible culprits among rhythm inters, typically all rhythm inters until
     * abscissa of wrong slot, plus any tuplet sign even located after wrong slot abscissa!
     */
    private void postAlternatives ()
    {
        // Determine the variables we can play with
        Set<Inter> varSet = new HashSet<Inter>();
        Slot lastSlot = stack.getLastSlot();

        for (Slot slot : stack.getSlots()) {
            for (ChordInter chord : slot.getChords()) {
                if (chord instanceof RestChordInter) {
                    varSet.add(chord);
                }
            }
        }

        for (Inter inter : config.inters) {
            if (inter instanceof TupletInter) {
                varSet.add(inter);
            }
        }

        for (Inter inter : config.inters) {
            Staff staff = inter.getStaff();
            Point center = inter.getCenter();
            final double xOffset;

            if (staff != null) {
                xOffset = stack.getXOffset(center, Arrays.asList(staff));
            } else {
                xOffset = stack.getXOffset(center);
            }

            if (xOffset <= lastSlot.getXOffset()) {
                varSet.add(inter);
            } else {
                break;
            }
        }

        final List<Inter> vars = new ArrayList<Inter>(varSet);
        Collections.sort(vars, Inter.byCenterAbscissa);

        final int n = vars.size();

        // This should be used only for rather small sizes ...
        final boolean[][] bools = Combinations.getVectors(n);
        int targetIdx = candidateIndex + 1;

        for (boolean[] vector : bools) {
            Config newConfig = config.copy();

            for (int i = 0; i < n; i++) {
                if (!vector[i]) {
                    Inter inter = vars.get(i);
                    // Delete inter (and its augmentation dots) from config
                    newConfig.remove(inter);
                }
            }

            if (!failures.contains(newConfig)) {
                if (!candidates.contains(newConfig)) {
                    logger.debug("Ins{}", newConfig.ids());
                    candidates.add(targetIdx++, newConfig);
                } else {
                    int idx = candidates.indexOf(newConfig);

                    if (idx > targetIdx) {
                        Collections.swap(candidates, idx, targetIdx++);
                        logger.debug("Pro{}", newConfig.ids());
                    }
                }
            }
        }
    }

    //-----------//
    // postRests //
    //-----------//
    /**
     * Try to augment the global candidates list with configurations derived from the
     * current one by removing rests.
     * <p>
     * The sequence of inserted candidates starts by the most promising ones.
     *
     * @param rests the list of rests, sorted by ascending grade
     * @param delta (not used) the excess time on the voice being checked
     */
    private void postRests (List<ChordInter> rests,
                            Rational delta)
    {
        final int n = rests.size();

        // This should be used only for rather small sizes of rests collection ...
        final boolean[][] bools = Combinations.getVectors(n);
        int targetIdx = candidateIndex + 1;

        for (boolean[] vector : bools) {
            Config newConfig = config.copy();

            for (int i = 0; i < n; i++) {
                if (!vector[i]) {
                    // Delete inter (and its augmentation dots) from config
                    newConfig.remove(rests.get(i));
                }
            }

            if (!failures.contains(newConfig)) {
                if (!candidates.contains(newConfig)) {
                    logger.info("Ins{}", newConfig.ids());
                    candidates.add(targetIdx++, newConfig);
                } else {
                    int idx = candidates.indexOf(newConfig);

                    if (idx > targetIdx) {
                        Collections.swap(candidates, idx, targetIdx++);
                        logger.info("Pro{}", newConfig.ids());
                    }
                }
            }
        }
    }

    //------------------//
    // verifyRepeatDots //
    //------------------//
    /**
     * Look for repeat sign on each side of the measure stack.
     * If positive, flag the related stack bar-line(s) as such and delete any other
     * interpretations
     * for these repeat dots.
     */
    private void verifyRepeatDots ()
    {
        final SIGraph sig = stack.getSystem().getSig();

        for (HorizontalSide side : HorizontalSide.values()) {
            final List<RepeatDotInter> dots = new ArrayList<RepeatDotInter>();
            int barCount = 0;

            for (Measure measure : stack.getMeasures()) {
                final Part part = measure.getPart();
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
    /**
     * Meant to store one configuration of rhythm adjustable data.
     */
    private static class Config
    {
        //~ Static fields/initializers -------------------------------------------------------------

        /** Compare configs by their decreasing size. */
        public static Comparator<Config> byReverseSize = new Comparator<Config>()
        {
            @Override
            public int compare (Config c1,
                                Config c2)
            {
                return Integer.compare(c2.inters.size(), c1.inters.size());
            }
        };

        //~ Instance fields ------------------------------------------------------------------------
        /** Rhythm data for this config, always ordered byFullAbscissa. */
        private final TreeSet<Inter> inters = new TreeSet<Inter>(Inter.byFullAbscissa);

        //~ Constructors ---------------------------------------------------------------------------
        public Config (Collection<? extends Inter> inters)
        {
            this.inters.addAll(inters);
        }

        //~ Methods --------------------------------------------------------------------------------
        public void add (Inter inter)
        {
            // Add inter to config
            inters.add(inter);

            // As well as its augmentation dot(s) if relevant (checked in backup sig)
            if (inter instanceof RestChordInter) {
                RestChordInter restChord = (RestChordInter) inter;
                RestInter rest = (RestInter) restChord.getNotes().get(0);
                AugmentationDotInter firstDot = rest.getFirstAugmentationDot();

                if (firstDot != null) {
                    inters.add(firstDot);

                    AugmentationDotInter secondDot = firstDot.getSecondAugmentationDot();

                    if (secondDot != null) {
                        inters.add(secondDot);
                    }
                }
            }
        }

        public Config copy ()
        {
            return new Config(inters);
        }

        @Override
        public boolean equals (Object obj)
        {
            if (!(obj instanceof Config)) {
                return false;
            }

            Config that = (Config) obj;

            return inters.equals(that.inters);
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;

            return hash;
        }

        public String ids ()
        {
            return Inters.ids(inters);
        }

        public void remove (Inter inter)
        {
            // Remove the inter from config
            inters.remove(inter);

            // As well as its augmentation dot(s) if relevant
            if (inter instanceof RestChordInter) {
                RestChordInter restChord = (RestChordInter) inter;
                RestInter rest = (RestInter) restChord.getNotes().get(0);
                AugmentationDotInter firstDot = rest.getFirstAugmentationDot();

                if (firstDot != null) {
                    inters.remove(firstDot);

                    AugmentationDotInter secondDot = firstDot.getSecondAugmentationDot();

                    if (secondDot != null) {
                        inters.remove(secondDot);
                    }
                }
            }
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
    private class StackBackup
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Initial rhythm data. */
        private List<Inter> initials;

        /** The SIG where work is done. */
        private final SIGraph sig;

        /** The attic where data can be saved to and restored from. */
        private final SigAttic attic = new SigAttic();

        //~ Constructors ---------------------------------------------------------------------------
        public StackBackup ()
        {
            sig = stack.getSystem().getSig();
        }

        //~ Methods --------------------------------------------------------------------------------
        //--------//
        // freeze //
        //--------//
        public void freeze (Collection<? extends Inter> keptInters)
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

        //---------//
        // install //
        //---------//
        /**
         * Try to install the provided configuration.
         *
         * @param config the configuration to install
         * @return true if successful, false if an error was detected
         */
        public boolean install (Config config)
        {
            // Clear the stack
            for (Inter inter : initials) {
                stack.removeInter(inter);
                inter.delete();
            }

            // Restore just the partition
            attic.restore(sig, config.inters);

            for (Inter inter : config.inters) {
                stack.addInter(inter);
            }

            // Reset all rhythm data within the stack
            stack.resetRhythm();

            // Count augmentation dots on chords
            countChordDots();

            // Link tuplets
            List<TupletInter> toDelete = new TupletsBuilder(stack).linkTuplets();

            if (!toDelete.isEmpty()) {
                config.inters.removeAll(toDelete);
            }

            // Build slots & voices
            return new SlotsBuilder(stack, StackTuner.this).process();
        }

        //---------------//
        // resetInitials //
        //---------------//
        public void resetInitials ()
        {
            // Clear any inter from initials
            for (Inter inter : initials) {
                stack.removeInter(inter);
            }

            // Restore the initial config
            attic.restore(sig, initials);
        }

        //------//
        // save //
        //------//
        public void save (Collection<Inter> inters)
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
