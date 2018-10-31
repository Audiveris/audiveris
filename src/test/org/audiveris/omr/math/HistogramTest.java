//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   H i s t o g r a m T e s t                                    //
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

import junit.framework.Assert;

import org.audiveris.omr.math.Histogram;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Etiolles
 */
public class HistogramTest
{
    //~ Instance fields ----------------------------------------------------------------------------

    private Set<Integer> keySet;

    private Set<Integer> valueSet;

    private List<Entry<Integer, Integer>> expMaxima = new ArrayList<Entry<Integer, Integer>>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new HistogramTest object.
     */
    public HistogramTest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Test of bucketSet method, of class Histogram.
     */
    @Test
    public void testBucketSet ()
    {
        System.out.println("bucketSet");

        Histogram<Integer> instance = createHistogram();
        Set<Integer> expResult = keySet;
        Set result = instance.bucketSet();
        assertEquals(expResult, result);
    }

    /**
     * Test of clear method, of class Histogram.
     */
    @Test
    public void testClear ()
    {
        System.out.println("clear");

        Histogram<Integer> instance = createHistogram();
        instance.clear();
        assertEquals(0, instance.getTotalCount());
    }

    /**
     * Test of dataString method, of class Histogram.
     */
    @Test
    public void testDataString ()
    {
        //        System.out.println("dataString");
        //
        //        Histogram instance = new Histogram();
        //        String    expResult = "";
        //        String    result = instance.dataString();
        //        assertEquals(expResult, result);
        //        fail("The test case is a prototype.");
    }

    /**
     * Test of entrySet method, of class Histogram.
     */
    @Test
    public void testEntrySet ()
    {
        System.out.println("entrySet");

        Histogram<Integer> instance = createHistogram();
        Set<Map.Entry<Integer, Integer>> result = instance.entrySet();
        assertEquals(instance.size(), result.size());

        for (Map.Entry<Integer, Integer> entry : result) {
            int key = entry.getKey();
            int val = entry.getValue();
            assertEquals(instance.getCount(key), val);
        }
    }

