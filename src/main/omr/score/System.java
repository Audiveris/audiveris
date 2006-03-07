//-----------------------------------------------------------------------//
//                                                                       //
//                              S y s t e m                              //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import omr.sheet.SystemInfo;
import omr.ui.view.Zoom;
import omr.util.Dumper;
import omr.util.Logger;
import omr.util.TreeNode;

import static omr.score.ScoreConstants.*;

import java.awt.*;
import java.util.List;

/**
 * Class <code>System</code> encapsulates a system in a score.
 * <p>A system contains two direct children : staves and slurs, each in its
 * dedicated list.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class System
        extends MusicNode
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(System.class);

    //~ Instance variables ------------------------------------------------

    // Related info from sheet analysis
    private SystemInfo info;

    // Specific Children
    private final StaveList staves;
    private final SlurList slurs;

    // Cached attributes
    private int top;

    // Cached attributes
    private int left;

    // Cached attributes
    private int width;

    // Cached attributes
    private int height;

    // Actual cached display origin
    private Point origin;

    // First, and last measure ids
    private int firstMeasureId = 0;
    private int lastMeasureId = 0;

    //~ Constructors ------------------------------------------------------

    //--------//
    // System //
    //--------//
    /**
     * Default constructor (needed by XML binder)
     */
    public System ()
    {
        this(null, null, 0, 0, 0, 0);
    }

    //--------//
    // System //
    //--------//
    /**
     * Create a system with all needed parameters
     *
     * @param info   the physical information retrieved from the sheet
     * @param score  the containing score
     * @param top    the ordinate, in units, of the upper left point of the
     *               system in its containing score
     * @param left   the abscissa, in units, of the upper left
     * @param width  the system width, in units
     * @param height the system height, in units
     */
    public System (SystemInfo info,
                   Score score,
                   int top,
                   int left,
                   int width,
                   int height)
    {
        super(score);

        // Allocate stave and slur (node) lists
        staves = new StaveList(this);
        slurs = new SlurList(this);

        this.info = info;
        this.top = top;
        this.left = left;
        this.width = width;
        this.height = height;

        if (logger.isDebugEnabled()) {
            Dumper.dump(this, "Constructed");
        }
    }

    //~ Methods -----------------------------------------------------------

    //-------------------//
    // getFirstMeasureId //
    //-------------------//
    /**
     * Report the id of the first measure in the system, knowing that 0 is
     * the id of the very first measure of the very first system in the
     * score
     *
     * @return the first measure id
     */
    public int getFirstMeasureId ()
    {
        return firstMeasureId;
    }

    //---------------//
    // getFirstStave //
    //---------------//
    /**
     * Report the first stave in this system
     *
     * @return the first stave entity
     */
    public Stave getFirstStave ()
    {
        return (Stave) getStaves().get(0);
    }

    //-----------//
    // setHeight //
    //-----------//
    /**
     * Set the system height (top of first stave, down to bottom of last
     * stave)
     *
     * @param height system height, in units
     */
    public void setHeight (int height)
    {
        this.height = height;
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the system height
     *
     * @return height in units
     */
    public int getHeight ()
    {
        return height;
    }

    //---------//
    // getInfo //
    //---------//
    /**
     * Report the physical information retrieved from the sheet for this
     * system
     *
     * @return the information entity
     */
    public SystemInfo getInfo ()
    {
        return info;
    }

    //------------------//
    // setLastMeasureId //
    //------------------//
    /**
     * Remember the id of the last measure in this system
     *
     * @param lastMeasureId last measure index
     */
    public void setLastMeasureId (int lastMeasureId)
    {
        this.lastMeasureId = lastMeasureId;
    }

    //------------------//
    // getLastMeasureId //
    //------------------//
    /**
     * Report the id of the last measure in this system
     *
     * @return the last measure id
     */
    public int getLastMeasureId ()
    {
        return lastMeasureId;
    }

    //--------------//
    // getLastStave //
    //--------------//
    /**
     * Report the last stave in this system
     *
     * @return the last stave entity
     */
    public Stave getLastStave ()
    {
        return (Stave) getStaves().get(getStaves().size() - 1);
    }

    //---------//
    // setLeft //
    //---------//
    /**
     * Set the abscissa of the left side of the system in the score
     *
     * @param left x in units of the left edge
     */
    public void setLeft (int left)
    {
        this.left = left;
    }

    //---------//
    // getLeft //
    //---------//
    /**
     * Report the abscissa of the left edge of this system in its
     * containing score
     *
     * @return x, in units, of the left side
     */
    public int getLeft ()
    {
        return left;
    }

    //-----------//
    // getOrigin //
    //-----------//
    /**
     * Report the display origin for this system
     *
     * @return the display origin
     */
    public Point getOrigin ()
    {
        return origin;
    }

    //------------------//
    // getRightPosition //
    //------------------//
    /**
     * Return the actual display position of the right side.
     *
     * @return the display abscissa of the right system edge
     */
    public int getRightPosition ()
    {
        return (origin.x + width) - 1;
    }

    //----------//
    // setSlurs //
    //----------//
    /**
     * Set the collection of slurs
     *
     * @param slurs the collection of slurs
     */
    public void setSlurs (List<TreeNode> slurs)
    {
        final List<TreeNode> list = getSlurs();
        if (list != slurs) {
            list.clear();
            list.addAll(slurs);
        }
    }

    //----------//
    // getSlurs //
    //----------//
    /**
     * Report the collection of slurs
     *
     * @return the slur list, which may be empty but not null
     */
    public List<TreeNode> getSlurs ()
    {
        return slurs.getChildren();
    }

    //-----------//
    // setStaves //
    //-----------//
    /**
     * Set the collection of staves
     *
     * @param staves the collection of staves
     */
    public void setStaves (List<TreeNode> staves)
    {
        final List<TreeNode> list = getStaves();
        if (list != staves) {
            list.clear();
            list.addAll(staves);
        }
    }

    //-----------//
    // getStaves //
    //-----------//
    /**
     * Report the collection of staves
     *
     * @return the stave list
     */
    public List<TreeNode> getStaves ()
    {
        return staves.getChildren();
    }

    //--------//
    // setTop //
    //--------//
    /**
     * Set the ordinate of the upper left corner of the system in the score
     *
     * @param top y in units of the upper left corner
     */
    public void setTop (int top)
    {
        this.top = top;
    }

    //--------//
    // getTop //
    //--------//
    /**
     * Report the ordinate in the score of the upper left point of the system
     *
     * @return y, in units, for the upper left point of the system
     */
    public int getTop ()
    {
        return top;
    }

    //----------//
    // setWidth //
    //----------//
    /**
     * Set the width of the system
     *
     * @param width width, in units, between left edge and right edge
     */
    public void setWidth (int width)
    {
        this.width = width;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the system width
     *
     * @return system width, in units
     */
    public int getWidth ()
    {
        return width;
    }

    //----------//
    // addChild //
    //----------//
    /**
     * Overrides normal behavior, to deal with the separation of children
     * into slurs and staves
     *
     * @param node the node to insert (either a slur or a stave)
     */
    @Override
    public void addChild (TreeNode node)
    {
        if (node instanceof Stave) {
            staves.addChild(node);
            node.setContainer(staves);
        } else if (node instanceof Slur) {
            slurs.addChild(node);
            node.setContainer(slurs);
        } else if (node instanceof MusicNode) {
            children.add(node);
            node.setContainer(this);
        } else {
            // Programming error
            Dumper.dump(node);
            logger.severe("System node not Stave nor Slur");
        }
    }

    //--------------//
    // sheetToScore //
    //--------------//
    /**
     * Compute the score display point that correspond to a given sheet
     * point, since systems are displayed horizontally in the score
     * display, while they are located one under the other in a sheet.
     *
     * @param pagPt the point in the sheet
     * @param scrPt the corresponding point in score display
     *
     * @see #scoreToSheet
     */
    public void sheetToScore (PagePoint pagPt,
                              Point scrPt)
    {
        scrPt.x = (origin.x + pagPt.x) - left;
        scrPt.y = (origin.y + pagPt.y) - top;
    }

    //--------------//
    // scoreToSheet //
    //--------------//
    /**
     * Compute the point in the sheet that corresponds to a given point in
     * the score display
     *
     * @param scrPt the point in the score display
     * @param pagPt the corresponding sheet point
     *
     * @see #sheetToScore
     */
    public void scoreToSheet (Point scrPt,
                              PagePoint pagPt)
    {
        pagPt.x = (left + scrPt.x) - origin.x;
        pagPt.y = (top + scrPt.y) - origin.y;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return a string based on upper left corner
     */
    @Override
    public String toString ()
    {
        return "{System" + " top=" + top + " left=" + left + " height="
               + height + " width=" + width + "}";
    }

    //---------//
    // xLocate //
    //---------//
    /**
     * Return the position of given x, relative to the system.
     *
     * @param x the abscissa value of the point (scrPt)
     *
     * @return -1 for left, 0 for middle, +1 for right
     */
    public int xLocate (int x)
    {
        if (x < origin.x) {
            return -1;
        }

        if (x > getRightPosition()) {
            return +1;
        }

        return 0;
    }

    //---------//
    // yLocate //
    //---------//
    /**
     * Return the position of given y, relative to the system
     *
     * @param y the ordinate value of the point (pagPt)
     *
     * @return -1 for above, 0 for middle, +1 for below
     */
    public int yLocate (int y)
    {
        if (y < top) {
            return -1;
        }

        if (y > (top + height + STAFF_HEIGHT)) {
            return +1;
        }

        return 0;
    }

    //-------------//
    // computeNode //
    //-------------//
    /**
     * The <code>computeNode</code> method overrides the normal routine,
     * for specific system computation. The various 'systems' are aligned
     * horizontally, rather than vertically as they were in the original
     * music sheet.
     *
     * @return true
     */
    @Override
        protected boolean computeNode ()
    {
        super.computeNode();

        // Containing score
        Score score = (Score) container;

        // Is there a Previous System ?
        System prevSystem = (System) getPreviousSibling();

        if (prevSystem == null) {
            // Very first system in the score
            origin = new Point(SCORE_INIT_X, SCORE_INIT_Y);
            firstMeasureId = 0;
        } else {
            // Not the first system
            origin = new Point(prevSystem.origin);
            origin.translate(prevSystem.width - 1 + INTER_SYSTEM, 0);
            firstMeasureId = prevSystem.lastMeasureId;
        }

        if (logger.isDebugEnabled()) {
            Dumper.dump(this, "Computed");
        }

        return true;
    }

    //-----------//
    // paintNode //
    //-----------//
    /**
     * Specific <code>paintNode</code> method, just the system left and
     * right sides are drawn
     *
     * @param g the graphic context
     * @param comp the containing component
     *
     * @return true if painted was actually done, so that depending
     *         entities (stave, slurs) are also rendered, false otherwise
     *         to stop the painting
     */
    @Override
        protected boolean paintNode (Graphics g,
                                     Zoom zoom,
                                     Component comp)
    {
        // What is the clipping region
        Rectangle clip = g.getClipBounds();

        // Check that our system is impacted
        if (xIntersect(zoom.unscaled(clip.x),
                       zoom.unscaled(clip.x + clip.width))) {
            g.setColor(Color.lightGray);

            // Draw the system left edge
            g.drawLine(zoom.scaled(origin.x), zoom.scaled(origin.y),
                       zoom.scaled(origin.x),
                       zoom.scaled(origin.y + height + STAFF_HEIGHT));

            // Draw the system right edge
            g.drawLine(zoom.scaled(origin.x + width), zoom.scaled(origin.y),
                       zoom.scaled(origin.x + width),
                       zoom.scaled(origin.y + height + STAFF_HEIGHT));

            return true;
        } else {
            return false;
        }
    }

    //------------//
    // xIntersect //
    //------------//
    /**
     * Check for intersection of a given stick determined by (left, right)
     * with the system abscissa range
     *
     * @param left  min abscissa
     * @param right max abscissa
     *
     * @return true if overlap, false otherwise
     */
    private boolean xIntersect (int left,
                                int right)
    {
        if (left > getRightPosition()) {
            return false;
        }

        if (right < origin.x) {
            return false;
        }

        return true;
    }

    //~ Classes -----------------------------------------------------------

    //-----------//
    // StaveList //
    //-----------//
    private static class StaveList
            extends MusicNode
    {
        StaveList (MusicNode container)
        {
            super(container);
        }
    }

    //----------//
    // SlurList //
    //----------//
    private static class SlurList
            extends MusicNode
    {
        SlurList (MusicNode container)
        {
            super(container);
        }
    }
}
