//----------------------------------------------------------------------------//
//                                                                            //
//                       L e g e n d r e M o m e n t s                        //
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
 * Class {@code LegendreMoments} defines a descriptor for orthogonal
 * Legendre moments.
 *
 * @author Hervé Bitteur
 */
public interface LegendreMoments
        extends OrthogonalMoments<LegendreMoments>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Chosen polynomial order. */
    public static final int ORDER = 10;

}
