//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         N e a r L i n e                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.run.Orientation;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import omr.util.Entity;

/**
 * Interface {@code NearLine} describes an entity close to a straight line.
 *
 * @author Hervé Bitteur
 */
public interface NearLine
        extends Entity
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the co-tangent of glyph line angle with abscissa axis.
     *
     * @return co-tangent of heading angle (dx/dy).
     */
    double getInvertedSlope ();

    /**
     * Return the approximating straight line computed on the glyph.
     *
     * @return The absolute line
     */
    Line2D getLine ();

    /**
     * Return the mean quadratic distance of the defining population of points to the
     * resulting line.
     * This can be used to measure how well the line fits the points.
     *
     * @return the absolute value of the mean distance
     */
    double getMeanDistance ();

    /**
     * Report the tangent of glyph line angle with abscissa axis.
     *
     * @return tangent of heading angle (dy/dx).
     */
    double getSlope ();

    /**
     * Report the absolute point at the beginning, along the provided orientation,
     * of the approximating line.
     *
     * @param orientation the general orientation reference
     * @return the starting point of the glyph line
     */
    Point2D getStartPoint (Orientation orientation);

    /**
     * Report the absolute point at the end, along the provided orientation,
     * of the approximating line.
     *
     * @param orientation the general orientation reference
     * @return the ending point of the line
     */
    Point2D getStopPoint (Orientation orientation);

    /**
     * Render the glyph main line onto the provided graphics.
     *
     * @param g graphics environment
     */
    void renderLine (Graphics2D g);
}
