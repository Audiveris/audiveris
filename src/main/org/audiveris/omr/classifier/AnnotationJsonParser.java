//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A n n o t a t i o n J s o n P a r s e r                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.classifier;

import org.audiveris.omrdataset.api.OmrShape;

import org.json.JSONArray;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Class {@code AnnotationJsonParser} parses a JSON-formatted classifier output
 *
 * @author Raphael Emberger
 */
public class AnnotationJsonParser
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AnnotationJsonParser.class);

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // parse //
    //-------//
    /**
     * Parses the output from the neural network.
     *
     * @param json  The JSON formatted output of the neural network.
     * @param ratio the ratio initially applied to the input image
     * @return A yet to be specified format for AudiVeris to digest.
     */
    public static ArrayList<Annotation> parse (String json,
                                               double ratio)
    {
        final ArrayList<Annotation> annotations = new ArrayList<Annotation>();
        logger.debug("json: {}", json);

        JSONArray array = extractArray(json);

        if (array == null) {
            return null;
        }

        for (int index = 0; index < array.length(); index++) {
            Annotation annotation = parseSymbol(array.getJSONArray(index), ratio);

            if (annotation != null) {
                logger.debug("{}", annotation);
                annotations.add(annotation);
            }
        }

        logger.info("Annotations: {}", annotations.size());

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
    static JSONArray extractArray (String json)
    {
        try {
            JSONObject obj = new JSONObject(json);

            return obj.getJSONArray("bounding_boxes");
        } catch (Exception e) {
            logger.warn("Couldn't extract the sample array out of string " + json, e);

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
     * @param ratio      the ratio initially applied to the input image
     * @return the Annotation parsed out of jsonSymbol
     */
    static Annotation parseSymbol (JSONArray jsonSymbol,
                                   double ratio)
    {
        // Symbol ID, make sure it is recognized as an OmrShape value
        final String symbolId = jsonSymbol.getString(4);
        final OmrShape omrShape;

        try {
            omrShape = OmrShape.valueOf(symbolId);
        } catch (IllegalArgumentException ex) {
            logger.warn("Unknown OmrShape {}", symbolId);

            return null;
        }

        Annotation annotation = new Annotation(
                (int) Math.rint(jsonSymbol.getInt(0) / ratio),
                (int) Math.rint(jsonSymbol.getInt(1) / ratio),
                (int) Math.rint(jsonSymbol.getInt(2) / ratio),
                (int) Math.rint(jsonSymbol.getInt(3) / ratio),
                omrShape,
                jsonSymbol.getDouble(5));
        logger.debug("{}", annotation);

        return annotation;
    }
}
