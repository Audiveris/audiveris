//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           V o i c e s                                          //
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

import org.audiveris.omr.score.LogicalPart;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import static org.audiveris.omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Class {@code Voices} connects voices and harmonizes their IDs (and thus colors)
 * within a stack, a system, a page or a score.
 *
 * @author Hervé Bitteur
 */
public abstract class Voices
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Voices.class);

    /** To sort voices by their ID. */
    public static final Comparator<Voice> byId = new Comparator<Voice>()
    {
        @Override
        public int compare (Voice v1,
                            Voice v2)
        {
            return Integer.compare(v1.getId(), v2.getId());
        }
    };

    /** To sort voices by vertical position within their containing measure or stack. */
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

                return Inters.byOrdinate.compare(c1, c2);
            }

            // No common slot found, use index of first slot for each voice
            if ((firstSlot1 != null) && (firstSlot2 != null)) {
                return Integer.compare(firstSlot1.getId(), firstSlot2.getId());
            }

            // Use ordinate (there is a whole rest)
            AbstractChordInter c1 = v1.getFirstChord();
            AbstractChordInter c2 = v2.getFirstChord();

            return Inters.byOrdinate.compare(c1, c2);
        }
    };

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // refinePage //
    //------------//
    /**
     * Connect voices within the same logical part across all systems of a page.
     *
     * @param page the page to process
     */
    public static void refinePage (Page page)
    {
        logger.debug("PageStep.refinePage");

        final SystemInfo firstSystem = page.getFirstSystem();
        final SlurAdapter systemSlurAdapter = new SlurAdapter()
        {
            @Override
            public SlurInter getInitialSlur (SlurInter slur)
            {
                return slur.getExtension(LEFT);
            }
        };

        for (LogicalPart logicalPart : page.getLogicalParts()) {
            for (SystemInfo system : page.getSystems()) {
                Part part = system.getPartById(logicalPart.getId());

                if (part != null) {
                    if (system != firstSystem) {
                        // Check tied voices from previous system
                        for (Voice voice : part.getFirstMeasure().getVoices()) {
                            Integer tiedId = getTiedId(voice, systemSlurAdapter);

                            if ((tiedId != null) && (voice.getId() != tiedId)) {
                                part.swapVoiceId(voice.getId(), tiedId);
                            }
                        }
                    }
                }
            }
        }
    }

    //-------------//
    // refineScore //
    //-------------//
    /**
     * Connect voices within the same logical part across all pages of a score.
     * <p>
     * Ties across sheets cannot easily be persisted, so we detect and use them on the fly.
     *
     * @param score the score to process
     * @return the count of modifications made
     */
    public static int refineScore (Score score)
    {
        int modifs = 0;
        SystemInfo prevSystem = null; // Last system of preceding page, if any

        for (int pageNumber = 1; pageNumber <= score.getPageCount(); pageNumber++) {
            Page page = score.getPage(pageNumber);

            if (prevSystem != null) {
                for (LogicalPart scorePart : score.getLogicalParts()) {
                    // Check tied voices from same logicalPart in previous page
                    final LogicalPart logicalPart = page.getLogicalPartById(scorePart.getId());

                    if (logicalPart == null) {
                        continue; // logical part not found in this page
                    }

                    final Part part = page.getFirstSystem().getPartById(logicalPart.getId());

                    if (part == null) {
                        continue; // logical part not found in the first system of this page
                    }

                    final List<SlurInter> orphans = part.getSlurs(SlurInter.isBeginningOrphan);

                    final Part precedingPart = prevSystem.getPartById(
                            logicalPart.getId());

                    if (precedingPart != null) {
                        final List<SlurInter> precOrphans = precedingPart.getSlurs(
                                SlurInter.isEndingOrphan);

                        final Map<SlurInter, SlurInter> links = part.getCrossSlurLinks(
                                precedingPart); // Links: Slur -> prevSlur

                        // Apply the links possibilities
                        for (Map.Entry<SlurInter, SlurInter> entry : links.entrySet()) {
                            final SlurInter slur = entry.getKey();
                            final SlurInter prevSlur = entry.getValue();

                            slur.checkTie(prevSlur);
                        }

                        // Purge orphans across pages
                        orphans.removeAll(links.keySet());
                        precOrphans.removeAll(links.values());
                        SlurInter.discardOrphans(precOrphans, RIGHT);

                        final SlurAdapter pageSlurAdapter = new SlurAdapter()
                        {
                            @Override
                            public SlurInter getInitialSlur (SlurInter slur)
                            {
                                return links.get(slur);
                            }
                        };

                        for (Voice voice : part.getFirstMeasure().getVoices()) {
                            Integer tiedId = getTiedId(voice, pageSlurAdapter);

                            if ((tiedId != null) && (voice.getId() != tiedId)) {
                                logicalPart.swapVoiceId(page, voice.getId(), tiedId);
                                modifs++;
                            }
                        }
                    }

                    SlurInter.discardOrphans(orphans, LEFT);
                }
            }

            prevSystem = page.getLastSystem();
        }

        return modifs;
    }

    //-------------//
    // refineStack //
    //-------------//
    /**
     * Refine voice IDs within a stack.
     * <p>
     * When this method is called, initial IDs have been assigned according to voice creation
     * (whole voices first, then slot voices, with each voice remaining in its part).
     * See {@link Slot#buildVoices(java.util.List)} and {@link Slot#assignVoices()} methods.
     * <p>
     * Here we simply rename the IDs from top to bottom (roughly), within each part.
     *
     * @param stack the stack to process
     */
    public static void refineStack (MeasureStack stack)
    {
        // Within each measure, sort voices vertically and rename them accordingly.
        for (Measure measure : stack.getMeasures()) {
            measure.sortVoices();
            measure.renameVoices();
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
        final SlurAdapter measureSlurAdapter = new SlurAdapter()
        {
            @Override
            public SlurInter getInitialSlur (SlurInter slur)
            {
                return slur;
            }
        };

        for (Part part : system.getParts()) {
            for (MeasureStack stack : system.getMeasureStacks()) {
                if (stack != firstStack) {
                    // Check tied voices from same part in previous measure
                    final Measure measure = stack.getMeasureAt(part);
                    final List<Voice> measureVoices = measure.getVoices(); // Sorted vertically

                    for (Voice voice : measureVoices) {
                        Integer tiedId = getTiedId(voice, measureSlurAdapter);

                        if ((tiedId != null) && (voice.getId() != tiedId)) {
                            measure.swapVoiceId(voice, tiedId);
                        }
                    }
                }
            }
        }
    }

    //-----------//
    // getTiedId //
    //-----------//
    /**
     * Check whether the provided voice is tied (via a tie slur) to a previous voice
     * and thus must use the same ID.
     *
     * @param voice       the voice to check
     * @param slurAdapter to provide the linked slur at previous location
     * @return the imposed ID if any, null otherwise
     */
    private static Integer getTiedId (Voice voice,
                                      SlurAdapter slurAdapter)
    {
        final AbstractChordInter firstChord = voice.getFirstChord();
        final SIGraph sig = firstChord.getSig();

        // Is there an incoming tie on a head of this chord?
        for (Inter note : firstChord.getNotes()) {
            if (note instanceof HeadInter) {
                for (Relation r : sig.getRelations(note, SlurHeadRelation.class)) {
                    SlurHeadRelation shRel = (SlurHeadRelation) r;

                    if (shRel.getSide() == RIGHT) {
                        SlurInter slur = (SlurInter) sig.getOppositeInter(note, r);

                        if (slur.isTie()) {
                            SlurInter prevSlur = slurAdapter.getInitialSlur(slur);

                            if (prevSlur != null) {
                                HeadInter left = prevSlur.getHead(LEFT);

                                if (left != null) {
                                    final Voice leftVoice = left.getVoice();
                                    logger.debug("{} ties {} over to {}", slur, voice, leftVoice);

                                    // Can be null if rhythm could not process the whole measure
                                    if (leftVoice != null) {
                                        return leftVoice.getId();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //-------------//
    // SlurAdapter //
    //-------------//
    private static interface SlurAdapter
    {
        //~ Methods --------------------------------------------------------------------------------

        /**
         * Report the slur connected to the left of the provided one.
         * This can be the extending slur when looking in previous system, or the slur itself when
         * looking in previous measure within the same system.
         *
         * @param slur the slur to follow
         * @return the extending slur (or the slur itself)
         */
        SlurInter getInitialSlur (SlurInter slur);
    }
}
