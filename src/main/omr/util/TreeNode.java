//----------------------------------------------------------------------------//
//                                                                            //
//                              T r e e N o d e                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>TreeNode</code> handles a node in a tree hierarchy, which is
 * typically the tree organization of a score entity.
 *
 * <p/> A TreeNode has : <ul>
 *
 * <li> A parent (which may be null) to which the TreeNode belongs
 *
 * <li> A list (which may be empty) of contained chidren, for which the TreeNode
 * is the parent.
 *
 * </ul>
 *
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class TreeNode
    implements java.io.Serializable
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TreeNode.class);

    //~ Instance fields --------------------------------------------------------

    /**
     * Children : the list of nodes just below in the tree
     */
    protected final List<TreeNode> children = new ArrayList<TreeNode>();

    /**
     * Container : the node just above in the tree
     */
    protected TreeNode parent;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // TreeNode //
    //----------//
    /**
     * Create a node in the tree, given its parent
     *
     *
     * @param parent the containing node, or null otherwise
     */
    public TreeNode (TreeNode parent)
    {
        if (logger.isFineEnabled()) {
            logger.fine("new TreeNode parent=" + parent);
        }

        if (parent != null) {
            parent.addChild(this);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // setChildren //
    //-------------//
    /**
     * Set a (new) list of children
     *
     * @param children the list of nodes to register as children of the node at
     *                 hand
     */
    public void setChildren (List<?extends TreeNode> children)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setChildren of " + this);
        }

        if (this.children != children) {
            this.children.clear();
            this.children.addAll(children);
        }
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
        if (logger.isFineEnabled()) {
            logger.fine("getChildren of " + this);
        }

        return children;
    }

    //-------------------//
    // setChildrenParent //
    //-------------------//
    /**
     * Register this node as the parent of all its children
     */
    public void setChildrenParent ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("setChildrenParent of " + this);
        }

        // Make all children point to this node as parent
        for (TreeNode node : children) {
            node.setParent(this);
            node.setChildrenParent(); // Recursively
        }
    }

    //----------------//
    // getNextSibling //
    //----------------//
    /**
     * Report the next node in the children of this node parent
     *
     *
     * @return the next sibling node, or null if none
     */
    public TreeNode getNextSibling ()
    {
        if (parent != null) {
            int index = parent.children.indexOf(this);

            if (index < (parent.children.size() - 1)) {
                return parent.children.get(index + 1);
            }
        }

        return null;
    }

    //-----------//
    // setParent //
    //-----------//
    /**
     * Modify the link to the parent of this node
     *
     * @param parent the (new) parent
     */
    public void setParent (TreeNode parent)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setParent parent=" + parent + " for " + this);
        }

        this.parent = parent;
    }

    //-----------//
    // getParent //
    //-----------//
    /**
     * Report the parent of this node
     *
     * @return the node just higher in the tree, or null if none
     */
    public TreeNode getParent ()
    {
        return parent;
    }

    //--------------------//
    // getPreviousSibling //
    //--------------------//
    /**
     * Report the previous node in the children of this node parent
     *
     *
     * @return the previous sibling node, or null if none
     */
    public TreeNode getPreviousSibling ()
    {
        if (parent != null) {
            int index = parent.children.indexOf(this);

            if (index > 0) {
                return parent.children.get(index - 1);
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
        if (logger.isFineEnabled()) {
            logger.fine("addChild node=" + node + " for " + this);
        }

        children.add(node);
        node.setParent(this);
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
