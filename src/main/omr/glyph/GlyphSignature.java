//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h S i g n a t u r e                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.util.Implement;
import omr.util.PointFacade;
import omr.util.RectangleFacade;

import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>GlyphSignature</code> is used to implement a map of glyphs,
 * based only on their physical properties.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
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
