//----------------------------------------------------------------------------//
//                                                                            //
//                              R a t i o n a l                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import javax.xml.bind.annotation.*;

/**
 * Class <code>Rational</code> hosts a pair composed of a numerator and a
 * denominator
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "rational")
public class Rational
{
    //~ Instance fields --------------------------------------------------------

    /** Denominator value */
    @XmlAttribute
    public final int den;

    /** Numerator value */
    @XmlAttribute
    public final int num;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // Rational //
    //----------//
    /**
     * Create a final Rational instance
     *
     * @param num numerator value
     * @param den denominator value
     */
    public Rational (int num,
                     int den)
    {
        this.num = num;
        this.den = den;
    }

    //----------//
    // Rational //
    //----------//
    /**
     * Clone a Rational instance
     *
     * @param rational the rational to clone
     */
    public Rational (Rational rational)
    {
        this.num = rational.num;
        this.den = rational.den;
    }

    //----------//
    // Rational //
    //----------//
    /** Needed for JAXB */
    private Rational ()
    {
        num = den = 0;
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (!(obj instanceof Rational)) {
            return false;
        }

        Rational r = (Rational) obj;

        return (this.num == r.num) && (this.den == r.den);
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 5;
        hash = (89 * hash) + den;
        hash = (89 * hash) + num;

        return hash;
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
