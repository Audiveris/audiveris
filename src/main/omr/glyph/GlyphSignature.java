//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h S i g n a t u r e                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;

import omr.util.Implement;
import omr.util.RectangleFacade;

import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code GlyphSignature} is used to implement a map of glyphs,
 * based only on their physical properties.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "glyph-signature")
public class GlyphSignature
    implements Comparable<GlyphSignature>
{
    //~ Instance fields --------------------------------------------------------

    /** Glyph weight */
    @XmlElement
    private final int weight;

    /** Glyph contour box */
    private Rectangle contourBox;

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

    //----------------//
    // GlyphSignature //
    //----------------//
    /**
     * Needed by JAXB
     */
    private GlyphSignature ()
    {
        weight = 0;
        contourBox = null;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getWeight //
    //-----------//
    public int getWeight ()
    {
        return weight;
    }

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
            GlyphSignature that = (GlyphSignature) obj;

            return (weight == that.weight) &&
                   (contourBox.x == that.contourBox.x) &&
                   (contourBox.y == that.contourBox.y) &&
                   (contourBox.width == that.contourBox.width) &&
                   (contourBox.height == that.contourBox.height);
        } else {
            return false;
        }
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

        if (contourBox != null) {
            sb.append(" Rectangle[x=")
              .append(contourBox.x)
              .append(",y=")
              .append(contourBox.y)
              .append(",width=")
              .append(contourBox.width)
              .append(",height=")
              .append(contourBox.height)
              .append("]");
        }

        sb.append("}");

        return sb.toString();
    }

    //------------------//
    // setXmlContourBox //
    //------------------//
    @XmlElement(name = "contour-box")
    private void setXmlContourBox (RectangleFacade xr)
    {
        contourBox = xr.getRectangle();
    }

    //------------------//
    // getXmlContourBox //
    //------------------//
    private RectangleFacade getXmlContourBox ()
    {
        if (contourBox != null) {
            return new RectangleFacade(contourBox);
        } else {
            return null;
        }
    }
}
