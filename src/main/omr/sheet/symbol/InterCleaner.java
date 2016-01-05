//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     I n t e r C l e a n e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.symbol;

import omr.sheet.Part;
import omr.sheet.SystemInfo;

/**
 * Class {@code InterCleaner} is a workaround to purge containers of Inter instances
 * deleted from SIG but still referenced from some containers.
 * <p>
 * This accounts for:<ul>
 * <li>Part -> slurs</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
class InterCleaner
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final SystemInfo system;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code InterCleaner} object.
     *
     * @param system the system to be processed
     */
    public InterCleaner (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void purgeContainers ()
    {
        // Parts
        for (Part part : system.getParts()) {
            part.purgeContainers();
        }

        // Standalone glyphs
        system.clearStandaloneGlyphs();
    }
}
