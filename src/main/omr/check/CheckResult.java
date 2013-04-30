//----------------------------------------------------------------------------//
//                                                                            //
//                           C h e c k R e s u l t                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.check;


/**
 * Class {@code CheckResult} encapsulates the <b>result</b> of a check,
 * composed of a value (double) and a flag which can be RED, YELLOW or GREEN.
 *
 * @author Hervé Bitteur
 */
public class CheckResult
{
    //~ Instance fields --------------------------------------------------------

    /** Numerical result value */
    public double value;

    /** Flag the result (RED, YELLOW, GREEN) */
    public int flag;
}
