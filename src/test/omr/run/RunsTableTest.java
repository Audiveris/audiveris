//----------------------------------------------------------------------------//
//                                                                            //
//                         R u n s T a b l e T e s t                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import static omr.run.Orientation.*;

import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Etiolles
 */
public class RunsTableTest
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Dimension dim = new Dimension(10, 5);

    private static final int level = 0;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new RunsTableTest object.
     */
    public RunsTableTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // testClone //
    //-----------//
    /**
     * Test of copy method, of class RunsTable.
     */
    @Test
    public void testClone ()
            throws Exception
    {
        System.out.println("clone");

        RunsTable instance = createHorizontalInstance();
        RunsTable expResult = instance;
        RunsTable result = instance.copy();

        if (!expResult.isIdentical(result)) {
            fail("Clone not identical to original");
        }
    }

    //----------//
    // testDump //
    //----------//
    /**
     * Test of dump method, of class RunsTable.
     */
    @Test
    public void testDump ()
    {
        System.out.println("dump");

        RunsTable instance = createHorizontalInstance();
        System.out.println(instance.dumpOf());

        instance = createVerticalInstance();
        System.out.println(instance.dumpOf());
    }

    //------------------//
    // testGetDimension //
    //------------------//
    /**
     * Test of getDimension method, of class RunsTable.
     */
    @Test
    public void testGetDimension ()
    {
        System.out.println("getDimension");

        RunsTable instance = createHorizontalInstance();
        Dimension expResult = new Dimension(10, 5);
        Dimension result = instance.getDimension();
        assertEquals(expResult, result);

        instance = createVerticalInstance();
        result = instance.getDimension();
        assertEquals(expResult, result);
    }

    /**
     * Test of getHeight method, of class RunsTable.
     */
    @Test
    public void testGetHeight ()
    {
        System.out.println("getHeight");

        RunsTable instance = createHorizontalInstance();
        int expResult = dim.height;
        int result = instance.getHeight();
        assertEquals(expResult, result);
    }

    //--------------------//
    // testGetOrientation //
    //--------------------//
    /**
     * Test of getOrientation method, of class RunsTable.
     */
    @Test
    public void testGetOrientation ()
    {
        System.out.println("getOrientation");

        RunsTable instance = createHorizontalInstance();
        Orientation expResult = HORIZONTAL;
        Orientation result = instance.getOrientation();
        assertEquals(expResult, result);
    }

    /**
     * Test of getPixel method, of class RunsTable.
     */
    @Test
    public void testGetPixel ()
    {
        System.out.println("getPixel");

        int x = 1;
        int y = 0;
        RunsTable instance = createHorizontalInstance();
        int expResult = level;
        int result = instance.getPixel(x, y);
        assertEquals(expResult, result);
    }

    //-----------------//
    // testGetSequence //
    //-----------------//
    /**
     * Test of getSequence method, of class RunsTable.
     */
    @Test
    public void testGetSequence ()
    {
        System.out.println("getSequence");

        int index = 0;
        RunsTable instance = createHorizontalInstance();
        List<Run> expResult = new ArrayList<>();
        expResult.add(new Run(1, 2, level));
        expResult.add(new Run(5, 3, level));

        List<Run> result = instance.getSequence(index);

        for (Run run : result) {
            Run other = expResult.get(result.indexOf(run));

            if (!run.isIdentical(other)) {
                fail("Non identical " + run + " vs " + other);
            }
        }
    }

    //-------------//
    // testGetSize //
    //-------------//
    /**
     * Test of getSize method, of class RunsTable.
     */
    @Test
    public void testGetSize ()
    {
        System.out.println("getSize");

        RunsTable instance = createHorizontalInstance();
        int expResult = 5;
        int result = instance.getSize();
        assertEquals(expResult, result);
    }

    /**
     * Test of getWidth method, of class RunsTable.
     */
    @Test
    public void testGetWidth ()
    {
        System.out.println("getWidth");

        RunsTable instance = createHorizontalInstance();
        int expResult = dim.width;
        int result = instance.getWidth();
        assertEquals(expResult, result);
    }

    //    /**
    //     * Test of setMaxForeground method, of class RunsTable.
    //     */
    //    @Test
    //    public void testSetMaxForeground() {
    //        System.out.println("setMaxForeground");
    //        int level = 0;
    //        RunsTable instance = null;
    //        instance.setMaxForeground(level);
    //        fail("The test case is a prototype.");
    //    }
    //
    //    /**
    //     * Test of getMaxForeground method, of class RunsTable.
    //     */
    //    @Test
    //    public void testGetMaxForeground() {
    //        System.out.println("getMaxForeground");
    //        RunsTable instance = null;
    //        int expResult = 0;
    //        int result = instance.getMaxForeground();
    //        assertEquals(expResult, result);
    //        fail("The test case is a prototype.");
    //    }
