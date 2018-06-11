package org.audiveris.omr.classifier;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Class {@code AnnotationParser} parses the output from the neural network.
 *
 * @author Raphael Emberger
 */
public class AnnotationParser {

    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AnnotationParser.class);

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // parse //
    //-------//
    /**
     * Parses the output from the neural network.
     *
     * @param json The JSON formatted output of the neural network.
     * @return A yet to be specified format for AudiVeris to digest.
     */
    public static ArrayList<Annotation> parse(String json, double ratio) {
        ArrayList<Annotation> annotations = new ArrayList<>();
        logger.info("json: {}", json);
        JSONArray array = extractArray(json);
        if (array == null) return null;
        for (int index = 0; index < array.length(); index++) {
            Annotation annotation = parseSymbol(array.getJSONArray(index), ratio);
            logger.debug("{}", annotation);
            if (annotation == null) {
                continue;
            }
            annotations.add(annotation);
        }
        return annotations;
    }

    //--------------//
    // extractArray //
    //--------------//
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

    //-------------//
    // parseSymbol //
    //-------------//
    /**
     * Parses a jsonSymbol from the array and returns a proper object representation.
     *
     * @param jsonSymbol The JSON represented jsonSymbol.
     * @return The parsed jsonSymbol.
     */
    static Annotation parseSymbol(JSONArray jsonSymbol, double ratio) {
        String symbolId = jsonSymbol.getString(4);
        Annotation annotation = AnnotationFactory.produce(symbolId);
        if (annotation == null) {
            return null;
        }
        annotation.assignCoordinates(
                (int) (Math.round(jsonSymbol.getInt(0) / ratio)),
                (int) (Math.round(jsonSymbol.getInt(1) / ratio)),
                (int) (Math.round(jsonSymbol.getInt(2) / ratio)),
                (int) (Math.round(jsonSymbol.getInt(3) / ratio))
        );
        return annotation;
    }
}
