//-----------------------------------------------------------------------//
//                                                                       //
//                                S l u r                                //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import omr.ui.view.Zoom;
import omr.util.Dumper;
import omr.util.Logger;

import java.awt.*;

/**
 * Class <code>Slur</code> encapsulates a slur (a circle arc) in a system
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Slur
    extends MusicNode
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(Slur.class);

    //~ Instance variables ------------------------------------------------

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

    //~ Constructors ------------------------------------------------------

    //------//
    // Slur //
    //------//
    /**
     * NoArg constructor (needed by XML binder)
     */
    public Slur ()
    {
        super(null);

        if (logger.isFineEnabled()) {
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
     * @param radius the radius of the circle, in units, &lt;0 if center if
     *                    above, &gt;0 if center is below
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

        if (logger.isFineEnabled()) {
            Dumper.dump(this, "Constructed");
        }
    }

    //~ Methods -----------------------------------------------------------

    //---------------//
    // getLeftcolumn //
    //---------------//
    /**
     * Report the abscissa of the left point
     *
     * @return x in units for left point
     */
    public int getLeftcolumn ()
    {
        return leftcolumn;
    }

    //------------//
    // getLeftrow //
    //------------//
    /**
     * Report the ordinate of the left point
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
    // getRadius //
    //-----------//
    /**
     * Report the radius of the slur
     *
     * @return radius in units, &lt;0 upwards, &gt;0 downwards
     */
    public int getRadius ()
    {
        return radius;
    }

    //----------------//
    // getRightcolumn //
    //----------------//
    /**
     * Report the abscissa of the right point
     *
     * @return x in units for the right point
     */
    public int getRightcolumn ()
    {
        return rightcolumn;
    }

    //-------------//
    // getRightrow //
    //-------------//
    /**
     * Report the ordinate of the right point
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
    @Override
        public String toString ()
    {
        return "{Slur:" +
            " leftrow=" + leftrow +
            " leftcolumn=" + leftcolumn +
            " rightrow=" + rightrow +
            " rightcolumn=" + rightcolumn +
            " radius=" + radius + "}";
    }

    //-----------//
    // paintNode //
    //-----------//
    @Override
        protected boolean paintNode (Graphics g,
                                     Zoom zoom,
                                     Component comp)
    {
        // Compute data needed for drawing
        if (arc == null) {
            arc = new Arc(leftcolumn, leftrow, rightcolumn, rightrow, radius);
        }

        arc.draw(g, getOrigin(), zoom);

        return true;
    }

    //~ Classes -----------------------------------------------------------

    //-----//
    // Arc //
    //-----//
    private static class Arc
    {
        //~ Instance variables --------------------------------------------

        private final Point upperleft;
        private final int squareside;
        private final int startangle;
        private final int arcangle;

        //~ Constructors --------------------------------------------------

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

        //~ Methods -------------------------------------------------------

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
