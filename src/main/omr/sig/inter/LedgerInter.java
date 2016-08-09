//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       L e d g e r I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sig.inter;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.sig.GradeImpacts;

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

    //--------//
    // delete //
    //--------//
    /**
     * Since a ledger instance is held by its containing staff, make sure staff
     * ledgers collection is updated.
     */
    @Override
    public void delete ()
    {
        if (staff != null) {
            staff.removeLedger(this);
        }

        super.delete();
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
