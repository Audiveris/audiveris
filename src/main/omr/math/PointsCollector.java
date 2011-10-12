//----------------------------------------------------------------------------//
//                                                                            //
//                       P o i n t s C o l l e c t o r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import omr.score.common.PixelRectangle;

/**
 * Class {@code PointsCollector} is meant to cumulate points coordinates,
 * perhaps within a provided <b>absolute</b> region of interest.
 */
public class PointsCollector
{
    //~ Instance fields --------------------------------------------------------

    /** The absolute region of interest, if any */
    private PixelRectangle roi;

    /** The current number of points in this collector */
    private int count;

    /** The abscissae */
    private int[] xx;

    /** The ordinates */
    private int[] yy;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // PointsCollector //
    //-----------------//
    /**
     * Creates a new PointsCollector object, with absolute roi area taken as
     * capacity
     * @param roi the absolute roi to be used by the collector
     */
    public PointsCollector (PixelRectangle roi)
    {
        this(roi, roi.width * roi.height);
    }

    //-----------------//
    // PointsCollector //
    //-----------------//
    /**
     * Creates a new PointsCollector object.
     * @param roi the absolute roi to be used by the collector
     * @param capacity the collector capacity
     */
    public PointsCollector (PixelRectangle roi,
                            int            capacity)
    {
        this.roi = roi;
        xx = new int[capacity];
        yy = new int[capacity];
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getCount //
    //----------//
    public final int getCount ()
    {
        return count;
    }

    //--------//
    // getRoi //
    //--------//
    public PixelRectangle getRoi ()
    {
        return roi;
    }

    //------------//
    // getXValues //
    //------------//
    /**
     * Report the current abscissae
     * @return sequence of abscissae
     */
    public final int[] getXValues ()
    {
        return xx;
    }

    //------------//
    // getYValues //
    //------------//
    /**
     * Report the current ordinates
     * @return sequence of ordinates
     */
    public final int[] getYValues ()
    {
        return yy;
    }

    //---------//
    // include //
    //---------//
    /**
     * Include one point
     * @param x point abscissa
     * @param y point ordinate
     * @throws ArrayIndexOutOfBoundsException
     */
    public final void include (int x,
                               int y)
    {
        xx[count] = x;
        yy[count] = y;
        count++;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{")
          .append(getClass().getSimpleName())
          .append(" count:")
          .append(count);

        sb.append("}");

        return sb.toString();
    }
}
