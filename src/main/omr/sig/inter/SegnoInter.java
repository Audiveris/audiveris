//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S e g n o I n t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.math.GeoUtil;

import omr.sig.relation.SegnoBarRelation;

import java.awt.Point;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SegnoInter} represents a segno (sign) interpretation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "segno")
public class SegnoInter
        extends AbstractDirectionInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code SegnoInter} object.
     *
     * @param glyph the segno glyph
     * @param grade the interpretation quality
     */
    public SegnoInter (Glyph glyph,
                       double grade)
    {
        super(glyph, glyph.getBounds(), Shape.SEGNO, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    public SegnoInter ()
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
     * (Try to) connect this segno with a suitable barline.
     *
     * @return true if successful
     */
    public boolean linkWithBarline ()
    {
        Point center = getCenter();
        List<BarlineInter> bars = getStaff().getBars();
        BarlineInter bar = BarlineInter.getClosestBarline(bars, center);

        if ((bar != null) && (GeoUtil.xOverlap(getBounds(), bar.getBounds()) > 0)) {
            sig.addEdge(this, bar, new SegnoBarRelation());

            return true;
        }

        return false;
    }
}
