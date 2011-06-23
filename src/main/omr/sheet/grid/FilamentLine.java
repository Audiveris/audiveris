//----------------------------------------------------------------------------//
//                                                                            //
//                          F i l a m e n t L i n e                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.glyph.GlyphSection;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.util.HorizontalSide;

import java.awt.Graphics2D;
import java.util.*;

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

    /** Unique id */
    private int id;

    /** Relative position within the cluster */
    final int pos;

    /** Underlying filament */
    LineFilament fil;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // FilamentLine //
    //--------------//
    /**
     * Creates a new FilamentLine object.
     *
     * @param id the assigned id
     * @param pos the relative position within the line cluster
     */
    public FilamentLine (int id,
                         int pos)
    {
        this.id = id;
        this.pos = pos;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getCenter //
    //-----------//
    public PixelPoint getCenter ()
    {
        PixelPoint left = getStartPoint();
        PixelPoint right = getStopPoint();

        return new PixelPoint((left.x + right.x) / 2, (left.y + right.y) / 2);
    }

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
    public PixelPoint getEndPoint (HorizontalSide side)
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
    public void setEndingPoints (PixelPoint pStart,
                                 PixelPoint pStop)
    {
        fil.setEndingPoints(pStart, pStop);
    }

    //-------//
    // getId //
    //-------//
    public int getId ()
    {
        return id;
    }

    //--------------//
    // getLeftPoint //
    //--------------//
    public PixelPoint getLeftPoint ()
    {
        return getStartPoint();
    }

    //---------------//
    // getRightPoint //
    //---------------//
    public PixelPoint getRightPoint ()
    {
        return getStopPoint();
    }

    //-------------//
    // getSections //
    //-------------//
    public Collection<GlyphSection> getSections ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //----------//
    // getSlope //
    //----------//
    public double getSlope (HorizontalSide side)
    {
        PixelPoint pt = getEndPoint(side);

        return fil.slopeAt(pt.x);
    }

    //---------------//
    // getStartPoint //
    //---------------//
    public PixelPoint getStartPoint ()
    {
        return fil.getStartPoint();
    }

    //---------------//
    // getStartSlope //
    //---------------//
    public double getStartSlope ()
    {
        PixelPoint pt = getStartPoint();

        return fil.slopeAt(pt.x);
    }

    //--------------//
    // getStopPoint //
    //--------------//
    public PixelPoint getStopPoint ()
    {
        return fil.getStopPoint();
    }

    //--------------//
    // getStopSlope //
    //--------------//
    public double getStopSlope ()
    {
        PixelPoint pt = getStopPoint();

        return fil.slopeAt(pt.x);
    }

    //---------------//
    // isWithinRange //
    //---------------//
    /**
     * Report wheter the provided abscissa lies within the line range
     * @param x the provided abscissa
     * @return true if within range
     */
    public boolean isWithinRange (int x)
    {
        return (x >= getStartPoint().x) && (x <= getStopPoint().x);
    }

    //-----//
    // add //
    //-----//
    public void add (LineFilament fil)
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
        sb.append(pos);
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
        return fil.positionAt(x);
    }
}
