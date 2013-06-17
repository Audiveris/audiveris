//----------------------------------------------------------------------------//
//                                                                            //
//                              L i n e I n f o                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.lag.Section;

import omr.math.Line;

import omr.util.HorizontalSide;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;

/**
 * Interface {@code LineInfo} describes the handling of one staff line.
 *
 * @author Hervé Bitteur
 */
public interface LineInfo
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report the absolute contour rectangle
     *
     * @return the contour box (with minimum height of 1)
     */
    public Rectangle getBounds ();

    /**
     * Selector for the left or right ending point of the line
     *
     * @param side proper horizontal side
     * @return left point
     */
    public Point2D getEndPoint (HorizontalSide side);

    /**
     * Report the id of this line
     *
     * @return the line id (debugging info)
     */
    public int getId ();

    /**
     * Selector for the left point of the line
     *
     * @return left point
     */
    public Point2D getLeftPoint ();

    /**
     * Selector for the right point of the line
     *
     * @return right point
     */
    public Point2D getRightPoint ();

    /**
     * Report the lag sections that compose the staff line
     *
     * @return a collection of the line sections
     */
    public Collection<Section> getSections ();

    /**
     * Paint the computed line on the provided environment.
     *
     * @param g the graphics context
     */
    public void render (Graphics2D g);

    /**
     * Retrieve the precise intersection with a rather vertical line.
     *
     * @param vertical the rather vertical line
     * @return the precise intersection
     */
    public Point2D verticalIntersection (Line vertical);

    /**
     * Retrieve the staff line ordinate at given abscissa x, using int
     * values
     *
     * @param x the given abscissa
     * @return the corresponding y value
     */
    public int yAt (int x);

    /**
     * Retrieve the staff line ordinate at given abscissa x, using
     * double values
     *
     * @param x the given abscissa
     * @return the corresponding y value
     */
    public double yAt (double x);
}
