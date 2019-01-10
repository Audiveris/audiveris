//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P a r t C o n n e c t i o n                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import java.util.List;

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
 * <li>A part with 2 staves, if any, is used to align connections.</li>
 * <li>Otherwise, since additional parts appear at the top of a system, rather than at the bottom,
 * we process part connections bottom up.</li>
 * <li>When possible, we use the part names (or abbreviations) to help the connection algorithm.
 * (this is not yet implemented).
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class PartConnection
{

    private static final Logger logger = LoggerFactory.getLogger(PartConnection.class);

    private List<ResultEntry> resultEntries;

    /**
     * Creates a new {@code PartConnection} object.
     *
     * @param sequences the list of sequences of part candidates
     */
    public PartConnection (List<List<Candidate>> sequences)
    {
        resultEntries = connect(sequences);
    }

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
     * @return the entry created
     */
    private ResultEntry addResultEntry (int resultIndex,
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
        List<Candidate> candidates = new ArrayList<>();
        candidates.add(candidate);
        entry.result = result;
        entry.candidates = candidates;
        resultEntries.add(resultIndex, entry);

        return entry;
    }

    //---------//
    // biIndex //
    //---------//
    /**
     * Report the index in provided candidate sequence of the (first) 2-staff part.
     *
     * @param sequence sequence of parts
     * @return index of bi-staff part in sequence, or -1 if not found
     */
    private int biIndex (List<Candidate> sequence)
    {
        for (Candidate candidate : sequence) {
            if (candidate.getStaffCount() == 2) {
                return sequence.indexOf(candidate);
            }
        }

        return -1;
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
        /** Resulting sequence of logical parts. */
        resultEntries = new ArrayList<>();

        // Two-staff entry found, if any
        ResultEntry biEntry = null;

        // Process each sequence of candidate parts in turn
        for (int iSeq = 0; iSeq < sequences.size(); iSeq++) {
            final List<Candidate> sequence = sequences.get(iSeq);
            final int biIndex = biIndex(sequence);

            if (iSeq == 0) {
                dispatch(sequence, resultEntries, +1);

                if (biIndex != -1) {
                    biEntry = resultEntries.get(biIndex);
                }
            } else {
                if ((biEntry != null) && (biIndex != -1)) {
                    // Align on the biEntry
                    biEntry.candidates.add(sequence.get(biIndex));

                    // Process parts above bi
                    dispatch(
                            sequence.subList(0, biIndex),
                            resultEntries.subList(0, resultEntries.indexOf(biEntry)),
                            -1);

                    // Process parts below bi
                    dispatch(
                            sequence.subList(biIndex + 1, sequence.size()),
                            resultEntries.subList(
                                    resultEntries.indexOf(biEntry) + 1,
                                    resultEntries.size()),
                            +1);
                } else {
                    dispatch(sequence, resultEntries, -1);
                }
            }
        }

        // Assign numbers to logical parts
        renumberResults();

        return resultEntries;
    }

    /**
     * Dispatch the list of candidate parts (perhaps a sublist of a system) to the
     * current sublist of logical parts.
     *
     * @param sequence the candidate parts to dispatch
     * @param results  (input/output) the current sublist of logicals
     * @param dir      -1 or +1 for browsing up or down
     */
    private void dispatch (List<Candidate> sequence,
                           List<ResultEntry> results,
                           int dir)
    {
        final int ic1 = (dir > 0) ? 0 : (sequence.size() - 1);
        final int ic2 = (dir > 0) ? sequence.size() : (-1);
        int resultIndex = (dir > 0) ? (-1) : results.size();

        for (int ic = ic1; ic != ic2; ic += dir) {
            Candidate candidate = sequence.get(ic);
            logger.debug("Processing {}", candidate);

            // Check with logical parts currently defined
            resultIndex += dir;
            logger.debug("resultIndex:{}", resultIndex);

            if (((dir > 0) && (resultIndex >= results.size())) || ((dir < 0)
                                                                           && (resultIndex < 0))) {
                logger.debug("No more entries available");

                // Create a brand new logical part for this candidate
                addResultEntry((dir < 0) ? 0 : resultIndex, candidate, results);
            } else {
                ResultEntry resultEntry = results.get(resultIndex);
                LogicalPart result = resultEntry.result;
                logger.debug("Part:{}", result);

                // Check we are compatible in terms of staves
                if (result.getStaffCount() != candidate.getStaffCount()) {
                    logger.debug("Count incompatibility");

                    // Create a brand new logical part for this candidate
                    addResultEntry(resultIndex, candidate, results);
                } else {
                    // We are compatible
                    resultEntry.candidates.add(candidate);
                }
            }
        }
    }

    //-----------------//
    // renumberResults //
    //-----------------//
    /**
     * Reverse and renumber the result entries.
     */
    private void renumberResults ()
    {
        ///Collections.reverse(resultEntries);
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

        /**
         * Report the abbreviation, if any, that relates to this part
         *
         * @return the abbreviation if any
         */
        String getAbbreviation ();

        /**
         * Report the name of the part, if any
         *
         * @return the part name if any
         */
        String getName ();

        /**
         * Report the number of staves in the part
         *
         * @return the number of staves
         */
        int getStaffCount ();

        /**
         * Assign an id to the candidate (and recursively to its affiliates if any).
         *
         * @param id the assigned id value
         */
        void setId (int id);
    }

    //-------------//
    // ResultEntry //
    //-------------//
    /**
     * Entry to gather results related to a single part.
     */
    public static class ResultEntry
    {

        /** Resulting logical part. */
        LogicalPart result;

        /** Affiliated candidates. */
        List<Candidate> candidates;
    }
}
