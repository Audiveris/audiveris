//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             B a s i c R u n S e q u e n c e T e s t                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Unit tests for BasicRunSequence.
 *
 * @author Hervé Bitteur
 */
public class BasicRunSequenceTest
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BasicRunSequenceTest object.
     */
    public BasicRunSequenceTest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Test
    public void testAddEmpty ()
    {
        System.out.println("testAddEmpty");

        List<Run> list = Collections.EMPTY_LIST;
        BasicRunSequence seq = new BasicRunSequence(list);
        System.out.printf("beforeAdd seq: %s%n", seq);
        dump(seq);

        seq.add(new Run(0, 1));
        System.out.printf("afterAdd seq: %s%n", seq);
        dump(seq);
        assertEquals("[1]", seq.toString());

        seq.add(new Run(20, 2));
        System.out.printf("afterAdd seq: %s%n", seq);
        dump(seq);
        assertEquals("[1, 19, 2]", seq.toString());

        seq.add(new Run(30, 3));
        System.out.printf("afterAdd seq: %s%n", seq);
        dump(seq);
        assertEquals("[1, 19, 2, 8, 3]", seq.toString());

        seq.add(new Run(1, 19));
        System.out.printf("afterAdd seq: %s%n", seq);
        dump(seq);
        assertEquals("[22, 8, 3]", seq.toString());

        seq.add(new Run(22, 8));
        System.out.printf("afterAdd seq: %s%n", seq);
        dump(seq);
        assertEquals("[33]", seq.toString());

        seq.add(new Run(33, 7));
        System.out.printf("afterAdd seq: %s%n", seq);
        dump(seq);
        assertEquals("[40]", seq.toString());
    }

    @Test
    public void testAddMid ()
    {
        System.out.println("testAddMid");

        List<Run> list = Arrays.asList(new Run(0, 3),
                                       new Run(30, 5));
        BasicRunSequence seq = new BasicRunSequence(list);
        System.out.printf("beforeAdd seq: %s%n", seq);
        dump(seq);

        seq.add(new Run(10, 4));
        System.out.printf("afterAdd seq: %s%n", seq);
        dump(seq);
        assertEquals("[3, 7, 4, 16, 5]", seq.toString());
    }

    @Test
    public void testOverwrite1 ()
    {
        System.out.println("testOverwrite1");

        List<Run> list = Arrays.asList(new Run(0, 3),
                                       new Run(30, 5));
        BasicRunSequence seq = new BasicRunSequence(list);
        System.out.printf("beforeAdd seq: %s%n", seq);
        dump(seq);

        boolean bool = seq.add(new Run(30, 2));
        System.out.printf("afterAdd seq: %s%n", seq);
        dump(seq);
        assertFalse(bool);
    }

    @Test
    public void testOverwrite2 ()
    {
        System.out.println("testOverwrite2");

        List<Run> list = Arrays.asList(new Run(0, 3),
                                       new Run(30, 5));
        BasicRunSequence seq = new BasicRunSequence(list);
        System.out.printf("beforeAdd seq: %s%n", seq);
        dump(seq);

        boolean bool = seq.add(new Run(32, 1));
        System.out.printf("afterAdd seq: %s%n", seq);
        dump(seq);
        assertFalse(bool);
    }

    @Test
    public void testOverwrite3 ()
    {
        System.out.println("testOverwrite3");

        List<Run> list = Arrays.asList(new Run(0, 3),
                                       new Run(30, 5));
        BasicRunSequence seq = new BasicRunSequence(list);
        System.out.printf("beforeAdd seq: %s%n", seq);
        dump(seq);

        boolean bool = seq.add(new Run(30, 6));
        System.out.printf("afterAdd seq: %s%n", seq);
        dump(seq);
        assertFalse(bool);
    }

    @Test
    public void testEncode01 ()
    {
        System.out.println("testEncode01");

        List<Run> list = Arrays.asList(new Run(10, 3));
        BasicRunSequence seq = new BasicRunSequence(list);
        System.out.printf("seq: %s%n", seq);
        dump(seq);
        assertEquals("[0, 10, 3]", seq.toString());
    }

    @Test
    public void testEncode1 ()
    {
        System.out.println("testEncode1");

        List<Run> list = Arrays.asList(new Run(0, 3));
        BasicRunSequence seq = new BasicRunSequence(list);
        System.out.printf("seq: %s%n", seq);
        assertEquals("[3]", seq.toString());
        dump(seq);
    }

    @Test
    public void testEncode2 ()
    {
        System.out.println("testEncode2");

        List<Run> list = Arrays.asList(new Run(0, 3), new Run(23, 5));
        BasicRunSequence seq = new BasicRunSequence(list);
        System.out.printf("seq: %s%n", seq);
        assertEquals("[3, 20, 5]", seq.toString());
        dump(seq);
    }

    @Test
    public void testEncode3 ()
    {
        System.out.println("testEncode3");

        List<Run> list = Arrays.asList(new Run(0, 3),
                                       new Run(23, 5),
                                       new Run(30, 4));
        BasicRunSequence seq = new BasicRunSequence(list);
        System.out.printf("seq: %s%n", seq);
        assertEquals("[3, 20, 5, 2, 4]", seq.toString());
        dump(seq);
    }

    @Test
    public void testEncodeEmpty ()
    {
        System.out.println("testEncodeEmpty");

        List<Run> list = Collections.EMPTY_LIST;
        BasicRunSequence seq = new BasicRunSequence(list);
        System.out.printf("seq: %s%n", seq);
        dump(seq);
        assertEquals("[0]", seq.toString());
    }

    @Test
    public void testIterator ()
    {
        System.out.println("testIterator");

        List<Run> list = Arrays.asList(new Run(0, 3),
                                       new Run(23, 5),
                                       new Run(30, 4));
        BasicRunSequence seq = new BasicRunSequence(list);

        for (Run run : seq) {
            System.out.println("run: " + run);
        }
    }

    @Test
    public void testIterator2 ()
    {
        System.out.println("testIterator2");

        List<Run> list = Arrays.asList(new Run(0, 3),
                                       new Run(23, 5),
                                       new Run(30, 4));
        BasicRunSequence seq = new BasicRunSequence(list);

        for (Iterator<Run> it = seq.iterator(); it.hasNext();) {
            Run run = it.next();
            System.out.println("run: " + run);

            if (run.isIdentical(new Run(23, 5))) {
                System.out.printf("removing %s%n", run);
                it.remove();
            }
        }
    }

    @Test
    public void testRemove01 ()
    {
        System.out.println("testRemove01");

        List<Run> list = Arrays.asList(new Run(10, 3));
        BasicRunSequence seq = new BasicRunSequence(list);
        System.out.printf("before seq: %s%n", seq);
        dump(seq);
        assertEquals("[0, 10, 3]", seq.toString());
        seq.remove(new Run(10, 3));
        System.out.printf("after seq: %s%n", seq);
        dump(seq);
        assertEquals("[0]", seq.toString());
        assertEquals(0, seq.size());
    }

    @Test
    public void testRemove1 ()
    {
        System.out.println("testRemove1");

        List<Run> list = Arrays.asList(new Run(0, 1),
                                       new Run(20, 2),
                                       new Run(40, 4));
        BasicRunSequence seq = new BasicRunSequence(list);
        System.out.printf("before seq: %s%n", seq);
        dump(seq);
        assertEquals("[1, 19, 2, 18, 4]", seq.toString());

        seq.remove(new Run(20, 2));
        System.out.printf("after seq: %s%n", seq);
        dump(seq);
        assertEquals("[1, 39, 4]", seq.toString());
        assertEquals(2, seq.size());

        seq.remove(new Run(0, 1));
        System.out.printf("after seq: %s%n", seq);
        dump(seq);
        assertEquals("[0, 40, 4]", seq.toString());
        assertEquals(1, seq.size());

        seq.remove(new Run(40, 4));
        System.out.printf("after seq: %s%n", seq);
        dump(seq);
        assertEquals("[0]", seq.toString());
        assertEquals(0, seq.size());
    }

    @Test
    public void testSize3 ()
    {
        System.out.println("testSize3");

        List<Run> list = Arrays.asList(new Run(0, 3),
                                       new Run(23, 5),
                                       new Run(30, 4));
        BasicRunSequence seq = new BasicRunSequence(list);
        System.out.printf("seq: %s%n", seq);
        dump(seq);
        assertEquals(3, seq.size());
    }

    private void dump (RunSequence seq)
    {
        for (Run run : seq) {
            System.out.println("   " + run);
        }
    }
}
