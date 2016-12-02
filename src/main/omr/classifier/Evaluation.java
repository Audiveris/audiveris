//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      E v a l u a t i o n                                       //
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
package omr.classifier;

import omr.constant.Constant;
import omr.glyph.Shape;

/**
 * Class {@code Evaluation} gathers a glyph shape, its grade and, if any, details about
 * its failure (name of the check that failed).
 *
 * @author Hervé Bitteur
 */
public class Evaluation
        implements Comparable<Evaluation>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Absolute confidence in shape manually assigned by the user. */
    public static final double MANUAL = 3;

    /** Confidence for in structurally assigned. */
    public static final double ALGORITHM = 2;

    //~ Instance fields ----------------------------------------------------------------------------
    /** The evaluated shape. */
    public Shape shape;

    /**
     * The evaluation grade (larger is better), generally provided by
     * the neural network classifier in the range 0 - 1.
     */
    public double grade;

    /** The specific check that failed, if any. */
    public Failure failure;

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
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
        return Double.compare(that.grade, this.grade); // Reverse order: highest to lowest
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
            sb.append(" failure:").append(failure);
        }

        sb.append(")");

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Failure //
    //---------//
    /**
     * A class to handle which specific check has failed in the
     * evaluation.
     */
    public static class Failure
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The name of the test that failed. */
        public final String test;

        //~ Constructors ---------------------------------------------------------------------------
        public Failure (String test)
        {
            this.test = test;
        }

        //~ Methods --------------------------------------------------------------------------------
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
        //~ Constructors ---------------------------------------------------------------------------

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
