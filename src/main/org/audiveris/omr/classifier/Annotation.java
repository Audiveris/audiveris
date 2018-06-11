package org.audiveris.omr.classifier;

import org.audiveris.omr.util.AbstractEntity;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omrdataset.api.OmrShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.awt.*;

/**
 * Class {@code Sample} Represents one unit of the output from the detection web service.
 *
 * @author Raphael Emberger
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "annotation")
public final class Annotation extends AbstractEntity {

    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Annotation.class);

    //~ Instance fields ----------------------------------------------------------------------------

    private int number;
    private String symbolId;
    private String symbolDesc;

    @XmlElement(name = "bounds")
    @XmlJavaTypeAdapter(Jaxb.RectangleAdapter.class)
    private final Rectangle bounds;

    @XmlAttribute(name = "omr-shape")
    private final OmrShape omrShape;

    //~ Constructors -------------------------------------------------------------------------------

    public Annotation(int number, String symbolId, String symbolDesc) {
        this.number = number;
        this.symbolId = symbolId;
        this.symbolDesc = symbolDesc;
        this.bounds = new Rectangle();
        this.omrShape = OmrShape.valueOf(symbolId);
    }

    // Meant for JAXB
    private Annotation() {
        this.bounds = null;
        this.omrShape = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public boolean contains(Point point) {
        return bounds.contains(point); // TODO: to be improved???
    }

    public OmrShape getOmrShape() {
        return omrShape;
    }

    //-------------//
    // getNumber //
    //-------------//

    /**
     * Annotation number.
     */
    public int getNumber() {
        return number;
    }

    //-------------//
    // getSymbolId //
    //-------------//

    /**
     * Annotation ID.
     */
    public String getSymbolId() {
        return symbolId;
    }

    //---------------//
    // getSymbolDesc //
    //---------------//

    /**
     * Description of the symbol.
     */
    public String getSymbolDesc() {
        return symbolDesc;
    }

    //---------//
    // getBounds //
    //---------//

    /**
     * Rectangle of the symbol.
     */
    public Rectangle getBounds() {
        return bounds;
    }

    //-------------------//
    // assignCoordinates //
    //-------------------//

    /**
     * Assigns arguments as coordinates to the symbol.
     *
     * @param x1 First argument.
     * @param x2 Second argument.
     * @param x3 Third argument.
     * @param x4 Fourth argument.
     */
    public void assignCoordinates(int x1, int x2, int x3, int x4) {
        bounds.x = x1;
        bounds.y = x2;
        bounds.width = x3 - x1 + 1;
        bounds.height = x4 - x2 + 1;
    }

    //----------//
    // deepCopy //
    //----------//

    /**
     * makes a deep copy of the symbol.
     *
     * @return the copied symbol.
     */
    public Annotation deepCopy() {
        Annotation annotation = new Annotation(number, symbolId, symbolDesc);
        annotation.bounds.x = bounds.x;
        annotation.bounds.y = bounds.y;
        annotation.bounds.width = bounds.width;
        annotation.bounds.height = bounds.height;
        return annotation;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("annotation{");
        sb.append(bounds).append(" ").append(omrShape);
        sb.append("}");

        return sb.toString();
    }
}
