//----------------------------------------------------------------------------//
//                                                                            //
//                                 G r a d e s                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

/**
 * Class {@code Grades} gathers utility methods dealing with grade
 * values.
 *
 * @author Hervé Bitteur
 */
public class Grades
{
    //~ Methods ----------------------------------------------------------------

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

    public static double contextual (double target,
                                     double ratio,
                                     double source)
    {
        return (source * support(target, ratio)) + ((1 - source) * target);
    }

    public static double contextual (double target,
                                     double ratio1,
                                     double source1,
                                     double ratio2,
                                     double source2)
    {
        return (source1 * source2 * support(target, ratio1 * ratio2))
               + ((1 - source1) * source2 * support(target, ratio2))
               + (source1 * (1 - source2) * support(target, ratio1))
               + ((1 - source1) * (1 - source2) * target);
    }

    public static double contextual (double target,
                                     double[] ratios,
                                     double[] sources)
    {
        assert ratios != null : "Null ratios array";
        assert sources != null : "Null sources array";
        assert ratios.length == sources.length : "Arrays of different lengths";

        final int n = sources.length; // Nb of supporting sources
        final int combNb = (int) Math.pow(2, n);
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

    public static double support (double target,
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
