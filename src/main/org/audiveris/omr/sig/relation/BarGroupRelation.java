//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 B a r G r o u p R e l a t i o n                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sig.relation;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BarGroupRelation} groups 2 bar lines.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "bar-group")
public class BarGroupRelation
        extends Relation
{

    /** Horizontal white gap (in interline) between the two bar lines. */
    private final double xGap;

    /**
     * Creates a new BarGroupRelation object.
     *
     * @param xGap white gap between the two grouped bar lines
     */
    public BarGroupRelation (double xGap)
    {
        this.xGap = xGap;
    }

    // For JAXB
    private BarGroupRelation ()
    {
        this.xGap = 0;
    }

    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        return false;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        return false;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append("@(").append(String.format("%.2f", xGap)).append(")");

        return sb.toString();
    }
}
