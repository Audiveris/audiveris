//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  G l y p h S i g n a t u r e                                   //
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
package org.audiveris.omr.glyph;

import org.audiveris.omr.moments.GeometricMoments;

/**
 * Class {@code GlyphSignature} is used to implement a map of glyphs,
 * based only on their physical properties.
 * <p>
 * The signature is implemented using the glyph moments.</p>
 *
 * @author Hervé Bitteur
 */
public class GlyphSignature
        implements Comparable<GlyphSignature>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Glyph absolute weight */
    private final int weight;

    /** Glyph normalized moments */
    private final GeometricMoments moments;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new GlyphSignature object.
     *
     * @param glyph     the glyph to compute signature upon
     * @param interline global sheet interline
     */
    public GlyphSignature (Glyph glyph,
                           int interline)
    {
        weight = glyph.getWeight();
        moments = new GeometricMoments(glyph.getGeometricMoments(interline));
    }

    //----------------//
    // GlyphSignature //
    //----------------//
    /**
     * Needed by JAXB.
     */
    private GlyphSignature ()
    {
        weight = 0;
        moments = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // compareTo //
    //-----------//
    @Override
    public int compareTo (GlyphSignature other)
    {
        if (weight < other.weight) {
            return -1;
        } else if (weight > other.weight) {
            return 1;
        }

        final double[] values = moments.getValues();
        final double[] otherValues = other.moments.getValues();

        for (int i = 0; i < values.length; i++) {
            int cmp = Double.compare(values[i], otherValues[i]);

            if (cmp != 0) {
                return cmp;
            }
        }

        return 0; // Equal
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof GlyphSignature) {
            return compareTo((GlyphSignature) obj) == 0;
        } else {
            return false;
        }
    }

    //-----------//
    // getWeight //
    //-----------//
    public int getWeight ()
    {
        return weight;
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (41 * hash) + this.weight;

        return hash;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{GSig");

        sb.append(" weight=").append(weight);

        sb.append(" moments=").append(moments);
        sb.append("}");

        return sb.toString();
    }
}
