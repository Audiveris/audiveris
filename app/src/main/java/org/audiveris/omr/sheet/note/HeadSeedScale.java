//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   H e a d S e e d S c a l e                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map.Entry;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>HeadSeedScale</code> handles in a sheet the typical abscissa distance
 * between head and stem seed, per head shape and per head horizontal side.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class HeadSeedScale
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HeadSeedScale.class);

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** Flat list of <code>HeadSeed</code> instances. */
    @XmlElement(name = "head-seed")
    private ArrayList<HeadSeed> list; // Used during [un]marshalling only

    // Transient data
    //---------------

    private final EnumMap<Shape, EnumMap<HorizontalSide, Double>> globalMap = new EnumMap<>(
            Shape.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>HeadSeedScale</code> object.
     */
    public HeadSeedScale ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // afterMarshal //
    //--------------//
    @SuppressWarnings("unused")
    private void afterMarshal (Marshaller m)
    {
        list = null;
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled
     * for this object, but before this object is set to the parent object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller u,
                                 Object parent)
    {
        if (list != null) {
            for (HeadSeed value : list) {
                putDx(value.shape, value.side, value.dx);
            }

            list = null;
        }
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    /**
     * Called immediately before the marshalling of this object begins.
     */
    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        list = new ArrayList<>();

        for (Entry<Shape, EnumMap<HorizontalSide, Double>> entry : globalMap.entrySet()) {
            final Shape shape = entry.getKey();

            for (Entry<HorizontalSide, Double> e : entry.getValue().entrySet()) {
                final HorizontalSide hSide = e.getKey();
                final Double dx = e.getValue();
                list.add(new HeadSeed(shape, hSide, dx));
            }
        }
    }

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
        final EnumMap<HorizontalSide, Double> sideMap = globalMap.get(shape);

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
        EnumMap<HorizontalSide, Double> sideMap = globalMap.get(shape);

        if (sideMap == null) {
            globalMap.put(shape, sideMap = new EnumMap<>(HorizontalSide.class));
        }

        sideMap.put(hSide, dx);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder("HeadSeeds{");
        boolean outerStarted = false;

        for (Entry<Shape, EnumMap<HorizontalSide, Double>> entry : globalMap.entrySet()) {
            if (outerStarted) {
                sb.append(' ');
            }

            sb.append(entry.getKey()).append('[');

            boolean innerStarted = false;

            for (Entry<HorizontalSide, Double> e : entry.getValue().entrySet()) {
                if (innerStarted) {
                    sb.append(',');
                }

                sb.append(e.getKey().name().charAt(0)).append(':').append(
                        String.format("%.1f", e.getValue()));
                innerStarted = true;
            }

            sb.append(']');
            outerStarted = true;
        }

        return sb.append('}').toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // HeadSeed //
    //----------//
    /**
     * Class <code>HeadSeed</code> describes, for the whole containing sheet,
     * the typical horizontal distance measured between a specific head shape
     * and a stem seed on a specific side of the head.
     */
    private static class HeadSeed
    {
        /** Shape of note head. */
        @XmlAttribute(name = "shape")
        public final Shape shape;

        /** Head side where the stem seed is located. */
        @XmlAttribute(name = "side")
        public final HorizontalSide side;

        /**
         * The dx attribute represents the typical horizontal distance measured
         * in pixels between seed center and head side.
         * <p>
         * Dx is positive if seed center lies outside head bounds and negative otherwise.
         * <p>
         * Dx value is defined with 1 digit maximum after the dot.
         */
        @XmlAttribute(name = "dx")
        @XmlJavaTypeAdapter(Jaxb.Double1Adapter.class)
        public final Double dx;

        // No-argument constructor needed by JAXB
        @SuppressWarnings("unused")
        private HeadSeed ()
        {
            this(null, null, null);
        }

        public HeadSeed (Shape shape,
                         HorizontalSide side,
                         Double dx)
        {
            this.shape = shape;
            this.side = side;
            this.dx = dx;
        }
    }
}
