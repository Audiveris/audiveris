//-----------------------------------------------------------------------//
//                                                                       //
//                            T r e e N o d e                            //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>TreeNode</code> handles a node in a tree hierarchy, which is
 * typically the tree organization of a score entity.
 *
 * <p/> A TreeNode has : <ul>
 *
 * <li> A container (which may be null) to which the TreeNode belongs
 *
 * <li> A list (which may be empty) of contained chidren, for which the
 * TreeNode is the container.
 *
 * </ul>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class TreeNode
    implements java.io.Serializable
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(TreeNode.class);

    //~ Instance variables ------------------------------------------------

    /**
     * Container : the node just above in the tree
     */
    protected TreeNode container;

    /**
     * Children : the list of nodes just below in the tree
     */
    protected final List<TreeNode> children = new ArrayList<TreeNode>();

    //~ Constructors ------------------------------------------------------

    //----------//
    // TreeNode //
    //----------//

    /**
     * Create a node in the tree, given its container
     *
     * @param container the containing node, or null otherwise
     */
    public TreeNode (TreeNode container)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("new TreeNode container=" + container);
        }

        if (container != null) {
            container.addChild(this);
        }
    }

    //~ Methods -----------------------------------------------------------

    //-------------//
    // setChildren //
    //-------------//

    /**
     * Set a (new) list of children
     *
     * @param children the list of nodes to register as children of the
     *                 node at hand
     */
    public void setChildren (List<? extends TreeNode> children)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("setChildren of " + this);
        }

        this.children.clear();
        this.children.addAll(children);
    }

    //-------------//
    // getChildren //
    //-------------//

    /**
     * Report the list of (direct) children
     *
     * @return the children
     */
    public List<TreeNode> getChildren ()
    {
        if (logger.isDebugEnabled()) {
            logger.debug("getChildren of " + this);
        }

        return children;
    }

    //----------------------//
    // setChildrenContainer //
    //----------------------//

    /**
     * Register this node as the container of all its children
     */
    public void setChildrenContainer ()
    {
        if (logger.isDebugEnabled()) {
            logger.debug("setChildrenContainer of " + this);
        }

        // Make all children point to this node as container
        for (TreeNode node : children) {
            node.setContainer(this);
            node.setChildrenContainer(); // Recursively
        }
    }

    //--------------//
    // setContainer //
    //--------------//

    /**
     * Modify the link to the container of this node
     *
     * @param container the (new) container
     */
    public void setContainer (TreeNode container)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("setContainer container=" + container + " for "
                         + this);
        }

        this.container = container;
    }

    //--------------//
    // getContainer //
    //--------------//

    /**
     * Report the container of this node
     *
     * @return the node just higher in the tree, or null if none
     */
    public TreeNode getContainer ()
    {
        return container;
    }

    //----------------//
    // getNextSibling //
    //----------------//

    /**
     * Report the next node in the children of this node container
     *
     * @return the next sibling node, or null if none
     */
    public TreeNode getNextSibling ()
    {
        if (container != null) {
            int index = container.children.indexOf(this);

            if (index < (container.children.size() - 1)) {
                return container.children.get(index + 1);
            }
        }

        return null;
    }

    //--------------------//
    // getPreviousSibling //
    //--------------------//

    /**
     * Report the previous node in the children of this node container
     *
     * @return the previous sibling node, or null if none
     */
    public TreeNode getPreviousSibling ()
    {
        if (container != null) {
            int index = container.children.indexOf(this);

            if (index > 0) {
                return container.children.get(index - 1);
            }
        }

        return null;
    }

    //----------//
    // addChild //
    //----------//

    /**
     * Add a child in the list of node children
     *
     * @param node the child to include
     */
    public void addChild (TreeNode node)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("addChild node=" + node + " for " + this);
        }

        children.add(node);
        node.setContainer(this);
    }

    //--------------//
    // dumpChildren //
    //--------------//

    /**
     * Utility to dump recursively all the children of this node, with no
     * starting indentation
     */
    public void dumpChildren ()
    {
        dumpChildren(0);
    }

    //--------------//
    // dumpChildren //
    //--------------//

    /**
     * Utility to dump recursively all the children of this node, with the
     * starting indentation specified
     *
     * @param level the starting indentation level
     */
    public void dumpChildren (final int level)
    {
        for (TreeNode node : children) {
            if (node.dumpNode(level)) {
                node.dumpChildren(level + 1);
            }
        }
    }

    //----------//
    // dumpNode //
    //----------//

    /**
     * Utility to dump the current node, with no indentation
     *
     * @return true, so that processing continues
     */
    public boolean dumpNode ()
    {
        return dumpNode(0);
    }

    //----------//
    // dumpNode //
    //----------//

    /**
     * Utility to dump the current node, with the specified level of
     * indentation.
     *
     * @param level the desired indentation level
     *
     * @return true, so that processing continues
     */
    public boolean dumpNode (int level)
    {
        Dumper.dump(this, level);

        return true; // Let computation continue down the tree
    }
}
