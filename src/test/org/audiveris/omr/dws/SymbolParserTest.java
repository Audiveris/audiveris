package org.audiveris.omr.dws;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.FileInputStream;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Test for parsing the neural network output.
 *
 * @author Raphael Emberger
 */
public class SymbolParserTest {
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolParserTest.class);

    //~ Fields -------------------------------------------------------------------------------------

    private String jsonString;

    //~ Methods ------------------------------------------------------------------------------------
    @Before
    public void setUp() {
        String file = "data/examples/Bach_Fuge_C_DUR.json";
        try (FileInputStream inputStream = new FileInputStream(file)) {
            jsonString = IOUtils.toString(inputStream);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Test the parsing method.
     */
    @Test
    public void parse() {
        ArrayList<Symbol> symbols = SymbolParser.parse(jsonString);
        assertNotNull(symbols);
        assertEquals(438, symbols.size());
        Symbol symbol = symbols.get(1);
        assertEquals("noteheadBlack", symbol.getSymbolId());
    }

    /**
     * Extract the sample array from the JSON string.
     */
    @Test
    public void extractArray() {
        JSONArray array = SymbolParser.extractArray(jsonString);
        assertNotNull(array);
        assertTrue(array.length() > 10);
    }

    /**
     * Handle faulty input appropriately.
     */
    @Test
    public void doNotExtractFaultyArray() {
        String json = "[1001, 159, 1005, 163, \"articStaccatoBelow\"]";
        assertNull(SymbolParser.extractArray(json));
        json = "[[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]";
        assertNull(SymbolParser.extractArray(json));
        json = "{[[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]}";
        assertNull(SymbolParser.extractArray(json));
        json = "{\"Niet!\": [[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]}";
        assertNull(SymbolParser.extractArray(json));
    }

    /**
     * Parse a sample from the array.
     */
    @Test
    public void parseSample() {
        String json = "[1001, 159, 1005, 163, \"articStaccatoBelow\"]";
        JSONArray array = new JSONArray(json);
        Symbol symbol = SymbolParser.parseSymbol(array);
        assertNotNull(symbol);
        Rectangle rectangle = symbol.getRectangle();
        assertEquals(1001, rectangle.x);
        assertEquals(159, rectangle.y);
        assertEquals(4, rectangle.width);
        assertEquals(4, rectangle.height);
        assertEquals("articStaccatoBelow", symbol.getSymbolId());
    }
}