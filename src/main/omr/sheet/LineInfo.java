//----------------------------------------------------------------------------//
//                                                                            //
//                              L i n e I n f o                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.math.Line;

import omr.stick.Stick;

import omr.ui.view.Zoom;

import java.awt.*;
import java.util.List;

/**
 * Class <code>LineInfo</code> handles one staff line.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LineInfo
    implements java.io.Serializable
{
    //~ Instance fields --------------------------------------------------------

    // Best line equation
    private Line                  line;

    // Related Builder
    private transient LineBuilder builder;
    private int                   id; // Just a sequential id for debug
    private int                   left;
    private int                   right;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // LineInfo //
    //----------//
    /**
     * Create info about one staff line
     *
     * @param id the line id (debug)
     * @param left computed abscissa of the left line end
     * @param right computed  abscissa of the right line end
     * */
    public LineInfo (int id,
                     int left,
                     int right)
    {
        this.id = id;
        this.left = left;
        this.right = right;
    }

    //~ Methods ----------------------------------------------------------------

    //-------//
    // getId //
    //-------//
    /**
     * Report the id of this line
     *
     * @return the line id (debugging info)
     */
    public int getId ()
    {
        return id;
    }

    //---------//
    // getLeft //
    //---------//
    /**
     * Selector for the left abscissa of the line
     *
     * @return left abscissa
     */
    public int getLeft ()
    {
        return left;
    }

    //---------//
    // getLine //
    //---------//
    /**
     * Selector for the best fitting line
     *
     * @return the line equation
     */
    public Line getLine ()
    {
        return line;
    }

    //----------//
    // getRight //
    //----------//
    /**
     * Selector for the right abscissa of the line
     *
     * @return right abscissa
     */
    public int getRight ()
    {
        return right;
    }

    //-----------//
    // getSticks //
    //-----------//
    /**
     * Returns the collection of sticks found in this area
     *
     * @return the sticks found
     */
    public List<Stick> getSticks ()
    {
        return builder.getSticks();
    }

    //---------//
    // cleanup //
    //---------//
    /**
     * Cleanup the line
     */
    public void cleanup ()
    {
        builder.cleanup();
    }

    //--------//
    // render //
    //--------//
    /**
     * Paint the computed line on the provided environment.
     *
     * @param g     the graphics context
     * @param z     the display zoom
     * @param left  the imposed left abscissa
     * @param right the imposed right abscissa
     */
    public void render (Graphics g,
                        Zoom     z,
                        int      left,
                        int      right)
    {
        // Paint the computed line
        if (line != null) {
            g.drawLine(
                z.scaled(left + 0.5),
                z.scaled(line.yAt(left + 0.5) + 0.5),
                z.scaled(right + 0.5),
                z.scaled(line.yAt(right + 0.5) + 0.5));
        }
    }

    //----------//
    // toString //
    //----------//
    /**
     * A readable information string
     *
     * @return a short description
     */
    @Override
    public String toString ()
    {
        return "LineInfo" + id + " left=" + left + " right=" + right +
               ((line != null) ? line.toString() : "");
    }

    //------------//
    // setBuilder //
    //------------//
    /**
     * Set the link back to the builder who holds the build context for this
     * line
     *
     * @param builder the LineBuilder
     */
    void setBuilder (LineBuilder builder)
    {
        this.builder = builder;
    }

    //---------//
    // setLine //
    //---------//
    /**
     * Store the underlying physical line
     *
     * @param line the line equation
     */
    void setLine (Line line)
    {
        this.line = line;
    }
}
