//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             L i n k                                            //
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;

import java.util.Collections;
import java.util.List;

/**
 * Seen from an Inter instance, class {@code Link} describes a potential relation with
 * another Inter instance (the partner).
 * <p>
 * This is meant to deal with a potential relation between the inter instance and the partner,
 * before perhaps recording the relation as an edge within the SIG.
 *
 * @author Hervé Bitteur
 */
public class Link
        implements Comparable<Link>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The other Inter instance, the one to be linked with. */
    public Inter partner;

    /** The concrete relation. */
    public final Relation relation;

    /** True for Inter as source and Partner as target, false for the reverse. */
    public final boolean outgoing;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Link} object.
     *
     * @param partner  the partnering inter instance
     * @param relation the relation with this partner
     * @param outgoing true if partner is target, false if it is source
     */
    public Link (Inter partner,
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

    //--------//
    // bestOf //
    //--------//
    public static Link bestOf (List<Link> links)
    {
        if (links.size() > 1) {
            Collections.sort(links);
        }

        return links.isEmpty() ? null : links.get(0);
    }

    @Override
    public int compareTo (Link that)
    {
        AbstractSupport s1 = (AbstractSupport) this.relation;
        AbstractSupport s2 = (AbstractSupport) that.relation;

        return Double.compare(s1.getGrade(), s2.getGrade());
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Link{");
        sb.append(partner);
        sb.append(" ").append(relation);
        sb.append(" ").append(outgoing ? "OUTGOING" : "INCOMING");
        sb.append("}");

        return sb.toString();
    }
}
