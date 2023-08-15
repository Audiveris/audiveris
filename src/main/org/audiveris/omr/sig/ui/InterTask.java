//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I n t e r T a s k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Link;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Class <code>InterTask</code> is the elementary task (focused on an Inter) that can be
 * done, undone and redone by the {@link InterController}.
 *
 * @author Hervé Bitteur
 */
public abstract class InterTask
        extends UITask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Task focus. */
    protected final Inter inter;

    /** Initial bounds of inter. */
    protected final Rectangle initialBounds;

    /** Relations inter is involved in. */
    protected Collection<Link> links;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>InterTask</code> object.
     *
     * @param sig           the underlying sig
     * @param inter         the inter task is focused upon
     * @param initialBounds the inter initial bounds
     * @param links         the relations around inter, perhaps null
     * @param actionName    name for action
     */
    protected InterTask (SIGraph sig,
                         Inter inter,
                         Rectangle initialBounds,
                         Collection<Link> links,
                         String actionName)
    {
        super(sig, actionName);
        this.inter = inter;
        this.initialBounds = (initialBounds != null) ? new Rectangle(initialBounds) : null;

        if (links != null) {
            this.links = new ArrayList<>(links);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Getter for involved inter.
     *
     * @return the inter involved
     */
    public Inter getInter ()
    {
        return inter;
    }

    public Collection<Link> getLinks ()
    {
        return links;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(actionName);
        sb.append(" ").append(inter);
        sb.append(" @").append(Integer.toHexString(inter.hashCode()));

        if (links != null) {
            for (Link link : links) {
                sb.append("\n       +  ").append(link);
            }
        }

        return sb.toString();
    }
}
