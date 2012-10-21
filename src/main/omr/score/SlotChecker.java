//----------------------------------------------------------------------------//
//                                                                            //
//                            S l o t C h e c k e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;

import omr.sheet.Scale;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Class {@code SlotChecker} performs checks on the sequence of slots
 * of every measure within a given system.
 *
 * @author Hervé Bitteur
 */
public class SlotChecker
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SlotChecker.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** The containing system. */
    private final ScoreSystem system;

    /** System scale. */
    private final Scale scale;

    /** Scaled parameters. */
    private final int minSlotSpacing;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // SlotChecker //
    //-------------//
    /**
     * Creates a new SlotChecker object.
     *
     * @param system the containing system
     */
    public SlotChecker (ScoreSystem system)
    {
        this.system = system;

        scale = system.getScale();

        minSlotSpacing = (int) Math.rint(
                scale.fracToPixels(Slot.defaultMargin.getTarget()));
    }

    //~ Methods ----------------------------------------------------------------
    //-------//
    // check //
    //-------//
    public void check (Measure measure)
    {
        // Allocate proper chords in every slot
        mergeSlots(measure);

        // Check that slots are not too close to each other
        checkMinSpacing(measure);
    }

    //------------------//
    // checkCommonStems //
    //------------------//
    /**
     * Check for common stems.
     *
     * @param prevStems stems of previous slot
     * @param stems     stems of the current slot
     * @return the set of common stems, perhaps empty
     */
    private Set<Glyph> checkCommonStems (Set<Glyph> prevStems,
                                         Set<Glyph> stems)
    {
        Set<Glyph> commons = new HashSet<>();
        for (Glyph stem : stems) {
            if (prevStems.contains(stem)) {
                commons.add(stem);
            }
        }

        return commons;
    }

    //-----------------//
    // checkMinSpacing //
    //-----------------//
    /**
     * Check that slots are not too close to each other.
     *
     * @param measure the containing measure
     */
    private void checkMinSpacing (Measure measure)
    {
        logger.fine("{0} checkMinSpacing", measure.getContextString());
        Slot prevSlot = null;

        int minSpacing = Integer.MAX_VALUE;
        Slot minSlot = null;

        for (Slot slot : measure.getSlots()) {
            if (prevSlot != null) {
                int spacing = slot.getX() - prevSlot.getX();

                if (minSpacing > spacing) {
                    minSpacing = spacing;
                    minSlot = slot;
                }
            }

            prevSlot = slot;
        }

        if (minSpacing < minSlotSpacing) {
            measure.addError(
                    minSlot.getLocationGlyph(),
                    "Suspicious narrow spacing of slots: "
                    + (float) scale.pixelsToFrac(minSpacing));
        }
    }

    //------------//
    // mergeSlots //
    //------------//
    /**
     * Check and merge slots if needed.
     *
     * @param measure the containing measure
     */
    private void mergeSlots (Measure measure)
    {
        logger.fine("{0} mergeSlots", measure.getContextString());

        // Loop on this measure, until no more merge is performed
        boolean merging;

        do {
            merging = false;

            // Allocate proper chords in every slot
            measure.getChords().retainAll(measure.getWholeChords());

            int id = 0;

            for (Slot slot : measure.getSlots()) {
                slot.getChords().clear();
                slot.setId(++id);
                slot.allocateChordsAndNotes();
            }

            // Check that the same chord is not linked to more than one slot
            Slot prevSlot = null;
            Set<Glyph> prevStems = null;

            for (Iterator<Slot> it = measure.getSlots().iterator(); it.hasNext();) {
                Slot slot = it.next();

                if (prevSlot != null) {
                    // Look for stem in common
                    Set<Glyph> commonStems = checkCommonStems(
                            prevStems,
                            slot.getStems());

                    if (!commonStems.isEmpty()) {
                        logger.fine(
                                "{0} merging slots #{1} & #{2} around {3}",
                                measure.getContextString(),
                                prevSlot.getId(),
                                slot.getId(),
                                Glyphs.toString("stems", commonStems));

                        prevSlot.includeSlot(slot);
                        it.remove();
                        merging = true;

                        break;
                    }
                }

                prevSlot = slot;
                prevStems = slot.getStems();
            }
        } while (merging);
    }
}
