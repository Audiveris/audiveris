//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S y s t e m B a c k u p                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SigBackup;

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

    /** Dedicated system. */
    private final SystemInfo system;

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
