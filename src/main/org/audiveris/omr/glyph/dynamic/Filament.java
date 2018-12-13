//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F i l a m e n t                                         //
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
package org.audiveris.omr.glyph.dynamic;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Comparator;

/**
 * Class {@code Filament} is the abstract basis for defining a growing long and thin
 * compound of sections.
 * <ul>
 * <li>{@link CurvedFilament} implements a perhaps wavy filament, for staff lines and ledgers
 * alignments.</li>
 * <li>{@link StraightFilament} implements a straight filament, for stems and legs of endings.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public abstract class Filament
        extends SectionCompound
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Filament.class);

    /**
     * For comparing Filament instances on their top ordinate.
     */
    public static final Comparator<Filament> topComparator = new Comparator<Filament>()
    {
        @Override
        public int compare (Filament f1,
                            Filament f2)
        {
            // Sort on top ordinate
            return Integer.signum(f1.getBounds().y - f2.getBounds().y);
        }
    };

    /** Absolute beginning point. */
    protected Point2D startPoint;

    /** Absolute ending point. */
    protected Point2D stopPoint;

    /** Scaling interline. */
    protected int interline;

    /**
     * Creates a new {@code Filament} object.
     *
     * @param interline scaling information
     */
    public Filament (int interline)
    {
        this.interline = interline;
    }

    /**
     * Force line (re)computation.
     */
    public abstract void computeLine ();

    //------------------//
    // getMeanCurvature //
    //------------------//
    /**
     * Report the average radius of curvature along all segments of the curve.
     * This is not a global radius, but rather a way to measure how straight the curve is.
     *
     * @return the average of radius measurements along all curve segments
     */
    public abstract double getMeanCurvature ();

    /**
     * Report the precise filament position for the provided coordinate.
     *
     * @param coord       the coordinate value (x for horizontal, y for vertical filament)
     * @param orientation the reference orientation
     * @return the pos value (y for horizontal filament, x for vertical filament)
     */
    public abstract double getPositionAt (double coord,
                                          Orientation orientation);

    /**
     * Report the slope with respect to desired orientation at the provided coordinate.
     *
     * @param coord       the coordinate value (x for horizontal, y for vertical)
     * @param orientation the reference orientation
     * @return the slope value (WRT x-axis for horizontal, y-axis for vertical)
     */
    public abstract double getSlopeAt (double coord,
                                       Orientation orientation);

    /**
     * Render the main guiding line of the compound, using the current foreground color.
     *
     * @param g          the graphic context
     * @param showPoints true to show the defining points
     * @param pointWidth width for any displayed defining point
     */
    public abstract void renderLine (Graphics2D g,
                                     boolean showPoints,
                                     double pointWidth);

    //----------//
    // getSlope //
    //----------//
    /**
     * Report the tangent of filament angle with abscissa axis.
     *
     * @return tangent of heading angle (dy/dx).
     */
    public double getSlope ()
    {
        return (stopPoint.getY() - startPoint.getY()) / (stopPoint.getX() - startPoint.getX());
    }

    //---------------//
    // getStartPoint //
    //---------------//
    /**
     * @return the startPoint
     */
    public Point2D getStartPoint ()
    {
        if (startPoint == null) {
            computeLine();
        }

        return startPoint;
    }

    //--------------//
    // getStopPoint //
    //--------------//
    /**
     * @return the stopPoint
     */
    public Point2D getStopPoint ()
    {
        if (stopPoint == null) {
            computeLine();
        }

        return stopPoint;
    }

    //-----------------//
    // setEndingPoints //
    //-----------------//
    /**
     * Assign the ending points.
     * <p>
     * This triggers a (re)-computing of line and bounds.
     *
     * @param startPoint the provided starting point
     * @param stopPoint  the providing stopping point
     */
    public void setEndingPoints (Point2D startPoint,
                                 Point2D stopPoint)
    {
        invalidateCache();
        this.startPoint = startPoint;
        this.stopPoint = stopPoint;

        computeLine();

        // Enlarge contour box if needed
        Rectangle box = getBounds();
        box.add(startPoint);
        box.add(stopPoint);
        setBounds(box);
    }

    //---------------------//
    // getRoughOrientation //
    //---------------------//
    /**
     * Report a (rough) orientation of the filament (vertical or horizontal)
     *
     * @return rough orientation
     */
    protected Orientation getRoughOrientation ()
    {
        checkBounds();

        return (bounds.height > bounds.width) ? Orientation.VERTICAL : Orientation.HORIZONTAL;
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    /**
     * Invalidate the cached data.
     */
    @Override
    protected void invalidateCache ()
    {
        super.invalidateCache();
        startPoint = stopPoint = null;
    }

    //---------------//
    // getProbeWidth //
    //---------------//
    /**
     * Report the width of the window used to determine filament position
     *
     * @return the scale-independent probe width
     */
    public static Scale.Fraction getProbeWidth ()
    {
        return constants.probeWidth;
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction probeWidth = new Scale.Fraction(
                0.5,
                "Width of probing window to retrieve filament ordinate");
    }
}
