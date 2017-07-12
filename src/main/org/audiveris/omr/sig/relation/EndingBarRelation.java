//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                E n d i n g B a r R e l a t i o n                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import org.audiveris.omr.util.HorizontalSide;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code EndingBarRelation} connects an ending side with a bar line.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "ending-bar")
public class EndingBarRelation
        extends AbstractRelation
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Which side of ending is used?. */
    @XmlAttribute(name = "side")
    private final HorizontalSide endingSide;

    /** Horizontal delta (in interline) between bar line and ending side. */
    private final double xDistance;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new EndingBarRelation object.
     *
     * @param endingSide which side of ending
     * @param xDistance  horizontal delta
     */
    public EndingBarRelation (HorizontalSide endingSide,
                              double xDistance)
    {
        this.endingSide = endingSide;
        this.xDistance = xDistance;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private EndingBarRelation ()
    {
        this.endingSide = null;
        this.xDistance = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * @return the endingSide
     */
    public HorizontalSide getEndingSide ()
    {
        return endingSide;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(endingSide).append("@(").append(String.format("%.2f", xDistance)).append(")");

        return sb.toString();
    }
}
