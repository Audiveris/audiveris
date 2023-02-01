//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S c o r e R e d u c t i o n                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
package org.audiveris.omr.score;

import org.audiveris.omr.score.PartRef;
import org.audiveris.omr.score.SystemRef;
import org.audiveris.omr.score.PartConnection.ResultEntry;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.SheetStub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>ScoreReduction</code> reduces the logical parts for a score.
 * <p>
 * <b>Features not yet implemented:</b>
 * <ul>
 * <li>Connection of slurs between pages</li>
 * <li>In part-list, handling of part-group beside score-part</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class ScoreReduction
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ScoreReduction.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Related score. */
    private final Score score;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new ScoreReduction object.
     *
     * @param score the score to process
     */
    public ScoreReduction (Score score)
    {
        this.score = score;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // reduce //
    //--------//
    /**
     * Build the score LogicalPart instances by connecting the pages systems parts.
     *
     * @param stubs valid selected stubs
     * @return the count of modifications done
     */
    public int reduce (List<SheetStub> stubs)
    {
        final List<List<PartRef>> sequences = buildSequences(score.getPageRefs(stubs));
        final PartConnection connection = new PartConnection(
                sequences,
                score.isLogicalsLocked() ? score.getLogicalParts() : null);
        final List<ResultEntry> resultEntries = connection.getResults();

        if (logger.isDebugEnabled()) {
            connection.dumpResults();
        }

        // Store the list of LogicalPart instances into score
        return storeResults(resultEntries) ? 1 : 0;
    }

    //----------------//
    // buildSequences //
    //----------------//
    /**
     * Build the system-sequences of parts, collected system per system, page per page.
     *
     * @param pageRefs the sequence of PageRef's
     * @return the sequences of PartRef's
     */
    private List<List<PartRef>> buildSequences (List<PageRef> pageRefs)
    {
        final List<List<PartRef>> sequences = new ArrayList<>();

        for (PageRef pageRef : pageRefs) {
            pageRef.getStub().checkSystems(); // For old OMRs

            for (SystemRef system : pageRef.getSystems()) {
                sequences.add(system.getParts());
            }
        }

        return sequences;
    }

    //--------------//
    // storeResults //
    //--------------//
    /**
     * Store the results as the score list of LogicalPart instances.
     *
     * @param resultEntries results from part connection
     * @return true if score part list has really been modified
     */
    private boolean storeResults (List<ResultEntry> resultEntries)
    {
        // Propagate to affiliates
        boolean modified = false;
        for (ResultEntry entry : resultEntries) {
            final int logId = entry.logical.getId();

            for (PartRef partRef : entry.partRefs) {
                modified |= partRef.setLogicalId(logId);

                // Update Part immediately if containing sheet is loaded
                // If not, it will get updated the next time sheet is loaded
                final SheetStub stub = partRef.getSystem().getPage().getStub();
                if (stub.hasSheet()) {
                    final Part part = partRef.getRealPart();
                    part.setId(logId);
                }
            }
        }

        if (!score.isLogicalsLocked()) {
            // (Over-)write score logicals
            final List<LogicalPart> newLogicals = new ArrayList<>();
            for (ResultEntry entry : resultEntries) {
                newLogicals.add(entry.logical);
            }

            score.setLogicalParts(newLogicals);
        }

        return modified;
    }
}
