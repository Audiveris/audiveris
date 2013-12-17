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
import omr.math.GeoPath;

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
    Rectangle getBounds ();

    /**
     * Selector for the left or right ending point of the line
     *
     * @param side proper horizontal side
     * @return left point
     */
    Point2D getEndPoint (HorizontalSide side);

    /**
     * Report the id of this line
     *
     * @return the line id (debugging info)
     */
    int getId ();

    /**
     * Selector for the left point of the line
     *
     * @return left point
     */
    Point2D getLeftPoint ();

    /**
     * Report the line path
     *
     * @return the path
     */
    GeoPath toPath();

    /**
     * Selector for the right point of the line
     *
     * @return right point
     */
    Point2D getRightPoint ();

    /**
     * Report the lag sections that compose the staff line
     *
     * @return a collection of the line sections
     */
    Collection<Section> getSections ();

    /**
     * Paint the computed line on the provided environment.
     *
     * @param g the graphics context
     */
    void render (Graphics2D g);

    /**
     * Retrieve the precise intersection with a rather vertical line.
     *
     * @param vertical the rather vertical line
     * @return the precise intersection
     */
    Point2D verticalIntersection (Line vertical);

    /**
     * Retrieve the staff line ordinate at given abscissa x, using int
     * values
     *
     * @param x the given abscissa
     * @return the corresponding y value
     */
    int yAt (int x);

    /**
     * Retrieve the staff line ordinate at given abscissa x, using
     * double values
     *
     * @param x the given abscissa
     * @return the corresponding y value
     */
    double yAt (double x);
}
