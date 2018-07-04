//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     P a c k a g e N o d e                                      //
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
package org.audiveris.omr.constant;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class {@code PackageNode} represents a package in the hierarchy of nodes.
 * <p>
 * It can have children, which can be sub-packages and units. For example, the unit/class
 * <b>omr.score.Page</b> will need PackageNode <b>omr</b> and PackageNode <b>omr.score</b>.
 *
 * @author Hervé Bitteur
 */
public class PackageNode
        extends Node
{
    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * The children, composed of either other {@code PackageNode} or
     * {@code ConstantSet}.
     */
    private final ConcurrentSkipListSet<Node> children = new ConcurrentSkipListSet<Node>(
            Node.nameComparator);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new PackageNode.
     *
     * @param name  the fully qualified package name
     * @param child the first child of the package (either a sub-package, or a
     *              ConstantSet).
     */
    public PackageNode (String name,
                        Node child)
    {
        super(name);

        if (child != null) {
            addChild(child);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addChild //
    //----------//
    /**
     * Add a child to the package children
     *
     * @param obj the child to add (sub-package or ConstantSet)
     */
    public final void addChild (Node obj)
    {
        children.add(obj);
    }

    //----------//
    // getChild //
    //----------//
    /**
     * Return the child at given index
     *
     * @param index the position in the ordered children list
     *
     * @return the desired child
     */
    public Object getChild (int index)
    {
        return children.toArray()[index];
    }

    //---------------//
    // getChildCount //
    //---------------//
    /**
     * Return the number of children currently in this package node
     *
     * @return the count of children
     */
    public int getChildCount ()
    {
        return children.size();
    }
}
