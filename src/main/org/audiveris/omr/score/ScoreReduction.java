//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S c o r e R e d u c t i o n                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.score.PartConnection.Candidate;
import org.audiveris.omr.score.PartConnection.ResultEntry;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.SheetStub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class {@code ScoreReduction} reduces the logical parts for a score,
 * based on the merge of Audiveris Page instances.
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
     * Build the score LogicalPart instances by connecting the pages logical parts.
     *
     * @param stubs valid selected stubs
     * @return the count of modifications done
     */
    public int reduce (List<SheetStub> stubs)
    {
        final List<List<Candidate>> sequences = buildSequences(score.getPages(stubs));

        // Connect the parts across all pages of the score
        PartConnection connection = new PartConnection(sequences);

        List<ResultEntry> resultEntries = connection.getResults();

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
     * Build the sequences of part candidates
     *
     * @param pages the sequence of pages
     * @return the sequences of page LogicalPart candidates
     */
    private List<List<Candidate>> buildSequences (List<Page> pages)
    {
        // Build candidates (here a candidate is a LogicalPart, with affiliated system parts)
        List<List<Candidate>> sequences = new ArrayList<>();

        for (Page page : pages) {
            List<Candidate> candidates = new ArrayList<>();
            List<LogicalPart> partList = page.getLogicalParts();

            if (partList != null) {
                for (LogicalPart logicalPart : partList) {
                    Candidate candidate = new LogicalPartCandidate(
                            logicalPart,
                            page,
                            page.getSystemPartsById(logicalPart.getId()));
                    candidates.add(candidate);
                }
            }

            sequences.add(candidates);
        }

        return sequences;
    }

    //--------------//
    // storeResults //
    //--------------//
    /**
     * Store the results as the score list of LogicalPart instances
     *
     * @param resultEntries results from part connection
     * @return true if score part list has really been modified
     */
    private boolean storeResults (List<ResultEntry> resultEntries)
    {
        List<LogicalPart> partList = new ArrayList<>();

        for (ResultEntry entry : resultEntries) {
            LogicalPart logicalPart = entry.result;
            partList.add(logicalPart);
        }

        if (!Objects.deepEquals(score.getLogicalParts(), partList)) {
            score.setLogicalParts(partList);

            return true;
        }

        return false;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------------------//
    // LogicalPartCandidate //
    //----------------------//
    /**
     * Wrapping class meant for a (Page) LogicalPart instance candidate.
     */
    private static class LogicalPartCandidate
            implements Candidate
    {

        private final LogicalPart logicalPart;

        private final Page page;

        private final List<Part> systemParts;

        LogicalPartCandidate (LogicalPart logicalPart,
                              Page page,
                              List<Part> systemParts)
        {
            this.logicalPart = logicalPart;
            this.page = page;
            this.systemParts = systemParts;
        }

        @Override
        public String getAbbreviation ()
        {
            return logicalPart.getAbbreviation();
        }

        @Override
        public String getName ()
        {
            return logicalPart.getName();
        }

        @Override
        public int getStaffCount ()
        {
            return logicalPart.getStaffCount();
        }

        @Override
        public void setId (int id)
        {
            logicalPart.setId(id);

            for (Part part : systemParts) {
                part.setId(id);
            }
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());

            sb.append("{");

            sb.append(page);

            sb.append(logicalPart);

            sb.append("}");

            for (Part part : systemParts) {
                sb.append("\n      ").append(part).append(" in ").append(part.getSystem());
            }

            return sb.toString();
        }
    }
}
