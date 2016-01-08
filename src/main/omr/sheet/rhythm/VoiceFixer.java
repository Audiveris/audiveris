//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       V o i c e F i x e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.score.LogicalPart;
import omr.score.Page;
import omr.score.Score;

import omr.sheet.Part;
import omr.sheet.SystemInfo;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.Inter;
import omr.sig.inter.SlurInter;
import omr.sig.relation.Relation;
import omr.sig.relation.SlurHeadRelation;
import static omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code VoiceFixer} connects voices and harmonizes their IDs (and thus colors)
 * within a stack, a system, a page or a score.
 *
 * @author Hervé Bitteur
 */
public abstract class VoiceFixer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(VoiceFixer.class);

    /** To sort voices by vertical position within their containing measure stack. */
    public static final Comparator<Voice> byOrdinate = new Comparator<Voice>()
    {
        @Override
        public int compare (Voice v1,
                            Voice v2)
        {
            if (v1.getMeasure().getStack() != v2.getMeasure().getStack()) {
                throw new IllegalArgumentException("Comparing voices in different stacks");
            }

            // Check if they are located in different parts
            Part p1 = v1.getMeasure().getPart();
            Part p2 = v2.getMeasure().getPart();

            if (p1 != p2) {
                return Part.byId.compare(p1, p2);
            }

            // Look for the first time slot with incoming chords for both voices.
            // If such slot exists, compare the two chords ordinates in that slot.
            Slot firstSlot1 = null;
            Slot firstSlot2 = null;

            for (Slot slot : v1.getMeasure().getStack().getSlots()) {
                Voice.SlotVoice vc1 = v1.getSlotInfo(slot);

                if ((vc1 == null) || (vc1.status != Voice.Status.BEGIN)) {
                    continue;
                }

                if (firstSlot1 == null) {
                    firstSlot1 = slot;
                }

                AbstractChordInter c1 = vc1.chord;

                Voice.SlotVoice vc2 = v2.getSlotInfo(slot);

                if ((vc2 == null) || (vc2.status != Voice.Status.BEGIN)) {
                    continue;
                }

                if (firstSlot2 == null) {
                    firstSlot2 = slot;
                }

                AbstractChordInter c2 = vc2.chord;

                return Inter.byOrdinate.compare(c1, c2);
            }

            // No common slot found, use index of first slot for each voice
            if ((firstSlot1 != null) && (firstSlot2 != null)) {
                return Integer.compare(firstSlot1.getId(), firstSlot2.getId());
            }

            // Use ordinate (there is a whole rest)
            AbstractChordInter c1 = v1.getFirstChord();
            AbstractChordInter c2 = v2.getFirstChord();

            return Inter.byOrdinate.compare(c1, c2);
        }
    };

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // refinePage //
    //------------//
    /**
     * Connect voices within the same logical part across all systems of a page,
     * paying attention to dummy parts that may exist.
     *
     * @param page the page to process
     */
    public static void refinePage (Page page)
    {
        final SystemInfo firstSystem = page.getFirstSystem();
        int voiceOffset = 0; // Offset for voice IDs in current part

        for (LogicalPart logicalPart : page.getLogicalParts()) {
            int logicalVoiceCount = 0;

            for (SystemInfo system : page.getSystems()) {
                Part part = system.getPartById(logicalPart.getId());

                if (part != null) {
                    boolean swapped = false;

                    if (system != firstSystem) {
                        // Check tied voices from previous system
                        final List<Voice> measureVoices = part.getFirstMeasure().getVoices();

                        for (Voice voice : measureVoices) {
                            Integer tiedId = getSystemTiedId(voice);

                            if ((tiedId != null) && (voice.getId() != tiedId)) {
                                part.swapVoiceId(voice.getId(), tiedId);
                                swapped = true;
                            }
                        }
                    }

                    final List<Integer> partIds = part.getVoiceIds();
                    final List<Integer> newPartVoices = new ArrayList<Integer>();

                    // Re-number IDs in part
                    for (int i = 0; i < partIds.size(); i++) {
                        final int id = partIds.get(i);
                        int newId = voiceOffset + i + 1;
                        newPartVoices.add(newId);

                        if (newId != id) {
                            part.swapVoiceId(i, newId);
                            swapped = true;
                        }
                    }

                    if (swapped) {
                        for (Measure measure : part.getMeasures()) {
                            measure.sortVoices();
                        }
                    }

                    part.setVoiceIds(newPartVoices);
                    logicalVoiceCount = Math.max(logicalVoiceCount, newPartVoices.size());
                }
            }

            // Voice IDs used in this logical part
            final List<Integer> newLogicalVoices = new ArrayList<Integer>();

            for (int i = 0; i < logicalVoiceCount; i++) {
                newLogicalVoices.add(voiceOffset + i + 1);
            }

            logicalPart.setVoiceIds(newLogicalVoices);
            logger.debug("{} voices:{}", logicalPart, newLogicalVoices);
            voiceOffset += logicalVoiceCount;
        }
    }

    //-------------//
    // refineScore //
    //-------------//
    /**
     * Connect voices within the same logical part across all pages of a score.
     *
     * @param score the score to process
     */
    public static void refineScore (Score score)
    {
    }

    //-------------//
    // refineStack //
    //-------------//
    /**
     * Refine voice IDs within a stack.
     * <p>
     * When this method is called, initial IDs have been assigned according to voice creation
     * (whole voices first, then slot voices, with each voice remaining in its part).
     * See Slot.buildVoices() and Slot.assignVoices() methods.
     * <p>
     * Here we simply rename the IDs from top to bottom.
     *
     * @param stack the stack to process
     */
    public static void refineStack (MeasureStack stack)
    {
        // Sort voices vertically in stack
        List<Voice> voices = new ArrayList<Voice>(stack.getVoices());
        Collections.sort(voices, byOrdinate);

        // Assign each voice ID according to its relative vertical position
        for (int i = 0; i < voices.size(); i++) {
            voices.get(i).setId(i + 1);
        }

        // Sort voices by ID within each measure
        for (Measure measure : stack.getMeasures()) {
            measure.sortVoices();
        }
    }

    //--------------//
    // refineSystem //
    //--------------//
    /**
     * Connect voices within the same part across all measures of a system.
     * <p>
     * When this method is called, each stack has a sequence of voices, the goal is now to
     * connect them from one stack to the other.
     *
     * @param system the system to process
     */
    public static void refineSystem (SystemInfo system)
    {
        final MeasureStack firstStack = system.getFirstMeasureStack();
        int count = 0; // Count of voice IDs in this system

        for (Part part : system.getParts()) {
            // Voice IDs assigned in this part
            final List<Integer> partIds = new ArrayList<Integer>();

            for (MeasureStack stack : system.getMeasureStacks()) {
                // Voices used within this measure
                final Measure measure = stack.getMeasureAt(part);
                final List<Voice> measureVoices = measure.getVoices(); // Sorted by ID & vertically

                if (stack != firstStack) {
                    // Check tied voices from previous measure
                    for (Voice voice : measureVoices) {
                        Integer tiedId = getMeasureTiedId(voice);

                        if ((tiedId != null) && (voice.getId() != tiedId)) {
                            measure.swapVoiceId(voice, tiedId);
                        }
                    }
                }

                // Connect un-assigned measure voices to part voices
                for (int i = 0; i < measureVoices.size(); i++) {
                    final Voice voice = measureVoices.get(i);
                    int newId = voice.getId();

                    if (!partIds.contains(newId)) {
                        partIds.add(newId = ++count); // Extend part list

                        if (voice.getId() != newId) {
                            measure.swapVoiceId(voice, newId);
                        }
                    }
                }
            }

            // Store voices IDs into their containing part
            logger.debug("System#{} {} {}", system.getId(), part, partIds);
            part.setVoiceIds(partIds);
        }
    }

    //------------------//
    // getMeasureTiedId //
    //------------------//
    /**
     * Check whether the provided (measure) voice is tied to a voice in previous measure
     * and thus must reuse the same ID.
     *
     * @param voice the voice to check
     * @return the imposed ID if any, null otherwise
     */
    private static Integer getMeasureTiedId (Voice voice)
    {
        final AbstractChordInter firstChord = voice.getFirstChord();
        final SIGraph sig = firstChord.getSig();

        // Is there an incoming tie on a head of this chord?
        for (Inter note : firstChord.getNotes()) {
            if (note instanceof AbstractHeadInter) {
                for (Relation r : sig.getRelations(note, SlurHeadRelation.class)) {
                    SlurHeadRelation shRel = (SlurHeadRelation) r;

                    if (shRel.getSide() == RIGHT) {
                        SlurInter slur = (SlurInter) sig.getOppositeInter(note, r);

                        if (slur.isTie()) {
                            AbstractHeadInter left = slur.getHead(LEFT);

                            if (left != null) {
                                final Voice leftVoice = left.getVoice();
                                logger.debug("{} ties {} to {}", slur, voice, leftVoice);

                                return leftVoice.getId();
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    //-----------------//
    // getSystemTiedId //
    //-----------------//
    /**
     * Check whether the provided (system) voice is tied to a voice in previous system
     * and thus must reuse the same ID.
     *
     * @param voice the voice to check
     * @return the imposed ID if any, null otherwise
     */
    private static Integer getSystemTiedId (Voice voice)
    {
        final AbstractChordInter firstChord = voice.getFirstChord();
        final SIGraph sig = firstChord.getSig();

        // Is there an incoming tie on a head of this chord?
        for (Inter note : firstChord.getNotes()) {
            if (note instanceof AbstractHeadInter) {
                for (Relation r : sig.getRelations(note, SlurHeadRelation.class)) {
                    SlurHeadRelation shRel = (SlurHeadRelation) r;

                    if (shRel.getSide() == RIGHT) {
                        SlurInter slur = (SlurInter) sig.getOppositeInter(note, r);

                        if (slur.isTie()) {
                            SlurInter prevSlur = slur.getExtension(LEFT);

                            if (prevSlur != null) {
                                AbstractHeadInter left = prevSlur.getHead(LEFT);

                                if (left != null) {
                                    final Voice leftVoice = left.getVoice();
                                    logger.debug("{} ties {} over to {}", slur, voice, leftVoice);

                                    return leftVoice.getId();
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}
