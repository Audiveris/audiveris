//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B e a m H o o k I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
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
 * Class {@code BeamHookInter} represents a beam hook interpretation.
 *
 * @see BeamInter
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "beam-hook")
public class BeamHookInter
        extends AbstractBeamInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new HookInter object.
     *
     * @param impacts the grade details
     * @param median  median beam line
     * @param height  beam height
     */
    public BeamHookInter (GradeImpacts impacts,
                          Line2D median,
                          double height)
    {
        super(Shape.BEAM_HOOK, impacts, median, height);
    }

    private BeamHookInter ()
    {
        super(null, null, null, 0);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // isHook //
    //--------//
    public boolean isHook ()
    {
        return true;
    }
}
