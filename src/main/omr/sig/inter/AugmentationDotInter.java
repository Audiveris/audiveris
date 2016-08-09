//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A u g m e n t a t i o n D o t I n t e r                            //
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

import omr.sheet.Part;
import omr.sheet.rhythm.Voice;

import omr.sig.relation.AugmentationRelation;
import omr.sig.relation.DoubleDotRelation;
import omr.sig.relation.Relation;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code AugmentationDotInter} represent an augmentation dot for a note or a rest.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "augmentation-dot")
public class AugmentationDotInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code AugmentationDotInter} object.
     *
     * @param glyph underlying glyph
     * @param grade evaluation value
     */
    public AugmentationDotInter (Glyph glyph,
                                 double grade)
    {
        super(glyph, null, Shape.AUGMENTATION_DOT, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private AugmentationDotInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getPart //
    //---------//
    @Override
    public Part getPart ()
    {
        if (part == null) {
            for (Relation rel : sig.getRelations(
                    this,
                    AugmentationRelation.class,
                    DoubleDotRelation.class)) {
                return part = sig.getOppositeInter(this, rel).getPart();
            }
        }

        return part;
    }

    //--------------------------//
    // getSecondAugmentationDot //
    //--------------------------//
    /**
     * Report the second augmentation dot, if any, that is linked to this (first)
     * augmentation dot.
     *
     * @return the second dot, if any, or null
     */
    public AugmentationDotInter getSecondAugmentationDot ()
    {
        for (Relation dd : sig.getRelations(this, DoubleDotRelation.class)) {
            return (AugmentationDotInter) sig.getOppositeInter(this, dd);
        }

        return null;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(
                this,
                AugmentationRelation.class,
                DoubleDotRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }
}
