//----------------------------------------------------------------------------//
//                                                                            //
//                            W o r k e r T e s t                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author hb115668
 */
public class WorkerTest
{
    //~ Static fields/initializers ---------------------------------------------

    private static final int NB = 10000;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new WorkerTest object.
     */
    public WorkerTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Test of interrupt method, of class Worker.
     */
    @Test
    public void testInterrupt ()
    {
        long start = System.currentTimeMillis();
        System.out.println(
            "\n" + Thread.currentThread() + " interrupt (start at " + start);

        Integer  expResult = null;
        MyWorker instance = new MyWorker();
        instance.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
        }

        instance.interrupt();

        Integer result = instance.get();
        System.out.println(
            Thread.currentThread() + " stop after " +
            (System.currentTimeMillis() - start) + " ms");
        System.out.println(Thread.currentThread() + " Got: " + result);
        assertEquals(expResult, result);
    }

    /**
     * Test of start method, of class Worker.
     */
    @Test
    public void testStart ()
    {
        long start = System.currentTimeMillis();
        System.out.println(
            "\n" + Thread.currentThread() + " start (start at " + start);

        Integer  expResult = (NB * (NB + 1)) / 2;
        MyWorker instance = new MyWorker();
        instance.start();

        Integer result = instance.get();
        System.out.println(
            Thread.currentThread() + " stop after " +
            (System.currentTimeMillis() - start) + " ms");
        System.out.println(Thread.currentThread() + " Got: " + result);
        assertEquals(expResult, result);
    }

    //~ Inner Classes ----------------------------------------------------------

    private static class MyWorker
        extends Worker<Integer>
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public Integer construct ()
        {
            System.out.println(Thread.currentThread() + " enter construct");

            Integer j = 0;

            // Waste some time ...
            for (int k = 0; k < 100; k++) {
                for (int i = 0; i < 1000000; i++) {
                    j += i;
                }

                for (int i = 0; i < 1000000; i++) {
                    j -= i;
                }
            }

            for (int i = 0; i <= NB; i++) {
                j += i;
            }

            System.out.println(
                Thread.currentThread() + " n=" + NB + " sum=" + j);

            System.out.println(Thread.currentThread() + " exit construct");

            return j;
        }
    }
}
