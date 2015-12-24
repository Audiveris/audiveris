//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    V o i d H e a d I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
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
