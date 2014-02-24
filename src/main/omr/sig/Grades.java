//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           G r a d e s                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Grades} gathers utility methods dealing with grade values.
 *
 * @author Hervé Bitteur
 */
public class Grades
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Grades.class);

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // clamp //
    //-------//
    /**
     * Constraint the provided value to lie within [0..1] range.
     *
     * @param value the value to check
     * @return the adjusted value
     */
    public static double clamp (double value)
    {
        if (value < 0) {
            value = 0;
        }

        if (value > 1) {
            value = 1;
        }

        return value;
    }

    //------------//
    // contextual //
    //------------//
    /**
     * Compute the contextual probability of a target when supported
     * by a source with 'ratio' value.
     *
     * @param target the intrinsic grade of the target
     * @param source the intrinsic grade of a supporting source
     * @param ratio  the ratio of supporting source
     * @return the resulting contextual probability for target
     */
    public static double contextual (double target,
                                     double source,
                                     double ratio)
    {
        return (source * support(target, ratio)) + ((1 - source) * target);
    }

    //------------//
    // contextual //
    //------------//
    /**
     * Compute the contextual probability of a target when supported
     * by two sources.
     *
     * @param target  the intrinsic grade of the target
     * @param source1 intrinsic grade of source #1
     * @param ratio1  ratio of supporting source #1
     * @param source2 intrinsic grade of source #2
     * @param ratio2  ratio of supporting source #2
     * @return the resulting contextual probability for target
     */
    public static double contextual (double target,
                                     double source1,
                                     double ratio1,
                                     double source2,
                                     double ratio2)
    {
        return (source1 * source2 * support(target, ratio1 * ratio2))
               + ((1 - source1) * source2 * support(target, ratio2))
               + (source1 * (1 - source2) * support(target, ratio1))
               + ((1 - source1) * (1 - source2) * target);
    }

    //------------//
    // contextual //
    //------------//
    /**
     * General computation of contextual probability for a target when
     * supported by an array of sources.
     *
     * @param target  the intrinsic grade of the target
     * @param sources the array of (intrinsic grades of the) supporting sources
     * @param ratios  the array of ratios of supporting sources, parallel to
     *                sources array
     * @return the resulting contextual probability for target
     */
    public static double contextual (double target,
                                     double[] sources,
                                     double[] ratios)
    {
        assert ratios != null : "Null ratios array";
        assert sources != null : "Null sources array";
        assert ratios.length == sources.length : "Arrays of different lengths";

        final int n = sources.length; // Nb of supporting sources
        final int combNb = (int) Math.pow(2, n); // Nb of combinations
        final boolean[] trueFalse = new boolean[]{true, false};

        // Define all combinations
        final boolean[][] bools = new boolean[combNb][n];

        int span = 1;

        for (int i = 0; i < n; i++) {
            int offset = 0;

            while (offset < combNb) {
                for (boolean b : trueFalse) {
                    for (int j = 0; j < span; j++) {
                        bools[j + offset][i] = b;
                    }

                    offset += span;
                }
            }

            span *= 2;
        }

        // Sum over all combinations
        double total = 0;

        for (boolean[] vector : bools) {
            double prob = 1;
            Double w = null;

            for (int i = 0; i < n; i++) {
                if (vector[i]) {
                    prob *= sources[i];

                    if (w != null) {
                        w *= ratios[i];
                    } else {
                        w = ratios[i];
                    }
                } else {
                    prob *= (1 - sources[i]);
                }
            }

            double value = (w != null) ? support(target, w) : target;
            double line = prob * value;

            //            System.out.printf(
            //                    "line: %.2f [prob:%.2f value:%.2f]%n",
            //                    line,
            //                    prob,
            //                    value);
            total += line;
        }

        //        System.out.println();
        //        System.out.printf("total: %.2f%n", total);
        return total;
    }

    //---------//
    // support //
    //---------//
    /**
     * Compute the actual support brought on a interpretation with
     * 'target' grade
     *
     * @param target the intrinsic grade of target
     * @param ratio  the supporting ratio of a source
     * @return the resulting support
     */
    private static double support (double target,
                                   double ratio)
    {
        double support = (ratio * target) / (1 + ((ratio - 1) * target));

        //        System.out.printf(
        //                "target:%.2f ratio:%.2f -> support:%.2f%n",
        //                target,
        //                ratio,
        //                support);
        return support;
    }

    //    private static void dump (boolean[] vector)
    //    {
    //        for (boolean b : vector) {
    //            System.out.print(b ? "T" : "F");
    //        }
    //
    //        System.out.println();
    //    }
}
