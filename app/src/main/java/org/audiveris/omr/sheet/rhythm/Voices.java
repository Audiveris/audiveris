//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           V o i c e s                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.score.PageNumber;
import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.score.PartRef;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.score.ScoreReduction;
import org.audiveris.omr.score.SystemRef;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice.VoiceKind;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.relation.NextInVoiceRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Class <code>Voices</code> connects voices and harmonizes their IDs (and thus colors)
 * within a stack, a system, a page or a score.
 *
 * @author Hervé Bitteur
 */
public abstract class Voices
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Voices.class);

    /** To sort voices by their ID. */
    public static final Comparator<Voice> byId = (Voice v1,
                                                  Voice v2) -> Integer.compare(
                                                          v1.getId(),
                                                          v2.getId());

    /** To sort voices by vertical position within their containing measure or stack. */
    public static final Comparator<Voice> byOrdinate = (Voice v1,
                                                        Voice v2) -> {
        if (v1.getMeasure().getStack() != v2.getMeasure().getStack()) {
            throw new IllegalArgumentException("Comparing voices in different stacks");
        }

        // Check if they are located in different parts
        Part p1 = v1.getMeasure().getPart();
        Part p2 = v2.getMeasure().getPart();

        if (p1 != p2) {
            return Part.byId.compare(p1, p2);
        }

        // Check voice kind
        VoiceKind k1 = v1.getKind();
        VoiceKind k2 = v2.getKind();

        if (k1 != k2) {
            return k1.compareTo(k2);
        }

        AbstractChordInter c1 = v1.getFirstChord();
        AbstractChordInter c2 = v2.getFirstChord();

        Slot firstSlot1 = c1.getSlot();
        Slot firstSlot2 = c2.getSlot();

        // Check if the voices started in different time slots
        // Beware of whole rests, they have no time slot
        if ((firstSlot1 != null) && (firstSlot2 != null)) {
            int comp = Integer.compare(firstSlot1.getId(), firstSlot2.getId());

            if (comp != 0) {
                return comp;
            }

            // Same first time slot, so let's use chord ordinate
            return Inters.byOrdinate.compare(c1, c2);
        } else {
            // We have at least one whole rest (which always starts on slot 1, by definition)
            if ((firstSlot2 != null) && (firstSlot2.getId() > 1)) {
                return -1;
            }

            if ((firstSlot1 != null) && (firstSlot1.getId() > 1)) {
                return 1;
            }

            // Both are at beginning of measure, so let's use chord ordinates
            return Inters.byOrdinate.compare(c1, c2);
        }
    };

    /** Sequence of colors for voices. */
    private static final int alpha = 200;

    private static final Color[] voiceColors = new Color[] {
            /** 1 Purple */
            new Color(128, 64, 255, alpha),
            /** 2 Green */
            new Color(0, 255, 0, alpha),
            /** 3 Brown */
            new Color(165, 42, 42, alpha),
            /** 4 Magenta */
            new Color(255, 0, 255, alpha),
            /** 5 Cyan */
            new Color(0, 255, 255, alpha),
            /** 6 Orange */
            new Color(255, 200, 0, alpha),
            /** 7 Pink */
            new Color(255, 150, 150, alpha),
            /** 8 BlueGreen */
            new Color(0, 128, 128, alpha) };

    //~ Constructors -------------------------------------------------------------------------------

    // Not meant to be instantiated.
    private Voices ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //---------//
    // colorOf //
    //---------//
    /**
     * Report the color to use when painting elements related to the provided voice ID.
     *
     * @param id the provided voice id
     * @return the color to use
     */
    public static Color colorOf (int id)
    {
        // Use table of colors, circular.
        int index = (id - 1) % voiceColors.length;

        return voiceColors[index];
    }

    //---------//
    // colorOf //
    //---------//
    /**
     * Report the color to use when painting elements related to the provided voice.
     *
     * @param voice the provided voice
     * @return the color to use
     */
    public static Color colorOf (Voice voice)
    {
        return colorOf(voice.getId());
    }

    //---------------//
    // getColorCount //
    //---------------//
    /**
     * Report the number of defined voice colors.
     *
     * @return count of colors
     */
    public static int getColorCount ()
    {
        return voiceColors.length;
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

        if (firstChord == null) {
            return null;
        }

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

    //------------//
    // refinePage //
    //------------//
    /**
     * Connect voices within the same logical part across all systems of a page/score.
     *
     * @param page the page to process
     */
    public static void refinePage (Page page)
    {
        logger.debug("PageStep.refinePage");

        // Across systems within a single page, the partnering slur is the left extension
        final SlurAdapter systemSlurAdapter = (SlurInter slur) -> slur.getExtension(LEFT);

        final Score score = page.getScore();
        final List<LogicalPart> logicalParts = score.getLogicalParts();

        if (logicalParts != null) {
            final PageNumber pageNumber = score.getPageNumber(page);
            final PageRef pageRef = pageNumber.getPageRef(score.getBook());
            final SystemRef firstSystemRef = pageRef.getSystems().get(0);

            for (LogicalPart logicalPart : logicalParts) {
                final int logicalId = logicalPart.getId();

                for (SystemRef systemRef : pageRef.getSystems()) {
                    for (PartRef partRef : systemRef.getParts()) {
                        final Integer partRefLogicalId = partRef.getLogicalId();

                        if ((partRefLogicalId != null) && (partRefLogicalId == logicalId)) {
                            final Part part = partRef.getRealPart();

                            if (systemRef != firstSystemRef) {
                                // Check tied voices from previous system
                                final Measure firstMeasure = part.getFirstMeasure();

                                // A part may have no measure (case of tablature, ignored today)
                                if (firstMeasure != null) {
                                    for (Voice voice : firstMeasure.getVoices()) {
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
     * @param stubs the valid selected stubs
     * @return the count of modifications made
     */
    public static int refineScore (Score score,
                                   List<SheetStub> stubs)
    {
        // Make sure logical parts are available
        if (score.getLogicalParts() == null) {
            logger.info("Retrieving logical parts");
            final Book theBook = score.getBook();
            final List<SheetStub> theStubs = theBook.getValidSelectedStubs();
            new ScoreReduction(score).reduce(theStubs);
            theBook.setModified(true);
        }

        int modifs = 0;
        SystemInfo prevSystem = null; // Last system of preceding page, if any

        for (Page page : score.getPages()) {
            final SheetStub stub = page.getSheet().getStub();
            if (!stubs.contains(stub)) {
                prevSystem = null;
                continue;
            }

            if (prevSystem != null) {
                for (LogicalPart logicalPart : score.getLogicalParts()) {
                    // Check tied voices from same logicalPart in previous page
                    final Part part = page.getFirstSystem().getPartById(logicalPart.getId());

                    if (part == null) {
                        continue; // logical part not found in the first system of this page
                    }

                    final List<SlurInter> orphans = part.getSlurs(SlurInter.isBeginningOrphan);

                    final Part precedingPart = prevSystem.getPartById(logicalPart.getId());

                    if (precedingPart != null) {
                        final List<SlurInter> precOrphans = precedingPart.getSlurs(
                                SlurInter.isEndingOrphan);

                        final Map<SlurInter, SlurInter> links = part.getCrossSlurLinks(
                                precedingPart); // Links: Slur -> prevSlur

                        // Apply the links possibilities
                        for (Map.Entry<SlurInter, SlurInter> entry : links.entrySet()) {
                            final SlurInter slur = entry.getKey();
                            final SlurInter prevSlur = entry.getValue();

                            slur.checkCrossTie(prevSlur);
                        }

                        // Purge orphans across pages
                        orphans.removeAll(links.keySet());
                        precOrphans.removeAll(links.values());
                        SlurInter.discardOrphans(precOrphans, RIGHT);

                        // Across pages within a score, use the links map
                        final SlurAdapter pageSlurAdapter = (SlurInter slur) -> links.get(slur);

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
        final SIGraph sig = system.getSig();

        // Across measures within a single system, the partnering slur is the slur itself
        final SlurAdapter measureSlurAdapter = (SlurInter slur) -> slur;

        for (Part part : system.getParts()) {
            Measure prevMeasure = null;

            for (MeasureStack stack : system.getStacks()) {
                final Measure measure = stack.getMeasureAt(part);

                measure.purgeVoices();
                measure.sortVoices();
                measure.renameVoices();

                final List<Voice> measureVoices = measure.getVoices(); // Sorted vertically (?)

                for (Voice voice : measureVoices) {
                    // Check voices from same part in previous measure
                    if (prevMeasure != null) {
                        // Tie-based voice link
                        final Integer tiedId = getTiedId(voice, measureSlurAdapter);

                        if ((tiedId != null) && (voice.getId() != tiedId)) {
                            measure.swapVoiceId(voice, tiedId);
                        }

                        final AbstractChordInter ch2 = voice.getFirstChord();

                        if (ch2 != null) {
                            // BeamGroup-based voice link
                            final BeamGroupInter beamGroup = ch2.getBeamGroup();
                            if ((beamGroup != null) && beamGroup.getMeasures().contains(
                                    prevMeasure)) {
                                AbstractChordInter prevCh = null;
                                for (AbstractChordInter ch : beamGroup.getChords()) {
                                    if (prevCh != null && ch == ch2) {
                                        if (voice.getId() != prevCh.getVoice().getId()) {
                                            measure.swapVoiceId(voice, prevCh.getVoice().getId());
                                        }

                                        break;
                                    }

                                    prevCh = ch;
                                }
                            }

                            // NextInVoiceRelation-based voice links
                            for (Relation rel : sig.getRelations(ch2, NextInVoiceRelation.class)) {
                                final Inter inter = sig.getOppositeInter(ch2, rel);
                                final AbstractChordInter ch1 = (AbstractChordInter) inter;

                                if (ch1.getMeasure() == prevMeasure) {
                                    if (voice.getId() != ch1.getVoice().getId()) {
                                        measure.swapVoiceId(voice, ch1.getVoice().getId());
                                    }

                                    break;
                                }
                            }
                        }
                    }

                    // Preferred voice IDs?
                    final AbstractChordInter ch1 = voice.getFirstChord();

                    if (ch1 != null) {
                        final Integer preferredVoiceId = ch1.getPreferredVoiceId();

                        if ((preferredVoiceId != null) && (preferredVoiceId != voice.getId())) {
                            measure.swapVoiceId(voice, preferredVoiceId);
                        }
                    }
                }

                prevMeasure = measure;
            }
        }
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------

    //-------------//
    // SlurAdapter //
    //-------------//
    /**
     * This adapter gives access to the partnering slur of a given slur.
     * <p>
     * There are different implementations:
     * <ul>
     * <li>measureSlurAdapter: Across measures in a single system.
     * <li>systemSlurAdapter: Across systems in a single page.
     * <li>pageSlurAdapter: Across pages in a (single) score.
     * </ul>
     */
    private static interface SlurAdapter
    {
        /**
         * Report the initial (that is: before) partnering slur.
         *
         * @param slur the slur to follow
         * @return the extending slur (or the slur itself)
         */
        SlurInter getInitialSlur (SlurInter slur);
    }
}
