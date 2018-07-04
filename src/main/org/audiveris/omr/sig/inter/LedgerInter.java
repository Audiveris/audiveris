//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       L e d g e r I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sig.GradeImpacts;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code LedgerInter} represents a Ledger interpretation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "ledger")
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

    /**
     * Creates a new LedgerInter object.
     *
     * @param glyph the underlying glyph
     * @param grade quality
     */
    public LedgerInter (Glyph glyph,
                        double grade)
    {
        super(glyph, null, Shape.LEDGER, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private LedgerInter ()
    {
        super(null, null, null, null);
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

    //-------//
    // added //
    //-------//
    /**
     * Since a ledger instance is held by its containing staff, make sure staff
     * ledgers collection is updated.
     *
     * @see #remove(boolean)
     */
    @Override
    public void added ()
    {
        super.added();

        if (staff != null) {
            staff.addLedger(this);
        }
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

    //--------//
    // remove //
    //--------//
    /**
     * Since a ledger instance is held by its containing staff, make sure staff
     * ledgers collection is updated.
     *
     * @param extensive true for non-manual removals only
     * @see #added()
     */
    @Override
    public void remove (boolean extensive)
    {
        if (staff != null) {
            staff.removeLedger(this);
        }

        super.remove(extensive);
    }

    /**
     * @param index the index to set
     */
    public void setIndex (int index)
    {
        this.index = index;
    }
}
