//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S y m b o l I n f o                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omrdataset.api;

import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code SymbolInfo} handles info about one OMR symbol (name, bounding box).
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class SymbolInfo
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolInfo.class);

    //~ Instance fields ----------------------------------------------------------------------------
    @XmlAttribute(name = "interline")
    @XmlJavaTypeAdapter(value = Jaxb.Double3Adapter.class, type = double.class)
    private final double interline;

    @XmlAttribute(name = "id")
    private Integer id;

    @XmlAttribute(name = "scale")
    @XmlJavaTypeAdapter(Jaxb.Double3Adapter.class)
    private final Double scale;

    @XmlAttribute(name = "shape")
    @XmlJavaTypeAdapter(OmrShapeAdapter.class)
    private OmrShape omrShape;

    @XmlElement(name = "Bounds")
    @XmlJavaTypeAdapter(Jaxb.Rectangle2DAdapter.class)
    private final Rectangle2D bounds;

    /** Inner symbols, if any. */
    @XmlElement(name = "Symbol")
    private List<SymbolInfo> innerSymbols;

    /** Signals an invalid symbol. */
    private boolean invalid;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SymbolInfo} object.
     *
     * @param omrShape  symbol OMR shape
     * @param interline related interline
     * @param id        symbol id, if any
     * @param scale     ratio WRT standard symbol size, optional
     * @param bounds    symbol bounding box within containing image
     */
    public SymbolInfo (OmrShape omrShape,
                       int interline,
                       Integer id,
                       Double scale,
                       Rectangle2D bounds)
    {
        this.omrShape = omrShape;
        this.interline = interline;
        this.id = id;
        this.scale = scale;
        this.bounds = bounds;
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private SymbolInfo ()
    {
        omrShape = null;
        interline = 0;
        id = null;
        scale = null;
        bounds = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Add an inner symbol within this one.
     *
     * @param symbol the inner symbol to add
     */
    public void addInnerSymbol (SymbolInfo symbol)
    {
        if (innerSymbols == null) {
            innerSymbols = new ArrayList<>();
        }

        innerSymbols.add(symbol);
    }

    /**
     * @return a COPY of the bounds
     */
    public Rectangle2D getBounds ()
    {
        Rectangle2D copy = new Rectangle2D.Double();
        copy.setRect(bounds);

        return copy;
    }

    /**
     * Report symbol id (a positive integer)
     *
     * @return symbol id or 0
     */
    public int getId ()
    {
        if (id == null) {
            return 0;
        }

        return id;
    }

    /**
     * Assign ID value.
     *
     * @param id new ID value
     */
    public void setId (int id)
    {
        this.id = id;
    }

    /**
     * Report the inner symbols, if any
     *
     * @return un-mutable list of inner symbols, perhaps empty but never null
     */
    public List<SymbolInfo> getInnerSymbols ()
    {
        if (innerSymbols == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(innerSymbols);
    }

    /**
     * @return the interline
     */
    public double getInterline ()
    {
        return interline;
    }

    /**
     * Report whether symbol is invalid.
     *
     * @return the invalid
     */
    public boolean isInvalid ()
    {
        return invalid;
    }

    /**
     * Set symbol as invalid.
     */
    public void setInvalid ()
    {
        this.invalid = true;
    }

    /**
     * @return the omrShape, perhaps null
     */
    public OmrShape getOmrShape ()
    {
        return omrShape;
    }

    /**
     * @return the scale, perhaps null
     */
    public Double getScale ()
    {
        return scale;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Symbol{");
        sb.append("shape:").append(omrShape);

        if (invalid) {
            sb.append(" INVALID");
        }

        if ((innerSymbols != null) && !innerSymbols.isEmpty()) {
            sb.append(" OUTER");
        }

        sb.append(" interline:").append(interline);

        if (id != null) {
            sb.append(" id:").append(id);
        }

        if (scale != null) {
            sb.append(" scale:").append(scale);
        }

        sb.append(" ").append(bounds);

        sb.append("}");

        return sb.toString();
    }

    /**
     * If there is a special name for a smaller version of this symbol, use it.
     */
    public void useSmallName ()
    {
        OmrShape smallShape = getSmallShape(omrShape);

        if (smallShape != null) {
            setOmrShape(smallShape);
        }
    }

    /**
     * Called after all the properties (except IDREF) are unmarshalled
     * for this object, but before this object is set to the parent object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        if (omrShape == null) {
            logger.warn("*** Null shape {}", this);
        }
    }

    /**
     * Report the name, if any, for a small version of the provided shape
     *
     * @param omrShape the provided shape
     * @return the shape for small version, or null
     */
    private OmrShape getSmallShape (OmrShape omrShape)
    {
        if (omrShape == null) {
            return null;
        }

        switch (omrShape) {
        // Clefs (change)
        case cClefAlto:
            return OmrShape.cClefAltoChange;

        case cClefTenor:
            return OmrShape.cClefTenorChange;

        case fClef:
            return OmrShape.fClefChange;

        case gClef:
            return OmrShape.gClefChange;

        // Accidentals
        case accidentalFlat:
            return OmrShape.accidentalFlatSmall;

        case accidentalNatural:
            return OmrShape.accidentalNaturalSmall;

        case accidentalSharp:
            return OmrShape.accidentalSharpSmall;

        // Flags
        case flag8thUp:
            return OmrShape.flag8thUpSmall;

        case flag8thDown:
            return OmrShape.flag8thDownSmall;

        // Note heads
        case noteheadBlack:
            return OmrShape.noteheadBlackSmall;

        case noteheadHalf:
            return OmrShape.noteheadHalfSmall;

        case noteheadWhole:
            return OmrShape.noteheadWholeSmall;

        case noteheadDoubleWhole:
            return OmrShape.noteheadDoubleWholeSmall;

        default:
            return null;
        }
    }

    /**
     * @param omrShape the omrShape to set
     */
    private void setOmrShape (OmrShape omrShape)
    {
        logger.debug("Renamed scaled {} as {}", this, omrShape);
        this.omrShape = omrShape;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------------//
    // OmrShapeAdapter //
    //-----------------//
    /**
     * We need a specific adapter to warn about unknown shape names.
     */
    public static class OmrShapeAdapter
            extends XmlAdapter<String, OmrShape>
    {

        @Override
        public String marshal (OmrShape shape)
                throws Exception
        {
            if (shape == null) {
                return null;
            }

            return shape.toString();
        }

        @Override
        public OmrShape unmarshal (String string)
                throws Exception
        {
            try {
                return OmrShape.valueOf(string);
            } catch (IllegalArgumentException ex) {
                logger.warn("*** Unknown shape name: {}", string);

                return null;
            }
        }
    }
}
