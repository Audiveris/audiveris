//----------------------------------------------------------------------------//
//                                                                            //
//                         P a r t C o n n e c t o r                          //
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

import java.util.*;
import java.util.Map.Entry;

/**
 * Class {@code PartConnector} is in charge of finding the connections of parts
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
public class PartConnector
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(PartConnector.class);

    //~ Instance fields --------------------------------------------------------

    /** Input data */
    private final Set<List<Candidate>> sequences;

    /** Record which candidates are mapped to which ScorePart. Useful??? TODO */
    private final Map<ScorePart, Set<Candidate>> partMap = new HashMap<ScorePart, Set<Candidate>>();

    /** Record which ScorePart is mapped to which candidate */
    private final Map<Candidate, ScorePart> candidateMap = new HashMap<Candidate, ScorePart>();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // PartConnector //
    //---------------//
    /**
     * Creates a new PartConnector object.
     * @param sequences a set of sequences of parts
     */
    public PartConnector (Set<List<Candidate>> sequences)
    {
        this.sequences = sequences;
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // connect //
    //---------//
    public List<ScorePart> connect ()
    {
        List<ScorePart> scoreParts = new ArrayList<ScorePart>();

        // Process each sequence of parts in turn
        // (typically a sequence of parts is a system)
        for (List<Candidate> sequence : sequences) {
            // Current index in scoreParts sequence (built in reverse order)
            int scorePartIndex = -1;

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
                        candidate.getStaffCOunt());
                }

                // Check with scoreParts currently defined
                scorePartIndex++;

                if (logger.isFineEnabled()) {
                    logger.fine("scorePartIndex:" + scorePartIndex);
                }

                if (scorePartIndex >= scoreParts.size()) {
                    if (logger.isFineEnabled()) {
                        logger.fine("No more scoreParts available");
                    }

                    // Create a brand new score part for this candidate
                    ScorePart part = createScorePart(candidate);
                    scoreParts.add(scorePartIndex, part);
                } else {
                    ScorePart scorePart = scoreParts.get(scorePartIndex);

                    if (logger.isFineEnabled()) {
                        logger.fine("Part:" + scorePart);
                    }

                    // Check we are connectable in terms of staves
                    if (scorePart.getStaffCount() != candidate.getStaffCOunt()) {
                        if (logger.isFineEnabled()) {
                            logger.fine("Count incompatibility");
                        }

                        // Create a brand new score part for this candidate
                        scorePart = createScorePart(candidate);
                        scoreParts.add(scorePartIndex, scorePart);
                    } else {
                        // Can we use names? Just for fun for the time being
                        if ((candidate.getName() != null) &&
                            (scorePart.getName() != null)) {
                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    "Names OK: " +
                                    candidate.getName().equalsIgnoreCase(
                                        scorePart.getName()));
                            }
                        }

                        // We are compatible
                        if (logger.isFineEnabled()) {
                            logger.fine("Compatible");
                        }

                        partMap.get(scorePart)
                               .add(candidate);
                        candidateMap.put(candidate, scorePart);
                    }
                }
            }
        }

        // Reverse and number ScorePart instances
        Collections.reverse(scoreParts);

        for (int i = 0; i < scoreParts.size(); i++) {
            ScorePart scorePart = scoreParts.get(i);
            int       id = i + 1;
            scorePart.setId(id);

            // Forge a ScorePart name if none has been assigned
            if (scorePart.getName() == null) {
                scorePart.setName("Part_" + id);
            }

            if (logger.isFineEnabled()) {
                logger.fine("Final " + scorePart);
            }
        }

        return scoreParts;
    }

    //----------------//
    // connectSystems //
    //----------------//
    /**
     * Convenient method to connect parts across systems of a score (1 page)
     * @param score the containing score
     */
    public static void connectSystems (Score score)
    {
        // Build candidates
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

        PartConnector   connector = new PartConnector(sequences);
        List<ScorePart> scoreParts = connector.connect();
        score.setPartList(scoreParts);

        // Make the connections
        Map<Candidate, ScorePart> candidateMap = connector.getCandidateMap();

        if (logger.isFineEnabled()) {
            logger.fine("Candidates:" + candidateMap.size());
        }

        for (Entry<Candidate, ScorePart> entry : candidateMap.entrySet()) {
            SystemPartCandidate candidate = (SystemPartCandidate) entry.getKey();
            SystemPart          systemPart = candidate.systemPart;
            ScorePart           scorePart = entry.getValue();
            systemPart.setScorePart(scorePart);
            systemPart.setId(scorePart.getId());
        }
    }

    //-----------------//
    // getCandidateMap //
    //-----------------//
    /**
     * @return the candidateMap
     */
    public Map<Candidate, ScorePart> getCandidateMap ()
    {
        return candidateMap;
    }

    //-----------------//
    // createScorePart //
    //-----------------//
    private ScorePart createScorePart (Candidate candidate)
    {
        // Create a brand new score part for this candidate
        ScorePart scorePart = new ScorePart(
            0, // Id is irrelevant for the time being
            candidate.getStaffCOunt());
        scorePart.setName(candidate.getName());
        scorePart.setAbbreviation(candidate.getAbbreviation());

        // Link ScorePart -> candidates
        Set<Candidate> candidates = new HashSet<Candidate>();
        candidates.add(candidate);
        partMap.put(scorePart, candidates);

        // Link candidate -> ScorePart
        candidateMap.put(candidate, scorePart);

        if (logger.isFineEnabled()) {
            logger.fine("Created " + scorePart + " from " + candidate);
        }

        return scorePart;
    }

    //~ Inner Interfaces -------------------------------------------------------

    //-----------//
    // Candidate //
    //-----------//
    /**
     * Interface {@code Candidate} is used to process system part candidates,
     * regardless whether they are provided as {@link omr.score.entity.SystemPart}
     * instances (produced by the scanning of just one page) or as ProxyMusic
     * {@link proxymusic.ScorePart} instances (used when merging MusicXML files).
     */
    public static interface Candidate
    {
        //~ Methods ------------------------------------------------------------

        /** Report the abbreviation, if any, that relates to this part */
        public String getAbbreviation ();

        /** Report the name of the part, if any */
        public String getName ();

        /** Report the number of staves in the part */
        public int getStaffCOunt ();
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------------------//
    // SystemPartCandidate //
    //---------------------//
    /**
     * Wrapping class meant for a SystemPart instance
     */
    private static class SystemPartCandidate
        implements Candidate
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

        public String getName ()
        {
            return systemPart.getName();
        }

        public int getStaffCOunt ()
        {
            return systemPart.getStaves()
                             .size();
        }

        @Override
        public String toString ()
        {
            return systemPart.toString();
        }
    }
}
