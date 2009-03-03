//----------------------------------------------------------------------------//
//                                                                            //
//                           P a c k a g e N o d e                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.constant;

import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class <code>PackageNode</code> represents a package in the hierarchy of
 * nodes. It can have children, which can be sub-packages and units. For
 * example, the unit/class <b>omr.score.Page</b> will need PackageNode
 * <b>omr</b> and PackageNode <b>omr.score</b>.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class PackageNode
    extends Node
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The children, composed of either other <code>PackageNode</code> or
     * <code>ConstantSet</code>.
     */
    private final ConcurrentSkipListSet<Node> children = new ConcurrentSkipListSet<Node>(
        new Comparator<Node>() {
                public int compare (Node n1,
                                    Node n2)
                {
                    return n1.getName()
                             .compareTo(n2.getName());
                }
            });

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // PackageNode //
    //-------------//
    /**
     * Create a new PackageNode.
     *
     * @param name  the fully qualified package name
     * @param child the first child of the package (either a sub-package, or a
     *              ConstantSet).
     */
    public PackageNode (String name,
                        Node   child)
    {
        super(name);

        if (child != null) {
            addChild(child);
        }
    }

    //~ Methods ----------------------------------------------------------------

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

    //----------//
    // addChild //
    //----------//
    /**
     * Add a child to the package children
     *
     * @param obj the child to add (sub-package or ConstantSet)
     */
    public void addChild (Node obj)
    {
        children.add(obj);
    }
}
