//----------------------------------------------------------------------------//
//                                                                            //
//                         P a g e R e d u c t i o n                          //
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
import omr.score.entity.SystemPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class {@code PageReduction} reduces the parts of each system to a list of
 * parts defined at page level.
 *
 * @author Hervé Bitteur
 */
public class PageReduction
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(PageReduction.class);

    //~ Instance fields --------------------------------------------------------
    /** Related page */
    private final Page page;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new PageReduction object.
     *
     * @param page the page to process
     */
    public PageReduction (Page page)
    {
        this.page = page;
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // reduce //
    //--------//
    /**
     * Process a page by merging information from the page systems
     */
    public void reduce ()
    {
        if (page.getSystems().isEmpty()) {
            return;
        }

        /* Connect parts across the systems */
        PartConnection connection = PartConnection.connectPageSystems(page);

        // Build part list
        List<ScorePart> scoreParts = new ArrayList<>();

        for (Result result : connection.getResultMap().keySet()) {
            scoreParts.add((ScorePart) result.getUnderlyingObject());
        }

        page.setPartList(scoreParts);

        // Make the connections: (system) SystemPart -> (page) ScorePart
        Map<Candidate, Result> candidateMap = connection.getCandidateMap();
        logger.debug("Candidates:{}", candidateMap.size());

        for (Map.Entry<Candidate, Result> entry : candidateMap.entrySet()) {
            Candidate candidate = entry.getKey();
            SystemPart systemPart = (SystemPart) candidate.getUnderlyingObject();

            Result result = entry.getValue();
            ScorePart scorePart = (ScorePart) result.getUnderlyingObject();

            // Connect (system) part -> (page) part
            systemPart.setScorePart(scorePart);

            // Use same ID
            systemPart.setId(scorePart.getId());
        }
    }
}
