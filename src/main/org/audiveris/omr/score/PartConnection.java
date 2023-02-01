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

import org.audiveris.omr.sheet.SheetStub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Class <code>PartConnection</code> is in charge of finding the connections of parts across
 * systems (and pages) so that a part always represents the same instrument all along
 * the score.
 * <p>
 * <p>
 * The strategy used to build LogicalPart's out of PartRef's is based on the following assumptions:
 * <ul>
 * <li>For a part of a system to be connected to a part of another system, they must exhibit the
 * same count of staves and the same count of lines in corresponding staves</li>
 * <li>Parts cannot be swapped from one system to the other. In other words, we cannot have say
 * partA followed by partB in a system, and partB followed by partA in another system.</li>
 * <li>A part with 2 staves, if any, is used as a pivot to align connections.</li>
 * <li>Otherwise, since additional parts appear at the top of a system, rather than at the bottom,
 * we process part connections bottom up.</li>
 * <li>When possible, we use the part names (or abbreviations) to help the connection algorithm,
 * but this is questionable for lack of OCR reliability on part names and abbreviations.
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
     * The sorted list of resulting parts, each with its affiliated parts.
     */
    private final List<ResultEntry> resultEntries = new ArrayList<>();

    /**
     * Are the score LogicalPart's locked?.
     */
    private boolean logicalsLocked;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>PartConnection</code> object.
     *
     * @param sequences the list of sequences of parts
     * @param logicals  the pre-populated list of LogicalPart's, or null if un-locked
     */
    public PartConnection (List<List<PartRef>> sequences,
                           List<LogicalPart> logicals)
    {
        if (logicals != null) {
            logicalsLocked = true;

            // Allocate the resultEntries
            for (LogicalPart logical : logicals) {
                resultEntries.add(new ResultEntry(logical));
            }
        }

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
            sb.append("\n").append(entry.logical);

            for (PartRef partRef : entry.partRefs) {
                final SystemRef system = partRef.getSystem();
                final int rank = 1 + system.getParts().indexOf(partRef);
                final PageRef page = system.getPage();
                final SheetStub stub = page.getStub();

                // @formatter:off
                sb.append("\n   ").append(partRef)
                        .append(" rank:").append(rank)
                        .append(" in ").append(stub)
                        .append(", Page#").append(page.getId())
                        .append(", System#").append(system.getId());
                // @formatter:on
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
     * Build a new ResultEntry based on provided PartRef and insert it at last position,
     * according to provided direction, in the list of results.
     *
     * @param dir     -1 for up, +1 for down
     * @param partRef the provided candidate partRef
     * @param results (output) the results to be augmented
     */
    private void addResultEntry (int dir,
                                 PartRef partRef,
                                 List<ResultEntry> results)
    {
        // Create a brand new logical part for this candidate part
        final LogicalPart logical = new LogicalPart(
                0, // This id is irrelevant for the time being
                partRef.getStaffCount(),
                partRef.getLineCounts());
        logical.setName(partRef.getName());
        logical.setAbbreviation(null);
        logical.setLineCounts(partRef.getLineCounts());
        logger.debug("Created {} from {}", logical, partRef);

        final ResultEntry entry = new ResultEntry(logical);
        entry.partRefs.add(partRef);

        // Insert at proper end of results
        results.add(dir < 0 ? 0 : results.size(), entry);
    }

    //---------//
    // biIndex //
    //---------//
    /**
     * Report the index in provided PartRef's sequence of the (first) 2-staff PartRef.
     *
     * @param sequence sequence of partRef's
     * @return index of bi-staff part in sequence, or -1 if not found
     */
    private int biIndex (List<PartRef> sequence)
    {
        for (PartRef partRef : sequence) {
            if (partRef.getStaffCount() == 2) {
                return sequence.indexOf(partRef);
            }
        }

        return -1;
    }

    //---------//
    // connect //
    //---------//
    /**
     * The heart of the part connection algorithm, when we connect parts across several pages.
     *
     * @param sequences a list of sequences of candidate parts (one sequence = one system)
     */
    private void connect (List<List<PartRef>> sequences)
    {
        // Two-staff entry found, if any
        ResultEntry biEntry = null;

        // Process each sequence of candidate parts in turn
        // One such sequence = one system
        for (int iSeq = 0; iSeq < sequences.size(); iSeq++) {
            final List<PartRef> sequence = sequences.get(iSeq);
            final int biIndex = biIndex(sequence);

            if (iSeq == 0) {
                dispatch(sequence, resultEntries, +1);

                if (biIndex != -1) {
                    biEntry = resultEntries.get(biIndex);
                }
            } else {
                if ((biEntry != null) && (biIndex != -1)) {
                    // Align on the biEntry
                    biEntry.partRefs.add(sequence.get(biIndex));

                    // Process parts above biEntry
                    dispatch(
                            sequence.subList(0, biIndex),
                            resultEntries.subList(0, resultEntries.indexOf(biEntry)),
                            -1);

                    // Process parts below biEntry
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

        if (!logicalsLocked) {
            // Assign ids to logical parts
            renumberResults();
        }
    }

    //----------//
    // dispatch //
    //----------//
    /**
     * Dispatch the list of candidate PartRef's (the parts of a system) to the
     * current list of logical parts.
     *
     * @param sequence the (system) candidate parts to dispatch
     * @param results  (input/output) the current sub-list of logical parts defined so far
     * @param dir      -1 or +1 for browsing up or down
     */
    private void dispatch (List<PartRef> sequence,
                           List<ResultEntry> results,
                           int dir)
    {
        final int ic1 = (dir > 0) ? 0 : (sequence.size() - 1); // Starting index value
        final int ic2 = (dir > 0) ? sequence.size() : (-1); // Breaking index value
        int resultIndex = (dir > 0) ? (-1) : results.size(); // Current index in existing results

        // All result names
        final Set<String> resNames = new LinkedHashSet<>();
        for (ResultEntry result : results) {
            final String name = result.logical.getName();
            if (name != null)
                resNames.add(name);
        }

        CandidateLoop:
        for (int ic = ic1; ic != ic2; ic += dir) {
            final PartRef partRef = sequence.get(ic);
            final List<Integer> lineCounts = partRef.getLineCounts();
            logger.debug("\nCandidate {}", partRef.toQualifiedString());

            // Check with logical parts already defined
            resultIndex += dir;
            for (; ((dir > 0) && (resultIndex < results.size())) || ((dir < 0)
                    && (resultIndex >= 0)); resultIndex += dir) {
                final ResultEntry resultEntry = results.get(resultIndex);
                final LogicalPart logical = resultEntry.logical;
                logger.debug("  Comparing with {}", logical);

                // Check parts are compatible in terms of staves counts
                if (logical.getStaffCount() != partRef.getStaffCount()) {
                    logger.debug("   Staff count incompatibility");
                    continue;
                }

                // Check parts are compatible in terms of line counts
                // For backward compatibility w/ old OMRs, check is made only if line counts exist
                if (!lineCounts.isEmpty() && !logical.getLineCounts().isEmpty() && !Objects
                        .deepEquals(logical.getLineCounts(), lineCounts)) {
                    logger.debug("    Line counts incompatibility");
                    continue;
                }

                // Check candidate name
                // @formatter:off
                final String name = partRef.getName();
                if ((name != null)
                    && !name.equalsIgnoreCase(logical.getName())
                    && !name.equalsIgnoreCase(logical.getAbbreviation())) {
                    logger.debug("    Name incompatibility");
                    continue;
                }
                // @formatter:on

                logger.debug("    Success");
                resultEntry.partRefs.add(partRef);

                // Use name of affiliate part to define abbreviation of logical?
                final String resAbbrev = logical.getAbbreviation();
                if (resAbbrev == null) {
                    final String resName = logical.getName();
                    final String affiName = partRef.getName();
                    // @formatter:off
                    if ((affiName != null)
                        && !affiName.equals(resName)
                        && (resName == null || affiName.length() < logical.getName().length())
                        && !resNames.contains(affiName)) {
                        logical.setAbbreviation(affiName);
                    }
                    // @formatter:on
                }

                continue CandidateLoop;
            }

            logger.debug("  No more entries available");

            if (!logicalsLocked) {
                // Create a brand new logical part for this candidate
                addResultEntry(dir, partRef, results);
            } else {
                logger.warn("  Cannot map {} to any logical", partRef.toQualifiedString());
            }
        }
    }

    //-----------------//
    // renumberResults //
    //-----------------//
    /**
     * Renumber the logical entries.
     */
    private void renumberResults ()
    {
        for (int i = 0; i < resultEntries.size(); i++) {
            final ResultEntry resultEntry = resultEntries.get(i);
            final int id = i + 1;
            resultEntry.logical.setId(id);

            logger.debug("Final {}", resultEntry.logical);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-------------//
    // ResultEntry //
    //-------------//
    /**
     * Entry to gather results related to a single logical part.
     */
    public static class ResultEntry
    {
        /** Resulting logical part. */
        final LogicalPart logical;

        /** Affiliated candidate parts. */
        final List<PartRef> partRefs = new ArrayList<>();

        public ResultEntry (LogicalPart logical)
        {
            this.logical = logical;
        }
    }
}
