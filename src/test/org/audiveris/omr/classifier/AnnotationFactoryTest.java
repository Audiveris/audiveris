package org.audiveris.omr.classifier;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * Test for the symbol factory.
 *
 * @author Raphael Emberger
 */
public class AnnotationFactoryTest {
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AnnotationFactoryTest.class);

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Tests if the symbols get produced correctly.
     */
    @Test
    public void produceSymbol() {
        String symbolId = "fingering0";
        Annotation annotation = AnnotationFactory.produce(symbolId);
        assertNotNull(annotation);
        assertEquals(symbolId, annotation.getSymbolId());
    }

    /**
     * Tests if the factory returns null if the symbols hasn't been found.
     */
    @Test
    public void doNotProduceSymbol() {
        Annotation annotation = AnnotationFactory.produce("An OmInOuS sYmBoL iD");
        assertNull(annotation);
    }

    /**
     * Tests if the factory alters fields of the source array if the produced symbol
     * is being altered. If so, it doesn't deep copy the symbol before returning.
     */
    @Test
    public void doNotAlterFields() {
        String symbolId = "fingering0";
        Annotation annotation = AnnotationFactory.produce(symbolId);
        annotation.assignCoordinates(0, 1, 2, 3);
        assertNotNull(annotation.getBounds());
        Annotation annotation1 = AnnotationFactory.produce(symbolId);
        assertTrue(annotation1.getBounds().x == 0);
    }
}