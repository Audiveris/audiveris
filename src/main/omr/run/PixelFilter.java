//----------------------------------------------------------------------------//
//                                                                            //
//                           P i x e l F i l t e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
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

    /**
     * Initialize the various data elements needed by the source.
     */
    void initialize ();

    /**
     * Dispose of the various data elements of the source.
     */
    void dispose ();

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
     * Structure used to report precise context of the source.
     * It can be extended for more specialized data.
     */
    class Context
    {

        /** Threshold used on pixel value. */
        public final double threshold;

        public Context (double threshold)
        {
            this.threshold = threshold;
        }
    }

    /**
     * Report a management descriptor of this implementation.
     *
     * @return the related descriptor
     */
    FilterDescriptor getImplementationDescriptor ();
}
