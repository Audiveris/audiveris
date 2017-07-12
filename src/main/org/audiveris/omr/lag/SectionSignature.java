//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S e c t i o n S i g n a t u r e                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import java.awt.Rectangle;

/**
 * Class {@code SectionSignature} defines a signature for a section
 *
 * @author Hervé Bitteur
 */
public class SectionSignature
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Section weight */
    private final int weight;

    /** Section bounds */
    private final Rectangle bounds;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SectionSignature object.
     *
     * @param weight the section weight
     * @param bounds the section bounds
     */
    public SectionSignature (int weight,
                             Rectangle bounds)
    {
        this.weight = weight;
        this.bounds = bounds;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof SectionSignature) {
            SectionSignature that = (SectionSignature) obj;

            return (weight == that.weight) && (bounds.x == that.bounds.x)
                   && (bounds.y == that.bounds.y) && (bounds.width == that.bounds.width)
                   && (bounds.height == that.bounds.height);
        } else {
            return false;
        }
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (41 * hash) + this.weight;

        return hash;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{SSig");
        sb.append(" weight=").append(weight);

        if (bounds != null) {
            sb.append(" Rectangle[x=").append(bounds.x).append(",y=").append(bounds.y)
                    .append(",width=").append(bounds.width).append(",height=").append(bounds.height)
                    .append("]");
        }

        sb.append("}");

        return sb.toString();
    }
}
