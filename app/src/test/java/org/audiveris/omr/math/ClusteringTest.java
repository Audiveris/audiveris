package org.audiveris.omr.math;

import static org.audiveris.omr.math.Clustering.*;

import org.junit.Test;

/**
 *
 * @author herve
 */
public class ClusteringTest
{

    static java.util.Random random = new java.util.Random(111);

    /**
     * Creates a new ClusteringTest object.
     */
    public ClusteringTest ()
    {
    }

    public static double gaussianSample (double mu,
                                         double sigma)
    {
        return ((random.nextGaussian()) * sigma) + mu;
    }

    public static double[] generateSample (int N)
    {
        double[] x = new double[N];

        // generate random values according to some laws
        for (int i = 0; i < N; i++) {
            double r = (100 * i) / (double) N;

            if (r < 20) { // 20% of law #1
                x[i] = gaussianSample(2.0, 0.5);
            } else if (r < 70) { // 50% of law #2
                x[i] = gaussianSample(6.0, 1.0);
            } else { // 30% of law #3
                x[i] = gaussianSample(10.0, 2.0);
            }
        }

        return x;
    }

    /**
     * Test of expectationMaximization method, of class Clustering.
     */
    @Test
    public void testAlgorithmEM ()
    {
        //        System.out.println("expectationMaximization");
        //        double[] x = null;
        //        Clustering.Law[] laws = null;
        //        double[] expResult = null;
        //        double[] result = Clustering.expectationMaximization(x, laws);
        //        assertArrayEquals(expResult, result);
        //        // TODO review the generated test code and remove the default call to fail.
        //        fail("The test case is a prototype.");

        // generate random values
        int N = 4000;
        double[] x = generateSample(N);

        // initial guess for the laws parameters (uniform)
        int G = 3;
        Law[] laws = new Law[G];
        laws[0] = new Gaussian(4.0, 1.0);
        laws[1] = new Gaussian(8.0, 1.0);
        laws[2] = new Gaussian(12.0, 1.0);

        // perform EM algorithm
        double[] pi = Clustering.EM(x, laws);

        // display mixture coefficients and law parameters
        for (int k = 0; k < G; k++) {
            System.out.printf("%f * %s%n", pi[k], laws[k]);
        }
    }
}
