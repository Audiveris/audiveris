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
package omr.sheet.rhythm;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.math.Combinations;
import omr.math.Rational;

import omr.sheet.Part;
import omr.sheet.PartBarline;
import omr.sheet.Staff;
import omr.sheet.StaffBarline;
import omr.sheet.beam.BeamGroup;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.AugmentationDotInter;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.ChordInter;
import omr.sig.inter.FlagInter;
import omr.sig.inter.Inter;
import omr.sig.inter.Inters;
import omr.sig.inter.RepeatDotInter;
import omr.sig.inter.RestChordInter;
import omr.sig.inter.TupletInter;
import omr.sig.relation.AugmentationRelation;
import omr.sig.relation.DoubleDotRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.RepeatDotBarRelation;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code StackTuner} adjusts the rhythm content of a given MeasureStack.
 * <p>
 * These are two separate kinds of rhythm data:
 * <ol>
 * <li>The core of rhythm data is made of the head-based chords (and beam groups). It has already
 * been validated by previous steps and is thus never called into question again.</li>
 * <li>Rhythm data brought by symbol-based items (precisely: rest-based chords, flags, augmentation
 * dots and tuplets) are used as adjustment variables.</li>
 * </ol>
 * <p>
 * To limit the number of configurations to check for adjustment, we use equivalence classes.
 * For example, all heads in a chord have an augmentation dot or none of them, so it would be
 * useless to consider each of these dots as a separate variable.
 * A similar example relates to conflicting flag inters (perhaps a mix of FLAG_1, FLAG_2, FLAG_3...)
 * on the same head chord, here what really matters is the resulting count of flags on the chord.
 * <p>
 * Time signatures have to be considered differently, since their value may be called into
 * question, based on intrinsic measure rhythm. Moreover, a two-pass approach is needed when the
 * current page does not start with a time signature.
 * <p>
 * Once a good configuration has been chosen, we should care about conflicts between rhythm data and
 * other symbol-based items (for example a tuplet sign may conflict with a dynamic sign).
 * Perhaps give priority (frozen inter) to rhythm data over non-rhythm data?
 *
 * @author Hervé Bitteur
 */
