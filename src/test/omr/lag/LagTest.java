//-----------------------------------------------------------------------//
//                                                                       //
//                             L a g T e s t                             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.lag;

import omr.util.BaseTestCase;

import static junit.framework.Assert.*;
import junit.framework.*;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.*;

/**
 * Class <code>LagTest</code> gathers some basic tests to exercise unitary
 * Lag features.
 */
public class LagTest
    extends BaseTestCase
{
    //~ Static variables/initializers -------------------------------------

    public static class MyLag
        extends Lag<MyLag, MySection>
    {
        protected MyLag (Oriented orientation)
        {
            super(orientation);
        }
    }

    public static class MySection
        extends Section<MyLag, MySection>
    {
        public MySection()
        {
        }
    }

    //~ Instance variables ------------------------------------------------

    MyLag vLag;
    MyLag hLag;

    //~ Constructors ------------------------------------------------------

    //~ Methods -----------------------------------------------------------

    //-------//
    // setUp //
    //-------//
    protected void setUp ()
    {
        vLag = new MyLag(new VerticalOrientation());
        vLag.setId("My Vertical Lag");
        vLag.setVertexClass(MySection.class);

        hLag = new MyLag(new HorizontalOrientation());
        hLag.setId("My Horizontal Lag");
        hLag.setVertexClass(MySection.class);
    }

    //----------//
    // testHLag //
    //----------//
    public void testHLag()
    {
        assertNotNull("Lag hLag was not allocated", hLag);
        assertFalse("hLag is not vertical", hLag.isVertical());
    }

    public void testHSwitchRef()
    {
        Point cp = new Point(12,34);
        Point xy = hLag.switchRef(cp, null);
        assertEquals("Non expected switch.",
                     cp, xy);
        xy = new Point();
        hLag.switchRef(cp, xy);
        assertEquals("Non expected switch.",
                     cp, xy);
    }

    public void testCreateSectionNoRun()
    {
        try {
            MySection s = hLag.createSection(123, null);
            fail("IllegalArgumentException should be raised"+
                 " when creating section with a null run");
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    public void testHSection()
    {
        // Test of horizontal section
        Run r1 = new Run(100, 10, 127);
        MySection s1 = hLag.createSection(180, r1);
        hLag.dump(null);
        Run r2 = new Run(101, 20, 127);
        s1.append(r2);
        dump("s1 dump:", s1);
        commonAssertions(s1);
        assertEquals("Bad ContourBox", s1.getContourBox(),
                     new Rectangle(100, 180, 21, 2));
    }

//     public void testGetFirstRectRun()
//     {
//         Run r1 = new Run(100, 10, 127);
//         MySection s1 = hLag.createSection(180, r1);
//         assertSame("First run in rectangle.",
//                    r1, hLag.getFirstRectRun(100, 200, 180, 280));
//     }

    //~ Methods -----------------------------------------------------------

    //----------//
    // testVLag //
    //----------//
    public void testVLag()
    {
        assertNotNull("Lag vLag was not allocated", vLag);
        assertTrue("vLag is vertical", vLag.isVertical());
    }

    //----------------//
    // testVSwitchRef //
    //----------------//
    public void testVSwitchRef()
    {
        Point cp = new Point(12,34);
        Point xy = vLag.switchRef(cp, null);
        assertEquals("Expected switch.",
                     new Point(cp.y, cp.x), xy);
        xy = new Point();
        vLag.switchRef(cp, xy);
        assertEquals("Expected switch.",
                     new Point(cp.y, cp.x), xy);
    }

    //--------------//
    // testVSection //
    //--------------//
    public void testVSection()
    {
        // Test of vertical sections
        MySection s2 = vLag.createSection(180, new Run(100, 10, 127));
        s2.append(new Run(101, 20, 127));
        vLag.dump(null);
        dump("s2 dump:", s2);
        commonAssertions(s2);
        assertEquals("Bad ContourBox", s2.getContourBox(),
                     new Rectangle(180, 100, 2, 21));

        MySection s3 = vLag.createSection(180, new Run(100, 10, 127));
        s3.append(new Run(101, 20, 127));

        // And an edge between 2 sections (of the same lag)
        MySection.addEdge(s2, s3);
    }

    //-----------------//
    // testGetSections //
    //-----------------//
    public void testGetSections()
    {
        MySection s2 = vLag.createSection(180, new Run(100, 10, 127));
        s2.append(new Run(101, 20, 127));

        MySection s3 = vLag.createSection(180, new Run(100, 10, 127));
        s3.append(new Run(101, 20, 127));

        List<MySection> sections = new ArrayList<MySection>();
        sections.add(s2);
        sections.add(s3);
        Collection<MySection> lagSections
            = new ArrayList<MySection>(vLag.getSections());
        assertEquals("Retrieved sections.",
                     sections, lagSections);
    }

    //-------------------//
    // testGetSectionsIn //
    //-------------------//
    public void testGetSectionsIn()
    {
        MySection s2 = vLag.createSection(180, new Run(100, 10, 127));
        s2.append(new Run(101, 20, 127));

        MySection s3 = vLag.createSection(200, new Run(150, 10, 127));
        s3.append(new Run(161, 20, 127));
        s3.append(new Run(170, 15, 127));

        List<MySection> founds = null;

        founds = vLag.getSectionsIn(new Rectangle(0,0,0,0));
        assertEquals("No section.", 0, founds.size());

        founds = vLag.getSectionsIn(new Rectangle(100,180, 1, 1));
        assertEquals("One section.", 1, founds.size());

        founds = vLag.getSectionsIn(new Rectangle(100,180, 51, 21));
        assertEquals("Two sections.", 2, founds.size());
    }

    //-------------------//
    // testGetSectionsIn //
    //-------------------//
    public void testPurgeTinySections()
    {
        MySection s2 = vLag.createSection(180, new Run(100, 10, 127));
        s2.append(new Run(101, 20, 127));
        s2.dump();

        MySection s3 = vLag.createSection(200, new Run(150, 10, 127));
        s3.append(new Run(161, 20, 127));
        s3.append(new Run(170, 15, 127));
        s3.dump();

        List<MySection> purged = null;

        purged = vLag.purgeTinySections(4000);
        assertEquals("One section.", 1, purged.size());
        assertSame("s2 must have been purged.", s2, purged.get(0));
        assertEquals("One section left.", 1, vLag.getVertexCount());
    }

    //~ Methods private ---------------------------------------------------

    //------------------//
    // commonAssertions //
    //------------------//
    private void commonAssertions (MySection s)
    {
        assertEquals("Bad Bounds", s.getBounds(),
                            new Rectangle(100, 180, 21, 2));
        assertEquals("Bad Centroid", s.getCentroid(),
                            new Point(109, 180));
        assertTrue("Bad Containment", s.contains(100, 180));
        assertFalse("Bad Containment", s.contains(100, 181));
        assertTrue("Bad Containment", s.contains(101, 181));
        assertFalse("Bad Containment", s.contains(110, 180));
        assertFalse("Bad Containment", s.contains(121, 181));
        assertEquals("Bad FirstPos", s.getFirstPos(), 180);
        assertEquals("Bad LastPos", s.getLastPos(), 181);
        assertEquals("Bad MaxRunLength", s.getMaxRunLength(), 20);
        assertEquals("Bad MeanRunLength", s.getMeanRunLength(), 15);
        assertEquals("Bad RunNb", s.getRunNb(), 2);
        assertEquals("Bad Start", s.getStart(), 100);
        assertEquals("Bad Stop", s.getStop(), 120);
        assertEquals("Bad Weight", s.getWeight(), 30);
    }

    //------//
    // dump //
    //------//
    private void dump (String title,
                       MySection section)
    {
        if (title != null) {
            System.out.println();
            System.out.println(title);
        }

        System.out.println(section.toString());

        //        System.out.println ("getRunAt(0)=" + section.getRunAt(0));
        System.out.println("getBounds=" + section.getBounds());
        System.out.println("getCentroid=" + section.getCentroid());
        System.out.println("getContourBox=" + section.getContourBox());
        System.out.println("getFirstAdjacency=" + section.getFirstAdjacency());
        System.out.println("getFirstPos=" + section.getFirstPos());
        System.out.println("getFirstRun=" + section.getFirstRun());
        System.out.println("getInfo=" + section.getInfo());
        System.out.println("getLastAdjacency=" + section.getLastAdjacency());
        System.out.println("getLastPos=" + section.getLastPos());
        System.out.println("getLastRun=" + section.getLastRun());
        System.out.println("getMaxRunLength=" + section.getMaxRunLength());
        System.out.println("getMeanRunLength=" + section.getMeanRunLength());
        System.out.println("getRunNb=" + section.getRunNb());
        System.out.println("getStart=" + section.getStart());
        System.out.println("getStop=" + section.getStop());
        System.out.println("getWeight=" + section.getWeight());
        System.out.println("getContour=");
        dump(section.getContour());
        //section.getLag().dump("\nLag dump:");
        section.drawAscii();
    }

    //------//
    // dump //
    //------//
    private void dump (Polygon polygon)
    {
        for (int i = 0; i < polygon.npoints; i++) {
            System.out.println(i + ": x" + polygon.xpoints[i] + ",y"
                               + polygon.ypoints[i]);
        }
    }
}

