//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h S i g n a t u r e                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.util.Implement;

import java.awt.Rectangle;

/**
 * Class <code>GlyphSignature</code> is used to implement a map of glyphs,
 * based only on their physical properties.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class GlyphSignature
    implements Comparable<GlyphSignature>
{
    //~ Instance fields --------------------------------------------------------

    /** Glyph weight */
    private final int weight;

    /** Glyph contour box */
    private final Rectangle contourBox;

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
        contourBox = glyph.getContourBox();
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // compareTo //
    //-----------//
    @Implement(Comparable.class)
    public int compareTo (GlyphSignature other)
    {
        if (weight < other.weight) {
            return -1;
        } else if (weight > other.weight) {
            return 1;
        }

        if (contourBox.x < other.contourBox.x) {
            return -1;
        } else if (contourBox.x > other.contourBox.x) {
            return 1;
        }

        if (contourBox.y < other.contourBox.y) {
            return -1;
        } else if (contourBox.y > other.contourBox.y) {
            return 1;
        }

        if (contourBox.width < other.contourBox.width) {
            return -1;
        } else if (contourBox.width > other.contourBox.width) {
            return 1;
        }

        if (contourBox.height < other.contourBox.height) {
            return -1;
        } else if (contourBox.height > other.contourBox.height) {
            return 1;
        }

        return 0; // Equal
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Sig ")
          .append("w")
          .append(weight)
          .append(" ")
          .append(contourBox)
          .append("}");

        return sb.toString();
    }
}
