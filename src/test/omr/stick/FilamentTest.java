//----------------------------------------------------------------------------//
//                                                                            //
//                          F i l a m e n t T e s t                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.stick;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;

import omr.run.Orientation;
import omr.run.Run;

import omr.score.common.PixelPoint;

import omr.sheet.Scale;
import omr.sheet.grid.Filament;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.geom.Point2D;

/**
 * Unitary tests for {@link Filament} class.
 * @author Hervé Bitteur
 */
public class FilamentTest
{
    //~ Instance fields --------------------------------------------------------

    private Scale       scale = new Scale(20, 3);
    private Orientation orientation;
    private GlyphLag    lag;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FilamentTest object.
     */
    public FilamentTest ()
    {
        System.out.println("FilamentTest");
    }

    //~ Methods ----------------------------------------------------------------

    @BeforeClass
    public static void setUpClass ()
        throws Exception
    {
        System.out.println("setUpClass");
    }

    @AfterClass
    public static void tearDownClass ()
        throws Exception
    {
        System.out.println("tearDownClass");
    }

    @Before
    public void setUp ()
    {
        System.out.println("setUp");
    }

    @After
    public void tearDown ()
    {
        System.out.println("tearDown");
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

            Filament     instance = createNakedInstance();
            GlyphSection section = createSectionOne();
            instance.addSection(section);
            instance.drawAscii();
            instance.dump();
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
            int      id = 2;
            boolean  expResult = true;
            boolean  result = instance.containsSection(id);
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of dump method, of class Filament.
     */
    @Test
    public void testDump ()
    {
        System.out.println("+++ dump");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            instance.drawAscii();
            instance.dump();
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
            Filament result = instance.getAncestor();
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
            instance.drawAscii();

            double coord = 100;
            double expResult = 25.5;
            double result = instance.getPositionAt(coord);
            assertEquals(expResult, result, 0.0);

            coord = 115;
            expResult = 28.9;
            result = instance.getPositionAt(coord);
            assertEquals(expResult, result, 0.1);

            coord = 119;
            expResult = 29.5;
            result = instance.getPositionAt(coord);
            assertEquals(expResult, result, 0.1);

            coord = 158;
            expResult = 30;
            result = instance.getPositionAt(coord);
            assertEquals(expResult, result, 0.1);

            coord = 160;
            expResult = 29.7;
            result = instance.getPositionAt(coord);
            assertEquals(expResult, result, 0.1);

            coord = 170;
            expResult = 28;
            result = instance.getPositionAt(coord);
            assertEquals(expResult, result, 0.1);

            coord = 180;
            expResult = 28;
            result = instance.getPositionAt(coord);
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
     * Test of getRefDistance method, of class Filament.
     */
    @Test
    public void testGetRefDistance ()
    {
        System.out.println("+++ getRefDistance");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            Integer  expResult = null;
            Integer  result = instance.getRefDistance();
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of getResultingThicknessAt method, of class Filament.
     */
    @Test
    public void testGetResultingThicknessAt ()
    {
        System.out.println("getResultingThicknessAt");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            instance.include(createFilTwo());
            instance.include(createFilThree());
            instance.drawAscii();

            Filament that = createFilFour();
            that.drawAscii();

            int    coord = 260;
            double expResult = 3;
            double result = instance.getResultingThicknessAt(that, coord);
            System.out.println("resulting thickness: " + result);
            assertEquals(expResult, result, 0.1);
        }
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
            Point2D  expResult = orientation.absolute(
                new Point2D.Double(100, 25.5));
            Point2D  result = instance.getStartPoint();
            instance.drawAscii();
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
            Point2D  expResult = orientation.absolute(
                new Point2D.Double(171, 27.8));
            Point2D  result = instance.getStopPoint();
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
            instance.drawAscii();

            int    coord = 100;
            int    expResult = 2;
            double result = instance.getThicknessAt(coord);
            assertEquals(expResult, result, delta);

            coord = 105;
            expResult = 2;
            result = instance.getThicknessAt(coord);
            assertEquals(expResult, result, delta);

            coord = 110;
            expResult = 2;
            result = instance.getThicknessAt(coord);
            assertEquals(expResult, result, delta);

            coord = 111;
            expResult = 2;
            result = instance.getThicknessAt(coord);
            assertEquals(expResult, result, delta);

            coord = 115;
            expResult = 2;
            result = instance.getThicknessAt(coord);
            assertEquals(expResult, result, delta);

            coord = 125;
            expResult = 8;
            result = instance.getThicknessAt(coord);
            assertEquals(expResult, result, delta);
        }
    }

    /**
     * Test of include method, of class Filament.
     */
    @Test
    public void testInclude ()
    {
        System.out.println("+++ include");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            Filament instance = createFil();
            Filament that = createFilTwo();
            instance.include(that);

            that = createFilThree();
            instance.include(that);

            that = createFilFour();
            instance.include(that);

            instance.drawAscii();
            instance.dump();
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
            instance.include(createFilTwo());
            instance.include(createFilThree());
            instance.include(createFilFour());

            PixelPoint pStart = new PixelPoint(80, 26);
            Point2D    pStop = instance.getStopPoint();
            instance.setEndingPoints(pStart, pStop);

            instance.drawAscii();
            instance.dump();
        }
    }

