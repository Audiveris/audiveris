//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T a r g e t P a g e                                       //
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
package org.audiveris.omr.sheet.grid;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code TargetPage} is an immutable perfect destination object for a page.
 *
 * @author Hervé Bitteur
 */
public class TargetPage
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Page width */
    public final double width;

    /** Page height */
    public final double height;

    /** Sequence of systems */
    public final List<TargetSystem> systems = new ArrayList<TargetSystem>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TargetPage object.
     *
     * @param width  page width
     * @param height page height
     */
    public TargetPage (double width,
                       double height)
    {
        this.width = width;
        this.height = height;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Page");

        sb.append(" width:").append(width);
        sb.append(" height:").append(height);

        sb.append("}");

        return sb.toString();
    }
}
