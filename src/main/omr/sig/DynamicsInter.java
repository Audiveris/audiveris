//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    D y n a m i c s I n t e r                                   //
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
 * Class {@code DynamicsInter} represents a dynamics indication (such as mf).
 *
 * @author Hervé Bitteur
 */
public class DynamicsInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new DynamicsInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public DynamicsInter (Glyph glyph,
                          Shape shape,
                          double grade)
    {
        super(glyph, null, shape, grade);
    }
}
