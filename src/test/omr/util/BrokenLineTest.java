//----------------------------------------------------------------------------//
//                                                                            //
//                        B r o k e n L i n e T e s t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.awt.Point;
import java.util.Arrays;
import java.util.List;

/**
 * Class <code>BrokenLineTest</code> is a set of unitary tests for the
 * <code>BrokenLine</code> class.
 *
 * @author Hervé Bitteur
 * @version $Id$
 */
public class BrokenLineTest
{
    //~ Instance fields --------------------------------------------------------

    private Point      p0 = new Point(1, 5);
    private Point      p1 = new Point(10, 5);
    private Point      p2 = new Point(10, 1);
    private Point      p3 = new Point(1, 1);
    private BrokenLine instance;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BrokenLineTest object.
     */
    public BrokenLineTest ()
    {
        ///System.out.println("BrokenLineTest");
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Test of areColinear method, of class BrokenLine.
     */
    @Test
    public void isColinear ()
    {
        System.out.println("isColinear");
        instance.insertPointAfter(new Point(0, 1), p3);
        assertTrue(instance.isColinear(p3));
        assertFalse(instance.isColinear(p2));
    }

    /**
     * Test of getPoint method, of class BrokenLine.
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
     * Test of getPoints method, of class BrokenLine.
     */
    @Test
    public void getSequence ()
    {
        System.out.println("getSequence");

        List<Point> expResult = Arrays.asList(p0, p1, p2, p3);
        List<Point> result = instance.getPoints();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSequenceString method, of class BrokenLine.
     */
    @Test
    public void getSequenceString ()
    {
        System.out.println("getSequenceString");

        String expResult = "[(1,5) (10,5) (10,1) (1,1)]";
        String result = instance.getSequenceString();
        assertEquals(expResult, result);
    }

    @Before
    public void setUp ()
    {
        ///System.out.println("setUp");
        instance = new BrokenLine(p0, p1, p2, p3);
    }

    /**
     * Test of addPoint method, of class BrokenLine.
     */
    @Test
    public void addPoint ()
    {
        System.out.println("addPoint");
        System.out.println("before: " + instance.getSequenceString());

        Point point = new Point(2, 3);
        instance.addPoint(point);
        System.out.println("after : " + instance.getSequenceString());

        ///assertEquals(5, instance.size());
    }

    /**
     * Test of addPointEmpty method, of class BrokenLine.
     */
    @Test
    public void addPointEmpty ()
    {
        System.out.println("addPointEmpty");
        instance = new BrokenLine();
        System.out.println("before: " + instance.getSequenceString());

        Point point = new Point(2, 3);
        instance.addPoint(point);
        System.out.println("after : " + instance.getSequenceString());

        ///assertEquals(1, instance.size());
    }

    /**
     * Test of findPoint method, of class BrokenLine.
     */
    @Test
    public void findPoint ()
    {
        Point expResult = p2;
        System.out.println("findPoint " + expResult);
        System.out.println("before: " + instance.getSequenceString());

        Point point = new Point(11, 2);
        assertEquals(p1, instance.findPoint(point));
    }

    /**
     * Test of findSegment method, of class BrokenLine.
     */
    @Test
    public void findSegment ()
    {
        System.out.println("findSegment");
        System.out.println("before: " + instance.getSequenceString());

        Point point = new Point(11, 3);
        assertEquals(p1, instance.findSegment(point));

        instance = new BrokenLine();
        assertEquals(null, instance.findSegment(point));
        instance.addPoint(p1);
        assertEquals(null, instance.findSegment(point));
        instance.addPoint(p2);
        assertEquals(p1, instance.findSegment(point));
    }

    /**
     * Test of indexOf method, of class BrokenLine.
     */
    @Test
    public void indexOf ()
    {
        int expResult = 2;
        int result = instance.indexOf(p2);
        assertEquals(expResult, result);
    }

    /**
     * Test of insertPoint method, of class BrokenLine.
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
     * Test of insertPointAfter method, of class BrokenLine.
     */
    @Test
    public void insertPointAfter ()
    {
        System.out.println("insertPointAfter " + p2);
        System.out.println("before: " + instance.getSequenceString());

        Point point = new Point(5, 2);
        instance.insertPointAfter(point, p2);
        System.out.println("after : " + instance.getSequenceString());
        assertEquals(3, instance.indexOf(point));
    }

    /**
     * Test of movePoint method, of class BrokenLine.
     */
    @Test
    public void movePoint ()
    {
        System.out.println("movePoint " + p1);
        System.out.println("before: " + instance.getSequenceString());

        Point location = new Point(11, 6);
        p1.setLocation(location);
        
        System.out.println("after : " + instance.getSequenceString());
        assertEquals(location, instance.getPoint(1));
    }

    /**
     * Test of removePoint method, of class BrokenLine.
     */
    @Test
    public void removePoint ()
    {
        System.out.println("removePoint " + p2);
        System.out.println("before: " + instance.getSequenceString());

        instance.removePoint(p2);
        System.out.println("after : " + instance.getSequenceString());
        assertTrue(p3.equals(instance.getPoint(2)));
    }
}
