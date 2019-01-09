//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       C l u s t e r i n g                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.math;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Clustering} gathers objects according to their similarity.
 * It uses the implementation of Expectation-Maximization algorithm published by Xavier Philippeau
 * on <a
 * href=
 * "http://www.developpez.net/forums/d740672/autres-langages/algorithmes/contribuez/java-algorithme-expectation-maximization-em/">
 * this site</a>
 *
 * @author Xavier Philippeau
 */
public class Clustering
{

    private static final Logger logger = LoggerFactory.getLogger(Clustering.class);

    private static final double TWO_PI_SQ = Math.sqrt(2 * Math.PI);

    private static final double EPSILON = 1E-10;

    private static final int MAX_ITER = 10;

    // Not meant to be instantiated.
    private Clustering ()
    {
    }

    /**
     * Compute the mixture coefficients using Expectation-Maximization algorithm.
     *
     * @param x    sample values
     * @param laws instances of the laws
     * @return mixture coefficients
     */
    public static double[] EM (double[] x,
                               Law[] laws)
    {
        int N = x.length;
        int G = laws.length;

        double[] pi = new double[G];
        double[][] t = new double[G][N];

        // initial guess for the mixture coefficients (uniform)
        for (int k = 0; k < G; k++) {
            pi[k] = 1.0 / G;
        }

        // iterative loop (until convergence or max iterations)
        double convergence;

        for (int loop = 0; loop < MAX_ITER; loop++) {
            convergence = 0;

            // ---- E Step ----
            //(Bayes inversion formula)
            for (int i = 0; i < N; i++) {
                double denominator = 0;

                for (int l = 0; l < G; l++) {
                    denominator += (pi[l] * laws[l].proba(x[i]));
                }

                for (int k = 0; k < G; k++) {
                    double numerator = pi[k] * laws[k].proba(x[i]);
                    t[k][i] = numerator / denominator;
                }
            }

            // ---- M Step ----
            // mixture coefficients (maximum likelihood estimate of binomial distribution)
            for (int k = 0; k < G; k++) {
                double savedpi = pi[k];
                double newPi = 0;

                for (int i = 0; i < N; i++) {
                    newPi += t[k][i];
                }

                newPi /= N;
                pi[k] = newPi;

                double deltaPi = newPi - savedpi;
                convergence += (deltaPi * deltaPi);
            }

            // update the parameters of the laws
            for (int k = 0; k < G; k++) {
                laws[k].improveParameters(N, x, t[k]);
            }

            if (convergence < EPSILON) {
                logger.debug("convergence:{} loop:{}", convergence, loop);

                break;
            }
        }

        return pi;
    }

    /**
     * Model description.
     */
    public static interface Law
    {

        /**
         * improve law parameters
         *
         * @param N  number of samples
         * @param x  samples
         * @param tk probability of each sample
         */
        void improveParameters (int N,
                                double[] x,
                                double[] tk);

        /**
         * @param x some value
         * @return the probability of the value x
         */
        double proba (double x);
    }

    /**
     * Gaussian implementation of Law.
     */
    public static class Gaussian
            implements Law
    {

        private double mean = 0;

        private double sigma = 0;

        /**
         * Creates a new Gaussian object.
         *
         * @param mean  DOCUMENT ME!
         * @param sigma DOCUMENT ME!
         */
        public Gaussian (double mean,
                         double sigma)
        {
            this.mean = mean;
            this.sigma = sigma;
        }

        /**
         * Report gaussian mean value
         *
         * @return mean value
         */
        public double getMean ()
        {
            return mean;
        }

        @Override
        public void improveParameters (int N,
                                       double[] x,
                                       double[] tk)
        {
            double sumTkX = 0;
            double sumTk = 0;

            for (int i = 0; i < N; i++) {
                sumTkX += (tk[i] * x[i]);
            }

            for (int i = 0; i < N; i++) {
                sumTk += tk[i];
            }

            mean = sumTkX / sumTk;

            double sumTkXc2 = 0;

            for (int i = 0; i < N; i++) {
                sumTkXc2 += (tk[i] * (x[i] - mean) * (x[i] - mean));
            }

            sigma = Math.sqrt(sumTkXc2 / sumTk);
        }

        @Override
        public double proba (double x)
        {
            double t = (x - mean);

            if (sigma <= EPSILON) {
                if (Math.abs(t) <= EPSILON) {
                    return 1.0;
                } else {
                    return 0.0;
                }
            } else {
                return Math.exp(-(t * t) / (2 * sigma * sigma)) / (sigma * TWO_PI_SQ);
            }
        }

        @Override
        public String toString ()
        {
            return String.format("Gaussian (mean=%.3f, sigma=%.3f)", mean, sigma);
        }
    }
}
