/*
 *
 * Copyright © Audiveris 2025. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this
 * program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.audiveris.omr.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Hervé Bitteur
 */
public class ArrangementsTest
{

    public ArrangementsTest ()
    {
    }
    //
    //    @Test
    //    public void testMain ()
    //    {
    //        System.out.println("main");
    //        String[] args = null;
    //        Arrangements.main(args);
    //        fail("The test case is a prototype.");
    //    }

    @Test
    public void testGenerate_GenericType_int ()
    {
        System.out.println("\ntestGenerate_GenericType_int");
        final Integer[] array = { 10, 20, 30 };
        final int bucketSize = 2;

        List<List<Integer>> results = Arrangements.generate(array, bucketSize);
        System.out.println("Results: " + results.size());
        for (List<Integer> arrangement : results) {
            System.out.println(arrangement);
        }

        assertEquals(results.size(), 6);
        assertEquals(results.get(0), Arrays.asList(10, 20));
        assertEquals(results.get(1), Arrays.asList(10, 30));
        assertEquals(results.get(2), Arrays.asList(20, 10));
        assertEquals(results.get(3), Arrays.asList(20, 30));
        assertEquals(results.get(4), Arrays.asList(30, 20));
        assertEquals(results.get(5), Arrays.asList(30, 10));
    }

    @Test
    public void testGenerate_List ()
    {
        System.out.println("\ntestGenerate_List");
        final List<Integer> list = Arrays.asList(10, 20, 30);
        final int bucketSize = 2;
        List<List<Integer>> results = Arrangements.generate(list, bucketSize);

        System.out.println("Results: " + results.size());
        for (List<Integer> arrangement : results) {
            System.out.println(arrangement);
        }

        assertEquals(results.size(), 6);
        assertEquals(results.get(0), Arrays.asList(10, 20));
        assertEquals(results.get(1), Arrays.asList(10, 30));
        assertEquals(results.get(2), Arrays.asList(20, 10));
        assertEquals(results.get(3), Arrays.asList(20, 30));
        assertEquals(results.get(4), Arrays.asList(30, 20));
        assertEquals(results.get(5), Arrays.asList(30, 10));
    }

    //    @Test
    //    public void testReduce ()
    //    {
    //        System.out.println("reduce");
    //        Arrangements.reduce(null);
    //        fail("The test case is a prototype.");
    //    }

}