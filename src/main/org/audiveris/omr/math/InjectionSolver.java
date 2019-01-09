//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 I n j e c t i o n S o l v e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
 * each domain/range elements pair. This implementation is based on a brute-force approach and thus
 * should be used with small sizes only.
 *
 * @author Hervé Bitteur
 */
public class InjectionSolver
{

    /** Size of domain. */
    private final int domainSize;

    /** Size of range. */
    private final int rangeSize;

    /** Distance function between a domain item and a range item. */
    private final Distance distance;

    /** Array parallel to range, to indicate which range items are free. */
    private final boolean[] free;

    /** Minimum cost found so far. */
    private int bestCost = Integer.MAX_VALUE;

    /** Best configuration found so far. Parallel to domain, it maps domain on range */
    private final int[] bestConfig;

    /** Configuration being worked upon. */
    private final int[] config;

    /**
     * Creates a new instance of InjectionSolver
     *
     * @param domainSize size of the domain collection
     * @param rangeSize  size of the range collection
     * @param distance   the distance
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
    }

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
        sb.append("bestConfig=[");

        for (int i = 0; i < bestConfig.length; i++) {
            sb.append(" ").append(bestConfig[i]);
        }

        sb.append("]");
        sb.append(" cost=").append(bestCost);

        System.out.println(sb.toString());
    }

    //---------//
    // inspect //
    //---------//
    /**
     * For a provided domain item, find the range item that leads to optimal config.
     *
     * @param id   index of provided domain item
     * @param cost current config cost
     */
    private void inspect (final int id,
                          final int cost)
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

    /**
     * Interface {@code Distance} provides the measurement for individual mapping costs.
     */
    public static interface Distance
    {

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
