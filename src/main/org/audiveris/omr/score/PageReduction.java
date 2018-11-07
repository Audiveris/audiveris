//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   P a g e R e d u c t i o n                                    //
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
package org.audiveris.omr.score;

import org.audiveris.omr.score.PartConnection.Candidate;
import org.audiveris.omr.score.PartConnection.ResultEntry;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class {@code PageReduction} reduces the parts of each system to a list of parts
 * defined at page level.
 *
 * @author Hervé Bitteur
 */
public class PageReduction
{

    private static final Logger logger = LoggerFactory.getLogger(PageReduction.class);

    /** Related page. */
    private final Page page;

    /**
     * Creates a new PageReduction object.
     *
     * @param page the page to process
     */
    public PageReduction (Page page)
    {
        this.page = page;
    }

    //--------//
    // reduce //
    //--------//
    /**
     * Build the page LogicalPart instances by connecting the page systems (physical)
     * Part instances.
     */
    public void reduce ()
    {
        final List<List<Candidate>> sequences = buildSequences(page);

        // Connect the parts across all systems of the page
        final PartConnection connection = new PartConnection(sequences);
        final List<PartConnection.ResultEntry> resultEntries = connection.getResults();

        if (logger.isDebugEnabled()) {
            connection.dumpResults();
        }

        // Store the list of LogicalPart instances into page
        storeResults(resultEntries);
    }

    //----------------//
    // buildSequences //
    //----------------//
    /**
     * Build the sequences of part candidates
     *
     * @param page the containing (Audiveris) page
     * @return the sequences of system part candidates
     */
    private List<List<Candidate>> buildSequences (Page page)
    {
        // Build candidates (here, a candidate is a Part)
        List<List<Candidate>> sequences = new ArrayList<List<Candidate>>();

        for (SystemInfo system : page.getSystems()) {
            List<Candidate> parts = new ArrayList<Candidate>();

            for (Part systemPart : system.getParts()) {
                parts.add(new PartCandidate(systemPart));
            }

            sequences.add(parts);
        }

        return sequences;
    }

    //--------------//
    // storeResults //
    //--------------//
    /**
     * Store the results as the page list of LogicalPart instances
     *
     * @param resultEntries results from part connection
     */
    private void storeResults (List<ResultEntry> resultEntries)
    {
        List<LogicalPart> logicalParts = new ArrayList<LogicalPart>();

        for (ResultEntry entry : resultEntries) {
            LogicalPart logicalPart = entry.result;
            logicalParts.add(logicalPart);
        }

        if (!Objects.deepEquals(page.getLogicalParts(), logicalParts)) {
            page.setLogicalParts(logicalParts);
            page.getSheet().getStub().setModified(true);
        }
    }

    //---------------//
    // PartCandidate //
    //---------------//
    /**
     * Wrapping class meant for a (System) physical Part instance.
     */
    private static class PartCandidate
            implements Candidate
    {

        private final Part systemPart;

        public PartCandidate (Part part)
        {
            this.systemPart = part;
        }

        @Override
        public String getAbbreviation ()
        {
            return null;
        }

        @Override
        public String getName ()
        {
            return systemPart.getName();
        }

        @Override
        public int getStaffCount ()
        {
            return systemPart.getStaves().size();
        }

        @Override
        public void setId (int id)
        {
            systemPart.setId(id);
        }

        @Override
        public String toString ()
        {
            return systemPart + " in " + systemPart.getSystem();
        }
    }
}
