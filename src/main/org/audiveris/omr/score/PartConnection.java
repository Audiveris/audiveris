//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P a r t C o n n e c t i o n                                   //
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

import org.audiveris.omr.util.IntUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class <code>PartConnection</code> is in charge of finding the connections of parts across
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
 * same count of staves and the same count of lines in corresponding staves</li>
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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PartConnection.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * The sorted list of resulting parts, each with its affiliated candidates.
     */
    private final List<ResultEntry> resultEntries = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>PartConnection</code> object.
     *
     * @param sequences the list of sequences of part candidates
     */
    public PartConnection (List<List<Candidate>> sequences)
    {
        connect(sequences);
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
     * Build a new ResultEntry based on provided Candidate and insert it at last position,
     * according to provided direction, in the list of results.
     *
     * @param dir       -1 for up, +1 for down
     * @param candidate the provided candidate
     * @param results   the results to be augmented
     */
    private void addResultEntry (int dir,
                                 Candidate candidate,
                                 List<ResultEntry> results)
    {
        // Create a brand new logical part for this candidate
        // Its id is irrelevant for the time being
        final LogicalPart result
                = new LogicalPart(0, candidate.getStaffCount(), candidate.getLineCounts());
        result.setName(candidate.getName());
        result.setAbbreviation(candidate.getAbbreviation());
        logger.debug("Created {} from {}", result, candidate);

        final ResultEntry entry = new ResultEntry();
        List<Candidate> candidates = new ArrayList<>();
        candidates.add(candidate);
        entry.result = result;
        entry.candidates = candidates;

        // Insert at proper end of results
        results.add(dir < 0 ? 0 : results.size(), entry);
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
     */
    private void connect (List<List<Candidate>> sequences)
    {
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
    }

    //----------//
    // dispatch //
    //----------//
    /**
     * Dispatch the list of candidate parts (for example a sublist of a system) to the
     * current sublist of logical parts.
     *
     * @param sequence the candidate parts to dispatch
     * @param results  (input/output) the current sublist of logical parts
     * @param dir      -1 or +1 for browsing up or down
     */
    private void dispatch (List<Candidate> sequence,
                           List<ResultEntry> results,
                           int dir)
    {
        final int ic1 = (dir > 0) ? 0 : (sequence.size() - 1);
        final int ic2 = (dir > 0) ? sequence.size() : (-1);
        int resultIndex = (dir > 0) ? (-1) : results.size();

        CandidateLoop:
        for (int ic = ic1; ic != ic2; ic += dir) {
            final Candidate candidate = sequence.get(ic);
            final List<Integer> lineCounts = candidate.getLineCounts();
            logger.debug("\nCandidate {} [{}]", candidate, IntUtil.toCsvString(lineCounts));

            // Check with logical parts currently defined
            resultIndex += dir;
            for (; ((dir > 0) && (resultIndex < results.size()))
                           || ((dir < 0) && (resultIndex >= 0)); resultIndex += dir) {
                final ResultEntry resultEntry = results.get(resultIndex);
                final LogicalPart result = resultEntry.result;
                logger.debug("   Comparing with {}/ {}", resultIndex, result);

                // Check parts are compatible in terms of staves counts
                if (result.getStaffCount() == candidate.getStaffCount()) {

                    // Check parts are compatible in terms of line counts
                    // (for backward compatibility, check is made only if line counts exist)
                    if (lineCounts.isEmpty() || result.getLineCounts().isEmpty()
                                || Objects.deepEquals(result.getLineCounts(), lineCounts)) {
                        logger.debug("   Success");
                        resultEntry.candidates.add(candidate);

                        // Use name of affiliate to define abbreviation of logical?
                        final String abbrev = resultEntry.result.getAbbreviation();
                        if (abbrev == null) {
                            final String affiName = candidate.getName();
                            if ((affiName != null) && !affiName.equals(resultEntry.result.getName())) {
                                resultEntry.result.setAbbreviation(affiName);
                            }
                        }

                        continue CandidateLoop;
                    } else {
                        logger.debug("   Line count incompatibility");
                    }
                } else {
                    logger.debug("   Staff count incompatibility");
                }
            }

            logger.debug("   No more entries available");

            // Create a brand new logical part for this candidate
            addResultEntry(dir, candidate, results);
        }
    }

    //-----------------//
    // renumberResults //
    //-----------------//
    /**
     * Renumber the result entries.
     */
    private void renumberResults ()
    {
        for (int i = 0; i < resultEntries.size(); i++) {
            final ResultEntry resultEntry = resultEntries.get(i);
            final LogicalPart result = resultEntry.result;
            final int id = i + 1;
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
     * Interface <code>Candidate</code> is used to process part candidates, regardless whether they
     * are provided as:
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
         * Report the line count for each staff.
         *
         * @return the list of line counts
         */
        List<Integer> getLineCounts ();

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

    //~ Inner Classes ------------------------------------------------------------------------------
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
