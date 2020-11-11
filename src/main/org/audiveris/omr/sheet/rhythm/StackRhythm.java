//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S t a c k R h y t h m                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.inter.AbstractChordInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Class {@code StackRhythm} computes the rhythm content of a given MeasureStack.
 * <p>
 * This class builds the time slots and voices that result from stack content, and checks for the
 * "time correctness" of the stack.
 *
 * @author Hervé Bitteur
 */
public class StackRhythm
{

    private static final Logger logger = LoggerFactory.getLogger(StackRhythm.class);

    /** The dedicated measure stack. */
    private final MeasureStack stack;

    /**
     * Creates a new {@code StackRhythm} object.
     *
     * @param stack the measure stack to process
     */
    public StackRhythm (MeasureStack stack)
    {
        this.stack = stack;
    }

    //---------//
    // process //
    //---------//
    /**
     * Determine stack rhythm data (voices and timing).
     *
     * @param expectedDuration The expected duration for this stack, null if not known
     */
    public void process (Rational expectedDuration)
    {
        stack.setExpectedDuration(expectedDuration);

        try {
            if (!doProcess()) {
                logger.info("{}{} no correct rhythm", stack.getSystem().getLogPrefix(), stack);
            }
        } catch (Exception ex) {
            logger.warn("StackRhythm error processing {} " + ex, stack, ex);
        }
    }

    //-------------------------//
    // readStackActualDuration //
    //-------------------------//
    /**
     * Retrieve stack actual duration.
     */
    private void readStackActualDuration ()
    {
        try {
            // Determine stack actual duration
            final Rational actualDuration;

            if (stack.getVoices().isEmpty()) {
                actualDuration = Rational.ZERO; // No voice, hence no duration
            } else {
                Rational slotsDur = stack.getSlotsDuration();

                if (!slotsDur.equals(Rational.ZERO)) {
                    actualDuration = slotsDur; // Slots found, use slots-based duration
                } else {
                    actualDuration = stack.getExpectedDuration(); // No slot, just whole/multi rests
                }
            }

            stack.setActualDuration(actualDuration);
        } catch (Exception ex) {
            logger.warn("StackRhythm. Error visiting " + stack + " " + ex, ex);
        }
    }

    //----------------------//
    // checkStavesAreFilled //
    //----------------------//
    /**
     * Except in "merged grand staff" case, no staff should be left empty.
     *
     * @return true if OK
     */
    private boolean checkStavesAreFilled ()
    {
        // Use a temporary map: (staff -> chords)
        Map<Staff, List<AbstractChordInter>> map = new HashMap<>();

        for (Staff staff : stack.getSystem().getStaves()) {
            if (!staff.isTablature()) {
                map.put(staff, new ArrayList<>());
            }
        }

        // Populate map of staves with installed chords
        for (Measure measure : stack.getMeasures()) {
            for (AbstractChordInter chord : measure.getStandardChords()) {
                for (Staff staff : chord.getStaves()) {
                    map.get(staff).add(chord);
                }
            }
        }

        // Look for empty staves
        for (Map.Entry<Staff, List<AbstractChordInter>> entry : map.entrySet()) {
            if (entry.getValue().isEmpty()) {
                logger.info("{} staff#{} is empty", stack, entry.getKey().getId());

                return false;
            }
        }

        return true;
    }

    //-----------//
    // doProcess //
    //-----------//
    /**
     * Try to process stack rhythm.
     *
     * @return OK if successful
     */
    private boolean doProcess ()
    {
        stack.resetRhythm();

        boolean stackOk = true;

        // Process measure by measure
        for (Measure measure : stack.getMeasures()) {
            final boolean measureOk = new MeasureRhythm(measure).process();

            if (!measureOk) {
                measure.setAbnormal(true);
            }

            stackOk &= measureOk;
        }

        generateStackSlots(); // Gather all chords into stack slots

        readStackActualDuration(); // Read stack actual duration

        return stackOk;
    }

    //--------------------//
    // generateStackSlots //
    //--------------------//
    /**
     * Using the voices detected in each measure of the stack, dispatch all chords in
     * separate stack-level slots according to their time value.
     */
    private void generateStackSlots ()
    {
        stack.getSlots().clear();

        int slotCount = 0;
        final TreeMap<Rational, List<AbstractChordInter>> times = new TreeMap<>();

        for (Measure measure : stack.getMeasures()) {
            for (Voice voice : measure.getVoices()) {
                if (!voice.isWhole()) {
                    for (AbstractChordInter chord : voice.getChords()) {
                        Rational time = chord.getTimeOffset();

                        if (time != null) {
                            List<AbstractChordInter> list = times.get(time);

                            if (list == null) {
                                times.put(time, list = new ArrayList<>());
                            }

                            list.add(chord);
                        } else {
                            measure.setAbnormal(true);
                        }
                    }
                }
            }
        }

        TreeMap<Rational, Slot> timeToSlot = new TreeMap<>();

        for (Entry<Rational, List<AbstractChordInter>> entry : times.entrySet()) {
            Slot slot = new Slot(++slotCount, stack, entry.getValue());
            slot.setTimeOffset(entry.getKey());
            timeToSlot.put(entry.getKey(), slot);
            stack.getSlots().add(slot);
        }

        for (Measure measure : stack.getMeasures()) {
            for (Voice voice : measure.getVoices()) {
                if (!voice.isWhole()) {
                    for (AbstractChordInter chord : voice.getChords()) {
                        Rational time = chord.getTimeOffset();

                        if (time != null) {
                            Slot slot = timeToSlot.get(time);
                            voice.putSlotInfo(slot, new SlotVoice(chord, SlotVoice.Status.BEGIN));
                        } else {
                            measure.setAbnormal(true);
                        }
                    }

                    voice.completeSlotTable();
                }
            }
        }
    }
}
