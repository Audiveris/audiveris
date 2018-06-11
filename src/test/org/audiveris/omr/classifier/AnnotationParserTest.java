package org.audiveris.omr.classifier;

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
public class AnnotationParserTest {
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AnnotationParserTest.class);

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
        ArrayList<Annotation> annotations = AnnotationParser.parse(jsonString, 1.0);
        assertNotNull(annotations);
        assertEquals(438, annotations.size());
        Annotation annotation = annotations.get(1);
        assertEquals("noteheadBlack", annotation.getSymbolId());
    }

    /**
     * Extract the sample array from the JSON string.
     */
    @Test
    public void extractArray() {
        JSONArray array = AnnotationParser.extractArray(jsonString);
        assertNotNull(array);
        assertTrue(array.length() > 10);
    }

    /**
     * Handle faulty input appropriately.
     */
    @Test
    public void doNotExtractFaultyArray() {
        String json = "[1001, 159, 1005, 163, \"articStaccatoBelow\"]";
        assertNull(AnnotationParser.extractArray(json));
        json = "[[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]";
        assertNull(AnnotationParser.extractArray(json));
        json = "{[[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]}";
        assertNull(AnnotationParser.extractArray(json));
        json = "{\"Niet!\": [[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]}";
        assertNull(AnnotationParser.extractArray(json));
    }

    /**
     * Parse a sample from the array.
     */
    @Test
    public void parseSample() {
        String json = "[1001, 159, 1005, 163, \"articStaccatoBelow\"]";
        JSONArray array = new JSONArray(json);
        Annotation annotation = AnnotationParser.parseSymbol(array, 1.0);
        assertNotNull(annotation);
        Rectangle bounds = annotation.getBounds();
        assertEquals(1001, bounds.x);
        assertEquals(159, bounds.y);
        assertEquals(5, bounds.width);
        assertEquals(5, bounds.height);
        assertEquals("articStaccatoBelow", annotation.getSymbolId());
    }
}