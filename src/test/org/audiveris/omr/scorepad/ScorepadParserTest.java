package org.audiveris.omr.scorepad;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
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
public class ScorepadParserTest {
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ScorepadParserTest.class);

    //~ Fields -------------------------------------------------------------------------------------

    private String jsonString;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new ScorepadParserTest object.
     */
    public ScorepadParserTest() {
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
        ScorepadParser.parse(jsonString);
    }

    /**
     * Extract the sample array from the JSON string.
     */
    @Test
    public void extractArray() {
        JSONArray array = ScorepadParser.extractArray(jsonString);
        assertNotNull(array);
        assertTrue(array.length() > 10);
    }

    /**
     * Handle faulty input appropriately.
     */
    @Test
    public void dontExtractFaultyArray() {
        String json = "[1001, 159, 1005, 163, \"articStaccatoBelow\"]";
        assertNull(ScorepadParser.extractArray(json));
        json = "[[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]";
        assertNull(ScorepadParser.extractArray(json));
        json = "{[[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]}";
        assertNull(ScorepadParser.extractArray(json));
        json = "{\"Niet!\": [[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]}";
        assertNull(ScorepadParser.extractArray(json));
    }

    /**
     * Parse a sample from the array.
     */
    @Test
    public void parseSample() {
        String json = "{\"test\": [1001, 159, 1005, 163, \"articStaccatoBelow\"]}";
        JSONArray array = new JSONObject(json).getJSONArray("test");
        Object sample = ScorepadParser.parseSample(array);
        // TODO: Make a class for the parsed samples.
    }
}