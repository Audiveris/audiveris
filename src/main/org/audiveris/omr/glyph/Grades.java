//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          G r a d e s                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import org.audiveris.omr.constant.Constant;
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

    /** Minimum grade for a validation (during training phase). */
    public static final double validationMinGrade = constants.validationMinGrade.getValue();

    /** Minimum grade for a time glyph. */
    public static final double timeMinGrade = constants.timeMinGrade.getValue();

    /** Minimum grade for a symbol glyph. */
    public static final double symbolMinGrade = constants.symbolMinGrade.getValue();

    /** Minimum grade for a key signature item, phase #1 (component-based). */
    public static final double keyAlterMinGrade1 = constants.keyAlterMinGrade1.getValue();

    /** Minimum grade for a key signature item, phase #2 (staff slice-based). */
    public static final double keyAlterMinGrade2 = constants.keyAlterMinGrade2.getValue();

    /** Minimum grade for a key signature. */
    public static final double keySigMinGrade = constants.keySigMinGrade.getValue();

    // Minimum specific values
    //------------------------
    /** Minimum grade for a clef glyph. */
    public static final double clefMinGrade = constants.clefMinGrade.getValue();

    /** Grade for a good beam. */
    public static final double goodBeamGrade = constants.goodBeamGrade.getValue();

    /** Grade for a good BarConnector. */
    public static final double goodBarConnectorGrade = constants.goodBarConnectorGrade.getValue();

    /** Grade for a rather good head. */
    public static final double ratherGoodHeadGrade = constants.ratherGoodHeadGrade.getValue();

    /** Grade for a good relation. */
    public static final double goodRelationGrade = constants.goodRelationGrade.getValue();

    /** The minimum contextual grade for an interpretation. */
    public static final double minContextualGrade = constants.minContextualGrade.getValue();

    // Minimum global values
    //----------------------
    /** Ratio applied on intrinsic value, to leave room for contextual. */
    public static final double intrinsicRatio = constants.intrinsicRatio.getValue();

    /** The minimum grade to consider an interpretation as acceptable. */
    public static final double minInterGrade = intrinsicRatio * constants.minInterGrade.getValue();

    /** Grade for a good interpretation. */
    public static final double goodInterGrade = intrinsicRatio * constants.goodInterGrade.getValue();

    //~ Constructors -------------------------------------------------------------------------------
    private Grades ()
    {
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio intrinsicRatio = new Constant.Ratio(
                0.8,
                "Reduction ratio applied on any intrinsic grade");

        //
        // Minimum values (please keep them sorted by decreasing value)
        //
        private final Evaluation.Grade validationMinGrade = new Evaluation.Grade(
                0.80,
                "Minimum grade for a validation");

        private final Constant.Ratio minContextualGrade = new Constant.Ratio(
                0.5,
                "Default minimum interpretation contextual grade");

        private final Constant.Ratio goodBarConnectorGrade = new Constant.Ratio(
                0.65,
                "Good interpretation grade for a bar connector");

        private final Constant.Ratio goodInterGrade = new Constant.Ratio(
                0.5,
                "Default good interpretation grade");

        private final Constant.Ratio goodRelationGrade = new Constant.Ratio(
                0.5,
                "Default good relation grade");

        private final Constant.Ratio goodBeamGrade = new Constant.Ratio(
                0.35,
                "Minimum grade for being a good beam");

        private final Constant.Ratio ratherGoodHeadGrade = new Constant.Ratio(
                0.3,
                "Rather good interpretation grade for a head");

        private final Evaluation.Grade symbolMinGrade = new Evaluation.Grade(
                0.15,
                "Minimum grade for a symbol");

        private final Constant.Ratio minInterGrade = new Constant.Ratio(
                0.1,
                "Default minimum interpretation grade");

        private final Evaluation.Grade keyAlterMinGrade1 = new Evaluation.Grade(
                0.1,
                "Minimum grade for a key item symbol, phase 1 (component)");

        private final Evaluation.Grade timeMinGrade = new Evaluation.Grade(
                0.1,
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
