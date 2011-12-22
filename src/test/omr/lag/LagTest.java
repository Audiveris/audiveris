//----------------------------------------------------------------------------//
//                                                                            //
//                               L a g T e s t                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.math.Histogram;

import omr.run.Orientation;
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

    // Lags and RunsTable instances
    Lag       vLag;
    RunsTable vTable;
    Lag       hLag;
    RunsTable hTable;

    //~ Methods ----------------------------------------------------------------

    //------------------------//
    // testCreateSectionNoRun //
    //------------------------//
    public void testCreateSectionNoRun ()
    {
        try {
            Section s = hLag.createSection(123, null);
            fail(
                "IllegalArgumentException should be raised" +
                " when creating section with a null run");
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    public void testGetRectangleCendroidEmpty ()
    {
        Section        s2 = hLag.createSection(
            180,
            createRun(hTable, 180, 100, 10));

        PixelRectangle roi = new PixelRectangle(0, 0, 20, 20);
        Point          pt = null;

        pt = s2.getRectangleCentroid(roi);
        System.out.println("roi=" + roi + " pt=" + pt);
        assertNull("External roi should give a null centroid", pt);
    }

    public void testGetRectangleCendroidHori ()
    {
        int     p = 180;
        Section s1 = hLag.createSection(180, createRun(hTable, p++, 100, 10));
        s1.append(createRun(hTable, p++, 102, 20));
        s1.append(createRun(hTable, p++, 102, 20));
        s1.append(createRun(hTable, p++, 102, 20));

        PixelRectangle roi = null;
        Point          pt = null;

        roi = new PixelRectangle(100, 180, 1, 1);
        pt = s1.getRectangleCentroid(roi);
        System.out.println("roi=" + roi + " pt=" + pt);

        PixelPoint expected = new PixelPoint(100, 180);
        assertEquals("Wrong pt", expected, pt);
    }

    public void testGetRectangleCendroidNull ()
    {
        Section s2 = hLag.createSection(180, createRun(hTable, 180, 100, 10));

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
        int     p = 50;
        Section s1 = vLag.createSection(p, createRun(vTable, p, 100, 10));
        p++;
        s1.append(createRun(vTable, p++, 102, 20));
        s1.append(createRun(vTable, p++, 102, 20));
        s1.append(createRun(vTable, p++, 102, 20));
        s1.drawAscii();

        PixelRectangle roi = null;
        Point          pt = null;

        roi = new PixelRectangle(48, 102, 5, 3);
        pt = s1.getRectangleCentroid(roi);
        System.out.println("roi=" + roi + " pt=" + pt);

        PixelPoint expected = new PixelPoint(51, 103);
        assertEquals("Wrong pt", expected, pt);
    }

    //-----------------//
    // testGetSections //
    //-----------------//
    public void testGetSections ()
    {
        Section s2 = vLag.createSection(180, new Run(100, 10, 127));
        s2.append(new Run(101, 20, 127));

        Section s3 = vLag.createSection(180, new Run(100, 10, 127));
        s3.append(new Run(101, 20, 127));

        List<Section> sections = new ArrayList<Section>();
        sections.add(s2);
        sections.add(s3);
        Collections.sort(sections, Section.idComparator);

        List<Section> lagSections = new ArrayList<Section>(vLag.getSections());
        Collections.sort(lagSections, Section.idComparator);
        assertEquals("Retrieved sections.", sections, lagSections);
    }

    //----------//
    // testHLag //
    //----------//
    public void testHLag ()
    {
        assertNotNull("Lag hLag was not allocated", hLag);
        assertFalse("hLag is not vertical", hLag.getOrientation().isVertical());
    }

    public void testHSection ()
    {
        // Test of horizontal section
        Run     r1 = new Run(100, 10, 127);
        Section s1 = hLag.createSection(180, r1);
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
        PixelPoint xy = hLag.getOrientation()
                            .absolute(cp);
        assertEquals("Non expected switch.", cp, xy);
    }

    //---------------//
    // testHoriented //
    //---------------//
    public void testHoriented ()
    {
        PixelPoint xy = new PixelPoint(12, 34);
        Point      cp = hLag.getOrientation()
                            .oriented(xy);
        assertEquals("Non expected switch.", cp, xy);
    }

    //--------------------//
    // testLookupSections //
    //--------------------//
    public void testLookupSections ()
    {
        Section s2 = vLag.createSection(180, new Run(100, 10, 127));
        s2.append(new Run(101, 20, 127));

        Section s3 = vLag.createSection(200, new Run(150, 10, 127));
        s3.append(new Run(161, 20, 127));
        s3.append(new Run(170, 15, 127));

        Set<Section> founds = null;

        founds = vLag.lookupSections(new PixelRectangle(0, 0, 0, 0));
        assertEquals("No section.", 0, founds.size());

        founds = vLag.lookupSections(new PixelRectangle(180, 100, 2, 21));
        assertEquals("One section.", 1, founds.size());

        founds = vLag.lookupSections(new PixelRectangle(180, 100, 23, 85));
        assertEquals("Two sections.", 2, founds.size());
    }

    //---------------//
    // testTranslate //
    //---------------//
    public void testTranslate ()
    {
        Section s1 = vLag.createSection(1, createRun(vTable, 1, 2, 5));
        s1.append(createRun(vTable, 2, 0, 3));
        s1.append(createRun(vTable, 3, 1, 2));
        s1.append(createRun(vTable, 4, 1, 1));
        s1.append(createRun(vTable, 5, 1, 6));
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
        assertTrue("vLag is vertical", vLag.getOrientation().isVertical());
    }

    //---------------//
    // testVLagHisto //
    //---------------//
    public void testVLagHisto ()
    {
        Section s1 = vLag.createSection(1, createRun(vTable, 1, 2, 5));
        s1.append(createRun(vTable, 2, 0, 3));
        s1.append(createRun(vTable, 3, 1, 2));
        s1.append(createRun(vTable, 4, 1, 1));
        s1.append(createRun(vTable, 5, 1, 6));
        s1.drawAscii();

        Roi                roi = new BasicRoi(new PixelRectangle(0, 0, 6, 7));

        String             expV = "{Histogram 1-5 size:5 [1:5 2:3 3:2 4:1 5:6]}";
        String             expH = "{Histogram 0-6 size:7 [0:1 1:4 2:4 3:2 4:2 5:2 6:2]}";

        Histogram<Integer> histoVS = roi.getSectionHistogram(
            Orientation.VERTICAL,
            Collections.singletonList(s1));
        System.out.println("histoVS=" + histoVS);
        assertEquals("Wrong histogram", expV, histoVS.toString());

        Histogram<Integer> histoHS = roi.getSectionHistogram(
            Orientation.HORIZONTAL,
            Collections.singletonList(s1));
        System.out.println("histoHS=" + histoHS);
        assertEquals("Wrong histogram", expH, histoHS.toString());

        Histogram<Integer> histoVR = roi.getRunHistogram(
            Orientation.VERTICAL,
            vTable);
        System.out.println("histoVR=" + histoVR);
        assertEquals("Wrong histogram", expV, histoVR.toString());

        Histogram<Integer> histoHR = roi.getRunHistogram(
            Orientation.HORIZONTAL,
            vTable);
        System.out.println("histoHR=" + histoHR);
        assertEquals("Wrong histogram", expH, histoHR.toString());
    }

    //--------------//
    // testVSection //
    //--------------//
    public void testVSection ()
    {
        // Test of vertical sections
        Section s2 = vLag.createSection(180, new Run(100, 10, 127));
        s2.append(new Run(101, 20, 127));
        vLag.dump(null);
        dump("s2 dump:", s2);
        commonAssertions(s2);
        assertEquals(
            "Bad ContourBox",
            s2.getContourBox(),
            new Rectangle(180, 100, 2, 21));

        Section s3 = vLag.createSection(180, new Run(100, 10, 127));
        s3.append(new Run(101, 20, 127));

        // And an edge between 2 sections (of the same lag)
        s2.addTarget(s3);
    }

    //---------------//
    // testVabsolute //
    //---------------//
    public void testVabsolute ()
    {
        Point      cp = new Point(12, 34);
        PixelPoint xy = vLag.getOrientation()
                            .absolute(cp);
        assertEquals("Expected switch.", new Point(cp.y, cp.x), xy);
    }

    //---------------//
    // testVoriented //
    //---------------//
    public void testVoriented ()
    {
        PixelPoint xy = new PixelPoint(12, 34);
        Point      cp = vLag.getOrientation()
                            .absolute(xy);
        assertEquals("Expected switch.", new Point(cp.y, cp.x), xy);
    }

    //-------//
    // setUp //
    //-------//
    @Override
    protected void setUp ()
    {
        vLag = new BasicLag("My Vertical Lag", Orientation.VERTICAL);
        vTable = new RunsTable(
            "Vert Runs",
            Orientation.VERTICAL,
            new Dimension(100, 200)); // Absolute
        vLag.setRuns(vTable);

        hLag = new BasicLag("My Horizontal Lag", Orientation.HORIZONTAL);
        hTable = new RunsTable(
            "Hori Runs",
            Orientation.HORIZONTAL,
            new Dimension(100, 200)); // Absolute
        hLag.setRuns(hTable);
    }

    //------------------//
    // commonAssertions //
    //------------------//
    private void commonAssertions (Section s)
    {
        Orientation ori = s.getGraph()
                           .getOrientation();
        assertEquals(
            "Bad Bounds",
            s.getOrientedBounds(),
            new Rectangle(100, 180, 21, 2));
        assertEquals(
            "Bad Centroid",
            s.getCentroid(),
            ori.absolute(new Point(109, 180)));

        //        assertTrue("Bad Containment", s.contains(100, 180));
        //        assertFalse("Bad Containment", s.contains(100, 181));
        //        assertTrue("Bad Containment", s.contains(101, 181));
        //        assertFalse("Bad Containment", s.contains(110, 180));
        //        assertFalse("Bad Containment", s.contains(121, 181));
        Point pt;
        pt = ori.absolute(new Point(100, 180));
        assertTrue("Bad Containment", s.contains(pt.x, pt.y));

        pt = ori.absolute(new Point(100, 181));
        assertFalse("Bad Containment", s.contains(pt.x, pt.y));

        pt = ori.absolute(new Point(101, 181));
        assertTrue("Bad Containment", s.contains(pt.x, pt.y));

        pt = ori.absolute(new Point(110, 180));
        assertFalse("Bad Containment", s.contains(pt.x, pt.y));

        pt = ori.absolute(new Point(121, 181));
        assertFalse("Bad Containment", s.contains(pt.x, pt.y));

        assertEquals("Bad FirstPos", s.getFirstPos(), 180);
        assertEquals("Bad LastPos", s.getLastPos(), 181);
        assertEquals("Bad MaxRunLength", s.getMaxRunLength(), 20);
        assertEquals("Bad MeanRunLength", s.getMeanRunLength(), 15);
        assertEquals("Bad RunNb", s.getRunCount(), 2);
        assertEquals("Bad Start", s.getStartCoord(), 100);
        assertEquals("Bad Stop", s.getStopCoord(), 120);
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
    private void dump (String  title,
                       Section section)
    {
        if (title != null) {
            System.out.println();
            System.out.println(title);
        }

        System.out.println(section.toString());

        //        System.out.println ("getRunAtPos(0)=" + section.getRunAtPos(0));
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
        System.out.println("getRunNb=" + section.getRunCount());
        System.out.println("getStart=" + section.getStartCoord());
        System.out.println("getStop=" + section.getStopCoord());
        System.out.println("getWeight=" + section.getWeight());
        System.out.println("getContour=");
        dump(section.getPolygon());
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
}
