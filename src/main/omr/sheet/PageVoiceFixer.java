//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   P a g e V o i c e F i x e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.score.entity.Page;
import omr.score.entity.ScorePart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * Class {@code PageVoiceFixer} harmonizes the IDs (and thus colors) for the voices of
 * all systems in a page.
 *
 * @author Hervé Bitteur
 */
public class PageVoiceFixer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PageVoiceFixer.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Dedicated page. */
    private final Page page;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PageVoiceFixer} object.
     *
     * @param page the dedicated page
     */
    public PageVoiceFixer (Page page)
    {
        this.page = page;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // refine //
    //--------//
    /**
     * Process the page, paying attention to dummy parts that may exists.
     */
    public void refine ()
    {
        // Number of voice IDs for each ScorePart within this page
        final Map<ScorePart, Integer> globalMap = new LinkedHashMap<ScorePart, Integer>();

        for (SystemInfo system : page.getSystems()) {
            for (Part part : system.getAllParts()) {
                // Voice IDs that start within this part
                final SortedSet<Integer> incomings = part.getVoiceIds();

                // Number of Global IDs already assigned in this part
                Integer globals = globalMap.get(part.getScorePart());

                if ((globals == null) || (globals < incomings.size())) {
                    globalMap.put(part.getScorePart(), incomings.size());
                }
            }
        }

        int voiceOffset = 0; // Offset for voice ids in current part

        for (ScorePart scorePart : page.getPartList()) {
            logger.info("{} voices:{}", scorePart, globalMap.get(scorePart));

            for (SystemInfo system : page.getSystems()) {
                for (MeasureStack stack : system.getMeasureStacks()) {
                    Collections.sort(stack.getVoices(), Voice.byId);
                }

                Part part = system.getPartByModel(scorePart);
                final List<Integer> partVoices = new ArrayList<Integer>(part.getVoiceIds());

                for (int i = 0; i < partVoices.size(); i++) {
                    final int id = partVoices.get(i);
                    int newId = voiceOffset + i + 1;

                    if (newId != id) {
                        part.swapVoiceId(i, newId);
                    }
                }
            }

            voiceOffset += globalMap.get(scorePart);
        }
    }
}
