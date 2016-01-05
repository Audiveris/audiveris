//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S y s t e m V o i c e F i x e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import omr.sheet.Part;

/**
 * Class {@code SystemVoiceFixer} harmonizes the ids (and thus colors) for the voices
 * of all stacks in a system.
 *
 * @author Hervé Bitteur
 */
public class SystemVoiceFixer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SystemVoiceFixer.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Dedicated system. */
    private final SystemInfo system;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SystemVoiceFixer} object.
     *
     * @param system dedicated system
     */
    public SystemVoiceFixer (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // refine //
    //--------//
    /**
     * Connect voices (reusing the same ID) within the same staff across system measures.
     * <p>
     * Unless a voice goes from one staff to the other in the same stack, make sure that the same
     * voice id appears only in the same staff.
     * Retrieve for each staff of the system the sequence of voices IDs used.
     */
    public void refine ()
    {
        int count = 0; // Count of voice ids in this system

        // Assigned voice IDs for each part within this system
        final Map<Part, List<Integer>> globalMap = new LinkedHashMap<Part, List<Integer>>();

        for (MeasureStack stack : system.getMeasureStacks()) {
            for (Part part : system.getParts()) {
                // Global IDs already assigned in this part
                List<Integer> globals = globalMap.get(part);

                if (globals == null) {
                    globalMap.put(part, globals = new ArrayList<Integer>());
                }

                // Voices that start within this measure
                final Measure measure = stack.getMeasureAt(part);
                final List<Voice> incomings = measure.getVoices();

                // Connect incoming voices to global ones
                // This simplistic approach uses position in lists
                // TODO: Voice connection should respect ties across measure barline
                for (int i = 0; i < incomings.size(); i++) {
                    final Voice voice = incomings.get(i);

                    if (i < globals.size()) {
                        final int global = globals.get(i);

                        if (voice.getId() != global) {
                            measure.swapVoiceId(voice, global);
                        }
                    } else {
                        // Extend globals list
                        final int newId = ++count;
                        globals.add(newId);
                        measure.swapVoiceId(voice, newId);
                    }
                }
            }
        }

        logger.debug("idMap: {}", globalMap);
    }
}
