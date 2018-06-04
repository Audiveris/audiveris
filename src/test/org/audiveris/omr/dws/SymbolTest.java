package org.audiveris.omr.dws;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        assertNull(symbol.getFrom());
        assertNull(symbol.getTo());
        symbol.assignCoordinates(1, 2, 3, 4);
        assertNotNull(symbol.getFrom());
        assertNotNull(symbol.getTo());
        assertEquals(1, symbol.getFrom().x);
        assertEquals(2, symbol.getFrom().y);
        assertEquals(3, symbol.getTo().x);
        assertEquals(4, symbol.getTo().y);
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
        assertNotSame(symbol1.getFrom().x, symbol.getFrom().x);
        assertNotSame(symbol1.getFrom().y, symbol.getFrom().y);
        assertNotSame(symbol1.getTo().x, symbol.getTo().x);
        assertNotSame(symbol1.getTo().y, symbol.getTo().y);
    }
}