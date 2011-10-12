//----------------------------------------------------------------------------//
//                                                                            //
//                            E v a l u a t i o n                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.Constant;

/**
 * Class <code>Evaluation</code> gathers a glyph shape, its doubt and, if any,
 * details about its failure (name of the check that failed)
 *
 * @author Hervé Bitteur
 */
public class Evaluation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Absolutely no doubt for shape manually assigned by the user */
    public static final double MANUAL = -1;

    /** No doubt for shape structurally assigned */
    public static final double ALGORITHM = 0;

    //~ Instance fields --------------------------------------------------------

    /** The evaluated shape */
    public Shape shape;

    /**
     * The evaluation doubt (smaller is better), generally provided by the
     * neural network evaluator
     */
    public double doubt;

    /**
     * The specific check that failed, if any.
     */
    public Failure failure;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // Evaluation //
    //------------//
    /**
     * Create an initialized evaluation
     *
     *
     * @param shape the shape this evaluation measures
     * @param doubt the measurement result (smaller is better)
     */
    public Evaluation (Shape  shape,
                       double doubt)
    {
        this.shape = shape;
        this.doubt = doubt;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Just a readable output
     *
     * @return an ascii description of the evaluation
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(shape);
        sb.append("(");

        if (doubt == MANUAL) {
            sb.append("MANUAL");
        } else if (doubt == ALGORITHM) {
            sb.append("ALGORITHM");
        } else {
            sb.append((float) doubt);
        }

        if (failure != null) {
            sb.append(" failure:")
              .append(failure);
        }

        sb.append(")");

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------//
    // Doubt //
    //-------//
    /**
     * A subclass of Constant.Double, meant to store a doubt constant.
     */
    public static class Doubt
        extends Constant.Double
    {
        //~ Constructors -------------------------------------------------------

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public Doubt (double           defaultValue,
                      java.lang.String description)
        {
            super("Doubt", defaultValue, description);
        }
    }

    //---------//
    // Failure //
    //---------//
    /**
     * A class to handle which specific check has failed in the evaluation
     */
    public static class Failure
    {
        //~ Instance fields ----------------------------------------------------

        /** The name of the test that failed */
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
}
