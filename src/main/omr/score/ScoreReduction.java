//----------------------------------------------------------------------------//
//                                                                            //
//                        S c o r e R e d u c t i o n                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.score.PartConnection.Candidate;
import omr.score.PartConnection.Result;
import omr.score.entity.Page;
import omr.score.entity.ScorePart;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;

import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class {@code ScoreReduction} is the "reduce" part of a MapReduce
 * job for a given score, based on the merge of Audiveris Page
 * instances.
 * <ol>
 * <li>Any Map task processes a score page and produces the related
 * XML fragment as its output.</li>
 * <li>The Reduce task takes all the XML fragments as input and
 * consolidates them in a global Score output.</li></ol>
 *
 * <p>Typical calling of the feature is as follows:
 * <code>
 * <pre>
 * Map&lt;Integer, String&gt; fragments = ...;
 * ScoreReduction reduction = new ScoreReduction(fragments);
 * String output = reduction.reduce();
 * Map&lt;Integer, Status&gt; statuses = reduction.getStatuses();
 * </pre>
 * </code>
 * </p>
 *
 * <p><b>Features not yet implemented:</b> <ul>
 * <li>Connection of slurs between pages</li>
 * <li>In part-list, handling of part-group beside score-part</li>
 * </ul></p>
 *
 * @author Hervé Bitteur
 */
public class ScoreReduction
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(ScoreReduction.class);

    //~ Instance fields --------------------------------------------------------
    /** Related score. */
    private final Score score;

    /** Pages to process. */
    private final SortedMap<Integer, Page> pages = new TreeMap<>();

    /** Global connection of parts. */
    private PartConnection connection;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new ScoreReduction object.
     *
     * @param score the score to process
     */
    public ScoreReduction (Score score)
    {
        this.score = score;

        for (TreeNode pn : score.getPages()) {
            Page page = (Page) pn;
            pages.put(page.getIndex(), page);
        }
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // reduce //
    //--------//
    /**
     * Process a score by merging information from the score pages.
     */
    public void reduce ()
    {
        if (score.getPages().isEmpty()) {
            return;
        }

        /* Connect parts across the pages */
        connection = PartConnection.connectScorePages(pages);

        // Force the ids of all ScorePart's
        numberResults();

        // Create score part-list and connect to pages and systems parts
        addPartList();

        // Debug: List all candidates per result
        if (logger.isDebugEnabled()) {
            dumpResultMapping();
        }
    }

    //-------------//
    // addPartList //
    //-------------//
    /**
     * Build the part-list as the sequence of Result/ScorePart
     * instances, and map each of them to a Part.
     */
    private void addPartList ()
    {
        // Map (page) ScorePart -> (score) ScorePart data
        List<ScorePart> partList = new ArrayList<>();

        for (Result result : connection.getResultMap().keySet()) {
            ScorePart scorePart = (ScorePart) result.getUnderlyingObject();
            partList.add(scorePart);
        }

        // Need map: pagePart instance -> set of related systemPart instances
        // (Since we only have the reverse link)
        Map<ScorePart, List<SystemPart>> page2syst = new LinkedHashMap<>();

        for (TreeNode pn : score.getPages()) {
            Page page = (Page) pn;

            for (TreeNode sn : page.getSystems()) {
                ScoreSystem system = (ScoreSystem) sn;

                for (TreeNode n : system.getParts()) {
                    SystemPart systPart = (SystemPart) n;

                    ScorePart pagePart = systPart.getScorePart();
                    List<SystemPart> cousins = page2syst.get(pagePart);

                    if (cousins == null) {
                        cousins = new ArrayList<>();
                        page2syst.put(pagePart, cousins);
                    }

                    cousins.add(systPart);
                }
            }
        }

        // Align each candidate to its related result (System -> Page -> Score)
        for (Result result : connection.getResultMap().keySet()) {
            ScorePart scorePart = (ScorePart) result.getUnderlyingObject();
            int newId = scorePart.getId();

            for (Candidate candidate : connection.getResultMap().get(result)) {
                ScorePart pagePart = (ScorePart) candidate.getUnderlyingObject();
                // Update (page) part id
                pagePart.setId(newId);

                // Update all related (system) part id
                for (SystemPart systPart : page2syst.get(pagePart)) {
                    systPart.setId(newId);
                }
            }
        }

        score.setPartList(partList);
    }

    //-------------------//
    // dumpResultMapping //
    //-------------------//
    /**
     * Debug: List details of all candidates per result.
     */
    private void dumpResultMapping ()
    {
        for (Entry<Result, Set<Candidate>> entry : connection.getResultMap().
                entrySet()) {
            logger.debug("Result: {}", entry.getKey());

            for (Candidate candidate : entry.getValue()) {
                logger.debug("* candidate: {}", candidate);
            }
        }
    }

    //---------------//
    // numberResults //
    //---------------//
    /**
     * Force the id of each result (score-part) as 1, 2, ...
     */
    private void numberResults ()
    {
        int partIndex = 0;

        for (Result result : connection.getResultMap().keySet()) {
            ScorePart scorePart = (ScorePart) result.getUnderlyingObject();
            scorePart.setId(++partIndex);
        }
    }
}
