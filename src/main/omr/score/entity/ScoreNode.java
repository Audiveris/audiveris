//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e N o d e                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.score.Score;

import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code ScoreNode} handles a node in the tree hierarchy of a score
 * entity.
 *
 * @author Hervé Bitteur
 */
public abstract class ScoreNode
        extends VisitableNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            ScoreNode.class);

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // ScoreNode //
    //-----------//
    /**
     * Create a node in the tree, given its container
     *
     * @param container the containing node, or null otherwise
     */
    public ScoreNode (VisitableNode container)
    {
        super(container);
    }

    //~ Methods ----------------------------------------------------------------
    //------------------//
    // getContextString //
    //------------------//
    /**
     * Report a string that describes the context (containment chain, score
     * excluded) of this entity.
     *
     * @return the properly filled context string
     */
    public String getContextString ()
    {
        return ""; // Empty by default
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Report the containing score
     *
     * @return the containing score
     */
    public Score getScore ()
    {
        for (TreeNode c = this; c != null; c = c.getParent()) {
            if (c instanceof Score) {
                return (Score) c;
            }
        }

        return null;
    }
}
