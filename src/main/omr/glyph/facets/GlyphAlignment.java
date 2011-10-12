//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h A l i g n m e n t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.math.Line;

import omr.run.Orientation;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

/**
 * Interface {@code GlyphAlignment} describes glyph alignment.
 * The key feature is the approximating Line on all points of the glyph.
 * The line can be the least-square fitted line, or a natural spline for more
 * complex cases.
 *
 * <ul>
 * <li>Staff lines, ledgers, alternate ends are examples of rather
 * horizontal glyphs.</li>
 * <li>Bar lines, stems are examples of rather vertical glyphs.</li>
 * <li>Other glyphs have no dominant orientation.</li>
 * </ul>
 *
 * <p>Note that a glyph has no predefined orientation, only the slope of its
 * approximating line is relevant and allows to disambiguate between the
 * start point and the stop point. If abs(tangent) is less than 45 degrees we
 * have a rather horizontal glyph, otherwise a rather vertical glyph.</p>
 *
 * @author Hervé Bitteur
 */
public interface GlyphAlignment
    extends GlyphFacet
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report the ratio of length over thickness
     * @param orientation the general orientation reference
     * @return the "slimness" of the stick
     * @see #getLength
     * @see #getThickness
     */
    double getAspect (Orientation orientation);

    /**
     * Force the locations of start point and stop points
     * @param pStart new start point
     * @param pStop new stop point
     */
    void setEndingPoints (Point2D pStart,
                          Point2D pStop);

    /**
     * Compute the number of pixels stuck on first side of the stick
     * @return the number of pixels
     */
    int getFirstStuck ();

    /**
     * Report the co-tangent of glyph line angle with abscissa axis
     * @return co-tangent of heading angle (dx/dy).
     */
    double getInvertedSlope ();

    /**
     * Compute the nb of pixels stuck on last side of the stick
     * @return the number of pixels
     */
    int getLastStuck ();

    /**
     * Report the length of the stick, along the provided orientation
     * @param orientation the general orientation reference
     * @return the stick length in pixels
     */
    int getLength (Orientation orientation);

    /**
     * Return the approximating line computed on the stick, as an
     * <b>absolute</b> line, with x for horizontal axis and y for vertical axis
     * @return The absolute line
     */
    Line getLine ();

    /**
     * Return the mean quadratic distance of the defining population of points
     * to the resulting line. This can be used to measure how well the line fits
     * the points.
     *
     * @return the absolute value of the mean distance
     */
    double getMeanDistance ();

    /**
     * Return the position (ordinate for horizontal stick, abscissa for vertical
     * stick) at the middle of the stick
     * @return the position of the middle of the stick
     */
    int getMidPos ();

    /**
     * Report the precise stick position for the provided coordinate .
     * @param coord the coord value (x for horizontal, y for vertical)
     * @param orientation the general orientation reference
     * @return the pos value (y for horizontal, x for vertical)
     */
    double getPositionAt (double      coord,
                          Orientation orientation);

    /**
     * Report whether the angle of the approximating line is outside the range
     * [-PI/4 .. +PI/4]
     * @return true if rather vertical, false for rather horizontal
     */
    boolean isRatherVertical ();

    /**
     * Report the tangent of glyph line angle with abscissa axis
     * @return tangent of heading angle (dy/dx).
     */
    double getSlope ();

    /**
     * Report the absolute point at the beginning of the approximating line
     * @return the starting point of the stick line
     */
    Point2D getStartPoint ();

    /**
     * Report the absolute point at the end of the approximating line
     * @return the ending point of the line
     */
    Point2D getStopPoint ();

    /**
     * Report the stick thickness across the desired orientation
     * @param orientation the general orientation reference
     * @return the thickness in pixels
     */
    int getThickness (Orientation orientation);

    /**
     * Report the resulting thickness of this stick at the provided coordinate,
     * using a predefined probe width
     * @param coord the desired abscissa
     * @param orientation the general orientation reference
     * @return the thickness measured, expressed in number of pixels.
     */
    double getThicknessAt (double      coord,
                           Orientation orientation);

    /**
     * Render the main guiding line of the stick, using the current foreground
     * color.
     * @param g the graphic context
     */
    void renderLine (Graphics2D g);
}
