//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F i l a m e n t                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.dynamic;

import omr.constant.ConstantSet;

import omr.run.Orientation;

import omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Comparator;

/**
 * Class {@code Filament} is the abstract basis for defining a growing long & thin
 * compound of sections.
 * <ul>
 * <li>{@link CurvedFilament} implements a perhaps wavy filament, for staff lines and ledgers
 * alignments.</li>
 * <li>{@link StraightFilament} implements a straight filament, for stems and legs of endings.</li>
 * </ul>
 */
public abstract class Filament
        extends SectionCompound
{
    //~ Static fields/initializers -----------------------------------------------------------------

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

    //~ Instance fields ----------------------------------------------------------------------------
    /** Absolute beginning point. */
    protected Point2D startPoint;

    /** Absolute ending point. */
    protected Point2D stopPoint;

    /** Scaling interline. */
    protected int interline;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Filament} object.
     *
     * @param interline scaling information
     */
    public Filament (int interline)
    {
        this.interline = interline;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
    protected Orientation getRoughOrientation ()
    {
        checkBounds();

        return (bounds.height > bounds.width) ? Orientation.VERTICAL : Orientation.HORIZONTAL;
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    protected void invalidateCache ()
    {
        super.invalidateCache();
        startPoint = stopPoint = null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction probeWidth = new Scale.Fraction(
                0.5,
                "Width of probing window to retrieve filament ordinate");
    }
}
