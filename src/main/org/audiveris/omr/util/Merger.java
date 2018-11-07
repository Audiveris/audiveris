//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           M e r g e r                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code Merger}
 *
 * @author Hervé Bitteur
 */
public abstract class Merger<E>
{

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
