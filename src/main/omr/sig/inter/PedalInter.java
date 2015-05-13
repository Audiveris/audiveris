//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P e d a l I n t e r                                      //
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
import omr.glyph.facets.Glyph;

/**
 * Class {@code PedalInter} represents a pedal (start) or pedal up (stop) event
 *
 * @author Hervé Bitteur
 */
public class PedalInter
        extends AbstractDirectionInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code PedalInter} object.
     *
     * @param glyph the pedal glyph
     * @param shape PEDAL_MARK (start) or PEDAL_UP_MARK (stop)
     * @param grade the interpretation quality
     */
    public PedalInter (Glyph glyph,
                       Shape shape,
                       double grade)
    {
        super(glyph, glyph.getBounds(), shape, grade);
    }
}
