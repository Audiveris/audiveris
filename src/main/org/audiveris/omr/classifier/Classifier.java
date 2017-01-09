//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       C l a s s i f i e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.classifier;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.SystemInfo;

import java.util.EnumSet;

/**
 * Interface {@code Classifier} defines the features of a glyph shape classifier.
 * <p>
 * <img src="doc-files/Classifier.png">
 *
 * @author Hervé Bitteur
 */
public interface Classifier
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Number of shapes to differentiate. */
    public static final int SHAPE_COUNT = 1 + Shape.LAST_PHYSICAL_SHAPE.ordinal();

    /** Empty conditions set. */
    public static final EnumSet<Condition> NO_CONDITIONS = EnumSet.noneOf(Condition.class);

    //~ Enumerations -------------------------------------------------------------------------------
    /** Optional conditions for evaluation. */
    public static enum Condition
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Make sure the shape is not blacklisted by the glyph at hand. */
        ALLOWED,
        /** Make sure all specific checks are successfully passed. */
        CHECKED;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report the sorted sequence of best evaluation(s) found by the classifier on the
     * provided glyph.
     *
     * @param glyph      the glyph to evaluate
     * @param system     the system containing the glyph to evaluate
     * @param count      the desired maximum sequence length, min 1 and max SHAPE_COUNT
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
     * Report the sorted sequence of best evaluation(s) found by the classifier on the
     * provided glyph, with no system but an interline value.
     *
     * @param glyph      the glyph to evaluate
     * @param interline  the relevant scaling information
     * @param count      the desired maximum sequence length min 1 and max SHAPE_COUNT
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
     * Report the name of this classifier.
     *
     * @return the classifier declared name
     */
    String getName ();

    /**
     * Run the classifier with the specified glyph, and return the natural sequence of
     * all interpretations (ordered by Shape ordinal) with no additional check.
     *
     * @param glyph     the glyph to be examined
     * @param interline the relevant scaling interline
     * @return all shape-ordered evaluations
     */
    Evaluation[] getNaturalEvaluations (Glyph glyph,
                                        int interline);

    /**
     * Use a threshold on glyph weight, to tell if the provided glyph is just {@link
     * Shape#NOISE} or a real glyph.
     *
     * @param glyph     the glyph to be checked
     * @param interline the relevant scaling interline
     * @return true if not noise, false otherwise
     */
    boolean isBigEnough (Glyph glyph,
                         int interline);

    /**
     * Use a threshold on glyph weight, to tell if the provided glyph is just {@link
     * Shape#NOISE} or a real glyph.
     *
     * @param weight the <b>normalized</b> glyph weight
     * @return true if not noise, false otherwise
     */
    boolean isBigEnough (double weight);
}
