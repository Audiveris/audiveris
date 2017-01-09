/*
 * Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
 * This software is released under the GNU General Public License.
 * Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
 */
package org.audiveris.omr.math;

import org.audiveris.omr.math.Combinations;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Hervé Bitteur
 */
public class CombinationsTest
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CombinationsTest.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code CombinationsTest} object.
     */
    public CombinationsTest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    @BeforeClass
    public static void setUpClass ()
    {
    }

    @AfterClass
    public static void tearDownClass ()
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
     * Test of getVectors method, of class Combinations.
     */
    @Test
    public void testGetVectors2 ()
    {
        System.out.println("getVectors2");

        int n = 2;
        boolean[][] result = Combinations.getVectors(n);
        logger.info("{}", Combinations.dumpOf(result));
    }

    /**
     * Test of getVectors method, of class Combinations.
     */
    @Test
    public void testGetVectors4 ()
    {
        System.out.println("getVectors4");

        int n = 4;
        boolean[][] result = Combinations.getVectors(n);
        logger.info("{}", Combinations.dumpOf(result));
    }

    /**
     * Test of getVectors method, of class Combinations.
     */
    @Test
    public void testGetVectors8 ()
    {
        System.out.println("getVectors8");

        int n = 8;
        boolean[][] result = Combinations.getVectors(n);
        logger.info("{}", Combinations.dumpOf(result));
    }
}
