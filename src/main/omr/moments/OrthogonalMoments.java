//----------------------------------------------------------------------------//
//                                                                            //
//                     O r t h o g o n a l M o m e n t s                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.moments;

/**
 * Interface {@code OrthogonalMoments} is a general definition for a
 * descriptor of orthogonal moments.
 *
 * @param <D> the descriptor type
 *
 * @author Hervé Bitteur
 */
public interface OrthogonalMoments<D extends OrthogonalMoments<D>>
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report the distance to another descriptor instance.
     *
     * @param that the other instance
     * @return the measured distance
     */
    double distanceTo (D that);

    /**
     * Report the moment for m and n orders.
     *
     * @param m m order
     * @param n n order
     * @return moments(m, n)
     */
    double getMoment (int m,
                      int n);

    /**
     * Assign the moment for m and n orders.
     *
     * @param m     m order
     * @param n     n order
     * @param value the moment value
     */
    void setMoment (int m,
                    int n,
                    double value);
    //    /**
    //     * Report a label for the (m,n) moment.
    //     * @param m m order
    //     * @param n n order
    //     * @return label for (m, n)
    //     */
    //    String getLabel (int m,
    //                     int n);
    //
}
