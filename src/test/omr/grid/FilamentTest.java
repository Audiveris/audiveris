//----------------------------------------------------------------------------//
//                                                                            //
//                          F i l a m e n t T e s t                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.lag.BasicLag;
import omr.lag.Lag;
import omr.lag.Section;

import omr.run.Orientation;
import omr.run.Run;

import omr.sheet.Scale;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * Unitary tests for {@link Filament} class.
 *
 * @author Hervé Bitteur
 */
public class FilamentTest
{
    //~ Instance fields --------------------------------------------------------

    private Scale scale = new Scale(20, 3);

    private Orientation orientation;

    private Lag lag;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new FilamentTest object.
     */
    public FilamentTest ()
    {
        System.out.println("FilamentTest");
    }

    //~ Methods ----------------------------------------------------------------
    @Before
    public void setUp ()
    {
        System.out.println("setUp");
    }

    @BeforeClass
    public static void setUpClass ()
            throws Exception
    {
        System.out.println("setUpClass");
    }

    @After
    public void tearDown ()
    {
        System.out.println("tearDown");
    }

    @AfterClass
    public static void tearDownClass ()
            throws Exception
    {
        System.out.println("tearDownClass");
    }

    /**
     * Test of addSection method, of class Filament.
     */
    @Test
    public void testAddSection ()
    {
        System.out.println("+++ addSection");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createNakedInstance();
            Section section = createSectionOne();
            instance.addSection(section);
            System.out.println(instance.asciiDrawing());
            System.out.println(instance.dumpOf());
        }
    }

    /**
     * Test of containsSection method, of class Filament.
     */
    @Test
    public void testContainsSection ()
    {
        System.out.println("+++ containsSection");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            int id = 2;
            boolean expResult = true;
            boolean result = instance.containsSection(id);
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of dumpOf method, of class Filament.
     */
    @Test
    public void testDump ()
    {
        System.out.println("+++ dump");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            instance.asciiDrawing();
            instance.dumpOf();
        }
    }

    /**
     * Test of getAncestor method, of class Filament.
     */
    @Test
    public void testGetAncestor ()
    {
        System.out.println("+++ getAncestor");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            Filament expResult = instance;
            Filament result = (Filament) instance.getAncestor();
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of getPositionAt method, of class Filament.
     */
    @Test
    public void testGetPositionAt ()
    {
        System.out.println("+++ getPositionAt");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            instance.asciiDrawing();

            double coord = 100;
            double expResult = 25.5;
            double result = instance.getPositionAt(coord, orientation);
            assertEquals(expResult, result, 0.0);

            coord = 115;
            expResult = 28.9;
            result = instance.getPositionAt(coord, orientation);
            assertEquals(expResult, result, 0.1);

            coord = 119;
            expResult = 29.5;
            result = instance.getPositionAt(coord, orientation);
            assertEquals(expResult, result, 0.1);

            coord = 158;
            expResult = 30;
            result = instance.getPositionAt(coord, orientation);
            assertEquals(expResult, result, 0.1);

            coord = 160;
            expResult = 29.7;
            result = instance.getPositionAt(coord, orientation);
            assertEquals(expResult, result, 0.1);

            coord = 170;
            expResult = 28;
            result = instance.getPositionAt(coord, orientation);
            assertEquals(expResult, result, 0.1);

            coord = 180;
            expResult = 28;
            result = instance.getPositionAt(coord, orientation);
            assertEquals(expResult, result, 0.1);
        }
    }

    /**
     * Test of getProbeWidth method, of class Filament.
     */
    @Test
    public void testGetProbeWidth ()
    {
        //        System.out.println("getProbeWidth");
        //        Fraction expResult = null;
        //        Fraction result = Filament.getProbeWidth();
        //        assertEquals(expResult, result);
        //        fail("The test case is a prototype.");
    }

    /**
     * Test of getStartPoint method, of class Filament.
     */
    @Test
    public void testGetStartPoint ()
    {
        System.out.println("+++ getStartPoint");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            Point2D expResult = orientation.absolute(
                    new Point2D.Double(100, 25.5));
            Point2D result = instance.getStartPoint(Orientation.HORIZONTAL);
            instance.asciiDrawing();
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of getStopPoint method, of class Filament.
     */
    @Test
    public void testGetStopPoint ()
    {
        System.out.println("+++ getStopPoint");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            Point2D expResult = orientation.absolute(
                    new Point2D.Double(171, 27.8));
            Point2D result = instance.getStopPoint(Orientation.HORIZONTAL);
            assertEquals(expResult.getX(), result.getX(), 0.1);
            assertEquals(expResult.getY(), result.getY(), 0.1);
        }
    }

    /**
     * Test of getThicknessAt method, of class Filament.
     */
    @Test
    public void testGetThicknessAt ()
    {
        System.out.println("+++ getThicknessAt");

        double delta = 0.5;

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            instance.asciiDrawing();

            int coord = 100;
            int expResult = 2;
            double result = instance.getThicknessAt(coord, orientation);
            assertEquals(expResult, result, delta);

            coord = 105;
            expResult = 2;
            result = instance.getThicknessAt(coord, orientation);
            assertEquals(expResult, result, delta);

            coord = 110;
            expResult = 2;
            result = instance.getThicknessAt(coord, orientation);
            assertEquals(expResult, result, delta);

            coord = 111;
            expResult = 2;
            result = instance.getThicknessAt(coord, orientation);
            assertEquals(expResult, result, delta);

            coord = 115;
            expResult = 1;
            result = instance.getThicknessAt(coord, orientation);
            assertEquals(expResult, result, delta);

            coord = 125;
            expResult = 0;
            result = instance.getThicknessAt(coord, orientation);
            assertEquals(expResult, result, delta);
        }
    }

    /**
     * Test of includeSections method, of class Filament.
     */
    @Test
    public void testInclude ()
    {
        System.out.println("+++ include");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            Filament that = createFilTwo();
            instance.stealSections(that);

            that = createFilThree();
            instance.stealSections(that);

            that = createFilFour();
            instance.stealSections(that);

            instance.asciiDrawing();
            instance.dumpOf();
        }
    }

    /**
     * Test of invalidateCache method, of class Filament.
     */
    @Test
    public void testInvalidateCache ()
    {
        System.out.println("+++ invalidateCache");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            instance.invalidateCache();
        }
    }

    /**
     * Test of renderLine method, of class Filament.
     */
    @Test
    public void testRenderLine ()
    {
        //        System.out.println("renderLine");
        //        Graphics2D g = null;
        //        Filament instance = null;
        //        instance.renderLine(g);
        //        fail("The test case is a prototype.");
    }

    /**
     * Test of setEndingPoints method, of class Filament.
     */
    @Test
    public void testSetEndingPoints ()
    {
        System.out.println("+++ setEndingPoints");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            instance.stealSections(createFilTwo());
            instance.stealSections(createFilThree());
            instance.stealSections(createFilFour());

            Point pStart = new Point(80, 26);
            Point2D pStop = instance.getStopPoint(Orientation.HORIZONTAL);
            instance.setEndingPoints(pStart, pStop);

            instance.asciiDrawing();
            instance.dumpOf();
        }
    }

    /**
     * Test of slopeAt method, of class Filament.
     */
    @Test
    public void testSlopeAt ()
    {
        System.out.println("+++ slopeAt");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            instance.asciiDrawing();

            assertEquals(0.2, instance.slopeAt(100, ori), 0.1);
            assertEquals(0.2, instance.slopeAt(110, ori), 0.1);
            assertEquals(0.1, instance.slopeAt(120, ori), 0.1);
            assertEquals(0.1, instance.slopeAt(125, ori), 0.1);
            assertEquals(0.1, instance.slopeAt(130, ori), 0.1);
            assertEquals(0.0, instance.slopeAt(135, ori), 0.1);
            assertEquals(-0.1, instance.slopeAt(155, ori), 0.1);
            assertEquals(-0.2, instance.slopeAt(171, ori), 0.1);
        }
    }

    /**
     * Test of trueLength method, of class Filament.
     */
    @Test
    public void testTrueLength ()
    {
        System.out.println("+++ trueLength");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            instance.asciiDrawing();
            instance.dumpOf();

            int expResult = 39;
            int result = instance.trueLength();
            assertEquals(expResult, result);
        }
    }

    private Filament createFil ()
    {
        Filament fil = createNakedInstance();
        fil.addSection(createSectionOne());
        fil.addSection(createSectionTwo());
        fil.addSection(createSectionThree());

        return fil;
    }

    private Filament createFilFour ()
    {
        Filament fil = createNakedInstance();
        final int level = 127;
        Run r1 = new Run(260, 5, level);
        Section s = lag.createSection(35, r1);
        fil.addSection(s);

        return fil;
    }

    private Filament createFilThree ()
    {
        Filament fil = createNakedInstance();
        final int level = 127;
        Run r1 = new Run(227, 35, level);
        Section s = lag.createSection(37, r1);
        fil.addSection(s);

        return fil;
    }

    private Filament createFilTwo ()
    {
        Filament fil = createNakedInstance();
        final int level = 127;
        Run r1 = new Run(200, 5, level);
        Section s = lag.createSection(34, r1);
        Run r2 = new Run(198, 10, level);
        s.append(r2);
        fil.addSection(s);

        return fil;
    }

    private Filament createNakedInstance ()
    {
        lag = new BasicLag("lag", orientation);

        return new Filament(scale);
    }

    private Section createSectionOne ()
    {
        final int level = 127;
        Run r1 = new Run(100, 10, level);
        Section s = lag.createSection(25, r1);
        Run r2 = new Run(100, 20, level);
        s.append(r2);

        //s.dumpOf();
        return s;
    }

    private Section createSectionThree ()
    {
        final int level = 127;
        Run r1 = new Run(160, 10, level);
        Section s = lag.createSection(27, r1);
        Run r2 = new Run(160, 12, level);
        s.append(r2);

        Run r3 = new Run(160, 5, level);
        s.append(r3);

        //s.dumpOf();
        return s;
    }

    private Section createSectionTwo ()
    {
        final int level = 127;
        Run r1 = new Run(130, 20, level);
        Section s = lag.createSection(30, r1);
        Run r2 = new Run(131, 20, level);
        s.append(r2);

        Run r3 = new Run(132, 20, level);
        s.append(r3);

        //s.dumpOf();
        return s;
    }

    private void setOrientation (Orientation orientation)
    {
        this.orientation = orientation;
        System.out.println("Orientation: " + orientation);
    }
}
