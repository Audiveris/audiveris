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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code VoiceFixer} harmonizes the IDs (and thus colors) for the voices
 * of a stack, a system, a page or a score.
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
    /**
     * Connect voices within the same logical part across all systems of a page,
     * paying attention to dummy parts that may exist.
     *
     * @param page the page to process
     */
    public static void refinePage (Page page)
    {
        // Number of voice IDs for each LogicalPart within this page
        final Map<LogicalPart, Integer> globalMap = new LinkedHashMap<LogicalPart, Integer>();

        for (SystemInfo system : page.getSystems()) {
            for (Part part : system.getParts()) {
                // Voice IDs that start within this part
                final List<Integer> incomings = part.getVoiceIds();

                // Number of Global IDs already assigned in this part
                Integer globals = globalMap.get(part.getLogicalPart());

                if ((globals == null) || (globals < incomings.size())) {
                    globalMap.put(part.getLogicalPart(), incomings.size());
                }
            }
        }

        int voiceOffset = 0; // Offset for voice ids in current part

        for (LogicalPart logicalPart : page.getLogicalParts()) {
            logger.info("{} voices:{}", logicalPart, globalMap.get(logicalPart));

            for (SystemInfo system : page.getSystems()) {
                for (MeasureStack stack : system.getMeasureStacks()) {
                    Collections.sort(stack.getVoices(), Voice.byId);
                }

                Part part = system.getPhysicalPart(logicalPart);
                final List<Integer> partVoices = new ArrayList<Integer>(part.getVoiceIds());

                for (int i = 0; i < partVoices.size(); i++) {
                    final int id = partVoices.get(i);
                    int newId = voiceOffset + i + 1;

                    if (newId != id) {
                        part.swapVoiceId(i, newId);
                    }
                }
            }

            voiceOffset += globalMap.get(logicalPart);
        }
    }

    /**
     * Connect voices within the same logical part across all pages of a score.
     *
     * @param score the score to process
     */
    public static void refineScore (Score score)
    {
    }

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
        final SIGraph sig = system.getSig();
        int count = 0; // Count of voice ids in this system

        // Assigned voice IDs for each part within this system
        final Map<Part, List<Integer>> systemMap = new LinkedHashMap<Part, List<Integer>>();
        final MeasureStack firstStack = system.getFirstMeasureStack();

        for (MeasureStack stack : system.getMeasureStacks()) {
            for (Part part : system.getParts()) {
                // Global IDs already assigned in this part
                List<Integer> globals = systemMap.get(part);

                if (globals == null) {
                    systemMap.put(part, globals = new ArrayList<Integer>());
                }

                // Voices that start within this measure
                final Measure measure = stack.getMeasureAt(part);
                final List<Voice> incomings = measure.getVoices(); // Sorted by ID (and vertically)
                final List<Voice> assigned = new ArrayList<Voice>();
                final List<Voice> swapped = new ArrayList<Voice>();

                if (stack != firstStack) {
                    // Look for ties, as they carry voice across measure limits
                    VoiceLoop:
                    for (Voice voice : incomings) {
                        AbstractChordInter firstChord = voice.getFirstChord();

                        // Is there an incoming tie on a head of this chord?
                        for (Inter n : firstChord.getNotes()) {
                            if (n instanceof AbstractHeadInter) {
                                for (Relation r : sig.getRelations(n, SlurHeadRelation.class)) {
                                    SlurHeadRelation shRel = (SlurHeadRelation) r;

                                    if (shRel.getSide() == RIGHT) {
                                        SlurInter slur = (SlurInter) sig.getOppositeInter(n, r);

                                        if (slur.isTie()) {
                                            AbstractHeadInter left = slur.getHead(LEFT);

                                            if (left != null) {
                                                final Voice leftVoice = left.getVoice();
                                                logger.debug("{} ties {}", slur, leftVoice);
                                                swapped.add(
                                                        measure.swapVoiceId(voice, leftVoice.getId()));
                                                assigned.add(voice);

                                                continue VoiceLoop;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Connect un-assigned incoming voices to global ones
                // This simplistic approach uses position in lists
                for (int i = 0; i < incomings.size(); i++) {
                    final Voice voice = incomings.get(i);

                    if (assigned.contains(voice)) {
                        continue;
                    }

                    if (swapped.contains(voice)) {
                        if (!globals.contains(voice.getId())) {
                            // Extend globals list
                            final int newId = ++count;
                            globals.add(newId);

                            if (voice.getId() != newId) {
                                measure.swapVoiceId(voice, newId);
                            }
                        }

                        continue;
                    }

                    if (i < globals.size()) {
                        final int global = globals.get(i);

                        if (voice.getId() != global) {
                            measure.swapVoiceId(voice, global);
                        }
                    } else {
                        // Extend globals list
                        final int newId = ++count;
                        globals.add(newId);

                        if (voice.getId() != newId) {
                            measure.swapVoiceId(voice, newId);
                        }
                    }
                }
            }
        }

        logger.debug("System#{} idMap: {}", system.getId(), systemMap);

        // Store voices into their containing part
        for (Part part : system.getParts()) {
            part.setVoiceIds(systemMap.get(part));
        }
    }
}