public class StackTuner
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StackTuner.class);

    /** Relevant rhythm classes. */
    public static final Class<?>[] rhythmClasses = new Class<?>[]{
        ChordInter.class, // Chords (heads & rests)
        FlagInter.class, // (standard) Flags
        TupletInter.class, // Tuplet signs
        AugmentationDotInter.class // Augmentation dots
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated measure stack. */
    private final MeasureStack stack;

    /** Full mode (or raw mode: 1st phase meant to just guess expected duration). */
    private final boolean fullMode;

    /** To temporarily save inters and their relations, outside of the standard sig. */
    private final StackBackup backup;

    /** All configurations that were tested so far and failed. */
    private final Set<StackConfig> failures = new LinkedHashSet<StackConfig>();

    /** Configurations still to be tested for this stack. */
    private final List<StackConfig> candidates = new ArrayList<StackConfig>();

    /** Current index in candidates. */
    private int candidateIndex;

    /** Current configuration in stack. */
    private StackConfig config;

    /** Too close RestChordInter's to remove from current config. */
    private final Set<RestChordInter> toRemove = new HashSet<RestChordInter>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StackTuner} object.
     *
     * @param stack    the measure stack to process
     * @param fullMode true for full processing, false for raw processing (meant to guess expected
     *                 measure duration)
     */
    public StackTuner (MeasureStack stack,
                       boolean fullMode)
    {
        this.stack = stack;
        this.fullMode = fullMode;

        backup = new StackBackup(stack);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Process the stack to find out a suitable configuration of rhythm data.
     *
     * @param systemInters    The collection of relevant rhythm inters in containing system
     * @param systemOptionals The optional rhythm data at system level, or null
     * @param initialDuration The expected duration for this stack, or null
     */
    public void process (List<Inter> systemInters,
                         SystemBackup systemOptionals,
                         Rational initialDuration)
    {
        stack.setExpectedDuration(initialDuration);
        stack.clearInters();

        // Populate stack with relevant rhythm data
        populate(systemInters);

        final SIGraph sig = stack.getSystem().getSig();

        // Adjustment rhythm variables (no head-based chords)
        final List<Inter> rhythms = new ArrayList<Inter>();
        final List<ChordInter> allRestChords = new ArrayList<ChordInter>(stack.getRestChords());
        rhythms.addAll(allRestChords); // Rest-chords only (no Head-chords)
        rhythms.addAll(stack.getRhythms()); // Add all non-chord data
        logger.debug("{} rhythms: {}", stack, rhythms.size());

        // Initial config
        StackConfig orgConfig = new StackConfig(rhythms);
        candidates.add(orgConfig);

        // Determine possible partitions of rhythm data (augmented by discarded rhythm inters)
        if (fullMode && (systemOptionals != null) && !systemOptionals.getSeeds().isEmpty()) {
            List<Inter> optionals = stack.filter(systemOptionals.getSeeds());

            if (!optionals.isEmpty()) {
                logger.debug("{} optionals: {}", stack, optionals.size());

                // Limit the count of optionals
                optionals = filterOptionals(optionals);

                systemOptionals.restore(optionals);

                final List<Inter> allRhythms = new ArrayList<Inter>(rhythms);
                allRhythms.addAll(optionals);
                backup.save(allRhythms);

                List<List<Inter>> partitions = sig.getPartitions(null, allRhythms);

                for (List<Inter> partition : partitions) {
                    // Make sure that all inters are relevant in this partition
                    filterPartition(partition);

                    StackConfig cfg = new StackConfig(partition);

                    if (!candidates.contains(cfg)) {
                        candidates.add(cfg);
                    }
                }

                if (candidates.size() > 1) {
                    logger.debug("{} configs: {}", stack, candidates.size());
                }
            } else {
                backup.save(rhythms);
            }
        } else {
            backup.save(rhythms);
        }

        logger.debug("{} candidates: {}", stack, candidates.size());

        StackConfig goodConfig = null; // The very first good config found, if any

        // Process each identified configuration, until a good one is found
        for (candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
            final StackConfig candidate = candidates.get(candidateIndex);
            logger.debug("{} config {}/{}", stack, candidateIndex + 1, candidates.size());

            if (!failures.contains(candidate)) {
                try {
                    goodConfig = checkConfig(candidate, false);

                    if (goodConfig != null) {
                        break;
                    }
                } catch (Exception ex) {
                    logger.warn("Error " + ex + " checkConfig " + candidate, ex);
                }
            }
        }

        if (goodConfig != null) {
            // Re-install the goodConfig if different from current config, then freeze it
            if (!goodConfig.equals(config)) {
                logger.debug("Re-installing good config {}", goodConfig);
                backup.install(goodConfig, toRemove, false);
            }

            //
            //            // Try to re-insert removed rests chords? (TODO: rethink this!)
            //            List<Inter> keptRestChords = sig.inters(goodConfig.getInters(), RestChordInter.class);
            //            List<Inter> removedRestChords = new ArrayList<Inter>(allRestChords);
            //            removedRestChords.removeAll(keptRestChords);
            //
            //            if (!removedRestChords.isEmpty()) {
            //                StackConfig improved = improveConfig(goodConfig, removedRestChords);
            //
            //                if (improved != null) {
            //                    logger.debug("Installing better config {}", improved);
            //                    backup.install(improved, toRemove, false);
            //                    goodConfig = improved;
            //                }
            //            }
            //
            // Protect rhythm data against other symbols???
            backup.freeze(goodConfig.getInters());
        } else {
            // We failed
            if (fullMode) {
                logger.info("{}{} no good rhythm config", stack.getSystem().getLogPrefix(), stack);
                // We re-install the original config
                backup.install(orgConfig, toRemove, false);
            }
        }
    }

    //----------------//
    // resetFromSeeds //
    //----------------//
    public void resetFromSeeds ()
    {
        backup.resetFromSeeds();
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
     * @param config      newConfig the config to consider
     * @param improveMode true if we are trying to improve a good config (adding rather than
     *                    removing rhythms items)
     * @return the config if successful, null otherwise
     */
    private StackConfig checkConfig (StackConfig newConfig,
                                     boolean improveMode)
    {
        if (logger.isDebugEnabled()) {
            logger.info("Chk{} {}", newConfig.ids(), newConfig);
        }

        if (!newConfig.equals(config)) {
            config = newConfig;

            // Installation computes the time slots, and may fail
            if (!backup.install(config, toRemove, true)) {
                for (ChordInter chord : toRemove) {
                    removeChord(chord);
                }

                failures.add(config);

                //
                //                if (fullMode && !improveMode) {
                //                    postAlternatives();
                //                }
                //
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

                        logger.debug("{} Excess {} in {} from:{}", stack, delta, voice, rests);

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
                            logger.debug("{} Abnormal rest-only {} rests:{}", stack, voice, rests);

                            StackConfig newConfig = config.copy();
                            newConfig.getInters().removeAll(rests);

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
    // filterOptionals //
    //-----------------//
    /**
     * Limit the number of optional rhythms data by all means.
     */
    private List<Inter> filterOptionals (List<Inter> optionals)
    {
        final int maxCount = constants.maxOptionals.getValue();
        Collections.sort(optionals, Inter.byReverseBestGrade);

        if (optionals.size() > maxCount) {
            // Drop the data with lower grade
            return optionals.subList(0, maxCount);
        } else {
            return optionals;
        }
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

        if (!allDots.isEmpty()) {
            logger.debug("filterPartition on {}", allDots);
        }

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
    private StackConfig improveConfig (StackConfig goodConfig,
                                       List<Inter> vars)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Removed rest chords: {}", Inters.ids(vars));
        }

        Collections.sort(vars, Inter.byCenterAbscissa);
        candidates.clear();
        candidateIndex = -1;

        final int n = vars.size();

        // This should be used only for rather small sizes ...
        final boolean[][] bools = Combinations.getVectors(n);
        int targetIdx = candidateIndex + 1;

        for (boolean[] vector : bools) {
            StackConfig newConfig = config.copy();

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
        List<StackConfig> betterConfigs = new ArrayList<StackConfig>();

        for (candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
            final StackConfig candidate = candidates.get(candidateIndex);

            if (!failures.contains(candidate)) {
                StackConfig betterConfig = checkConfig(candidate, true);

                if (betterConfig != null) {
                    betterConfigs.add(betterConfig);
                }
            }
        }

        logger.debug("{} better configs: {}", stack, betterConfigs.size());

        if (!betterConfigs.isEmpty()) {
            // Pick up the longest one?
            if (betterConfigs.size() > 1) {
                Collections.sort(betterConfigs, StackConfig.byReverseSize);
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

    //
    //    //------------------//
    //    // postAlternatives //
    //    //------------------//
    //    /**
    //     * Try to post relevant alternatives to the current config with incorrect slots.
    //     * <p>
    //     * Slots were not correctly built, building stopped at wrong slot.
    //     * Determine the possible culprits among rhythm inters, typically all rhythm inters until
    //     * abscissa of wrong slot, plus any tuplet sign even located after wrong slot abscissa!
    //     */
    //    private void postAlternatives ()
    //    {
    //        // Determine the variables we can play with
    //        Set<Inter> varSet = new HashSet<Inter>();
    //        Slot lastSlot = stack.getLastSlot();
    //
    //        for (Slot slot : stack.getSlots()) {
    //            for (ChordInter chord : slot.getChords()) {
    //                if (chord instanceof RestChordInter) {
    //                    varSet.add(chord);
    //                }
    //            }
    //        }
    //
    //        for (Inter inter : config.getInters()) {
    //            if (inter instanceof TupletInter) {
    //                varSet.add(inter);
    //            }
    //        }
    //
    //        for (Inter inter : config.getInters()) {
    //            Staff staff = inter.getStaff();
    //            Point center = inter.getCenter();
    //            final double xOffset;
    //
    //            if (staff != null) {
    //                xOffset = stack.getXOffset(center, Arrays.asList(staff));
    //            } else {
    //                xOffset = stack.getXOffset(center);
    //            }
    //
    //            if (xOffset <= lastSlot.getXOffset()) {
    //                varSet.add(inter);
    //            } else {
    //                break;
    //            }
    //        }
    //
    //        List<Inter> vars = new ArrayList<Inter>(varSet);
    //        final int maxCount = constants.maxTotalRhythm.getValue();
    //        int n = vars.size();
    //
    //        if (n > maxCount) {
    //            Collections.sort(vars, Inter.byBestGrade);
    //            vars = vars.subList(0, maxCount);
    //            n = maxCount;
    //        }
    //
    //        Collections.sort(vars, Inter.byCenterAbscissa);
    //
    //        // This should be used only for rather small sizes ...
    //        final boolean[][] bools = Combinations.getVectors(n);
    //        int targetIdx = candidateIndex + 1;
    //
    //        for (boolean[] vector : bools) {
    //            StackConfig newConfig = config.copy();
    //
    //            for (int i = 0; i < n; i++) {
    //                if (!vector[i]) {
    //                    Inter inter = vars.get(i);
    //                    // Delete inter (and its augmentation dots) from config
    //                    newConfig.remove(inter);
    //                }
    //            }
    //
    //            if (!failures.contains(newConfig)) {
    //                if (!candidates.contains(newConfig)) {
    //                    logger.debug("Ins{}", newConfig.ids());
    //                    candidates.add(targetIdx++, newConfig);
    //                } else {
    //                    int idx = candidates.indexOf(newConfig);
    //
    //                    if (idx > targetIdx) {
    //                        Collections.swap(candidates, idx, targetIdx++);
    //                        logger.debug("Pro{}", newConfig.ids());
    //                    }
    //                }
    //            }
    //        }
    //    }
    //
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
            StackConfig newConfig = config.copy();

            for (int i = 0; i < n; i++) {
                if (!vector[i]) {
                    // Delete inter (and its augmentation dots) from config
                    newConfig.remove(rests.get(i));
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

    //-------------//
    // removeChord //
    //-------------//
    /**
     * Post a candidate, based on current config with the provided chord removed
     *
     * @param chord the chord to "remove" before posting a new candidate
     */
    private void removeChord (ChordInter chord)
    {
        if (logger.isDebugEnabled() || chord.isVip()) {
            logger.info("VIP removing {} causing close slots", chord);
        }

        StackConfig newConfig = config.copy();
        newConfig.getInters().remove(chord);

        if (!failures.contains(newConfig) && !candidates.contains(newConfig)) {
            candidates.add(candidateIndex + 1, newConfig);
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
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer maxOptionals = new Constant.Integer(
                "Inter count",
                5,
                "Maximum optional rhythm data considered for a measure stack");

        //
        //        private final Constant.Integer maxTotalRhythm = new Constant.Integer(
        //                "Inter count",
        //                5,
        //                "Maximum total rhythm data considered for a measure stack");
    }
}
