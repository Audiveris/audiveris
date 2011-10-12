//----------------------------------------------------------------------------//
//                                                                            //
//                                  P e a k                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;


/**
 * Class <code>Peak</code> encapsulates a peak in the histogram of horizontal
 * projections. When reading sequentially the projection histogram, a peak is
 * created when the threshold is passed, and the peak ordinate is extended until
 * we get under the threshold again.
 *
 * @author Hervé Bitteur
 */
public class Peak
{
    //~ Instance fields --------------------------------------------------------

    /** Y value at top of the peak */
    private final int yTop;

    /** Histogram maximum within this peak */
    private int max;

    /** Y value at bottom of the peak */
    private int yBottom;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Peak //
    //------//
    /**
     * Create a peak, starting at ordinate 'y', for which we have 'val'
     * projections.
     *
     * @param y   ordinate value at the beginning of the peak
     * @param val number of pixels cumulated at 'y' ordinate
     */
    public Peak (int y,
                 int val)
    {
        yTop = y;
        yBottom = y; // To be increased later by peak extensions
        max = val; // To be increased later by peak extensions
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getBottom //
    //-----------//
    /**
     * Selector for last ordinate value
     *
     * @return bottom ordinate of the peak
     */
    public int getBottom ()
    {
        return yBottom;
    }

    //--------//
    // getMax //
    //--------//
    /**
     * Report the maximum histogram value across this peak
     *
     * @return the highest histo value
     */
    public int getMax ()
    {
        return max;
    }

    //--------//
    // getTop //
    //--------//
    /**
     * Selector for the ordinate value at the beginning of the peak
     *
     * @return y for top of the peak
     */
    public int getTop ()
    {
        return yTop;
    }

    //--------//
    // extend //
    //--------//
    /**
     * Continues a peak, extending its ordinate range, and perhaps its max value
     * if this projection is the highest one since the beginning of the peak.
     *
     * @param y   ordinate of this peak horizontal slice
     * @param val number of pixels cumulated at 'y' ordinate
     */
    public void extend (int y,
                        int val)
    {
        yBottom = y;
        max = Math.max(max, val);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a short description of this peak
     *
     * @return the description
     */
    @Override
    public String toString ()
    {
        return "{Peak " + yTop + "-" + yBottom + " max=" + max + "}";
    }
}
