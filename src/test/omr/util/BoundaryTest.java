//----------------------------------------------------------------------------//
//                                                                            //
//                          B o u n d a r y T e s t                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.util;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class <code>BoundaryTest</code> is a set of unitary tests for the
 * <code>Boundary</code> class.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class BoundaryTest
{
    //~ Instance fields --------------------------------------------------------

    private Point    p0 = new Point(1, 5);
    private Point    p1 = new Point(10, 5);
    private Point    p2 = new Point(10, 1);
    private Point    p3 = new Point(1, 1);
    private Boundary instance;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BoundaryTest object.
     */
    public BoundaryTest ()
    {
        ///System.out.println("BoundaryTest");
    }

    //~ Methods ----------------------------------------------------------------

    @BeforeClass
    public static void setUpClass ()
        throws Exception
    {
        ///System.out.println("setUpClass");
    }

    @AfterClass
    public static void tearDownClass ()
        throws Exception
    {
        ///System.out.println("tearDownClass");
    }

    /**
     * Test of getBounds method, of class Boundary.
     */
    @Test
    public void getBounds ()
    {
        System.out.println("getBounds");

        Rectangle expResult = new Rectangle(1, 1, 9, 4);
        Rectangle result = instance.getBounds();
        assertEquals(expResult, result);
    }

    /**
     * Test of getPoint method, of class Boundary.
     */
    @Test
    public void getPoint ()
    {
        System.out.println("getPoint");

        int   index = 2;
        Point expResult = p2;
        Point result = instance.getPoint(index);
        assertTrue(expResult.equals(result));
    }

    /**
     * Test of getSequence method, of class Boundary.
     */
    @Test
    public void getSequence ()
    {
        System.out.println("getSequence");

        Point[] expResult = new Point[] { p0, p1, p2, p3 };
        Point[] result = instance.getSequence();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSequenceString method, of class Boundary.
     */
    @Test
    public void getSequenceString ()
    {
        System.out.println("getSequenceString");

        String   expResult = "[(1,5) (10,5) (10,1) (1,1)]";
        String   result = instance.getSequenceString();
        assertEquals(expResult, result);
    }

    /**
     * Test of setStickyDistance method, of class Boundary.
     */
    @Test
    public void setStickyDistance ()
    {
        System.out.println("setStickyDistance");

        int      stickyDistance = 123;
        instance.setStickyDistance(stickyDistance);
        assertEquals(stickyDistance, instance.getStickyDistance());
    }

    /**
     * Test of getStickyDistance method, of class Boundary.
     */
    @Test
    public void getStickyDistance ()
    {
        System.out.println("getStickyDistance");

        int      expResult = 1;
        int      result = instance.getStickyDistance();
        assertEquals(expResult, result);
    }

    @Before
    public void setUp ()
    {
        ///System.out.println("setUp");
        instance = new Boundary(
            new Point(1, 5),
            new Point(10, 5),
            new Point(10, 1),
            new Point(1, 1));
        instance.setStickyDistance(1);
    }

    /**
     * Test of addPoint method, of class Boundary.
     */
    @Test
    public void addPoint ()
    {
        System.out.println("addPoint");
        System.out.println("before: " + instance.getSequenceString());

        Point point = new Point(2, 3);
        instance.addPoint(point);
        System.out.println("after : " + instance.getSequenceString());
        assertEquals(5, instance.size());
    }

    /**
     * Test of addPointEmpty method, of class Boundary.
     */
    @Test
    public void addPointEmpty ()
    {
        System.out.println("addPointEmpty");
        instance = new Boundary();
        System.out.println("before: " + instance.getSequenceString());

        Point point = new Point(2, 3);
        instance.addPoint(point);
        System.out.println("after : " + instance.getSequenceString());
        assertEquals(1, instance.size());
    }

    /**
     * Test of areColinear method, of class Boundary.
     */
    @Test
    public void areColinear ()
    {
        System.out.println("areColinear");

        instance.insertPoint(2, new Point(12, 5));
        assertTrue(instance.areColinear(0, 1, 2));
        assertFalse(instance.areColinear(1, 2, 3));
    }

    /**
     * Test of findPoint method, of class Boundary.
     */
    @Test
    public void findPoint ()
    {
        int expResult = 2;
        System.out.println("findPoint " + expResult);
        System.out.println("before: " + instance.getSequenceString());

        Point point = new Point(11, 2);
        instance.setStickyDistance(1);

        int result = instance.findPoint(point);
        assertEquals(expResult, result);

        instance.setStickyDistance(0);
        assertEquals(-1, instance.findPoint(point));
    }

    /**
     * Test of findSegment method, of class Boundary.
     */
    @Test
    public void findSegment ()
    {
        System.out.println("findSegment");
        System.out.println("before: " + instance.getSequenceString());

        Point point = new Point(11, 3);
        instance.setStickyDistance(2);
        assertEquals(1, instance.findSegment(point));

        instance.setStickyDistance(0);
        assertEquals(-1, instance.findSegment(point));

        instance = new Boundary();
        instance.setStickyDistance(2);
        assertEquals(-1, instance.findSegment(point));
        instance.addPoint(p1);
        assertEquals(-1, instance.findSegment(point));
        instance.addPoint(p2);
        assertEquals(0, instance.findSegment(point));
    }

    /**
     * Test of insertPoint method, of class Boundary.
     */
    @Test
    public void insertPoint ()
    {
        int index = 2;
        System.out.println("insertPoint " + index);
        System.out.println("before: " + instance.getSequenceString());

        Point point = new Point(5, 2);
        instance.insertPoint(index, point);
        System.out.println("after : " + instance.getSequenceString());
        assertTrue(point.equals(instance.getPoint(index)));
    }

    /**
     * Test of movePoint method, of class Boundary.
     */
    @Test
    public void movePoint ()
    {
        int index = 1;
        System.out.println("movePoint " + index);
        System.out.println("before: " + instance.getSequenceString());

        Point location = new Point(11, 6);
        instance.movePoint(index, location);
        System.out.println("after : " + instance.getSequenceString());
        assertTrue(location.equals(instance.getPoint(index)));
    }

    /**
     * Test of removePoint method, of class Boundary.
     */
    @Test
    public void removePoint ()
    {
        int index = 2;
        System.out.println("removePoint " + index);
        System.out.println("before: " + instance.getSequenceString());

        instance.removePoint(index);
        System.out.println("after : " + instance.getSequenceString());
        assertTrue(p3.equals(instance.getPoint(index)));
    }

    /**
     * Test of render method, of class Boundary.
     */
    @Test
    public void render ()
    {
        System.out.println("render");

        // Nothing we can easily test
    }

    /**
     * Test of size method, of class Boundary.
     */
    @Test
    public void size ()
    {
        System.out.println("size");

        int expResult = 4;
        int result = instance.size();
        assertEquals(expResult, result);
    }

    @After
    public void tearDown ()
    {
        ///System.out.println("tearDown");
    }
}
