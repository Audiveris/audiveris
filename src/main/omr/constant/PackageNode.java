//----------------------------------------------------------------------------//
//                                                                            //
//                           P a c k a g e N o d e                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.constant;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class {@code PackageNode} represents a package in the hierarchy of
 * nodes. It can have children, which can be sub-packages and units. For
 * example, the unit/class <b>omr.score.Page</b> will need PackageNode
 * <b>omr</b> and PackageNode <b>omr.score</b>.
 *
 * @author Hervé Bitteur
 */
public class PackageNode
    extends Node
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The children, composed of either other {@code PackageNode} or
     * {@code ConstantSet}.
     */
    private final ConcurrentSkipListSet<Node> children = new ConcurrentSkipListSet<>(
        Node.nameComparator);

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
