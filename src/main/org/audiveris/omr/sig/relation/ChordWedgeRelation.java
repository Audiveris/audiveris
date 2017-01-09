//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               C h o r d W e d g e R e l a t i o n                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ChordWedgeRelation} represents a support relation between a chord and
 * a wedge nearby.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "chord-wedge")
public class ChordWedgeRelation
        extends AbstractSupport
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Left or right side of the wedge. */
    @XmlAttribute
    private final HorizontalSide side;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ChordWedgeRelation} object.
     *
     * @param side which side of the wedge
     */
    public ChordWedgeRelation (HorizontalSide side)
    {
        this.side = side;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private ChordWedgeRelation ()
    {
        this.side = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getSide //
    //---------//
    /**
     * @return the side
     */
    public HorizontalSide getSide ()
    {
        return side;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return super.toString() + "/" + side;
    }
}
