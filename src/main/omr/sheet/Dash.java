//-----------------------------------------------------------------------//
//                                                                       //
//                                D a s h                                //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.math.Line;
import omr.stick.Stick;
import omr.ui.Zoom;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Class <code>Dash</code> is used to handle a horizontal segment, which
 * can represent a ledger, a legato sign or the horizontal part of an
 * alternate ending.
 *
 * <p>The role of a Dash, as compared to a plain {@link omr.stick.Stick} is
 * to handle the horizontal segment (its Line and contour box), even if the
 * underlying stick has been discarded. Doing so saves us the need to
 * serialize the whole horizontal GlyphLag.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Dash
    implements java.io.Serializable
{
    //~ Instance variables ------------------------------------------------

    private Line line;                  // Approximating line
    private Rectangle box;              // Contour box

    private transient Stick stick;      // The underlying stick if any

    //~ Constructors ------------------------------------------------------

    //------//
    // Dash //
    //------//
    public Dash (Stick stick)
    {
        this.stick = stick;
        line = stick.getLine();
        box = stick.getContourBox();
    }

    //~ Methods -----------------------------------------------------------

    //---------//
    // getLine //
    //---------//
    /**
     * Report the approximating line
     *
     * @return the dash line
     */
    public Line getLine()
    {
        if (line == null) {
            if (stick != null) {
                line = stick.getLine();
            }
        }

        return line;
    }

    //---------------//
    // getContourBox //
    //---------------//
    /**
     * Report the contour box, horizontally oriented, and so directly
     * usable for display of intersection test.
     *
     * @return the contour box
     */
    public Rectangle getContourBox()
    {
        return box;
    }

    //----------//
    // getStick //
    //----------//
    /**
     * Report the underlying stick, or null if not found
     *
     * @return the underlying stick, null otherwise
     */
    public Stick getStick()
    {
        return stick;
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the dash.
     *
     * @param g the graphics context
     * @param z the scaling zoom
     */
    public void render (Graphics g,
                        Zoom     z)
    {
        Rectangle b = z.scaled(box);

        if (b.intersects(g.getClipBounds())) {
            Line line = getLine();
            Point start = new Point
                (b.x,
                 z.scaled(line.yAt((double) box.x) + 0.5));
            Point stop = new Point
                (b.x + b.width,
                 z.scaled(line.yAt((double) box.x + box.width + 1) + 0.5));
            g.drawLine(start.x, start.y, stop.x, stop.y);
        }
    }

    //---------------//
    // renderContour //
    //---------------//
    /**
     * Render the contour box of the dash, using the current foreground
     * color
     *
     * @param g the graphic context
     * @param z the display zoom
     */
    public boolean renderContour (Graphics g,
                                  Zoom z)
    {
        // Check the clipping
        Rectangle b = z.scaled(box);

        if (b.intersects(g.getClipBounds())) {
            g.drawRect(b.x, b.y, b.width, b.height);
            return true;
        } else {
            return false;
        }
    }

    //------------------//
    // getDashIndexAtX //
    //------------------//
    /**
     * Retrieve the index of the very first dash in the provided ordered
     * list, whose left abscissa is equal or greater than the provided x
     * value.
     *
     * @param list the list to search, ordered by increasing abscissa
     * @param x the desired abscissa
     * @return the index of the first suitable dash, or list.size() if no
     * such dash can be found.
     */
    public static int getDashIndexAtX (List<? extends Dash> list,
                                       int x)
    {
        int low = 0;
        int high = list.size()-1;
        while (low <= high) {
            int mid = (low + high) >> 1;
            int gx = list.get(mid).getContourBox().x;

            if (gx < x) {
                low = mid + 1;
            } else if (gx > x) {
                high = mid - 1;
            } else {
                // We are pointing to a dash with desired x
                // Let's now pick the very first one
                for (mid = mid -1; mid >= 0; mid--) {
                    if (list.get(mid).getContourBox().x < x)
                        break;
                }

                return mid +1;
            }
        }

        return low;
    }
}
