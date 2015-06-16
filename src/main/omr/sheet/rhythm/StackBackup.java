//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S t a c k B a c k u p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.sig.SigBackup;
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
 * Class {@code StackBackup} manages the rhythm adjustable configuration of a
 * MeasureStack.
 * <ol>
 * <li>It saves the comprehensive initial set of rhythm data, perhaps with conflicting items.</li>
 * <li>It can install a specific configuration of rhythm data for testing.</li>
 * <li>It can freeze the stack when a final good configuration has been chosen.</li>
 * </ol>
 */
public class StackBackup
        extends SigBackup
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The underlying stack. */
    private final MeasureStack stack;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StackBackup} object.
     *
     * @param stack the underlying measure stack
     */
    public StackBackup (MeasureStack stack)
    {
        super(stack.getSystem().getSig());
        this.stack = stack;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // freeze //
    //--------//
    public void freeze (Collection<? extends Inter> keptInters)
    {
        // For those chords that have not been kept, delete the member notes
        List<Inter> discardedInters = new ArrayList<Inter>(seeds);
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
     * @param config   (input/output) the configuration to install
     * @param toRemove (output) rest chord inters to remove, if any
     * @param failFast true to stop processing on first error
     * @return true if successful, false if an error was detected
     */
    public boolean install (StackConfig config,
                            Set<RestChordInter> toRemove,
                            boolean failFast)
    {
        // Clear the stack
        for (Inter inter : seeds) {
            stack.removeInter(inter);
            inter.delete();
        }

        // Restore just the partition
        attic.restore(sig, config.getInters());

        for (Inter inter : config.getInters()) {
            stack.addInter(inter);
        }

        // Reset all rhythm data within the stack
        stack.resetRhythm();

        // Count augmentation dots on chords
        // (this implies that chord notes are present with their potential relation to dot)
        countChordDots();

        // Link tuplets
        List<TupletInter> toDelete = new TupletsBuilder(stack).linkTuplets();

        if (!toDelete.isEmpty()) {
            config.getInters().removeAll(toDelete);
        }

        // Build slots & voices
        return new SlotsBuilder(stack, toRemove, failFast).process();
    }

    //----------------//
    // resetFromSeeds //
    //----------------//
    public void resetFromSeeds ()
    {
        // Clear any inter from seeds
        for (Inter inter : seeds) {
            stack.removeInter(inter);
        }

        // Restore the initial config
        attic.restore(sig, seeds);
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
