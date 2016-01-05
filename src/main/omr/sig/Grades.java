//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           G r a d e s                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.math.Combinations;

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

    //
    //    //------------//
    //    // contextual //
    //    //------------//
    //    /**
    //     * Compute the contextual probability of an inter when supported
    //     * by a partner through a supporting relation with 'ratio' value.
    //     *
    //     * @param inter the intrinsic grade of the inter
    //     * @param partner the intrinsic grade of a supporting partner
    //     * @param ratio  the ratio of supporting relation
    //     * @return the resulting contextual probability for inter
    //     */
    //    public static double contextual (double inter,
    //                                     double partner,
    //                                     double ratio)
    //    {
    //        return (partner * support(inter, ratio)) + ((1 - partner) * inter);
    //    }
    //
    //    //------------//
    //    // contextual //
    //    //------------//
    //    /**
    //     * Compute the contextual probability of an inter when supported by two partners.
    //     *
    //     * @param inter  the intrinsic grade of the target
    //     * @param partner1 intrinsic grade of partner #1
    //     * @param ratio1  ratio of supporting partner #1
    //     * @param partner2 intrinsic grade of partner #2
    //     * @param ratio2  ratio of supporting partner #2
    //     * @return the resulting contextual probability for target
    //     */
    //    public static double contextual (double inter,
    //                                     double partner1,
    //                                     double ratio1,
    //                                     double partner2,
    //                                     double ratio2)
    //    {
    //        return (partner1 * partner2 * support(inter, ratio1 * ratio2))
    //               + ((1 - partner1) * partner2 * support(inter, ratio2))
    //               + (partner1 * (1 - partner2) * support(inter, ratio1))
    //               + ((1 - partner1) * (1 - partner2) * inter);
    //    }
    //
    //------------//
    // contextual //
    //------------//
    /**
     * General computation of contextual probability for an inter when supported by an
     * array of partners.
     *
     * @param inter    the (intrinsic grade of the) inter
     * @param partners the array of (intrinsic grades of the) supporting partners
     * @param ratios   the array of ratios of supporting partners, parallel to partners array
     * @return the resulting contextual probability for inter
     */
    public static double contextual (double inter,
                                     double[] partners,
                                     double[] ratios)
    {
        assert ratios != null : "Null ratios array";
        assert partners != null : "Null sources array";
        assert ratios.length == partners.length : "Arrays of different lengths";

        // Nb of supporting partners
        final int n = partners.length;

        //        final int combNb = (int) Math.pow(2, n); // Nb of combinations
        //        final boolean[] trueFalse = new boolean[]{true, false};
        //
        //        final boolean[][] bools = new boolean[combNb][n];
        //
        //        int span = 1;
        //
        //        for (int i = 0; i < n; i++) {
        //            int offset = 0;
        //
        //            while (offset < combNb) {
        //                for (boolean b : trueFalse) {
        //                    for (int j = 0; j < span; j++) {
        //                        bools[j + offset][i] = b;
        //                    }
        //
        //                    offset += span;
        //                }
        //            }
        //
        //            span *= 2;
        //        }
        // Define all combinations
        final boolean[][] bools = Combinations.getVectors(n);

        // Sum over all combinations
        double total = 0;

        for (boolean[] vector : bools) {
            double prob = 1;
            Double w = null;

            for (int i = 0; i < n; i++) {
                if (vector[i]) {
                    prob *= partners[i];

                    if (w != null) {
                        w *= ratios[i];
                    } else {
                        w = ratios[i];
                    }
                } else {
                    prob *= (1 - partners[i]);
                }
            }

            double value = (w != null) ? support(inter, w) : inter;
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
     * Compute the actual support brought on a interpretation with 'inter' grade
     *
     * @param inter the intrinsic grade of inter
     * @param ratio the supporting ratio of a partner
     * @return the resulting support
     */
    private static double support (double inter,
                                   double ratio)
    {
        double support = (ratio * inter) / (1 + ((ratio - 1) * inter));

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
