//----------------------------------------------------------------------------//
//                                                                            //
//                          F i l a m e n t L i n e                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.lag.Section;

import omr.run.Orientation;

import omr.score.common.PixelRectangle;

import omr.util.HorizontalSide;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Collection;

/**
 * Class {@code FilamentLine} implements a staff line (or a part of it),
 * based on filaments
 *
 * @author Hervé Bitteur
 */
public class FilamentLine
    implements LineInfo
{
    //~ Instance fields --------------------------------------------------------

    /** Underlying filament */
    LineFilament fil;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // FilamentLine //
    //--------------//
    /**
     * Creates a new FilamentLine object.
     * @param fil the initial filament to add
     */
    public FilamentLine (LineFilament fil)
    {
        add(fil);
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getContourBox //
    //---------------//
    public PixelRectangle getContourBox ()
    {
        return fil.getContourBox();
    }

    //-------------//
    // getEndPoint //
    //-------------//
    public Point2D getEndPoint (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return getStartPoint();
        } else {
            return getStopPoint();
        }
    }

    //-----------------//
    // setEndingPoints //
    //-----------------//
    public void setEndingPoints (Point2D pStart,
                                 Point2D pStop)
    {
        fil.setEndingPoints(pStart, pStop);
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
    public int getId ()
    {
        return fil.getId();
    }

    //--------------//
    // getLeftPoint //
    //--------------//
    public Point2D getLeftPoint ()
    {
        return getStartPoint();
    }

    //---------------//
    // getRightPoint //
    //---------------//
    public Point2D getRightPoint ()
    {
        return getStopPoint();
    }

    //-------------//
    // getSections //
    //-------------//
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

    //---------------//
    // isWithinRange //
    //---------------//
    /**
     * Report whether the provided abscissa lies within the line range
     * @param x the provided abscissa
     * @return true if within range
     */
    public boolean isWithinRange (double x)
    {
        return (x >= getStartPoint()
                         .getX()) && (x <= getStopPoint()
                                               .getX());
    }

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

    //---------//
    // cleanup //
    //---------//
    public void cleanup ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //---------//
    // include //
    //---------//
    public void include (FilamentLine that)
    {
        add(that.fil);
    }

    //--------//
    // render //
    //--------//
    public void render (Graphics2D g,
                        int        left,
                        int        right)
    {
        // Ignore left and right
        render(g);
    }

    //--------//
    // render //
    //--------//
    public void render (Graphics2D g)
    {
        fil.renderLine(g);
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

        sb.append("F")
          .append(fil.getId());

        sb.append("]");

        sb.append(fil.trueLength());

        return sb.toString();
    }

    //-----//
    // yAt //
    //-----//
    public int yAt (int x)
    {
        return (int) Math.rint(yAt((double) x));
    }

    //-----//
    // yAt //
    //-----//
    public double yAt (double x)
    {
        return fil.getPositionAt(x, Orientation.HORIZONTAL);
    }
}
