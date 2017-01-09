//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 I n j e c t i o n S o l v e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import java.util.Arrays;

/**
 * Class {@code InjectionSolver} handles the injection of a collection of elements
 * (called domain) into another collection of elements (called range, or co-domain).
 * <p>
 * It finds a mapping that minimizes the global mapping distance, given the individual distance for
 * each domain/range elements pair. This implementation uses brute force, and thus should be used
 * with small sizes only.
 *
 * @author Hervé Bitteur
 */
public class InjectionSolver
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final int domainSize;

    private final int rangeSize;

    private final Distance distance;

    private final boolean[] free;

    private int bestCost = Integer.MAX_VALUE;

    private final int[] bestConfig;

    private final int[] config;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new instance of InjectionSolver
     *
     * @param domainSize size of the domain collection
     * @param rangeSize  size of the range collection
     * @param distance
     */
    public InjectionSolver (int domainSize,
                            int rangeSize,
                            Distance distance)
    {
        // Parameters of the solver
        this.domainSize = domainSize;
        this.rangeSize = rangeSize;
        this.distance = distance;

        free = new boolean[rangeSize];
        bestConfig = new int[domainSize];
        config = new int[domainSize];

        //        System.out.println(
        //            "InjectionSolver domainSize=" + domainSize + " rangeSize=" +
        //            rangeSize);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // solve //
    //-------//
    /**
     * Report (one of) the mapping(s) for which the global distance is minimum.
     *
     * @return an array parallel to the domain collection, which for each (domain) element gives the
     *         mapped range element
     */
    public int[] solve ()
    {
        Arrays.fill(free, true);
        inspect(0, 0);

        return bestConfig;
    }

    //------//
    // dump //
    //------//
    private void dump ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("bestCost=").append(bestCost);
        sb.append(" [");

        for (int i = 0; i < bestConfig.length; i++) {
            sb.append(" ").append(bestConfig[i]);
        }

        sb.append("]");

        System.out.println(sb.toString());
    }

    //---------//
    // inspect //
    //---------//
    private void inspect (int id,
                          int cost)
    {
        //        System.out.println("inspect id=" + id + " cost=" + cost);
        for (int ir = 0; ir < rangeSize; ir++) {
            if (free[ir]) {
                free[ir] = false;
                config[id] = ir;

                int newCost = cost + distance.getDistance(id, ir);

                /// System.out.println("ir=" + ir + " newCost=" + newCost);
                if (id < (domainSize - 1)) {
                    inspect(id + 1, newCost);
                } else if (newCost < bestCost) {
                    // Record best config so far
                    System.arraycopy(config, 0, bestConfig, 0, domainSize);
                    bestCost = newCost;

                    // dump();
                }

                free[ir] = true;
            }
        }
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    /**
     * Interface {@code Distance} provides the measurement for individual mapping costs.
     */
    public static interface Distance
    {
        //~ Methods --------------------------------------------------------------------------------

        /**
         * Report the distance when mapping element 'id' of domain to element 'ir' of
         * range
         *
         * @param id index of domain element
         * @param ir index of range element
         * @return the cost of mapping these two elements
         */
        int getDistance (int id,
                         int ir);
    }
}
