//----------------------------------------------------------------------------//
//                                                                            //
//                                   L a g s                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

/**
 * Class {@code Lags} gathers info for lags.
 *
 * @author Hervé Bitteur
 */
public class Lags
{
    //~ Static fields/initializers ---------------------------------------------

    /** Horizontal (partial) lag. It complements vLag. */
    public static final String HLAG = "hLag";

    /** Vertical (partial) lag. It complements hLag. */
    public static final String VLAG = "vLag";

    /** Horizontal out-of-staves lag. */
    public static final String FULL_HLAG = "fullHLag";

    /** Spot lag. */
    public static final String SPOT_LAG = "spotLag";

    /** Head Lag. */
    public static final String HEAD_LAG = "headLag";

    /** Split lag. */
    public static final String SPLIT_LAG = "splitLag";

}
