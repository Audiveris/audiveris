//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    R u n T a b l e T e s t                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.image.GlobalFilter;
import static omr.run.Orientation.*;

import omr.util.Predicate;

import ij.process.ByteProcessor;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.Point;

/**
 *
 * @author Hervé Bitteur
 */
public class RunTableTest
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Dimension dim = new Dimension(10, 5);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new RunsTableTest object.
     */
    public RunTableTest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Before
    public void setUp ()
            throws Exception
    {
    }

    @BeforeClass
    public static void setUpClass ()
            throws Exception
    {
    }

    @After
    public void tearDown ()
            throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass ()
            throws Exception
    {
    }

    //----------//
    // testCopy //
    //----------//
    /**
     * Test of copy method, of class RunTable.
     */
    @Test
    public void testCopy_0args ()
            throws Exception
    {
        System.out.println("copy");

        RunTable instance = createHorizontalInstance();
        RunTable expResult = instance;
        RunTable result = instance.copy();

        if (!expResult.isIdentical(result)) {
            fail("Copy not identical to original");
        }
    }

    /**
     * Test of copy method, of class RunTable.
     */
    @Test
    public void testCopy_String ()
    {
        System.out.println("copy");

        RunTable instance = createHorizontalInstance();
        RunTable expResult = createHorizontalInstance();
        RunTable result = instance.copy("hori");

        if (!expResult.isIdentical(result) || !expResult.getName().equals(result.getName())) {
            fail("Copy not identical to original");
        }
    }

    /**
     * Test of dumpOf method, of class RunTable.
     */
    @Test
    public void testDumpOf ()
    {
        System.out.println("dumpOf");

        RunTable instance = createHorizontalInstance();
        System.out.println(instance.dumpOf());

        instance = createVerticalInstance();
        System.out.println(instance.dumpOf());
    }

    /**
     * Test of dumpSequences method, of class RunTable.
     */
    @Test
    public void testDumpSequences ()
    {
        System.out.println("dumpSequences");

        RunTable instance = createHorizontalInstance();
        instance.dumpSequences();

        instance = createVerticalInstance();
        instance.dumpSequences();
    }

    /**
     * Test of get method, of class RunTable.
     */
    @Test
    public void testGet ()
    {
        System.out.println("get");

        RunTable instance = createVerticalInstance();
        assertEquals(255, instance.get(0, 0));
        assertEquals(0, instance.get(1, 0));
        assertEquals(0, instance.get(2, 0));
        assertEquals(255, instance.get(3, 0));
        assertEquals(0, instance.get(0, 1));
        assertEquals(255, instance.get(1, 1));
    }

    /**
     * Test of getBuffer method, of class RunTable.
     */
    @Test
    public void testGetBuffer ()
    {
        System.out.println("getBuffer");

        RunTable instance = createVerticalInstance();
        String expResult = "ip[width=10, height=5, bits=8, min=0.0, max=255.0]";
        ByteProcessor result = instance.getBuffer();
        System.out.println("result: " + result);
        assertEquals(expResult, result.toString());
    }

    //------------------//
    // testGetDimension //
    //------------------//
    /**
     * Test of getDimension method, of class RunTable.
     */
    @Test
    public void testGetDimension ()
    {
        System.out.println("getDimension");

        RunTable instance = createHorizontalInstance();
        Dimension expResult = new Dimension(10, 5);
        Dimension result = instance.getDimension();
        assertEquals(expResult, result);

        instance = createVerticalInstance();
        result = instance.getDimension();
        assertEquals(expResult, result);
    }

    /**
     * Test of getHeight method, of class RunTable.
     */
    @Test
    public void testGetHeight ()
    {
        System.out.println("getHeight");

        RunTable instance = createHorizontalInstance();
        int expResult = dim.height;
        int result = instance.getHeight();
        assertEquals(expResult, result);
    }

    /**
     * Test of getName method, of class RunTable.
     */
    @Test
    public void testGetName ()
    {
        System.out.println("getName");

        RunTable instance = createHorizontalInstance();
        String expResult = "hori";
        String result = instance.getName();
        assertEquals(expResult, result);
    }

    //--------------------//
    // testGetOrientation //
    //--------------------//
    /**
     * Test of getOrientation method, of class RunTable.
     */
    @Test
    public void testGetOrientation ()
    {
        System.out.println("getOrientation");

        RunTable instance = createHorizontalInstance();
        Orientation expResult = HORIZONTAL;
        Orientation result = instance.getOrientation();
        assertEquals(expResult, result);
    }

    /**
     * Test of getRunAt method, of class RunTable.
     */
    @Test
    public void testGetRunAt ()
    {
        System.out.println("getRunAt");

        RunTable instance = createHorizontalInstance();
        Run result = instance.getRunAt(0, 0);
        Run expResult = null;
        assertEquals(expResult, result);

        result = instance.getRunAt(6, 0);
        expResult = new Run(5, 3);
        assertEquals(expResult.toString(), result.toString());
    }

    /**
     * Test of getSequence method, of class RunTable.
     */
    @Test
    public void testGetSequence ()
    {
        System.out.println("getSequence");

        int index = 1;
        RunTable instance = createHorizontalInstance();
        RunSequence expResult = new BasicRunSequence();
        expResult.add(new Run(0, 1));
        expResult.add(new Run(4, 2));
        System.out.println("expResult: " + expResult);

        RunSequence result = instance.getSequence(index);
        assertEquals(expResult, result);
    }

    //-------------//
    // testGetSize //
    //-------------//
    /**
     * Test of getSize method, of class RunTable.
     */
    @Test
    public void testGetSize ()
    {
        System.out.println("getSize");

        RunTable instance = createHorizontalInstance();
        int expResult = 5;
        int result = instance.getSize();
        assertEquals(expResult, result);
    }

    /**
     * Test of getTotalRunCount method, of class RunTable.
     */
    @Test
    public void testGetTotalRunCount ()
    {
        System.out.println("getTotalRunCount");

        RunTable instance = createHorizontalInstance();
        int expResult = 11;
        int result = instance.getTotalRunCount();
        assertEquals(expResult, result);

        instance = createVerticalInstance();
        expResult = 19;
        result = instance.getTotalRunCount();
        assertEquals(expResult, result);
    }

    /**
     * Test of getWidth method, of class RunTable.
     */
    @Test
    public void testGetWidth ()
    {
        System.out.println("getWidth");

        RunTable instance = createHorizontalInstance();
        int expResult = dim.width;
        int result = instance.getWidth();
        assertEquals(expResult, result);
    }

    /**
     * Test of include method, of class RunTable.
     */
    @Test
    public void testInclude ()
    {
        System.out.println("include");

        RunTable instance = createHorizontalInstance();

        ByteProcessor buffer = instance.getBuffer();
        buffer.invert();

        GlobalFilter filter = new GlobalFilter(buffer, 127);
        RunTableFactory factory = new RunTableFactory(HORIZONTAL);
        RunTable that = factory.createTable("inverted", filter.filteredImage());
        that.dumpSequences();
        System.out.println("inverted" + that.dumpOf());

        instance.include(that);
        instance.dumpSequences();
        System.out.println("full" + instance.dumpOf());
        assertEquals(5, instance.getTotalRunCount());
    }

    /**
     * Test of isIdentical method, of class RunTable.
     */
    @Test
    public void testIsIdentical ()
    {
        System.out.println("isIdentical");

        RunTable that = createHorizontalInstance();
        RunTable instance = createHorizontalInstance();
        boolean expResult = true;
        boolean result = instance.isIdentical(that);
        assertEquals(expResult, result);

        that = createVerticalInstance();
        expResult = false;
        result = instance.isIdentical(that);
        assertEquals(expResult, result);
    }

    /**
     * Test of lookupRun method, of class RunTable.
     */
    @Test
    public void testLookupRun ()
    {
        System.out.println("lookupRun");

        RunTable instance = createHorizontalInstance();
        Point point = new Point(6, 0);
        Run expResult = new Run(5, 3);
        Run result = instance.lookupRun(point);
        assertEquals(expResult.toString(), result.toString());

        point = new Point(7, 1);
        result = instance.lookupRun(point);
        assertEquals(null, result);
    }

    /**
     * Test of purge method, of class RunTable.
     */
    @Test
    public void testPurge_Predicate ()
    {
        System.out.println("purge_predicate");

        RunTable instance = createHorizontalInstance();
        Predicate<Run> predicate1 = new Predicate<Run>()
        {
            @Override
            public boolean check (Run run)
            {
                return run.getLength() <= 2;
            }
        };

        RunTable result = instance.purge(predicate1);
        instance.dumpSequences();
        System.out.println("table after1:" + instance.dumpOf());
        assertEquals(3, result.getTotalRunCount());

        Predicate<Run> predicate2 = new Predicate<Run>()
        {
            @Override
            public boolean check (Run run)
            {
                return run.getLength() > 2;
            }
        };

        result = instance.purge(predicate2);
        instance.dumpSequences();
        System.out.println("table after2:" + instance.dumpOf());
        assertEquals(0, result.getTotalRunCount());
    }

    /**
     * Test of purge method, of class RunTable.
     */
    @Test
    public void testPurge_Predicate_RunTable ()
    {
        System.out.println("purge_predicate_removed");

        Predicate<Run> predicate1 = new Predicate<Run>()
        {
            @Override
            public boolean check (Run run)
            {
                return run.getLength() == 2;
            }
        };

        RunTable instance = createHorizontalInstance();
        instance.dumpSequences();
        System.out.println("table before1:" + instance.dumpOf());

        RunTable removed = new RunTable("purged", HORIZONTAL, dim.width, dim.height);
        instance.purge(predicate1, removed);
        instance.dumpSequences();
        System.out.println("table after1:" + instance.dumpOf());

        removed.dumpSequences();
        System.out.println("purge after1:" + removed.dumpOf());

        Predicate<Run> predicate2 = new Predicate<Run>()
        {
            @Override
            public boolean check (Run run)
            {
                return run.getLength() == 1;
            }
        };

        instance.purge(predicate2, removed);
        instance.dumpSequences();
        System.out.println("table after2:" + instance.dumpOf());

        removed.dumpSequences();
        System.out.println("purge after2:" + removed.dumpOf());
    }

    /**
     * Test of removeRun method, of class RunTable.
     */
    @Test
    public void testRemoveRun ()
    {
        System.out.println("removeRun");

        int pos = 3;
        Run run = new Run(4, 1);
        RunTable instance = createHorizontalInstance();
        RunSequence seq1 = instance.getSequence(pos);
        System.out.println("sequence before: " + seq1);
        System.out.println("table before:" + instance.dumpOf());
        instance.removeRun(pos, run);

        RunSequence seq2 = instance.getSequence(pos);
        System.out.println("sequence  after: " + seq2);
        System.out.println("table after:" + instance.dumpOf());
    }

    /**
     * Test of setSequence method, of class RunTable.
     */
    @Test
    public void testSetSequence ()
    {
        System.out.println("setSequence");

        int index = 1;
        RunSequence seq = new BasicRunSequence(new short[]{0, 3, 7});
        RunTable instance = createHorizontalInstance();
        System.out.println("table before:" + instance.dumpOf());
        instance.setSequence(index, seq);
        System.out.println("table after:" + instance.dumpOf());
    }

    /**
     * Test of toString method, of class RunTable.
     */
    @Test
    public void testToString ()
    {
        System.out.println("toString");

        RunTable instance = createHorizontalInstance();
        String expResult = "{RunTable hori HORIZONTAL 10x5 runs:11}";
        String result = instance.toString();
        assertEquals(expResult, result);
    }

    //--------------------------//
    // createHorizontalInstance //
    //--------------------------//
    private RunTable createHorizontalInstance ()
    {
        RunTable instance = new RunTable("hori", HORIZONTAL, dim.width, dim.height);

        RunSequence seq;

        seq = instance.getSequence(0);
        seq.add(new Run(1, 2));
        seq.add(new Run(5, 3));

        seq = instance.getSequence(1);
        seq.add(new Run(0, 1));
        seq.add(new Run(4, 2));

        seq = instance.getSequence(2);
        seq.add(new Run(3, 1));
        seq.add(new Run(5, 4));

        seq = instance.getSequence(3);
        seq.add(new Run(0, 2));
        seq.add(new Run(4, 1));
        seq.add(new Run(8, 2));

        seq = instance.getSequence(4);
        seq.add(new Run(2, 2));
        seq.add(new Run(6, 4));

        return instance;
    }

    //------------------------//
    // createVerticalInstance //
    //------------------------//
    private RunTable createVerticalInstance ()
    {
        RunTable hori = createHorizontalInstance();
        ByteProcessor buffer = hori.getBuffer();
        GlobalFilter filter = new GlobalFilter(buffer, 127);
        RunTableFactory factory = new RunTableFactory(VERTICAL);

        return factory.createTable("vert", filter.filteredImage());
    }
}
//    /**
//     * Test of setMaxForeground method, of class RunTable.
//     */
//    @Test
//    public void testSetMaxForeground() {
//        System.out.println("setMaxForeground");
//        int level = 0;
//        RunTable instance = null;
//        instance.setMaxForeground(level);
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMaxForeground method, of class RunTable.
//     */
//    @Test
//    public void testGetMaxForeground() {
//        System.out.println("getMaxForeground");
//        RunTable instance = null;
//        int expResult = 0;
//        int result = instance.getMaxForeground();
//        assertEquals(expResult, result);
//        fail("The test case is a prototype.");
//    }
//    /**
//     * Test of isIdentical method, of class RunTable.
//     */
//    @Test
//    public void testIsIdentical ()
//    {
//        System.out.println("isIdentical");
//
//        RunTable that = createHorizontalInstance();
//        RunTable instance = createHorizontalInstance();
//        boolean   expResult = true;
//        boolean   result = instance.isIdentical(that);
//        assertEquals(expResult, result);
//    }
//
