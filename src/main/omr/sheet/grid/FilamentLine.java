//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    F i l a m e n t L i n e                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.lag.Section;

import omr.math.GeoPath;
import omr.math.Line;
import omr.math.LineUtil;

import omr.run.Orientation;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Class {@code FilamentLine} implements a staff line (or a part of it), based on
 * filaments.
 *
 * @author Hervé Bitteur
 */
public class FilamentLine
        implements LineInfo
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Underlying filament. */
    LineFilament fil;

    /** Imposed ending points, if any. */
    private final Map<HorizontalSide, Point2D> endPoints = new EnumMap<HorizontalSide, Point2D>(
            HorizontalSide.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new FilamentLine object.
     *
     * @param fil the initial filament to add
     */
    public FilamentLine (LineFilament fil)
    {
        add(fil);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----//
    // add //
    //-----//
    public final void add (LineFilament fil)
    {
        if (this.fil == null) {
            this.fil = fil;
        } else {
            this.fil.include(fil);
        }
    }

    //-----------//
    // checkLine //
    //-----------//
    public void checkLine ()
    {
        if (!fil.isLineDefined()) {
            fil.setEndingPoints(endPoints.get(LEFT), endPoints.get(RIGHT));
        }
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        return fil.getBounds();
    }

    //-------------//
    // getEndPoint //
    //-------------//
    @Override
    public Point2D getEndPoint (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return getStartPoint();
        } else {
            return getStopPoint();
        }
    }

    //-------------//
    // getFilament //
    //-------------//
    public LineFilament getFilament ()
    {
        return fil;
    }

    //-------//
    // getId //
    //-------//
    @Override
    public int getId ()
    {
        return fil.getId();
    }

    //--------------//
    // getLeftPoint //
    //--------------//
    @Override
    public Point2D getLeftPoint ()
    {
        return getStartPoint();
    }

    //---------------//
    // getRightPoint //
    //---------------//
    @Override
    public Point2D getRightPoint ()
    {
        return getStopPoint();
    }

    //-------------//
    // getSections //
    //-------------//
    @Override
    public Collection<Section> getSections ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //----------//
    // getSlope //
    //----------//
    public double getSlope (HorizontalSide side)
    {
        return fil.slopeAt(getEndPoint(side).getX(), Orientation.HORIZONTAL);
    }

    //---------------//
    // getStartPoint //
    //---------------//
    public Point2D getStartPoint ()
    {
        return fil.getStartPoint(Orientation.HORIZONTAL);
    }

    //---------------//
    // getStartSlope //
    //---------------//
    public double getStartSlope ()
    {
        return fil.slopeAt(getStartPoint().getX(), Orientation.HORIZONTAL);
    }

    //--------------//
    // getStopPoint //
    //--------------//
    public Point2D getStopPoint ()
    {
        return fil.getStopPoint(Orientation.HORIZONTAL);
    }

    //--------------//
    // getStopSlope //
    //--------------//
    public double getStopSlope ()
    {
        return fil.slopeAt(getStopPoint().getX(), Orientation.HORIZONTAL);
    }

    //---------//
    // include //
    //---------//
    public void include (FilamentLine that)
    {
        add(that.fil);
    }

    //---------------//
    // isWithinRange //
    //---------------//
    /**
     * Report whether the provided abscissa lies within the line range
     *
     * @param x the provided abscissa
     * @return true if within range
     */
    public boolean isWithinRange (double x)
    {
        return (x >= getStartPoint().getX()) && (x <= getStopPoint().getX());
    }

    //--------//
    // render //
    //--------//
    @Override
    public void render (Graphics2D g)
    {
        fil.renderLine(g);
    }

    //-----------------//
    // setEndingPoints //
    //-----------------//
    public void setEndingPoints (Point2D pStart,
                                 Point2D pStop)
    {
        endPoints.put(LEFT, pStart);
        endPoints.put(RIGHT, pStop);
        fil.setEndingPoints(pStart, pStop);
    }

    //--------//
    // toPath //
    //--------//
    @Override
    public GeoPath toPath ()
    {
        return fil.getLine().toPath();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Line#");
        sb.append(fil.getClusterPos());
        sb.append("[");

        sb.append("F").append(fil.getId());

        sb.append("]");

        sb.append(fil.trueLength());

        return sb.toString();
    }

    //----------------------//
    // verticalIntersection //
    //----------------------//
    @Override
    public Point2D verticalIntersection (Line vertical)
    {
        // We need two points on the rather vertical line
        Point2D startPoint = new Point2D.Double(vertical.xAtY(0.0), 0.0);
        Point2D stopPoint = new Point2D.Double(vertical.xAtY(1000.0), 1000.0);

        // First, get a rough intersection
        Point2D pt = LineUtil.intersection(
                getEndPoint(LEFT),
                getEndPoint(RIGHT),
                startPoint,
                stopPoint);

        // Second, get a precise ordinate
        double y = yAt(pt.getX());

        // Third, get a precise abscissa
        double x = vertical.xAtY(y);

        return new Point2D.Double(x, y);
    }

    //-----//
    // yAt //
    //-----//
    @Override
    public int yAt (int x)
    {
        return (int) Math.rint(yAt((double) x));
    }

    //-----//
    // yAt //
    //-----//
    @Override
    public double yAt (double x)
    {
        return fil.getPositionAt(x, Orientation.HORIZONTAL);
    }
}
