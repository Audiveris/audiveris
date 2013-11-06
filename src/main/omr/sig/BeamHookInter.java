//----------------------------------------------------------------------------//
//                                                                            //
//                          B e a m H o o k I n t e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import java.awt.geom.Line2D;

/**
 * Class {@code BeamHookInter} represents a beam hook
 * interpretation.
 *
 * @see FullBeamInter
 *
 * @author Hervé Bitteur
 */
public class BeamHookInter
        extends AbstractBeamInter
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new HookInter object.
     *
     * @param glyph   the underlying glyph
     * @param impacts the grade details
     * @param median  median beam line
     * @param height  beam height
     */
    public BeamHookInter (Glyph glyph,
                          GradeImpacts impacts,
                          Line2D median,
                          double height)
    {
        super(glyph, Shape.BEAM_HOOK, impacts, median, height);
    }
}
