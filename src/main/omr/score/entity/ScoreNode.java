//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e N o d e                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.log.Logger;

import omr.score.Score;

import omr.sheet.Scale;

import omr.util.Navigable;
import omr.util.TreeNode;

/**
 * Class <code>ScoreNode</code> handles a node in the tree hierarchy of a score
 * entity.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreNode
    extends VisitableNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreNode.class);

    //~ Instance fields --------------------------------------------------------

    /** The containing score */
    @Navigable(false)
    private Score score;

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

        // Set the score link
        for (TreeNode c = this; c != null; c = c.getParent()) {
            if (c instanceof Score) {
                score = (Score) c;

                break;
            }
        }
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
    // getScale //
    //----------//
    /**
     * Report the global scale of this score (and sheet)
     *
     * @return the global scale
     */
    public Scale getScale ()
    {
        return getScore()
                   .getScale();
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
        return score;
    }
}
