//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S t a c k T u n e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import org.audiveris.omr.math.Rational;
import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.TupletInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Class {@code StackTuner} computes the rhythm content of a given MeasureStack.
 * <p>
 * These are two separate kinds of rhythm data:
 * <ol>
 * <li>The core of rhythm data is made of the head-based chords (and beam groups). It has already
 * been validated by previous steps.</li>
 * <li>Rhythm data brought by symbol-based items (flags, rest-based chords, augmentation dots and
 * tuplets) are named FRATs.</li>
 * </ol>
 * <p>
 * This class builds the time slots and voices that result from stack content, and checks for the
 * "time correctness" of the stack.
 *
 * @author Hervé Bitteur
 */
public class StackTuner
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StackTuner.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated measure stack. */
    private final MeasureStack stack;

    /** Fail fast mode, just meant to guess expected duration. */
    private final boolean failFast;

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
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Process the stack to find out a correct configuration of rhythm data.
     *
     * @param initialDuration The expected duration for this stack, or null
     */
    public void process (Rational initialDuration)
    {
        stack.setExpectedDuration(initialDuration);

        try {
            if (!check() && !failFast) {
                logger.info("{}{} no correct rhythm", stack.getSystem().getLogPrefix(), stack);
            }
        } catch (Exception ex) {
            logger.warn("Error " + ex + " checkConfig ", ex);
        }
    }

    //-------//
    // check //
    //-------//
    /**
     * Check stack current configuration.
     *
     * @return OK if the configuration if correct
     */
    private boolean check ()
    {
        // Compute and check the time slots
        if (!checkSlots(failFast)) {
            return false;
        }

        // Check that each voice looks correct
        return !failFast && checkVoices();
    }

    //------------//
    // checkSlots //
    //------------//
    /**
     * Check the resulting time slots of current stack configuration.
     *
     * @param failFast true to stop processing on first error
     * @return true if successful, false if an error was detected
     */
    private boolean checkSlots (boolean failFast)
    {
        // Reset all rhythm data within the stack
        stack.resetRhythm();

        // Count augmentation dots on chords
        // (this implies that chord notes are present with their potential relation to dot)
        countChordDots();

        // Link tuplets
        final Set<TupletInter> toDelete = new TupletsBuilder(stack).linkStackTuplets();

        if (!toDelete.isEmpty()) {
            for (TupletInter tuplet : toDelete) {
                tuplet.remove();
            }
        }

        // Build slots & voices
        return new SlotsBuilder(stack, failFast).process();
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

    //----------------//
    // countChordDots //
    //----------------//
    private void countChordDots ()
    {
        // Determine augmentation dots for each chord
        for (AbstractChordInter chord : stack.getStandardChords()) {
            chord.countDots();
        }
    }
}
