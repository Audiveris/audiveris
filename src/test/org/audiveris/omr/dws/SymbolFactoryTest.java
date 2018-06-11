package org.audiveris.omr.dws;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * Test for the symbol factory.
 *
 * @author Raphael Emberger
 */
public class SymbolFactoryTest {
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolFactoryTest.class);

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Tests if the symbols get produced correctly.
     */
    @Test
    public void produceSymbol() {
        String symbolId = "fingering0";
        Symbol symbol = SymbolFactory.produce(symbolId);
        assertNotNull(symbol);
        assertEquals(symbolId, symbol.getSymbolId());
    }

    /**
     * Tests if the factory returns null if the symbols hasn't been found.
     */
    @Test
    public void doNotProduceSymbol() {
        Symbol symbol = SymbolFactory.produce("An OmInOuS sYmBoL iD");
        assertNull(symbol);
    }

    /**
     * Tests if the factory alters fields of the source array if the produced symbol
     * is being altered. If so, it doesn't deep copy the symbol before returning.
     */
    @Test
    public void doNotAlterFields() {
        String symbolId = "fingering0";
        Symbol symbol = SymbolFactory.produce(symbolId);
        symbol.assignCoordinates(0, 1, 2, 3);
        assertNotNull(symbol.getRectangle());
        Symbol symbol1 = SymbolFactory.produce(symbolId);
        assertNull(symbol1.getRectangle());
    }
}