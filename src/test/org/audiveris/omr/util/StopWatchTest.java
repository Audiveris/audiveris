/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.audiveris.omr.util;

import org.junit.Test;

/**
 *
 * @author Etiolles
 */
public class StopWatchTest
{

    StopWatch instance = new StopWatch("Utility Watch");

    int j;

    /**
     * Creates a new StopWatchTest object.
     */
    public StopWatchTest ()
    {
    }

    @Test
    public void testOne ()
    {
        System.out.println("void");
        instance.start("task #1");

        instance.print();
    }

    @Test
    public void testOverlap ()
    {
        System.out.println("overlap");

        instance.start("task #1");
        waste();

        instance.start("task #2");
        waste();
        waste();

        instance.start("task #3");
        waste();
        waste();
        waste();
        instance.print();
    }

    @Test
    public void testSimple ()
    {
        System.out.println("simple");

        instance.start("task #1");
        waste();
        instance.stop();

        instance.start("task #2");
        waste();
        waste();
        instance.stop();

        instance.start("task #3");
        waste();
        waste();
        waste();
        instance.print();
    }

    @Test
    public void testVoid ()
    {
        System.out.println("void");

        instance.print();
    }

    private void waste ()
    {
        for (int i = 0; i < 10000000; i++) {
            j = i / 2;
        }
    }
}
