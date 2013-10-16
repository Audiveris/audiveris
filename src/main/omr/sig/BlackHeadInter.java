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

import java.awt.Rectangle;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

/**
 * Class {@code BlackHeadInter} represents a black note head
 * interpretation.
 *
 * @author Hervé Bitteur
 */
public class BlackHeadInter
        extends BasicInter
{
    //~ Instance fields --------------------------------------------------------

    /** Pitch step. */
    private final int pitch;

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
                           Impacts impacts,
                           int pitch)
    {
        super(glyph, Shape.NOTEHEAD_BLACK, impacts.computeGrade());
        this.setImpacts(impacts);
        this.pitch = pitch;
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
        super(box, Shape.NOTEHEAD_BLACK, grade);
        this.pitch = pitch;
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-------------//
    // getMinGrade //
    //-------------//
    public static double getMinGrade ()
    {
        return BasicInter.getMinGrade();
    }

    //----------//
    // getPitch //
    //----------//
    /**
     * @return the pitch
     */
    public int getPitch ()
    {
        return pitch;
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
        public double computeGrade ()
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
