//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S y s t e m B a c k u p                                    //
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
import omr.sig.SigBackup;

/**
 * Class {@code SystemBackup} manages the saving of selected inters in a system.
 * <p>
 * It gathers the rhythm data that has been discarded by symbol reduction, but may be reconsidered
 * for rhythm tunings.
 *
 * @author Hervé Bitteur
 */
public class SystemBackup
        extends SigBackup
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Dedicated system. */
    private final SystemInfo system;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SystemBackup} object.
     *
     * @param system the dedicated system
     */
    public SystemBackup (SystemInfo system)
    {
        super(system.getSig());
        this.system = system;
    }
}
