package org.audiveris.omr.classifier;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

import static org.junit.Assert.*;

/**
 * Test for a sample.
 *
 * @author Raphael Emberger
 */
public class AnnotationTest {
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AnnotationTest.class);

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Tests how the symple assigns coordinates to itself.
     */
    @Test
    public void assignCoordinates() {
        Annotation annotation = new Annotation(0, "articStaccatoBelow", "test");
        assertTrue(annotation.getBounds().x == 0);
        annotation.assignCoordinates(1, 2, 3, 4);
        Rectangle bounds = annotation.getBounds();
        assertNotNull(bounds);
        assertEquals(1, bounds.x);
        assertEquals(2, bounds.y);
        assertEquals(3, bounds.width);
        assertEquals(3, bounds.height);
    }

    /**
     * Tests the deep copying regarding fields.
     */
    @Test
    public void deepCopiesFields() {
        Annotation annotation = new Annotation(0, "articStaccatoBelow", "test");
        Annotation annotation1 = annotation.deepCopy();
        assertEquals(annotation.getNumber(), annotation1.getNumber());
        assertEquals(annotation.getSymbolId(), annotation1.getSymbolId());
        assertEquals(annotation.getSymbolDesc(), annotation1.getSymbolDesc());
    }

    /**
     * Tests the deep copying regarding coordinates.
     */
    @Test
    public void deepCopiesCoordinates() {
        Annotation annotation = new Annotation(0, "articStaccatoBelow", "test");
        annotation.assignCoordinates(1, 2, 3, 4);
        Annotation annotation1 = annotation.deepCopy();
        annotation1.assignCoordinates(5, 6, 7, 8);
        assertNotSame(annotation1.getBounds(), annotation.getBounds());
    }
}