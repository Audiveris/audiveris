//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P a r t C o n n e c t i o n                                   //
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
package org.audiveris.omr.score;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * Class {@code PartConnection} is in charge of finding the connections of parts across
 * systems (and pages) so that a part always represents the same instrument all along
 * the score.
 * <p>
 * This work is done across:
 * <ul>
 * <li>The various systems of a page using Audiveris SystemInfo instances.</li>
 * <li>The various pages of a score using Audiveris Page instances.</li>
 * </ul>
 * <p>
 * We have taken a generic approach, abstracting the different types of Candidates.
 * <p>
 * The strategy used to build Results out of Candidates is based on the following assumptions:
 * <ul>
 * <li>For a part of a system to be connected to a part of another system, they must exhibit the
 * same count of staves.</li>
 * <li>Parts cannot be swapped from one system to the other. In other words, we cannot have say
 * partA followed by partB in a system, and partB followed by partA in another system.</li>
 * <li>Additional parts appear at the top of a system, rather than at the bottom. So we process part
 * connections bottom up.</li>
 * <li>When possible, we use the part names (or abbreviations) to help the connection algorithm.
 * (this is not yet fully implemented).
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class PartConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PartConnection.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private List<ResultEntry> resultEntries;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PartConnection} object.
     *
     * @param sequences the list of sequences of part candidates
     */
    public PartConnection (List<List<Candidate>> sequences)
    {
        resultEntries = connect(sequences);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // dumpResults //
    //-------------//
    /**
     * Dump content of connection results.
     */
    public void dumpResults ()
    {
        StringBuilder sb = new StringBuilder();

        for (ResultEntry entry : resultEntries) {
            sb.append("\n").append(entry.result);

            for (Candidate candidate : entry.candidates) {
                sb.append("\n   ").append(candidate);
            }
        }

        logger.info("PartConnection results:{}", sb);
    }

    //------------//
    // getResults //
    //------------//
    /**
     * Report the connections results.
     *
     * @return the connection results
     */
    public List<ResultEntry> getResults ()
    {
        return resultEntries;
    }

    //----------------//
    // addResultEntry //
    //----------------//
    /**
     * Build a new ResultEntry based on provided Candidate and insert it at the desired
     * resultIndex in the list of resultEntries.
     *
     * @param resultIndex
     * @param candidate
     * @param resultEntries
     */
    private void addResultEntry (int resultIndex,
                                 Candidate candidate,
                                 List<ResultEntry> resultEntries)
    {
        // Create a brand new logical part for this candidate
        // Id is irrelevant for the time being
        LogicalPart result = new LogicalPart(0, candidate.getStaffCount());
        result.setName(candidate.getName());
        result.setAbbreviation(candidate.getAbbreviation());
        logger.debug("Created {} from {}", result, candidate);

        ResultEntry entry = new ResultEntry();
        List<Candidate> candidates = new ArrayList<Candidate>();
        candidates.add(candidate);
        entry.result = result;
        entry.candidates = candidates;
        resultEntries.add(resultIndex, entry);
    }

    //---------//
    // connect //
    //---------//
    /**
     * The heart of the part connection algorithm, organized to work through interfaces
     * in order to use the same piece of code, when we connect parts across systems of
     * one page, or when we connect parts across several pages.
     *
     * @param sequences a list of sequences of candidate parts
     * @return the sorted list of resulting parts, each with its affiliated candidates
     */
    private List<ResultEntry> connect (List<List<Candidate>> sequences)
    {
        /** Resulting sequence of logical parts */
        resultEntries = new ArrayList<ResultEntry>();

        // Process each sequence of candidate parts in turn
        for (List<Candidate> sequence : sequences) {
            // Current index in resultEntries (built in reverse order)
            int resultIndex = -1;

            if (logger.isDebugEnabled()) {
                logger.debug("Processing new sequence ...");

                for (Candidate candidate : sequence) {
                    logger.debug("- {}", candidate);
                }
            }

            // Process the sequence in reverse order (bottom up)
            for (ListIterator<Candidate> it = sequence.listIterator(sequence.size());
                    it.hasPrevious();) {
                Candidate candidate = it.previous();
                logger.debug(
                        "Processing candidate {} count:{}",
                        candidate,
                        candidate.getStaffCount());

                // Check with logical parts currently defined
                resultIndex++;
                logger.debug("resultIndex:{}", resultIndex);

                if (resultIndex >= resultEntries.size()) {
                    logger.debug("No more entries available");

                    // Create a brand new logical part for this candidate
                    addResultEntry(resultIndex, candidate, resultEntries);
                } else {
                    ResultEntry resultEntry = resultEntries.get(resultIndex);
                    LogicalPart result = resultEntry.result;
                    logger.debug("Part:{}", result);

                    // Check we are compatible in terms of staves
                    if (result.getStaffCount() != candidate.getStaffCount()) {
                        logger.debug("Count incompatibility");

                        // Create a brand new logical part for this candidate
                        addResultEntry(resultIndex, candidate, resultEntries);
                    } else {
                        // Can we use names? Just for fun for the time being
                        if ((candidate.getName() != null) && (result.getName() != null)) {
                            boolean namesOk = candidate.getName().equalsIgnoreCase(
                                    result.getName());

                            logger.debug("Names OK: {}", namesOk);

                            if (!namesOk) {
                                logger.debug(
                                        "\"{}\" vs \"{}\"",
                                        candidate.getName(),
                                        result.getName());
                            }
                        }

                        // We are compatible
                        resultEntry.candidates.add(candidate);
                    }
                }
            }
        }

        // Reverse and renumber
        renumberResults();

        return resultEntries;
    }

    //-----------------//
    // renumberResults //
    //-----------------//
    /**
     * Reverse and renumber the result entries.
     */
    private void renumberResults ()
    {
        Collections.reverse(resultEntries);

        for (int i = 0; i < resultEntries.size(); i++) {
            ResultEntry resultEntry = resultEntries.get(i);
            LogicalPart result = resultEntry.result;
            int id = i + 1;
            result.setId(id);

            for (Candidate candidate : resultEntry.candidates) {
                candidate.setId(id);
            }

            logger.debug("Final {}", result);
        }
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //-----------//
    // Candidate //
    //-----------//
    /**
     * Interface {@code Candidate} is used to process part candidates, regardless whether they are
     * provided as:
     * <ul>
     * <li>System physical {@link org.audiveris.omr.sheet.Part} instances when merging system parts
     * into the logical parts of one page.</li>
     * <li>Page {@link org.audiveris.omr.score.LogicalPart} instances when merging pages logical
     * parts into the logical parts of one score.</li>
     * </ul>
     */
    public static interface Candidate
    {
        //~ Methods --------------------------------------------------------------------------------

        /** Report the abbreviation, if any, that relates to this part
         *
         * @return the abbreviation if any
         */
        String getAbbreviation ();

        /** Report the name of the part, if any
         *
         * @return the part name if any
         */
        String getName ();

        /** Report the number of staves in the part
         *
         * @return the number of staves
         */
        int getStaffCount ();

        /** Assign an id to the candidate (and recursively to its affiliates if any).
         *
         * @param id the assigned id value
         */
        void setId (int id);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // ResultEntry //
    //-------------//
    public static class ResultEntry
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Resulting logical part. */
        LogicalPart result;

        /** Affiliated candidates. */
        List<Candidate> candidates;
    }
}
