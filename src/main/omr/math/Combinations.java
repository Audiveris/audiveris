//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C o m b i n a t i o n s                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.math;

/**
 * Class {@code Combinations}
 *
 * @author Hervé Bitteur
 */
public abstract class Combinations
{
    //~ Static fields/initializers -----------------------------------------------------------------

    public static final boolean[] TRUE_FALSE = new boolean[]{true, false};

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
                for (boolean b : TRUE_FALSE) {
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
