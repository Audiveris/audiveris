//----------------------------------------------------------------------------//
//                                                                            //
//                              T r e e N o d e                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.Main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code TreeNode} handles a node in a tree hierarchy,
 * which is typically the tree organization of a score entity.
 * <p/>
 * A TreeNode has : <ul> <li> A parent (which may be null) to which the TreeNode
 * belongs <li> A list (which may be empty) of contained chidren, for which the
 * TreeNode is the parent. </ul>
 *
 * @author Hervé Bitteur
 */
public abstract class TreeNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(TreeNode.class);

    //~ Instance fields --------------------------------------------------------
    /**
     * Children : the list of nodes just below in the tree
     */
    protected final List<TreeNode> children = new ArrayList<>();

    /**
     * Container : the node just above in the tree
     */
    @Navigable(false)
    protected TreeNode parent;

    //~ Constructors -----------------------------------------------------------
    //----------//
    // TreeNode //
    //----------//
    /**
     * Create a node in the tree, given its parent.
     *
     * @param parent the containing node, or null otherwise
     */
    public TreeNode (TreeNode parent)
    {
        logger.debug("new TreeNode parent={}", parent);

        if (parent != null) {
            parent.addChild(this);
        }
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // addChild //
    //----------//
    /**
     * Add a child in the list of node children.
     *
     * @param node the child to include
     */
    public synchronized void addChild (TreeNode node)
    {
        logger.debug("addChild {} for {}", node, this);

        children.add(node);
        node.setParent(this);
    }

    //--------------//
    // dumpChildren //
    //--------------//
    /**
     * Utility to dump recursively all the children of this node,
     * with no starting indentation.
     */
    public void dumpChildren ()
    {
        dumpChildren(0);
    }

    //--------------//
    // dumpChildren //
    //--------------//
    /**
     * Utility to dump recursively all the children of this node,
     * with the starting indentation specified.
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
     * Utility to dump the current node, with no indentation.
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
     * @return true, so that processing continues
     */
    public boolean dumpNode (int level)
    {
        Main.dumping.dump(this, level);

        return true; // Let computation continue down the tree
    }

    //---------------//
    // getChildIndex //
    //---------------//
    /**
     * Report the index (counted from 0) of this node within the
     * children sequence of its parent.
     *
     * @return the child index (or -1 if there is no parent)
     */
    public int getChildIndex ()
    {
        if (parent != null) {
            return parent.children.indexOf(this);
        } else {
            return -1;
        }
    }

    //-------------//
    // getChildren //
    //-------------//
    /**
     * Report the list of (direct) children.
     *
     * @return the children
     */
    public List<TreeNode> getChildren ()
    {
        logger.debug("getChildren of {}", this);

        return children;
    }

    //-------------//
    // getChildren //
    //-------------//
    /**
     * Report the list of (direct) children.
     *
     * @return the children
     */
    @SuppressWarnings("unchecked")
    public synchronized List<TreeNode> getChildrenCopy ()
    {
        logger.debug("getChildrenCopy of {}", this);

        return new ArrayList<TreeNode>(children);
    }

    //----------------//
    // getNextSibling //
    //----------------//
    /**
     * Report the next node in the children of this node parent.
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
    // getParent //
    //-----------//
    /**
     * Report the parent of this node.
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
     * Report the previous node in the children of this node parent.
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

    //-------------------//
    // setChildrenParent //
    //-------------------//
    /**
     * Register this node as the parent of all its children.
     */
    public void setChildrenParent ()
    {
        logger.debug("setChildrenParent of {}", this);

        // Make all children point to this node as parent
        for (TreeNode node : children) {
            node.setParent(this);
            node.setChildrenParent(); // Recursively
        }
    }

    //-----------//
    // setParent //
    //-----------//
    /**
     * Modify the link to the parent of this node.
     *
     * @param parent the (new) parent
     */
    public void setParent (TreeNode parent)
    {
        logger.debug("setParent parent={} for {}", parent, this);
        this.parent = parent;
    }
}
