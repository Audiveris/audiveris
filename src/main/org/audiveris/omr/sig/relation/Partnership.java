//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P a r t n e r s h i p                                     //
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;

/**
 * Class {@code Partnership} encapsulates a partnering vertex with the related edge.
 *
 * @author Hervé Bitteur
 */
public class Partnership
{
    //~ Instance fields ----------------------------------------------------------------------------

    public final Inter partner;

    public final Relation relation;

    public final boolean outgoing;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Partnership} object.
     *
     * @param partner  the partnering inter instance
     * @param relation the relation with this partner
     * @param outgoing true if partner is target, false if it is source
     */
    public Partnership (Inter partner,
                        Relation relation,
                        boolean outgoing)
    {
        this.partner = partner;
        this.relation = relation;
        this.outgoing = outgoing;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Add the relation between the provided inter and the partner.
     *
     * @param inter the provided inter
     */
    public void applyTo (Inter inter)
    {
        final SIGraph sig = inter.getSig();
        final Inter source = outgoing ? inter : partner;
        final Inter target = outgoing ? partner : inter;

        sig.addEdge(source, target, relation);
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Partnership{");
        sb.append(partner);
        sb.append(" ").append(relation);
        sb.append(" ").append(outgoing ? "OUTGOING" : "INCOMING");
        sb.append("}");

        return sb.toString();
    }
}
