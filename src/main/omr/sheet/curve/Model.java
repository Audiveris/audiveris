//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            M o d e l                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.Collection;

/**
 * Interface {@code Model} attempts to factor out characteristics that are typical
 * of a slur or line shape (or a side portion for a long slur).
 * <p>
 * The main purpose is to decouple slur retrieval from the actual mathematical item handled (circle,
 * parabola, quadCurve, line, ...)
 *
 * @author Hervé Bitteur
 */
public interface Model
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the item shape as /--\ (above) or \--/ (below) or ---- (flat).
     *
     * @return 1 for above, -1 for below, 0 for flat.
     */
    int above ();

    /**
     * Report whether the curve turns counter-clockwise from first to last point.
     *
     * @return 1 for counter-clockwise, -1 for clockwise, 0 for straight
     */
    int ccw ();

    /**
     * Compute the fitting distance on the provided points.
     *
     * @param points collection of points, perhaps not ordered
     * @return the fitting distance
     */
    double computeDistance (Collection<? extends Point2D> points);

    /**
     * Report the angle with the x-axis of the tangent at item end, designated by
     * 'reverse'
     *
     * @param reverse true for first end, false for last
     * @return the angle of tangent (within -PI..PI), or null if failed
     */
    Double getAngle (boolean reverse);

    /**
     * Report the bounding rectangle of the item
     *
     * @return the bounding box
     */
    Rectangle getBounds ();

    /**
     * Report the drawable shape of this model.
     *
     * @return the AWT shape
     */
    Shape getCurve ();

    /**
     * Tell how well the model fits the slur.
     *
     * @return the mean distance between candidate slur points and the model
     */
    double getDistance ();

    /**
     * Report the unit tangent vector at item end, designated by 'reverse'.
     *
     * @param reverse true for first end, false for last
     * @return the tangent unit vector
     */
    Point2D getEndVector (boolean reverse);

    /**
     * Report the middle point of the curve.
     *
     * @return mid point
     */
    Point2D getMidPoint ();

    /**
     * Reverse the internal data.
     */
    void reverse ();

    /**
     * Remember the computed distance in the instance.
     *
     * @param dist the computed distance
     */
    void setDistance (double dist);
}
