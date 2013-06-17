//----------------------------------------------------------------------------//
//                                                                            //
//                               E l l i p s e                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import static java.lang.Math.*;

/**
 * Class {@code Ellipse} implements the direct algorithm of Fitzgibbon
 * et al, improved by Halir et al, to find the ellipse which best
 * approximates a collection of points.
 * The ellipse is defined through the 6 coefficients of its algebraic equation:
 *
 * <p> A*x**2 + B*x*y + C*y**2 + D*x + E*y + F = 0
 *
 * <p>It can also compute the ellipse characteristics (center, theta, major,
 * minor) from its algebraic equation.
 *
 * @author Hervé Bitteur
 */
public class Ellipse
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Ellipse.class);

    /** Contraint such that 4*A*C - B**2 = 1 */
    private static final Matrix C1 = new Matrix(
            new double[][]{
        {0, 0, 2},
        {0, -1, 0},
        {2, 0, 0}
    });

    /** Inverse of Constraint */
    private static final Matrix C1inv = new Matrix(
            new double[][]{
        {0, 0, 0.5},
        {0, -1, 0},
        {0.5, 0, 0}
    });

    /** Epsilon value for vertical or horizontal ellipses */
    private static final double EPSILON = 1.0e-15;

    //~ Instance fields --------------------------------------------------------
    /**
     * Array of coefficients that define ellipse algebraic equation
     */
    protected double[] coeffs = new double[6];

    /** Coefficient of x**2 */
    protected double A;

    /** Coefficient of x*y */
    protected double B;

    /** Coefficient of y**2 */
    protected double C;

    /** Coefficient of x */
    protected double D;

    /** Coefficient of y */
    protected double E;

    /** Coefficient of 1 */
    protected double F;

    /** Mean algebraic distance between ellipse and the defining points */
    protected double distance;

    // Ellipse characteristics
    /** Center of ellipse */
    protected Point2D.Double center;

    /** Angle of main axis */
    protected Double angle;

    /** 1/2 Major axis */
    protected Double major;

    /** 1/2 Minor axis */
    protected Double minor;

    //~ Constructors -----------------------------------------------------------
    //---------//
    // Ellipse //
    //---------//
    /**
     * Creates a new instance of Ellipse, defined by a set of points
     *
     * @param x array of abscissae
     * @param y array of ordinates
     */
    public Ellipse (double[] x,
                    double[] y)
    {
        fit(x, y);
        computeCharacteristics();
    }

    /**
     * Creates a new Ellipse object.
     */
    protected Ellipse ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // getAngle //
    //----------//
    /**
     * Report the angle between the major axis and the abscissae axis
     *
     * @return the major axis angle, in radians
     */
    public double getAngle ()
    {
        if (angle == null) {
            computeCharacteristics();
        }

        return angle;
    }

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Report the center of the ellipse, using the same coordinate system as the
     * defining data points
     *
     * @return the ellipse center
     */
    public Point2D.Double getCenter ()
    {
        if (center == null) {
            computeCharacteristics();
        }

        return center;
    }

    //-----------------//
    // getCoefficients //
    //-----------------//
    /**
     * Report the coefficients of the ellipse, as defined by the algebraic
     * equation
     *
     * @return the algebraic coefficients, all packed in one array
     */
    public double[] getCoefficients ()
    {
        return coeffs;
    }

    //-------------//
    // getDistance //
    //-------------//
    /**
     * Report the mean algebraic distance between the data points and the
     * ellipse
     *
     * @return the mean algebraic distance
     */
    public double getDistance ()
    {
        return distance;
    }

    //----------//
    // getMajor //
    //----------//
    /**
     * Report the 1/2 length of the major axis
     *
     * @return the half major length
     */
    public Double getMajor ()
    {
        if (major == null) {
            computeCharacteristics();
        }

        return major;
    }

    //----------//
    // getMinor //
    //----------//
    /**
     * Report the 1/2 length of the minor axis
     *
     * @return the half minor length
     */
    public Double getMinor ()
    {
        if (minor == null) {
            computeCharacteristics();
        }

        return minor;
    }

    //-------//
    // print //
    //-------//
    protected static void print (Matrix m,
                                 String title)
    {
        StringBuilder sb = new StringBuilder();

        if (title != null) {
            sb.append(String.format("%s%n", title));
        } else {
            sb.append(String.format("%n"));
        }

        for (int row = 0; row < m.getRowDimension(); row++) {
            sb.append("    ");

            for (int col = 0; col < m.getColumnDimension(); col++) {
                sb.append(String.format("%15g  ", m.get(row, col)));
            }

            sb.append(String.format("%n"));
        }

        sb.append(String.format("%n"));

        logger.info(sb.toString());
    }

    //---------------------//
    // computeAngleAndAxes //
    //---------------------//
    /**
     * Compute the angle (between -PI/2 and +PI/2) of the major axis, as well as
     * the lengths of the major and minor axes.
     */
    protected void computeAngleAndAxes ()
    {
        if (abs(B) < EPSILON) {
            if (A <= C) {
                // Ellipse is horizontal
                angle = 0d;
                major = sqrt(1 / A);
                minor = sqrt(1 / C);
            } else {
                // Ellipse is vertical
                angle = PI / 2;
                major = sqrt(1 / C);
                minor = sqrt(1 / A);
            }
        } else {
            // Angle (modulo PI/2)
            double R = (C - A) / B;
            double tg = R - sqrt((R * R) + 1);
            angle = atan(tg);

            // Axes lengths
            double P = (2 * tg) / (1 + (tg * tg));

            if ((B / P) <= (-B / P)) {
                major = sqrt(2 / ((A + C) + (B / P)));
                minor = sqrt(2 / ((A + C) - (B / P)));
            } else {
                // Switch
                major = sqrt(2 / ((A + C) - (B / P)));
                minor = sqrt(2 / ((A + C) + (B / P)));

                if (angle < 0) {
                    angle += (PI / 2);
                } else {
                    angle -= (PI / 2);
                }
            }

            ///System.out.println("R=" + R + " tg=" + tg + " P=" + P);
        }
    }

    //---------------//
    // computeCenter //
    //---------------//
    /**
     * Compute ellipse center, based on the equation parameters
     *
     * @return the ellipse center
     */
    protected Point2D.Double computeCenter ()
    {
        /**
         * Let's consider the points where the ellipse is crossed by the
         * vertical line located at abscissa x : We have an equation in y, of
         * degree 2, governed by its discriminent.
         *
         * A*x**2 + B*x*y + C*y**2 + D*x + E*y + F = 0 becomes :
         *
         * C*y**2 + y*(B*x + E) + (Ax**2 + D*x + F)
         *
         * For the vertical tangents, we have a double root, thus with a null
         * discriminent (which gives us the two x values of these tangents)
         *
         * (B*x + E)**2 - 4*C*(Ax**2 + D*x + F) = 0
         *
         * Rewritten as an x-equation :
         *
         * (B**2 -4*A*C)*x**2 + (2*B*E - 4*C*D)*x + E**2 -4*C*F
         *
         * By symmetry, the ellipse center is right in the middle, so its
         * abscissa is half of the sum of the two roots (-b/2a) :
         *
         * centerX = (2*C*D - B*E) / (B**2 -4*A*C)
         *
         * And a similar approach on horizontal tangents would give :
         *
         * centerY = (2*A*E - B*D) / (B**2 -4*A*C)
         */
        double den = (B * B) - (4 * A * C);
        double x = ((2 * C * D) - (B * E)) / den;
        double y = ((2 * A * E) - (B * D)) / den;

        return new Point2D.Double(x, y);
    }

    //--------------------------//
    // computeCenterTranslation //
    //--------------------------//
    /**
     * Perform a change in variables, by using the ellipse center as the system
     * center. This nullifies parameters D and E.
     */
    protected void computeCenterTranslation ()
    {
        double x = center.x;
        double y = center.y;

        // Perform translation to ellipse center
        F += (((A * x * x) + (B * x * y) + (C * y * y)) + (D * x) + (E * y));
        //D += ((2 * A * x) + (B * y));
        D = 0;
        //E += ((B * x) + (2 * C * y));
        E = 0;

        // Normalize
        A /= -F;
        B /= -F;
        C /= -F;
        F = -1;

        // Update the coeffs array accordingly
        coeffs[0] = A;
        coeffs[1] = B;
        coeffs[2] = C;
        coeffs[3] = D;
        coeffs[4] = E;
        coeffs[5] = F;
    }

    //------------------------//
    // computeCharacteristics //
    //------------------------//
    /**
     * Compute the typical ellipse characteristic parameters (center, angle,
     * major, minor) out of the algebraic coefficients.
     */
    protected void computeCharacteristics ()
    {
        System.out.println("-- computeCharacteristics");

        // Compute ellipse center
        center = computeCenter();

        // Translate to ellipse center
        computeCenterTranslation();

        // Compute angle and axes
        computeAngleAndAxes();
    }

    //-----//
    // fit //
    //-----//
    /**
     * Compute the algebraic parameters of the ellipse that best fits the
     * provided set of data points.
     *
     * @param x the sequence of abscissae
     * @param y the sequence of ordinates
     */
    protected void fit (double[] x,
                        double[] y)
    {
        System.out.println("-- fit");

        // Check input
        if (x.length != y.length) {
            throw new IllegalArgumentException(
                    "x & y arrays have different lengths");
        }

        if (x.length < 6) {
            throw new IllegalArgumentException("Less than 6 defining points");
        }

        /**
         * Contraint matrix C is decomposed in
         * (C1 | 0 )
         * (---+---)
         * ( 0 | 0 )
         * */
        ///print(C1,    "C1");
        ///print(C1inv, "C1inv");
        /** number of points */
        int nbPoints = x.length;

        /**
         * Design matrix D is decomposed in
         * (D1|D2)
         */
        Matrix D1 = new Matrix(nbPoints, 3);
        Matrix D2 = new Matrix(nbPoints, 3);

        for (int i = 0; i < nbPoints; i++) {
            final double tx = x[i];
            final double ty = y[i];
            D1.set(i, 0, tx * tx);
            D1.set(i, 1, tx * ty);
            D1.set(i, 2, ty * ty);
            D2.set(i, 0, tx);
            D2.set(i, 1, ty);
            D2.set(i, 2, 1);
        }

        ///print(D1, "D1");
        ///print(D2, "D2");

        /**
         * Scatter matrix S is decomposed in 4 matrices
         * (S1 | S2)
         * (---+---)
         * (S2'| S3)
         */
        /** S1 = D1'.D1 */
        Matrix S1 = D1.transpose()
                .times(D1);

        /** S2 = D1'.D2 */
        Matrix S2 = D1.transpose()
                .times(D2);

        /** S3 = D2'.D2 */
        Matrix S3 = D2.transpose()
                .times(D2);

        ///print(S2, "S2");
        ///print(S1, "S1");
        ///print(S3, "S3");

        /**
         * Initial equation S.A = lambda.C.A can be rewritten :
         *
         * (S1 | S2) (A1) (C1 | 0 ) (A1)
         * (---+---) . (--) = lambda . (---+---) . (--)
         * (S2'| S3) (A2) ( 0 | 0 ) (A2)
         *
         * which is equivalent to :
         * S1.A1 + S2.A2 = lambda.C1.A1
         * S2'.A1 + S3.A2 = 0
         *
         * So
         * A2 = -S3inv.S2'.A1
         * (S1 - S2.S3inv.S2').A1 = lambda.C1.A1 or
         * C1inv.(S1 - S2.S3inv.S2').A1 = lambda.A1
         *
         * Contraint is now
         * A1'.C1.A1 = 1
         *
         * w/ Reduced scatter matrix M = C1inv.S1 - S2.S3inv.S2'
         * we now have :
         *
         * M.A1 = lambda.A1
         * A1'.C1.A1 = 1
         * A2 = -S3inv.S2'.A1
         */
        Matrix M = C1inv.times(
                S1.minus(S2.times(S3.inverse()).times(S2.transpose())));

        ///print(M, "M");

        /** Retrieve eigen vectors and values for A1 */
        EigenvalueDecomposition ed = new EigenvalueDecomposition(M);
        Matrix eigenVectors = ed.getV();
        double[] eigenValues = ed.getRealEigenvalues();

        ///print(eigenVectors, "EigenVectors");
        ///System.out.println("EigenValues");
        ///for (double v : eigenValues) {
        ///    System.out.print(String.format(" %g", v));
        ///}
        ///System.out.println();

        /**
         * Evaluate A1'.C1.A1 for each eigenvector,
         * and keep the one with positive result
         */
        Matrix A1;
        double lambda = 0;
        int index = 0;

        for (int i = 0; i < 3; i++) {
            A1 = eigenVectors.getMatrix(0, 2, i, i);

            ///print(A1, "vector " + i);
            Matrix R = A1.transpose()
                    .times(C1)
                    .times(A1);

            ///print(R, "R");
            if (R.get(0, 0) > 0) {
                lambda = eigenValues[i];
                index = i;
            }
        }

        /** Copy the first 3 coefficients from A1 */
        A1 = eigenVectors.getMatrix(0, 2, index, index);

        print(A1, "A1");

        for (int i = 0; i < 3; i++) {
            coeffs[i] = A1.get(i, 0);
        }

        /** Copy the 3 other coefficients from A2 = -S3inv.S2'.A1 */
        Matrix A2 = S3.inverse()
                .times(S2.transpose())
                .times(A1)
                .uminus();

        print(A2, "A2");

        for (int i = 0; i < 3; i++) {
            coeffs[i + 3] = A2.get(i, 0);
        }

        /** Store also coeffs as individual variables to ease formulae */
        A = coeffs[0]; // Nothing to do with matrix A
        B = coeffs[1];
        C = coeffs[2];
        D = coeffs[3];
        E = coeffs[4];
        F = coeffs[5];

        /**
         * Compute the mean distance
         * || D.A ||**2 = A'.D'.D.A = A'.S.A = lambda.A'.C.A = lambda
         */
        if (lambda > 0) {
            distance = Math.sqrt(lambda / nbPoints);
        } else {
            distance = 0;
        }
        ///System.out.println("lambda distance=" + distance);
        // Let's try a brutal distance computation
        {
            Matrix D = new Matrix(nbPoints, 6);
            D.setMatrix(0, nbPoints - 1, 0, 2, D1);
            D.setMatrix(0, nbPoints - 1, 3, 5, D2);

            Matrix A = new Matrix(6, 1);
            A.setMatrix(0, 2, 0, 0, A1);
            A.setMatrix(3, 5, 0, 0, A2);

            Matrix DA = D.times(A);
            double s = 0;

            for (int i = 0; i < nbPoints; i++) {
                double val = DA.get(i, 0);
                s += (val * val);
            }

            s /= nbPoints;
            distance = sqrt(s);
        }

        ///System.out.println("distance: " + distance);
    }
}
