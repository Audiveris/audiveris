package org.audiveris.omr.dws;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code DataParser} parses the output from the neural network.
 *
 * @author Raphael Emberger
 */
public class DataParser {

    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DataParser.class);

    /**
     * Parses the output from the neural network.
     *
     * @param json The JSON formatted output of the neural network.
     * @return A yet to be specified format for AudiVeris to digest.
     */
    public static Object parse(String json) {
        JSONArray array = extractArray(json);
        if (array == null) return null;
        for (int index = 0; index < array.length(); index++) {
            Object sample = parseSample(array.getJSONArray(index));
            // TODO: Process samples.
        }
        return null;
    }

    /**
     * Extracts the array of the data to parse from the JSON String.
     *
     * @param json The JSON formatted string to extract the data from.
     * @return A JSONArray.
     */
    static JSONArray extractArray(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return obj.getJSONArray("bounding_boxes");
        } catch (Exception e) {
            logger.info("Couldn't extract the sample array out of string", e);
            return null;
        }
    }

    /**
     * Parses a sample from the array and returns a proper object representation.
     *
     * @param sample The JSON represented sample.
     * @return The parsed sample.
     */
    static Object parseSample(JSONArray sample) {
        // TODO: Parse samples.
        return null;
    }
}
