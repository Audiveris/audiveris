//----------------------------------------------------------------------------//
//                                                                            //
//                         B l a c k H e a d I n t e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import java.awt.Rectangle;

/**
 * Class {@code BlackHeadInter} represents a black note head
 * interpretation.
 *
 * @author Hervé Bitteur
 */
public class BlackHeadInter
        extends AbstractNoteInter
{
    //~ Constructors -----------------------------------------------------------

    //----------------//
    // BlackHeadInter //
    //----------------//
    /**
     * Creates a new BlackHeadInter object from a closing-based
     * retrieval.
     *
     * @param glyph   the underlying glyph
     * @param impacts the grade details
     * @param pitch   the note pitch
     */
    public BlackHeadInter (Glyph glyph,
                           GradeImpacts impacts,
                           int pitch)
    {
        super(glyph, Shape.NOTEHEAD_BLACK, impacts, pitch);
    }

    //----------------//
    // BlackHeadInter //
    //----------------//
    /**
     * Creates a new BlackHeadInter object from a distance-matching
     * retrieval.
     *
     * @param box   the object bounds
     * @param grade the assignment quality
     * @param pitch the note pitch
     */
    public BlackHeadInter (Rectangle box,
                           double grade,
                           int pitch)
    {
        super(box, Shape.NOTEHEAD_BLACK, grade, pitch);
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // getMinGrade //
    //-------------//
    public static double getMinGrade ()
    {
        return AbstractInter.getMinGrade();
    }

    //~ Inner Classes ----------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            implements GradeImpacts
    {
        //~ Instance fields ----------------------------------------------------

        final double shape;

        final double pitch;

        //~ Constructors -------------------------------------------------------
        public Impacts (double shape,
                        double pitch)
        {
            this.shape = shape;
            this.pitch = pitch;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public double getGrade ()
        {
            return (shape + pitch) / 2;
        }

        @Override
        public String toString ()
        {
            return String.format("shape:%.2f pitch:%.2f", shape, pitch);
        }
    }
}
