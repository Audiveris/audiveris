//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C o m b i n a t i o n s                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

/**
 * Class {@code Combinations}
 *
 * @author Hervé Bitteur
 */
public abstract class Combinations
{
    //~ Static fields/initializers -----------------------------------------------------------------

    public static final boolean[] trueFalse = new boolean[]{true, false};

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // dumpOf //
    //--------//
    public static String dumpOf (boolean[][] vectors)
    {
        StringBuilder sb = new StringBuilder();

        for (boolean[] vector : vectors) {
            sb.append("\n");
            sb.append(dumpOf(vector));
        }

        return sb.toString();
    }

    //--------//
    // dumpOf //
    //--------//
    public static String dumpOf (boolean[] vector)
    {
        StringBuilder sb = new StringBuilder();

        for (boolean b : vector) {
            sb.append(b ? "1 " : "0 ");
        }

        return sb.toString();
    }

    //------------//
    // getVectors //
    //------------//
    /**
     * Return the array of possible combinations in a sequence of n items.
     * <p>
     * The last positions vary first, here is an example for a sequence of 4 items:
     * <pre>
     * 1 1 1 1
     * 1 1 1 0
     * 1 1 0 1
     * 1 1 0 0
     * 1 0 1 1
     * 1 0 1 0
     * 1 0 0 1
     * 1 0 0 0
     * 0 1 1 1
     * 0 1 1 0
     * 0 1 0 1
     * 0 1 0 0
     * 0 0 1 1
     * 0 0 1 0
     * 0 0 0 1
     * 0 0 0 0
     * </pre>
     *
     * @param n the sequence size
     * @return the array of possible combinations
     */
    public static boolean[][] getVectors (int n)
    {
        final int combNb = (int) Math.pow(2, n); // Nb of combinations
        final boolean[][] bools = new boolean[combNb][n];
        int span = 1;

        for (int i = n - 1; i >= 0; i--) {
            ///for (int i = 0; i < n; i++) {
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

        return bools;
    }
}