    /**
     * Test of setRefDistance method, of class Filament.
     */
    @Test
    public void testSetRefDistance ()
    {
        System.out.println("+++ setRefDistance");

        for (Orientation ori : Orientation.values()) {
            setOrientation(ori);

            int      refDist = 0;
            Filament instance = createFil();
            instance.setRefDistance(refDist);
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
            instance.drawAscii();

            assertEquals(0.2, instance.slopeAt(100), 0.1);
            assertEquals(0.2, instance.slopeAt(110), 0.1);
            assertEquals(0.1, instance.slopeAt(120), 0.1);
            assertEquals(0.1, instance.slopeAt(125), 0.1);
            assertEquals(0.1, instance.slopeAt(130), 0.1);
            assertEquals(0.0, instance.slopeAt(135), 0.1);
            assertEquals(-0.1, instance.slopeAt(155), 0.1);
            assertEquals(-0.2, instance.slopeAt(171), 0.1);
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
            instance.drawAscii();
            instance.dump();

            int expResult = 39;
            int result = instance.trueLength();
            assertEquals(expResult, result);
        }
    }

    private void setOrientation (Orientation orientation)
    {
        this.orientation = orientation;
        System.out.println("Orientation: " + orientation);
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
        Filament     fil = createNakedInstance();
        final int    level = 127;
        Run          r1 = new Run(260, 5, level);
        GlyphSection s = lag.createSection(35, r1);
        fil.addSection(s);

        return fil;
    }

    private Filament createFilThree ()
    {
        Filament     fil = createNakedInstance();
        final int    level = 127;
        Run          r1 = new Run(227, 35, level);
        GlyphSection s = lag.createSection(37, r1);
        fil.addSection(s);

        return fil;
    }

    private Filament createFilTwo ()
    {
        Filament     fil = createNakedInstance();
        final int    level = 127;
        Run          r1 = new Run(200, 5, level);
        GlyphSection s = lag.createSection(34, r1);
        Run          r2 = new Run(198, 10, level);
        s.append(r2);
        fil.addSection(s);

        return fil;
    }

    private Filament createNakedInstance ()
    {
        lag = new GlyphLag("lag", StickSection.class, orientation);

        return new Filament(scale);
    }

    private GlyphSection createSectionOne ()
    {
        final int    level = 127;
        Run          r1 = new Run(100, 10, level);
        GlyphSection s = lag.createSection(25, r1);
        Run          r2 = new Run(100, 20, level);
        s.append(r2);

        //s.dump();
        return s;
    }

    private GlyphSection createSectionThree ()
    {
        final int    level = 127;
        Run          r1 = new Run(160, 10, level);
        GlyphSection s = lag.createSection(27, r1);
        Run          r2 = new Run(160, 12, level);
        s.append(r2);

        Run r3 = new Run(160, 5, level);
        s.append(r3);

        //s.dump();
        return s;
    }

    private GlyphSection createSectionTwo ()
    {
        final int    level = 127;
        Run          r1 = new Run(130, 20, level);
        GlyphSection s = lag.createSection(30, r1);
        Run          r2 = new Run(131, 20, level);
        s.append(r2);

        Run r3 = new Run(132, 20, level);
        s.append(r3);

        //s.dump();
        return s;
    }
}
