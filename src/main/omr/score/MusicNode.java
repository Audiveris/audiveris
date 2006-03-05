//-----------------------------------------------------------------------//
//                                                                       //
//                           M u s i c N o d e                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import omr.lag.Lag;
import omr.util.Logger;
import omr.util.TreeNode;
import omr.ui.Zoom;

import java.awt.*;

/**
 * Class <code>MusicNode</code> handles a node in the tree hierarchy of a
 * score entity.
 * <p/>
 * <p/>
 * The use of TreeNode for the Score hierarchy is the following :
 * <pre>
 *                               Score                                    |
 *                                 O                                      |
 *                                 |                                      |
 *                                 |                                      |
 *                              System                                    |
 *                                 O                                      |
 *                                 |                                      |
 *                        ___________________                             |
 *                       {StaveList, SlurList}                    Dummy   |
 *                             O         O                                |
 *                             |         |                                |
 *                             |         |                                |
 *                           Stave     Slur                               |
 *                             O                                          |
 *                             |                                          |
 *        _____________________________________________                   |
 *       {LyricList, TextList, DynamicList, MeasureList}          Dummy   |
 *             O         O            O            O                      |
 *             |         |            |            |                      |
 *             |         |            |            |                      |
 *           Lyric     Text        Dynamic      Measure                   |
 *             O                                   O                      |
 *             |                                   |                      |
 *             |                _______________________________           |
 *          Syllable           {ClefList, KeysigList, ChordList}  Dummy   |
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
 * <p/>
 * <p/>
 * Since the various score entities are organized in a tree of MusicNode
 * instances, we use a lot the ability to browse the complete hierarchy,
 * starting generally from the top (the score instance). </p>
 *
 * <p> This is launched by calling an <b>xxxChildren()</b> method on the
 * score instance. To benefit from this, one only has to provide a
 * overriding version of the <b>xxxNode()</b> method for the sub-classes
 * where some processing is needed, since the hierarchy traversal is done
 * automatically.
 *
 * <p/> Currently, traversals are implemented for: <ul>
 *
 * <li> Computation of cached parameters, by use of <b>computeNode()</b>
 * </li>
 *
 * <li> Painting of music entities in the dedicated score view, by use of
 * <b>paintNode()</b> </li>
 *
 * <li> Colorization (setting colors) of related sections, by use of
 * <b>colorizeNode()</b> </li>
 *
 * <li> Rendering of related sections (with preset colors) in the dedicated
 * sheet view, by use of <b>renderNode()</b> </li>
 *
 * </ul> </p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class MusicNode
    extends TreeNode
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(MusicNode.class);

    //~ Constructors ------------------------------------------------------

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

        if (logger.isDebugEnabled()) {
            logger.debug("new MusicNode container=" + container);
        }
    }

    //~ Methods -----------------------------------------------------------

    //------------------//
    // colorizeChildren //
    //------------------//
    /**
     * Just forward the colorizing instruction to the direct depending
     * children.
     *
     * @param lag       the lag to be colorized
     * @param viewIndex the provided lag view index
     * @param color     the color to be used
     */
    public void colorizeChildren (Lag lag,
                                  int viewIndex,
                                  Color color)
    {
        for (TreeNode node : children) {
            MusicNode child = (MusicNode) node;

            if (child.colorizeNode(lag, viewIndex, color)) {
                child.colorizeChildren(lag, viewIndex, color);
            }
        }
    }

    //-----------------//
    // computeChildren //
    //-----------------//
    /**
     * Pattern to launch computation recursively on all children of this
     * node
     */
    public void computeChildren ()
    {
        if (logger.isDebugEnabled()) {
            logger.debug("computeChildren of " + this);
        }

        for (TreeNode node : children) {
            MusicNode child = (MusicNode) node;
            if (child.computeNode()) {
                if (child.children.size() > 0 ) {
                    child.computeChildren();
                }
            }
        }
    }

    //---------------//
    // paintChildren //
    //---------------//
    /**
     * Just forwards the paint instruction to the direct depending
     * children.
     *
     * @param g the graphics context
     * @param comp the containing component
     */
    public void paintChildren (Graphics g,
                               Zoom z,
                               Component comp)
    {
        //if (logger.isDebugEnabled ()) logger.debug ("paintChildren of " + this);
        for (TreeNode node : children) {
            MusicNode child = (MusicNode) node;
            if (child.paintNode(g, z, comp)) {
                child.paintChildren(g, z, comp);
            }
        }
    }

    //----------------//
    // renderChildren //
    //----------------//
    /**
     * Just forwards the rendering instruction to the direct depending
     * children.
     *
     * @param g the graphics context
     * @param z the display zoom
     */
    public void renderChildren (final Graphics g,
                                Zoom z)
    {
        //if (logger.isDebugEnabled ()) logger.debug ("renderChildren of " + this);
        for (TreeNode node : children) {
            MusicNode child = (MusicNode) node;
            if (child.renderNode(g, z)) {
                child.renderChildren(g, z);
            }
        }
    }

    //--------------//
    // colorizeNode //
    //--------------//
    /**
     * Placeholder for colorizing the sections that compose the physical
     * info that corresponds to this MusicNode.
     *
     * @param lag       the lag to be colorized
     * @param viewIndex the provided lag view index
     *
     * @return true if processing must continue
     */
    protected boolean colorizeNode (Lag lag,
                                    int viewIndex,
                                    Color color)
    {
        return true;
    }

    //-------------//
    // computeNode //
    //-------------//
    /**
     * Placeholder for specific computation on this node
     *
     * @return true if processing must continue
     */
    protected boolean computeNode ()
    {
        if (logger.isDebugEnabled()) {
            logger.debug("computeNode of " + this);
        }

        return true; // Let computation continue down the tree
    }

    //-----------//
    // paintNode //
    //-----------//
    /**
     * Placeholder for painting the node at hand, and returning true is the
     * rendering has been made, so that (contained) children will be
     * painted only if their container has been painted, at least
     * partially.
     *
     * @param g the graphics context
     * @param comp the containing component
     *
     * @return true if wholy or partly painted
     */
    protected boolean paintNode (Graphics g,
                                 Zoom z,
                                 Component comp)
    {
        return true;
    }

    //------------//
    // renderNode //
    //------------//
    /**
     * Placeholder for rendering the node at hand, and returning true is
     * the rendering has been made, so that (contained) children will be
     * rendered only if their container has been rendered, at least
     * partially.
     *
     * @param g the graphics context
     * @param z the display zoom
     *
     * @return true if wholy or partly painted
     */
    protected boolean renderNode (Graphics g,
                                  Zoom z)
    {
        return true;
    }
}
