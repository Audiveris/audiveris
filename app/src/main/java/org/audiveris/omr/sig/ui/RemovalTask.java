//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R e m o v a l T a s k                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Link;

import java.util.Collection;

/**
 * Class <code>RemovalTask</code> removes an inter (with its relations).
 *
 * @author Hervé Bitteur
 */
public class RemovalTask
        extends InterTask
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>RemovalTask</code> object, which saves the current inter links
     * for potential undo.
     *
     * @param inter the inter to remove
     */
    public RemovalTask (Inter inter)
    {
        this(inter, null);
    }

    /**
     * Creates a new <code>RemovalTask</code> object, with its current links.
     * <p>
     * Useful when inter is no longer in sig when this task is performed.
     *
     * @param inter the inter to remove
     * @param links the inter links to save, to be used if/when the removal is undone.
     *              If null, use all the current relations when the inter is about to be removed.
     */
    public RemovalTask (Inter inter,
                        Collection<Link> links)
    {
        super(inter.getSig(), inter, inter.getBounds(), links, "del");
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public void performDo ()
    {
        if (links == null) {
            links = inter.getLinks();
        }

        inter.remove(false);
    }

    @Override
    public void performUndo ()
    {
        inter.setBounds(initialBounds);
        sig.addVertex(inter);

        for (Link link : links) {
            link.applyTo(inter);
        }
    }
}
