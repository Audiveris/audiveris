//----------------------------------------------------------------------------//
//                                                                            //
//                       P o i n t s C o l l e c t o r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.awt.Rectangle;
import java.util.Arrays;

/**
 * Class {@code PointsCollector} is meant to cumulate points
 * coordinates, perhaps within a provided <b>absolute</b> region of
 * interest.
 */
public class PointsCollector
{
    //~ Instance fields --------------------------------------------------------

    /** The absolute region of interest, if any */
    private Rectangle roi;

    /** The current number of points in this collector */
    private int size;

    /** The abscissae */
    private int[] xx;

    /** The ordinates */
    private int[] yy;

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // PointsCollector //
    //-----------------//
    /**
     * Creates a new PointsCollector object, with absolute roi area
     * taken as capacity.
     *
     * @param roi the absolute roi to be used by the collector
     */
    public PointsCollector (Rectangle roi)
    {
        this(roi, roi.width * roi.height);
    }

    //-----------------//
    // PointsCollector //
    //-----------------//
    /**
     * Creates a new PointsCollector object.
     *
     * @param roi      the absolute roi to be used by the collector
     * @param capacity the collector capacity
     */
    public PointsCollector (Rectangle roi,
                            int capacity)
    {
        this.roi = roi;
        xx = new int[capacity];
        yy = new int[capacity];
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // getSize //
    //----------//
    /**
     * Report the current number of points collected.
     *
     * @return the current number of points
     */
    public final int getSize ()
    {
        return size;
    }

    //--------//
    // getRoi //
    //--------//
    /**
     * Report the absolute region of interest for this collector
     *
     * @return the related ROI if any, null otherwise
     */
    public Rectangle getRoi ()
    {
        return roi;
    }

    //------------//
    // getXValues //
    //------------//
    /**
     * Report the current abscissae.
     *
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
     * Report the current ordinates.
     *
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
     * Include one point (while increasing capacity if needed).
     *
     * @param x point abscissa
     * @param y point ordinate
     */
    public final void include (int x,
                               int y)
    {
        ensureCapacity(size + 1);
        xx[size] = x;
        yy[size] = y;
        size++;
    }

    //----------------//
    // ensureCapacity //
    //----------------//
    /**
     * Increases the capacity of this instance.
     *
     * @param minCapacity the desired minimum capacity
     */
    public void ensureCapacity (int minCapacity)
    {
        int oldCapacity = xx.length;

        if (minCapacity > oldCapacity) {
            int newCapacity = ((oldCapacity * 3) / 2) + 1;

            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }

            xx = Arrays.copyOf(xx, newCapacity);
            yy = Arrays.copyOf(yy, newCapacity);
        }
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
                .append(" size:")
                .append(size);

        sb.append("}");

        return sb.toString();
    }
}
