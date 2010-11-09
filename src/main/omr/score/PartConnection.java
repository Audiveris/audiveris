//----------------------------------------------------------------------------//
//                                                                            //
//                        P a r t C o n n e c t i o n                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.log.Logger;

import omr.score.entity.ScorePart;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;

import omr.util.TreeNode;

import proxymusic.PartList;
import proxymusic.PartName;
import proxymusic.ScorePartwise;

import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;

/**
 * Class {@code PartConnection} is in charge of finding the connections of parts
 * across systems (and pages) so that a part always represents the same
 * instrument all along the score.
 *
 * <p>This work is done across the various systems of a page. It is done also
 * across the various pages of a score. The same strategy is used across systems
 * and across pages. It is based on the following assumptions:
 * <ul>
 * <li>For a  part of a system to be connected to a part of another system,
 * they must exhibit the same count of staves.</li>
 * <li>Parts cannot be swapped from one system to the other. In other words, we
 * cannot have say partA followed by partB in a system, and partB followed by
 * partA in another system.</li>
 * <li>Additional parts appear at the top of a system, rather than at the
 * bottom. So we process part connections bottom up.</li>
 * <li>When possible, we use the part names (or abbreviations) to help the
 * connection algorithm.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class PartConnection
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(PartConnection.class);

    //~ Instance fields --------------------------------------------------------

    /** Input data */
    private final Set<List<Candidate>> sequences;

    /** Record the sorted set of candidates per result */
    private final SortedMap<Result, SortedSet<Candidate>> resultMap = new TreeMap<Result, SortedSet<Candidate>>();

    /** Record which result is mapped to which candidate */
    private final SortedMap<Candidate, Result> candidateMap = new TreeMap<Candidate, Result>();

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // PartConnection //
    //----------------//
    /**
     * Creates a new PartConnection object.
     * Not meant to be called directly, use static methods
     * {@link #connectSystems} or {@link #connectPages} instead.
     * @param sequences a set of sequences of parts
     */
    private PartConnection (Set<List<Candidate>> sequences)
    {
        this.sequences = sequences;

        connect();
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // connectPages //
    //--------------//
    /**
     * Convenient method to connect parts across pages.
     * This method is to be used when merging the results of several pages.
     * Here we work with ProxyMusic entities, since we expect each page result
     * to be provided via MusicXML.
     * @param pages the sequence of pages, as (proxymusic) ScorePartwise
     * instances
     */
    public static PartConnection connectPages (SortedMap<Integer, ScorePartwise> pages)
    {
        // Build candidates (here a candidate is a ScorePart)
        Set<List<Candidate>> sequences = new LinkedHashSet<List<Candidate>>();

        for (Entry<Integer, ScorePartwise> entry : pages.entrySet()) {
            int             index = entry.getKey();
            ScorePartwise   page = entry.getValue();
            PartList        partList = page.getPartList();
            List<Candidate> parts = new ArrayList<Candidate>();

            for (Object obj : partList.getPartGroupOrScorePart()) {
                // TODO: For the time being, we ignore part-group elements.
                if (obj instanceof proxymusic.ScorePart) {
                    proxymusic.ScorePart scorePart = (proxymusic.ScorePart) obj;
                    parts.add(new PMScorePartCandidate(scorePart, page, index));
                }
            }

            sequences.add(parts);
        }

        return new PartConnection(sequences);
    }

    //----------------//
    // connectSystems //
    //----------------//
    /**
     * Convenient method to connect parts across systems of a score (1 page).
     * This method is to be used when processing one page, and simply connecting
     * the parts of the systems that appear on this page. Here we work with
     * Audiveris entities.
     * @param score the containing score
     */
    public static void connectSystems (Score score)
    {
        // Build candidates (a candidate is a SystemPart)
        Set<List<Candidate>> sequences = new LinkedHashSet<List<Candidate>>();

        for (TreeNode sn : score.getSystems()) {
            ScoreSystem     system = (ScoreSystem) sn;
            List<Candidate> parts = new ArrayList<Candidate>();

            for (TreeNode pn : system.getParts()) {
                SystemPart systemPart = (SystemPart) pn;
                parts.add(new SystemPartCandidate(systemPart));
            }

            sequences.add(parts);
        }

        PartConnection  connector = new PartConnection(sequences);

        // Build part list
        List<ScorePart> scoreParts = new ArrayList<ScorePart>();

        for (Result result : connector.getResultMap()
                                      .keySet()) {
            scoreParts.add((ScorePart) result.getUnderlyingObject());
        }

        score.setPartList(scoreParts);

        // Make the connections
        Map<Candidate, Result> candidateMap = connector.getCandidateMap();

        if (logger.isFineEnabled()) {
            logger.fine("Candidates:" + candidateMap.size());
        }

        for (Entry<Candidate, Result> entry : candidateMap.entrySet()) {
            Candidate  candidate = entry.getKey();
            SystemPart systemPart = (SystemPart) candidate.getUnderlyingObject();

            Result     result = entry.getValue();
            ScorePart  scorePart = (ScorePart) result.getUnderlyingObject();

            systemPart.setScorePart(scorePart);
            systemPart.setId(scorePart.getId());
        }
    }

    //-----------------//
    // getCandidateMap //
    //-----------------//
    /**
     * @return the candidateMap (candidate -> assigned result)
     */
    public Map<Candidate, Result> getCandidateMap ()
    {
        return candidateMap;
    }

    //--------------//
    // getResultMap //
    //--------------//
    /**
     * @return the resultMap ((sorted) result -> (sorted) set of candidates)
     */
    public SortedMap<Result, SortedSet<Candidate>> getResultMap ()
    {
        return resultMap;
    }

    //---------//
    // connect //
    //---------//
    /**
     * The heart of the part connection algorithm, organized to work through
     * interfaces in order to use the same piece of code, when we connect
     * systems of one page, or when we connect parts across several pages.
     */
    private void connect ()
    {
        /** Resulting sequence of ScorePart's */
        final List<Result> results = new ArrayList<Result>();

        /** Temporary map, to record the sorted set of candidates per result */
        final Map<Result, SortedSet<Candidate>> rawMap = new HashMap<Result, SortedSet<Candidate>>();

        // Process each sequence of parts in turn
        // (typically a sequence of parts is a system)
        for (List<Candidate> sequence : sequences) {
            // Current index in results sequence (built in reverse order)
            int resultIndex = -1;

            if (logger.isFineEnabled()) {
                logger.fine("Processing new sequence ...");

                for (Candidate candidate : sequence) {
                    logger.fine("- " + candidate);
                }
            }

            // Process the sequence in reverse order (bottom up)
            for (ListIterator<Candidate> it = sequence.listIterator(
                sequence.size()); it.hasPrevious();) {
                Candidate candidate = it.previous();

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Processing candidate " + candidate + " count:" +
                        candidate.getStaffCount());
                }

                // Check with scoreParts currently defined
                resultIndex++;

                if (logger.isFineEnabled()) {
                    logger.fine("scorePartIndex:" + resultIndex);
                }

                if (resultIndex >= results.size()) {
                    if (logger.isFineEnabled()) {
                        logger.fine("No more scoreParts available");
                    }

                    // Create a brand new score part for this candidate
                    createResult(resultIndex, candidate, results, rawMap);
                } else {
                    Result result = results.get(resultIndex);

                    if (logger.isFineEnabled()) {
                        logger.fine("Part:" + result);
                    }

                    // Check we are connectable in terms of staves
                    if (result.getStaffCount() != candidate.getStaffCount()) {
                        if (logger.isFineEnabled()) {
                            logger.fine("Count incompatibility");
                        }

                        // Create a brand new score part for this candidate
                        createResult(resultIndex, candidate, results, rawMap);
                    } else {
                        // Can we use names? Just for fun for the time being
                        if ((candidate.getName() != null) &&
                            (result.getName() != null)) {
                            boolean namesOk = candidate.getName()
                                                       .equalsIgnoreCase(
                                result.getName());

                            if (logger.isFineEnabled()) {
                                logger.fine("Names OK: " + namesOk);

                                if (!namesOk) {
                                    logger.fine(
                                        "\"" + candidate.getName() +
                                        "\" vs \"" + result.getName() + "\"");
                                }
                            }
                        }

                        // We are compatible
                        if (logger.isFineEnabled()) {
                            logger.fine("Compatible");
                        }

                        candidateMap.put(candidate, result);
                        rawMap.get(result)
                              .add(candidate);
                    }
                }
            }
        }

        // Reverse and number ScorePart instances
        Collections.reverse(results);

        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            int    id = i + 1;
            result.setId(id);

            // Forge a ScorePart name if none has been assigned
            if (result.getName() == null) {
                result.setName("Part_" + id);
            }

            if (logger.isFineEnabled()) {
                logger.fine("Final " + result);
            }
        }

        // Now that results are ordered, we can deliver the sorted map
        resultMap.putAll(rawMap);
    }

    //--------------//
    // createResult //
    //--------------//
    private Result createResult (int                               resultIndex,
                                 Candidate                         candidate,
                                 List<Result>                      results,
                                 Map<Result, SortedSet<Candidate>> rawMap)
    {
        SortedSet<Candidate> candidates = new TreeSet<Candidate>();
        Result               result = candidate.createResult();
        candidateMap.put(candidate, result);
        candidates.add(candidate);
        rawMap.put(result, candidates);
        results.add(resultIndex, result);

        return result;
    }

    //~ Inner Interfaces -------------------------------------------------------

    //-----------//
    // Candidate //
    //-----------//
    /**
     * Interface {@code Candidate} is used to process part candidates,
     * regardless whether they are provided as Audiveris {@link
     * omr.score.entity.SystemPart} instances (produced by the scanning of just
     * one page) or as ProxyMusic {@link proxymusic.ScorePart} instances (used
     * when merging MusicXML files).
     */
    public static interface Candidate
        extends Comparable<Candidate>
    {
        //~ Methods ------------------------------------------------------------

        /** Report the abbreviation, if any, that relates to this part */
        public String getAbbreviation ();

        /** Index of the input: System # for SystemPart, Page # for ScorePart */
        public int getInputIndex ();

        /** Report the name of the part, if any */
        public String getName ();

        /** Report the number of staves in the part */
        public int getStaffCount ();

        /** Report the underlying object */
        public Object getUnderlyingObject ();

        /** Create a related result instance consistent with this type */
        public Result createResult ();
    }

    //--------//
    // Result //
    //--------//
    /**
     * Interface {@code Result} is used to process resulting ScorePart instances,
     * regardless whether they are instances of standard Audiveris {@link
     * ScorePart} or instances of ProxyMusic {@link proxymusic.ScorePart}.
     */
    public static interface Result
        extends Comparable<Result>
    {
        //~ Methods ------------------------------------------------------------

        public void setAbbreviation (String abbreviation);

        public String getAbbreviation ();

        /** Report the candidate object used to build this result */
        public Candidate getCandidate ();

        public void setId (int id);

        public int getId ();

        public void setName (String name);

        public String getName ();

        public int getStaffCount ();

        public Object getUnderlyingObject ();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------------------//
    // AbstractCandidate //
    //-------------------//
    private abstract static class AbstractCandidate
        implements Candidate
    {
        //~ Methods ------------------------------------------------------------

        public int compareTo (Candidate other)
        {
            return Integer.signum(getInputIndex() - other.getInputIndex());
        }
    }

    //----------------//
    // AbstractResult //
    //----------------//
    private abstract static class AbstractResult
        implements Result
    {
        //~ Instance fields ----------------------------------------------------

        protected final Candidate candidate;

        //~ Constructors -------------------------------------------------------

        public AbstractResult (Candidate candidate)
        {
            this.candidate = candidate;
        }

        //~ Methods ------------------------------------------------------------

        public Candidate getCandidate ()
        {
            return candidate;
        }

        public int compareTo (Result other)
        {
            return Integer.signum(getId() - other.getId());
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{");
            sb.append(getClass().getSimpleName());

            sb.append(" id:")
              .append(getId());

            sb.append(" name:\"")
              .append(getName())
              .append("\"");

            if (getAbbreviation() != null) {
                sb.append(" abbr:\"")
                  .append(getAbbreviation())
                  .append("\"");
            }

            sb.append(" staffCount:")
              .append(getStaffCount());

            sb.append("}");

            return sb.toString();
        }
    }

    //----------------------//
    // PMScorePartCandidate //
    //----------------------//
    /**
     * Wrapping class meant for a proxymusic ScorePart instance candidate
     */
    private static class PMScorePartCandidate
        extends AbstractCandidate
    {
        //~ Instance fields ----------------------------------------------------

        private final proxymusic.ScorePart scorePart;
        private final ScorePartwise        scorePartwise;
        private final int                  inputIndex;
        private Integer                    staffCount;

        //~ Constructors -------------------------------------------------------

        public PMScorePartCandidate (proxymusic.ScorePart scorePart,
                                     ScorePartwise        scorePartwise,
                                     int                  inputIndex)
        {
            this.scorePart = scorePart;
            this.scorePartwise = scorePartwise;
            this.inputIndex = inputIndex;
        }

        //~ Methods ------------------------------------------------------------

        public String getAbbreviation ()
        {
            PartName partName = scorePart.getPartAbbreviation();

            if (partName != null) {
                return partName.getValue();
            } else {
                return null;
            }
        }

        public int getInputIndex ()
        {
            return inputIndex;
        }

        public String getName ()
        {
            PartName partName = scorePart.getPartName();

            if (partName != null) {
                return partName.getValue();
            } else {
                return null;
            }
        }

        public int getStaffCount ()
        {
            if (staffCount == null) {
                // Determine the corresponding staff count
                // We have to dig into the first measure of the part itself
                staffCount = 1; // Default value

                String id = scorePart.getId();
                logger.fine("scorePart id:" + id);

                for (ScorePartwise.Part part : scorePartwise.getPart()) {
                    if (part.getId() != scorePart) {
                        continue;
                    }

                    // Get first measure of this part
                    ScorePartwise.Part.Measure firstMeasure = part.getMeasure()
                                                                  .get(0);

                    // Look for Attributes element
                    for (Object obj : firstMeasure.getNoteOrBackupOrForward()) {
                        if (!(obj instanceof proxymusic.Attributes)) {
                            continue;
                        }

                        proxymusic.Attributes attributes = (proxymusic.Attributes) obj;
                        BigInteger            staves = attributes.getStaves();

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

        public Object getUnderlyingObject ()
        {
            return scorePart;
        }

        public PMScorePartResult createResult ()
        {
            // Create a brand new score part for this candidate
            // Id is irrelevant for the time being

            /** Factory for proxymusic entities */
            proxymusic.ObjectFactory factory = new proxymusic.ObjectFactory();

            PMScorePartResult result = new PMScorePartResult(
                this,
                getStaffCount(),
                factory.createScorePart());

            result.setName(getName());
            result.setAbbreviation(getAbbreviation());

            if (logger.isFineEnabled()) {
                logger.fine("Created " + result + " from " + this);
            }

            return result;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{");

            sb.append(getClass().getSimpleName());

            sb.append(" page#")
              .append(inputIndex);

            sb.append(" \"")
              .append(getName())
              .append("\"");

            sb.append(" staffCount:")
              .append(getStaffCount());

            sb.append("}");

            return sb.toString();
        }
    }

    //-------------------//
    // PMScorePartResult //
    //-------------------//
    /**
     * Wrapping class meant for a proxymusic ScorePart instance result
     */
    private static class PMScorePartResult
        extends AbstractResult
    {
        //~ Instance fields ----------------------------------------------------

        private final int                  staffCount;
        private final proxymusic.ScorePart scorePart;
        private int                        id;

        //~ Constructors -------------------------------------------------------

        public PMScorePartResult (Candidate            candidate,
                                  int                  staffCount,
                                  proxymusic.ScorePart scorePart)
        {
            super(candidate);
            this.staffCount = staffCount;
            this.scorePart = scorePart;
        }

        //~ Methods ------------------------------------------------------------

        public void setAbbreviation (String abbreviation)
        {
            proxymusic.ObjectFactory factory = new proxymusic.ObjectFactory();
            PartName                 partName = factory.createPartName();
            scorePart.setPartAbbreviation(partName);
            partName.setValue(abbreviation);
        }

        public String getAbbreviation ()
        {
            PartName partName = scorePart.getPartAbbreviation();

            if (partName != null) {
                return partName.getValue();
            } else {
                return null;
            }
        }

        public void setId (int id)
        {
            this.id = id;
            scorePart.setId("P" + id);
        }

        public int getId ()
        {
            return id;
        }

        public void setName (String name)
        {
            proxymusic.ObjectFactory factory = new proxymusic.ObjectFactory();
            proxymusic.PartName      partName = factory.createPartName();
            scorePart.setPartName(partName);
            partName.setValue(name);
        }

        public String getName ()
        {
            PartName partName = scorePart.getPartName();

            if (partName != null) {
                return partName.getValue();
            } else {
                return null;
            }
        }

        public int getStaffCount ()
        {
            return staffCount;
        }

        public proxymusic.ScorePart getUnderlyingObject ()
        {
            return scorePart;
        }
    }

    //-----------------//
    // ScorePartResult //
    //-----------------//
    /**
     * Wrapping class meant for a ScorePart instance result
     */
    private static class ScorePartResult
        extends AbstractResult
    {
        //~ Instance fields ----------------------------------------------------

        private final ScorePart scorePart;

        //~ Constructors -------------------------------------------------------

        public ScorePartResult (Candidate candidate,
                                ScorePart scorePart)
        {
            super(candidate);
            this.scorePart = scorePart;
        }

        //~ Methods ------------------------------------------------------------

        public void setAbbreviation (String abbreviation)
        {
            scorePart.setAbbreviation(abbreviation);
        }

        public String getAbbreviation ()
        {
            return scorePart.getAbbreviation();
        }

        public void setId (int id)
        {
            scorePart.setId(id);
        }

        public int getId ()
        {
            return scorePart.getId();
        }

        public void setName (String name)
        {
            scorePart.setName(name);
        }

        public String getName ()
        {
            return scorePart.getName();
        }

        public int getStaffCount ()
        {
            return scorePart.getStaffCount();
        }

        public ScorePart getUnderlyingObject ()
        {
            return scorePart;
        }
    }

    //---------------------//
    // SystemPartCandidate //
    //---------------------//
    /**
     * Wrapping class meant for a SystemPart instance
     */
    private static class SystemPartCandidate
        extends AbstractCandidate
    {
        //~ Instance fields ----------------------------------------------------

        private final SystemPart systemPart;

        //~ Constructors -------------------------------------------------------

        public SystemPartCandidate (SystemPart part)
        {
            this.systemPart = part;
        }

        //~ Methods ------------------------------------------------------------

        public String getAbbreviation ()
        {
            return null;
        }

        public int getInputIndex ()
        {
            return systemPart.getSystem()
                             .getId();
        }

        public String getName ()
        {
            return systemPart.getName();
        }

        public int getStaffCount ()
        {
            return systemPart.getStaves()
                             .size();
        }

        public Object getUnderlyingObject ()
        {
            return systemPart;
        }

        public ScorePartResult createResult ()
        {
            // Create a brand new score part for this candidate
            // Id is irrelevant for the time being
            ScorePart       scorePart = new ScorePart(0, getStaffCount());
            ScorePartResult result = new ScorePartResult(this, scorePart);
            result.setName(getName());
            result.setAbbreviation(getAbbreviation());

            if (logger.isFineEnabled()) {
                logger.fine("Created " + result + " from " + this);
            }

            return result;
        }

        @Override
        public String toString ()
        {
            return systemPart.toString();
        }
    }
}
