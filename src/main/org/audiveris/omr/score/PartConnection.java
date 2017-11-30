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

import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.proxymusic.PartList;
import org.audiveris.proxymusic.PartName;
import org.audiveris.proxymusic.ScorePartwise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class {@code PartConnection} is in charge of finding the connections of parts across
 * systems (and pages) so that a part always represents the same instrument all along
 * the score.
 * <p>
 * This work is done across:
 * <ul>
 * <li>The various systems of a page using Audiveris SystemInfo instances.</li>
 * <li>The various pages of a score using Audiveris Page instances.</li>
 * <li>The various pages of a score using Proxymusic ScorePartwise instances.</li>
 * </ul>
 * <p>
 * All together, this sums up to three different cases to handle, so we have taken a generic
 * approach, abstracting the different types into Candidates and Results.
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

    private static final Logger logger = LoggerFactory.getLogger(
            PartConnection.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Input data. */
    private final Set<List<Candidate>> sequences;

    /** Record the set of candidates per result. */
    private final SortedMap<Result, Set<Candidate>> resultMap = new TreeMap<Result, Set<Candidate>>();

    /** Record which result is mapped to which candidate. */
    private final Map<Candidate, Result> candidateMap = new LinkedHashMap<Candidate, Result>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new PartConnection object.
     * Not meant to be called directly, use proper static methods instead:
     * {@link #connectPageSystems},
     * {@link #connectScorePages} or
     * {@link #connectProxyPages}.
     *
     * @param sequences a set of sequences of parts
     */
    private PartConnection (Set<List<Candidate>> sequences)
    {
        this.sequences = sequences;

        connect();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------------//
    // connectPageSystems //
    //--------------------//
    /**
     * Convenient method to connect parts across systems of a page.
     * <p>
     * This method is to be used when processing one page, and simply connecting the parts of the
     * systems that appear on this page. Here we work with Audiveris SystemInfo entities.
     *
     * @param page the containing page
     * @return the mapping found
     */
    public static PartConnection connectPageSystems (Page page)
    {
        // Build candidates (here, a candidate is a Part)
        Set<List<Candidate>> sequences = new LinkedHashSet<List<Candidate>>();

        for (SystemInfo system : page.getSystems()) {
            List<Candidate> parts = new ArrayList<Candidate>();

            for (Part systemPart : system.getParts()) {
                parts.add(new PartCandidate(systemPart));
            }

            sequences.add(parts);
        }

        return new PartConnection(sequences);
    }

    //-------------------//
    // connectProxyPages //
    //-------------------//
    /**
     * Convenient method to connect parts across pages.
     * <p>
     * This method is to be used when merging the results of several pages.
     * Here we work with ProxyMusic ScorePartwise entities, since we expect each page result to be
     * provided via MusicXML.
     *
     * @param pages the sequence of pages, as (proxymusic) ScorePartwise instances
     * @return the mapping found
     */
    public static PartConnection connectProxyPages (SortedMap<Integer, ScorePartwise> pages)
    {
        // Build candidates (here a candidate is a LogicalPart)
        Set<List<Candidate>> sequences = new LinkedHashSet<List<Candidate>>();

        for (Entry<Integer, ScorePartwise> entry : pages.entrySet()) {
            int index = entry.getKey();
            ScorePartwise page = entry.getValue();
            PartList partList = page.getPartList();
            List<Candidate> parts = new ArrayList<Candidate>();

            for (Object obj : partList.getPartGroupOrScorePart()) {
                // TODO: For the time being, we ignore part-group elements.
                if (obj instanceof org.audiveris.proxymusic.ScorePart) {
                    org.audiveris.proxymusic.ScorePart logicalPart = (org.audiveris.proxymusic.ScorePart) obj;
                    parts.add(new PMScorePartCandidate(logicalPart, page, index));
                }
            }

            sequences.add(parts);
        }

        return new PartConnection(sequences);
    }

    //-------------------//
    // connectScorePages //
    //-------------------//
    /**
     * Convenient method to connect parts across pages.
     * <p>
     * This method is to be used when merging the results of several pages.
     * Here we work directly with Audiveris Page entities
     *
     * @param pages the sequence of pages, as (audiveris) Page instances
     * @return the mapping found
     */
    public static PartConnection connectScorePages (SortedMap<Integer, Page> pages)
    {
        // Build candidates (here a candidate is a LogicalPart)
        Set<List<Candidate>> sequences = new LinkedHashSet<List<Candidate>>();

        for (Entry<Integer, Page> entry : pages.entrySet()) {
            Page page = entry.getValue();
            List<Candidate> parts = new ArrayList<Candidate>();
            List<LogicalPart> partList = page.getLogicalParts();

            if (partList != null) {
                for (LogicalPart logicalPart : partList) {
                    parts.add(new LogicalPartCandidate(logicalPart, page));
                }
            }

            sequences.add(parts);
        }

        return new PartConnection(sequences);
    }

    //-----------------//
    // getCandidateMap //
    //-----------------//
    /**
     * Report an unmodifiable view of which resulting part has been assigned to any
     * given candidate
     *
     * @return the candidateMap (candidate &rarr; assigned result)
     */
    public Map<Candidate, Result> getCandidateMap ()
    {
        return Collections.unmodifiableMap(candidateMap);
    }

    //--------------//
    // getResultMap //
    //--------------//
    /**
     * Report which candidate parts have been mapped to any given result
     *
     * @return the resultMap ((sorted) result &rarr; (unsorted) set of candidates)
     */
    public SortedMap<Result, Set<Candidate>> getResultMap ()
    {
        return resultMap;
    }

    //---------//
    // connect //
    //---------//
    /**
     * The heart of the part connection algorithm, organized to work through interfaces
     * in order to use the same piece of code, when we connect parts across systems of
     * one page, or when we connect parts across several pages.
     */
    private void connect ()
    {
        /** Resulting sequence of ScorePart's */
        final List<Result> results = new ArrayList<Result>();

        /** Temporary map, to record the set of candidates per result */
        final Map<Result, Set<Candidate>> rawMap = new HashMap<Result, Set<Candidate>>();

        // Process each sequence of parts in turn (typically a sequence of parts is a system)
        for (List<Candidate> sequence : sequences) {
            // Current index in results sequence (built in reverse order)
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

                // Check with scoreParts currently defined
                resultIndex++;
                logger.debug("scorePartIndex:{}", resultIndex);

                if (resultIndex >= results.size()) {
                    logger.debug("No more scoreParts available");

                    // Create a brand new score part for this candidate
                    createResult(resultIndex, candidate, results, rawMap);
                } else {
                    Result result = results.get(resultIndex);
                    logger.debug("Part:{}", result);

                    // Check we are connectable in terms of staves
                    if (result.getStaffCount() != candidate.getStaffCount()) {
                        logger.debug("Count incompatibility");

                        // Create a brand new score part for this candidate
                        createResult(resultIndex, candidate, results, rawMap);
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
                        candidateMap.put(candidate, result);
                        logger.debug(
                                "Compatible." + " Mapped candidate {} to result {}",
                                candidate,
                                result);

                        rawMap.get(result).add(candidate);
                    }
                }
            }
        }

        // Reverse and number LogicalPart instances
        Collections.reverse(results);

        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            int id = i + 1;
            result.setId(id);

            // Forge a LogicalPart name if none has been assigned
            if (result.getName() == null) {
                result.setName("Part_" + id);
            }

            logger.debug("Final {}", result);
        }

        // Now that results are ordered, we can deliver the sorted map
        resultMap.putAll(rawMap);
    }

    //--------------//
    // createResult //
    //--------------//
    private Result createResult (int resultIndex,
                                 Candidate candidate,
                                 List<Result> results,
                                 Map<Result, Set<Candidate>> rawMap)
    {
        Set<Candidate> candidates = new LinkedHashSet<Candidate>();
        Result result = candidate.createResult();
        candidateMap.put(candidate, result);
        logger.debug("Creation. Mapped candidate {} to result {}", candidate, result);

        candidates.add(candidate);
        rawMap.put(result, candidates);
        results.add(resultIndex, result);

        return result;
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //-----------//
    // Candidate //
    //-----------//
    /**
     * Interface {@code Candidate} is used to process part candidates, regardless whether they are
     * provided:
     * <ul>
     * <li>as Audiveris {@link org.audiveris.omr.sheet.Part} instances
     * (produced by the scanning of just one page)</li>
     * <li>as Audiveris {@link org.audiveris.omr.score.LogicalPart}
     * (when merging Audiveris pages)</li>
     * <li>as ProxyMusic {@link org.audiveris.proxymusic.ScorePart} instances
     * (used when merging MusicXML files).</li>
     * </ul>
     */
    public static interface Candidate
    {
        //~ Methods --------------------------------------------------------------------------------

        /** Create a related result instance consistent with this type
         *
         * @return the result
         */
        Result createResult ();

        /** Report the abbreviation, if any, that relates to this part
         *
         * @return the abbreviation if any
         */
        String getAbbreviation ();

        /** Index of the input: System # for Part, Page # for LogicalPart
         *
         * @return the input index
         */
        int getInputIndex ();

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

        /** Report the underlying object
         *
         * @return the object
         */
        Object getUnderlyingObject ();
    }

    //--------//
    // Result //
    //--------//
    /**
     * Interface {@code Result} is used to process resulting LogicalPart instances,
     * regardless whether they are instances of standard Audiveris {@link LogicalPart}
     * or instances of ProxyMusic {@link org.audiveris.proxymusic.ScorePart}.
     */
    public static interface Result
            extends Comparable<Result>
    {
        //~ Methods --------------------------------------------------------------------------------

        /** Report the part abbreviation, if any
         *
         * @return the abbreviation string assigned to the part, or null
         */
        String getAbbreviation ();

        /** Report the candidate object used to build this result
         *
         * @return the candidate
         */
        Candidate getCandidate ();

        /** Report the part id
         *
         * @return the part ID
         */
        int getId ();

        /** Report the part name
         *
         * @return the part name
         */
        String getName ();

        /** Report the number of staves in that part
         *
         * @return the number of staves in part
         */
        int getStaffCount ();

        /** Report the actual underlying instance
         *
         * @return the underlying object
         */
        Object getUnderlyingObject ();

        /** Assign an abbreviation to the part
         *
         * @param abbreviation
         */
        void setAbbreviation (String abbreviation);

        /** Assign an unique id to the part
         *
         * @param id
         */
        void setId (int id);

        /** Assign a name to the part
         *
         * @param name
         */
        void setName (String name);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------------//
    // AbstractResult //
    //----------------//
    private abstract static class AbstractResult
            implements Result
    {
        //~ Instance fields ------------------------------------------------------------------------

        protected final Candidate candidate;

        //~ Constructors ---------------------------------------------------------------------------
        public AbstractResult (Candidate candidate)
        {
            this.candidate = candidate;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int compareTo (Result other)
        {
            return Integer.signum(getId() - other.getId());
        }

        @Override
        public Candidate getCandidate ()
        {
            return candidate;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append("{");

            sb.append(" id:").append(getId());

            sb.append(" name:\"").append(getName()).append("\"");

            if (getAbbreviation() != null) {
                sb.append(" abbr:\"").append(getAbbreviation()).append("\"");
            }

            sb.append(" staffCount:").append(getStaffCount());

            sb.append("}");

            return sb.toString();
        }
    }

    //----------------------//
    // LogicalPartCandidate //
    //----------------------//
    /**
     * Wrapping class meant for a LogicalPart instance candidate.
     */
    private static class LogicalPartCandidate
            implements Candidate
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final LogicalPart logicalPart;

        private final Page page;

        //~ Constructors ---------------------------------------------------------------------------
        public LogicalPartCandidate (LogicalPart logicalPart,
                                     Page page)
        {
            this.logicalPart = logicalPart;
            this.page = page;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public LogicalPartResult createResult ()
        {
            // Create a brand new score part for this candidate
            // Id is irrelevant for the time being
            LogicalPartResult result = new LogicalPartResult(
                    this,
                    new LogicalPart(0, getStaffCount()));
            result.setName(getName());
            result.setAbbreviation(getAbbreviation());
            logger.debug("Created {} from {}", result, this);

            return result;
        }

        @Override
        public String getAbbreviation ()
        {
            return logicalPart.getAbbreviation();
        }

        @Override
        public int getInputIndex ()
        {
            return page.getSheet().getStub().getNumber();
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
        public Object getUnderlyingObject ()
        {
            return logicalPart;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());

            sb.append("{");

            sb.append(" page#").append(getInputIndex());

            sb.append(" \"").append(getName()).append("\"");

            sb.append(" staffCount:").append(getStaffCount());

            sb.append("}");

            return sb.toString();
        }
    }

    //-------------------//
    // LogicalPartResult //
    //-------------------//
    /**
     * Wrapping class meant for a LogicalPart instance result.
     */
    private static class LogicalPartResult
            extends AbstractResult
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final LogicalPart logicalPart;

        //~ Constructors ---------------------------------------------------------------------------
        public LogicalPartResult (Candidate candidate,
                                  LogicalPart logicalPart)
        {
            super(candidate);
            this.logicalPart = logicalPart;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String getAbbreviation ()
        {
            return logicalPart.getAbbreviation();
        }

        @Override
        public int getId ()
        {
            return logicalPart.getId();
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
        public LogicalPart getUnderlyingObject ()
        {
            return logicalPart;
        }

        @Override
        public void setAbbreviation (String abbreviation)
        {
            logicalPart.setAbbreviation(abbreviation);
        }

        @Override
        public void setId (int id)
        {
            logicalPart.setId(id);
        }

        @Override
        public void setName (String name)
        {
            logicalPart.setName(name);
        }
    }

    //----------------------//
    // PMScorePartCandidate //
    //----------------------//
    /**
     * Wrapping class meant for a proxymusic LogicalPart instance candidate.
     */
    private static class PMScorePartCandidate
            implements Candidate
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final org.audiveris.proxymusic.ScorePart logicalPart;

        private final ScorePartwise scorePartwise;

        private final int inputIndex;

        private Integer staffCount;

        //~ Constructors ---------------------------------------------------------------------------
        public PMScorePartCandidate (org.audiveris.proxymusic.ScorePart logicalPart,
                                     ScorePartwise scorePartwise,
                                     int inputIndex)
        {
            this.logicalPart = logicalPart;
            this.scorePartwise = scorePartwise;
            this.inputIndex = inputIndex;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public PMScorePartResult createResult ()
        {
            // Create a brand new score part for this candidate
            // Id is irrelevant for the time being

            /** Factory for proxymusic entities */
            org.audiveris.proxymusic.ObjectFactory factory = new org.audiveris.proxymusic.ObjectFactory();

            PMScorePartResult result = new PMScorePartResult(
                    this,
                    getStaffCount(),
                    factory.createScorePart());

            result.setName(getName());
            result.setAbbreviation(getAbbreviation());
            logger.debug("Created {} from {}", result, this);

            return result;
        }

        @Override
        public String getAbbreviation ()
        {
            PartName partName = logicalPart.getPartAbbreviation();

            if (partName != null) {
                return partName.getValue();
            } else {
                return null;
            }
        }

        @Override
        public int getInputIndex ()
        {
            return inputIndex;
        }

        @Override
        public String getName ()
        {
            PartName partName = logicalPart.getPartName();

            if (partName != null) {
                return partName.getValue();
            } else {
                return null;
            }
        }

        @Override
        public int getStaffCount ()
        {
            if (staffCount == null) {
                // Determine the corresponding staff count
                // We have to dig into the first measure of the part itself
                staffCount = 1; // Default value

                String id = logicalPart.getId();

                ///logger.debug("logicalPart id:" + id);
                for (ScorePartwise.Part part : scorePartwise.getPart()) {
                    if (part.getId() != logicalPart) {
                        continue;
                    }

                    // Get first measure of this part
                    ScorePartwise.Part.Measure firstMeasure = part.getMeasure().get(0);

                    // Look for Attributes element
                    for (Object obj : firstMeasure.getNoteOrBackupOrForward()) {
                        if (!(obj instanceof org.audiveris.proxymusic.Attributes)) {
                            continue;
                        }

                        org.audiveris.proxymusic.Attributes attributes = (org.audiveris.proxymusic.Attributes) obj;
                        BigInteger staves = attributes.getStaves();

                        if (staves != null) {
                            staffCount = staves.intValue();

                            break;
                        }
                    }

                    break;
                }
            }

            return staffCount;
        }

        @Override
        public Object getUnderlyingObject ()
        {
            return logicalPart;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());

            sb.append("{");

            sb.append(" page#").append(inputIndex);

            sb.append(" \"").append(getName()).append("\"");

            sb.append(" staffCount:").append(getStaffCount());

            sb.append("}");

            return sb.toString();
        }
    }

    //-------------------//
    // PMScorePartResult //
    //-------------------//
    /**
     * Wrapping class meant for a proxymusic LogicalPart instance result.
     */
    private static class PMScorePartResult
            extends AbstractResult
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final int staffCount;

        private final org.audiveris.proxymusic.ScorePart logicalPart;

        private int id;

        //~ Constructors ---------------------------------------------------------------------------
        public PMScorePartResult (Candidate candidate,
                                  int staffCount,
                                  org.audiveris.proxymusic.ScorePart logicalPart)
        {
            super(candidate);
            this.staffCount = staffCount;
            this.logicalPart = logicalPart;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String getAbbreviation ()
        {
            PartName partName = logicalPart.getPartAbbreviation();

            if (partName != null) {
                return partName.getValue();
            } else {
                return null;
            }
        }

        @Override
        public int getId ()
        {
            return id;
        }

        @Override
        public String getName ()
        {
            PartName partName = logicalPart.getPartName();

            if (partName != null) {
                return partName.getValue();
            } else {
                return null;
            }
        }

        @Override
        public int getStaffCount ()
        {
            return staffCount;
        }

        @Override
        public org.audiveris.proxymusic.ScorePart getUnderlyingObject ()
        {
            return logicalPart;
        }

        @Override
        public void setAbbreviation (String abbreviation)
        {
            org.audiveris.proxymusic.ObjectFactory factory = new org.audiveris.proxymusic.ObjectFactory();
            PartName partName = factory.createPartName();
            logicalPart.setPartAbbreviation(partName);
            partName.setValue(abbreviation);
        }

        @Override
        public void setId (int id)
        {
            this.id = id;
            logicalPart.setId("P" + id);
        }

        @Override
        public void setName (String name)
        {
            org.audiveris.proxymusic.ObjectFactory factory = new org.audiveris.proxymusic.ObjectFactory();
            org.audiveris.proxymusic.PartName partName = factory.createPartName();
            logicalPart.setPartName(partName);
            partName.setValue(name);
        }
    }

    //---------------//
    // PartCandidate //
    //---------------//
    /**
     * Wrapping class meant for a Part instance.
     */
    private static class PartCandidate
            implements Candidate
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Part systemPart;

        //~ Constructors ---------------------------------------------------------------------------
        public PartCandidate (Part part)
        {
            this.systemPart = part;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public LogicalPartResult createResult ()
        {
            // Create a brand new score part for this candidate
            // Id is irrelevant for the time being
            LogicalPartResult result = new LogicalPartResult(
                    this,
                    new LogicalPart(0, getStaffCount()));
            result.setName(getName());
            result.setAbbreviation(getAbbreviation());
            logger.debug("Created {} from {}", result, this);

            return result;
        }

        @Override
        public String getAbbreviation ()
        {
            return null;
        }

        @Override
        public int getInputIndex ()
        {
            return systemPart.getSystem().getId();
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
        public Object getUnderlyingObject ()
        {
            return systemPart;
        }

        @Override
        public String toString ()
        {
            return "S" + getInputIndex() + "-" + systemPart.toString();
        }
    }
}
