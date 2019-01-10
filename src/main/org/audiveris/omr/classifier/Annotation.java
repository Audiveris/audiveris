//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       A n n o t a t i o n                                      //
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
package org.audiveris.omr.classifier;

import org.audiveris.omr.util.AbstractEntity;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omrdataset.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import javax.xml.bind.Unmarshaller;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code Annotation} represents one unit of the output from the detection web
 * service.
 *
 * @author Raphael Emberger
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "annotation")
public class Annotation
        extends AbstractEntity
{

    private static final Logger logger = LoggerFactory.getLogger(
            Annotation.class);

    /** OmrShape as guessed by the classifier. */
    @XmlAttribute(name = "omr-shape")
    private final OmrShape omrShape;

    /** Confidence in guess. */
    @XmlAttribute(name = "confidence")
    private double confidence;

    /** Bounding box of the guessed symbol. */
    @XmlElement(name = "bounds")
    @XmlJavaTypeAdapter(Jaxb.RectangleAdapter.class)
    private final Rectangle bounds;

    /**
     * Creates a new {@code Annotation} object.
     *
     * @param bounds     bounding box of the symbol
     * @param omrShape   name of the symbol shape
     * @param confidence probability of being a true positive
     */
    public Annotation (Rectangle bounds,
                       OmrShape omrShape,
                       double confidence)
    {
        this.bounds = bounds;
        this.omrShape = omrShape;
        this.confidence = confidence;
    }

    /**
     * Creates a new {@code Annotation} object.
     *
     * @param x1         x min
     * @param y1         y min
     * @param x2         x max
     * @param y2         y max
     * @param omrShape   symbol name
     * @param confidence probability of being a true positive
     */
    public Annotation (int x1,
                       int y1,
                       int x2,
                       int y2,
                       OmrShape omrShape,
                       double confidence)
    {
        this(new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1), omrShape, confidence);
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private Annotation ()
    {
        this.bounds = null;
        this.omrShape = null;
        this.confidence = 0;
    }

    //----------//
    // contains //
    //----------//
    /**
     * {@inheritDoc}
     * <p>
     * Beware, this simplistic implementation is based only on the bounding rectangle.
     *
     * @param point the provided point
     * @return true if the annotation contains the point.
     */
    @Override
    public boolean contains (Point point)
    {
        return bounds.contains(point); // TODO: to be improved???
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        return new Rectangle(bounds);
    }

    //---------------//
    // getConfidence //
    //---------------//
    /**
     * Report (a kind of) probability for this symbol to be a true positive.
     *
     * @return a double value within [0..1]
     */
    public double getConfidence ()
    {
        return confidence;
    }

    //-------------//
    // getOmrShape //
    //-------------//
    /**
     * Report the Omr shape for this symbol.
     *
     * @return symbol OmrShape
     */
    public OmrShape getOmrShape ()
    {
        return omrShape;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" ").append(omrShape);
        sb.append(" ").append(String.format("%.2f", confidence));
        sb.append(" ").append(bounds);

        return sb.toString();
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Dirty hack for old annotations without confidence information.
     * TODO: TO BE REMOVED ASAP!
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller m,
                                 Object parent)
    {
        if (confidence <= 0) {
            confidence = 1.0;
        }
    }
}
