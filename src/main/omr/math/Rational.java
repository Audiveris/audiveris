//----------------------------------------------------------------------------//
//                                                                            //
//                              R a t i o n a l                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.math.BigInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Rational} implements non-mutable rational numbers
 * (composed of a numerator and a denominator).
 *
 * <p>Invariants:<ol>
 * <li>The rational data is always kept in reduced form : gcd(num,den)==1</li>
 * <li>The denominator value is always kept positive : den >= 1</li>
 * </ol></p>
 *
 * <p>It is (un)marshallable through JAXB.</p>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "rational")
public class Rational
        implements Comparable<Rational>
{
    //~ Static fields/initializers ---------------------------------------------

    /** The zero rational instance */
    public static final Rational ZERO = new Rational(0, 1);

    /** The one rational instance */
    public static final Rational ONE = new Rational(1, 1);

    /** Max rational value */
    public static final Rational MAX_VALUE = new Rational(Integer.MAX_VALUE, 1);

    //~ Instance fields --------------------------------------------------------
    /** Final denominator value */
    @XmlAttribute
    public final int den;

    /** Final numerator value */
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
     * @throws IllegalArgumentException if the provided denominator is zero
     */
    public Rational (int num,
                     int den)
    {
        if (den == 0) {
            throw new IllegalArgumentException("Denominator is zero");
        }

        // Reduction
        int gcd = GCD.gcd(num, den);
        num /= gcd;
        den /= gcd;

        // Positive denominator
        if (den < 0) {
            den = -den;
            num = -num;
        }

        // Record final values
        this.num = num;
        this.den = den;
    }

    //----------//
    // Rational //
    //----------//
    /** Needed for JAXB */
    private Rational ()
    {
        num = den = 1;
    }

    //~ Methods ----------------------------------------------------------------
    //-----//
    // abs //
    //-----//
    /**
     * Report the absolute value
     *
     * @return |num| / den
     */
    public Rational abs ()
    {
        return new Rational(Math.abs(num), den);
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Comparison
     *
     * @param that the other rational instance
     * @return -1,0,1 if this <,==,> that respectively
     */
    @Override
    public int compareTo (Rational that)
    {
        int a = this.num * that.den;
        int b = this.den * that.num;

        // Detect overflow, using the fact that den's are always >= 1
        if ((Integer.signum(b) != Integer.signum(that.num))
            || (Integer.signum(a) != Integer.signum(this.num))) {
            BigInteger bigThisNum = BigInteger.valueOf(this.num);
            BigInteger bigThisDen = BigInteger.valueOf(this.den);
            BigInteger bigThatNum = BigInteger.valueOf(that.num);
            BigInteger bigThatDen = BigInteger.valueOf(that.den);
            BigInteger A = bigThisNum.multiply(bigThatDen);
            BigInteger B = bigThisDen.multiply(bigThatNum);

            return A.compareTo(B);
        } else {
            return Integer.signum(a - b);
        }

        ///return Integer.signum((this.num * that.den) - (this.den * that.num));
    }

    //---------//
    // divides //
    //---------//
    /**
     * Division
     *
     * @param that the other rational instance
     * @return this / that
     */
    public Rational divides (Rational that)
    {
        return times(that.inverse());
    }

    //---------//
    // divides //
    //---------//
    /**
     * Division
     *
     * @param that the integer to divide by
     * @return this / that
     */
    public Rational divides (int that)
    {
        return new Rational(num, den * that);
    }

    //--------//
    // equals //
    //--------//
    /**
     * Identity
     *
     * @param obj the instance to compare to
     * @return true if this value equals that value
     */
    @Override
    public boolean equals (Object obj)
    {
        if (!(obj instanceof Rational)) {
            return false;
        } else {
            return compareTo((Rational) obj) == 0;
        }
    }

    //-----//
    // gcd //
    //-----//
    public static Rational gcd (Rational a,
                                Rational b)
    {
        if (a.num == 0) {
            return b;
        } else {
            return new Rational(1, GCD.lcm(a.den, b.den));
        }
    }

    //-----//
    // gcd //
    //-----//
    public static Rational gcd (Rational... vals)
    {
        Rational s = Rational.ZERO;

        for (Rational val : vals) {
            s = gcd(s, val);
        }

        return s;
    }

    //----------//
    // hashCode //
    //----------//
    /** {@inheritDoc } */
    @Override
    public int hashCode ()
    {
        int hash = 5;
        hash = (89 * hash) + den;
        hash = (89 * hash) + num;

        return hash;
    }

    //---------//
    // inverse //
    //---------//
    /**
     * Unary inversion
     *
     * @return 1 / this
     */
    public Rational inverse ()
    {
        return new Rational(den, num);
    }

    //-------//
    // minus //
    //-------//
    /**
     * Substraction
     *
     * @param that the other rational instance
     * @return this - that
     */
    public Rational minus (Rational that)
    {
        return plus(that.opposite());
    }

    //-------//
    // minus //
    //-------//
    /**
     * Substraction
     *
     * @param that the integer to substract
     * @return this - that
     */
    public Rational minus (int that)
    {
        return plus(-that);
    }

    //----------//
    // opposite //
    //----------//
    /**
     * Unary negation
     *
     * @return -this
     */
    public Rational opposite ()
    {
        return new Rational(-num, den);
    }

    //------//
    // plus //
    //------//
    /**
     * Addition
     *
     * @param that the other rational instance
     * @return this + that
     */
    public Rational plus (Rational that)
    {
        if (this.equals(ZERO)) {
            return that;
        }

        if (that.equals(ZERO)) {
            return this;
        }

        return new Rational(
                (this.num * that.den) + (this.den * that.num),
                this.den * that.den);
    }

    //------//
    // plus //
    //------//
    /**
     * Addition
     *
     * @param that the integer to add
     * @return this + that
     */
    public Rational plus (int that)
    {
        return plus(new Rational(that, 1));
    }

    //-------//
    // times //
    //-------//
    /**
     * Multiplication
     *
     * @param that the other rational instance
     * @return this * that
     */
    public Rational times (Rational that)
    {
        return new Rational(this.num * that.num, this.den * that.den);
    }

    //-------//
    // times //
    //-------//
    /**
     * Multiplication
     *
     * @param that the integer to multiply by
     * @return this * that
     */
    public Rational times (int that)
    {
        return new Rational(num * that, den);
    }

    //----------//
    // toDouble //
    //----------//
    public double toDouble ()
    {
        return (double) num / den;
    }

    //----------//
    // toString //
    //----------//
    /** {@inheritDoc } */
    @Override
    public String toString ()
    {
        if (den == 1) {
            return num + "";
        } else {
            return num + "/" + den;
        }
    }
}
