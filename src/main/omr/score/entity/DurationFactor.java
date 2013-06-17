//----------------------------------------------------------------------------//
//                                                                            //
//                        D u r a t i o n F a c t o r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.math.Rational;

/**
 * Class {@code DurationFactor} handles a rational representation of
 * duration modification
 *
 * @author Hervé Bitteur
 */
public class DurationFactor
        extends Rational
{
    //~ Instance fields --------------------------------------------------------

    /** Actual numerator value, generally 2 or 4. */
    public final int actualNum;

    /** Actual denominator value, generally 3 or 6. */
    public final int actualDen;

    //~ Constructors -----------------------------------------------------------
    //----------------//
    // DurationFactor //
    //----------------//
    /**
     * Creates a new instance of DurationFactor
     *
     * @param num numerator
     * @param den denominator
     */
    public DurationFactor (int num,
                           int den)
    {
        super(num, den);

        actualNum = num;
        actualDen = den;
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        if (actualDen == 1) {
            return actualNum + "";
        } else {
            return actualNum + "/" + actualDen;
        }
    }
}
