//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     R h y t h m B a c k u p                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.sig.SIGraph;
import omr.sig.SigAttic;
import omr.sig.inter.ChordInter;
import omr.sig.inter.Inter;
import omr.sig.inter.InterEnsemble;
import omr.sig.inter.RestChordInter;
import omr.sig.inter.TupletInter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Class {@code RhythmBackup} manages the rhythm adjustable configuration of a
 * MeasureStack.
 * <ol>
 * <li>It saves the comprehensive initial set of rhythm data, perhaps with conflicting items.</li>
 * <li>It can install a specific configuration of rhythm data for testing.</li>
 * <li>It can freeze the stack when a final good configuration has been chosen.</li>
 * </ol>
 */
public class RhythmBackup
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The underlying stack. */
    private final MeasureStack stack;

    /** Initial rhythm data. */
    private List<Inter> initials;

    /** The SIG where work is done. */
    private final SIGraph sig;

    /** The attic where data can be saved to and restored from. */
    private final SigAttic attic = new SigAttic();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RhythmBackup} object.
     *
     * @param stack the underlying measure stack
     */
    public RhythmBackup (MeasureStack stack)
    {
        this.stack = stack;
        sig = stack.getSystem().getSig();
    }

    //~ Methods ------------------------------------------------------------------------------------
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
     * @param config   the configuration to install
     * @param toRemove (output)chord inters to remove, if any
     * @return true if successful, false if an error was detected
     */
    public boolean install (RhythmConfig config,
                            Set<RestChordInter> toRemove)
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
        return new SlotsBuilder(stack, toRemove).process();
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
