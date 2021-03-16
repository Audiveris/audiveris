//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                J u n c t i o n S h i f t P o l i c y                           //
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
package org.audiveris.omr.lag;

import org.audiveris.omr.run.Run;

/**
 * Class {@code JunctionShiftPolicy} defines a junction policy based on the shift
 * between the candidate run and the last run of the section.
 *
 * @author Hervé Bitteur
 */
public class JunctionShiftPolicy
        implements JunctionPolicy
{

    /**
     * Maximum value acceptable for shift.
     */
    private final int maxShift;

    /**
     * Creates an instance of policy based on shift of runs.
     *
     * @param maxShift the maximum possible shift between two consecutive rows
     */
    public JunctionShiftPolicy (int maxShift)
    {
        this.maxShift = maxShift;
    }

    //---------------//
    // consistentRun //
    //---------------//
    /**
     * Check whether the Run is consistent with the provided Section, according to this
     * junction policy, based on run position and last section run position.
     *
     * @param run     the Run candidate
     * @param section the potentially hosting Section
     * @return true if consistent, false otherwise
     */
    @Override
    public boolean consistentRun (Run run,
                                  Section section)
    {
        // Check based on positions of the two runs
        Run last = section.getLastRun();

        return (Math.abs(run.getStart() - last.getStart()) <= maxShift) && (Math.abs(
                run.getStop() - last.getStop()) <= maxShift);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{JunctionShiftPolicy maxShift=" + maxShift + "}";
    }
}
