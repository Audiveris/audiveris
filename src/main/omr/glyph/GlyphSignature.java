//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h S i g n a t u r e                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;

import omr.moments.GeometricMoments;

/**
 * Class {@code GlyphSignature} is used to implement a map of glyphs,
 * based only on their physical properties.
 *
 * <p>The signature is implemented using the glyph moments.</p>
 *
 * @author Hervé Bitteur
 */
public class GlyphSignature
        implements Comparable<GlyphSignature>
{
    //~ Instance fields --------------------------------------------------------

    /** Glyph absolute weight */
    private final int weight;

    /** Glyph normalized moments */
    private GeometricMoments moments;

    //~ Constructors -----------------------------------------------------------
    //----------------//
    // GlyphSignature //
    //----------------//
    /**
     * Creates a new GlyphSignature object.
     *
     * @param glyph the glyph to compute signature upon
     */
    public GlyphSignature (Glyph glyph)
    {
        weight = glyph.getWeight();
        moments = new GeometricMoments(glyph.getGeometricMoments());
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

    //~ Methods ----------------------------------------------------------------
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

        final Double[] values = moments.getValues();
        final Double[] otherValues = other.moments.getValues();

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

        sb.append(" weight=")
                .append(weight);

        sb.append(" moments=")
                .append(moments);
        sb.append("}");

        return sb.toString();
    }
}
