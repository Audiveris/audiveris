//-----------------------------------------------------------------------//
//                                                                       //
//                               S t a v e                               //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import omr.lag.Lag;
import omr.sheet.BarInfo;
import omr.sheet.StaveInfo;
import omr.ui.view.Zoom;
import omr.util.Dumper;
import omr.util.Logger;
import omr.util.TreeNode;

import static omr.score.ScoreConstants.*;

import java.awt.*;
import java.util.List;

/**
 * Class <code>Stave</code> handles a staff in a system.
 *
 * <p/> It comprises measures, lyric info, text and dynamic
 * indications. Each kind of these elements is kept in a dedicated
 * collection, so the direct children of a stave are in fact these 4
 * collections. </p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Stave
        extends MusicNode
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(Stave.class);

    //~ Instance variables ------------------------------------------------

    // Related info from sheet analysis
    private StaveInfo info;

    // Attributes
    private int top;

    // Attributes
    private int left;

    // Attributes
    private int width;

    // Attributes
    private int size;

    // Attributes
    private int stavelink;

    // Specific children
    private MeasureList measures;
    private LyricList lyriclines;
    private TextList texts;
    private DynamicList dynamics;

    // Starting bar line (the others are linked to measures)
    private BarInfo startingBar;

    // Actual cached display origin
    private Point origin;

    // First, and last measure ids
    private int firstMeasureId;

    // First, and last measure ids
    private int lastMeasureId;

    //~ Constructors ------------------------------------------------------

    //-------//
    // Stave //
    //-------//
    /**
     * Default constructor (needed by XML binder)
     */
    public Stave ()
    {
        super(null);
        allocateChildren();

        if (logger.isFineEnabled()) {
            Dumper.dump(this, "Construction");
        }
    }

    //-------//
    // Stave // Building this Stave
    //-------//
    /**
     * Build a stave, given all its parameters
     *
     * @param info      the physical information read from the sheet
     * @param system    the containing system
     * @param top       the ordinate,in units, wrt to the score upper left
     *                  corner
     * @param left      the abscissa, in units, wrt to the score upper left
     *                  corner
     * @param width     the stave width, in units
     * @param size      the stave height, in units ??? TBD ???
     * @param stavelink the index of the stave in the containing system,
     *                  starting at 0
     */
    public Stave (StaveInfo info,
                  System system,
                  int top,
                  int left,
                  int width,
                  int size,
                  int stavelink)
    {
        super(system);
        allocateChildren();

        this.info = info;
        this.top = top;
        this.left = left;
        this.width = width;
        this.size = size;
        this.stavelink = stavelink;

        if (logger.isFineEnabled()) {
            Dumper.dump(this, "Constructed");
        }
    }

    //~ Methods -----------------------------------------------------------

    //-------------//
    // setDynamics //
    //-------------//
    /**
     * Set the dynamics collection
     *
     * @param dynamics the dynamics collection
     */
    public void setDynamics (List<TreeNode> dynamics)
    {
        this.dynamics = new DynamicList(this);
        getDynamics().addAll(dynamics);
    }

    //-------------//
    // getDynamics //
    //-------------//
    /**
     * Report the dynamics collection
     *
     * @return the dynamics collection
     */
    public List<TreeNode> getDynamics ()
    {
        return dynamics.getChildren();
    }

    //-----------------//
    // getFirstMeasure //
    //-----------------//
    /**
     * Report the first measure in this stave
     *
     * @return the first measure entity
     */
    public Measure getFirstMeasure ()
    {
        return (Measure) getMeasures().get(0);
    }

    //-------------------//
    // getFirstMeasureId //
    //-------------------//
    /**
     * Report the id (0 is the very first measure id in the score) of the
     * first measure of the stave
     *
     * @return the measure id of the first measure
     */
    public int getFirstMeasureId ()
    {
        return firstMeasureId;
    }

    //---------//
    // getInfo //
    //---------//
    /**
     * Report the physical information retrieved from the sheet
     *
     * @return the info entity for this stave
     */
    public StaveInfo getInfo ()
    {
        return info;
    }

    //----------------//
    // getLastMeasure //
    //----------------//
    /**
     * Report the last measure in the stave
     *
     * @return the laste measure entity
     */
    public Measure getLastMeasure ()
    {
        return (Measure) getMeasures().get(getMeasures().size() - 1);
    }

    //------------------//
    // getLastMeasureId //
    //------------------//
    /**
     * Report the id of the last measure in the stave
     *
     * @return the measure id f the last measure
     */
    public int getLastMeasureId ()
    {
        return lastMeasureId;
    }

    //---------//
    // setLeft //
    //---------//
    /**
     * Set the abscissa of the starting point in the score
     *
     * @param left x in units
     */
    public void setLeft (int left)
    {
        this.left = left;
    }

    //---------//
    // getLeft //
    //---------//
    /**
     * Report the abscissa of the left side of the stave, wrt to the score
     *
     * @return x, in units, of the left side
     */
    public int getLeft ()
    {
        return left;
    }

    //---------------//
    // setLyriclines //
    //---------------//
    /**
     * Set the collection of lyrics
     *
     * @param lyriclines the collection of lyrics
     */
    public void setLyriclines (List<TreeNode> lyriclines)
    {
        this.lyriclines = new LyricList(this);
        getLyriclines().addAll(lyriclines);
    }

    //---------------//
    // getLyriclines //
    //---------------//
    /**
     * Report the collection of lyrics
     *
     * @return the lyrics collection, which may be empty
     */
    public List<TreeNode> getLyriclines ()
    {
        return lyriclines.getChildren();
    }

    //--------------//
    // getMeasureAt //
    //--------------//
    /**
     * Return measure with leftlinex = x (within dx error). This is used in
     * Bars retrieval, to check that the bar candidate is actually a bar,
     * since there is a measure starting at this abscissa in each stave of
     * the system (this test is relevant only for systems with more than
     * one stave).
     *
     * @param x  starting abscissa of the desired measure
     * @param dx tolerance in abscissa
     *
     * @return the measure found within tolerance, or null otherwise
     */
    public Measure getMeasureAt (int x,
                                 int dx)
    {
        for (TreeNode node : getMeasures()) {
            Measure measure = (Measure) node;

            if (Math.abs(measure.getLeftlinex() - x) <= dx) {
                return measure;
            }
        }

        return null; // Not found
    }

    //-------------//
    // setMeasures //
    //-------------//
    /**
     * Set the collection of measures
     *
     * @param measures the collection of measures
     */
    public void setMeasures (List<TreeNode> measures)
    {
        if (getMeasures() != measures) {
            getMeasures().clear();
            getMeasures().addAll(measures);
        }
    }

    //-------------//
    // getMeasures //
    //-------------//
    /**
     * Report the collection of measures
     *
     * @return the list of measures
     */
    public List<TreeNode> getMeasures ()
    {
        return measures.getChildren();
    }

    //-----------//
    // getOrigin //
    //-----------//
    /**
     * Report the display origin in the score display of this stave
     *
     * @return the origin
     */
    public Point getOrigin ()
    {
        return origin;
    }

    //---------//
    // setSize //
    //---------//
    /**
     * Set the height of the stave
     *
     * @param size height in units
     */
    public void setSize (int size)
    {
        this.size = size;
    }

    //---------//
    // getSize //
    //---------//
    /**
     * Report the height of the stave
     *
     * @return height in units (to be confirmed TBD)
     */
    public int getSize ()
    {
        return size;
    }

    //----------------//
    // setStartingBar //
    //----------------//
    /**
     * Set the bar line that starts the stave
     *
     * @param bar the related bar info
     */
    public void setStartingBar (BarInfo bar)
    {
        startingBar = bar;
    }

    //--------------//
    // setStavelink //
    //--------------//
    /**
     * Set the stave index in the containing system
     *
     * @param stavelink system relative index, starting from 0
     */
    public void setStavelink (int stavelink)
    {
        this.stavelink = stavelink;
    }

    //--------------//
    // getStavelink //
    //--------------//
    /**
     * Report the stave index within the containing system
     *
     * @return the index, counting from 0
     */
    public int getStavelink ()
    {
        return stavelink;
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * Report the containing system
     *
     * @return the system entity
     */
    public System getSystem ()
    {
        // Beware, stave is not a direct child of System, there is an
        // intermediate StaveList to skip
        return (System) container.getContainer();
    }

    //--------//
    // getTop //
    //--------//
    /**
     * Report the ordinate of the starting point of this stave, wrt to the
     * score
     *
     * @return y, in units, in the containing score
     */
    public int getTop ()
    {
        return top;
    }

    //----------//
    // setWidth //
    //----------//
    /**
     * Set the stave width
     *
     * @param width width in units f the stave
     */
    public void setWidth (int width)
    {
        this.width = width;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width of the stave
     *
     * @return the width in units
     */
    public int getWidth ()
    {
        return width;
    }

    //----------//
    // addChild // Overrides normal behavior
    //----------//
    /**
     * Override normal behavior, so that a given child is store in its
     * proper type collection (measure to measure list, lyrics to lyrics
     * list, etc...)
     *
     * @param node the child to insert in the stave
     */
    @Override
        public void addChild (TreeNode node)
    {
        if (node instanceof Measure) {
            measures.addChild(node);
            node.setContainer(measures);

            //      } else if (node instanceof Lyricline) {
            //          lyriclines.addChild (node);
            //          node.setContainer (lyriclines);
            //      } else if (node instanceof Text) {
            //          texts.addChild (node);
            //          node.setContainer (texts);
            //      } else if (node instanceof Dynamic) {
            //          dynamics.addChild (node);
            //          node.setContainer (dynamics);
        } else if (node instanceof TreeNode) {
            // Meant for the 4 lists
            children.add(node);
            node.setContainer(this);
        } else {
            // Programming error
            Dumper.dump(node);
            logger.severe("Stave node not known");
        }
    }

    //------------------------//
    // incrementLastMeasureId //
    //------------------------//
    /**
     * Methd called to signal a new measure built in this stave
     */
    public void incrementLastMeasureId ()
    {
        getSystem().setLastMeasureId(++lastMeasureId);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return a string based of the location and size of the stave
     */
    public String toString ()
    {
        return "{Stave" +
            " top=" + top +
            " left=" + left +
            " width=" + width +
            " size=" + size +
            " origin=" + origin + "}";
    }

    //--------------//
    // colorizeNode //
    //--------------//
    /**
     * Colorize the physical information of this stave, which is just the
     * starting bar line if any
     *
     * @param lag       the lag to be colorized
     * @param viewIndex the provided lag view index
     * @param color     the color to be used
     *
     * @return true if processing must continue
     */
    protected boolean colorizeNode (Lag lag,
                                    int viewIndex,
                                    Color color)
    {
        // Set color for the starting bar line, if any
        if (startingBar != null) {
            startingBar.colorize(lag, viewIndex, color);
        }

        return true;
    }

    //-------------//
    // computeNode //
    //-------------//
    /**
     * Override the method, so that internal computation can take place
     *
     * @return true
     */
    protected boolean computeNode ()
    {
        super.computeNode();

        // Display origin for system
        Point sysorg = getSystem().getOrigin();

        // Display origin for the stave
        origin = new Point(sysorg.x + (left - getSystem().getLeft()),
                           sysorg.y + (top - getSystem().getTop()));

        // First/Last measure ids
        firstMeasureId = lastMeasureId = getSystem().getFirstMeasureId();

        return true;
    }

    //-----------//
    // paintNode //
    //-----------//
    @Override
        protected boolean paintNode (Graphics g,
                                     Zoom zoom,
                                     Component comp)
    {
        //System.out.println ("StaveItf. origin=" + origin + " size=" + size);
        g.setColor(Color.black);

        // Draw the staff lines
        for (int i = 0; i < LINE_NB; i++) {
            // Y of this staff line
            int y = zoom.scaled(origin.y + (i * INTER_LINE));
            g.drawLine(zoom.scaled(origin.x), y,
                       zoom.scaled(origin.x + width), y);
        }

        return true; // Meaning : we've drawn something
    }

    //------------//
    // renderNode //
    //------------//
    /**
     * Render the physical information of this stave
     *
     * @param g the graphics context
     * @param z the display zoom
     *
     * @return true if rendered
     */
    @Override
    protected boolean renderNode (Graphics g,
                                  Zoom z)
    {
        // Render the staff lines
        if (info != null) {
            info.render(g, z);

            if (startingBar != null) {
                startingBar.render(g, z);
            }

            return true;
        }

        return false;
    }

    //------------------//
    // allocateChildren //
    //------------------//
    private void allocateChildren ()
    {
        // Allocate specific children lists
        measures = new MeasureList(this);
        lyriclines = new LyricList(this);
        texts = new TextList(this);
        dynamics = new DynamicList(this);
    }

    //~ Classes -----------------------------------------------------------

    //-------------//
    // MeasureList //
    //-------------//
    private static class MeasureList
        extends MusicNode
    {
        MeasureList (MusicNode container)
        {
            super(container);
        }
    }

    //-----------//
    // LyricList //
    //-----------//
    private static class LyricList
            extends MusicNode
    {
        LyricList (MusicNode container)
        {
            super(container);
        }
    }

    //----------//
    // TextList //
    //----------//
    private static class TextList
            extends MusicNode
    {
        TextList (MusicNode container)
        {
            super(container);
        }
    }

    //-------------//
    // DynamicList //
    //-------------//
    private static class DynamicList
            extends MusicNode
    {
        DynamicList (MusicNode container)
        {
            super(container);
        }
    }
}
