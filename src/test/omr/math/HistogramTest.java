//----------------------------------------------------------------------------//
//                                                                            //
//                         H i s t o g r a m T e s t                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import omr.math.Histogram.Pair;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Etiolles
 */
public class HistogramTest
{
    //~ Instance fields --------------------------------------------------------

    private Histogram<Integer> histo;
    private Set<Integer>       keySet;
    private Set<Integer>       valueSet;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new HistogramTest object.
     */
    public HistogramTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    @BeforeClass
    public static void setUpClass ()
        throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass ()
        throws Exception
    {
    }

    @Before
    public void setUp ()
    {
    }

    @After
    public void tearDown ()
    {
    }

    /**
     * Test of bucketSet method, of class Histogram.
     */
    @Test
    public void testBucketSet ()
    {
        System.out.println("bucketSet");

        Histogram<Integer> instance = createHistogram();
        Set<Integer>       expResult = keySet;
        Set                result = instance.bucketSet();
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
     * Test of entrySet method, of class Histogram.
     */
    @Test
    public void testEntrySet ()
    {
        System.out.println("entrySet");

        Histogram<Integer>               instance = createHistogram();
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
        int                expResult = 3;
        int                result = instance.firstBucket();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCount method, of class Histogram.
     */
    @Test
    public void testGetCount ()
    {
        System.out.println("getCount");

        int                bucket = 4;
        Histogram<Integer> instance = createHistogram();
        int                expResult = 10;
        int                result = instance.getCount(bucket);
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
        int                expResult = 12;
        int                result = instance.getMaxCount();
        assertEquals(expResult, result);
    }

    /**
     * Test of getPeaks method, of class Histogram.
     */
    @Test
    public void testGetPeaks ()
    {
        System.out.println("getPeaks");

        int                minCount = 6;
        Histogram<Integer> instance = createHistogram();
        List               expResult = Arrays.asList(
            new Pair(4, 5),
            new Pair(10, 10));
        List               result = instance.getPeaks(minCount);

        assertEquals(expResult.size(), result.size());

        Iterator<Pair<Integer>> expIt = expResult.iterator();
        Iterator<Pair<Integer>> resIt = result.iterator();

        while (expIt.hasNext()) {
            Pair expPair = expIt.next();
            Pair resPair = resIt.next();
            assertEquals(expPair.first, resPair.first);
            assertEquals(expPair.second, resPair.second);
        }
    }

    /**
     * Test of getTotalCount method, of class Histogram.
     */
    @Test
    public void testGetTotalCount ()
    {
        System.out.println("getTotalCount");

        Histogram<Integer> instance = createHistogram();
        int                expResult = 33;
        int                result = instance.getTotalCount();
        assertEquals(expResult, result);
    }

    /**
     * Test of increaseCount method, of class Histogram.
     */
    @Test
    public void testIncreaseCount ()
    {
        System.out.println("increaseCount");

        int                bucket = 10;
        int                delta = 50;
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
        int                expResult = 11;
        int                result = instance.lastBucket();
        assertEquals(expResult, result);
    }

    /**
     * Test of print method, of class Histogram.
     */
    @Test
    public void testPrint ()
    {
        System.out.println("print");

        PrintStream        stream = System.out;
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
        int                expResult = keySet.size();
        int                result = instance.size();
        assertEquals(expResult, result);
    }

    /**
     * Test of values method, of class Histogram.
     */
    @Test
    public void testValues ()
    {
        System.out.println("values");

        Histogram<Integer>  instance = createHistogram();
        Collection<Integer> expResult = valueSet;
        Collection<Integer> result = instance.values();
        assertEquals(expResult.size(), result.size());

        Iterator<Integer> expIt = expResult.iterator();
        Iterator<Integer> resIt = result.iterator();

        while (expIt.hasNext()) {
            assertEquals(expIt.next(), resIt.next());
        }
    }

    private Histogram<Integer> createHistogram ()
    {
        histo = new Histogram<Integer>();
        histo.increaseCount(10, 6);
        histo.increaseCount(3, 2);
        histo.increaseCount(4, 10);
        histo.increaseCount(5, 12);
        histo.increaseCount(8, 3);
        histo.increaseCount(11, 0);

        keySet = new TreeSet<Integer>();
        keySet.addAll(Arrays.asList(3, 4, 5, 8, 10, 11));

        valueSet = new LinkedHashSet<Integer>();
        valueSet.addAll(Arrays.asList(2, 10, 12, 3, 6, 0));

        return histo;
    }
}
