//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R e m o v a l T a s k                                     //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Link;

/**
 * Class {@code RemovalTask} removes an inter (with its relations).
 *
 * @author Hervé Bitteur
 */
public class RemovalTask
        extends InterTask
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code RemovalTask} object.
     *
     * @param inter the inter to remove
     */
    public RemovalTask (Inter inter)
    {
        super(inter.getSig(), inter, inter.getBounds(), null);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void performDo ()
    {
        links = linksOf(inter);
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

    @Override
    protected String actionName ()
    {
        return "del";
    }
}
