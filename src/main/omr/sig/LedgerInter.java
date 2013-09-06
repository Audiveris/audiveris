//----------------------------------------------------------------------------//
//                                                                            //
//                             L e d g e r I n t e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

/**
 * Class {@code LedgerInter} represents a Ledger interpretation.
 *
 * @author Hervé Bitteur
 */
public class LedgerInter
        extends BasicInter
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LedgerInter object.
     *
     * @param glyph the underlying glyph
     * @param grade the assignment quality
     */
    public LedgerInter (Glyph glyph,
                        double grade)
    {
        super(glyph, Shape.LEDGER, grade);
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }
}
