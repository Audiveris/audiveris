//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          G r a d e s                                           //
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
package org.audiveris.omr.glyph;

import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.constant.ConstantSet;

/**
 * Class {@code Grades} gathers in one class all the various evaluation grades used
 * throughout the application.
 *
 * @author Hervé Bitteur
 */
public abstract class Grades
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    // Minimum values
    //---------------
    /** Minimum grade for a clef glyph */
    public static final double clefMinGrade = constants.clefMinGrade.getValue();
//
//    /** Minimum grade for a forte glyph */
//    public static final double forteMinGrade = constants.forteMinGrade.getValue();
//
//    /** Minimum grade for a hook glyph */
//    public static final double hookMinGrade = constants.hookMinGrade.getValue();
//

    /** Minimum grade for a key signature */
    public static final double keySigMinGrade = constants.keySigMinGrade.getValue();

    /** Minimum grade for a key signature item, phase #1 (component-based) */
    public static final double keyAlterMinGrade1 = constants.keyAlterMinGrade1.getValue();

    /** Minimum grade for a key signature item, phase #2 (staff slice-based) */
    public static final double keyAlterMinGrade2 = constants.keyAlterMinGrade2.getValue();
//
//    /** Minimum grade for a glyph left over */
//    public static final double leftOverMinGrade = constants.leftOverMinGrade.getValue();
//
//    /** Minimum grade for a leaf glyph */
//    public static final double ledgerNoteMinGrade = constants.ledgerNoteMinGrade.getValue();
//
//    /** Minimum grade for a merged note */
//    public static final double mergedNoteMinGrade = constants.mergedNoteMinGrade.getValue();
//

    /** Minimum grade for a symbol glyph */
    public static final double symbolMinGrade = constants.symbolMinGrade.getValue();
//
//    /** Minimum grade for a text glyph */
//    public static final double textMinGrade = constants.textMinGrade.getValue();
//

    /** Minimum grade for a time glyph */
    public static final double timeMinGrade = constants.timeMinGrade.getValue();

    /** Minimum grade for a validation */
    public static final double validationMinGrade = constants.validationMinGrade.getValue();

    //~ Constructors -------------------------------------------------------------------------------
    private Grades ()
    {
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        //
        // Minimum values (please keep them sorted by decreasing value)
        //
        private final Evaluation.Grade validationMinGrade = new Evaluation.Grade(
                0.80,
                "Minimum grade for a validation");

        private final Evaluation.Grade symbolMinGrade = new Evaluation.Grade(
                0.15,
                "Minimum grade for a symbol");

        private final Evaluation.Grade keyAlterMinGrade1 = new Evaluation.Grade(
                0.10,
                "Minimum grade for a key item symbol, phase 1 (component)");

        private final Evaluation.Grade timeMinGrade = new Evaluation.Grade(
                0.10,
                "Minimum grade for a time sig");

        private final Evaluation.Grade clefMinGrade = new Evaluation.Grade(
                0.03,
                "Minimum grade for a clef");

        private final Evaluation.Grade keySigMinGrade = new Evaluation.Grade(
                0.01,
                "Minimum grade for a key signature");

        private final Evaluation.Grade keyAlterMinGrade2 = new Evaluation.Grade(
                0.01,
                "Minimum grade for a key item symbol, phase 2 (staff slice)");
    }
}
