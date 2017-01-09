//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S m a l l C h o r d I n t e r                                 //
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
package org.audiveris.omr.sig.inter;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SmallChordInter} is a AbstractChordInter composed of small heads.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "small-chord")
public class SmallChordInter
        extends AbstractChordInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code SmallChordInter} object.
     *
     * @param grade the intrinsic grade
     */
    public SmallChordInter (double grade)
    {
        super(grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private SmallChordInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "SmallChord";
    }
}
