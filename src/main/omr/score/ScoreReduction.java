//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S c o r e R e d u c t i o n                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.score.PartConnection.Candidate;
import omr.score.PartConnection.Result;

import omr.sheet.Part;
import omr.sheet.SystemInfo;

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
 * Class {@code ScoreReduction} is the "reduce" part of a MapReduce job for a score,
 * based on the merge of Audiveris Page instances.
 * <ol>
 * <li>Any Map task processes a score page and produces the related XML fragment as its output.</li>
 * <li>The Reduce task takes all the XML fragments as input and consolidates them in a global Score
 * output.</li></ol>
 * <p>
 * Typical calling of the feature is as follows:
 * <code>
 * <pre>
 *      Map&lt;Integer, String&gt; fragments = ...;
 *      ScoreReduction reduction = new ScoreReduction(fragments);
 *      String output = reduction.reduce();
 *      Map&lt;Integer, Status&gt; statuses = reduction.getStatuses();
 * </pre>
 * </code>
 * </p>
 *
 * <b>Features not yet implemented:</b> <ul>
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

    /** Pages to process. */
    private final SortedMap<Integer, Page> pages = new TreeMap<Integer, Page>();

    /** Global connection of parts. */
    private PartConnection connection;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ScoreReduction object.
     *
     * @param score the score to process
     */
    public ScoreReduction (Score score)
    {
        this.score = score;

        for (Page page : score.getPages()) {
            pages.put(page.getSheet().getNumber(), page);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // reduce //
    //--------//
    /**
     * Process a score by merging information from the score pages.
     */
    public void reduce ()
    {
        /* Connect parts across the pages */
        connection = PartConnection.connectScorePages(pages);

        // Force the ids of all LogicalPart's
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
     * Build the part-list as the sequence of Result/LogicalPart instances, and map each
     * of them to a Part.
     */
    private void addPartList ()
    {
        // Map (page) LogicalPart -> (score) LogicalPart data
        List<LogicalPart> partList = new ArrayList<LogicalPart>();

        for (Result result : connection.getResultMap().keySet()) {
            LogicalPart logicalPart = (LogicalPart) result.getUnderlyingObject();
            partList.add(logicalPart);
        }

        // Need map: pagePart instance -> set of related systemPart instances
        // (Since we only have the reverse link)
        Map<LogicalPart, List<Part>> page2syst = new LinkedHashMap<LogicalPart, List<Part>>();

        for (Page page : score.getPages()) {
            for (SystemInfo system : page.getSystems()) {
                for (Part systPart : system.getParts()) {
                    LogicalPart pagePart = systPart.getLogicalPart();
                    List<Part> cousins = page2syst.get(pagePart);

                    if (cousins == null) {
                        cousins = new ArrayList<Part>();
                        page2syst.put(pagePart, cousins);
                    }

                    cousins.add(systPart);
                }
            }
        }

        // Align each candidate to its related result (System -> Page -> Score)
        for (Result result : connection.getResultMap().keySet()) {
            LogicalPart logicalPart = (LogicalPart) result.getUnderlyingObject();
            int newId = logicalPart.getId();

            for (Candidate candidate : connection.getResultMap().get(result)) {
                LogicalPart pagePart = (LogicalPart) candidate.getUnderlyingObject();
                // Update (page) part id
                pagePart.setId(newId);

                // Update all related (system) part id
                for (Part systPart : page2syst.get(pagePart)) {
                    systPart.setId(newId);
                }
            }
        }

        score.setLogicalParts(partList);
    }

    //-------------------//
    // dumpResultMapping //
    //-------------------//
    /**
     * Debug: List details of all candidates per result.
     */
    private void dumpResultMapping ()
    {
        for (Entry<Result, Set<Candidate>> entry : connection.getResultMap().entrySet()) {
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
     * Force the id of each result (logical-part) as 1, 2, ...
     */
    private void numberResults ()
    {
        int partIndex = 0;

        for (Result result : connection.getResultMap().keySet()) {
            LogicalPart logicalPart = (LogicalPart) result.getUnderlyingObject();
            logicalPart.setId(++partIndex);
        }
    }
}
