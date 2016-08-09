//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    V o i d H e a d I n t e r                                   //
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

import omr.glyph.Shape;

import omr.image.Anchored.Anchor;

import omr.sheet.Staff;

import omr.sig.GradeImpacts;

import java.awt.Point;
import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code VoidHeadInter} represents a void note head interpretation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "void-head")
public class VoidHeadInter
        extends AbstractHeadInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new VoidHeadInter object.
     *
     * @param pivot   the template pivot
     * @param anchor  relative pivot configuration
     * @param bounds  the object bounds
     * @param impacts the grade details
     * @param staff   the related staff
     * @param pitch   the note pitch
     */
    public VoidHeadInter (Point pivot,
                          Anchor anchor,
                          Rectangle bounds,
                          GradeImpacts impacts,
                          Staff staff,
                          double pitch)
    {
        super(pivot, anchor, bounds, Shape.NOTEHEAD_VOID, impacts, staff, pitch);
    }

    private VoidHeadInter ()
    {
        super(null, null, null, null, null, null, 0);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // duplicate //
    //-----------//
    /**
     * Duplicate this void note head.
     *
     * @return the (mirror) void head
     */
    public VoidHeadInter duplicate ()
    {
        VoidHeadInter clone = new VoidHeadInter(pivot, anchor, bounds, impacts, staff, pitch);
        clone.setGlyph(this.glyph);
        clone.setMirror(this);

        if (impacts == null) {
            clone.setGrade(this.grade);
        }

        sig.addVertex(clone);
        setMirror(clone);

        return clone;
    }

    //------------------//
    // duplicateAsBlack //
    //------------------//
    /**
     * Duplicate this void note head into a <b>black</b> note head.
     *
     * @return the (mirror) <b>black</b> head
     */
    public BlackHeadInter duplicateAsBlack ()
    {
        BlackHeadInter clone = new BlackHeadInter(pivot, anchor, bounds, impacts, staff, pitch);
        clone.setGlyph(this.glyph);
        clone.setMirror(this);

        if (impacts == null) {
            clone.setGrade(this.grade);
        }

        sig.addVertex(clone);
        setMirror(clone);

        return clone;
    }
}
