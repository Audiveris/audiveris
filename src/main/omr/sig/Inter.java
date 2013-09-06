//----------------------------------------------------------------------------//
//                                                                            //
//                        I n t e r p r e t a t i o n                         //
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
 * Interface {@code Interpretation} defines a possible interpretation
 * for a glyph.
 *
 * @author Hervé Bitteur
 */
public interface Inter
        extends VisitableInter
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report the glyph which is concerned by this interpretation.
     *
     * @return the glyph
     */
    Glyph getGlyph ();

    /**
     * @return the grade
     */
    double getGrade ();

    /**
     * @return the shape
     */
    Shape getShape ();

    /**
     * Report whether the interpretation has a good grade.
     *
     * @return true if grade is good
     */
    boolean isGood ();

    /**
     * @param grade the grade to set
     */
    void setGrade (double grade);

    /**
     * @param shape the shape to set (AVOID ASAP)
     */
    void setShape (Shape shape);
}
