//----------------------------------------------------------------------------//
//                                                                            //
//                             M u s i c N o d e                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.lag.Lag;

import omr.score.visitor.Visitable;
import omr.score.visitor.Visitor;

import omr.ui.view.Zoom;

import omr.util.Implement;
import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.*;
import java.util.List;

/**
 * Class <code>MusicNode</code> handles a node in the tree hierarchy of a score
 * entity.
 *
 * <p>
 * The use of TreeNode for the Score hierarchy is the following :
 * <pre>
 *                               Score                                    |
 *                                 O                                      |
 *                                 |                                      |
 *                                 |                                      |
 *                               Page                                     |
 *                                 O                                      |
 *                                 |                                      |
 *                                 |                                      |
 *                              System                                    |
 *                                 O                                      |
 *                                 |                                      |
 *                        ___________________                             |
 *                       {StaffList, SlurList}                    Dummy   |
 *                             O         O                                |
 *                             |         |                                |
 *                             |         |                                |
 *                           Staff     Slur                               |
 *                             O                                          |
 *                             |                                          |
 *        _____________________________________________                   |
 *       {LyricList, TextList, DynamicList, MeasureList}          Dummy   |
 *          O            O            O            O                      |
 *          |            |            |            |                      |
 *          |            |            |            |                      |
 *        Lyric        Text        Dynamic      Measure                   |
 *          O                                      O                      |
 *          |                                      |                      |
 *          |                   _______________________________           |
 *       Syllable              {ClefList, KeysigList, ChordList}  Dummy   |
 *                                  O           O          O              |
 *                                  |           |          |              |
 *                                  |           |          |              |
 *                                Clef       Keysig      Chord            |
 *                                                         O              |
 *                                                         |              |
 *                                                         |              |
 *                                                       Note             |
 *  </pre>
 * </p>
 *
 * <p> Since the various score entities are organized in a tree of MusicNode
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
public class MusicNode
    extends TreeNode
    implements Visitable
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MusicNode.class);

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // MusicNode //
    //-----------//
    /**
     * Create a node in the tree, given its container
     *
     * @param container the containing node, or null otherwise
     */
    public MusicNode (MusicNode container)
    {
        super(container);

        if (logger.isFineEnabled()) {
            logger.fine("new MusicNode container=" + container);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Implement(Visitable.class)
    public boolean accept (Visitor visitor)
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
    public void acceptChildren (Visitor visitor)
    {
        ///logger.info(children.size() + " children for " + this);
        for (TreeNode node : children) {
            MusicNode child = (MusicNode) node;

            if (child.accept(visitor)) {
                child.acceptChildren(visitor);
            }
        }
    }
}
