//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S t a c k T u n e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.RestChordInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
 * To limit the number of configurations to check for correctness, we use equivalence classes.
 * For example, all heads in a chord have an augmentation dot or none of them, so it would be
 * useless to consider each of these dots as a separate variable.
 * A similar example (TODO: still to be implemented) relates to conflicting flag inters (perhaps a
 * mix of FLAG_1, FLAG_2, FLAG_3...) on the same head chord, here what really matters is the
 * resulting count of flags on the chord.
 * <p>
 * Time signatures have to be considered differently, since their value may be called into
 * question, based on intrinsic measure rhythm. Moreover, a two-pass approach is needed when the
 * current page does not start with a time signature.
 * <p>
 * Once a correct configuration has been chosen, we must care about conflicts between rhythm data
 * and other symbol-based items (for example a tuplet sign may conflict with a dynamic sign).
 * We give priority (frozen inter) to rhythm data (detected as correct) over non-rhythm data (even
 * if some non-rhythm data may exhibit higher grades than correct rhythm data).
 *
 * @author Hervé Bitteur
 */
public class StackTuner
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StackTuner.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated measure stack. */
    private final MeasureStack stack;

    /** Fail fast mode, just meant to guess expected duration. */
    private final boolean failFast;

    /** To temporarily save inters and their relations, outside of the standard sig. */
    private final StackBackup backup;

    /** Current configuration in stack. */
    private StackConfig config;

    /** Too close RestChordInter's to remove from current config. */
    private final Set<RestChordInter> toRemove = new LinkedHashSet<RestChordInter>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StackTuner} object.
     *
     * @param stack    the measure stack to process
     * @param failFast true for raw processing (meant only to guess expected measure duration)
     */
    public StackTuner (MeasureStack stack,
                       boolean failFast)
    {
        this.stack = stack;
        this.failFast = failFast;

        backup = new StackBackup(stack);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Process the stack to find out a correct configuration of rhythm data.
     *
     * @param systemGoodFrats The good FRAT inters at system level (non null)
     * @param initialDuration The expected duration for this stack, or null
     */
    public void process (List<Inter> systemGoodFrats,
                         Rational initialDuration)
    {
        stack.setExpectedDuration(initialDuration);
        stack.clearFrats();

        // Good FRAT data for the stack
        final List<Inter> goods = stack.filter(systemGoodFrats);
        Collections.sort(goods, Inter.byAbscissa);
        logger.debug("{} goods: {} {}", stack, goods.size(), Inters.ids(goods));
        populateGoodFrats(goods);

        // Initial config
        final StackConfig orgConfig = new StackConfig(goods);
        backup.save(goods);

        try {
            StackConfig correctConfig = checkConfig(orgConfig);

            if (correctConfig != null) {
                // Protect correct rhythm data against other symbols
                backup.freeze(correctConfig.getInters());
            } else if (!failFast) {
                SystemInfo system = stack.getSystem();
                logger.info("{}{} no correct rhythm", system.getLogPrefix(), stack);
            }
        } catch (Exception ex) {
            logger.warn("Error " + ex + " checkConfig " + orgConfig, ex);
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
     * Check the provided configuration.
     * <p>
     * If OK we return the configuration.
     * If not OK we return null, perhaps after posting additional config candidates.
     *
     * @param config newConfig the configuration to consider
     * @return the configuration if successful, null otherwise
     */
    private StackConfig checkConfig (StackConfig newConfig)
    {
        if (logger.isDebugEnabled()) {
            logger.info("Chk{} {}", newConfig.ids(), newConfig);
        }

        if (!newConfig.equals(config)) {
            config = newConfig;

            // Installation computes the time slots, and may fail
            if (!backup.install(config, toRemove, failFast)) {
                return null;
            }
        }

        // Check that each voice looks correct
        if (!failFast && checkVoices()) {
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
     *
     * @return true if all voices are OK, false otherwise
     */
    private boolean checkVoices ()
    {
        try {
            Rational stackDur = stack.getCurrentDuration();

            if (!stackDur.equals(Rational.ZERO)) {
                // Make sure the stack duration is not bigger than limit (TODO: why???)
                if (stackDur.compareTo(stack.getExpectedDuration()) <= 0) {
                    stack.setActualDuration(stackDur);
                } else {
                    stack.setActualDuration(stackDur);

                    ///stack.setActualDuration(stack.getExpectedDuration());
                }
            }

            stack.checkDuration(); // Compute voices terminations

            if (logger.isDebugEnabled()) {
                stack.printVoices(null);
            }

            Rational expectedDuration = stack.getExpectedDuration();
            logger.debug("{} expected:{} current:{}", stack, expectedDuration, stackDur);

            for (Voice voice : stack.getVoices()) {
                Rational voiceDur = voice.getDuration();
                TimeRational inferred = voice.getInferredTimeSignature();
                logger.debug("{} ends at {} ts: {}", voice, voiceDur, inferred);

                if (voiceDur != null) {
                    Rational delta = voiceDur.minus(expectedDuration);
                    final int sign = delta.compareTo(Rational.ZERO);

                    if (sign > 0) {
                        return false;
                    }
                }
            }

            return true; // Success!
        } catch (Exception ex) {
            logger.warn("StackTuner. Error visiting " + stack + " " + ex, ex);
        }

        return false;
    }

    //-------------------//
    // populateGoodFrats //
    //-------------------//
    /**
     * Populate the stack with the good FRAT Inter instances.
     *
     * @param stackGoods good FRAT inters at stack level
     */
    private void populateGoodFrats (List<Inter> stackGoods)
    {
        for (Inter inter : stackGoods) {
            stack.addInter(inter);
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

        private final Constant.Integer maxPoors = new Constant.Integer(
                "Inter count",
                5,
                "Maximum poor rhythm data considered for a measure stack");
    }
}
//    //---------//
//    // process //
//    //---------//
//    /**
//     * Process the stack to find out a correct configuration of rhythm data.
//     *
//     * @param systemGoodFrats The good FRAT inters at system level (non null)
//     * @param systemPoorFrats The poor FRAT data at system level, or null
//     * @param initialDuration The expected duration for this stack, or null
//     */
//    public void process (List<Inter> systemGoodFrats,
//                         SystemBackup systemPoorFrats,
//                         Rational initialDuration)
//    {
//        stack.setExpectedDuration(initialDuration);
//        stack.clearFrats();
//
//        // Good FRAT data for the stack
//        final List<Inter> goods = stack.filter(systemGoodFrats);
//        Collections.sort(goods, Inter.byAbscissa);
//        logger.debug("{} goods: {} {}", stack, goods.size(), Inters.ids(goods));
//        populateGoodFrats(goods);
//
//        // Poor FRAT data for the stack
//        final List<Inter> poors = filterPoors(systemPoorFrats);
//
//        // Initial config
//        final StackConfig orgConfig = new StackConfig(goods);
//        candidates.add(orgConfig);
//
//        // Determine possible partitions of all FRAT data (goods + poors)
//        if (fullMode && !poors.isEmpty()) {
//            logger.debug("{} poors: {} {}", stack, poors.size(), Inters.ids(poors));
//            systemPoorFrats.restore(poors);
//
//            final List<Inter> allFrat = new ArrayList<Inter>(goods);
//            allFrat.addAll(poors);
//            backup.save(allFrat);
//
//            final SIGraph sig = stack.getSystem().getSig();
//            List<List<Inter>> partitions = sig.getPartitions(null, allFrat);
//            addCandidates(partitions);
//        } else {
//            backup.save(goods);
//        }
//
//        StackConfig correctConfig = null; // The very first correct config found, if any
//        logger.debug("{} candidates: {}", stack, candidates.size()); // NR: always 1
//
//        // Process each identified configuration, until a correct one is found
//        for (candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
//            final StackConfig candidate = candidates.get(candidateIndex);
//            logger.debug("{} config {}/{}", stack, candidateIndex + 1, candidates.size());
//
//            if (!failures.contains(candidate)) {
//                try {
//                    correctConfig = checkConfig(candidate, false);
//
//                    if (correctConfig != null) {
//                        break;
//                    }
//                } catch (Exception ex) {
//                    logger.warn("Error " + ex + " checkConfig " + candidate, ex);
//                }
//            }
//        }
//
//        if (correctConfig != null) {
//            // Re-install the correctConfig if different from current config
//            if (!correctConfig.equals(config)) {
//                logger.debug("Re-installing correct config {}", correctConfig);
//                backup.install(correctConfig, toRemove, false);
//            }
//
//            // Protect correct rhythm data against other symbols
//            backup.freeze(correctConfig.getInters());
//        } else if (fullMode) {
//            logger.info(
//                    "{}*** {} no correct rhythm config",
//                    stack.getSystem().getLogPrefix(),
//                    stack);
//            // Too bad, simply re-install the original config
//            backup.install(orgConfig, toRemove, false);
//        }
//    }
//    //-----------//
//    // postRests //
//    //-----------//
//    /**
//     * Try to augment the global candidates list with configurations derived from the
//     * current one by removing rests.
//     * <p>
//     * The sequence of inserted candidates starts by the most promising ones.
//     *
//     * @param rests the list of rests, sorted by ascending grade
//     * @param delta (not used) the excess time on the voice being checked
//     */
//    private void postRests (List<AbstractChordInter> rests,
//                            Rational delta)
//    {
//        final int n = rests.size();
//
//        // This should be used only for rather small sizes of rests collection ...
//        final boolean[][] bools = Combinations.getVectors(n);
//
//        for (boolean[] vector : bools) {
//            StackConfig newConfig = config.copy();
//
//            for (int i = 0; i < n; i++) {
//                if (!vector[i]) {
//                    // Delete inter (and its augmentation dots) from config
//                    newConfig.remove(rests.get(i));
//                }
//            }
//
//            if (!failures.contains(newConfig)) {
//                if (!candidates.contains(newConfig)) {
//                    candidates.add(newConfig);
//                }
//            }
//        }
//    }
//
//    //-------------//
//    // removeChord //
//    //-------------//
//    /**
//     * Post a candidate, based on current config with the provided chord removed
//     *
//     * @param chord the chord to "remove" before posting a new candidate
//     */
//    private void removeChord (AbstractChordInter chord)
//    {
//        if (logger.isDebugEnabled() || chord.isVip()) {
//            logger.info("VIP removing {} causing close slots", chord);
//        }
//
//        StackConfig newConfig = config.copy();
//        newConfig.getInters().remove(chord);
//
//        if (!failures.contains(newConfig) && !candidates.contains(newConfig)) {
//            ///candidates.add(candidateIndex + 1, newConfig);
//            candidates.add(newConfig);
//        }
//    }
//
//    //-----------------//
//    // filterPartition //
//    //-----------------//
//    /**
//     * Filter the content of a partition for irrelevant inters.
//     * Augmentation dots need their augmented entity (head/rest or first dot)
//     *
//     * @param partition the partition to check and reduce if needed
//     */
//    private void filterPartition (List<Inter> partition)
//    {
//        final SIGraph sig = stack.getSystem().getSig();
//        final List<Inter> allDots = sig.inters(partition, AugmentationDotInter.class);
//        final List<Inter> secondDots = new ArrayList<Inter>();
//
//        if (!allDots.isEmpty()) {
//            logger.trace("filterPartition on {}", allDots);
//        }
//
//        // Pass #1 for simple augmentation of a rest
//        // Make sure the rest augmented by a dot is contained by the partition
//        for (Inter dot : allDots) {
//            Set<Relation> simpleRels = sig.getRelations(dot, AugmentationRelation.class);
//
//            if (simpleRels.isEmpty()) {
//                secondDots.add(dot); // Since this dot must be a second dot
//            } else {
//                boolean augFound = false;
//
//                for (Relation rel : simpleRels) {
//                    Inter augInter = sig.getOppositeInter(dot, rel);
//
//                    // If the augmented entity is a note head, it's OK
//                    if (augInter instanceof HeadInter) {
//                        augFound = true;
//
//                        break;
//                    }
//
//                    // Here, the augmented entity is a rest, make sure its chord is in the partition
//                    Inter restChord = augInter.getEnsemble();
//
//                    if (partition.contains(restChord)) {
//                        augFound = true;
//
//                        break;
//                    }
//                }
//
//                if (!augFound) {
//                    logger.debug("Isolated first {} removed from partition", dot);
//                    partition.remove(dot);
//                }
//            }
//        }
//
//        // Pass #2 for double augmentation
//        // Make sure the first dot (augmented by a second dot) is contained by the partition
//        // If not, remove the second dot.
//        for (Inter dot : secondDots) {
//            Set<Relation> doubleRels = sig.getRelations(dot, DoubleDotRelation.class);
//            boolean augFound = false;
//
//            for (Relation rel : doubleRels) {
//                Inter firstDot = sig.getOppositeInter(dot, rel);
//
//                if (partition.contains(firstDot)) {
//                    augFound = true;
//                }
//            }
//
//            if (!augFound) {
//                logger.debug("Isolated second {} removed from partition", dot);
//                partition.remove(dot);
//            }
//        }
//    }
//
//    //-------------//
//    // filterPoors //
//    //-------------//
//    /**
//     * Retrieve the stack poor data out of the system poor data.
//     * Limit the number of poor rhythms data by all means (this is very questionable!!!)
//     */
//    private List<Inter> filterPoors (SystemBackup systemPoors)
//    {
//        if ((systemPoors == null) || systemPoors.getSeeds().isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        final List<Inter> poors = stack.filter(systemPoors.getSeeds());
//        Collections.sort(poors, Inter.byReverseBestGrade);
//
//        final int maxCount = constants.maxPoors.getValue();
//
//        if (poors.size() > maxCount) {
//            // Drop the data with lower grade
//            return poors.subList(0, maxCount);
//        } else {
//            return poors;
//        }
//    }
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
//        Set<Inter> varSet = new LinkedHashSet<Inter>();
//        Slot lastSlot = stack.getLastSlot();
//
//        for (Slot slot : stack.getSlots()) {
//            for (AbstractChordInter chord : slot.getChords()) {
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

//
//    //---------------//
//    // improveConfig //
//    //---------------//
//    private StackConfig improveConfig (StackConfig goodConfig,
//                                       List<Inter> vars)
//    {
//        if (logger.isDebugEnabled()) {
//            logger.debug("Removed rest chords: {}", Inters.ids(vars));
//        }
//
//        Collections.sort(vars, Inter.byCenterAbscissa);
//        candidates.clear();
//        candidateIndex = -1;
//
//        final int n = vars.size();
//
//        // This should be used only for rather small sizes ...
//        final boolean[][] bools = Combinations.getVectors(n);
//        int targetIdx = candidateIndex + 1;
//
//        for (boolean[] vector : bools) {
//            StackConfig newConfig = config.copy();
//
//            for (int i = 0; i < n; i++) {
//                if (vector[i]) {
//                    newConfig.add(vars.get(i));
//                }
//            }
//
//            if (!failures.contains(newConfig)) {
//                if (!goodConfig.equals(newConfig)) {
//                    logger.debug("Inc{}", newConfig.ids());
//                    candidates.add(targetIdx++, newConfig);
//                }
//            }
//        }
//
//        // Try all combinations
//        List<StackConfig> betterConfigs = new ArrayList<StackConfig>();
//
//        for (candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
//            final StackConfig candidate = candidates.get(candidateIndex);
//
//            if (!failures.contains(candidate)) {
//                StackConfig betterConfig = checkConfig(candidate, true);
//
//                if (betterConfig != null) {
//                    betterConfigs.add(betterConfig);
//                }
//            }
//        }
//
//        logger.debug("{} better configs: {}", stack, betterConfigs.size());
//
//        if (!betterConfigs.isEmpty()) {
//            // Pick up the longest one?
//            if (betterConfigs.size() > 1) {
//                Collections.sort(betterConfigs, StackConfig.byReverseSize);
//            }
//
//            return betterConfigs.get(0);
//        } else {
//            return null;
//        }
//    }
//
