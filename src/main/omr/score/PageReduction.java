//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   P a g e R e d u c t i o n                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.score.PartConnection.Candidate;
import omr.score.PartConnection.Result;

import omr.sheet.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class {@code PageReduction} reduces the parts of each system to a list of parts
 * defined at page level.
 *
 * @author Hervé Bitteur
 */
public class PageReduction
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PageReduction.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related page. */
    private final Page page;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new PageReduction object.
     *
     * @param page the page to process
     */
    public PageReduction (Page page)
    {
        this.page = page;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // reduce //
    //--------//
    /**
     * Process a page by merging information from the page systems.
     */
    public void reduce ()
    {
        // Connect parts across the page systems
        PartConnection connection = PartConnection.connectPageSystems(page);

        // Build logical part list and store it in page
        List<LogicalPart> logicalParts = new ArrayList<LogicalPart>();

        for (Result result : connection.getResultMap().keySet()) {
            logicalParts.add((LogicalPart) result.getUnderlyingObject());
        }

        page.setLogicalParts(logicalParts);

        // Make the connections: (system) Part -> (page) LogicalPart
        Map<Candidate, Result> candidateMap = connection.getCandidateMap();
        logger.debug("Candidates:{}", candidateMap.size());

        for (Map.Entry<Candidate, Result> entry : candidateMap.entrySet()) {
            Candidate candidate = entry.getKey();
            Part systemPart = (Part) candidate.getUnderlyingObject();

            Result result = entry.getValue();
            LogicalPart logicalPart = (LogicalPart) result.getUnderlyingObject();

            // Connect (system) part -> (page) LogicalPart
            systemPart.setLogicalPart(logicalPart);

            // Use same ID
            systemPart.setId(logicalPart.getId());
        }
    }
}
