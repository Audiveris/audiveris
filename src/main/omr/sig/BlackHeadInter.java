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
     * Creates a new BlackHeadInter object from a distance-matching
     * retrieval.
     *
     * @param box   the object bounds
     * @param impacts the grade details
     * @param pitch the note pitch
     */
    public BlackHeadInter (Rectangle box,
                           GradeImpacts impacts,
                           int pitch)
    {
        super(box, Shape.NOTEHEAD_BLACK, impacts, pitch);
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
    @Deprecated
    public static class Impacts
            extends BasicImpacts
    {
        //~ Static fields/initializers -----------------------------------------

        private static final String[] NAMES = new String[]{"shape", "pitch"};

        private static final double[] WEIGHTS = new double[]{1, 1};

        //~ Constructors -------------------------------------------------------
        public Impacts (double shape,
                        double pitch)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, shape);
            setImpact(1, pitch);
        }
    }
}
