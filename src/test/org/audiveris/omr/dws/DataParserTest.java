package org.audiveris.omr.dws;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;

import static org.junit.Assert.*;

/**
 * Test for parsing the neural network output.
 *
 * @author Raphael Emberger
 */
public class DataParserTest {
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DataParserTest.class);

    //~ Fields -------------------------------------------------------------------------------------

    private String jsonString;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new DataParserTest object.
     */
    public DataParserTest() {
    }

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
        // TODO: Inquire about how to represent the parsed data.
        DataParser.parse(jsonString);
    }

    /**
     * Extract the sample array from the JSON string.
     */
    @Test
    public void extractArray() {
        JSONArray array = DataParser.extractArray(jsonString);
        assertNotNull(array);
        assertTrue(array.length() > 10);
    }

    /**
     * Handle faulty input appropriately.
     */
    @Test
    public void doNotExtractFaultyArray() {
        String json = "[1001, 159, 1005, 163, \"articStaccatoBelow\"]";
        assertNull(DataParser.extractArray(json));
        json = "[[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]";
        assertNull(DataParser.extractArray(json));
        json = "{[[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]}";
        assertNull(DataParser.extractArray(json));
        json = "{\"Niet!\": [[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]}";
        assertNull(DataParser.extractArray(json));
    }

    /**
     * Parse a sample from the array.
     */
    @Test
    public void parseSample() {
        String json = "[1001, 159, 1005, 163, \"articStaccatoBelow\"]";
        JSONArray array = new JSONArray(json);
        Object sample = DataParser.parseSample(array);
        // TODO: Make a class for the parsed samples.
    }
}