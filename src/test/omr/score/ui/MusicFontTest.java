/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.score.ui;

import omr.glyph.Shape;

import omr.score.ui.MusicFont.Alignment;
import omr.score.ui.MusicFont.Alignment.Horizontal;
import omr.score.ui.MusicFont.Alignment.Vertical;
import omr.score.ui.MusicFont.CharDesc;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Font;
import java.awt.Point;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author Etiolles
 */
public class MusicFontTest
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new MusicFontTest object.
     */
    public MusicFontTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    @BeforeClass
    public static void setUpClass ()
        throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass ()
        throws Exception
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
     * Test of getCharDesc method, of class MusicFont.
     */
    @Test
    public void testGetCharDesc ()
    {
        System.out.println("getCharDesc");

        Shape    shape = Shape.F_CLEF;
        CharDesc result = MusicFont.getCharDesc(shape);
        assertEquals(1, result.codes.length);
        assertEquals(63 + 0xf000, result.codes[0]);
    }

    /**
     * Test of getFont method, of class MusicFont.
     */
    @Test
    public void testGetFont ()
    {
        System.out.println("getFont");

        int  staffHeight = 80;
        int  expResult = 219;
        Font result = MusicFont.getFont(staffHeight);
        assertEquals(expResult, result.getSize());
    }

    /**
     * Test of getCodes method, of class MusicFont.
     */
    @Test
    public void testGetPointCodes ()
    {
        System.out.println("getPointCodes");

        Shape shape = Shape.F_CLEF;
        int[] expResult = new int[] { 63 + 0xf000 };
        int[] result = MusicFont.getCodes(shape);
        assertEquals(expResult.length, result.length);

        for (int i = 0; i < expResult.length; i++) {
            assertEquals(expResult[i], result[i]);
        }
    }

    /**
     * Test of toStartPoint method, of class MusicFont.
     */
    @Test
    public void testToStartPoint ()
    {
        System.out.println("toStartPoint");
        double delta = 0.5d;

        CharDesc          desc = MusicFont.getCharDesc(Shape.F_CLEF);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Font              font = new Font("SToccata", Font.PLAIN, 100);

        TextLayout        textLayout = new TextLayout(
            desc.getString(),
            font,
            frc);
        Rectangle2D       rect = textLayout.getBounds();
        assertEquals(
            new Rectangle2D.Float(0f, -35.359375f, 24.65625f, 28.515625f),
            rect);

        Alignment alignment = new Alignment(Horizontal.CENTER, Vertical.TOP);
        Point2D     expResult = new Point2D.Double(-12, 35);
        Point2D     result = MusicFont.toOrigin(textLayout.getBounds(), alignment);
        assertEquals(expResult.getX(), result.getX(), delta);
        assertEquals(expResult.getY(), result.getY(), delta);

        AffineTransform AT = AffineTransform.getScaleInstance(2.0, 2.);
        font = font.deriveFont(AT);
        textLayout = new TextLayout(desc.getString(), font, frc);
        rect = textLayout.getBounds();
        assertEquals(
            new Rectangle2D.Float(0f, -70.703125f, 49.3125f, 57.03125f),
            rect);

        alignment = new Alignment(Horizontal.CENTER, Vertical.TOP);
        expResult = new Point(-25, 71);
        result = MusicFont.toOrigin(textLayout.getBounds(), alignment);
        assertEquals(expResult.getX(), result.getX(), delta);
        assertEquals(expResult.getY(), result.getY(), delta);
    }
}
