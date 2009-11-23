//----------------------------------------------------------------------------//
//                                                                            //
//                         V i s i t a b l e N o d e                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.log.Logger;

import omr.score.visitor.ScoreVisitor;
import omr.score.visitor.Visitable;

import omr.util.Implement;
import omr.util.TreeNode;

/**
 * Class <code>VisitableNode</code> is a node which can accept a score visitor
 * for itself and for its children.
 *
 * <img src="doc-files/Visitable-Hierarchy.jpg" />
 *
 * @see ScoreVisitor
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class VisitableNode
    extends TreeNode
    implements Visitable
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(VisitableNode.class);

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VisitableNode object.
     *
     * @param container the parent in the hierarchy
     */
    public VisitableNode (VisitableNode container)
    {
        super(container);
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    /**
     * This class is visitable by definition, and calls the proper polymorphic
     * visit() method in the provided visitor. The returned boolean is used to
     * tell whether the visit shall continue to the children of this class
     * @param visitor the specific visitor which browses this class
     * @return false if children should not be (automatically) visited,
     * true otherwise which should be the default
     */
    @Implement(Visitable.class)
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------------//
    // acceptChildren //
    //----------------//
    /**
     * Pattern to traverse the children of this node, and recursively the
     * grand-children, etc...
     *
     * @param visitor concrete visitor object to define the actual processing
     */
    public void acceptChildren (ScoreVisitor visitor)
    {
        ///logger.info(children.size() + " children for " + this + " parent=" + parent);
        for (TreeNode node : getChildrenCopy()) {
            VisitableNode child = (VisitableNode) node;

            if (child.accept(visitor)) {
                child.acceptChildren(visitor);
            }
        }
    }
}
