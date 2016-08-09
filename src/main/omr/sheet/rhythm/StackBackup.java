//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S t a c k B a c k u p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.rhythm;

import omr.sig.SigBackup;
import omr.sig.inter.AbstractChordInter;
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

        for (AbstractChordInter chord : stack.getStandardChords()) {
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

            if (inter instanceof AbstractChordInter) {
                for (Inter member : ((AbstractChordInter) inter).getNotes()) {
                    member.delete();
                }
            }
        }

        // Restore just the configuration content
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
        Set<TupletInter> toDelete = new TupletsBuilder(stack).linkTuplets();

        if (!toDelete.isEmpty()) {
            config.getInters().removeAll(toDelete);
            sig.deleteInters(toDelete);
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
        // Determine augmentation dots for each chord
        for (AbstractChordInter chord : stack.getStandardChords()) {
            chord.countDots();
        }
    }
}
