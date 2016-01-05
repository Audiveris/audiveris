//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B e a m I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;

import omr.sig.GradeImpacts;

import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BeamInter} represents a (full) beam interpretation, as opposed
 * to a beam hook interpretation.
 *
 * @see BeamHookInter
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "beam")
public class BeamInter
        extends AbstractBeamInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BeamInter object.
     *
     * @param impacts the grade details
     * @param median  median beam line
     * @param height  beam height
     */
    public BeamInter (GradeImpacts impacts,
                      Line2D median,
                      double height)
    {
        super(Shape.BEAM, impacts, median, height);
    }

    private BeamInter ()
    {
        super(null, null, null, 0);
    }
}
