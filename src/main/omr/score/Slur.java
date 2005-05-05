//-----------------------------------------------------------------------//
//                                                                       //
//                                S l u r                                //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.score;

import omr.util.Dumper;
import omr.util.Logger;
import omr.ui.Zoom;

import java.awt.*;

/**
 * Class <code>Slur</code> encapsulates a slur (a circle arc) in a system
 */
public class Slur
        extends MusicNode
{
    //~ Static variables/initializers ----------------------------------------

    private static final Logger logger = Logger.getLogger(Slur.class);

    //~ Instance variables ---------------------------------------------------

    // Cached attributes
    private int leftrow;

    // Cached attributes
    private int leftcolumn;

    // Cached attributes
    private int rightrow;

    // Cached attributes
    private int rightcolumn;

    // Cached attributes
    private int radius;

    // Additional drawing data
    private Arc arc;

    //~ Constructors ---------------------------------------------------------

    //------//
    // Slur //
    //------//

    /**
     * Default constructure (needed by Castor)
     */
    public Slur ()
    {
        super(null);

        if (logger.isDebugEnabled()) {
            Dumper.dump(this, "Construction");
        }
    }

    //------//
    // Slur //
    //------//

    /**
     * Create a slur with all the specified parameters
     *
     * @param system      the containing system
     * @param leftrow     y (in units) of the left point, wrt the system
     *                    origin
     * @param leftcolumn  x (in units) of the left point
     * @param rightrow    y (in units) of the right point
     * @param rightcolumn x (in units) of the right point
     * @param radius      the radius of the circle, in units, &lt;0 if center
     *                    if above, &gt;0 if center is below
     */
    public Slur (System system,
                 int leftrow,
                 int leftcolumn,
                 int rightrow,
                 int rightcolumn,
                 int radius)
    {
        super(system);

        this.leftrow = leftrow;
        this.leftcolumn = leftcolumn;
        this.rightrow = rightrow;
        this.rightcolumn = rightcolumn;
        this.radius = radius;

        if (logger.isDebugEnabled()) {
            Dumper.dump(this, "Constructed");
        }
    }

    //~ Methods --------------------------------------------------------------

    //---------------//
    // setLeftcolumn //
    //---------------//

    /**
     * Set the abscissa of the left point (needed by Castor)
     *
     * @param leftcolumn x for left point, in units
     */
    public void setLeftcolumn (int leftcolumn)
    {
        this.leftcolumn = leftcolumn;
    }

    //---------------//
    // getLeftcolumn //
    //---------------//

    /**
     * Report the abscissa of the left point (needed by Castor)
     *
     * @return x in units for left point
     */
    public int getLeftcolumn ()
    {
        return leftcolumn;
    }

    //------------//
    // setLeftrow //
    //------------//

    /**
     * Set the ordinate of the left point (needed by Castor)
     *
     * @param leftrow y for left point, in units, wrt to the system origin
     */
    public void setLeftrow (int leftrow)
    {
        this.leftrow = leftrow;
    }

    //------------//
    // getLeftrow //
    //------------//

    /**
     * Report the ordinate of the left point (needed by Castor)
     *
     * @return y in units for left point
     */
    public int getLeftrow ()
    {
        return leftrow;
    }

    //-----------//
    // getOrigin //
    //-----------//

    /**
     * Report the display origin (which is the origin of the containing
     * system) used for slur x and y relative coordinates
     *
     * @return the base origin
     */
    public Point getOrigin ()
    {
        return ((System) container.getContainer()).getOrigin();
    }

    //-----------//
    // setRadius //
    //-----------//

    /**
     * Set the slur radius (needed by Castor)
     *
     * @param radius radius in unit, &lt;0 upward, &gt;0 downward
     */
    public void setRadius (int radius)
    {
        this.radius = radius;
    }

    //-----------//
    // getRadius //
    //-----------//

    /**
     * Report the radius of the slur (needed by Castor)
     *
     * @return radius in units, &lt;0 upwards, &gt;0 downwards
     */
    public int getRadius ()
    {
        return radius;
    }

    //----------------//
    // setRightcolumn //
    //----------------//

    /**
     * Set the abscissa of the right point (needed by Castor)
     *
     * @param rightcolumn x of the right point
     */
    public void setRightcolumn (int rightcolumn)
    {
        this.rightcolumn = rightcolumn;
    }

    //----------------//
    // getRightcolumn //
    //----------------//

    /**
     * Report the abscissa of the right point (needed by Castor)
     *
     * @return x in units for the right point
     */
    public int getRightcolumn ()
    {
        return rightcolumn;
    }

    //-------------//
    // setRightrow //
    //-------------//

    /**
     * Set the ordinate of the right point (needed by Castor)
     *
     * @param rightrow y for right point
     */
    public void setRightrow (int rightrow)
    {
        this.rightrow = rightrow;
    }

    //-------------//
    // getRightrow //
    //-------------//

    /**
     * Report the ordinate of the right point (neede by Castor)
     *
     * @return y in units for the right point
     */
    public int getRightrow ()
    {
        return rightrow;
    }

    //----------//
    // toString //
    //----------//

    /**
     * Report a readable description for this slur
     *
     * @return a string with all slur parameters
     */
    public String toString ()
    {
        return "{Slur:" + " leftrow=" + leftrow + " leftcolumn=" + leftcolumn
               + " rightrow=" + rightrow + " rightcolumn=" + rightcolumn
               + " radius=" + radius + "}";
    }

    //-----------//
    // paintNode //
    //-----------//

    /**
     * Overrides the method, so that this slur node be rendered
     *
     * @param g the graphics context
     *
     * @return true
     */
    protected boolean paintNode (Graphics g)
    {
        // Compute data needed for drawing
        if (arc == null) {
            arc = new Arc(leftcolumn, leftrow, rightcolumn, rightrow, radius);
        }

        arc.draw(g, getOrigin(), ScoreView.getZoom());

        return true;
    }

    //~ Classes --------------------------------------------------------------

    //-----//
    // Arc //
    //-----//
    private static class Arc
    {
        //~ Instance variables -----------------------------------------------

        private Point upperleft;
        private int squareside;
        private int startangle;
        private int arcangle;

        //~ Constructors -----------------------------------------------------

        //-----//
        // Arc //
        //-----//
        public Arc (int x1,
                    int y1,
                    int x2,
                    int y2,
                    int radius)
        {
            int rad = Math.abs(radius);
            int sign = radius / rad;
            double dx = x2 - x1;
            double dy = y2 - y1;
            double L2 = (dx * dx) + (dy * dy);
            double L = Math.sqrt(L2);

            double xm = x1 + (dx / 2);
            double ym = y1 + (dy / 2);

            double h = Math.sqrt((radius * radius) - (L2 / 4));

            double x0 = xm + ((sign * dy * h) / L);
            double y0 = ym - ((sign * dx * h) / L);

            squareside = 2 * rad;
            upperleft = new Point((int) x0 - rad, (int) y0 - rad);
            startangle = -(int) Math.toDegrees(Math.atan2(y1 - y0, x1 - x0));
            arcangle = sign * (int) Math.toDegrees(2 * Math.acos(h / rad));
        }

        //~ Methods ----------------------------------------------------------

        //------//
        // draw //
        //------//
        public void draw (Graphics g,
                          Point origin,
                          Zoom zoom)
        {
            g.drawArc(zoom.scaled(origin.x + upperleft.x),
                      zoom.scaled(origin.y + upperleft.y),
                      zoom.scaled(squareside), zoom.scaled(squareside),
                      startangle, arcangle);
        }
    }
}
