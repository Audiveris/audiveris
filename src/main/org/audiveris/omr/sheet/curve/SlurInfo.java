//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S l u r I n f o                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.curve;

import org.audiveris.omr.math.LineUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code SlursInfo} gathers physical description of a slur.
 * <p>
 * It is not meant to be persistent.
 * During slur building in SLURS step, it allows to check and implement the connection of smaller
 * slurs into larger slurs.
 * <p>
 * Short and medium slurs generally fit a global circle rather well.
 * But a long slur may be closer to an ellipsis, hence the use of local osculating circles, one at
 * the start and one at the end of slur, in order to more accurately evaluate slur extensions.
 *
 * @author Hervé Bitteur
 */
public class SlurInfo
        extends Curve
{

    private static final Logger logger = LoggerFactory.getLogger(SlurInfo.class);

    /** Approximating first side model. */
    protected Model firstModel;

    /** Approximating last side model. */
    protected Model lastModel;

    /** Number of points for side circles. */
    protected final int sideLength;

    /** Is the slur: above heads, below heads or flat.
     * 1 for above, -1 for below, 0 for flat
     */
    protected int above;

    /** Unity vector from segment middle to circle center. */
    protected Point2D bisUnit;

    /** True for slur rather horizontal. */
    protected Boolean horizontal;

    /** Global Bézier curve for the slur. */
    protected CubicCurve2D curve;

    /**
     * Creates a new SlurInfo object.
     *
     * @param id            curve id
     * @param firstJunction first junction point, if any
     * @param lastJunction  second junction point, if any
     * @param points        sequence of defining points
     * @param model         underlying model, if any
     * @param parts         set of arcs used for this curve
     * @param sideLength    length of side circles
     */
    public SlurInfo (int id,
                     Point firstJunction,
                     Point lastJunction,
                     List<Point> points,
                     Model model,
                     Collection<Arc> parts,
                     int sideLength)
    {
        super(id, firstJunction, lastJunction, points, model, parts);
        this.sideLength = sideLength;
    }

    //---------//
    // isAbove //
    //---------//
    /**
     * Report whether the slur shape is /--\ rather than \--/.
     *
     * @return the above flag
     */
    public int above ()
    {
        return above;
    }

    //------------------//
    // computeSideModel //
    //------------------//
    /**
     * Compute a side model (on side designated by reverse) out of the provided points.
     * The points may be just the slur points (when computing side model of the slur) or the slur
     * points augmented by the points of an extension arc (when attempting to add an arc).
     *
     * @param points  the full sequence of points
     * @param reverse desired side
     * @return the side model, or null if unsuccessful
     */
    public Model computeSideModel (List<Point> points,
                                   boolean reverse)
    {
        int np = points.size();

        if (np < sideLength) {
            return null;
        }

        if (reverse) {
            points = points.subList(0, sideLength);
        } else {
            points = points.subList(np - sideLength, np);
        }

        np = points.size();

        Point p0 = points.get(0);
        Point p1 = points.get(np / 2);
        Point p2 = points.get(np - 1);

        // Choose a circle-model, otherwise a line-model
        CircleModel rough = CircleModel.create(p0, p1, p2);

        if (rough != null) {
            return rough;
        } else {
            return new LineModel(points);
        }
    }

    //------------//
    // getBisUnit //
    //------------//
    /**
     * @return the bisUnit
     */
    public Point2D getBisUnit ()
    {
        return bisUnit;
    }

    //----------//
    // getCurve //
    //----------//
    /**
     * Report the left-to-right Bézier curve which best approximates the slur.
     * <p>
     * It is built by combining the left half (point &amp; control point) of left circle curve and
     * the right half (control point &amp; point) of right circle curve.
     * Vectors from point to related control point are applied a ratio extension so that curve
     * middle point (M) fits on slur middle point (M').
     * We apply the same ratio on both vectors, which may not be the best choice but that's enough
     * for a first version.
     * On a bezier curve, naming P the middle point of segment (P1,P2) and C the middle point of
     * segment (CP1,CP2), we always have vector PC = 4/3 of vector PM.
     * So, (PC' - PC) = 4/3 (PM' - PM) or (ratio - 1) * PC = 4/3 * deltaM, which gives ratio value.
     *
     * @return the bezier curve
     */
    public CubicCurve2D getCurve ()
    {
        if (curve == null) {
            Model leftModel = getSideModel(true);
            Model rightModel = getSideModel(false);

            if ((leftModel == null) || (rightModel == null)) {
                ///logger.warn("No side circle");
                return null;
            }

            // Assume we have circle models on both ends
            if (!(leftModel instanceof CircleModel) || !(rightModel instanceof CircleModel)) {
                return null;
            }

            CubicCurve2D left = (CubicCurve2D) leftModel.getCurve();
            CubicCurve2D right = (CubicCurve2D) rightModel.getCurve();

            if (left == right) {
                curve = left;
            } else {
                double x1 = left.getX1();
                double y1 = left.getY1();
                double cx1 = left.getCtrlX1();
                double cy1 = left.getCtrlY1();
                double cx2 = right.getCtrlX2();
                double cy2 = right.getCtrlY2();
                double x2 = right.getX2();
                double y2 = right.getY2();

                // Compute affinity ratio out of mid point translation
                Point midPt = points.get(points.size() / 2); // Approximately
                double mx = (x1 + x2 + (3 * (cx1 + cx2))) / 8;
                double my = (y1 + y2 + (3 * (cy1 + cy2))) / 8;
                double deltaM = Math.hypot(midPt.x - mx, midPt.y - my);
                double pc = Math.hypot((cx1 + cx2) - (x1 + x2), (cy1 + cy2) - (y1 + y2)) / 2;
                double ratio = 1 + ((4 * deltaM) / (3 * pc));

                // Apply ratio on vectors to control points
                curve = new CubicCurve2D.Double(
                        x1,
                        y1,
                        x1 + (ratio * (cx1 - x1)), // cx1'
                        y1 + (ratio * (cy1 - y1)), // cy1'
                        x2 + (ratio * (cx2 - x2)), // cx2'
                        y2 + (ratio * (cy2 - y2)), // cy2'
                        x2,
                        y2);
            }
        }

        return curve;
    }

    //--------------//
    // getEndVector //
    //--------------//
    /**
     * Report the unit tangent vector at the item end designated by 'reverse' value.
     *
     * @param reverse true for first end, false for last
     * @return the tangent unit vector
     */
    public Point2D getEndVector (boolean reverse)
    {
        return getSideModel(reverse).getEndVector(reverse);
    }

    //-------------//
    // getMidPoint //
    //-------------//
    public Point2D getMidPoint ()
    {
        return model.getMidPoint();
    }

    //--------------//
    // getSideModel //
    //--------------//
    /**
     * Report the osculating model on the desired side.
     * Note that a small slur (a slur with not more than sideLength points) has just one global
     * model which is returned.
     *
     * @param reverse the desired side
     * @return the side model on desired side
     */
    public Model getSideModel (boolean reverse)
    {
        if (reverse) {
            if (firstModel == null) {
                if (getLength() <= sideLength) {
                    firstModel = model;
                } else {
                    firstModel = computeSideModel(points, reverse);
                }
            }

            return firstModel;
        } else {
            if (lastModel == null) {
                if (getLength() <= sideLength) {
                    lastModel = model;
                } else {
                    lastModel = computeSideModel(points, reverse);
                }
            }

            return lastModel;
        }
    }

    //---------------//
    // getSidePoints //
    //---------------//
    /**
     * Build the sequence of points needed to define a side model.
     *
     * @param reverse desired side
     * @return the sequence of defining points
     */
    public List<Point> getSidePoints (boolean reverse)
    {
        return getSidePoints(reverse, sideLength);
    }

    //---------------//
    // getSidePoints //
    //---------------//
    /**
     * Build the sequence of points needed to define a side model.
     *
     * @param reverse desired side
     * @param length  desired max number of points
     * @return the sequence of defining points
     */
    public List<Point> getSidePoints (boolean reverse,
                                      int length)
    {
        if (points.size() <= length) {
            return points;
        } else if (reverse) {
            return points.subList(0, length);
        } else {
            return points.subList(points.size() - length, points.size());
        }
    }

    //--------------//
    // hasSideModel //
    //--------------//
    /**
     * Report whether the slur has a specific model on the desired side.
     *
     * @param reverse desired side
     * @return true if there is indeed a side model, which is not the global one
     */
    public boolean hasSideModel (boolean reverse)
    {
        if (reverse) {
            return (firstModel != null) && (firstModel != model);
        } else {
            return (lastModel != null) && (lastModel != model);
        }
    }

    //--------------//
    // isHorizontal //
    //--------------//
    /**
     * Report whether slur is horizontal (rather than vertical)
     *
     * @return true if horizontal
     */
    public boolean isHorizontal ()
    {
        return horizontal;
    }

    //---------------//
    // setHorizontal //
    //---------------//
    /**
     * @param horizontal the horizontal to set
     */
    public void setHorizontal (boolean horizontal)
    {
        this.horizontal = horizontal;
    }

    //----------//
    // setModel //
    //----------//
    @Override
    public void setModel (Model model)
    {
        if (model != null) {
            super.setModel(model);
            above = model.above();
            bisUnit = computeBisector(above > 0);
        }
    }

    //---------------//
    // setSideModel //
    //---------------//
    public void setSideModel (Model model,
                              boolean reverse)
    {
        if (reverse) {
            firstModel = model;
        } else {
            lastModel = model;
        }
    }

    //-----------------//
    // computeBisector //
    //-----------------//
    /**
     * Compute bisector vector.
     *
     * @param above is the slur above (or below)
     * @return the unit bisector
     */
    protected Point2D.Double computeBisector (boolean above)
    {
        Line2D bisector = LineUtil.bisector(getEnd(above), getEnd(!above));
        double length = bisector.getP1().distance(bisector.getP2());

        return new Point2D.Double((bisector.getX2() - bisector.getX1()) / length, (bisector.getY2()
                                                                                           - bisector
                                          .getY1()) / length);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (horizontal != null) {
            sb.append(" ").append(horizontal ? "H" : "V");
        }

        return sb.toString();
    }
}
