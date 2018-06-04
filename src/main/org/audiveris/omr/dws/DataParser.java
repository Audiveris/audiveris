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
            Symbol symbol = parseSymbol(array.getJSONArray(index));
            if (symbol == null) {
                continue;
            }
            // TODO: Process symbols.
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
     * Parses a jsonSymbol from the array and returns a proper object representation.
     *
     * @param jsonSymbol The JSON represented jsonSymbol.
     * @return The parsed jsonSymbol.
     */
    static Symbol parseSymbol(JSONArray jsonSymbol) {
        String symbolId = jsonSymbol.getString(4);
        Symbol symbol = SymbolFactory.produce(symbolId);
        if (symbol == null) {
            return null;
        }
        symbol.assignCoordinates(
                jsonSymbol.getInt(0),
                jsonSymbol.getInt(1),
                jsonSymbol.getInt(2),
                jsonSymbol.getInt(3)
        );
        return symbol;
    }
}
