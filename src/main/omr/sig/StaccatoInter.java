//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S t a c c a t o I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

/**
 * Class {@code StaccatoInter} represents a staccato dot.
 *
 * @author Hervé Bitteur
 */
public class StaccatoInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new StaccatoInter object.
     *
     * @param glyph underlying glyph
     * @param grade evaluation value
     */
    public StaccatoInter (Glyph glyph,
                          double grade)
    {
        super(glyph, null, Shape.STACCATO, grade);
    }
}
