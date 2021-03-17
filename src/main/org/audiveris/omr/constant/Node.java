//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            N o d e                                             //
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
package org.audiveris.omr.constant;

import java.util.Comparator;

/**
 * Abstract class {@code Node} represents a node in the hierarchy of packages and units
 * (aka classes).
 *
 * @author Hervé Bitteur
 */
public abstract class Node
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** For comparing Node instances according to their name */
    public static final Comparator<Node> nameComparator = new Comparator<Node>()
    {
        @Override
        public int compare (Node n1,
                            Node n2)
        {
            return n1.getName().compareTo(n2.getName());
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** (Fully qualified) name of the node */
    private final String name;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new Node.
     *
     * @param name the fully qualified node name
     */
    public Node (String name)
    {
        this.name = name;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getName //
    //---------//
    /**
     * Report the fully qualified name for this node.
     *
     * @return the fully qualified node name
     */
    public String getName ()
    {
        return name;
    }

    //---------------//
    // getSimpleName //
    //---------------//
    /**
     * Return the last path component (non-qualified).
     *
     * @return the non-qualified node name
     */
    public String getSimpleName ()
    {
        int dot = name.lastIndexOf('.');

        if (dot != -1) {
            return name.substring(dot + 1);
        } else {
            return name;
        }
    }

    //----------//
    // toString //
    //----------//
    /**
     * Since {@code toString()} is used by JTreeTable to display the
     * node name, this method returns the last path component of the
     * node, in other words the non-qualified name.
     *
     * @return the non-qualified node name
     */
    @Override
    public String toString ()
    {
        return getSimpleName();
    }
}
