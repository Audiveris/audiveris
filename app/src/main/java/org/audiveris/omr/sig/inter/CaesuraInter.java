//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C a e s u r a I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>CaesuraInter</code> represents a caesura sign above a staff.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "caesura")
public class CaesuraInter
        extends AbstractPauseInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CaesuraInter.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private CaesuraInter ()
    {
    }

    /**
     * Creates a new <code>CaesuraInter</code> object.
     *
     * @param glyph the caesura glyph
     * @param grade the interpretation quality
     */
    public CaesuraInter (Glyph glyph,
                         Double grade)
    {
        super(glyph, Shape.CAESURA, grade);
    }
}
