//----------------------------------------------------------------------------//
//                                                                            //
//                               L a g T e s t                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.math.Histogram;

import omr.run.Orientation;
import omr.run.Oriented;
import omr.run.Run;
import omr.run.RunsTable;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.util.BaseTestCase;
import static junit.framework.Assert.*;

import java.awt.Dimension;
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
    //~ Instance fields --------------------------------------------------------

    MyLag vLag;
    MyLag hLag;

    //~ Methods ----------------------------------------------------------------

    //------------------------//
    // testCreateSectionNoRun //
    //------------------------//
    public void testCreateSectionNoRun ()
    {
        try {
            MySection s = hLag.createSection(123, null);
            fail(
                "IllegalArgumentException should be raised" +
                " when creating section with a null run");
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    public void testGetRectangleCendroidEmpty ()
    {
        MySection      s2 = hLag.createSection(180, new Run(100, 10, 127));

        PixelRectangle roi = new PixelRectangle(0, 0, 20, 20);
        Point          pt = null;

        pt = s2.getRectangleCentroid(roi);
        System.out.println("roi=" + roi + " pt=" + pt);
        assertNull("External roi should give a null centroid", pt);
    }

    public void testGetRectangleCendroidHori ()
    {
        MySection s2 = hLag.createSection(180, new Run(100, 10, 127));
        s2.append(new Run(102, 20, 127));
        s2.append(new Run(102, 20, 127));
        s2.append(new Run(102, 20, 127));
        s2.setSignature();

        PixelRectangle roi = null;
        Point          pt = null;

        roi = new PixelRectangle(100, 180, 1, 1);
        pt = s2.getRectangleCentroid(roi);
        System.out.println("roi=" + roi + " pt=" + pt);

        PixelPoint expected = new PixelPoint(100, 180);
        assertEquals("Wrong pt", expected, pt);
    }

    public void testGetRectangleCendroidNull ()
    {
        MySection s2 = hLag.createSection(180, new Run(100, 10, 127));

        try {
            PixelRectangle roi = null;
            System.out.println("roi=" + roi);

            Point pt = s2.getRectangleCentroid(roi);
            fail(
                "IllegalArgumentException should be raised" +
                " when rectangle of interest is null");
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    public void testGetRectangleCendroidVert ()
    {
        MySection s2 = vLag.createSection(180, new Run(100, 10, 127));
        s2.append(new Run(102, 20, 127));
        s2.append(new Run(102, 20, 127));
        s2.append(new Run(102, 20, 127));
        s2.setSignature();

        PixelRectangle roi = null;
        Point          pt = null;

        roi = new PixelRectangle(179, 102, 4, 3);
        pt = s2.getRectangleCentroid(roi);
        System.out.println("roi=" + roi + " pt=" + pt);

        PixelPoint expected = new PixelPoint(181, 104);
        assertEquals("Wrong pt", expected, pt);
    }

    //-----------------//
    // testGetSections //
    //-----------------//
    public void testGetSections ()
    {
        MySection s2 = vLag.createSection(180, new Run(100, 10, 127));
        s2.append(new Run(101, 20, 127));
        s2.setSignature();

        MySection s3 = vLag.createSection(180, new Run(100, 10, 127));
        s3.append(new Run(101, 20, 127));
        s3.setSignature();

        List<MySection> sections = new ArrayList<MySection>();
        sections.add(s2);
        sections.add(s3);
        Collections.sort(sections, Section.idComparator);

        List<MySection> lagSections = new ArrayList<MySection>(
            vLag.getSections());
        Collections.sort(lagSections, Section.idComparator);
        assertEquals("Retrieved sections.", sections, lagSections);
    }

    //-------------------//
    // testGetSectionsIn //
    //-------------------//
    public void testGetSectionsIn ()
    {
        MySection s2 = vLag.createSection(180, new Run(100, 10, 127));
        s2.append(new Run(101, 20, 127));
        s2.setSignature();

        MySection s3 = vLag.createSection(200, new Run(150, 10, 127));
        s3.append(new Run(161, 20, 127));
        s3.append(new Run(170, 15, 127));
        s3.setSignature();

        List<MySection> founds = null;

        founds = vLag.getSectionsIn(new Rectangle(0, 0, 0, 0));
        assertEquals("No section.", 0, founds.size());

        founds = vLag.getSectionsIn(new Rectangle(100, 180, 1, 1));
        assertEquals("One section.", 1, founds.size());

        founds = vLag.getSectionsIn(new Rectangle(100, 180, 51, 21));
        assertEquals("Two sections.", 2, founds.size());
    }

    //----------//
    // testHLag //
    //----------//
    public void testHLag ()
    {
        assertNotNull("Lag hLag was not allocated", hLag);
        assertFalse("hLag is not vertical", hLag.isVertical());
    }

    public void testHSection ()
    {
        // Test of horizontal section
        Run       r1 = new Run(100, 10, 127);
        MySection s1 = hLag.createSection(180, r1);
        hLag.dump(null);

        Run r2 = new Run(101, 20, 127);
        s1.append(r2);
        dump("s1 dump:", s1);
        commonAssertions(s1);
        assertEquals(
            "Bad ContourBox",
            s1.getContourBox(),
            new Rectangle(100, 180, 21, 2));
    }

    //---------------//
    // testHabsolute //
    //---------------//
    public void testHabsolute ()
    {
        Point      cp = new Point(12, 34);
        PixelPoint xy = hLag.absolute(cp);
        assertEquals("Non expected switch.", cp, xy);
    }

    //---------------//
    // testHoriented //
    //---------------//
    public void testHoriented ()
    {
        PixelPoint xy = new PixelPoint(12, 34);
        Point      cp = hLag.oriented(xy);
        assertEquals("Non expected switch.", cp, xy);
    }

    //-------------------//
    // testGetSectionsIn //
    //-------------------//
    public void testPurgeTinySections ()
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

    //---------------//
    // testSignature //
    //---------------//
    public void testSignature ()
    {
        MySection s2 = vLag.createSection(180, new Run(100, 10, 127));
        s2.append(new Run(101, 20, 127));

        try {
            SectionSignature sig = s2.getSignature();
            fail(
                "Exception should be raised" +
                " when getting a null vertex signature");
        } catch (Exception expected) {
            checkException(expected);
        }

        s2.setSignature();

        try {
            s2.setSignature();
        } catch (Exception ex) {
            fail(
                "Exception raised" +
                " when setting signature with equal value");
        }

        MySection s3 = vLag.createSection(200, new Run(150, 10, 127));
        s3.append(new Run(161, 20, 127));
        s3.append(new Run(170, 15, 127));
        s3.setSignature();

        assertSame(
            "Vertex retrieved via signature",
            s2,
            vLag.getVertexBySignature(s2.getSignature()));

        assertNotSame(
            "Vertex retrieved via wrong signature",
            s2,
            vLag.getVertexBySignature(s3.getSignature()));
    }

    //---------------//
    // testTranslate //
    //---------------//
    public void testTranslate ()
    {
        RunsTable table = new RunsTable(
            "Vertical",
            Orientation.VERTICAL,
            new Dimension(20, 20));
        vLag.addRuns(table);

        MySection s1 = vLag.createSection(1, createRun(table, 1, 2, 5));
        s1.append(createRun(table, 2, 0, 3));
        s1.append(createRun(table, 3, 1, 2));
        s1.append(createRun(table, 4, 1, 1));
        s1.append(createRun(table, 5, 1, 6));
        dump("Before translation", s1);

        PixelPoint vector = new PixelPoint(10, 2);
        s1.translate(vector);
        dump("After translation", s1);
    }

    //----------//
    // testVLag //
    //----------//
    public void testVLag ()
    {
        assertNotNull("Lag vLag was not allocated", vLag);
        assertTrue("vLag is vertical", vLag.isVertical());
    }

    //---------------//
    // testVLagHisto //
    //---------------//
    public void testVLagHisto ()
    {
        RunsTable table = new RunsTable(
            "Vertical",
            Orientation.VERTICAL,
            new Dimension(20, 20));
        vLag.addRuns(table);

        MySection s1 = vLag.createSection(1, createRun(table, 1, 2, 5));
        s1.append(createRun(table, 2, 0, 3));
        s1.append(createRun(table, 3, 1, 2));
        s1.append(createRun(table, 4, 1, 1));
        s1.append(createRun(table, 5, 1, 6));
        s1.drawAscii();

        MyLag.Roi          roi = vLag.createAbsoluteRoi(
            new PixelRectangle(0, 0, 6, 7));
        Histogram<Integer> histoV = roi.getHistogram(Orientation.VERTICAL);
        histoV.print(System.out);

        Histogram<Integer> histoH = roi.getHistogram(Orientation.HORIZONTAL);
        histoH.print(System.out);
    }

    //--------------//
    // testVSection //
    //--------------//
    public void testVSection ()
    {
        // Test of vertical sections
        MySection s2 = vLag.createSection(180, new Run(100, 10, 127));
        s2.append(new Run(101, 20, 127));
        vLag.dump(null);
        dump("s2 dump:", s2);
        commonAssertions(s2);
        assertEquals(
            "Bad ContourBox",
            s2.getContourBox(),
            new Rectangle(180, 100, 2, 21));

        MySection s3 = vLag.createSection(180, new Run(100, 10, 127));
        s3.append(new Run(101, 20, 127));

        // And an edge between 2 sections (of the same lag)
        MySection.addEdge(s2, s3);
    }

    //---------------//
    // testVabsolute //
    //---------------//
    public void testVabsolute ()
    {
        Point      cp = new Point(12, 34);
        PixelPoint xy = vLag.absolute(cp);
        assertEquals("Expected switch.", new Point(cp.y, cp.x), xy);
    }

    //---------------//
    // testVoriented //
    //---------------//
    public void testVoriented ()
    {
        PixelPoint xy = new PixelPoint(12, 34);
        Point      cp = vLag.absolute(xy);
        assertEquals("Expected switch.", new Point(cp.y, cp.x), xy);
    }

    //-------//
    // setUp //
    //-------//
    @Override
    protected void setUp ()
    {
        vLag = new MyLag("My Vertical Lag", Orientation.VERTICAL);
        hLag = new MyLag("My Horizontal Lag", Orientation.HORIZONTAL);
    }

    //------------------//
    // commonAssertions //
    //------------------//
    private void commonAssertions (MySection s)
    {
        assertEquals(
            "Bad Bounds",
            s.getOrientedBounds(),
            new Rectangle(100, 180, 21, 2));
        assertEquals(
            "Bad Centroid",
            s.getCentroid(),
            s.getGraph().absolute(new Point(109, 180)));
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

    //-----------//
    // createRun //
    //-----------//
    private Run createRun (RunsTable table,
                           int       alignment,
                           int       start,
                           int       length)
    {
        Run run = new Run(start, length, 127);

        table.getSequence(alignment)
             .add(run);

        return run;
    }

    //------//
    // dump //
    //------//
    private void dump (String    title,
                       MySection section)
    {
        if (title != null) {
            System.out.println();
            System.out.println(title);
        }

        System.out.println(section.toString());

        //        System.out.println ("getRunAt(0)=" + section.getRunAt(0));
        System.out.println("getOrientedBounds=" + section.getOrientedBounds());
        System.out.println("getCentroid=" + section.getCentroid());
        System.out.println("getContourBox=" + section.getContourBox());
        System.out.println("getFirstAdjacency=" + section.getFirstAdjacency());
        System.out.println("getFirstPos=" + section.getFirstPos());
        System.out.println("getFirstRun=" + section.getFirstRun());
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
            System.out.println(
                i + ": x" + polygon.xpoints[i] + ",y" + polygon.ypoints[i]);
        }
    }

    //------//
    // dump //
    //------//
    private void dump (int[]  ints,
                       String title)
    {
        System.out.println("\n" + title);

        for (int i = 0; i < ints.length; i++) {
            System.out.print(i + ":");

            for (int k = 0; k < ints[i]; k++) {
                System.out.print("x");
            }

            System.out.println();
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------//
    // MyLag //
    //-------//
    public static class MyLag
        extends Lag<MyLag, MySection>
    {
        //~ Constructors -------------------------------------------------------

        protected MyLag (String   name,
                         Oriented orientation)
        {
            super(name, MySection.class, orientation);
        }
    }

    //-----------//
    // MySection //
    //-----------//
    public static class MySection
        extends Section<MyLag, MySection>
    {
    }
}
