//----------------------------------------------------------------------------//
//                                                                            //
//                            E v a l u a t i o n                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.Constant;

/**
 * Class {@code Evaluation} gathers a glyph shape, its grade and,
 * if any, details about its failure (name of the check that failed).
 *
 * @author Hervé Bitteur
 */
public class Evaluation
        implements Comparable<Evaluation>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Absolute confidence in shape manually assigned by the user. */
    public static final double MANUAL = 300;

    /** Confidence for in structurally assigned. */
    public static final double ALGORITHM = 200;

    //~ Instance fields --------------------------------------------------------
    /** The evaluated shape. */
    public Shape shape;

    /**
     * The evaluation grade (larger is better), generally provided by
     * the neural network evaluator in the range 0 - 100.
     */
    public double grade;

    /** The specific check that failed, if any. */
    public Failure failure;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // Evaluation //
    //------------//
    /**
     * Create an initialized evaluation instance.
     *
     * @param shape the shape this evaluation measures
     * @param grade the measurement result (larger is better)
     */
    public Evaluation (Shape shape,
                       double grade)
    {
        this.shape = shape;
        this.grade = grade;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // compareTo //
    //-----------//
    /**
     * To sort from best to worst.
     *
     * @param that the other evaluation instance
     * @return -1,0 or +1
     */
    @Override
    public int compareTo (Evaluation that)
    {
        if (this.grade > that.grade) {
            return -1;
        }

        if (this.grade < that.grade) {
            return +1;
        }

        return 0;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(shape);
        sb.append("(");

        if (grade == MANUAL) {
            sb.append("MANUAL");
        } else if (grade == ALGORITHM) {
            sb.append("ALGORITHM");
        } else {
            sb.append((float) grade);
        }

        if (failure != null) {
            sb.append(" failure:")
                    .append(failure);
        }

        sb.append(")");

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------
    //---------//
    // Failure //
    //---------//
    /**
     * A class to handle which specific check has failed in the
     * evaluation.
     */
    public static class Failure
    {
        //~ Instance fields ----------------------------------------------------

        /** The name of the test that failed. */
        public final String test;

        //~ Constructors -------------------------------------------------------
        public Failure (String test)
        {
            this.test = test;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public String toString ()
        {
            return test;
        }
    }

    //-------//
    // Grade //
    //-------//
    /**
     * A subclass of Constant.Double, meant to store a grade constant.
     */
    public static class Grade
            extends Constant.Double
    {
        //~ Constructors -------------------------------------------------------

        /**
         * Specific constructor, where unit & name are assigned later.
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public Grade (double defaultValue,
                      java.lang.String description)
        {
            super("Grade", defaultValue, description);
        }
    }
}
