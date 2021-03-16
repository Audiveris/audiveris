//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S m a l l C h o r d I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SmallChordInter} is a chord composed of small heads,
 * typically (slashed) Acciaccatura and (un-slashed) Appoggiatura.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "small-chord")
public class SmallChordInter
        extends HeadChordInter
{

    /**
     * Creates a new {@code SmallChordInter} object.
     *
     * @param grade the intrinsic grade
     */
    public SmallChordInter (Double grade)
    {
        super(grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private SmallChordInter ()
    {
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "SmallChord";
    }
}
