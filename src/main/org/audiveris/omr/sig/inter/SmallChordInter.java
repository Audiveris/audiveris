//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S m a l l C h o r d I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>SmallChordInter</code> is a general head chord composed of <b>small</b> heads.
 * <p>
 * For specific grace structures (acciaccatura and appoggiatura) we use sub-class
 * {@link GraceChordInter}.
 *
 * @see GraceChordInter
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "small-chord")
public class SmallChordInter
        extends HeadChordInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>SmallChordInter</code> object.
     *
     * @param grade the intrinsic grade
     */
    public SmallChordInter (Double grade)
    {
        super(grade);
    }

    /**
     * Protected constructor meant for GraceChordInter from a glyph recognized as a grace note.
     *
     * @param glyph underlying glyph
     * @param shape GRACE_NOTE, GRACE_NOTE_DOWN, GRACE_NOTE_SLASH or GRACE_NOTE_SLASH_DOWN
     * @param grade evaluation value
     */
    protected SmallChordInter (Glyph glyph,
                               Shape shape,
                               Double grade)
    {
        super(glyph, shape, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected SmallChordInter ()
    {
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

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "SmallChord";
    }
}
