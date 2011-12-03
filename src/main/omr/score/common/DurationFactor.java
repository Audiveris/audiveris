//----------------------------------------------------------------------------//
//                                                                            //
//                        D u r a t i o n F a c t o r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.common;

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
    }
}
