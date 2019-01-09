//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  J u n c t i o n P o l i c y                                   //
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
package org.audiveris.omr.lag;

import org.audiveris.omr.run.Run;

/**
 * Interface {@code JunctionPolicy} encapsulates the policy that decides if a run can
 * extend a given section.
 * <p>
 * If not, the run is part of a new section, linked to the previous one by a junction.
 *
 * @author Hervé Bitteur
 */
public interface JunctionPolicy
{

    //---------------//
    // consistentRun //
    //---------------//
    /**
     * Check if provided run is consistent with the section defined so far.
     *
     * @param run     the candidate run for section extension
     * @param section the to-be extended section
     * @return true is extension is compatible with the defined junction policy
     */
    public abstract boolean consistentRun (Run run,
                                           Section section);
}
