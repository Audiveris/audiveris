//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         B e a m L i n e                                        //
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
package org.audiveris.omr.sheet.beam;

import org.audiveris.omr.util.Vip;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code BeamLine} represents a sequence of aligned BeamItem instances.
 * There may be one or several BeamLine instances in a BeamStructure, one for each index.
 *
 * @author Hervé Bitteur
 */
public class BeamLine
        implements Vip
{

    /** Items that compose the line, from left to right. */
    private final List<BeamItem> items = new ArrayList<>();

    /** VIP flag. */
    private boolean vip;

    /** The median line from left item to right item. */
    final Line2D median;

    /** The constant height of the line. */
    final double height;

    /**
     * Creates a new BeamLine object.
     *
     * @param median the global line
     * @param height constant height for the line
     */
    public BeamLine (Line2D median,
                     double height)
    {
        this.median = median;
        this.height = height;
    }

    /**
     * @return the items
     */
    public List<BeamItem> getItems ()
    {
        return items;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip (boolean vip)
    {
        this.vip = vip;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        for (BeamItem item : items) {
            sb.append(" ").append(item);
        }

        return sb.toString();
    }
}
