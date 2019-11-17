//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S t r a i g h t F i l a m e n t                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.glyph.dynamic;

import org.audiveris.omr.glyph.NearLine;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.math.BasicLine;
import org.audiveris.omr.math.Line;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.run.Orientation;
import static org.audiveris.omr.run.Orientation.HORIZONTAL;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Class {@code StraightFilament} is a filament of sections, expected to be sufficiently
 * straight to be represented as a basic line.
 *
 * @author Hervé Bitteur
 */
public class StraightFilament
        extends Filament
        implements NearLine
{

    /** The approximating line. */
    protected BasicLine line;

    /**
     * Creates a new {@code StraightFilament} object.
     *
     * @param interline scaling information
     */
    public StraightFilament (int interline)
    {
        super(interline);
    }

    //-------------//
    // computeLine //
    //-------------//
    @Override
    public void computeLine ()
    {
        line = new BasicLine();

        for (Section section : getMembers()) {
            Line sectionLine = section.getAbsoluteLine();

            if (sectionLine != null) {
                line.includeLine(sectionLine);
            } else {
                // Just 1 point
                Point cp = new Point(section.getStartCoord(), section.getFirstPos());
                line.includePoint(section.getOrientation().absolute(cp));
            }
        }

        Rectangle box = getBounds();

        // We have a problem if compound is just 1 pixel: no computable slope!
        if (getWeight() <= 1) {
            startPoint = stopPoint = box.getLocation();

            return;
        }

        double top = box.y;
        double bot = (box.y + box.height) - 1;
        double left = box.x;
        double right = (box.x + box.width) - 1;

        if (getRoughOrientation().isVertical()) {
            // Use line intersections with top & bottom box sides
            startPoint = new Point2D.Double(line.xAtY(top), top);
            stopPoint = new Point2D.Double(line.xAtY(bot), bot);
        } else {
            // Use line intersections with left & right box sides
            startPoint = new Point2D.Double(left, line.yAtX(left));
            stopPoint = new Point2D.Double(right, line.yAtX(right));
        }
    }

    //--------------//
    // getBasicLine //
    //--------------//
    /**
     * Return the approximating line as a BasicLine
     *
     * @return the BasicLine instance
     */
    public BasicLine getBasicLine ()
    {
        checkLine();

        return line;
    }

    //------------------//
    // getInvertedSlope //
    //------------------//
    @Override
    public double getInvertedSlope ()
    {
        checkLine();

        return LineUtil.getInvertedSlope(line.toDouble());
    }

    //---------//
    // getLine //
    //---------//
    @Override
    public Line2D getLine ()
    {
        checkLine();

        return line.toDouble();
    }

    //------------------//
    // getMeanCurvature //
    //------------------//
    @Override
    public double getMeanCurvature ()
    {
        return Double.POSITIVE_INFINITY;
    }

    //-----------------//
    // getMeanDistance //
    //-----------------//
    @Override
    public double getMeanDistance ()
    {
        checkLine();

        return line.getMeanDistance();
    }

    //---------------//
    // getPositionAt //
    //---------------//
    @Override
    public double getPositionAt (double coord,
                                 Orientation orientation)
    {
        if (orientation == HORIZONTAL) {
            return getBasicLine().yAtX(coord);
        } else {
            return getBasicLine().xAtY(coord);
        }
    }

    //---------------//
    // getCenterLine //
    //---------------//
    @Override
    public Line2D getCenterLine ()
    {
        checkLine();

        return line.toCenterLine();
    }

    //------------//
    // getSlopeAt //
    //------------//
    @Override
    public double getSlopeAt (double coord,
                              Orientation orientation)
    {
        checkLine();

        if (orientation == Orientation.HORIZONTAL) {
            return (stopPoint.getY() - startPoint.getY()) / (stopPoint.getX() - startPoint.getX());
        } else {
            return (stopPoint.getX() - startPoint.getX()) / (stopPoint.getY() - startPoint.getY());
        }
    }

    //---------------//
    // getStartPoint //
    //---------------//
    @Override
    public Point2D getStartPoint (Orientation orientation)
    {
        if (startPoint == null) {
            computeLine();
        }

        if (orientation == getRoughOrientation()) {
            return startPoint;
        }

        throw new IllegalArgumentException("Orientation does not match");
    }

    //--------------//
    // getStopPoint //
    //--------------//
    @Override
    public Point2D getStopPoint (Orientation orientation)
    {
        if (stopPoint == null) {
            computeLine();
        }

        if (orientation == getRoughOrientation()) {
            return stopPoint;
        }

        throw new IllegalArgumentException("Orientation does not match");
    }

    //------------//
    // renderLine //
    //------------//
    @Override
    public void renderLine (Graphics2D g,
                            boolean showPoints,
                            double pointWidth)
    {
        Rectangle clip = g.getClipBounds();

        if ((clip == null) || clip.intersects(getBounds())) {
            checkLine();

            if (startPoint != null) {
                g.draw(getCenterLine());

                // Then the absolute defining points?
                if (showPoints) {
                    double r = pointWidth / 2; // Point radius
                    Ellipse2D ellipse = new Ellipse2D.Double();

                    for (Point2D p : new Point2D[]{startPoint, stopPoint}) {
                        ellipse.setFrame(p.getX() - r, p.getY() - r, 2 * r, 2 * r);
                        g.fill(ellipse);
                    }
                }
            }
        }
    }

    //------------//
    // renderLine //
    //------------//
    @Override
    public void renderLine (Graphics2D g)
    {
        renderLine(g, false, 0);
    }

    //-----------//
    // checkLine //
    //-----------//
    /**
     * Make sure the approximating line is available
     */
    private void checkLine ()
    {
        if (line == null) {
            computeLine();
        }
    }

    //-------------//
    // Constructor //
    //-------------//
    /**
     * Kind of 'constructor' for a StraightFilament.
     */
    public static class Constructor
            implements CompoundFactory.CompoundConstructor
    {

        private final int interline;

        /**
         * Create the constructor.
         *
         * @param interline the related interline
         */
        public Constructor (int interline)
        {
            this.interline = interline;
        }

        /**
         * Create a new instance of StraightFilament.
         *
         * @return the new instance
         */
        @Override
        public SectionCompound newInstance ()
        {
            return new StraightFilament(interline);
        }
    }
}
