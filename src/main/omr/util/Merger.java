//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           M e r g e r                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code Merger}
 *
 * @author Hervé Bitteur
 */
public abstract class Merger<E>
{
    //~ Methods ------------------------------------------------------------------------------------

    public void mergeAll (List<E> list)
    {
        List<E> merged = new ArrayList<E>();

        // Browse list
        for (E current : list) {
            E candidate = current;

            // Keep on working while we do have a candidate to check for merge
            CandidateLoop:
            while (true) {
                // Set up candidate environment
                setupCandidate(candidate);

                // Check candidate vs all preceding ones until current excluded
                HeadsLoop:
                for (E head : list) {
                    if (head == current) {
                        break CandidateLoop; // Actual end of sub-list
                    }

                    // Check for a possible merge
                    if ((head != candidate) && (!isMerged(head)) && canMerge(head, candidate)) {
                        merge(head, candidate); // Head swallows candidate
                        merged.add(candidate);
                        candidate = head; // This is a new candidate

                        break HeadsLoop;
                    }
                }
            }
        }

        // Discard the merged filaments
        list.removeAll(merged);
    }

    protected abstract boolean canMerge (E head,
                                         E candidate);

    protected abstract boolean isMerged (E e);

    protected abstract boolean merge (E head,
                                      E candidate);

    protected void setupCandidate (E candidate)
    {
    }
}
