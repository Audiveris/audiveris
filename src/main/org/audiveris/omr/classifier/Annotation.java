//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       A n n o t a t i o n                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright ©  Audiveris 2017. All rights reserved.
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

import org.audiveris.omr.util.AbstractEntity;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omrdataset.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code Annotation}
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "annotation")
public class Annotation
        extends AbstractEntity
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            Annotation.class);

    //~ Instance fields ----------------------------------------------------------------------------
    @XmlElement(name = "bounds")
    @XmlJavaTypeAdapter(Jaxb.RectangleAdapter.class)
    private final Rectangle bounds;

    @XmlAttribute(name = "omr-shape")
    private final OmrShape omrShape;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Annotation} object.
     *
     * @param bounds   bounding box of the symbol
     * @param omrShape name of the symbol shape
     */
    public Annotation (Rectangle bounds,
                       OmrShape omrShape)
    {
        this.bounds = bounds;
        this.omrShape = omrShape;
    }

    /**
     * Creates a new {@code Annotation} object.
     *
     * @param x1       x min
     * @param y1       y min
     * @param x2       x max
     * @param y2       y max
     * @param omrShape symbol name
     */
    public Annotation (int x1,
                       int y1,
                       int x2,
                       int y2,
                       OmrShape omrShape)
    {
        this(new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1), omrShape);
    }

    // Meant for JAXB
    private Annotation ()
    {
        this.bounds = null;
        this.omrShape = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public boolean contains (Point point)
    {
        return bounds.contains(point); // TODO: to be improved???
    }

    @Override
    public Rectangle getBounds ()
    {
        return new Rectangle(bounds);
    }

    public OmrShape getOmrShape ()
    {
        return omrShape;
    }

    //-----------------//
    // readAnnotations //
    //-----------------//
    /**
     * Read annotations out of a (json-formatted) file.
     *
     * @param jsonPath path to json file
     * @param ratio
     * @return the annotations read
     * @throws Exception
     */
    public static List<Annotation> readAnnotations (String jsonPath,
                                                    double ratio)
            throws Exception
    {
        logger.info("Reading annotations from file {}", jsonPath);

        InputStream in = new FileInputStream(new File(jsonPath));
        List<Annotation> annotations = readAnnotations(in, ratio);
        in.close();

        return annotations;
    }

    //-----------------//
    // readAnnotations //
    //-----------------//
    /**
     * Read annotations out of a (json-formatted) input stream.
     *
     * @param in    the input stream (to be closed by caller)
     * @param ratio
     * @return the annotations read
     * @throws Exception
     */
    public static List<Annotation> readAnnotations (InputStream in,
                                                    double ratio)
            throws Exception
    {
        logger.debug("Reading annotations from stream");

        List<Annotation> annotations = new ArrayList<Annotation>();
        JsonReader reader = Json.createReader(in);
        JsonObject root = reader.readObject();
        logger.info("root: {}", root);

        JsonArray results = root.getJsonArray("bounding_boxes");

        for (Iterator<JsonValue> it = results.iterator(); it.hasNext();) {
            JsonValue value = it.next();
            JsonArray t = value.asJsonArray();
            String omrShapeString = t.getString(4);

            try {
                OmrShape omrShape = OmrShape.valueOf(omrShapeString);
                Annotation ann = new Annotation(
                        (int) Math.rint(t.getInt(0) / ratio),
                        (int) Math.rint(t.getInt(1) / ratio),
                        (int) Math.rint(t.getInt(2) / ratio),
                        (int) Math.rint(t.getInt(3) / ratio),
                        omrShape);
                logger.debug("{}", ann);
                annotations.add(ann);
            } catch (IllegalArgumentException ex) {
                logger.error("Unknown OmrShape {}", omrShapeString);
            }
        }

        logger.info("Annotations: {}", annotations.size());

        return annotations;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("annotation{");
        sb.append(bounds).append(" ").append(omrShape);
        sb.append("}");

        return sb.toString();
    }
}