    /**
     * Test of firstBucket method, of class Histogram.
     */
    @Test
    public void testFirstBucket ()
    {
        System.out.println("firstBucket");

        Histogram<Integer> instance = createHistogram();
        int expResult = 3;
        int result = instance.firstBucket();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCount method, of class Histogram.
     */
    @Test
    public void testGetCount ()
    {
        System.out.println("getCount");

        int bucket = 4;
        Histogram<Integer> instance = createHistogram();
        int expResult = 10;
        int result = instance.getCount(bucket);
        assertEquals(expResult, result);
    }

    /**
     * Test of getMaxBucket method, of class Histogram.
     */
    @Test
    public void testGetMaxBucket ()
    {
        System.out.println("getMaxBucket");

        Histogram instance = createHistogram();
        int expResult = 5;
        Number result = instance.getMaxBucket();
        assertEquals(expResult, result);
    }

    /**
     * Test of getMaxCount method, of class Histogram.
     */
    @Test
    public void testGetMaxCount ()
    {
        System.out.println("getMaxCount");

        Histogram<Integer> instance = createHistogram();
        int expResult = 12;
        int result = instance.getMaxCount();
        assertEquals(expResult, result);
    }

    //
    //    /**
    //     * Test of getCountPeaks method, of class Histogram.
    //     */
    //    @Test
    //    public void testGetPeaks ()
    //    {
    //        System.out.println("getCountPeaks");
    //
    //        int minCount = 6;
    //        Histogram<Integer> instance = createHistogram();
    //        instance.print(System.out);
    //
    //        List<PeakEntry<Integer>> expResult = Arrays.asList(
    //                new PeakEntry<Integer>(new Peak<Integer>(4, 5, 5), 12.0),
    //                new PeakEntry<Integer>(new Peak<Integer>(10, 10, 10), 6.0));
    //        List<PeakEntry<Integer>> result = instance.getCountPeaks(minCount, true, true);
    //        System.out.println("result: " + result);
    //
    //        assertEquals(expResult.size(), result.size());
    //
    //        Iterator<PeakEntry<Integer>> expIt = expResult.iterator();
    //        Iterator<PeakEntry<Integer>> resIt = result.iterator();
    //
    //        while (expIt.hasNext()) {
    //            PeakEntry<Integer> expPeak = expIt.next();
    //            PeakEntry<Integer> resPeak = resIt.next();
    //            assertEquals(expPeak.toString(), resPeak.toString());
    //        }
    //    }
    //
    /**
     * Test of getTotalCount method, of class Histogram.
     */
    @Test
    public void testGetTotalCount ()
    {
        System.out.println("getTotalCount");

        Histogram<Integer> instance = createHistogram();
        int expResult = 33;
        int result = instance.getTotalCount();
        assertEquals(expResult, result);
    }

    /**
     * Test of increaseCount method, of class Histogram.
     */
    @Test
    public void testIncreaseCount ()
    {
        System.out.println("increaseCount");

        int bucket = 10;
        int delta = 50;
        Histogram<Integer> instance = createHistogram();
        instance.increaseCount(bucket, delta);

        int expResult = 56;
        int result = instance.getCount(bucket);
        assertEquals(expResult, result);
    }

    /**
     * Test of lastBucket method, of class Histogram.
     */
    @Test
    public void testLastBucket ()
    {
        System.out.println("lastBucket");

        Histogram<Integer> instance = createHistogram();
        int expResult = 11;
        int result = instance.lastBucket();
        assertEquals(expResult, result);
    }

    /**
     * Test of print method, of class Histogram.
     */
    @Test
    public void testPrint ()
    {
        System.out.println("print");

        PrintStream stream = System.out;
        Histogram<Integer> instance = createHistogram();
        instance.print(stream);
    }

    /**
     * Test of size method, of class Histogram.
     */
    @Test
    public void testSize ()
    {
        System.out.println("size");

        Histogram<Integer> instance = createHistogram();
        int expResult = keySet.size();
        int result = instance.size();
        assertEquals(expResult, result);
    }

    /**
     * Test of values method, of class Histogram.
     */
    @Test
    public void testValues ()
    {
        System.out.println("values");

        Histogram<Integer> instance = createHistogram();
        Collection<Integer> expResult = valueSet;
        Collection<Integer> result = instance.values();
        assertEquals(expResult.size(), result.size());

        Iterator<Integer> expIt = expResult.iterator();
        Iterator<Integer> resIt = result.iterator();

        while (expIt.hasNext()) {
            assertEquals(expIt.next(), resIt.next());
        }
    }

    //- Asserts ----------------------------------------------------------------
    private int assertAreSameLength (List expecteds,
                                     List actuals,
                                     String header)
    {
        if (expecteds == null) {
            Assert.fail(header + "expected array was null");
        }

        if (actuals == null) {
            Assert.fail(header + "actual array was null");
        }

        int actualsLength = actuals.size();
        int expectedsLength = expecteds.size();

        if (actualsLength != expectedsLength) {
            Assert.fail(
                    header + "list lengths differed, expected.length=" + expectedsLength
                    + " actual.length=" + actualsLength);
        }

        return expectedsLength;
    }

    private void assertElementsEqual (Entry<Integer, Integer> expected,
                                      Entry<Integer, Integer> actual)
    {
        if (!expected.getKey().equals(actual.getKey())
            || !expected.getValue().equals(actual.getValue())) {
            throw new AssertionError("Expected: " + expected + " Actual: " + actual);
        }
    }

    private Histogram<Integer> createHistogram ()
    {
        Histogram<Integer> histo = new Histogram<Integer>();
        histo.increaseCount(3, 2);
        histo.increaseCount(4, 10);
        histo.increaseCount(5, 12);
        histo.increaseCount(8, 3);
        histo.increaseCount(10, 6);
        histo.increaseCount(11, 0);

        keySet = new TreeSet<Integer>();
        keySet.addAll(Arrays.asList(3, 4, 5, 8, 10, 11));

        valueSet = new LinkedHashSet<Integer>();
        valueSet.addAll(Arrays.asList(2, 10, 12, 3, 6, 0));

        return histo;
    }

    private Histogram<Integer> createHistogram (int... vals)
    {
        Histogram<Integer> histo = new Histogram<Integer>();

        for (int i = 0; i < vals.length; i++) {
            histo.increaseCount(i, vals[i]);
        }

        return histo;
    }
}
