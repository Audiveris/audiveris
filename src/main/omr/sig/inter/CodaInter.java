//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C o d a I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.math.GeoUtil;

import omr.sig.relation.CodaBarRelation;

import java.awt.Point;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code CodaInter} represents a coda interpretation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "coda")
public class CodaInter
        extends AbstractDirectionInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code CodaInter} object.
     *
     * @param glyph the coda glyph
     * @param grade the interpretation quality
     */
    public CodaInter (Glyph glyph,
                      double grade)
    {
        super(glyph, glyph.getBounds(), Shape.CODA, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    public CodaInter ()
    {
        super(null, null, null, 0);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // delete //
    //--------//
    @Override
    public void delete ()
    {
        if (staff != null) {
            staff.removeOtherInter(this);
        }

        super.delete();
    }

    //-----------------//
    // linkWithBarline //
    //-----------------//
    /**
     * (Try to) connect this coda with a suitable barline.
     *
     * @return true if successful
     */
    public boolean linkWithBarline ()
    {
        Point center = getCenter();
        List<BarlineInter> bars = getStaff().getBars();
        BarlineInter bar = BarlineInter.getClosestBarline(bars, center);

        if ((bar != null) && (GeoUtil.xOverlap(getBounds(), bar.getBounds()) > 0)) {
            sig.addEdge(this, bar, new CodaBarRelation());

            return true;
        }

        return false;
    }
}
