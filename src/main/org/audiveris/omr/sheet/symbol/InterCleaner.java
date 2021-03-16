//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     I n t e r C l e a n e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sheet.symbol;

import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.SystemInfo;

/**
 * Class {@code InterCleaner} is a workaround to purge containers of Inter instances
 * deleted from SIG but still referenced from some containers.
 * <p>
 * This accounts for:
 * <ul>
 * <li>Part -> lyrics</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
class InterCleaner
{

    private final SystemInfo system;

    /**
     * Creates a new {@code InterCleaner} object.
     *
     * @param system the system to be processed
     */
    InterCleaner (SystemInfo system)
    {
        this.system = system;
    }

    public void purgeContainers ()
    {
        // Parts
        for (Part part : system.getParts()) {
            part.purgeContainers();
        }
    }
}
