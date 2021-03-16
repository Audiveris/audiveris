//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   H e a d S e e d S c a l e                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sheet.note;

import java.util.ArrayList;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map.Entry;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code HeadSeedScale} handles the head-seed typical abscissa distance, per shape
 * and per head horizontal side, in a sheet.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "head-seeds")
public class HeadSeedScale
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HeadSeedScale.class);

    //~ Instance fields ----------------------------------------------------------------------------
    final EnumMap<Shape, EnumMap<HorizontalSide, Double>> global = new EnumMap<>(Shape.class);

    //~ Constructors -------------------------------------------------------------------------------
    public HeadSeedScale ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // getDx //
    //-------//
    /**
     * Report the typical dx between heads bounds and stems seeds for the provided head
     * shape and head horizontal side.
     * <p>
     * NOTA: dx values are positive outside head bounds and negative inside.
     *
     * @param shape provided head shape
     * @param hSide horizontal side WRT head
     * @return the typical dx value (in pixels), or null if information is not available
     */
    public Double getDx (Shape shape,
                         HorizontalSide hSide)
    {
        final EnumMap<HorizontalSide, Double> sideMap = global.get(shape);

        if (sideMap != null) {
            return sideMap.get(hSide);
        }

        return null;
    }

    //-------//
    // putDx //
    //-------//
    /**
     * Assign the typical dx for the provided head shape and horizontal head shape.
     *
     * @param shape provided head shape
     * @param hSide provided head side
     * @param dx    assigned abscissa distance in pixels (negative if inside head bounds)
     */
    public void putDx (Shape shape,
                       HorizontalSide hSide,
                       double dx)
    {
        EnumMap<HorizontalSide, Double> sideMap = global.get(shape);

        if (sideMap == null) {
            global.put(shape, sideMap = new EnumMap<>(HorizontalSide.class));
        }

        sideMap.put(hSide, dx);
    }

    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder("HeadSeeds{");
        boolean outerStarted = false;

        for (Entry<Shape, EnumMap<HorizontalSide, Double>> entry : global.entrySet()) {
            if (outerStarted) {
                sb.append(' ');
            }

            sb.append(entry.getKey()).append('[');
            boolean innerStarted = false;

            for (Entry<HorizontalSide, Double> e : entry.getValue().entrySet()) {
                if (innerStarted) {
                    sb.append(',');
                }

                sb.append(e.getKey().name().charAt(0)).append(':')
                        .append(String.format("%.1f", e.getValue()));
                innerStarted = true;
            }

            sb.append(']');
            outerStarted = true;
        }

        return sb.append('}').toString();
    }

    //------------//
    // getContent //
    //------------//
    /**
     * Extract all values from this HeadSeedScale.
     *
     * @return opaque content
     */
    public Content getContent ()
    {
        final Content content = new Content();

        for (Entry<Shape, EnumMap<HorizontalSide, Double>> entry : global.entrySet()) {
            final Shape shape = entry.getKey();

            for (Entry<HorizontalSide, Double> e : entry.getValue().entrySet()) {
                final HorizontalSide hSide = e.getKey();
                final Double dx = e.getValue();
                content.list.add(new Content.Value(shape, hSide, dx));
            }
        }

        return content;
    }

    //------------//
    // setContent //
    //------------//
    /**
     * Populate this HeadSeedScale with values.
     *
     * @param content provided content
     */
    public void setContent (Content content)
    {
        for (Content.Value value : content.list) {
            putDx(value.shape, value.side, value.dx);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Content //
    //---------//
    /**
     * Opaque type meant to support (un-)marshalling of HeadSeedScale.
     */
    public static class Content
    {

        @XmlElement(name = "head-seed")
        private final ArrayList<Value> list = new ArrayList<>();

        private Content ()
        {
        }

        private static class Value
        {

            @XmlAttribute
            public final Shape shape;

            @XmlAttribute
            public final HorizontalSide side;

            @XmlAttribute
            @XmlJavaTypeAdapter(Jaxb.Double1Adapter.class)
            public final Double dx;

            public Value (Shape shape,
                          HorizontalSide side,
                          Double dx)
            {
                this.shape = shape;
                this.side = side;
                this.dx = dx;
            }

            // No-arg constructor needed by JAXB
            @SuppressWarnings("unused")
            private Value ()
            {
                this(null, null, null);
            }
        }
    }
}
