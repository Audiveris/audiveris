//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S c o r e R e d u c t i o n                                   //
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

import org.audiveris.omr.score.PartConnection.Candidate;
import org.audiveris.omr.score.PartConnection.Result;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
 * <pre>
 * <code>
 *      Map&lt;Integer, String&gt; fragments = ...;
 *      ScoreReduction reduction = new ScoreReduction(fragments);
 *      String output = reduction.reduce();
 *      Map&lt;Integer, Status&gt; statuses = reduction.getStatuses();
 * </code>
 * </pre>
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
            pages.put(page.getSheet().getStub().getNumber(), page);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // reduce //
    //--------//
    /**
     * Process a score by merging information from the score pages.
     *
     * @return the count of modifications done
     */
    public int reduce ()
    {
        int modifs = 0;
        /* Connect parts across the pages */
        connection = PartConnection.connectScorePages(pages);

        // Force the ids of all LogicalPart's
        numberResults();

        // Create score part-list and connect to pages and systems parts
        modifs += addPartList();

        // Debug: List all candidates per result
        if (logger.isDebugEnabled()) {
            dumpResultMapping();
        }

        return modifs;
    }

    //-------------//
    // addPartList //
    //-------------//
    /**
     * Build the part-list as the sequence of Result/LogicalPart instances, and map each
     * of them to a Part.
     *
     * @return the count of modifications done
     */
    private int addPartList ()
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
                        page2syst.put(pagePart, cousins = new ArrayList<Part>());
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

        if (Objects.deepEquals(score.getLogicalParts(), partList)) {
            return 0;
        }

        score.setLogicalParts(partList);

        return 1;
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
