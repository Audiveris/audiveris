package org.audiveris.omr.dws;

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
public class SymbolTest {
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolTest.class);

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Tests how the symple assigns coordinates to itself.
     */
    @Test
    public void assignCoordinates() {
        Symbol symbol = new Symbol(0, "test", "test");
        assertNull(symbol.getRectangle());
        symbol.assignCoordinates(1, 2, 3, 4);
        Rectangle rectangle = symbol.getRectangle();
        assertNotNull(rectangle);
        assertEquals(1, rectangle.x);
        assertEquals(2, rectangle.y);
        assertEquals(2, rectangle.width);
        assertEquals(2, rectangle.height);
    }

    /**
     * Tests the deep copying regarding fields.
     */
    @Test
    public void deepCopiesFields() {
        Symbol symbol = new Symbol(0, "test", "test");
        Symbol symbol1 = symbol.deepCopy();
        assertEquals(symbol.getNumber(), symbol1.getNumber());
        assertEquals(symbol.getSymbolId(), symbol1.getSymbolId());
        assertEquals(symbol.getSymbolDesc(), symbol1.getSymbolDesc());
    }

    /**
     * Tests the deep copying regarding coordinates.
     */
    @Test
    public void deepCopiesCoordinates() {
        Symbol symbol = new Symbol(0, "test", "test");
        symbol.assignCoordinates(1, 2, 3, 4);
        Symbol symbol1 = symbol.deepCopy();
        symbol1.assignCoordinates(5, 6, 7, 8);
        assertNotSame(symbol1.getRectangle(), symbol.getRectangle());
    }
}