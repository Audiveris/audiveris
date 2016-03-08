//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       C l a s s i f i e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.sheet.SystemInfo;

import java.util.EnumSet;

/**
 * Interface {@code Classifier} defines the features of a glyph shape classifier.
 *
 * @author Hervé Bitteur
 */
public interface Classifier
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Empty conditions set */
    public static final EnumSet<Condition> NO_CONDITIONS = EnumSet.noneOf(Condition.class);

    //~ Enumerations -------------------------------------------------------------------------------
    /** Conditions for evaluation */
    public static enum Condition
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Make sure the shape is not blacklisted by the glyph at hand */
        ALLOWED,
        /** Make
         * sure all specific checks are successfully passed */
        CHECKED;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report the sorted sequence of best evaluation(s) found by the evaluator on the
     * provided glyph.
     *
     * @param glyph      the glyph to evaluate
     * @param system     the system containing the glyph to evaluate
     * @param count      the desired maximum sequence length
     * @param minGrade   the minimum evaluation grade to be acceptable
     * @param conditions optional conditions, perhaps empty
     * @return the sequence of evaluations, perhaps empty but not null
     */
    Evaluation[] evaluate (Glyph glyph,
                           SystemInfo system,
                           int count,
                           double minGrade,
                           EnumSet<Condition> conditions);

    /**
     * Report the sorted sequence of best evaluation(s) found by the evaluator on the
     * provided glyph, with no system but an interline value.
     *
     * @param glyph      the glyph to evaluate
     * @param interline  scaling information
     * @param count      the desired maximum sequence length
     * @param minGrade   the minimum evaluation grade to be acceptable
     * @param conditions optional conditions, perhaps empty
     * @return the sequence of evaluations, perhaps empty but not null
     */
    Evaluation[] evaluate (Glyph glyph,
                           int interline,
                           int count,
                           double minGrade,
                           EnumSet<Condition> conditions);

    /**
     * Report the evaluation of a glyph regarding a specific shape.
     *
     * @param glyph     the glyph to evaluate
     * @param interline the global sheet interline
     * @param shape     the specific shape to evaluate
     * @return the evaluation
     */
    Evaluation evaluateAs (Glyph glyph,
                           int interline,
                           Shape shape);

    /**
     * Report the name of this evaluator.
     *
     * @return the evaluator declared name
     */
    String getName ();

    /**
     * Run the evaluator with the specified glyph, and return the natural sequence of
     * all interpretations (ordered by Shape ordinal) with no additional check.
     *
     * @param glyph     the glyph to be examined
     * @param interline the global sheet interline
     * @return all shape-ordered evaluations
     */
    Evaluation[] getNaturalEvaluations (Glyph glyph,
                                        int interline);

    /**
     * Use a threshold on glyph weight, to tell if the provided glyph is just {@link
     * Shape#NOISE}, or a real glyph.
     *
     * @param glyph     the glyph to be checked
     * @param interline the global sheet interline
     * @return true if not noise, false otherwise
     */
    boolean isBigEnough (Glyph glyph,
                         int interline);

    /**
     * Use a threshold on glyph weight, to tell if the provided glyph is just {@link
     * Shape#NOISE}, or a real glyph.
     *
     * @param weight the <b>normalized</b> glyph weight
     * @return true if not noise, false otherwise
     */
    boolean isBigEnough (double weight);
}
