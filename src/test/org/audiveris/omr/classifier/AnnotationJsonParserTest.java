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
public class AnnotationJsonParserTest
{

    private static final Logger logger = LoggerFactory.getLogger(AnnotationJsonParserTest.class);

    private String jsonString;

    @Before
    public void setUp ()
    {
        String file = "data/examples/Bach_Fuge_C_DUR.json";
        try (FileInputStream inputStream = new FileInputStream(file)) {
            jsonString = IOUtils.toString(inputStream, "UTF-8");
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Test the parsing method.
     */
    @Test
    public void parse ()
    {
        ArrayList<Annotation> annotations = AnnotationJsonParser.parse(jsonString, 1.0);
        assertNotNull(annotations);
        assertEquals(438, annotations.size());
        Annotation annotation = annotations.get(1);
        assertEquals("noteheadBlack", annotation.getOmrShape().name());
    }

    /**
     * Extract the sample array from the JSON string.
     */
    @Test
    public void extractArray ()
    {
        JSONArray array = AnnotationJsonParser.extractArray(jsonString);
        assertNotNull(array);
        assertTrue(array.length() > 10);
    }

    /**
     * Handle faulty input appropriately.
     */
    @Test
    public void doNotExtractFaultyArray ()
    {
        String json = "[1001, 159, 1005, 163, \"articStaccatoBelow\"]";
        assertNull(AnnotationJsonParser.extractArray(json));
        json
                = "[[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]";
        assertNull(AnnotationJsonParser.extractArray(json));
        json
                = "{[[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]}";
        assertNull(AnnotationJsonParser.extractArray(json));
        json
                = "{\"Niet!\": [[1001, 159, 1005, 163, \"articStaccatoBelow\"],[1001, 159, 1005, 163, \"articStaccatoBelow\"]]}";
        assertNull(AnnotationJsonParser.extractArray(json));
    }

    /**
     * Parse a sample from the array.
     */
    @Test
    public void parseSample ()
    {
        ///String json = "[1001, 159, 1005, 163, \"articStaccatoBelow\", 0.97]";
        String json = "[1001, 159, 1005, 163, \"articStaccatoBelow\", 1]";
        logger.info("json: {}", json);
        JSONArray array = new JSONArray(json);
        Annotation annotation = AnnotationJsonParser.parseSymbol(array, 1.0);
        assertNotNull(annotation);
        logger.info("Parsed: {}", annotation);
        Rectangle bounds = annotation.getBounds();
        assertEquals(1001, bounds.x);
        assertEquals(159, bounds.y);
        assertEquals(5, bounds.width);
        assertEquals(5, bounds.height);
        assertEquals("articStaccatoBelow", annotation.getOmrShape().name());
        ///assertEquals(0.97, annotation.getConfidence(), 0.01);
        assertEquals(1.0, annotation.getConfidence(), 0.01);
    }
}
