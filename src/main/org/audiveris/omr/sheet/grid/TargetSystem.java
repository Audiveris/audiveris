//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T a r g e t S y s t e m                                     //
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
package org.audiveris.omr.sheet.grid;

import org.audiveris.omr.sheet.SystemInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code TargetSystem} is an immutable perfect destination object for a system.
 *
 * @author Hervé Bitteur
 */
public class TargetSystem
{

    /** Raw information */
    public final SystemInfo info;

    /** Id for debug */
    public final int id;

    /** Ordinate of top of first staff in containing page */
    public final double top;

    /** Left abscissa in containing page */
    public final double left;

    /** Right abscissa in containing page */
    public final double right;

    /** Sequence of staves */
    public final List<TargetStaff> staves = new ArrayList<>();

    /**
     * Creates a new TargetSystem object.
     *
     * @param info  the original raw information
     * @param top   ordinate of top
     * @param left  abscissa of left
     * @param right abscissa of right
     */
    public TargetSystem (SystemInfo info,
                         double top,
                         double left,
                         double right)
    {
        this.info = info;
        this.top = top;
        this.left = left;
        this.right = right;

        id = info.getId();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{System");
        sb.append("#").append(id);
        sb.append(" top:").append(top);
        sb.append(" left:").append(left);
        sb.append(" right:").append(right);
        sb.append("}");

        return sb.toString();
    }
}
