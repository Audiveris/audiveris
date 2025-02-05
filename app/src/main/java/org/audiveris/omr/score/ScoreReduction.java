//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S c o r e R e d u c t i o n                                   //
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
package org.audiveris.omr.score;

import org.audiveris.omr.score.PartCollation.Record;
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

    //--------------//
    // applyRecords //
    //--------------//
    /**
     * Apply the collation results.
     * <ol>
     * <li>Within each record, all the affiliated PartRef's are assigned the LogicalPart id.
     * <li>If score logicals are not locked, they get replaced by records logicals.
     * </ol>
     *
     * @param records results from part collation
     * @return true if PartRef's have really been modified
     */
    private boolean applyRecords (List<Record> records)
    {
        // Propagate to affiliates
        boolean anyModified = false;
        for (Record record : records) {
            final int logId = record.logical.getId();

            for (PartRef partRef : record.partRefs) {
                if (partRef.setLogicalId(logId)) {
                    anyModified = true;

                    // Update Part immediately if containing sheet is loaded
                    // If not, it will get updated the next time sheet is loaded
                    final SheetStub stub = partRef.getSystem().getPage().getStub();
                    if (stub.hasSheet()) {
                        try {
                            final Part part = partRef.getRealPart();
                            part.setId(logId);
                        } catch (Exception ex) {
                            logger.warn(
                                    "Error in applyRecords {}\n  {} system#{} {}\n  {}",
                                    ex.getMessage(),
                                    stub,
                                    partRef.getSystem().getId(),
                                    partRef,
                                    partRef.getSystem(),
                                    ex);
                        }
                    }
                }
            }
        }

        if (!score.isLogicalsLocked()) {
            // (Over-)write score logicals
            final List<LogicalPart> newLogicals = new ArrayList<>();
            for (Record record : records) {
                newLogicals.add(record.logical);
            }

            score.setLogicalParts(newLogicals);
        }

        return anyModified;
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
        final PartCollation collation = new PartCollation(
                sequences,
                score.isLogicalsLocked() ? score.getLogicalParts() : null);
        final List<Record> records = collation.getRecords();

        if (logger.isDebugEnabled()) {
            collation.dumpRecords();
        }

        // Store the list of LogicalPart instances into score
        return applyRecords(records) ? 1 : 0;
    }
}
