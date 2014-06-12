//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       L e d g e r I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
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
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * Index of virtual line relative to staff.
     * Above staff if index is negative (-1, -2, etc)
     *
     * -2 -
     * -1 -
     * ---------------------------------
     * ---------------------------------
     * ---------------------------------
     * ---------------------------------
     * ---------------------------------
     * +1 -
     * +2 -
     *
     * Below staff if index is positive (+1, +2, etc)
     */
    private Integer index;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new LedgerInter object.
     *
     * @param glyph   the underlying glyph
     * @param impacts the assignment details
     */
    public LedgerInter (Glyph glyph,
                        GradeImpacts impacts)
    {
        super(glyph, null, Shape.LEDGER, impacts);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        if (index != null) {
            sb.append(" index:").append(index);
        }

        return sb.toString();
    }

    /**
     * @return the index
     */
    public Integer getIndex ()
    {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex (int index)
    {
        this.index = index;
    }
}
