//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S a m p l e                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import org.audiveris.omr.glyph.BasicGlyph;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code Sample} represents a sample of a shape with the related glyph.
 * Such Sample instances are used to train classifiers.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "sample")
public class Sample
        extends BasicGlyph
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            Sample.class);

    /** For comparing Sample instances by shape. */
    public static final Comparator<Sample> byShape = new Comparator<Sample>()
    {
        @Override
        public int compare (Sample s1,
                            Sample s2)
        {
            return Integer.compare(s1.getShape().ordinal(), s2.getShape().ordinal());
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Assigned shape. */
    @XmlAttribute(name = "shape")
    protected final Shape shape;

    /** Scaling information. */
    @XmlAttribute(name = "interline")
    protected final int interline;

    /** Staff-based pitch. */
    @XmlAttribute(name = "pitch")
    @XmlJavaTypeAdapter(Jaxb.Double1Adapter.class)
    protected Double pitch;

    /** True for artificial (font-based) sample. */
    private boolean symbol;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ShapeSample} object.
     *
     * @param left      sheet-based abscissa of top-left corner
     * @param top       sheet-based ordinate of top-left corner
     * @param runTable  runs of pixels
     * @param interline scaling information
     * @param id        sample id
     * @param shape     assigned shape
     * @param pitch     pitch WRT related staff
     */
    public Sample (int left,
                   int top,
                   RunTable runTable,
                   int interline,
                   int id,
                   Shape shape,
                   Double pitch)
    {
        super(left, top, runTable);
        this.id = id;
        this.shape = shape;
        this.interline = interline;
        this.pitch = pitch;
    }

    /**
     * Creates a new {@code Sample} object from a glyph.
     *
     * @param glyph     the originating glyph
     * @param interline sheet interline
     * @param shape     assigned shape
     * @param pitch     pitch WRT related staff
     */
    public Sample (Glyph glyph,
                   int interline,
                   Shape shape,
                   Double pitch)
    {
        this(
                glyph.getLeft(),
                glyph.getTop(),
                glyph.getRunTable(),
                interline,
                glyph.getId(),
                shape,
                pitch);
    }

    /**
     * No-arg constructor needed for JAXB unmarshalling.
     */
    private Sample ()
    {
        this(0, 0, null, 0, 0, null, null);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------------//
    // getRecordableShape //
    //--------------------//
    /**
     * Report the shape to record for the provided shape.
     * <p>
     * For example shapes WHOLE_REST and HALF_REST differ only by their pitch position, so they both
     * end up to HW_REST_set physical shape.
     *
     * @param shape the provided shape
     * @return the recordable shape to use, or null
     */
    public static Shape getRecordableShape (Shape shape)
    {
        if (shape == null) {
            return null;
        }

        Shape physicalShape = shape.getPhysicalShape();

        if (physicalShape.isTrainable() && (physicalShape != Shape.NOISE)) {
            return physicalShape;
        } else {
            return null;
        }
    }

    /**
     * We need equality strictly based on reference.
     *
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise.
     */
    @Override
    public boolean equals (Object obj)
    {
        return this == obj;
    }

    public int getInterline ()
    {
        return interline;
    }

    /**
     * Report the sample pitch with respect to related staff.
     *
     * @return pitch value (0 for mid line, -4 for top line, +4 for bottom line)
     */
    public Double getPitch ()
    {
        return pitch;
    }

    public Shape getShape ()
    {
        return shape;
    }

    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (97 * hash) + Objects.hashCode(this.shape);
        hash = (97 * hash) + this.interline;
        hash = (97 * hash) + super.hashCode();

        return hash;
    }

    /**
     * @return the symbol
     */
    public boolean isSymbol ()
    {
        return symbol;
    }

    /**
     * @param symbol the symbol to set
     */
    public void setSymbol (boolean symbol)
    {
        this.symbol = symbol;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if ((groups != null) && !groups.isEmpty()) {
            sb.append(' ').append(groups);
        }

        if (getShape() != null) {
            sb.append(" ").append(getShape());

            if (getShape().getPhysicalShape() != getShape()) {
                sb.append(" physical=").append(getShape().getPhysicalShape());
            }
        }

        return sb.toString();
    }
}
