//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               J u n c t i o n A l l P o l i c y                                //
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
 * Class {@code JunctionAllPolicy} defines a junction policy which imposes no condition
 * on run consistency, thus taking all runs considered.
 *
 * @author Hervé Bitteur
 */
public class JunctionAllPolicy
        implements JunctionPolicy
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** singleton. */
    public static final JunctionAllPolicy INSTANCE = new JunctionAllPolicy();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates an instance of this policy.
     */
    public JunctionAllPolicy ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // consistentRun //
    //---------------//
    /**
     * Check whether the Run is consistent with the provided Section,
     * according to this junction policy.
     *
     * @param run     the Run candidate
     * @param section the potentially hosting Section
     * @return always true
     */
    @Override
    public boolean consistentRun (Run run,
                                  Section section)
    {
        return true;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{JunctionAllPolicy}";
    }
}
