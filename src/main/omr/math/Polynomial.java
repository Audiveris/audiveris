//----------------------------------------------------------------------------//
//                                                                            //
//                            P o l y n o m i a l                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

/**
 * Class {@code Polynomial} is a simple polynomial implementation.
 *
 * See http://introcs.cs.princeton.edu/java/92symbolic/Polynomial.java.html
 *
 * @author Hervé Bitteur
 */
public class Polynomial
{
    //~ Instance fields --------------------------------------------------------

    /** The degree of polynomial */
    protected int degree;

    /** Polynomial coefficient vector, from low to high order */
    protected double[] coefficients;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // Polynomial //
    //------------//
    /**
     * Creates a new Polynomial object (actually just a monomial).
     * Example new Polynomial(3,2) = 3x^2
     *
     * @param c      coefficient
     * @param degree degree of the monomial term
     */
    public Polynomial (double c,
                       int degree)
    {
        if (degree < 0) {
            throw new IllegalArgumentException("Negative polynomial degree");
        }

        coefficients = new double[degree + 1];
        coefficients[degree] = c;
        this.degree = degree();
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // compose //
    //---------//
    /**
     * Compose with that other polynomial.
     *
     * @param that the other polynomial
     * @return a(b(x))
     */
    public Polynomial compose (Polynomial that)
    {
        Polynomial result = new Polynomial(0, 0);

        for (int i = this.degree; i >= 0; i--) {
            Polynomial term = new Polynomial(this.coefficients[i], 0);
            result = term.plus(that.times(result));
        }

        return result;
    }

    //--------//
    // degree //
    //--------//
    /**
     * Report the actual degree.
     *
     * @return the degree of this polynomial (0 for the zero polynomial)
     */
    public final int degree ()
    {
        for (int i = coefficients.length - 1; i >= 0; i--) {
            if (coefficients[i] != 0) {
                return i;
            }
        }

        return 0;
    }

    //------------//
    // derivative //
    //------------//
    /**
     * Report the derivative of this polynomial.
     *
     * @return the derivative
     */
    public Polynomial derivative ()
    {
        if (degree == 0) {
            return new Polynomial(0, 0);
        }

        Polynomial derivative = new Polynomial(0, degree - 1);
        derivative.degree = degree - 1;

        for (int i = 0; i < degree; i++) {
            derivative.coefficients[i] = (i + 1) * coefficients[i + 1];
        }

        return derivative;
    }

    //----//
    // eq //
    //----//
    /**
     * Check whether this represent the same polynomial as that.
     *
     * @param that the other polynomial
     * @return true if equal
     */
    public boolean eq (Polynomial that)
    {
        if (degree != that.degree) {
            return false;
        }

        for (int i = degree; i >= 0; i--) {
            if (coefficients[i] != that.coefficients[i]) {
                return false;
            }
        }

        return true;
    }

    //----------//
    // evaluate //
    //----------//
    /**
     * Return the evaluation of this polynomial for the provided x.
     *
     * @param x the provided x
     * @return value at x
     */
    public double evaluate (double x)
    {
        // use Horner's method 
        double result = 0;

        for (int i = degree; i >= 0; i--) {
            result = coefficients[i] + (x * result);
        }

        return result;
    }

    //------//
    // main //
    //------//
    // test client
    public static void main (String[] args)
    {
        Polynomial zero = new Polynomial(0, 0);

        Polynomial p1 = new Polynomial(4, 3); // 4x^3
        Polynomial p2 = new Polynomial(3, 2); // 3x^2
        Polynomial p3 = new Polynomial(1, 0); // 1
        Polynomial p4 = new Polynomial(2, 1); // 2x
        Polynomial p = p1.plus(p2)
                .plus(p3)
                .plus(p4); // 4x^3 + 3x^2 + 2x + 1

        Polynomial q1 = new Polynomial(3, 2); // 3x^2
        Polynomial q2 = new Polynomial(5, 0); // 5
        Polynomial q = q1.plus(q2); // 3x^2 + 5

        Polynomial r = p.plus(q);
        Polynomial s = p.times(q);
        Polynomial t = p.compose(q);

        System.out.println("zero(x) =     " + zero);
        System.out.println("p(x) =        " + p);
        System.out.println("q(x) =        " + q);
        System.out.println("p(x) + q(x) = " + r);
        System.out.println("p(x) * q(x) = " + s);
        System.out.println("p(q(x))     = " + t);
        System.out.println("0 - p(x)    = " + zero.minus(p));
        System.out.println("p(3)        = " + p.evaluate(3));
        System.out.println("p'(x)       = " + p.derivative());
        System.out.println("p''(x)      = " + p.derivative().derivative());
        System.out.println(
                "p'''(x)     = " + p.derivative().derivative().derivative());
        System.out.println(
                "p''''(x)    = "
                + p.derivative().derivative().derivative().derivative());
    }

    //-------//
    // minus //
    //-------//
    /**
     * Report this - that.
     *
     * @param that other polynomial
     * @return this - that
     */
    public Polynomial minus (Polynomial that)
    {
        Polynomial result = new Polynomial(
                0,
                Math.max(this.degree, that.degree));

        for (int i = 0; i <= this.degree; i++) {
            result.coefficients[i] += this.coefficients[i];
        }

        for (int i = 0; i <= that.degree; i++) {
            result.coefficients[i] -= that.coefficients[i];
        }

        result.degree = result.degree();

        return result;
    }

    //------//
    // plus //
    //------//
    /**
     * Report this + that.
     *
     * @param that other polynomial
     * @return this + that
     */
    public Polynomial plus (Polynomial that)
    {
        Polynomial result = new Polynomial(
                0,
                Math.max(this.degree, that.degree));

        for (int i = 0; i <= this.degree; i++) {
            result.coefficients[i] += this.coefficients[i];
        }

        for (int i = 0; i <= that.degree; i++) {
            result.coefficients[i] += that.coefficients[i];
        }

        result.degree = result.degree();

        return result;
    }

    //-------//
    // times //
    //-------//
    /**
     * Report this * that.
     *
     * @param that other polynomial
     * @return this * that
     */
    public Polynomial times (Polynomial that)
    {
        Polynomial result = new Polynomial(0, this.degree + that.degree);

        for (int i = 0; i <= this.degree; i++) {
            for (int j = 0; j <= that.degree; j++) {
                result.coefficients[i + j] += (this.coefficients[i] * that.coefficients[j]);
            }
        }

        result.degree = result.degree();

        return result;
    }

    //-------//
    // times //
    //-------//
    /**
     * Simple multiplication by a scalar
     *
     * @param scalar the scalar multiplicator value
     * @return the new polynomial (this * scalar)
     */
    public Polynomial times (double scalar)
    {
        Polynomial result = new Polynomial(0, degree);

        for (int i = 0; i <= degree; i++) {
            result.coefficients[i] = coefficients[i] * scalar;
        }

        result.degree = result.degree();

        return result;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Print by decreasing term degree.
     *
     * @return the polynomial terms, presented by decreasing degree
     */
    @Override
    public String toString ()
    {
        if (degree == 0) {
            return "" + coefficients[0];
        }

        if (degree == 1) {
            return coefficients[1] + "x + " + coefficients[0];
        }

        String s = coefficients[degree] + "x^" + degree;

        for (int i = degree - 1; i >= 0; i--) {
            if (coefficients[i] == 0) {
                continue;
            } else if (coefficients[i] > 0) {
                s = s + " + " + (coefficients[i]);
            } else if (coefficients[i] < 0) {
                s = s + " - " + (-coefficients[i]);
            }

            if (i == 1) {
                s += "x";
            } else if (i > 1) {
                s = s + "x^" + i;
            }
        }

        return s;
    }
}
