//----------------------------------------------------------------------------//
//                                                                            //
//                               S t e m I n t e r                            //
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

/**
 * Class {@code StemInter} represents instances of Stem
 * interpretations.
 *
 * @author Hervé Bitteur
 */
public class StemInter
        extends BasicInter
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new StemInter object.
     *
     * @param glyph the underlying glyph
     * @param grade the assignment quality
     */
    public StemInter (Glyph glyph,
                      double grade)
    {
        super(glyph, Shape.STEM, grade);
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
}
