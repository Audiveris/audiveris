//----------------------------------------------------------------------------//
//                                                                            //
//                        D u r a t i o n F a c t o r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.common;


/**
 * Class <code>DurationFactor</code> handles a rational representation of
 * duration modification
 *
 * @author Herv&eacute Bitteur
 */
public class DurationFactor
{
    //~ Instance fields --------------------------------------------------------

    /** Numerator */
    private final int num;

    /** Denominator */
    private final int den;

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
        this.num = num;
        this.den = den;
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getDenominator //
    //----------------//
    /**
     * Report the integer denominator of the factor
     *
     * @return the denominator
     */
    public int getDenominator ()
    {
        return den;
    }

    //--------------//
    // getNumerator //
    //--------------//
    /**
     * Report the integer numerator of the factor
     *
     * @return the numerator
     */
    public int getNumerator ()
    {
        return num;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return num + "/" + den;
    }
}
