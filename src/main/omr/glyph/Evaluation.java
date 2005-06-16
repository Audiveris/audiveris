//-----------------------------------------------------------------------//
//                                                                       //
//                          E v a l u a t i o n                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph;

/**
 * Class <code>Evaluation</code> gathers a pair composed of a glyph
 * shape and its grade.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Evaluation
{
    //~ Instance variables ------------------------------------------------

    /** The evaluated shape */
    public Shape shape;

    /** The evaluation grade (smaller is better) */
    public double grade;

    //~ Constructors ------------------------------------------------------

    //------------//
    // Evaluation //
    //------------//
    /**
     * Create an uninitialized evaluation
     */
    public Evaluation()
    {
    }

    //------------//
    // Evaluation //
    //------------//
    /**
     * Create an initialized evaluation
     *
     * @param shape the shape this evaluation measures
     * @param grade the measurement result (smaller is better)
     */
    public Evaluation(Shape shape,
                      double grade)
    {
        this.shape = shape;
        this.grade = grade;
    }

    //~ Methods -----------------------------------------------------------

    /**
     * Just a readable output
     *
     * @return an ascii description of the evaluation
     */
    @Override
        public String toString ()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(shape);
        sb.append("(").append((float) grade).append(")");

        return sb.toString();
    }
}
