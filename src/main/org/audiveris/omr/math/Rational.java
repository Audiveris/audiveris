//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        R a t i o n a l                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.math;

import java.math.BigInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class {@code Rational} implements non-mutable rational numbers
 * (composed of a numerator and a denominator).
 * <p>
 * Invariants:
 * <ol>
 * <li>The rational data is always kept in reduced form : gcd(num,den) == 1</li>
 * <li>The denominator value is always kept positive : den &ge; 1</li>
 * </ol>
 * <p>
 * It is (un)marshallable through JAXB.
 * </p>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "rational")
@XmlType(propOrder = {"num", "den"})
public class Rational
        extends Number
        implements Comparable<Rational>
{

    /** 0 rational instance. */
    public static final Rational ZERO = new Rational(0, 1);

    /** 1/4 rational value. */
    public static final Rational ONE_OVER_FOUR = new Rational(1, 4);

    /** 1/2 rational instance. */
    public static final Rational HALF = new Rational(1, 2);

    /** 1 rational instance. */
    public static final Rational ONE = new Rational(1, 1);

    /** 3/2 rational value. */
    public static final Rational THREE_OVER_TWO = new Rational(3, 2);

    /** Max rational value. */
    public static final Rational MAX_VALUE = new Rational(Integer.MAX_VALUE, 1);

    /** Final numerator value. */
    @XmlAttribute
    public final int num;

    /** Final denominator value. */
    @XmlAttribute
    public final int den;

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

    /** Needed for JAXB. */
    private Rational ()
    {
        num = den = 1;
    }

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
     * Comparison for total ordering.
     *
     * @param that the other rational instance
     * @return -1, 0, 1 if this &lt;, ==, &gt; that respectively
     */
    @Override
    public int compareTo (Rational that)
    {
        if (this == that) {
            return 0;
        }

        int a = this.num * that.den;
        int b = this.den * that.num;

        // Detect overflow, using the fact that den's are always >= 1
        if ((Integer.signum(b) != Integer.signum(that.num)) || (Integer.signum(a) != Integer.signum(
                this.num))) {
            BigInteger bigThisNum = BigInteger.valueOf(this.num);
            BigInteger bigThisDen = BigInteger.valueOf(this.den);
            BigInteger bigThatNum = BigInteger.valueOf(that.num);
            BigInteger bigThatDen = BigInteger.valueOf(that.den);
            BigInteger A = bigThisNum.multiply(bigThatDen);
            BigInteger B = bigThisDen.multiply(bigThatNum);

            return A.compareTo(B);
        } else {
            return Integer.compare(a, b);
        }
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

    //-------------//
    // doubleValue //
    //-------------//
    @Override
    public double doubleValue ()
    {
        return (double) num / den;
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

    //------------//
    // floatValue //
    //------------//
    @Override
    public float floatValue ()
    {
        return (float) doubleValue();
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
    // intValue //
    //----------//
    @Override
    public int intValue ()
    {
        return (int) Math.rint(doubleValue());
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

    //-----------//
    // longValue //
    //-----------//
    @Override
    public long longValue ()
    {
        return (long) Math.rint(doubleValue());
    }

    //-------//
    // minus //
    //-------//
    /**
     * Subtraction
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
     * Subtraction
     *
     * @param that the integer to subtract
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

        return new Rational((this.num * that.den) + (this.den * that.num), this.den * that.den);
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
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        if (den == 1) {
            return num + "";
        } else {
            return num + "/" + den;
        }
    }

    //--------//
    // decode //
    //--------//
    /**
     * Decode provided string as a rational value.
     *
     * @param str input string, such as "3/4" or "5" (equivalent of "5/1")
     * @return decoded rational value
     * @throws NumberFormatException if input string is invalid
     */
    public static Rational decode (String str)
    {
        final String[] tokens = str.split("\\s*/\\s*");

        switch (tokens.length) {
        case 2: {
            int num = Integer.decode(tokens[0].trim());
            int den = Integer.decode(tokens[1].trim());

            return new Rational(num, den);
        }
        case 1: {
            int num = Integer.decode(tokens[0].trim());
            return new Rational(num, 1);
        }
        default:
            throw new NumberFormatException(str);
        }
    }

    //-----//
    // gcd //
    //-----//
    /**
     * Compute the Greatest Common Divisor of two rational values.
     *
     * @param a a rational
     * @param b another rational
     * @return the GCD of a and b
     */
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
    /**
     * Compute the Greatest Common Divisor of several rational values.
     *
     * @param vals rational values
     * @return the GCD of all
     */
    public static Rational gcd (Rational... vals)
    {
        Rational s = Rational.ZERO;

        for (Rational val : vals) {
            s = gcd(s, val);
        }

        return s;
    }

    //---------//
    // Adapter //
    //---------//
    /**
     * JAXB adapter to un/marshal a Rational object as "num/den".
     */
    public static class Adapter
            extends XmlAdapter<String, Rational>
    {

        @Override
        public String marshal (Rational val)
                throws Exception
        {
            if (val == null) {
                return null;
            }

            return val.toString();
        }

        @Override
        public Rational unmarshal (String str)
                throws Exception
        {
            return decode(str);
        }
    }
}
