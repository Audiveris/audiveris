//----------------------------------------------------------------------------//
//                                                                            //
//                                G r a d e s                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.ConstantSet;

/**
 * Class {@code Grades} gathers in one class all the various
 * evaluation grades used throughout the application.
 *
 * @author Hervé Bitteur
 */
public class Grades
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    // Maximum values
    //---------------
    /** <b>Maximum</b> grade for a glyph before being merged in a compound */
    public static final double compoundPartMaxGrade = constants.compoundPartMaxGrade.getValue();

    // Minimum values
    //---------------
    /** Minimum grade for a bass clef glyph */
    public static final double bassMinGrade = constants.bassMinGrade.getValue();

    /** Minimum grade for a bass clef glyph */
    public static final double clefMinGrade = constants.clefMinGrade.getValue();

    /** Minimum grade for a consistent note */
    public static final double consistentNoteMinGrade = constants.consistentNoteMinGrade.getValue();

    /** Minimum grade for a hook glyph */
    public static final double forteMinGrade = constants.forteMinGrade.getValue();

    /** Minimum grade for a hook glyph */
    public static final double hookMinGrade = constants.hookMinGrade.getValue();

    /** Minimum grade for a key signature */
    public static final double keySigMinGrade = constants.keySigMinGrade.getValue();

    /** Minimum grade for a glyph left over */
    public static final double leftOverMinGrade = constants.leftOverMinGrade.getValue();

    /** Minimum grade for a leaf glyph */
    public static final double ledgerNoteMinGrade = constants.ledgerNoteMinGrade.getValue();

    /** Minimum grade for a merged note */
    public static final double mergedNoteMinGrade = constants.mergedNoteMinGrade.getValue();

    /** Minimum grade for a part of split glyph */
    public static final double partMinGrade = constants.partMinGrade.getValue();

    /** Minimum grade for a patterns-issued glyph */
    public static final double patternsMinGrade = constants.patternsMinGrade.getValue();

    /** Minimum grade for a symbol glyph */
    public static final double symbolMinGrade = constants.symbolMinGrade.getValue();

    /** Minimum grade for a text glyph */
    public static final double textMinGrade = constants.textMinGrade.getValue();

    /** Minimum grade for a time glyph */
    public static final double timeMinGrade = constants.timeMinGrade.getValue();

    /** Minimum grade for a validation */
    public static final double validationMinGrade = constants.validationMinGrade.getValue();

    /** No minimum grade */
    public static final double noMinGrade = 0;

    //~ Constructors -----------------------------------------------------------
    private Grades ()
    {
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Evaluation.Grade compoundPartMaxGrade = new Evaluation.Grade(
                90,
                "*MAXIMUM* grade for a suitable compound part");

        //----------------------------------------------------------------------
        // Minimum values (please keep them sorted by decreasing value)
        //
        Evaluation.Grade validationMinGrade = new Evaluation.Grade(
                83,
                "Minimum grade for a validation");

        //
        Evaluation.Grade symbolMinGrade = new Evaluation.Grade(
                77,
                "Minimum grade for a symbol");

        //
        Evaluation.Grade patternsMinGrade = new Evaluation.Grade(
                67,
                "Minimum grade for pattern phase");

        //
        Evaluation.Grade partMinGrade = new Evaluation.Grade(
                35,
                "Minimum grade for a part of a split glyph");

        //
        Evaluation.Grade bassMinGrade = new Evaluation.Grade(
                33,
                "Minimum grade for a bass clef");

        //
        Evaluation.Grade hookMinGrade = new Evaluation.Grade(
                20,
                "Minimum grade for beam hook verification");

        //
        Evaluation.Grade mergedNoteMinGrade = new Evaluation.Grade(
                20,
                "Minimum grade for a merged note");

        //
        Evaluation.Grade leftOverMinGrade = new Evaluation.Grade(
                10,
                "Minimum grade for a glyph left over");

        //
        Evaluation.Grade ledgerNoteMinGrade = new Evaluation.Grade(
                10,
                "Minimum grade for a ledger note");

        //
        Evaluation.Grade clefMinGrade = new Evaluation.Grade(
                0.3,
                "Minimum grade for a clef");

        //
        Evaluation.Grade consistentNoteMinGrade = new Evaluation.Grade(
                0.1,
                "Minimum grade for a consistent note head");

        //
        Evaluation.Grade forteMinGrade = new Evaluation.Grade(
                0.01,
                "Minimum grade for glyph close to Forte");

        //
        Evaluation.Grade keySigMinGrade = new Evaluation.Grade(
                0.01,
                "Minimum grade for a key signature");

        //
        Evaluation.Grade textMinGrade = new Evaluation.Grade(
                0.01,
                "Minimum grade for a text symbol");

        //
        Evaluation.Grade timeMinGrade = new Evaluation.Grade(
                0,
                "Minimum grade for a time sig");

    }
}
