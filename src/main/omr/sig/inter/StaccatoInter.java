//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S t a c c a t o I n t e r                                   //
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

import omr.sheet.rhythm.Voice;

import omr.sig.relation.Relation;
import omr.sig.relation.ChordStaccatoRelation;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code StaccatoInter} represents a staccato dot.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "staccato")
public class StaccatoInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new StaccatoInter object.
     *
     * @param glyph underlying glyph
     * @param grade evaluation value
     */
    public StaccatoInter (Glyph glyph,
                          double grade)
    {
        super(glyph, null, Shape.STACCATO, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private StaccatoInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, ChordStaccatoRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }
}