//    /**
//     * Test of isIdentical method, of class RunsTable.
//     */
//    @Test
//    public void testIsIdentical ()
//    {
//        System.out.println("isIdentical");
//
//        RunsTable that = createHorizontalInstance();
//        RunsTable instance = createHorizontalInstance();
//        boolean   expResult = true;
//        boolean   result = instance.isIdentical(that);
//        assertEquals(expResult, result);
//    }
//    //-----------//
//    // testPurge //
//    //-----------//
//    /**
//     * Test of purge method, of class RunsTable.
//     */
//    @Test
//    public void testPurge ()
//    {
//        System.out.println("purge");
//
//        Predicate<Run> predicate = new Predicate<Run>() {
//            public boolean check (Run run)
//            {
//                return run.getLength() > 2;
//            }
//        };
//
//        String    name = "purge";
//        System.out.println("HORIZONTAL");
//        RunsTable instance = createHorizontalInstance();
//        instance.dump(System.out);
//
//        RunsTable purge = instance.purge(predicate, name);
//
//        purge.dump(System.out);
//        instance.dump(System.out);
//
//        System.out.println("VERTICAL");
//        instance = createVerticalInstance();
//        instance.dump(System.out);
//
//        purge = instance.purge(predicate, name);
//
//        purge.dump(System.out);
//        instance.dump(System.out);
//    }
    //--------------//
    // testToString //
    //--------------//
    /**
     * Test of toString method, of class RunsTable.
     */
    @Test
    public void testToString ()
    {
        System.out.println("toString");

        RunsTable instance = createHorizontalInstance();
        String expResult = "{RunsTable hori HORIZONTAL 10x5}";
        String result = instance.toString();
        assertEquals(expResult, result);
    }

    //--------------------------//
    // createHorizontalInstance //
    //--------------------------//
    private RunsTable createHorizontalInstance ()
    {
        RunsTable instance = new RunsTable("hori", HORIZONTAL, dim);

        List<Run> seq;

        seq = instance.getSequence(0);
        seq.add(new Run(1, 2, level));
        seq.add(new Run(5, 3, level));

        seq = instance.getSequence(1);
        seq.add(new Run(0, 1, level));
        seq.add(new Run(4, 2, level));

        seq = instance.getSequence(2);
        seq.add(new Run(3, 1, level));
        seq.add(new Run(5, 4, level));

        seq = instance.getSequence(3);
        seq.add(new Run(0, 2, level));
        seq.add(new Run(4, 1, level));
        seq.add(new Run(8, 2, level));

        seq = instance.getSequence(4);
        seq.add(new Run(2, 2, level));
        seq.add(new Run(6, 4, level));

        return instance;
    }

    //------------------------//
    // createVerticalInstance //
    //------------------------//
    private RunsTable createVerticalInstance ()
    {
        RunsTable hori = createHorizontalInstance();
        RunsTableFactory factory = new RunsTableFactory(
                VERTICAL, new GlobalFilter(hori, 127), 0);

        return factory.createTable("vert");
    }
}
