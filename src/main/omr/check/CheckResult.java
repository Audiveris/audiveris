//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C h e c k R e s u l t                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.check;

/**
 * Class {@code CheckResult} encapsulates the <b>result</b> of a check, composed of a
 * value (double) and a flag which can be RED, YELLOW or GREEN.
 *
 * @author Hervé Bitteur
 */
public class CheckResult
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Numerical result value. */
    public double value;

    /** The resulting grade, in 0..1 range. */
    public double grade;

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return String.format("%.2f(%.2f)", value, grade);
    }
}
