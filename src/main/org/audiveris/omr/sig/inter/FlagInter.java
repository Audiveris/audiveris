//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F l a g I n t e r                                       //
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code FlagInter} represents one or several flags.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "flag")
public class FlagInter
        extends AbstractFlagInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new FlagInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public FlagInter (Glyph glyph,
                      Shape shape,
                      double grade)
    {
        super(glyph, shape, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected FlagInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // delete //
    //--------//
    @Override
    public void delete (boolean extensive)
    {
        // Remove it from containing measure
        MeasureStack stack = sig.getSystem().getMeasureStackAt(getCenter());

        if (stack != null) {
            stack.removeInter(this);
        }

        super.delete(extensive);
    }

    //---------//
    // getPart //
    //---------//
    @Override
    public Part getPart ()
    {
        if (part == null) {
            // Flag -> Stem
            for (Relation fsRel : sig.getRelations(this, FlagStemRelation.class)) {
                StemInter stem = (StemInter) sig.getOppositeInter(this, fsRel);

                // Stem -> Head
                for (Relation hsRel : sig.getRelations(stem, HeadStemRelation.class)) {
                    Inter head = sig.getOppositeInter(stem, hsRel);

                    return part = head.getPart();
                }
            }
        }

        return super.getPart();
    }

    //----------//
    // getStaff //
    //----------//
    @Override
    public Staff getStaff ()
    {
        if (staff == null) {
            // Flag -> Stem
            for (Relation fsRel : sig.getRelations(this, FlagStemRelation.class)) {
                StemInter stem = (StemInter) sig.getOppositeInter(this, fsRel);

                // Stem -> Head
                for (Relation hsRel : sig.getRelations(stem, HeadStemRelation.class)) {
                    Inter head = sig.getOppositeInter(stem, hsRel);

                    return staff = head.getStaff();
                }
            }
        }

        return staff;
    }

    //----------//
    // undelete //
    //----------//
    @Override
    public void undelete ()
    {
        super.undelete();

        MeasureStack stack = sig.getSystem().getMeasureStackAt(getCenter());

        if (stack != null) {
            stack.addInter(this);
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
    }
}
