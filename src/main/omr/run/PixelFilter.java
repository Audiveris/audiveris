//----------------------------------------------------------------------------//
//                                                                            //
//                           P i x e l F i l t e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

/**
 * Interface {@code PixelFilter} reports the foreground pixels of a
 * {@link PixelSource}.
 *
 * @author Hervé Bitteur
 */
public interface PixelFilter
        extends PixelSource
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report the source context at provided location.
     * This is meant for administration and display purposes, it does not need
     * to be very efficient.
     *
     * @param x abscissa value
     * @param y ordinate value
     * @return the contextual data at this location
     */
    Context getContext (int x,
                        int y);

    /**
     * Report whether the pixel at location (x,y) is a foreground pixel
     * or not.
     * It is assumed that this feature is efficiently implemented, since it will
     * be typically called several million times.
     *
     * @param x abscissa value
     * @param y ordinate value
     * @return true for a foreground pixel, false for a background pixel
     */
    boolean isFore (int x,
                    int y);

    //~ Inner Classes ----------------------------------------------------------
    /**
     * Structure used to report precise context of the source.
     * It can be extended for more specialized data.
     */
    class Context
    {
        //~ Instance fields ----------------------------------------------------

        /** Threshold used on pixel value. */
        public final double threshold;

        //~ Constructors -------------------------------------------------------
        public Context (double threshold)
        {
            this.threshold = threshold;
        }
    }
}
