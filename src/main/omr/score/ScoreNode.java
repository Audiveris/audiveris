//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e N o d e                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.score.visitor.ScoreVisitor;
import omr.score.visitor.Visitable;

import omr.sheet.Scale;

import omr.util.Implement;
import omr.util.Logger;
import omr.util.TreeNode;

/**
 * Class <code>ScoreNode</code> handles a node in the tree hierarchy of a score
 * entity.
 *
 * <p>
 * The use of TreeNode for the Score hierarchy is the following :
 * <pre>
 *                               Score                                                      |
 *                                 O                                                        |
 *                                 |                                                        |
 *                                 |                                                        |
 *                               Page  (not yet implemented)                                |
 *                                 O                                                        |
 *                                 |                                                        |
 *                                 |                                                        |
 *                              System                                                      |
 *                                 O                                                        |
 *                                 |                                                        |
 *                                 |                                                        |
 *                            SystemPart                                                    |
 *                                 O                                                        |
 *                                 |                                                        |
 *          _________________________________________________                               |
 *         {startingBarline, StaffList, MeasureList, SlurList}                              |
 *                                O            O         O                                  |
 *                                |            |         |                                  |
 *                                |            |         |                                  |
 *                              Staff       Measure    Slur                                 |
 *                                             O                                            |
 *                                             |                                            |
 * _______________________________________________________________________________________  |
 *{ClefList, KeyList, Time, ChordList, BeamList, LyricList, TextList, DynamicList, Barline} |
 *     O        O       O        O         O          O         O            O              |
 *     |        |       |        |         |          |         |            |              |
 *     |        |       |        |         |          |         |            |              |
 *   Clef    KeySig. TimeSig.  Chord     Beam       Lyric     Text        Dynamic           |
 *                               O                    O                                     |
 *                               |                    |                                     |
 *                               |                    |                                     |
 *                             Note               Syllable                                  |
 *  </pre>
 * </p>
 *
 * <p> Since the various score entities are organized in a tree of ScoreNode
 * instances, we use a lot the ability to browse the complete hierarchy,
 * starting from the top (the score instance).</p>
 *
 * <p> This is implemented via score visitors, with the added functionality that
 * any specific visited node can stop the visit to nodes below it in the
 * hierarchy.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreNode
    extends TreeNode
    implements Visitable
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreNode.class);

    //~ Instance fields --------------------------------------------------------

    /** The containing score */
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
    public ScoreNode (ScoreNode container)
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

    //--------//
    // accept //
    //--------//
    @Implement(Visitable.class)
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------------//
    // acceptChildren //
    //----------------//
    /**
     * Pattern to traverse the children of this node
     *
     * @param visitor concrete visitor object to define the actual processing
     */
    public void acceptChildren (ScoreVisitor visitor)
    {
        ///logger.info(children.size() + " children for " + this);
        for (TreeNode node : children) {
            ScoreNode child = (ScoreNode) node;

            if (child.accept(visitor)) {
                child.acceptChildren(visitor);
            }
        }
    }
}
