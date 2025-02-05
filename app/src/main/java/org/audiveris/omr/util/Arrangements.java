//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     A r r a n g e m e n t s                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.util;

import java.util.*;

/**
 * Class <code>Arrangements</code> generates all the possible arrangements of a given size,
 * among a collection of items.
 *
 * @author Hervé Bitteur
 */
public abstract class Arrangements
{
    //~ Constructors -------------------------------------------------------------------------------

    private Arrangements ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //----------//
    // generate //
    //----------//
    /**
     * @param <T>        the specific type of items
     * @param array      the array of items to pick from
     * @param bucketSize the size of each bucket
     * @return the collection of generated buckets
     */
    public static <T> List<List<T>> generate (T[] array,
                                              int bucketSize)
    {
        final List<List<T>> arrangements = new ArrayList<>();
        helper(array, bucketSize, 0, new ArrayList<>(), arrangements);

        return arrangements;
    }

    //----------//
    // generate //
    //----------//
    /**
     * @param <T>        the specific type of items
     * @param list       the list of items to pick from
     * @param bucketSize the size of each bucket
     * @return the collection of generated buckets
     */
    public static <T> List<List<T>> generate (List<T> list,
                                              int bucketSize)
    {
        @SuppressWarnings("unchecked")
        final T[] array = (T[]) list.toArray();

        return generate(array, bucketSize);
    }

    //--------//
    // helper //
    //--------//
    /**
     * Recursive processing.
     *
     * @param <T>        the specific type of items
     * @param array      the array of items to pick from
     * @param bucketSize the size of each bucket
     * @param start      (input) the starting index in array
     * @param current    (input/output) the current bucket being built
     * @param results    (output) the collection of generated buckets
     */
    private static <T> void helper (T[] array,
                                    int bucketSize,
                                    int start,
                                    List<T> current,
                                    List<List<T>> results)
    {
        if (current.size() == bucketSize) {
            // Bucket is complete
            results.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < array.length; i++) {
            current.add(array[i]);

            // Permutation
            T temp = array[start];
            array[start] = array[i];
            array[i] = temp;

            helper(array, bucketSize, start + 1, current, results);

            // Permutation
            temp = array[start];
            array[start] = array[i];
            array[i] = temp;

            current.remove(current.size() - 1);
        }
    }

    //--------//
    // reduce //
    //--------//
    /**
     * Remove the duplicates in the provided collection of results.
     *
     * @param <T>     specific type of arrangements
     * @param results (input/output) the results to reduce
     */
    public static <T> void reduce (List<List<T>> results)
    {
        for (int i = 0; i < results.size(); i++) {
            final List<T> current = results.get(i);

            for (int j = i + 1; j < results.size(); j++) {
                final List<T> other = results.get(j);

                if (other.equals(current)) {
                    results.remove(j--);
                }
            }
        }
    }
    //
    //    public static void main (String[] args)
    //    {
    //        final int bucketSize = 3; // Size of desired buckets
    //
    //        // Array
    //        final Integer[] array = { 1, 2, 3, null, null };
    //
    //        final List<List<Integer>> resArray = generate(array, bucketSize);
    //        System.out.println("\nResArray: " + resArray.size());
    //        for (List<Integer> arrangement : resArray) {
    //            System.out.println(arrangement);
    //        }
    //
    //        reduce(resArray);
    //        System.out.println("\nReduced Array: " + resArray.size());
    //        for (List<Integer> res : resArray) {
    //            System.out.println(res);
    //        }
    //
    //        // List
    //        final List<Integer> list = Arrays.asList(10, 20, 30, 40);
    //        final List<List<Integer>> resList = generate(list, bucketSize);
    //        System.out.println("\nResList: " + resList.size());
    //        for (List<Integer> res : resList) {
    //            System.out.println(res);
    //        }
    //
    //        reduce(resList);
    //        System.out.println("\nReduced List: " + resList.size());
    //        for (List<Integer> res : resList) {
    //            System.out.println(res);
    //        }
    //    }
}
